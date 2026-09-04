package com.proofcart.policy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proofcart.domain.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Batch evaluation of the policy engine across the full rule surface.
 *
 * A demo proves one path works; this measures the whole decision surface at once and prints a
 * scoreboard, so the claim "the engine gates every money action correctly" has a number behind
 * it instead of a screenshot. Every scenario declares the decision it SHOULD produce; any
 * disagreement is printed as an honest exception list and fails the build rather than being
 * quietly tuned to match whatever the engine currently does.
 *
 * Run `mvn -Dtest=PolicyEngineEvaluationTest test` to regenerate the numbers in
 * docs/POLICY_EVALUATION.md.
 */
class PolicyEngineEvaluationTest {

    private final PolicyEngine engine = new PolicyEngine(new ObjectMapper());

    private static final String MERCHANT = "merch-1";
    private static final String FUTURE = "2099-01-01T00:00:00Z";
    private static final String PAST = "2020-01-01T00:00:00Z";

    record Scenario(String id, String rule, String description,
                    PolicyEngine.PolicyEngineInput input, PolicyDecision expected) {
    }

    record Outcome(Scenario scenario, PolicyDecision actual) {
        boolean correct() {
            return scenario.expected() == actual;
        }
    }

    @Test
    void evaluateFullRuleSurface() {
        List<Scenario> scenarios = buildScenarios();
        List<Outcome> outcomes = scenarios.stream()
                .map(s -> new Outcome(s, engine.runPolicyEngine(s.input()).decision()))
                .toList();

        List<Outcome> mismatches = outcomes.stream().filter(o -> !o.correct()).toList();
        printReport(outcomes, mismatches);

        assertTrue(mismatches.isEmpty(),
                mismatches.size() + " scenario(s) did not produce the expected decision — see the report above.");
    }

    // ── Scenario matrix ──────────────────────────────────────────────────────

