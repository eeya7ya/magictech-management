package com.magictech.modules.sales.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Sales Order View Model - FIXED VERSION
 * Data transfer object for UI layer
 */
public class SalesOrderViewModel {

    private Long id;
    private String orderType; // "PROJECT" or "CUSTOMER"
    private Long projectId;
    private String projectName;
    private Long customerId;
    private String customerName;
    private BigDecimal subtotal;  // Changed from Double to BigDecimal
    private BigDecimal tax;  // Changed from Double to BigDecimal
    private BigDecimal saleDiscount;  // Changed from Double to BigDecimal
    private BigDecimal crewCost;  // Changed from Double to BigDecimal
    private BigDecimal additionalMaterials;  // Changed from Double to BigDecimal
    private BigDecimal totalAmount;  // Changed from Double to BigDecimal
    private String status; // "DRAFT", "CONFIRMED", "PUSHED_TO_PROJECT", "CANCELLED"
    private String notes;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private List<SalesOrderItemViewModel> items;
    private Integer totalItems;

    // Constructors
    public SalesOrderViewModel() {
    }

    public SalesOrderViewModel(Long id, String orderType, String status,
                               BigDecimal totalAmount, LocalDateTime createdAt) {
        this.id = id;
        this.orderType = orderType;
        this.status = status;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
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

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
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

    public List<SalesOrderItemViewModel> getItems() {
        return items;
    }

    public void setItems(List<SalesOrderItemViewModel> items) {
        this.items = items;
    }

    public Integer getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(Integer totalItems) {
        this.totalItems = totalItems;
    }

    @Override
    public String toString() {
        return "SalesOrderViewModel{" +
                "id=" + id +
                ", orderType='" + orderType + '\'' +
                ", status='" + status + '\'' +
                ", totalAmount=" + totalAmount +
                ", totalItems=" + totalItems +
                '}';
    }
}