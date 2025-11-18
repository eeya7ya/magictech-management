package com.magictech.core.ui.notification;

import com.magictech.core.approval.PendingApproval;
import com.magictech.core.approval.PendingApprovalService;
import com.magictech.core.notification.Notification;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Notification panel - side panel for viewing and managing notifications
 * Shows both notifications and pending approvals
 */
public class NotificationPanel extends VBox {

    private final NotificationManager notificationManager;
    private PendingApprovalService approvalService;

    private static final String PURPLE_COLOR = "#7c3aed";
    private static final String DARK_PURPLE = "#6b21a8";
    private static final String LIGHT_PURPLE = "#a78bfa";

    private ListView<Notification> notificationListView;
    private ListView<PendingApproval> approvalListView;
    private TabPane tabPane;

    public NotificationPanel(NotificationManager manager, PendingApprovalService approvalService) {
        this.notificationManager = manager;
        this.approvalService = approvalService;
        buildUI();
        loadData();
    }

    private void buildUI() {
        setPadding(new Insets(0));
        setSpacing(0);
        setMinWidth(400);
        setMaxWidth(400);

        // Background - black with purple accent
        setStyle("-fx-background-color: #1a1a1a;");

        // Header
        HBox header = createHeader();

        // Tab pane for notifications and approvals
        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color: #1a1a1a;");

        // Notifications tab
        Tab notificationsTab = new Tab("🔔 Notifications");
        notificationsTab.setContent(createNotificationsView());
        notificationsTab.setStyle("-fx-background-color: #1a1a1a;");

        // Approvals tab (only for SALES role)
        Tab approvalsTab = new Tab("✅ Pending Approvals");
        approvalsTab.setContent(createApprovalsView());
        approvalsTab.setStyle("-fx-background-color: #1a1a1a;");

        tabPane.getTabs().addAll(notificationsTab, approvalsTab);

        VBox.setVgrow(tabPane, Priority.ALWAYS);

        getChildren().addAll(header, tabPane);
    }