    private List<Scenario> buildScenarios() {
        List<Scenario> s = new ArrayList<>();

        // Budget
        s.add(sc("BUD-1", "budget", "Total below budget",
                rules(90000, null, null, null, true, null), cart(item(clean(), 1, 84000)), 84000, PolicyDecision.ALLOWED));
        s.add(sc("BUD-2", "budget", "Total exactly equal to budget (boundary)",
                rules(84000, null, null, null, true, null), cart(item(clean(), 1, 84000)), 84000, PolicyDecision.ALLOWED));
        s.add(sc("BUD-3", "budget", "Total one paisa over budget (boundary)",
                rules(83999, null, null, null, true, null), cart(item(clean(), 1, 84000)), 84000, PolicyDecision.BLOCKED));
        s.add(sc("BUD-4", "budget", "No budget stated",
                rules(null, null, null, null, true, null), cart(item(clean(), 1, 999999)), 999999, PolicyDecision.ALLOWED));

        // Allergens
        s.add(sc("ALG-1", "allergens", "Excluded allergen absent from product",
                rules(null, null, List.of("peanuts"), null, true, null), cart(item(clean(), 1, 10000)), 10000, PolicyDecision.ALLOWED));
        s.add(sc("ALG-2", "allergens", "Excluded allergen present in product",
                rules(null, null, List.of("peanuts"), null, true, null),
                cart(item(withAllergens(List.of("peanuts")), 1, 10000)), 10000, PolicyDecision.BLOCKED));
        s.add(sc("ALG-3", "allergens", "Allergen match is case-insensitive",
                rules(null, null, List.of("PEANUTS"), null, true, null),
                cart(item(withAllergens(List.of("peanuts")), 1, 10000)), 10000, PolicyDecision.BLOCKED));
        s.add(sc("ALG-4", "allergens", "No allergen restriction, allergenic product",
                rules(null, null, null, null, true, null),
                cart(item(withAllergens(List.of("peanuts")), 1, 10000)), 10000, PolicyDecision.ALLOWED));
        s.add(sc("ALG-5", "allergens", "One of several excluded allergens present",
                rules(null, null, List.of("soy", "peanuts", "milk"), null, true, null),
                cart(item(withAllergens(List.of("milk")), 1, 10000)), 10000, PolicyDecision.BLOCKED));

        // Dietary tags
        s.add(sc("TAG-1", "dietary_tags", "Required tag present",
                rules(null, List.of("vegan"), null, null, true, null),
                cart(item(withTags(List.of("vegan", "gluten-free")), 1, 10000)), 10000, PolicyDecision.ALLOWED));
        s.add(sc("TAG-2", "dietary_tags", "Required tag missing",
                rules(null, List.of("vegan"), null, null, true, null),
                cart(item(withTags(List.of("keto")), 1, 10000)), 10000, PolicyDecision.BLOCKED));
        s.add(sc("TAG-3", "dietary_tags", "All of several required tags present",
                rules(null, List.of("vegan", "gluten-free"), null, null, true, null),
                cart(item(withTags(List.of("vegan", "gluten-free", "organic")), 1, 10000)), 10000, PolicyDecision.ALLOWED));
        s.add(sc("TAG-4", "dietary_tags", "One of several required tags missing",
                rules(null, List.of("vegan", "organic"), null, null, true, null),
                cart(item(withTags(List.of("vegan")), 1, 10000)), 10000, PolicyDecision.BLOCKED));

        // Delivery
        s.add(sc("DEL-1", "delivery", "'today' with same-day product",
                rules(null, null, null, "today", true, null), cart(item(withDelivery(0), 1, 10000)), 10000, PolicyDecision.ALLOWED));
        s.add(sc("DEL-2", "delivery", "'today' with next-day product",
                rules(null, null, null, "today", true, null), cart(item(withDelivery(1), 1, 10000)), 10000, PolicyDecision.BLOCKED));
        s.add(sc("DEL-3", "delivery", "'tomorrow' with next-day product (boundary)",
                rules(null, null, null, "tomorrow", true, null), cart(item(withDelivery(1), 1, 10000)), 10000, PolicyDecision.ALLOWED));
        s.add(sc("DEL-4", "delivery", "'tomorrow' with 2-day product",
                rules(null, null, null, "tomorrow", true, null), cart(item(withDelivery(2), 1, 10000)), 10000, PolicyDecision.BLOCKED));
        s.add(sc("DEL-5", "delivery", "Numeric window (3 days) satisfied",
                rules(null, null, null, 3, true, null), cart(item(withDelivery(2), 1, 10000)), 10000, PolicyDecision.ALLOWED));
        s.add(sc("DEL-6", "delivery", "No delivery requirement, slow product",
                rules(null, null, null, null, true, null), cart(item(withDelivery(9), 1, 10000)), 10000, PolicyDecision.ALLOWED));

        // Returnability
        s.add(sc("RET-1", "returnability", "Returnable required, product returnable",
                rules(null, null, null, null, true, true), cart(item(clean(), 1, 10000)), 10000, PolicyDecision.ALLOWED));
        s.add(sc("RET-2", "returnability", "Returnable required, product non-returnable",
                rules(null, null, null, null, true, true), cart(item(nonReturnable(), 1, 10000)), 10000, PolicyDecision.BLOCKED));
        s.add(sc("RET-3", "returnability", "No return requirement, product non-returnable",
                rules(null, null, null, null, true, false), cart(item(nonReturnable(), 1, 10000)), 10000, PolicyDecision.ALLOWED));

        // Subscription
        s.add(sc("SUB-1", "subscription", "One-time requested, subscription-only product",
                rules(null, null, null, null, false, null), cart(item(subscriptionOnly(), 1, 10000)), 10000, PolicyDecision.BLOCKED));
        s.add(sc("SUB-2", "subscription", "One-time requested, subscription offered but optional",
                rules(null, null, null, null, false, null), cart(item(subscriptionOptional(), 1, 10000)), 10000, PolicyDecision.ALLOWED));
        s.add(sc("SUB-3", "subscription", "Subscriptions allowed, subscription-only product",
                rules(null, null, null, null, true, null), cart(item(subscriptionOnly(), 1, 10000)), 10000, PolicyDecision.ALLOWED));

        // Stock
        s.add(sc("STK-1", "stock", "Stock exceeds quantity",
                rules(null, null, null, null, true, null), cart(item(withStock(10), 2, 10000)), 20000, PolicyDecision.ALLOWED));
        s.add(sc("STK-2", "stock", "Stock exactly equals quantity (boundary)",
                rules(null, null, null, null, true, null), cart(item(withStock(2), 2, 10000)), 20000, PolicyDecision.ALLOWED));
        s.add(sc("STK-3", "stock", "Stock below quantity",
                rules(null, null, null, null, true, null), cart(item(withStock(1), 2, 10000)), 20000, PolicyDecision.BLOCKED));
        s.add(sc("STK-4", "stock", "Zero stock",
                rules(null, null, null, null, true, null), cart(item(withStock(0), 1, 10000)), 10000, PolicyDecision.BLOCKED));

        // Intent contract expiry
        s.add(new Scenario("EXP-1", "contract_expiry", "Intent contract still valid",
                input(rules(null, null, null, null, true, null), cart(item(clean(), 1, 10000)), 10000, MERCHANT, FUTURE, null, null),
                PolicyDecision.ALLOWED));
        s.add(new Scenario("EXP-2", "contract_expiry", "Intent contract expired",
                input(rules(null, null, null, null, true, null), cart(item(clean(), 1, 10000)), 10000, MERCHANT, PAST, null, null),
                PolicyDecision.BLOCKED));

        // Merchant ownership
        s.add(new Scenario("OWN-1", "merchant_ownership", "Item belongs to the cart's merchant",
                input(rules(null, null, null, null, true, null), cart(item(clean(), 1, 10000)), 10000, MERCHANT, FUTURE, null, null),
                PolicyDecision.ALLOWED));
        s.add(new Scenario("OWN-2", "merchant_ownership", "Item belongs to a different merchant",
                input(rules(null, null, null, null, true, null), cart(item(clean(), 1, 10000)), 10000, "merch-OTHER", FUTURE, null, null),
                PolicyDecision.BLOCKED));

        // Product version drift → re-approval, not a hard block
        ProductSnapshot v1 = clean();
        s.add(new Scenario("DRF-1", "product_version", "Live product version matches snapshot",
                input(rules(null, null, null, null, true, null), cart(item(v1, 1, 10000)), 10000, MERCHANT, FUTURE,
                        List.of(liveOf(v1, 1)), null),
                PolicyDecision.ALLOWED));
        s.add(new Scenario("DRF-2", "product_version", "Live product version newer than snapshot",
                input(rules(null, null, null, null, true, null), cart(item(v1, 1, 10000)), 10000, MERCHANT, FUTURE,
                        List.of(liveOf(v1, 2)), null),
                PolicyDecision.REAPPROVAL_REQUIRED));
        s.add(new Scenario("DRF-3", "product_version", "Product no longer exists in live catalog",
                input(rules(null, null, null, null, true, null), cart(item(v1, 1, 10000)), 10000, MERCHANT, FUTURE,
                        List.of(), null),
                PolicyDecision.REAPPROVAL_REQUIRED));

        // Offer hash drift → re-approval
        List<CartItem> hashItems = cart(item(clean(), 1, 84000));
        s.add(new Scenario("HSH-1", "offer_hash", "Stored offer hash matches current cart",
                input(rules(null, null, null, null, true, null), hashItems, 84000, MERCHANT, FUTURE, null,
                        engine.computeOfferHash(hashItems, 84000)),
                PolicyDecision.ALLOWED));
        s.add(new Scenario("HSH-2", "offer_hash", "Stored offer hash differs from current cart",
                input(rules(null, null, null, null, true, null), hashItems, 84000, MERCHANT, FUTURE, null,
                        "0000000000000000000000000000000000000000000000000000000000000000"),
                PolicyDecision.REAPPROVAL_REQUIRED));

        // Precedence & multi-item
        s.add(new Scenario("MIX-1", "precedence", "Hard failure alongside version drift → block wins over re-approval",
                input(rules(50000, null, null, null, true, null), cart(item(v1, 1, 84000)), 84000, MERCHANT, FUTURE,
                        List.of(liveOf(v1, 2)), null),
                PolicyDecision.BLOCKED));
        s.add(sc("MIX-2", "precedence", "Two simultaneous hard failures (budget + allergen)",
                rules(50000, null, List.of("peanuts"), null, true, null),
                cart(item(withAllergens(List.of("peanuts")), 1, 84000)), 84000, PolicyDecision.BLOCKED));
        s.add(sc("MIX-3", "multi_item", "Multi-item cart, all items compliant",
                rules(200000, List.of("vegan"), null, null, true, null),
                cart(item(withTags(List.of("vegan")), 1, 50000), item2(withTags(List.of("vegan")), 1, 60000)),
                110000, PolicyDecision.ALLOWED));
        s.add(sc("MIX-4", "multi_item", "Multi-item cart, one item violates a rule",
                rules(200000, List.of("vegan"), null, null, true, null),
                cart(item(withTags(List.of("vegan")), 1, 50000), item2(withTags(List.of("keto")), 1, 60000)),
                110000, PolicyDecision.BLOCKED));
        s.add(sc("MIX-5", "baseline", "No constraints at all, ordinary product",
                rules(null, null, null, null, null, null), cart(item(clean(), 1, 10000)), 10000, PolicyDecision.ALLOWED));

        return s;
    }

