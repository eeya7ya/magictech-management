package com.magictech.modules.storage.repository;

import com.magictech.modules.storage.entity.StorageItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Storage Item Repository - UPDATED
 */
@Repository
public interface StorageItemRepository extends JpaRepository<StorageItem, Long> {

    /**
     * Find all active storage items
     */
    List<StorageItem> findByActiveTrue();

    /**
     * Find items by manufacture
     */
    List<StorageItem> findByManufactureAndActiveTrue(String manufacture);

    /**
     * Find items by product name (case-insensitive)
     */
    List<StorageItem> findByProductNameContainingIgnoreCaseAndActiveTrue(String productName);

    /**
     * Find items by code
     */
    List<StorageItem> findByCodeAndActiveTrue(String code);

    /**
     * Find items by serial number
     */
    List<StorageItem> findBySerialNumberAndActiveTrue(String serialNumber);

    /**
     * Search items across multiple fields (NEW FIELDS)
     */
    @Query("SELECT s FROM StorageItem s WHERE s.active = true AND " +
            "(LOWER(s.manufacture) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(s.productName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(s.code) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(s.serialNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<StorageItem> searchItems(@Param("searchTerm") String searchTerm);

    /**
     * Find items with low quantity (for alerts)
     */
    List<StorageItem> findByQuantityLessThanAndActiveTrue(Integer quantity);

    /**
     * Count active items
     */
    long countByActiveTrue();

    /**
     * Count items by manufacture
     */
    long countByManufactureAndActiveTrue(String manufacture);

    /**
     * Find items created by specific user
     */
    List<StorageItem> findByCreatedByAndActiveTrue(String username);

    /**
     * Find items by ID list (for multi-select operations) - ✅ CRITICAL METHOD
     */
    List<StorageItem> findByIdIn(List<Long> ids);

    /**
     * Find all items by IDs (Spring Data JPA built-in) - ✅ ALTERNATIVE
     */
    @Override
    List<StorageItem> findAllById(Iterable<Long> ids);
}