package com.proofcart.domain.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "personalization_events")
public class PersonalizationEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "buyer_id", nullable = false)
    private UUID buyerId;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "search_term")
    private String searchTerm;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    public UUID getId() { return id; }
    public UUID getBuyerId() { return buyerId; }
    public void setBuyerId(UUID buyerId) { this.buyerId = buyerId; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getSearchTerm() { return searchTerm; }
    public void setSearchTerm(String searchTerm) { this.searchTerm = searchTerm; }
    public Instant getCreatedAt() { return createdAt; }
}
