package com.magictech.modules.finance;

import com.magictech.core.auth.User;
import com.magictech.core.module.BaseModuleController;
import com.magictech.core.ui.SceneManager;
import com.magictech.modules.projects.entity.Project;
import com.magictech.modules.projects.repository.ProjectRepository;
import com.magictech.modules.sales.entity.WorkflowStepCompletion;
import com.magictech.modules.sales.service.ProjectWorkflowService;
import com.magictech.modules.sales.service.WorkflowStepService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

/**
 * Finance Module Controller
 * Handles invoicing, payments, bank guarantees, and financial tracking
 *
 * WORKFLOW INTEGRATION:
 * - Receives "Bank Guarantee" requests from Sales (Step 3)
 * - Uploads bank guarantee Excel sheets
 * - Notifies sales user when complete
 *
 * NO STORAGE ACCESS - Finance only handles workflow requests
 */
@Component
public class FinanceController extends BaseModuleController {

    @Autowired
    private WorkflowStepService stepService;

    @Autowired
    private ProjectWorkflowService workflowService;

    @Autowired
    private ProjectRepository projectRepository;

    private com.magictech.core.ui.components.DashboardBackgroundPane backgroundPane;
    private ListView<WorkflowRequest> pendingRequestsList;

    private static final String HEADER_COLOR = "#eab308"; // Yellow/Gold

    @Override
    protected void setupUI() {
        StackPane stackRoot = new StackPane();
        backgroundPane = new com.magictech.core.ui.components.DashboardBackgroundPane();

        BorderPane contentPane = new BorderPane();
        contentPane.setStyle("-fx-background-color: transparent;");

        // Header
        VBox header = createHeader();
        contentPane.setTop(header);

        // Main content - Bank Guarantee Requests only
        VBox mainContent = createBankGuaranteeContent();
        contentPane.setCenter(mainContent);

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
                "-fx-background-color: linear-gradient(to right, " + HEADER_COLOR + ", #ca8a04);" +
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
        Label iconLabel = new Label("💰");
        iconLabel.setFont(new Font(32));
        Label titleLabel = new Label("Finance Module");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 26px; -fx-font-weight: bold;");
        titleBox.getChildren().addAll(iconLabel, titleLabel);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Label userLabel = new Label("👤 " + (currentUser != null ? currentUser.getUsername() : "User"));
        userLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.9); -fx-font-size: 14px;");

        headerBar.getChildren().addAll(backButton, titleBox, userLabel);
        headerContainer.getChildren().add(headerBar);

