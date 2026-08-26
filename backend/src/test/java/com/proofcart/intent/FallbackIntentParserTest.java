package com.proofcart.intent;

import com.proofcart.domain.IntentRules;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FallbackIntentParserTest {

    private final FallbackIntentParser parser = new FallbackIntentParser();

    @Test
    void shouldExtractBudget() {
        IntentRules rules = parser.parse("Buy vegan snacks under Rs. 900");
        assertEquals(90000, rules.maxTotalPaise());
    }

    @Test
    void shouldExtractDietaryTags() {
        IntentRules rules = parser.parse("I want vegan and gluten-free snacks");
        assertTrue(rules.mustHaveTags().contains("vegan"));
        assertTrue(rules.mustHaveTags().contains("gluten-free"));
    }

    @Test
    void shouldExtractAllergens() {
        IntentRules rules = parser.parse("no peanuts please");
        assertTrue(rules.excludedAllergens().contains("peanuts"));
    }

    @Test
    void shouldExtractDelivery() {
        IntentRules rules = parser.parse("deliver today please");
        assertEquals("today", rules.deliveryRequirement());
    }

    @Test
    void shouldExtractSubscription() {
        IntentRules rules = parser.parse("one-time purchase only, no subscription");
        assertFalse(rules.subscriptionAllowed());
    }
}
