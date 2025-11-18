package com.magictech.modules.sales.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Sales Order Item View Model - FIXED VERSION
 * Data transfer object for UI layer
 */
public class SalesOrderItemViewModel {

    private Long id;
    private Long salesOrderId;
    private Long storageItemId;
    private String storageItemName;
    private String storageItemDescription;
    private String storageItemCategory;
    private Integer quantity;  // Changed from Double to Integer for item count
    private BigDecimal unitPrice;  // Changed from Double to BigDecimal
    private BigDecimal totalPrice;  // Changed from Double to BigDecimal
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public SalesOrderItemViewModel() {
    }

    public SalesOrderItemViewModel(Long id, Long storageItemId, String storageItemName,
                                   Integer quantity, BigDecimal unitPrice, BigDecimal totalPrice) {
        this.id = id;
        this.storageItemId = storageItemId;
        this.storageItemName = storageItemName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSalesOrderId() {
        return salesOrderId;
    }

    public void setSalesOrderId(Long salesOrderId) {
        this.salesOrderId = salesOrderId;
    }

    public Long getStorageItemId() {
        return storageItemId;
    }

    public void setStorageItemId(Long storageItemId) {
        this.storageItemId = storageItemId;
    }

    public String getStorageItemName() {
        return storageItemName;
    }

    public void setStorageItemName(String storageItemName) {
        this.storageItemName = storageItemName;
    }

    public String getStorageItemDescription() {
        return storageItemDescription;
    }

    public void setStorageItemDescription(String storageItemDescription) {
        this.storageItemDescription = storageItemDescription;
    }

    public String getStorageItemCategory() {
        return storageItemCategory;
    }

    public void setStorageItemCategory(String storageItemCategory) {
        this.storageItemCategory = storageItemCategory;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "SalesOrderItemViewModel{" +
                "id=" + id +
                ", storageItemName='" + storageItemName + '\'' +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", totalPrice=" + totalPrice +
                '}';
    }
}