    // ── Reporting ────────────────────────────────────────────────────────────

    private void printReport(List<Outcome> outcomes, List<Outcome> mismatches) {
        long correct = outcomes.stream().filter(Outcome::correct).count();
        long blocked = outcomes.stream().filter(o -> o.actual() == PolicyDecision.BLOCKED).count();
        long reapproval = outcomes.stream().filter(o -> o.actual() == PolicyDecision.REAPPROVAL_REQUIRED).count();
        long allowed = outcomes.stream().filter(o -> o.actual() == PolicyDecision.ALLOWED).count();

        StringBuilder out = new StringBuilder();
        out.append("\n=== POLICY ENGINE BATCH EVALUATION ===\n");
        out.append(String.format("Scenarios: %d | Correct: %d | Incorrect: %d | Accuracy: %.1f%%%n",
                outcomes.size(), correct, outcomes.size() - correct, (100.0 * correct) / outcomes.size()));
        out.append(String.format("Decisions produced — ALLOWED: %d | BLOCKED: %d | REAPPROVAL_REQUIRED: %d%n%n",
                allowed, blocked, reapproval));

        out.append("| ID | Rule | Scenario | Expected | Actual | OK |\n");
        out.append("|---|---|---|---|---|---|\n");
        for (Outcome o : outcomes) {
            out.append(String.format("| %s | %s | %s | %s | %s | %s |%n",
                    o.scenario().id(), o.scenario().rule(), o.scenario().description(),
                    o.scenario().expected(), o.actual(), o.correct() ? "yes" : "**NO**"));
        }

        out.append("\nException list: ");
        if (mismatches.isEmpty()) {
            out.append("none — every scenario produced its expected decision.\n");
        } else {
            out.append(mismatches.size()).append(" mismatch(es)\n");
            for (Outcome m : mismatches) {
                out.append(String.format("  - %s (%s): expected %s, got %s%n",
                        m.scenario().id(), m.scenario().description(), m.scenario().expected(), m.actual()));
            }
        }
        System.out.println(out);
    }

