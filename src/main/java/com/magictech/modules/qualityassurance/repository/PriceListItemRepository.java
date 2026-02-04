package com.magictech.modules.qualityassurance.repository;

import com.magictech.modules.qualityassurance.entity.PriceListItem;
import com.magictech.modules.qualityassurance.entity.PriceListItem.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for PriceListItem entity
 */
@Repository
public interface PriceListItemRepository extends JpaRepository<PriceListItem, Long> {

    /**
     * Find all active items
     */
    List<PriceListItem> findByActiveTrue();

    /**
     * Find all items for a specific price list
     */
    List<PriceListItem> findByPriceListIdAndActiveTrue(Long priceListId);

    /**
     * Find all items for a specific price list ordered by manufacturer and model
     */
    List<PriceListItem> findByPriceListIdAndActiveTrueOrderByManufacturerAscModelAsc(Long priceListId);

    /**
     * Find items by manufacturer
     */
    List<PriceListItem> findByPriceListIdAndManufacturerAndActiveTrue(Long priceListId, String manufacturer);

    /**
     * Find item by model
     */
    Optional<PriceListItem> findByPriceListIdAndModelAndActiveTrue(Long priceListId, String model);

    /**
     * Find items by model (case-insensitive)
     */
    @Query("SELECT p FROM PriceListItem p WHERE p.priceListId = :priceListId AND p.active = true " +
           "AND LOWER(p.model) = LOWER(:model)")
    List<PriceListItem> findByModelIgnoreCase(@Param("priceListId") Long priceListId, @Param("model") String model);

    /**
     * Get all distinct manufacturers from a price list
     */
    @Query("SELECT DISTINCT p.manufacturer FROM PriceListItem p WHERE p.priceListId = :priceListId AND p.active = true ORDER BY p.manufacturer")
    List<String> findDistinctManufacturersByPriceListId(@Param("priceListId") Long priceListId);

    /**
     * Get all distinct manufacturers from active price list items
     */
    @Query("SELECT DISTINCT p.manufacturer FROM PriceListItem p WHERE p.active = true ORDER BY p.manufacturer")
    List<String> findAllDistinctManufacturers();

    /**
     * Search items by model or description
     */
    @Query("SELECT p FROM PriceListItem p WHERE p.priceListId = :priceListId AND p.active = true " +
           "AND (LOWER(p.model) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<PriceListItem> searchItems(@Param("priceListId") Long priceListId, @Param("searchTerm") String searchTerm);

    /**
     * Search items by model or description across all active price lists
     */
    @Query("SELECT p FROM PriceListItem p WHERE p.active = true " +
           "AND (LOWER(p.model) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<PriceListItem> searchAllItems(@Param("searchTerm") String searchTerm);

    /**
     * Find items by match status
     */
    List<PriceListItem> findByPriceListIdAndMatchStatusAndActiveTrue(Long priceListId, MatchStatus matchStatus);

    /**
     * Find item by storage item ID
     */
    List<PriceListItem> findByStorageItemIdAndActiveTrue(Long storageItemId);

    /**
     * Update match status for items linked to a storage item
     */
    @Modifying
    @Query("UPDATE PriceListItem p SET p.matchStatus = :status, p.storageItemId = :storageItemId " +
           "WHERE p.model = :model AND p.active = true")
    int updateMatchStatusByModel(@Param("model") String model, @Param("status") MatchStatus status,
                                  @Param("storageItemId") Long storageItemId);

    /**
     * Count items by price list
     */
    long countByPriceListIdAndActiveTrue(Long priceListId);

    /**
     * Count items by manufacturer
     */
    long countByPriceListIdAndManufacturerAndActiveTrue(Long priceListId, String manufacturer);

    /**
     * Count matched items
     */
    long countByPriceListIdAndMatchStatusAndActiveTrue(Long priceListId, MatchStatus matchStatus);

    /**
     * Delete all items for a price list (soft delete)
     */
    @Modifying
    @Query("UPDATE PriceListItem p SET p.active = false WHERE p.priceListId = :priceListId")
    int softDeleteByPriceListId(@Param("priceListId") Long priceListId);

    /**
     * Find items from the latest active price list
     */
    @Query("SELECT p FROM PriceListItem p WHERE p.priceListId = " +
           "(SELECT MAX(pl.id) FROM PriceList pl WHERE pl.active = true) " +
           "AND p.active = true ORDER BY p.manufacturer, p.model")
    List<PriceListItem> findItemsFromLatestPriceList();

    /**
     * Find items from the latest active price list by manufacturer
     */
    @Query("SELECT p FROM PriceListItem p WHERE p.priceListId = " +
           "(SELECT MAX(pl.id) FROM PriceList pl WHERE pl.active = true) " +
           "AND p.manufacturer = :manufacturer AND p.active = true ORDER BY p.model")
    List<PriceListItem> findItemsFromLatestPriceListByManufacturer(@Param("manufacturer") String manufacturer);
}
