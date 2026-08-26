package com.proofcart.catalog.openfoodfacts;

import com.proofcart.domain.entity.ProductEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class OpenFoodFactsProductMapper {

    // Default mock merchant for imported products
    private static final UUID MOCK_MERCHANT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    public ProductEntity mapToEntity(OpenFoodFactsResponse.ProductData data) {
        ProductEntity entity = new ProductEntity();
        
        // Use a deterministic UUID based on barcode, or random if barcode missing
        String code = data.getCode() != null ? data.getCode() : UUID.randomUUID().toString();
        entity.setId(UUID.nameUUIDFromBytes(code.getBytes()));
        entity.setMerchantId(MOCK_MERCHANT_ID);
        
        String name = data.getProductName();
        if (name == null || name.isBlank()) {
            name = "Unknown Product (" + code + ")";
        }
        entity.setName(name);
        
        String description = data.getBrands() != null ? "Brand: " + data.getBrands() : "";
        if (data.getIngredientsText() != null && !data.getIngredientsText().isBlank()) {
            description += " | Ingredients: " + data.getIngredientsText();
        }
        if (description.length() > 255) {
            description = description.substring(0, 252) + "..."; // truncate for DB constraint
        }
        entity.setDescription(description);

        // Mock price (between 50 and 500 rupees = 5000 to 50000 paise)
        int randomPrice = (int) (Math.random() * 45000) + 5000;
        entity.setPricePaise(randomPrice);

        // Mock stock
        entity.setStockQuantity(100);
        
        // Parse Dietary Tags
        List<String> dietaryTags = new ArrayList<>();
        if (data.getLabelsTags() != null) {
            dietaryTags = data.getLabelsTags().stream()
                    .map(tag -> tag.replace("en:", "").toLowerCase())
                    .collect(Collectors.toList());
        }
        entity.setDietaryTags(dietaryTags);

        // Parse Allergens
        List<String> allergens = new ArrayList<>();
        if (data.getAllergensTags() != null) {
            allergens = data.getAllergensTags().stream()
                    .map(tag -> tag.replace("en:", "").toLowerCase())
                    .collect(Collectors.toList());
        }
        entity.setAllergens(allergens);

        // Fixed mock policies
        entity.setDeliveryDays(1); // Next day delivery
        entity.setReturnable(false);
        entity.setSubscriptionAvailable(false);
        entity.setVersion(1);

        return entity;
    }
}
