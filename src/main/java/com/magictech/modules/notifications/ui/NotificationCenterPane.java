package com.magictech.modules.notifications.ui;

import com.magictech.core.auth.User;
import com.magictech.modules.notifications.entity.Notification;
import com.magictech.modules.notifications.service.NotificationService;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Notification Center UI Component
 * Displays bell icon with badge and notification dropdown
 */
@Component
public class NotificationCenterPane extends HBox {

    @Autowired
    private NotificationService notificationService;

    @Value("${app.notifications.polling.interval:5000}")
    private long pollingInterval;

    private User currentUser;
    private Label badgeLabel;
    private Button bellButton;
    private StackPane bellContainer;
    private VBox notificationDropdown;
    private ListView<Notification> notificationListView;
    private PauseTransition pollingTimer;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, HH:mm");
    private static final int MAX_NOTIFICATION_HEIGHT = 400;

    public NotificationCenterPane() {
        super();
        this.setAlignment(Pos.CENTER_RIGHT);
        this.setSpacing(10);
    }

    /**
     * Initialize the notification center for a user
     */
    public void initialize(User user) {
        this.currentUser = user;
        buildUI();
        startPolling();
        loadNotifications();
    }

    /**
     * Build the notification center UI
     */
    private void buildUI() {
        // Bell icon button
        FontIcon bellIcon = new FontIcon(FontAwesomeSolid.BELL);
        bellIcon.setIconSize(20);
        bellIcon.setStyle("-fx-icon-color: white;");

        bellButton = new Button();
        bellButton.setGraphic(bellIcon);
        bellButton.getStyleClass().addAll("button", "flat");
        bellButton.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-cursor: hand; " +
            "-fx-padding: 8px;"
        );

        // Badge for unread count
        badgeLabel = new Label("0");
        badgeLabel.setStyle(
            "-fx-background-color: #f44336; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 10px; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 2px 6px; " +
            "-fx-background-radius: 10px; " +
            "-fx-min-width: 18px; " +
            "-fx-alignment: center;"
        );
        badgeLabel.setVisible(false);

        // Stack bell and badge
        bellContainer = new StackPane(bellButton, badgeLabel);
        StackPane.setAlignment(bellButton, Pos.CENTER);
        StackPane.setAlignment(badgeLabel, Pos.TOP_RIGHT);
        StackPane.setMargin(badgeLabel, new Insets(-5, -5, 0, 0));

        // Create notification dropdown
        createNotificationDropdown();

        // Click handler to toggle dropdown
        bellButton.setOnAction(e -> toggleDropdown());

