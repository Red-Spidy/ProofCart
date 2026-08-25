package com.proofcart.domain;

public record CartItem(
        String productId,
        Integer quantity,
        Integer unitPricePaise,
        Integer lineTotalPaise,
        ProductSnapshot snapshot
) {
}
