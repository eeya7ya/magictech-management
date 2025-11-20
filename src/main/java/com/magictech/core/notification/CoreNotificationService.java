package com.magictech.core.notification;

import com.magictech.core.auth.User;
import com.magictech.core.auth.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing notifications with multi-channel delivery (in-app, email, push, SMS)
 */
@Service("coreNotificationService")
@Transactional
public class CoreNotificationService {

    private static final Logger log = LoggerFactory.getLogger(CoreNotificationService.class);

    @Autowired
    private CoreNotificationRepository notificationRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private GmailOAuth2Service gmailOAuth2Service;

    @Autowired
    private WebSocketNotificationService webSocketService;

    @Autowired
    private NotificationPreferencesService preferencesService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Create a notification for a specific user (with email delivery)
     */
    public Notification createUserNotification(Long userId, String module, String type,
                                               String title, String message, String createdBy) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setModule(module);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setCreatedBy(createdBy);

        // Default priority if not set
        if (notification.getPriority() == null) {
            notification.setPriority("NORMAL");
        }

        Notification saved = notificationRepository.save(notification);

        // Send email notification asynchronously
        sendEmailForNotification(userId, saved);

        // Send WebSocket notification for real-time delivery
        try {
            webSocketService.sendToUser(userId, saved);
        } catch (Exception e) {
            log.warn("Failed to send WebSocket notification to user {}: {}", userId, e.getMessage());
        }

