package com.magictech.core.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.Optional;

/**
 * NotificationPreferencesService
 *
 * Manages user notification preferences for all channels (email, push, SMS, in-app)
 */
@Service
@Transactional
public class NotificationPreferencesService {

    private static final Logger log = LoggerFactory.getLogger(NotificationPreferencesService.class);

    @Autowired
    private NotificationPreferencesRepository repository;

    // ==================== CRUD Operations ====================

    /**
     * Get preferences for a user (creates default if not exists)
     */
    public NotificationPreferences getOrCreatePreferences(Long userId) {
        return repository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("Creating default notification preferences for user: {}", userId);
                    NotificationPreferences prefs = new NotificationPreferences(userId);
                    return repository.save(prefs);
                });
    }

    /**
     * Get preferences for a user (returns Optional)
     */
    public Optional<NotificationPreferences> getPreferences(Long userId) {
        return repository.findByUserId(userId);
    }

    /**
     * Update preferences for a user
     */
    public NotificationPreferences updatePreferences(Long userId, NotificationPreferences updatedPrefs) {
        NotificationPreferences existing = getOrCreatePreferences(userId);

        // Update all fields
        existing.setEmailEnabled(updatedPrefs.getEmailEnabled());
        existing.setPushEnabled(updatedPrefs.getPushEnabled());
        existing.setSmsEnabled(updatedPrefs.getSmsEnabled());
        existing.setInAppEnabled(updatedPrefs.getInAppEnabled());

        existing.setNotifyApprovalRequests(updatedPrefs.getNotifyApprovalRequests());
        existing.setNotifyProjectUpdates(updatedPrefs.getNotifyProjectUpdates());
        existing.setNotifyTaskAssignments(updatedPrefs.getNotifyTaskAssignments());
        existing.setNotifySalesOrders(updatedPrefs.getNotifySalesOrders());
        existing.setNotifyLowStock(updatedPrefs.getNotifyLowStock());
        existing.setNotifyCustomerUpdates(updatedPrefs.getNotifyCustomerUpdates());
        existing.setNotifySystemAlerts(updatedPrefs.getNotifySystemAlerts());

        existing.setDigestMode(updatedPrefs.getDigestMode());
        existing.setDigestFrequency(updatedPrefs.getDigestFrequency());
        existing.setDigestTime(updatedPrefs.getDigestTime());

        existing.setQuietHoursEnabled(updatedPrefs.getQuietHoursEnabled());
        existing.setQuietHoursStart(updatedPrefs.getQuietHoursStart());
        existing.setQuietHoursEnd(updatedPrefs.getQuietHoursEnd());

        existing.setMinPriorityEmail(updatedPrefs.getMinPriorityEmail());
        existing.setMinPriorityPush(updatedPrefs.getMinPriorityPush());
        existing.setMinPrioritySms(updatedPrefs.getMinPrioritySms());

        return repository.save(existing);
    }

    /**
     * Delete preferences for a user
     */
    public void deletePreferences(Long userId) {
        repository.deleteByUserId(userId);
        log.info("Deleted notification preferences for user: {}", userId);
    }

    // ==================== Channel Management ====================

    /**
     * Enable/disable email notifications for a user
     */
    public void setEmailEnabled(Long userId, boolean enabled) {
        NotificationPreferences prefs = getOrCreatePreferences(userId);
        prefs.setEmailEnabled(enabled);
        repository.save(prefs);
    }

    /**
     * Enable/disable push notifications for a user
     */
    public void setPushEnabled(Long userId, boolean enabled) {
        NotificationPreferences prefs = getOrCreatePreferences(userId);
        prefs.setPushEnabled(enabled);
        repository.save(prefs);
    }

    /**
     * Enable/disable SMS notifications for a user
     */
    public void setSmsEnabled(Long userId, boolean enabled) {
        NotificationPreferences prefs = getOrCreatePreferences(userId);
        prefs.setSmsEnabled(enabled);
        repository.save(prefs);
    }

    // ==================== Quiet Hours Management ====================

    /**
     * Set quiet hours for a user
     */
    public void setQuietHours(Long userId, boolean enabled, LocalTime start, LocalTime end) {
        NotificationPreferences prefs = getOrCreatePreferences(userId);
        prefs.setQuietHoursEnabled(enabled);
        prefs.setQuietHoursStart(start);
        prefs.setQuietHoursEnd(end);
        repository.save(prefs);
    }

    // ==================== Digest Mode Management ====================

    /**
     * Set digest mode for a user
     */
    public void setDigestMode(Long userId, boolean enabled, NotificationPreferences.DigestFrequency frequency, LocalTime time) {
        NotificationPreferences prefs = getOrCreatePreferences(userId);
        prefs.setDigestMode(enabled);
        prefs.setDigestFrequency(frequency);
        prefs.setDigestTime(time);
        repository.save(prefs);
    }

    // ==================== Priority Management ====================

    /**
     * Set minimum priority for email notifications
     */
    public void setMinPriorityEmail(Long userId, String priority) {
        NotificationPreferences prefs = getOrCreatePreferences(userId);
        prefs.setMinPriorityEmail(priority);
        repository.save(prefs);
    }

    /**
     * Set minimum priority for push notifications
     */
    public void setMinPriorityPush(Long userId, String priority) {
        NotificationPreferences prefs = getOrCreatePreferences(userId);
        prefs.setMinPriorityPush(priority);
        repository.save(prefs);
    }

    /**
     * Set minimum priority for SMS notifications
     */
    public void setMinPrioritySms(Long userId, String priority) {
        NotificationPreferences prefs = getOrCreatePreferences(userId);
        prefs.setMinPrioritySms(priority);
        repository.save(prefs);
    }

    // ==================== Business Logic ====================

    /**
     * Check if user should receive email notification
     */
    public boolean shouldSendEmail(Long userId, String priority) {
        NotificationPreferences prefs = getOrCreatePreferences(userId);

        if (prefs.isInQuietHours()) {
            log.debug("User {} is in quiet hours. Skipping email notification.", userId);
            return false;
        }

        return prefs.shouldSendEmail(priority);
    }

    /**
     * Check if user should receive push notification
     */
    public boolean shouldSendPush(Long userId, String priority) {
        NotificationPreferences prefs = getOrCreatePreferences(userId);

        if (prefs.isInQuietHours()) {
            log.debug("User {} is in quiet hours. Skipping push notification.", userId);
            return false;
        }

        return prefs.shouldSendPush(priority);
    }

    /**
     * Check if user should receive SMS notification
     */
    public boolean shouldSendSms(Long userId, String priority) {
        NotificationPreferences prefs = getOrCreatePreferences(userId);

        if (prefs.isInQuietHours()) {
            log.debug("User {} is in quiet hours. Skipping SMS notification.", userId);
            return false;
        }

        return prefs.shouldSendSms(priority);
    }

    /**
     * Check if user has enabled a specific notification type
     */
    public boolean isNotificationTypeEnabled(Long userId, String notificationType) {
        NotificationPreferences prefs = getOrCreatePreferences(userId);

        return switch (notificationType.toUpperCase()) {
            case "APPROVAL_REQUEST" -> prefs.getNotifyApprovalRequests();
            case "PROJECT_UPDATE" -> prefs.getNotifyProjectUpdates();
            case "TASK_ASSIGNMENT" -> prefs.getNotifyTaskAssignments();
            case "SALES_ORDER" -> prefs.getNotifySalesOrders();
            case "LOW_STOCK" -> prefs.getNotifyLowStock();
            case "CUSTOMER_UPDATE" -> prefs.getNotifyCustomerUpdates();
            case "SYSTEM_ALERT" -> prefs.getNotifySystemAlerts();
            default -> true; // Default to enabled for unknown types
        };
    }
}
