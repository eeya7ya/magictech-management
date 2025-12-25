package com.magictech.modules.sales.ui;

import com.magictech.core.auth.User;
import com.magictech.core.auth.UserRepository;
import com.magictech.core.auth.UserRole;
import com.magictech.core.ui.components.RoadmapProgressBar;
import com.magictech.modules.projects.entity.Project;
import com.magictech.modules.sales.entity.MissingItemRequest;
import com.magictech.modules.sales.entity.ProjectWorkflow;
import com.magictech.modules.sales.entity.SiteSurveyData;
import com.magictech.modules.sales.entity.WorkflowStepCompletion;
import com.magictech.modules.sales.repository.SiteSurveyDataRepository;
import com.magictech.modules.sales.service.ProjectWorkflowService;
import com.magictech.modules.sales.service.WorkflowStepService;
import com.magictech.modules.sales.service.WorkflowEmailService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Main workflow dialog for 8-step project lifecycle
 * Shown as popup when creating "Sell as New Project"
 *
 * Features:
 * - Minimize/Maximize/Close window controls
 * - Auto-advance after step completion
 * - Step 4: Navigate to project elements tab for adding items from storage
 */
public class WorkflowDialog extends Stage {

    /**
     * Callback interface for workflow dialog events
     */
    public interface WorkflowDialogCallback {
        /**
         * Called when user clicks "Add Elements" in Step 4
         * @param project The project to add elements to
         * @param workflowId The workflow ID for tracking
         */
        void onNavigateToProjectElements(Project project, Long workflowId);

        /**
         * Called when workflow dialog is restored from minimized state
         */
        default void onWorkflowRestored() {}
    }

    private final ProjectWorkflowService workflowService;
    private final WorkflowStepService stepService;
    private final SiteSurveyDataRepository siteSurveyRepository;
    private final com.magictech.modules.sales.repository.SizingPricingDataRepository sizingPricingRepository;
    private final com.magictech.modules.sales.repository.BankGuaranteeDataRepository bankGuaranteeRepository;
    private final Project project;
    private final User currentUser;
    private ProjectWorkflow workflow;

    // New dependencies for role-based user assignment
    private final UserRepository userRepository;
    private final WorkflowEmailService workflowEmailService;

    // Callback for communicating with parent controller
    private WorkflowDialogCallback callback;

    private VBox mainContainer;
    private RoadmapProgressBar progressBar;
    private VBox stepContainer;
    private Button nextButton;
    private Button backButton;
    private Button closeButton;

