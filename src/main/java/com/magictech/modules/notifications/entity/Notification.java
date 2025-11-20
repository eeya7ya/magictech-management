package com.magictech.modules.notifications.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Notification entity for storing user notifications
 */
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_user_read", columnList = "user_id,is_read"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private NotificationPriority priority = NotificationPriority.NORMAL;

    @Column(name = "module_source", length = 50)
    private String moduleSource;  // Which module generated this (SALES, STORAGE, PROJECTS)

    @Column(name = "reference_id")
    private Long referenceId;  // ID of related entity (order ID, project ID, etc.)

    @Column(name = "reference_type", length = 50)
    private String referenceType;  // Entity type (SalesOrder, Project, etc.)

    @Column(name = "is_read")
    private Boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "is_sent_email")
    private Boolean isSentEmail = false;

    @Column(name = "is_sent_desktop")
    private Boolean isSentDesktop = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column
    private Boolean active = true;

    // Action URL (optional, for clickable notifications)
    @Column(name = "action_url", length = 500)
    private String actionUrl;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.active == null) this.active = true;
        if (this.isRead == null) this.isRead = false;
        if (this.isSentEmail == null) this.isSentEmail = false;
        if (this.isSentDesktop == null) this.isSentDesktop = false;
        if (this.priority == null) this.priority = NotificationPriority.NORMAL;
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

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public NotificationPriority getPriority() {
        return priority;
    }

    public void setPriority(NotificationPriority priority) {
        this.priority = priority;
    }

    public String getModuleSource() {
        return moduleSource;
    }

    public void setModuleSource(String moduleSource) {
        this.moduleSource = moduleSource;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Long referenceId) {
        this.referenceId = referenceId;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
        if (Boolean.TRUE.equals(isRead) && this.readAt == null) {
            this.readAt = LocalDateTime.now();
        }
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public Boolean getIsSentEmail() {
        return isSentEmail;
    }

    public void setIsSentEmail(Boolean isSentEmail) {
        this.isSentEmail = isSentEmail;
    }

    public Boolean getIsSentDesktop() {
        return isSentDesktop;
    }

    public void setIsSentDesktop(Boolean isSentDesktop) {
        this.isSentDesktop = isSentDesktop;
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

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public void setActionUrl(String actionUrl) {
        this.actionUrl = actionUrl;
    }

    // Helper methods
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }
}