    /**
     * Create header with title and close button
     */
    private HBox createHeader() {
        HBox header = new HBox(10);
        header.setPadding(new Insets(15));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle(String.format(
            "-fx-background-color: linear-gradient(to right, %s, %s);",
            PURPLE_COLOR, DARK_PURPLE
        ));

        Label title = new Label("Notifications");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));
        title.setTextFill(Color.WHITE);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button markAllReadBtn = new Button("Mark All Read");
        markAllReadBtn.setStyle(
            "-fx-background-color: white; " +
            "-fx-text-fill: " + PURPLE_COLOR + "; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 5; " +
            "-fx-cursor: hand; " +
            "-fx-padding: 5 10 5 10;"
        );
        markAllReadBtn.setOnAction(e -> handleMarkAllRead());

        Button refreshBtn = new Button("🔄");
        refreshBtn.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 16px; " +
            "-fx-cursor: hand;"
        );
        refreshBtn.setOnAction(e -> loadData());

        header.getChildren().addAll(title, spacer, markAllReadBtn, refreshBtn);
        return header;
    }

    /**
     * Create notifications list view
     */
    private ScrollPane createNotificationsView() {
        notificationListView = new ListView<>();
        notificationListView.setStyle("-fx-background-color: #1a1a1a;");
        notificationListView.setCellFactory(lv -> new NotificationCell());
        notificationListView.setItems(notificationManager.getNotifications());

        notificationListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Notification selected = notificationListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    handleNotificationClick(selected);
                }
            }
        });

        ScrollPane scrollPane = new ScrollPane(notificationListView);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: #1a1a1a;");

        return scrollPane;
    }

    /**
     * Create approvals list view
     */
    private ScrollPane createApprovalsView() {
        approvalListView = new ListView<>();
        approvalListView.setStyle("-fx-background-color: #1a1a1a;");
        approvalListView.setCellFactory(lv -> new ApprovalCell());

        ScrollPane scrollPane = new ScrollPane(approvalListView);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: #1a1a1a;");

        return scrollPane;
    }

    /**
     * Load data
     */
    private void loadData() {
        // Notifications are already bound to NotificationManager
        notificationManager.refresh();

        // Load approvals
        if (approvalService != null && notificationManager.getCurrentUser() != null) {
            String userRole = notificationManager.getCurrentUser().getRole().name();
            if ("SALES".equals(userRole) || "MASTER".equals(userRole)) {
                List<PendingApproval> approvals = approvalService.getPendingApprovals(userRole);
                approvalListView.getItems().setAll(approvals);
            }
        }
    }

    /**
     * Handle mark all read
     */
    private void handleMarkAllRead() {
        notificationManager.markAllAsRead();
    }

    /**
     * Handle notification click
     */
    private void handleNotificationClick(Notification notification) {
        notificationManager.markAsRead(notification.getId());
        // TODO: Navigate to related content based on notification type
    }

    /**
     * Notification cell renderer
     */
    private class NotificationCell extends ListCell<Notification> {
        @Override
        protected void updateItem(Notification notification, boolean empty) {
            super.updateItem(notification, empty);

            if (empty || notification == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            VBox cell = new VBox(5);
            cell.setPadding(new Insets(10));

            // Background color based on read status
            String bgColor = notification.getIsRead() ? "#2a2a2a" : "#3a2a5a"; // Purple tint for unread
            cell.setStyle(String.format(
                "-fx-background-color: %s; " +
                "-fx-border-color: #444; " +
                "-fx-border-width: 0 0 1 0;",
                bgColor
            ));

            // Header with icon and time
            HBox header = new HBox(10);
            header.setAlignment(Pos.CENTER_LEFT);

            Label icon = new Label(getPriorityIcon(notification.getPriority()));
            Label time = new Label(formatTime(notification));
            time.setStyle("-fx-text-fill: #999; -fx-font-size: 10px;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            header.getChildren().addAll(icon, spacer, time);

            // Title
            Label title = new Label(notification.getTitle());
            title.setFont(Font.font("System", FontWeight.BOLD, 13));
            title.setTextFill(Color.web("#ffffff"));
            title.setWrapText(true);

            // Message
            Label message = new Label(notification.getMessage());
            message.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 11px;");
            message.setWrapText(true);

            cell.getChildren().addAll(header, title, message);

            setGraphic(cell);
            setText(null);
        }

        private String getPriorityIcon(String priority) {
            return switch (priority) {
                case "URGENT" -> "🚨";
                case "HIGH" -> "⚠️";
                case "NORMAL" -> "🔔";
                case "LOW" -> "ℹ️";
                default -> "📩";
            };
        }

        private String formatTime(Notification notification) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm");
            return notification.getCreatedAt().format(formatter);
        }
    }

    /**
     * Approval cell renderer
     */
    private class ApprovalCell extends ListCell<PendingApproval> {
        @Override
        protected void updateItem(PendingApproval approval, boolean empty) {
            super.updateItem(approval, empty);

            if (empty || approval == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            VBox cell = new VBox(10);
            cell.setPadding(new Insets(10));
            cell.setStyle(
                "-fx-background-color: #2a2a2a; " +
                "-fx-border-color: #444; " +
                "-fx-border-width: 0 0 1 0;"
            );

            // Header
            Label title = new Label("📦 Project Element Addition Request");
            title.setFont(Font.font("System", FontWeight.BOLD, 13));
            title.setTextFill(Color.web(LIGHT_PURPLE));

            // Details
            Label details = new Label(String.format(
                "Requested by: %s\nQuantity: %d\nExpires: %s",
                approval.getRequestedBy(),
                approval.getQuantity(),
                approval.getExpiresAt().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"))
            ));
            details.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 11px;");

            // Action buttons
            HBox actions = new HBox(10);
            actions.setAlignment(Pos.CENTER_RIGHT);

            Button approveBtn = new Button("✓ Approve");
            approveBtn.setStyle(
                "-fx-background-color: #22c55e; " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-background-radius: 5; " +
                "-fx-cursor: hand;"
            );
            approveBtn.setOnAction(e -> handleApprove(approval));

            Button rejectBtn = new Button("✗ Reject");
            rejectBtn.setStyle(
                "-fx-background-color: #ef4444; " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-background-radius: 5; " +
                "-fx-cursor: hand;"
            );
            rejectBtn.setOnAction(e -> handleReject(approval));

            actions.getChildren().addAll(rejectBtn, approveBtn);

            cell.getChildren().addAll(title, details, actions);

            setGraphic(cell);
            setText(null);
        }

        private void handleApprove(PendingApproval approval) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Approve Request");
            dialog.setHeaderText("Approve element addition request");
            dialog.setContentText("Notes (optional):");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(notes -> {
                try {
                    approvalService.approveRequest(
                        approval.getId(),
                        notificationManager.getCurrentUser().getUsername(),
                        notes
                    );
                    loadData(); // Refresh
                    showSuccess("Request approved successfully");
                } catch (Exception ex) {
                    showError("Failed to approve: " + ex.getMessage());
                }
            });
        }

        private void handleReject(PendingApproval approval) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Reject Request");
            dialog.setHeaderText("Reject element addition request");
            dialog.setContentText("Reason for rejection:");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(reason -> {
                try {
                    approvalService.rejectRequest(
                        approval.getId(),
                        notificationManager.getCurrentUser().getUsername(),
                        reason
                    );
                    loadData(); // Refresh
                    showSuccess("Request rejected");
                } catch (Exception ex) {
                    showError("Failed to reject: " + ex.getMessage());
                }
            });
        }
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
