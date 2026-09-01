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

    // ─── Tamper-evident chain: each cart's events are linked and HMAC-signed so the trail
    // can be independently re-verified later, not just displayed as an unverifiable log. ───
    @Column(name = "chain_sequence")
    private Integer chainSequence;
    @Column(name = "prev_hash")
    private String prevHash;
    @Column(name = "hash")
    private String hash;

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

    public Integer getChainSequence() {
        return chainSequence;
    }

    public void setChainSequence(Integer v) {
        chainSequence = v;
    }

    public String getPrevHash() {
        return prevHash;
    }

    public void setPrevHash(String v) {
        prevHash = v;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String v) {
        hash = v;
    }
}
