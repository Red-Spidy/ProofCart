package com.proofcart.checkout;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proofcart.domain.CartItem;
import com.proofcart.domain.IntentRules;
import com.proofcart.domain.PolicyResult;
import com.proofcart.domain.Product;
import com.proofcart.domain.entity.CheckoutOrderEntity;
import com.proofcart.domain.entity.IntentContractEntity;
import com.proofcart.domain.entity.ProductEntity;
import com.proofcart.domain.entity.ProofCartEntity;
import com.proofcart.domain.repo.CheckoutOrderRepository;
import com.proofcart.domain.repo.IntentContractRepository;
import com.proofcart.domain.repo.ProductRepository;
import com.proofcart.domain.repo.ProofCartRepository;
import com.proofcart.policy.PolicyEngine;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
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

    public CheckoutController(
            ProofCartRepository cartRepo,
            ProductRepository productRepo,
            IntentContractRepository intentRepo,
            PolicyEngine policyEngine,
            CheckoutOrderRepository orderRepo,
            ObjectMapper objectMapper,
            @Value("${razorpay.key.id:}") String keyId,
            @Value("${razorpay.key.secret:}") String keySecret) {
        this.cartRepo = cartRepo;
        this.productRepo = productRepo;
        this.intentRepo = intentRepo;
        this.policyEngine = policyEngine;
        this.orderRepo = orderRepo;
        this.objectMapper = objectMapper;

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

            if (!cart.getApproved()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Cart not approved by buyer"));
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
                    p.getReturnable(), p.getSubscriptionAvailable(), p.getVersion(), p.getUpdatedAt() != null ? p.getUpdatedAt().toString() : null
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

            // All checks passed again. Create Razorpay order.
            String rzpOrderId = "order_mock_" + UUID.randomUUID().toString().substring(0, 8);
            if (razorpayClient != null) {
                JSONObject orderRequest = new JSONObject();
                orderRequest.put("amount", cart.getTotalPaise());
                orderRequest.put("currency", "INR");
                orderRequest.put("receipt", cart.getId().toString());
                Order rzpOrder = razorpayClient.orders.create(orderRequest);
                rzpOrderId = rzpOrder.get("id");
            }

            CheckoutOrderEntity order = new CheckoutOrderEntity();
            order.setBuyerId(cart.getBuyerId());
            order.setCartId(cart.getId());
            order.setMerchantId(cart.getMerchantId());
            order.setRazorpayOrderId(rzpOrderId);
            order.setAmountPaise(cart.getTotalPaise());
            order.setStatus("CREATED");
            orderRepo.save(order);

            return ResponseEntity.ok(Map.of(
                    "orderId", order.getId(),
                    "razorpayOrderId", rzpOrderId,
                    "amountPaise", cart.getTotalPaise()
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
