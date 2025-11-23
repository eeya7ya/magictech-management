package com.magictech.modules.projects;

import com.magictech.core.auth.User;
import com.magictech.core.module.BaseModuleController;
import com.magictech.modules.projects.entity.Project;
import com.magictech.modules.projects.service.ProjectService;
import com.magictech.modules.sales.entity.ProjectWorkflow;
import com.magictech.modules.sales.entity.SiteSurveyData;
import com.magictech.modules.sales.entity.WorkflowStepCompletion;
import com.magictech.modules.sales.repository.SiteSurveyDataRepository;
import com.magictech.modules.sales.service.ProjectWorkflowService;
import com.magictech.modules.sales.service.SiteSurveyExcelService;
import com.magictech.modules.sales.service.WorkflowStepService;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Projects Module Controller - Complete Implementation
 * Handles project dashboard, site survey management, workflow integration
 */
public class ProjectsController extends BaseModuleController {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectWorkflowService workflowService;

    @Autowired
    private WorkflowStepService stepService;

    @Autowired
    private SiteSurveyDataRepository siteSurveyRepository;

    @Autowired
    private SiteSurveyExcelService excelService;

    private VBox contentArea;
    private TableView<ProjectViewModel> projectTable;
    private ObservableList<ProjectViewModel> projectData;
    private TextField searchField;
    private Label pendingCountLabel;
    private VBox pendingActionsPanel;

    @Override
    protected void setupUI() {
        VBox header = createHeader();
        VBox toolbar = createToolbar();

        // Pending actions panel (collapsible)
        pendingActionsPanel = createPendingActionsPanel();

        // Projects table
        projectTable = createProjectTable();

        contentArea = new VBox(15);
        contentArea.setPadding(new Insets(20));
        contentArea.getChildren().addAll(toolbar, pendingActionsPanel, projectTable);
        VBox.setVgrow(projectTable, Priority.ALWAYS);

        getRootPane().setTop(header);
        getRootPane().setCenter(contentArea);
    }

    private VBox createHeader() {
        VBox header = new VBox(10);
        header.setPadding(new Insets(20, 30, 20, 30));
        header.setStyle("-fx-background-color: linear-gradient(to right, #a855f7, #9333ea);");

        Label title = new Label("Project Team Module");
        title.setFont(Font.font("System", FontWeight.BOLD, 24));
        title.setStyle("-fx-text-fill: white;");

        Label subtitle = new Label("Site Surveys | Workflow Requests | Project Management");
        subtitle.setFont(Font.font("System", 14));
        subtitle.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.9);");

