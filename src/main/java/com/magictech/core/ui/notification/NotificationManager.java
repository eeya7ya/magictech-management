package com.magictech.core.ui.notification;

import com.magictech.core.auth.User;
import com.magictech.core.notification.Notification;
import com.magictech.core.notification.NotificationService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Notification manager for handling real-time notifications in JavaFX UI
 * Singleton pattern - manages notification polling, popup display, and badge counts
 */
@Component
public class NotificationManager {

    @Autowired
    private NotificationService notificationService;

    private User currentUser;
    private Stage primaryStage;

    private final ObservableList<Notification> notifications = FXCollections.observableArrayList();
    private final List<Consumer<Long>> badgeUpdateListeners = new ArrayList<>();
    private final List<Consumer<Notification>> newNotificationListeners = new ArrayList<>();

    private ScheduledExecutorService pollExecutor;
    private boolean isPolling = false;

    private static final int POLL_INTERVAL_SECONDS = 10; // Poll every 10 seconds

    /**
     * Initialize notification manager with user and stage
     */
    public void initialize(User user, Stage stage) {
        this.currentUser = user;
        this.primaryStage = stage;
        loadNotifications();
        startPolling();
    }

    /**
     * Load all notifications for current user
     */
    public void loadNotifications() {
        if (currentUser == null) return;

        try {
            List<Notification> userNotifications = notificationService.getAllNotifications(
                currentUser.getId(),
                currentUser.getRole().name()
            );

            Platform.runLater(() -> {
                notifications.clear();
                notifications.addAll(userNotifications);
                notifyBadgeUpdate();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Start polling for new notifications
     */
    public void startPolling() {
        if (isPolling) return;

        pollExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("NotificationPoller");
            return thread;
        });

        pollExecutor.scheduleAtFixedRate(this::pollNewNotifications,
                POLL_INTERVAL_SECONDS,
                POLL_INTERVAL_SECONDS,
                TimeUnit.SECONDS);

        isPolling = true;
    }

    /**
     * Stop polling
     */
    public void stopPolling() {
        if (pollExecutor != null) {
            pollExecutor.shutdown();
            isPolling = false;
        }
    }

    /**
     * Poll for new notifications (runs in background thread)
     */
    private void pollNewNotifications() {
        if (currentUser == null) return;

        try {
            // Get unshown notifications (notifications that haven't been displayed as popup yet)
            List<Notification> unshownNotifications = notificationService.getUnshownNotifications(
                currentUser.getId(),
                currentUser.getRole().name()
            );

            if (!unshownNotifications.isEmpty()) {
                Platform.runLater(() -> {
                    for (Notification notification : unshownNotifications) {
                        // Add to list
                        notifications.add(0, notification); // Add to front

                        // Show popup
                        showNotificationPopup(notification);

                        // Mark as shown
                        notificationService.markAsShown(notification.getId());

                        // Notify listeners
                        notifyNewNotification(notification);
                    }

                    notifyBadgeUpdate();
                });
            }

            // Also update badge count in case notifications were marked as read elsewhere
            long unreadCount = notificationService.getUnreadCount(
                currentUser.getId(),
                currentUser.getRole().name()
            );

            Platform.runLater(() -> notifyBadgeUpdate(unreadCount));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Show notification popup
     */
    private void showNotificationPopup(Notification notification) {
        if (primaryStage == null || primaryStage.getScene() == null) return;

        Scene scene = primaryStage.getScene();
        NotificationPopup popup = new NotificationPopup(notification, this);
        popup.show(scene);
    }

    /**
     * Mark notification as read
     */
    public void markAsRead(Long notificationId) {
        notificationService.markAsRead(notificationId);

        // Update local list
        notifications.stream()
                .filter(n -> n.getId().equals(notificationId))
                .findFirst()
                .ifPresent(n -> n.setIsRead(true));

        notifyBadgeUpdate();
    }

    /**
     * Mark all notifications as read
     */
    public void markAllAsRead() {
        if (currentUser == null) return;

        notificationService.markAllAsRead(currentUser.getId(), currentUser.getRole().name());

        // Update local list
        notifications.forEach(n -> n.setIsRead(true));

        notifyBadgeUpdate();
    }

    /**
     * Get unread count
     */
    public long getUnreadCount() {
        if (currentUser == null) return 0;

        return notifications.stream()
                .filter(n -> !n.getIsRead())
                .count();
    }

    /**
     * Get all notifications (observable list for UI binding)
     */
    public ObservableList<Notification> getNotifications() {
        return notifications;
    }

    /**
     * Add badge update listener
     */
    public void addBadgeUpdateListener(Consumer<Long> listener) {
        badgeUpdateListeners.add(listener);
        listener.accept(getUnreadCount()); // Initial update
    }

    /**
     * Add new notification listener
     */
    public void addNewNotificationListener(Consumer<Notification> listener) {
        newNotificationListeners.add(listener);
    }

    /**
     * Remove badge update listener
     */
    public void removeBadgeUpdateListener(Consumer<Long> listener) {
        badgeUpdateListeners.remove(listener);
    }

    /**
     * Remove new notification listener
     */
    public void removeNewNotificationListener(Consumer<Notification> listener) {
        newNotificationListeners.remove(listener);
    }

    /**
     * Notify all badge update listeners
     */
    private void notifyBadgeUpdate() {
        long count = getUnreadCount();
        notifyBadgeUpdate(count);
    }

    /**
     * Notify all badge update listeners with specific count
     */
    private void notifyBadgeUpdate(long count) {
        for (Consumer<Long> listener : badgeUpdateListeners) {
            listener.accept(count);
        }
    }

    /**
     * Notify all new notification listeners
     */
    private void notifyNewNotification(Notification notification) {
        for (Consumer<Notification> listener : newNotificationListeners) {
            listener.accept(notification);
        }
    }

    /**
     * Cleanup - call when application closes
     */
    public void cleanup() {
        stopPolling();
        badgeUpdateListeners.clear();
        newNotificationListeners.clear();
        notifications.clear();
        currentUser = null;
        primaryStage = null;
    }

    /**
     * Get current user
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Refresh notifications manually
     */
    public void refresh() {
        loadNotifications();
    }
}
