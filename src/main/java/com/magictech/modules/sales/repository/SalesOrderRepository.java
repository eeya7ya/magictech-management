package com.magictech.modules.sales.repository;

import com.magictech.modules.sales.entity.SalesOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Sales Order Repository
 * Database access layer for SalesOrder entity
 */
@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {

    /**
     * Find order by ID (active only)
     */
    Optional<SalesOrder> findByIdAndActiveTrue(Long id);

    /**
     * Find all active orders
     */
    List<SalesOrder> findByActiveTrue();

    /**
     * Find orders by type
     */
    List<SalesOrder> findByOrderTypeAndActiveTrue(String orderType);

    /**
     * Find orders by status
     */
    List<SalesOrder> findByStatusAndActiveTrue(String status);

    /**
     * Find orders by customer ID
     */
    List<SalesOrder> findByCustomerIdAndActiveTrue(Long customerId);

    /**
     * Find orders by project ID
     */
    List<SalesOrder> findByProjectIdAndActiveTrue(Long projectId);

    /**
     * Count total active orders
     */
    long countByActiveTrue();
}