package com.magictech.core.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Notification entity
 */
@Repository
public interface CoreNotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Find unread notifications for a specific user
     */
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    /**
     * Find unread notifications for a role
     */
    List<Notification> findByTargetRoleAndIsReadFalseOrderByCreatedAtDesc(String targetRole);

    /**
     * Find all notifications for a user (read and unread)
     */
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find all notifications for a role
     */
    List<Notification> findByTargetRoleOrderByCreatedAtDesc(String targetRole);

    /**
     * Find notifications by module
     */
    List<Notification> findByModuleOrderByCreatedAtDesc(String module);

    /**
     * Find unshown notifications (for popup display)
     */
    @Query("SELECT n FROM Notification n WHERE n.isShown = false AND " +
           "(n.userId = :userId OR n.targetRole = :role) " +
           "ORDER BY n.createdAt DESC")
    List<Notification> findUnshownNotifications(@Param("userId") Long userId, @Param("role") String role);

    /**
     * Count unread notifications for user
     */
    long countByUserIdAndIsReadFalse(Long userId);

    /**
     * Count unread notifications for role
     */
    long countByTargetRoleAndIsReadFalse(String targetRole);

    /**
     * Delete notifications older than specified date (for 3-month cleanup)
     */
    void deleteByCreatedAtBefore(LocalDateTime cutoffDate);

    /**
     * Find notifications by related entity
     */
    List<Notification> findByRelatedIdAndRelatedType(Long relatedId, String relatedType);

    /**
     * Find notifications by type
     */
    List<Notification> findByTypeOrderByCreatedAtDesc(String type);

    /**
     * Get recent notifications (last N days)
     */
    @Query("SELECT n FROM Notification n WHERE " +
           "(n.userId = :userId OR n.targetRole = :role) AND " +
           "n.createdAt >= :since " +
           "ORDER BY n.createdAt DESC")
    List<Notification> findRecentNotifications(
            @Param("userId") Long userId,
            @Param("role") String role,
            @Param("since") LocalDateTime since
    );

    /**
     * Count unread notifications for a specific module and user
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE " +
           "(n.userId = :userId OR n.targetRole = :role) AND " +
           "n.isRead = false AND n.module = :module")
    long countUnreadByModule(
            @Param("userId") Long userId,
            @Param("role") String role,
            @Param("module") String module
    );
}
