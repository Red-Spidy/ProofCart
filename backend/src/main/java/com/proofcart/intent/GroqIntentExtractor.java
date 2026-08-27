package com.proofcart.intent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proofcart.domain.IntentRules;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class GroqIntentExtractor {

    private static final String SYSTEM_PROMPT = """
            You are a rule-extraction assistant for a safe shopping application called ProofCart.
            
            Your ONLY job is to read a buyer's shopping request and output a JSON object matching this exact schema:
            
            {
              "maxTotalPaise": number | null,
              "mustHaveTags": string[],
              "excludedAllergens": string[],
              "deliveryRequirement": "today" | "tomorrow" | number | null,
              "subscriptionAllowed": boolean,
              "mustBeReturnable": boolean,
              "needsClarification": boolean,
              "clarificationQuestion": string | null,
              "confidence": number
            }
            
            RULES:
            - Convert rupees to paise (multiply by 100). Never use floats.
            - Dietary tags: "vegan","vegetarian","gluten-free","dairy-free","keto","organic","sugar-free"
            - Allergens: "peanuts","tree-nuts","milk","eggs","wheat","soy","fish","shellfish","sesame"
            - Do NOT make up product information, prices, or stock.
            - If the buyer says "no subscription" or "one-time", set subscriptionAllowed to false.
            - Respond ONLY with valid JSON. No markdown, no explanation.
            """;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final FallbackIntentParser fallbackParser;

    public GroqIntentExtractor(
            @Value("${groq.api.key:}") String apiKey,
            ObjectMapper objectMapper,
            FallbackIntentParser fallbackParser) {
        this.objectMapper = objectMapper;
        this.fallbackParser = fallbackParser;
        if (apiKey != null && !apiKey.isBlank()) {
            this.restClient = RestClient.builder()
                    .baseUrl("https://api.groq.com/openai/v1")
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();
        } else {
            this.restClient = null;
        }
    }

    public IntentExtractionResult extractIntent(String prompt) {
        if (restClient != null) {
            try {
                var requestBody = Map.of(
                        "model", "llama-3.3-70b-versatile",
                        "messages", List.of(
                                Map.of("role", "system", "content", SYSTEM_PROMPT),
                                Map.of("role", "user", "content", prompt)
                        ),
                        "temperature", 0.1,
                        "max_tokens", 512,
                        "response_format", Map.of("type", "json_object")
                );

                String responseStr = restClient.post()
                        .uri("/chat/completions")
                        .body(requestBody)
                        .retrieve()
                        .body(String.class);

                var root = objectMapper.readTree(responseStr);
                String content = root.path("choices").path(0).path("message").path("content").asText();
                IntentRules rules = objectMapper.readValue(content, IntentRules.class);
                return new IntentExtractionResult(rules, "groq");
            } catch (Exception e) {
                System.err.println("[intentExtractor] Groq failed, using fallback parser: " + e.getMessage());
            }
        }

        // Fallback
        return new IntentExtractionResult(fallbackParser.parse(prompt), "fallback");
    }

    public record IntentExtractionResult(IntentRules rules, String source) {
    }
}
