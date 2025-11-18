package com.magictech.core.notification;

import com.magictech.core.auth.User;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private NotificationRepository notificationRepository;

    /**
     * Create a notification for a specific user
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
        return notificationRepository.save(notification);
    }

    /**
     * Create a notification for all users with a specific role
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
        return notificationRepository.save(notification);
    }

    /**
     * Create a notification with related entity information
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
        return notificationRepository.save(notification);
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
}
