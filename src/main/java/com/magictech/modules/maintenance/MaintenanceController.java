package com.magictech.modules.maintenance;

import com.magictech.core.module.BaseModuleController;
import com.magictech.core.ui.SceneManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.text.Font;

/**
 * Maintenance Module Controller
 * PLACEHOLDER - Full functionality will be added in future update.
 *
 * Planned features:
 * - Service request management
 * - Equipment tracking
 * - Maintenance scheduling
 * - Client communication
 */
public class MaintenanceController extends BaseModuleController {

    private com.magictech.core.ui.components.DashboardBackgroundPane backgroundPane;
    private static final String HEADER_COLOR = "#22c55e"; // Green

    @Override
    protected void setupUI() {
        StackPane stackRoot = new StackPane();
        backgroundPane = new com.magictech.core.ui.components.DashboardBackgroundPane();

        BorderPane contentPane = new BorderPane();
        contentPane.setStyle("-fx-background-color: transparent;");

        // Header
        VBox header = createHeader();
        contentPane.setTop(header);

        // Coming Soon Content
        VBox comingSoonContent = createComingSoonContent();
        contentPane.setCenter(comingSoonContent);

        stackRoot.getChildren().addAll(backgroundPane, contentPane);

        BorderPane root = getRootPane();
        root.setCenter(stackRoot);
        root.setStyle("-fx-background-color: transparent;");
    }

    private VBox createHeader() {
        VBox headerContainer = new VBox();

        HBox headerBar = new HBox();
        headerBar.setAlignment(Pos.CENTER_LEFT);
        headerBar.setSpacing(20);
        headerBar.setPadding(new Insets(20, 30, 20, 30));
        headerBar.setStyle(
                "-fx-background-color: linear-gradient(to right, " + HEADER_COLOR + ", #16a34a);" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 15, 0, 0, 3);"
        );

        Button backButton = new Button("← Back");
        backButton.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.2);" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 10 20;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;"
        );
        backButton.setOnAction(e -> handleBack());

        HBox titleBox = new HBox(15);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        Label iconLabel = new Label("🔧");
        iconLabel.setFont(new Font(32));
        Label titleLabel = new Label("Maintenance Module");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 26px; -fx-font-weight: bold;");
        titleBox.getChildren().addAll(iconLabel, titleLabel);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Label userLabel = new Label("👤 " + (currentUser != null ? currentUser.getUsername() : "User"));
        userLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.9); -fx-font-size: 14px;");

        headerBar.getChildren().addAll(backButton, titleBox, userLabel);
        headerContainer.getChildren().add(headerBar);

        return headerContainer;
    }

    private VBox createComingSoonContent() {
        VBox content = new VBox(30);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(60));
        content.setStyle("-fx-background-color: transparent;");

        // Coming Soon Card
        VBox card = new VBox(25);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(50, 80, 50, 80));
        card.setMaxWidth(600);
        card.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.8);" +
                "-fx-background-radius: 20;" +
                "-fx-border-color: rgba(34, 197, 94, 0.4);" +
                "-fx-border-radius: 20;" +
                "-fx-border-width: 2;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 20, 0, 0, 10);"
        );

        // Icon
        Label iconLabel = new Label("🚧");
        iconLabel.setFont(new Font(72));

        // Title
        Label titleLabel = new Label("Coming Soon");
        titleLabel.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Subtitle
        Label subtitleLabel = new Label("Maintenance Module is under development");
        subtitleLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: rgba(255, 255, 255, 0.7);");

        // Feature list
        VBox featureList = new VBox(12);
        featureList.setAlignment(Pos.CENTER_LEFT);
        featureList.setPadding(new Insets(20, 0, 20, 40));

        String[] features = {
                "🔧 Service Request Management",
                "📦 Equipment Tracking",
                "📅 Maintenance Scheduling",
                "👥 Client Communication",
                "📊 Analytics & Reporting"
        };

        for (String feature : features) {
            Label featureLabel = new Label(feature);
            featureLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: rgba(255, 255, 255, 0.8);");
            featureList.getChildren().add(featureLabel);
        }

        // Info text
        Label infoLabel = new Label("This module will be available in a future update.");
        infoLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255, 255, 255, 0.5); -fx-font-style: italic;");

        card.getChildren().addAll(iconLabel, titleLabel, subtitleLabel, featureList, infoLabel);
        content.getChildren().add(card);

        return content;
    }

    private void handleBack() {
        if (backgroundPane != null) {
            backgroundPane.stopAnimation();
        }
        SceneManager.getInstance().showMainDashboard();
    }

    @Override
    protected void loadData() {
        // No data to load - placeholder module
    }

    @Override
    public void refresh() {
        // Nothing to refresh
    }

    public void immediateCleanup() {
        if (backgroundPane != null) {
            backgroundPane.stopAnimation();
            backgroundPane = null;
        }
    }
}
