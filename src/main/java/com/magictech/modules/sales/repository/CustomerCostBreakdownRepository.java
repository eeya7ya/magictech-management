package com.magictech.modules.sales.repository;

import com.magictech.modules.sales.entity.CustomerCostBreakdown;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for CustomerCostBreakdown entity
 */
@Repository
public interface CustomerCostBreakdownRepository extends JpaRepository<CustomerCostBreakdown, Long> {

    /**
     * Find all cost breakdowns for a customer
     */
    List<CustomerCostBreakdown> findByCustomerIdAndActiveTrueOrderByOrderDateDesc(Long customerId);

    /**
     * Find cost breakdown by sales order ID
     */
    Optional<CustomerCostBreakdown> findBySalesOrderIdAndActiveTrue(Long salesOrderId);

    /**
     * Find all active cost breakdowns
     */
    List<CustomerCostBreakdown> findByActiveTrueOrderByOrderDateDesc();

    /**
     * Find cost breakdowns within date range
     */
    @Query("SELECT c FROM CustomerCostBreakdown c WHERE c.active = true AND " +
           "c.orderDate >= :startDate AND c.orderDate <= :endDate " +
           "ORDER BY c.orderDate DESC")
    List<CustomerCostBreakdown> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);

    /**
     * Get total sales for a customer
     */
    @Query("SELECT COALESCE(SUM(c.totalCost), 0) FROM CustomerCostBreakdown c " +
           "WHERE c.customerId = :customerId AND c.active = true")
    Double getTotalSalesForCustomer(@Param("customerId") Long customerId);

    /**
     * Get top customers by revenue
     */
    @Query("SELECT c.customerId, SUM(c.totalCost) as revenue FROM CustomerCostBreakdown c " +
           "WHERE c.active = true " +
           "GROUP BY c.customerId " +
           "ORDER BY revenue DESC")
    List<Object[]> getTopCustomersByRevenue();

    /**
     * Count orders for a customer
     */
    long countByCustomerIdAndActiveTrue(Long customerId);
}
