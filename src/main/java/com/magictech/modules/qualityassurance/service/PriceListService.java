package com.magictech.modules.qualityassurance.service;

import com.magictech.modules.qualityassurance.entity.PriceList;
import com.magictech.modules.qualityassurance.entity.PriceListItem;
import com.magictech.modules.qualityassurance.entity.PriceListItem.MatchStatus;
import com.magictech.modules.qualityassurance.repository.PriceListItemRepository;
import com.magictech.modules.qualityassurance.repository.PriceListRepository;
import com.magictech.modules.storage.entity.StorageItem;
import com.magictech.modules.storage.repository.StorageItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing price lists and their items
 */
@Service
@Transactional
public class PriceListService {

    @Autowired
    private PriceListRepository priceListRepository;

    @Autowired
    private PriceListItemRepository priceListItemRepository;

    @Autowired
    private StorageItemRepository storageItemRepository;

    // ========== Price List Operations ==========

    /**
     * Create a new price list
     */
    public PriceList createPriceList(String filename, String uploadedBy) {
        PriceList priceList = new PriceList(filename, uploadedBy);

        // Auto-version if same filename exists
        Integer maxVersion = priceListRepository.getMaxVersionForFilename(filename);
        priceList.setVersion(maxVersion + 1);

        return priceListRepository.save(priceList);
    }

    /**
     * Create a new price list with details
     */
    public PriceList createPriceList(String filename, Long fileSize, String mimeType, String uploadedBy, String description) {
        PriceList priceList = new PriceList(filename, uploadedBy);
        priceList.setFileSize(fileSize);
        priceList.setMimeType(mimeType);
        priceList.setDescription(description);

        // Auto-version if same filename exists
        Integer maxVersion = priceListRepository.getMaxVersionForFilename(filename);
        priceList.setVersion(maxVersion + 1);

        return priceListRepository.save(priceList);
    }

    /**
     * Get all active price lists
     */
    public List<PriceList> getAllPriceLists() {
        return priceListRepository.findByActiveTrueOrderByUploadedAtDesc();
    }

    /**
     * Get the latest active price list
     */
    public Optional<PriceList> getLatestPriceList() {
        return priceListRepository.findFirstByActiveTrueOrderByUploadedAtDesc();
    }

    /**
     * Get price list by ID
     */
    public Optional<PriceList> getPriceListById(Long id) {
        return priceListRepository.findById(id);
    }

    /**
     * Deactivate a price list (soft delete)
     */
    public void deactivatePriceList(Long priceListId) {
        priceListRepository.findById(priceListId).ifPresent(priceList -> {
            priceList.setActive(false);
            priceListRepository.save(priceList);
            // Also deactivate all items
            priceListItemRepository.softDeleteByPriceListId(priceListId);
        });
    }

    // ========== Price List Item Operations ==========

    /**
     * Add an item to a price list
     */
    public PriceListItem addItem(PriceListItem item) {
        return priceListItemRepository.save(item);
    }

    /**
     * Add multiple items to a price list
     */
    public List<PriceListItem> addItems(List<PriceListItem> items) {
        return priceListItemRepository.saveAll(items);
    }

    /**
     * Get all items for a price list
     */
    public List<PriceListItem> getItemsByPriceList(Long priceListId) {
        return priceListItemRepository.findByPriceListIdAndActiveTrueOrderByManufacturerAscModelAsc(priceListId);
    }

    /**
     * Get all items from the latest price list
     */
    public List<PriceListItem> getItemsFromLatestPriceList() {
        return priceListItemRepository.findItemsFromLatestPriceList();
    }

    /**
     * Get items by manufacturer from the latest price list
     */
    public List<PriceListItem> getItemsFromLatestPriceListByManufacturer(String manufacturer) {
        return priceListItemRepository.findItemsFromLatestPriceListByManufacturer(manufacturer);
    }

    /**
     * Get items by manufacturer
     */
    public List<PriceListItem> getItemsByManufacturer(Long priceListId, String manufacturer) {
        return priceListItemRepository.findByPriceListIdAndManufacturerAndActiveTrue(priceListId, manufacturer);
    }

    /**
     * Get all distinct manufacturers from the latest price list
     */
    public List<String> getAllManufacturers() {
        return priceListItemRepository.findAllDistinctManufacturers();
    }

    /**
     * Get distinct manufacturers for a specific price list
     */
    public List<String> getManufacturersByPriceList(Long priceListId) {
        return priceListItemRepository.findDistinctManufacturersByPriceListId(priceListId);
    }

