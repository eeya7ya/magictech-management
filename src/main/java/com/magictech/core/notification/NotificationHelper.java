package com.magictech.core.notification;

import com.magictech.core.auth.User;
import com.magictech.core.auth.UserRepository;
import com.magictech.core.auth.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * NotificationHelper - Simplified utility for sending notifications
 *
 * This class provides easy-to-use methods for sending notifications to users
 * with Gmail integration. Users who have linked their Gmail accounts will
 * receive email notifications automatically.
 *
 * Usage Examples:
 *
 * 1. Send notification to a specific user:
 *    notificationHelper.notifyUser(userId, "Task Complete",
 *                                  "Your task has been completed successfully");
 *
 * 2. Send notification to all users with a role:
 *    notificationHelper.notifyRole(UserRole.SALES, "New Order",
 *                                  "A new sales order has been created");
 *
 * 3. Send urgent notification:
 *    notificationHelper.notifyUserUrgent(userId, "Critical Alert",
 *                                        "Immediate action required");
 */
@Component
public class NotificationHelper {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GmailOAuth2Service gmailOAuth2Service;

    // ==================== Send to Specific User ====================

    /**
     * Send a normal priority notification to a specific user
     * User will receive email if they have linked their Gmail account
     *
     * @param userId Target user ID
     * @param title Notification title
     * @param message Notification message
     * @return Created notification
     */
    public Notification notifyUser(Long userId, String title, String message) {
        return notificationService.createUserNotification(
            userId,
            "SYSTEM",
            "GENERAL",
            title,
            message,
            "System"
        );
    }

    /**
     * Send a normal priority notification to a user by username
     *
     * @param username Target username
     * @param title Notification title
     * @param message Notification message
     * @return Created notification or null if user not found
     */
    public Notification notifyUser(String username, String title, String message) {
        return userRepository.findByUsernameIgnoreCase(username)
            .map(user -> notifyUser(user.getId(), title, message))
            .orElse(null);
    }

    /**
     * Send a notification to a specific user with custom priority
     *
     * @param userId Target user ID
     * @param title Notification title
     * @param message Notification message
     * @param priority Priority level: URGENT, HIGH, NORMAL, LOW
     * @return Created notification
     */
    public Notification notifyUser(Long userId, String title, String message, String priority) {
        return notificationService.createNotificationWithRelation(
            null, // No role targeting
            "SYSTEM",
            "GENERAL",
            title,
            message,
            userId,
            "USER",
            priority,
            "System"
        );
    }

    /**
     * Send an URGENT notification to a specific user
     *
     * @param userId Target user ID
     * @param title Notification title
     * @param message Notification message
     * @return Created notification
     */
    public Notification notifyUserUrgent(Long userId, String title, String message) {
        return notifyUser(userId, title, message, "URGENT");
    }

    /**
     * Send a HIGH priority notification to a specific user
     *
     * @param userId Target user ID
     * @param title Notification title
     * @param message Notification message
     * @return Created notification
     */
    public Notification notifyUserHigh(Long userId, String title, String message) {
        return notifyUser(userId, title, message, "HIGH");
    }

    // ==================== Send to Role (Multiple Users) ====================

    /**
     * Send a notification to all users with a specific role
     * All users with the role will receive email if they have linked Gmail
     *
     * @param role Target user role
     * @param title Notification title
     * @param message Notification message
     * @return Created notification
     */
    public Notification notifyRole(UserRole role, String title, String message) {
        return notificationService.createRoleNotification(
            role.name(),
            "SYSTEM",
            "GENERAL",
            title,
            message,
            "System"
        );
    }

    /**
     * Send an URGENT notification to all users with a specific role
     *
     * @param role Target user role
     * @param title Notification title
     * @param message Notification message
     * @return Created notification
     */
    public Notification notifyRoleUrgent(UserRole role, String title, String message) {
        return notificationService.createNotificationWithRelation(
            role.name(),
            "SYSTEM",
            "GENERAL",
            title,
            message,
            null,
            null,
            "URGENT",
            "System"
        );
    }

    /**
     * Send a HIGH priority notification to all users with a specific role
     *
     * @param role Target user role
     * @param title Notification title
     * @param message Notification message
     * @return Created notification
     */
    public Notification notifyRoleHigh(UserRole role, String title, String message) {
        return notificationService.createNotificationWithRelation(
            role.name(),
            "SYSTEM",
            "GENERAL",
            title,
            message,
            null,
            null,
            "HIGH",
            "System"
        );
    }

    // ==================== Module-Specific Notifications ====================

