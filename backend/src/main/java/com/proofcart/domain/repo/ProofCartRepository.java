package com.proofcart.domain.repo;

import com.proofcart.domain.entity.ProofCartEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProofCartRepository extends JpaRepository<ProofCartEntity, UUID> {
}
