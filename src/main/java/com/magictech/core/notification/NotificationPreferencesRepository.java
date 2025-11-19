package com.magictech.core.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationPreferencesRepository extends JpaRepository<NotificationPreferences, Long> {

    /**
     * Find notification preferences for a specific user
     */
    Optional<NotificationPreferences> findByUserId(Long userId);

    /**
     * Check if preferences exist for a user
     */
    boolean existsByUserId(Long userId);

    /**
     * Delete preferences for a user
     */
    void deleteByUserId(Long userId);
}
