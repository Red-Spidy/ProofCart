package com.proofcart.checkout;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proofcart.audit.AuditEventService;
import com.proofcart.domain.CartItem;
import com.proofcart.domain.IntentRules;
import com.proofcart.domain.PolicyResult;
import com.proofcart.domain.Product;
import com.proofcart.domain.entity.AgentTokenEntity;
import com.proofcart.domain.entity.CheckoutOrderEntity;
import com.proofcart.domain.entity.IntentContractEntity;
import com.proofcart.domain.entity.ProductEntity;
import com.proofcart.domain.entity.ProofCartEntity;
import com.proofcart.domain.repo.*;
import com.proofcart.inventory.InventoryReservationService;
import com.proofcart.mandate.AgentMandateService;
import com.proofcart.policy.PolicyEngine;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    private final ProofCartRepository cartRepo;
    private final ProductRepository productRepo;
    private final IntentContractRepository intentRepo;
    private final PolicyEngine policyEngine;
    private final CheckoutOrderRepository orderRepo;
    private final ObjectMapper objectMapper;
    private final RazorpayClient razorpayClient;
    private final InventoryReservationService inventory;
    private final InventoryReservationRepository reservations;
    private final AuditEventService audit;
    private final AgentTokenRepository agentTokens;
    private final AgentMandateService mandateService;

    public CheckoutController(
            ProofCartRepository cartRepo,
            ProductRepository productRepo,
            IntentContractRepository intentRepo,
            PolicyEngine policyEngine,
            CheckoutOrderRepository orderRepo,
            ObjectMapper objectMapper,
            InventoryReservationService inventory,
            InventoryReservationRepository reservations,
            @Value("${razorpay.key.id:}") String keyId,
            @Value("${razorpay.key.secret:}") String keySecret,
            AuditEventService audit,
            AgentTokenRepository agentTokens,
            AgentMandateService mandateService) {
        this.cartRepo = cartRepo;
        this.productRepo = productRepo;
        this.intentRepo = intentRepo;
        this.policyEngine = policyEngine;
        this.orderRepo = orderRepo;
        this.objectMapper = objectMapper;
        this.inventory = inventory;
        this.reservations = reservations;
        this.audit = audit;
        this.agentTokens = agentTokens;
        this.mandateService = mandateService;

        RazorpayClient client = null;
        try {
            if (keyId != null && !keyId.isBlank()) {
                client = new RazorpayClient(keyId, keySecret);
            }
        } catch (Exception e) {
            System.err.println("Razorpay client init failed: " + e.getMessage());
        }
        this.razorpayClient = client;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createCheckout(@RequestBody Map<String, String> request) {
        try {
            UUID cartId = UUID.fromString(request.get("cartId"));
            ProofCartEntity cart = cartRepo.findById(cartId).orElseThrow();

            // Ownership check: buyer may only checkout their own cart
            String authenticatedBuyerId = getAuthenticatedBuyerId();
            if (authenticatedBuyerId != null && !cart.getBuyerId().toString().equals(authenticatedBuyerId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied: cart belongs to another buyer."));
            }

            if (!cart.getApproved()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Cart not approved by buyer"));
            }

            CheckoutOrderEntity existing = orderRepo.findFirstByCartIdAndStatusOrderByCreatedAtDesc(cartId, "CREATED");
            if (existing != null) {
                if (reservations.existsByOrderIdAndStatus(existing.getId(), InventoryReservationService.RESERVED)) {
                    return ResponseEntity.ok(Map.of(
                            "orderId", existing.getId(), "razorpayOrderId", existing.getRazorpayOrderId(),
                            "amountPaise", existing.getAmountPaise()));
                }
                existing.setStatus("EXPIRED");
                orderRepo.save(existing);
            }

            // Re-verify policy against current DB state
            IntentRules rules = new IntentRules(null, List.of(), List.of(), null, true, false, false, null, 1.0);
            String expiresAt = null;
            if (cart.getIntentContractId() != null) {
                IntentContractEntity intent = intentRepo.findById(cart.getIntentContractId()).orElseThrow();
                rules = objectMapper.readValue(intent.getExtractedRulesJson(), IntentRules.class);
                expiresAt = intent.getExpiresAt().toString();
            }

            List<ProductEntity> liveEntityList = productRepo.findByMerchantId(cart.getMerchantId());
            List<Product> liveProducts = liveEntityList.stream().map(p -> new Product(
                    p.getId().toString(), p.getMerchantId().toString(), p.getName(), p.getDescription(),
                    p.getPricePaise(), p.getStockQuantity(), p.getDietaryTags(), p.getAllergens(), p.getDeliveryDays(),
                    p.getReturnable(), p.getSubscriptionAvailable(), p.getSubscriptionOnly(), p.getVersion(), p.getUpdatedAt() != null ? p.getUpdatedAt().toString() : null
            )).toList();

            List<CartItem> cartItems = objectMapper.readValue(cart.getSnapshotDataJson(), new TypeReference<List<CartItem>>() {
            });

            PolicyEngine.PolicyEngineInput input = new PolicyEngine.PolicyEngineInput(
                    rules, cartItems, cart.getTotalPaise(), cart.getMerchantId().toString(), expiresAt, liveProducts, cart.getOfferHash()
            );

            PolicyResult result = policyEngine.runPolicyEngine(input);

            if (!"ALLOWED".equals(result.decision().name())) {
                cart.setPolicyDecision(result.decision().name());
                cart.setApproved(false);
                cart.setPolicyChecksJson(objectMapper.writeValueAsString(result.checks()));
                cartRepo.save(cart);
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Cart is no longer safe to checkout",
                        "policyResult", result
                ));
            }

            // All policy checks passed. If an AI agent (not the buyer's own session) is driving
            // this checkout, its spending mandate must also clear before any order is created.
            UUID agentTokenId = getAgentTokenId();
            if (agentTokenId != null) {
                AgentTokenEntity token = agentTokens.findById(agentTokenId).orElse(null);
                if (token != null) {
                    try {
                        mandateService.enforce(token, cart.getMerchantId(), cart.getTotalPaise());
                    } catch (AgentMandateService.MandateViolationException e) {
                        audit.record(cart.getBuyerId(), cart.getMerchantId(), cart.getId(), null, "MANDATE_BLOCKED", e.getMessage());
                        return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
                    }
                }
            }

            // All checks passed. Create Razorpay order.
            if (razorpayClient == null) {
                return ResponseEntity.status(503).body(Map.of("error", "Payment gateway not configured. Set RAZORPAY_KEY_ID in environment."));
            }
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", cart.getTotalPaise());
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", cart.getId().toString());
            Order rzpOrder = razorpayClient.orders.create(orderRequest);
            String rzpOrderId = rzpOrder.get("id");

            CheckoutOrderEntity order = new CheckoutOrderEntity();
            order.setBuyerId(cart.getBuyerId());
            order.setCartId(cart.getId());
            order.setMerchantId(cart.getMerchantId());
            order.setRazorpayOrderId(rzpOrderId);
            order.setAmountPaise(cart.getTotalPaise());
            order.setStatus("CREATED");
            order.setAgentTokenId(agentTokenId);
            orderRepo.save(order);
            audit.record(order.getBuyerId(), order.getMerchantId(), order.getCartId(), order.getId(), "CHECKOUT_CREATED", "Razorpay checkout order created.");

            try {
                inventory.reserve(order.getId(), cartItems);
            } catch (InventoryReservationService.InventoryUnavailableException |
                     InventoryReservationService.InventoryBusyException e) {
                order.setStatus("STOCK_UNAVAILABLE");
                orderRepo.save(order);
                return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
            }

            return ResponseEntity.ok(Map.of(
                    "orderId", order.getId(),
                    "razorpayOrderId", rzpOrderId,
                    "amountPaise", cart.getTotalPaise()
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    private String getAuthenticatedBuyerId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof String principal) {
            if ("anonymousUser".equals(principal)) return null;
            return principal;
        }
        return null;
    }

    private UUID getAgentTokenId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getCredentials() instanceof UUID id) return id;
        return null;
    }
}
