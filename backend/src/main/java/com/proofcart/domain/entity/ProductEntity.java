package com.proofcart.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "products")


public class ProductEntity {
    @Id
    private UUID id;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "price_paise", nullable = false)
    private Integer pricePaise;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "dietary_tags")
    private List<String> dietaryTags;

    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> allergens;

    @Column(name = "delivery_days", nullable = false)
    private Integer deliveryDays;

    @Column(nullable = false)
    private Boolean returnable;

    @Column(name = "subscription_available", nullable = false)
    private Boolean subscriptionAvailable;

    @Column(nullable = false)
    private Integer version;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    public ProductEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(UUID merchantId) {
        this.merchantId = merchantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getPricePaise() {
        return pricePaise;
    }

    public void setPricePaise(Integer pricePaise) {
        this.pricePaise = pricePaise;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public List<String> getDietaryTags() {
        return dietaryTags;
    }

    public void setDietaryTags(List<String> dietaryTags) {
        this.dietaryTags = dietaryTags;
    }

    public List<String> getAllergens() {
        return allergens;
    }

    public void setAllergens(List<String> allergens) {
        this.allergens = allergens;
    }

    public Integer getDeliveryDays() {
        return deliveryDays;
    }

    public void setDeliveryDays(Integer deliveryDays) {
        this.deliveryDays = deliveryDays;
    }

    public Boolean getReturnable() {
        return returnable;
    }

    public void setReturnable(Boolean returnable) {
        this.returnable = returnable;
    }

    public Boolean getSubscriptionAvailable() {
        return subscriptionAvailable;
    }

    public void setSubscriptionAvailable(Boolean subscriptionAvailable) {
        this.subscriptionAvailable = subscriptionAvailable;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
