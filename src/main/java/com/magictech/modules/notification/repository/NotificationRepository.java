package com.magictech.modules.notification.repository;

import com.magictech.modules.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for Notification entity
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Find all unread notifications
     */
    List<Notification> findByIsReadFalse();

    /**
     * Find notifications for a specific user
     */
    List<Notification> findByTargetUser(String targetUser);

    /**
     * Find unread notifications for a specific user
     */
    List<Notification> findByTargetUserAndIsReadFalse(String targetUser);

    /**
     * Find notifications by type
     */
    List<Notification> findByType(String type);

    /**
     * Find notifications by entity type and ID
     */
    List<Notification> findByEntityTypeAndEntityId(String entityType, Long entityId);

    /**
     * Find recent notifications (within last N days)
     */
    @Query("SELECT n FROM Notification n WHERE n.createdAt >= :sinceDate ORDER BY n.createdAt DESC")
    List<Notification> findRecentNotifications(@Param("sinceDate") LocalDateTime sinceDate);

    /**
     * Find all notifications ordered by creation date (most recent first)
     */
    List<Notification> findAllByOrderByCreatedAtDesc();

    /**
     * Count unread notifications
     */
    long countByIsReadFalse();

    /**
     * Count unread notifications for a specific user
     */
    long countByTargetUserAndIsReadFalse(String targetUser);
}
