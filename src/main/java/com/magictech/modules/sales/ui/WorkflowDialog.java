package com.magictech.modules.sales.ui;

import com.magictech.core.auth.User;
import com.magictech.modules.projects.entity.Project;
import com.magictech.modules.sales.entity.MissingItemRequest;
import com.magictech.modules.sales.entity.ProjectWorkflow;
import com.magictech.modules.sales.entity.SiteSurveyData;
import com.magictech.modules.sales.entity.WorkflowStepCompletion;
import com.magictech.modules.sales.repository.SiteSurveyDataRepository;
import com.magictech.modules.sales.service.ProjectWorkflowService;
import com.magictech.modules.sales.service.WorkflowStepService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Main workflow dialog for 8-step project lifecycle
 * Shown as popup when creating "Sell as New Project"
 */
public class WorkflowDialog extends Stage {

    private final ProjectWorkflowService workflowService;
    private final WorkflowStepService stepService;
    private final SiteSurveyDataRepository siteSurveyRepository;
    private final com.magictech.modules.sales.repository.SizingPricingDataRepository sizingPricingRepository;
    private final Project project;
    private final User currentUser;
    private ProjectWorkflow workflow;

    private VBox mainContainer;
    private HBox progressBar;
    private VBox stepContainer;
    private Button nextButton;
    private Button backButton;
    private Button closeButton;

    private int currentStep = 1;
    private final String[] stepTitles = {
        "Site Survey Check",
        "Selection & Design Check",
        "Bank Guarantee Check",
        "Missing Item Check",
        "Tender Acceptance Check",
        "Project Team Finished Check",
        "After Sales Check",
        "Completion"
    };

    public WorkflowDialog(Project project, User currentUser,
                          ProjectWorkflowService workflowService,
                          WorkflowStepService stepService,
                          SiteSurveyDataRepository siteSurveyRepository,
                          com.magictech.modules.sales.repository.SizingPricingDataRepository sizingPricingRepository) {
        this.project = project;
        this.currentUser = currentUser;
        this.workflowService = workflowService;
        this.stepService = stepService;
        this.siteSurveyRepository = siteSurveyRepository;
        this.sizingPricingRepository = sizingPricingRepository;

        initStyle(StageStyle.UTILITY);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Project Workflow - " + project.getProjectName());

        createWorkflow();
        buildUI();
        loadCurrentStep();
    }

    private void createWorkflow() {
        // Create workflow if doesn't exist
        Optional<ProjectWorkflow> existing = workflowService.getWorkflowByProjectId(project.getId());
        if (existing.isPresent()) {
            workflow = existing.get();
            currentStep = workflow.getCurrentStep();
        } else {
            workflow = workflowService.createWorkflow(project.getId(), currentUser);
            currentStep = 1;
        }
    }

    private void buildUI() {
        mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(20));
        mainContainer.setStyle("-fx-background-color: #f5f5f5;");
        mainContainer.setPrefWidth(800);
        mainContainer.setPrefHeight(600);

        // Header
        Label headerLabel = new Label("Project Workflow");
        headerLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        headerLabel.setTextFill(Color.web("#2c3e50"));

        Label projectLabel = new Label("Project: " + project.getProjectName());
        projectLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        projectLabel.setTextFill(Color.web("#7f8c8d"));

        VBox header = new VBox(5, headerLabel, projectLabel);
        header.setAlignment(Pos.CENTER);

        // Progress bar
        progressBar = createProgressBar();

        // Step container
        stepContainer = new VBox(15);
        stepContainer.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 20;");

        // Buttons
        HBox buttonBox = createButtonBox();

        mainContainer.getChildren().addAll(header, progressBar, stepContainer, buttonBox);

