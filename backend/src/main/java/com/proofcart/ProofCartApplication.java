package com.proofcart;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@OpenAPIDefinition(info = @Info(
        title = "ProofCart API",
        description = "Safe AI shopping: an AI agent must prove a product matches the buyer's " +
                "rules before it's allowed to pay. See /api/ops/health for the AI ops watchdog.",
        version = "v1"
))
public class ProofCartApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProofCartApplication.class, args);
    }

}
