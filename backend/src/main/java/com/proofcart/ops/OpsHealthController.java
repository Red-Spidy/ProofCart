package com.proofcart.ops;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ops")
@Tag(name = "Ops", description = "AI ops watchdog — live dependency health with AI-generated incident diagnosis")
public class OpsHealthController {

    private final SystemHealthService healthService;

    public OpsHealthController(SystemHealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping("/health")
    @Operation(
            summary = "Check DB, Redis, Razorpay, and Groq — including whether the API keys still work",
            description = "Exercises each dependency with a real authenticated call, not just a liveness ping. " +
                    "A rotated/expired API key surfaces here as \"key rejected\", not a generic timeout. " +
                    "If anything is down, the response includes an AI-generated (Groq) plain-English " +
                    "incident summary, or a deterministic fallback if Groq itself is unavailable."
    )
    public SystemHealthReport health() {
        return healthService.check();
    }
}
