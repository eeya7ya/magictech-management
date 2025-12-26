package com.magictech.modules.storage.repository;

import com.magictech.modules.storage.entity.StorageItemLocation;
import com.magictech.modules.storage.entity.StorageLocation;
import com.magictech.modules.storage.entity.StorageItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for StorageItemLocation junction entity
 */
@Repository
public interface StorageItemLocationRepository extends JpaRepository<StorageItemLocation, Long> {

    // Find all items in a specific location
    List<StorageItemLocation> findByStorageLocationAndActiveTrue(StorageLocation location);

    // Find all items in a specific location by location ID
    @Query("SELECT sil FROM StorageItemLocation sil WHERE sil.storageLocation.id = :locationId AND sil.active = true")
    List<StorageItemLocation> findByLocationId(@Param("locationId") Long locationId);

    // Find all locations for a specific item
    List<StorageItemLocation> findByStorageItemAndActiveTrue(StorageItem item);

    // Find all locations for a specific item by item ID
    @Query("SELECT sil FROM StorageItemLocation sil WHERE sil.storageItem.id = :itemId AND sil.active = true")
    List<StorageItemLocation> findByItemId(@Param("itemId") Long itemId);

    // Find specific item in specific location
    Optional<StorageItemLocation> findByStorageItemAndStorageLocationAndActiveTrue(
            StorageItem item, StorageLocation location);

    // Find by item ID and location ID
    @Query("SELECT sil FROM StorageItemLocation sil WHERE sil.storageItem.id = :itemId AND sil.storageLocation.id = :locationId AND sil.active = true")
    Optional<StorageItemLocation> findByItemIdAndLocationId(
            @Param("itemId") Long itemId,
            @Param("locationId") Long locationId);

    // Count items in a location
    @Query("SELECT COUNT(sil) FROM StorageItemLocation sil WHERE sil.storageLocation.id = :locationId AND sil.active = true")
    long countItemsInLocation(@Param("locationId") Long locationId);

    // Sum total quantity in a location
    @Query("SELECT COALESCE(SUM(sil.quantity), 0) FROM StorageItemLocation sil WHERE sil.storageLocation.id = :locationId AND sil.active = true")
    int sumQuantityInLocation(@Param("locationId") Long locationId);

    // Find low stock items in a location
    @Query("SELECT sil FROM StorageItemLocation sil WHERE sil.storageLocation.id = :locationId AND sil.active = true AND sil.minStockLevel IS NOT NULL AND sil.quantity <= sil.minStockLevel")
    List<StorageItemLocation> findLowStockInLocation(@Param("locationId") Long locationId);

    // Find all active records (for total view)
    List<StorageItemLocation> findByActiveTrue();

    // Search items within a location
    @Query("SELECT sil FROM StorageItemLocation sil WHERE sil.storageLocation.id = :locationId AND sil.active = true AND " +
           "(LOWER(sil.storageItem.productName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(sil.storageItem.manufacture) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(sil.storageItem.code) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<StorageItemLocation> searchInLocation(
            @Param("locationId") Long locationId,
            @Param("searchTerm") String searchTerm);

    // Get total quantity of an item across all locations
    @Query("SELECT COALESCE(SUM(sil.quantity), 0) FROM StorageItemLocation sil WHERE sil.storageItem.id = :itemId AND sil.active = true")
    int getTotalQuantityForItem(@Param("itemId") Long itemId);

    // Get location summary (count and quantity per location)
    @Query("SELECT sil.storageLocation.id, sil.storageLocation.name, COUNT(sil), SUM(sil.quantity) " +
           "FROM StorageItemLocation sil WHERE sil.active = true " +
           "GROUP BY sil.storageLocation.id, sil.storageLocation.name")
    List<Object[]> getLocationSummary();

    // Delete all records for a location (soft delete)
    @Modifying
    @Query("UPDATE StorageItemLocation sil SET sil.active = false WHERE sil.storageLocation.id = :locationId")
    void softDeleteByLocationId(@Param("locationId") Long locationId);

    // Delete all records for an item (soft delete)
    @Modifying
    @Query("UPDATE StorageItemLocation sil SET sil.active = false WHERE sil.storageItem.id = :itemId")
    void softDeleteByItemId(@Param("itemId") Long itemId);
}
