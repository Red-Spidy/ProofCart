package com.proofcart.cache;

import com.proofcart.domain.entity.ProductEntity;
import com.proofcart.domain.repo.ProductRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class CatalogCacheService {

    private static final String CATALOG_KEY_PREFIX = "catalog:merchant:";
    private static final long RETRY_AFTER_FAILURE_SECONDS = 30;
    private final ProductRepository productRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private volatile Instant redisRetryAfter = Instant.EPOCH;

    public CatalogCacheService(ProductRepository productRepository, RedisTemplate<String, Object> redisTemplate) {
        this.productRepository = productRepository;
        this.redisTemplate = redisTemplate;
    }

    @SuppressWarnings("unchecked")
    public List<ProductEntity> getCatalogForMerchant(UUID merchantId) {
        String key = CATALOG_KEY_PREFIX + merchantId;

        if (redisAvailable()) {
            try {
                List<ProductEntity> cached = (List<ProductEntity>) redisTemplate.opsForValue().get(key);
                if (cached != null) {
                    return cached;
                }
            } catch (Exception e) {
                markRedisUnavailable(e);
            }
        }

        // Fallback to DB
        List<ProductEntity> products = productRepository.findByMerchantId(merchantId);

        if (redisAvailable()) {
            try {
                // Cache for 60 seconds as per requirements
                redisTemplate.opsForValue().set(key, products, 60, TimeUnit.SECONDS);
            } catch (Exception e) {
                markRedisUnavailable(e);
            }
        }

        return products;
    }

    public void invalidateCatalogCache(UUID merchantId) {
        if (redisAvailable()) {
            try {
                redisTemplate.delete(CATALOG_KEY_PREFIX + merchantId);
            } catch (Exception e) {
                markRedisUnavailable(e);
            }
        }
    }

    private boolean redisAvailable() {
        return Instant.now().isAfter(redisRetryAfter);
    }

    private void markRedisUnavailable(Exception error) {
        redisRetryAfter = Instant.now().plusSeconds(RETRY_AFTER_FAILURE_SECONDS);
        System.err.println("Redis cache unavailable; using database fallback for " + RETRY_AFTER_FAILURE_SECONDS + " seconds: " + error.getMessage());
    }
}
