package com.proofcart.cart;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    public ProofCartController(ProofCartRepository proofCartRepository, ProductRepository productRepository, IntentContractRepository intentContractRepository, PolicyEngine policyEngine, ObjectMapper objectMapper) {
        this.proofCartRepository = proofCartRepository;
        this.productRepository = productRepository;
        this.intentContractRepository = intentContractRepository;
        this.policyEngine = policyEngine;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<?> createProofCart(@RequestBody Map<String, Object> request) {
        // Mock buyer for Phase 2
        UUID buyerId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID merchantId = UUID.fromString(request.get("merchantId").toString());
        Object rawIntentId = request.get("intentContractId");
        UUID intentId = (rawIntentId != null && !rawIntentId.toString().equals("null") && !rawIntentId.toString().isEmpty())
                ? UUID.fromString(rawIntentId.toString()) : null;
        List<Map<String, Object>> itemsRaw = (List<Map<String, Object>>) request.get("items");

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
                int qty = (int) rawItem.get("quantity");
                ProductEntity liveP = productMap.get(pId);
                if (liveP == null) {
                    liveP = productRepository.findById(pId).orElse(null);
                }
                if (liveP == null) continue;

                ProductSnapshot snap = new ProductSnapshot(
                        liveP.getId().toString(), liveP.getMerchantId().toString(), liveP.getName(), liveP.getDescription(),
                        liveP.getPricePaise(), liveP.getStockQuantity(), liveP.getDietaryTags(), liveP.getAllergens(),
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

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approveCart(@PathVariable UUID id) {
        ProofCartEntity cart = proofCartRepository.findById(id).orElseThrow();
        if (!"ALLOWED".equals(cart.getPolicyDecision())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cart is not ALLOWED"));
        }
        cart.setApproved(true);
        proofCartRepository.save(cart);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
