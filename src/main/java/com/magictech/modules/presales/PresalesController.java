package com.magictech.modules.presales;

import com.magictech.core.auth.User;
import com.magictech.modules.projects.entity.Project;
import com.magictech.modules.projects.repository.ProjectRepository;
import com.magictech.modules.sales.entity.SiteSurveyData;
import com.magictech.modules.sales.entity.WorkflowStepCompletion;
import com.magictech.modules.sales.repository.SiteSurveyDataRepository;
import com.magictech.modules.sales.service.ProjectWorkflowService;
import com.magictech.modules.sales.service.WorkflowStepService;
import com.magictech.modules.storage.base.BaseStorageModuleController;
import com.magictech.modules.storage.config.ModuleStorageConfig;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
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
 * Handles quotations, initial customer contact, and pre-sales activities
 * Extends BaseStorageModuleController for standard storage-based operations
 *
 * WORKFLOW INTEGRATION:
 * - Receives "Selection & Design" requests from Sales (Step 2)
 * - Uploads "Sizing & Pricing" Excel sheets
 * - Notifies sales user when complete
 */
@Component
public class PresalesController extends BaseStorageModuleController {

    @Autowired
    private WorkflowStepService stepService;

    @Autowired
    private ProjectWorkflowService workflowService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private SiteSurveyDataRepository siteSurveyRepository;

    private User currentUser;
    private ListView<WorkflowRequest> pendingRequestsList;

    @Override
    protected ModuleStorageConfig getModuleConfig() {
        return ModuleStorageConfig.PRESALES;
    }

    @Override
    protected String getHeaderColor() {
        return "#06b6d4"; // Cyan color
    }

    @Override
    protected String getModuleIcon() {
        return "📋";
    }

