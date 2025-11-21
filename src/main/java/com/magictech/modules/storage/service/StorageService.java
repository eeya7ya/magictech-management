package com.magictech.modules.storage.service;

import com.magictech.modules.storage.entity.StorageItem;
import com.magictech.modules.storage.repository.StorageItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Storage Service - FIXED DELETE BUG
 * Business logic layer for storage operations
 */
@Service
@Transactional
public class StorageService {

    private static final Logger logger = LoggerFactory.getLogger(StorageService.class);

    @Autowired
    private StorageItemRepository repository;

    /**
     * Get all active storage items ONLY
     * ✅ CRITICAL: Must use findByActiveTrue() to exclude soft-deleted items
     */
    public List<StorageItem> getAllItems() {
        List<StorageItem> activeItems = repository.findByActiveTrue();
        System.out.println("📊 Loading items: Found " + activeItems.size() + " active items");
        return activeItems;
    }

    /**
     * Alias method for getAllItems - for compatibility
     */
    public List<StorageItem> findAllActive() {
        return getAllItems();
    }

    /**
     * Get ALL items including deleted (for admin/debugging)
     */
    public List<StorageItem> getAllItemsIncludingDeleted() {
        return repository.findAll();
    }

    /**
     * Get item by ID
     */
    public Optional<StorageItem> getItemById(Long id) {
        return repository.findById(id);
    }

    /**
     * Get item by ID (alias for getItemById)
     */
    public Optional<StorageItem> findById(Long id) {
        return getItemById(id);
    }

    /**
     * Create new storage item
     */
    public StorageItem createItem(StorageItem item) {
        if (item.getDateAdded() == null) {
            item.setDateAdded(LocalDateTime.now());
        }
        item.setActive(true);
        return repository.save(item);
    }

    /**
     * Update existing storage item
     */
    public StorageItem updateItem(Long id, StorageItem updatedItem) {
        Optional<StorageItem> existingItem = repository.findById(id);

        if (existingItem.isPresent()) {
            StorageItem item = existingItem.get();
            item.setManufacture(updatedItem.getManufacture());
            item.setProductName(updatedItem.getProductName());
            item.setCode(updatedItem.getCode());
            item.setSerialNumber(updatedItem.getSerialNumber());
            item.setQuantity(updatedItem.getQuantity());
            item.setPrice(updatedItem.getPrice());
            item.setLastUpdated(LocalDateTime.now());

            return repository.save(item);
        }

        throw new RuntimeException("Storage item not found with id: " + id);
    }

    /**
     * Delete single item - PERMANENT DELETE (removes from database)
     */
    @Transactional
    public void deleteItem(Long id) {
        System.out.println("🗑️ PERMANENT DELETE - Removing item ID: " + id);
        repository.deleteById(id);
        repository.flush(); // Force commit
        System.out.println("✓ Item ID " + id + " permanently deleted from database");
    }

    /**
     * Delete multiple items - PERMANENT DELETE (removes from database)
     */
    @Transactional
    public void deleteItems(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            System.out.println("⚠️ No IDs provided for deletion");
            return;
        }

        System.out.println("🗑️ PERMANENT DELETE - Removing " + ids.size() + " items: " + ids);

        // Delete all by IDs
        repository.deleteAllById(ids);
        repository.flush(); // Force commit

