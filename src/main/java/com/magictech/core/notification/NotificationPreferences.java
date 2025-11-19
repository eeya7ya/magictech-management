package com.magictech.core.notification;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * NotificationPreferences Entity
 *
 * Stores user-specific notification preferences for all channels (email, push, SMS).
 * Allows users to customize which notifications they receive and how they receive them.
 */
@Entity
@Table(name = "notification_preferences")
public class NotificationPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    // ==================== Channel Preferences ====================

    @Column(name = "email_enabled", nullable = false)
    private Boolean emailEnabled = true;

    @Column(name = "push_enabled", nullable = false)
    private Boolean pushEnabled = true;

    @Column(name = "sms_enabled", nullable = false)
    private Boolean smsEnabled = false;

    @Column(name = "in_app_enabled", nullable = false)
    private Boolean inAppEnabled = true;

    // ==================== Notification Type Preferences ====================

    @Column(name = "notify_approval_requests", nullable = false)
    private Boolean notifyApprovalRequests = true;

    @Column(name = "notify_project_updates", nullable = false)
    private Boolean notifyProjectUpdates = true;

    @Column(name = "notify_task_assignments", nullable = false)
    private Boolean notifyTaskAssignments = true;

    @Column(name = "notify_sales_orders", nullable = false)
    private Boolean notifySalesOrders = true;

    @Column(name = "notify_low_stock", nullable = false)
    private Boolean notifyLowStock = true;

    @Column(name = "notify_customer_updates", nullable = false)
    private Boolean notifyCustomerUpdates = true;

    @Column(name = "notify_system_alerts", nullable = false)
    private Boolean notifySystemAlerts = true;

    // ==================== Digest Mode ====================

    @Column(name = "digest_mode", nullable = false)
    private Boolean digestMode = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "digest_frequency", length = 20)
    private DigestFrequency digestFrequency = DigestFrequency.DAILY;

    @Column(name = "digest_time")
    private LocalTime digestTime = LocalTime.of(9, 0); // 9:00 AM default

    // ==================== Quiet Hours ====================

    @Column(name = "quiet_hours_enabled", nullable = false)
    private Boolean quietHoursEnabled = false;

    @Column(name = "quiet_hours_start")
    private LocalTime quietHoursStart = LocalTime.of(22, 0); // 10:00 PM

    @Column(name = "quiet_hours_end")
    private LocalTime quietHoursEnd = LocalTime.of(8, 0); // 8:00 AM

    // ==================== Priority Filtering ====================

    @Column(name = "min_priority_email", length = 20)
    private String minPriorityEmail = "NORMAL"; // LOW, NORMAL, HIGH, URGENT

    @Column(name = "min_priority_push", length = 20)
    private String minPriorityPush = "NORMAL";

    @Column(name = "min_priority_sms", length = 20)
    private String minPrioritySms = "URGENT";

    // ==================== Metadata ====================

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ==================== Enums ====================

    public enum DigestFrequency {
        DAILY,
        WEEKLY,
        MONTHLY
    }

    // ==================== Lifecycle Hooks ====================

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();

        // Set defaults if not specified
        if (this.emailEnabled == null) this.emailEnabled = true;
        if (this.pushEnabled == null) this.pushEnabled = true;
        if (this.smsEnabled == null) this.smsEnabled = false;
        if (this.inAppEnabled == null) this.inAppEnabled = true;
        if (this.digestMode == null) this.digestMode = false;
        if (this.quietHoursEnabled == null) this.quietHoursEnabled = false;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ==================== Constructors ====================

    public NotificationPreferences() {
    }

    public NotificationPreferences(Long userId) {
        this.userId = userId;
    }

    // ==================== Business Logic ====================

    /**
     * Check if notifications should be sent during quiet hours
     */
    public boolean isInQuietHours() {
        if (!quietHoursEnabled) {
            return false;
        }

        LocalTime now = LocalTime.now();

        // Handle overnight quiet hours (e.g., 22:00 - 08:00)
        if (quietHoursStart.isAfter(quietHoursEnd)) {
            return now.isAfter(quietHoursStart) || now.isBefore(quietHoursEnd);
        } else {
            // Same-day quiet hours (e.g., 12:00 - 14:00)
            return now.isAfter(quietHoursStart) && now.isBefore(quietHoursEnd);
        }
    }

    /**
     * Check if notification should be sent via email based on priority
     */
    public boolean shouldSendEmail(String priority) {
        if (!emailEnabled) return false;
        return comparePriority(priority, minPriorityEmail) >= 0;
    }

    /**
     * Check if notification should be sent via push based on priority
     */
    public boolean shouldSendPush(String priority) {
        if (!pushEnabled) return false;
        return comparePriority(priority, minPriorityPush) >= 0;
    }

    /**
     * Check if notification should be sent via SMS based on priority
     */
    public boolean shouldSendSms(String priority) {
        if (!smsEnabled) return false;
        return comparePriority(priority, minPrioritySms) >= 0;
    }

    /**
     * Compare two priority levels
     * Returns: -1 if p1 < p2, 0 if p1 == p2, 1 if p1 > p2
     */
    private int comparePriority(String p1, String p2) {
        int level1 = getPriorityLevel(p1);
        int level2 = getPriorityLevel(p2);
        return Integer.compare(level1, level2);
    }

    private int getPriorityLevel(String priority) {
        return switch (priority) {
            case "LOW" -> 0;
            case "NORMAL" -> 1;
            case "HIGH" -> 2;
            case "URGENT" -> 3;
            default -> 1; // Default to NORMAL
        };
    }

    // ==================== Getters and Setters ====================

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

    public Boolean getEmailEnabled() {
        return emailEnabled;
    }

    public void setEmailEnabled(Boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    public Boolean getPushEnabled() {
        return pushEnabled;
    }

    public void setPushEnabled(Boolean pushEnabled) {
        this.pushEnabled = pushEnabled;
    }

    public Boolean getSmsEnabled() {
        return smsEnabled;
    }

    public void setSmsEnabled(Boolean smsEnabled) {
        this.smsEnabled = smsEnabled;
    }

    public Boolean getInAppEnabled() {
        return inAppEnabled;
    }

    public void setInAppEnabled(Boolean inAppEnabled) {
        this.inAppEnabled = inAppEnabled;
    }

    public Boolean getNotifyApprovalRequests() {
        return notifyApprovalRequests;
    }

    public void setNotifyApprovalRequests(Boolean notifyApprovalRequests) {
        this.notifyApprovalRequests = notifyApprovalRequests;
    }

    public Boolean getNotifyProjectUpdates() {
        return notifyProjectUpdates;
    }

    public void setNotifyProjectUpdates(Boolean notifyProjectUpdates) {
        this.notifyProjectUpdates = notifyProjectUpdates;
    }

    public Boolean getNotifyTaskAssignments() {
        return notifyTaskAssignments;
    }

    public void setNotifyTaskAssignments(Boolean notifyTaskAssignments) {
        this.notifyTaskAssignments = notifyTaskAssignments;
    }

    public Boolean getNotifySalesOrders() {
        return notifySalesOrders;
    }

    public void setNotifySalesOrders(Boolean notifySalesOrders) {
        this.notifySalesOrders = notifySalesOrders;
    }

    public Boolean getNotifyLowStock() {
        return notifyLowStock;
    }

    public void setNotifyLowStock(Boolean notifyLowStock) {
        this.notifyLowStock = notifyLowStock;
    }

    public Boolean getNotifyCustomerUpdates() {
        return notifyCustomerUpdates;
    }

    public void setNotifyCustomerUpdates(Boolean notifyCustomerUpdates) {
        this.notifyCustomerUpdates = notifyCustomerUpdates;
    }

    public Boolean getNotifySystemAlerts() {
        return notifySystemAlerts;
    }

    public void setNotifySystemAlerts(Boolean notifySystemAlerts) {
        this.notifySystemAlerts = notifySystemAlerts;
    }

    public Boolean getDigestMode() {
        return digestMode;
    }

    public void setDigestMode(Boolean digestMode) {
        this.digestMode = digestMode;
    }

    public DigestFrequency getDigestFrequency() {
        return digestFrequency;
    }

    public void setDigestFrequency(DigestFrequency digestFrequency) {
        this.digestFrequency = digestFrequency;
    }

    public LocalTime getDigestTime() {
        return digestTime;
    }

    public void setDigestTime(LocalTime digestTime) {
        this.digestTime = digestTime;
    }

    public Boolean getQuietHoursEnabled() {
        return quietHoursEnabled;
    }

    public void setQuietHoursEnabled(Boolean quietHoursEnabled) {
        this.quietHoursEnabled = quietHoursEnabled;
    }

    public LocalTime getQuietHoursStart() {
        return quietHoursStart;
    }

    public void setQuietHoursStart(LocalTime quietHoursStart) {
        this.quietHoursStart = quietHoursStart;
    }

    public LocalTime getQuietHoursEnd() {
        return quietHoursEnd;
    }

    public void setQuietHoursEnd(LocalTime quietHoursEnd) {
        this.quietHoursEnd = quietHoursEnd;
    }

    public String getMinPriorityEmail() {
        return minPriorityEmail;
    }

    public void setMinPriorityEmail(String minPriorityEmail) {
        this.minPriorityEmail = minPriorityEmail;
    }

    public String getMinPriorityPush() {
        return minPriorityPush;
    }

    public void setMinPriorityPush(String minPriorityPush) {
        this.minPriorityPush = minPriorityPush;
    }

    public String getMinPrioritySms() {
        return minPrioritySms;
    }

    public void setMinPrioritySms(String minPrioritySms) {
        this.minPrioritySms = minPrioritySms;
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

    @Override
    public String toString() {
        return "NotificationPreferences{" +
                "id=" + id +
                ", userId=" + userId +
                ", emailEnabled=" + emailEnabled +
                ", pushEnabled=" + pushEnabled +
                ", smsEnabled=" + smsEnabled +
                ", digestMode=" + digestMode +
                ", quietHoursEnabled=" + quietHoursEnabled +
                '}';
    }
}
