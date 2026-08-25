package com.proofcart.intent;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/intents")
public class IntentController {

    private final GroqIntentExtractor intentExtractor;

    public IntentController(GroqIntentExtractor intentExtractor) {
        this.intentExtractor = intentExtractor;
    }

    @PostMapping("/parse")
    public ResponseEntity<Map<String, Object>> parseIntent(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        if (prompt == null || prompt.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "prompt is required"));
        }

        GroqIntentExtractor.IntentExtractionResult result = intentExtractor.extractIntent(prompt);
        return ResponseEntity.ok(Map.of(
                "rules", result.rules(),
                "source", result.source()
        ));
    }
}
