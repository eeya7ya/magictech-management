package com.magictech.modules.sales.entity;

import com.magictech.modules.projects.entity.Project;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Cost breakdown entity for projects
 * Formula: Total = elements_total + (elements_total * tax_rate) - (elements_total * sale_offer_rate) +
 *          installation_cost + licenses_cost + additional_cost
 */
@Entity
@Table(name = "project_cost_breakdowns")
public class ProjectCostBreakdown {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false, unique = true)
    private Long projectId;

    // Elements subtotal (auto-calculated from project elements)
    @Column(name = "elements_subtotal", precision = 15, scale = 2)
    private BigDecimal elementsSubtotal = BigDecimal.ZERO;

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

    // Notes and metadata
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

    // Lifecycle hooks
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        calculateTotals();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        calculateTotals();
    }

    // Business logic
    public void calculateTotals() {
        if (elementsSubtotal == null) elementsSubtotal = BigDecimal.ZERO;
        if (taxRate == null) taxRate = BigDecimal.ZERO;
        if (saleOfferRate == null) saleOfferRate = BigDecimal.ZERO;
        if (installationCost == null) installationCost = BigDecimal.ZERO;
        if (licensesCost == null) licensesCost = BigDecimal.ZERO;
        if (additionalCost == null) additionalCost = BigDecimal.ZERO;

        // Calculate tax amount: elements_subtotal * tax_rate
        this.taxAmount = elementsSubtotal.multiply(taxRate);

        // Calculate discount amount: elements_subtotal * sale_offer_rate
        this.discountAmount = elementsSubtotal.multiply(saleOfferRate);

        // Total = subtotal + tax - discount + installation + licenses + additional
        this.totalCost = elementsSubtotal
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

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public BigDecimal getElementsSubtotal() {
        return elementsSubtotal;
    }

    public void setElementsSubtotal(BigDecimal elementsSubtotal) {
        this.elementsSubtotal = elementsSubtotal;
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

    @Override
    public String toString() {
        return "ProjectCostBreakdown{" +
                "id=" + id +
                ", projectId=" + projectId +
                ", elementsSubtotal=" + elementsSubtotal +
                ", totalCost=" + totalCost +
                '}';
    }
}
