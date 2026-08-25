package com.proofcart.domain;

import java.util.List;

public record IntentRules(
        Integer maxTotalPaise,
        List<String> mustHaveTags,
        List<String> excludedAllergens,
        Object deliveryRequirement, // "today", "tomorrow", or Integer days
        Boolean subscriptionAllowed,
        Boolean mustBeReturnable,
        Boolean needsClarification,
        String clarificationQuestion,
        Double confidence
) {
}