        return saved;
    }

    /**
     * Create a notification for all users with a specific role (with email delivery)
     */
    public Notification createRoleNotification(String targetRole, String module, String type,
                                               String title, String message, String createdBy) {
        Notification notification = new Notification();
        notification.setTargetRole(targetRole);
        notification.setModule(module);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setCreatedBy(createdBy);

        // Default priority if not set
        if (notification.getPriority() == null) {
            notification.setPriority("NORMAL");
        }

        Notification saved = notificationRepository.save(notification);

        // Send email to all users with this role asynchronously
        sendEmailForRoleNotification(targetRole, saved);

        // Send WebSocket notification for real-time delivery to all users with role
        try {
            webSocketService.sendToRole(targetRole, saved);
        } catch (Exception e) {
            log.warn("Failed to send WebSocket notification to role {}: {}", targetRole, e.getMessage());
        }

        return saved;
    }

    /**
     * Create a notification with related entity information (with email delivery)
     */
    public Notification createNotificationWithRelation(String targetRole, String module, String type,
                                                       String title, String message,
                                                       Long relatedId, String relatedType,
                                                       String priority, String createdBy) {
        Notification notification = new Notification();
        notification.setTargetRole(targetRole);
        notification.setModule(module);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRelatedId(relatedId);
        notification.setRelatedType(relatedType);
        notification.setPriority(priority);
        notification.setCreatedBy(createdBy);

        Notification saved = notificationRepository.save(notification);

        // Send email to all users with this role asynchronously
        sendEmailForRoleNotification(targetRole, saved);

        // Send WebSocket notification for real-time delivery to all users with role
        try {
            webSocketService.sendToRole(targetRole, saved);
        } catch (Exception e) {
            log.warn("Failed to send WebSocket notification to role {}: {}", targetRole, e.getMessage());
        }

        return saved;
    }

    /**
     * Get all unread notifications for a user
     */
    public List<Notification> getUnreadNotifications(Long userId, String userRole) {
        List<Notification> userNotifications = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        List<Notification> roleNotifications = notificationRepository.findByTargetRoleAndIsReadFalseOrderByCreatedAtDesc(userRole);

        // Combine both lists
        userNotifications.addAll(roleNotifications);
        return userNotifications;
    }

    /**
     * Get all notifications for a user (read and unread)
     */
    public List<Notification> getAllNotifications(Long userId, String userRole) {
        List<Notification> userNotifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<Notification> roleNotifications = notificationRepository.findByTargetRoleOrderByCreatedAtDesc(userRole);

        // Combine both lists
        userNotifications.addAll(roleNotifications);
        return userNotifications;
    }

    /**
     * Get unshown notifications (for popup display)
     */
    public List<Notification> getUnshownNotifications(Long userId, String userRole) {
        return notificationRepository.findUnshownNotifications(userId, userRole);
    }

    /**
     * Mark notification as read
     */
    public void markAsRead(Long notificationId) {
        Optional<Notification> notificationOpt = notificationRepository.findById(notificationId);
        if (notificationOpt.isPresent()) {
            Notification notification = notificationOpt.get();
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }

    /**
     * Mark notification as shown (popup displayed)
     */
    public void markAsShown(Long notificationId) {
        Optional<Notification> notificationOpt = notificationRepository.findById(notificationId);
        if (notificationOpt.isPresent()) {
            Notification notification = notificationOpt.get();
            notification.setIsShown(true);
            notificationRepository.save(notification);
        }
    }

    /**
     * Mark all notifications as read for a user
     */
    public void markAllAsRead(Long userId, String userRole) {
        List<Notification> notifications = getUnreadNotifications(userId, userRole);
        for (Notification notification : notifications) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
        }
        notificationRepository.saveAll(notifications);
    }

    /**
     * Get unread count for a user
     */
    public long getUnreadCount(Long userId, String userRole) {
        long userCount = notificationRepository.countByUserIdAndIsReadFalse(userId);
        long roleCount = notificationRepository.countByTargetRoleAndIsReadFalse(userRole);
        return userCount + roleCount;
    }

    /**
     * Delete old notifications (3 months)
     */
    public void deleteOldNotifications() {
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
        notificationRepository.deleteByCreatedAtBefore(threeMonthsAgo);
    }

    /**
     * Get notifications by module
     */
    public List<Notification> getNotificationsByModule(String module) {
        return notificationRepository.findByModuleOrderByCreatedAtDesc(module);
    }

    /**
     * Get notifications by related entity
     */
    public List<Notification> getNotificationsByRelatedEntity(Long relatedId, String relatedType) {
        return notificationRepository.findByRelatedIdAndRelatedType(relatedId, relatedType);
    }

    /**
     * Delete notification
     */
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    /**
     * Get recent notifications (last 7 days)
     */
    public List<Notification> getRecentNotifications(Long userId, String userRole) {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        return notificationRepository.findRecentNotifications(userId, userRole, sevenDaysAgo);
    }

    /**
     * Get unread count for a specific module
     */
    public long getUnreadCountByModule(Long userId, String userRole, String module) {
        return notificationRepository.countUnreadByModule(userId, userRole, module);
    }

    // ==================== Email Notification Helpers ====================

    /**
     * Send email notification to a specific user
     */
    @Async
    protected void sendEmailForNotification(Long userId, Notification notification) {
        try {
            // Get user details
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                log.warn("User not found for email notification: {}", userId);
                return;
            }

            User user = userOpt.get();
            if (user.getEmail() == null || user.getEmail().isEmpty()) {
                log.debug("User {} has no email address. Skipping email notification.", userId);
                return;
            }

            // Check user preferences
            if (!preferencesService.shouldSendEmail(userId, notification.getPriority())) {
                log.debug("User {} has disabled email notifications for priority: {}",
                        userId, notification.getPriority());
                return;
            }

            // Check notification type preference
            if (!preferencesService.isNotificationTypeEnabled(userId, notification.getType())) {
                log.debug("User {} has disabled notifications for type: {}", userId, notification.getType());
                return;
            }

            // Send email via OAuth2 if user has linked Google account, otherwise fallback to SMTP
            String subject = "🔔 " + notification.getTitle();
            String actionUrl = buildActionUrl(notification);

            boolean emailSent = false;

            // Try OAuth2 Gmail first
            if (gmailOAuth2Service.canSendEmail(userId)) {
                try {
                    emailSent = gmailOAuth2Service.sendNotificationEmail(
                            user,
                            user.getEmail(),
                            notification.getTitle(),
                            notification.getMessage(),
                            notification.getPriority(),
                            actionUrl
                    ).get(); // Wait for async completion
                    log.info("Email sent via OAuth2 Gmail to: {} ({})", user.getUsername(), user.getEmail());
                } catch (Exception e) {
                    log.warn("OAuth2 email failed for user {}, falling back to SMTP: {}", userId, e.getMessage());
                }
            }

            // Fallback to SMTP if OAuth2 failed or not configured
            if (!emailSent) {
                emailService.sendNotificationEmail(
                        user.getEmail(),
                        subject,
                        notification.getTitle(),
                        notification.getMessage(),
                        notification.getPriority(),
                        actionUrl
                );
                log.info("Email notification sent via SMTP to user: {} ({})", user.getUsername(), user.getEmail());
            }

        } catch (Exception e) {
            log.error("Failed to send email notification to user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Send email notification to all users with a specific role
     */
    @Async
    protected void sendEmailForRoleNotification(String targetRole, Notification notification) {
        try {
            // Get all users with this role
            List<User> users = userRepository.findByRoleAndActiveTrue(com.magictech.core.auth.UserRole.valueOf(targetRole));

            if (users.isEmpty()) {
                log.warn("No users found with role: {}", targetRole);
                return;
            }

            log.info("Sending email notification to {} users with role: {}", users.size(), targetRole);

            for (User user : users) {
                if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                    // Check user preferences
                    if (preferencesService.shouldSendEmail(user.getId(), notification.getPriority()) &&
                            preferencesService.isNotificationTypeEnabled(user.getId(), notification.getType())) {

                        String subject = "🔔 " + notification.getTitle();
                        String actionUrl = buildActionUrl(notification);

                        boolean emailSent = false;

                        // Try OAuth2 Gmail first
                        if (gmailOAuth2Service.canSendEmail(user.getId())) {
                            try {
                                emailSent = gmailOAuth2Service.sendNotificationEmail(
                                        user,
                                        user.getEmail(),
                                        notification.getTitle(),
                                        notification.getMessage(),
                                        notification.getPriority(),
                                        actionUrl
                                ).get(); // Wait for async completion
                            } catch (Exception e) {
                                log.warn("OAuth2 email failed for user {}, falling back to SMTP", user.getId());
                            }
                        }

                        // Fallback to SMTP if OAuth2 failed or not configured
                        if (!emailSent) {
                            emailService.sendNotificationEmail(
                                    user.getEmail(),
                                    subject,
                                    notification.getTitle(),
                                    notification.getMessage(),
                                    notification.getPriority(),
                                    actionUrl
                            );
                        }
                    }
                }
            }

            log.info("Role-based email notification completed for role: {}", targetRole);

        } catch (Exception e) {
            log.error("Failed to send role-based email notification: {}", e.getMessage());
        }
    }

    /**
     * Build action URL from notification
     */
    private String buildActionUrl(Notification notification) {
        if (notification.getRelatedId() != null && notification.getRelatedType() != null) {
            return String.format("magictech://open/%s/%d",
                    notification.getRelatedType().toLowerCase(),
                    notification.getRelatedId());
        }

        // Module-based URLs
        if (notification.getModule() != null) {
            return "magictech://open/" + notification.getModule().toLowerCase();
        }

        return null;
    }
}
