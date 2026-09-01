package com.proofcart.domain.repo;

import com.proofcart.domain.entity.CheckoutOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface CheckoutOrderRepository extends JpaRepository<CheckoutOrderEntity, UUID> {
    // A market order shares one razorpayOrderId across several merchants' sub-orders, so this
    // returns every CheckoutOrderEntity collected under that payment — a single-merchant
    // checkout is simply the common case where the list has exactly one element.
    List<CheckoutOrderEntity> findByRazorpayOrderId(String razorpayOrderId);

    CheckoutOrderEntity findFirstByCartIdAndStatusOrderByCreatedAtDesc(UUID cartId, String status);
    List<CheckoutOrderEntity> findByBuyerIdOrderByCreatedAtDesc(UUID buyerId);
    List<CheckoutOrderEntity> findByAgentTokenIdAndCreatedAtAfter(UUID agentTokenId, Instant after);
}
