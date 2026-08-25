package com.proofcart.cache;

import com.proofcart.domain.entity.ProductEntity;
import com.proofcart.domain.repo.ProductRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class CatalogCacheService {

    private static final String CATALOG_KEY_PREFIX = "catalog:merchant:";
    private final ProductRepository productRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public CatalogCacheService(ProductRepository productRepository, RedisTemplate<String, Object> redisTemplate) {
        this.productRepository = productRepository;
        this.redisTemplate = redisTemplate;
    }

    @SuppressWarnings("unchecked")
    public List<ProductEntity> getCatalogForMerchant(UUID merchantId) {
        String key = CATALOG_KEY_PREFIX + merchantId;

        try {
            List<ProductEntity> cached = (List<ProductEntity>) redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            System.err.println("Redis cache miss/failure: " + e.getMessage());
        }

        // Fallback to DB
        List<ProductEntity> products = productRepository.findByMerchantId(merchantId);

        try {
            // Cache for 60 seconds as per requirements
            redisTemplate.opsForValue().set(key, products, 60, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("Redis cache set failure: " + e.getMessage());
        }

        return products;
    }

    public void invalidateCatalogCache(UUID merchantId) {
        try {
            redisTemplate.delete(CATALOG_KEY_PREFIX + merchantId);
        } catch (Exception e) {
            System.err.println("Redis cache invalidate failure: " + e.getMessage());
        }
    }
}
