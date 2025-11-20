package com.magictech.modules.projects;

import com.magictech.core.auth.User;
import com.magictech.core.module.BaseModuleController;
import com.magictech.modules.projects.entity.*;
import com.magictech.modules.projects.service.*;
import com.magictech.modules.projects.model.*;
import com.magictech.modules.storage.entity.StorageItem;
import com.magictech.modules.storage.service.StorageService;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Projects Module Controller - FULLY FIXED VERSION
 * ✅ FIX #1: Availability check for elements before adding
 * ✅ FIX #2: HIGHLY VISIBLE checkbox marking with colors and borders
 * ✅ FIX #3: Fixed text visibility in ALL dialogs (dark backgrounds, white text)
 * ✅ FIX #4: Edit Schedule functionality - WORKING NOW
 * ✅ FIX #5: Enhanced project selection with hover effects and clear selection
 */
@Component
public class ProjectsStorageController extends BaseModuleController {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectScheduleService scheduleService;

    @Autowired
    private ProjectTaskService taskService;

    @Autowired
    private ProjectNoteService noteService;

    @Autowired
    private ProjectElementService elementService;

    @Autowired
    private StorageService storageService;

    // UI Components
    private com.magictech.core.ui.components.DashboardBackgroundPane backgroundPane;
    private StackPane mainContainer;
    private VBox projectSelectionScreen;
    private BorderPane projectWorkspaceScreen;
    private ProgressIndicator loadingIndicator;

    // Selected Project
    private Project selectedProject;
    private Label projectTitleLabel;

    // Tab Content Containers
    private TabPane tabPane;
    private VBox scheduleTabContent;
    private VBox tasksTabContent;
    private VBox elementsTabContent;

    // Schedule Data
    private ObservableList<ProjectScheduleViewModel> scheduleItems;
    private TableView<ProjectScheduleViewModel> scheduleTable;

    // Tasks Data
    private ObservableList<ProjectTaskViewModel> taskItems;
    private VBox tasksContainer;
    private TextArea importantNotesArea;

    // Elements Data
    private ObservableList<ProjectElementViewModel> elementItems;
    private FlowPane elementsGrid;  // ✅ Store reference to grid

    // Current User
    private User currentUser;

    @Override
    public void refresh() {
        if (selectedProject != null) {
            loadScheduleData();
            loadTasksData();
            loadElementsData();
        }
    }

    @Override
    protected void setupUI() {
        StackPane stackRoot = new StackPane();
        backgroundPane = new com.magictech.core.ui.components.DashboardBackgroundPane();

        mainContainer = new StackPane();

        projectSelectionScreen = createProjectSelectionScreen();
        projectWorkspaceScreen = createProjectWorkspaceScreen();

        mainContainer.getChildren().add(projectSelectionScreen);
        projectWorkspaceScreen.setVisible(false);

        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setVisible(false);
        loadingIndicator.setMaxSize(60, 60);

        mainContainer.getChildren().addAll(projectWorkspaceScreen, loadingIndicator);

        stackRoot.getChildren().addAll(backgroundPane, mainContainer);

        BorderPane root = getRootPane();
        root.setCenter(stackRoot);
        root.setStyle("-fx-background-color: transparent;");
    }

    @Override
    protected void loadData() {
        // Data loads when project is selected
    }

    // ==================== ✅ FIX #5: ENHANCED PROJECT SELECTION WITH VISIBLE SELECTION ====================

