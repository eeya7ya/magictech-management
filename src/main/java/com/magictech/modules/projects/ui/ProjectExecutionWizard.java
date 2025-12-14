package com.magictech.modules.projects.ui;

import com.magictech.core.auth.User;
import com.magictech.core.ui.components.RoadmapProgressBar;
import com.magictech.modules.projects.entity.Project;
import com.magictech.modules.projects.entity.ProjectSchedule;
import com.magictech.modules.projects.entity.ProjectTask;
import com.magictech.modules.projects.service.ProjectScheduleService;
import com.magictech.modules.projects.service.ProjectTaskService;
import com.magictech.modules.sales.entity.ProjectWorkflow;
import com.magictech.modules.sales.service.ProjectWorkflowService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Project Execution Wizard for the Projects team
 *
 * Triggered when Sales accepts a tender (Step 5 of Sales workflow)
 * Contains 3 steps:
 * 1. Time Scheduling - Set up project schedule
 * 2. Task Achievements - Track task completion percentage
 * 3. Project Achievements - Final completion confirmation
 *
 * Features:
 * - Modern roadmap-style progress bar
 * - Minimize/maximize functionality
 * - Integration with Sales workflow
 */
public class ProjectExecutionWizard extends Stage {

    /**
     * Callback interface for wizard events
     */
    public interface ProjectWizardCallback {
        /**
         * Called when user needs to navigate to Schedule tab
         */
        void onNavigateToSchedule(Project project);

        /**
         * Called when user needs to navigate to Tasks tab
         */
        void onNavigateToTasks(Project project);

        /**
         * Called when project execution is completed
         */
        void onProjectCompleted(Project project, Long workflowId, boolean success, String explanation);

        /**
         * Called when wizard is restored from minimized state
         */
        default void onWizardRestored() {}
    }

    // Services (injected via constructor)
    private final ProjectWorkflowService workflowService;
    private final ProjectScheduleService scheduleService;
    private final ProjectTaskService taskService;

    // Data
    private final Project project;
    private final Long salesWorkflowId;
    private final User currentUser;
    private ProjectWizardCallback callback;

    // UI Components
    private VBox mainContainer;
    private RoadmapProgressBar progressBar;
    private VBox stepContainer;
    private Button backButton;
    private Button nextButton;

    // Minimized state
    private boolean isMinimized = false;
    private VBox minimizedBar;
    private Scene fullScene;
    private Scene minimizedScene;
    private double savedX, savedY, savedWidth, savedHeight;

    // Step tracking
    private int currentStep = 1;
    private final List<String> stepTitles = Arrays.asList(
        "Time Scheduling",
        "Task Achievements",
        "Project Achievements"
    );

    // Step completion flags
    private boolean step1Completed = false;
    private boolean step2Completed = false;
    private boolean step3Completed = false;

    /**
     * Create a new Project Execution Wizard
     */
    public ProjectExecutionWizard(Project project, Long salesWorkflowId, User currentUser,
                                   ProjectWorkflowService workflowService,
                                   ProjectScheduleService scheduleService,
                                   ProjectTaskService taskService) {
        this.project = project;
        this.salesWorkflowId = salesWorkflowId;
        this.currentUser = currentUser;
        this.workflowService = workflowService;
        this.scheduleService = scheduleService;
        this.taskService = taskService;

        initStyle(StageStyle.DECORATED);
        initModality(Modality.NONE);
        setTitle("Project Execution - " + project.getProjectName());
        setResizable(true);

        buildUI();
        buildMinimizedBar();
        loadCurrentStep();
    }

    /**
     * Set the callback for wizard events
     */
    public void setCallback(ProjectWizardCallback callback) {
        this.callback = callback;
    }

    /**
     * Get the project associated with this wizard
     */
    public Project getProject() {
        return project;
    }

    /**
     * Get the sales workflow ID
     */
    public Long getSalesWorkflowId() {
        return salesWorkflowId;
    }

    private void buildUI() {
        mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(25));
        mainContainer.setStyle("-fx-background-color: linear-gradient(to bottom, #0f172a, #1e293b);");
        mainContainer.setPrefWidth(850);
        mainContainer.setPrefHeight(650);

        // Header
        VBox header = createHeader();