        header.getChildren().addAll(title, subtitle);
        return header;
    }

    private VBox createToolbar() {
        VBox toolbar = new VBox(10);

        // Search bar
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);

        Label searchLabel = new Label("🔍 Search:");
        searchLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        searchField = new TextField();
        searchField.setPromptText("Search by project name, location, status...");
        searchField.setPrefWidth(400);
        searchField.textProperty().addListener((obs, old, newVal) -> filterProjects());

        searchBox.getChildren().addAll(searchLabel, searchField);

        // Action buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);

        Button refreshButton = new Button("🔄 Refresh");
        refreshButton.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16;");
        refreshButton.setOnAction(e -> refresh());

        Button viewAllButton = new Button("📋 View All Projects");
        viewAllButton.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16;");
        viewAllButton.setOnAction(e -> {
            searchField.clear();
            refresh();
        });

        pendingCountLabel = new Label("⚠️ Pending Actions: 0");
        pendingCountLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        pendingCountLabel.setStyle("-fx-text-fill: #dc2626; -fx-padding: 8 16; -fx-background-color: #fee2e2; -fx-background-radius: 6;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        buttonBox.getChildren().addAll(refreshButton, viewAllButton, spacer, pendingCountLabel);

        toolbar.getChildren().addAll(searchBox, buttonBox);
        return toolbar;
    }

    private VBox createPendingActionsPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(15));
        panel.setStyle("-fx-background-color: #fef3c7; -fx-border-color: #f59e0b; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;");
        panel.setVisible(false); // Hidden by default, shown when there are pending actions

        Label title = new Label("⚡ Pending Site Survey Requests");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));
        title.setStyle("-fx-text-fill: #92400e;");

        panel.getChildren().add(title);
        return panel;
    }

    private TableView<ProjectViewModel> createProjectTable() {
        TableView<ProjectViewModel> table = new TableView<>();
        table.setPlaceholder(new Label("No projects found. All projects with workflow requests will appear here."));

        // ID column
        TableColumn<ProjectViewModel, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> data.getValue().idProperty());
        idCol.setPrefWidth(60);

        // Project Name column
        TableColumn<ProjectViewModel, String> nameCol = new TableColumn<>("Project Name");
        nameCol.setCellValueFactory(data -> data.getValue().projectNameProperty());
        nameCol.setPrefWidth(250);

        // Location column
        TableColumn<ProjectViewModel, String> locationCol = new TableColumn<>("Location");
        locationCol.setCellValueFactory(data -> data.getValue().projectLocationProperty());
        locationCol.setPrefWidth(200);

        // Status column
        TableColumn<ProjectViewModel, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> data.getValue().statusProperty());
        statusCol.setPrefWidth(120);
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    String color = switch (status) {
                        case "COMPLETED" -> "-fx-background-color: #d1fae5; -fx-text-fill: #065f46;";
                        case "IN_PROGRESS" -> "-fx-background-color: #dbeafe; -fx-text-fill: #1e40af;";
                        case "ON_HOLD" -> "-fx-background-color: #fee2e2; -fx-text-fill: #991b1b;";
                        default -> "-fx-background-color: #f3f4f6; -fx-text-fill: #374151;";
                    };
                    setStyle(color + "-fx-padding: 4 8; -fx-background-radius: 4;");
                }
            }
        });

        // Workflow Step column
        TableColumn<ProjectViewModel, String> workflowCol = new TableColumn<>("Workflow Step");
        workflowCol.setCellValueFactory(data -> data.getValue().workflowStepProperty());
        workflowCol.setPrefWidth(150);

        // Site Survey Status column
        TableColumn<ProjectViewModel, String> surveyCol = new TableColumn<>("Site Survey");
        surveyCol.setCellValueFactory(data -> data.getValue().siteSurveyStatusProperty());
        surveyCol.setPrefWidth(150);
        surveyCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String surveyStatus, boolean empty) {
                super.updateItem(surveyStatus, empty);
                if (empty || surveyStatus == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(surveyStatus);
                    String color = switch (surveyStatus) {
                        case "✅ Completed" -> "-fx-background-color: #d1fae5; -fx-text-fill: #065f46;";
                        case "⚠️ REQUESTED" -> "-fx-background-color: #fef3c7; -fx-text-fill: #92400e;";
                        case "❌ Pending" -> "-fx-background-color: #fee2e2; -fx-text-fill: #991b1b;";
                        default -> "-fx-text-fill: #6b7280;";
                    };
                    setStyle(color + "-fx-font-weight: bold; -fx-padding: 4 8; -fx-background-radius: 4;");
                }
            }
        });

        // Actions column
        TableColumn<ProjectViewModel, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(350);
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button viewButton = new Button("👁️ View");
            private final Button uploadButton = new Button("📤 Upload Survey");
            private final Button downloadButton = new Button("💾 Download");

            {
                viewButton.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 4 8;");
                uploadButton.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 4 8;");
                downloadButton.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 4 8;");

                viewButton.setOnAction(e -> {
                    ProjectViewModel vm = getTableView().getItems().get(getIndex());
                    viewProjectDetails(vm);
                });

                uploadButton.setOnAction(e -> {
                    ProjectViewModel vm = getTableView().getItems().get(getIndex());
                    uploadSiteSurvey(vm);
                });

                downloadButton.setOnAction(e -> {
                    ProjectViewModel vm = getTableView().getItems().get(getIndex());
                    downloadSiteSurvey(vm);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    ProjectViewModel vm = getTableView().getItems().get(getIndex());
                    HBox buttons = new HBox(5);
                    buttons.setAlignment(Pos.CENTER_LEFT);

                    buttons.getChildren().add(viewButton);

                    // Show upload button if survey requested or pending
                    if ("⚠️ REQUESTED".equals(vm.getSiteSurveyStatus()) || "❌ Pending".equals(vm.getSiteSurveyStatus())) {
                        buttons.getChildren().add(uploadButton);
                    }

                    // Show download button if survey exists
                    if ("✅ Completed".equals(vm.getSiteSurveyStatus())) {
                        buttons.getChildren().add(downloadButton);
                    }

                    setGraphic(buttons);
                }
            }
        });

        table.getColumns().addAll(idCol, nameCol, locationCol, statusCol, workflowCol, surveyCol, actionsCol);
        return table;
    }

    @Override
    protected void loadData() {
        try {
            List<Project> projects = projectService.getAllProjects();
            projectData = FXCollections.observableArrayList();

            int pendingCount = 0;
            pendingActionsPanel.getChildren().clear();
            Label title = new Label("⚡ Pending Site Survey Requests");
            title.setFont(Font.font("System", FontWeight.BOLD, 16));
            title.setStyle("-fx-text-fill: #92400e;");
            pendingActionsPanel.getChildren().add(title);

            for (Project project : projects) {
                ProjectViewModel vm = new ProjectViewModel(project);

                // Get workflow if exists
                Optional<ProjectWorkflow> workflowOpt = workflowService.getWorkflowByProjectId(project.getId());
                if (workflowOpt.isPresent()) {
                    ProjectWorkflow workflow = workflowOpt.get();
                    vm.setWorkflowStep("Step " + workflow.getCurrentStep() + "/8");

                    // Check site survey status (Step 1)
                    Optional<WorkflowStepCompletion> step1 = stepService.getStep(workflow.getId(), 1);
                    if (step1.isPresent()) {
                        WorkflowStepCompletion stepComp = step1.get();

                        // Check if there's a site survey data
                        Optional<SiteSurveyData> surveyOpt = siteSurveyRepository.findByProjectIdAndActiveTrue(project.getId());

                        if (surveyOpt.isPresent()) {
                            vm.setSiteSurveyStatus("✅ Completed");
                            vm.setHasSiteSurvey(true);
                        } else if (Boolean.TRUE.equals(stepComp.getNeedsExternalAction()) &&
                                   "PROJECT".equals(stepComp.getExternalModule()) &&
                                   !Boolean.TRUE.equals(stepComp.getExternalActionCompleted())) {
                            vm.setSiteSurveyStatus("⚠️ REQUESTED");
                            vm.setHasSiteSurvey(false);
                            pendingCount++;

                            // Add to pending panel
                            addPendingActionCard(project, workflow);
                        } else {
                            vm.setSiteSurveyStatus("❌ Pending");
                            vm.setHasSiteSurvey(false);
                        }
                    }
                } else {
                    vm.setWorkflowStep("No Workflow");
                    vm.setSiteSurveyStatus("-");
                }

                projectData.add(vm);
            }

            projectTable.setItems(projectData);

            // Update pending count
            pendingCountLabel.setText("⚠️ Pending Actions: " + pendingCount);
            pendingActionsPanel.setVisible(pendingCount > 0);

            System.out.println("✅ Loaded " + projects.size() + " projects with " + pendingCount + " pending site survey requests");
        } catch (Exception ex) {
            showError("Failed to load projects: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void addPendingActionCard(Project project, ProjectWorkflow workflow) {
        HBox card = new HBox(15);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: white; -fx-border-color: #f59e0b; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;");
        card.setAlignment(Pos.CENTER_LEFT);

        VBox info = new VBox(5);
        Label projectLabel = new Label("📁 " + project.getProjectName());
        projectLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        projectLabel.setStyle("-fx-text-fill: #1f2937;");

        Label requestLabel = new Label("Requested by Sales - Site Survey Needed");
        requestLabel.setFont(Font.font("System", 11));
        requestLabel.setStyle("-fx-text-fill: #6b7280;");

        info.getChildren().addAll(projectLabel, requestLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button uploadBtn = new Button("📤 Upload Survey");
        uploadBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16;");
        uploadBtn.setOnAction(e -> {
            ProjectViewModel vm = projectData.stream()
                .filter(p -> p.getId().equals(project.getId()))
                .findFirst()
                .orElse(null);
            if (vm != null) {
                uploadSiteSurvey(vm);
            }
        });

        card.getChildren().addAll(info, spacer, uploadBtn);
        pendingActionsPanel.getChildren().add(card);
    }

    private void viewProjectDetails(ProjectViewModel vm) {
        Stage detailStage = new Stage();
        detailStage.initModality(Modality.APPLICATION_MODAL);
        detailStage.setTitle("Project Details - " + vm.getProjectName());

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #f9fafb;");

        // Header
        Label header = new Label("📋 " + vm.getProjectName());
        header.setFont(Font.font("System", FontWeight.BOLD, 20));
        header.setStyle("-fx-text-fill: #1f2937;");

        // Project info
        VBox infoBox = createInfoBox(vm);

        // Site Survey section
        VBox surveyBox = createSiteSurveySection(vm);

        // Action buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        if ("⚠️ REQUESTED".equals(vm.getSiteSurveyStatus()) || "❌ Pending".equals(vm.getSiteSurveyStatus())) {
            Button uploadBtn = new Button("📤 Upload Site Survey");
            uploadBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
            uploadBtn.setOnAction(e -> {
                detailStage.close();
                uploadSiteSurvey(vm);
            });
            buttonBox.getChildren().add(uploadBtn);
        }

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: #6b7280; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
        closeBtn.setOnAction(e -> detailStage.close());
        buttonBox.getChildren().add(closeBtn);

        content.getChildren().addAll(header, new Separator(), infoBox, surveyBox, buttonBox);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #f9fafb;");

        Scene scene = new Scene(scroll, 700, 600);
        detailStage.setScene(scene);
        detailStage.show();
    }

    private VBox createInfoBox(ProjectViewModel vm) {
        VBox box = new VBox(10);
        box.setPadding(new Insets(15));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        Label title = new Label("ℹ️ Project Information");
        title.setFont(Font.font("System", FontWeight.BOLD, 14));

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);

        addInfoRow(grid, 0, "Location:", vm.getProjectLocation());
        addInfoRow(grid, 1, "Status:", vm.getStatus());
        addInfoRow(grid, 2, "Workflow:", vm.getWorkflowStep());
        addInfoRow(grid, 3, "Site Survey:", vm.getSiteSurveyStatus());

        box.getChildren().addAll(title, grid);
        return box;
    }

    private void addInfoRow(GridPane grid, int row, String label, String value) {
        Label labelNode = new Label(label);
        labelNode.setFont(Font.font("System", FontWeight.BOLD, 12));
        labelNode.setStyle("-fx-text-fill: #6b7280;");

        Label valueNode = new Label(value != null ? value : "-");
        valueNode.setFont(Font.font("System", 12));
        valueNode.setStyle("-fx-text-fill: #1f2937;");

        grid.add(labelNode, 0, row);
        grid.add(valueNode, 1, row);
    }

    private VBox createSiteSurveySection(ProjectViewModel vm) {
        VBox box = new VBox(15);
        box.setPadding(new Insets(15));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        Label title = new Label("📊 Site Survey Data");
        title.setFont(Font.font("System", FontWeight.BOLD, 14));

        if (vm.isHasSiteSurvey()) {
            // Show survey info
            try {
                Optional<SiteSurveyData> surveyOpt = siteSurveyRepository.findByProjectIdAndActiveTrue(vm.getId());
                if (surveyOpt.isPresent()) {
                    SiteSurveyData survey = surveyOpt.get();

                    VBox surveyInfo = new VBox(8);
                    surveyInfo.setPadding(new Insets(10));
                    surveyInfo.setStyle("-fx-background-color: #f0fdf4; -fx-border-color: #10b981; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;");

                    Label fileLabel = new Label("📄 File: " + survey.getFileName());
                    fileLabel.setFont(Font.font("System", FontWeight.BOLD, 12));

                    Label uploadedBy = new Label("👤 Uploaded by: " + survey.getUploadedBy());
                    uploadedBy.setFont(Font.font("System", 11));

                    Label uploadedAt = new Label("📅 Uploaded: " + formatDateTime(survey.getUploadedAt()));
                    uploadedAt.setFont(Font.font("System", 11));

                    Label surveyBy = new Label("🔍 Survey done by: " + survey.getSurveyDoneBy() + " (" + survey.getSurveyDoneByUser() + ")");
                    surveyBy.setFont(Font.font("System", 11));

                    if (survey.getNotes() != null && !survey.getNotes().isEmpty()) {
                        Label notes = new Label("📝 Notes: " + survey.getNotes());
                        notes.setFont(Font.font("System", 11));
                        notes.setWrapText(true);
                        surveyInfo.getChildren().add(notes);
                    }

                    surveyInfo.getChildren().addAll(fileLabel, uploadedBy, uploadedAt, surveyBy);

                    HBox actionButtons = new HBox(10);
                    actionButtons.setAlignment(Pos.CENTER_LEFT);

                    Button viewDataBtn = new Button("👁️ View Survey Data");
                    viewDataBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16;");
                    viewDataBtn.setOnAction(e -> viewSiteSurveyData(survey));

                    Button downloadBtn = new Button("💾 Download Excel File");
                    downloadBtn.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16;");
                    downloadBtn.setOnAction(e -> downloadSiteSurvey(vm));

                    actionButtons.getChildren().addAll(viewDataBtn, downloadBtn);

                    box.getChildren().addAll(title, surveyInfo, actionButtons);
                }
            } catch (Exception ex) {
                Label error = new Label("❌ Error loading survey: " + ex.getMessage());
                error.setStyle("-fx-text-fill: #dc2626;");
                box.getChildren().addAll(title, error);
            }
        } else {
            Label noSurvey = new Label("⚠️ No site survey data available yet");
            noSurvey.setStyle("-fx-text-fill: #92400e; -fx-font-size: 12px;");
            box.getChildren().addAll(title, noSurvey);
        }

        return box;
    }

    private void uploadSiteSurvey(ProjectViewModel vm) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Upload Site Survey Excel");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls")
        );

        File file = fileChooser.showOpenDialog(getRootPane().getScene().getWindow());
        if (file != null) {
            try {
                byte[] fileData = Files.readAllBytes(file.toPath());

                // Get workflow
                Optional<ProjectWorkflow> workflowOpt = workflowService.getWorkflowByProjectId(vm.getId());
                if (workflowOpt.isPresent()) {
                    // Submit site survey from project team
                    workflowService.submitSiteSurveyFromProject(
                        workflowOpt.get().getId(),
                        fileData,
                        file.getName(),
                        currentUser
                    );

                    showSuccess("Site survey uploaded successfully! Sales team has been notified.");
                    refresh();
                } else {
                    showError("No workflow found for this project. Please contact Sales team.");
                }
            } catch (Exception ex) {
                showError("Failed to upload site survey: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    private void downloadSiteSurvey(ProjectViewModel vm) {
        try {
            Optional<SiteSurveyData> surveyOpt = siteSurveyRepository.findByProjectIdAndActiveTrue(vm.getId());
            if (surveyOpt.isEmpty()) {
                showWarning("No site survey available for this project");
                return;
            }

            SiteSurveyData survey = surveyOpt.get();

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
                }
                showSuccess("Site survey downloaded successfully!");
            }
        } catch (Exception ex) {
            showError("Failed to download site survey: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void viewSiteSurveyData(SiteSurveyData survey) {
        Stage dataStage = new Stage();
        dataStage.initModality(Modality.APPLICATION_MODAL);
        dataStage.setTitle("Site Survey Data - " + survey.getFileName());

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #f9fafb;");

        // Header
        Label header = new Label("📊 Site Survey Data");
        header.setFont(Font.font("System", FontWeight.BOLD, 20));
        header.setStyle("-fx-text-fill: #1f2937;");

        // Metadata box
        VBox metadataBox = new VBox(10);
        metadataBox.setPadding(new Insets(15));
        metadataBox.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        Label metadataTitle = new Label("ℹ️ Survey Information");
        metadataTitle.setFont(Font.font("System", FontWeight.BOLD, 14));

        GridPane metaGrid = new GridPane();
        metaGrid.setHgap(15);
        metaGrid.setVgap(10);
        addInfoRow(metaGrid, 0, "File Name:", survey.getFileName());
        addInfoRow(metaGrid, 1, "File Size:", String.format("%.2f KB", survey.getFileSize() / 1024.0));
        addInfoRow(metaGrid, 2, "Uploaded By:", survey.getUploadedBy());
        addInfoRow(metaGrid, 3, "Uploaded At:", formatDateTime(survey.getUploadedAt()));
        addInfoRow(metaGrid, 4, "Survey By:", survey.getSurveyDoneBy() + " (" + survey.getSurveyDoneByUser() + ")");

        metadataBox.getChildren().addAll(metadataTitle, metaGrid);

        // Parsed data display
        VBox dataBox = new VBox(10);
        dataBox.setPadding(new Insets(15));
        dataBox.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        Label dataTitle = new Label("📄 Parsed Excel Data");
        dataTitle.setFont(Font.font("System", FontWeight.BOLD, 14));

        TextArea dataTextArea = new TextArea();
        dataTextArea.setWrapText(true);
        dataTextArea.setEditable(false);
        dataTextArea.setPrefRowCount(20);
        dataTextArea.setStyle("-fx-font-family: 'Monospaced'; -fx-font-size: 11px;");

        if (survey.getParsedData() != null && !survey.getParsedData().isEmpty()) {
            try {
                // Pretty print JSON
                dataTextArea.setText(formatJsonForDisplay(survey.getParsedData()));
            } catch (Exception e) {
                dataTextArea.setText(survey.getParsedData());
            }
        } else {
            dataTextArea.setText("No parsed data available");
        }

        dataBox.getChildren().addAll(dataTitle, dataTextArea);
        VBox.setVgrow(dataTextArea, Priority.ALWAYS);

        // Action buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button copyButton = new Button("📋 Copy to Clipboard");
        copyButton.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
        copyButton.setOnAction(e -> {
            if (survey.getParsedData() != null) {
                javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                javafx.scene.input.ClipboardContent clipContent = new javafx.scene.input.ClipboardContent();
                clipContent.putString(survey.getParsedData());
                clipboard.setContent(clipContent);
                showSuccess("Data copied to clipboard!");
            }
        });

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: #6b7280; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
        closeBtn.setOnAction(e -> dataStage.close());

        buttonBox.getChildren().addAll(copyButton, closeBtn);

        content.getChildren().addAll(header, new Separator(), metadataBox, dataBox, buttonBox);
        VBox.setVgrow(dataBox, Priority.ALWAYS);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #f9fafb;");

        Scene scene = new Scene(scroll, 900, 700);
        dataStage.setScene(scene);
        dataStage.show();
    }

    private String formatJsonForDisplay(String json) {
        // Simple JSON pretty-print: add newlines and indentation
        StringBuilder result = new StringBuilder();
        int indent = 0;
        boolean inQuotes = false;

        for (int i = 0; i < json.length(); i++) {
            char ch = json.charAt(i);

            if (ch == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            }

            if (!inQuotes) {
                switch (ch) {
                    case '{':
                    case '[':
                        result.append(ch).append('\n');
                        indent++;
                        result.append("  ".repeat(indent));
                        break;
                    case '}':
                    case ']':
                        result.append('\n');
                        indent--;
                        result.append("  ".repeat(indent));
                        result.append(ch);
                        break;
                    case ',':
                        result.append(ch).append('\n');
                        result.append("  ".repeat(indent));
                        break;
                    case ':':
                        result.append(ch).append(' ');
                        break;
                    default:
                        result.append(ch);
                }
            } else {
                result.append(ch);
            }
        }

        return result.toString();
    }

    private void filterProjects() {
        String searchTerm = searchField.getText().toLowerCase();
        if (searchTerm.isEmpty()) {
            projectTable.setItems(projectData);
        } else {
            ObservableList<ProjectViewModel> filtered = projectData.filtered(vm ->
                vm.getProjectName().toLowerCase().contains(searchTerm) ||
                (vm.getProjectLocation() != null && vm.getProjectLocation().toLowerCase().contains(searchTerm)) ||
                (vm.getStatus() != null && vm.getStatus().toLowerCase().contains(searchTerm))
            );
            projectTable.setItems(filtered);
        }
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "-";
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    @Override
    public void refresh() {
        System.out.println("🔄 Refreshing projects module data");
        loadData();
    }

    /**
     * ViewModel for Project table binding
     */
    public static class ProjectViewModel {
        private final LongProperty id = new SimpleLongProperty();
        private final StringProperty projectName = new SimpleStringProperty();
        private final StringProperty projectLocation = new SimpleStringProperty();
        private final StringProperty status = new SimpleStringProperty();
        private final StringProperty workflowStep = new SimpleStringProperty("-");
        private final StringProperty siteSurveyStatus = new SimpleStringProperty("-");
        private final BooleanProperty hasSiteSurvey = new SimpleBooleanProperty(false);

        public ProjectViewModel(Project project) {
            this.id.set(project.getId());
            this.projectName.set(project.getProjectName());
            this.projectLocation.set(project.getProjectLocation());
            this.status.set(project.getStatus() != null ? project.getStatus() : "Planning");
        }

        public Long getId() { return id.get(); }
        public LongProperty idProperty() { return id; }

        public String getProjectName() { return projectName.get(); }
        public StringProperty projectNameProperty() { return projectName; }

        public String getProjectLocation() { return projectLocation.get(); }
        public StringProperty projectLocationProperty() { return projectLocation; }

        public String getStatus() { return status.get(); }
        public StringProperty statusProperty() { return status; }

        public String getWorkflowStep() { return workflowStep.get(); }
        public StringProperty workflowStepProperty() { return workflowStep; }
        public void setWorkflowStep(String step) { workflowStep.set(step); }

        public String getSiteSurveyStatus() { return siteSurveyStatus.get(); }
        public StringProperty siteSurveyStatusProperty() { return siteSurveyStatus; }
        public void setSiteSurveyStatus(String status) { siteSurveyStatus.set(status); }

        public boolean isHasSiteSurvey() { return hasSiteSurvey.get(); }
        public BooleanProperty hasSiteSurveyProperty() { return hasSiteSurvey; }
        public void setHasSiteSurvey(boolean has) { hasSiteSurvey.set(has); }
    }
}
