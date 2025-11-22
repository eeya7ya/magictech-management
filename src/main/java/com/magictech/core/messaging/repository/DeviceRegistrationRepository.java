package com.magictech.core.messaging.repository;

import com.magictech.core.messaging.entity.DeviceRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing DeviceRegistration entities.
 */
@Repository
public interface DeviceRegistrationRepository extends JpaRepository<DeviceRegistration, Long> {

    /**
     * Find device by device ID.
     */
    Optional<DeviceRegistration> findByDeviceIdAndActiveTrue(String deviceId);

    /**
     * Find all active devices.
     */
    List<DeviceRegistration> findByActiveTrue();

    /**
     * Find devices by module type.
     */
    List<DeviceRegistration> findByModuleTypeAndActiveTrue(String moduleType);

    /**
     * Find devices by status.
     */
    List<DeviceRegistration> findByStatusAndActiveTrue(String status);

    /**
     * Find online devices.
     */
    @Query("SELECT d FROM DeviceRegistration d WHERE d.active = true AND d.status = 'ONLINE'")
    List<DeviceRegistration> findOnlineDevices();

    /**
     * Find online devices for a specific module.
     */
    @Query("SELECT d FROM DeviceRegistration d WHERE d.active = true AND " +
           "d.status = 'ONLINE' AND d.moduleType = :moduleType")
    List<DeviceRegistration> findOnlineDevicesByModule(@Param("moduleType") String moduleType);

    /**
     * Find devices by user ID.
     */
    List<DeviceRegistration> findByUserIdAndActiveTrue(Long userId);

    /**
     * Find devices that haven't sent heartbeat recently (considered offline).
     */
    @Query("SELECT d FROM DeviceRegistration d WHERE d.active = true AND " +
           "d.status = 'ONLINE' AND d.lastHeartbeat < :threshold")
    List<DeviceRegistration> findStaleDevices(@Param("threshold") LocalDateTime threshold);

    /**
     * Count online devices by module.
     */
    @Query("SELECT COUNT(d) FROM DeviceRegistration d WHERE d.active = true AND " +
           "d.status = 'ONLINE' AND d.moduleType = :moduleType")
    long countOnlineDevicesByModule(@Param("moduleType") String moduleType);

    /**
     * Check if a device exists and is active.
     */
    boolean existsByDeviceIdAndActiveTrue(String deviceId);

    /**
     * Find the most recent device registration for a specific username.
     * Used to get the user's last logout time regardless of which device they used.
     */
    @Query("SELECT d FROM DeviceRegistration d WHERE d.active = true AND " +
           "d.username = :username AND d.lastSeen IS NOT NULL " +
           "ORDER BY d.lastSeen DESC")
    List<DeviceRegistration> findByUsernameOrderByLastSeenDesc(@Param("username") String username);
}
