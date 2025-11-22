package com.magictech.core.messaging.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Tracks which users have seen which notifications.
 * This allows multiple users to see the same notification (e.g., MASTER + PROJECTS both see project creation).
 * Each user sees it once, but marking it as "seen" for one user doesn't hide it from others.
 */
@Entity
@Table(name = "notification_user_status",
    uniqueConstraints = @UniqueConstraint(columnNames = {"notification_id", "username"}))
public class NotificationUserStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notification_id", nullable = false)
    private Long notificationId;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "seen_at", nullable = false)
    private LocalDateTime seenAt;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    // Lifecycle hooks
    @PrePersist
    protected void onCreate() {
        this.seenAt = LocalDateTime.now();
        if (this.active == null) {
            this.active = true;
        }
    }

    // Constructors
    public NotificationUserStatus() {
    }

    public NotificationUserStatus(Long notificationId, String username) {
        this.notificationId = notificationId;
        this.username = username;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDateTime getSeenAt() {
        return seenAt;
    }

    public void setSeenAt(LocalDateTime seenAt) {
        this.seenAt = seenAt;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
