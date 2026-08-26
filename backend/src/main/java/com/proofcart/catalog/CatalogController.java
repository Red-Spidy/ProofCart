package com.proofcart.catalog;

import com.proofcart.domain.entity.ProductEntity;
import com.proofcart.domain.repo.ProductRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Public catalog endpoint — no authentication required.
 * Returns all products for a given merchant, usable by the Angular frontend on page load.
 */
@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final ProductRepository productRepository;
    private final CatalogService catalogService;

    public CatalogController(ProductRepository productRepository, CatalogService catalogService) {
        this.productRepository = productRepository;
        this.catalogService = catalogService;
    }

    @GetMapping("/{merchantId}")
    public ResponseEntity<?> getCatalog(@PathVariable String merchantId, @RequestParam(required = false) String q) {
        try {
            UUID mid = UUID.fromString(merchantId);
            List<ProductEntity> products;
            
            if (q != null && !q.isBlank()) {
                // If there's a search query, fetch from Open Food Facts & sync to DB
                products = catalogService.searchAndSyncCatalog(q);
            } else {
                // Default: get local seeded products
                products = productRepository.findByMerchantId(mid);
            }

            if (products.isEmpty()) {
                return ResponseEntity.ok(Map.of("products", List.of(), "message", "No products found"));
            }

            List<Map<String, Object>> result = products.stream().map(p -> {
                java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("id", p.getId().toString());
                m.put("merchantId", p.getMerchantId().toString());
                m.put("name", p.getName());
                m.put("description", p.getDescription());
                m.put("pricePaise", p.getPricePaise());
                m.put("stockQuantity", p.getStockQuantity());
                m.put("dietaryTags", p.getDietaryTags() != null ? p.getDietaryTags() : List.of());
                m.put("allergens", p.getAllergens() != null ? p.getAllergens() : List.of());
                m.put("deliveryDays", p.getDeliveryDays());
                m.put("returnable", p.getReturnable());
                m.put("subscriptionAvailable", p.getSubscriptionAvailable());
                m.put("version", p.getVersion());
                return m;
            }).toList();

            return ResponseEntity.ok(Map.of("products", result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid merchantId format."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
