package com.magictech.core.approval;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Pending approval entity for cross-module approval workflows
 * Auto-rejects after 2 days timeout
 */
@Entity
@Table(name = "pending_approvals")
public class PendingApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type", nullable = false, length = 50)
    private String type; // "PROJECT_ELEMENT_ADD", "PROJECT_ELEMENT_REMOVE", etc.

    @Column(name = "requested_by", nullable = false, length = 100)
    private String requestedBy; // Username who made the request

    @Column(name = "requested_by_user_id")
    private Long requestedByUserId;

    @Column(name = "approver_role", nullable = false, length = 50)
    private String approverRole; // UserRole that needs to approve (e.g., "SALES")

    @Column(name = "approver_user_id")
    private Long approverUserId; // Specific user if targeted approval

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "storage_item_id")
    private Long storageItemId;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING"; // "PENDING", "APPROVED", "REJECTED", "TIMEOUT"

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt; // Auto-calculated: createdAt + 2 days

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "processed_by", length = 100)
    private String processedBy; // Username who approved/rejected

    @Column(name = "processing_notes", columnDefinition = "TEXT")
    private String processingNotes; // Reason for rejection or approval notes

    @Column(name = "notification_id")
    private Long notificationId; // Link to notification sent

    // Lifecycle hooks
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.expiresAt = this.createdAt.plusDays(2); // Auto-reject after 2 days
        if (this.status == null) this.status = "PENDING";
    }

    // Constructors
    public PendingApproval() {
    }

    public PendingApproval(String type, String requestedBy, String approverRole) {
        this.type = type;
        this.requestedBy = requestedBy;
        this.approverRole = approverRole;
    }

    // Helper methods
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt) && "PENDING".equals(status);
    }

    public void approve(String approvedBy, String notes) {
        this.status = "APPROVED";
        this.processedBy = approvedBy;
        this.processedAt = LocalDateTime.now();
        this.processingNotes = notes;
    }

    public void reject(String rejectedBy, String reason) {
        this.status = "REJECTED";
        this.processedBy = rejectedBy;
        this.processedAt = LocalDateTime.now();
        this.processingNotes = reason;
    }

    public void timeout() {
        this.status = "TIMEOUT";
        this.processedAt = LocalDateTime.now();
        this.processingNotes = "Automatically rejected after 2 days timeout";
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public Long getRequestedByUserId() {
        return requestedByUserId;
    }

    public void setRequestedByUserId(Long requestedByUserId) {
        this.requestedByUserId = requestedByUserId;
    }

    public String getApproverRole() {
        return approverRole;
    }

    public void setApproverRole(String approverRole) {
        this.approverRole = approverRole;
    }

    public Long getApproverUserId() {
        return approverUserId;
    }

    public void setApproverUserId(Long approverUserId) {
        this.approverUserId = approverUserId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getStorageItemId() {
        return storageItemId;
    }

    public void setStorageItemId(Long storageItemId) {
        this.storageItemId = storageItemId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public String getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(String processedBy) {
        this.processedBy = processedBy;
    }

    public String getProcessingNotes() {
        return processingNotes;
    }

    public void setProcessingNotes(String processingNotes) {
        this.processingNotes = processingNotes;
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }

    @Override
    public String toString() {
        return "PendingApproval{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", requestedBy='" + requestedBy + '\'' +
                ", approverRole='" + approverRole + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", expiresAt=" + expiresAt +
                '}';
    }
}
