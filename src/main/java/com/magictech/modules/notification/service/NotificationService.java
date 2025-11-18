package com.magictech.modules.notification.service;

import com.magictech.modules.notification.entity.Notification;
import com.magictech.modules.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service class for managing notifications
 */
@Service
@Transactional
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationRepository notificationRepository;

    /**
     * Create a new notification
     */
    public Notification createNotification(Notification notification) {
        logger.info("Creating notification: type={}, title={}", notification.getType(), notification.getTitle());
        return notificationRepository.save(notification);
    }

    /**
     * Create a notification for project creation
     */
    public Notification createProjectNotification(Long projectId, String projectName, String createdBy) {
        Notification notification = new Notification();
        notification.setType("PROJECT_CREATED");
        notification.setTitle("New Project Created");
        notification.setMessage(String.format("A new project '%s' has been created by %s",
                                              projectName, createdBy != null ? createdBy : "System"));
        notification.setEntityType("Project");
        notification.setEntityId(projectId);
        notification.setCreatedBy(createdBy);
        notification.setIsRead(false);

        logger.info("Creating project notification for project ID: {}, name: {}", projectId, projectName);
        return createNotification(notification);
    }

    /**
     * Get all notifications
     */
    public List<Notification> getAllNotifications() {
        return notificationRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Get all unread notifications
     */
    public List<Notification> getUnreadNotifications() {
        return notificationRepository.findByIsReadFalse();
    }

    /**
     * Get notifications for a specific user
     */
    public List<Notification> getNotificationsForUser(String username) {
        return notificationRepository.findByTargetUser(username);
    }

    /**
     * Get unread notifications for a specific user
     */
    public List<Notification> getUnreadNotificationsForUser(String username) {
        return notificationRepository.findByTargetUserAndIsReadFalse(username);
    }

    /**
     * Get notifications by type
     */
    public List<Notification> getNotificationsByType(String type) {
        return notificationRepository.findByType(type);
    }

    /**
     * Get notifications for a specific entity
     */
    public List<Notification> getNotificationsForEntity(String entityType, Long entityId) {
        return notificationRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    /**
     * Get recent notifications (last N days)
     */
    public List<Notification> getRecentNotifications(int days) {
        LocalDateTime sinceDate = LocalDateTime.now().minusDays(days);
        return notificationRepository.findRecentNotifications(sinceDate);
    }

    /**
     * Mark a notification as read
     */
    public void markAsRead(Long notificationId) {
        Optional<Notification> notification = notificationRepository.findById(notificationId);
        if (notification.isPresent()) {
            Notification n = notification.get();
            n.setIsRead(true);
            notificationRepository.save(n);
            logger.info("Marked notification {} as read", notificationId);
        } else {
            logger.warn("Notification with ID {} not found", notificationId);
        }
    }

    /**
     * Mark all notifications as read
     */
    public void markAllAsRead() {
        List<Notification> unreadNotifications = notificationRepository.findByIsReadFalse();
        for (Notification notification : unreadNotifications) {
            notification.setIsRead(true);
        }
        notificationRepository.saveAll(unreadNotifications);
        logger.info("Marked all {} notifications as read", unreadNotifications.size());
    }

    /**
     * Mark all notifications for a user as read
     */
    public void markAllAsReadForUser(String username) {
        List<Notification> unreadNotifications = notificationRepository.findByTargetUserAndIsReadFalse(username);
        for (Notification notification : unreadNotifications) {
            notification.setIsRead(true);
        }
        notificationRepository.saveAll(unreadNotifications);
        logger.info("Marked {} notifications as read for user {}", unreadNotifications.size(), username);
    }

    /**
     * Get count of unread notifications
     */
    public long getUnreadCount() {
        return notificationRepository.countByIsReadFalse();
    }

    /**
     * Get count of unread notifications for a specific user
     */
    public long getUnreadCountForUser(String username) {
        return notificationRepository.countByTargetUserAndIsReadFalse(username);
    }

    /**
     * Delete a notification
     */
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
        logger.info("Deleted notification with ID: {}", notificationId);
    }

    /**
     * Delete all read notifications
     */
    public void deleteAllReadNotifications() {
        List<Notification> readNotifications = notificationRepository.findAll().stream()
                .filter(n -> Boolean.TRUE.equals(n.getIsRead()))
                .toList();
        notificationRepository.deleteAll(readNotifications);
        logger.info("Deleted {} read notifications", readNotifications.size());
    }

    /**
     * Get a notification by ID
     */
    public Optional<Notification> getNotificationById(Long id) {
        return notificationRepository.findById(id);
    }
}
