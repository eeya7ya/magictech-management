package com.magictech.modules.sales.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Sales Order Entity - FIXED WITH BIGDECIMAL
 * Represents an order linked to either a Project or Customer
 */
@Entity
@Table(name = "sales_orders")
public class SalesOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Order type is required")
    @Column(name = "order_type", nullable = false, length = 20)
    private String orderType; // "PROJECT" or "CUSTOMER"

    @Column(name = "project_id")
    private Long projectId; // Link to project if orderType = "PROJECT"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer; // Link to customer if orderType = "CUSTOMER"

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal tax = BigDecimal.ZERO;

    @Column(name = "sale_discount", precision = 15, scale = 2)
    private BigDecimal saleDiscount = BigDecimal.ZERO;

    @Column(name = "crew_cost", precision = 15, scale = 2)
    private BigDecimal crewCost = BigDecimal.ZERO;

    @Column(name = "additional_materials", precision = 15, scale = 2)
    private BigDecimal additionalMaterials = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 30)
    private String status = "DRAFT"; // DRAFT, CONFIRMED, PUSHED_TO_PROJECT, CANCELLED

    @Column(length = 2000)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(nullable = false)
    private Boolean active = true;

    @OneToMany(mappedBy = "salesOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SalesOrderItem> items = new ArrayList<>();

    // Constructors
    public SalesOrder() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.active = true;
        this.status = "DRAFT";
        this.subtotal = BigDecimal.ZERO;
        this.tax = BigDecimal.ZERO;
        this.saleDiscount = BigDecimal.ZERO;
        this.crewCost = BigDecimal.ZERO;
        this.additionalMaterials = BigDecimal.ZERO;
        this.totalAmount = BigDecimal.ZERO;
    }

    public SalesOrder(String orderType) {
        this();
        this.orderType = orderType;
    }

    // Business Logic Methods

    /**
     * Calculate all totals automatically
     * Formula: totalAmount = subtotal + tax - saleDiscount + crewCost + additionalMaterials
     */
    public void calculateTotals() {
        // Calculate subtotal from items
        this.subtotal = items.stream()
                .filter(item -> item.getActive())
                .map(SalesOrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate total amount
        this.totalAmount = this.subtotal
                .add(this.tax != null ? this.tax : BigDecimal.ZERO)
                .subtract(this.saleDiscount != null ? this.saleDiscount : BigDecimal.ZERO)
                .add(this.crewCost != null ? this.crewCost : BigDecimal.ZERO)
                .add(this.additionalMaterials != null ? this.additionalMaterials : BigDecimal.ZERO);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public void setTax(BigDecimal tax) {
        this.tax = tax;
    }

    public BigDecimal getSaleDiscount() {
        return saleDiscount;
    }

    public void setSaleDiscount(BigDecimal saleDiscount) {
        this.saleDiscount = saleDiscount;
    }

    public BigDecimal getCrewCost() {
        return crewCost;
    }

    public void setCrewCost(BigDecimal crewCost) {
        this.crewCost = crewCost;
    }

    public BigDecimal getAdditionalMaterials() {
        return additionalMaterials;
    }

    public void setAdditionalMaterials(BigDecimal additionalMaterials) {
        this.additionalMaterials = additionalMaterials;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
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

    public List<SalesOrderItem> getItems() {
        return items;
    }

    public void setItems(List<SalesOrderItem> items) {
        this.items = items;
    }

    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.active == null) {
            this.active = true;
        }
        if (this.status == null) {
            this.status = "DRAFT";
        }
        calculateTotals();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        calculateTotals();
    }

    @Override
    public String toString() {
        return "SalesOrder{" +
                "id=" + id +
                ", orderType='" + orderType + '\'' +
                ", status='" + status + '\'' +
                ", totalAmount=" + totalAmount +
                '}';
    }
}