        System.out.println("✓ Successfully PERMANENTLY deleted " + ids.size() + " items from database");
    }

    /**
     * Permanently delete item from database (HARD DELETE - USE WITH CAUTION)
     * ⚠️ NOTE: This is now the DEFAULT delete behavior
     */
    @Transactional
    public void permanentlyDeleteItem(Long id) {
        deleteItem(id); // Use the main delete method
    }

    /**
     * Permanently delete multiple items (HARD DELETE - USE WITH CAUTION)
     * ⚠️ NOTE: This is now the DEFAULT delete behavior
     */
    @Transactional
    public void permanentlyDeleteItems(List<Long> ids) {
        deleteItems(ids); // Use the main delete method
    }

    /**
     * Search items across multiple fields
     */
    public List<StorageItem> searchItems(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllItems();
        }
        return repository.searchItems(searchTerm.trim());
    }

    /**
     * Get items by manufacture
     */
    public List<StorageItem> getItemsByManufacture(String manufacture) {
        return repository.findByManufactureAndActiveTrue(manufacture);
    }

    /**
     * Get low stock items (quantity < threshold)
     */
    public List<StorageItem> getLowStockItems(Integer threshold) {
        return repository.findByQuantityLessThanAndActiveTrue(threshold);
    }

    /**
     * Update item quantity
     */
    @Transactional
    public StorageItem updateQuantity(Long id, Integer newQuantity) {
        Optional<StorageItem> item = repository.findById(id);

        if (item.isPresent()) {
            StorageItem storageItem = item.get();
            storageItem.setQuantity(newQuantity);
            storageItem.setLastUpdated(LocalDateTime.now());
            return repository.save(storageItem);
        }

        throw new RuntimeException("Storage item not found with id: " + id);
    }

    /**
     * Deduct quantity from storage item (used when approving project elements)
     */
    @Transactional
    public StorageItem deductQuantity(Long id, Integer quantityToDeduct) {
        StorageItem item = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Storage item not found with id: " + id));

        int currentQuantity = item.getQuantity() != null ? item.getQuantity() : 0;
        int newQuantity = Math.max(0, currentQuantity - quantityToDeduct);

        item.setQuantity(newQuantity);
        item.setLastUpdated(LocalDateTime.now());

        logger.info("Deducted {} units from storage item {} (was: {}, now: {})",
                quantityToDeduct, id, currentQuantity, newQuantity);

        return repository.save(item);
    }

    /**
     * Add quantity back to storage item (used when rejecting project elements)
     */
    @Transactional
    public StorageItem addQuantity(Long id, Integer quantityToAdd) {
        StorageItem item = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Storage item not found with id: " + id));

        int currentQuantity = item.getQuantity() != null ? item.getQuantity() : 0;
        int newQuantity = currentQuantity + quantityToAdd;

        item.setQuantity(newQuantity);
        item.setLastUpdated(LocalDateTime.now());

        logger.info("Added {} units to storage item {} (was: {}, now: {})",
                quantityToAdd, id, currentQuantity, newQuantity);

        return repository.save(item);
    }

    /**
     * Get total count of active items
     */
    public long getTotalItemCount() {
        return repository.countByActiveTrue();
    }

    /**
     * Get count by manufacture
     */
    public long getCountByManufacture(String manufacture) {
        return repository.countByManufactureAndActiveTrue(manufacture);
    }

    /**
     * Get items created by specific user
     */
    public List<StorageItem> getItemsByUser(String username) {
        return repository.findByCreatedByAndActiveTrue(username);
    }

    /**
     * Check if item exists
     */
    public boolean itemExists(Long id) {
        return repository.existsById(id);
    }

    /**
     * Bulk create items
     */
    @Transactional
    public List<StorageItem> createBulkItems(List<StorageItem> items) {
        items.forEach(item -> {
            if (item.getDateAdded() == null) {
                item.setDateAdded(LocalDateTime.now());
            }
            item.setActive(true);
        });
        return repository.saveAll(items);
    }

    /**
     * Get all manufactures (distinct)
     */
    public List<String> getAllManufactures() {
        return repository.findByActiveTrue().stream()
                .map(StorageItem::getManufacture)
                .filter(m -> m != null && !m.isEmpty())
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * Restore deleted item (set active=true)
     */
    @Transactional
    public void restoreItem(Long id) {
        Optional<StorageItem> itemOpt = repository.findById(id);

        if (itemOpt.isPresent()) {
            StorageItem item = itemOpt.get();
            item.setActive(true);
            item.setLastUpdated(LocalDateTime.now());
            repository.save(item);
            repository.flush();
            System.out.println("✓ Restored item ID: " + id);
        }
    }

    /**
     * Get all inactive (deleted) items
     */
    public List<StorageItem> getDeletedItems() {
        return repository.findAll().stream()
                .filter(item -> !item.getActive())
                .toList();
    }
}