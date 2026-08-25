package com.proofcart.domain;

import java.util.List;

public record ProductSnapshot(
        String id,
        String merchantId,
        String name,
        String description,
        Integer pricePaise,
        Integer stockQuantity,
        List<String> dietaryTags,
        List<String> allergens,
        Integer deliveryDays,
        Boolean returnable,
        Boolean subscriptionAvailable,
        Integer version,
        String updatedAt,
        String snapshotAt
) {
}
