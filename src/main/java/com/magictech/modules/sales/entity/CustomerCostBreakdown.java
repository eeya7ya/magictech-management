package com.magictech.modules.sales.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Cost breakdown entity for customer orders (non-project sales)
 * Formula: Total = items_total + (items_total * tax_rate) - (items_total * sale_offer_rate) +
 *          installation_cost + licenses_cost + additional_cost
 */
@Entity
@Table(name = "customer_cost_breakdowns")
public class CustomerCostBreakdown {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "sales_order_id", unique = true)
    private Long salesOrderId; // Link to sales order if applicable

    // Items subtotal (from order items)
    @Column(name = "items_subtotal", precision = 15, scale = 2)
    private BigDecimal itemsSubtotal = BigDecimal.ZERO;

    // Percentage rates (0.00 to 1.00, where 0.15 = 15%)
    @Column(name = "tax_rate", precision = 5, scale = 4)
    private BigDecimal taxRate = BigDecimal.ZERO;

    @Column(name = "sale_offer_rate", precision = 5, scale = 4)
    private BigDecimal saleOfferRate = BigDecimal.ZERO;

    // Fixed costs
    @Column(name = "installation_cost", precision = 15, scale = 2)
    private BigDecimal installationCost = BigDecimal.ZERO;

    @Column(name = "licenses_cost", precision = 15, scale = 2)
    private BigDecimal licensesCost = BigDecimal.ZERO;

    @Column(name = "additional_cost", precision = 15, scale = 2)
    private BigDecimal additionalCost = BigDecimal.ZERO;

    // Calculated amounts
    @Column(name = "tax_amount", precision = 15, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 15, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_cost", precision = 15, scale = 2)
    private BigDecimal totalCost = BigDecimal.ZERO;

    // Order metadata
    @Column(name = "order_date")
    private LocalDateTime orderDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    // Lifecycle hooks
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.orderDate == null) this.orderDate = LocalDateTime.now();
        if (this.active == null) this.active = true;
        calculateTotals();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        calculateTotals();
    }

    // Business logic
    public void calculateTotals() {
        if (itemsSubtotal == null) itemsSubtotal = BigDecimal.ZERO;
        if (taxRate == null) taxRate = BigDecimal.ZERO;
        if (saleOfferRate == null) saleOfferRate = BigDecimal.ZERO;
        if (installationCost == null) installationCost = BigDecimal.ZERO;
        if (licensesCost == null) licensesCost = BigDecimal.ZERO;
        if (additionalCost == null) additionalCost = BigDecimal.ZERO;

        // Calculate tax amount: items_subtotal * tax_rate
        this.taxAmount = itemsSubtotal.multiply(taxRate);

        // Calculate discount amount: items_subtotal * sale_offer_rate
        this.discountAmount = itemsSubtotal.multiply(saleOfferRate);

        // Total = subtotal + tax - discount + installation + licenses + additional
        this.totalCost = itemsSubtotal
                .add(taxAmount)
                .subtract(discountAmount)
                .add(installationCost)
                .add(licensesCost)
                .add(additionalCost);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getSalesOrderId() {
        return salesOrderId;
    }

    public void setSalesOrderId(Long salesOrderId) {
        this.salesOrderId = salesOrderId;
    }

    public BigDecimal getItemsSubtotal() {
        return itemsSubtotal;
    }

    public void setItemsSubtotal(BigDecimal itemsSubtotal) {
        this.itemsSubtotal = itemsSubtotal;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public BigDecimal getSaleOfferRate() {
        return saleOfferRate;
    }

    public void setSaleOfferRate(BigDecimal saleOfferRate) {
        this.saleOfferRate = saleOfferRate;
    }

    public BigDecimal getInstallationCost() {
        return installationCost;
    }

    public void setInstallationCost(BigDecimal installationCost) {
        this.installationCost = installationCost;
    }

    public BigDecimal getLicensesCost() {
        return licensesCost;
    }

    public void setLicensesCost(BigDecimal licensesCost) {
        this.licensesCost = licensesCost;
    }

    public BigDecimal getAdditionalCost() {
        return additionalCost;
    }

    public void setAdditionalCost(BigDecimal additionalCost) {
        this.additionalCost = additionalCost;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "CustomerCostBreakdown{" +
                "id=" + id +
                ", customerId=" + customerId +
                ", itemsSubtotal=" + itemsSubtotal +
                ", totalCost=" + totalCost +
                ", orderDate=" + orderDate +
                '}';
    }
}