    // ── Builders ─────────────────────────────────────────────────────────────

    private Scenario sc(String id, String rule, String desc, IntentRules rules,
                        List<CartItem> items, int total, PolicyDecision expected) {
        return new Scenario(id, rule, desc, input(rules, items, total, MERCHANT, FUTURE, null, null), expected);
    }

    private PolicyEngine.PolicyEngineInput input(IntentRules rules, List<CartItem> items, int total,
                                                 String merchantId, String expiresAt,
                                                 List<Product> liveProducts, String offerHash) {
        return new PolicyEngine.PolicyEngineInput(rules, items, total, merchantId, expiresAt, liveProducts, offerHash);
    }

    private IntentRules rules(Integer maxPaise, List<String> tags, List<String> allergens,
                              Object delivery, Boolean subscriptionAllowed, Boolean mustBeReturnable) {
        return new IntentRules(maxPaise, tags == null ? List.of() : tags, allergens == null ? List.of() : allergens,
                delivery, subscriptionAllowed, mustBeReturnable, false, null, 0.9);
    }

    private List<CartItem> cart(CartItem... items) {
        return List.of(items);
    }

    private CartItem item(ProductSnapshot snap, int qty, int unitPaise) {
        return new CartItem(snap.id(), qty, unitPaise, unitPaise * qty, snap);
    }

