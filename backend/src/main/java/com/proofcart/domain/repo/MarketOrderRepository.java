package com.proofcart.domain.repo;

import com.proofcart.domain.entity.MarketOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarketOrderRepository extends JpaRepository<MarketOrderEntity, UUID> {
    Optional<MarketOrderEntity> findByRazorpayOrderId(String razorpayOrderId);

    List<MarketOrderEntity> findByBuyerIdOrderByCreatedAtDesc(UUID buyerId);
}
