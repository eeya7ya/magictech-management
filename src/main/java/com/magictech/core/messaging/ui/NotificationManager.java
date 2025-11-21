package com.magictech.core.messaging.ui;

import com.magictech.core.auth.User;
import com.magictech.core.messaging.constants.NotificationConstants;
import com.magictech.core.messaging.dto.NotificationMessage;
import com.magictech.core.messaging.service.DeviceRegistrationService;
import com.magictech.core.messaging.service.NotificationListenerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages notification UI integration for JavaFX application.
 * Handles device registration, channel subscription, and popup display.
 */
@Component
public class NotificationManager {

    private static final Logger logger = LoggerFactory.getLogger(NotificationManager.class);

    @Autowired
    private NotificationListenerService listenerService;

    @Autowired
    private DeviceRegistrationService deviceService;

    private ScheduledExecutorService heartbeatScheduler;
    private Queue<NotificationPopup> activePopups = new ConcurrentLinkedQueue<>();
    private boolean isInitialized = false;

    /**
     * Initialize notification system for a logged-in user.
     *
     * @param user The logged-in user
     * @param moduleType The module type (sales, projects, storage, etc.)
     */
    public void initialize(User user, String moduleType) {
        if (isInitialized) {
            logger.warn("NotificationManager already initialized, skipping");
            return;
        }

        try {
            logger.info("Initializing NotificationManager for user {} in module {}", user.getUsername(), moduleType);

            // Register device
            deviceService.registerDevice(user, moduleType);
            logger.info("Device registered successfully");

            // Subscribe to appropriate channels based on module type
            subscribeToChannels(moduleType);

            // Register notification listener to show popups
            listenerService.addListener(this::handleNotification);

            // Start heartbeat scheduler
            startHeartbeat();

            isInitialized = true;
            logger.info("NotificationManager initialized successfully");

        } catch (Exception e) {
            logger.error("Failed to initialize NotificationManager: {}", e.getMessage(), e);
        }
    }

    /**
     * Subscribe to appropriate Redis channels based on module type.
     */
    private void subscribeToChannels(String moduleType) {
        try {
            // Wait a bit for listener service to be fully ready
            Thread.sleep(1000);

            if (NotificationConstants.MODULE_STORAGE.equalsIgnoreCase(moduleType)) {
                // Storage module subscribes to ALL channels
                listenerService.subscribeToAll();
                logger.info("Subscribed to all notification channels (Storage module)");
            } else {
                // Other modules subscribe only to their specific channels
                listenerService.subscribeToModule(moduleType.toLowerCase());
                logger.info("Subscribed to {} module notifications", moduleType);
            }
        } catch (Exception e) {
            logger.error("Error subscribing to channels: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle incoming notification by showing a popup.
     */
    private void handleNotification(NotificationMessage message) {
        try {
            logger.info("Received notification: {}", message.getTitle());

            // Create and show popup
            NotificationPopup popup = new NotificationPopup();
            popup.show(message);
            activePopups.add(popup);

            // Remove from active popups after it auto-dismisses
            schedulePopupRemoval(popup);

        } catch (Exception e) {
            logger.error("Error handling notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Schedule removal of popup from active list after it auto-dismisses.
     */
    private void schedulePopupRemoval(NotificationPopup popup) {
        new Thread(() -> {
            try {
                Thread.sleep(6000); // Wait slightly longer than auto-dismiss time
                activePopups.remove(popup);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * Start heartbeat scheduler to keep device status updated.
     */
    private void startHeartbeat() {
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "notification-heartbeat");
            thread.setDaemon(true);
            return thread;
        });

        // Send heartbeat every 60 seconds
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                deviceService.sendHeartbeat();
                logger.debug("Heartbeat sent successfully");
            } catch (Exception e) {
                logger.error("Error sending heartbeat: {}", e.getMessage(), e);
            }
        }, 60, 60, TimeUnit.SECONDS);

        logger.info("Heartbeat scheduler started");
    }

    /**
     * Cleanup notification system (call on logout or app shutdown).
     */
    public void cleanup() {
        if (!isInitialized) {
            return;
        }

        try {
            logger.info("Cleaning up NotificationManager");

            // Stop heartbeat scheduler
            if (heartbeatScheduler != null) {
                heartbeatScheduler.shutdown();
                try {
                    if (!heartbeatScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                        heartbeatScheduler.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    heartbeatScheduler.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            // Dismiss all active popups
            for (NotificationPopup popup : activePopups) {
                try {
                    popup.dismiss();
                } catch (Exception e) {
                    logger.warn("Error dismissing popup: {}", e.getMessage());
                }
            }
            activePopups.clear();

            // Unsubscribe from all channels
            listenerService.unsubscribeAll();

            // Mark device as offline
            deviceService.deactivateCurrentDevice();

            isInitialized = false;
            logger.info("NotificationManager cleaned up successfully");

        } catch (Exception e) {
            logger.error("Error during NotificationManager cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Check if notification manager is initialized.
     */
    public boolean isInitialized() {
        return isInitialized;
    }
}
