package com.magictech.core.messaging.ui;

import com.magictech.core.messaging.dto.NotificationMessage;
import com.magictech.modules.projects.service.ProjectElementService;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import com.magictech.MainApp;

/**
 * JavaFX notification popup that displays notifications in the bottom-right corner.
 * - Regular notifications: Auto-dismiss after 5 seconds
 * - Approval notifications: Persistent until Accept/Reject is clicked
 * - Plays notification sound on display
 */
public class NotificationPopup {

    private static final double POPUP_WIDTH = 350;
    private static final double POPUP_HEIGHT = 100;
    private static final double MARGIN = 20;
    private static final int AUTO_DISMISS_SECONDS = 5;

    private Stage stage;
    private Timeline autoCloseTimeline;
    private boolean isApprovalNotification = false;
    private NotificationMessage currentMessage; // Store current message for marking as resolved
    private String currentUsername; // Store username for marking as resolved

    /**
     * Show a notification popup.
     *
     * @param message The notification message to display
     * @param username The username of the current user (for marking approvals as resolved)
     */
    public void show(NotificationMessage message, String username) {
        this.currentMessage = message; // Store the message
        this.currentUsername = username; // Store the username
        Platform.runLater(() -> {
            try {
                createAndShowPopup(message);
            } catch (Exception e) {
                System.err.println("Error showing notification popup: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Create and show the notification popup.
     */
    private void createAndShowPopup(NotificationMessage message) {
        stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);

        // Check if this is an approval notification
        isApprovalNotification = "APPROVAL_REQUESTED".equals(message.getAction());

        // Create UI
        VBox root = createNotificationUI(message);
        Scene scene = new Scene(root, POPUP_WIDTH, POPUP_HEIGHT);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);

        // Apply CSS styling
        applyStyles(root, message.getType());

        stage.setScene(scene);

        // Position at bottom-right
        positionPopup();

        // Initial opacity for fade-in animation
        stage.setOpacity(0);
        stage.show();

        // Animate fade-in
        fadeIn();

        // Play notification sound
        playNotificationSound();

        // Setup auto-dismiss ONLY for regular notifications
        // Approval notifications must be manually dismissed via Accept/Reject
        if (!isApprovalNotification) {
            setupAutoDismiss();
            // Click to dismiss for regular notifications only
            root.setOnMouseClicked(event -> dismiss());
        } else {
            // For approval notifications, add visual indicator that it's persistent
            root.setStyle(root.getStyle() + "-fx-border-color: #f39c12; -fx-border-width: 2;");
        }
    }

    /**
     * Create the notification UI.
     */
    private VBox createNotificationUI(NotificationMessage message) {
        // Icon based on type
        FontIcon icon = getIconForType(message.getType());
        icon.setIconSize(24);

        // Title
        Label titleLabel = new Label(message.getTitle());
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Message
        Label messageLabel = new Label(message.getMessage());
        messageLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: white;");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(POPUP_WIDTH - 80);

        // Module badge
        Label moduleLabel = new Label(message.getModule().toUpperCase());
        moduleLabel.setStyle("-fx-font-size: 10px; -fx-padding: 2 6; -fx-background-color: rgba(255,255,255,0.3); " +
                            "-fx-background-radius: 3; -fx-text-fill: white;");

        // Layout
        VBox textBox = new VBox(5);
        textBox.getChildren().addAll(titleLabel, messageLabel, moduleLabel);

        HBox contentBox = new HBox(15);
        contentBox.setAlignment(Pos.CENTER_LEFT);
        contentBox.getChildren().addAll(icon, textBox);

        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        root.getChildren().add(contentBox);

        // Add approval buttons if this is an approval request
        if ("APPROVAL_REQUESTED".equals(message.getAction())) {
            HBox buttonBox = createApprovalButtons(message);
            root.getChildren().add(buttonBox);
            // Increase popup height for buttons
            stage.setHeight(130);
        }

        root.setStyle("-fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 2);");

        return root;
    }

    /**
     * Create approval action buttons (Accept/Reject).
     */
    private HBox createApprovalButtons(NotificationMessage message) {
        Button acceptBtn = new Button("Accept");
        acceptBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-padding: 5 15; " +
                          "-fx-background-radius: 5; -fx-cursor: hand; -fx-font-size: 11px;");

        Button rejectBtn = new Button("Reject");
        rejectBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 5 15; " +
                          "-fx-background-radius: 5; -fx-cursor: hand; -fx-font-size: 11px;");

        // Handle accept
        acceptBtn.setOnAction(e -> {
            handleApproval(message.getEntityId(), true);
            dismiss();
        });

        // Handle reject
        rejectBtn.setOnAction(e -> {
            handleApproval(message.getEntityId(), false);
            dismiss();
        });

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(acceptBtn, rejectBtn);

        return buttonBox;
    }

    /**
     * Handle approval/rejection of project element.
     */
    private void handleApproval(Long elementId, boolean approved) {
        new Thread(() -> {
            try {
                // Get Spring context and service beans
                ApplicationContext context = MainApp.getSpringContext();
                ProjectElementService elementService = context.getBean(ProjectElementService.class);
                com.magictech.core.messaging.service.NotificationService notificationService =
                    context.getBean(com.magictech.core.messaging.service.NotificationService.class);

                // Approve or reject the project element
                if (approved) {
                    elementService.approveElement(elementId, currentUsername);
                    System.out.println("Project element " + elementId + " approved by " + currentUsername);
                } else {
                    elementService.rejectElement(elementId, currentUsername, "Rejected from notification");
                    System.out.println("Project element " + elementId + " rejected by " + currentUsername);
                }

                // IMPORTANT: Mark the notification as resolved so it doesn't appear again
                if (currentMessage != null && currentMessage.getNotificationId() != null) {
                    notificationService.markAsResolved(currentMessage.getNotificationId(), currentUsername);
                    System.out.println("Notification " + currentMessage.getNotificationId() +
                        " marked as resolved by " + currentUsername);
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                System.err.println("Error handling approval: " + ex.getMessage());
            }
        }).start();
    }

    /**
     * Get icon based on notification type.
     */
    private FontIcon getIconForType(String type) {
        FontIcon icon = switch (type != null ? type.toUpperCase() : "INFO") {
            case "SUCCESS" -> new FontIcon(FontAwesomeSolid.CHECK_CIRCLE);
            case "WARNING" -> new FontIcon(FontAwesomeSolid.EXCLAMATION_TRIANGLE);
            case "ERROR" -> new FontIcon(FontAwesomeSolid.TIMES_CIRCLE);
            default -> new FontIcon(FontAwesomeSolid.INFO_CIRCLE);
        };
        icon.setIconColor(javafx.scene.paint.Color.WHITE);
        return icon;
    }

    /**
     * Apply CSS styles based on notification type.
     */
    private void applyStyles(VBox root, String type) {
        String backgroundColor = switch (type != null ? type.toUpperCase() : "INFO") {
            case "SUCCESS" -> "-fx-background-color: linear-gradient(to right, #11998e, #38ef7d);";
            case "WARNING" -> "-fx-background-color: linear-gradient(to right, #f39c12, #f1c40f);";
            case "ERROR" -> "-fx-background-color: linear-gradient(to right, #e74c3c, #c0392b);";
            default -> "-fx-background-color: linear-gradient(to right, #3498db, #2980b9);";
        };

        root.setStyle(root.getStyle() + backgroundColor);
    }

    /**
     * Position popup at bottom-right of screen.
     */
    private void positionPopup() {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

        double x = screenBounds.getMaxX() - POPUP_WIDTH - MARGIN;
        double y = screenBounds.getMaxY() - POPUP_HEIGHT - MARGIN;

        stage.setX(x);
        stage.setY(y);
    }

    /**
     * Fade-in animation.
     */
    private void fadeIn() {
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), stage.getScene().getRoot());
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        // Also animate stage opacity
        Timeline stageOpacity = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(stage.opacityProperty(), 0)),
            new KeyFrame(Duration.millis(300), new KeyValue(stage.opacityProperty(), 1))
        );
        stageOpacity.play();
    }

    /**
     * Fade-out animation.
     */
    private void fadeOut() {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), stage.getScene().getRoot());
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(event -> stage.close());
        fadeOut.play();

        // Also animate stage opacity
        Timeline stageOpacity = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(stage.opacityProperty(), 1)),
            new KeyFrame(Duration.millis(300), new KeyValue(stage.opacityProperty(), 0))
        );
        stageOpacity.play();
    }

    /**
     * Setup auto-dismiss timer.
     */
    private void setupAutoDismiss() {
        autoCloseTimeline = new Timeline(
            new KeyFrame(Duration.seconds(AUTO_DISMISS_SECONDS), event -> dismiss())
        );
        autoCloseTimeline.play();
    }

    /**
     * Play notification sound.
     */
    private void playNotificationSound() {
        try {
            // Try to load notification sound from resources
            String soundPath = getClass().getResource("/sounds/notification.wav") != null
                ? getClass().getResource("/sounds/notification.wav").toExternalForm()
                : null;

            if (soundPath != null) {
                AudioClip notificationSound = new AudioClip(soundPath);
                notificationSound.setVolume(0.5); // 50% volume
                notificationSound.play();
            } else {
                // Fallback to system beep if sound file not found
                java.awt.Toolkit.getDefaultToolkit().beep();
            }
        } catch (Exception e) {
            // Silently fail - sound is not critical
            System.err.println("Could not play notification sound: " + e.getMessage());
        }
    }

    /**
     * Dismiss the notification.
     */
    public void dismiss() {
        if (autoCloseTimeline != null) {
            autoCloseTimeline.stop();
        }
        fadeOut();
    }

    /**
     * Immediately dismiss the notification without animation.
     * Used during cleanup/logout to ensure popups are closed immediately.
     */
    public void dismissImmediately() {
        if (autoCloseTimeline != null) {
            autoCloseTimeline.stop();
        }
        if (stage != null && stage.isShowing()) {
            Platform.runLater(() -> stage.close());
        }
    }

    /**
     * Check if popup is showing.
     */
    public boolean isShowing() {
        return stage != null && stage.isShowing();
    }
}
