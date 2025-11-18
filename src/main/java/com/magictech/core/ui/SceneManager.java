package com.magictech.core.ui;

import com.magictech.core.auth.User;
import com.magictech.core.module.ModuleConfig;
import com.magictech.core.ui.controllers.MainDashboardController;
import com.magictech.modules.storage.StorageController;
import com.magictech.modules.maintenance.MaintenanceStorageController;
import com.magictech.modules.projects.ProjectsStorageController;
import com.magictech.modules.pricing.PricingStorageController;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import com.magictech.modules.sales.SalesStorageController;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Scene Manager - WITH LOADING OVERLAY + PROPER BEAN MANAGEMENT
 * Eliminates flickering with smooth loading transitions
 */
@Component
public class SceneManager {

    private static SceneManager instance;

    private Stage primaryStage;
    private User currentUser;
    private StackPane loadingOverlay;
    private boolean isTransitioning = false;

    @Autowired
    private SpringFXMLLoader springFXMLLoader;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private com.magictech.core.ui.notification.NotificationManager notificationManager;

    // Track active controllers for cleanup
    private MainDashboardController activeDashboard;
    private Object activeModuleController;

    public SceneManager() {
        instance = this;
    }

    public static SceneManager getInstance() {
        return instance;
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
        createLoadingOverlay();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * ✅ Create elegant loading overlay
     */
    private void createLoadingOverlay() {
        loadingOverlay = new StackPane();
        loadingOverlay.setStyle(
                "-fx-background-color: rgba(15, 23, 42, 0.95);" +
                        "-fx-background-radius: 0;"
        );

        VBox loadingBox = new VBox(20);
        loadingBox.setAlignment(Pos.CENTER);

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setStyle("-fx-progress-color: #8b5cf6;");
        spinner.setPrefSize(60, 60);

        Label loadingLabel = new Label("Loading...");
        loadingLabel.setStyle(
                "-fx-text-fill: white;" +
                        "-fx-font-size: 18px;" +
                        "-fx-font-weight: bold;"
        );

        loadingBox.getChildren().addAll(spinner, loadingLabel);
        loadingOverlay.getChildren().add(loadingBox);
        loadingOverlay.setVisible(false);
    }

    /**
     * ✅ Show loading overlay with fade-in
     */
    private void showLoading() {
        if (isTransitioning) return;
        isTransitioning = true;

        Platform.runLater(() -> {
            if (primaryStage.getScene() != null) {
                StackPane currentRoot = (StackPane) primaryStage.getScene().getRoot();
                if (!currentRoot.getChildren().contains(loadingOverlay)) {
                    currentRoot.getChildren().add(loadingOverlay);
                }

                loadingOverlay.setOpacity(0);
                loadingOverlay.setVisible(true);
                loadingOverlay.toFront();

                FadeTransition fadeIn = new FadeTransition(Duration.millis(150), loadingOverlay);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                fadeIn.play();
            }
        });
    }

    /**
     * ✅ Hide loading overlay with fade-out
     */
    private void hideLoading() {
        Platform.runLater(() -> {
            if (loadingOverlay != null && loadingOverlay.isVisible()) {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(200), loadingOverlay);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(e -> {
                    loadingOverlay.setVisible(false);
                    isTransitioning = false;
                });
                fadeOut.play();
            } else {
                isTransitioning = false;
            }
        });
    }

    /**
     * ✅ IMMEDIATE cleanup - no delays
     */
    private void immediateCleanup() {
        // ✅ Clean up NotificationManager
        if (notificationManager != null) {
            try {
                notificationManager.cleanup();
            } catch (Exception e) {
                System.err.println("NotificationManager cleanup warning: " + e.getMessage());
            }
        }

        // Clean up dashboard
        if (activeDashboard != null) {
            try {
                activeDashboard.immediateCleanup();
            } catch (Exception e) {
                System.err.println("Dashboard cleanup warning: " + e.getMessage());
            }
            activeDashboard = null;
        }

        // Clean up module controller
        if (activeModuleController != null) {
            try {
                if (activeModuleController instanceof StorageController) {
                    ((StorageController) activeModuleController).immediateCleanup();
                } else if (activeModuleController instanceof SalesStorageController) {
                    ((SalesStorageController) activeModuleController).immediateCleanup();
                } else if (activeModuleController instanceof MaintenanceStorageController) {
                    ((MaintenanceStorageController) activeModuleController).immediateCleanup();
                } else if (activeModuleController instanceof ProjectsStorageController) {
                    ((ProjectsStorageController) activeModuleController).immediateCleanup();
                } else if (activeModuleController instanceof PricingStorageController) {
                    ((PricingStorageController) activeModuleController).immediateCleanup();
                }
            } catch (Exception e) {
                System.err.println("Module cleanup warning: " + e.getMessage());
            }
            activeModuleController = null;
        }
    }

