package com.proofcart.domain.repo;

import com.proofcart.domain.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {
    List<ProductEntity> findByMerchantId(UUID merchantId);

    @Modifying
    @Query("""
            update ProductEntity p
               set p.reservedQuantity = coalesce(p.reservedQuantity, 0) + :quantity,
                   p.version = p.version + 1,
                   p.updatedAt = :updatedAt
             where p.id = :productId
               and p.stockQuantity - coalesce(p.reservedQuantity, 0) >= :quantity
            """)
    int reserveAvailableStock(@Param("productId") UUID productId, @Param("quantity") int quantity, @Param("updatedAt") Instant updatedAt);

    @Modifying
    @Query("""
            update ProductEntity p
               set p.reservedQuantity = coalesce(p.reservedQuantity, 0) - :quantity,
                   p.version = p.version + 1,
                   p.updatedAt = :updatedAt
             where p.id = :productId and coalesce(p.reservedQuantity, 0) >= :quantity
            """)
    int releaseReservedStock(@Param("productId") UUID productId, @Param("quantity") int quantity, @Param("updatedAt") Instant updatedAt);

    @Modifying
    @Query("""
            update ProductEntity p
               set p.stockQuantity = p.stockQuantity - :quantity,
                   p.reservedQuantity = coalesce(p.reservedQuantity, 0) - :quantity,
                   p.version = p.version + 1,
                   p.updatedAt = :updatedAt
             where p.id = :productId
               and p.stockQuantity >= :quantity
               and coalesce(p.reservedQuantity, 0) >= :quantity
            """)
    int captureReservedStock(@Param("productId") UUID productId, @Param("quantity") int quantity, @Param("updatedAt") Instant updatedAt);
}
