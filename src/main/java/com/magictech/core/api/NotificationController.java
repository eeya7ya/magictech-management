package com.magictech.core.api;

import com.magictech.core.notification.Notification;
import com.magictech.core.notification.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API controller for managing notifications
 */
@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    /**
     * Get all notifications for a user
     */
    @GetMapping("/user/{userId}/role/{userRole}")
    public ResponseEntity<List<Notification>> getAllNotifications(
            @PathVariable Long userId,
            @PathVariable String userRole) {
        try {
            List<Notification> notifications = notificationService.getAllNotifications(userId, userRole);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get unread notifications count
     */
    @GetMapping("/user/{userId}/role/{userRole}/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @PathVariable Long userId,
            @PathVariable String userRole) {
        try {
            long count = notificationService.getUnreadCount(userId, userRole);
            Map<String, Long> response = new HashMap<>();
            response.put("count", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create a test notification for a specific role
     */
    @PostMapping("/test/role/{targetRole}")
    public ResponseEntity<Notification> createTestNotification(@PathVariable String targetRole) {
        try {
            Notification notification = notificationService.createRoleNotification(
                    targetRole,
                    "SYSTEM",
                    "TEST_NOTIFICATION",
                    "🔔 Test Notification",
                    "This is a test notification to verify the notification system is working correctly. " +
                    "You should see this popup slide in from the top-right corner.",
                    "System"
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(notification);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create a notification for a specific user
     */
    @PostMapping("/user/{userId}")
    public ResponseEntity<Notification> createUserNotification(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request) {
        try {
            String module = request.getOrDefault("module", "SYSTEM");
            String type = request.getOrDefault("type", "INFO");
            String title = request.get("title");
            String message = request.get("message");
            String createdBy = request.getOrDefault("createdBy", "System");

            if (title == null || message == null) {
                return ResponseEntity.badRequest().build();
            }

            Notification notification = notificationService.createUserNotification(
                    userId, module, type, title, message, createdBy
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(notification);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create a notification for all users with a specific role
     */
    @PostMapping("/role/{targetRole}")
    public ResponseEntity<Notification> createRoleNotification(
            @PathVariable String targetRole,
            @RequestBody Map<String, String> request) {
        try {
            String module = request.getOrDefault("module", "SYSTEM");
            String type = request.getOrDefault("type", "INFO");
            String title = request.get("title");
            String message = request.get("message");
            String priority = request.getOrDefault("priority", "NORMAL");
            String createdBy = request.getOrDefault("createdBy", "System");

            if (title == null || message == null) {
                return ResponseEntity.badRequest().build();
            }

            Notification notification = notificationService.createNotificationWithRelation(
                    targetRole, module, type, title, message, null, null, priority, createdBy
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(notification);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Mark notification as read
     */
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId) {
        try {
            notificationService.markAsRead(notificationId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Mark all notifications as read for a user
     */
    @PutMapping("/user/{userId}/role/{userRole}/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @PathVariable Long userId,
            @PathVariable String userRole) {
        try {
            notificationService.markAllAsRead(userId, userRole);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete a notification
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long notificationId) {
        try {
            notificationService.deleteNotification(notificationId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        response.put("service", "notification-service");
        return ResponseEntity.ok(response);
    }
}
