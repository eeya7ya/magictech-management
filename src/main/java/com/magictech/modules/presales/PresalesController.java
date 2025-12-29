package com.magictech.modules.presales;

import com.magictech.core.auth.User;
import com.magictech.core.module.BaseModuleController;
import com.magictech.core.module.ModuleConfig;
import com.magictech.core.ui.SceneManager;
import com.magictech.modules.projects.entity.Project;
import com.magictech.modules.projects.repository.ProjectRepository;
import com.magictech.modules.sales.entity.SiteSurveyData;
import com.magictech.modules.sales.entity.WorkflowStepCompletion;
import com.magictech.modules.sales.repository.SiteSurveyDataRepository;
import com.magictech.modules.sales.service.ProjectWorkflowService;
import com.magictech.modules.sales.service.WorkflowStepService;
import com.magictech.modules.storage.service.AvailabilityRequestService;
import com.magictech.modules.storage.service.StorageService;
import com.magictech.modules.storage.ui.FastSelectionPanel;
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
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

/**
 * Presales Module Controller
 * Handles quotations, sizing/pricing, and pre-sales activities
 *
 * TABS:
 * 1. Fast Selection - Browse products with hierarchical filtering, request availability
 * 2. Sizing & Pricing Requests - Workflow requests from Sales (Step 2)
 */
@Component
public class PresalesController extends BaseModuleController {

    @Autowired
    private WorkflowStepService stepService;

    @Autowired
    private ProjectWorkflowService workflowService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private SiteSurveyDataRepository siteSurveyRepository;

    @Autowired
    private StorageService storageService;

    @Autowired
    private AvailabilityRequestService availabilityRequestService;

    private com.magictech.core.ui.components.DashboardBackgroundPane backgroundPane;
    private ListView<WorkflowRequest> pendingRequestsList;
    private FastSelectionPanel fastSelectionPanel;

    private static final String HEADER_COLOR = "#06b6d4"; // Cyan

    @Override
    protected void setupUI() {
        StackPane stackRoot = new StackPane();
        backgroundPane = new com.magictech.core.ui.components.DashboardBackgroundPane();

        BorderPane contentPane = new BorderPane();
        contentPane.setStyle("-fx-background-color: transparent;");

        // Header
        VBox header = createHeader();
        contentPane.setTop(header);

        // Main content with tabs
        TabPane tabPane = createMainTabs();
        contentPane.setCenter(tabPane);

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
                "-fx-background-color: linear-gradient(to right, " + HEADER_COLOR + ", #0891b2);" +
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
        Label iconLabel = new Label("📋");
        iconLabel.setFont(new Font(32));
        Label titleLabel = new Label("Presales Module");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 26px; -fx-font-weight: bold;");
        titleBox.getChildren().addAll(iconLabel, titleLabel);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Label userLabel = new Label("👤 " + (currentUser != null ? currentUser.getUsername() : "User"));
        userLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.9); -fx-font-size: 14px;");

        headerBar.getChildren().addAll(backButton, titleBox, userLabel);
        headerContainer.getChildren().add(headerBar);