        Scene scene = new Scene(mainContainer);
        setScene(scene);
    }

    private HBox createProgressBar() {
        HBox progressContainer = new HBox(5);
        progressContainer.setAlignment(Pos.CENTER);
        progressContainer.setPadding(new Insets(10));

        for (int i = 1; i <= 8; i++) {
            VBox stepBox = createProgressStep(i);
            progressContainer.getChildren().add(stepBox);

            if (i < 8) {
                // Add connector line
                Region connector = new Region();
                connector.setPrefWidth(30);
                connector.setPrefHeight(2);
                connector.setStyle("-fx-background-color: #bdc3c7;");
                progressContainer.getChildren().add(connector);
            }
        }

        return progressContainer;
    }

    private VBox createProgressStep(int stepNum) {
        VBox stepBox = new VBox(5);
        stepBox.setAlignment(Pos.CENTER);

        // Circle
        StackPane circle = new StackPane();
        circle.setPrefSize(40, 40);
        circle.setStyle(getStepStyle(stepNum));

        Label numLabel = new Label(String.valueOf(stepNum));
        numLabel.setTextFill(Color.WHITE);
        numLabel.setFont(Font.font("System", FontWeight.BOLD, 16));

        circle.getChildren().add(numLabel);

        // Step name
        Label nameLabel = new Label("Step " + stepNum);
        nameLabel.setFont(Font.font("System", FontWeight.NORMAL, 10));
        nameLabel.setTextFill(Color.web("#7f8c8d"));

        stepBox.getChildren().addAll(circle, nameLabel);
        return stepBox;
    }

    private String getStepStyle(int stepNum) {
        Optional<WorkflowStepCompletion> stepOpt = stepService.getStep(workflow.getId(), stepNum);

        if (stepOpt.isPresent() && Boolean.TRUE.equals(stepOpt.get().getCompleted())) {
            return "-fx-background-color: #27ae60; -fx-background-radius: 20;"; // Green - completed
        } else if (stepNum == currentStep) {
            return "-fx-background-color: #3498db; -fx-background-radius: 20;"; // Blue - current
        } else if (stepNum < currentStep) {
            return "-fx-background-color: #f39c12; -fx-background-radius: 20;"; // Orange - in progress
        } else {
            return "-fx-background-color: #bdc3c7; -fx-background-radius: 20;"; // Gray - pending
        }
    }

    private HBox createButtonBox() {
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(10));

        backButton = new Button("← Back");
        backButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        backButton.setOnAction(e -> handleBack());

        nextButton = new Button("Next →");
        nextButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        nextButton.setOnAction(e -> handleNext());

        closeButton = new Button("Close");
        closeButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        closeButton.setOnAction(e -> close());

        buttonBox.getChildren().addAll(backButton, nextButton, closeButton);
        return buttonBox;
    }

    private void loadCurrentStep() {
        stepContainer.getChildren().clear();

        // CRITICAL FIX: Reset Next button handler to default BEFORE loading step content
        // This prevents Steps 1 and 2's custom handlers from persisting into other steps
        nextButton.setOnAction(e -> handleNext());
        nextButton.setDisable(false); // Re-enable by default

        // Step title
        Label stepTitle = new Label("Step " + currentStep + ": " + stepTitles[currentStep - 1]);
        stepTitle.setFont(Font.font("System", FontWeight.BOLD, 20));
        stepTitle.setTextFill(Color.web("#2c3e50"));

        stepContainer.getChildren().add(stepTitle);

        // Load step-specific content
        switch (currentStep) {
            case 1 -> loadStep1_SiteSurvey();
            case 2 -> loadStep2_SelectionDesign();
            case 3 -> loadStep3_BankGuarantee();
            case 4 -> loadStep4_MissingItem();
            case 5 -> loadStep5_TenderAcceptance();
            case 6 -> loadStep6_ProjectFinished();
            case 7 -> loadStep7_AfterSales();
            case 8 -> loadStep8_Completion();
        }

        // Update buttons
        backButton.setDisable(currentStep == 1);

        // Update progress bar
        progressBar.getChildren().clear();
        progressBar = createProgressBar();
        mainContainer.getChildren().set(1, progressBar);
    }

    // STEP 1: Site Survey
    private void loadStep1_SiteSurvey() {
        // CRITICAL FIX: Refresh workflow state from database first
        refreshWorkflow();

        System.out.println("🔍 DEBUG: Loading Step 1, current workflow step: " + workflow.getCurrentStep());

        // Check if site survey already exists (uploaded by Project team or Sales)
        Optional<SiteSurveyData> surveyOpt = siteSurveyRepository.findByWorkflowIdAndActiveTrue(workflow.getId());

        if (surveyOpt.isPresent()) {
            System.out.println("✅ DEBUG: Site survey found - " + surveyOpt.get().getFileName());
            // Site survey completed - show download/view UI
            showSiteSurveyCompleted(surveyOpt.get());
        } else {
            System.out.println("❌ DEBUG: No site survey found");
            // Site survey not uploaded yet - show original request options
            showSiteSurveyRequestOptions();
        }
    }

    private void showSiteSurveyRequestOptions() {
        // ORIGINAL FUNCTIONALITY - PRESERVED
        Label question = new Label("Does Project need site survey?");
        question.setFont(Font.font("System", FontWeight.NORMAL, 16));

        Button yesButton = new Button("Yes - I'll do it myself");
        yesButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        yesButton.setOnAction(e -> handleSiteSurveySales());

        Button yesProjectButton = new Button("Yes - Request from Project Team");
        yesProjectButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        yesProjectButton.setOnAction(e -> handleSiteSurveyProject());

        HBox buttons = new HBox(10, yesButton, yesProjectButton);
        buttons.setAlignment(Pos.CENTER);

        stepContainer.getChildren().addAll(question, buttons);
    }

    private void showSiteSurveyCompleted(SiteSurveyData survey) {
        // NEW FUNCTIONALITY - Show completion status with download options
        VBox completionBox = new VBox(15);
        completionBox.setAlignment(Pos.CENTER_LEFT);
        completionBox.setPadding(new Insets(20));
        completionBox.setStyle("-fx-background-color: #d1fae5; -fx-border-color: #10b981; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label statusLabel = new Label("✅ Site Survey Completed");
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        statusLabel.setTextFill(Color.web("#065f46"));

        Label fileLabel = new Label("📄 File: " + survey.getFileName());
        fileLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));

        String uploaderTeam = "SALES".equals(survey.getSurveyDoneBy()) ? "SALES team" : "PROJECT team";
        Label uploaderLabel = new Label("👤 Uploaded by: " + survey.getSurveyDoneByUser() + " (" + uploaderTeam + ")");
        uploaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));

        Label dateLabel = new Label("📅 Date: " + formatDateTime(survey.getUploadedAt()));
        dateLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));

        Label sizeLabel = new Label("💾 Size: " + formatFileSize(survey.getFileSize()));
        sizeLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));

        // Action buttons
        HBox actionButtons = new HBox(10);
        actionButtons.setAlignment(Pos.CENTER);
        actionButtons.setPadding(new Insets(10, 0, 0, 0));

        Button downloadButton = new Button("📥 Download Excel File");
        downloadButton.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20; -fx-font-weight: bold;");
        downloadButton.setOnAction(e -> handleDownloadSiteSurvey(survey));

        Button viewButton = new Button("👁️ View Survey Data");
        viewButton.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20; -fx-font-weight: bold;");
        viewButton.setOnAction(e -> handleViewSiteSurvey(survey));

        actionButtons.getChildren().addAll(downloadButton, viewButton);

        completionBox.getChildren().addAll(statusLabel, fileLabel, uploaderLabel, dateLabel, sizeLabel, actionButtons);

        // Info message about progression
        Label progressInfo = new Label("✅ Step 1 completed. Click 'Next →' to proceed to Step 2.");
        progressInfo.setFont(Font.font("System", FontWeight.BOLD, 14));
        progressInfo.setTextFill(Color.web("#059669"));
        progressInfo.setPadding(new Insets(15, 0, 0, 0));

        stepContainer.getChildren().addAll(completionBox, progressInfo);

        // CRITICAL FIX: Check step completion status and force enable Next button
        Optional<WorkflowStepCompletion> step1Opt = stepService.getStep(workflow.getId(), 1);
        boolean step1Completed = step1Opt.isPresent() && Boolean.TRUE.equals(step1Opt.get().getCompleted());

        System.out.println("🔍 DEBUG: Step 1 completion status from DB: " + step1Completed);
        System.out.println("🔍 DEBUG: Workflow current step: " + workflow.getCurrentStep());

        // FORCE enable Next button since site survey exists
        nextButton.setDisable(false);

        // OVERRIDE Next button handler to force progression
        // This ensures Next works even if canMoveToNextStep() has issues
        nextButton.setOnAction(e -> {
            System.out.println("🔘 DEBUG: Next button clicked from Step 1 (survey completed)");
            // If workflow already advanced to Step 2+, sync currentStep
            if (workflow.getCurrentStep() >= 2) {
                System.out.println("✅ DEBUG: Workflow already at step " + workflow.getCurrentStep() + ", syncing...");
                currentStep = workflow.getCurrentStep();
                loadCurrentStep();
            } else if (step1Completed) {
                // Step 1 is completed, move to Step 2
                System.out.println("✅ DEBUG: Step 1 completed, moving to Step 2");
                currentStep = 2;
                loadCurrentStep();
            } else {
                // Survey exists but step NOT completed - AUTO-RECOVER
                System.out.println("⚠️ WARNING: Survey file exists but step 1 is NOT completed!");
                System.out.println("   This is a corrupted state from a previous transaction rollback.");
                System.out.println("   Auto-recovering by completing the step...");

                // Auto-complete the step since the file exists and is valid
                try {
                    handleForceCompleteStep1(survey);
                } catch (Exception ex) {
                    // If auto-recovery fails, show manual recovery dialog
                    System.out.println("❌ Auto-recovery failed: " + ex.getMessage());
                    showRecoveryDialog(survey);
                }
            }
        });
    }

    private void handleSiteSurveySales() {
        // Show file chooser for Excel upload
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Upload Site Survey Excel");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls")
        );

        File file = fileChooser.showOpenDialog(this);
        if (file != null) {
            try {
                byte[] fileData = Files.readAllBytes(file.toPath());
                workflowService.processSiteSurveySales(workflow.getId(), fileData,
                    file.getName(), currentUser);

                showSuccess("Site survey uploaded successfully!");
                refreshWorkflow();
                loadCurrentStep();
            } catch (Exception ex) {
                showError("Failed to upload site survey: " + ex.getMessage());
            }
        }
    }

    private void handleSiteSurveyProject() {
        workflowService.requestSiteSurveyFromProject(workflow.getId(), currentUser);
        showInfo("Site survey request sent to Project Team. Waiting for their response...");
    }

    // STEP 2: Selection & Design
    private void loadStep2_SelectionDesign() {
        // CRITICAL FIX: Refresh workflow state from database first
        refreshWorkflow();

        System.out.println("🔍 DEBUG: Loading Step 2, current workflow step: " + workflow.getCurrentStep());

        // Check if sizing/pricing already exists (uploaded by Presales team)
        Optional<com.magictech.modules.sales.entity.SizingPricingData> sizingOpt =
            sizingPricingRepository.findByWorkflowIdAndActiveTrue(workflow.getId());

        if (sizingOpt.isPresent()) {
            System.out.println("✅ DEBUG: Sizing/pricing data found - " + sizingOpt.get().getFileName());
            // Sizing/pricing completed - show download/view UI
            showSizingPricingCompleted(sizingOpt.get());
        } else {
            // Check if request is pending (waiting for Presales)
            Optional<WorkflowStepCompletion> step2Opt = stepService.getStep(workflow.getId(), 2);
            if (step2Opt.isPresent()) {
                WorkflowStepCompletion step2 = step2Opt.get();
                if (Boolean.TRUE.equals(step2.getNeedsExternalAction()) &&
                    Boolean.FALSE.equals(step2.getExternalActionCompleted())) {
                    System.out.println("⏳ DEBUG: Step 2 request pending - waiting for Presales");
                    // Request is pending - show waiting UI
                    showSelectionDesignPendingUI(step2);
                    return;
                }
            }

            System.out.println("❌ DEBUG: No sizing/pricing data found and no pending request");
            // Not uploaded yet and no pending request - show original request options
            showSelectionDesignRequestOptions();
        }
    }

    private void showSelectionDesignPendingUI(WorkflowStepCompletion step) {
        // NEW: Show pending/waiting state (like Step 1 does)
        VBox pendingBox = new VBox(15);
        pendingBox.setAlignment(Pos.CENTER_LEFT);
        pendingBox.setPadding(new Insets(20));
        pendingBox.setStyle("-fx-background-color: #fef3c7; -fx-border-color: #f59e0b; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label statusLabel = new Label("⏳ Waiting for Presales Team");
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        statusLabel.setTextFill(Color.web("#92400e"));

        Label messageLabel = new Label("You requested sizing & pricing from the Presales team.");
        messageLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        messageLabel.setWrapText(true);

        Label infoLabel = new Label("📋 The Presales team has been notified and will upload the sizing/pricing Excel file soon.");
        infoLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        infoLabel.setWrapText(true);
        infoLabel.setTextFill(Color.web("#78350f"));

        Label dateLabel = new Label("📅 Requested: " + formatDateTime(step.getCreatedAt()));
        dateLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        dateLabel.setTextFill(Color.web("#78350f"));

        // Refresh button
        Button refreshButton = new Button("🔄 Check Status");
        refreshButton.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20; -fx-font-weight: bold;");
        refreshButton.setOnAction(e -> {
            refreshWorkflow();
            loadCurrentStep();
        });

        pendingBox.getChildren().addAll(statusLabel, messageLabel, infoLabel, dateLabel, refreshButton);

        Label waitingInfo = new Label("💡 This dialog will automatically show the completed state once Presales uploads the file.");
        waitingInfo.setFont(Font.font("System", FontWeight.NORMAL, 12));
        waitingInfo.setTextFill(Color.web("#6b7280"));
        waitingInfo.setPadding(new Insets(15, 0, 0, 0));
        waitingInfo.setWrapText(true);

        stepContainer.getChildren().addAll(pendingBox, waitingInfo);

        // Disable Next button while waiting
        nextButton.setDisable(true);
    }

    private void showSelectionDesignRequestOptions() {
        Label question = new Label("Does it need a selection and design?");
        question.setFont(Font.font("System", FontWeight.NORMAL, 16));

        Button noButton = new Button("No");
        noButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        noButton.setOnAction(e -> {
            System.out.println("\n🔘 'No' button clicked for Step 2");
            try {
                workflowService.markSelectionDesignNotNeeded(workflow.getId(), currentUser);
                System.out.println("✅ Successfully marked as not needed");
                showSuccess("Marked as not needed");
                refreshWorkflow();
                loadCurrentStep();
            } catch (Exception ex) {
                System.err.println("❌ Error marking as not needed: " + ex.getMessage());
                ex.printStackTrace();
                showError("Error: " + ex.getMessage());
            }
        });

        Button yesButton = new Button("Yes - Request from Presales");
        yesButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        yesButton.setOnAction(e -> {
            System.out.println("\n🔘 'Yes - Request from Presales' button clicked");
            System.out.println("   Workflow ID: " + workflow.getId());
            System.out.println("   Current User: " + currentUser.getUsername());
            System.out.println("   Project ID: " + project.getId());
            System.out.println("   Project Name: " + project.getProjectName());

            try {
                System.out.println("   Calling workflowService.requestSelectionDesignFromPresales()...");
                workflowService.requestSelectionDesignFromPresales(workflow.getId(), currentUser);
                System.out.println("✅ Request sent successfully");

                System.out.println("   Refreshing workflow...");
                refreshWorkflow();

                System.out.println("   Reloading UI...");
                loadCurrentStep();

                System.out.println("✅ UI updated successfully - should now show pending state");
            } catch (Exception ex) {
                System.err.println("❌ ERROR in 'Yes - Request from Presales' button:");
                System.err.println("   Error message: " + ex.getMessage());
                System.err.println("   Error type: " + ex.getClass().getName());
                ex.printStackTrace();
                showError("Failed to send request: " + ex.getMessage());
            }
        });

        HBox buttons = new HBox(10, noButton, yesButton);
        buttons.setAlignment(Pos.CENTER);

        stepContainer.getChildren().addAll(question, buttons);
    }

    private void showSizingPricingCompleted(com.magictech.modules.sales.entity.SizingPricingData sizing) {
        // NEW FUNCTIONALITY - Show completion status with download options
        VBox completionBox = new VBox(15);
        completionBox.setAlignment(Pos.CENTER_LEFT);
        completionBox.setPadding(new Insets(20));
        completionBox.setStyle("-fx-background-color: #dbeafe; -fx-border-color: #3b82f6; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label statusLabel = new Label("✅ Sizing & Pricing Completed");
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        statusLabel.setTextFill(Color.web("#1e40af"));

        Label fileLabel = new Label("📄 File: " + sizing.getFileName());
        fileLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));

        Label uploaderLabel = new Label("👤 Uploaded by: " + sizing.getUploadedBy() + " (PRESALES team)");
        uploaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));

        Label dateLabel = new Label("📅 Date: " + formatDateTime(sizing.getUploadedAt()));
        dateLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));

        Label sizeLabel = new Label("💾 Size: " + formatFileSize(sizing.getFileSize()));
        sizeLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));

        // Action buttons
        HBox actionButtons = new HBox(10);
        actionButtons.setAlignment(Pos.CENTER);
        actionButtons.setPadding(new Insets(10, 0, 0, 0));

        Button downloadButton = new Button("📥 Download Sizing/Pricing File");
        downloadButton.setStyle("-fx-background-color: #06b6d4; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20; -fx-font-weight: bold;");
        downloadButton.setOnAction(e -> handleDownloadSizingPricing(sizing));

        actionButtons.getChildren().add(downloadButton);

        completionBox.getChildren().addAll(statusLabel, fileLabel, uploaderLabel, dateLabel, sizeLabel, actionButtons);

        // Info message about progression
        Label progressInfo = new Label("✅ Step 2 completed. Click 'Next →' to proceed to Step 3.");
        progressInfo.setFont(Font.font("System", FontWeight.BOLD, 14));
        progressInfo.setTextFill(Color.web("#1e40af"));
        progressInfo.setPadding(new Insets(15, 0, 0, 0));

        stepContainer.getChildren().addAll(completionBox, progressInfo);

        // CRITICAL FIX: Check step completion status and force enable Next button
        Optional<WorkflowStepCompletion> step2Opt = stepService.getStep(workflow.getId(), 2);
        boolean step2Completed = step2Opt.isPresent() && Boolean.TRUE.equals(step2Opt.get().getCompleted());

        System.out.println("🔍 DEBUG: Step 2 completion status from DB: " + step2Completed);
        System.out.println("🔍 DEBUG: Workflow current step: " + workflow.getCurrentStep());

        // FORCE enable Next button since sizing/pricing exists
        nextButton.setDisable(false);

        // OVERRIDE Next button handler to force progression
        nextButton.setOnAction(e -> {
            System.out.println("🔘 DEBUG: Next button clicked from Step 2 (sizing completed)");
            // If workflow already advanced to Step 3+, sync currentStep
            if (workflow.getCurrentStep() >= 3) {
                System.out.println("✅ DEBUG: Workflow already at step " + workflow.getCurrentStep() + ", syncing...");
                currentStep = workflow.getCurrentStep();
                loadCurrentStep();
            } else if (step2Completed) {
                // Step 2 is completed, move to Step 3
                System.out.println("✅ DEBUG: Step 2 completed, moving to Step 3");
                currentStep = 3;
                loadCurrentStep();
            } else {
                // Sizing exists but step NOT completed - AUTO-RECOVER
                System.out.println("⚠️ WARNING: Sizing file exists but step 2 is NOT completed!");
                System.out.println("   This is a corrupted state from a previous transaction rollback.");
                System.out.println("   Auto-recovering by completing the step...");

                // Auto-complete the step since the file exists and is valid
                try {
                    handleForceCompleteStep2();
                } catch (Exception ex) {
                    // If auto-recovery fails, show manual recovery dialog
                    System.out.println("❌ Auto-recovery failed: " + ex.getMessage());
                    showRecoveryDialogStep2(sizing);
                }
            }
        });
    }

    private void handleDownloadSizingPricing(com.magictech.modules.sales.entity.SizingPricingData sizing) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Sizing & Pricing Excel");
        fileChooser.setInitialFileName(sizing.getFileName());
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls")
        );

        File file = fileChooser.showSaveDialog(this);
        if (file != null) {
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(sizing.getExcelFile());
                showSuccess("Sizing & Pricing file downloaded successfully!\nSaved to: " + file.getAbsolutePath());
            } catch (Exception ex) {
                showError("Failed to download sizing & pricing: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    // STEP 3: Bank Guarantee
    private void loadStep3_BankGuarantee() {
        Label question = new Label("Does project needs Bank Guarantee?");
        question.setFont(Font.font("System", FontWeight.NORMAL, 16));

        Button noButton = new Button("No");
        noButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        noButton.setOnAction(e -> {
            workflowService.markBankGuaranteeNotNeeded(workflow.getId(), currentUser);
            showSuccess("Marked as not needed");
            refreshWorkflow();
            loadCurrentStep();
        });

        Button yesButton = new Button("Yes - Request from Finance");
        yesButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        yesButton.setOnAction(e -> {
            workflowService.requestBankGuarantee(workflow.getId(), currentUser);
            showInfo("Bank guarantee request sent to Finance Team");
        });

        HBox buttons = new HBox(10, noButton, yesButton);
        buttons.setAlignment(Pos.CENTER);

        stepContainer.getChildren().addAll(question, buttons);
    }

    // STEP 4: Missing Item
    private void loadStep4_MissingItem() {
        Label question = new Label("Is there any missing item?");
        question.setFont(Font.font("System", FontWeight.NORMAL, 16));

        Button noButton = new Button("No");
        noButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        noButton.setOnAction(e -> {
            workflowService.markNoMissingItems(workflow.getId(), currentUser);
            showSuccess("Marked as no missing items");
            refreshWorkflow();
            loadCurrentStep();
        });

        Button yesButton = new Button("Yes - Submit Item Details");
        yesButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        yesButton.setOnAction(e -> showMissingItemDialog());

        HBox buttons = new HBox(10, noButton, yesButton);
        buttons.setAlignment(Pos.CENTER);

        stepContainer.getChildren().addAll(question, buttons);
    }

    private void showMissingItemDialog() {
        Dialog<MissingItemRequest> dialog = new Dialog<>();
        dialog.setTitle("Submit Missing Item Request");
        dialog.setHeaderText("Enter missing item details");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField itemNameField = new TextField();
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPrefRowCount(3);
        TextField quantityField = new TextField();
        TextArea specsArea = new TextArea();
        specsArea.setPrefRowCount(3);
        ComboBox<String> urgencyBox = new ComboBox<>();
        urgencyBox.getItems().addAll("LOW", "MEDIUM", "HIGH", "CRITICAL");
        urgencyBox.setValue("MEDIUM");

        grid.add(new Label("Item Name:*"), 0, 0);
        grid.add(itemNameField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descriptionArea, 1, 1);
        grid.add(new Label("Quantity:"), 0, 2);
        grid.add(quantityField, 1, 2);
        grid.add(new Label("Specifications:"), 0, 3);
        grid.add(specsArea, 1, 3);
        grid.add(new Label("Urgency:"), 0, 4);
        grid.add(urgencyBox, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                MissingItemRequest request = new MissingItemRequest();
                request.setItemName(itemNameField.getText());
                request.setItemDescription(descriptionArea.getText());
                request.setQuantityNeeded(Integer.parseInt(quantityField.getText()));
                request.setItemSpecifications(specsArea.getText());
                request.setUrgencyLevel(urgencyBox.getValue());
                return request;
            }
            return null;
        });

        Optional<MissingItemRequest> result = dialog.showAndWait();
        result.ifPresent(request -> {
            workflowService.submitMissingItemRequest(workflow.getId(), request, currentUser);
            showInfo("Missing item request submitted. Waiting for MASTER/SALES_MANAGER approval...");
        });
    }

    // STEP 5: Tender Acceptance
    private void loadStep5_TenderAcceptance() {
        Label question = new Label("Does the Tender Accepted?");
        question.setFont(Font.font("System", FontWeight.NORMAL, 16));

        Button yesButton = new Button("Yes - Tender Accepted");
        yesButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        yesButton.setOnAction(e -> {
            workflowService.markTenderAccepted(workflow.getId(), currentUser);
            showSuccess("Tender accepted! Project team will be notified to start work.");
        });

        Button noButton = new Button("No - Tender Rejected");
        noButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        noButton.setOnAction(e -> showRejectionDialog());

        HBox buttons = new HBox(10, yesButton, noButton);
        buttons.setAlignment(Pos.CENTER);

        stepContainer.getChildren().addAll(question, buttons);
    }

    private void showRejectionDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Tender Rejection");
        dialog.setHeaderText("Tender Rejected");
        dialog.setContentText("Please explain why the tender was rejected:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(reason -> {
            workflowService.markTenderRejected(workflow.getId(), reason, currentUser);
            showWarning("Tender marked as rejected. Workflow status updated.");
            close();
        });
    }

    // STEP 6: Project Finished
    private void loadStep6_ProjectFinished() {
        Label question = new Label("Does Project team Finished?");
        question.setFont(Font.font("System", FontWeight.NORMAL, 16));

        Button yesButton = new Button("Yes - Upload Project Cost");
        yesButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        yesButton.setOnAction(e -> handleProjectCostUpload());

        Button noButton = new Button("No - Report Delay");
        noButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        noButton.setOnAction(e -> showDelayDialog());

        HBox buttons = new HBox(10, yesButton, noButton);
        buttons.setAlignment(Pos.CENTER);

        stepContainer.getChildren().addAll(question, buttons);
    }

    private void handleProjectCostUpload() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Upload Project Cost Excel");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls")
        );

        File file = fileChooser.showOpenDialog(this);
        if (file != null) {
            try {
                byte[] fileData = Files.readAllBytes(file.toPath());
                workflowService.confirmProjectFinished(workflow.getId(), fileData,
                    file.getName(), currentUser);

                showSuccess("Project cost uploaded successfully!");
                refreshWorkflow();
                loadCurrentStep();
            } catch (Exception ex) {
                showError("Failed to upload project cost: " + ex.getMessage());
            }
        }
    }

    private void showDelayDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Report Project Delay");
        dialog.setHeaderText("Project Delay - DANGER Alert");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        DatePicker expectedDatePicker = new DatePicker();
        TextArea delayDetailsArea = new TextArea();
        delayDetailsArea.setPrefRowCount(5);

        grid.add(new Label("Expected Completion Date:"), 0, 0);
        grid.add(expectedDatePicker, 1, 0);
        grid.add(new Label("Delay Details:"), 0, 1);
        grid.add(delayDetailsArea, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            LocalDateTime expectedDate = expectedDatePicker.getValue().atStartOfDay();
            workflowService.reportProjectDelay(workflow.getId(), expectedDate,
                delayDetailsArea.getText(), currentUser);
            showWarning("DANGER alert sent to MASTER about project delay");
        });
    }

    // STEP 7: After Sales
    private void loadStep7_AfterSales() {
        Label question = new Label("After Sales check?");
        question.setFont(Font.font("System", FontWeight.NORMAL, 16));

        Button noButton = new Button("No");
        noButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        noButton.setOnAction(e -> {
            workflowService.markAfterSalesNotNeeded(workflow.getId(), currentUser);
            showSuccess("Marked as not needed");
            refreshWorkflow();
            loadCurrentStep();
        });

        Button yesButton = new Button("Yes - Request QA Check");
        yesButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        yesButton.setOnAction(e -> {
            workflowService.requestAfterSalesCheck(workflow.getId(), currentUser);
            showInfo("After-sales check request sent to Quality Assurance Team");
        });

        HBox buttons = new HBox(10, noButton, yesButton);
        buttons.setAlignment(Pos.CENTER);

        stepContainer.getChildren().addAll(question, buttons);
    }

    // STEP 8: Completion
    private void loadStep8_Completion() {
        Label question = new Label("Complete the workflow?");
        question.setFont(Font.font("System", FontWeight.NORMAL, 16));

        Label info = new Label("All project data will be pushed to storage analysis.");
        info.setFont(Font.font("System", FontWeight.NORMAL, 14));
        info.setTextFill(Color.web("#7f8c8d"));

        Button completeButton = new Button("✓ Complete Workflow");
        completeButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 15 30;");
        completeButton.setOnAction(e -> {
            workflowService.completeWorkflow(workflow.getId(), currentUser);
            showSuccess("Workflow completed! All data pushed to storage analysis.");
            close();
        });

        VBox content = new VBox(15, question, info, completeButton);
        content.setAlignment(Pos.CENTER);

        stepContainer.getChildren().add(content);
    }

    private void handleNext() {
        System.out.println("🔘 DEBUG: handleNext() called from step " + currentStep);

        if (canMoveToNextStep()) {
            System.out.println("✅ DEBUG: Can move to next step, incrementing from " + currentStep);
            currentStep++;
            loadCurrentStep();
        } else {
            System.out.println("❌ DEBUG: Cannot move to next step - step " + currentStep + " not completed");
            showWarning("Please complete the current step before proceeding to the next step.");
        }
    }

    private void handleBack() {
        if (currentStep > 1) {
            currentStep--;
            loadCurrentStep();
        }
    }

    private boolean canMoveToNextStep() {
        // CRITICAL FIX: Refresh workflow state from database before checking
        refreshWorkflow();

        System.out.println("🔍 DEBUG: canMoveToNextStep() checking step " + currentStep);

        Optional<WorkflowStepCompletion> stepOpt = stepService.getStep(workflow.getId(), currentStep);
        if (stepOpt.isPresent()) {
            boolean completed = Boolean.TRUE.equals(stepOpt.get().getCompleted());
            System.out.println("🔍 DEBUG: Step " + currentStep + " completion status: " + completed);
            return completed;
        }

        System.out.println("❌ DEBUG: Step " + currentStep + " not found in database");
        return false;
    }

    private void refreshWorkflow() {
        System.out.println("🔄 DEBUG: Refreshing workflow from database (ID: " + workflow.getId() + ")");
        int oldStep = workflow.getCurrentStep();
        workflow = workflowService.getWorkflowById(workflow.getId()).orElse(workflow);
        currentStep = workflow.getCurrentStep();
        System.out.println("🔄 DEBUG: Workflow refreshed - Step changed from " + oldStep + " to " + currentStep);
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

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // NEW METHODS FOR SITE SURVEY DOWNLOAD AND VIEW

    private void handleDownloadSiteSurvey(SiteSurveyData survey) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Site Survey Excel");
        fileChooser.setInitialFileName(survey.getFileName());
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls")
        );

        File file = fileChooser.showSaveDialog(this);
        if (file != null) {
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(survey.getExcelFile());
                showSuccess("Site survey downloaded successfully!\nSaved to: " + file.getAbsolutePath());
            } catch (Exception ex) {
                showError("Failed to download site survey: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    private void handleViewSiteSurvey(SiteSurveyData survey) {
        // Create a dialog to show survey data
        Dialog<Void> viewDialog = new Dialog<>();
        viewDialog.setTitle("Site Survey Data - " + survey.getFileName());
        viewDialog.setHeaderText("📊 Site Survey Information");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(600);

        // Metadata section
        Label metadataTitle = new Label("ℹ️ Survey Metadata");
        metadataTitle.setFont(Font.font("System", FontWeight.BOLD, 16));

        GridPane metadataGrid = new GridPane();
        metadataGrid.setHgap(15);
        metadataGrid.setVgap(10);
        metadataGrid.setPadding(new Insets(10, 0, 0, 0));

        addGridRow(metadataGrid, 0, "File Name:", survey.getFileName());
        addGridRow(metadataGrid, 1, "File Size:", formatFileSize(survey.getFileSize()));
        addGridRow(metadataGrid, 2, "Survey Done By:", survey.getSurveyDoneByUser() + " (" + survey.getSurveyDoneBy() + ")");
        addGridRow(metadataGrid, 3, "Uploaded By:", survey.getUploadedBy());
        addGridRow(metadataGrid, 4, "Upload Date:", formatDateTime(survey.getUploadedAt()));

        Separator separator = new Separator();

        // Parsed data section
        Label dataTitle = new Label("📄 Parsed Excel Data");
        dataTitle.setFont(Font.font("System", FontWeight.BOLD, 16));

        TextArea dataTextArea = new TextArea();
        dataTextArea.setWrapText(true);
        dataTextArea.setEditable(false);
        dataTextArea.setPrefRowCount(15);
        dataTextArea.setStyle("-fx-font-family: 'Monospaced'; -fx-font-size: 11px;");

        if (survey.getParsedData() != null && !survey.getParsedData().isEmpty()) {
            dataTextArea.setText(survey.getParsedData());
        } else {
            dataTextArea.setText("No parsed data available");
        }

        content.getChildren().addAll(metadataTitle, metadataGrid, separator, dataTitle, dataTextArea);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(500);

        viewDialog.getDialogPane().setContent(scrollPane);
        viewDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        viewDialog.showAndWait();
    }

    private void addGridRow(GridPane grid, int row, String label, String value) {
        Label labelNode = new Label(label);
        labelNode.setFont(Font.font("System", FontWeight.BOLD, 12));
        labelNode.setStyle("-fx-text-fill: #6b7280;");

        Label valueNode = new Label(value != null ? value : "-");
        valueNode.setFont(Font.font("System", 12));
        valueNode.setStyle("-fx-text-fill: #1f2937;");

        grid.add(labelNode, 0, row);
        grid.add(valueNode, 1, row);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "-";
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private String formatFileSize(Long bytes) {
        if (bytes == null) return "0 KB";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }

    /**
     * NEW: Recovery dialog for inconsistent workflow state
     * Shown when site survey exists but step is not completed
     */
    private void showRecoveryDialog(SiteSurveyData survey) {
        Alert recoveryAlert = new Alert(Alert.AlertType.WARNING);
        recoveryAlert.setTitle("Workflow State Recovery");
        recoveryAlert.setHeaderText("⚠️ Inconsistent Workflow State Detected");

        String message = "The site survey file exists in the database, but Step 1 is not marked as completed.\n\n" +
                        "This happened because a previous upload transaction was rolled back.\n\n" +
                        "File: " + survey.getFileName() + "\n" +
                        "Uploaded by: " + survey.getUploadedBy() + "\n\n" +
                        "RECOVERY OPTIONS:\n" +
                        "1. Force Complete Step - Mark Step 1 as completed and advance to Step 2\n" +
                        "2. Delete & Re-upload - Delete the existing file and upload again\n\n" +
                        "What would you like to do?";

        recoveryAlert.setContentText(message);

        ButtonType forceCompleteButton = new ButtonType("Force Complete Step", ButtonBar.ButtonData.OK_DONE);
        ButtonType deleteReuploadButton = new ButtonType("Delete & Re-upload", ButtonBar.ButtonData.NO);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        recoveryAlert.getButtonTypes().setAll(forceCompleteButton, deleteReuploadButton, cancelButton);

        recoveryAlert.showAndWait().ifPresent(response -> {
            if (response == forceCompleteButton) {
                handleForceCompleteStep1(survey);
            } else if (response == deleteReuploadButton) {
                handleDeleteAndReupload(survey);
            }
        });
    }

    /**
     * Force complete Step 1 by manually marking it as completed
     */
    private void handleForceCompleteStep1(SiteSurveyData survey) {
        try {
            System.out.println("🔧 RECOVERY: Force completing Step 1 for workflow " + workflow.getId());

            // Get step 1
            Optional<WorkflowStepCompletion> step1Opt = stepService.getStep(workflow.getId(), 1);
            if (step1Opt.isEmpty()) {
                showError("Cannot find Step 1 in database!");
                return;
            }

            WorkflowStepCompletion step1 = step1Opt.get();

            // Manually complete the step
            stepService.completeStep(step1, currentUser);

            // Manually advance workflow to step 2
            if (workflow.getCurrentStep() == 1) {
                workflow.setCurrentStep(2);
                workflow.markStepCompleted(1);
                workflowService.getWorkflowById(workflow.getId()).ifPresent(w -> {
                    w.setCurrentStep(2);
                    w.markStepCompleted(1);
                });
            }

            showSuccess("✅ Step 1 manually completed!\n\nWorkflow advanced to Step 2.");

            // Refresh and reload
            refreshWorkflow();
            currentStep = 2;
            loadCurrentStep();

        } catch (Exception ex) {
            showError("Failed to force complete step: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Delete existing survey and allow re-upload
     */
    private void handleDeleteAndReupload(SiteSurveyData survey) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText("Delete Site Survey File?");
        confirmAlert.setContentText("Are you sure you want to delete the existing site survey file?\n\n" +
                                   "File: " + survey.getFileName() + "\n\n" +
                                   "You will need to upload it again.");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    System.out.println("🗑️ RECOVERY: Deleting site survey for workflow " + workflow.getId());

                    // Soft delete the survey
                    survey.setActive(false);
                    siteSurveyRepository.save(survey);

                    showSuccess("Site survey deleted successfully.\n\nPlease upload a new file.");

                    // Reload step to show upload options again
                    refreshWorkflow();
                    loadCurrentStep();

                } catch (Exception ex) {
                    showError("Failed to delete survey: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        });
    }

    /**
     * Recovery dialog for Step 2 inconsistent state
     */
    private void showRecoveryDialogStep2(com.magictech.modules.sales.entity.SizingPricingData sizing) {
        Alert recoveryAlert = new Alert(Alert.AlertType.WARNING);
        recoveryAlert.setTitle("Workflow State Recovery - Step 2");
        recoveryAlert.setHeaderText("⚠️ Inconsistent Workflow State Detected");

        String message = "The sizing/pricing file exists in the database, but Step 2 is not marked as completed.\n\n" +
                        "This happened because a previous upload transaction was rolled back.\n\n" +
                        "File: " + sizing.getFileName() + "\n" +
                        "Uploaded by: " + sizing.getUploadedBy() + "\n\n" +
                        "RECOVERY OPTIONS:\n" +
                        "1. Force Complete Step - Mark Step 2 as completed and advance to Step 3\n" +
                        "2. Delete & Re-upload - Delete the existing file and re-upload\n\n" +
                        "What would you like to do?";

        recoveryAlert.setContentText(message);

        ButtonType forceCompleteButton = new ButtonType("Force Complete Step", ButtonBar.ButtonData.OK_DONE);
        ButtonType deleteReuploadButton = new ButtonType("Delete & Re-upload", ButtonBar.ButtonData.NO);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        recoveryAlert.getButtonTypes().setAll(forceCompleteButton, deleteReuploadButton, cancelButton);

        recoveryAlert.showAndWait().ifPresent(response -> {
            if (response == forceCompleteButton) {
                handleForceCompleteStep2();
            } else if (response == deleteReuploadButton) {
                handleDeleteAndReuploadStep2(sizing);
            }
        });
    }

    /**
     * Force complete Step 2
     */
    private void handleForceCompleteStep2() {
        try {
            System.out.println("🔧 RECOVERY: Force completing Step 2 for workflow " + workflow.getId());

            Optional<WorkflowStepCompletion> step2Opt = stepService.getStep(workflow.getId(), 2);
            if (step2Opt.isEmpty()) {
                showError("Cannot find Step 2 in database!");
                return;
            }

            WorkflowStepCompletion step2 = step2Opt.get();
            stepService.completeStep(step2, currentUser);

            if (workflow.getCurrentStep() == 2) {
                workflow.setCurrentStep(3);
                workflow.markStepCompleted(2);
                workflowService.getWorkflowById(workflow.getId()).ifPresent(w -> {
                    w.setCurrentStep(3);
                    w.markStepCompleted(2);
                });
            }

            showSuccess("✅ Step 2 manually completed!\n\nWorkflow advanced to Step 3.");
            refreshWorkflow();
            currentStep = 3;
            loadCurrentStep();

        } catch (Exception ex) {
            showError("Failed to force complete step: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Delete Step 2 sizing/pricing file
     */
    private void handleDeleteAndReuploadStep2(com.magictech.modules.sales.entity.SizingPricingData sizing) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText("Delete Sizing/Pricing File?");
        confirmAlert.setContentText("Are you sure you want to delete the existing sizing/pricing file?\n\n" +
                                   "File: " + sizing.getFileName() + "\n\n" +
                                   "Presales will need to upload it again.");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    System.out.println("🗑️ RECOVERY: Deleting sizing/pricing for workflow " + workflow.getId());

                    sizing.setActive(false);
                    sizingPricingRepository.save(sizing);

                    showSuccess("Sizing/pricing file deleted successfully.\n\nPlease request from Presales again.");
                    refreshWorkflow();
                    loadCurrentStep();

                } catch (Exception ex) {
                    showError("Failed to delete sizing/pricing: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        });
    }
}
