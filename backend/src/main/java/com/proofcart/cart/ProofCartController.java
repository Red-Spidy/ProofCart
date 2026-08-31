package com.proofcart.cart;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proofcart.audit.AuditEventService;
import com.proofcart.domain.CartItem;
import com.proofcart.domain.IntentRules;
import com.proofcart.domain.PolicyResult;
import com.proofcart.domain.ProductSnapshot;
import com.proofcart.domain.entity.IntentContractEntity;
import com.proofcart.domain.entity.ProductEntity;
import com.proofcart.domain.entity.ProofCartEntity;
import com.proofcart.domain.repo.IntentContractRepository;
import com.proofcart.domain.repo.ProductRepository;
import com.proofcart.domain.repo.ProofCartRepository;
import com.proofcart.policy.PolicyEngine;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/proof-carts")
public class ProofCartController {

    private final ProofCartRepository proofCartRepository;
    private final ProductRepository productRepository;
    private final IntentContractRepository intentContractRepository;
    private final PolicyEngine policyEngine;
    private final ObjectMapper objectMapper;
    private final AuditEventService audit;

    public ProofCartController(ProofCartRepository proofCartRepository, ProductRepository productRepository, IntentContractRepository intentContractRepository, PolicyEngine policyEngine, ObjectMapper objectMapper, AuditEventService audit) {
        this.proofCartRepository = proofCartRepository;
        this.productRepository = productRepository;
        this.intentContractRepository = intentContractRepository;
        this.policyEngine = policyEngine;
        this.objectMapper = objectMapper;
        this.audit = audit;
    }

    // ─── GET /api/proof-carts/{id} ───────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<?> getCart(@PathVariable UUID id) {
        try {
            ProofCartEntity cart = proofCartRepository.findById(id).orElseThrow();
            String buyerId = getAuthenticatedBuyerId();

            // Ownership check
            if (buyerId != null && !cart.getBuyerId().toString().equals(buyerId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied."));
            }

            List<CartItem> items = objectMapper.readValue(cart.getSnapshotDataJson(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, CartItem.class));
            List<?> policyChecks = objectMapper.readValue(cart.getPolicyChecksJson(), List.class);

            Map<String, Object> policyResult = new LinkedHashMap<>();
            policyResult.put("decision", cart.getPolicyDecision());
            policyResult.put("checks", policyChecks);

            return ResponseEntity.ok(Map.of(
                    "id", cart.getId(),
                    "policyResult", policyResult,
                    "offerHash", cart.getOfferHash(),
                    "items", items
            ));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", "Cart not found"));
        }
    }

    // ─── POST /api/proof-carts ───────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<?> createProofCart(@RequestBody Map<String, Object> request) {
        // Resolve buyer identity from authenticated context (set by BuyerAuthFilter)
        String buyerIdStr = getAuthenticatedBuyerId();
        if (buyerIdStr == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid X-Buyer-Id header."));
        }
        UUID buyerId = UUID.fromString(buyerIdStr);
        UUID merchantId = UUID.fromString(request.get("merchantId").toString());
        Object rawIntentId = request.get("intentContractId");
        UUID intentId = (rawIntentId != null && !rawIntentId.toString().equals("null") && !rawIntentId.toString().isEmpty())
                ? UUID.fromString(rawIntentId.toString()) : null;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> itemsRaw = (List<Map<String, Object>>) request.get("items");

        // ── Input validation ─────────────────────────────────────────────────
        if (itemsRaw == null || itemsRaw.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cart must contain at least one item."));
        }

        Set<String> seenProductIds = new HashSet<>();
        for (Map<String, Object> rawItem : itemsRaw) {
            String pid = rawItem.get("productId") != null ? rawItem.get("productId").toString() : null;
            if (pid == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Each item must have a productId."));
            }
            if (!seenProductIds.add(pid)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Duplicate productId: " + pid));
            }
            Object qtyObj = rawItem.get("quantity");
            if (qtyObj == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Each item must have a quantity."));
            }
            int qty;
            try {
                qty = ((Number) qtyObj).intValue();
            } catch (ClassCastException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Quantity must be a number."));
            }
            if (qty <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Quantity must be a positive integer. Got: " + qty));
            }
        }

