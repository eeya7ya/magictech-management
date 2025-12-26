package com.magictech.modules.storage.service;

import com.magictech.modules.storage.entity.StorageItem;
import com.magictech.modules.storage.entity.StorageLocation;
import com.magictech.modules.storage.entity.StorageItemLocation;
import com.magictech.modules.storage.repository.StorageItemRepository;
import com.magictech.modules.storage.repository.StorageLocationRepository;
import com.magictech.modules.storage.repository.StorageItemLocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing storage item locations (junction table operations)
 */
@Service
@Transactional
public class StorageItemLocationService {

    @Autowired
    private StorageItemLocationRepository itemLocationRepository;

    @Autowired
    private StorageItemRepository itemRepository;

    @Autowired
    private StorageLocationRepository locationRepository;

    // ==================== CRUD Operations ====================

    /**
     * Add an item to a location with specified quantity
     */
    public StorageItemLocation addItemToLocation(Long itemId, Long locationId, Integer quantity, String createdBy) {
        StorageItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Storage item not found: " + itemId));

        StorageLocation location = locationRepository.findById(locationId)
                .orElseThrow(() -> new RuntimeException("Storage location not found: " + locationId));

        // Check if relationship already exists
        Optional<StorageItemLocation> existing = itemLocationRepository
                .findByItemIdAndLocationId(itemId, locationId);

        if (existing.isPresent()) {
            // Update existing quantity
            StorageItemLocation sil = existing.get();
            sil.setQuantity(sil.getQuantity() + quantity);
            return itemLocationRepository.save(sil);
        }

        // Create new relationship
        StorageItemLocation sil = new StorageItemLocation(item, location, quantity);
        sil.setCreatedBy(createdBy);
        return itemLocationRepository.save(sil);
    }

    /**
     * Set exact quantity for an item in a location
     */
    public StorageItemLocation setItemQuantityInLocation(Long itemId, Long locationId, Integer quantity, String updatedBy) {
        Optional<StorageItemLocation> existing = itemLocationRepository.findByItemIdAndLocationId(itemId, locationId);

        if (existing.isPresent()) {
            StorageItemLocation sil = existing.get();
            sil.setQuantity(quantity);
            return itemLocationRepository.save(sil);
        } else {
            return addItemToLocation(itemId, locationId, quantity, updatedBy);
        }
    }

    /**
     * Update item location details
     */
    public StorageItemLocation updateItemLocation(Long id, StorageItemLocation updated) {
        StorageItemLocation existing = itemLocationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item location not found: " + id));

        existing.setQuantity(updated.getQuantity());
        existing.setBinLocation(updated.getBinLocation());
        existing.setShelfNumber(updated.getShelfNumber());
        existing.setRowNumber(updated.getRowNumber());
        existing.setMinStockLevel(updated.getMinStockLevel());
        existing.setMaxStockLevel(updated.getMaxStockLevel());
        existing.setNotes(updated.getNotes());

        return itemLocationRepository.save(existing);
    }

    /**
     * Remove item from location (soft delete)
     */
    public void removeItemFromLocation(Long itemId, Long locationId) {
        Optional<StorageItemLocation> existing = itemLocationRepository.findByItemIdAndLocationId(itemId, locationId);
        existing.ifPresent(sil -> {
            sil.setActive(false);
            itemLocationRepository.save(sil);
        });
    }

    /**
     * Hard delete item from location
     */
    public void hardRemoveItemFromLocation(Long id) {
        itemLocationRepository.deleteById(id);
    }

    // ==================== Transfer Operations ====================

    /**
     * Transfer quantity from one location to another
     */
    public void transferItem(Long itemId, Long fromLocationId, Long toLocationId, Integer quantity, String transferredBy) {
        // Get source
        StorageItemLocation source = itemLocationRepository.findByItemIdAndLocationId(itemId, fromLocationId)
                .orElseThrow(() -> new RuntimeException("Item not found in source location"));

        if (source.getQuantity() < quantity) {
            throw new RuntimeException("Insufficient quantity in source location. Available: " + source.getQuantity());
        }

        // Reduce from source
        source.setQuantity(source.getQuantity() - quantity);
        itemLocationRepository.save(source);

        // Add to destination
        addItemToLocation(itemId, toLocationId, quantity, transferredBy);
    }