    /**
     * Search items by model or description
     */
    public List<PriceListItem> searchItems(Long priceListId, String searchTerm) {
        return priceListItemRepository.searchItems(priceListId, searchTerm);
    }

    /**
     * Search all items across all price lists
     */
    public List<PriceListItem> searchAllItems(String searchTerm) {
        return priceListItemRepository.searchAllItems(searchTerm);
    }

    /**
     * Get item by ID
     */
    public Optional<PriceListItem> getItemById(Long itemId) {
        return priceListItemRepository.findById(itemId);
    }

    /**
     * Find item by model
     */
    public Optional<PriceListItem> findItemByModel(Long priceListId, String model) {
        return priceListItemRepository.findByPriceListIdAndModelAndActiveTrue(priceListId, model);
    }

    // ========== Model Matching Operations ==========

    /**
     * Check if a model exists in Storage
     */
    public boolean modelExistsInStorage(String model) {
        List<StorageItem> items = storageItemRepository.findByModelAndActiveTrue(model);
        return !items.isEmpty();
    }

    /**
     * Find storage item by model
     */
    public Optional<StorageItem> findStorageItemByModel(String model) {
        List<StorageItem> items = storageItemRepository.findByModelAndActiveTrue(model);
        return items.isEmpty() ? Optional.empty() : Optional.of(items.get(0));
    }

    /**
     * Update match status for all items in a price list
     */
    public void updateMatchStatusForPriceList(Long priceListId) {
        List<PriceListItem> items = priceListItemRepository.findByPriceListIdAndActiveTrue(priceListId);

        for (PriceListItem item : items) {
            updateMatchStatusForItem(item);
        }
    }

    /**
     * Update match status for a single item
     */
    public void updateMatchStatusForItem(PriceListItem item) {
        Optional<StorageItem> storageItem = findStorageItemByModel(item.getModel());

        if (storageItem.isPresent()) {
            item.setMatchStatus(MatchStatus.MATCHED);
            item.setStorageItemId(storageItem.get().getId());
        } else {
            item.setMatchStatus(MatchStatus.NOT_FOUND);
            item.setStorageItemId(null);
        }

        priceListItemRepository.save(item);
    }

    /**
     * Get matched items count
     */
    public long getMatchedItemsCount(Long priceListId) {
        return priceListItemRepository.countByPriceListIdAndMatchStatusAndActiveTrue(priceListId, MatchStatus.MATCHED);
    }

    /**
     * Get not found items count
     */
    public long getNotFoundItemsCount(Long priceListId) {
        return priceListItemRepository.countByPriceListIdAndMatchStatusAndActiveTrue(priceListId, MatchStatus.NOT_FOUND);
    }

    /**
     * Get items count by price list
     */
    public long getItemsCount(Long priceListId) {
        return priceListItemRepository.countByPriceListIdAndActiveTrue(priceListId);
    }

    /**
     * Get items count by manufacturer
     */
    public long getItemsCountByManufacturer(Long priceListId, String manufacturer) {
        return priceListItemRepository.countByPriceListIdAndManufacturerAndActiveTrue(priceListId, manufacturer);
    }

    // ========== Statistics ==========

    /**
     * Get statistics for a price list
     */
    public PriceListStats getStats(Long priceListId) {
        long totalItems = priceListItemRepository.countByPriceListIdAndActiveTrue(priceListId);
        long matchedItems = priceListItemRepository.countByPriceListIdAndMatchStatusAndActiveTrue(priceListId, MatchStatus.MATCHED);
        long notFoundItems = priceListItemRepository.countByPriceListIdAndMatchStatusAndActiveTrue(priceListId, MatchStatus.NOT_FOUND);
        List<String> manufacturers = priceListItemRepository.findDistinctManufacturersByPriceListId(priceListId);

        return new PriceListStats(totalItems, matchedItems, notFoundItems, manufacturers.size());
    }

    /**
     * Statistics holder class
     */
    public static class PriceListStats {
        private final long totalItems;
        private final long matchedItems;
        private final long notFoundItems;
        private final int manufacturersCount;

        public PriceListStats(long totalItems, long matchedItems, long notFoundItems, int manufacturersCount) {
            this.totalItems = totalItems;
            this.matchedItems = matchedItems;
            this.notFoundItems = notFoundItems;
            this.manufacturersCount = manufacturersCount;
        }

        public long getTotalItems() { return totalItems; }
        public long getMatchedItems() { return matchedItems; }
        public long getNotFoundItems() { return notFoundItems; }
        public int getManufacturersCount() { return manufacturersCount; }
        public long getNotCheckedItems() { return totalItems - matchedItems - notFoundItems; }
    }
}
