package com.proofcart.ops;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ops")
public class OpsHealthController {

    private final SystemHealthService healthService;

    public OpsHealthController(SystemHealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping("/health")
    public SystemHealthReport health() {
        return healthService.check();
    }
}
