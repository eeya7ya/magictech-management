package com.magictech.core.messaging.repository;

import com.magictech.core.messaging.entity.NotificationUserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing NotificationUserStatus entities.
 * Tracks which users have seen which notifications.
 */
@Repository
public interface NotificationUserStatusRepository extends JpaRepository<NotificationUserStatus, Long> {

    /**
     * Check if a specific user has seen a specific notification.
     */
    boolean existsByNotificationIdAndUsernameAndActiveTrue(Long notificationId, String username);

    /**
     * Find status record for a specific notification and user.
     */
    Optional<NotificationUserStatus> findByNotificationIdAndUsernameAndActiveTrue(Long notificationId, String username);

    /**
     * Find all notifications seen by a specific user.
     */
    List<NotificationUserStatus> findByUsernameAndActiveTrue(String username);

    /**
     * Find all users who have seen a specific notification.
     */
    List<NotificationUserStatus> findByNotificationIdAndActiveTrue(Long notificationId);
}
