package com.magictech.core.ui.controllers;

import com.magictech.core.approval.PendingApprovalService;
import com.magictech.core.auth.User;
import com.magictech.core.ui.SceneManager;
import com.magictech.core.ui.components.DashboardBackgroundPane;
import com.magictech.core.ui.notification.NotificationManager;
import com.magictech.core.ui.notification.NotificationPanel;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
public class MainDashboardController {

    @FXML private BorderPane dashboardRoot;
    @FXML private DashboardBackgroundPane dashboardBackground;
    @FXML private Label welcomeLabel;
    @FXML private Label roleLabel;
    @FXML private FlowPane modulesContainer;
    @FXML private javafx.scene.control.Button userManagementButton;

    @Autowired
    private SceneManager sceneManager;

    @Autowired
    private UserManagementController userManagementController;

    @Autowired
    private NotificationManager notificationManager;

    @Autowired
    private PendingApprovalService pendingApprovalService;

    private User currentUser;
    private NotificationPanel notificationPanel;
    private Button notificationButton;
    private Label notificationBadge;
    private HashMap<String, Label> moduleBadges = new HashMap<>();

    @FXML
    public void initialize() {
        // Don't load modules here - currentUser is null at this point
        //playEntranceAnimation();
    }

    public void initializeWithUser(User user) {
        this.currentUser = user;

        if (currentUser != null) {
            welcomeLabel.setText("Welcome, " + currentUser.getUsername());
            roleLabel.setText("Role: " + currentUser.getRole().getDisplayName());

            // Show user management button only for MASTER role
            if (userManagementButton != null) {
                userManagementButton.setVisible(
                    currentUser.getRole() == com.magictech.core.auth.UserRole.MASTER
                );
                userManagementButton.setManaged(
                    currentUser.getRole() == com.magictech.core.auth.UserRole.MASTER
                );
            }

            // Initialize notification manager
            notificationManager.initialize(currentUser, sceneManager.getPrimaryStage());

            // Add notification button to header
            addNotificationButton();

            // ✅ FIX: Load modules AFTER user is set
            loadModules();

            // Setup notification listeners for badge updates
            setupNotificationListeners();
        }
    }

    private void loadModules() {
        modulesContainer.getChildren().clear();

        if (currentUser == null) {
            return; // No user, no modules
        }

        // Role-based module loading
        com.magictech.core.auth.UserRole role = currentUser.getRole();

        // Add modules based on role
        if (role == com.magictech.core.auth.UserRole.MASTER) {
            // Admin sees all modules
            modulesContainer.getChildren().addAll(
                    createModuleCard("📦", "Storage Management",
                            "Full inventory control • All data access • Master storage operations",
                            "module-red", "storage"),
                    createModuleCard("🛒", "Sales Team Module",
                            "Manage sales operations • View availability & pricing • Track inventory",
                            "module-blue", "sales"),
                    createModuleCard("📁", "Projects Team Module",
                            "Coordinate projects • Track resources • Manage team collaboration",
                            "module-purple", "projects"),
                    createModuleCard("💰", "Pricing Module",
                            "Configure pricing models • Manage quotes • Availability-based pricing",
                            "module-orange", "pricing"),
                    createModuleCard("🔧", "Maintenance Team Module",
                            "Handle maintenance requests • Equipment tracking • Service schedules",
                            "module-green", "maintenance")
            );
        } else if (role == com.magictech.core.auth.UserRole.STORAGE) {
            modulesContainer.getChildren().add(
                    createModuleCard("📦", "Storage Management",
                            "Full inventory control • All data access • Master storage operations",
                            "module-red", "storage")
            );
        } else if (role == com.magictech.core.auth.UserRole.SALES) {
            modulesContainer.getChildren().add(
                    createModuleCard("🛒", "Sales Team Module",
                            "Manage sales operations • View availability & pricing • Track inventory",
                            "module-blue", "sales")
            );
        } else if (role == com.magictech.core.auth.UserRole.PROJECTS ||
                   role == com.magictech.core.auth.UserRole.PROJECT_SUPPLIER) {
            modulesContainer.getChildren().add(
                    createModuleCard("📁", "Projects Team Module",
                            "Coordinate projects • Track resources • Manage team collaboration",
                            "module-purple", "projects")
            );
        } else if (role == com.magictech.core.auth.UserRole.PRICING) {
            modulesContainer.getChildren().add(
                    createModuleCard("💰", "Pricing Module",
                            "Configure pricing models • Manage quotes • Availability-based pricing",
                            "module-orange", "pricing")
            );
        } else if (role == com.magictech.core.auth.UserRole.MAINTENANCE) {
            modulesContainer.getChildren().add(
                    createModuleCard("🔧", "Maintenance Team Module",
                            "Handle maintenance requests • Equipment tracking • Service schedules",
                            "module-green", "maintenance")
            );
        }
    }

