package com.magictech.modules.storage.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Storage Item Entity - NEW STRUCTURE
 * Matches your table: ID | Manufacture | Product Name | Code | Serial Number | Quantity | Price
 */
@Entity
@Table(name = "storage_items")
public class StorageItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "manufacture", length = 200)
    private String manufacture;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "code", length = 100)
    private String code;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 0;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    // Metadata fields
    @Column(name = "date_added", nullable = false)
    private LocalDateTime dateAdded;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(nullable = false)
    private Boolean active = true;

    // Custom fields storage (JSON format)
    @Column(name = "custom_fields", columnDefinition = "TEXT")
    private String customFields;

    // Constructors
    public StorageItem() {
        this.dateAdded = LocalDateTime.now();
        this.active = true;
        this.quantity = 0;
    }

    public StorageItem(String manufacture, String productName, String code,
                       String serialNumber, Integer quantity, BigDecimal price) {
        this();
        this.manufacture = manufacture;
        this.productName = productName;
        this.code = code;
        this.serialNumber = serialNumber;
        this.quantity = quantity != null ? quantity : 0;
        this.price = price;
    }

    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        if (dateAdded == null) {
            dateAdded = LocalDateTime.now();
        }
        if (active == null) {
            active = true;
        }
        if (quantity == null) {
            quantity = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getManufacture() {
        return manufacture;
    }

    public void setManufacture(String manufacture) {
        this.manufacture = manufacture;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity != null ? quantity : 0;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public LocalDateTime getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(LocalDateTime dateAdded) {
        this.dateAdded = dateAdded;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getCustomFields() {
        return customFields;
    }

    public void setCustomFields(String customFields) {
        this.customFields = customFields;
    }

    @Override
    public String toString() {
        return "StorageItem{" +
                "id=" + id +
                ", manufacture='" + manufacture + '\'' +
                ", productName='" + productName + '\'' +
                ", code='" + code + '\'' +
                ", serialNumber='" + serialNumber + '\'' +
                ", quantity=" + quantity +
                ", price=" + price +
                ", active=" + active +
                '}';
    }
}