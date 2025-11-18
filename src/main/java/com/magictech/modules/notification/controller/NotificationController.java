package com.magictech.modules.notification.controller;

import com.magictech.modules.notification.entity.Notification;
import com.magictech.modules.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST API Controller for Notifications
 */
@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    /**
     * Get all notifications
     * GET /api/notifications
     */
    @GetMapping
    public ResponseEntity<List<Notification>> getAllNotifications() {
        List<Notification> notifications = notificationService.getAllNotifications();
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get all unread notifications
     * GET /api/notifications/unread
     */
    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications() {
        List<Notification> notifications = notificationService.getUnreadNotifications();
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get notifications for a specific user
     * GET /api/notifications/user/{username}
     */
    @GetMapping("/user/{username}")
    public ResponseEntity<List<Notification>> getNotificationsForUser(@PathVariable String username) {
        List<Notification> notifications = notificationService.getNotificationsForUser(username);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get unread notifications for a specific user
     * GET /api/notifications/user/{username}/unread
     */
    @GetMapping("/user/{username}/unread")
    public ResponseEntity<List<Notification>> getUnreadNotificationsForUser(@PathVariable String username) {
        List<Notification> notifications = notificationService.getUnreadNotificationsForUser(username);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get notifications by type
     * GET /api/notifications/type/{type}
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<Notification>> getNotificationsByType(@PathVariable String type) {
        List<Notification> notifications = notificationService.getNotificationsByType(type);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get notifications for a specific entity
     * GET /api/notifications/entity/{entityType}/{entityId}
     */
    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<List<Notification>> getNotificationsForEntity(
            @PathVariable String entityType,
            @PathVariable Long entityId) {
        List<Notification> notifications = notificationService.getNotificationsForEntity(entityType, entityId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get recent notifications (last N days)
     * GET /api/notifications/recent?days=7
     */
    @GetMapping("/recent")
    public ResponseEntity<List<Notification>> getRecentNotifications(
            @RequestParam(defaultValue = "7") int days) {
        List<Notification> notifications = notificationService.getRecentNotifications(days);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get a specific notification by ID
     * GET /api/notifications/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getNotificationById(@PathVariable Long id) {
        Optional<Notification> notification = notificationService.getNotificationById(id);
        if (notification.isPresent()) {
            return ResponseEntity.ok(notification.get());
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Notification not found with ID: " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * Mark a notification as read
     * PUT /api/notifications/{id}/read
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            notificationService.markAsRead(id);
            response.put("success", true);
            response.put("message", "Notification marked as read");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to mark notification as read: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Mark all notifications as read
     * PUT /api/notifications/read-all
     */
    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead() {
        Map<String, Object> response = new HashMap<>();
        try {
            notificationService.markAllAsRead();
            response.put("success", true);
            response.put("message", "All notifications marked as read");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to mark all notifications as read: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Mark all notifications for a user as read
     * PUT /api/notifications/user/{username}/read-all
     */
    @PutMapping("/user/{username}/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsReadForUser(@PathVariable String username) {
        Map<String, Object> response = new HashMap<>();
        try {
            notificationService.markAllAsReadForUser(username);
            response.put("success", true);
            response.put("message", "All notifications marked as read for user: " + username);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to mark notifications as read: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get count of unread notifications
     * GET /api/notifications/count/unread
     */
    @GetMapping("/count/unread")
    public ResponseEntity<Map<String, Object>> getUnreadCount() {
        Map<String, Object> response = new HashMap<>();
        long count = notificationService.getUnreadCount();
        response.put("success", true);
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    /**
     * Get count of unread notifications for a user
     * GET /api/notifications/user/{username}/count/unread
     */
    @GetMapping("/user/{username}/count/unread")
    public ResponseEntity<Map<String, Object>> getUnreadCountForUser(@PathVariable String username) {
        Map<String, Object> response = new HashMap<>();
        long count = notificationService.getUnreadCountForUser(username);
        response.put("success", true);
        response.put("username", username);
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a notification
     * DELETE /api/notifications/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteNotification(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            notificationService.deleteNotification(id);
            response.put("success", true);
            response.put("message", "Notification deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to delete notification: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Delete all read notifications
     * DELETE /api/notifications/read
     */
    @DeleteMapping("/read")
    public ResponseEntity<Map<String, Object>> deleteAllReadNotifications() {
        Map<String, Object> response = new HashMap<>();
        try {
            notificationService.deleteAllReadNotifications();
            response.put("success", true);
            response.put("message", "All read notifications deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to delete read notifications: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Create a custom notification (for testing or manual creation)
     * POST /api/notifications
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createNotification(@RequestBody Notification notification) {
        Map<String, Object> response = new HashMap<>();
        try {
            Notification created = notificationService.createNotification(notification);
            response.put("success", true);
            response.put("message", "Notification created successfully");
            response.put("notification", created);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to create notification: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
