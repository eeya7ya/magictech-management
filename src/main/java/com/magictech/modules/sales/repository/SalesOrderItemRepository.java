package com.magictech.modules.sales.repository;

import com.magictech.modules.sales.entity.SalesOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Sales Order Item Repository
 * Database access layer for SalesOrderItem entity
 */
@Repository
public interface SalesOrderItemRepository extends JpaRepository<SalesOrderItem, Long> {

    /**
     * Find all items for a sales order
     */
    List<SalesOrderItem> findBySalesOrderIdAndActiveTrue(Long salesOrderId);

    /**
     * Find items by storage item ID
     */
    List<SalesOrderItem> findByStorageItemIdAndActiveTrue(Long storageItemId);

    /**
     * Count items in an order
     */
    long countBySalesOrderIdAndActiveTrue(Long salesOrderId);

    /**
     * Delete all items for a sales order (soft delete)
     */
    void deleteBySalesOrderId(Long salesOrderId);
}