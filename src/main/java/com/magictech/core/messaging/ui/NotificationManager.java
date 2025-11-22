package com.magictech.core.messaging.ui;

import com.magictech.core.auth.User;
import com.magictech.core.messaging.constants.NotificationConstants;
import com.magictech.core.messaging.dto.NotificationMessage;
import com.magictech.core.messaging.entity.Notification;
import com.magictech.core.messaging.service.DeviceRegistrationService;
import com.magictech.core.messaging.service.NotificationListenerService;
import com.magictech.core.messaging.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
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

    @Autowired
    private NotificationService notificationService;

    private ScheduledExecutorService heartbeatScheduler;
    private Queue<NotificationPopup> activePopups = new ConcurrentLinkedQueue<>();
    private boolean isInitialized = false;
    private User currentUser; // Track current user for role-based filtering

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
            this.currentUser = user; // Store current user
            logger.info("Initializing NotificationManager for user {} (role: {}) in module {}",
                user.getUsername(), user.getRole(), moduleType);

            // Register device
            deviceService.registerDevice(user, moduleType);
            logger.info("Device registered successfully");

            // Subscribe to appropriate channels based on module type
            subscribeToChannels(moduleType);

            // Register notification listener to show popups
            listenerService.addListener(this::handleNotification);

            // Load and display missed notifications (last 7 days)
            loadMissedNotifications(moduleType);

            // Start heartbeat scheduler
            startHeartbeat();

            isInitialized = true;
            logger.info("NotificationManager initialized successfully");

        } catch (Exception e) {
            logger.error("Failed to initialize NotificationManager: {}", e.getMessage(), e);
        }
    }

    /**
     * Subscribe to appropriate Redis channels based on module type and user role.
     * MASTER users see everything from all 5 modules.
     */
    private void subscribeToChannels(String moduleType) {
        try {
            // Wait a bit for listener service to be fully ready
            Thread.sleep(1000);

            // MASTER users subscribe to ALL channels (see everything from all modules)
            if (currentUser.getRole() == com.magictech.core.auth.UserRole.MASTER) {
                listenerService.subscribeToAll();
                logger.info("Subscribed to all notification channels (MASTER user sees everything)");
            }
            // Storage module also subscribes to ALL channels
            else if (NotificationConstants.MODULE_STORAGE.equalsIgnoreCase(moduleType)) {
                listenerService.subscribeToAll();
                logger.info("Subscribed to all notification channels (Storage module)");
            }
            // Other modules subscribe only to their specific channels
            else {
                listenerService.subscribeToModule(moduleType.toLowerCase());
                logger.info("Subscribed to {} module notifications", moduleType);
            }
        } catch (Exception e) {
            logger.error("Error subscribing to channels: {}", e.getMessage(), e);
        }
    }

    /**
     * Load and display missed notifications from database.
     * - Regular notifications: Only show since last logout
     * - Approval notifications: Show ALL unresolved approvals (for authorized users)
     */
    private void loadMissedNotifications(String moduleType) {
        try {
            // FIRST: Load unresolved approval notifications if user is authorized
            // These persist across logins until someone resolves them
            // NOTE: We DO NOT filter by sender for approvals - all authorized users must see them
            if (isAuthorizedForApprovals()) {
                List<Notification> unresolvedApprovals =
                    notificationService.getUnresolvedApprovalNotifications("APPROVAL_REQUESTED");

                logger.info("Found {} unresolved approval notifications for user {} (role: {})",
                    unresolvedApprovals.size(), currentUser.getUsername(), currentUser.getRole());

                for (Notification notification : unresolvedApprovals) {
                    logger.info("Showing approval notification: {} (entityId: {}, createdBy: {})",
                        notification.getTitle(), notification.getEntityId(), notification.getCreatedBy());

                    NotificationMessage message = convertToMessage(notification);
                    handleNotification(message);
                    Thread.sleep(300);
                }
            } else {
                logger.info("User {} (role: {}) is NOT authorized to see approval notifications",
                    currentUser.getUsername(), currentUser.getRole());
            }

            // SECOND: Load regular missed notifications since last logout
            java.time.LocalDateTime lastSeen = deviceService.getPreviousLastSeen();

            if (lastSeen == null) {
                // First time login - load notifications from the last 7 days
                // This ensures new users see recent important notifications
                lastSeen = java.time.LocalDateTime.now().minusDays(7);
                logger.info("First login detected, loading notifications from the last 7 days (since {})", lastSeen);
            }

            // Get missed notifications since last logout (or last 7 days for first login)
            List<Notification> missedNotifications;

            // MASTER users get ALL missed notifications from all 5 modules
            if (currentUser.getRole() == com.magictech.core.auth.UserRole.MASTER) {
                missedNotifications = notificationService.getMissedNotifications(null, lastSeen);
                logger.info("Loading ALL missed notifications for MASTER user");
            }
            // Storage module gets ALL missed notifications
            else if (NotificationConstants.MODULE_STORAGE.equalsIgnoreCase(moduleType)) {
                missedNotifications = notificationService.getMissedNotifications(null, lastSeen);
            }
            // Other modules get only their module's missed notifications
            else {
                missedNotifications = notificationService.getMissedNotificationsByModule(moduleType.toLowerCase(), lastSeen);
            }

            logger.info("Found {} regular missed notifications since {} for module {}",
                missedNotifications.size(), lastSeen, moduleType);

            // Get current device ID to filter out sender's own notifications
            String currentDeviceId = deviceService.getCurrentDeviceId();
            logger.debug("Current device ID: {}", currentDeviceId);

            // Display each notification (excluding approval requests - already shown above)
            for (Notification notification : missedNotifications) {
                // Skip approval requests - already handled above
                if ("APPROVAL_REQUESTED".equals(notification.getAction())) {
                    continue;
                }

                // IMPORTANT: Filter out notifications created by this user's device
                // This implements the excludeSender behavior for missed notifications
                // (the real-time listener already does this for live notifications)
                String sourceDeviceId = notification.getSourceDeviceId();
                if (currentDeviceId != null && sourceDeviceId != null &&
                    currentDeviceId.equals(sourceDeviceId)) {
                    logger.debug("Skipping notification '{}' - created by current device (excludeSender)",
                        notification.getTitle());
                    continue; // Don't show user their own notifications
                }

                NotificationMessage message = convertToMessage(notification);
                handleNotification(message);
                Thread.sleep(300);
            }

        } catch (Exception e) {
            logger.error("Error loading missed notifications: {}", e.getMessage(), e);
        }
    }

    /**
     * Convert database Notification entity to NotificationMessage DTO.
     */
    private NotificationMessage convertToMessage(Notification notification) {
        return new NotificationMessage.Builder()
            .notificationId(notification.getId()) // IMPORTANT: Include notification ID for marking as resolved
            .type(notification.getType())
            .module(notification.getModule())
            .action(notification.getAction())
            .entityType(notification.getEntityType())
            .entityId(notification.getEntityId())
            .title(notification.getTitle())
            .message(notification.getMessage())
            .targetModule(notification.getTargetModule())
            .priority(notification.getPriority())
            .createdBy(notification.getCreatedBy())
            .sourceDeviceId(notification.getSourceDeviceId())
            .metadata(notification.getMetadata())
            .build();
    }

    /**
     * Handle incoming notification by showing a popup.
     * Filters approval notifications based on user role (SALES, STORAGE, MASTER only).
     * Popups stack vertically to avoid overlapping.
     */
    private void handleNotification(NotificationMessage message) {
        try {
            logger.info("Received notification: {} (action: {})", message.getTitle(), message.getAction());

            // Filter approval notifications - only show to authorized users
            if ("APPROVAL_REQUESTED".equals(message.getAction())) {
                if (!isAuthorizedForApprovals()) {
                    logger.info("User {} (role: {}) is not authorized to see approval notifications - skipping",
                        currentUser.getUsername(), currentUser.getRole());
                    return;
                }
                logger.info("User {} (role: {}) is authorized to see approval notifications",
                    currentUser.getUsername(), currentUser.getRole());
            }

            // Calculate stack index based on currently active popups
            // This ensures new popups appear above existing ones instead of overlapping
            int stackIndex = activePopups.size();
            logger.debug("Creating popup at stack index {} (currently {} active popups)", stackIndex, activePopups.size());

            // Create and show popup at calculated stack position
            NotificationPopup popup = new NotificationPopup();
            popup.show(message, currentUser.getUsername(), stackIndex); // Pass stack index for proper positioning
            activePopups.add(popup);

            // Remove from active popups after it auto-dismisses
            schedulePopupRemoval(popup);

        } catch (Exception e) {
            logger.error("Error handling notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Check if current user is authorized to see approval notifications.
     * Only SALES, STORAGE, and MASTER users can see approval requests.
     */
    private boolean isAuthorizedForApprovals() {
        if (currentUser == null || currentUser.getRole() == null) {
            return false;
        }

        return switch (currentUser.getRole()) {
            case MASTER -> true;   // Master has full access
            case SALES -> true;    // Sales can approve project elements
            case STORAGE -> true;  // Storage management can approve
            default -> false;      // All other roles cannot see approvals
        };
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
     * Dismisses all active popups including persistent approval notifications.
     */
    public void cleanup() {
        if (!isInitialized) {
            return;
        }

        try {
            logger.info("Cleaning up NotificationManager - dismissing all active popups");

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

            // IMPORTANT: Dismiss all active popups (including persistent approval notifications)
            // This ensures approval notifications don't remain visible after logout
            logger.info("Dismissing {} active popup(s)", activePopups.size());
            for (NotificationPopup popup : activePopups) {
                try {
                    if (popup.isShowing()) {
                        popup.dismissImmediately(); // Use immediate dismiss to ensure popups close on logout
                        logger.debug("Dismissed popup immediately");
                    }
                } catch (Exception e) {
                    logger.warn("Error dismissing popup: {}", e.getMessage());
                }
            }
            activePopups.clear();

            // Unsubscribe from all channels
            listenerService.unsubscribeAll();

            // Mark device as offline
            deviceService.deactivateCurrentDevice();

            // Clear user context
            currentUser = null;

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
