package com.proofcart.domain.repo;

import com.proofcart.domain.entity.IntentContractEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IntentContractRepository extends JpaRepository<IntentContractEntity, UUID> {
}