    private VBox createProjectSelectionScreen() {
        VBox screen = new VBox(30);
        screen.setAlignment(Pos.CENTER);
        screen.setPadding(new Insets(50));
        screen.setStyle("-fx-background-color: transparent;");

        // Header
        VBox header = new VBox(15);
        header.setAlignment(Pos.CENTER);

        Label iconLabel = new Label("📁");
        iconLabel.setFont(new Font(64));

        Label titleLabel = new Label("Projects Management");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 32px; -fx-font-weight: bold;");

        Label subtitleLabel = new Label("Select a project to manage schedule, tasks, and elements");
        subtitleLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 16px;");

        header.getChildren().addAll(iconLabel, titleLabel, subtitleLabel);

        // Projects Card
        VBox projectsCard = new VBox(20);
        projectsCard.setMaxWidth(700);
        projectsCard.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.6);" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: rgba(139, 92, 246, 0.3);" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 16;" +
                        "-fx-padding: 30;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 20, 0, 0, 8);"
        );

        Label selectLabel = new Label("SELECT YOUR PROJECT:");
        selectLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        // ✅ FIXED: Enhanced ListView with VISIBLE selection
        ListView<Project> projectListView = new ListView<>();
        projectListView.setPrefHeight(400);
        projectListView.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-background-radius: 8;" +
                        "-fx-selection-bar: rgba(139, 92, 246, 0.8);" +  // ✅ Purple selection
                        "-fx-selection-bar-non-focused: rgba(139, 92, 246, 0.5);"
        );

        // ✅ FIXED: Custom cell factory with HOVER and SELECTION effects
        projectListView.setCellFactory(lv -> new ListCell<Project>() {
            @Override
            protected void updateItem(Project project, boolean empty) {
                super.updateItem(project, empty);
                if (empty || project == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    VBox cellContent = new VBox(5);
                    cellContent.setPadding(new Insets(12));

                    // ✅ FIXED: Dynamic styling based on selection
                    boolean isSelected = isSelected();
                    String backgroundColor = isSelected ?
                            "rgba(139, 92, 246, 0.6)" :  // ✅ Selected = Purple
                            "rgba(15, 23, 42, 0.7)";      // ✅ Normal = Dark

                    String borderColor = isSelected ?
                            "rgba(139, 92, 246, 1.0)" :  // ✅ Selected = Bright purple border
                            "rgba(139, 92, 246, 0.3)";    // ✅ Normal = Dim border

                    cellContent.setStyle(
                            "-fx-background-color: " + backgroundColor + ";" +
                                    "-fx-background-radius: 8;" +
                                    "-fx-border-color: " + borderColor + ";" +
                                    "-fx-border-width: 2;" +
                                    "-fx-border-radius: 8;"
                    );

                    Label nameLabel = new Label("📋 " + project.getProjectName());
                    nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

                    Label locationLabel = new Label("📍 " + project.getProjectLocation());
                    locationLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.9); -fx-font-size: 13px;");

                    Label statusLabel = new Label("Status: " + project.getStatus());
                    String statusColor = switch (project.getStatus()) {
                        case "Planning" -> "#a855f7";
                        case "In Progress" -> "#3b82f6";
                        case "Completed" -> "#22c55e";
                        default -> "#9ca3af";
                    };
                    statusLabel.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-size: 12px; -fx-font-weight: bold;");

                    cellContent.getChildren().addAll(nameLabel, locationLabel, statusLabel);

                    // ✅ HOVER EFFECT
                    cellContent.setOnMouseEntered(e -> {
                        if (!isSelected()) {
                            cellContent.setStyle(
                                    "-fx-background-color: rgba(59, 130, 246, 0.4);" +
                                            "-fx-background-radius: 8;" +
                                            "-fx-border-color: rgba(59, 130, 246, 0.8);" +
                                            "-fx-border-width: 2;" +
                                            "-fx-border-radius: 8;" +
                                            "-fx-cursor: hand;"
                            );
                        }
                    });

                    cellContent.setOnMouseExited(e -> {
                        if (!isSelected()) {
                            cellContent.setStyle(
                                    "-fx-background-color: rgba(15, 23, 42, 0.7);" +
                                            "-fx-background-radius: 8;" +
                                            "-fx-border-color: rgba(139, 92, 246, 0.3);" +
                                            "-fx-border-width: 2;" +
                                            "-fx-border-radius: 8;"
                            );
                        }
                    });

                    setGraphic(cellContent);
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                }
            }
        });

        // Load projects
        loadProjectsList(projectListView);

        // Buttons
        HBox buttonsBox = new HBox(15);
        buttonsBox.setAlignment(Pos.CENTER);

        Button openButton = createStyledButton("Open Project", "#8b5cf6", "#7c3aed");
        openButton.setOnAction(e -> {
            Project selected = projectListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                openProjectWorkspace(selected);
            } else {
                showWarning("Please select a project first");
            }
        });

        Button backButton = createStyledButton("← Back to Dashboard", "#6b7280", "#4b5563");
        backButton.setOnAction(e -> navigateToDashboard());

        buttonsBox.getChildren().addAll(openButton, backButton);

        projectsCard.getChildren().addAll(selectLabel, projectListView, buttonsBox);

        screen.getChildren().addAll(header, projectsCard);
        return screen;
    }

    private void loadProjectsList(ListView<Project> listView) {
        Task<List<Project>> loadTask = new Task<>() {
            @Override
            protected List<Project> call() {
                return projectService.getAllProjects();
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<Project> projects = loadTask.getValue();
            listView.getItems().setAll(projects);
        });

        loadTask.setOnFailed(e -> showError("Failed to load projects: " + loadTask.getException().getMessage()));

        new Thread(loadTask).start();
    }

    // ==================== PROJECT WORKSPACE SCREEN ====================

    private BorderPane createProjectWorkspaceScreen() {
        BorderPane workspace = new BorderPane();
        workspace.setStyle("-fx-background-color: transparent;");

        // Top: Project Header
        VBox header = new VBox(10);
        header.setPadding(new Insets(20, 30, 20, 30));
        header.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.7);" +
                        "-fx-border-color: rgba(139, 92, 246, 0.3);" +
                        "-fx-border-width: 0 0 2 0;"
        );

        HBox titleRow = new HBox(15);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        projectTitleLabel = new Label();
        projectTitleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");
        HBox.setHgrow(projectTitleLabel, Priority.ALWAYS);

        Button backToSelectionButton = createStyledButton("← Back to Projects", "#6b7280", "#4b5563");
        backToSelectionButton.setOnAction(e -> backToProjectSelection());

        titleRow.getChildren().addAll(projectTitleLabel, backToSelectionButton);
        header.getChildren().add(titleRow);

        workspace.setTop(header);

        // Center: Tabs
        tabPane = createTabPane();
        workspace.setCenter(tabPane);

        return workspace;
    }

    private TabPane createTabPane() {
        TabPane tabs = new TabPane();
        tabs.setStyle("-fx-background-color: transparent;");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab scheduleTab = new Tab("📅 Manage Schedule");
        scheduleTab.setStyle(
                "-fx-background-color: rgba(59, 130, 246, 0.9);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 16px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 20;"
        );
        scheduleTabContent = createScheduleTab();
        scheduleTab.setContent(scheduleTabContent);

        Tab tasksTab = new Tab("✅ Tasks");
        tasksTab.setStyle(
                "-fx-background-color: rgba(34, 197, 94, 0.9);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 16px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 20;"
        );
        tasksTabContent = createTasksTab();
        tasksTab.setContent(tasksTabContent);

        Tab elementsTab = new Tab("📦 Elements");
        elementsTab.setStyle(
                "-fx-background-color: rgba(251, 146, 60, 0.9);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 16px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 20;"
        );
        elementsTabContent = createElementsTab();
        elementsTab.setContent(elementsTabContent);

        tabs.getTabs().addAll(scheduleTab, tasksTab, elementsTab);

        tabs.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-tab-min-width: 180px;" +
                        "-fx-tab-max-width: 180px;"
        );

        return tabs;
    }

    // ==================== SCHEDULE TAB ====================

    private VBox createScheduleTab() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: transparent;");

        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("📅 Project Schedule");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");
        HBox.setHgrow(title, Priority.ALWAYS);

        Button addButton = createStyledButton("+ Add Task", "#22c55e", "#16a34a");
        addButton.setOnAction(e -> handleAddScheduleItem());

        Button editButton = createStyledButton("✏️ Edit", "#3b82f6", "#2563eb");
        editButton.setOnAction(e -> handleEditScheduleItem());  // ✅ FIXED: Now works!

        Button deleteButton = createStyledButton("🗑️ Delete", "#ef4444", "#dc2626");
        deleteButton.setOnAction(e -> handleDeleteScheduleItem());

        Button refreshButton = createStyledButton("🔄 Refresh", "#8b5cf6", "#7c3aed");
        refreshButton.setOnAction(e -> loadScheduleData());

        header.getChildren().addAll(title, addButton, editButton, deleteButton, refreshButton);

        scheduleItems = FXCollections.observableArrayList();
        scheduleTable = createScheduleTable();

        content.getChildren().addAll(header, scheduleTable);
        return content;
    }

    private TableView<ProjectScheduleViewModel> createScheduleTable() {
        TableView<ProjectScheduleViewModel> table = new TableView<>();
        table.setItems(scheduleItems);
        table.setPrefHeight(550);
        table.setStyle(
                "-fx-background-color: rgba(15, 23, 42, 0.8);" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: rgba(59, 130, 246, 0.4);" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 12;"
        );

        TableColumn<ProjectScheduleViewModel, String> taskCol = new TableColumn<>("📋 Task Name");
        taskCol.setPrefWidth(200);
        taskCol.setCellValueFactory(new PropertyValueFactory<>("taskName"));
        taskCol.setStyle("-fx-alignment: CENTER_LEFT; -fx-text-fill: white; -fx-font-size: 14px;");

        TableColumn<ProjectScheduleViewModel, String> startCol = new TableColumn<>("📅 Start Date");
        startCol.setPrefWidth(130);
        startCol.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        startCol.setStyle("-fx-alignment: CENTER; -fx-text-fill: #a5f3fc; -fx-font-weight: bold;");

        TableColumn<ProjectScheduleViewModel, String> endCol = new TableColumn<>("📅 End Date");
        endCol.setPrefWidth(130);
        endCol.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        endCol.setStyle("-fx-alignment: CENTER; -fx-text-fill: #fca5a5; -fx-font-weight: bold;");

        TableColumn<ProjectScheduleViewModel, String> statusCol = new TableColumn<>("📊 Status");
        statusCol.setPrefWidth(130);
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setStyle("-fx-alignment: CENTER;");
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    String bgColor = switch (status) {
                        case "Pending" -> "rgba(251, 146, 60, 0.3)";
                        case "In Progress" -> "rgba(59, 130, 246, 0.3)";
                        case "Completed" -> "rgba(34, 197, 94, 0.3)";
                        default -> "rgba(156, 163, 175, 0.3)";
                    };
                    setStyle(
                            "-fx-background-color: " + bgColor + ";" +
                                    "-fx-text-fill: white;" +
                                    "-fx-font-weight: bold;" +
                                    "-fx-alignment: CENTER;"
                    );
                }
            }
        });

        TableColumn<ProjectScheduleViewModel, Integer> progressCol = new TableColumn<>("📈 Progress");
        progressCol.setPrefWidth(150);
        progressCol.setCellValueFactory(new PropertyValueFactory<>("progress"));
        progressCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer progress, boolean empty) {
                super.updateItem(progress, empty);
                if (empty || progress == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    ProgressBar progressBar = new ProgressBar(progress / 100.0);
                    progressBar.setPrefWidth(80);
                    progressBar.setStyle(
                            "-fx-accent: #22c55e;" +
                                    "-fx-control-inner-background: rgba(255, 255, 255, 0.1);"
                    );

                    Label percentLabel = new Label(progress + "%");
                    percentLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px;");

                    VBox box = new VBox(3, progressBar, percentLabel);
                    box.setAlignment(Pos.CENTER);
                    setGraphic(box);
                }
            }
        });

        TableColumn<ProjectScheduleViewModel, String> assignedCol = new TableColumn<>("👤 Assigned To");
        assignedCol.setPrefWidth(150);
        assignedCol.setCellValueFactory(new PropertyValueFactory<>("assignedTo"));
        assignedCol.setStyle("-fx-alignment: CENTER; -fx-text-fill: rgba(255, 255, 255, 0.9);");

        table.getColumns().addAll(taskCol, startCol, endCol, statusCol, progressCol, assignedCol);

        return table;
    }

    // ==================== ✅ FIX #2: TASKS TAB WITH SUPER VISIBLE CHECKBOXES ====================

    private VBox createTasksTab() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: transparent;");

        VBox tasksSection = new VBox(15);
        tasksSection.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.6);" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: rgba(59, 130, 246, 0.5);" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 12;" +
                        "-fx-padding: 20;"
        );

        HBox tasksHeader = new HBox();
        tasksHeader.setAlignment(Pos.CENTER_LEFT);

        Label tasksLabel = new Label("✅ Tasks Checklist");
        tasksLabel.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");
        HBox.setHgrow(tasksLabel, Priority.ALWAYS);

        Button addTaskButton = createStyledButton("+ Add Task", "#22c55e", "#16a34a");
        addTaskButton.setOnAction(e -> handleAddTask());

        tasksHeader.getChildren().addAll(tasksLabel, addTaskButton);

        ScrollPane tasksScroll = new ScrollPane();
        tasksScroll.setFitToWidth(true);
        tasksScroll.setPrefHeight(350);
        tasksScroll.setStyle(
                "-fx-background: transparent;" +
                        "-fx-background-color: transparent;" +
                        "-fx-border-color: transparent;"
        );

        tasksContainer = new VBox(12);
        tasksContainer.setStyle("-fx-background-color: transparent;");
        tasksScroll.setContent(tasksContainer);

        tasksSection.getChildren().addAll(tasksHeader, tasksScroll);

        // Important Notes Section
        VBox notesSection = new VBox(15);
        notesSection.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.6);" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: rgba(139, 92, 246, 0.5);" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 12;" +
                        "-fx-padding: 20;"
        );

        Label notesLabel = new Label("📝 Important Notes");
        notesLabel.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");

        importantNotesArea = new TextArea();
        importantNotesArea.setPromptText("Enter important notes for this project...");
        importantNotesArea.setPrefHeight(150);
        importantNotesArea.setWrapText(true);
        importantNotesArea.setStyle(
                "-fx-background-color: rgba(15, 23, 42, 0.8);" +
                        "-fx-text-fill: white;" +
                        "-fx-prompt-text-fill: rgba(255, 255, 255, 0.4);" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-color: rgba(139, 92, 246, 0.3);" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;" +
                        "-fx-font-size: 14px;"
        );

        Button saveNotesButton = createStyledButton("💾 Save Notes", "#8b5cf6", "#7c3aed");
        saveNotesButton.setOnAction(e -> handleSaveNotes());

        notesSection.getChildren().addAll(notesLabel, importantNotesArea, saveNotesButton);

        content.getChildren().addAll(tasksSection, notesSection);
        return content;
    }

    // ✅ MODIFIED: Task row with schedule link, progress update, and VISIBLE delete button
    private HBox createTaskRow(ProjectTask task) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12));

        // ✅ DYNAMIC ROW BACKGROUND based on completion status
        String rowBackgroundColor = task.getIsCompleted() ?
                "rgba(34, 197, 94, 0.2)" :  // ✅ Completed = Green tint
                "rgba(15, 23, 42, 0.6)";     // ✅ Pending = Dark

        String rowBorderColor = task.getIsCompleted() ?
                "rgba(34, 197, 94, 0.6)" :   // ✅ Completed = Green border
                "rgba(139, 92, 246, 0.2)";    // ✅ Pending = Purple border

        row.setStyle(
                "-fx-background-color: " + rowBackgroundColor + ";" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-color: " + rowBorderColor + ";" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 8;"
        );

        // ✅ SUPER VISIBLE CHECKBOX
        CheckBox checkBox = new CheckBox();
        checkBox.setSelected(task.getIsCompleted());

        String checkboxStyle = task.getIsCompleted() ?
                "-fx-border-color: #22c55e;" +
                        "-fx-border-width: 3px;" +
                        "-fx-background-color: rgba(34, 197, 94, 0.3);" +
                        "-fx-padding: 6;" +
                        "-fx-background-radius: 6;" +
                        "-fx-border-radius: 6;" +
                        "-fx-mark-color: #22c55e;" :
                "-fx-border-color: #8b5cf6;" +
                        "-fx-border-width: 3px;" +
                        "-fx-background-color: rgba(139, 92, 246, 0.2);" +
                        "-fx-padding: 6;" +
                        "-fx-background-radius: 6;" +
                        "-fx-border-radius: 6;" +
                        "-fx-mark-color: #8b5cf6;";

        checkBox.setStyle(checkboxStyle);
        checkBox.setMinSize(30, 30);
        checkBox.setMaxSize(30, 30);

        // ✅ Task info with schedule name
        VBox taskInfo = new VBox(5);

        Label taskNameLabel = new Label(task.getTaskTitle());
        taskNameLabel.setStyle(
                task.getIsCompleted() ?
                        "-fx-text-fill: #22c55e; -fx-font-size: 15px; -fx-strikethrough: true; -fx-font-weight: bold;" :
                        "-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;"
        );

        // ✅ Show linked schedule name
        String scheduleText = task.getScheduleTaskName() != null ?
                "📅 Linked to: " + task.getScheduleTaskName() :
                "⚠️ No schedule link";
        Label scheduleLabel = new Label(scheduleText);
        scheduleLabel.setStyle("-fx-text-fill: rgba(139, 92, 246, 0.9); -fx-font-size: 12px; -fx-font-style: italic;");

        taskInfo.getChildren().addAll(taskNameLabel, scheduleLabel);
        HBox.setHgrow(taskInfo, Priority.ALWAYS);

        // ✅ CHECKBOX ACTION - Updates schedule progress
        checkBox.setOnAction(e -> {
            task.setIsCompleted(checkBox.isSelected());

            // ✅ UPDATE DATABASE
            taskService.toggleTaskCompletion(
                    task.getId(),
                    currentUser != null ? currentUser.getUsername() : "system"
            );

            // ✅ UPDATE SCHEDULE PROGRESS
            if (task.getScheduleTaskName() != null) {
                updateScheduleProgress(task.getScheduleTaskName());
            }

            // ✅ UPDATE CHECKBOX STYLING
            String newCheckboxStyle = checkBox.isSelected() ?
                    "-fx-border-color: #22c55e;" +
                            "-fx-border-width: 3px;" +
                            "-fx-background-color: rgba(34, 197, 94, 0.3);" +
                            "-fx-padding: 6;" +
                            "-fx-background-radius: 6;" +
                            "-fx-border-radius: 6;" +
                            "-fx-mark-color: #22c55e;" :
                    "-fx-border-color: #8b5cf6;" +
                            "-fx-border-width: 3px;" +
                            "-fx-background-color: rgba(139, 92, 246, 0.2);" +
                            "-fx-padding: 6;" +
                            "-fx-background-radius: 6;" +
                            "-fx-border-radius: 6;" +
                            "-fx-mark-color: #8b5cf6;";

            checkBox.setStyle(newCheckboxStyle);

            // ✅ UPDATE ROW STYLING
            String newRowBg = checkBox.isSelected() ?
                    "rgba(34, 197, 94, 0.2)" :
                    "rgba(15, 23, 42, 0.6)";
            String newRowBorder = checkBox.isSelected() ?
                    "rgba(34, 197, 94, 0.6)" :
                    "rgba(139, 92, 246, 0.2)";

            row.setStyle(
                    "-fx-background-color: " + newRowBg + ";" +
                            "-fx-background-radius: 8;" +
                            "-fx-border-color: " + newRowBorder + ";" +
                            "-fx-border-width: 2;" +
                            "-fx-border-radius: 8;"
            );

            // ✅ UPDATE LABEL STYLING
            if (checkBox.isSelected()) {
                taskNameLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 15px; -fx-strikethrough: true; -fx-font-weight: bold;");
            } else {
                taskNameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");
            }
        });

        // Task details (priority and due date)
        VBox detailsBox = new VBox(3);

        if (task.getPriority() != null) {
            Label priorityLabel = new Label("Priority: " + task.getPriority());
            priorityLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 12px;");
            detailsBox.getChildren().add(priorityLabel);
        }

        if (task.getDueDate() != null) {
            Label dueDateLabel = new Label("Due: " + task.getDueDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
            dueDateLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 12px;");
            detailsBox.getChildren().add(dueDateLabel);
        }

        // ✅ VISIBLE DELETE BUTTON - Text button instead of just emoji
        Button deleteButton = new Button("🗑️ Delete");
        deleteButton.setStyle(
                "-fx-background-color: rgba(239, 68, 68, 0.3);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 13px;" +
                        "-fx-padding: 8 16;" +
                        "-fx-background-radius: 6;" +
                        "-fx-border-color: #ef4444;" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 6;" +
                        "-fx-cursor: hand;"
        );

        deleteButton.setOnMouseEntered(e -> deleteButton.setStyle(
                "-fx-background-color: #ef4444;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 13px;" +
                        "-fx-padding: 8 16;" +
                        "-fx-background-radius: 6;" +
                        "-fx-border-color: #dc2626;" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 6;" +
                        "-fx-cursor: hand;"
        ));

        deleteButton.setOnMouseExited(e -> deleteButton.setStyle(
                "-fx-background-color: rgba(239, 68, 68, 0.3);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 13px;" +
                        "-fx-padding: 8 16;" +
                        "-fx-background-radius: 6;" +
                        "-fx-border-color: #ef4444;" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 6;" +
                        "-fx-cursor: hand;"
        ));

        deleteButton.setOnAction(e -> handleDeleteTask(task));

        row.getChildren().addAll(checkBox, taskInfo, detailsBox, deleteButton);
        return row;
    }

    /**
     * ✅ NEW METHOD: Update schedule progress based on completed tasks
     */
    private void updateScheduleProgress(String scheduleTaskName) {
        Task<Void> updateTask = new Task<>() {
            @Override
            protected Void call() {
                // Get all tasks for this schedule item
                List<ProjectTask> allTasks = taskService.getTasksByProject(selectedProject.getId());
                List<ProjectTask> scheduleTasks = allTasks.stream()
                        .filter(t -> scheduleTaskName.equals(t.getScheduleTaskName()))
                        .collect(Collectors.toList());

                if (scheduleTasks.isEmpty()) return null;

                // Calculate completion percentage
                long completedCount = scheduleTasks.stream()
                        .filter(ProjectTask::getIsCompleted)
                        .count();

                int progressPercentage = (int) ((completedCount * 100) / scheduleTasks.size());

                // Update schedule in database
                List<ProjectSchedule> schedules = scheduleService.getSchedulesByProject(selectedProject.getId());
                ProjectSchedule schedule = schedules.stream()
                        .filter(s -> scheduleTaskName.equals(s.getTaskName()))
                        .findFirst()
                        .orElse(null);

                if (schedule != null) {
                    schedule.setProgress(progressPercentage);

                    // Auto-update status based on progress
                    if (progressPercentage == 0) {
                        schedule.setStatus("Pending");
                    } else if (progressPercentage == 100) {
                        schedule.setStatus("Completed");
                    } else {
                        schedule.setStatus("In Progress");
                    }

                    scheduleService.updateSchedule(schedule.getId(), schedule);
                }

                return null;
            }
        };

        updateTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                loadScheduleData();  // Refresh schedule table
                showSuccess("✓ Task completed! Schedule progress updated.");
            });
        });

        updateTask.setOnFailed(e -> {
            System.err.println("Failed to update schedule progress: " + updateTask.getException().getMessage());
        });

        new Thread(updateTask).start();
    }

    // ==================== ✅ REDESIGNED: ELEMENTS TAB - Shows Project Items ====================

    private VBox createElementsTab() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: transparent;");

        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("📦 Project Elements");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");
        HBox.setHgrow(title, Priority.ALWAYS);

        Button addButton = createStyledButton("+ Add Element", "#22c55e", "#16a34a");
        addButton.setOnAction(e -> handleAddElement());

        Button refreshButton = createStyledButton("🔄 Refresh", "#8b5cf6", "#7c3aed");
        refreshButton.setOnAction(e -> loadElementsData());

        header.getChildren().addAll(title, addButton, refreshButton);

        // ✅ NEW: ScrollPane with Grid of Element Cards
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(600);
        scrollPane.setStyle(
                "-fx-background: transparent;" +
                        "-fx-background-color: transparent;" +
                        "-fx-border-color: transparent;"
        );

        elementItems = FXCollections.observableArrayList();

        // Grid to hold element cards
        elementsGrid = new FlowPane();  // ✅ Store reference
        elementsGrid.setHgap(20);
        elementsGrid.setVgap(20);
        elementsGrid.setPadding(new Insets(10));
        elementsGrid.setStyle("-fx-background-color: transparent;");

        scrollPane.setContent(elementsGrid);

        content.getChildren().addAll(header, scrollPane);
        return content;
    }

    /**
     * ✅ NEW: Create element card for displaying project items
     */
    private VBox createElementCard(ProjectElementViewModel element) {
        VBox card = new VBox(15);
        card.setPrefWidth(350);
        card.setMinHeight(200);
        card.setPadding(new Insets(20));
        card.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.7);" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: rgba(251, 146, 60, 0.5);" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 12;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 10, 0, 0, 4);"
        );

        // Header with item name
        Label itemNameLabel = new Label("📦 " + element.getStorageItemName());
        itemNameLabel.setWrapText(true);
        itemNameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        // Separator
        javafx.scene.shape.Line separator = new javafx.scene.shape.Line();
        separator.setEndX(300);
        separator.setStroke(javafx.scene.paint.Color.web("rgba(251, 146, 60, 0.3)"));
        separator.setStrokeWidth(2);

        // Quantity info
        HBox quantityBox = new HBox(20);
        quantityBox.setAlignment(Pos.CENTER_LEFT);

        VBox neededBox = new VBox(5);
        Label neededTitleLabel = new Label("Needed");
        neededTitleLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.6); -fx-font-size: 12px;");
        Label neededValueLabel = new Label(String.valueOf(element.getQuantityNeeded()));
        neededValueLabel.setStyle("-fx-text-fill: #fca5a5; -fx-font-size: 24px; -fx-font-weight: bold;");
        neededBox.getChildren().addAll(neededTitleLabel, neededValueLabel);

        VBox allocatedBox = new VBox(5);
        Label allocatedTitleLabel = new Label("Allocated");
        allocatedTitleLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.6); -fx-font-size: 12px;");
        Label allocatedValueLabel = new Label(String.valueOf(element.getQuantityAllocated()));
        allocatedValueLabel.setStyle("-fx-text-fill: #86efac; -fx-font-size: 24px; -fx-font-weight: bold;");
        allocatedBox.getChildren().addAll(allocatedTitleLabel, allocatedValueLabel);

        quantityBox.getChildren().addAll(neededBox, allocatedBox);

        // Status badge
        Label statusLabel = new Label(element.getStatus());
        String statusColor = switch (element.getStatus()) {
            case "Pending" -> "#f59e0b";
            case "Allocated" -> "#22c55e";
            case "In Use" -> "#3b82f6";
            case "Returned" -> "#8b5cf6";
            default -> "#9ca3af";
        };
        statusLabel.setStyle(
                "-fx-background-color: " + statusColor + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 6 12;" +
                        "-fx-background-radius: 6;" +
                        "-fx-font-size: 12px;"
        );

        // Notes (if any)
        VBox notesBox = new VBox(5);
        if (element.getNotes() != null && !element.getNotes().isEmpty()) {
            Label notesTitle = new Label("📝 Notes:");
            notesTitle.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 12px;");

            Label notesContent = new Label(element.getNotes());
            notesContent.setWrapText(true);
            notesContent.setMaxWidth(300);
            notesContent.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.9); -fx-font-size: 13px;");

            notesBox.getChildren().addAll(notesTitle, notesContent);
        }

        // Delete button
        Button deleteButton = new Button("🗑️ Remove from Project");
        deleteButton.setMaxWidth(Double.MAX_VALUE);
        deleteButton.setStyle(
                "-fx-background-color: rgba(239, 68, 68, 0.2);" +
                        "-fx-text-fill: #ef4444;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 20;" +
                        "-fx-background-radius: 6;" +
                        "-fx-border-color: #ef4444;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 6;" +
                        "-fx-cursor: hand;"
        );

        deleteButton.setOnMouseEntered(e -> deleteButton.setStyle(
                "-fx-background-color: #ef4444;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 20;" +
                        "-fx-background-radius: 6;" +
                        "-fx-border-color: #dc2626;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 6;" +
                        "-fx-cursor: hand;"
        ));

        deleteButton.setOnMouseExited(e -> deleteButton.setStyle(
                "-fx-background-color: rgba(239, 68, 68, 0.2);" +
                        "-fx-text-fill: #ef4444;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 20;" +
                        "-fx-background-radius: 6;" +
                        "-fx-border-color: #ef4444;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 6;" +
                        "-fx-cursor: hand;"
        ));

        deleteButton.setOnAction(e -> handleDeleteElementById(element.getId()));

        card.getChildren().addAll(itemNameLabel, separator, quantityBox, statusLabel, notesBox, deleteButton);
        return card;
    }

    /**
     * ✅ MODIFIED: Delete element and return quantity to storage
     */
    private void handleDeleteElementById(long elementId) {
        // First, get the element details before deletion
        Task<ProjectElement> getElementTask = new Task<>() {
            @Override
            protected ProjectElement call() {
                return elementService.getElementById(elementId);
            }
        };

        getElementTask.setOnSucceeded(e -> {
            ProjectElement element = getElementTask.getValue();

            if (element == null) {
                showError("Element not found");
                return;
            }

            // Show confirmation with return info
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Remove Element");
            confirm.setHeaderText("Remove this element from project?");

            String itemName = element.getStorageItem() != null ?
                    element.getStorageItem().getProductName() : "Unknown Item";
            int returnQty = element.getQuantityAllocated() != null ?
                    element.getQuantityAllocated() : 0;

            confirm.setContentText(
                    "Item: " + itemName + "\n" +
                            "Allocated Quantity: " + returnQty + "\n\n" +
                            "This will:\n" +
                            "• Remove element from project\n" +
                            "• Return " + returnQty + " units to storage\n" +
                            "• Update storage database\n\n" +
                            "Continue?"
            );

            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    // Delete and return quantity
                    Task<Void> deleteTask = new Task<>() {
                        @Override
                        protected Void call() {
                            // ✅ RETURN QUANTITY TO STORAGE
                            if (element.getStorageItem() != null && returnQty > 0) {
                                StorageItem storageItem = element.getStorageItem();
                                int currentQty = storageItem.getQuantity();
                                int newQty = currentQty + returnQty;

                                storageItem.setQuantity(newQty);
                                storageService.updateItem(storageItem.getId(), storageItem);

                                System.out.println("✓ Returned " + returnQty + " units to storage");
                                System.out.println("✓ Storage quantity: " + currentQty + " → " + newQty);
                            }

                            // Delete element from project
                            elementService.deleteElement(elementId);

                            return null;
                        }
                    };

                    deleteTask.setOnSucceeded(event -> {
                        loadElementsData();
                        showSuccess("✓ Element removed and " + returnQty + " units returned to storage");
                    });

                    deleteTask.setOnFailed(event ->
                            showError("Delete failed: " + deleteTask.getException().getMessage())
                    );

                    new Thread(deleteTask).start();
                }
            });
        });

        getElementTask.setOnFailed(e -> {
            showError("Failed to retrieve element: " + getElementTask.getException().getMessage());
        });

        new Thread(getElementTask).start();
    }

    // ==================== DIALOG HANDLERS ====================

    // ✅ FIX #3: FIXED TEXT VISIBILITY IN DIALOGS (Dark background + white text)
    private void handleAddScheduleItem() {
        Dialog<ProjectScheduleViewModel> dialog = new Dialog<>();
        dialog.setTitle("📅 Add Schedule Task");
        dialog.setHeaderText("Create new schedule task for " + selectedProject.getProjectName());

        // ✅ FIXED: Header text color
        dialog.getDialogPane().lookup(".header-panel").setStyle(
                "-fx-background-color: #1e293b;" +
                        "-fx-text-fill: white;"
        );
        dialog.getDialogPane().lookup(".header-panel .label").setStyle("-fx-text-fill: white;");

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));
        grid.setStyle("-fx-background-color: #0f172a;");  // ✅ Dark background

        // ✅ FIXED: All labels are now WHITE and VISIBLE
        Label taskNameLbl = new Label("Task Name:*");
        taskNameLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        TextField taskNameField = new TextField();
        taskNameField.setPromptText("Enter task name");
        taskNameField.setStyle(
                "-fx-background-color: #1e293b;" +
                        "-fx-text-fill: white;" +
                        "-fx-prompt-text-fill: #9ca3af;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 10;" +
                        "-fx-border-color: rgba(139, 92, 246, 0.3);" +
                        "-fx-border-radius: 4;"
        );

        Label startLbl = new Label("Start Date:");
        startLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        DatePicker startDatePicker = new DatePicker();
        startDatePicker.setPromptText("Select start date");
        startDatePicker.setStyle("-fx-background-color: #1e293b;");
        startDatePicker.getEditor().setStyle(
                "-fx-text-fill: white;" +
                        "-fx-background-color: #1e293b;" +
                        "-fx-prompt-text-fill: #9ca3af;"
        );

        Label endLbl = new Label("End Date:");
        endLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        DatePicker endDatePicker = new DatePicker();
        endDatePicker.setPromptText("Select end date");
        endDatePicker.setStyle("-fx-background-color: #1e293b;");
        endDatePicker.getEditor().setStyle(
                "-fx-text-fill: white;" +
                        "-fx-background-color: #1e293b;" +
                        "-fx-prompt-text-fill: #9ca3af;"
        );

        Label statusLbl = new Label("Status:");
        statusLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("Pending", "In Progress", "Completed");
        statusCombo.setValue("Pending");
        statusCombo.setStyle(
                "-fx-background-color: #1e293b;" +
                        "-fx-text-fill: white;"
        );

        Label progressLbl = new Label("Progress (%):");
        progressLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        Spinner<Integer> progressSpinner = new Spinner<>(0, 100, 0, 5);
        progressSpinner.setEditable(true);
        progressSpinner.setStyle("-fx-background-color: #1e293b;");
        progressSpinner.getEditor().setStyle(
                "-fx-text-fill: white;" +
                        "-fx-background-color: #1e293b;"
        );

        Label assignedLbl = new Label("Assigned To:");
        assignedLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        TextField assignedToField = new TextField();
        assignedToField.setPromptText("Assigned to (optional)");
        assignedToField.setStyle(
                "-fx-background-color: #1e293b;" +
                        "-fx-text-fill: white;" +
                        "-fx-prompt-text-fill: #9ca3af;" +
                        "-fx-padding: 10;" +
                        "-fx-border-color: rgba(139, 92, 246, 0.3);" +
                        "-fx-border-radius: 4;"
        );

        grid.add(taskNameLbl, 0, 0);
        grid.add(taskNameField, 1, 0);
        grid.add(startLbl, 0, 1);
        grid.add(startDatePicker, 1, 1);
        grid.add(endLbl, 0, 2);
        grid.add(endDatePicker, 1, 2);
        grid.add(statusLbl, 0, 3);
        grid.add(statusCombo, 1, 3);
        grid.add(progressLbl, 0, 4);
        grid.add(progressSpinner, 1, 4);
        grid.add(assignedLbl, 0, 5);
        grid.add(assignedToField, 1, 5);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setStyle("-fx-background-color: #0f172a;");

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                if (taskNameField.getText().isEmpty()) {
                    showWarning("Task name is required");
                    return null;
                }
                ProjectScheduleViewModel vm = new ProjectScheduleViewModel();
                vm.setTaskName(taskNameField.getText());
                vm.setStartDate(startDatePicker.getValue() != null ? startDatePicker.getValue().toString() : "");
                vm.setEndDate(endDatePicker.getValue() != null ? endDatePicker.getValue().toString() : "");
                vm.setStatus(statusCombo.getValue());
                vm.setProgress(progressSpinner.getValue());
                vm.setAssignedTo(assignedToField.getText());
                return vm;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(vm -> {
            Task<ProjectSchedule> saveTask = new Task<>() {
                @Override
                protected ProjectSchedule call() {
                    ProjectSchedule entity = new ProjectSchedule();
                    entity.setProject(selectedProject);
                    entity.setTaskName(vm.getTaskName());
                    if (!vm.getStartDate().isEmpty()) {
                        entity.setStartDate(LocalDate.parse(vm.getStartDate()));
                    }
                    if (!vm.getEndDate().isEmpty()) {
                        entity.setEndDate(LocalDate.parse(vm.getEndDate()));
                    }
                    entity.setStatus(vm.getStatus());
                    entity.setProgress(vm.getProgress());
                    entity.setAssignedTo(vm.getAssignedTo());
                    entity.setCreatedBy(currentUser != null ? currentUser.getUsername() : "system");
                    return scheduleService.createSchedule(entity);
                }
            };

            saveTask.setOnSucceeded(e -> {
                loadScheduleData();
                showSuccess("✓ Schedule task added!");
            });

            saveTask.setOnFailed(e -> showError("Failed to add schedule task: " + saveTask.getException().getMessage()));

            new Thread(saveTask).start();
        });
    }

    // ✅ FIX #4: EDIT SCHEDULE - NOW FULLY WORKING!
    private void handleEditScheduleItem() {
        ProjectScheduleViewModel selected = scheduleTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Please select a schedule item to edit");
            return;
        }

        // ✅ FIXED: Create edit dialog with pre-filled data
        Dialog<ProjectScheduleViewModel> dialog = new Dialog<>();
        dialog.setTitle("✏️ Edit Schedule Task");
        dialog.setHeaderText("Editing: " + selected.getTaskName());

        // ✅ FIXED: Header styling
        dialog.getDialogPane().lookup(".header-panel").setStyle(
                "-fx-background-color: #1e293b;" +
                        "-fx-text-fill: white;"
        );
        if (dialog.getDialogPane().lookup(".header-panel .label") != null) {
            dialog.getDialogPane().lookup(".header-panel .label").setStyle("-fx-text-fill: white;");
        }

        ButtonType saveButtonType = new ButtonType("💾 Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));
        grid.setStyle("-fx-background-color: #0f172a;");

        // ✅ Task Name (pre-filled)
        Label taskNameLbl = new Label("Task Name:*");
        taskNameLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        TextField taskNameField = new TextField(selected.getTaskName());
        taskNameField.setStyle(
                "-fx-background-color: #1e293b;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 10;" +
                        "-fx-border-color: rgba(139, 92, 246, 0.3);" +
                        "-fx-border-radius: 4;"
        );

        // ✅ Start Date (pre-filled)
        Label startLbl = new Label("Start Date:");
        startLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        DatePicker startDatePicker = new DatePicker();
        if (!selected.getStartDate().isEmpty()) {
            startDatePicker.setValue(LocalDate.parse(selected.getStartDate()));
        }
        startDatePicker.setStyle("-fx-background-color: #1e293b;");
        startDatePicker.getEditor().setStyle(
                "-fx-text-fill: white;" +
                        "-fx-background-color: #1e293b;"
        );

        // ✅ End Date (pre-filled)
        Label endLbl = new Label("End Date:");
        endLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        DatePicker endDatePicker = new DatePicker();
        if (!selected.getEndDate().isEmpty()) {
            endDatePicker.setValue(LocalDate.parse(selected.getEndDate()));
        }
        endDatePicker.setStyle("-fx-background-color: #1e293b;");
        endDatePicker.getEditor().setStyle(
                "-fx-text-fill: white;" +
                        "-fx-background-color: #1e293b;"
        );

        // ✅ Status (pre-filled)
        Label statusLbl = new Label("Status:");
        statusLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("Pending", "In Progress", "Completed");
        statusCombo.setValue(selected.getStatus());
        statusCombo.setStyle(
                "-fx-background-color: #1e293b;" +
                        "-fx-text-fill: white;"
        );

        // ✅ Progress (pre-filled)
        Label progressLbl = new Label("Progress (%):");
        progressLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        Spinner<Integer> progressSpinner = new Spinner<>(0, 100, selected.getProgress(), 5);
        progressSpinner.setEditable(true);
        progressSpinner.setStyle("-fx-background-color: #1e293b;");
        progressSpinner.getEditor().setStyle(
                "-fx-text-fill: white;" +
                        "-fx-background-color: #1e293b;"
        );

        // ✅ Assigned To (pre-filled)
        Label assignedLbl = new Label("Assigned To:");
        assignedLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        TextField assignedToField = new TextField(selected.getAssignedTo());
        assignedToField.setStyle(
                "-fx-background-color: #1e293b;" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 10;" +
                        "-fx-border-color: rgba(139, 92, 246, 0.3);" +
                        "-fx-border-radius: 4;"
        );

        grid.add(taskNameLbl, 0, 0);
        grid.add(taskNameField, 1, 0);
        grid.add(startLbl, 0, 1);
        grid.add(startDatePicker, 1, 1);
        grid.add(endLbl, 0, 2);
        grid.add(endDatePicker, 1, 2);
        grid.add(statusLbl, 0, 3);
        grid.add(statusCombo, 1, 3);
        grid.add(progressLbl, 0, 4);
        grid.add(progressSpinner, 1, 4);
        grid.add(assignedLbl, 0, 5);
        grid.add(assignedToField, 1, 5);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setStyle("-fx-background-color: #0f172a;");

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                if (taskNameField.getText().isEmpty()) {
                    showWarning("Task name is required");
                    return null;
                }
                selected.setTaskName(taskNameField.getText());
                selected.setStartDate(startDatePicker.getValue() != null ? startDatePicker.getValue().toString() : "");
                selected.setEndDate(endDatePicker.getValue() != null ? endDatePicker.getValue().toString() : "");
                selected.setStatus(statusCombo.getValue());
                selected.setProgress(progressSpinner.getValue());
                selected.setAssignedTo(assignedToField.getText());
                return selected;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updatedVm -> {
            Task<ProjectSchedule> updateTask = new Task<>() {
                @Override
                protected ProjectSchedule call() {
                    // Create updated entity from view model
                    ProjectSchedule entity = new ProjectSchedule();
                    entity.setId(updatedVm.getId());
                    entity.setProject(selectedProject);
                    entity.setTaskName(updatedVm.getTaskName());
                    if (!updatedVm.getStartDate().isEmpty()) {
                        entity.setStartDate(LocalDate.parse(updatedVm.getStartDate()));
                    }
                    if (!updatedVm.getEndDate().isEmpty()) {
                        entity.setEndDate(LocalDate.parse(updatedVm.getEndDate()));
                    }
                    entity.setStatus(updatedVm.getStatus());
                    entity.setProgress(updatedVm.getProgress());
                    entity.setAssignedTo(updatedVm.getAssignedTo());
                    return scheduleService.updateSchedule(entity.getId(), entity);
                }
            };

            updateTask.setOnSucceeded(e -> {
                loadScheduleData();
                showSuccess("✓ Schedule task updated!");
            });

            updateTask.setOnFailed(e -> showError("Failed to update schedule: " + updateTask.getException().getMessage()));

            new Thread(updateTask).start();
        });
    }

    private void handleDeleteScheduleItem() {
        ProjectScheduleViewModel selected = scheduleTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Please select a schedule item to delete");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Schedule Item");
        confirm.setHeaderText("Delete this schedule item?");
        confirm.setContentText("This action cannot be undone!");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Task<Void> deleteTask = new Task<>() {
                    @Override
                    protected Void call() {
                        scheduleService.deleteSchedule(selected.getId());
                        return null;
                    }
                };

                deleteTask.setOnSucceeded(e -> {
                    loadScheduleData();
                    showSuccess("✓ Schedule item deleted");
                });

                deleteTask.setOnFailed(e -> showError("Delete failed: " + deleteTask.getException().getMessage()));

                new Thread(deleteTask).start();
            }
        });
    }

    // ✅ MODIFIED: Task linked to Schedule items with progress tracking
    private void handleAddTask() {
        Dialog<ProjectTaskViewModel> dialog = new Dialog<>();
        dialog.setTitle("✅ Add Task");
        dialog.setHeaderText("Create new task for " + selectedProject.getProjectName());

        // ✅ FIXED: Header styling
        if (dialog.getDialogPane().lookup(".header-panel") != null) {
            dialog.getDialogPane().lookup(".header-panel").setStyle(
                    "-fx-background-color: #1e293b;" +
                            "-fx-text-fill: white;"
            );
        }
        if (dialog.getDialogPane().lookup(".header-panel .label") != null) {
            dialog.getDialogPane().lookup(".header-panel .label").setStyle("-fx-text-fill: white;");
        }

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));
        grid.setStyle("-fx-background-color: #0f172a;");

        // ✅ NEW: Schedule Item Selection (REQUIRED)
        Label scheduleLabel = new Label("Related Schedule Task:*");
        scheduleLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        ComboBox<String> scheduleCombo = new ComboBox<>();
        // Load schedule items from current project
        if (scheduleItems != null && !scheduleItems.isEmpty()) {
            for (ProjectScheduleViewModel schedule : scheduleItems) {
                scheduleCombo.getItems().add(schedule.getTaskName());
            }
        }
        scheduleCombo.setPromptText("Select schedule task");
        scheduleCombo.setStyle(
                "-fx-background-color: #1e293b;" +
                        "-fx-text-fill: white;" +
                        "-fx-prompt-text-fill: #9ca3af;"
        );
        // ✅ FIXED: Force ComboBox text to be white
        scheduleCombo.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: white;");
                }
            }
        });
        scheduleCombo.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: white; -fx-background-color: #1e293b;");
                }
            }
        });

        Label titleLabel = new Label("Task Title:*");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        TextField taskTitleField = new TextField();
        taskTitleField.setPromptText("Enter task title (Required)");
        taskTitleField.setStyle(
                "-fx-background-color: #1e293b;" +
                        "-fx-text-fill: white;" +
                        "-fx-prompt-text-fill: #9ca3af;" +
                        "-fx-padding: 10;" +
                        "-fx-border-color: rgba(139, 92, 246, 0.3);" +
                        "-fx-border-radius: 4;"
        );

        Label detailsLabel = new Label("Task Details:");
        detailsLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        TextArea taskDetailsArea = new TextArea();
        taskDetailsArea.setPromptText("Enter task details (Optional)");
        taskDetailsArea.setPrefRowCount(3);
        taskDetailsArea.setWrapText(true);
        taskDetailsArea.setStyle(
                "-fx-control-inner-background: #1e293b;" +  // ✅ FIXED: Dark inner background
                        "-fx-background-color: #1e293b;" +
                        "-fx-text-fill: white;" +
                        "-fx-prompt-text-fill: #9ca3af;" +
                        "-fx-padding: 10;" +
                        "-fx-border-color: rgba(139, 92, 246, 0.3);" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 4;"
        );

        Label priorityLabel = new Label("Priority:");
        priorityLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        ComboBox<String> priorityCombo = new ComboBox<>();
        priorityCombo.getItems().addAll("Low", "Medium", "High", "Critical");
        priorityCombo.setValue("Medium");
        priorityCombo.setStyle(
                "-fx-background-color: #1e293b;" +
                        "-fx-text-fill: white;"
        );
        // ✅ FIXED: Force ComboBox text to be white
        priorityCombo.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: white;");
                }
            }
        });
        priorityCombo.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: white; -fx-background-color: #1e293b;");
                }
            }
        });

        Label dueDateLabel = new Label("Due Date:");
        dueDateLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        DatePicker dueDatePicker = new DatePicker();
        dueDatePicker.setPromptText("Select due date");
        dueDatePicker.setStyle("-fx-background-color: #1e293b;");
        dueDatePicker.getEditor().setStyle(
                "-fx-text-fill: white;" +
                        "-fx-background-color: #1e293b;" +
                        "-fx-prompt-text-fill: #9ca3af;"
        );

        Label timeLabel = new Label("Due Time:");
        timeLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        HBox timeBox = new HBox(10);
        Spinner<Integer> hourSpinner = new Spinner<>(0, 23, 12);
        hourSpinner.setPrefWidth(70);
        hourSpinner.setEditable(true);
        hourSpinner.getEditor().setStyle(
                "-fx-text-fill: white;" +
                        "-fx-background-color: #1e293b;"
        );

        Label colonLabel = new Label(":");
        colonLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        Spinner<Integer> minuteSpinner = new Spinner<>(0, 59, 0);
        minuteSpinner.setPrefWidth(70);
        minuteSpinner.setEditable(true);
        minuteSpinner.getEditor().setStyle(
                "-fx-text-fill: white;" +
                        "-fx-background-color: #1e293b;"
        );

        timeBox.getChildren().addAll(hourSpinner, colonLabel, minuteSpinner);

        grid.add(scheduleLabel, 0, 0);
        grid.add(scheduleCombo, 1, 0);
        grid.add(titleLabel, 0, 1);
        grid.add(taskTitleField, 1, 1);
        grid.add(detailsLabel, 0, 2);
        grid.add(taskDetailsArea, 1, 2);
        grid.add(priorityLabel, 0, 3);
        grid.add(priorityCombo, 1, 3);
        grid.add(dueDateLabel, 0, 4);
        grid.add(dueDatePicker, 1, 4);
        grid.add(timeLabel, 0, 5);
        grid.add(timeBox, 1, 5);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setStyle("-fx-background-color: #0f172a;");

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                if (taskTitleField.getText().isEmpty()) {
                    showWarning("Task title is required");
                    return null;
                }
                if (scheduleCombo.getValue() == null || scheduleCombo.getValue().isEmpty()) {
                    showWarning("Please select a schedule task");
                    return null;
                }

                ProjectTaskViewModel vm = new ProjectTaskViewModel();
                vm.setTaskTitle(taskTitleField.getText());
                vm.setTaskDetails(taskDetailsArea.getText());
                vm.setPriority(priorityCombo.getValue());
                vm.setScheduleTaskName(scheduleCombo.getValue());  // ✅ Link to schedule

                if (dueDatePicker.getValue() != null) {
                    LocalDate date = dueDatePicker.getValue();
                    int hour = hourSpinner.getValue();
                    int minute = minuteSpinner.getValue();
                    vm.setDueDate(date.atTime(hour, minute));
                }

                return vm;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(vm -> {
            Task<ProjectTask> saveTask = new Task<>() {
                @Override
                protected ProjectTask call() {
                    ProjectTask entity = new ProjectTask();
                    entity.setProject(selectedProject);
                    entity.setTaskTitle(vm.getTaskTitle());
                    entity.setTaskDetails(vm.getTaskDetails());
                    entity.setPriority(vm.getPriority());
                    entity.setDueDate(vm.getDueDate());
                    entity.setScheduleTaskName(vm.getScheduleTaskName());  // ✅ Store schedule link
                    entity.setIsCompleted(false);
                    entity.setCreatedBy(currentUser != null ? currentUser.getUsername() : "system");
                    return taskService.createTask(entity);
                }
            };

            saveTask.setOnSucceeded(e -> {
                loadTasksData();
                showSuccess("✓ Task added and linked to schedule!");
            });

            saveTask.setOnFailed(e -> showError("Failed to add task: " + saveTask.getException().getMessage()));

            new Thread(saveTask).start();
        });
    }

    // ✅ FIX #1: AVAILABILITY CHECK when adding elements
    private void handleAddElement() {
        Stage dialogStage = new Stage();
        dialogStage.setTitle("📦 Add Project Element");
        dialogStage.initModality(Modality.APPLICATION_MODAL);

        VBox mainLayout = new VBox(20);
        mainLayout.setPadding(new Insets(30));
        mainLayout.setStyle("-fx-background-color: linear-gradient(to bottom right, #1e293b, #0f172a);");

        Label titleLabel = new Label("Select Storage Item & Add to Project");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

        TableView<StorageItem> storageTable = new TableView<>();
        storageTable.setPrefHeight(450);
        storageTable.setStyle(
                "-fx-background-color: #0f172a;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: rgba(139, 92, 246, 0.4);" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 12;" +
                        "-fx-control-inner-background: #0f172a;" +
                        "-fx-control-inner-background-alt: #1e293b;"
        );

        // ✅ Columns with white text
        TableColumn<StorageItem, String> nameCol = new TableColumn<>("📦 Product Name");
        nameCol.setPrefWidth(250);
        nameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        nameCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10;");
                }
            }
        });

        TableColumn<StorageItem, String> manufactureCol = new TableColumn<>("🏭 Manufacture");
        manufactureCol.setPrefWidth(180);
        manufactureCol.setCellValueFactory(new PropertyValueFactory<>("manufacture"));
        manufactureCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
                }
            }
        });

        TableColumn<StorageItem, String> codeCol = new TableColumn<>("🔢 Code");
        codeCol.setPrefWidth(140);
        codeCol.setCellValueFactory(new PropertyValueFactory<>("code"));
        codeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
                }
            }
        });

        TableColumn<StorageItem, String> serialCol = new TableColumn<>("🔖 Serial");
        serialCol.setPrefWidth(140);
        serialCol.setCellValueFactory(new PropertyValueFactory<>("serialNumber"));
        serialCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
                }
            }
        });

        // Location column removed - property doesn't exist in StorageItem
        // If you need location, add it to StorageItem entity first

        TableColumn<StorageItem, Void> actionCol = new TableColumn<>("⚡ Action");
        actionCol.setPrefWidth(120);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button addButton = new Button("+ Add");

            {
                addButton.setStyle(
                        "-fx-background-color: #22c55e;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 8 16;" +
                                "-fx-background-radius: 6;" +
                                "-fx-cursor: hand;"
                );

                addButton.setOnMouseEntered(e -> addButton.setStyle(
                        "-fx-background-color: #16a34a;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 8 16;" +
                                "-fx-background-radius: 6;" +
                                "-fx-cursor: hand;"
                ));

                addButton.setOnMouseExited(e -> addButton.setStyle(
                        "-fx-background-color: #22c55e;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 8 16;" +
                                "-fx-background-radius: 6;" +
                                "-fx-cursor: hand;"
                ));

                addButton.setOnAction(e -> {
                    StorageItem selectedItem = getTableView().getItems().get(getIndex());
                    handleAddStorageItemToProject(selectedItem, dialogStage);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(addButton);
                }
            }
        });

        storageTable.getColumns().addAll(nameCol, manufactureCol, codeCol, serialCol, actionCol);

        // Load storage items
        Task<List<StorageItem>> loadTask = new Task<>() {
            @Override
            protected List<StorageItem> call() {
                return storageService.getAllItems();
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<StorageItem> items = loadTask.getValue();
            storageTable.setItems(FXCollections.observableArrayList(items));
        });

        new Thread(loadTask).start();

        HBox buttonsBox = new HBox(15);
        buttonsBox.setAlignment(Pos.CENTER);

        Button closeButton = createStyledButton("✗ Close", "#6b7280", "#4b5563");
        closeButton.setOnAction(e -> dialogStage.close());

        buttonsBox.getChildren().add(closeButton);

        mainLayout.getChildren().addAll(titleLabel, storageTable, buttonsBox);

        Scene scene = new Scene(mainLayout, 1200, 700);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }

    // ✅ FIX #1: NO quantity shown upfront - user specifies first, then we check
    private void handleAddStorageItemToProject(StorageItem storageItem, Stage parentDialog) {
        // ✅ Proceed directly to quantity dialog WITHOUT showing available quantity
        Dialog<Integer> quantityDialog = new Dialog<>();
        quantityDialog.setTitle("📦 Specify Quantity");
        quantityDialog.setHeaderText("Add: " + storageItem.getProductName());

        // ✅ FIXED: Header styling
        if (quantityDialog.getDialogPane().lookup(".header-panel") != null) {
            quantityDialog.getDialogPane().lookup(".header-panel").setStyle(
                    "-fx-background-color: #1e293b;" +
                            "-fx-text-fill: white;"
            );
        }

        ButtonType confirmButtonType = new ButtonType("✓ Check Availability", ButtonBar.ButtonData.OK_DONE);
        quantityDialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));
        grid.setStyle("-fx-background-color: #0f172a;");

        // ✅ Quantity needed spinner (no max limit shown)
        Label neededLabel = new Label("Quantity Needed:*");
        neededLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        Spinner<Integer> quantitySpinner = new Spinner<>(1, 999999, 1);  // ✅ No max limit visible
        quantitySpinner.setEditable(true);
        quantitySpinner.setPrefWidth(150);
        quantitySpinner.setStyle("-fx-background-color: #1e293b;");
        quantitySpinner.getEditor().setStyle(
                "-fx-text-fill: white;" +
                        "-fx-background-color: #1e293b;"
        );

        // Notes field
        Label notesLabel = new Label("Notes (Optional):");
        notesLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        TextField notesField = new TextField();
        notesField.setPromptText("Add notes about this element");
        notesField.setPrefWidth(300);
        notesField.setStyle(
                "-fx-background-color: #1e293b;" +
                        "-fx-text-fill: white;" +
                        "-fx-prompt-text-fill: #9ca3af;" +
                        "-fx-border-color: rgba(139, 92, 246, 0.3);" +
                        "-fx-border-radius: 4;"
        );

        grid.add(neededLabel, 0, 0);
        grid.add(quantitySpinner, 1, 0);
        grid.add(notesLabel, 0, 1);
        grid.add(notesField, 1, 1);

        quantityDialog.getDialogPane().setContent(grid);
        quantityDialog.getDialogPane().setStyle("-fx-background-color: #0f172a;");

        quantityDialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                return quantitySpinner.getValue();
            }
            return null;
        });

        quantityDialog.showAndWait().ifPresent(requestedQty -> {
            // ✅ NOW CHECK AVAILABILITY after user specified quantity
            int availableQty = storageItem.getQuantity();

            if (requestedQty > availableQty) {
                // ❌ NOT AVAILABLE
                Alert notAvailableAlert = new Alert(Alert.AlertType.ERROR);
                notAvailableAlert.setTitle("❌ Insufficient Stock");
                notAvailableAlert.setHeaderText("Cannot allocate " + requestedQty + " units");
                notAvailableAlert.setContentText(
                        "The requested quantity is not available in stock.\n\n" +
                                "Requested: " + requestedQty + " units\n" +
                                "Status: ❌ UNAVAILABLE\n\n" +
                                "Please request a smaller quantity or restock this item."
                );
                notAvailableAlert.showAndWait();
                return;
            }

            // ✅ AVAILABLE - Show confirmation
            Alert availableAlert = new Alert(Alert.AlertType.CONFIRMATION);
            availableAlert.setTitle("✅ Stock Available");
            availableAlert.setHeaderText("Confirm Addition");
            availableAlert.setContentText(
                    "Item: " + storageItem.getProductName() + "\n" +
                            "Requested Quantity: " + requestedQty + " units\n" +
                            "Status: ✅ AVAILABLE\n\n" +
                            "This will:\n" +
                            "• Add element to project\n" +
                            "• Deduct " + requestedQty + " from storage\n" +
                            "• Update storage database\n\n" +
                            "Confirm to proceed?"
            );

            availableAlert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    // Create project element and update storage
                    Task<ProjectElement> addElementTask = new Task<>() {
                        @Override
                        protected ProjectElement call() {
                            // Create project element
                            ProjectElement element = new ProjectElement();
                            element.setProject(selectedProject);
                            element.setStorageItem(storageItem);
                            element.setQuantityNeeded(requestedQty);
                            element.setQuantityAllocated(requestedQty);
                            element.setStatus("ALLOCATED");
                            element.setNotes(notesField.getText());
                            element.setDateAdded(LocalDateTime.now());
                            element.setCreatedBy(currentUser != null ? currentUser.getUsername() : "system");
                            element.setActive(true);

                            // Save element
                            ProjectElement saved = elementService.createElement(element);

                            // Update storage quantity
                            storageItem.setQuantity(storageItem.getQuantity() - requestedQty);
                            storageService.updateItem(storageItem);

                            return saved;
                        }
                    };

                    addElementTask.setOnSucceeded(event -> {
                        Platform.runLater(() -> {
                            loadElementsData(); // Refresh elements
                            showSuccess("✓ Element added successfully!\n" +
                                      "Storage quantity updated.");
                            parentDialog.close();
                        });
                    });

                    addElementTask.setOnFailed(event ->
                            showError("Failed to add element: " + addElementTask.getException().getMessage())
                    );

                    new Thread(addElementTask).start();
                }
            });
        });
    }

    private void handleDeleteTask(ProjectTask task) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Task");
        confirm.setHeaderText("Delete this task?");
        confirm.setContentText("This action cannot be undone!");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Task<Void> deleteTask = new Task<>() {
                    @Override
                    protected Void call() {
                        taskService.deleteTask(task.getId());
                        return null;
                    }
                };

                deleteTask.setOnSucceeded(e -> {
                    loadTasksData();
                    showSuccess("✓ Task deleted");
                });

                deleteTask.setOnFailed(e -> showError("Delete failed: " + deleteTask.getException().getMessage()));

                new Thread(deleteTask).start();
            }
        });
    }

    private void handleSaveNotes() {
        if (selectedProject == null) return;

        String notesContent = importantNotesArea.getText();

        Task<Void> saveTask = new Task<>() {
            @Override
            protected Void call() {
                List<ProjectNote> existingNotes = noteService.getNotesByProject(selectedProject.getId());

                if (existingNotes.isEmpty()) {
                    ProjectNote newNote = new ProjectNote();
                    newNote.setProject(selectedProject);
                    newNote.setImportantDescription(notesContent);
                    newNote.setCreatedBy(currentUser != null ? currentUser.getUsername() : "system");
                    noteService.createNote(newNote);
                } else {
                    ProjectNote note = existingNotes.get(0);
                    note.setImportantDescription(notesContent);
                    noteService.updateNote(note.getId(), note);
                }
                return null;
            }
        };

        saveTask.setOnSucceeded(e -> showSuccess("✓ Notes saved!"));
        saveTask.setOnFailed(e -> showError("Failed to save notes: " + saveTask.getException().getMessage()));

        new Thread(saveTask).start();
    }