    // Minimized state tracking
    private boolean isMinimized = false;
    private VBox minimizedBar;
    private Scene fullScene;
    private Scene minimizedScene;
    private double savedX, savedY, savedWidth, savedHeight;

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
                          com.magictech.modules.sales.repository.SizingPricingDataRepository sizingPricingRepository,
                          com.magictech.modules.sales.repository.BankGuaranteeDataRepository bankGuaranteeRepository,
                          UserRepository userRepository,
                          WorkflowEmailService workflowEmailService) {
        this.project = project;
        this.currentUser = currentUser;
        this.workflowService = workflowService;
        this.stepService = stepService;
        this.siteSurveyRepository = siteSurveyRepository;
        this.sizingPricingRepository = sizingPricingRepository;
        this.bankGuaranteeRepository = bankGuaranteeRepository;
        this.userRepository = userRepository;
        this.workflowEmailService = workflowEmailService;

        // Use DECORATED style for window controls (minimize, maximize, close)
        initStyle(StageStyle.DECORATED);
        // Non-modal so user can interact with main window when minimized
        initModality(Modality.NONE);
        setTitle("Project Workflow - " + project.getProjectName());
        setResizable(true);

        createWorkflow();
        buildUI();
        buildMinimizedBar();
        loadCurrentStep();
    }

    /**
     * Set the callback for workflow dialog events
     * @param callback The callback implementation
     */
    public void setCallback(WorkflowDialogCallback callback) {
        this.callback = callback;
    }

    /**
     * Get the project associated with this workflow dialog
     */
    public Project getProject() {
        return project;
    }

    /**
     * Get the workflow ID
     */
    public Long getWorkflowId() {
        return workflow != null ? workflow.getId() : null;
    }

    /**
     * Get the current step number
     */
    public int getCurrentStepNumber() {
        return currentStep;
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
        mainContainer.setPadding(new Insets(25));
        mainContainer.setStyle("-fx-background-color: linear-gradient(to bottom, #0f172a, #1e293b);");
        mainContainer.setPrefWidth(900);
        mainContainer.setPrefHeight(700);

        // Header with modern styling
        VBox header = new VBox(8);
        header.setAlignment(Pos.CENTER);

        Label iconLabel = new Label("📋");
        iconLabel.setFont(Font.font(40));

        Label headerLabel = new Label("Project Workflow");
        headerLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
        headerLabel.setTextFill(Color.WHITE);

        Label projectLabel = new Label("Project: " + project.getProjectName());
        projectLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        projectLabel.setTextFill(Color.web("#94a3b8"));

        header.getChildren().addAll(iconLabel, headerLabel, projectLabel);

        // Modern roadmap-style progress bar
        progressBar = new RoadmapProgressBar(java.util.Arrays.asList(stepTitles));
        progressBar.setCurrentStep(currentStep);

        // Step container with modern styling
        stepContainer = new VBox(15);
        stepContainer.setStyle(
            "-fx-background-color: rgba(30, 41, 59, 0.8);" +
            "-fx-background-radius: 12;" +
            "-fx-border-radius: 12;" +
            "-fx-border-color: rgba(100, 116, 139, 0.3);" +
            "-fx-border-width: 1;" +
            "-fx-padding: 25;"
        );
        VBox.setVgrow(stepContainer, Priority.ALWAYS);

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
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(15, 0, 0, 0));

        backButton = new Button("← Back");
        backButton.setStyle(
            "-fx-background-color: #475569;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 12 25;" +
            "-fx-background-radius: 8;" +
            "-fx-cursor: hand;"
        );
        backButton.setOnAction(e -> handleBack());

        nextButton = new Button("Next →");
        nextButton.setStyle(
            "-fx-background-color: linear-gradient(to right, #3b82f6, #2563eb);" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 12 25;" +
            "-fx-background-radius: 8;" +
            "-fx-cursor: hand;"
        );
        nextButton.setOnAction(e -> handleNext());

        // Minimize button - allows user to minimize dialog and work on main screen
        Button minimizeButton = new Button("▬ Minimize");
        minimizeButton.setStyle(
            "-fx-background-color: #f59e0b;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 12 25;" +
            "-fx-background-radius: 8;" +
            "-fx-cursor: hand;"
        );
        minimizeButton.setOnAction(e -> minimizeToBar());

        closeButton = new Button("Close");
        closeButton.setStyle(
            "-fx-background-color: #ef4444;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 12 25;" +
            "-fx-background-radius: 8;" +
            "-fx-cursor: hand;"
        );
        closeButton.setOnAction(e -> {
            if (progressBar != null) {
                progressBar.cleanup();
            }
            close();
        });

        buttonBox.getChildren().addAll(backButton, nextButton, minimizeButton, closeButton);
        return buttonBox;
    }

    /**
     * Build the minimized bar that appears at bottom of screen
     */
    private void buildMinimizedBar() {
        minimizedBar = new VBox();
        minimizedBar.setStyle(
            "-fx-background-color: linear-gradient(to right, #2c3e50, #3498db);" +
            "-fx-padding: 10 20;" +
            "-fx-background-radius: 8 8 0 0;"
        );
        minimizedBar.setAlignment(Pos.CENTER);

        HBox content = new HBox(15);
        content.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label("📋");
        iconLabel.setFont(Font.font(18));

        Label titleLabel = new Label("Workflow: " + project.getProjectName() + " (Step " + currentStep + ")");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        titleLabel.setId("minimizedTitle");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button restoreButton = new Button("▢ Restore");
        restoreButton.setStyle(
            "-fx-background-color: #27ae60;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 12px;" +
            "-fx-padding: 5 15;" +
            "-fx-background-radius: 4;"
        );
        restoreButton.setOnAction(e -> restoreFromBar());

        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
            "-fx-background-color: #e74c3c;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 12px;" +
            "-fx-padding: 5 10;" +
            "-fx-background-radius: 4;"
        );
        closeBtn.setOnAction(e -> close());

        content.getChildren().addAll(iconLabel, titleLabel, spacer, restoreButton, closeBtn);
        minimizedBar.getChildren().add(content);

        minimizedScene = new Scene(minimizedBar, 500, 50);
    }

    /**
     * Minimize the dialog to a bar at the bottom of the screen
     */
    public void minimizeToBar() {
        if (isMinimized) return;

        System.out.println("📉 Minimizing workflow dialog to bar");

        // Save current position and size
        savedX = getX();
        savedY = getY();
        savedWidth = getWidth();
        savedHeight = getHeight();

        // Update minimized bar title with current step
        Label titleLabel = (Label) minimizedBar.lookup("#minimizedTitle");
        if (titleLabel != null) {
            titleLabel.setText("Workflow: " + project.getProjectName() + " (Step " + currentStep + ")");
        }

        // Get screen bounds
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

        // Switch to minimized scene
        fullScene = getScene();
        setScene(minimizedScene);

        // Position at bottom center of screen
        setWidth(500);
        setHeight(50);
        setX((screenBounds.getWidth() - 500) / 2);
        setY(screenBounds.getHeight() - 60);

        isMinimized = true;
        setAlwaysOnTop(true);
    }

    /**
     * Restore the dialog from minimized state
     */
    public void restoreFromBar() {
        if (!isMinimized) return;

        System.out.println("📈 Restoring workflow dialog from bar");

        // Switch back to full scene
        setScene(fullScene);

        // Restore position and size
        setWidth(savedWidth > 0 ? savedWidth : 800);
        setHeight(savedHeight > 0 ? savedHeight : 600);
        setX(savedX > 0 ? savedX : 100);
        setY(savedY > 0 ? savedY : 100);

        isMinimized = false;
        setAlwaysOnTop(false);

        // Refresh workflow state
        refreshWorkflow();
        loadCurrentStep();

        // Notify callback
        if (callback != null) {
            callback.onWorkflowRestored();
        }
    }

    /**
     * Check if the dialog is currently minimized
     */
    public boolean isDialogMinimized() {
        return isMinimized;
    }

    /**
     * Advance to next step and update UI - called after step completion
     */
    private void advanceToNextStepAndUpdateUI() {
        System.out.println("🚀 AUTO-ADVANCE: Syncing UI with database state");
        refreshWorkflow();
        currentStep = workflow.getCurrentStep();
        System.out.println("   Current step after refresh: " + currentStep);
        loadCurrentStep();
    }

    private void loadCurrentStep() {
        stepContainer.getChildren().clear();

        // CRITICAL FIX: Reset Next button handler to default BEFORE loading step content
        // This prevents Steps 1 and 2's custom handlers from persisting into other steps
        nextButton.setOnAction(e -> handleNext());
        nextButton.setDisable(false); // Re-enable by default

        // CRITICAL FIX: Save the current step BEFORE loading step content
        // Load step methods may call refreshWorkflow() which could change currentStep
        final int stepToLoad = currentStep;

        // Step title with modern styling
        Label stepTitle = new Label("Step " + stepToLoad + ": " + stepTitles[stepToLoad - 1]);
        stepTitle.setFont(Font.font("System", FontWeight.BOLD, 22));
        stepTitle.setTextFill(Color.WHITE);

        stepContainer.getChildren().add(stepTitle);

        // Load step-specific content
        switch (stepToLoad) {
            case 1 -> loadStep1_SiteSurvey();
            case 2 -> loadStep2_SelectionDesign();
            case 3 -> loadStep3_BankGuarantee();
            case 4 -> loadStep4_MissingItem();
            case 5 -> loadStep5_TenderAcceptance();
            case 6 -> loadStep6_ProjectFinished();
            case 7 -> loadStep7_AfterSales();
            case 8 -> loadStep8_Completion();
        }

        // CRITICAL FIX: Restore currentStep to the step we're displaying
        // This ensures the progress bar shows the correct current step
        currentStep = stepToLoad;

        // Update buttons
        backButton.setDisable(currentStep == 1);

        // Update roadmap progress bar - set current step and completion states
        progressBar.setCurrentStep(currentStep);
        updateProgressBarCompletionStates();
    }

    /**
     * Update the roadmap progress bar with completion states from database
     */
    private void updateProgressBarCompletionStates() {
        for (int i = 1; i <= 8; i++) {
            Optional<WorkflowStepCompletion> stepOpt = stepService.getStep(workflow.getId(), i);
            boolean completed = stepOpt.isPresent() && Boolean.TRUE.equals(stepOpt.get().getCompleted());
            progressBar.setStepCompleted(i, completed);
        }
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
            // Check if request is pending (waiting for Project team) - SAME PATTERN AS STEP 2
            Optional<WorkflowStepCompletion> step1Opt = stepService.getStep(workflow.getId(), 1);
            if (step1Opt.isPresent()) {
                WorkflowStepCompletion step1 = step1Opt.get();
                if (Boolean.TRUE.equals(step1.getNeedsExternalAction()) &&
                    "PROJECT".equals(step1.getExternalModule()) &&
                    Boolean.FALSE.equals(step1.getExternalActionCompleted())) {
                    System.out.println("⏳ DEBUG: Step 1 request pending - waiting for Project team");
                    // Request is pending - show waiting UI
                    showSiteSurveyPendingUI(step1);
                    return;
                }
            }

            System.out.println("❌ DEBUG: No site survey found and no pending request");
            // Site survey not uploaded yet and no pending request - show original request options
            showSiteSurveyRequestOptions();
        }
    }

    private void showSiteSurveyRequestOptions() {
        Label question = new Label("Does Project need site survey?");
        question.setFont(Font.font("System", FontWeight.NORMAL, 16));
        question.setTextFill(Color.WHITE);

        // Option 1: Do it yourself
        Button yesButton = new Button("Yes - I'll do it myself");
        yesButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        yesButton.setOnAction(e -> handleSiteSurveySales());

        // Option 2: Request from a specific Project Team member
        VBox projectRequestBox = new VBox(10);
        projectRequestBox.setAlignment(Pos.CENTER_LEFT);
        projectRequestBox.setPadding(new Insets(15));
        projectRequestBox.setStyle("-fx-background-color: rgba(52, 152, 219, 0.1); -fx-border-color: #3498db; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label selectLabel = new Label("Or assign to a specific Project Manager:");
        selectLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        selectLabel.setTextFill(Color.WHITE);

        // Dropdown for Project team users
        ComboBox<User> projectUserCombo = new ComboBox<>();
        projectUserCombo.setPromptText("Select Project Manager...");
        projectUserCombo.setPrefWidth(300);

        // Load Project team users
        List<User> projectUsers = userRepository.findByRoleAndActiveTrue(UserRole.PROJECTS);
        // Also include PROJECT_SUPPLIER role
        projectUsers.addAll(userRepository.findByRoleAndActiveTrue(UserRole.PROJECT_SUPPLIER));
        projectUserCombo.getItems().addAll(projectUsers);

        // Custom cell factory to show username
        projectUserCombo.setCellFactory(lv -> new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                setText(empty || user == null ? null : user.getUsername() +
                    (user.getEmail() != null ? " (" + user.getEmail() + ")" : ""));
            }
        });
        projectUserCombo.setButtonCell(new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                setText(empty || user == null ? null : user.getUsername());
            }
        });

        Button sendRequestButton = new Button("📧 Send Request to Selected User");
        sendRequestButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        sendRequestButton.setDisable(true);

        // Enable button only when a user is selected
        projectUserCombo.setOnAction(e -> sendRequestButton.setDisable(projectUserCombo.getValue() == null));

        sendRequestButton.setOnAction(e -> {
            User selectedUser = projectUserCombo.getValue();
            if (selectedUser != null) {
                handleSiteSurveyProjectWithUser(selectedUser);
            }
        });

        // Email notification info
        Label emailInfo = new Label("An email notification will be sent to the selected user.");
        emailInfo.setFont(Font.font("System", FontWeight.NORMAL, 12));
        emailInfo.setTextFill(Color.web("#94a3b8"));

        projectRequestBox.getChildren().addAll(selectLabel, projectUserCombo, sendRequestButton, emailInfo);

        VBox optionsBox = new VBox(20);
        optionsBox.setAlignment(Pos.CENTER);
        optionsBox.getChildren().addAll(question, yesButton, projectRequestBox);

        stepContainer.getChildren().add(optionsBox);
    }

    /**
     * NEW: Show pending/waiting state for Step 1 when waiting for Project team
     * Same pattern as Step 2's showSelectionDesignPendingUI
     */
    private void showSiteSurveyPendingUI(WorkflowStepCompletion step) {
        VBox pendingBox = new VBox(15);
        pendingBox.setAlignment(Pos.CENTER_LEFT);
        pendingBox.setPadding(new Insets(20));
        pendingBox.setStyle("-fx-background-color: #fef3c7; -fx-border-color: #f59e0b; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label statusLabel = new Label("⏳ Waiting for Project Team");
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        statusLabel.setTextFill(Color.web("#92400e"));

        Label messageLabel = new Label("You requested a site survey from the Project team.");
        messageLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        messageLabel.setWrapText(true);

        Label infoLabel = new Label("📋 The Project team has been notified and will upload the site survey Excel file soon.");
        infoLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        infoLabel.setWrapText(true);
        infoLabel.setTextFill(Color.web("#78350f"));

        Label dateLabel = new Label("📅 Requested: " + formatDateTime(step.getCreatedAt()));
        dateLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        dateLabel.setTextFill(Color.web("#78350f"));

        // Refresh button to check if Project team has uploaded
        Button refreshButton = new Button("🔄 Check Status");
        refreshButton.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20; -fx-font-weight: bold;");
        refreshButton.setOnAction(e -> {
            refreshWorkflow();
            loadCurrentStep();
        });

        pendingBox.getChildren().addAll(statusLabel, messageLabel, infoLabel, dateLabel, refreshButton);

        Label waitingInfo = new Label("💡 Click 'Check Status' to see if the Project team has uploaded the survey. This will automatically show the completed state once uploaded.");
        waitingInfo.setFont(Font.font("System", FontWeight.NORMAL, 12));
        waitingInfo.setTextFill(Color.web("#6b7280"));
        waitingInfo.setPadding(new Insets(15, 0, 0, 0));
        waitingInfo.setWrapText(true);

        stepContainer.getChildren().addAll(pendingBox, waitingInfo);

        // Disable Next button while waiting for Project team
        nextButton.setDisable(true);
    }

    private void showSiteSurveyCompleted(SiteSurveyData survey) {
        // NEW FUNCTIONALITY - Show completion status with download options for Excel OR ZIP
        VBox completionBox = new VBox(15);
        completionBox.setAlignment(Pos.CENTER_LEFT);
        completionBox.setPadding(new Insets(20));
        completionBox.setStyle("-fx-background-color: #d1fae5; -fx-border-color: #10b981; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label statusLabel = new Label("✅ Site Survey Completed");
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        statusLabel.setTextFill(Color.web("#065f46"));

        // Determine file type and show appropriate info
        boolean hasExcel = survey.hasExcelFile();
        boolean hasZip = survey.hasZipFile();
        String fileType = hasZip ? (hasExcel ? "Excel + ZIP" : "ZIP") : "Excel";

        Label fileTypeLabel = new Label("📁 File Type: " + fileType);
        fileTypeLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        fileTypeLabel.setTextFill(Color.web("#065f46"));

        // Show Excel file info if present
        VBox fileInfoBox = new VBox(5);
        if (hasExcel) {
            Label excelLabel = new Label("📊 Excel: " + survey.getFileName() + " (" + formatFileSize(survey.getFileSize()) + ")");
            excelLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
            fileInfoBox.getChildren().add(excelLabel);
        }

        // Show ZIP file info if present
        if (hasZip) {
            Label zipLabel = new Label("📦 ZIP: " + survey.getZipFileName() + " (" + formatFileSize(survey.getZipFileSize()) + ")");
            zipLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
            fileInfoBox.getChildren().add(zipLabel);
        }

        String uploaderTeam = "SALES".equals(survey.getSurveyDoneBy()) ? "SALES team" : "PROJECT team";
        Label uploaderLabel = new Label("👤 Uploaded by: " + survey.getSurveyDoneByUser() + " (" + uploaderTeam + ")");
        uploaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));

        Label dateLabel = new Label("📅 Date: " + formatDateTime(survey.getUploadedAt()));
        dateLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));

        // Action buttons
        HBox actionButtons = new HBox(10);
        actionButtons.setAlignment(Pos.CENTER);
        actionButtons.setPadding(new Insets(10, 0, 0, 0));

        if (hasExcel) {
            Button downloadExcelBtn = new Button("📥 Download Excel");
            downloadExcelBtn.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20; -fx-font-weight: bold;");
            downloadExcelBtn.setOnAction(e -> handleDownloadSiteSurveyExcel(survey));
            actionButtons.getChildren().add(downloadExcelBtn);
        }

        if (hasZip) {
            Button downloadZipBtn = new Button("📦 Download ZIP");
            downloadZipBtn.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20; -fx-font-weight: bold;");
            downloadZipBtn.setOnAction(e -> handleDownloadSiteSurveyZip(survey));
            actionButtons.getChildren().add(downloadZipBtn);
        }

        Button viewButton = new Button("👁️ View Survey Data");
        viewButton.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20; -fx-font-weight: bold;");
        viewButton.setOnAction(e -> handleViewSiteSurvey(survey));
        actionButtons.getChildren().add(viewButton);

        completionBox.getChildren().addAll(statusLabel, fileTypeLabel, fileInfoBox, uploaderLabel, dateLabel, actionButtons);

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

        // OVERRIDE Next button handler to handle dynamic state changes
        nextButton.setOnAction(e -> {
            System.out.println("🔘 DEBUG: Next button clicked from Step 1 (survey completed)");

            // CRITICAL FIX: Always refresh workflow state first
            refreshWorkflow();

            // Re-check step completion after refresh
            Optional<WorkflowStepCompletion> step1Fresh = stepService.getStep(workflow.getId(), 1);
            boolean step1CompletedNow = step1Fresh.isPresent() && Boolean.TRUE.equals(step1Fresh.get().getCompleted());

            System.out.println("🔍 DEBUG: After refresh - Step 1 completed: " + step1CompletedNow + ", Workflow at step: " + workflow.getCurrentStep());

            // If workflow advanced to Step 2+, sync UI
            if (workflow.getCurrentStep() >= 2) {
                System.out.println("✅ DEBUG: Workflow already at step " + workflow.getCurrentStep() + ", syncing...");
                currentStep = workflow.getCurrentStep();
                loadCurrentStep();
            } else if (step1CompletedNow) {
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
        // Show file chooser for Excel OR ZIP upload
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Upload Site Survey (Excel or ZIP)");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("All Supported Files", "*.xlsx", "*.xls", "*.zip"),
            new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls"),
            new FileChooser.ExtensionFilter("ZIP Archives", "*.zip")
        );

        File file = fileChooser.showOpenDialog(this);
        if (file != null) {
            try {
                byte[] fileData = Files.readAllBytes(file.toPath());
                String fileName = file.getName();
                boolean isZip = fileName.toLowerCase().endsWith(".zip");

                if (isZip) {
                    // Upload as ZIP file
                    workflowService.processSiteSurveySalesWithZip(workflow.getId(), fileData,
                        fileName, currentUser);
                } else {
                    // Upload as Excel file
                    workflowService.processSiteSurveySales(workflow.getId(), fileData,
                        fileName, currentUser);
                }

                showSuccess("Site survey uploaded successfully! Moving to Step 2...");
                refreshWorkflow();

                // Automatically progress to Step 2
                currentStep = 2;
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

    /**
     * NEW: Handle site survey request with a specific assigned user.
     * Assigns the user to Step 1 and sends an email notification.
     */
    private void handleSiteSurveyProjectWithUser(User assignedUser) {
        try {
            // Get Step 1 completion record
            Optional<WorkflowStepCompletion> step1Opt = stepService.getStep(workflow.getId(), 1);
            if (step1Opt.isEmpty()) {
                showError("Could not find Step 1 for this workflow.");
                return;
            }

            WorkflowStepCompletion step1 = step1Opt.get();

            // Assign the selected user to this step
            stepService.assignUserToStep(step1, assignedUser, currentUser);

            // Mark that this step needs external action
            stepService.markNeedsExternalAction(step1, "PROJECT");

            // Also call the original workflow service method for compatibility
            workflowService.requestSiteSurveyFromProject(workflow.getId(), currentUser);

            // Send email notification to the assigned user
            boolean emailSent = workflowEmailService.sendStepAssignmentEmail(step1, assignedUser, currentUser, project);

            String message = "Site survey request sent to " + assignedUser.getUsername() + "!";
            if (emailSent) {
                message += "\n\n📧 Email notification sent to " + assignedUser.getEmail();
            } else {
                message += "\n\n⚠️ Could not send email notification (SMTP not configured for user).";
            }

            showInfo(message);

            // Refresh the UI to show pending state
            refreshWorkflow();
            loadCurrentStep();

        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Failed to send site survey request: " + ex.getMessage());
        }
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
        question.setTextFill(Color.WHITE);

        // No button - skip this step
        Button noButton = new Button("No - Skip this step");
        noButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        noButton.setOnAction(e -> {
            try {
                workflowService.markSelectionDesignNotNeeded(workflow.getId(), currentUser);
                showSuccess("Marked as not needed");
                refreshWorkflow();
                loadCurrentStep();
            } catch (Exception ex) {
                ex.printStackTrace();
                showError("Error: " + ex.getMessage());
            }
        });

        // User selection for Presales team
        VBox presalesRequestBox = new VBox(10);
        presalesRequestBox.setAlignment(Pos.CENTER_LEFT);
        presalesRequestBox.setPadding(new Insets(15));
        presalesRequestBox.setStyle("-fx-background-color: rgba(17, 153, 142, 0.1); -fx-border-color: #11998e; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label selectLabel = new Label("Yes - Assign to a Presales team member:");
        selectLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        selectLabel.setTextFill(Color.WHITE);

        // Dropdown for Presales users
        ComboBox<User> presalesUserCombo = new ComboBox<>();
        presalesUserCombo.setPromptText("Select Presales user...");
        presalesUserCombo.setPrefWidth(300);

        // Load Presales team users
        List<User> presalesUsers = userRepository.findByRoleAndActiveTrue(UserRole.PRESALES);
        presalesUserCombo.getItems().addAll(presalesUsers);

        // Custom cell factory
        presalesUserCombo.setCellFactory(lv -> new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                setText(empty || user == null ? null : user.getUsername() +
                    (user.getEmail() != null ? " (" + user.getEmail() + ")" : ""));
            }
        });
        presalesUserCombo.setButtonCell(new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                setText(empty || user == null ? null : user.getUsername());
            }
        });

        Button sendRequestButton = new Button("📧 Send Request to Selected User");
        sendRequestButton.setStyle("-fx-background-color: #11998e; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        sendRequestButton.setDisable(true);

        presalesUserCombo.setOnAction(e -> sendRequestButton.setDisable(presalesUserCombo.getValue() == null));

        sendRequestButton.setOnAction(e -> {
            User selectedUser = presalesUserCombo.getValue();
            if (selectedUser != null) {
                handleSelectionDesignWithUser(selectedUser);
            }
        });

        Label emailInfo = new Label("An email notification will be sent to request sizing/pricing.");
        emailInfo.setFont(Font.font("System", FontWeight.NORMAL, 12));
        emailInfo.setTextFill(Color.web("#94a3b8"));

        presalesRequestBox.getChildren().addAll(selectLabel, presalesUserCombo, sendRequestButton, emailInfo);

        VBox optionsBox = new VBox(20);
        optionsBox.setAlignment(Pos.CENTER);
        optionsBox.getChildren().addAll(question, noButton, presalesRequestBox);

        stepContainer.getChildren().add(optionsBox);
    }

    /**
     * Handle selection/design request with a specific assigned Presales user.
     */
    private void handleSelectionDesignWithUser(User assignedUser) {
        try {
            Optional<WorkflowStepCompletion> step2Opt = stepService.getStep(workflow.getId(), 2);
            if (step2Opt.isEmpty()) {
                showError("Could not find Step 2 for this workflow.");
                return;
            }

            WorkflowStepCompletion step2 = step2Opt.get();

            // Assign the selected user
            stepService.assignUserToStep(step2, assignedUser, currentUser);
            stepService.markNeedsExternalAction(step2, "PRESALES");

            // Call original workflow service method
            workflowService.requestSelectionDesignFromPresales(workflow.getId(), currentUser);

            // Send email notification
            boolean emailSent = workflowEmailService.sendStepAssignmentEmail(step2, assignedUser, currentUser, project);

            String message = "Sizing/design request sent to " + assignedUser.getUsername() + "!";
            if (emailSent) {
                message += "\n\n📧 Email notification sent to " + assignedUser.getEmail();
            } else {
                message += "\n\n⚠️ Could not send email notification (SMTP not configured).";
            }

            showInfo(message);
            refreshWorkflow();
            loadCurrentStep();

        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Failed to send request: " + ex.getMessage());
        }
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

        // OVERRIDE Next button handler to handle dynamic state changes
        nextButton.setOnAction(e -> {
            System.out.println("🔘 DEBUG: Next button clicked from Step 2 (sizing completed)");

            // CRITICAL FIX: Always refresh workflow state first
            refreshWorkflow();

            // Re-check step completion after refresh
            Optional<WorkflowStepCompletion> step2Fresh = stepService.getStep(workflow.getId(), 2);
            boolean step2CompletedNow = step2Fresh.isPresent() && Boolean.TRUE.equals(step2Fresh.get().getCompleted());

            System.out.println("🔍 DEBUG: After refresh - Step 2 completed: " + step2CompletedNow + ", Workflow at step: " + workflow.getCurrentStep());

            // If workflow already advanced to Step 3+, sync UI
            if (workflow.getCurrentStep() >= 3) {
                System.out.println("✅ DEBUG: Workflow already at step " + workflow.getCurrentStep() + ", syncing...");
                currentStep = workflow.getCurrentStep();
                loadCurrentStep();
            } else if (step2CompletedNow) {
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
        // CRITICAL FIX: Refresh workflow state and check for bank guarantee data
        refreshWorkflow();

        // Check if bank guarantee already exists (uploaded by Finance team)
        Optional<com.magictech.modules.sales.entity.BankGuaranteeData> bankGuaranteeOpt =
            getBankGuaranteeRepository().findByWorkflowIdAndActiveTrue(workflow.getId());

        if (bankGuaranteeOpt.isPresent()) {
            // Bank guarantee completed - show download/view UI
            showBankGuaranteeCompleted(bankGuaranteeOpt.get());
        } else {
            // Check if request is pending (waiting for Finance)
            Optional<WorkflowStepCompletion> step3Opt = stepService.getStep(workflow.getId(), 3);
            if (step3Opt.isPresent()) {
                WorkflowStepCompletion step3 = step3Opt.get();
                if (Boolean.TRUE.equals(step3.getNeedsExternalAction()) &&
                    Boolean.FALSE.equals(step3.getExternalActionCompleted())) {
                    // Request is pending - show waiting UI
                    showBankGuaranteePendingUI(step3);
                    return;
                }
            }

            // Not uploaded yet and no pending request - show original request options
            showBankGuaranteeRequestOptions();
        }
    }

    private void showBankGuaranteeRequestOptions() {
        Label question = new Label("Does project need Bank Guarantee?");
        question.setFont(Font.font("System", FontWeight.NORMAL, 16));
        question.setTextFill(Color.WHITE);

        // No button - skip this step
        Button noButton = new Button("No - Skip this step");
        noButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        noButton.setOnAction(e -> {
            try {
                workflowService.markBankGuaranteeNotNeeded(workflow.getId(), currentUser);
                showSuccess("Marked as not needed");
                refreshWorkflow();
                loadCurrentStep();
            } catch (Exception ex) {
                ex.printStackTrace();
                showError("Error: " + ex.getMessage());
            }
        });

        // User selection for Finance team
        VBox financeRequestBox = new VBox(10);
        financeRequestBox.setAlignment(Pos.CENTER_LEFT);
        financeRequestBox.setPadding(new Insets(15));
        financeRequestBox.setStyle("-fx-background-color: rgba(238, 156, 167, 0.1); -fx-border-color: #ee9ca7; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label selectLabel = new Label("Yes - Assign to a Finance team member:");
        selectLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        selectLabel.setTextFill(Color.WHITE);

        // Dropdown for Finance users
        ComboBox<User> financeUserCombo = new ComboBox<>();
        financeUserCombo.setPromptText("Select Finance user...");
        financeUserCombo.setPrefWidth(300);

        // Load Finance team users
        List<User> financeUsers = userRepository.findByRoleAndActiveTrue(UserRole.FINANCE);
        financeUserCombo.getItems().addAll(financeUsers);

        // Custom cell factory
        financeUserCombo.setCellFactory(lv -> new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                setText(empty || user == null ? null : user.getUsername() +
                    (user.getEmail() != null ? " (" + user.getEmail() + ")" : ""));
            }
        });
        financeUserCombo.setButtonCell(new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                setText(empty || user == null ? null : user.getUsername());
            }
        });

        Button sendRequestButton = new Button("📧 Send Request to Selected User");
        sendRequestButton.setStyle("-fx-background-color: #ee9ca7; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        sendRequestButton.setDisable(true);

        financeUserCombo.setOnAction(e -> sendRequestButton.setDisable(financeUserCombo.getValue() == null));

        sendRequestButton.setOnAction(e -> {
            User selectedUser = financeUserCombo.getValue();
            if (selectedUser != null) {
                handleBankGuaranteeWithUser(selectedUser);
            }
        });

        Label emailInfo = new Label("An email notification will be sent to request bank guarantee.");
        emailInfo.setFont(Font.font("System", FontWeight.NORMAL, 12));
        emailInfo.setTextFill(Color.web("#94a3b8"));

        financeRequestBox.getChildren().addAll(selectLabel, financeUserCombo, sendRequestButton, emailInfo);

        VBox optionsBox = new VBox(20);
        optionsBox.setAlignment(Pos.CENTER);
        optionsBox.getChildren().addAll(question, noButton, financeRequestBox);

        stepContainer.getChildren().add(optionsBox);
    }

    /**
     * Handle bank guarantee request with a specific assigned Finance user.
     */
    private void handleBankGuaranteeWithUser(User assignedUser) {
        try {
            Optional<WorkflowStepCompletion> step3Opt = stepService.getStep(workflow.getId(), 3);
            if (step3Opt.isEmpty()) {
                showError("Could not find Step 3 for this workflow.");
                return;
            }

            WorkflowStepCompletion step3 = step3Opt.get();

            // Assign the selected user
            stepService.assignUserToStep(step3, assignedUser, currentUser);
            stepService.markNeedsExternalAction(step3, "FINANCE");

            // Call original workflow service method
            workflowService.requestBankGuarantee(workflow.getId(), currentUser);

            // Send email notification
            boolean emailSent = workflowEmailService.sendStepAssignmentEmail(step3, assignedUser, currentUser, project);

            String message = "Bank guarantee request sent to " + assignedUser.getUsername() + "!";
            if (emailSent) {
                message += "\n\n📧 Email notification sent to " + assignedUser.getEmail();
            } else {
                message += "\n\n⚠️ Could not send email notification (SMTP not configured).";
            }

            showInfo(message);
            refreshWorkflow();
            loadCurrentStep();

        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Failed to send request: " + ex.getMessage());
        }
    }

    private void showBankGuaranteePendingUI(WorkflowStepCompletion step) {
        VBox pendingBox = new VBox(15);
        pendingBox.setAlignment(Pos.CENTER_LEFT);
        pendingBox.setPadding(new Insets(20));
        pendingBox.setStyle("-fx-background-color: #fef3c7; -fx-border-color: #f59e0b; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label statusLabel = new Label("⏳ Waiting for Finance Team");
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        statusLabel.setTextFill(Color.web("#92400e"));

        Label messageLabel = new Label("You requested bank guarantee from the Finance team.");
        messageLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        messageLabel.setWrapText(true);

        Label infoLabel = new Label("💰 The Finance team will upload the bank guarantee Excel file soon.");
        infoLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        infoLabel.setWrapText(true);
        infoLabel.setTextFill(Color.web("#78350f"));

        Button refreshButton = new Button("🔄 Check Status");
        refreshButton.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20; -fx-font-weight: bold;");
        refreshButton.setOnAction(e -> {
            refreshWorkflow();
            loadCurrentStep();
        });

        pendingBox.getChildren().addAll(statusLabel, messageLabel, infoLabel, refreshButton);
        stepContainer.getChildren().add(pendingBox);

        // Disable Next button while waiting
        nextButton.setDisable(true);
    }

    private void showBankGuaranteeCompleted(com.magictech.modules.sales.entity.BankGuaranteeData bankGuarantee) {
        VBox completionBox = new VBox(15);
        completionBox.setAlignment(Pos.CENTER_LEFT);
        completionBox.setPadding(new Insets(20));
        completionBox.setStyle("-fx-background-color: #d1fae5; -fx-border-color: #10b981; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label statusLabel = new Label("✅ Bank Guarantee Completed");
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        statusLabel.setTextFill(Color.web("#065f46"));

        Label fileLabel = new Label("📄 File: " + bankGuarantee.getFileName());
        fileLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));

        Label uploaderLabel = new Label("👤 Uploaded by: " + bankGuarantee.getUploadedBy() + " (FINANCE team)");
        uploaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));

        Label dateLabel = new Label("📅 Date: " + formatDateTime(bankGuarantee.getUploadedAt()));
        dateLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));

        Label sizeLabel = new Label("💾 Size: " + formatFileSize(bankGuarantee.getFileSize()));
        sizeLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));

        // Action button
        HBox actionButtons = new HBox(10);
        actionButtons.setAlignment(Pos.CENTER);
        actionButtons.setPadding(new Insets(10, 0, 0, 0));

        Button downloadButton = new Button("📥 Download Bank Guarantee File");
        downloadButton.setStyle("-fx-background-color: #eab308; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20; -fx-font-weight: bold;");
        downloadButton.setOnAction(e -> handleDownloadBankGuarantee(bankGuarantee));

        actionButtons.getChildren().add(downloadButton);

        completionBox.getChildren().addAll(statusLabel, fileLabel, uploaderLabel, dateLabel, sizeLabel, actionButtons);

        // Info message about progression
        Label progressInfo = new Label("✅ Step 3 completed. Click 'Next →' to proceed to Step 4.");
        progressInfo.setFont(Font.font("System", FontWeight.BOLD, 14));
        progressInfo.setTextFill(Color.web("#065f46"));
        progressInfo.setPadding(new Insets(15, 0, 0, 0));

        stepContainer.getChildren().addAll(completionBox, progressInfo);

        // FORCE enable Next button since bank guarantee exists
        nextButton.setDisable(false);

        // OVERRIDE Next button handler to handle dynamic state changes
        nextButton.setOnAction(e -> {
            System.out.println("🔘 DEBUG: Next button clicked from Step 3 (bank guarantee completed)");

            // CRITICAL FIX: Always refresh workflow state first
            refreshWorkflow();

            // Re-check step completion after refresh
            Optional<WorkflowStepCompletion> step3Fresh = stepService.getStep(workflow.getId(), 3);
            boolean step3CompletedNow = step3Fresh.isPresent() && Boolean.TRUE.equals(step3Fresh.get().getCompleted());

            System.out.println("🔍 DEBUG: After refresh - Step 3 completed: " + step3CompletedNow + ", Workflow at step: " + workflow.getCurrentStep());

            // If workflow advanced to Step 4+, sync UI
            if (workflow.getCurrentStep() >= 4) {
                System.out.println("✅ DEBUG: Workflow already at step " + workflow.getCurrentStep() + ", syncing...");
                currentStep = workflow.getCurrentStep();
                loadCurrentStep();
            } else if (step3CompletedNow) {
                // Step 3 is completed, move to Step 4
                System.out.println("✅ DEBUG: Step 3 completed, moving to Step 4");
                currentStep = 4;
                loadCurrentStep();
            } else {
                System.out.println("⚠️ WARNING: Bank guarantee exists but step 3 is NOT completed!");
                showWarning("Step 3 is not yet completed. Please wait for the workflow to update.");
            }
        });
    }

    private void handleDownloadBankGuarantee(com.magictech.modules.sales.entity.BankGuaranteeData bankGuarantee) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Bank Guarantee Excel");
        fileChooser.setInitialFileName(bankGuarantee.getFileName());
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls")
        );

        File file = fileChooser.showSaveDialog(this);
        if (file != null) {
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
                fos.write(bankGuarantee.getExcelFile());
                showSuccess("Bank guarantee downloaded successfully!\nSaved to: " + file.getAbsolutePath());
            } catch (Exception ex) {
                showError("Failed to download bank guarantee: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    private com.magictech.modules.sales.repository.BankGuaranteeDataRepository getBankGuaranteeRepository() {
        return bankGuaranteeRepository;
    }

    // STEP 4: Missing Item Check
    // This step allows Sales to add elements from storage database to the project
    private void loadStep4_MissingItem() {
        VBox contentBox = new VBox(15);
        contentBox.setAlignment(Pos.CENTER_LEFT);
        contentBox.setPadding(new Insets(10));

        Label question = new Label("Is there any missing item?");
        question.setFont(Font.font("System", FontWeight.BOLD, 18));
        question.setTextFill(Color.web("#2c3e50"));

        Label infoLabel = new Label(
            "If items are missing from the project, click 'Yes - Add Elements' to:\n" +
            "• Open the Project Elements tab\n" +
            "• Add items from the storage database\n" +
            "• Notify Presales about the changes"
        );
        infoLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        infoLabel.setWrapText(true);
        infoLabel.setTextFill(Color.web("#7f8c8d"));

        // No missing items - complete step and advance
        Button noButton = new Button("No");
        noButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20; -fx-font-weight: bold;");
        noButton.setOnAction(e -> {
            workflowService.markNoMissingItems(workflow.getId(), currentUser);
            showSuccess("Marked as no missing items. Advancing to Step 5...");
            // Auto-advance to next step
            advanceToNextStepAndUpdateUI();
        });

        // Yes - Navigate to Project Elements tab to add items from storage
        Button yesButton = new Button("Yes - Add Elements");
        yesButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20; -fx-font-weight: bold;");
        yesButton.setOnAction(e -> handleNavigateToProjectElements());

        HBox buttons = new HBox(15, noButton, yesButton);
        buttons.setAlignment(Pos.CENTER);
        buttons.setPadding(new Insets(20, 0, 0, 0));

        // Info box about workflow
        VBox infoBox = new VBox(8);
        infoBox.setStyle("-fx-background-color: #e3f2fd; -fx-border-color: #2196f3; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 15;");

        Label infoTitle = new Label("📋 Step 4 Workflow:");
        infoTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        infoTitle.setTextFill(Color.web("#1565c0"));

        Label infoContent = new Label(
            "1. Click 'Yes - Add Elements' to minimize this dialog\n" +
            "2. Navigate to Project Elements tab\n" +
            "3. Add items from storage database to the project\n" +
            "4. Click 'Complete Step 4' button when done\n" +
            "5. Presales team will be notified of the changes"
        );
        infoContent.setFont(Font.font("System", FontWeight.NORMAL, 12));
        infoContent.setTextFill(Color.web("#1976d2"));
        infoContent.setWrapText(true);

        infoBox.getChildren().addAll(infoTitle, infoContent);

        contentBox.getChildren().addAll(question, infoLabel, buttons, infoBox);
        stepContainer.getChildren().add(contentBox);
    }

    /**
     * Handle navigation to Project Elements tab for adding items from storage
     * This minimizes the workflow dialog and notifies the parent controller
     */
    private void handleNavigateToProjectElements() {
        System.out.println("📦 Step 4: User clicked 'Yes - Add Elements'");
        System.out.println("   Project: " + project.getProjectName());
        System.out.println("   Workflow ID: " + workflow.getId());

        if (callback != null) {
            // Minimize the dialog
            minimizeToBar();

            // Notify parent controller to navigate to project elements tab
            callback.onNavigateToProjectElements(project, workflow.getId());

            showInfo(
                "Workflow minimized. Navigate to Project Elements tab to add items.\n\n" +
                "Click 'Complete Step 4' when you're done adding elements."
            );
        } else {
            // Fallback if no callback is set - show legacy dialog
            System.out.println("⚠️ WARNING: No callback set, showing legacy dialog");
            showLegacyMissingItemDialog();
        }
    }

    /**
     * Complete Step 4 after elements have been added
     * Called by SalesStorageController when user clicks "Complete Step 4" button
     */
    public void completeStep4WithElements() {
        System.out.println("✅ Completing Step 4 with elements added");

        // Mark step 4 as completed in the workflow service
        workflowService.completeStep4WithElements(workflow.getId(), currentUser);

        // Restore dialog and advance to next step
        restoreFromBar();
        showSuccess("Step 4 completed! Elements have been added. Presales team notified.");

        // Auto-advance to Step 5
        advanceToNextStepAndUpdateUI();
    }

    /**
     * Legacy dialog for missing items - used as fallback if callback not set
     */
    private void showLegacyMissingItemDialog() {
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
                try {
                    request.setQuantityNeeded(Integer.parseInt(quantityField.getText()));
                } catch (NumberFormatException ex) {
                    request.setQuantityNeeded(1);
                }
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
        // CRITICAL FIX: Refresh workflow state from database first
        refreshWorkflow();

        System.out.println("🔍 DEBUG: Loading Step 5, current workflow step: " + workflow.getCurrentStep());

        // Check if step 5 is already completed (Project team completed execution)
        Optional<WorkflowStepCompletion> step5Opt = stepService.getStep(workflow.getId(), 5);
        if (step5Opt.isPresent()) {
            WorkflowStepCompletion step5 = step5Opt.get();

            // Check if step is completed
            if (Boolean.TRUE.equals(step5.getCompleted())) {
                System.out.println("✅ DEBUG: Step 5 is already completed");
                showTenderAcceptanceCompleted(step5);
                return;
            }

            // Check if tender was accepted and waiting for PROJECT team
            if (Boolean.TRUE.equals(step5.getNeedsExternalAction()) &&
                "PROJECT".equals(step5.getExternalModule()) &&
                Boolean.FALSE.equals(step5.getExternalActionCompleted())) {
                System.out.println("⏳ DEBUG: Step 5 waiting for Project team");
                showTenderAcceptancePendingUI(step5);
                return;
            }
        }

        System.out.println("❌ DEBUG: Step 5 - showing tender acceptance options");
        showTenderAcceptanceOptions();
    }

    private void showTenderAcceptanceOptions() {
        Label question = new Label("Does the Tender Accepted?");
        question.setFont(Font.font("System", FontWeight.BOLD, 18));
        question.setTextFill(Color.WHITE);

        Label infoLabel = new Label(
            "If the tender is accepted, select a Project team member to execute the project.\n" +
            "If rejected, please provide a reason."
        );
        infoLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        infoLabel.setWrapText(true);
        infoLabel.setTextFill(Color.web("#94a3b8"));

        // ========== PROJECT USER SELECTION ==========
        VBox selectionBox = new VBox(10);
        selectionBox.setStyle("-fx-background-color: rgba(39, 174, 96, 0.1); -fx-border-color: #27ae60; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 15;");

        Label selectLabel = new Label("📋 Select Project Team Member to Execute:");
        selectLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        selectLabel.setTextFill(Color.web("#27ae60"));

        ComboBox<User> projectUserCombo = new ComboBox<>();
        projectUserCombo.setPromptText("Select a Project Manager...");
        projectUserCombo.setPrefWidth(350);
        projectUserCombo.setStyle("-fx-background-color: white; -fx-font-size: 14px;");

        // Load PROJECT users
        List<User> projectUsers = userRepository.findByRoleAndActiveTrue(UserRole.PROJECTS);
        projectUserCombo.getItems().addAll(projectUsers);

        // Custom cell factory to display user info
        projectUserCombo.setCellFactory(lv -> new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                } else {
                    setText(user.getUsername() + (user.getEmail() != null ? " (" + user.getEmail() + ")" : ""));
                }
            }
        });
        projectUserCombo.setButtonCell(new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText("Select a Project Manager...");
                } else {
                    setText(user.getUsername() + (user.getEmail() != null ? " (" + user.getEmail() + ")" : ""));
                }
            }
        });

        Label userCountLabel = new Label("👥 Available project managers: " + projectUsers.size());
        userCountLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        userCountLabel.setTextFill(Color.web("#6b7280"));

        selectionBox.getChildren().addAll(selectLabel, projectUserCombo, userCountLabel);

        Button yesButton = new Button("Yes - Assign to Project Manager");
        yesButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20; -fx-font-weight: bold;");
        yesButton.setDisable(true); // Disabled until user is selected

        // Enable button only when user is selected
        projectUserCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            yesButton.setDisable(newVal == null);
        });

        yesButton.setOnAction(e -> {
            User selectedUser = projectUserCombo.getValue();
            if (selectedUser != null) {
                handleTenderAcceptedWithUser(selectedUser);
            }
        });

        Button noButton = new Button("No - Tender Rejected");
        noButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20; -fx-font-weight: bold;");
        noButton.setOnAction(e -> showRejectionDialog());

        HBox buttons = new HBox(15, yesButton, noButton);
        buttons.setAlignment(Pos.CENTER);
        buttons.setPadding(new Insets(20, 0, 0, 0));

        stepContainer.getChildren().addAll(question, infoLabel, selectionBox, buttons);
    }

    /**
     * Handle tender acceptance with assigned Project user
     */
    private void handleTenderAcceptedWithUser(User assignedUser) {
        try {
            // Get Step 5 and assign the selected user
            Optional<WorkflowStepCompletion> step5Opt = stepService.getStep(workflow.getId(), 5);
            if (step5Opt.isPresent()) {
                WorkflowStepCompletion step5 = step5Opt.get();

                // Assign the project user to this step
                stepService.assignUserToStep(step5, assignedUser, currentUser);

                // Set target role for tracking
                step5.setTargetRole("PROJECTS");

                // Mark tender as accepted (this sets external action needed)
                workflowService.markTenderAccepted(workflow.getId(), currentUser);

                // Send email notification to assigned user
                boolean emailSent = workflowEmailService.sendStepAssignmentEmail(step5, assignedUser, currentUser, project);

                String successMsg = "Tender accepted! Assigned to " + assignedUser.getUsername() + " for project execution.";
                if (emailSent) {
                    successMsg += "\n📧 Email notification sent.";
                } else {
                    successMsg += "\n⚠️ Could not send email (SMTP not configured for user).";
                }
                showSuccess(successMsg);

                // Refresh and reload to show pending state
                refreshWorkflow();
                loadCurrentStep();
            }
        } catch (Exception ex) {
            showError("Failed to assign project execution: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Show pending/waiting state for Step 5 when waiting for Project team
     */
    private void showTenderAcceptancePendingUI(WorkflowStepCompletion step) {
        VBox pendingBox = new VBox(15);
        pendingBox.setAlignment(Pos.CENTER_LEFT);
        pendingBox.setPadding(new Insets(20));
        pendingBox.setStyle("-fx-background-color: #fef3c7; -fx-border-color: #f59e0b; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label statusLabel = new Label("⏳ Waiting for Project Team");
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        statusLabel.setTextFill(Color.web("#92400e"));

        // Show assigned user info
        String assignedInfo = "Tender has been accepted!";
        if (step.getAssignedUsername() != null) {
            assignedInfo += " Assigned to: " + step.getAssignedUsername();
            if (step.getAssignedUserEmail() != null) {
                assignedInfo += " (" + step.getAssignedUserEmail() + ")";
            }
        } else {
            assignedInfo += " The Project team has been notified to start work.";
        }
        Label messageLabel = new Label(assignedInfo);
        messageLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        messageLabel.setWrapText(true);

        // Show email status
        if (step.getAssignedUsername() != null) {
            String emailStatus = Boolean.TRUE.equals(step.getEmailSent())
                ? "📧 Email notification sent successfully"
                : "⚠️ Email notification not sent";
            Label emailLabel = new Label(emailStatus);
            emailLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
            emailLabel.setTextFill(Color.web("#78350f"));
            pendingBox.getChildren().add(emailLabel);
        }

        Label infoLabel = new Label("📋 " + (step.getAssignedUsername() != null ? step.getAssignedUsername() : "The Project team") + " will execute the project and notify you when completed.");
        infoLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        infoLabel.setWrapText(true);
        infoLabel.setTextFill(Color.web("#78350f"));

        Label dateLabel = new Label("📅 Accepted: " + formatDateTime(step.getAssignedAt() != null ? step.getAssignedAt() : step.getCreatedAt()));
        dateLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        dateLabel.setTextFill(Color.web("#78350f"));

        // Refresh button to check if Project team has completed
        Button refreshButton = new Button("🔄 Check Status");
        refreshButton.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20; -fx-font-weight: bold;");
        refreshButton.setOnAction(e -> {
            refreshWorkflow();
            loadCurrentStep();
        });

        pendingBox.getChildren().addAll(statusLabel, messageLabel, infoLabel, dateLabel, refreshButton);

        Label waitingInfo = new Label("💡 Click 'Check Status' to see if " + (step.getAssignedUsername() != null ? step.getAssignedUsername() : "the Project team") + " has completed the work. The workflow will automatically advance once they confirm completion.");
        waitingInfo.setFont(Font.font("System", FontWeight.NORMAL, 12));
        waitingInfo.setTextFill(Color.web("#6b7280"));
        waitingInfo.setPadding(new Insets(15, 0, 0, 0));
        waitingInfo.setWrapText(true);

        stepContainer.getChildren().addAll(pendingBox, waitingInfo);

        // Disable Next button while waiting for Project team
        nextButton.setDisable(true);
    }

    /**
     * Show completed state for Step 5 when Project team has finished
     */
    private void showTenderAcceptanceCompleted(WorkflowStepCompletion step) {
        VBox completionBox = new VBox(15);
        completionBox.setAlignment(Pos.CENTER_LEFT);
        completionBox.setPadding(new Insets(20));
        completionBox.setStyle("-fx-background-color: #d1fae5; -fx-border-color: #10b981; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label statusLabel = new Label("✅ Tender Accepted & Project Completed");
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        statusLabel.setTextFill(Color.web("#065f46"));

        // Show who was assigned
        String assignedInfo = "The tender was accepted";
        if (step.getAssignedUsername() != null) {
            assignedInfo += " and assigned to " + step.getAssignedUsername();
        }
        assignedInfo += ". Work has been completed.";
        Label messageLabel = new Label(assignedInfo);
        messageLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        messageLabel.setWrapText(true);

        // Show completed by (may be different from assigned)
        String completedBy = step.getCompletedBy() != null ? step.getCompletedBy() :
                            (step.getAssignedUsername() != null ? step.getAssignedUsername() : "Project Team");
        Label completedLabel = new Label("✓ Completed by: " + completedBy);
        completedLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        completedLabel.setTextFill(Color.web("#065f46"));

        Label dateLabel = new Label("📅 Completed: " + formatDateTime(step.getCompletedAt()));
        dateLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        dateLabel.setTextFill(Color.web("#065f46"));

        completionBox.getChildren().addAll(statusLabel, messageLabel, completedLabel, dateLabel);

        // Info message about progression
        Label progressInfo = new Label("✅ Step 5 completed. Click 'Next →' to proceed to Step 6.");
        progressInfo.setFont(Font.font("System", FontWeight.BOLD, 14));
        progressInfo.setTextFill(Color.web("#059669"));
        progressInfo.setPadding(new Insets(15, 0, 0, 0));

        stepContainer.getChildren().addAll(completionBox, progressInfo);

        // Enable Next button since step is completed
        nextButton.setDisable(false);
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
        fileChooser.setTitle("Upload Project Cost (Excel or ZIP)");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("All Supported Files", "*.xlsx", "*.xls", "*.zip"),
            new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls"),
            new FileChooser.ExtensionFilter("ZIP Archives", "*.zip")
        );

        File file = fileChooser.showOpenDialog(this);
        if (file != null) {
            try {
                byte[] fileData = Files.readAllBytes(file.toPath());
                String fileName = file.getName();
                boolean isZip = fileName.toLowerCase().endsWith(".zip");

                if (isZip) {
                    workflowService.confirmProjectFinishedWithZip(workflow.getId(), fileData,
                        fileName, currentUser);
                } else {
                    workflowService.confirmProjectFinished(workflow.getId(), fileData,
                        fileName, currentUser);
                }

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
        VBox contentBox = new VBox(15);
        contentBox.setAlignment(Pos.CENTER_LEFT);
        contentBox.setPadding(new Insets(10));

        Label question = new Label("After Sales Support Required?");
        question.setFont(Font.font("System", FontWeight.BOLD, 18));
        question.setTextFill(Color.WHITE);

        Label infoLabel = new Label(
            "If after-sales support is needed, select a QA team member to handle the request."
        );
        infoLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        infoLabel.setWrapText(true);
        infoLabel.setTextFill(Color.web("#94a3b8"));

        // ========== QA USER SELECTION ==========
        VBox selectionBox = new VBox(10);
        selectionBox.setStyle("-fx-background-color: rgba(156, 39, 176, 0.1); -fx-border-color: #9c27b0; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 15;");

        Label selectLabel = new Label("🔍 Select Quality Assurance Team Member:");
        selectLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        selectLabel.setTextFill(Color.web("#9c27b0"));

        ComboBox<User> qaUserCombo = new ComboBox<>();
        qaUserCombo.setPromptText("Select a QA team member...");
        qaUserCombo.setPrefWidth(350);
        qaUserCombo.setStyle("-fx-background-color: white; -fx-font-size: 14px;");

        // Load QUALITY_ASSURANCE users
        List<User> qaUsers = userRepository.findByRoleAndActiveTrue(UserRole.QUALITY_ASSURANCE);
        qaUserCombo.getItems().addAll(qaUsers);

        // Custom cell factory to display user info
        qaUserCombo.setCellFactory(lv -> new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                } else {
                    setText(user.getUsername() + (user.getEmail() != null ? " (" + user.getEmail() + ")" : ""));
                }
            }
        });
        qaUserCombo.setButtonCell(new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText("Select a QA team member...");
                } else {
                    setText(user.getUsername() + (user.getEmail() != null ? " (" + user.getEmail() + ")" : ""));
                }
            }
        });

        Label userCountLabel = new Label("👥 Available QA team members: " + qaUsers.size());
        userCountLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        userCountLabel.setTextFill(Color.web("#6b7280"));

        selectionBox.getChildren().addAll(selectLabel, qaUserCombo, userCountLabel);

        Button noButton = new Button("No - Not Needed");
        noButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20; -fx-font-weight: bold;");
        noButton.setOnAction(e -> {
            workflowService.markAfterSalesNotNeeded(workflow.getId(), currentUser);
            showSuccess("Marked as not needed. Advancing to completion...");
            refreshWorkflow();
            loadCurrentStep();
        });

        Button yesButton = new Button("Yes - Assign to QA");
        yesButton.setStyle("-fx-background-color: #9c27b0; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20; -fx-font-weight: bold;");
        yesButton.setDisable(true); // Disabled until user is selected

        // Enable button only when user is selected
        qaUserCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            yesButton.setDisable(newVal == null);
        });

        yesButton.setOnAction(e -> {
            User selectedUser = qaUserCombo.getValue();
            if (selectedUser != null) {
                handleAfterSalesWithUser(selectedUser);
            }
        });

        HBox buttons = new HBox(15, noButton, yesButton);
        buttons.setAlignment(Pos.CENTER);
        buttons.setPadding(new Insets(20, 0, 0, 0));

        contentBox.getChildren().addAll(question, infoLabel, selectionBox, buttons);
        stepContainer.getChildren().add(contentBox);
    }

    /**
     * Handle after-sales check with assigned QA user
     */
    private void handleAfterSalesWithUser(User assignedUser) {
        try {
            // Get Step 7 and assign the selected QA user
            Optional<WorkflowStepCompletion> step7Opt = stepService.getStep(workflow.getId(), 7);
            if (step7Opt.isPresent()) {
                WorkflowStepCompletion step7 = step7Opt.get();

                // Assign the QA user to this step
                stepService.assignUserToStep(step7, assignedUser, currentUser);

                // Set target role for tracking
                step7.setTargetRole("QUALITY_ASSURANCE");

                // Mark as needing QA check
                workflowService.requestAfterSalesCheck(workflow.getId(), currentUser);

                // Send email notification to assigned user
                boolean emailSent = workflowEmailService.sendStepAssignmentEmail(step7, assignedUser, currentUser, project);

                String successMsg = "After-sales check assigned to " + assignedUser.getUsername() + ".";
                if (emailSent) {
                    successMsg += "\n📧 Email notification sent.";
                } else {
                    successMsg += "\n⚠️ Could not send email (SMTP not configured for user).";
                }
                showInfo(successMsg);

                refreshWorkflow();
                loadCurrentStep();
            }
        } catch (Exception ex) {
            showError("Failed to assign after-sales check: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // STEP 8: Completion
    private void loadStep8_Completion() {
        VBox contentBox = new VBox(15);
        contentBox.setAlignment(Pos.CENTER_LEFT);
        contentBox.setPadding(new Insets(10));

        Label question = new Label("Complete the Workflow?");
        question.setFont(Font.font("System", FontWeight.BOLD, 18));
        question.setTextFill(Color.WHITE);

        Label info = new Label("All project data will be pushed to storage analysis.\nSelect a Manager/Master for final sign-off approval.");
        info.setFont(Font.font("System", FontWeight.NORMAL, 14));
        info.setTextFill(Color.web("#94a3b8"));
        info.setWrapText(true);

        // ========== MASTER USER SELECTION ==========
        VBox selectionBox = new VBox(10);
        selectionBox.setStyle("-fx-background-color: rgba(255, 193, 7, 0.1); -fx-border-color: #ffc107; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 15;");

        Label selectLabel = new Label("👑 Select Manager for Final Approval:");
        selectLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        selectLabel.setTextFill(Color.web("#f57c00"));

        ComboBox<User> masterUserCombo = new ComboBox<>();
        masterUserCombo.setPromptText("Select a Manager...");
        masterUserCombo.setPrefWidth(350);
        masterUserCombo.setStyle("-fx-background-color: white; -fx-font-size: 14px;");

        // Load MASTER users
        List<User> masterUsers = userRepository.findByRoleAndActiveTrue(UserRole.MASTER);
        masterUserCombo.getItems().addAll(masterUsers);

        // Custom cell factory to display user info
        masterUserCombo.setCellFactory(lv -> new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                } else {
                    setText(user.getUsername() + (user.getEmail() != null ? " (" + user.getEmail() + ")" : ""));
                }
            }
        });
        masterUserCombo.setButtonCell(new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText("Select a Manager...");
                } else {
                    setText(user.getUsername() + (user.getEmail() != null ? " (" + user.getEmail() + ")" : ""));
                }
            }
        });

        Label userCountLabel = new Label("👥 Available managers: " + masterUsers.size());
        userCountLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        userCountLabel.setTextFill(Color.web("#6b7280"));

        selectionBox.getChildren().addAll(selectLabel, masterUserCombo, userCountLabel);

        // Complete button - requires master selection
        Button completeButton = new Button("✓ Request Final Approval");
        completeButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 15 30; -fx-font-weight: bold;");
        completeButton.setDisable(true); // Disabled until user is selected

        // Enable button only when user is selected
        masterUserCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            completeButton.setDisable(newVal == null);
        });

        completeButton.setOnAction(e -> {
            User selectedUser = masterUserCombo.getValue();
            if (selectedUser != null) {
                handleCompletionWithUser(selectedUser);
            }
        });

        // Skip approval option (for MASTER users)
        Button selfCompleteButton = new Button("Complete Without Approval");
        selfCompleteButton.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        selfCompleteButton.setVisible(currentUser.getRole() == UserRole.MASTER);
        selfCompleteButton.setOnAction(e -> {
            workflowService.completeWorkflow(workflow.getId(), currentUser);
            showSuccess("Workflow completed! All data pushed to storage analysis.");
            close();
        });

        HBox buttons = new HBox(15, completeButton, selfCompleteButton);
        buttons.setAlignment(Pos.CENTER);
        buttons.setPadding(new Insets(20, 0, 0, 0));

        contentBox.getChildren().addAll(question, info, selectionBox, buttons);
        stepContainer.getChildren().add(contentBox);
    }

    /**
     * Handle workflow completion with manager approval request
     */
    private void handleCompletionWithUser(User approver) {
        try {
            // Get Step 8 and assign the selected manager
            Optional<WorkflowStepCompletion> step8Opt = stepService.getStep(workflow.getId(), 8);
            if (step8Opt.isPresent()) {
                WorkflowStepCompletion step8 = step8Opt.get();

                // Assign the master user for final approval
                stepService.assignUserToStep(step8, approver, currentUser);

                // Set target role for tracking
                step8.setTargetRole("MASTER");

                // Mark as needing external action (manager approval)
                stepService.markNeedsExternalAction(step8, "MASTER");

                // Send email notification to manager
                boolean emailSent = workflowEmailService.sendStepAssignmentEmail(step8, approver, currentUser, project);

                String successMsg = "Workflow completion request sent to " + approver.getUsername() + " for final approval.";
                if (emailSent) {
                    successMsg += "\n📧 Email notification sent.";
                } else {
                    successMsg += "\n⚠️ Could not send email (SMTP not configured for user).";
                }
                showSuccess(successMsg);

                // Don't close - show waiting state
                refreshWorkflow();
                loadCurrentStep();
            }
        } catch (Exception ex) {
            showError("Failed to request completion approval: " + ex.getMessage());
            ex.printStackTrace();
        }
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
        // Legacy method - redirects to appropriate download based on file type
        if (survey.hasExcelFile()) {
            handleDownloadSiteSurveyExcel(survey);
        } else if (survey.hasZipFile()) {
            handleDownloadSiteSurveyZip(survey);
        } else {
            showError("No file available for download");
        }
    }

    private void handleDownloadSiteSurveyExcel(SiteSurveyData survey) {
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
                showSuccess("Excel file downloaded successfully!\nSaved to: " + file.getAbsolutePath());
            } catch (Exception ex) {
                showError("Failed to download Excel file: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    private void handleDownloadSiteSurveyZip(SiteSurveyData survey) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Site Survey ZIP Archive");
        fileChooser.setInitialFileName(survey.getZipFileName());
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("ZIP Archives", "*.zip")
        );

        File file = fileChooser.showSaveDialog(this);
        if (file != null) {
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(survey.getZipFile());
                showSuccess("ZIP archive downloaded successfully!\nSaved to: " + file.getAbsolutePath());
            } catch (Exception ex) {
                showError("Failed to download ZIP archive: " + ex.getMessage());
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
