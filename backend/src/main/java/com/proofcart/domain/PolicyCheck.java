package com.proofcart.domain;

public record PolicyCheck(
        String rule,
        Boolean passed,
        String message
) {
}
