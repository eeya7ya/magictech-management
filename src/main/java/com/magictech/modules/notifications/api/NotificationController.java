package com.magictech.modules.notifications.api;

import com.magictech.modules.notifications.entity.Notification;
import com.magictech.modules.notifications.entity.NotificationPriority;
import com.magictech.modules.notifications.entity.NotificationType;
import com.magictech.modules.notifications.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for notification management
 */
@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    @Autowired
    @Qualifier("moduleNotificationService")
    private NotificationService notificationService;

    /**
     * GET /api/notifications/user/{userId}
     * Get all notifications for a user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notification>> getUserNotifications(@PathVariable Long userId) {
        try {
            List<Notification> notifications = notificationService.getUserNotifications(userId);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/notifications/user/{userId}/unread
     * Get unread notifications for a user
     */
    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications(@PathVariable Long userId) {
        try {
            List<Notification> notifications = notificationService.getUnreadNotifications(userId);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/notifications/user/{userId}/count
     * Get unread notification count for a user
     */
    @GetMapping("/user/{userId}/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable Long userId) {
        try {
            Long count = notificationService.countUnread(userId);
            Map<String, Long> response = new HashMap<>();
            response.put("count", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/notifications/user/{userId}/recent?days=7
     * Get recent notifications (default last 7 days)
     */
    @GetMapping("/user/{userId}/recent")
    public ResponseEntity<List<Notification>> getRecentNotifications(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "7") int days) {
        try {
            List<Notification> notifications = notificationService.getRecentNotifications(userId, days);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /api/notifications
     * Create a new notification
     */
    @PostMapping
    public ResponseEntity<Notification> createNotification(@RequestBody NotificationRequest request) {
        try {
            Notification notification = notificationService.createNotification(
                request.getUserId(),
                request.getTitle(),
                request.getMessage(),
                request.getType() != null ? request.getType() : NotificationType.INFO,
                request.getPriority() != null ? request.getPriority() : NotificationPriority.NORMAL,
                request.getModuleSource(),
                request.getReferenceId(),
                request.getReferenceType(),
                request.getActionUrl()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(notification);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * PUT /api/notifications/{id}/read
     * Mark notification as read
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Map<String, String>> markAsRead(@PathVariable Long id) {
        try {
            notificationService.markAsRead(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Notification marked as read");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * PUT /api/notifications/user/{userId}/read-all
     * Mark all notifications as read for a user
     */
    @PutMapping("/user/{userId}/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead(@PathVariable Long userId) {
        try {
            notificationService.markAllAsRead(userId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "All notifications marked as read");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * DELETE /api/notifications/{id}
     * Delete a notification (soft delete)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteNotification(@PathVariable Long id) {
        try {
            notificationService.deleteNotification(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Notification deleted");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * DELETE /api/notifications/user/{userId}
     * Delete all notifications for a user
     */
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Map<String, String>> deleteAllForUser(@PathVariable Long userId) {
        try {
            notificationService.deleteAllForUser(userId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "All notifications deleted for user");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Request DTO for creating notifications
     */
    public static class NotificationRequest {
        private Long userId;
        private String title;
        private String message;
        private NotificationType type;
        private NotificationPriority priority;
        private String moduleSource;
        private Long referenceId;
        private String referenceType;
        private String actionUrl;

        // Getters and setters
        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public NotificationType getType() {
            return type;
        }

        public void setType(NotificationType type) {
            this.type = type;
        }

        public NotificationPriority getPriority() {
            return priority;
        }

        public void setPriority(NotificationPriority priority) {
            this.priority = priority;
        }

        public String getModuleSource() {
            return moduleSource;
        }

        public void setModuleSource(String moduleSource) {
            this.moduleSource = moduleSource;
        }

        public Long getReferenceId() {
            return referenceId;
        }

        public void setReferenceId(Long referenceId) {
            this.referenceId = referenceId;
        }

        public String getReferenceType() {
            return referenceType;
        }

        public void setReferenceType(String referenceType) {
            this.referenceType = referenceType;
        }

        public String getActionUrl() {
            return actionUrl;
        }

        public void setActionUrl(String actionUrl) {
            this.actionUrl = actionUrl;
        }
    }
}
