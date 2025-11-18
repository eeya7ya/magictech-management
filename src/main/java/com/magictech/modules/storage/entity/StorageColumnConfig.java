package com.magictech.modules.storage.entity;

import jakarta.persistence.*;

/**
 * Storage Column Configuration
 * Allows dynamic addition/removal of table columns
 */
@Entity
@Table(name = "storage_column_configs")
public class StorageColumnConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "column_name", nullable = false, unique = true, length = 100)
    private String columnName;

    @Column(name = "column_label", nullable = false, length = 150)
    private String columnLabel;

    @Column(name = "column_type", nullable = false, length = 20)
    private String columnType; // TEXT, NUMBER, DATE, BOOLEAN

    @Column(name = "column_width")
    private Integer columnWidth = 150;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "visible", nullable = false)
    private Boolean visible = true;

    @Column(name = "editable", nullable = false)
    private Boolean editable = true;

    @Column(name = "required", nullable = false)
    private Boolean required = false;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false; // Built-in columns (can't delete)

    // Constructors
    public StorageColumnConfig() {}

    public StorageColumnConfig(String columnName, String columnLabel, String columnType,
                               Integer displayOrder, Boolean isDefault) {
        this.columnName = columnName;
        this.columnLabel = columnLabel;
        this.columnType = columnType;
        this.displayOrder = displayOrder;
        this.visible = true;
        this.editable = true;
        this.required = false;
        this.isDefault = isDefault;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getColumnLabel() {
        return columnLabel;
    }

    public void setColumnLabel(String columnLabel) {
        this.columnLabel = columnLabel;
    }

    public String getColumnType() {
        return columnType;
    }

    public void setColumnType(String columnType) {
        this.columnType = columnType;
    }

    public Integer getColumnWidth() {
        return columnWidth;
    }

    public void setColumnWidth(Integer columnWidth) {
        this.columnWidth = columnWidth;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

    public Boolean getEditable() {
        return editable;
    }

    public void setEditable(Boolean editable) {
        this.editable = editable;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    @Override
    public String toString() {
        return "StorageColumnConfig{" +
                "columnName='" + columnName + '\'' +
                ", columnLabel='" + columnLabel + '\'' +
                ", columnType='" + columnType + '\'' +
                ", displayOrder=" + displayOrder +
                '}';
    }
}