package com.magictech.modules.notifications.repository;

import com.magictech.modules.notifications.entity.Notification;
import com.magictech.modules.notifications.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Find all active notifications for a user
    List<Notification> findByUserIdAndActiveTrueOrderByCreatedAtDesc(Long userId);

    // Find unread notifications for a user
    List<Notification> findByUserIdAndIsReadFalseAndActiveTrueOrderByCreatedAtDesc(Long userId);

    // Count unread notifications
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.isRead = false AND n.active = true")
    Long countUnreadByUserId(@Param("userId") Long userId);

    // Find notifications by type
    List<Notification> findByUserIdAndTypeAndActiveTrueOrderByCreatedAtDesc(Long userId, NotificationType type);

    // Find recent notifications (last N days)
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.active = true " +
           "AND n.createdAt >= :since ORDER BY n.createdAt DESC")
    List<Notification> findRecentByUserId(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    // Find unsent email notifications
    @Query("SELECT n FROM Notification n WHERE n.isSentEmail = false AND n.active = true " +
           "AND n.priority IN ('HIGH', 'URGENT')")
    List<Notification> findUnsentEmailNotifications();

    // Find unsent desktop notifications
    @Query("SELECT n FROM Notification n WHERE n.isSentDesktop = false AND n.active = true")
    List<Notification> findUnsentDesktopNotifications();

    // Mark all as read for a user
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt " +
           "WHERE n.userId = :userId AND n.isRead = false AND n.active = true")
    void markAllAsReadByUserId(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);

    // Delete old notifications (cleanup)
    @Query("DELETE FROM Notification n WHERE n.createdAt < :before OR " +
           "(n.expiresAt IS NOT NULL AND n.expiresAt < :now)")
    void deleteOldNotifications(@Param("before") LocalDateTime before, @Param("now") LocalDateTime now);
}
