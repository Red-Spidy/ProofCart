package com.proofcart.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "webhook_events")
public class WebhookEventEntity {
    @Id
    private String id;
    @Column(name = "event_type", nullable = false)
    private String eventType;
    @Column(name = "processed_at")
    private final Instant processedAt = Instant.now();

    public WebhookEventEntity() {
    }

    public WebhookEventEntity(String id, String type) {
        this.id = id;
        this.eventType = type;
    }
}
