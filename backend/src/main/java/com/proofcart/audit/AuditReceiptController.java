package com.proofcart.audit;

import com.proofcart.domain.entity.CheckoutOrderEntity;
import com.proofcart.domain.entity.IntentContractEntity;
import com.proofcart.domain.entity.ProofCartEntity;
import com.proofcart.domain.repo.CheckoutOrderRepository;
import com.proofcart.domain.repo.IntentContractRepository;
import com.proofcart.domain.repo.ProofCartRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit-receipts")
public class AuditReceiptController {

    private final CheckoutOrderRepository orderRepo;
    private final ProofCartRepository cartRepo;
    private final IntentContractRepository intentRepo;

    public AuditReceiptController(CheckoutOrderRepository orderRepo, ProofCartRepository cartRepo, IntentContractRepository intentRepo) {
        this.orderRepo = orderRepo;
        this.cartRepo = cartRepo;
        this.intentRepo = intentRepo;
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getAuditReceipt(@PathVariable UUID orderId) {
        try {
            CheckoutOrderEntity order = orderRepo.findById(orderId).orElseThrow();
            ProofCartEntity cart = cartRepo.findById(order.getCartId()).orElseThrow();

            Object intentDetails = null;
            if (cart.getIntentContractId() != null) {
                IntentContractEntity intent = intentRepo.findById(cart.getIntentContractId()).orElseThrow();
                intentDetails = Map.of(
                        "prompt", intent.getRawPrompt(),
                        "extractedRules", intent.getExtractedRulesJson(),
                        "confidence", intent.getConfidence()
                );
            }

            return ResponseEntity.ok(Map.of(
                    "orderId", order.getId(),
                    "status", order.getStatus(),
                    "amountPaise", order.getAmountPaise(),
                    "razorpayOrderId", order.getRazorpayOrderId(),
                    "razorpayPaymentId", order.getRazorpayPaymentId() != null ? order.getRazorpayPaymentId() : "Pending",
                    "proofCart", Map.of(
                            "id", cart.getId(),
                            "offerHash", cart.getOfferHash(),
                            "policyDecision", cart.getPolicyDecision(),
                            "policyChecks", cart.getPolicyChecksJson(),
                            "approvedByBuyer", cart.getApproved()
                    ),
                    "intent", intentDetails != null ? intentDetails : "Direct cart purchase"
            ));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