    private CartItem item2(ProductSnapshot snap, int qty, int unitPaise) {
        ProductSnapshot second = new ProductSnapshot("prod-2", snap.merchantId(), "Second Item", snap.description(),
                snap.pricePaise(), snap.stockQuantity(), snap.dietaryTags(), snap.allergens(), snap.deliveryDays(),
                snap.returnable(), snap.subscriptionAvailable(), snap.subscriptionOnly(), snap.version(),
                snap.updatedAt(), snap.snapshotAt());
        return new CartItem("prod-2", qty, unitPaise, unitPaise * qty, second);
    }

    /** Baseline product: compliant with everything unless a variant below changes one attribute. */
    private ProductSnapshot clean() {
        return snapshot(List.of("vegan", "gluten-free"), List.of(), 0, true, false, false, 100, 1);
    }

    private ProductSnapshot withAllergens(List<String> allergens) {
        return snapshot(List.of("vegan"), allergens, 0, true, false, false, 100, 1);
    }

    private ProductSnapshot withTags(List<String> tags) {
        return snapshot(tags, List.of(), 0, true, false, false, 100, 1);
    }

    private ProductSnapshot withDelivery(int days) {
        return snapshot(List.of("vegan"), List.of(), days, true, false, false, 100, 1);
    }

    private ProductSnapshot withStock(int stock) {
        return snapshot(List.of("vegan"), List.of(), 0, true, false, false, stock, 1);
    }

    private ProductSnapshot nonReturnable() {
        return snapshot(List.of("vegan"), List.of(), 0, false, false, false, 100, 1);
    }

    private ProductSnapshot subscriptionOnly() {
        return snapshot(List.of("vegan"), List.of(), 0, true, true, true, 100, 1);
    }

    private ProductSnapshot subscriptionOptional() {
        return snapshot(List.of("vegan"), List.of(), 0, true, true, false, 100, 1);
    }

    private ProductSnapshot snapshot(List<String> tags, List<String> allergens, int deliveryDays,
                                     boolean returnable, boolean subscriptionAvailable, boolean subscriptionOnly,
                                     int stock, int version) {
        return new ProductSnapshot("prod-1", MERCHANT, "Test Product", "Desc", 10000, stock, tags, allergens,
                deliveryDays, returnable, subscriptionAvailable, subscriptionOnly, version,
                "2026-01-01T00:00:00Z", "2026-01-01T00:00:00Z");
    }

    private Product liveOf(ProductSnapshot snap, int liveVersion) {
        return new Product(snap.id(), snap.merchantId(), snap.name(), snap.description(), snap.pricePaise(),
                snap.stockQuantity(), snap.dietaryTags(), snap.allergens(), snap.deliveryDays(), snap.returnable(),
                snap.subscriptionAvailable(), snap.subscriptionOnly(), liveVersion, snap.updatedAt());
    }
}
