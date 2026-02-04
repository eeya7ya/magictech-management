package com.magictech.modules.qualityassurance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PriceListItem Entity - Individual items from price list Excel
 * Each item represents a product with pricing from different sheets (manufacturers)
 */
@Entity
@Table(name = "price_list_items")
public class PriceListItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "price_list_id", nullable = false)
    private Long priceListId;

    // Manufacturer comes from Excel sheet name (e.g., HIKVISION, SIB)
    @Column(name = "manufacturer", nullable = false, length = 200)
    private String manufacturer;

    // Model field - key for matching with StorageItem
    @Column(name = "model", nullable = false, length = 150)
    private String model;

    // Description from Excel
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Product image stored as BLOB (extracted from Excel)
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "image_data", columnDefinition = "BYTEA")
    private byte[] imageData;

    @Column(name = "image_type", length = 50)
    private String imageType; // e.g., "image/png", "image/jpeg"

    // Three price tiers from Excel columns
    @Column(name = "dpp_price", precision = 12, scale = 2)
    private BigDecimal dppPrice;

    @Column(name = "si_installer_price", precision = 12, scale = 2)
    private BigDecimal siInstallerPrice;

    @Column(name = "end_user_price", precision = 12, scale = 2)
    private BigDecimal endUserPrice;

    // Currency (default JOD based on screenshot)
    @Column(name = "currency", length = 10)
    private String currency = "JOD";

    // Row number from Excel for reference
    @Column(name = "excel_row_number")
    private Integer excelRowNumber;

    // Link to StorageItem if model matches
    @Column(name = "storage_item_id")
    private Long storageItemId;

    // Match status for quick filtering
    @Enumerated(EnumType.STRING)
    @Column(name = "match_status", length = 30)
    private MatchStatus matchStatus = MatchStatus.NOT_CHECKED;

    // Metadata fields
    @Column(name = "date_added", nullable = false)
    private LocalDateTime dateAdded;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(nullable = false)
    private Boolean active = true;

    // Match status enum
    public enum MatchStatus {
        NOT_CHECKED,    // Not yet checked against storage
        MATCHED,        // Model found in storage
        NOT_FOUND       // Model not found in storage
    }

    // Constructors
    public PriceListItem() {
        this.dateAdded = LocalDateTime.now();
        this.active = true;
        this.matchStatus = MatchStatus.NOT_CHECKED;
    }

    public PriceListItem(Long priceListId, String manufacturer, String model) {
        this();
        this.priceListId = priceListId;
        this.manufacturer = manufacturer;
        this.model = model;
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
        if (matchStatus == null) {
            matchStatus = MatchStatus.NOT_CHECKED;
        }
        if (currency == null) {
            currency = "JOD";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }

    // Helper methods
    public boolean hasImage() {
        return imageData != null && imageData.length > 0;
    }

    public boolean isMatched() {
        return matchStatus == MatchStatus.MATCHED;
    }

    public boolean isNotFound() {
        return matchStatus == MatchStatus.NOT_FOUND;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPriceListId() {
        return priceListId;
    }

    public void setPriceListId(Long priceListId) {
        this.priceListId = priceListId;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public void setImageData(byte[] imageData) {
        this.imageData = imageData;
    }

    public String getImageType() {
        return imageType;
    }

    public void setImageType(String imageType) {
        this.imageType = imageType;
    }

    public BigDecimal getDppPrice() {
        return dppPrice;
    }

    public void setDppPrice(BigDecimal dppPrice) {
        this.dppPrice = dppPrice;
    }

    public BigDecimal getSiInstallerPrice() {
        return siInstallerPrice;
    }

    public void setSiInstallerPrice(BigDecimal siInstallerPrice) {
        this.siInstallerPrice = siInstallerPrice;
    }

    public BigDecimal getEndUserPrice() {
        return endUserPrice;
    }

    public void setEndUserPrice(BigDecimal endUserPrice) {
        this.endUserPrice = endUserPrice;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getExcelRowNumber() {
        return excelRowNumber;
    }

    public void setExcelRowNumber(Integer excelRowNumber) {
        this.excelRowNumber = excelRowNumber;
    }

    public Long getStorageItemId() {
        return storageItemId;
    }

    public void setStorageItemId(Long storageItemId) {
        this.storageItemId = storageItemId;
    }

    public MatchStatus getMatchStatus() {
        return matchStatus;
    }

    public void setMatchStatus(MatchStatus matchStatus) {
        this.matchStatus = matchStatus;
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

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "PriceListItem{" +
                "id=" + id +
                ", priceListId=" + priceListId +
                ", manufacturer='" + manufacturer + '\'' +
                ", model='" + model + '\'' +
                ", dppPrice=" + dppPrice +
                ", siInstallerPrice=" + siInstallerPrice +
                ", endUserPrice=" + endUserPrice +
                ", matchStatus=" + matchStatus +
                ", active=" + active +
                '}';
    }
}
