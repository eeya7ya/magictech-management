package com.magictech.modules.sales.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * QuotationDesign Entity
 * Stores PDF documents with editing capabilities and version history.
 * Used by Sales and Presales modules for quotation documents.
 */
@Entity
@Table(name = "quotation_designs")
public class QuotationDesign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Entity relationship - which entity this quotation belongs to
    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType; // SALES_ORDER, PROJECT, CUSTOMER

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    // PDF Storage
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "pdf_data", columnDefinition = "BYTEA")
    private byte[] pdfData;

    // Original PDF backup (before any edits)
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "original_pdf_data", columnDefinition = "BYTEA")
    private byte[] originalPdfData;

    // Text annotations stored as JSON
    // Format: [{"page": 0, "x": 100, "y": 200, "text": "Hello", "fontSize": 12, "fontFamily": "Arial", "color": "#000000", "bold": false, "italic": false}]
    @Column(name = "pdf_annotations", columnDefinition = "TEXT")
    private String pdfAnnotations;

    // File metadata
    @Column(name = "filename", length = 255)
    private String filename;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "page_count")
    private Integer pageCount;

    // Versioning
    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "parent_version_id")
    private Long parentVersionId;

    @Column(name = "is_current_version", nullable = false)
    private Boolean isCurrentVersion = true;

    @Column(name = "version_note", length = 500)
    private String versionNote;

    // Module tracking
    @Column(name = "module_source", length = 50)
    private String moduleSource; // SALES, PRESALES

    // Audit fields
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Boolean active = true;

    // Constructors
    public QuotationDesign() {
    }

    public QuotationDesign(String entityType, Long entityId) {
        this.entityType = entityType;
        this.entityId = entityId;
    }

    // Lifecycle hooks
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.active == null) this.active = true;
        if (this.version == null) this.version = 1;
        if (this.isCurrentVersion == null) this.isCurrentVersion = true;
        if (this.mimeType == null) this.mimeType = "application/pdf";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public byte[] getPdfData() {
        return pdfData;
    }

    public void setPdfData(byte[] pdfData) {
        this.pdfData = pdfData;
        if (pdfData != null) {
            this.fileSize = (long) pdfData.length;
        }
    }

    public byte[] getOriginalPdfData() {
        return originalPdfData;
    }

    public void setOriginalPdfData(byte[] originalPdfData) {
        this.originalPdfData = originalPdfData;
    }

    public String getPdfAnnotations() {
        return pdfAnnotations;
    }

    public void setPdfAnnotations(String pdfAnnotations) {
        this.pdfAnnotations = pdfAnnotations;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Long getParentVersionId() {
        return parentVersionId;
    }

    public void setParentVersionId(Long parentVersionId) {
        this.parentVersionId = parentVersionId;
    }

    public Boolean getIsCurrentVersion() {
        return isCurrentVersion;
    }

    public void setIsCurrentVersion(Boolean isCurrentVersion) {
        this.isCurrentVersion = isCurrentVersion;
    }

    public String getVersionNote() {
        return versionNote;
    }

    public void setVersionNote(String versionNote) {
        this.versionNote = versionNote;
    }

    public String getModuleSource() {
        return moduleSource;
    }

    public void setModuleSource(String moduleSource) {
        this.moduleSource = moduleSource;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    // Helper methods
    public String getFileSizeFormatted() {
        if (fileSize == null || fileSize == 0) {
            return "0 B";
        }
        final String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = 0;
        double size = fileSize;
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        return String.format("%.2f %s", size, units[unitIndex]);
    }

    public boolean hasAnnotations() {
        return pdfAnnotations != null && !pdfAnnotations.isEmpty() && !pdfAnnotations.equals("[]");
    }

    public boolean hasPdf() {
        return pdfData != null && pdfData.length > 0;
    }

    @Override
    public String toString() {
        return "QuotationDesign{" +
                "id=" + id +
                ", entityType='" + entityType + '\'' +
                ", entityId=" + entityId +
                ", filename='" + filename + '\'' +
                ", version=" + version +
                ", isCurrentVersion=" + isCurrentVersion +
                '}';
    }
}
