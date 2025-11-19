package com.magictech.modules.sales.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Customer Document Entity
 * Stores PDF and other documents associated with customers
 */
@Entity
@Table(name = "customer_documents")
public class CustomerDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "document_name", nullable = false, length = 255)
    private String documentName;

    @Column(name = "document_type", length = 50)
    private String documentType; // PDF, DOCX, XLSX, IMAGE, etc.

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath; // Actual file path on server

    @Column(name = "file_size")
    private Long fileSize; // Size in bytes

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "category", length = 100)
    private String category; // CONTRACT, QUOTATION, INVOICE, ID_PROOF, OTHER

    @Column(name = "uploaded_by", length = 100)
    private String uploadedBy;

    @Column(name = "date_uploaded", nullable = false)
    private LocalDateTime dateUploaded;

    @Column(name = "last_accessed")
    private LocalDateTime lastAccessed;

    @Column(nullable = false)
    private Boolean active = true;

    public CustomerDocument() {
        this.dateUploaded = LocalDateTime.now();
        this.active = true;
    }

    @PrePersist
    protected void onCreate() {
        if (this.dateUploaded == null) {
            this.dateUploaded = LocalDateTime.now();
        }
        if (this.active == null) {
            this.active = true;
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public LocalDateTime getDateUploaded() {
        return dateUploaded;
    }

    public void setDateUploaded(LocalDateTime dateUploaded) {
        this.dateUploaded = dateUploaded;
    }

    public LocalDateTime getLastAccessed() {
        return lastAccessed;
    }

    public void setLastAccessed(LocalDateTime lastAccessed) {
        this.lastAccessed = lastAccessed;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    /**
     * Get human-readable file size
     */
    public String getFileSizeFormatted() {
        if (fileSize == null) return "Unknown";

        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.2f KB", fileSize / 1024.0);
        if (fileSize < 1024 * 1024 * 1024) return String.format("%.2f MB", fileSize / (1024.0 * 1024.0));
        return String.format("%.2f GB", fileSize / (1024.0 * 1024.0 * 1024.0));
    }

    @Override
    public String toString() {
        return "CustomerDocument{" +
                "id=" + id +
                ", documentName='" + documentName + '\'' +
                ", documentType='" + documentType + '\'' +
                ", category='" + category + '\'' +
                ", fileSize=" + getFileSizeFormatted() +
                ", uploadedBy='" + uploadedBy + '\'' +
                ", dateUploaded=" + dateUploaded +
                '}';
    }
}