        return headerContainer;
    }

    private TabPane createMainTabs() {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color: transparent;");

        // Tab 1: Fast Selection (Primary)
        Tab fastSelectionTab = new Tab("🎯 Fast Selection");
        fastSelectionTab.setContent(createFastSelectionContent());
        fastSelectionTab.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        // Tab 2: Sizing & Pricing Requests (Workflow)
        Tab workflowTab = new Tab("📋 Sizing & Pricing Requests");
        workflowTab.setContent(createWorkflowRequestsContent());
        workflowTab.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        tabPane.getTabs().addAll(fastSelectionTab, workflowTab);

        // Select Fast Selection by default
        tabPane.getSelectionModel().select(fastSelectionTab);

        return tabPane;
    }

    private VBox createFastSelectionContent() {
        VBox content = new VBox();
        content.setStyle("-fx-background-color: transparent;");

        fastSelectionPanel = new FastSelectionPanel();
        fastSelectionPanel.initialize(storageService, availabilityRequestService, currentUser, "PRESALES");

        // Handle add to selection callback
        fastSelectionPanel.setOnAddToSelection(items -> {
            showSuccess("✓ " + items.size() + " item(s) ready for quotation");
        });

        // Handle request created callback
        fastSelectionPanel.setOnRequestCreated(request -> {
            System.out.println("Availability request created: " + request.getId());
        });

        VBox.setVgrow(fastSelectionPanel, Priority.ALWAYS);
        content.getChildren().add(fastSelectionPanel);

        return content;
    }

    private VBox createWorkflowRequestsContent() {
        VBox workflowPanel = new VBox(20);
        workflowPanel.setPadding(new Insets(30));
        workflowPanel.setStyle("-fx-background-color: transparent;");

        // Header
        Label titleLabel = new Label("📋 Pending Sizing & Pricing Requests");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label subtitleLabel = new Label("Review site surveys and upload sizing/pricing Excel files");
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: rgba(255, 255, 255, 0.7);");

        VBox headerBox = new VBox(8);
        headerBox.getChildren().addAll(titleLabel, subtitleLabel);

        // Requests list
        pendingRequestsList = new ListView<>();
        pendingRequestsList.setPrefHeight(600);
        pendingRequestsList.setCellFactory(lv -> new WorkflowRequestCell());
        pendingRequestsList.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.6);" +
                "-fx-control-inner-background: rgba(15, 23, 42, 0.8);"
        );
        VBox.setVgrow(pendingRequestsList, Priority.ALWAYS);

        // Refresh button
        HBox buttonBox = new HBox();
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        Button refreshBtn = new Button("🔄 Refresh Requests");
        refreshBtn.setStyle("-fx-background-color: " + HEADER_COLOR + "; -fx-text-fill: white; " +
                          "-fx-padding: 12 24; -fx-font-size: 14px; -fx-font-weight: bold; " +
                          "-fx-background-radius: 8;");
        refreshBtn.setOnAction(e -> loadPendingRequests());
        buttonBox.getChildren().add(refreshBtn);

        workflowPanel.getChildren().addAll(headerBox, pendingRequestsList, buttonBox);

        // Load data
        Platform.runLater(this::loadPendingRequests);

        return workflowPanel;
    }

    private void loadPendingRequests() {
        System.out.println("📥 Loading pending requests for PRESALES module...");

        try {
            List<WorkflowStepCompletion> pendingSteps =
                stepService.getPendingExternalActions("PRESALES");

            System.out.println("   Found " + pendingSteps.size() + " pending step(s)");

            pendingRequestsList.getItems().clear();

            for (WorkflowStepCompletion step : pendingSteps) {
                Optional<Project> projectOpt = projectRepository.findById(step.getProjectId());
                if (projectOpt.isPresent()) {
                    Project project = projectOpt.get();
                    WorkflowRequest request = new WorkflowRequest(step, project);
                    pendingRequestsList.getItems().add(request);
                }
            }

            if (pendingRequestsList.getItems().isEmpty()) {
                showToastInfo("No pending presales requests");
            }
        } catch (Exception ex) {
            System.err.println("ERROR loading pending requests: " + ex.getMessage());
            showError("Failed to load requests: " + ex.getMessage());
        }
    }

    private void handleSubmitSizingPricing(WorkflowRequest request) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Upload Sizing & Pricing Excel");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls")
        );

        File file = fileChooser.showOpenDialog(getRootPane().getScene().getWindow());
        if (file != null) {
            try {
                byte[] fileData = Files.readAllBytes(file.toPath());
                workflowService.submitSizingPricing(
                    request.step.getWorkflowId(),
                    fileData,
                    file.getName(),
                    currentUser
                );

                showSuccess("✓ Sizing & Pricing submitted successfully!");
                loadPendingRequests();
            } catch (Exception ex) {
                showError("Failed to submit: " + ex.getMessage());
            }
        }
    }

    private void handleDownloadSiteSurvey(SiteSurveyData survey) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Site Survey Excel");
        fileChooser.setInitialFileName(survey.getFileName());
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls")
        );

        File file = fileChooser.showSaveDialog(getRootPane().getScene().getWindow());
        if (file != null) {
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(survey.getExcelFile());
                showSuccess("✓ Site survey downloaded successfully!");
            } catch (Exception ex) {
                showError("Failed to download: " + ex.getMessage());
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
        // Data is loaded when tabs are created
    }

    @Override
    public void refresh() {
        if (fastSelectionPanel != null) {
            fastSelectionPanel.refresh();
        }
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
                        "-fx-border-color: rgba(6, 182, 212, 0.3);" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-width: 1;"
                );

                VBox info = new VBox(5);
                Label projectLabel = new Label("📋 " + request.project.getProjectName());
                projectLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: white;");

                Label locationLabel = new Label("📍 " + request.project.getProjectLocation());
                locationLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 12px;");

                Label statusLabel = new Label("⏳ Waiting for Sizing & Pricing");
                statusLabel.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 12px;");

                info.getChildren().addAll(projectLabel, locationLabel, statusLabel);
                HBox.setHgrow(info, Priority.ALWAYS);

                // Check if site survey exists
                Optional<SiteSurveyData> surveyOpt = siteSurveyRepository
                    .findByWorkflowIdAndActiveTrue(request.step.getWorkflowId());

                HBox buttonsBox = new HBox(10);
                buttonsBox.setAlignment(Pos.CENTER_RIGHT);

                if (surveyOpt.isPresent()) {
                    Button downloadBtn = new Button("📥 Site Survey");
                    downloadBtn.setStyle(
                            "-fx-background-color: #8b5cf6;" +
                            "-fx-text-fill: white;" +
                            "-fx-padding: 8 16;" +
                            "-fx-background-radius: 6;" +
                            "-fx-font-size: 12px;"
                    );
                    downloadBtn.setOnAction(e -> handleDownloadSiteSurvey(surveyOpt.get()));
                    buttonsBox.getChildren().add(downloadBtn);
                }

                Button submitBtn = new Button("📤 Upload Sizing");
                submitBtn.setStyle(
                        "-fx-background-color: " + HEADER_COLOR + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 8 16;" +
                        "-fx-background-radius: 6;" +
                        "-fx-font-size: 12px;"
                );
                submitBtn.setOnAction(e -> handleSubmitSizingPricing(request));
                buttonsBox.getChildren().add(submitBtn);

                cell.getChildren().addAll(info, buttonsBox);
                setGraphic(cell);
                setStyle("-fx-background-color: transparent;");
            }
        }
    }
}
