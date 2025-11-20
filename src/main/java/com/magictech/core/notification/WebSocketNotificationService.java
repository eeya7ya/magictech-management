package com.magictech.core.notification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for sending real-time notifications via WebSocket
 * Pushes notifications to connected clients instantly
 */
@Service
public class WebSocketNotificationService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Send notification to specific user via WebSocket
     * @param userId User ID to send notification to
     * @param notification Notification object
     */
    public void sendToUser(Long userId, Notification notification) {
        try {
            Map<String, Object> message = buildNotificationMessage(notification);
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(userId),
                    "/queue/notifications",
                    message
            );
            System.out.println("📤 WebSocket notification sent to user " + userId);
        } catch (Exception e) {
            System.err.println("✗ Failed to send WebSocket notification to user " + userId + ": " + e.getMessage());
        }
    }

    /**
     * Send notification to all users with specific role
     * @param role User role (e.g., "PROJECTS", "SALES", "MASTER")
     * @param notification Notification object
     */
    public void sendToRole(String role, Notification notification) {
        try {
            Map<String, Object> message = buildNotificationMessage(notification);
            messagingTemplate.convertAndSend(
                    "/topic/notifications/" + role,
                    message
            );
            System.out.println("📤 WebSocket notification sent to role: " + role);
        } catch (Exception e) {
            System.err.println("✗ Failed to send WebSocket notification to role " + role + ": " + e.getMessage());
        }
    }

    /**
     * Broadcast notification to all connected users
     * @param notification Notification object
     */
    public void broadcastToAll(Notification notification) {
        try {
            Map<String, Object> message = buildNotificationMessage(notification);
            messagingTemplate.convertAndSend(
                    "/topic/notifications/broadcast",
                    message
            );
            System.out.println("📤 WebSocket notification broadcast to all users");
        } catch (Exception e) {
            System.err.println("✗ Failed to broadcast WebSocket notification: " + e.getMessage());
        }
    }

    /**
     * Send custom message to user
     * @param userId User ID
     * @param message Message map
     */
    public void sendCustomMessageToUser(Long userId, Map<String, Object> message) {
        try {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(userId),
                    "/queue/messages",
                    message
            );
        } catch (Exception e) {
            System.err.println("✗ Failed to send custom message: " + e.getMessage());
        }
    }

    /**
     * Build notification message map for WebSocket
     */
    private Map<String, Object> buildNotificationMessage(Notification notification) {
        Map<String, Object> message = new HashMap<>();
        message.put("id", notification.getId());
        message.put("type", notification.getType());
        message.put("title", notification.getTitle());
        message.put("message", notification.getMessage());
        message.put("priority", notification.getPriority());
        message.put("module", notification.getModule());
        message.put("relatedId", notification.getRelatedId());
        message.put("relatedType", notification.getRelatedType());
        message.put("createdAt", notification.getCreatedAt().toString());
        message.put("createdBy", notification.getCreatedBy());
        message.put("isRead", notification.getIsRead());
        return message;
    }

    /**
     * Send unread count update to user
     * @param userId User ID
     * @param unreadCount Unread notification count
     */
    public void sendUnreadCountUpdate(Long userId, long unreadCount) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "UNREAD_COUNT_UPDATE");
            message.put("count", unreadCount);
            message.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSendToUser(
                    String.valueOf(userId),
                    "/queue/notifications/count",
                    message
            );
        } catch (Exception e) {
            System.err.println("✗ Failed to send unread count update: " + e.getMessage());
        }
    }
}
