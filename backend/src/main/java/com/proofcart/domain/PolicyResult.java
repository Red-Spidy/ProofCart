package com.proofcart.domain;

import java.util.List;

public record PolicyResult(
        PolicyDecision decision,
        List<PolicyCheck> checks,
        String explanation,
        String summary
) {
}
