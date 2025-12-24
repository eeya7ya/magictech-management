package com.magictech.modules.finance;

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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

/**
 * Finance Module Controller
 * Handles invoicing, payments, and financial tracking
 *
 * WORKFLOW INTEGRATION:
 * - Receives "Bank Guarantee" requests from Sales (Step 3)
 * - Uploads bank guarantee Excel sheets
 * - Notifies sales user when complete
 */
@Component
public class FinanceController extends BaseStorageModuleController {

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
        return ModuleStorageConfig.FINANCE;
    }

    @Override
    protected String getHeaderColor() {
        return "#eab308"; // Yellow color
    }

    @Override
    protected String getModuleIcon() {
        return "💰";
    }

    @Override
    public void initialize(User user, com.magictech.core.module.ModuleConfig config) {
        this.currentUser = user;
        super.initialize(user, config);
        Platform.runLater(this::addWorkflowRequestsTab);
    }

    @Override
    protected void handleAddItem() {
        showToastInfo("Finance: Add item functionality will be implemented here");
    }

    @Override
    protected void handleEditItem() {
        showToastInfo("Finance: Edit item functionality will be implemented here");
    }

    @Override
    protected void handleBulkDelete() {
        showToastInfo("Finance: Bulk delete functionality will be implemented here");
    }

    /**
     * Add workflow requests as a separate tab
     * FIXED: Properly extracts content area while preserving header with back button
     */
    private void addWorkflowRequestsTab() {
        System.out.println("🔧 Adding workflow requests tab to Finance UI...");

        try {
            BorderPane rootPane = getRootPane();
            javafx.scene.Node centerNode = rootPane.getCenter();

            // The center is a StackPane containing backgroundPane and contentPane
            if (!(centerNode instanceof StackPane)) {
                System.err.println("❌ Expected StackPane, got: " + centerNode.getClass().getName());
                return;
            }
            StackPane stackRoot = (StackPane) centerNode;

            // Find the contentPane (BorderPane) inside the StackPane
            BorderPane contentPane = null;
            for (javafx.scene.Node child : stackRoot.getChildren()) {
                if (child instanceof BorderPane) {
                    contentPane = (BorderPane) child;
                    break;
                }
            }

            if (contentPane == null) {
                System.err.println("❌ Could not find contentPane in StackPane");
                return;
            }

            // Get the actual content area (VBox with toolbar and table)
            javafx.scene.Node mainContent = contentPane.getCenter();

            // Create TabPane
            TabPane tabPane = new TabPane();
            tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
            tabPane.setStyle("-fx-background-color: transparent;");

            // Tab 1: Storage Items (existing functionality)
            Tab storageTab = new Tab("📦 Storage Items");
            storageTab.setContent(mainContent); // Move only the content area to tab
            storageTab.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

            // Tab 2: Bank Guarantee Requests (new dedicated tab)
            Tab workflowTab = new Tab("💰 Bank Guarantee Requests");
            workflowTab.setContent(createWorkflowRequestsContent());
            workflowTab.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

            tabPane.getTabs().addAll(storageTab, workflowTab);

            // Set workflow tab as selected by default
            tabPane.getSelectionModel().select(workflowTab);

            // Replace only the center content of contentPane with TabPane
            // This preserves the header (top) with back button
            contentPane.setCenter(tabPane);
            System.out.println("✅ Workflow requests tab added successfully (header preserved)");

            // Initial load
            loadPendingRequests();

        } catch (Exception e) {
            System.err.println("❌ ERROR adding workflow requests tab:");
            e.printStackTrace();
        }
    }

    private VBox createWorkflowRequestsContent() {
        VBox workflowPanel = new VBox(20);
        workflowPanel.setPadding(new Insets(30));
        workflowPanel.setStyle("-fx-background-color: transparent;");

        // Header
        Label titleLabel = new Label("💰 Pending Bank Guarantee Requests");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label subtitleLabel = new Label("Upload bank guarantee Excel files for pending projects");
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
        refreshBtn.setStyle("-fx-background-color: #eab308; -fx-text-fill: white; " +
                          "-fx-padding: 12 24; -fx-font-size: 14px; -fx-font-weight: bold; " +
                          "-fx-background-radius: 8;");
        refreshBtn.setOnAction(e -> loadPendingRequests());
        buttonBox.getChildren().add(refreshBtn);

        workflowPanel.getChildren().addAll(headerBox, pendingRequestsList, buttonBox);
        return workflowPanel;
    }

    private void loadPendingRequests() {
        List<WorkflowStepCompletion> pendingSteps =
            stepService.getPendingExternalActions("FINANCE");

        pendingRequestsList.getItems().clear();

        for (WorkflowStepCompletion step : pendingSteps) {
            Optional<Project> projectOpt = projectRepository.findById(step.getProjectId());
            projectOpt.ifPresent(project -> {
                WorkflowRequest request = new WorkflowRequest(step, project);
                pendingRequestsList.getItems().add(request);
            });
        }

        if (pendingRequestsList.getItems().isEmpty()) {
            showToastInfo("No pending bank guarantee requests");
        }
    }

    private void handleSubmitBankGuarantee(WorkflowRequest request) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Upload Bank Guarantee Excel");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls")
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

                showSuccess("✓ Bank Guarantee submitted successfully!");
                loadPendingRequests();
            } catch (Exception ex) {
                showError("Failed to submit: " + ex.getMessage());
            }
        }
    }

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
                cell.setPadding(new Insets(10));
                cell.setStyle("-fx-background-color: white; -fx-background-radius: 5;");

                VBox info = new VBox(5);
                Label projectLabel = new Label("Project: " + request.project.getProjectName());
                projectLabel.setStyle("-fx-font-weight: bold;");

                Label statusLabel = new Label("Status: Waiting for Bank Guarantee");
                statusLabel.setStyle("-fx-text-fill: #f39c12;");

                info.getChildren().addAll(projectLabel, statusLabel);
                HBox.setHgrow(info, Priority.ALWAYS);

                Button submitBtn = new Button("📤 Upload Bank Guarantee");
                submitBtn.setStyle("-fx-background-color: #eab308; -fx-text-fill: white;");
                submitBtn.setOnAction(e -> handleSubmitBankGuarantee(request));

                cell.getChildren().addAll(info, submitBtn);
                setGraphic(cell);
            }
        }
    }
}
