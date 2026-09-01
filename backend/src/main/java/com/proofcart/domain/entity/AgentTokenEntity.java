package com.proofcart.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
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

    // ─── Spending mandate: bounds on what this agent may commit without a fresh
    // buyer-issued token, mirroring NPCI UAP-style delegated payment mandates. ───
    @Column(name = "max_per_transaction_paise")
    private Integer maxPerTransactionPaise;
    @Column(name = "max_daily_paise")
    private Integer maxDailyPaise;
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "allowed_merchant_ids")
    private List<String> allowedMerchantIds;

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

    public Integer getMaxPerTransactionPaise() {
        return maxPerTransactionPaise;
    }

    public void setMaxPerTransactionPaise(Integer value) {
        maxPerTransactionPaise = value;
    }

    public Integer getMaxDailyPaise() {
        return maxDailyPaise;
    }

    public void setMaxDailyPaise(Integer value) {
        maxDailyPaise = value;
    }

    public List<String> getAllowedMerchantIds() {
        return allowedMerchantIds;
    }

    public void setAllowedMerchantIds(List<String> value) {
        allowedMerchantIds = value;
    }
}