    /**
     * Show login screen
     */
    public void showLoginScreen() {
        try {
            immediateCleanup();

            Parent root = springFXMLLoader.load("/fxml/login.fxml");
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            primaryStage.setScene(scene);
            primaryStage.setTitle("MagicTech - Login");
            primaryStage.centerOnScreen();
            primaryStage.setMaximized(false);

        } catch (IOException e) {
            System.err.println("Error loading login screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void showLogin() {
        showLoginScreen();
    }

    /**
     * ✅ FIXED: Show main dashboard with loading overlay
     */
    public void showMainDashboard() {
        if (isTransitioning) return;

        showLoading();

        // Small delay to ensure loading shows before heavy work
        Platform.runLater(() -> {
            try {
                Thread.sleep(100); // Let loading appear
            } catch (InterruptedException ignored) {}

            Platform.runLater(() -> {
                try {
                    // STEP 1: Cleanup
                    immediateCleanup();

                    // STEP 2: Load new dashboard
                    FXMLLoader loader = springFXMLLoader.getLoader("/fxml/main-dashboard.fxml");
                    Parent root = loader.load();

                    MainDashboardController controller = loader.getController();
                    controller.initializeWithUser(currentUser);

                    // ✅ Initialize NotificationManager
                    if (currentUser != null) {
                        notificationManager.initialize(currentUser, primaryStage);
                    }

                    activeDashboard = controller;

                    // STEP 3: Wrap in StackPane for loading overlay
                    StackPane wrappedRoot = new StackPane(root);
                    Scene scene = new Scene(wrappedRoot);
                    scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
                    scene.getStylesheets().add(getClass().getResource("/css/dashboard.css").toExternalForm());

                    primaryStage.setScene(scene);
                    primaryStage.setTitle("MagicTech - Dashboard");
                    primaryStage.setMaximized(true);

                    // Recreate loading overlay for new scene
                    createLoadingOverlay();

                    hideLoading();
                    System.out.println("✓ Dashboard loaded");

                } catch (Exception e) {
                    System.err.println("Error loading dashboard: " + e.getMessage());
                    e.printStackTrace();
                    hideLoading();
                }
            });
        });
    }

    /**
     * ✅ FIXED: Show Storage Module with loading + FRESH INSTANCE
     */
    public void showStorageModule() {
        if (isTransitioning) return;

        showLoading();

        Platform.runLater(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}

            Platform.runLater(() -> {
                try {
                    immediateCleanup();

                    // ✅ CREATE FRESH INSTANCE - not cached!
                    StorageController storageController = new StorageController();
                    // Manually inject dependencies
                    context.getAutowireCapableBeanFactory().autowireBean(storageController);

                    ModuleConfig config = ModuleConfig.createStorageConfig();
                    storageController.initialize(currentUser, config);

                    activeModuleController = storageController;

                    Parent root = storageController.getRootPane();
                    StackPane wrappedRoot = new StackPane(root);
                    Scene scene = new Scene(wrappedRoot);
                    scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
                    scene.getStylesheets().add(getClass().getResource("/css/dashboard.css").toExternalForm());

                    primaryStage.setScene(scene);
                    primaryStage.setTitle("MagicTech - Storage Management");
                    primaryStage.setMaximized(true);

                    createLoadingOverlay();
                    hideLoading();
                    System.out.println("✓ Storage module loaded (fresh instance)");

                } catch (Exception e) {
                    System.err.println("Error loading Storage Module: " + e.getMessage());
                    e.printStackTrace();
                    hideLoading();
                }
            });
        });
    }

