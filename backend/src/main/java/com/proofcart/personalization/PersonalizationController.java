package com.proofcart.personalization;

import com.proofcart.domain.entity.ProductEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/personalization")
public class PersonalizationController {
    private final PersonalizationService service;

    public PersonalizationController(PersonalizationService service) { this.service = service; }

    @PostMapping("/events")
    public ResponseEntity<?> record(@RequestBody EventRequest request, Authentication authentication) {
        try {
            service.record(buyerId(authentication), request.eventType(), request.productId(), request.searchTerm());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/recommendations")
    public ResponseEntity<?> recommendations(@RequestParam UUID merchantId, Authentication authentication) {
        List<Map<String, Object>> result = service.recommend(buyerId(authentication), merchantId).stream()
                .map(r -> productResponse(r.product(), r.reason())).toList();
        return ResponseEntity.ok(Map.of("products", result));
    }

    private UUID buyerId(Authentication authentication) {
        return UUID.fromString((String) authentication.getPrincipal());
    }

    private Map<String, Object> productResponse(ProductEntity p, String reason) {
        return Map.ofEntries(
                Map.entry("id", p.getId().toString()), Map.entry("merchantId", p.getMerchantId().toString()),
                Map.entry("name", p.getName()), Map.entry("description", p.getDescription() == null ? "" : p.getDescription()),
                Map.entry("pricePaise", p.getPricePaise()), Map.entry("stockQuantity", p.getStockQuantity()),
                Map.entry("dietaryTags", p.getDietaryTags() == null ? List.of() : p.getDietaryTags()),
                Map.entry("allergens", p.getAllergens() == null ? List.of() : p.getAllergens()),
                Map.entry("deliveryDays", p.getDeliveryDays()), Map.entry("returnable", p.getReturnable()),
                Map.entry("subscriptionAvailable", p.getSubscriptionAvailable()), Map.entry("reason", reason));
    }

    public record EventRequest(String eventType, UUID productId, String searchTerm) { }
}