    /**
     * Send a project-related notification
     *
     * @param userId Target user ID
     * @param projectId Project ID
     * @param title Notification title
     * @param message Notification message
     * @return Created notification
     */
    public Notification notifyProjectUpdate(Long userId, Long projectId, String title, String message) {
        return notificationService.createNotificationWithRelation(
            null,
            "PROJECTS",
            "PROJECT_UPDATE",
            title,
            message,
            projectId,
            "PROJECT",
            "HIGH",
            "System"
        );
    }

    /**
     * Send a sales order notification
     *
     * @param userId Target user ID
     * @param orderId Order ID
     * @param title Notification title
     * @param message Notification message
     * @return Created notification
     */
    public Notification notifySalesOrder(Long userId, Long orderId, String title, String message) {
        return notificationService.createNotificationWithRelation(
            null,
            "SALES",
            "ORDER_UPDATE",
            title,
            message,
            orderId,
            "ORDER",
            "NORMAL",
            "System"
        );
    }

    /**
     * Send a storage/inventory alert
     *
     * @param role Target role (typically STORAGE or MASTER)
     * @param itemId Storage item ID
     * @param title Notification title
     * @param message Notification message
     * @return Created notification
     */
    public Notification notifyStorageAlert(UserRole role, Long itemId, String title, String message) {
        return notificationService.createNotificationWithRelation(
            role.name(),
            "STORAGE",
            "INVENTORY_ALERT",
            title,
            message,
            itemId,
            "STORAGE_ITEM",
            "HIGH",
            "System"
        );
    }

    // ==================== Direct Gmail Sending (Without In-App Notification) ====================

    /**
     * Send email directly via Gmail (no in-app notification)
     * User must have linked their Gmail account
     *
     * @param fromUserId Sender user ID (must have Gmail linked)
     * @param toEmail Recipient email address
     * @param subject Email subject
     * @param message Email message
     * @return True if email sent successfully
     */
    public boolean sendEmailDirect(Long fromUserId, String toEmail, String subject, String message) {
        try {
            User sender = userRepository.findById(fromUserId).orElse(null);
            if (sender == null) {
                System.err.println("✗ Sender user not found: " + fromUserId);
                return false;
            }

            if (!gmailOAuth2Service.canSendEmail(fromUserId)) {
                System.err.println("✗ User " + sender.getUsername() + " has not linked their Gmail account");
                return false;
            }

            return gmailOAuth2Service.sendNotificationEmail(
                sender,
                toEmail,
                subject,
                message,
                "NORMAL",
                null
            ).get(); // Wait for async completion

        } catch (Exception e) {
            System.err.println("✗ Failed to send direct email: " + e.getMessage());
            return false;
        }
    }

    /**
     * Send email to another user in the system
     *
     * @param fromUserId Sender user ID (must have Gmail linked)
     * @param toUserId Recipient user ID
     * @param subject Email subject
     * @param message Email message
     * @return True if email sent successfully
     */
    public boolean sendEmailToUser(Long fromUserId, Long toUserId, String subject, String message) {
        User recipient = userRepository.findById(toUserId).orElse(null);
        if (recipient == null || recipient.getEmail() == null) {
            System.err.println("✗ Recipient not found or has no email: " + toUserId);
            return false;
        }

        return sendEmailDirect(fromUserId, recipient.getEmail(), subject, message);
    }

    // ==================== Utility Methods ====================

    /**
     * Check if a user has linked their Gmail account
     *
     * @param userId User ID
     * @return True if user can send/receive Gmail notifications
     */
    public boolean hasGmailLinked(Long userId) {
        return gmailOAuth2Service.canSendEmail(userId);
    }

    /**
     * Check if a username has linked their Gmail account
     *
     * @param username Username
     * @return True if user can send/receive Gmail notifications
     */
    public boolean hasGmailLinked(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
            .map(user -> hasGmailLinked(user.getId()))
            .orElse(false);
    }

    /**
     * Get all users with a specific role
     *
     * @param role User role
     * @return List of users with the role
     */
    public List<User> getUsersByRole(UserRole role) {
        return userRepository.findByRoleAndActiveTrue(role);
    }

    /**
     * Get unread notification count for a user
     *
     * @param userId User ID
     * @param userRole User role
     * @return Number of unread notifications
     */
    public long getUnreadCount(Long userId, UserRole userRole) {
        return notificationService.getUnreadCount(userId, userRole.name());
    }

    /**
     * Mark all notifications as read for a user
     *
     * @param userId User ID
     * @param userRole User role
     */
    public void markAllAsRead(Long userId, UserRole userRole) {
        notificationService.markAllAsRead(userId, userRole.name());
    }
}
