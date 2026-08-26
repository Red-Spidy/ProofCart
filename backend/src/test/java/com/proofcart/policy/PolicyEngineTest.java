package com.proofcart.policy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proofcart.domain.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PolicyEngineTest {

    private final PolicyEngine engine = new PolicyEngine(new ObjectMapper());

    @Test
    void shouldAllowValidCart() {
        IntentRules rules = new IntentRules(90000, List.of("vegan"), List.of("peanuts"), "today", false, true, false, null, 0.9);
        ProductSnapshot snapshot = new ProductSnapshot("prod-1", "merch-1", "Vegan Snack", "Desc", 84000, 10, List.of("vegan"), List.of(), 0, true, false, 1, "2023-01-01", "2023-01-01");
        CartItem item = new CartItem("prod-1", 1, 84000, 84000, snapshot);

        PolicyEngine.PolicyEngineInput input = new PolicyEngine.PolicyEngineInput(rules, List.of(item), 84000, "merch-1", "2099-01-01T00:00:00Z", null, null);
        PolicyResult result = engine.runPolicyEngine(input);

        assertEquals(PolicyDecision.ALLOWED, result.decision());
        assertTrue(result.checks().stream().allMatch(PolicyCheck::passed));
    }

    @Test
    void shouldBlockWhenOverBudget() {
        IntentRules rules = new IntentRules(90000, List.of(), List.of(), null, true, false, false, null, 0.9);
        ProductSnapshot snapshot = new ProductSnapshot("prod-1", "merch-1", "Expensive Snack", "Desc", 100000, 10, List.of(), List.of(), 0, true, true, 1, "2023-01-01", "2023-01-01");
        CartItem item = new CartItem("prod-1", 1, 100000, 100000, snapshot);

        PolicyEngine.PolicyEngineInput input = new PolicyEngine.PolicyEngineInput(rules, List.of(item), 100000, "merch-1", "2099-01-01T00:00:00Z", null, null);
        PolicyResult result = engine.runPolicyEngine(input);

        assertEquals(PolicyDecision.BLOCKED, result.decision());
        assertFalse(result.checks().stream().filter(c -> "budget".equals(c.rule())).findFirst().get().passed());
    }
}
