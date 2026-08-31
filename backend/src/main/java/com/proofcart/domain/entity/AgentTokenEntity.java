package com.proofcart.domain.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agent_tokens")
public class AgentTokenEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "buyer_id", nullable = false)
    private UUID buyerId;
    @Column(nullable = false)
    private String name;
    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;
    @Column(nullable = false)
    private boolean revoked = false;
    @Column(name = "created_at", nullable = false)
    private final Instant createdAt = Instant.now();
    @Column(name = "expires_at")
    private Instant expiresAt;

    public UUID getId() {
        return id;
    }

    public UUID getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(UUID value) {
        buyerId = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String value) {
        name = value;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String value) {
        tokenHash = value;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean value) {
        revoked = value;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant value) {
        expiresAt = value;
    }
}
