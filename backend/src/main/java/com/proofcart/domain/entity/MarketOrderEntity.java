package com.proofcart.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * A single Razorpay payment collected once and settled across multiple merchants' proof carts
 * that share one intent contract — e.g. "birthday dinner: cake from Bakery, flowers from
 * FlowerShop, under ₹1500" fulfilled in one checkout instead of N separate ones.
 * {@code settlementJson} is the per-merchant breakdown of that single collection, the ledger a
 * real marketplace would use to route funds on to each seller.
 */
@Entity
@Table(name = "market_orders")
public class MarketOrderEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "buyer_id", nullable = false)
    private UUID buyerId;

    @Column(name = "intent_contract_id", nullable = false)
    private UUID intentContractId;

    @Column(name = "razorpay_order_id", nullable = false, unique = true)
    private String razorpayOrderId;

    @Column(name = "total_paise", nullable = false)
    private Integer totalPaise;

    @Column(nullable = false)
    private String status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settlement_json", nullable = false)
    private String settlementJson;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

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

    public UUID getIntentContractId() {
        return intentContractId;
    }

    public void setIntentContractId(UUID intentContractId) {
        this.intentContractId = intentContractId;
    }

    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public void setRazorpayOrderId(String razorpayOrderId) {
        this.razorpayOrderId = razorpayOrderId;
    }

    public Integer getTotalPaise() {
        return totalPaise;
    }

    public void setTotalPaise(Integer totalPaise) {
        this.totalPaise = totalPaise;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSettlementJson() {
        return settlementJson;
    }

    public void setSettlementJson(String settlementJson) {
        this.settlementJson = settlementJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
