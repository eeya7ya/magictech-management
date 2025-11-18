package com.magictech.modules.storage.config;

import java.util.*;

/**
 * Module Storage Configuration
 * Defines which columns each module can see and edit
 */
public enum ModuleStorageConfig {

    SALES(
            "Sales Module",
            Arrays.asList("id", "manufacture", "productName", "code", "serialNumber", "availabilityStatus", "price"),
            Arrays.asList("manufacture", "productName", "code", "serialNumber", "price"),
            true // Can modify quantity through availability
    ),

    MAINTENANCE(
            "Maintenance Module",
            Arrays.asList("id", "manufacture", "productName", "code", "serialNumber", "availabilityStatus"),
            Arrays.asList("manufacture", "productName", "code", "serialNumber"),
            true
    ),

    PROJECTS(
            "Projects Module",
            Arrays.asList("id", "manufacture", "productName", "code", "serialNumber", "availabilityStatus"),
            Arrays.asList("manufacture", "productName", "code", "serialNumber"),
            true
    ),

    PRICING(
            "Pricing Module",
            Arrays.asList("id", "manufacture", "productName", "code", "serialNumber", "availabilityStatus", "price"),
            Arrays.asList("manufacture", "productName", "code", "serialNumber", "price"),
            true
    ),

    STORAGE(
            "Storage Module",
            Arrays.asList("id", "manufacture", "productName", "code", "serialNumber", "quantity", "price", "dateAdded"),
            Arrays.asList("manufacture", "productName", "code", "serialNumber", "quantity", "price"),
            false // Storage sees actual quantity numbers
    );

    private final String displayName;
    private final List<String> visibleColumns;
    private final List<String> editableColumns;
    private final boolean useAvailabilityStatus;

    ModuleStorageConfig(String displayName, List<String> visibleColumns,
                        List<String> editableColumns, boolean useAvailabilityStatus) {
        this.displayName = displayName;
        this.visibleColumns = visibleColumns;
        this.editableColumns = editableColumns;
        this.useAvailabilityStatus = useAvailabilityStatus;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getVisibleColumns() {
        return visibleColumns;
    }

    public List<String> getEditableColumns() {
        return editableColumns;
    }

    public boolean isUseAvailabilityStatus() {
        return useAvailabilityStatus;
    }

    public boolean isColumnVisible(String columnName) {
        return visibleColumns.contains(columnName);
    }

    public boolean isColumnEditable(String columnName) {
        return editableColumns.contains(columnName);
    }

    /**
     * Get column configuration details
     */
    public static class ColumnConfig {
        private String columnName;
        private String displayLabel;
        private int width;
        private String alignment;

        public ColumnConfig(String columnName, String displayLabel, int width, String alignment) {
            this.columnName = columnName;
            this.displayLabel = displayLabel;
            this.width = width;
            this.alignment = alignment;
        }

        public String getColumnName() { return columnName; }
        public String getDisplayLabel() { return displayLabel; }
        public int getWidth() { return width; }
        public String getAlignment() { return alignment; }
    }

    /**
     * Get column configurations for a module
     */
    public List<ColumnConfig> getColumnConfigs() {
        List<ColumnConfig> configs = new ArrayList<>();

        if (isColumnVisible("id")) {
            configs.add(new ColumnConfig("id", "ID", 60, "CENTER"));
        }
        if (isColumnVisible("manufacture")) {
            configs.add(new ColumnConfig("manufacture", "Manufacture", 150, "CENTER-LEFT"));
        }
        if (isColumnVisible("productName")) {
            configs.add(new ColumnConfig("productName", "Product Name", 200, "CENTER-LEFT"));
        }
        if (isColumnVisible("code")) {
            configs.add(new ColumnConfig("code", "Code", 120, "CENTER"));
        }
        if (isColumnVisible("serialNumber")) {
            configs.add(new ColumnConfig("serialNumber", "Serial Number", 150, "CENTER"));
        }
        if (isColumnVisible("quantity")) {
            configs.add(new ColumnConfig("quantity", "Quantity", 100, "CENTER"));
        }
        if (isColumnVisible("availabilityStatus")) {
            configs.add(new ColumnConfig("availabilityStatus", "Availability", 120, "CENTER"));
        }
        if (isColumnVisible("price")) {
            configs.add(new ColumnConfig("price", "Price", 120, "CENTER-RIGHT"));
        }
        if (isColumnVisible("dateAdded")) {
            configs.add(new ColumnConfig("dateAdded", "Date Added", 150, "CENTER"));
        }

        return configs;
    }
}