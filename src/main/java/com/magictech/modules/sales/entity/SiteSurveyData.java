package com.magictech.modules.sales.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Step 1: Site Survey Excel Data Storage
 * Stores uploaded Excel files with images/photos for site surveys
 */
@Entity
@Table(name = "site_survey_data")
public class SiteSurveyData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "workflow_id", nullable = false)
    private Long workflowId;

    // Excel file storage
    @Lob
    @Column(name = "excel_file", columnDefinition = "BYTEA")
    private byte[] excelFile;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    // Parsed data (JSON format for flexibility)
    @Column(name = "parsed_data", columnDefinition = "TEXT")
    private String parsedData;

    // Survey metadata
    @Column(name = "survey_done_by", length = 50)
    private String surveyDoneBy; // "SALES" or "PROJECT"

    @Column(name = "survey_done_by_user", length = 100)
    private String surveyDoneByUser;

    @Column(name = "survey_done_by_user_id")
    private Long surveyDoneByUserId;

    @Column(name = "uploaded_by", length = 100, nullable = false)
    private String uploadedBy;

    @Column(name = "uploaded_by_id")
    private Long uploadedById;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

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

    public String getSurveyDoneBy() {
        return surveyDoneBy;
    }

    public void setSurveyDoneBy(String surveyDoneBy) {
        this.surveyDoneBy = surveyDoneBy;
    }

    public String getSurveyDoneByUser() {
        return surveyDoneByUser;
    }

    public void setSurveyDoneByUser(String surveyDoneByUser) {
        this.surveyDoneByUser = surveyDoneByUser;
    }

    public Long getSurveyDoneByUserId() {
        return surveyDoneByUserId;
    }

    public void setSurveyDoneByUserId(Long surveyDoneByUserId) {
        this.surveyDoneByUserId = surveyDoneByUserId;
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
}
