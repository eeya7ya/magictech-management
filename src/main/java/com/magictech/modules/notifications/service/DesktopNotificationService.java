package com.magictech.modules.notifications.service;

import com.magictech.modules.notifications.entity.Notification;
import com.magictech.modules.notifications.repository.NotificationRepository;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.awt.*;

/**
 * Service for sending desktop/system tray notifications
 */
@Service
public class DesktopNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(DesktopNotificationService.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Value("${app.notifications.desktop.enabled:true}")
    private boolean desktopNotificationsEnabled;

    private static final boolean SYSTEM_TRAY_SUPPORTED = SystemTray.isSupported();

    /**
     * Send desktop notification (JavaFX + System Tray)
     */
    @Async
    public void sendDesktopNotification(Notification notification) {
        if (!desktopNotificationsEnabled) {
            logger.debug("Desktop notifications disabled, skipping notification {}", notification.getId());
            notification.setIsSentDesktop(true);
            notificationRepository.save(notification);
            return;
        }

        try {
            // Try JavaFX notification first (if in JavaFX context)
            sendJavaFXNotification(notification);

            // Fallback to system tray for broader compatibility
            sendSystemTrayNotification(notification);

            // Mark as sent
            notification.setIsSentDesktop(true);
            notificationRepository.save(notification);

            logger.info("Sent desktop notification {}: {}", notification.getId(), notification.getTitle());

        } catch (Exception e) {
            logger.error("Failed to send desktop notification {}: {}",
                        notification.getId(), e.getMessage(), e);
        }
    }

    /**
     * Send JavaFX notification (ControlsFX)
     */
    private void sendJavaFXNotification(Notification notification) {
        try {
            Platform.runLater(() -> {
                try {
                    String title = notification.getType().getIcon() + " " + notification.getTitle();
                    String message = notification.getMessage();

                    // Truncate long messages
                    if (message.length() > 100) {
                        message = message.substring(0, 97) + "...";
                    }

                    Notifications notificationBuilder = Notifications.create()
                        .title(title)
                        .text(message)
                        .hideAfter(Duration.seconds(5))
                        .position(Pos.BOTTOM_RIGHT);

                    // Set style based on priority
                    switch (notification.getPriority()) {
                        case URGENT -> notificationBuilder.showError();
                        case HIGH -> notificationBuilder.showWarning();
                        case NORMAL -> notificationBuilder.showInformation();
                        case LOW -> notificationBuilder.showInformation();
                    }

                    logger.debug("Displayed JavaFX notification: {}", title);

                } catch (Exception e) {
                    logger.debug("Could not show JavaFX notification (may not be in FX context): {}",
                               e.getMessage());
                }
            });
        } catch (Exception e) {
            logger.debug("JavaFX notification not available: {}", e.getMessage());
        }
    }

    /**
     * Send system tray notification (cross-platform)
     */
    private void sendSystemTrayNotification(Notification notification) {
        if (!SYSTEM_TRAY_SUPPORTED) {
            logger.debug("System tray not supported on this platform");
            return;
        }

        try {
            SystemTray tray = SystemTray.getSystemTray();

            // Create tray icon if not exists
            TrayIcon trayIcon = getTrayIcon();
            if (trayIcon == null) {
                return;
            }

            String title = notification.getType().getIcon() + " " + notification.getTitle();
            String message = notification.getMessage();

            // Truncate long messages
            if (message.length() > 200) {
                message = message.substring(0, 197) + "...";
            }

            TrayIcon.MessageType messageType = switch (notification.getPriority()) {
                case URGENT, HIGH -> TrayIcon.MessageType.WARNING;
                case NORMAL -> TrayIcon.MessageType.INFO;
                case LOW -> TrayIcon.MessageType.NONE;
            };

            trayIcon.displayMessage(title, message, messageType);

            logger.debug("Displayed system tray notification: {}", title);

        } catch (Exception e) {
            logger.debug("Could not show system tray notification: {}", e.getMessage());
        }
    }

    /**
     * Get or create system tray icon
     */
    private TrayIcon getTrayIcon() {
        try {
            SystemTray tray = SystemTray.getSystemTray();

            // Check if icon already exists
            TrayIcon[] icons = tray.getTrayIcons();
            if (icons.length > 0) {
                return icons[0];
            }

            // Create new tray icon
            // Use a simple colored square as icon (you can replace with actual icon file)
            java.awt.Image image = new java.awt.image.BufferedImage(16, 16,
                                    java.awt.image.BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = ((java.awt.image.BufferedImage) image).createGraphics();
            graphics.setColor(new Color(33, 150, 243)); // Blue
            graphics.fillRect(0, 0, 16, 16);
            graphics.dispose();

            TrayIcon trayIcon = new TrayIcon(image, "MagicTech Management");
            trayIcon.setImageAutoSize(true);
            trayIcon.setToolTip("MagicTech Management System");

            tray.add(trayIcon);

            return trayIcon;

        } catch (Exception e) {
            logger.error("Failed to create system tray icon: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Show notification with custom action
     */
    public void showNotificationWithAction(String title, String message,
                                           Runnable onClickAction) {
        if (!desktopNotificationsEnabled) {
            return;
        }

        Platform.runLater(() -> {
            Notifications.create()
                .title(title)
                .text(message)
                .hideAfter(Duration.seconds(5))
                .position(Pos.BOTTOM_RIGHT)
                .onAction(event -> {
                    if (onClickAction != null) {
                        onClickAction.run();
                    }
                })
                .showInformation();
        });
    }
}
