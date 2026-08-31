package com.proofcart.domain.repo;

import com.proofcart.domain.entity.AuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, UUID> {
    List<AuditEventEntity> findByBuyerIdOrderByCreatedAtAsc(UUID buyerId);

    List<AuditEventEntity> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
}
