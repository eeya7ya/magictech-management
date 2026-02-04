package com.magictech.modules.qualityassurance.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * PriceList Entity - Tracks price list uploads
 * Each upload creates a new PriceList with associated PriceListItems
 */
@Entity
@Table(name = "price_lists")
public class PriceList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "filename", nullable = false, length = 255)
    private String filename;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "version")
    private Integer version = 1;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Audit fields
    @Column(name = "uploaded_by", length = 100)
    private String uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(nullable = false)
    private Boolean active = true;

    // Constructors
    public PriceList() {
        this.uploadedAt = LocalDateTime.now();
        this.active = true;
        this.version = 1;
    }

    public PriceList(String filename, String uploadedBy) {
        this();
        this.filename = filename;
        this.uploadedBy = uploadedBy;
    }

    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
        if (active == null) {
            active = true;
        }
        if (version == null) {
            version = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "PriceList{" +
                "id=" + id +
                ", filename='" + filename + '\'' +
                ", version=" + version +
                ", uploadedBy='" + uploadedBy + '\'' +
                ", uploadedAt=" + uploadedAt +
                ", active=" + active +
                '}';
    }
}
