package com.proofcart.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proofcart.catalog.CatalogService;
import com.proofcart.domain.CartItem;
import com.proofcart.domain.IntentRules;
import com.proofcart.domain.entity.CheckoutOrderEntity;
import com.proofcart.domain.entity.IntentContractEntity;
import com.proofcart.domain.entity.ProductEntity;
import com.proofcart.domain.entity.ProofCartEntity;
import com.proofcart.domain.repo.CheckoutOrderRepository;
import com.proofcart.domain.repo.IntentContractRepository;
import com.proofcart.domain.repo.ProductRepository;
import com.proofcart.domain.repo.ProofCartRepository;
import com.proofcart.intent.GroqIntentExtractor;
import com.proofcart.upsell.UpsellService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * JSON-RPC bridge exposing read/prepare-only tools to an AI buyer.
 */
@RestController
@RequestMapping("/api/mcp")
public class ProofCartMcpTools {
    private final ProductRepository products;
    private final CatalogService catalog;
    private final GroqIntentExtractor intents;
    private final IntentContractRepository contracts;
    private final ProofCartRepository carts;
    private final CheckoutOrderRepository orders;
    private final ObjectMapper mapper;
    private final UpsellService upsell;

    public ProofCartMcpTools(ProductRepository products, CatalogService catalog, GroqIntentExtractor intents, IntentContractRepository contracts, ProofCartRepository carts, CheckoutOrderRepository orders, ObjectMapper mapper, UpsellService upsell) {
        this.products = products;
        this.catalog = catalog;
        this.intents = intents;
        this.contracts = contracts;
        this.carts = carts;
        this.orders = orders;
        this.mapper = mapper;
        this.upsell = upsell;
    }

    @PostMapping
    public ResponseEntity<?> handle(@RequestBody Map<String, Object> request, Authentication auth) {
        try {
            if (auth == null || auth.getName() == null) return error(request, -32001, "Authentication required");
            Map<String, Object> params = request.get("params") instanceof Map<?, ?> p ? cast(p) : Map.of();
            String name = String.valueOf(params.get("name"));
            Map<String, Object> args = params.get("arguments") instanceof Map<?, ?> p ? cast(p) : params;
            Object result = switch (name) {
                case "search_catalog" -> search(args);
                case "create_intent_contract" -> createIntent(args, UUID.fromString(auth.getName()));
                case "evaluate_proof_cart" -> evaluate(args, UUID.fromString(auth.getName()));
                case "create_checkout_review" -> checkoutReview(args, UUID.fromString(auth.getName()));
                case "suggest_upsell" -> suggestUpsell(args, UUID.fromString(auth.getName()));
                case "get_audit_receipt" -> receipt(args, UUID.fromString(auth.getName()));
                default -> throw new IllegalArgumentException("Unknown tool: " + name);
            };
            return ResponseEntity.ok(Map.of("jsonrpc", "2.0", "id", request.get("id"), "result", Map.of("content", List.of(Map.of("type", "text", "text", mapper.writeValueAsString(result))))));
        } catch (SecurityException e) {
            return error(request, -32003, e.getMessage());
        } catch (Exception e) {
            return error(request, -32602, e.getMessage() == null ? "Invalid request" : e.getMessage());
        }
    }

    private Object search(Map<String, Object> args) {
        UUID merchantId = UUID.fromString(String.valueOf(args.get("merchantId")));
        String q = args.get("query") == null ? null : String.valueOf(args.get("query"));
        List<ProductEntity> found = q == null || q.isBlank() ? products.findByMerchantId(merchantId) : catalog.searchAndSyncCatalog(q);
        return Map.of("products", found.stream().map(this::productMap).toList());
    }

    private Map<String, Object> productMap(ProductEntity p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("merchantId", p.getMerchantId());
        m.put("name", p.getName());
        m.put("description", p.getDescription() == null ? "" : p.getDescription());
        m.put("pricePaise", p.getPricePaise());
        m.put("stockQuantity", Math.max(0, p.getStockQuantity() - p.getReservedQuantity()));
        m.put("dietaryTags", p.getDietaryTags() == null ? List.of() : p.getDietaryTags());
        m.put("allergens", p.getAllergens() == null ? List.of() : p.getAllergens());
        m.put("deliveryDays", p.getDeliveryDays());
        m.put("returnable", p.getReturnable());
        m.put("subscriptionAvailable", p.getSubscriptionAvailable());
        return m;
    }

