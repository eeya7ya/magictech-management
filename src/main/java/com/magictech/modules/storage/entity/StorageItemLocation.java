package com.magictech.modules.storage.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * StorageItemLocation Entity - Junction table for Many-to-Many relationship
 * Tracks which items are in which storage locations with quantity per location
 *
 * This allows the same StorageItem to exist in multiple locations with different quantities
 */
@Entity
@Table(name = "storage_item_locations",
       uniqueConstraints = @UniqueConstraint(columnNames = {"storage_item_id", "storage_location_id"}))
public class StorageItemLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "storage_item_id", nullable = false)
    private StorageItem storageItem;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "storage_location_id", nullable = false)
    private StorageLocation storageLocation;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 0;

    // Bin/shelf location within the storage
    @Column(name = "bin_location", length = 50)
    private String binLocation;

    @Column(name = "shelf_number", length = 50)
    private String shelfNumber;

    @Column(name = "row_number", length = 50)
    private String rowNumber;

    // Minimum stock level for this location (for alerts)
    @Column(name = "min_stock_level")
    private Integer minStockLevel;

    // Maximum stock level for this location
    @Column(name = "max_stock_level")
    private Integer maxStockLevel;

    // Notes about this item in this location
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Last inventory check date
    @Column(name = "last_inventory_check")
    private LocalDateTime lastInventoryCheck;

    @Column(name = "last_inventory_by", length = 100)
    private String lastInventoryBy;

    // Metadata fields
    @Column(name = "date_added", nullable = false)
    private LocalDateTime dateAdded;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(nullable = false)
    private Boolean active = true;

    // Constructors
    public StorageItemLocation() {
        this.dateAdded = LocalDateTime.now();
        this.active = true;
        this.quantity = 0;
    }

    public StorageItemLocation(StorageItem storageItem, StorageLocation storageLocation, Integer quantity) {
        this();
        this.storageItem = storageItem;
        this.storageLocation = storageLocation;
        this.quantity = quantity != null ? quantity : 0;
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

    // Helper methods
    public boolean isLowStock() {
        if (minStockLevel == null) return false;
        return quantity <= minStockLevel;
    }

    public boolean isOverStock() {
        if (maxStockLevel == null) return false;
        return quantity >= maxStockLevel;
    }

    public String getFullBinLocation() {
        StringBuilder sb = new StringBuilder();
        if (rowNumber != null && !rowNumber.isEmpty()) {
            sb.append("Row ").append(rowNumber);
        }
        if (shelfNumber != null && !shelfNumber.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("Shelf ").append(shelfNumber);
        }
        if (binLocation != null && !binLocation.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("Bin ").append(binLocation);
        }
        return sb.length() > 0 ? sb.toString() : "N/A";
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public StorageItem getStorageItem() {
        return storageItem;
    }

    public void setStorageItem(StorageItem storageItem) {
        this.storageItem = storageItem;
    }

    public StorageLocation getStorageLocation() {
        return storageLocation;
    }

    public void setStorageLocation(StorageLocation storageLocation) {
        this.storageLocation = storageLocation;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity != null ? quantity : 0;
    }

    public String getBinLocation() {
        return binLocation;
    }

    public void setBinLocation(String binLocation) {
        this.binLocation = binLocation;
    }

    public String getShelfNumber() {
        return shelfNumber;
    }

    public void setShelfNumber(String shelfNumber) {
        this.shelfNumber = shelfNumber;
    }

    public String getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(String rowNumber) {
        this.rowNumber = rowNumber;
    }

    public Integer getMinStockLevel() {
        return minStockLevel;
    }

    public void setMinStockLevel(Integer minStockLevel) {
        this.minStockLevel = minStockLevel;
    }

    public Integer getMaxStockLevel() {
        return maxStockLevel;
    }

    public void setMaxStockLevel(Integer maxStockLevel) {
        this.maxStockLevel = maxStockLevel;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getLastInventoryCheck() {
        return lastInventoryCheck;
    }

    public void setLastInventoryCheck(LocalDateTime lastInventoryCheck) {
        this.lastInventoryCheck = lastInventoryCheck;
    }

    public String getLastInventoryBy() {
        return lastInventoryBy;
    }

    public void setLastInventoryBy(String lastInventoryBy) {
        this.lastInventoryBy = lastInventoryBy;
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

    @Override
    public String toString() {
        return "StorageItemLocation{" +
                "id=" + id +
                ", storageItem=" + (storageItem != null ? storageItem.getProductName() : "null") +
                ", storageLocation=" + (storageLocation != null ? storageLocation.getName() : "null") +
                ", quantity=" + quantity +
                ", binLocation='" + binLocation + '\'' +
                ", active=" + active +
                '}';
    }
}
