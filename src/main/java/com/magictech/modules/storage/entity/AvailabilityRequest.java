package com.magictech.modules.storage.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Availability Request Entity
 * Tracks requests from Sales/Presales modules asking Storage for item availability.
 * Storage team receives these requests and responds via email.
 */
@Entity
@Table(name = "availability_requests")
public class AvailabilityRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Reference to the storage item being requested
    @Column(name = "storage_item_id", nullable = false)
    private Long storageItemId;

    // Cached item details for display
    @Column(name = "item_name", length = 200)
    private String itemName;

    @Column(name = "item_manufacture", length = 200)
    private String itemManufacture;

    @Column(name = "item_code", length = 100)
    private String itemCode;

    // Request details
    @Column(name = "quantity_requested", nullable = false)
    private Integer quantityRequested;

    @Column(name = "request_reason", columnDefinition = "TEXT")
    private String requestReason;

    // Project/Customer context (optional)
    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "project_name", length = 200)
    private String projectName;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    // Requester information
    @Column(name = "requester_module", length = 50, nullable = false)
    private String requesterModule; // SALES or PRESALES

    @Column(name = "requester_username", length = 100, nullable = false)
    private String requesterUsername;

    @Column(name = "requester_email", length = 200)
    private String requesterEmail;

    // Request status
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    // Response details
    @Column(name = "response_message", columnDefinition = "TEXT")
    private String responseMessage;

    @Column(name = "available_quantity")
    private Integer availableQuantity;

    @Column(name = "responded_by", length = 100)
    private String respondedBy;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    // Email tracking
    @Column(name = "email_sent")
    private Boolean emailSent = false;

    @Column(name = "email_sent_at")
    private LocalDateTime emailSentAt;

    // Metadata
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Boolean active = true;

    // Status enum
    public enum RequestStatus {
        PENDING,      // Waiting for storage team response
        IN_REVIEW,    // Being reviewed by storage
        AVAILABLE,    // Item is available
        PARTIAL,      // Partial quantity available
        UNAVAILABLE,  // Item not available
        RESPONDED,    // Response sent via email
        CLOSED        // Request completed/closed
    }

    // Constructors
    public AvailabilityRequest() {
        this.createdAt = LocalDateTime.now();
        this.active = true;
        this.status = RequestStatus.PENDING;
        this.emailSent = false;
    }

    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (active == null) {
            active = true;
        }
        if (status == null) {
            status = RequestStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStorageItemId() {
        return storageItemId;
    }

    public void setStorageItemId(Long storageItemId) {
        this.storageItemId = storageItemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemManufacture() {
        return itemManufacture;
    }

    public void setItemManufacture(String itemManufacture) {
        this.itemManufacture = itemManufacture;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public Integer getQuantityRequested() {
        return quantityRequested;
    }

    public void setQuantityRequested(Integer quantityRequested) {
        this.quantityRequested = quantityRequested;
    }

    public String getRequestReason() {
        return requestReason;
    }

    public void setRequestReason(String requestReason) {
        this.requestReason = requestReason;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getRequesterModule() {
        return requesterModule;
    }

    public void setRequesterModule(String requesterModule) {
        this.requesterModule = requesterModule;
    }

    public String getRequesterUsername() {
        return requesterUsername;
    }

    public void setRequesterUsername(String requesterUsername) {
        this.requesterUsername = requesterUsername;
    }

    public String getRequesterEmail() {
        return requesterEmail;
    }

    public void setRequesterEmail(String requesterEmail) {
        this.requesterEmail = requesterEmail;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public String getRespondedBy() {
        return respondedBy;
    }

    public void setRespondedBy(String respondedBy) {
        this.respondedBy = respondedBy;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }

    public Boolean getEmailSent() {
        return emailSent;
    }

    public void setEmailSent(Boolean emailSent) {
        this.emailSent = emailSent;
    }

    public LocalDateTime getEmailSentAt() {
        return emailSentAt;
    }

    public void setEmailSentAt(LocalDateTime emailSentAt) {
        this.emailSentAt = emailSentAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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

    @Override
    public String toString() {
        return "AvailabilityRequest{" +
                "id=" + id +
                ", itemName='" + itemName + '\'' +
                ", quantityRequested=" + quantityRequested +
                ", requesterModule='" + requesterModule + '\'' +
                ", requesterUsername='" + requesterUsername + '\'' +
                ", status=" + status +
                '}';
    }
}
