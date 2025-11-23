package com.magictech.modules.presales;

import com.magictech.core.auth.User;
import com.magictech.modules.projects.entity.Project;
import com.magictech.modules.projects.repository.ProjectRepository;
import com.magictech.modules.sales.entity.WorkflowStepCompletion;
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

        // Add workflow requests section to UI
        Platform.runLater(this::addWorkflowRequestsSection);
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
     * Add workflow requests section to the module UI
     */
    private void addWorkflowRequestsSection() {
        // Create workflow requests panel
        VBox workflowPanel = new VBox(15);
        workflowPanel.setPadding(new Insets(20));
        workflowPanel.setStyle("-fx-background-color: rgba(6, 182, 212, 0.1); " +
                              "-fx-background-radius: 10; -fx-border-color: #06b6d4; " +
                              "-fx-border-radius: 10; -fx-border-width: 2;");

        Label titleLabel = new Label("📋 Pending Sizing & Pricing Requests");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #06b6d4;");

        // Requests list
        pendingRequestsList = new ListView<>();
        pendingRequestsList.setPrefHeight(200);
        pendingRequestsList.setCellFactory(lv -> new WorkflowRequestCell());

        // Refresh button
        Button refreshBtn = new Button("🔄 Refresh Requests");
        refreshBtn.setStyle("-fx-background-color: #06b6d4; -fx-text-fill: white;");
        refreshBtn.setOnAction(e -> loadPendingRequests());

        workflowPanel.getChildren().addAll(titleLabel, pendingRequestsList, refreshBtn);

        // Add to main content
        try {
            BorderPane rootPane = getRootPane();
            if (rootPane.getCenter() instanceof VBox) {
                VBox mainContent = (VBox) rootPane.getCenter();
                if (mainContent.getChildren().size() > 0) {
                    mainContent.getChildren().add(1, workflowPanel);
                }
            }
        } catch (Exception e) {
            // Fallback: just load requests
            loadPendingRequests();
        }

        // Initial load
        loadPendingRequests();
    }

    /**
     * Load pending workflow requests for presales
     */
    private void loadPendingRequests() {
        List<WorkflowStepCompletion> pendingSteps =
            stepService.getPendingExternalActions("PRESALES");

        pendingRequestsList.getItems().clear();

        for (WorkflowStepCompletion step : pendingSteps) {
            Optional<Project> projectOpt = projectRepository.findById(step.getProjectId());
            projectOpt.ifPresent(project -> {
                WorkflowRequest request = new WorkflowRequest(step, project);
                pendingRequestsList.getItems().add(request);
            });
        }

        if (pendingRequestsList.getItems().isEmpty()) {
            showToastInfo("No pending presales requests");
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

                Button submitBtn = new Button("📤 Upload Sizing & Pricing");
                submitBtn.setStyle("-fx-background-color: #06b6d4; -fx-text-fill: white;");
                submitBtn.setOnAction(e -> handleSubmitSizingPricing(request));

                cell.getChildren().addAll(info, submitBtn);
                setGraphic(cell);
            }
        }
    }
}
