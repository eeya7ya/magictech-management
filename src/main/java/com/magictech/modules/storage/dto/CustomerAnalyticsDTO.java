package com.magictech.modules.storage.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for Customer Analytics
 * Used in Storage Module's Analytics Dashboard
 */
public class CustomerAnalyticsDTO {
    private Long customerId;
    private String customerName;
    private String email;
    private String phone;
    private Integer ordersCount;
    private BigDecimal totalSales;
    private LocalDateTime lastOrderDate;
    private String mostOrderedProduct;

    // Constructors
    public CustomerAnalyticsDTO() {
    }

    public CustomerAnalyticsDTO(Long customerId, String customerName, Integer ordersCount, BigDecimal totalSales) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.ordersCount = ordersCount;
        this.totalSales = totalSales;
    }

    // Getters and Setters
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Integer getOrdersCount() {
        return ordersCount;
    }

    public void setOrdersCount(Integer ordersCount) {
        this.ordersCount = ordersCount;
    }

    public BigDecimal getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(BigDecimal totalSales) {
        this.totalSales = totalSales;
    }

    public LocalDateTime getLastOrderDate() {
        return lastOrderDate;
    }

    public void setLastOrderDate(LocalDateTime lastOrderDate) {
        this.lastOrderDate = lastOrderDate;
    }

    public String getMostOrderedProduct() {
        return mostOrderedProduct;
    }

    public void setMostOrderedProduct(String mostOrderedProduct) {
        this.mostOrderedProduct = mostOrderedProduct;
    }
}