    private VBox createModuleCard(String icon, String title, String description, String colorClass, String moduleId) {
        VBox card = new VBox();
        card.getStyleClass().addAll("module-card", colorClass);
        card.setSpacing(0);
        card.setPrefWidth(340);
        card.setMinWidth(340);
        card.setMaxWidth(340);
        card.setUserData(moduleId);

        card.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.5); " +
                        "-fx-background-radius: 16; " +
                        "-fx-border-color: rgba(139, 92, 246, 0.6); " +
                        "-fx-border-width: 3; " +
                        "-fx-border-radius: 16; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 20, 0, 0, 8);"
        );

        HBox header = new HBox();
        header.getStyleClass().add("module-card-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(15);
        header.setPadding(new Insets(20, 24, 20, 24));
        header.setMinHeight(80);

        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("module-icon");
        iconLabel.setFont(new Font(32));

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("module-title");
        titleLabel.setFont(Font.font("System Bold", 17));
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        // Add notification badge
        StackPane badgeContainer = new StackPane();
        badgeContainer.setMinSize(24, 24);
        badgeContainer.setMaxSize(24, 24);

        Circle badgeCircle = new Circle(12);
        badgeCircle.setFill(Color.web("#ef4444")); // Red background

        Label badge = new Label("0");
        badge.setFont(Font.font("System", FontWeight.BOLD, 11));
        badge.setTextFill(Color.WHITE);
        badge.setAlignment(Pos.CENTER);

        badgeContainer.getChildren().addAll(badgeCircle, badge);
        badgeContainer.setVisible(false); // Initially hidden

        // Store badge reference for updates
        moduleBadges.put(moduleId, badge);

        Label arrowLabel = new Label("►");
        arrowLabel.getStyleClass().add("module-arrow");
        arrowLabel.setFont(new Font(18));

        header.getChildren().addAll(iconLabel, titleLabel, badgeContainer, arrowLabel);

        VBox content = new VBox();
        content.getStyleClass().add("module-card-content");
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(24));
        content.setMinHeight(100);

        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("module-description");
        descLabel.setWrapText(true);
        descLabel.setFont(new Font(13));
        descLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-text-alignment: center;");

        content.getChildren().add(descLabel);

        card.setOnMouseEntered(e -> animateCardHover(card, true));
        card.setOnMouseExited(e -> animateCardHover(card, false));
        card.setOnMouseClicked(e -> handleModuleClick(moduleId, card));

        card.getChildren().addAll(header, content);
        return card;
    }

    private void animateCardHover(VBox card, boolean isHover) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(200), card);

        if (isHover) {
            scale.setToX(1.03);
            scale.setToY(1.03);
        } else {
            scale.setToX(1.0);
            scale.setToY(1.0);
        }

        scale.play();
    }

    private void handleModuleClick(String moduleId, VBox clickedCard) {
        System.out.println("Module clicked: " + moduleId);

        if (clickedCard != null) {
            ScaleTransition pulse = new ScaleTransition(Duration.millis(100), clickedCard);
            pulse.setFromX(1.03);
            pulse.setFromY(1.03);
            pulse.setToX(0.98);
            pulse.setToY(0.98);
            pulse.setCycleCount(2);
            pulse.setAutoReverse(true);
            pulse.setOnFinished(e -> navigateToModule(moduleId));
            pulse.play();
        } else {
            navigateToModule(moduleId);
        }
    }

    // REPLACE the existing navigateToModule() method with this:
    private void navigateToModule(String moduleId) {
        if (dashboardBackground != null) {
            dashboardBackground.stopAnimation();
        }
        // ✅ NO FADE ANIMATION - Direct navigation with loading overlay
        sceneManager.showModule(moduleId);
    }




    /**
     * Add notification button to header
     */
    private void addNotificationButton() {
        // Find the header container (assuming it's the top of the BorderPane)
        javafx.scene.Node topNode = dashboardRoot.getTop();
        if (topNode instanceof HBox) {
            HBox headerBox = (HBox) topNode;

            // Create notification button with bell icon
            notificationButton = new Button("🔔");
            notificationButton.setFont(new Font(20));
            notificationButton.setStyle(
                "-fx-background-color: rgba(139, 92, 246, 0.3); " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 50%; " +
                "-fx-min-width: 45px; " +
                "-fx-min-height: 45px; " +
                "-fx-max-width: 45px; " +
                "-fx-max-height: 45px; " +
                "-fx-cursor: hand; " +
                "-fx-border-color: rgba(139, 92, 246, 0.6); " +
                "-fx-border-width: 2; " +
                "-fx-border-radius: 50%;"
            );

            // Create badge for notification count
            StackPane notificationContainer = new StackPane();
            Circle badgeCircle = new Circle(10);
            badgeCircle.setFill(Color.web("#ef4444"));
            badgeCircle.setTranslateX(15);
            badgeCircle.setTranslateY(-15);

            notificationBadge = new Label("0");
            notificationBadge.setFont(Font.font("System", FontWeight.BOLD, 10));
            notificationBadge.setTextFill(Color.WHITE);
            notificationBadge.setTranslateX(15);
            notificationBadge.setTranslateY(-15);

            notificationContainer.getChildren().addAll(notificationButton, badgeCircle, notificationBadge);
            badgeCircle.setVisible(false);
            notificationBadge.setVisible(false);

            // Add click handler
            notificationButton.setOnAction(e -> toggleNotificationPanel());

            // Add to header (before logout button if it exists)
            int insertIndex = headerBox.getChildren().size() - 1; // Before last element (logout button)
            if (insertIndex < 0) insertIndex = 0;
            headerBox.getChildren().add(insertIndex, notificationContainer);
        }
    }

    /**
     * Setup notification listeners for badge updates
     */
    private void setupNotificationListeners() {
        notificationManager.addBadgeUpdateListener(count -> {
            // Update header notification badge
            if (notificationBadge != null) {
                notificationBadge.setText(String.valueOf(count));
                notificationBadge.setVisible(count > 0);
                notificationBadge.getParent().getChildrenUnmodifiable().stream()
                    .filter(node -> node instanceof Circle)
                    .findFirst()
                    .ifPresent(circle -> circle.setVisible(count > 0));
            }

            // Update module badges
            updateModuleBadges();
        });
    }

    /**
     * Update module-specific notification badges
     */
    private void updateModuleBadges() {
        for (String moduleId : moduleBadges.keySet()) {
            String moduleUpperCase = moduleId.toUpperCase();
            long count = notificationManager.getUnreadCountByModule(moduleUpperCase);

            Label badge = moduleBadges.get(moduleId);
            if (badge != null) {
                badge.setText(String.valueOf(count));
                // Show/hide badge container
                javafx.scene.Node badgeContainer = badge.getParent();
                if (badgeContainer != null) {
                    badgeContainer.setVisible(count > 0);
                }
            }
        }
    }

    /**
     * Toggle notification panel
     */
    private void toggleNotificationPanel() {
        if (notificationPanel == null) {
            // Create notification panel
            notificationPanel = new NotificationPanel(notificationManager, pendingApprovalService);

            // Add to right side of dashboard
            dashboardRoot.setRight(notificationPanel);

            // Add slide-in animation
            notificationPanel.setTranslateX(400); // Start off-screen
            TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), notificationPanel);
            slideIn.setToX(0);
            slideIn.play();
        } else {
            // Slide out and remove
            TranslateTransition slideOut = new TranslateTransition(Duration.millis(300), notificationPanel);
            slideOut.setToX(400);
            slideOut.setOnFinished(e -> {
                dashboardRoot.setRight(null);
                notificationPanel = null;
            });
            slideOut.play();
        }
    }

    // ADD this new method:
    public void immediateCleanup() {
        if (dashboardBackground != null) {
            dashboardBackground.stopAnimation();
            dashboardBackground = null;
        }

        // Cleanup notification manager
        if (notificationManager != null) {
            notificationManager.stopPolling();
        }

        System.out.println("✓ Dashboard cleaned up immediately");
    }

    @FXML
    private void handleUserManagement() {
        if (currentUser != null && currentUser.getRole() == com.magictech.core.auth.UserRole.MASTER) {
            userManagementController.showUserManagement(sceneManager.getPrimaryStage());
        }
    }

    @FXML
    private void handleLogout() {
        if (dashboardBackground != null) {
            dashboardBackground.stopAnimation();
        }
        // ✅ NO FADE ANIMATION - Direct logout
        sceneManager.setCurrentUser(null);
        sceneManager.showLoginScreen();
    }
    private void playEntranceAnimation() {

    }

    private void animateModuleCards() {
        int delay = 0;

        for (javafx.scene.Node node : modulesContainer.getChildren()) {
            if (node instanceof VBox) {
                VBox card = (VBox) node;

                card.setOpacity(0);
                card.setTranslateY(20);

                FadeTransition fade = new FadeTransition(Duration.millis(400), card);
                fade.setFromValue(0);
                fade.setToValue(1);
                fade.setDelay(Duration.millis(delay));

                TranslateTransition slide = new TranslateTransition(Duration.millis(400), card);
                slide.setFromY(20);
                slide.setToY(0);
                slide.setDelay(Duration.millis(delay));
                slide.setInterpolator(Interpolator.EASE_OUT);

                ParallelTransition cardAnim = new ParallelTransition(fade, slide);
                cardAnim.play();

                delay += 80;
            }
        }
    }
}