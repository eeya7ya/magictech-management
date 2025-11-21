package com.magictech.core.ui.components;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;

/**
 * Toast notification component for instant local feedback.
 *
 * Key differences from NotificationPopup:
 * - Appears INSTANTLY (no network delay)
 * - Positioned at top-right (distributed notifications at bottom-right)
 * - Shorter duration (3 seconds vs 5 seconds)
 * - Supports stacking multiple toasts
 * - Different styling to differentiate from distributed notifications
 *
 * Use this for:
 * - Immediate feedback after user actions (create, update, delete)
 * - Local validation messages
 * - Quick status updates
 *
 * Use NotificationPopup for:
 * - Distributed notifications from other users/modules
 * - Important alerts that require attention
 */
public class ToastNotification {

    private static final double TOAST_WIDTH = 320;
    private static final double TOAST_HEIGHT = 80;
    private static final double MARGIN = 20;
    private static final double STACK_SPACING = 10;
    private static final int AUTO_DISMISS_SECONDS = 3;

    // Track all active toasts for stacking
    private static final List<ToastNotification> activeToasts = new ArrayList<>();

    private Stage stage;
    private Timeline autoCloseTimeline;
    private ToastType type;

    /**
     * Toast types for different feedback scenarios
     */
    public enum ToastType {
        SUCCESS,
        INFO,
        WARNING,
        ERROR
    }

    /**
     * Show a success toast (green)
     */
    public static void showSuccess(String message) {
        show("Success", message, ToastType.SUCCESS);
    }

    /**
     * Show an info toast (blue)
     */
    public static void showInfo(String message) {
        show("Info", message, ToastType.INFO);
    }

    /**
     * Show a warning toast (orange)
     */
    public static void showWarning(String message) {
        show("Warning", message, ToastType.WARNING);
    }

    /**
     * Show an error toast (red)
     */
    public static void showError(String message) {
        show("Error", message, ToastType.ERROR);
    }

    /**
     * Show a custom toast
     */
    public static void show(String title, String message, ToastType type) {
        Platform.runLater(() -> {
            try {
                ToastNotification toast = new ToastNotification();
                toast.createAndShow(title, message, type);
            } catch (Exception e) {
                System.err.println("Error showing toast notification: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Show a distributing notification toast (for actions in progress)
     */
    public static void showDistributing(String action) {
        showInfo(action + " - Notifying other modules...");
    }

    /**
     * Create and show the toast notification.
     */
    private void createAndShow(String title, String message, ToastType type) {
        this.type = type;

        stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);

        // Create UI
        VBox root = createToastUI(title, message, type);
        Scene scene = new Scene(root, TOAST_WIDTH, TOAST_HEIGHT);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);

        stage.setScene(scene);

        // Position at top-right with stacking
        positionToast();

        // Initial opacity for fade-in animation
        stage.setOpacity(0);
        stage.show();

        // Add to active toasts
        activeToasts.add(this);

        // Animate slide-in from right + fade-in
        slideInAndFadeIn();

        // Setup auto-dismiss
        setupAutoDismiss();

        // Click to dismiss
        root.setOnMouseClicked(event -> dismiss());
    }

    /**
     * Create the toast UI.
     */
    private VBox createToastUI(String title, String message, ToastType type) {
        // Icon based on type
        FontIcon icon = getIconForType(type);
        icon.setIconSize(20);

        // Title
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Message
        Label messageLabel = new Label(message);
        messageLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.9);");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(TOAST_WIDTH - 70);

        // Layout
        VBox textBox = new VBox(3);
        textBox.getChildren().addAll(titleLabel, messageLabel);

        HBox contentBox = new HBox(12);
        contentBox.setAlignment(Pos.CENTER_LEFT);
        contentBox.getChildren().addAll(icon, textBox);

        VBox root = new VBox();
        root.setPadding(new Insets(12));
        root.getChildren().add(contentBox);

        // Styling based on type
        String backgroundColor = getBackgroundColor(type);
        root.setStyle("-fx-background-radius: 8; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 12, 0, 0, 3); " +
                     backgroundColor);

        return root;
    }

    /**
     * Get icon based on toast type.
     */
    private FontIcon getIconForType(ToastType type) {
        FontIcon icon = switch (type) {
            case SUCCESS -> new FontIcon(FontAwesomeSolid.CHECK_CIRCLE);
            case WARNING -> new FontIcon(FontAwesomeSolid.EXCLAMATION_TRIANGLE);
            case ERROR -> new FontIcon(FontAwesomeSolid.TIMES_CIRCLE);
            default -> new FontIcon(FontAwesomeSolid.INFO_CIRCLE);
        };
        icon.setIconColor(javafx.scene.paint.Color.WHITE);
        return icon;
    }

    /**
     * Get background color based on toast type.
     */
    private String getBackgroundColor(ToastType type) {
        return switch (type) {
            case SUCCESS -> "-fx-background-color: linear-gradient(to right, #10b981, #059669);";
            case WARNING -> "-fx-background-color: linear-gradient(to right, #f59e0b, #d97706);";
            case ERROR -> "-fx-background-color: linear-gradient(to right, #ef4444, #dc2626);";
            default -> "-fx-background-color: linear-gradient(to right, #3b82f6, #2563eb);";
        };
    }

    /**
     * Position toast at top-right with stacking support.
     */
    private void positionToast() {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

        double x = screenBounds.getMaxX() - TOAST_WIDTH - MARGIN;

        // Calculate Y position based on number of active toasts
        int index = activeToasts.size() - 1; // Current toast index
        double y = screenBounds.getMinY() + MARGIN + (index * (TOAST_HEIGHT + STACK_SPACING));

        stage.setX(x);
        stage.setY(y);
    }

    /**
     * Slide-in from right and fade-in animation.
     */
    private void slideInAndFadeIn() {
        // Save original X position
        double originalX = stage.getX();

        // Start off-screen to the right
        stage.setX(originalX + 100);

        // Slide in animation
        Timeline slideIn = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(stage.xProperty(), originalX + 100),
                new KeyValue(stage.opacityProperty(), 0)
            ),
            new KeyFrame(Duration.millis(300),
                new KeyValue(stage.xProperty(), originalX, Interpolator.EASE_OUT),
                new KeyValue(stage.opacityProperty(), 1, Interpolator.EASE_IN)
            )
        );
        slideIn.play();
    }

