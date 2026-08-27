package com.proofcart.domain.repo;

import com.proofcart.domain.entity.InventoryReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservationEntity, UUID> {
    List<InventoryReservationEntity> findByOrderIdAndStatus(UUID orderId, String status);

    boolean existsByOrderIdAndStatus(UUID orderId, String status);

    List<InventoryReservationEntity> findByStatusAndExpiresAtBefore(String status, Instant now);

    @Modifying
    @Query("update InventoryReservationEntity r set r.status = :toStatus where r.id = :id and r.status = :fromStatus")
    int transitionStatus(@Param("id") UUID id, @Param("fromStatus") String fromStatus, @Param("toStatus") String toStatus);
}
