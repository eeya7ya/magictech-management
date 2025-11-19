package com.magictech.modules.sales.entity;

import com.magictech.modules.storage.entity.StorageItem;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Customer Element Entity
 * Links storage items to customers (similar to ProjectElement)
 */
@Entity
@Table(name = "customer_elements")
public class CustomerElement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "storage_item_id", nullable = false)
    private StorageItem storageItem;

    @Column(name = "quantity_needed")
    private Integer quantityNeeded = 0;

    @Column(name = "quantity_allocated")
    private Integer quantityAllocated = 0;

    @Column(length = 100)
    private String status; // PENDING, ALLOCATED, DELIVERED

    @Column(name = "unit_price")
    private java.math.BigDecimal unitPrice;

    @Column(name = "total_price")
    private java.math.BigDecimal totalPrice;

    @Column(length = 500)
    private String notes;

    @Column(name = "date_added", nullable = false)
    private LocalDateTime dateAdded;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(nullable = false)
    private Boolean active = true;

    public CustomerElement() {
        this.dateAdded = LocalDateTime.now();
        this.active = true;
        this.quantityNeeded = 0;
        this.quantityAllocated = 0;
        this.status = "PENDING";
    }

    @PrePersist
    protected void onCreate() {
        if (this.dateAdded == null) {
            this.dateAdded = LocalDateTime.now();
        }
        if (this.active == null) {
            this.active = true;
        }
        if (this.quantityNeeded == null) quantityNeeded = 0;
        if (this.quantityAllocated == null) quantityAllocated = 0;
        calculateTotalPrice();
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastUpdated = LocalDateTime.now();
        calculateTotalPrice();
    }

    private void calculateTotalPrice() {
        if (unitPrice != null && quantityNeeded != null) {
            totalPrice = unitPrice.multiply(new java.math.BigDecimal(quantityNeeded));
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public StorageItem getStorageItem() { return storageItem; }
    public void setStorageItem(StorageItem storageItem) { this.storageItem = storageItem; }

    public Integer getQuantityNeeded() { return quantityNeeded; }
    public void setQuantityNeeded(Integer quantityNeeded) {
        this.quantityNeeded = quantityNeeded;
        calculateTotalPrice();
    }

    public Integer getQuantityAllocated() { return quantityAllocated; }
    public void setQuantityAllocated(Integer quantityAllocated) {
        this.quantityAllocated = quantityAllocated;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public java.math.BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(java.math.BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        calculateTotalPrice();
    }

    public java.math.BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(java.math.BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getDateAdded() { return dateAdded; }
    public void setDateAdded(LocalDateTime dateAdded) { this.dateAdded = dateAdded; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
