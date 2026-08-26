package com.proofcart.domain.repo;

import com.proofcart.domain.entity.PersonalizationEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PersonalizationEventRepository extends JpaRepository<PersonalizationEventEntity, UUID> {
    List<PersonalizationEventEntity> findTop200ByBuyerIdOrderByCreatedAtDesc(UUID buyerId);
}
