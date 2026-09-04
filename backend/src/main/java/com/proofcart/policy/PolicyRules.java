package com.proofcart.policy;

import com.proofcart.domain.CartItem;
import com.proofcart.domain.PolicyCheck;
import com.proofcart.domain.Product;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PolicyRules {

    public static PolicyCheck checkBudget(Integer totalPaise, Integer maxTotalPaise) {
        if (maxTotalPaise == null) {
            return new PolicyCheck("budget", true, "No budget limit set.");
        }
        boolean passed = totalPaise <= maxTotalPaise;
        String totalRs = String.format("%.2f", totalPaise / 100.0);
        String maxRs = String.format("%.2f", maxTotalPaise / 100.0);
        return new PolicyCheck(
                "budget",
                passed,
                passed ? "Total ₹" + totalRs + " is within the ₹" + maxRs + " budget."
                        : "Total ₹" + totalRs + " exceeds the ₹" + maxRs + " budget."
        );
    }

    public static PolicyCheck checkStock(List<CartItem> items) {
        List<String> outOfStock = items.stream()
                .filter(i -> i.snapshot().stockQuantity() < i.quantity())
                .map(i -> i.snapshot().name())
                .toList();
        boolean passed = outOfStock.isEmpty();
        return new PolicyCheck(
                "stock",
                passed,
                passed ? "All items are in stock."
                        : "Out of stock: " + String.join(", ", outOfStock) + "."
        );
    }

    public static PolicyCheck checkDietaryTags(List<CartItem> items, List<String> mustHaveTags) {
        if (mustHaveTags == null || mustHaveTags.isEmpty()) {
            return new PolicyCheck("dietary_tags", true, "No dietary tag requirements.");
        }
        List<String> missing = new ArrayList<>();
        for (CartItem item : items) {
            List<String> tags = item.snapshot().dietaryTags();
            for (String tag : mustHaveTags) {
                if (tags == null || !tags.contains(tag)) {
                    missing.add("\"" + item.snapshot().name() + "\" is missing tag \"" + tag + "\"");
                }
            }
        }
        boolean passed = missing.isEmpty();
        return new PolicyCheck(
                "dietary_tags",
                passed,
                passed ? "All products have required tags: " + String.join(", ", mustHaveTags) + "."
                        : "Tag violations: " + String.join("; ", missing) + "."
        );
    }

    public static PolicyCheck checkAllergens(List<CartItem> items, List<String> excludedAllergens) {
        if (excludedAllergens == null || excludedAllergens.isEmpty()) {
            return new PolicyCheck("allergens", true, "No allergen restrictions.");
        }
        List<String> conflicts = new ArrayList<>();
        for (CartItem item : items) {
            List<String> allergens = item.snapshot().allergens();
            if (allergens == null) continue;
            for (String allergen : excludedAllergens) {
                // Case-insensitive match so "Peanuts" matches "peanuts"
                boolean found = allergens.stream().anyMatch(a -> a.equalsIgnoreCase(allergen));
                if (found) {
                    conflicts.add('"' + item.snapshot().name() + "\" contains " + allergen + " (excluded by buyer)");
                }
            }
        }
        boolean passed = conflicts.isEmpty();
        return new PolicyCheck(
                "allergens",
                passed,
                passed ? "No excluded allergens found (checked: " + String.join(", ", excludedAllergens) + ")."
                        : "Allergen conflicts: " + String.join("; ", conflicts) + "."
        );
    }

    public static PolicyCheck checkDelivery(List<CartItem> items, Object deliveryRequirement) {
        if (deliveryRequirement == null) {
            return new PolicyCheck("delivery", true, "No delivery deadline requirement.");
        }
        int computedDays;
        if ("today".equals(deliveryRequirement)) {
            computedDays = 0;
        } else if ("tomorrow".equals(deliveryRequirement)) {
            computedDays = 1;
        } else if (deliveryRequirement instanceof Number n) {
            computedDays = n.intValue();
        } else {
            try {
                computedDays = Integer.parseInt(deliveryRequirement.toString());
            } catch (Exception e) {
                computedDays = 0;
            }
        }

        final int maxAllowedDays = computedDays;

        List<String> slow = items.stream()
                .filter(i -> i.snapshot().deliveryDays() != null && i.snapshot().deliveryDays() > maxAllowedDays)
                .map(i -> "\"" + i.snapshot().name() + "\" (" + i.snapshot().deliveryDays() + " day(s))")
                .toList();
        boolean passed = slow.isEmpty();
        return new PolicyCheck(
                "delivery",
                passed,
                passed ? "All items can be delivered within " + maxAllowedDays + " day(s)."
                        : "Cannot meet delivery window (" + maxAllowedDays + " day(s)): " + String.join(", ", slow) + "."
        );
    }

    public static PolicyCheck checkReturnability(List<CartItem> items, Boolean mustBeReturnable) {
        if (mustBeReturnable == null || !mustBeReturnable) {
            return new PolicyCheck("returnability", true, "No return policy requirement.");
        }
        List<String> notReturnable = items.stream()
                .filter(i -> Boolean.FALSE.equals(i.snapshot().returnable()))
                .map(i -> "\"" + i.snapshot().name() + "\"")
                .toList();
        boolean passed = notReturnable.isEmpty();
        return new PolicyCheck(
                "returnability",
                passed,
                passed ? "All items are returnable."
                        : "Not returnable: " + String.join(", ", notReturnable) + "."
        );
    }

    public static PolicyCheck checkSubscription(List<CartItem> items, Boolean subscriptionAllowed) {
        // Only block products if buyer explicitly opted out of subscriptions AND the product
        // is SUBSCRIPTION ONLY (i.e., no one-time purchase option). Products that merely
        // *offer* a subscription but can also be bought one-time are fine.
        if (Boolean.TRUE.equals(subscriptionAllowed)) {
            return new PolicyCheck("subscription", true, "Subscription products are allowed.");
        }
        List<String> subscriptionOnlyProducts = items.stream()
                .filter(i -> Boolean.TRUE.equals(i.snapshot().subscriptionOnly()))
                .map(i -> '"' + i.snapshot().name() + '"')
                .toList();
        boolean passed = subscriptionOnlyProducts.isEmpty();
        return new PolicyCheck(
                "subscription",
                passed,
                passed ? "No subscription-only products in cart."
                        : "Subscription-only, no one-time option: " + String.join(", ", subscriptionOnlyProducts) + "."
        );
    }

    public static PolicyCheck checkProductVersionDrift(List<CartItem> items, List<Product> liveProducts) {
        Map<String, Product> liveMap = liveProducts.stream()
                .collect(Collectors.toMap(Product::id, Function.identity()));
        List<String> drifted = new ArrayList<>();

        for (CartItem item : items) {
            Product live = liveMap.get(item.productId());
            if (live == null) {
                drifted.add("\"" + item.snapshot().name() + "\" no longer exists");
            } else if (!live.version().equals(item.snapshot().version())) {
                drifted.add("\"" + live.name() + "\" was updated (version " + item.snapshot().version() + " → " + live.version() + ")");
            }
        }
        boolean passed = drifted.isEmpty();
        return new PolicyCheck(
                "product_version",
                passed,
                passed ? "All product data is current."
                        : "Product data changed after cart was created: " + String.join("; ", drifted) + ". Please review the updated cart."
        );
    }

    public static PolicyCheck checkContractExpiry(String expiresAt) {
        if (expiresAt == null) return new PolicyCheck("contract_expiry", true, "No expiry.");
        boolean expired = Instant.parse(expiresAt).isBefore(Instant.now());
        return new PolicyCheck(
                "contract_expiry",
                !expired,
                expired ? "Intent contract expired at " + expiresAt + ". Please create a new shopping request."
                        : "Intent contract is still valid."
        );
    }

    public static PolicyCheck checkMerchantOwnership(List<CartItem> items, String expectedMerchantId) {
        List<String> wrong = items.stream()
                .filter(i -> !expectedMerchantId.equals(i.snapshot().merchantId()))
                .map(i -> "\"" + i.snapshot().name() + "\"")
                .toList();
        boolean passed = wrong.isEmpty();
        return new PolicyCheck(
                "merchant_ownership",
                passed,
                passed ? "All products belong to the selected merchant."
                        : "Products from wrong merchant: " + String.join(", ", wrong) + "."
        );
    }
}
