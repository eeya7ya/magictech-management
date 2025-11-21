package com.magictech.core.messaging.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magictech.core.messaging.constants.NotificationConstants;
import com.magictech.core.messaging.dto.NotificationMessage;
import com.magictech.core.messaging.entity.Notification;
import com.magictech.core.messaging.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for publishing notifications to Redis and storing them in PostgreSQL.
 */
@Service
@Transactional
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DeviceRegistrationService deviceRegistrationService;

    /**
     * Publish a notification message to Redis and store in database.
     *
     * @param message The notification message to publish
     */
    public void publishNotification(NotificationMessage message) {
        try {
            // Set source device ID if not already set
            if (message.getSourceDeviceId() == null) {
                String currentDeviceId = deviceRegistrationService.getCurrentDeviceId();
                if (currentDeviceId != null) {
                    message.setSourceDeviceId(currentDeviceId);
                }
            }

            // Store notification in database first
            Notification notification = saveNotificationToDatabase(message);

            // Determine which channel(s) to publish to
            String targetModule = message.getTargetModule();

            if (targetModule != null && !targetModule.isEmpty()) {
                // Publish to specific module channel
                String moduleChannel = NotificationConstants.getModuleChannel(targetModule);
                publishToChannel(moduleChannel, message);
            } else {
                // Publish to all_notifications channel (broadcast)
                publishToChannel(NotificationConstants.CHANNEL_ALL_NOTIFICATIONS, message);
            }

            // Also publish to action-specific channel if action and entity type are provided
            if (message.getAction() != null && message.getEntityType() != null) {
                String actionChannel = NotificationConstants.getActionChannel(
                    message.getModule(),
                    message.getAction(),
                    message.getEntityType()
                );
                publishToChannel(actionChannel, message);
            }

            logger.info("Published notification: {} to module: {}", message.getTitle(), targetModule);

        } catch (Exception e) {
            logger.error("Error publishing notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish to a specific Redis channel.
     */
    private void publishToChannel(String channel, NotificationMessage message) {
        try {
            redisTemplate.convertAndSend(channel, message);
            logger.debug("Published to channel {}: {}", channel, message.getTitle());
        } catch (Exception e) {
            logger.error("Error publishing to channel {}: {}", channel, e.getMessage(), e);
        }
    }

    /**
     * Save notification to database.
     */
    private Notification saveNotificationToDatabase(NotificationMessage message) {
        Notification notification = new Notification();
        notification.setType(message.getType());
        notification.setModule(message.getModule());
        notification.setAction(message.getAction());
        notification.setEntityType(message.getEntityType());
        notification.setEntityId(message.getEntityId());
        notification.setTitle(message.getTitle());
        notification.setMessage(message.getMessage());
        notification.setTargetDeviceId(message.getTargetDeviceId());
        notification.setTargetModule(message.getTargetModule());
        notification.setPriority(message.getPriority() != null ?
            message.getPriority() : NotificationConstants.PRIORITY_MEDIUM);
        notification.setCreatedBy(message.getCreatedBy());
        notification.setSourceDeviceId(message.getSourceDeviceId());
        notification.setMetadata(message.getMetadata());

        return notificationRepository.save(notification);
    }

    /**
     * Send a project created notification from Sales to Projects module.
     */
    public void notifyProjectCreated(Long projectId, String projectName, String createdBy) {
        NotificationMessage message = new NotificationMessage.Builder()
            .type(NotificationConstants.TYPE_INFO)
            .module(NotificationConstants.MODULE_SALES)
            .action(NotificationConstants.ACTION_CREATED)
            .entityType(NotificationConstants.ENTITY_PROJECT)
            .entityId(projectId)
            .title("New Project Created")
            .message(String.format("A new project '%s' has been created from Sales module", projectName))
            .targetModule(NotificationConstants.MODULE_PROJECTS)
            .priority(NotificationConstants.PRIORITY_HIGH)
            .createdBy(createdBy)
            .excludeSender(true)  // Don't echo back to Sales - they already have instant feedback
            .build();

        publishNotification(message);
    }

    /**
     * Send a storage element approval request from Projects to Sales module.
     */
    public void notifyElementApprovalRequest(Long elementId, Long projectId, String projectName,
                                             String itemName, Integer quantity, String requestedBy) {
        NotificationMessage message = new NotificationMessage.Builder()
            .type(NotificationConstants.TYPE_WARNING)
            .module(NotificationConstants.MODULE_PROJECTS)
            .action("APPROVAL_REQUESTED")
            .entityType("PROJECT_ELEMENT")
            .entityId(elementId)
            .title("Storage Element Approval Required")
            .message(String.format("Project '%s' requests %d x %s - Approval needed",
                projectName, quantity, itemName))
            .targetModule(NotificationConstants.MODULE_SALES)
            .priority(NotificationConstants.PRIORITY_HIGH)
            .createdBy(requestedBy)
            .metadata(String.format("{\"projectId\":%d,\"elementId\":%d,\"quantity\":%d}",
                projectId, elementId, quantity))
            .build();

        publishNotification(message);
    }

    /**
     * Send a confirmation request notification from Projects to Sales module.
     */
    public void notifyConfirmationRequested(Long projectId, String projectName, String requestedBy) {
        NotificationMessage message = new NotificationMessage.Builder()
            .type(NotificationConstants.TYPE_WARNING)
            .module(NotificationConstants.MODULE_PROJECTS)
            .action(NotificationConstants.ACTION_CONFIRMATION_REQUESTED)
            .entityType(NotificationConstants.ENTITY_PROJECT)
            .entityId(projectId)
            .title("Confirmation Requested")
            .message(String.format("Project '%s' requires confirmation from Sales", projectName))
            .targetModule(NotificationConstants.MODULE_SALES)
            .priority(NotificationConstants.PRIORITY_URGENT)
            .createdBy(requestedBy)
            .build();

        publishNotification(message);
    }

    /**
     * Send a project completed notification to Storage and Pricing modules.
     */
    public void notifyProjectCompleted(Long projectId, String projectName, String completedBy) {
        // Notify Storage module
        NotificationMessage storageMessage = new NotificationMessage.Builder()
            .type(NotificationConstants.TYPE_SUCCESS)
            .module(NotificationConstants.MODULE_PROJECTS)
            .action(NotificationConstants.ACTION_COMPLETED)
            .entityType(NotificationConstants.ENTITY_PROJECT)
            .entityId(projectId)
            .title("Project Completed")
            .message(String.format("Project '%s' has been completed and is ready for analysis", projectName))
            .targetModule(NotificationConstants.MODULE_STORAGE)
            .priority(NotificationConstants.PRIORITY_HIGH)
            .createdBy(completedBy)
            .build();

        publishNotification(storageMessage);

        // Notify Pricing module
        NotificationMessage pricingMessage = new NotificationMessage.Builder()
            .type(NotificationConstants.TYPE_SUCCESS)
            .module(NotificationConstants.MODULE_PROJECTS)
            .action(NotificationConstants.ACTION_COMPLETED)
            .entityType(NotificationConstants.ENTITY_PROJECT)
            .entityId(projectId)
            .title("Project Completed")
            .message(String.format("Project '%s' has been completed and is ready for pricing analysis", projectName))
            .targetModule(NotificationConstants.MODULE_PRICING)
            .priority(NotificationConstants.PRIORITY_HIGH)
            .createdBy(completedBy)
            .build();

        publishNotification(pricingMessage);
    }

    /**
     * Mark notification as read.
     */
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setReadStatus(true);
            notificationRepository.save(notification);
        });
    }

    /**
     * Get missed notifications for a device since last connection.
     * If deviceId is null, returns ALL missed notifications (for storage/admin).
     */
    public java.util.List<Notification> getMissedNotifications(String deviceId, LocalDateTime lastSeen) {
        if (deviceId == null) {
            // Get all notifications after timestamp
            return notificationRepository.findByTimestampAfterAndActiveTrue(lastSeen);
        }
        return notificationRepository.findMissedNotifications(deviceId, lastSeen);
    }

    /**
     * Get missed notifications for a module since last connection.
     */
    public java.util.List<Notification> getMissedNotificationsByModule(String module, LocalDateTime lastSeen) {
        return notificationRepository.findMissedNotificationsByModule(module, lastSeen);
    }

    /**
     * Get recent notifications for a module.
     * If module is null, returns ALL recent notifications (for storage/admin).
     */
    public java.util.List<Notification> getRecentNotifications(String module, int days) {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(days);

        if (module == null || module.isEmpty()) {
            // Get all recent notifications
            return notificationRepository.findByTimestampAfterAndActiveTrue(fromDate);
        } else {
            // Get module-specific notifications
            return notificationRepository.findRecentNotificationsByModule(module, fromDate);
        }
    }

    /**
     * Mark a notification as resolved (for approval notifications).
     * This is called when someone approves/rejects an approval request.
     *
     * @param notificationId The notification ID
     * @param resolvedBy Username of the person resolving the notification
     */
    public void markAsResolved(Long notificationId, String resolvedBy) {
        try {
            notificationRepository.findById(notificationId).ifPresent(notification -> {
                notification.setResolved(true);
                notification.setResolvedBy(resolvedBy);
                notification.setResolvedAt(LocalDateTime.now());
                notificationRepository.save(notification);
                logger.info("Notification {} marked as resolved by {}", notificationId, resolvedBy);
            });
        } catch (Exception e) {
            logger.error("Error marking notification as resolved: {}", e.getMessage(), e);
        }
    }

    /**
     * Get unresolved approval notifications.
     * Used to show approval notifications to all authorized users until someone resolves them.
     *
     * @param action The action type (e.g., "APPROVAL_REQUESTED")
     * @return List of unresolved approval notifications
     */
    public java.util.List<Notification> getUnresolvedApprovalNotifications(String action) {
        return notificationRepository.findByActionAndResolvedFalseAndActiveTrue(action);
    }

    /**
     * Send a refresh notification to Projects module to update UI after approval/rejection.
     * This notification is NOT stored in database - it's just a real-time UI refresh trigger.
     */
    public void sendProjectsRefreshNotification(Long projectId, String message) {
        try {
            NotificationMessage refreshMessage = new NotificationMessage.Builder()
                .type("REFRESH")
                .module(NotificationConstants.MODULE_SALES)
                .action("REFRESH_PROJECTS")
                .entityType(NotificationConstants.ENTITY_PROJECT)
                .entityId(projectId)
                .title("Projects Updated")
                .message(message)
                .targetModule(NotificationConstants.MODULE_PROJECTS)
                .priority(NotificationConstants.PRIORITY_LOW)
                .build();

            // Publish directly to Redis without storing in database (this is just a UI refresh trigger)
            String channel = NotificationConstants.getChannelForModule(NotificationConstants.MODULE_PROJECTS);
            String messageJson = objectMapper.writeValueAsString(refreshMessage);
            redisTemplate.convertAndSend(channel, messageJson);

            logger.info("Sent refresh notification to Projects module for project {}", projectId);
        } catch (JsonProcessingException e) {
            logger.error("Failed to send refresh notification: {}", e.getMessage(), e);
        }
    }
}
