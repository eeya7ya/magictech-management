package com.magictech.modules.storage;

import com.magictech.modules.projects.entity.*;
import com.magictech.modules.projects.service.*;
import com.magictech.modules.sales.entity.*;
import com.magictech.modules.sales.service.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Analysis Dashboard Controller - READ-ONLY
 * Allows Storage module users to view project/customer details for analysis
 * NO EDITING ALLOWED - Pure read-only view
 */
@Component
public class AnalysisDashboardController {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectElementService projectElementService;

    @Autowired
    private ProjectTaskService projectTaskService;

    @Autowired
    private ProjectScheduleService projectScheduleService;

    @Autowired
    private ProjectDocumentService projectDocumentService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerElementService customerElementService;

    @Autowired
    private CustomerTaskService customerTaskService;

    @Autowired
    private CustomerScheduleService customerScheduleService;

    @Autowired
    private CustomerDocumentService customerDocumentService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Create embedded analysis dashboard view (no modal window)
     */
    public VBox createEmbeddedView() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: transparent;");

        // Header
        Label titleLabel = new Label("📊 ANALYSIS DASHBOARD");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: bold;");

        Label subtitleLabel = new Label("View Project & Customer Details (Read-Only)");
        subtitleLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 16px;");

        VBox header = new VBox(10, titleLabel, subtitleLabel);
        header.setAlignment(Pos.CENTER);

        // Selection tabs
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color: transparent;");

        Tab projectsTab = new Tab("📁 Projects");
        projectsTab.setContent(createProjectsViewEmbedded());

        Tab customersTab = new Tab("👥 Customers");
        customersTab.setContent(createCustomersViewEmbedded());

        tabPane.getTabs().addAll(projectsTab, customersTab);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        root.getChildren().addAll(header, tabPane);

