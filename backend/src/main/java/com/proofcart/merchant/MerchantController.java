package com.proofcart.merchant;

import com.proofcart.account.AccountService;
import com.proofcart.cache.CatalogCacheService;
import com.proofcart.domain.entity.Merchant;
import com.proofcart.domain.entity.ProductEntity;
import com.proofcart.domain.repo.MerchantRepository;
import com.proofcart.domain.repo.ProductRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/merchant")
public class MerchantController {
    private final AccountService accounts;
    private final MerchantRepository merchants;
    private final ProductRepository products;
    private final CatalogCacheService catalogCache;

    public MerchantController(AccountService accounts, MerchantRepository merchants,
                              ProductRepository products, CatalogCacheService catalogCache) {
        this.accounts = accounts;
        this.merchants = merchants;
        this.products = products;
        this.catalogCache = catalogCache;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(Authentication authentication) {
        try {
            UUID ownerId = userId(authentication);
            accounts.requireMerchant(ownerId);
            return merchants.findByOwnerId(ownerId)
                    .<ResponseEntity<?>>map(merchant -> ResponseEntity.ok(Map.of(
                            "onboardingRequired", false,
                            "merchant", merchantResponse(merchant),
                            "products", products.findByMerchantId(merchant.getId()).stream().map(this::productResponse).toList())))
                    .orElseGet(() -> ResponseEntity.ok(Map.of("onboardingRequired", true, "products", List.of())));
        } catch (AccountService.ForbiddenRoleException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/store")
    public ResponseEntity<?> createStore(@RequestBody StoreRequest request, Authentication authentication) {
        try {
            UUID ownerId = userId(authentication);
            accounts.requireMerchant(ownerId);
            Merchant merchant = merchants.findByOwnerId(ownerId).orElseGet(Merchant::new);
            merchant.setOwnerId(ownerId);
            merchant.setName(required(request.name(), "Store name"));
            merchant.setDescription(blankToNull(request.description()));
            Merchant saved = merchants.save(merchant);
            return ResponseEntity.ok(Map.of("merchant", merchantResponse(saved)));
        } catch (AccountService.ForbiddenRoleException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/products")
    public ResponseEntity<?> createProduct(@RequestBody ProductRequest request, Authentication authentication) {
        try {
            Merchant merchant = ownedMerchant(userId(authentication));
            ProductEntity product = new ProductEntity();
            product.setId(UUID.randomUUID());
            applyProduct(product, request);
            product.setMerchantId(merchant.getId());
            product.setReservedQuantity(0);
            product.setVersion(1);
            ProductEntity saved = products.save(product);
            catalogCache.invalidateCatalogCache(merchant.getId());
            return ResponseEntity.ok(Map.of("product", productResponse(saved)));
        } catch (AccountService.ForbiddenRoleException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable UUID id, @RequestBody ProductRequest request, Authentication authentication) {
        try {
            Merchant merchant = ownedMerchant(userId(authentication));
            ProductEntity product = products.findById(id).orElseThrow(() -> new IllegalArgumentException("Product not found."));
            if (!merchant.getId().equals(product.getMerchantId()))
                return ResponseEntity.status(403).body(Map.of("error", "Access denied."));
            applyProduct(product, request);
            ProductEntity saved = products.save(product);
            catalogCache.invalidateCatalogCache(merchant.getId());
            return ResponseEntity.ok(Map.of("product", productResponse(saved)));
        } catch (AccountService.ForbiddenRoleException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Merchant ownedMerchant(UUID ownerId) {
        accounts.requireMerchant(ownerId);
        return merchants.findByOwnerId(ownerId).orElseThrow(() -> new IllegalArgumentException("Create your store before managing products."));
    }

    private void applyProduct(ProductEntity product, ProductRequest request) {
        int stock = request.stockQuantity() == null ? -1 : request.stockQuantity();
        if (stock < product.getReservedQuantity())
            throw new IllegalArgumentException("Stock cannot be lower than units currently reserved for checkout.");
        if (request.pricePaise() == null || request.pricePaise() < 1)
            throw new IllegalArgumentException("Price must be at least 1 paise.");
        product.setName(required(request.name(), "Product name"));
        product.setDescription(blankToNull(request.description()));
        product.setPricePaise(request.pricePaise());
        product.setStockQuantity(stock);
        product.setDietaryTags(request.dietaryTags() == null ? List.of() : request.dietaryTags());
        product.setAllergens(request.allergens() == null ? List.of() : request.allergens());
        product.setDeliveryDays(request.deliveryDays() == null ? 0 : Math.max(0, request.deliveryDays()));
        product.setReturnable(Boolean.TRUE.equals(request.returnable()));
        product.setSubscriptionAvailable(Boolean.TRUE.equals(request.subscriptionAvailable()));
    }

    private String required(String value, String field) {
        if (value == null || value.trim().isBlank()) throw new IllegalArgumentException(field + " is required.");
        return value.trim().substring(0, Math.min(value.trim().length(), 255));
    }

    private String blankToNull(String value) {
        return value == null || value.trim().isBlank() ? null : value.trim();
    }

    private UUID userId(Authentication authentication) {
        return UUID.fromString((String) authentication.getPrincipal());
    }

    private Map<String, Object> merchantResponse(Merchant merchant) {
        return Map.of("id", merchant.getId().toString(), "name", merchant.getName(), "description", merchant.getDescription() == null ? "" : merchant.getDescription());
    }

    private Map<String, Object> productResponse(ProductEntity p) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("id", p.getId().toString());
        result.put("name", p.getName());
        result.put("description", p.getDescription() == null ? "" : p.getDescription());
        result.put("pricePaise", p.getPricePaise());
        result.put("stockQuantity", p.getStockQuantity());
        result.put("reservedQuantity", p.getReservedQuantity());
        result.put("availableQuantity", p.getStockQuantity() - p.getReservedQuantity());
        result.put("dietaryTags", p.getDietaryTags() == null ? List.of() : p.getDietaryTags());
        result.put("allergens", p.getAllergens() == null ? List.of() : p.getAllergens());
        result.put("deliveryDays", p.getDeliveryDays());
        result.put("returnable", p.getReturnable());
        result.put("subscriptionAvailable", p.getSubscriptionAvailable());
        return result;
    }

    public record StoreRequest(String name, String description) {
    }

    public record ProductRequest(String name, String description, Integer pricePaise, Integer stockQuantity,
                                 List<String> dietaryTags, List<String> allergens, Integer deliveryDays,
                                 Boolean returnable, Boolean subscriptionAvailable) {
    }
}