        this.getChildren().add(bellContainer);
    }

    /**
     * Create the notification dropdown panel
     */
    private void createNotificationDropdown() {
        notificationDropdown = new VBox(10);
        notificationDropdown.setStyle(
            "-fx-background-color: white; " +
            "-fx-border-color: #e0e0e0; " +
            "-fx-border-width: 1px; " +
            "-fx-background-radius: 4px; " +
            "-fx-border-radius: 4px; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 2);"
        );
        notificationDropdown.setPadding(new Insets(10));
        notificationDropdown.setMaxWidth(350);
        notificationDropdown.setMaxHeight(MAX_NOTIFICATION_HEIGHT);
        notificationDropdown.setVisible(false);

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("Notifications");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Button markAllReadBtn = new Button("Mark all read");
        markAllReadBtn.getStyleClass().addAll("button", "small");
        markAllReadBtn.setOnAction(e -> markAllAsRead());

        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-cursor: hand; " +
            "-fx-font-size: 14px;"
        );
        closeBtn.setOnAction(e -> notificationDropdown.setVisible(false));

        header.getChildren().addAll(titleLabel, markAllReadBtn, closeBtn);

        // Notification list
        notificationListView = new ListView<>();
        notificationListView.setCellFactory(param -> new NotificationCell());
        notificationListView.setPlaceholder(new Label("No notifications"));
        notificationListView.setPrefHeight(MAX_NOTIFICATION_HEIGHT - 80);

        // Footer with view all button
        Button viewAllBtn = new Button("View All Notifications");
        viewAllBtn.getStyleClass().addAll("button", "small");
        viewAllBtn.setMaxWidth(Double.MAX_VALUE);

        notificationDropdown.getChildren().addAll(header, new Separator(), notificationListView, viewAllBtn);
    }

    /**
     * Toggle dropdown visibility
     */
    private void toggleDropdown() {
        if (notificationDropdown.getParent() == null) {
            // Add dropdown to scene if not already added
            if (this.getScene() != null && this.getScene().getRoot() instanceof Pane) {
                Pane root = (Pane) this.getScene().getRoot();
                if (root instanceof StackPane) {
                    ((StackPane) root).getChildren().add(notificationDropdown);
                }
            }
        }

        notificationDropdown.setVisible(!notificationDropdown.isVisible());

        if (notificationDropdown.isVisible()) {
            // Position dropdown below bell
            positionDropdown();
            loadNotifications();
        }
    }

    /**
     * Position dropdown below the bell icon
     */
    private void positionDropdown() {
        double x = bellContainer.localToScene(bellContainer.getBoundsInLocal()).getMinX();
        double y = bellContainer.localToScene(bellContainer.getBoundsInLocal()).getMaxY() + 10;

        notificationDropdown.setLayoutX(x - 300); // Align to right
        notificationDropdown.setLayoutY(y);
    }

    /**
     * Load notifications from service
     */
    private void loadNotifications() {
        if (currentUser == null) return;

        try {
            List<Notification> notifications = notificationService.getRecentNotifications(
                currentUser.getId(), 7
            );

            Platform.runLater(() -> {
                notificationListView.getItems().clear();
                notificationListView.getItems().addAll(notifications);

                // Update badge count
                updateBadgeCount();
            });

        } catch (Exception e) {
            System.err.println("Error loading notifications: " + e.getMessage());
        }
    }

    /**
     * Update badge count with unread notifications
     */
    private void updateBadgeCount() {
        if (currentUser == null) return;

        try {
            Long count = notificationService.countUnread(currentUser.getId());

            Platform.runLater(() -> {
                if (count > 0) {
                    badgeLabel.setText(count > 99 ? "99+" : String.valueOf(count));
                    badgeLabel.setVisible(true);
                } else {
                    badgeLabel.setVisible(false);
                }
            });

        } catch (Exception e) {
            System.err.println("Error updating badge count: " + e.getMessage());
        }
    }

    /**
     * Mark all notifications as read
     */
    private void markAllAsRead() {
        if (currentUser == null) return;

        try {
            notificationService.markAllAsRead(currentUser.getId());
            loadNotifications();
        } catch (Exception e) {
            System.err.println("Error marking all as read: " + e.getMessage());
        }
    }

    /**
     * Start polling for new notifications
     */
    private void startPolling() {
        pollingTimer = new PauseTransition(Duration.millis(pollingInterval));
        pollingTimer.setOnFinished(e -> {
            updateBadgeCount();
            pollingTimer.playFromStart();
        });
        pollingTimer.play();
    }

    /**
     * Stop polling (cleanup)
     */
    public void stopPolling() {
        if (pollingTimer != null) {
            pollingTimer.stop();
            pollingTimer = null;
        }
    }

    /**
     * Custom cell for notification list
     */
    private class NotificationCell extends ListCell<Notification> {
        @Override
        protected void updateItem(Notification notification, boolean empty) {
            super.updateItem(notification, empty);

            if (empty || notification == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            VBox cell = new VBox(5);
            cell.setPadding(new Insets(8));
            cell.setStyle(
                notification.getIsRead()
                    ? "-fx-background-color: #f5f5f5;"
                    : "-fx-background-color: #e3f2fd; -fx-font-weight: bold;"
            );

            // Title with icon
            HBox titleBox = new HBox(5);
            titleBox.setAlignment(Pos.CENTER_LEFT);

            Label iconLabel = new Label(notification.getType().getIcon());
            iconLabel.setStyle("-fx-font-size: 14px;");

            Label titleLabel = new Label(notification.getTitle());
            titleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
            titleLabel.setWrapText(true);
            HBox.setHgrow(titleLabel, Priority.ALWAYS);

            titleBox.getChildren().addAll(iconLabel, titleLabel);

            // Message
            Label messageLabel = new Label(notification.getMessage());
            messageLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
            messageLabel.setWrapText(true);
            messageLabel.setMaxWidth(300);

            // Time and actions
            HBox footer = new HBox(10);
            footer.setAlignment(Pos.CENTER_LEFT);

            Label timeLabel = new Label(notification.getCreatedAt().format(TIME_FORMATTER));
            timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #999;");
            HBox.setHgrow(timeLabel, Priority.ALWAYS);

            if (!notification.getIsRead()) {
                Button markReadBtn = new Button("Mark read");
                markReadBtn.getStyleClass().addAll("button", "small");
                markReadBtn.setStyle("-fx-font-size: 9px; -fx-padding: 2px 6px;");
                markReadBtn.setOnAction(e -> {
                    notificationService.markAsRead(notification.getId());
                    loadNotifications();
                });
                footer.getChildren().add(markReadBtn);
            }

            footer.getChildren().add(0, timeLabel);

            cell.getChildren().addAll(titleBox, messageLabel, footer);

            // Click to mark as read
            cell.setOnMouseClicked(e -> {
                if (!notification.getIsRead()) {
                    notificationService.markAsRead(notification.getId());
                    loadNotifications();
                }
            });

            cell.setStyle(cell.getStyle() + "-fx-cursor: hand;");

            setGraphic(cell);
        }
    }
}
