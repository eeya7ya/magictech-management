package com.magictech.core.messaging.service;

import com.magictech.core.auth.User;
import com.magictech.core.messaging.constants.NotificationConstants;
import com.magictech.core.messaging.entity.DeviceRegistration;
import com.magictech.core.messaging.repository.DeviceRegistrationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing device registrations.
 * Handles device registration, heartbeat monitoring, and status tracking.
 */
@Service
@Transactional
public class DeviceRegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(DeviceRegistrationService.class);

    @Autowired
    private DeviceRegistrationRepository deviceRepository;

    @Value("${magictech.device.heartbeat.timeout:300}") // 5 minutes default
    private int heartbeatTimeoutSeconds;

    private String currentDeviceId;

    private LocalDateTime previousLastSeen;

    /**
     * Register or update a device.
     * Captures the previous lastSeen timestamp before updating (accessible via getPreviousLastSeen()).
     * IMPORTANT: Tracks lastSeen per-USER, not per-device, to handle multi-user devices correctly.
     */
    public DeviceRegistration registerDevice(User user, String moduleType) {
        try {
            // Generate or retrieve device ID
            String deviceId = getOrCreateDeviceId();

            // Check if device already exists
            Optional<DeviceRegistration> existing = deviceRepository.findByDeviceIdAndActiveTrue(deviceId);

            // CRITICAL FIX: Capture the CURRENT USER's previous lastSeen (not the device's lastSeen)
            // This allows multiple users to share the same device without inheriting each other's timestamps
            List<DeviceRegistration> userPreviousSessions = deviceRepository.findByUsernameOrderByLastSeenDesc(user.getUsername());
            if (!userPreviousSessions.isEmpty()) {
                previousLastSeen = userPreviousSessions.get(0).getLastSeen();
                logger.debug("Found previous session for user {}: lastSeen = {}", user.getUsername(), previousLastSeen);
            } else {
                previousLastSeen = null;
                logger.debug("No previous session found for user {}", user.getUsername());
            }

            DeviceRegistration device;
            if (existing.isPresent()) {
                // Update existing device
                device = existing.get();
                device.setUserId(user.getId());
                device.setUsername(user.getUsername());
                device.setModuleType(moduleType);
                device.setStatus(NotificationConstants.DEVICE_STATUS_ONLINE);
                device.setLastHeartbeat(LocalDateTime.now());
            } else {
                // Create new device
                device = new DeviceRegistration(deviceId, moduleType, user.getId(), user.getUsername());

                // Set device info
                try {
                    InetAddress localhost = InetAddress.getLocalHost();
                    device.setIpAddress(localhost.getHostAddress());
                    device.setHostname(localhost.getHostName());
                } catch (Exception e) {
                    logger.warn("Could not get device network info: {}", e.getMessage());
                }

                device.setDeviceName(String.format("%s - %s", user.getUsername(), moduleType));
                device.setApplicationVersion("1.0-SNAPSHOT");
            }

            device = deviceRepository.save(device);
            currentDeviceId = deviceId;

            logger.info("Registered device {} for user {} in module {} (user's previous lastSeen: {})",
                deviceId, user.getUsername(), moduleType, previousLastSeen);

            return device;

        } catch (Exception e) {
            logger.error("Error registering device: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to register device", e);
        }
    }

    /**
     * Send heartbeat for current device.
     */
    public void sendHeartbeat() {
        if (currentDeviceId == null) {
            logger.warn("No device registered, skipping heartbeat");
            return;
        }

        try {
            Optional<DeviceRegistration> device = deviceRepository.findByDeviceIdAndActiveTrue(currentDeviceId);

            if (device.isPresent()) {
                DeviceRegistration dev = device.get();
                dev.setLastHeartbeat(LocalDateTime.now());
                dev.setStatus(NotificationConstants.DEVICE_STATUS_ONLINE);
                deviceRepository.save(dev);

                logger.debug("Sent heartbeat for device {}", currentDeviceId);
            }
        } catch (Exception e) {
            logger.error("Error sending heartbeat: {}", e.getMessage(), e);
        }
    }

    /**
     * Mark device as offline.
     */
    public void setOffline(String deviceId) {
        try {
            Optional<DeviceRegistration> device = deviceRepository.findByDeviceIdAndActiveTrue(deviceId);

            if (device.isPresent()) {
                DeviceRegistration dev = device.get();
                dev.setStatus(NotificationConstants.DEVICE_STATUS_OFFLINE);
                deviceRepository.save(dev);

                logger.info("Device {} marked as offline", deviceId);
            }
        } catch (Exception e) {
            logger.error("Error setting device offline: {}", e.getMessage(), e);
        }
    }

    /**
     * Deactivate current device (logout).
     */
    public void deactivateCurrentDevice() {
        if (currentDeviceId != null) {
            setOffline(currentDeviceId);
        }
    }

    /**
     * Get all online devices for a module.
     */
    public List<DeviceRegistration> getOnlineDevicesByModule(String moduleType) {
        return deviceRepository.findOnlineDevicesByModule(moduleType);
    }

    /**
     * Get all online devices.
     */
    public List<DeviceRegistration> getAllOnlineDevices() {
        return deviceRepository.findOnlineDevices();
    }

    /**
     * Get device by ID.
     */
    public Optional<DeviceRegistration> getDeviceById(String deviceId) {
        return deviceRepository.findByDeviceIdAndActiveTrue(deviceId);
    }

    /**
     * Generate or retrieve device ID.
     * In a real application, this would be based on hardware identifiers.
     */
    private String getOrCreateDeviceId() {
        // For now, generate a UUID-based device ID
        // In production, you might want to use MAC address or other hardware ID
        if (currentDeviceId == null) {
            currentDeviceId = UUID.randomUUID().toString();
        }
        return currentDeviceId;
    }

    /**
     * Get current device ID.
     */
    public String getCurrentDeviceId() {
        return currentDeviceId;
    }

    /**
     * Get the last seen time of the current device.
     * Returns null if this is a first-time login.
     */
    public LocalDateTime getLastSeenTime() {
        if (currentDeviceId == null) {
            return null;
        }

        try {
            Optional<DeviceRegistration> device = deviceRepository.findByDeviceIdAndActiveTrue(currentDeviceId);
            return device.map(DeviceRegistration::getLastSeen).orElse(null);
        } catch (Exception e) {
            logger.error("Error getting last seen time: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get the previous last seen time for the CURRENT USER (before registerDevice() was called).
     * This is the timestamp that should be used to query for missed notifications.
     * Returns null if this is the user's first-time login.
     *
     * IMPORTANT: This returns the USER's previous lastSeen, not the device's.
     * This allows multiple users to share the same device without inheriting each other's timestamps.
     */
    public LocalDateTime getPreviousLastSeen() {
        return previousLastSeen;
    }

    /**
     * Scheduled task to check for stale devices (missed heartbeats).
     * Runs every minute.
     */
    @Scheduled(fixedRate = 60000) // Every 60 seconds
    public void checkStaleDevices() {
        try {
            LocalDateTime threshold = LocalDateTime.now().minusSeconds(heartbeatTimeoutSeconds);
            List<DeviceRegistration> staleDevices = deviceRepository.findStaleDevices(threshold);

            for (DeviceRegistration device : staleDevices) {
                device.setStatus(NotificationConstants.DEVICE_STATUS_OFFLINE);
                deviceRepository.save(device);

                logger.info("Device {} marked as offline due to missed heartbeat", device.getDeviceId());
            }

            if (!staleDevices.isEmpty()) {
                logger.info("Marked {} devices as offline due to missed heartbeats", staleDevices.size());
            }

        } catch (Exception e) {
            logger.error("Error checking stale devices: {}", e.getMessage(), e);
        }
    }

    /**
     * Count online devices by module.
     */
    public long countOnlineDevicesByModule(String moduleType) {
        return deviceRepository.countOnlineDevicesByModule(moduleType);
    }
}
