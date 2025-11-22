package com.magictech.modules.sales.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Step 4: Missing Item Request
 * Submitted by Sales to MASTER and SALES_MANAGER for approval
 * Contains specific item details that are missing
 */
@Entity
@Table(name = "missing_item_requests")
public class MissingItemRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "workflow_id", nullable = false)
    private Long workflowId;

    // Item details
    @Column(name = "item_name", length = 255, nullable = false)
    private String itemName;

    @Column(name = "item_description", columnDefinition = "TEXT")
    private String itemDescription;

    @Column(name = "quantity_needed")
    private Integer quantityNeeded;

    @Column(name = "item_specifications", columnDefinition = "TEXT")
    private String itemSpecifications;

    @Column(name = "urgency_level", length = 50)
    private String urgencyLevel; // LOW, MEDIUM, HIGH, CRITICAL

    // Request metadata
    @Column(name = "requested_by", length = 100, nullable = false)
    private String requestedBy; // Sales user

    @Column(name = "requested_by_id")
    private Long requestedById;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    // Approval status
    @Column(name = "approval_status", length = 50)
    @Enumerated(EnumType.STRING)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @Column(name = "approved_by_master", length = 100)
    private String approvedByMaster;

    @Column(name = "approved_by_master_id")
    private Long approvedByMasterId;

    @Column(name = "master_approval_at")
    private LocalDateTime masterApprovalAt;

    @Column(name = "approved_by_sales_manager", length = 100)
    private String approvedBySalesManager;

    @Column(name = "approved_by_sales_manager_id")
    private Long approvedBySalesManagerId;

    @Column(name = "sales_manager_approval_at")
    private LocalDateTime salesManagerApprovalAt;

    @Column(name = "approval_notes", columnDefinition = "TEXT")
    private String approvalNotes;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // Item delivery confirmation
    @Column(name = "item_delivered")
    private Boolean itemDelivered = false;

    @Column(name = "delivery_confirmed_by", length = 100)
    private String deliveryConfirmedBy;

    @Column(name = "delivery_confirmed_at")
    private LocalDateTime deliveryConfirmedAt;

    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt;

    @Column(nullable = false)
    private Boolean active = true;

    // Approval status enum
    public enum ApprovalStatus {
        PENDING,
        APPROVED_BY_MASTER,
        APPROVED_BY_SALES_MANAGER,
        FULLY_APPROVED,
        REJECTED
    }

    @PrePersist
    protected void onCreate() {
        this.requestedAt = LocalDateTime.now();
        this.lastUpdatedAt = LocalDateTime.now();
        if (this.active == null) this.active = true;
        if (this.approvalStatus == null) this.approvalStatus = ApprovalStatus.PENDING;
        if (this.itemDelivered == null) this.itemDelivered = false;
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastUpdatedAt = LocalDateTime.now();
    }

    // Helper method to check if fully approved
    public boolean isFullyApproved() {
        return approvalStatus == ApprovalStatus.FULLY_APPROVED ||
               (approvedByMaster != null && approvedBySalesManager != null);
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

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }

    public Integer getQuantityNeeded() {
        return quantityNeeded;
    }

    public void setQuantityNeeded(Integer quantityNeeded) {
        this.quantityNeeded = quantityNeeded;
    }

    public String getItemSpecifications() {
        return itemSpecifications;
    }

    public void setItemSpecifications(String itemSpecifications) {
        this.itemSpecifications = itemSpecifications;
    }

    public String getUrgencyLevel() {
        return urgencyLevel;
    }

    public void setUrgencyLevel(String urgencyLevel) {
        this.urgencyLevel = urgencyLevel;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public Long getRequestedById() {
        return requestedById;
    }

    public void setRequestedById(Long requestedById) {
        this.requestedById = requestedById;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public ApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(ApprovalStatus approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public String getApprovedByMaster() {
        return approvedByMaster;
    }

    public void setApprovedByMaster(String approvedByMaster) {
        this.approvedByMaster = approvedByMaster;
        if (approvedByMaster != null && this.masterApprovalAt == null) {
            this.masterApprovalAt = LocalDateTime.now();
            updateApprovalStatus();
        }
    }

    public Long getApprovedByMasterId() {
        return approvedByMasterId;
    }

    public void setApprovedByMasterId(Long approvedByMasterId) {
        this.approvedByMasterId = approvedByMasterId;
    }

    public LocalDateTime getMasterApprovalAt() {
        return masterApprovalAt;
    }

    public void setMasterApprovalAt(LocalDateTime masterApprovalAt) {
        this.masterApprovalAt = masterApprovalAt;
    }

    public String getApprovedBySalesManager() {
        return approvedBySalesManager;
    }

    public void setApprovedBySalesManager(String approvedBySalesManager) {
        this.approvedBySalesManager = approvedBySalesManager;
        if (approvedBySalesManager != null && this.salesManagerApprovalAt == null) {
            this.salesManagerApprovalAt = LocalDateTime.now();
            updateApprovalStatus();
        }
    }

    public Long getApprovedBySalesManagerId() {
        return approvedBySalesManagerId;
    }

    public void setApprovedBySalesManagerId(Long approvedBySalesManagerId) {
        this.approvedBySalesManagerId = approvedBySalesManagerId;
    }

    public LocalDateTime getSalesManagerApprovalAt() {
        return salesManagerApprovalAt;
    }

    public void setSalesManagerApprovalAt(LocalDateTime salesManagerApprovalAt) {
        this.salesManagerApprovalAt = salesManagerApprovalAt;
    }

    public String getApprovalNotes() {
        return approvalNotes;
    }

    public void setApprovalNotes(String approvalNotes) {
        this.approvalNotes = approvalNotes;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Boolean getItemDelivered() {
        return itemDelivered;
    }

    public void setItemDelivered(Boolean itemDelivered) {
        this.itemDelivered = itemDelivered;
    }

    public String getDeliveryConfirmedBy() {
        return deliveryConfirmedBy;
    }

    public void setDeliveryConfirmedBy(String deliveryConfirmedBy) {
        this.deliveryConfirmedBy = deliveryConfirmedBy;
    }

    public LocalDateTime getDeliveryConfirmedAt() {
        return deliveryConfirmedAt;
    }

    public void setDeliveryConfirmedAt(LocalDateTime deliveryConfirmedAt) {
        this.deliveryConfirmedAt = deliveryConfirmedAt;
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

    // Helper method to update approval status
    private void updateApprovalStatus() {
        if (approvedByMaster != null && approvedBySalesManager != null) {
            this.approvalStatus = ApprovalStatus.FULLY_APPROVED;
        } else if (approvedByMaster != null) {
            this.approvalStatus = ApprovalStatus.APPROVED_BY_MASTER;
        } else if (approvedBySalesManager != null) {
            this.approvalStatus = ApprovalStatus.APPROVED_BY_SALES_MANAGER;
        }
    }
}
