package com.proofcart.market;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proofcart.audit.AuditEventService;
import com.proofcart.domain.CartItem;
import com.proofcart.domain.IntentRules;
import com.proofcart.domain.PolicyResult;
import com.proofcart.domain.Product;
import com.proofcart.domain.entity.CheckoutOrderEntity;
import com.proofcart.domain.entity.IntentContractEntity;
import com.proofcart.domain.entity.MarketOrderEntity;
import com.proofcart.domain.entity.ProductEntity;
import com.proofcart.domain.entity.ProofCartEntity;
import com.proofcart.domain.repo.*;
import com.proofcart.inventory.InventoryReservationService;
import com.proofcart.policy.PolicyEngine;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestrates a single shopping request across MULTIPLE merchants — e.g. "birthday dinner:
 * cake from Bakery, flowers from FlowerShop, both under a combined ₹1500" — fulfilled as one
 * buyer payment instead of N separate checkouts.
 * <p>
 * No new cart abstraction is needed: {@link ProofCartEntity} already carries an
 * {@code intentContractId} foreign key, and nothing stops two carts for two different merchants
 * referencing the same intent contract. This controller only adds the aggregation and combined
 * settlement layer on top of carts created the normal way via {@code POST /api/proof-carts}.
 * <p>
 * Settlement note: this collects one Razorpay payment for the combined total and records a
 * per-merchant settlement ledger — the same pattern a marketplace without payout-splitting
 * infrastructure uses today. Wiring the ledger to Razorpay Route transfers (which requires Route
 * to be enabled on the account) is a drop-in change isolated to {@link #buildSettlement}.
 */
@RestController
@RequestMapping("/api/market-orders")
public class MarketOrderController {

    private final ProofCartRepository cartRepo;
    private final ProductRepository productRepo;
    private final IntentContractRepository intentRepo;
    private final PolicyEngine policyEngine;
    private final CheckoutOrderRepository orderRepo;
    private final MarketOrderRepository marketOrderRepo;
    private final ObjectMapper objectMapper;
    private final RazorpayClient razorpayClient;
    private final InventoryReservationService inventory;
    private final AuditEventService audit;

    public MarketOrderController(
            ProofCartRepository cartRepo,
            ProductRepository productRepo,
            IntentContractRepository intentRepo,
            PolicyEngine policyEngine,
            CheckoutOrderRepository orderRepo,
            MarketOrderRepository marketOrderRepo,
            ObjectMapper objectMapper,
            InventoryReservationService inventory,
            @Value("${razorpay.key.id:}") String keyId,
            @Value("${razorpay.key.secret:}") String keySecret,
            AuditEventService audit) {
        this.cartRepo = cartRepo;
        this.productRepo = productRepo;
        this.intentRepo = intentRepo;
        this.policyEngine = policyEngine;
        this.orderRepo = orderRepo;
        this.marketOrderRepo = marketOrderRepo;
        this.objectMapper = objectMapper;
        this.inventory = inventory;
        this.audit = audit;

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

    // ─── GET /api/market-orders/by-intent/{intentContractId} ───────────────
    // Read-only aggregation of every cart the buyer has created for this shopping request,
    // across however many merchants they span.

    @GetMapping("/by-intent/{intentContractId}")
    public ResponseEntity<?> getByIntent(@PathVariable UUID intentContractId) {
        UUID buyerId = requireBuyerId();
        if (buyerId == null) return ResponseEntity.status(401).body(Map.of("error", "Authentication required."));

        List<ProofCartEntity> carts = cartRepo.findByIntentContractIdAndBuyerId(intentContractId, buyerId);
        int total = carts.stream().mapToInt(ProofCartEntity::getTotalPaise).sum();
        boolean allAllowed = !carts.isEmpty() && carts.stream().allMatch(c -> "ALLOWED".equals(c.getPolicyDecision()));
        boolean allApproved = !carts.isEmpty() && carts.stream().allMatch(c -> Boolean.TRUE.equals(c.getApproved()));
        long merchantCount = carts.stream().map(ProofCartEntity::getMerchantId).distinct().count();

        List<Map<String, Object>> summary = carts.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("cartId", c.getId());
            m.put("merchantId", c.getMerchantId());
            m.put("totalPaise", c.getTotalPaise());
            m.put("policyDecision", c.getPolicyDecision());
            m.put("approved", c.getApproved());
            return m;
        }).toList();

        return ResponseEntity.ok(Map.of(
                "carts", summary,
                "totalPaise", total,
                "merchantCount", merchantCount,
                "allAllowed", allAllowed,
                "allApproved", allApproved,
                "readyForCombinedCheckout", merchantCount >= 2 && allAllowed && allApproved
        ));
    }

    // ─── POST /api/market-orders/checkout ───────────────────────────────────
    // Re-verifies every sub-cart against live data exactly like the single-merchant flow (a
    // multi-merchant order is never less safe than a single-merchant one), plus a combined
    // buyer-level budget check across merchants, then collects ONE Razorpay payment and records
    // a per-merchant settlement ledger.

    @PostMapping("/checkout")
    public ResponseEntity<?> checkoutMarketOrder(@RequestBody Map<String, String> request) {
        try {
            UUID buyerId = requireBuyerId();
            if (buyerId == null) return ResponseEntity.status(401).body(Map.of("error", "Authentication required."));

            UUID intentContractId = UUID.fromString(request.get("intentContractId"));
            List<ProofCartEntity> carts = cartRepo.findByIntentContractIdAndBuyerId(intentContractId, buyerId);
            if (carts.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No carts found for this shopping request."));
            }
            long merchantCount = carts.stream().map(ProofCartEntity::getMerchantId).distinct().count();
            if (merchantCount < 2) {
                return ResponseEntity.badRequest().body(Map.of("error",
                        "A market order needs carts from at least two merchants — use /api/checkout/create for a single merchant."));
            }
            for (ProofCartEntity c : carts) {
                if (!Boolean.TRUE.equals(c.getApproved())) {
                    return ResponseEntity.badRequest().body(Map.of("error",
                            "Cart for merchant " + c.getMerchantId() + " has not been approved by the buyer yet."));
                }
            }

            IntentContractEntity intent = intentRepo.findById(intentContractId).orElseThrow();
            IntentRules rules = objectMapper.readValue(intent.getExtractedRulesJson(), IntentRules.class);
            String expiresAt = intent.getExpiresAt().toString();

            List<List<CartItem>> itemsPerCart = new ArrayList<>();
            int grandTotal = 0;

            for (ProofCartEntity cart : carts) {
                List<ProductEntity> liveEntityList = productRepo.findByMerchantId(cart.getMerchantId());
                List<Product> liveProducts = liveEntityList.stream().map(p -> new Product(
                        p.getId().toString(), p.getMerchantId().toString(), p.getName(), p.getDescription(),
                        p.getPricePaise(), p.getStockQuantity(), p.getDietaryTags(), p.getAllergens(), p.getDeliveryDays(),
                        p.getReturnable(), p.getSubscriptionAvailable(), p.getVersion(),
                        p.getUpdatedAt() != null ? p.getUpdatedAt().toString() : null
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
                            "error", "Cart for merchant " + cart.getMerchantId() + " is no longer safe to check out",
                            "cartId", cart.getId(), "policyResult", result));
                }
                itemsPerCart.add(cartItems);
                grandTotal += cart.getTotalPaise();
            }

            // Even if every sub-cart individually cleared its own merchant-scoped policy run,
            // the buyer's budget is for the whole request — it must hold across merchants too.
            if (rules.maxTotalPaise() != null && grandTotal > rules.maxTotalPaise()) {
                return ResponseEntity.badRequest().body(Map.of("error",
                        "Combined total ₹" + (grandTotal / 100.0) + " across " + merchantCount
                                + " merchants exceeds the ₹" + (rules.maxTotalPaise() / 100.0) + " budget."));
            }

            if (razorpayClient == null) {
                return ResponseEntity.status(503).body(Map.of("error", "Payment gateway not configured. Set RAZORPAY_KEY_ID in environment."));
            }

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", grandTotal);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "market-" + intentContractId);
            Order rzpOrder = razorpayClient.orders.create(orderRequest);
            String rzpOrderId = rzpOrder.get("id");

            List<CheckoutOrderEntity> subOrders = new ArrayList<>();
            for (int i = 0; i < carts.size(); i++) {
                ProofCartEntity cart = carts.get(i);
                CheckoutOrderEntity order = new CheckoutOrderEntity();
                order.setBuyerId(buyerId);
                order.setCartId(cart.getId());
                order.setMerchantId(cart.getMerchantId());
                order.setRazorpayOrderId(rzpOrderId);
                order.setAmountPaise(cart.getTotalPaise());
                order.setStatus("CREATED");
                orderRepo.save(order);
                subOrders.add(order);
                audit.record(buyerId, cart.getMerchantId(), cart.getId(), order.getId(), "CHECKOUT_CREATED",
                        "Market order: one Razorpay payment shared across " + merchantCount + " merchants.");

                try {
                    inventory.reserve(order.getId(), itemsPerCart.get(i));
                } catch (InventoryReservationService.InventoryUnavailableException |
                         InventoryReservationService.InventoryBusyException e) {
                    order.setStatus("STOCK_UNAVAILABLE");
                    orderRepo.save(order);
                    // Roll back reservations already made for earlier merchants in this same
                    // market order — a partial multi-merchant reservation is not a valid state.
                    for (int j = 0; j < i; j++) {
                        inventory.release(subOrders.get(j).getId(), InventoryReservationService.RELEASED);
                        subOrders.get(j).setStatus("STOCK_UNAVAILABLE");
                        orderRepo.save(subOrders.get(j));
                    }
                    return ResponseEntity.status(409).body(Map.of("error",
                            "Stock unavailable for merchant " + cart.getMerchantId() + ": " + e.getMessage()));
                }
            }

            List<Map<String, Object>> settlement = buildSettlement(carts, subOrders);

            MarketOrderEntity marketOrder = new MarketOrderEntity();
            marketOrder.setBuyerId(buyerId);
            marketOrder.setIntentContractId(intentContractId);
            marketOrder.setRazorpayOrderId(rzpOrderId);
            marketOrder.setTotalPaise(grandTotal);
            marketOrder.setStatus("CREATED");
            marketOrder.setSettlementJson(objectMapper.writeValueAsString(settlement));
            marketOrderRepo.save(marketOrder);

            return ResponseEntity.ok(Map.of(
                    "marketOrderId", marketOrder.getId(),
                    "razorpayOrderId", rzpOrderId,
                    "amountPaise", grandTotal,
                    "merchantCount", merchantCount,
                    "settlement", settlement
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Per-merchant breakdown of a single collected payment — the settlement ledger a platform
     * without Razorpay Route access uses to know how much each seller is owed. Isolated here so
     * swapping in real Route transfers later touches only this method.
     */
    private List<Map<String, Object>> buildSettlement(List<ProofCartEntity> carts, List<CheckoutOrderEntity> subOrders) {
        List<Map<String, Object>> settlement = new ArrayList<>();
        for (int i = 0; i < carts.size(); i++) {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("merchantId", carts.get(i).getMerchantId());
            line.put("cartId", carts.get(i).getId());
            line.put("orderId", subOrders.get(i).getId());
            line.put("amountPaise", carts.get(i).getTotalPaise());
            settlement.add(line);
        }
        return settlement;
    }

    private UUID requireBuyerId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof String principal
                && !"anonymousUser".equals(principal)) {
            return UUID.fromString(principal);
        }
        return null;
    }
}
