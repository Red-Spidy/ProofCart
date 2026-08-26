package com.proofcart.catalog;

import com.proofcart.catalog.openfoodfacts.OpenFoodFactsClient;
import com.proofcart.catalog.openfoodfacts.OpenFoodFactsProductMapper;
import com.proofcart.catalog.openfoodfacts.OpenFoodFactsResponse;
import com.proofcart.domain.entity.ProductEntity;
import com.proofcart.domain.repo.ProductRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CatalogService {

    private final OpenFoodFactsClient openFoodFactsClient;
    private final OpenFoodFactsProductMapper productMapper;
    private final ProductRepository productRepository;

    public CatalogService(OpenFoodFactsClient openFoodFactsClient,
                          OpenFoodFactsProductMapper productMapper,
                          ProductRepository productRepository) {
        this.openFoodFactsClient = openFoodFactsClient;
        this.productMapper = productMapper;
        this.productRepository = productRepository;
    }

    @Cacheable(value = "catalogSearch", key = "#query")
    public List<ProductEntity> searchAndSyncCatalog(String query) {
        // Fetch from external API
        List<OpenFoodFactsResponse.ProductData> apiProducts = openFoodFactsClient.searchProducts(query);
        
        if (apiProducts == null || apiProducts.isEmpty()) {
            return List.of();
        }

        // Map to internal entities
        List<ProductEntity> mappedEntities = apiProducts.stream()
                .map(productMapper::mapToEntity)
                .collect(Collectors.toList());

        // Save to DB (upsert if exists, ignoring duplicates for simplicity by using saveAll which merges)
        // Note: For a robust system we'd handle existing items better, but since UUIDs are 
        // deterministic based on barcode, saveAll will overwrite/update existing records.
        return productRepository.saveAll(mappedEntities);
    }
}