    // ==================== Read Operations ====================

    /**
     * Get all items in a specific location
     */
    public List<StorageItemLocation> getItemsInLocation(Long locationId) {
        return itemLocationRepository.findByLocationId(locationId);
    }

    /**
     * Get all locations for a specific item
     */
    public List<StorageItemLocation> getLocationsForItem(Long itemId) {
        return itemLocationRepository.findByItemId(itemId);
    }

    /**
     * Get all item-location records
     */
    public List<StorageItemLocation> getAllItemLocations() {
        return itemLocationRepository.findByActiveTrue();
    }

    /**
     * Search items within a location
     */
    public List<StorageItemLocation> searchInLocation(Long locationId, String searchTerm) {
        return itemLocationRepository.searchInLocation(locationId, searchTerm);
    }

    /**
     * Get low stock items in a location
     */
    public List<StorageItemLocation> getLowStockInLocation(Long locationId) {
        return itemLocationRepository.findLowStockInLocation(locationId);
    }

    /**
     * Get total quantity of an item across all locations
     */
    public int getTotalQuantityForItem(Long itemId) {
        return itemLocationRepository.getTotalQuantityForItem(itemId);
    }

    /**
     * Find by ID
     */
    public Optional<StorageItemLocation> findById(Long id) {
        return itemLocationRepository.findById(id);
    }

    // ==================== Inventory Operations ====================

    /**
     * Record inventory check
     */
    public StorageItemLocation recordInventoryCheck(Long id, Integer actualQuantity, String checkedBy) {
        StorageItemLocation sil = itemLocationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item location not found: " + id));

        sil.setQuantity(actualQuantity);
        sil.setLastInventoryCheck(LocalDateTime.now());
        sil.setLastInventoryBy(checkedBy);

        return itemLocationRepository.save(sil);
    }

    // ==================== DTO for View ====================

    /**
     * DTO for displaying items in location with full details
     */
    public static class ItemInLocationDTO {
        private Long itemLocationId;
        private Long itemId;
        private String manufacture;
        private String productName;
        private String code;
        private String serialNumber;
        private Integer quantity;
        private String binLocation;
        private String locationName;
        private Long locationId;
        private boolean lowStock;

        public ItemInLocationDTO(StorageItemLocation sil) {
            this.itemLocationId = sil.getId();
            this.itemId = sil.getStorageItem().getId();
            this.manufacture = sil.getStorageItem().getManufacture();
            this.productName = sil.getStorageItem().getProductName();
            this.code = sil.getStorageItem().getCode();
            this.serialNumber = sil.getStorageItem().getSerialNumber();
            this.quantity = sil.getQuantity();
            this.binLocation = sil.getFullBinLocation();
            this.locationName = sil.getStorageLocation().getName();
            this.locationId = sil.getStorageLocation().getId();
            this.lowStock = sil.isLowStock();
        }

        // Getters
        public Long getItemLocationId() { return itemLocationId; }
        public Long getItemId() { return itemId; }
        public String getManufacture() { return manufacture; }
        public String getProductName() { return productName; }
        public String getCode() { return code; }
        public String getSerialNumber() { return serialNumber; }
        public Integer getQuantity() { return quantity; }
        public String getBinLocation() { return binLocation; }
        public String getLocationName() { return locationName; }
        public Long getLocationId() { return locationId; }
        public boolean isLowStock() { return lowStock; }
    }

    /**
     * Get all items in a location as DTOs
     */
    public List<ItemInLocationDTO> getItemDTOsInLocation(Long locationId) {
        return getItemsInLocation(locationId).stream()
                .map(ItemInLocationDTO::new)
                .toList();
    }

    /**
     * Get all items across all locations as DTOs (for Total view)
     */
    public List<ItemInLocationDTO> getAllItemDTOs() {
        return getAllItemLocations().stream()
                .map(ItemInLocationDTO::new)
                .toList();
    }
}
