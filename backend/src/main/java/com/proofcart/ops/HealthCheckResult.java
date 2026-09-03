package com.proofcart.ops;

public record HealthCheckResult(String name, String status, long latencyMs, String detail) {

    public static HealthCheckResult up(String name, long latencyMs) {
        return new HealthCheckResult(name, "UP", latencyMs, null);
    }

    public static HealthCheckResult down(String name, long latencyMs, String detail) {
        return new HealthCheckResult(name, "DOWN", latencyMs, detail);
    }

    public static HealthCheckResult degraded(String name, long latencyMs, String detail) {
        return new HealthCheckResult(name, "DEGRADED", latencyMs, detail);
    }
}
