package com.magictech.core.ui.components;

import com.magictech.core.messaging.dto.NotificationMessage;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

/**
 * Toast-style notification popup that appears in the top-right corner
 * Automatically fades out after a few seconds
 */
public class NotificationPopup extends Popup {

    private static final double POPUP_WIDTH = 350;
    private static final double POPUP_HEIGHT = 80;
    private static final double MARGIN_RIGHT = 20;
    private static final double MARGIN_TOP = 80;

    private VBox container;

    public NotificationPopup(NotificationMessage message, Window owner) {
        setupUI(message);
        setupAnimation(owner);
    }

    private void setupUI(NotificationMessage message) {
        // Main container
        container = new VBox(8);
        container.setPrefWidth(POPUP_WIDTH);
        container.setPrefHeight(POPUP_HEIGHT);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setStyle(getStyleForType(message.getType()));

        // Icon based on notification type
        FontIcon icon = getIconForType(message.getType());
        icon.setIconSize(24);

        // Title
        Label titleLabel = new Label(message.getTitle());
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: white;");

        // Message
        Label messageLabel = new Label(message.getMessage());
        messageLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: white; -fx-wrap-text: true;");
        messageLabel.setMaxWidth(POPUP_WIDTH - 60);

        // Header with icon and title
        HBox header = new HBox(10, icon, titleLabel);
        header.setAlignment(Pos.CENTER_LEFT);

        // Content
        VBox content = new VBox(5, header, messageLabel);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setStyle("-fx-padding: 15;");

        container.getChildren().add(content);

        // Make it clickable to close
        container.setOnMouseClicked(e -> hide());

        getContent().add(container);
    }

    private void setupAnimation(Window owner) {
        if (owner == null) return;

        // Position at top-right corner
        double x = owner.getX() + owner.getWidth() - POPUP_WIDTH - MARGIN_RIGHT;
        double y = owner.getY() + MARGIN_TOP;

        setX(x);
        setY(y);

        // Slide in from right
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), container);
        slideIn.setFromX(POPUP_WIDTH);
        slideIn.setToX(0);

        // Fade in
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), container);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        // Auto-hide after 5 seconds
        PauseTransition pause = new PauseTransition(Duration.seconds(5));
        pause.setOnFinished(e -> {
            // Fade out
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), container);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(evt -> hide());
            fadeOut.play();
        });

        // Play animations
        slideIn.play();
        fadeIn.play();
        pause.play();
    }

    private String getStyleForType(String type) {
        if (type == null) type = "INFO";

        return switch (type.toUpperCase()) {
            case "SUCCESS" -> "-fx-background-color: #2ecc71; -fx-background-radius: 8; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 2, 2);";
            case "ERROR", "DANGER" -> "-fx-background-color: #e74c3c; -fx-background-radius: 8; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 2, 2);";
            case "WARNING" -> "-fx-background-color: #f39c12; -fx-background-radius: 8; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 2, 2);";
            default -> "-fx-background-color: #3498db; -fx-background-radius: 8; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 2, 2);";
        };
    }

    private FontIcon getIconForType(String type) {
        if (type == null) type = "INFO";

        return switch (type.toUpperCase()) {
            case "SUCCESS" -> new FontIcon(MaterialDesign.MDI_CHECK_CIRCLE);
            case "ERROR", "DANGER" -> new FontIcon(MaterialDesign.MDI_ALERT_CIRCLE);
            case "WARNING" -> new FontIcon(MaterialDesign.MDI_ALERT);
            default -> new FontIcon(MaterialDesign.MDI_INFORMATION);
        };
    }

    /**
     * Show notification popup on a specific window
     */
    public static void showNotification(NotificationMessage message, Window owner) {
        if (owner == null || message == null) return;

        javafx.application.Platform.runLater(() -> {
            // Find the topmost focused window (handles dialogs being open)
            Window targetWindow = findTopmostWindow(owner);

            NotificationPopup popup = new NotificationPopup(message, targetWindow);

            // Make popup appear on top of all windows including dialogs
            popup.setAutoHide(false);

            popup.show(targetWindow);

            System.out.println("🎨 Displaying notification popup: " + message.getTitle());
        });
    }

    /**
     * Find the topmost window (handles cases where dialogs are open)
     */
    private static Window findTopmostWindow(Window primaryWindow) {
        // Get all windows and find the focused one or the topmost visible one
        java.util.List<Window> allWindows = Window.getWindows();

        // First, try to find a focused window
        for (Window window : allWindows) {
            if (window.isFocused() && window.isShowing()) {
                return window;
            }
        }

        // If no focused window, return the first showing window (usually the topmost)
        for (int i = allWindows.size() - 1; i >= 0; i--) {
            Window window = allWindows.get(i);
            if (window.isShowing()) {
                return window;
            }
        }

        return primaryWindow;
    }
}
