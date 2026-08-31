package com.proofcart.domain.repo;

import com.proofcart.domain.entity.AgentTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgentTokenRepository extends JpaRepository<AgentTokenEntity, UUID> {
    Optional<AgentTokenEntity> findByTokenHashAndRevokedFalse(String tokenHash);

    List<AgentTokenEntity> findByBuyerIdOrderByCreatedAtDesc(UUID buyerId);
}
