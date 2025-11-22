package com.magictech.core.ui;

import com.magictech.core.auth.User;
import com.magictech.core.messaging.ui.NotificationManager;
import com.magictech.core.module.ModuleConfig;
import com.magictech.core.ui.controllers.MainDashboardController;
import com.magictech.modules.storage.StorageController;
import com.magictech.modules.maintenance.MaintenanceStorageController;
import com.magictech.modules.projects.ProjectsStorageController;
import com.magictech.modules.presales.PresalesController;
import com.magictech.modules.qualityassurance.QualityAssuranceController;
import com.magictech.modules.finance.FinanceController;
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
    private NotificationManager notificationManager;

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
        // Clean up notification system
        try {
            if (notificationManager != null && notificationManager.isInitialized()) {
                notificationManager.cleanup();
                System.out.println("✓ Notification system cleaned up");
            }
        } catch (Exception e) {
            System.err.println("Notification cleanup warning: " + e.getMessage());
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
                } else if (activeModuleController instanceof PricingController) {
                    ((PricingController) activeModuleController).immediateCleanup();
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
            primaryStage.setMaximized(true); // ✅ Keep fullscreen mode consistent
            primaryStage.centerOnScreen();

        } catch (IOException e) {
            System.err.println("Error loading login screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void showLogin() {
        showLoginScreen();
    }

    /**
     * ✅ FIXED: Show main dashboard with NO white flash
     */
    public void showMainDashboard() {
        if (isTransitioning) return;
        isTransitioning = true;

        Platform.runLater(() -> {
            try {
                // STEP 1: Cleanup immediately
                immediateCleanup();

                // STEP 2: Load new dashboard
                FXMLLoader loader = springFXMLLoader.getLoader("/fxml/main-dashboard.fxml");
                Parent root = loader.load();

                MainDashboardController controller = loader.getController();
                controller.initializeWithUser(currentUser);

                activeDashboard = controller;

                // STEP 3: Wrap in StackPane with DARK background
                StackPane wrappedRoot = new StackPane(root);
                wrappedRoot.setStyle("-fx-background-color: rgb(15, 20, 28);");

                // STEP 4: Create scene with DARK fill to prevent white flash
                Scene scene = new Scene(wrappedRoot);
                scene.setFill(javafx.scene.paint.Color.rgb(15, 20, 28));
                scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
                scene.getStylesheets().add(getClass().getResource("/css/dashboard.css").toExternalForm());

                // STEP 5: Set scene IMMEDIATELY (no delays)
                primaryStage.setScene(scene);
                primaryStage.setTitle("MagicTech - Dashboard");
                primaryStage.setMaximized(true);

                // Recreate loading overlay for new scene
                createLoadingOverlay();
                isTransitioning = false;

                // Initialize notification system
                // Use "storage" as default module type since it subscribes to all channels
                try {
                    notificationManager.initialize(currentUser, "storage");
                    System.out.println("✓ Notification system initialized");
                } catch (Exception e) {
                    System.err.println("Warning: Failed to initialize notification system: " + e.getMessage());
                }

                System.out.println("✓ Dashboard loaded - NO WHITE FLASH");

            } catch (Exception e) {
                System.err.println("Error loading dashboard: " + e.getMessage());
                e.printStackTrace();
                isTransitioning = false;
            }
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
     * ✅ Show Presales Module
     */
    public void showPresalesModule() {
        if (isTransitioning) return;

        showLoading();

        Platform.runLater(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}

            Platform.runLater(() -> {
                try {
                    immediateCleanup();

                    PresalesController presalesController = new PresalesController();
                    context.getAutowireCapableBeanFactory().autowireBean(presalesController);

                    ModuleConfig config = ModuleConfig.createPresalesConfig();
                    presalesController.initialize(currentUser, config);

                    activeModuleController = presalesController;

                    Parent root = presalesController.getRootPane();
                    StackPane wrappedRoot = new StackPane(root);
                    Scene scene = new Scene(wrappedRoot);
                    scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
                    scene.getStylesheets().add(getClass().getResource("/css/dashboard.css").toExternalForm());

                    primaryStage.setScene(scene);
                    primaryStage.setTitle("MagicTech - Presales Module");
                    primaryStage.setMaximized(true);

                    createLoadingOverlay();
                    hideLoading();
                    System.out.println("✓ Presales module loaded");

                } catch (Exception e) {
                    System.err.println("Error loading Presales Module: " + e.getMessage());
                    e.printStackTrace();
                    hideLoading();
                }
            });
        });
    }

    /**
     * ✅ Show Quality Assurance Module (formerly Pricing)
     */
    public void showQualityAssuranceModule() {
        if (isTransitioning) return;

        showLoading();

        Platform.runLater(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}

            Platform.runLater(() -> {
                try {
                    immediateCleanup();

                    QualityAssuranceController qaController = new QualityAssuranceController();
                    context.getAutowireCapableBeanFactory().autowireBean(qaController);

                    ModuleConfig config = ModuleConfig.createQualityAssuranceConfig();
                    qaController.initialize(currentUser, config);

                    activeModuleController = qaController;

                    Parent root = qaController.getRootPane();
                    StackPane wrappedRoot = new StackPane(root);
                    Scene scene = new Scene(wrappedRoot);
                    scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
                    scene.getStylesheets().add(getClass().getResource("/css/dashboard.css").toExternalForm());

                    primaryStage.setScene(scene);
                    primaryStage.setTitle("MagicTech - Quality Assurance Module");
                    primaryStage.setMaximized(true);

                    createLoadingOverlay();
                    hideLoading();
                    System.out.println("✓ Quality Assurance module loaded");

                } catch (Exception e) {
                    System.err.println("Error loading Quality Assurance Module: " + e.getMessage());
                    e.printStackTrace();
                    hideLoading();
                }
            });
        });
    }

    /**
     * ✅ Show Finance Module
     */
    public void showFinanceModule() {
        if (isTransitioning) return;

        showLoading();

        Platform.runLater(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}

            Platform.runLater(() -> {
                try {
                    immediateCleanup();

                    FinanceController financeController = new FinanceController();
                    context.getAutowireCapableBeanFactory().autowireBean(financeController);

                    ModuleConfig config = ModuleConfig.createFinanceConfig();
                    financeController.initialize(currentUser, config);

                    activeModuleController = financeController;

                    Parent root = financeController.getRootPane();
                    StackPane wrappedRoot = new StackPane(root);
                    Scene scene = new Scene(wrappedRoot);
                    scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
                    scene.getStylesheets().add(getClass().getResource("/css/dashboard.css").toExternalForm());

                    primaryStage.setScene(scene);
                    primaryStage.setTitle("MagicTech - Finance Module");
                    primaryStage.setMaximized(true);

                    createLoadingOverlay();
                    hideLoading();
                    System.out.println("✓ Finance module loaded");

                } catch (Exception e) {
                    System.err.println("Error loading Finance Module: " + e.getMessage());
                    e.printStackTrace();
                    hideLoading();
                }
            });
        });
    }

    /**
     * Show Customers Module (Customer Management)
     */
    public void showCustomersModule() {
        if (isTransitioning) return;

        showLoading();

        Platform.runLater(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}

            Platform.runLater(() -> {
                try {
                    immediateCleanup();

                    com.magictech.modules.sales.CustomerManagementController customersController =
                        new com.magictech.modules.sales.CustomerManagementController();
                    context.getAutowireCapableBeanFactory().autowireBean(customersController);

                    ModuleConfig config = ModuleConfig.createSalesConfig();
                    customersController.initialize(currentUser, config);

                    activeModuleController = customersController;

                    Parent root = customersController.getRootPane();
                    StackPane wrappedRoot = new StackPane(root);
                    Scene scene = new Scene(wrappedRoot);
                    scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
                    scene.getStylesheets().add(getClass().getResource("/css/dashboard.css").toExternalForm());

                    primaryStage.setScene(scene);
                    primaryStage.setTitle("MagicTech - Customer Management");
                    primaryStage.setMaximized(true);

                    createLoadingOverlay();
                    hideLoading();
                    System.out.println("✓ Customer Management module loaded");

                } catch (Exception e) {
                    System.err.println("Error loading Customer Management Module: " + e.getMessage());
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
            case "presales":
                showPresalesModule();
                break;
            case "sales":
                showSalesModule();
                break;
            case "customers":
                showCustomersModule();
                break;
            case "qualityassurance":
            case "qa":
                showQualityAssuranceModule();
                break;
            case "finance":
                showFinanceModule();
                break;
            case "maintenance":
                showMaintenanceModule();
                break;
            case "projects":
                showProjectsModule();
                break;
            // Legacy support for old "pricing" route
            case "pricing":
                showQualityAssuranceModule();
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