        return root;
    }

    /**
     * Show analysis dashboard window (DEPRECATED - use createEmbeddedView instead)
     */
    @Deprecated
    public void show() {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Analysis Dashboard - Storage Management");

        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #0f172a, #1e293b);");

        // Header
        Label titleLabel = new Label("📊 ANALYSIS DASHBOARD");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: bold;");

        Label subtitleLabel = new Label("View Project & Customer Details (Read-Only)");
        subtitleLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 16px;");

        VBox header = new VBox(10, titleLabel, subtitleLabel);
        header.setAlignment(Pos.CENTER);

        // Selection tabs
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color: transparent;");

        Tab projectsTab = new Tab("📁 Projects");
        projectsTab.setContent(createProjectsView(stage));

        Tab customersTab = new Tab("👥 Customers");
        customersTab.setContent(createCustomersView(stage));

        tabPane.getTabs().addAll(projectsTab, customersTab);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        // Close button
        Button closeButton = new Button("Close Dashboard");
        closeButton.setStyle(
                "-fx-background-color: #6b7280;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 12 30;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        );
        closeButton.setOnAction(e -> stage.close());

        HBox buttonBox = new HBox(closeButton);
        buttonBox.setAlignment(Pos.CENTER);

        root.getChildren().addAll(header, tabPane, buttonBox);

        Scene scene = new Scene(root, 1000, 700);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Create embedded projects view (no Stage reference)
     */
    private VBox createProjectsViewEmbedded() {
        VBox view = new VBox(15);
        view.setPadding(new Insets(20));
        view.setStyle("-fx-background-color: rgba(30, 41, 59, 0.6); -fx-background-radius: 12;");

        Label infoLabel = new Label("ℹ️ Select a project to view its complete details (READ-ONLY)");
        infoLabel.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 14px; -fx-font-weight: bold;");

        // Projects list
        ListView<Project> projectsListView = new ListView<>();
        projectsListView.setPrefHeight(400);
        projectsListView.setStyle("-fx-background-color: rgba(51, 65, 85, 0.8);");

        projectsListView.setCellFactory(lv -> new ListCell<Project>() {
            @Override
            protected void updateItem(Project project, boolean empty) {
                super.updateItem(project, empty);
                if (empty || project == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox cellContent = new VBox(5);
                    cellContent.setPadding(new Insets(10));

                    Label nameLabel = new Label("📁 " + project.getProjectName());
                    nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

                    Label detailsLabel = new Label(
                            "Location: " + (project.getProjectLocation() != null ? project.getProjectLocation() : "N/A") +
                                    " • Status: " + (project.getStatus() != null ? project.getStatus() : "ACTIVE")
                    );
                    detailsLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 12px;");

                    cellContent.getChildren().addAll(nameLabel, detailsLabel);
                    setGraphic(cellContent);
                    setStyle("-fx-background-color: transparent;");
                }
            }
        });

        // Load projects
        loadProjects(projectsListView);

        // View button
        Button viewButton = new Button("👁️ View Details");
        viewButton.setStyle(
                "-fx-background-color: #3b82f6;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 10 20;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        );
        viewButton.setOnAction(e -> {
            Project selected = projectsListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showProjectDetails(selected);
            } else {
                showAlert("Please select a project first");
            }
        });

        VBox.setVgrow(projectsListView, Priority.ALWAYS);
        view.getChildren().addAll(infoLabel, projectsListView, viewButton);

        return view;
    }

    /**
     * Create embedded customers view (no Stage reference)
     */
    private VBox createCustomersViewEmbedded() {
        VBox view = new VBox(15);
        view.setPadding(new Insets(20));
        view.setStyle("-fx-background-color: rgba(30, 41, 59, 0.6); -fx-background-radius: 12;");

        Label infoLabel = new Label("ℹ️ Select a customer to view their complete details (READ-ONLY)");
        infoLabel.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 14px; -fx-font-weight: bold;");

        // Customers list
        ListView<Customer> customersListView = new ListView<>();
        customersListView.setPrefHeight(400);
        customersListView.setStyle("-fx-background-color: rgba(51, 65, 85, 0.8);");

        customersListView.setCellFactory(lv -> new ListCell<Customer>() {
            @Override
            protected void updateItem(Customer customer, boolean empty) {
                super.updateItem(customer, empty);
                if (empty || customer == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox cellContent = new VBox(5);
                    cellContent.setPadding(new Insets(10));

                    Label nameLabel = new Label("👤 " + customer.getName());
                    nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

                    Label detailsLabel = new Label(
                            "Email: " + (customer.getEmail() != null ? customer.getEmail() : "N/A") +
                                    " • Phone: " + (customer.getPhone() != null ? customer.getPhone() : "N/A")
                    );
                    detailsLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 12px;");

                    cellContent.getChildren().addAll(nameLabel, detailsLabel);
                    setGraphic(cellContent);
                    setStyle("-fx-background-color: transparent;");
                }
            }
        });

        // Load customers
        loadCustomers(customersListView);

        // View button
        Button viewButton = new Button("👁️ View Details");
        viewButton.setStyle(
                "-fx-background-color: #3b82f6;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 10 20;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        );
        viewButton.setOnAction(e -> {
            Customer selected = customersListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showCustomerDetails(selected);
            } else {
                showAlert("Please select a customer first");
            }
        });

        VBox.setVgrow(customersListView, Priority.ALWAYS);
        view.getChildren().addAll(infoLabel, customersListView, viewButton);

        return view;
    }

    /**
     * Create projects view
     */
    private VBox createProjectsView(Stage parentStage) {
        VBox view = new VBox(15);
        view.setPadding(new Insets(20));
        view.setStyle("-fx-background-color: rgba(30, 41, 59, 0.6); -fx-background-radius: 12;");

        Label infoLabel = new Label("ℹ️ Select a project to view its complete details (READ-ONLY)");
        infoLabel.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 14px; -fx-font-weight: bold;");

        // Projects list
        ListView<Project> projectsListView = new ListView<>();
        projectsListView.setPrefHeight(400);
        projectsListView.setStyle("-fx-background-color: rgba(51, 65, 85, 0.8);");

        projectsListView.setCellFactory(lv -> new ListCell<Project>() {
            @Override
            protected void updateItem(Project project, boolean empty) {
                super.updateItem(project, empty);
                if (empty || project == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox cellContent = new VBox(5);
                    cellContent.setPadding(new Insets(10));

                    Label nameLabel = new Label("📁 " + project.getProjectName());
                    nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

                    Label detailsLabel = new Label(
                            "Location: " + (project.getProjectLocation() != null ? project.getProjectLocation() : "N/A") +
                                    " • Status: " + (project.getStatus() != null ? project.getStatus() : "ACTIVE")
                    );
                    detailsLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 12px;");

                    cellContent.getChildren().addAll(nameLabel, detailsLabel);
                    setGraphic(cellContent);
                    setStyle("-fx-background-color: transparent;");
                }
            }
        });

        // Load projects
        loadProjects(projectsListView);

        // View button
        Button viewButton = new Button("👁️ View Details");
        viewButton.setStyle(
                "-fx-background-color: #3b82f6;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 10 20;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        );
        viewButton.setOnAction(e -> {
            Project selected = projectsListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showProjectDetails(selected);
            } else {
                showAlert("Please select a project first");
            }
        });

        VBox.setVgrow(projectsListView, Priority.ALWAYS);
        view.getChildren().addAll(infoLabel, projectsListView, viewButton);

        return view;
    }

    /**
     * Create customers view
     */
    private VBox createCustomersView(Stage parentStage) {
        VBox view = new VBox(15);
        view.setPadding(new Insets(20));
        view.setStyle("-fx-background-color: rgba(30, 41, 59, 0.6); -fx-background-radius: 12;");

        Label infoLabel = new Label("ℹ️ Select a customer to view their complete details (READ-ONLY)");
        infoLabel.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 14px; -fx-font-weight: bold;");

        // Customers list
        ListView<Customer> customersListView = new ListView<>();
        customersListView.setPrefHeight(400);
        customersListView.setStyle("-fx-background-color: rgba(51, 65, 85, 0.8);");

        customersListView.setCellFactory(lv -> new ListCell<Customer>() {
            @Override
            protected void updateItem(Customer customer, boolean empty) {
                super.updateItem(customer, empty);
                if (empty || customer == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox cellContent = new VBox(5);
                    cellContent.setPadding(new Insets(10));

                    Label nameLabel = new Label("👤 " + customer.getName());
                    nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

                    Label detailsLabel = new Label(
                            "Email: " + (customer.getEmail() != null ? customer.getEmail() : "N/A") +
                                    " • Phone: " + (customer.getPhone() != null ? customer.getPhone() : "N/A")
                    );
                    detailsLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 12px;");

                    cellContent.getChildren().addAll(nameLabel, detailsLabel);
                    setGraphic(cellContent);
                    setStyle("-fx-background-color: transparent;");
                }
            }
        });

        // Load customers
        loadCustomers(customersListView);

        // View button
        Button viewButton = new Button("👁️ View Details");
        viewButton.setStyle(
                "-fx-background-color: #3b82f6;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 10 20;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        );
        viewButton.setOnAction(e -> {
            Customer selected = customersListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showCustomerDetails(selected);
            } else {
                showAlert("Please select a customer first");
            }
        });

        VBox.setVgrow(customersListView, Priority.ALWAYS);
        view.getChildren().addAll(infoLabel, customersListView, viewButton);

        return view;
    }

    /**
     * Load projects from database
     */
    private void loadProjects(ListView<Project> listView) {
        Task<List<Project>> loadTask = new Task<>() {
            @Override
            protected List<Project> call() {
                return projectService.getAllProjects();
            }
        };

        loadTask.setOnSucceeded(e -> {
            listView.getItems().setAll(loadTask.getValue());
        });

        loadTask.setOnFailed(e -> {
            showAlert("Failed to load projects: " + loadTask.getException().getMessage());
        });

        new Thread(loadTask).start();
    }

    /**
     * Load customers from database
     */
    private void loadCustomers(ListView<Customer> listView) {
        Task<List<Customer>> loadTask = new Task<>() {
            @Override
            protected List<Customer> call() {
                return customerService.getAllCustomers();
            }
        };

        loadTask.setOnSucceeded(e -> {
            listView.getItems().setAll(loadTask.getValue());
        });

        loadTask.setOnFailed(e -> {
            showAlert("Failed to load customers: " + loadTask.getException().getMessage());
        });

        new Thread(loadTask).start();
    }

    /**
     * Show project details (READ-ONLY)
     */
    private void showProjectDetails(Project project) {
        Stage detailsStage = new Stage();
        detailsStage.initModality(Modality.APPLICATION_MODAL);
        detailsStage.setTitle("Project Details - " + project.getProjectName() + " (READ-ONLY)");

        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #1e293b;");

        // Header
        Label titleLabel = new Label("📁 " + project.getProjectName());
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

        Label readOnlyLabel = new Label("🔒 READ-ONLY MODE - No editing allowed");
        readOnlyLabel.setStyle(
                "-fx-text-fill: #fbbf24;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-color: rgba(251, 191, 36, 0.2);" +
                        "-fx-padding: 8 15;" +
                        "-fx-background-radius: 6;"
        );

        VBox header = new VBox(10, titleLabel, readOnlyLabel);
        header.setAlignment(Pos.CENTER);

        // Tabs for different sections
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab infoTab = new Tab("ℹ️ Info");
        infoTab.setContent(createProjectInfoView(project));

        Tab elementsTab = new Tab("📦 Elements");
        elementsTab.setContent(createProjectElementsView(project));

        Tab tasksTab = new Tab("✅ Tasks");
        tasksTab.setContent(createProjectTasksView(project));

        Tab scheduleTab = new Tab("📅 Schedule");
        scheduleTab.setContent(createProjectScheduleView(project));

        Tab documentsTab = new Tab("📄 Documents");
        documentsTab.setContent(createProjectDocumentsView(project));

        tabPane.getTabs().addAll(infoTab, elementsTab, tasksTab, scheduleTab, documentsTab);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        // Close button
        Button closeButton = new Button("Close");
        closeButton.setStyle("-fx-background-color: #6b7280; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 8;");
        closeButton.setOnAction(e -> detailsStage.close());

        HBox buttonBox = new HBox(closeButton);
        buttonBox.setAlignment(Pos.CENTER);

        root.getChildren().addAll(header, tabPane, buttonBox);

        Scene scene = new Scene(root, 900, 700);
        detailsStage.setScene(scene);
        detailsStage.show();
    }

    /**
     * Show customer details (READ-ONLY)
     */
    private void showCustomerDetails(Customer customer) {
        Stage detailsStage = new Stage();
        detailsStage.initModality(Modality.APPLICATION_MODAL);
        detailsStage.setTitle("Customer Details - " + customer.getName() + " (READ-ONLY)");

        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #1e293b;");

        // Header
        Label titleLabel = new Label("👤 " + customer.getName());
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

        Label readOnlyLabel = new Label("🔒 READ-ONLY MODE - No editing allowed");
        readOnlyLabel.setStyle(
                "-fx-text-fill: #fbbf24;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-color: rgba(251, 191, 36, 0.2);" +
                        "-fx-padding: 8 15;" +
                        "-fx-background-radius: 6;"
        );

        VBox header = new VBox(10, titleLabel, readOnlyLabel);
        header.setAlignment(Pos.CENTER);

        // Tabs for different sections
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab infoTab = new Tab("ℹ️ Info");
        infoTab.setContent(createCustomerInfoView(customer));

        Tab elementsTab = new Tab("📦 Elements");
        elementsTab.setContent(createCustomerElementsView(customer));

        Tab tasksTab = new Tab("✅ Tasks");
        tasksTab.setContent(createCustomerTasksView(customer));

        Tab scheduleTab = new Tab("📅 Schedule");
        scheduleTab.setContent(createCustomerScheduleView(customer));

        Tab documentsTab = new Tab("📄 Documents");
        documentsTab.setContent(createCustomerDocumentsView(customer));

        tabPane.getTabs().addAll(infoTab, elementsTab, tasksTab, scheduleTab, documentsTab);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        // Close button
        Button closeButton = new Button("Close");
        closeButton.setStyle("-fx-background-color: #6b7280; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 8;");
        closeButton.setOnAction(e -> detailsStage.close());

        HBox buttonBox = new HBox(closeButton);
        buttonBox.setAlignment(Pos.CENTER);

        root.getChildren().addAll(header, tabPane, buttonBox);

        Scene scene = new Scene(root, 900, 700);
        detailsStage.setScene(scene);
        detailsStage.show();
    }

    // ==================== PROJECT VIEW CREATORS ====================

    private ScrollPane createProjectInfoView(Project project) {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: rgba(51, 65, 85, 0.6);");

        content.getChildren().addAll(
                createInfoRow("Project Name:", project.getProjectName()),
                createInfoRow("Location:", project.getProjectLocation() != null ? project.getProjectLocation() : "N/A"),
                createInfoRow("Status:", project.getStatus() != null ? project.getStatus() : "ACTIVE"),
                createInfoRow("Issue Date:", project.getDateOfIssue() != null ? project.getDateOfIssue().format(DATE_FORMATTER) : "N/A"),
                createInfoRow("Completion Date:", project.getDateOfCompletion() != null ? project.getDateOfCompletion().format(DATE_FORMATTER) : "N/A"),
                createInfoRow("Notes:", project.getNotes() != null ? project.getNotes() : "No notes")
        );

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        return scrollPane;
    }

    private ScrollPane createProjectElementsView(Project project) {
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: rgba(51, 65, 85, 0.6);");

        List<ProjectElement> elements = projectElementService.getProjectElements(project.getId());

        if (elements.isEmpty()) {
            Label emptyLabel = new Label("No elements found");
            emptyLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.5); -fx-font-size: 14px;");
            content.getChildren().add(emptyLabel);
        } else {
            for (ProjectElement element : elements) {
                VBox elementCard = new VBox(5);
                elementCard.setPadding(new Insets(15));
                elementCard.setStyle(
                        "-fx-background-color: rgba(30, 41, 59, 0.8);" +
                                "-fx-background-radius: 8;" +
                                "-fx-border-color: #3b82f6;" +
                                "-fx-border-width: 1;" +
                                "-fx-border-radius: 8;"
                );

                Label nameLabel = new Label("📦 " + element.getStorageItem().getProductName());
                nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

                Label qtyLabel = new Label("Quantity Needed: " + element.getQuantityNeeded());
                qtyLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.8); -fx-font-size: 12px;");

                Label statusLabel = new Label("Status: " + (element.getStatus() != null ? element.getStatus() : "PENDING"));
                statusLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 12px;");

                elementCard.getChildren().addAll(nameLabel, qtyLabel, statusLabel);
                content.getChildren().add(elementCard);
            }
        }

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        return scrollPane;
    }

    private ScrollPane createProjectTasksView(Project project) {
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: rgba(51, 65, 85, 0.6);");

        List<ProjectTask> tasks = projectTaskService.getProjectTasks(project.getId());

        if (tasks.isEmpty()) {
            Label emptyLabel = new Label("No tasks found");
            emptyLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.5); -fx-font-size: 14px;");
            content.getChildren().add(emptyLabel);
        } else {
            for (ProjectTask task : tasks) {
                VBox taskCard = new VBox(5);
                taskCard.setPadding(new Insets(15));
                taskCard.setStyle(
                        "-fx-background-color: rgba(30, 41, 59, 0.8);" +
                                "-fx-background-radius: 8;" +
                                "-fx-border-color: " + (task.getIsCompleted() ? "#22c55e" : "#ef4444") + ";" +
                                "-fx-border-width: 2;" +
                                "-fx-border-radius: 8;"
                );

                Label statusIcon = new Label(task.getIsCompleted() ? "✅" : "❌");
                Label titleLabel = new Label(task.getTaskTitle());
                titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

                HBox titleBox = new HBox(10, statusIcon, titleLabel);
                titleBox.setAlignment(Pos.CENTER_LEFT);

                Label detailsLabel = new Label(task.getTaskDetails() != null ? task.getTaskDetails() : "");
                detailsLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.8); -fx-font-size: 12px;");
                detailsLabel.setWrapText(true);

                taskCard.getChildren().addAll(titleBox, detailsLabel);
                content.getChildren().add(taskCard);
            }
        }

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        return scrollPane;
    }

    private ScrollPane createProjectScheduleView(Project project) {
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: rgba(51, 65, 85, 0.6);");

        List<ProjectSchedule> schedules = projectScheduleService.getProjectSchedules(project.getId());

        if (schedules.isEmpty()) {
            Label emptyLabel = new Label("No schedule found");
            emptyLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.5); -fx-font-size: 14px;");
            content.getChildren().add(emptyLabel);
        } else {
            for (ProjectSchedule schedule : schedules) {
                HBox scheduleRow = new HBox(15);
                scheduleRow.setPadding(new Insets(10));
                scheduleRow.setStyle("-fx-background-color: rgba(30, 41, 59, 0.8); -fx-background-radius: 6;");
                scheduleRow.setAlignment(Pos.CENTER_LEFT);

                Label taskLabel = new Label("📅 " + schedule.getTaskName());
                taskLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");
                taskLabel.setPrefWidth(200);

                Label dateLabel = new Label(
                        (schedule.getStartDate() != null ? schedule.getStartDate().format(DATE_FORMATTER) : "N/A") +
                                " → " +
                                (schedule.getEndDate() != null ? schedule.getEndDate().format(DATE_FORMATTER) : "N/A")
                );
                dateLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.8); -fx-font-size: 12px;");

                scheduleRow.getChildren().addAll(taskLabel, dateLabel);
                content.getChildren().add(scheduleRow);
            }
        }

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        return scrollPane;
    }

    private ScrollPane createProjectDocumentsView(Project project) {
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: rgba(51, 65, 85, 0.6);");

        List<ProjectDocument> documents = projectDocumentService.getProjectDocuments(project.getId());

        if (documents.isEmpty()) {
            Label emptyLabel = new Label("No documents found");
            emptyLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.5); -fx-font-size: 14px;");
            content.getChildren().add(emptyLabel);
        } else {
            for (ProjectDocument doc : documents) {
                HBox docRow = new HBox(15);
                docRow.setPadding(new Insets(10));
                docRow.setStyle("-fx-background-color: rgba(30, 41, 59, 0.8); -fx-background-radius: 6;");
                docRow.setAlignment(Pos.CENTER_LEFT);

                Label nameLabel = new Label("📄 " + doc.getDocumentName());
                nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
                nameLabel.setPrefWidth(250);

                Label typeLabel = new Label(doc.getDocumentType());
                typeLabel.setStyle("-fx-text-fill: #3b82f6; -fx-font-size: 12px; -fx-font-weight: bold;");

                Label categoryLabel = new Label(doc.getCategory() != null ? doc.getCategory() : "OTHER");
                categoryLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 12px;");

                docRow.getChildren().addAll(nameLabel, typeLabel, categoryLabel);
                content.getChildren().add(docRow);
            }
        }

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        return scrollPane;
    }

    // ==================== CUSTOMER VIEW CREATORS (Similar to Project) ====================

    private ScrollPane createCustomerInfoView(Customer customer) {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: rgba(51, 65, 85, 0.6);");

        content.getChildren().addAll(
                createInfoRow("Customer Name:", customer.getName()),
                createInfoRow("Email:", customer.getEmail() != null ? customer.getEmail() : "N/A"),
                createInfoRow("Phone:", customer.getPhone() != null ? customer.getPhone() : "N/A"),
                createInfoRow("Company:", customer.getCompany() != null ? customer.getCompany() : "N/A"),
                createInfoRow("Address:", customer.getAddress() != null ? customer.getAddress() : "N/A"),
                createInfoRow("Created:", customer.getCreatedAt() != null ? customer.getCreatedAt().format(DATETIME_FORMATTER) : "N/A")
        );

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        return scrollPane;
    }

    private ScrollPane createCustomerElementsView(Customer customer) {
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: rgba(51, 65, 85, 0.6);");

        List<CustomerElement> elements = customerElementService.getCustomerElements(customer.getId());

        if (elements.isEmpty()) {
            Label emptyLabel = new Label("No elements found");
            emptyLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.5); -fx-font-size: 14px;");
            content.getChildren().add(emptyLabel);
        } else {
            for (CustomerElement element : elements) {
                VBox elementCard = new VBox(5);
                elementCard.setPadding(new Insets(15));
                elementCard.setStyle(
                        "-fx-background-color: rgba(30, 41, 59, 0.8);" +
                                "-fx-background-radius: 8;" +
                                "-fx-border-color: #8b5cf6;" +
                                "-fx-border-width: 1;" +
                                "-fx-border-radius: 8;"
                );

                Label nameLabel = new Label("📦 " + element.getStorageItem().getProductName());
                nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

                Label qtyLabel = new Label("Quantity Needed: " + element.getQuantityNeeded());
                qtyLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.8); -fx-font-size: 12px;");

                Label statusLabel = new Label("Status: " + (element.getStatus() != null ? element.getStatus() : "PENDING"));
                statusLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 12px;");

                elementCard.getChildren().addAll(nameLabel, qtyLabel, statusLabel);
                content.getChildren().add(elementCard);
            }
        }

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        return scrollPane;
    }

    private ScrollPane createCustomerTasksView(Customer customer) {
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: rgba(51, 65, 85, 0.6);");

        List<CustomerTask> tasks = customerTaskService.getCustomerTasks(customer.getId());

        if (tasks.isEmpty()) {
            Label emptyLabel = new Label("No tasks found");
            emptyLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.5); -fx-font-size: 14px;");
            content.getChildren().add(emptyLabel);
        } else {
            for (CustomerTask task : tasks) {
                VBox taskCard = new VBox(5);
                taskCard.setPadding(new Insets(15));
                taskCard.setStyle(
                        "-fx-background-color: rgba(30, 41, 59, 0.8);" +
                                "-fx-background-radius: 8;" +
                                "-fx-border-color: " + (task.getIsCompleted() ? "#22c55e" : "#ef4444") + ";" +
                                "-fx-border-width: 2;" +
                                "-fx-border-radius: 8;"
                );

                Label statusIcon = new Label(task.getIsCompleted() ? "✅" : "❌");
                Label titleLabel = new Label(task.getTaskTitle());
                titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

                HBox titleBox = new HBox(10, statusIcon, titleLabel);
                titleBox.setAlignment(Pos.CENTER_LEFT);

                Label detailsLabel = new Label(task.getTaskDetails() != null ? task.getTaskDetails() : "");
                detailsLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.8); -fx-font-size: 12px;");
                detailsLabel.setWrapText(true);

                taskCard.getChildren().addAll(titleBox, detailsLabel);
                content.getChildren().add(taskCard);
            }
        }

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        return scrollPane;
    }

    private ScrollPane createCustomerScheduleView(Customer customer) {
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: rgba(51, 65, 85, 0.6);");

        List<CustomerSchedule> schedules = customerScheduleService.getCustomerSchedules(customer.getId());

        if (schedules.isEmpty()) {
            Label emptyLabel = new Label("No schedule found");
            emptyLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.5); -fx-font-size: 14px;");
            content.getChildren().add(emptyLabel);
        } else {
            for (CustomerSchedule schedule : schedules) {
                HBox scheduleRow = new HBox(15);
                scheduleRow.setPadding(new Insets(10));
                scheduleRow.setStyle("-fx-background-color: rgba(30, 41, 59, 0.8); -fx-background-radius: 6;");
                scheduleRow.setAlignment(Pos.CENTER_LEFT);

                Label taskLabel = new Label("📅 " + schedule.getTaskName());
                taskLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");
                taskLabel.setPrefWidth(200);

                Label dateLabel = new Label(
                        (schedule.getStartDate() != null ? schedule.getStartDate().format(DATE_FORMATTER) : "N/A") +
                                " → " +
                                (schedule.getEndDate() != null ? schedule.getEndDate().format(DATE_FORMATTER) : "N/A")
                );
                dateLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.8); -fx-font-size: 12px;");

                scheduleRow.getChildren().addAll(taskLabel, dateLabel);
                content.getChildren().add(scheduleRow);
            }
        }

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        return scrollPane;
    }

    private ScrollPane createCustomerDocumentsView(Customer customer) {
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: rgba(51, 65, 85, 0.6);");

        List<CustomerDocument> documents = customerDocumentService.getCustomerDocuments(customer.getId());

        if (documents.isEmpty()) {
            Label emptyLabel = new Label("No documents found");
            emptyLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.5); -fx-font-size: 14px;");
            content.getChildren().add(emptyLabel);
        } else {
            for (CustomerDocument doc : documents) {
                HBox docRow = new HBox(15);
                docRow.setPadding(new Insets(10));
                docRow.setStyle("-fx-background-color: rgba(30, 41, 59, 0.8); -fx-background-radius: 6;");
                docRow.setAlignment(Pos.CENTER_LEFT);

                Label nameLabel = new Label("📄 " + doc.getDocumentName());
                nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
                nameLabel.setPrefWidth(250);

                Label typeLabel = new Label(doc.getDocumentType());
                typeLabel.setStyle("-fx-text-fill: #8b5cf6; -fx-font-size: 12px; -fx-font-weight: bold;");

                Label categoryLabel = new Label(doc.getCategory() != null ? doc.getCategory() : "OTHER");
                categoryLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 12px;");

                docRow.getChildren().addAll(nameLabel, typeLabel, categoryLabel);
                content.getChildren().add(docRow);
            }
        }

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        return scrollPane;
    }

    // ==================== HELPER METHODS ====================

    private HBox createInfoRow(String label, String value) {
        HBox row = new HBox(15);
        row.setPadding(new Insets(8, 0, 8, 0));
        row.setAlignment(Pos.CENTER_LEFT);

        Label labelText = new Label(label);
        labelText.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 13px; -fx-font-weight: bold;");
        labelText.setPrefWidth(150);

        Label valueText = new Label(value);
        valueText.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
        valueText.setWrapText(true);
        HBox.setHgrow(valueText, Priority.ALWAYS);

        row.getChildren().addAll(labelText, valueText);
        return row;
    }

    private void showAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information");
            alert.setHeaderText(null);
            alert.setContentText(message);

            // Apply dark theme
            alert.getDialogPane().setStyle("-fx-background-color: #1e293b; -fx-text-fill: white;");
            alert.getDialogPane().lookup(".content.label").setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

            alert.showAndWait();
        });
    }
}
