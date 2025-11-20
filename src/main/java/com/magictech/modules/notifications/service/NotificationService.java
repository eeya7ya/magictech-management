package com.magictech.modules.notifications.service;

import com.magictech.core.auth.User;
import com.magictech.core.auth.UserRepository;
import com.magictech.modules.notifications.entity.Notification;
import com.magictech.modules.notifications.entity.NotificationPriority;
import com.magictech.modules.notifications.entity.NotificationType;
import com.magictech.modules.notifications.event.NotificationEvent;
import com.magictech.modules.notifications.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing notifications
 */
@Service
@Transactional
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GmailNotificationService gmailService;

    @Autowired
    private DesktopNotificationService desktopService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * Create and send notification
     */
    public Notification createNotification(Long userId, String title, String message,
                                          NotificationType type, NotificationPriority priority) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setPriority(priority);

        notification = notificationRepository.save(notification);

        // Send through appropriate channels based on priority
        sendNotificationChannels(notification);

        logger.info("Created notification {} for user {}: {}", notification.getId(), userId, title);
        return notification;
    }

    /**
     * Create notification with all details
     */
    public Notification createNotification(Long userId, String title, String message,
                                          NotificationType type, NotificationPriority priority,
                                          String moduleSource, Long referenceId,
                                          String referenceType, String actionUrl) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setPriority(priority);
        notification.setModuleSource(moduleSource);
        notification.setReferenceId(referenceId);
        notification.setReferenceType(referenceType);
        notification.setActionUrl(actionUrl);

        notification = notificationRepository.save(notification);
        sendNotificationChannels(notification);

        logger.info("Created notification {} for user {} from module {}: {}",
                   notification.getId(), userId, moduleSource, title);
        return notification;
    }

    /**
     * Listen to NotificationEvent and create notifications
     */
    @EventListener
    @Async
    public void handleNotificationEvent(NotificationEvent event) {
        logger.info("Received notification event: {} for {} users",
                   event.getTitle(), event.getUserIds().size());

        for (Long userId : event.getUserIds()) {
            createNotification(
                userId,
                event.getTitle(),
                event.getMessage(),
                event.getType(),
                event.getPriority(),
                event.getModuleSource(),
                event.getReferenceId(),
                event.getReferenceType(),
                event.getActionUrl()
            );
        }
    }

    /**
     * Send notification through appropriate channels (email, desktop)
     */
    private void sendNotificationChannels(Notification notification) {
        // Send desktop notification if priority warrants it
        if (notification.getPriority().shouldSendDesktop()) {
            desktopService.sendDesktopNotification(notification);
        }

        // Send email if high/urgent priority
        if (notification.getPriority().shouldSendEmail()) {
            gmailService.sendEmailNotification(notification);
        }
    }

    /**
     * Publish notification event (will be handled asynchronously)
     */
    public void publishNotification(NotificationEvent event) {
        eventPublisher.publishEvent(event);
    }

    /**
     * Get all notifications for a user
     */
    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdAndActiveTrueOrderByCreatedAtDesc(userId);
    }

    /**
     * Get unread notifications for a user
     */
    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseAndActiveTrueOrderByCreatedAtDesc(userId);
    }

    /**
     * Count unread notifications
     */
    public Long countUnread(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    /**
     * Get recent notifications (last 7 days)
     */
    public List<Notification> getRecentNotifications(Long userId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return notificationRepository.findRecentByUserId(userId, since);
    }

    /**
     * Mark notification as read
     */
    public void markAsRead(Long notificationId) {
        Optional<Notification> optionalNotification = notificationRepository.findById(notificationId);
        if (optionalNotification.isPresent()) {
            Notification notification = optionalNotification.get();
            notification.markAsRead();
            notificationRepository.save(notification);
            logger.debug("Marked notification {} as read", notificationId);
        }
    }

    /**
     * Mark all notifications as read for a user
     */
    public void markAllAsRead(Long userId) {
        List<Notification> unreadNotifications = getUnreadNotifications(userId);
        for (Notification notification : unreadNotifications) {
            notification.markAsRead();
            notificationRepository.save(notification);
        }
        logger.info("Marked {} notifications as read for user {}", unreadNotifications.size(), userId);
    }

    /**
     * Delete notification (soft delete)
     */
    public void deleteNotification(Long notificationId) {
        Optional<Notification> optionalNotification = notificationRepository.findById(notificationId);
        if (optionalNotification.isPresent()) {
            Notification notification = optionalNotification.get();
            notification.setActive(false);
            notificationRepository.save(notification);
            logger.debug("Soft deleted notification {}", notificationId);
        }
    }

    /**
     * Delete all notifications for a user
     */
    public void deleteAllForUser(Long userId) {
        List<Notification> notifications = getUserNotifications(userId);
        for (Notification notification : notifications) {
            notification.setActive(false);
        }
        notificationRepository.saveAll(notifications);
        logger.info("Deleted {} notifications for user {}", notifications.size(), userId);
    }

    /**
     * Scheduled job to cleanup old notifications (runs daily at 2 AM)
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupOldNotifications() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        LocalDateTime now = LocalDateTime.now();

        try {
            notificationRepository.deleteOldNotifications(thirtyDaysAgo, now);
            logger.info("Cleaned up old notifications older than 30 days");
        } catch (Exception e) {
            logger.error("Error cleaning up old notifications", e);
        }
    }

    /**
     * Process pending email notifications (for batch sending)
     */
    @Scheduled(fixedDelay = 60000) // Every minute
    public void processPendingEmailNotifications() {
        try {
            List<Notification> unsent = notificationRepository.findUnsentEmailNotifications();
            if (!unsent.isEmpty()) {
                logger.info("Processing {} unsent email notifications", unsent.size());
                for (Notification notification : unsent) {
                    gmailService.sendEmailNotification(notification);
                }
            }
        } catch (Exception e) {
            logger.error("Error processing pending email notifications", e);
        }
    }

    /**
     * Helper: Notify all users with specific role
     */
    public void notifyUsersByRole(String role, String title, String message,
                                  NotificationType type, NotificationPriority priority) {
        List<User> users = userRepository.findByRole(UserRole.valueOf(role));
        for (User user : users) {
            createNotification(user.getId(), title, message, type, priority);
        }
    }

    /**
     * Helper: Notify all active users
     */
    public void notifyAllUsers(String title, String message,
                               NotificationType type, NotificationPriority priority) {
        List<User> users = userRepository.findByActiveTrue();
        for (User user : users) {
            createNotification(user.getId(), title, message, type, priority);
        }
    }
}
