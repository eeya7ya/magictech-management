package com.magictech.core.notification;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Notification entity for cross-module communication
 * Supports real-time notifications with 3-month retention policy
 */
@Entity(name = "CoreNotification")
@Table(name = "core_notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "target_role", length = 50)
    private String targetRole; // UserRole that should see this notification

    @Column(name = "module", nullable = false, length = 50)
    private String module; // "SALES", "PROJECTS", "STORAGE", etc.

    @Column(name = "type", nullable = false, length = 50)
    private String type; // "PROJECT_CREATED", "ELEMENT_APPROVAL_REQUEST", "ELEMENT_APPROVED", "ELEMENT_REJECTED"

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "related_id")
    private Long relatedId; // ID of related entity (project, approval, etc.)

    @Column(name = "related_type", length = 50)
    private String relatedType; // "PROJECT", "APPROVAL", "SALES_ORDER", etc.

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "is_shown", nullable = false)
    private Boolean isShown = false; // For popup display tracking

    @Column(name = "priority", length = 20)
    private String priority = "NORMAL"; // "LOW", "NORMAL", "HIGH", "URGENT"

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    // Lifecycle hooks
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.isRead == null) this.isRead = false;
        if (this.isShown == null) this.isShown = false;
        if (this.priority == null) this.priority = "NORMAL";
    }

    // Constructors
    public Notification() {
    }

    public Notification(String module, String type, String title, String message) {
        this.module = module;
        this.type = type;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public Long getRelatedId() {
        return relatedId;
    }

    public void setRelatedId(Long relatedId) {
        this.relatedId = relatedId;
    }

    public String getRelatedType() {
        return relatedType;
    }

    public void setRelatedType(String relatedType) {
        this.relatedType = relatedType;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
        if (isRead && this.readAt == null) {
            this.readAt = LocalDateTime.now();
        }
    }

    public Boolean getIsShown() {
        return isShown;
    }

    public void setIsShown(Boolean isShown) {
        this.isShown = isShown;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", module='" + module + '\'' +
                ", type='" + type + '\'' +
                ", title='" + title + '\'' +
                ", isRead=" + isRead +
                ", priority='" + priority + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
