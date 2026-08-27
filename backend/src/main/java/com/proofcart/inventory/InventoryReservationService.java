package com.proofcart.inventory;

import com.proofcart.cache.CatalogCacheService;
import com.proofcart.domain.CartItem;
import com.proofcart.domain.entity.InventoryReservationEntity;
import com.proofcart.domain.repo.InventoryReservationRepository;
import com.proofcart.domain.repo.ProductRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Short-lived checkout stock holds. Conditional database updates remain correct if Redis is unavailable.
 */
@Service
public class InventoryReservationService {
    public static final String RESERVED = "RESERVED";
    public static final String CAPTURED = "CAPTURED";
    public static final String RELEASED = "RELEASED";
    public static final String EXPIRED = "EXPIRED";

    private static final Duration RESERVATION_TTL = Duration.ofMinutes(10);
    private static final Duration LOCK_TTL = Duration.ofSeconds(8);
    private static final String LOCK_PREFIX = "inventory:lock:";
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end", Long.class);

    private final ProductRepository products;
    private final InventoryReservationRepository reservations;
    private final RedisTemplate<String, Object> redis;
    private final CatalogCacheService catalogCache;

    public InventoryReservationService(ProductRepository products, InventoryReservationRepository reservations,
                                       RedisTemplate<String, Object> redis, CatalogCacheService catalogCache) {
        this.products = products;
        this.reservations = reservations;
        this.redis = redis;
        this.catalogCache = catalogCache;
    }

    @Transactional
    public void reserve(UUID orderId, List<CartItem> items) {
        releaseExpiredReservations();
        withProductLocks(items.stream().map(CartItem::productId).toList(), () -> {
            Instant expiresAt = Instant.now().plus(RESERVATION_TTL);
            for (CartItem item : items) {
                UUID productId = UUID.fromString(item.productId());
                if (products.reserveAvailableStock(productId, item.quantity(), Instant.now()) != 1) {
                    throw new InventoryUnavailableException("Sorry, " + item.snapshot().name() + " just sold out or no longer has enough stock.");
                }
                InventoryReservationEntity reservation = new InventoryReservationEntity();
                reservation.setOrderId(orderId);
                reservation.setProductId(productId);
                reservation.setQuantity(item.quantity());
                reservation.setStatus(RESERVED);
                reservation.setExpiresAt(expiresAt);
                reservations.save(reservation);
                catalogCache.invalidateCatalogCache(UUID.fromString(item.snapshot().merchantId()));
            }
        });
    }

    @Transactional
    public void capture(UUID orderId) {
        List<InventoryReservationEntity> held = reservations.findByOrderIdAndStatus(orderId, RESERVED);
        withProductLocks(held.stream().map(r -> r.getProductId().toString()).toList(), () -> {
            for (InventoryReservationEntity reservation : held) {
                if (reservations.transitionStatus(reservation.getId(), RESERVED, CAPTURED) == 1) {
                    if (products.captureReservedStock(reservation.getProductId(), reservation.getQuantity(), Instant.now()) != 1) {
                        throw new IllegalStateException("Inventory reservation cannot be captured.");
                    }
                    products.findById(reservation.getProductId())
                            .ifPresent(product -> catalogCache.invalidateCatalogCache(product.getMerchantId()));
                }
            }
        });
    }

    @Transactional
    public void release(UUID orderId, String finalStatus) {
        List<InventoryReservationEntity> held = reservations.findByOrderIdAndStatus(orderId, RESERVED);
        withProductLocks(held.stream().map(r -> r.getProductId().toString()).toList(), () -> {
            for (InventoryReservationEntity reservation : held) {
                if (reservations.transitionStatus(reservation.getId(), RESERVED, finalStatus) == 1) {
                    products.releaseReservedStock(reservation.getProductId(), reservation.getQuantity(), Instant.now());
                    products.findById(reservation.getProductId())
                            .ifPresent(product -> catalogCache.invalidateCatalogCache(product.getMerchantId()));
                }
            }
        });
    }

    @Scheduled(fixedDelayString = "${inventory.reservation-cleanup-ms:60000}")
    @Transactional
    public void releaseExpiredReservations() {
        for (InventoryReservationEntity reservation : reservations.findByStatusAndExpiresAtBefore(RESERVED, Instant.now())) {
            withProductLocks(List.of(reservation.getProductId().toString()), () -> {
                if (reservations.transitionStatus(reservation.getId(), RESERVED, EXPIRED) == 1) {
                    products.releaseReservedStock(reservation.getProductId(), reservation.getQuantity(), Instant.now());
                    products.findById(reservation.getProductId())
                            .ifPresent(product -> catalogCache.invalidateCatalogCache(product.getMerchantId()));
                }
            });
        }
    }

    private void withProductLocks(List<String> productIds, Runnable work) {
        List<RedisLock> locks = new ArrayList<>();
        try {
            for (String productId : productIds.stream().distinct().sorted().toList()) {
                RedisLock lock = tryLock(productId);
                if (lock == null)
                    throw new InventoryBusyException("Inventory is being updated. Please retry checkout.");
                locks.add(lock);
            }
            work.run();
        } finally {
            Collections.reverse(locks);
            locks.forEach(this::unlock);
        }
    }

    private RedisLock tryLock(String productId) {
        String key = LOCK_PREFIX + productId;
        String token = UUID.randomUUID().toString();
        try {
            Boolean acquired = redis.opsForValue().setIfAbsent(key, token, LOCK_TTL.toMillis(), TimeUnit.MILLISECONDS);
            return Boolean.TRUE.equals(acquired) ? new RedisLock(key, token) : null;
        } catch (DataAccessException e) {
            return new RedisLock(null, null);
        }
    }

    private void unlock(RedisLock lock) {
        if (lock.key == null) return;
        try {
            redis.execute(UNLOCK_SCRIPT, List.of(lock.key), lock.token);
        } catch (DataAccessException ignored) {
        }
    }

    private record RedisLock(String key, String token) {
    }

    public static class InventoryUnavailableException extends RuntimeException {
        public InventoryUnavailableException(String message) {
            super(message);
        }
    }

    public static class InventoryBusyException extends RuntimeException {
        public InventoryBusyException(String message) {
            super(message);
        }
    }
}
