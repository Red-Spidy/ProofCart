package com.proofcart.policy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proofcart.domain.*;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PolicyEngine {

    private final ObjectMapper objectMapper;

    public PolicyEngine(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String computeOfferHash(List<CartItem> items, Integer totalPaise) {
        try {
            var itemsList = items.stream().map(i -> Map.of(
                    "product_id", i.productId(),
                    "quantity", i.quantity(),
                    "unit_price_paise", i.unitPricePaise(),
                    "version", i.snapshot().version()
            )).toList();

            String canonical = objectMapper.writeValueAsString(Map.of(
                    "items", itemsList,
                    "totalPaise", totalPaise
            ));

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonical.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to compute offer hash", e);
        }
    }

    public PolicyResult runPolicyEngine(PolicyEngineInput input) {
        List<PolicyCheck> checks = new ArrayList<>();

        // -- Hard rules --
        checks.add(PolicyRules.checkContractExpiry(input.contractExpiresAt()));
        checks.add(PolicyRules.checkMerchantOwnership(input.items(), input.merchantId()));
        checks.add(PolicyRules.checkStock(input.items()));
        checks.add(PolicyRules.checkAllergens(input.items(), input.rules().excludedAllergens()));
        checks.add(PolicyRules.checkBudget(input.totalPaise(), input.rules().maxTotalPaise()));
        checks.add(PolicyRules.checkDietaryTags(input.items(), input.rules().mustHaveTags()));
        checks.add(PolicyRules.checkDelivery(input.items(), input.rules().deliveryRequirement()));
        checks.add(PolicyRules.checkReturnability(input.items(), input.rules().mustBeReturnable()));
        checks.add(PolicyRules.checkSubscription(input.items(), input.rules().subscriptionAllowed()));

        boolean hardFailed = checks.stream().anyMatch(c -> !c.passed());

        boolean versionDrifted = false;
        if (input.liveProducts() != null) {
            PolicyCheck versionCheck = PolicyRules.checkProductVersionDrift(input.items(), input.liveProducts());
            checks.add(versionCheck);
            if (!versionCheck.passed()) versionDrifted = true;
        }

        boolean hashDrifted = false;
        if (input.storedOfferHash() != null) {
            String currentHash = computeOfferHash(input.items(), input.totalPaise());
            hashDrifted = !currentHash.equals(input.storedOfferHash());
            checks.add(new PolicyCheck(
                    "offer_hash",
                    !hashDrifted,
                    hashDrifted ? "Cart content changed since it was first created. Please review." : "Cart content matches the original offer."
            ));
        }

        PolicyDecision decision;
        String summary;
        String explanation;

        if (hardFailed) {
            decision = PolicyDecision.BLOCKED;
            List<String> failedMsgs = checks.stream().filter(c -> !c.passed()).map(PolicyCheck::message).toList();
            summary = "Purchase blocked";
            explanation = "This purchase cannot proceed:\n• " + String.join("\n• ", failedMsgs);
        } else if (versionDrifted || hashDrifted) {
            decision = PolicyDecision.REAPPROVAL_REQUIRED;
            List<String> issues = checks.stream().filter(c -> !c.passed()).map(PolicyCheck::message).toList();
            summary = "Re-approval required";
            explanation = "Something changed since your last review. Please confirm before continuing:\n• " + String.join("\n• ", issues);
        } else {
            decision = PolicyDecision.ALLOWED;
            summary = "Ready for checkout";
            explanation = "All rules passed. The cart is safe to proceed to checkout.";
        }

        return new PolicyResult(decision, checks, explanation, summary);
    }

    public record PolicyEngineInput(
            IntentRules rules,
            List<CartItem> items,
            Integer totalPaise,
            String merchantId,
            String contractExpiresAt,
            List<Product> liveProducts,
            String storedOfferHash
    ) {
    }
}
