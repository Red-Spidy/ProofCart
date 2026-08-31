package com.proofcart.domain.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "buyer_id")
    private UUID buyerId;
    @Column(name = "merchant_id")
    private UUID merchantId;
    @Column(name = "cart_id")
    private UUID cartId;
    @Column(name = "order_id")
    private UUID orderId;
    @Column(name = "event_type", nullable = false)
    private String eventType;
    @Column(nullable = false)
    private String description;
    @Column(columnDefinition = "json")
    private String metadata = "{}";
    @Column(name = "created_at")
    private final Instant createdAt = Instant.now();

    public UUID getId() {
        return id;
    }

    public UUID getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(UUID v) {
        buyerId = v;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(UUID v) {
        merchantId = v;
    }

    public UUID getCartId() {
        return cartId;
    }

    public void setCartId(UUID v) {
        cartId = v;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID v) {
        orderId = v;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String v) {
        eventType = v;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String v) {
        description = v;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String v) {
        metadata = v;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