    /**
     * ✅ FIXED: Show Sales Module
     */
    public void showSalesModule() {
        if (isTransitioning) return;

        showLoading();

        Platform.runLater(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}

            Platform.runLater(() -> {
                try {
                    immediateCleanup();

                    SalesStorageController salesController = new SalesStorageController();
                    context.getAutowireCapableBeanFactory().autowireBean(salesController);

                    ModuleConfig config = ModuleConfig.createSalesConfig();
                    salesController.initialize(currentUser, config);

                    activeModuleController = salesController;

                    Parent root = salesController.getRootPane();
                    StackPane wrappedRoot = new StackPane(root);
                    Scene scene = new Scene(wrappedRoot);
                    scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
                    scene.getStylesheets().add(getClass().getResource("/css/dashboard.css").toExternalForm());

                    primaryStage.setScene(scene);
                    primaryStage.setTitle("MagicTech - Sales Module");
                    primaryStage.setMaximized(true);

                    createLoadingOverlay();
                    hideLoading();
                    System.out.println("✓ Sales module loaded");

                } catch (Exception e) {
                    System.err.println("Error loading Sales Module: " + e.getMessage());
                    e.printStackTrace();
                    hideLoading();
                }
            });
        });
    }

    /**
     * ✅ FIXED: Show Maintenance Module
     */
    public void showMaintenanceModule() {
        if (isTransitioning) return;

        showLoading();

        Platform.runLater(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}

            Platform.runLater(() -> {
                try {
                    immediateCleanup();

                    MaintenanceStorageController maintenanceController = new MaintenanceStorageController();
                    context.getAutowireCapableBeanFactory().autowireBean(maintenanceController);

                    ModuleConfig config = ModuleConfig.createMaintenanceConfig();
                    maintenanceController.initialize(currentUser, config);

                    activeModuleController = maintenanceController;

                    Parent root = maintenanceController.getRootPane();
                    StackPane wrappedRoot = new StackPane(root);
                    Scene scene = new Scene(wrappedRoot);
                    scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
                    scene.getStylesheets().add(getClass().getResource("/css/dashboard.css").toExternalForm());

                    primaryStage.setScene(scene);
                    primaryStage.setTitle("MagicTech - Maintenance Module");
                    primaryStage.setMaximized(true);

                    createLoadingOverlay();
                    hideLoading();
                    System.out.println("✓ Maintenance module loaded");

                } catch (Exception e) {
                    System.err.println("Error loading Maintenance Module: " + e.getMessage());
                    e.printStackTrace();
                    hideLoading();
                }
            });
        });
    }

    /**
     * ✅ FIXED: Show Projects Module
     */
    public void showProjectsModule() {
        if (isTransitioning) return;

        showLoading();

        Platform.runLater(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}

            Platform.runLater(() -> {
                try {
                    immediateCleanup();

                    ProjectsStorageController projectsController = new ProjectsStorageController();
                    context.getAutowireCapableBeanFactory().autowireBean(projectsController);

                    ModuleConfig config = ModuleConfig.createProjectsConfig();
                    projectsController.initialize(currentUser, config);

                    activeModuleController = projectsController;

                    Parent root = projectsController.getRootPane();
                    StackPane wrappedRoot = new StackPane(root);
                    Scene scene = new Scene(wrappedRoot);
                    scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
                    scene.getStylesheets().add(getClass().getResource("/css/dashboard.css").toExternalForm());

                    primaryStage.setScene(scene);
                    primaryStage.setTitle("MagicTech - Projects Module");
                    primaryStage.setMaximized(true);

                    createLoadingOverlay();
                    hideLoading();
                    System.out.println("✓ Projects module loaded");

                } catch (Exception e) {
                    System.err.println("Error loading Projects Module: " + e.getMessage());
                    e.printStackTrace();
                    hideLoading();
                }
            });
        });
    }

    /**
     * ✅ FIXED: Show Pricing Module
     */
    public void showPricingModule() {
        if (isTransitioning) return;

        showLoading();

        Platform.runLater(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}

            Platform.runLater(() -> {
                try {
                    immediateCleanup();

                    PricingStorageController pricingController = new PricingStorageController();
                    context.getAutowireCapableBeanFactory().autowireBean(pricingController);

                    ModuleConfig config = ModuleConfig.createPricingConfig();
                    pricingController.initialize(currentUser, config);

                    activeModuleController = pricingController;

                    Parent root = pricingController.getRootPane();
                    StackPane wrappedRoot = new StackPane(root);
                    Scene scene = new Scene(wrappedRoot);
                    scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
                    scene.getStylesheets().add(getClass().getResource("/css/dashboard.css").toExternalForm());

                    primaryStage.setScene(scene);
                    primaryStage.setTitle("MagicTech - Pricing Module");
                    primaryStage.setMaximized(true);

                    createLoadingOverlay();
                    hideLoading();
                    System.out.println("✓ Pricing module loaded");

                } catch (Exception e) {
                    System.err.println("Error loading Pricing Module: " + e.getMessage());
                    e.printStackTrace();
                    hideLoading();
                }
            });
        });
    }

    /**
     * Generic module routing
     */
    public void showModule(String moduleName) {
        switch (moduleName.toLowerCase()) {
            case "storage":
                showStorageModule();
                break;
            case "sales":
                showSalesModule();
                break;
            case "maintenance":
                showMaintenanceModule();
                break;
            case "projects":
                showProjectsModule();
                break;
            case "pricing":
                showPricingModule();
                break;
            default:
                System.err.println("Unknown module: " + moduleName);
                break;
        }
    }

    /**
     * Logout and return to login
     */
    public void logout() {
        immediateCleanup();
        currentUser = null;
        showLoginScreen();
    }
}