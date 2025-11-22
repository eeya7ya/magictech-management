package com.magictech.core.messaging.repository;

import com.magictech.core.messaging.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for managing Notification entities.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Find all active notifications.
     */
    List<Notification> findByActiveTrue();

    /**
     * Find unread notifications for a specific device.
     */
    List<Notification> findByTargetDeviceIdAndReadStatusFalseAndActiveTrue(String targetDeviceId);

    /**
     * Find unread notifications for a specific module.
     */
    List<Notification> findByTargetModuleAndReadStatusFalseAndActiveTrue(String targetModule);

    /**
     * Find notifications by module and active status.
     */
    List<Notification> findByModuleAndActiveTrue(String module);

    /**
     * Find unread notifications created after a specific timestamp.
     * Used for catching up on missed notifications (excludes already-read ones).
     */
    List<Notification> findByTimestampAfterAndActiveTrueAndReadStatusFalse(LocalDateTime timestamp);

    /**
     * Find ALL notifications created after a specific timestamp.
     * Used for viewing recent notification history (includes already-read ones).
     */
    List<Notification> findByTimestampAfterAndActiveTrue(LocalDateTime timestamp);

    /**
     * Find notifications for a specific device created after a timestamp.
     */
    @Query("SELECT n FROM Notification n WHERE n.active = true AND " +
           "(n.targetDeviceId = :deviceId OR n.targetDeviceId IS NULL) AND " +
           "n.timestamp > :timestamp ORDER BY n.timestamp DESC")
    List<Notification> findMissedNotifications(@Param("deviceId") String deviceId,
                                               @Param("timestamp") LocalDateTime timestamp);

    /**
     * Find unread notifications for a specific module created after a timestamp.
     * Excludes notifications that have already been read to prevent duplicates.
     */
    @Query("SELECT n FROM Notification n WHERE n.active = true AND " +
           "n.readStatus = false AND " +
           "(n.targetModule = :module OR n.targetModule IS NULL OR n.targetModule = 'ALL') AND " +
           "n.timestamp > :timestamp ORDER BY n.timestamp DESC")
    List<Notification> findMissedNotificationsByModule(@Param("module") String module,
                                                        @Param("timestamp") LocalDateTime timestamp);

    /**
     * Find recent notifications for a specific module (last N days).
     */
    @Query("SELECT n FROM Notification n WHERE n.active = true AND " +
           "n.targetModule = :module AND " +
           "n.timestamp > :fromDate ORDER BY n.timestamp DESC")
    List<Notification> findRecentNotificationsByModule(@Param("module") String module,
                                                        @Param("fromDate") LocalDateTime fromDate);

    /**
     * Count unread notifications for a device.
     */
    long countByTargetDeviceIdAndReadStatusFalseAndActiveTrue(String targetDeviceId);

    /**
     * Count unread notifications for a module.
     */
    long countByTargetModuleAndReadStatusFalseAndActiveTrue(String targetModule);

    /**
     * Find all broadcast notifications (no specific target).
     */
    @Query("SELECT n FROM Notification n WHERE n.active = true AND " +
           "n.targetDeviceId IS NULL AND " +
           "(n.targetModule IS NULL OR n.targetModule = 'ALL') " +
           "ORDER BY n.timestamp DESC")
    List<Notification> findBroadcastNotifications();

    /**
     * Find unresolved approval notifications by action type.
     * Used to show approval notifications to all authorized users until resolved.
     */
    List<Notification> findByActionAndResolvedFalseAndActiveTrue(String action);
}