    /**
     * Slide-out to right and fade-out animation.
     */
    private void slideOutAndFadeOut() {
        double originalX = stage.getX();

        Timeline slideOut = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(stage.xProperty(), originalX),
                new KeyValue(stage.opacityProperty(), 1)
            ),
            new KeyFrame(Duration.millis(300),
                new KeyValue(stage.xProperty(), originalX + 100, Interpolator.EASE_IN),
                new KeyValue(stage.opacityProperty(), 0, Interpolator.EASE_OUT)
            )
        );
        slideOut.setOnFinished(event -> {
            stage.close();
            activeToasts.remove(this);
            repositionRemainingToasts();
        });
        slideOut.play();
    }

    /**
     * Reposition remaining toasts after one is dismissed.
     */
    private void repositionRemainingToasts() {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double x = screenBounds.getMaxX() - TOAST_WIDTH - MARGIN;

        for (int i = 0; i < activeToasts.size(); i++) {
            ToastNotification toast = activeToasts.get(i);
            double targetY = screenBounds.getMinY() + MARGIN + (i * (TOAST_HEIGHT + STACK_SPACING));

            // Animate to new position
            Timeline reposition = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(toast.stage.yProperty(), toast.stage.getY())),
                new KeyFrame(Duration.millis(200), new KeyValue(toast.stage.yProperty(), targetY, Interpolator.EASE_BOTH))
            );
            reposition.play();
        }
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
     * Dismiss the toast.
     */
    public void dismiss() {
        if (autoCloseTimeline != null) {
            autoCloseTimeline.stop();
        }
        slideOutAndFadeOut();
    }

    /**
     * Check if toast is showing.
     */
    public boolean isShowing() {
        return stage != null && stage.isShowing();
    }

    /**
     * Dismiss all active toasts.
     */
    public static void dismissAll() {
        // Create a copy to avoid concurrent modification
        List<ToastNotification> toasts = new ArrayList<>(activeToasts);
        for (ToastNotification toast : toasts) {
            toast.dismiss();
        }
    }
}
