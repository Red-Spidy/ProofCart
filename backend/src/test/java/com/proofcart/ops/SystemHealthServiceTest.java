package com.proofcart.ops;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SystemHealthServiceTest {

    @Test
    void classifiesUnauthorizedAsKeyRejectedNotGenericFailure() {
        String detail = SystemHealthService.classifyHttpFailure(401, "Razorpay");
        assertTrue(detail.contains("key rejected"), "401 should be reported as a bad key, not a generic error");
        assertTrue(detail.contains("401"));
    }

    @Test
    void classifiesForbiddenAsKeyRejectedToo() {
        assertTrue(SystemHealthService.classifyHttpFailure(403, "Groq").contains("key rejected"));
    }

    @Test
    void classifiesServerErrorAsGenericFailureNotKeyRejected() {
        String detail = SystemHealthService.classifyHttpFailure(503, "Razorpay");
        assertFalse(detail.contains("key rejected"), "a 503 is a service outage, not a bad key");
        assertTrue(detail.contains("503"));
    }

    @Test
    void fallbackDiagnosisMentionsEveryFailingCheckByName() {
        List<HealthCheckResult> down = List.of(
                HealthCheckResult.down("database", 10, "connection refused"),
                HealthCheckResult.down("razorpay", 5, "Razorpay key rejected (HTTP 401) — likely rotated or revoked")
        );

        String diagnosis = SystemHealthService.fallbackDiagnosis(down);

        assertTrue(diagnosis.contains("database"));
        assertTrue(diagnosis.contains("razorpay"));
        assertTrue(diagnosis.contains("connection refused"));
    }

    @Test
    void fallbackDiagnosisNeverThrowsOnEmptyDetail() {
        List<HealthCheckResult> down = List.of(new HealthCheckResult("groq", "DOWN", 0, null));
        assertDoesNotThrow(() -> SystemHealthService.fallbackDiagnosis(down));
    }
}
