package com.proofcart.upsell;

import java.util.List;

public record UpsellSuggestion(
        String productId,
        String name,
        Integer pricePaise,
        String reason,
        List<String> dietaryTags,
        Boolean returnable
) {
}
