package com.proofcart.upsell;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proofcart.domain.CartItem;
import com.proofcart.domain.IntentRules;
import com.proofcart.domain.entity.ProductEntity;
import com.proofcart.domain.repo.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Cross-sell agent for an already-ALLOWED proof cart.
 * <p>
 * Eligibility, price and ranking are computed entirely in Java against the same rules the
 * policy engine enforces, so a suggestion can never violate the buyer's own contract. The LLM
 * (when configured) is used ONLY to phrase a short reason for candidates this service already
 * selected — it cannot introduce a product, change a price, or override an eligibility check.
 */
@Service
public class UpsellService {
    private static final String SYSTEM_PROMPT = """
            You are an upsell copywriter for a shopping app called ProofCart.
            You will receive the buyer's current cart items and a list of candidate add-on
            products that have ALREADY been verified as in-budget, in-stock and rule-compliant.
            Write one short buyer-facing reason (max 16 words) per candidate explaining why it
            pairs well with the cart. Do NOT invent facts, prices, stock or claims not present
            in the data you were given, and do NOT suggest any product other than the candidates.
            Respond ONLY with valid JSON: {"reasons": {"<productId>": "<reason>", ...}}
            """;

    private final ProductRepository products;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public UpsellService(ProductRepository products, ObjectMapper objectMapper,
                         @Value("${groq.api.key:}") String apiKey) {
        this.products = products;
        this.objectMapper = objectMapper;
        this.restClient = (apiKey == null || apiKey.isBlank()) ? null : RestClient.builder()
                .baseUrl("https://api.groq.com/openai/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public List<UpsellSuggestion> suggest(UUID merchantId, List<CartItem> cartItems, Integer cartTotalPaise, IntentRules rules) {
        Set<String> inCart = cartItems.stream().map(CartItem::productId).collect(Collectors.toSet());
        Set<String> cartTags = cartItems.stream()
                .flatMap(i -> Optional.ofNullable(i.snapshot().dietaryTags()).orElse(List.of()).stream())
                .collect(Collectors.toSet());

        Integer headroom = rules.maxTotalPaise() == null ? null : rules.maxTotalPaise() - cartTotalPaise;
        if (headroom != null && headroom <= 0) return List.of();
        // No explicit budget stated: cap suggestions at a third of the cart so they read as an
        // add-on rather than a second order.
        int priceCap = headroom != null ? headroom : Math.max(5000, cartTotalPaise / 3);

        int maxDeliveryDays = maxDeliveryDays(rules.deliveryRequirement());

        List<ProductEntity> candidates = products.findByMerchantId(merchantId).stream()
                .filter(p -> !inCart.contains(p.getId().toString()))
                .filter(p -> p.getStockQuantity() - p.getReservedQuantity() > 0)
                .filter(p -> p.getPricePaise() <= priceCap)
                .filter(p -> noExcludedAllergen(p, rules.excludedAllergens()))
                .filter(p -> hasAllRequiredTags(p, rules.mustHaveTags()))
                .filter(p -> !Boolean.TRUE.equals(rules.mustBeReturnable()) || Boolean.TRUE.equals(p.getReturnable()))
                .filter(p -> p.getDeliveryDays() == null || p.getDeliveryDays() <= maxDeliveryDays)
                .sorted(Comparator.<ProductEntity>comparingInt(p -> -tagOverlap(p, cartTags))
                        .thenComparingInt(ProductEntity::getPricePaise))
                .limit(3)
                .toList();

        if (candidates.isEmpty()) return List.of();

        Map<String, String> reasons = generateReasons(cartItems, candidates);

        return candidates.stream()
                .map(p -> new UpsellSuggestion(
                        p.getId().toString(), p.getName(), p.getPricePaise(),
                        reasons.getOrDefault(p.getId().toString(), fallbackReason(p, cartTags)),
                        p.getDietaryTags() == null ? List.of() : p.getDietaryTags(),
                        p.getReturnable()))
                .toList();
    }

    private boolean noExcludedAllergen(ProductEntity p, List<String> excluded) {
        if (excluded == null || excluded.isEmpty() || p.getAllergens() == null) return true;
        return excluded.stream().noneMatch(a -> p.getAllergens().stream().anyMatch(pa -> pa.equalsIgnoreCase(a)));
    }

    private boolean hasAllRequiredTags(ProductEntity p, List<String> required) {
        if (required == null || required.isEmpty()) return true;
        List<String> tags = p.getDietaryTags();
        return tags != null && tags.containsAll(required);
    }

    private int maxDeliveryDays(Object deliveryRequirement) {
        if (deliveryRequirement == null) return Integer.MAX_VALUE;
        if ("today".equals(deliveryRequirement)) return 0;
        if ("tomorrow".equals(deliveryRequirement)) return 1;
        if (deliveryRequirement instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(deliveryRequirement.toString());
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }

    private int tagOverlap(ProductEntity p, Set<String> cartTags) {
        if (p.getDietaryTags() == null) return 0;
        return (int) p.getDietaryTags().stream().filter(cartTags::contains).count();
    }

    private String fallbackReason(ProductEntity p, Set<String> cartTags) {
        if (p.getDietaryTags() != null && p.getDietaryTags().stream().anyMatch(cartTags::contains)) {
            return "Matches the dietary preferences already in your cart.";
        }
        return "Popular add-on that fits your budget and delivery window.";
    }

    private Map<String, String> generateReasons(List<CartItem> cartItems, List<ProductEntity> candidates) {
        if (restClient == null) return Map.of();
        try {
            var cartPayload = cartItems.stream()
                    .map(i -> Map.of("name", i.snapshot().name(),
                            "tags", Optional.ofNullable(i.snapshot().dietaryTags()).orElse(List.of())))
                    .toList();
            var candidatePayload = candidates.stream()
                    .map(p -> Map.of("productId", p.getId().toString(), "name", p.getName(),
                            "tags", p.getDietaryTags() == null ? List.of() : p.getDietaryTags()))
                    .toList();
            String userContent = objectMapper.writeValueAsString(Map.of("cartItems", cartPayload, "candidates", candidatePayload));

            var requestBody = Map.of(
                    "model", "llama-3.3-70b-versatile",
                    "messages", List.of(
                            Map.of("role", "system", "content", SYSTEM_PROMPT),
                            Map.of("role", "user", "content", userContent)
                    ),
                    "temperature", 0.4,
                    "max_tokens", 300,
                    "response_format", Map.of("type", "json_object")
            );

            String responseStr = restClient.post()
                    .uri("/chat/completions")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseStr);
            String content = root.path("choices").path(0).path("message").path("content").asText();
            JsonNode reasonsNode = objectMapper.readTree(content).path("reasons");
            Set<String> validIds = candidates.stream().map(p -> p.getId().toString()).collect(Collectors.toSet());
            Map<String, String> out = new HashMap<>();
            reasonsNode.fields().forEachRemaining(e -> {
                // Only accept reasons for candidates we actually selected — the model cannot
                // smuggle in a suggestion for a product it wasn't given.
                if (validIds.contains(e.getKey())) out.put(e.getKey(), e.getValue().asText());
            });
            return out;
        } catch (Exception e) {
            System.err.println("[upsell] Groq reasoning failed, using fallback text: " + e.getMessage());
            return Map.of();
        }
    }
}
