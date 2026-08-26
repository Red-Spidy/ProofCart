package com.proofcart.domain.repo;

import com.proofcart.domain.entity.CheckoutOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CheckoutOrderRepository extends JpaRepository<CheckoutOrderEntity, UUID> {
    CheckoutOrderEntity findByRazorpayOrderId(String razorpayOrderId);
    List<CheckoutOrderEntity> findByBuyerIdOrderByCreatedAtDesc(UUID buyerId);
}
