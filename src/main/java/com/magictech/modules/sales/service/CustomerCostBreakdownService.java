package com.magictech.modules.sales.service;

import com.magictech.modules.sales.entity.CustomerCostBreakdown;
import com.magictech.modules.sales.repository.CustomerCostBreakdownRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing customer cost breakdowns
 */
@Service
@Transactional
public class CustomerCostBreakdownService {

    @Autowired
    private CustomerCostBreakdownRepository breakdownRepository;

    /**
     * Create a new cost breakdown for customer
     */
    public CustomerCostBreakdown createBreakdown(CustomerCostBreakdown breakdown, String createdBy) {
        breakdown.setCreatedBy(createdBy);
        return breakdownRepository.save(breakdown);
    }

    /**
     * Update cost breakdown
     */
    public CustomerCostBreakdown updateBreakdown(Long id, CustomerCostBreakdown updated, String updatedBy) {
        Optional<CustomerCostBreakdown> existing = breakdownRepository.findById(id);
        if (existing.isEmpty()) {
            throw new RuntimeException("Cost breakdown not found: " + id);
        }

        CustomerCostBreakdown breakdown = existing.get();
        breakdown.setItemsSubtotal(updated.getItemsSubtotal());
        breakdown.setTaxRate(updated.getTaxRate());
        breakdown.setSaleOfferRate(updated.getSaleOfferRate());
        breakdown.setInstallationCost(updated.getInstallationCost());
        breakdown.setLicensesCost(updated.getLicensesCost());
        breakdown.setAdditionalCost(updated.getAdditionalCost());
        breakdown.setNotes(updated.getNotes());
        breakdown.setUpdatedBy(updatedBy);

        return breakdownRepository.save(breakdown);
    }

    /**
     * Get all cost breakdowns for a customer
     */
    public List<CustomerCostBreakdown> getBreakdownsByCustomer(Long customerId) {
        return breakdownRepository.findByCustomerIdAndActiveTrueOrderByOrderDateDesc(customerId);
    }

    /**
     * Get cost breakdown by sales order
     */
    public Optional<CustomerCostBreakdown> getBreakdownBySalesOrder(Long salesOrderId) {
        return breakdownRepository.findBySalesOrderIdAndActiveTrue(salesOrderId);
    }

    /**
     * Get all active cost breakdowns
     */
    public List<CustomerCostBreakdown> getAllBreakdowns() {
        return breakdownRepository.findByActiveTrueOrderByOrderDateDesc();
    }

    /**
     * Get cost breakdowns within date range
     */
    public List<CustomerCostBreakdown> getBreakdownsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return breakdownRepository.findByDateRange(startDate, endDate);
    }

    /**
     * Get total sales for customer
     */
    public Double getTotalSalesForCustomer(Long customerId) {
        return breakdownRepository.getTotalSalesForCustomer(customerId);
    }

    /**
     * Get top customers by revenue
     */
    public List<Object[]> getTopCustomersByRevenue() {
        return breakdownRepository.getTopCustomersByRevenue();
    }

    /**
     * Get order count for customer
     */
    public long getOrderCountForCustomer(Long customerId) {
        return breakdownRepository.countByCustomerIdAndActiveTrue(customerId);
    }

    /**
     * Soft delete cost breakdown
     */
    public void deleteBreakdown(Long id, String deletedBy) {
        Optional<CustomerCostBreakdown> breakdown = breakdownRepository.findById(id);
        if (breakdown.isPresent()) {
            CustomerCostBreakdown entity = breakdown.get();
            entity.setActive(false);
            entity.setUpdatedBy(deletedBy);
            breakdownRepository.save(entity);
        }
    }

    /**
     * Hard delete cost breakdown
     */
    public void hardDeleteBreakdown(Long id) {
        breakdownRepository.deleteById(id);
    }
}
