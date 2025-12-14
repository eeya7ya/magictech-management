package com.magictech.modules.sales.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Step 6: Project Cost Excel Data Storage
 * Uploaded by Sales after project team finishes
 * Contains complete project cost information
 */
@Entity
@Table(name = "project_cost_data")
public class ProjectCostData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "workflow_id", nullable = false)
    private Long workflowId;

    // Excel file storage
    @Lob
    @Column(name = "excel_file")
    private byte[] excelFile;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    // ZIP file storage - Alternative to Excel for bundling multiple files
    @Lob
    @Column(name = "zip_file")
    private byte[] zipFile;

    @Column(name = "zip_file_name", length = 255)
    private String zipFileName;

    @Column(name = "zip_file_size")
    private Long zipFileSize;

    @Column(name = "zip_mime_type", length = 100)
    private String zipMimeType;

    // File type indicator: EXCEL, ZIP, or BOTH
    @Column(name = "file_type", length = 20)
    private String fileType;

    // Parsed data (JSON format for flexibility)
    @Column(name = "parsed_data", columnDefinition = "TEXT")
    private String parsedData;

    // Sales metadata
    @Column(name = "uploaded_by", length = 100, nullable = false)
    private String uploadedBy; // Sales user who received project

    @Column(name = "uploaded_by_id")
    private Long uploadedById;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "project_received_confirmation")
    private Boolean projectReceivedConfirmation = false;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt;

    @Column(nullable = false)
    private Boolean active = true;

    @PrePersist
    protected void onCreate() {
        this.uploadedAt = LocalDateTime.now();
        this.lastUpdatedAt = LocalDateTime.now();
        if (this.active == null) this.active = true;
        if (this.projectReceivedConfirmation == null) this.projectReceivedConfirmation = false;
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastUpdatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(Long workflowId) {
        this.workflowId = workflowId;
    }

    public byte[] getExcelFile() {
        return excelFile;
    }

    public void setExcelFile(byte[] excelFile) {
        this.excelFile = excelFile;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
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

    public String getParsedData() {
        return parsedData;
    }

    public void setParsedData(String parsedData) {
        this.parsedData = parsedData;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public Long getUploadedById() {
        return uploadedById;
    }

    public void setUploadedById(Long uploadedById) {
        this.uploadedById = uploadedById;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public Boolean getProjectReceivedConfirmation() {
        return projectReceivedConfirmation;
    }

    public void setProjectReceivedConfirmation(Boolean projectReceivedConfirmation) {
        this.projectReceivedConfirmation = projectReceivedConfirmation;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    // ZIP file getters and setters
    public byte[] getZipFile() {
        return zipFile;
    }

    public void setZipFile(byte[] zipFile) {
        this.zipFile = zipFile;
    }

    public String getZipFileName() {
        return zipFileName;
    }

    public void setZipFileName(String zipFileName) {
        this.zipFileName = zipFileName;
    }

    public Long getZipFileSize() {
        return zipFileSize;
    }

    public void setZipFileSize(Long zipFileSize) {
        this.zipFileSize = zipFileSize;
    }

    public String getZipMimeType() {
        return zipMimeType;
    }

    public void setZipMimeType(String zipMimeType) {
        this.zipMimeType = zipMimeType;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    // Helper methods
    public boolean hasExcelFile() {
        return excelFile != null && excelFile.length > 0;
    }

    public boolean hasZipFile() {
        return zipFile != null && zipFile.length > 0;
    }

    public boolean hasAnyFile() {
        return hasExcelFile() || hasZipFile();
    }
}