        try {
            IntentContractEntity intentEntity = null;
            IntentRules rules = new IntentRules(null, List.of(), List.of(), null, true, false, false, null, 1.0);
            String expiresAt = null;

            if (intentId != null) {
                intentEntity = intentContractRepository.findById(intentId).orElseThrow();
                rules = objectMapper.readValue(intentEntity.getExtractedRulesJson(), IntentRules.class);
                expiresAt = intentEntity.getExpiresAt().toString();
            }

            List<ProductEntity> liveProducts = productRepository.findAll();
            Map<UUID, ProductEntity> productMap = new HashMap<>();
            for (ProductEntity p : liveProducts) productMap.put(p.getId(), p);

            List<CartItem> cartItems = new ArrayList<>();
            int totalPaise = 0;

            for (Map<String, Object> rawItem : itemsRaw) {
                UUID pId = UUID.fromString(rawItem.get("productId").toString());
                int qty = ((Number) rawItem.get("quantity")).intValue();
                ProductEntity liveP = productMap.get(pId);
                if (liveP == null) {
                    liveP = productRepository.findById(pId).orElse(null);
                }
                if (liveP == null) continue;

                ProductSnapshot snap = new ProductSnapshot(
                        liveP.getId().toString(), liveP.getMerchantId().toString(), liveP.getName(), liveP.getDescription(),
                        liveP.getPricePaise(), Math.max(0, liveP.getStockQuantity() - liveP.getReservedQuantity()), liveP.getDietaryTags(), liveP.getAllergens(),
                        liveP.getDeliveryDays(), liveP.getReturnable(), liveP.getSubscriptionAvailable(), liveP.getVersion(),
                        liveP.getUpdatedAt() != null ? liveP.getUpdatedAt().toString() : null, Instant.now().toString()
                );
                int lineTotal = liveP.getPricePaise() * qty;
                cartItems.add(new CartItem(pId.toString(), qty, liveP.getPricePaise(), lineTotal, snap));
                totalPaise += lineTotal;
            }

            PolicyEngine.PolicyEngineInput input = new PolicyEngine.PolicyEngineInput(
                    rules, cartItems, totalPaise, merchantId.toString(), expiresAt, null, null
            );
            PolicyResult result = policyEngine.runPolicyEngine(input);
            String offerHash = policyEngine.computeOfferHash(cartItems, totalPaise);

            ProofCartEntity cart = new ProofCartEntity();
            cart.setBuyerId(buyerId);
            cart.setMerchantId(merchantId);
            cart.setIntentContractId(intentId);
            cart.setTotalPaise(totalPaise);
            cart.setOfferHash(offerHash);
            cart.setSnapshotDataJson(objectMapper.writeValueAsString(cartItems));
            cart.setPolicyDecision(result.decision().name());
            cart.setPolicyChecksJson(objectMapper.writeValueAsString(result.checks()));
            cart.setApproved(false);

            ProofCartEntity saved = proofCartRepository.save(cart);
            audit.record(buyerId, merchantId, saved.getId(), null, "CART_EVALUATED", "Proof cart evaluated: " + result.decision().name());

            return ResponseEntity.ok(Map.of(
                    "id", saved.getId(),
                    "policyResult", result,
                    "offerHash", offerHash,
                    "items", cartItems
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ─── POST /api/proof-carts/{id}/approve ─────────────────────────────────

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approveCart(@PathVariable UUID id) {
        ProofCartEntity cart = proofCartRepository.findById(id).orElseThrow();

        // Ownership check
        String buyerId = getAuthenticatedBuyerId();
        if (buyerId != null && !cart.getBuyerId().toString().equals(buyerId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied: cart belongs to another buyer."));
        }

        if (!"ALLOWED".equals(cart.getPolicyDecision())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cart is not ALLOWED by the policy engine."));
        }
        cart.setApproved(true);
        proofCartRepository.save(cart);
        audit.record(cart.getBuyerId(), cart.getMerchantId(), cart.getId(), null, "CART_APPROVED", "Buyer explicitly approved the proof cart.");
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String getAuthenticatedBuyerId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof String principal) {
            // Reject Spring's default "anonymousUser" principal
            if ("anonymousUser".equals(principal)) return null;
            return principal;
        }
        return null;
    }
}
