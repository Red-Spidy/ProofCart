package com.proofcart.ops;

import java.time.Instant;
import java.util.List;

public record SystemHealthReport(String status, Instant timestamp, List<HealthCheckResult> checks, String diagnosis) {
}
