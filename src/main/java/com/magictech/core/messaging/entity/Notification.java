package com.magictech.core.messaging.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a notification in the system.
 * Stores notification history for tracking and retrieval of missed notifications.
 */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notification_type", nullable = false, length = 50)
    private String type; // INFO, WARNING, SUCCESS, ERROR

    @Column(name = "module", nullable = false, length = 50)
    private String module; // sales, projects, storage, maintenance, pricing

    @Column(name = "action", length = 50)
    private String action; // created, updated, deleted, confirmed, completed

    @Column(name = "entity_type", length = 50)
    private String entityType; // project, sales_order, storage_item

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "target_device_id", length = 100)
    private String targetDeviceId; // Specific device or null for broadcast

    @Column(name = "target_module", length = 50)
    private String targetModule; // Which module should receive this notification

    @Column(name = "read_status", nullable = false)
    private Boolean readStatus = false;

    @Column(name = "resolved", nullable = false)
    private Boolean resolved = false; // For approval notifications - true when approved/rejected

    @Column(name = "resolved_by", length = 100)
    private String resolvedBy; // Username of person who resolved the approval

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt; // When the approval was resolved

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "source_device_id", length = 100)
    private String sourceDeviceId; // Device that created this notification

    @Column(name = "priority", length = 20)
    private String priority; // LOW, MEDIUM, HIGH, URGENT

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON string for additional data

    @Column(nullable = false)
    private Boolean active = true;

    // Lifecycle hooks
    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
        if (this.active == null) {
            this.active = true;
        }
        if (this.readStatus == null) {
            this.readStatus = false;
        }
        if (this.resolved == null) {
            this.resolved = false;
        }
        if (this.priority == null) {
            this.priority = "MEDIUM";
        }
    }

    // Constructors
    public Notification() {
    }

    public Notification(String type, String module, String title, String message) {
        this.type = type;
        this.module = module;
        this.title = title;
        this.message = message;
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

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTargetDeviceId() {
        return targetDeviceId;
    }

    public void setTargetDeviceId(String targetDeviceId) {
        this.targetDeviceId = targetDeviceId;
    }

    public String getTargetModule() {
        return targetModule;
    }

    public void setTargetModule(String targetModule) {
        this.targetModule = targetModule;
    }

    public Boolean getReadStatus() {
        return readStatus;
    }

    public void setReadStatus(Boolean readStatus) {
        this.readStatus = readStatus;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getSourceDeviceId() {
        return sourceDeviceId;
    }

    public void setSourceDeviceId(String sourceDeviceId) {
        this.sourceDeviceId = sourceDeviceId;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getResolved() {
        return resolved;
    }

    public void setResolved(Boolean resolved) {
        this.resolved = resolved;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(String resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
