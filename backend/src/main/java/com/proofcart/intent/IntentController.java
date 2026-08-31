package com.proofcart.intent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proofcart.audit.AuditEventService;
import com.proofcart.domain.entity.IntentContractEntity;
import com.proofcart.domain.repo.IntentContractRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/intents")
public class IntentController {

    private final GroqIntentExtractor intentExtractor;
    private final IntentContractRepository intentContractRepository;
    private final ObjectMapper objectMapper;
    private final AuditEventService audit;

    public IntentController(GroqIntentExtractor intentExtractor, IntentContractRepository intentContractRepository, ObjectMapper objectMapper, AuditEventService audit) {
        this.intentExtractor = intentExtractor;
        this.intentContractRepository = intentContractRepository;
        this.objectMapper = objectMapper;
        this.audit = audit;
    }

    @PostMapping("/parse")
    public ResponseEntity<Map<String, Object>> parseIntent(@RequestBody Map<String, String> request, Authentication authentication) {
        String prompt = request.get("prompt");
        if (prompt == null || prompt.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "prompt is required"));
        }

        GroqIntentExtractor.IntentExtractionResult result = intentExtractor.extractIntent(prompt);

        // Save the intent contract to the database so it can be linked to the cart later
        try {
            IntentContractEntity entity = new IntentContractEntity();
            if (authentication == null)
                return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
            UUID buyerId = UUID.fromString(authentication.getName());
            entity.setBuyerId(buyerId);
            entity.setRawPrompt(prompt);
            entity.setExtractedRulesJson(objectMapper.writeValueAsString(result.rules()));
            entity.setConfidence(result.rules().confidence());
            entity.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
            entity.setCreatedAt(Instant.now());

            IntentContractEntity saved = intentContractRepository.save(entity);
            audit.record(buyerId, null, null, null, "INTENT_CREATED", "Buyer intent contract created.");

            return ResponseEntity.ok(Map.of(
                    "intentId", saved.getId(),
                    "rules", result.rules(),
                    "source", result.source()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
