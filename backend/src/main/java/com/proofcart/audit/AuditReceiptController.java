package com.proofcart.audit;

import com.proofcart.domain.entity.CheckoutOrderEntity;
import com.proofcart.domain.entity.IntentContractEntity;
import com.proofcart.domain.entity.ProofCartEntity;
import com.proofcart.domain.repo.CheckoutOrderRepository;
import com.proofcart.domain.repo.IntentContractRepository;
import com.proofcart.domain.repo.ProofCartRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    private final AuditEventService audit;

    public AuditReceiptController(CheckoutOrderRepository orderRepo, ProofCartRepository cartRepo, IntentContractRepository intentRepo, AuditEventService audit) {
        this.orderRepo = orderRepo;
        this.cartRepo = cartRepo;
        this.intentRepo = intentRepo;
        this.audit = audit;
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getAuditReceipt(@PathVariable UUID orderId, Authentication authentication) {
        try {
            CheckoutOrderEntity order = orderRepo.findById(orderId).orElseThrow();
            if (authentication == null || !order.getBuyerId().toString().equals(authentication.getName())) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied."));
            }
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

            // The full chain for this cart (intent → cart evaluation → approval → checkout →
            // payment) — not just events that happen to carry this orderId, so nothing the buyer
            // did before checkout existed silently drops out of their receipt.
            AuditEventService.ChainVerification verification = audit.verifyChain(cart.getId());

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
                    "events", audit.forCart(cart.getId()),
                    "chain", chainResponse(verification),
                    "intent", intentDetails != null ? intentDetails : "Direct cart purchase"
            ));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ─── GET /api/audit-receipts/{orderId}/verify ───────────────────────────
    // Recomputes the HMAC chain fresh on every call — a live cryptographic re-check, not a
    // cached flag — so a buyer, merchant, or auditor can confirm the trail hasn't been altered.

    @GetMapping("/{orderId}/verify")
    public ResponseEntity<?> verifyReceipt(@PathVariable UUID orderId, Authentication authentication) {
        try {
            CheckoutOrderEntity order = orderRepo.findById(orderId).orElseThrow();
            if (authentication == null || !order.getBuyerId().toString().equals(authentication.getName())) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied."));
            }
            ProofCartEntity cart = cartRepo.findById(order.getCartId()).orElseThrow();
            return ResponseEntity.ok(chainResponse(audit.verifyChain(cart.getId())));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    private Map<String, Object> chainResponse(AuditEventService.ChainVerification v) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("valid", v.valid());
        m.put("message", v.message());
        m.put("brokenEventId", v.brokenEventId());
        return m;
    }
}