    private Object createIntent(Map<String, Object> args, UUID buyerId) throws Exception {
        String prompt = String.valueOf(args.get("prompt"));
        if (prompt.isBlank() || "null".equals(prompt)) throw new IllegalArgumentException("prompt is required");
        var extracted = intents.extractIntent(prompt);
        IntentContractEntity e = new IntentContractEntity();
        e.setBuyerId(buyerId);
        e.setRawPrompt(prompt);
        e.setExtractedRulesJson(mapper.writeValueAsString(extracted.rules()));
        e.setConfidence(extracted.rules().confidence());
        e.setExpiresAt(java.time.Instant.now().plusSeconds(3600));
        var saved = contracts.save(e);
        return Map.of("intentId", saved.getId(), "rules", extracted.rules(), "source", extracted.source(), "expiresAt", saved.getExpiresAt());
    }

    private Object evaluate(Map<String, Object> args, UUID buyerId) throws Exception {
        ProofCartEntity c = ownedCart(args, buyerId);
        return Map.of("cartId", c.getId(), "decision", c.getPolicyDecision(), "checks", mapper.readValue(c.getPolicyChecksJson(), List.class), "approved", c.getApproved(), "offerHash", c.getOfferHash());
    }

    private Object checkoutReview(Map<String, Object> args, UUID buyerId) {
        ProofCartEntity c = ownedCart(args, buyerId);
        if (!"ALLOWED".equals(c.getPolicyDecision())) throw new IllegalArgumentException("Cart is not allowed");
        return Map.of("cartId", c.getId(), "reviewUrl", "/checkout/" + c.getId(), "requiresBuyerApproval", !Boolean.TRUE.equals(c.getApproved()));
    }

    private Object suggestUpsell(Map<String, Object> args, UUID buyerId) throws Exception {
        ProofCartEntity c = ownedCart(args, buyerId);
        if (!"ALLOWED".equals(c.getPolicyDecision())) return Map.of("suggestions", List.of());
        List<CartItem> items = mapper.readValue(c.getSnapshotDataJson(),
                mapper.getTypeFactory().constructCollectionType(List.class, CartItem.class));
        IntentRules rules = c.getIntentContractId() == null
                ? new IntentRules(null, List.of(), List.of(), null, true, false, false, null, 1.0)
                : mapper.readValue(contracts.findById(c.getIntentContractId()).orElseThrow().getExtractedRulesJson(), IntentRules.class);
        return Map.of("cartId", c.getId(), "suggestions", upsell.suggest(c.getMerchantId(), items, c.getTotalPaise(), rules));
    }

    private Object receipt(Map<String, Object> args, UUID buyerId) {
        CheckoutOrderEntity o = orders.findById(UUID.fromString(String.valueOf(args.get("orderId")))).orElseThrow(() -> new NoSuchElementException("Order not found"));
        if (!o.getBuyerId().equals(buyerId)) throw new SecurityException("Access denied");
        return Map.of("orderId", o.getId(), "status", o.getStatus(), "amountPaise", o.getAmountPaise(), "razorpayOrderId", o.getRazorpayOrderId());
    }

    private ProofCartEntity ownedCart(Map<String, Object> args, UUID buyerId) {
        ProofCartEntity c = carts.findById(UUID.fromString(String.valueOf(args.get("cartId")))).orElseThrow(() -> new NoSuchElementException("Cart not found"));
        if (!c.getBuyerId().equals(buyerId)) throw new SecurityException("Access denied");
        return c;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> cast(Map<?, ?> m) {
        return (Map<String, Object>) m;
    }

    private ResponseEntity<?> error(Map<String, Object> r, int code, String message) {
        return ResponseEntity.badRequest().body(Map.of("jsonrpc", "2.0", "id", r.get("id"), "error", Map.of("code", code, "message", message == null ? "Request rejected" : message)));}
}