        return headerContainer;
    }

    private VBox createBankGuaranteeContent() {
        VBox mainContent = new VBox(20);
        mainContent.setPadding(new Insets(30));
        mainContent.setStyle("-fx-background-color: transparent;");

        // Header section
        VBox headerSection = new VBox(8);
        Label titleLabel = new Label("💰 Bank Guarantee Requests");
        titleLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label subtitleLabel = new Label("Upload bank guarantee documents for pending project workflows");
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: rgba(255, 255, 255, 0.7);");

        headerSection.getChildren().addAll(titleLabel, subtitleLabel);

        // Stats cards
        HBox statsBox = createStatsCards();

        // Requests list
        VBox listSection = new VBox(15);
        listSection.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.6);" +
                "-fx-background-radius: 16;" +
                "-fx-border-color: rgba(234, 179, 8, 0.3);" +
                "-fx-border-radius: 16;" +
                "-fx-border-width: 1;" +
                "-fx-padding: 20;"
        );
        VBox.setVgrow(listSection, Priority.ALWAYS);

        HBox listHeader = new HBox(15);
        listHeader.setAlignment(Pos.CENTER_LEFT);

        Label listTitle = new Label("📋 Pending Requests");
        listTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        HBox.setHgrow(listTitle, Priority.ALWAYS);

        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.setStyle(
                "-fx-background-color: " + HEADER_COLOR + ";" +
                "-fx-text-fill: white;" +
                "-fx-padding: 10 20;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 8;"
        );
        refreshBtn.setOnAction(e -> loadPendingRequests());

        listHeader.getChildren().addAll(listTitle, refreshBtn);

        pendingRequestsList = new ListView<>();
        pendingRequestsList.setCellFactory(lv -> new WorkflowRequestCell());
        pendingRequestsList.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-control-inner-background: rgba(15, 23, 42, 0.6);"
        );
        VBox.setVgrow(pendingRequestsList, Priority.ALWAYS);

        listSection.getChildren().addAll(listHeader, pendingRequestsList);

        mainContent.getChildren().addAll(headerSection, statsBox, listSection);

        // Load data
        Platform.runLater(this::loadPendingRequests);

        return mainContent;
    }

    private HBox createStatsCards() {
        HBox statsBox = new HBox(20);
        statsBox.setPadding(new Insets(10, 0, 10, 0));

        // Pending card
        VBox pendingCard = createStatCard("⏳", "Pending", "0", "#f59e0b");
        pendingCard.setId("pendingCard");

        // Completed card
        VBox completedCard = createStatCard("✅", "Completed", "0", "#22c55e");
        completedCard.setId("completedCard");

        // Total card
        VBox totalCard = createStatCard("📊", "Total Processed", "0", "#8b5cf6");
        totalCard.setId("totalCard");

        statsBox.getChildren().addAll(pendingCard, completedCard, totalCard);

        return statsBox;
    }

    private VBox createStatCard(String icon, String label, String value, String color) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20));
        card.setPrefWidth(200);
        card.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.7);" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: " + color + "40;" +
                "-fx-border-radius: 12;" +
                "-fx-border-width: 2;"
        );

        Label iconLabel = new Label(icon);
        iconLabel.setFont(new Font(28));

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        valueLabel.setId("value");

        Label textLabel = new Label(label);
        textLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255, 255, 255, 0.7);");

        card.getChildren().addAll(iconLabel, valueLabel, textLabel);

        return card;
    }

    private void loadPendingRequests() {
        System.out.println("📥 Loading pending requests for FINANCE module...");

        try {
            List<WorkflowStepCompletion> pendingSteps =
                stepService.getPendingExternalActions("FINANCE");

            System.out.println("   Found " + pendingSteps.size() + " pending step(s)");

            pendingRequestsList.getItems().clear();

            for (WorkflowStepCompletion step : pendingSteps) {
                Optional<Project> projectOpt = projectRepository.findById(step.getProjectId());
                projectOpt.ifPresent(project -> {
                    WorkflowRequest request = new WorkflowRequest(step, project);
                    pendingRequestsList.getItems().add(request);
                });
            }

            // Update stats (simplified)
            updateStats(pendingSteps.size());

            if (pendingRequestsList.getItems().isEmpty()) {
                showToastInfo("No pending bank guarantee requests");
            }
        } catch (Exception ex) {
            System.err.println("ERROR loading pending requests: " + ex.getMessage());
            showError("Failed to load requests: " + ex.getMessage());
        }
    }

    private void updateStats(int pendingCount) {
        // Update stats display (find labels by traversing the scene graph)
        // This is a simplified approach
        Platform.runLater(() -> {
            // The stats are shown in the UI directly
            System.out.println("Stats: " + pendingCount + " pending requests");
        });
    }

    private void handleSubmitBankGuarantee(WorkflowRequest request) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Upload Bank Guarantee Document");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls"),
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File file = fileChooser.showOpenDialog(getRootPane().getScene().getWindow());
        if (file != null) {
            try {
                byte[] fileData = Files.readAllBytes(file.toPath());
                workflowService.submitBankGuarantee(
                    request.step.getWorkflowId(),
                    fileData,
                    file.getName(),
                    currentUser
                );

                showSuccess("✓ Bank Guarantee submitted successfully!\nSales team has been notified.");
                loadPendingRequests();
            } catch (Exception ex) {
                showError("Failed to submit: " + ex.getMessage());
            }
        }
    }

    private void handleBack() {
        if (backgroundPane != null) {
            backgroundPane.stopAnimation();
        }
        SceneManager.getInstance().showMainDashboard();
    }

    @Override
    protected void loadData() {
        // Data is loaded in setupUI
    }

    @Override
    public void refresh() {
        loadPendingRequests();
    }

    public void immediateCleanup() {
        if (backgroundPane != null) {
            backgroundPane.stopAnimation();
            backgroundPane = null;
        }
    }


    // Inner classes
    private static class WorkflowRequest {
        final WorkflowStepCompletion step;
        final Project project;

        WorkflowRequest(WorkflowStepCompletion step, Project project) {
            this.step = step;
            this.project = project;
        }
    }

    private class WorkflowRequestCell extends ListCell<WorkflowRequest> {
        @Override
        protected void updateItem(WorkflowRequest request, boolean empty) {
            super.updateItem(request, empty);

            if (empty || request == null) {
                setGraphic(null);
            } else {
                HBox cell = new HBox(15);
                cell.setAlignment(Pos.CENTER_LEFT);
                cell.setPadding(new Insets(15));
                cell.setStyle(
                        "-fx-background-color: rgba(30, 41, 59, 0.8);" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: rgba(234, 179, 8, 0.3);" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-width: 1;"
                );

                // Project info
                VBox info = new VBox(5);

                Label projectLabel = new Label("📋 " + request.project.getProjectName());
                projectLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: white;");

                Label locationLabel = new Label("📍 " + request.project.getProjectLocation());
                locationLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 12px;");

                Label stepLabel = new Label("📝 Workflow Step 3: Bank Guarantee Required");
                stepLabel.setStyle("-fx-text-fill: " + HEADER_COLOR + "; -fx-font-size: 12px;");

                Label dateLabel = new Label("🕐 Requested: " +
                        (request.step.getCreatedAt() != null ?
                                request.step.getCreatedAt().toLocalDate().toString() : "N/A"));
                dateLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.5); -fx-font-size: 11px;");

                info.getChildren().addAll(projectLabel, locationLabel, stepLabel, dateLabel);
                HBox.setHgrow(info, Priority.ALWAYS);

                // Action button
                Button submitBtn = new Button("📤 Upload Bank Guarantee");
                submitBtn.setStyle(
                        "-fx-background-color: " + HEADER_COLOR + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 12 20;" +
                        "-fx-background-radius: 8;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;"
                );
                submitBtn.setOnAction(e -> handleSubmitBankGuarantee(request));

                cell.getChildren().addAll(info, submitBtn);
                setGraphic(cell);
                setStyle("-fx-background-color: transparent;");
            }
        }
    }
}
