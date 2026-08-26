package com.proofcart.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "proof_carts")


public class ProofCartEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "buyer_id", nullable = false)
    private UUID buyerId;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "intent_contract_id")
    private UUID intentContractId;

    @Column(name = "total_paise", nullable = false)
    private Integer totalPaise;

    @Column(name = "offer_hash", nullable = false)
    private String offerHash;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snapshot_data", nullable = false)
    private String snapshotDataJson;

    @Column(name = "policy_decision", nullable = false)
    private String policyDecision;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "policy_checks", nullable = false)
    private String policyChecksJson;

    @Column(nullable = false)
    private Boolean approved = false;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    public ProofCartEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(UUID buyerId) {
        this.buyerId = buyerId;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(UUID merchantId) {
        this.merchantId = merchantId;
    }

    public UUID getIntentContractId() {
        return intentContractId;
    }

    public void setIntentContractId(UUID intentContractId) {
        this.intentContractId = intentContractId;
    }

    public Integer getTotalPaise() {
        return totalPaise;
    }

    public void setTotalPaise(Integer totalPaise) {
        this.totalPaise = totalPaise;
    }

    public String getOfferHash() {
        return offerHash;
    }

    public void setOfferHash(String offerHash) {
        this.offerHash = offerHash;
    }

    public String getSnapshotDataJson() {
        return snapshotDataJson;
    }

    public void setSnapshotDataJson(String snapshotDataJson) {
        this.snapshotDataJson = snapshotDataJson;
    }

    public String getPolicyDecision() {
        return policyDecision;
    }

    public void setPolicyDecision(String policyDecision) {
        this.policyDecision = policyDecision;
    }

    public String getPolicyChecksJson() {
        return policyChecksJson;
    }

    public void setPolicyChecksJson(String policyChecksJson) {
        this.policyChecksJson = policyChecksJson;
    }

    public Boolean getApproved() {
        return approved;
    }

    public void setApproved(Boolean approved) {
        this.approved = approved;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