        // Progress bar (roadmap style)
        progressBar = new RoadmapProgressBar(stepTitles);
        progressBar.setCurrentStep(currentStep);

        // Step container
        stepContainer = new VBox(20);
        stepContainer.setStyle(
            "-fx-background-color: rgba(30, 41, 59, 0.8);" +
            "-fx-background-radius: 12;" +
            "-fx-border-radius: 12;" +
            "-fx-border-color: rgba(100, 116, 139, 0.3);" +
            "-fx-border-width: 1;" +
            "-fx-padding: 25;"
        );
        VBox.setVgrow(stepContainer, Priority.ALWAYS);

        // Button bar
        HBox buttonBar = createButtonBar();

        mainContainer.getChildren().addAll(header, progressBar, stepContainer, buttonBar);

        fullScene = new Scene(mainContainer);
        setScene(fullScene);
    }

    private VBox createHeader() {
        VBox header = new VBox(8);
        header.setAlignment(Pos.CENTER);

        Label iconLabel = new Label("🚀");
        iconLabel.setFont(Font.font(36));

        Label titleLabel = new Label("Project Execution Wizard");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
        titleLabel.setTextFill(Color.WHITE);

        Label projectLabel = new Label("Project: " + project.getProjectName());
        projectLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        projectLabel.setTextFill(Color.web("#94a3b8"));

        header.getChildren().addAll(iconLabel, titleLabel, projectLabel);
        return header;
    }

    private HBox createButtonBar() {
        HBox buttonBar = new HBox(15);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(15, 0, 0, 0));

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

        Button closeButton = new Button("Close");
        closeButton.setStyle(
            "-fx-background-color: #ef4444;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 12 25;" +
            "-fx-background-radius: 8;" +
            "-fx-cursor: hand;"
        );
        closeButton.setOnAction(e -> close());

        buttonBar.getChildren().addAll(backButton, nextButton, minimizeButton, closeButton);
        return buttonBar;
    }

    private void buildMinimizedBar() {
        minimizedBar = new VBox();
        minimizedBar.setStyle(
            "-fx-background-color: linear-gradient(to right, #1e3a5f, #3b82f6);" +
            "-fx-padding: 12 25;" +
            "-fx-background-radius: 10 10 0 0;"
        );
        minimizedBar.setAlignment(Pos.CENTER);

        HBox content = new HBox(15);
        content.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label("🚀");
        iconLabel.setFont(Font.font(18));

        Label titleLabel = new Label("Project Execution: " + project.getProjectName() + " (Step " + currentStep + "/3)");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        titleLabel.setId("minimizedTitle");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button restoreButton = new Button("▢ Restore");
        restoreButton.setStyle(
            "-fx-background-color: #22c55e;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 12px;" +
            "-fx-padding: 6 15;" +
            "-fx-background-radius: 5;"
        );
        restoreButton.setOnAction(e -> restoreFromBar());

        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
            "-fx-background-color: #ef4444;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 12px;" +
            "-fx-padding: 6 12;" +
            "-fx-background-radius: 5;"
        );
        closeBtn.setOnAction(e -> close());

        content.getChildren().addAll(iconLabel, titleLabel, spacer, restoreButton, closeBtn);
        minimizedBar.getChildren().add(content);

        minimizedScene = new Scene(minimizedBar, 550, 55);
    }

    /**
     * Minimize the wizard to a bar at the bottom of the screen
     */
    public void minimizeToBar() {
        if (isMinimized) return;

        System.out.println("📉 Minimizing project execution wizard");

        savedX = getX();
        savedY = getY();
        savedWidth = getWidth();
        savedHeight = getHeight();

        // Update title
        Label titleLabel = (Label) minimizedBar.lookup("#minimizedTitle");
        if (titleLabel != null) {
            titleLabel.setText("Project Execution: " + project.getProjectName() + " (Step " + currentStep + "/3)");
        }

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

        fullScene = getScene();
        setScene(minimizedScene);

        setWidth(550);
        setHeight(55);
        setX((screenBounds.getWidth() - 550) / 2);
        setY(screenBounds.getHeight() - 65);

        isMinimized = true;
        setAlwaysOnTop(true);
    }

    /**
     * Restore the wizard from minimized state
     */
    public void restoreFromBar() {
        if (!isMinimized) return;

        System.out.println("📈 Restoring project execution wizard");

        setScene(fullScene);

        setWidth(savedWidth > 0 ? savedWidth : 850);
        setHeight(savedHeight > 0 ? savedHeight : 650);
        setX(savedX > 0 ? savedX : 100);
        setY(savedY > 0 ? savedY : 100);

        isMinimized = false;
        setAlwaysOnTop(false);

        refreshData();
        loadCurrentStep();

        if (callback != null) {
            callback.onWizardRestored();
        }
    }

    /**
     * Check if wizard is minimized
     */
    public boolean isDialogMinimized() {
        return isMinimized;
    }

    private void loadCurrentStep() {
        stepContainer.getChildren().clear();

        // Update progress bar
        progressBar.setCurrentStep(currentStep);
        progressBar.setStepCompleted(1, step1Completed);
        progressBar.setStepCompleted(2, step2Completed);
        progressBar.setStepCompleted(3, step3Completed);

        // Step title
        Label stepTitle = new Label("Step " + currentStep + ": " + stepTitles.get(currentStep - 1));
        stepTitle.setFont(Font.font("System", FontWeight.BOLD, 22));
        stepTitle.setTextFill(Color.WHITE);

        stepContainer.getChildren().add(stepTitle);

        // Load step-specific content
        switch (currentStep) {
            case 1 -> loadStep1_TimeScheduling();
            case 2 -> loadStep2_TaskAchievements();
            case 3 -> loadStep3_ProjectAchievements();
        }

        // Update button states
        backButton.setDisable(currentStep == 1);
        updateNextButtonText();
    }

    private void updateNextButtonText() {
        if (currentStep == 3) {
            nextButton.setText("Complete Project ✓");
            nextButton.setStyle(
                "-fx-background-color: linear-gradient(to right, #22c55e, #16a34a);" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 12 25;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;"
            );
        } else {
            nextButton.setText("Next →");
            nextButton.setStyle(
                "-fx-background-color: linear-gradient(to right, #3b82f6, #2563eb);" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 12 25;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;"
            );
        }
    }

    // ==================== STEP 1: TIME SCHEDULING ====================

    private void loadStep1_TimeScheduling() {
        VBox contentBox = new VBox(20);
        contentBox.setAlignment(Pos.TOP_LEFT);

        Label descLabel = new Label(
            "Set up the project timeline and schedule.\n" +
            "Click 'Go to Schedule Tab' to create and manage schedule entries."
        );
        descLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        descLabel.setTextFill(Color.web("#94a3b8"));
        descLabel.setWrapText(true);

        // Show current schedule status
        VBox statusBox = createScheduleStatusBox();

        // Button to navigate to schedule tab
        Button goToScheduleBtn = new Button("📅 Go to Schedule Tab");
        goToScheduleBtn.setStyle(
            "-fx-background-color: linear-gradient(to right, #8b5cf6, #7c3aed);" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 16px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 15 30;" +
            "-fx-background-radius: 10;" +
            "-fx-cursor: hand;"
        );
        goToScheduleBtn.setOnAction(e -> {
            if (callback != null) {
                minimizeToBar();
                callback.onNavigateToSchedule(project);
            }
        });

        // Mark as complete button
        Button markCompleteBtn = new Button("✓ Mark Scheduling Complete");
        markCompleteBtn.setStyle(
            "-fx-background-color: linear-gradient(to right, #22c55e, #16a34a);" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 12 25;" +
            "-fx-background-radius: 8;" +
            "-fx-cursor: hand;"
        );
        markCompleteBtn.setOnAction(e -> {
            step1Completed = true;
            progressBar.setStepCompleted(1, true);
            showInfo("Step 1 marked as complete!");
            // Auto-advance
            currentStep = 2;
            loadCurrentStep();
        });

        if (step1Completed) {
            markCompleteBtn.setDisable(true);
            markCompleteBtn.setText("✓ Completed");
        }

        HBox buttonRow = new HBox(15, goToScheduleBtn, markCompleteBtn);
        buttonRow.setAlignment(Pos.CENTER_LEFT);

        contentBox.getChildren().addAll(descLabel, statusBox, buttonRow);
        stepContainer.getChildren().add(contentBox);
    }

    private VBox createScheduleStatusBox() {
        VBox statusBox = new VBox(10);
        statusBox.setStyle(
            "-fx-background-color: rgba(59, 130, 246, 0.15);" +
            "-fx-border-color: rgba(59, 130, 246, 0.4);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 15;"
        );

        Label statusTitle = new Label("📊 Current Schedule Status");
        statusTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        statusTitle.setTextFill(Color.web("#60a5fa"));

        // Get schedule data
        List<ProjectSchedule> schedules = scheduleService.findByProjectId(project.getId());

        Label countLabel = new Label("Schedule Entries: " + schedules.size());
        countLabel.setFont(Font.font("System", FontWeight.NORMAL, 13));
        countLabel.setTextFill(Color.WHITE);

        statusBox.getChildren().addAll(statusTitle, countLabel);

        if (!schedules.isEmpty()) {
            for (ProjectSchedule schedule : schedules.subList(0, Math.min(3, schedules.size()))) {
                Label entryLabel = new Label("• " + schedule.getTaskName() +
                    " (" + formatDate(schedule.getStartDate()) + " - " + formatDate(schedule.getEndDate()) + ")");
                entryLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
                entryLabel.setTextFill(Color.web("#94a3b8"));
                statusBox.getChildren().add(entryLabel);
            }

            if (schedules.size() > 3) {
                Label moreLabel = new Label("... and " + (schedules.size() - 3) + " more");
                moreLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
                moreLabel.setTextFill(Color.web("#64748b"));
                statusBox.getChildren().add(moreLabel);
            }
        } else {
            Label noDataLabel = new Label("No schedule entries yet. Click 'Go to Schedule Tab' to add.");
            noDataLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
            noDataLabel.setTextFill(Color.web("#f59e0b"));
            statusBox.getChildren().add(noDataLabel);
        }

        return statusBox;
    }

    // ==================== STEP 2: TASK ACHIEVEMENTS ====================

    private void loadStep2_TaskAchievements() {
        VBox contentBox = new VBox(20);
        contentBox.setAlignment(Pos.TOP_LEFT);

        Label descLabel = new Label(
            "Track the completion percentage of project tasks.\n" +
            "Mark tasks as complete in the Tasks tab to increase overall progress."
        );
        descLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        descLabel.setTextFill(Color.web("#94a3b8"));
        descLabel.setWrapText(true);

        // Show task completion status
        VBox statusBox = createTaskStatusBox();

        // Button to navigate to tasks tab
        Button goToTasksBtn = new Button("📋 Go to Tasks Tab");
        goToTasksBtn.setStyle(
            "-fx-background-color: linear-gradient(to right, #f59e0b, #d97706);" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 16px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 15 30;" +
            "-fx-background-radius: 10;" +
            "-fx-cursor: hand;"
        );
        goToTasksBtn.setOnAction(e -> {
            if (callback != null) {
                minimizeToBar();
                callback.onNavigateToTasks(project);
            }
        });

        // Mark as complete button
        Button markCompleteBtn = new Button("✓ Mark Task Review Complete");
        markCompleteBtn.setStyle(
            "-fx-background-color: linear-gradient(to right, #22c55e, #16a34a);" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 12 25;" +
            "-fx-background-radius: 8;" +
            "-fx-cursor: hand;"
        );
        markCompleteBtn.setOnAction(e -> {
            step2Completed = true;
            progressBar.setStepCompleted(2, true);
            showInfo("Step 2 marked as complete!");
            // Auto-advance
            currentStep = 3;
            loadCurrentStep();
        });

        if (step2Completed) {
            markCompleteBtn.setDisable(true);
            markCompleteBtn.setText("✓ Completed");
        }

        HBox buttonRow = new HBox(15, goToTasksBtn, markCompleteBtn);
        buttonRow.setAlignment(Pos.CENTER_LEFT);

        contentBox.getChildren().addAll(descLabel, statusBox, buttonRow);
        stepContainer.getChildren().add(contentBox);
    }

    private VBox createTaskStatusBox() {
        VBox statusBox = new VBox(12);
        statusBox.setStyle(
            "-fx-background-color: rgba(245, 158, 11, 0.15);" +
            "-fx-border-color: rgba(245, 158, 11, 0.4);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 15;"
        );

        Label statusTitle = new Label("📊 Task Completion Status");
        statusTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        statusTitle.setTextFill(Color.web("#fbbf24"));

        // Get task data
        List<ProjectTask> tasks = taskService.findByProjectId(project.getId());
        long completedTasks = tasks.stream().filter(t -> Boolean.TRUE.equals(t.getIsCompleted())).count();
        double percentage = tasks.isEmpty() ? 0 : (completedTasks * 100.0 / tasks.size());

        Label countLabel = new Label(String.format("Tasks: %d / %d completed (%.1f%%)",
            completedTasks, tasks.size(), percentage));
        countLabel.setFont(Font.font("System", FontWeight.NORMAL, 13));
        countLabel.setTextFill(Color.WHITE);

        // Progress bar
        ProgressBar progressIndicator = new ProgressBar(percentage / 100.0);
        progressIndicator.setPrefWidth(300);
        progressIndicator.setStyle(
            "-fx-accent: #22c55e;" +
            "-fx-control-inner-background: rgba(100, 116, 139, 0.3);"
        );

        statusBox.getChildren().addAll(statusTitle, countLabel, progressIndicator);

        // Show incomplete tasks
        List<ProjectTask> incompleteTasks = tasks.stream()
            .filter(t -> !Boolean.TRUE.equals(t.getIsCompleted()))
            .limit(3)
            .toList();

        if (!incompleteTasks.isEmpty()) {
            Label pendingLabel = new Label("Pending Tasks:");
            pendingLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
            pendingLabel.setTextFill(Color.web("#f59e0b"));
            statusBox.getChildren().add(pendingLabel);

            for (ProjectTask task : incompleteTasks) {
                Label taskLabel = new Label("• " + task.getTaskTitle());
                taskLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
                taskLabel.setTextFill(Color.web("#94a3b8"));
                statusBox.getChildren().add(taskLabel);
            }
        }

        return statusBox;
    }

    // ==================== STEP 3: PROJECT ACHIEVEMENTS ====================

    private void loadStep3_ProjectAchievements() {
        VBox contentBox = new VBox(20);
        contentBox.setAlignment(Pos.TOP_LEFT);

        Label descLabel = new Label(
            "Final confirmation: Is the project successfully completed?\n" +
            "This will notify the Sales team to proceed with the next workflow step."
        );
        descLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        descLabel.setTextFill(Color.web("#94a3b8"));
        descLabel.setWrapText(true);

        // Summary box
        VBox summaryBox = createProjectSummaryBox();

        // Question
        Label questionLabel = new Label("Is the project successfully completed?");
        questionLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        questionLabel.setTextFill(Color.WHITE);

        // Yes button
        Button yesButton = new Button("✓ Yes - Project Completed Successfully");
        yesButton.setStyle(
            "-fx-background-color: linear-gradient(to right, #22c55e, #16a34a);" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 16px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 15 30;" +
            "-fx-background-radius: 10;" +
            "-fx-cursor: hand;"
        );
        yesButton.setOnAction(e -> handleProjectCompleted(true, null));

        // No button
        Button noButton = new Button("✗ No - Write Explanation");
        noButton.setStyle(
            "-fx-background-color: linear-gradient(to right, #ef4444, #dc2626);" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 16px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 15 30;" +
            "-fx-background-radius: 10;" +
            "-fx-cursor: hand;"
        );
        noButton.setOnAction(e -> showExplanationDialog());

        HBox buttonRow = new HBox(20, yesButton, noButton);
        buttonRow.setAlignment(Pos.CENTER);
        buttonRow.setPadding(new Insets(20, 0, 0, 0));

        contentBox.getChildren().addAll(descLabel, summaryBox, questionLabel, buttonRow);
        stepContainer.getChildren().add(contentBox);
    }

    private VBox createProjectSummaryBox() {
        VBox summaryBox = new VBox(12);
        summaryBox.setStyle(
            "-fx-background-color: rgba(34, 197, 94, 0.15);" +
            "-fx-border-color: rgba(34, 197, 94, 0.4);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 15;"
        );

        Label summaryTitle = new Label("📊 Project Summary");
        summaryTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        summaryTitle.setTextFill(Color.web("#4ade80"));

        // Get data
        List<ProjectSchedule> schedules = scheduleService.findByProjectId(project.getId());
        List<ProjectTask> tasks = taskService.findByProjectId(project.getId());
        long completedTasks = tasks.stream().filter(t -> Boolean.TRUE.equals(t.getIsCompleted())).count();
        double taskPercentage = tasks.isEmpty() ? 0 : (completedTasks * 100.0 / tasks.size());

        Label scheduleLabel = new Label("📅 Schedule Entries: " + schedules.size());
        scheduleLabel.setFont(Font.font("System", FontWeight.NORMAL, 13));
        scheduleLabel.setTextFill(Color.WHITE);

        Label taskLabel = new Label(String.format("📋 Tasks Completed: %d / %d (%.1f%%)",
            completedTasks, tasks.size(), taskPercentage));
        taskLabel.setFont(Font.font("System", FontWeight.NORMAL, 13));
        taskLabel.setTextFill(Color.WHITE);

        Label step1Label = new Label("Step 1 (Scheduling): " + (step1Completed ? "✓ Complete" : "○ Pending"));
        step1Label.setFont(Font.font("System", FontWeight.NORMAL, 13));
        step1Label.setTextFill(step1Completed ? Color.web("#22c55e") : Color.web("#94a3b8"));

        Label step2Label = new Label("Step 2 (Tasks): " + (step2Completed ? "✓ Complete" : "○ Pending"));
        step2Label.setFont(Font.font("System", FontWeight.NORMAL, 13));
        step2Label.setTextFill(step2Completed ? Color.web("#22c55e") : Color.web("#94a3b8"));

        summaryBox.getChildren().addAll(summaryTitle, scheduleLabel, taskLabel, step1Label, step2Label);
        return summaryBox;
    }

    private void showExplanationDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Project Not Completed");
        dialog.setHeaderText("Please explain why the project is not completed");

        TextArea explanationArea = new TextArea();
        explanationArea.setPromptText("Enter your explanation here...");
        explanationArea.setPrefRowCount(5);
        explanationArea.setWrapText(true);

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.getChildren().addAll(
            new Label("Explanation (required):"),
            explanationArea
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Disable OK button if no explanation
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(true);
        explanationArea.textProperty().addListener((obs, oldVal, newVal) -> {
            okButton.setDisable(newVal.trim().isEmpty());
        });

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return explanationArea.getText().trim();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(explanation -> handleProjectCompleted(false, explanation));
    }

    private void handleProjectCompleted(boolean success, String explanation) {
        step3Completed = true;
        progressBar.setStepCompleted(3, true);

        System.out.println("🏁 Project execution " + (success ? "completed successfully" : "not completed"));
        if (explanation != null) {
            System.out.println("   Explanation: " + explanation);
        }

        // Notify callback (this will notify Sales workflow to advance)
        if (callback != null) {
            callback.onProjectCompleted(project, salesWorkflowId, success, explanation);
        }

        // Show success message
        if (success) {
            showSuccess("Project completed successfully!\nSales team will be notified to proceed.");
        } else {
            showWarning("Project status recorded with explanation.\nSales team will be notified.");
        }

        // Close wizard
        cleanup();
        close();
    }

    private void handleBack() {
        if (currentStep > 1) {
            currentStep--;
            loadCurrentStep();
        }
    }

    private void handleNext() {
        if (currentStep < 3) {
            currentStep++;
            loadCurrentStep();
        } else {
            // On step 3, next triggers the completion flow
            // But we have Yes/No buttons for that, so this shouldn't happen
        }
    }

    private void refreshData() {
        // Refresh schedule and task data
        // Called when restoring from minimized state
    }

    /**
     * Mark Step 1 (scheduling) as complete externally
     */
    public void markStep1Complete() {
        step1Completed = true;
        if (!isMinimized) {
            progressBar.setStepCompleted(1, true);
            loadCurrentStep();
        }
    }

    /**
     * Mark Step 2 (tasks) as complete externally
     */
    public void markStep2Complete() {
        step2Completed = true;
        if (!isMinimized) {
            progressBar.setStepCompleted(2, true);
            loadCurrentStep();
        }
    }

    /**
     * Cleanup resources
     */
    public void cleanup() {
        if (progressBar != null) {
            progressBar.cleanup();
        }
    }

    private String formatDate(java.time.LocalDate date) {
        if (date == null) return "-";
        return date.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd"));
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Notice");
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
}
