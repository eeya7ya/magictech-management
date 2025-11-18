package com.magictech.core.ui.notification;

import com.magictech.core.notification.Notification;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

/**
 * Popup notification component - slides in from top-right corner
 * Purple theme with auto-dismiss after 5 seconds
 */
public class NotificationPopup extends StackPane {

    private final Notification notification;
    private final NotificationManager manager;
    private Timeline autoCloseTimeline;

    private static final String PURPLE_COLOR = "#7c3aed";
    private static final String DARK_PURPLE = "#6b21a8";
    private static final String LIGHT_PURPLE = "#a78bfa";

    public NotificationPopup(Notification notification, NotificationManager manager) {
        this.notification = notification;
        this.manager = manager;
        buildUI();
    }

    private void buildUI() {
        setMaxWidth(400);
        setMaxHeight(150);

        // Container with gradient background
        VBox container = new VBox(10);
        container.setPadding(new Insets(15));
        container.setAlignment(Pos.TOP_LEFT);

        // Background gradient (purple theme)
        String gradientStyle = String.format(
            "-fx-background-color: linear-gradient(to bottom right, %s, %s); " +
            "-fx-background-radius: 10; " +
            "-fx-border-radius: 10; " +
            "-fx-border-color: %s; " +
            "-fx-border-width: 2;",
            PURPLE_COLOR, DARK_PURPLE, LIGHT_PURPLE
        );
        container.setStyle(gradientStyle);

        // Drop shadow effect
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web(DARK_PURPLE, 0.5));
        shadow.setRadius(10);
        container.setEffect(shadow);

        // Header with icon and close button
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        // Priority icon
        Label icon = new Label(getPriorityIcon());
        icon.setStyle("-fx-font-size: 20px;");

        // Title
        Label title = new Label(notification.getTitle());
        title.setFont(Font.font("System", FontWeight.BOLD, 14));
        title.setTextFill(Color.WHITE);
        title.setWrapText(true);
        HBox.setHgrow(title, Priority.ALWAYS);

        // Close button
        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 16px; " +
            "-fx-cursor: hand; " +
            "-fx-padding: 0 5 0 5;"
        );
        closeBtn.setOnAction(e -> dismiss());

        header.getChildren().addAll(icon, title, closeBtn);

        // Message
        TextFlow messageFlow = new TextFlow();
        Text messageText = new Text(notification.getMessage());
        messageText.setFill(Color.web("#e9d5ff")); // Light purple text
        messageText.setFont(Font.font("System", 12));
        messageFlow.getChildren().add(messageText);
        messageFlow.setMaxWidth(370);

        // Action button (if applicable)
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        if (isActionable()) {
            Button actionBtn = new Button("View Details");
            actionBtn.setStyle(
                "-fx-background-color: white; " +
                "-fx-text-fill: " + PURPLE_COLOR + "; " +
                "-fx-font-weight: bold; " +
                "-fx-background-radius: 5; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 5 15 5 15;"
            );
            actionBtn.setOnAction(e -> handleAction());

            Button dismissBtn = new Button("Dismiss");
            dismissBtn.setStyle(
                "-fx-background-color: transparent; " +
                "-fx-text-fill: white; " +
                "-fx-border-color: white; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 5; " +
                "-fx-background-radius: 5; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 5 15 5 15;"
            );
            dismissBtn.setOnAction(e -> dismiss());

            actionBox.getChildren().addAll(dismissBtn, actionBtn);
        }

        container.getChildren().addAll(header, messageFlow);
        if (isActionable()) {
            container.getChildren().add(actionBox);
        }

        getChildren().add(container);

        // Auto-dismiss after 5 seconds for non-critical notifications
        if (!"HIGH".equals(notification.getPriority()) && !"URGENT".equals(notification.getPriority())) {
            autoCloseTimeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> dismiss()));
            autoCloseTimeline.play();
        }
    }

    /**
     * Get icon based on priority
     */
    private String getPriorityIcon() {
        return switch (notification.getPriority()) {
            case "URGENT" -> "🚨";
            case "HIGH" -> "⚠️";
            case "NORMAL" -> "🔔";
            case "LOW" -> "ℹ️";
            default -> "📩";
        };
    }

    /**
     * Check if notification requires action
     */
    private boolean isActionable() {
        String type = notification.getType();
        return "ELEMENT_APPROVAL_REQUEST".equals(type) ||
               "PROJECT_CREATED".equals(type) ||
               type.contains("APPROVAL");
    }

    /**
     * Handle action button click
     */
    private void handleAction() {
        // Mark as read
        manager.markAsRead(notification.getId());

        // TODO: Navigate to relevant module/view based on notification type
        // This will be implemented when integrating with SceneManager

        dismiss();
    }

    /**
     * Show popup in scene
     */
    public void show(Scene scene) {
        if (scene == null || scene.getRoot() == null) return;

        // Add to scene root
        if (scene.getRoot() instanceof Pane) {
            Pane root = (Pane) scene.getRoot();

            // Position at top-right
            setLayoutX(scene.getWidth() - getMaxWidth() - 20);
            setLayoutY(-getMaxHeight()); // Start off-screen

            root.getChildren().add(this);

            // Slide in animation
            TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), this);
            slideIn.setFromY(-getMaxHeight());
            slideIn.setToY(20);
            slideIn.setInterpolator(Interpolator.EASE_OUT);
            slideIn.play();
        }
    }

    /**
     * Dismiss popup
     */
    private void dismiss() {
        // Stop auto-close timer
        if (autoCloseTimeline != null) {
            autoCloseTimeline.stop();
        }

        // Slide out animation
        TranslateTransition slideOut = new TranslateTransition(Duration.millis(300), this);
        slideOut.setToY(-getMaxHeight() - 50);
        slideOut.setInterpolator(Interpolator.EASE_IN);
        slideOut.setOnFinished(e -> {
            if (getParent() instanceof Pane) {
                ((Pane) getParent()).getChildren().remove(this);
            }
        });
        slideOut.play();

        // Fade out
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), this);
        fadeOut.setToValue(0);
        fadeOut.play();
    }
}