// ==================== DATA LOADING ====================

    private void loadScheduleData() {
        if (selectedProject == null) return;

        Task<List<ProjectSchedule>> loadTask = new Task<>() {
            @Override
            protected List<ProjectSchedule> call() {
                return scheduleService.getSchedulesByProject(selectedProject.getId());
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<ProjectSchedule> schedules = loadTask.getValue();
            scheduleItems.clear();

            for (ProjectSchedule schedule : schedules) {
                ProjectScheduleViewModel vm = new ProjectScheduleViewModel();
                vm.setId(schedule.getId());
                vm.setTaskName(schedule.getTaskName());
                vm.setStartDate(schedule.getStartDate() != null ? schedule.getStartDate().toString() : "");
                vm.setEndDate(schedule.getEndDate() != null ? schedule.getEndDate().toString() : "");
                vm.setStatus(schedule.getStatus());
                vm.setProgress(schedule.getProgress());
                vm.setAssignedTo(schedule.getAssignedTo() != null ? schedule.getAssignedTo() : "");
                scheduleItems.add(vm);
            }

            scheduleTable.setItems(scheduleItems);
        });

        loadTask.setOnFailed(e -> showError("Failed to load schedule: " + loadTask.getException().getMessage()));

        new Thread(loadTask).start();
    }

    private void loadTasksData() {
        if (selectedProject == null) return;

        Task<List<ProjectTask>> loadTask = new Task<>() {
            @Override
            protected List<ProjectTask> call() {
                return taskService.getTasksByProject(selectedProject.getId());
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<ProjectTask> tasks = loadTask.getValue();
            tasksContainer.getChildren().clear();

            for (ProjectTask task : tasks) {
                HBox taskRow = createTaskRow(task);
                tasksContainer.getChildren().add(taskRow);
            }
        });

        loadTask.setOnFailed(e -> showError("Failed to load tasks: " + loadTask.getException().getMessage()));

        new Thread(loadTask).start();

        loadNotesData();
    }

    private void loadNotesData() {
        if (selectedProject == null) return;

        Task<List<ProjectNote>> loadTask = new Task<>() {
            @Override
            protected List<ProjectNote> call() {
                return noteService.getNotesByProject(selectedProject.getId());
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<ProjectNote> notes = loadTask.getValue();
            if (!notes.isEmpty()) {
                importantNotesArea.setText(notes.get(0).getImportantDescription());
            }
        });

        new Thread(loadTask).start();
    }

    private void loadElementsData() {
        if (selectedProject == null) {
            System.out.println("⚠️ Cannot load elements: selectedProject is null");
            return;
        }

        System.out.println("📦 Loading elements for project: " + selectedProject.getProjectName());
        System.out.println("📦 Elements grid reference: " + (elementsGrid != null ? "OK" : "NULL"));

        Task<List<ProjectElement>> loadTask = new Task<>() {
            @Override
            protected List<ProjectElement> call() {
                return elementService.getElementsByProject(selectedProject.getId());
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<ProjectElement> elements = loadTask.getValue();
            System.out.println("📦 Retrieved " + elements.size() + " elements from database");

            elementItems.clear();

            for (ProjectElement element : elements) {
                try {
                    ProjectElementViewModel vm = new ProjectElementViewModel();
                    vm.setId(element.getId());

                    // ✅ FIXED: Access StorageItem within try-catch to handle lazy loading
                    String itemName = "Unknown Item";
                    try {
                        if (element.getStorageItem() != null) {
                            itemName = element.getStorageItem().getProductName();
                        }
                    } catch (Exception ex) {
                        System.out.println("⚠️ Could not load StorageItem for element " + element.getId() + ": " + ex.getMessage());
                        // Use ID as fallback
                        itemName = "Item #" + (element.getStorageItem() != null ? "ID" : "Unknown");
                    }

                    vm.setStorageItemName(itemName);
                    vm.setQuantityNeeded(element.getQuantityNeeded() != null ? element.getQuantityNeeded() : 0);
                    vm.setQuantityAllocated(element.getQuantityAllocated() != null ? element.getQuantityAllocated() : 0);
                    vm.setStatus(element.getStatus() != null ? element.getStatus() : "Pending");
                    vm.setNotes(element.getNotes() != null ? element.getNotes() : "");
                    elementItems.add(vm);
                    System.out.println("  ✓ Added: " + vm.getStorageItemName());
                } catch (Exception ex) {
                    System.out.println("❌ Error processing element " + element.getId() + ": " + ex.getMessage());
                    ex.printStackTrace();
                }
            }

            System.out.println("📦 Total view models created: " + elementItems.size());

            Platform.runLater(() -> {
                System.out.println("📦 Platform.runLater executing...");
                System.out.println("📦 Elements grid is: " + (elementsGrid != null ? "NOT NULL" : "NULL"));

                // ✅ Use stored reference
                if (elementsGrid != null) {
                    System.out.println("📦 Clearing elements grid...");
                    elementsGrid.getChildren().clear();

                    if (elementItems.isEmpty()) {
                        System.out.println("📦 Showing empty state");
                        // Show empty state
                        VBox emptyState = new VBox(20);
                        emptyState.setAlignment(Pos.CENTER);
                        emptyState.setPadding(new Insets(100));

                        Label emptyIcon = new Label("📦");
                        emptyIcon.setStyle("-fx-font-size: 64px;");

                        Label emptyText = new Label("No Elements Added Yet");
                        emptyText.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.6); -fx-font-size: 18px;");

                        Label emptyHint = new Label("Click '+ Add Element' to add items to this project");
                        emptyHint.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.4); -fx-font-size: 14px;");

                        emptyState.getChildren().addAll(emptyIcon, emptyText, emptyHint);
                        elementsGrid.getChildren().add(emptyState);
                        System.out.println("📦 Empty state added to grid");
                    } else {
                        System.out.println("📦 Creating " + elementItems.size() + " cards...");
                        // Create cards for each element
                        int cardCount = 0;
                        for (ProjectElementViewModel vm : elementItems) {
                            VBox card = createElementCard(vm);
                            elementsGrid.getChildren().add(card);
                            cardCount++;
                            System.out.println("  ✓ Card " + cardCount + " added: " + vm.getStorageItemName());
                        }
                        System.out.println("📦 All " + cardCount + " cards added to grid");
                        System.out.println("📦 Grid children count: " + elementsGrid.getChildren().size());
                    }
                } else {
                    System.out.println("❌ ERROR: elementsGrid is NULL - cannot display elements!");
                }
                System.out.println("✓ Loaded " + elementItems.size() + " elements for project: " + selectedProject.getProjectName());
            });
        });

        loadTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                Throwable exception = loadTask.getException();
                System.out.println("❌ ERROR loading elements: " + exception.getMessage());
                showError("Failed to load elements: " + exception.getMessage());
                exception.printStackTrace();
            });
        });

        new Thread(loadTask).start();
    }

    // ==================== NAVIGATION ====================

    private void openProjectWorkspace(Project project) {
        this.selectedProject = project;
        projectTitleLabel.setText("📋 " + project.getProjectName());

        loadScheduleData();
        loadTasksData();

        // ✅ CRITICAL: Initialize elements grid if not already done
        if (elementsGrid == null) {
            System.out.println("⚠️ WARNING: elementsGrid was null, creating it now");
            // This shouldn't happen, but create it as fallback
            elementsGrid = new FlowPane();
            elementsGrid.setHgap(20);
            elementsGrid.setVgap(20);
            elementsGrid.setPadding(new Insets(10));
            elementsGrid.setStyle("-fx-background-color: transparent;");
        }

        loadElementsData();

        mainContainer.getChildren().remove(projectSelectionScreen);
        projectWorkspaceScreen.setVisible(true);

        System.out.println("✓ Opened project: " + project.getProjectName());
        System.out.println("✓ Elements grid reference: " + (elementsGrid != null ? "OK" : "NULL"));
    }

    private void backToProjectSelection() {
        this.selectedProject = null;
        projectWorkspaceScreen.setVisible(false);
        if (!mainContainer.getChildren().contains(projectSelectionScreen)) {
            mainContainer.getChildren().add(0, projectSelectionScreen);
        }
    }

    private void navigateToDashboard() {
        try {
            com.magictech.core.ui.SceneManager.getInstance().showMainDashboard();
        } catch (Exception e) {
            System.out.println("Could not navigate to dashboard, error: " + e.getMessage());
            showError("Navigation error: " + e.getMessage());
        }
    }

    // ==================== UTILITY METHODS ====================

    private Button createStyledButton(String text, String normalColor, String hoverColor) {
        Button button = new Button(text);
        button.setStyle(
                "-fx-background-color: " + normalColor + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 12 24;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 8, 0, 0, 2);"
        );

        button.setOnMouseEntered(e -> button.setStyle(
                "-fx-background-color: " + hoverColor + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 12 24;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.5), 12, 0, 0, 4);" +
                        "-fx-scale-x: 1.05;" +
                        "-fx-scale-y: 1.05;"
        ));

        button.setOnMouseExited(e -> button.setStyle(
                "-fx-background-color: " + normalColor + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 12 24;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 8, 0, 0, 2);"
        ));

        return button;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public void immediateCleanup() {
        if (selectedProject != null) {
            selectedProject = null;
        }

        if (scheduleItems != null) {
            scheduleItems.clear();
        }
        if (elementItems != null) {
            elementItems.clear();
        }
        if (tasksContainer != null) {
            tasksContainer.getChildren().clear();
        }

        if (projectWorkspaceScreen != null && projectWorkspaceScreen.isVisible()) {
            backToProjectSelection();
        }

        System.out.println("✓ ProjectsStorageController cleanup completed");
    }
}