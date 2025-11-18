package com.magictech.modules.storage.service;

import com.magictech.modules.storage.entity.StorageColumnConfig;
import com.magictech.modules.storage.repository.StorageColumnConfigRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing dynamic column configurations
 */
@Service
@Transactional
public class StorageColumnConfigService {

    @Autowired
    private StorageColumnConfigRepository repository;

    /**
     * Initialize default columns on startup
     */
    @PostConstruct
    public void initializeDefaultColumns() {
        if (repository.count() > 0) {
            System.out.println("Column configs already initialized.");
            return;
        }

        System.out.println("Initializing default storage columns...");

        // Create default columns matching your table structure
        createDefaultColumn("id", "ID", "NUMBER", 60, 0);
        createDefaultColumn("manufacture", "Manufacture", "TEXT", 150, 1);
        createDefaultColumn("productName", "Product Name", "TEXT", 200, 2);
        createDefaultColumn("code", "Code", "TEXT", 120, 3);
        createDefaultColumn("serialNumber", "Serial Number", "TEXT", 150, 4);
        createDefaultColumn("quantity", "Quantity", "NUMBER", 100, 5);
        createDefaultColumn("price", "Price", "NUMBER", 100, 6);

        System.out.println("Default columns initialized successfully!");
    }

    private void createDefaultColumn(String name, String label, String type, int width, int order) {
        StorageColumnConfig config = new StorageColumnConfig(name, label, type, order, true);
        config.setColumnWidth(width);
        config.setVisible(true);
        config.setEditable(!name.equals("id")); // ID is not editable
        config.setRequired(name.equals("productName")); // Only product name required
        repository.save(config);
    }

    /**
     * Get all visible columns in display order
     */
    public List<StorageColumnConfig> getVisibleColumns() {
        return repository.findByVisibleTrueOrderByDisplayOrderAsc();
    }

    /**
     * Get all columns (including hidden)
     */
    public List<StorageColumnConfig> getAllColumns() {
        return repository.findAllByOrderByDisplayOrderAsc();
    }

    /**
     * Add new custom column
     */
    public StorageColumnConfig addCustomColumn(String columnName, String columnLabel, String columnType) {
        if (repository.existsByColumnName(columnName)) {
            throw new IllegalArgumentException("Column already exists: " + columnName);
        }

        // Get max display order
        List<StorageColumnConfig> allColumns = repository.findAllByOrderByDisplayOrderAsc();
        int maxOrder = allColumns.isEmpty() ? 0 :
                allColumns.get(allColumns.size() - 1).getDisplayOrder() + 1;

        StorageColumnConfig config = new StorageColumnConfig();
        config.setColumnName(columnName);
        config.setColumnLabel(columnLabel);
        config.setColumnType(columnType);
        config.setDisplayOrder(maxOrder);
        config.setVisible(true);
        config.setEditable(true);
        config.setRequired(false);
        config.setIsDefault(false); // Custom column

        return repository.save(config);
    }

    /**
     * Update column configuration
     */
    public StorageColumnConfig updateColumn(Long id, StorageColumnConfig updated) {
        StorageColumnConfig existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Column not found"));

        // Don't allow changing default column names
        if (!existing.getIsDefault() || existing.getColumnName().equals(updated.getColumnName())) {
            existing.setColumnLabel(updated.getColumnLabel());
            existing.setColumnWidth(updated.getColumnWidth());
            existing.setVisible(updated.getVisible());
            existing.setEditable(updated.getEditable());
            existing.setRequired(updated.getRequired());
        }

        return repository.save(existing);
    }

    /**
     * Delete custom column (cannot delete default columns)
     */
    public void deleteColumn(Long id) {
        StorageColumnConfig column = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Column not found"));

        if (column.getIsDefault()) {
            throw new IllegalArgumentException("Cannot delete default column: " + column.getColumnName());
        }

        repository.deleteById(id);
    }

    /**
     * Toggle column visibility
     */
    public void toggleVisibility(Long id) {
        StorageColumnConfig column = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Column not found"));

        column.setVisible(!column.getVisible());
        repository.save(column);
    }

    /**
     * Reorder columns
     */
    public void reorderColumns(List<Long> columnIds) {
        for (int i = 0; i < columnIds.size(); i++) {
            Long columnId = columnIds.get(i);
            Optional<StorageColumnConfig> columnOpt = repository.findById(columnId);

            if (columnOpt.isPresent()) {
                StorageColumnConfig column = columnOpt.get();
                column.setDisplayOrder(i);
                repository.save(column);
            }
        }
    }
}