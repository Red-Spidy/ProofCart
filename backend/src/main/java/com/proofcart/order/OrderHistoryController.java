package com.proofcart.order;

import com.proofcart.domain.entity.CheckoutOrderEntity;
import com.proofcart.domain.entity.ProofCartEntity;
import com.proofcart.domain.repo.CheckoutOrderRepository;
import com.proofcart.domain.repo.ProofCartRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
public class OrderHistoryController {

    private final ProofCartRepository proofCartRepository;
    private final CheckoutOrderRepository checkoutOrderRepository;

    public OrderHistoryController(ProofCartRepository proofCartRepository, CheckoutOrderRepository checkoutOrderRepository) {
        this.proofCartRepository = proofCartRepository;
        this.checkoutOrderRepository = checkoutOrderRepository;
    }

    @GetMapping
    @Cacheable(value = "orderHistory", key = "T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName()")
    public ResponseEntity<?> getOrderHistory() {
        String buyerIdStr = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID buyerId;
        try {
            buyerId = UUID.fromString(buyerIdStr);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid user context"));
        }

        List<ProofCartEntity> carts = proofCartRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId);
        List<CheckoutOrderEntity> orders = checkoutOrderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId);

        // Map carts to a simplified DTO
        List<Map<String, Object>> mappedCarts = carts.stream().map(cart -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", cart.getId());
            // Derive a human-readable status from the entity fields
            String cartStatus = cart.getApproved() ? "APPROVED" : cart.getPolicyDecision();
            map.put("status", cartStatus);
            map.put("totalPaise", cart.getTotalPaise());
            map.put("createdAt", cart.getCreatedAt());

            // Find corresponding order if it exists
            CheckoutOrderEntity linkedOrder = orders.stream()
                    .filter(o -> o.getCartId().equals(cart.getId()))
                    .findFirst()
                    .orElse(null);

            if (linkedOrder != null) {
                map.put("orderId", linkedOrder.getId());
                map.put("paymentStatus", linkedOrder.getStatus());
            }
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("history", mappedCarts));
    }
}