    @Override
    public void initialize(User user, com.magictech.core.module.ModuleConfig config) {
        this.currentUser = user;
        super.initialize(user, config);

        // CRITICAL FIX: Add workflow requests TAB instead of inline section
        Platform.runLater(() -> {
            try {
                addWorkflowRequestsTab();
                System.out.println("✅ Workflow requests tab added to Presales UI");
            } catch (Exception ex) {
                System.err.println("❌ ERROR adding workflow requests tab: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
    }

    @Override
    protected void handleAddItem() {
        showToastInfo("Presales: Add item functionality will be implemented here");
    }

    @Override
    protected void handleEditItem() {
        showToastInfo("Presales: Edit item functionality will be implemented here");
    }

    @Override
    protected void handleBulkDelete() {
        showToastInfo("Presales: Bulk delete functionality will be implemented here");
    }

    /**
     * Add workflow requests as a separate tab
     * IMPROVED UX: Users can switch between Storage Items and Workflow Requests
     */
    private void addWorkflowRequestsTab() {
        System.out.println("🔧 Adding workflow requests tab to Presales UI...");

        try {
            BorderPane rootPane = getRootPane();
            javafx.scene.Node centerNode = rootPane.getCenter();

            // Create TabPane
            TabPane tabPane = new TabPane();
            tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
            tabPane.setStyle("-fx-background-color: transparent;");

            // Tab 1: Storage Items (existing functionality)
            Tab storageTab = new Tab("📦 Storage Items");
            storageTab.setContent(centerNode); // Move existing content to tab
            storageTab.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

            // Tab 2: Workflow Requests (new dedicated tab)
            Tab workflowTab = new Tab("📋 Sizing & Pricing Requests");
            workflowTab.setContent(createWorkflowRequestsContent());
            workflowTab.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

            tabPane.getTabs().addAll(storageTab, workflowTab);

            // Set workflow tab as selected by default (since it's the main function)
            tabPane.getSelectionModel().select(workflowTab);

            // Replace center content with TabPane
            rootPane.setCenter(tabPane);
            System.out.println("✅ Workflow requests tab added successfully");

            // Initial load
            loadPendingRequests();

        } catch (Exception e) {
            System.err.println("❌ ERROR adding workflow requests tab:");
            e.printStackTrace();
        }
    }

    /**
     * Create the content for the workflow requests tab
     */
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
        refreshBtn.setStyle("-fx-background-color: #06b6d4; -fx-text-fill: white; " +
                          "-fx-padding: 12 24; -fx-font-size: 14px; -fx-font-weight: bold; " +
                          "-fx-background-radius: 8;");
        refreshBtn.setOnAction(e -> {
            System.out.println("🔄 Refreshing pending requests...");
            loadPendingRequests();
        });
        buttonBox.getChildren().add(refreshBtn);

        workflowPanel.getChildren().addAll(headerBox, pendingRequestsList, buttonBox);
        return workflowPanel;
    }

    /**
     * Load pending workflow requests for presales
     */
    private void loadPendingRequests() {
        System.out.println("\n📥 Loading pending requests for PRESALES module...");

        try {
            List<WorkflowStepCompletion> pendingSteps =
                stepService.getPendingExternalActions("PRESALES");

            System.out.println("   Found " + pendingSteps.size() + " pending step(s) from database");

            pendingRequestsList.getItems().clear();

            for (WorkflowStepCompletion step : pendingSteps) {
                System.out.println("   Processing step: workflow_id=" + step.getWorkflowId() +
                                 ", project_id=" + step.getProjectId() +
                                 ", step_number=" + step.getStepNumber());

                Optional<Project> projectOpt = projectRepository.findById(step.getProjectId());
                if (projectOpt.isPresent()) {
                    Project project = projectOpt.get();
                    WorkflowRequest request = new WorkflowRequest(step, project);
                    pendingRequestsList.getItems().add(request);
                    System.out.println("   ✅ Added request for project: " + project.getProjectName());
                } else {
                    System.err.println("   ❌ Project not found for ID: " + step.getProjectId());
                }
            }

            if (pendingRequestsList.getItems().isEmpty()) {
                System.out.println("⚠️ No pending presales requests found");
                showToastInfo("No pending presales requests");
            } else {
                System.out.println("✅ Loaded " + pendingRequestsList.getItems().size() + " pending request(s)");
            }
        } catch (Exception ex) {
            System.err.println("❌ ERROR loading pending requests:");
            ex.printStackTrace();
            showError("Failed to load requests: " + ex.getMessage());
        }
    }

    /**
     * Handle sizing & pricing submission
     */
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

    /**
     * Handle downloading site survey from Step 1
     */
    private void handleDownloadSiteSurvey(SiteSurveyData survey) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Site Survey Excel (from Step 1)");
        fileChooser.setInitialFileName(survey.getFileName());
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls")
        );

        File file = fileChooser.showSaveDialog(getRootPane().getScene().getWindow());
        if (file != null) {
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(survey.getExcelFile());
                showSuccess("✓ Site survey downloaded successfully!\n" +
                           "File: " + survey.getFileName() + "\n" +
                           "Uploaded by: " + survey.getSurveyDoneByUser());
            } catch (Exception ex) {
                showError("Failed to download site survey: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    /**
     * Workflow request data class
     */
    private static class WorkflowRequest {
        final WorkflowStepCompletion step;
        final Project project;

        WorkflowRequest(WorkflowStepCompletion step, Project project) {
            this.step = step;
            this.project = project;
        }
    }

    /**
     * Custom cell for workflow requests
     */
    private class WorkflowRequestCell extends ListCell<WorkflowRequest> {
        @Override
        protected void updateItem(WorkflowRequest request, boolean empty) {
            super.updateItem(request, empty);

            if (empty || request == null) {
                setGraphic(null);
            } else {
                HBox cell = new HBox(15);
                cell.setAlignment(Pos.CENTER_LEFT);
                cell.setPadding(new Insets(10));
                cell.setStyle("-fx-background-color: white; -fx-background-radius: 5;");

                VBox info = new VBox(5);
                Label projectLabel = new Label("Project: " + request.project.getProjectName());
                projectLabel.setStyle("-fx-font-weight: bold;");

                Label statusLabel = new Label("Status: Waiting for Sizing & Pricing");
                statusLabel.setStyle("-fx-text-fill: #f39c12;");

                info.getChildren().addAll(projectLabel, statusLabel);
                HBox.setHgrow(info, Priority.ALWAYS);

                // Check if site survey exists for this workflow
                Optional<SiteSurveyData> surveyOpt = siteSurveyRepository
                    .findByWorkflowIdAndActiveTrue(request.step.getWorkflowId());

                HBox buttonsBox = new HBox(10);

                if (surveyOpt.isPresent()) {
                    // Add download site survey button
                    Button downloadBtn = new Button("📥 Download Site Survey");
                    downloadBtn.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white;");
                    downloadBtn.setOnAction(e -> handleDownloadSiteSurvey(surveyOpt.get()));
                    buttonsBox.getChildren().add(downloadBtn);
                }

                Button submitBtn = new Button("📤 Upload Sizing & Pricing");
                submitBtn.setStyle("-fx-background-color: #06b6d4; -fx-text-fill: white;");
                submitBtn.setOnAction(e -> handleSubmitSizingPricing(request));
                buttonsBox.getChildren().add(submitBtn);

                cell.getChildren().addAll(info, buttonsBox);
                setGraphic(cell);
            }
        }
    }
}
