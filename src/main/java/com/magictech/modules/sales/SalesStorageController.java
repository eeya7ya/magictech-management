package com.magictech.modules.sales;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.application.Platform;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import com.magictech.modules.projects.entity.ProjectElement;
import com.magictech.modules.projects.service.ProjectElementService;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.FlowPane;
import com.magictech.core.auth.User;
import com.magictech.core.module.BaseModuleController;
import com.magictech.modules.sales.entity.*;
import com.magictech.modules.sales.service.*;
import com.magictech.modules.projects.entity.Project;
import com.magictech.modules.projects.service.ProjectService;
import com.magictech.modules.storage.entity.StorageItem;
import com.magictech.modules.storage.service.StorageService;
import javafx.scene.control.Spinner;
import com.magictech.modules.projects.entity.ProjectElement;
import com.magictech.modules.projects.service.ProjectElementService;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.FlowPane;
import com.magictech.modules.sales.ui.WorkflowDialog;
import com.magictech.modules.sales.ui.WorkflowStatusCard;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.File;
import java.util.List;

@Component
public class SalesStorageController extends BaseModuleController {

    @Autowired private CustomerService customerService;
    @Autowired private SalesOrderService salesOrderService;
    @Autowired private SalesContractService salesContractService;
    @Autowired private ProjectService projectService;
    @Autowired private StorageService storageService;
    @Autowired private ProjectElementService elementService;
    @Autowired private com.magictech.modules.sales.service.ProjectCostBreakdownService costBreakdownService;
    @Autowired private com.magictech.modules.sales.service.CustomerCostBreakdownService customerCostBreakdownService;
    @Autowired private com.magictech.modules.sales.service.SalesExcelExportService salesExcelExportService;
    @Autowired private com.magictech.modules.sales.service.ProjectWorkflowService workflowService;
    @Autowired private com.magictech.modules.sales.service.WorkflowStepService stepService;
    @Autowired private com.magictech.modules.sales.repository.SiteSurveyDataRepository siteSurveyRepository;
    @Autowired private com.magictech.modules.sales.repository.SizingPricingDataRepository sizingPricingRepository;
    @Autowired private com.magictech.modules.sales.repository.BankGuaranteeDataRepository bankGuaranteeRepository;
    @Autowired private com.magictech.core.messaging.service.NotificationListenerService notificationListenerService;

    private com.magictech.core.ui.components.DashboardBackgroundPane backgroundPane;
    private StackPane mainContainer;
    private VBox dashboardScreen;
    private ListView<Project> projectsListView;
    private ListView<Customer> customersListView;
    private Project currentlyDisplayedProject; // Track which project is currently shown
    private java.util.function.Consumer<com.magictech.core.messaging.dto.NotificationMessage> notificationListener;
    // Note: currentUser is inherited from BaseModuleController - do not redeclare!

    @Override
    public void refresh() {
        loadDashboardData();
    }

    @Override
    protected void setupUI() {
        // Debug logging
        System.out.println("SalesStorageController.setupUI() - currentUser: " +
            (currentUser != null ? currentUser.getUsername() : "NULL"));

        StackPane stackRoot = new StackPane();
        backgroundPane = new com.magictech.core.ui.components.DashboardBackgroundPane();
        mainContainer = new StackPane();
        dashboardScreen = createDashboardScreen();

        mainContainer.getChildren().add(dashboardScreen);
        stackRoot.getChildren().addAll(backgroundPane, mainContainer);

        BorderPane root = getRootPane();
        root.setCenter(stackRoot);
        root.setStyle("-fx-background-color: transparent;");

        // Register notification listener for workflow updates
        registerWorkflowNotificationListener();
    }

    @Override
    protected void loadData() {
        loadDashboardData();
    }

    // ==================== MAIN DASHBOARD ====================
    private VBox createDashboardScreen() {
        VBox screen = new VBox(30);
        screen.setPadding(new Insets(40));
        screen.setStyle("-fx-background-color: transparent;");

        HBox header = createHeader();

        HBox submodulesContainer = new HBox(30);
        submodulesContainer.setAlignment(Pos.TOP_CENTER);
        HBox.setHgrow(submodulesContainer, Priority.ALWAYS);

        VBox projectsSubmodule = createProjectsSubmodule();
        HBox.setHgrow(projectsSubmodule, Priority.ALWAYS);

        VBox customersSubmodule = createCustomersSubmodule();
        HBox.setHgrow(customersSubmodule, Priority.ALWAYS);

        submodulesContainer.getChildren().addAll(projectsSubmodule, customersSubmodule);
        screen.getChildren().addAll(header, submodulesContainer);
        return screen;
    }

    private HBox createHeader() {
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 20, 0));

        Button backButton = createStyledButton("← Back to Dashboard", "#6b7280", "#4b5563");
        backButton.setOnAction(e -> navigateToDashboard());

        Label titleLabel = new Label("💼 Sales Module");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 32px; -fx-font-weight: bold;");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        header.getChildren().addAll(backButton, titleLabel);
        return header;
    }

    // ==================== PROJECTS SUBMODULE ====================
    private VBox createProjectsSubmodule() {
        VBox submodule = new VBox(20);
        submodule.setMaxWidth(700);
        submodule.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.6);" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: rgba(59, 130, 246, 0.4);" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 16;" +
                        "-fx-padding: 25;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 20, 0, 0, 8);"
        );

        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("🏗️ Projects");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Button editProjectBtn = createStyledButton("✏️ Edit", "#f59e0b", "#d97706");
        editProjectBtn.setOnAction(e -> {
            Project selected = projectsListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                openProjectDetails(selected);
            } else {
                showWarning("Please select a project first");
            }
        });

        Button deleteProjectBtn = createStyledButton("🗑️ Delete", "#ef4444", "#dc2626");
        deleteProjectBtn.setOnAction(e -> {
            Project selected = projectsListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                handleDeleteProject(selected);
            } else {
                showWarning("Please select a project first");
            }
        });

        Button exportProjectsBtn = createStyledButton("📥 Export", "#8b5cf6", "#7c3aed");
        exportProjectsBtn.setOnAction(e -> handleExportProjects());

        Button addProjectBtn = createStyledButton("+ New Project", "#3b82f6", "#2563eb");
        addProjectBtn.setOnAction(e -> handleAddProject());

        header.getChildren().addAll(titleLabel, editProjectBtn, deleteProjectBtn, exportProjectsBtn, addProjectBtn);

        projectsListView = new ListView<>();
        projectsListView.setPrefHeight(500);
        projectsListView.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-control-inner-background: rgba(15, 23, 42, 0.8);"
        );

        projectsListView.setCellFactory(lv -> new ListCell<Project>() {
            @Override
            protected void updateItem(Project project, boolean empty) {
                super.updateItem(project, empty);
                if (empty || project == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    VBox cellBox = new VBox(8);
                    cellBox.setPadding(new Insets(12));
                    cellBox.setStyle(
                            "-fx-background-color: rgba(30, 41, 59, 0.7);" +
                                    "-fx-background-radius: 8;" +
                                    "-fx-border-color: rgba(59, 130, 246, 0.3);" +
                                    "-fx-border-width: 1;" +
                                    "-fx-border-radius: 8;" +
                                    "-fx-cursor: hand;"
                    );

                    Label nameLabel = new Label("📋 " + project.getProjectName());
                    nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

                    Label locationLabel = new Label("📍 " + project.getProjectLocation());
                    locationLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.8); -fx-font-size: 12px;");

                    cellBox.getChildren().addAll(nameLabel, locationLabel);
                    cellBox.setOnMouseClicked(e -> openProjectDetails(project));

                    setGraphic(cellBox);
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                }
            }
        });

        loadProjects(projectsListView);

        submodule.getChildren().addAll(header, projectsListView);
        return submodule;
    }

    // ==================== CUSTOMERS SUBMODULE ====================
    private VBox createCustomersSubmodule() {
        VBox submodule = new VBox(20);
        submodule.setMaxWidth(700);
        submodule.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.6);" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: rgba(34, 197, 94, 0.4);" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 16;" +
                        "-fx-padding: 25;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 20, 0, 0, 8);"
        );

        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("👥 Customers");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Button editCustomerBtn = createStyledButton("✏️ Edit", "#f59e0b", "#d97706");
        editCustomerBtn.setOnAction(e -> {
            Customer selected = customersListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                handleEditCustomer(selected);
            } else {
                showWarning("Please select a customer first");
            }
        });

        Button deleteCustomerBtn = createStyledButton("🗑️ Delete", "#ef4444", "#dc2626");
        deleteCustomerBtn.setOnAction(e -> {
            Customer selected = customersListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                handleDeleteCustomer(selected);
            } else {
                showWarning("Please select a customer first");
            }
        });

        Button exportCustomersBtn = createStyledButton("📥 Export", "#8b5cf6", "#7c3aed");
        exportCustomersBtn.setOnAction(e -> handleExportCustomers());

        Button addCustomerBtn = createStyledButton("+ New Customer", "#22c55e", "#16a34a");
        addCustomerBtn.setOnAction(e -> handleAddCustomer());

        header.getChildren().addAll(titleLabel, editCustomerBtn, deleteCustomerBtn, exportCustomersBtn, addCustomerBtn);

        customersListView = new ListView<>();
        customersListView.setPrefHeight(500);
        customersListView.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-control-inner-background: rgba(15, 23, 42, 0.8);"
        );

        customersListView.setCellFactory(lv -> new ListCell<Customer>() {
            @Override
            protected void updateItem(Customer customer, boolean empty) {
                super.updateItem(customer, empty);
                if (empty || customer == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    VBox cellBox = new VBox(8);
                    cellBox.setPadding(new Insets(12));
                    cellBox.setStyle(
                            "-fx-background-color: rgba(30, 41, 59, 0.7);" +
                                    "-fx-background-radius: 8;" +
                                    "-fx-border-color: rgba(34, 197, 94, 0.3);" +
                                    "-fx-border-width: 1;" +
                                    "-fx-border-radius: 8;" +
                                    "-fx-cursor: hand;"
                    );

                    Label nameLabel = new Label("👤 " + customer.getName());
                    nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

                    String contactInfo = "";
                    if (customer.getPhone() != null && !customer.getPhone().isEmpty()) {
                        contactInfo = "📞 " + customer.getPhone();
                    }
                    if (customer.getEmail() != null && !customer.getEmail().isEmpty()) {
                        contactInfo += (contactInfo.isEmpty() ? "" : " • ") + "📧 " + customer.getEmail();
                    }

                    Label contactLabel = new Label(contactInfo);
                    contactLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.8); -fx-font-size: 12px;");

                    cellBox.getChildren().addAll(nameLabel, contactLabel);
                    cellBox.setOnMouseClicked(e -> openCustomerDetails(customer));

                    setGraphic(cellBox);
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                }
            }
        });

        loadCustomers(customersListView);

        submodule.getChildren().addAll(header, customersListView);
        return submodule;
    }

    // ==================== PROJECT DETAILS ====================
    private void openProjectDetails(Project project) {
        // Track currently displayed project for auto-refresh
        currentlyDisplayedProject = project;

        VBox mainLayout = new VBox(20);
        mainLayout.setPadding(new Insets(30));
        mainLayout.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(mainLayout, Priority.ALWAYS);

        // Header with back button
        HBox headerBox = new HBox(20);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(0, 0, 10, 0));

        Button backBtn = createStyledButton("← Back to Projects", "#6b7280", "#4b5563");
        backBtn.setOnAction(e -> {
            // Go back to sales dashboard showing projects list
            mainContainer.getChildren().clear();
            mainContainer.getChildren().add(dashboardScreen);
        });

        Label headerLabel = new Label("📋 " + project.getProjectName());
        headerLabel.setStyle("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: bold;");
        HBox.setHgrow(headerLabel, Priority.ALWAYS);

        headerBox.getChildren().addAll(backBtn, headerLabel);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        // ✅ WORKFLOW WIZARD Tab (Primary Interface - replaces Contract PDF)
        Tab workflowTab = new Tab("🔄 Workflow Wizard");
        workflowTab.setContent(createWorkflowWizardTab(project));
        styleTab(workflowTab, "#8b5cf6"); // Purple theme

        // ✅ Project Elements Tab (synchronized with Projects module) + Cost Breakdown
        Tab elementsTab = new Tab("📦 Project Elements");
        elementsTab.setContent(createProjectElementsTab(project));
        styleTab(elementsTab, "#a78bfa"); // Light purple theme

        // Contract PDF Tab (moved to last, optional)
        Tab contractsTab = new Tab("📄 Contract PDF");
        contractsTab.setContent(createSimplePDFTab(project));
        styleTab(contractsTab, "#7c3aed"); // Purple theme

        tabPane.getTabs().addAll(workflowTab, elementsTab, contractsTab);

        mainLayout.getChildren().addAll(headerBox, tabPane);

        // Show in mainContainer (inline navigation)
        Platform.runLater(() -> {
            mainContainer.getChildren().clear();
            mainContainer.getChildren().add(mainLayout);
        });
    }

    private VBox createProjectElementsTab(Project project) {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: rgba(15, 23, 42, 0.6);");
        VBox.setVgrow(content, Priority.ALWAYS);

        // Create TabPane for Cost Breakdown and Elements
        TabPane subTabPane = new TabPane();
        subTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(subTabPane, Priority.ALWAYS);

        // ============ TAB 1: Cost Breakdown ============
        Tab costTab = new Tab("💰 Cost Breakdown");
        VBox costContent = new VBox(20);
        costContent.setPadding(new Insets(30));
        costContent.setStyle("-fx-background-color: rgba(15, 23, 42, 0.6);");

        com.magictech.modules.sales.ui.CostBreakdownPanel breakdownPanel =
            new com.magictech.modules.sales.ui.CostBreakdownPanel();
        breakdownPanel.setId("costBreakdownPanel");

        // Load existing breakdown
        try {
            java.util.Optional<com.magictech.modules.sales.entity.ProjectCostBreakdown> existing =
                costBreakdownService.getBreakdownByProject(project.getId());
            if (existing.isPresent()) {
                breakdownPanel.loadBreakdown(existing.get());
            }
        } catch (Exception ex) {
            System.err.println("Error loading cost breakdown: " + ex.getMessage());
        }

        // Set save callback
        breakdownPanel.setOnSave(breakdown -> {
            try {
                breakdown.setProjectId(project.getId());
                String username = (currentUser != null) ? currentUser.getUsername() : "unknown";
                costBreakdownService.saveBreakdown(breakdown, username);
                showSuccess("Cost breakdown saved successfully!");
            } catch (Exception ex) {
                showError("Failed to save breakdown: " + ex.getMessage());
            }
        });

        costContent.getChildren().add(breakdownPanel);
        costTab.setContent(costContent);
        styleTab(costTab, "#eab308"); // Gold theme

        // ============ TAB 2: Project Elements ============
        Tab elementsTab = new Tab("📦 Project Elements");
        VBox elementsContent = new VBox(20);
        elementsContent.setPadding(new Insets(30));
        elementsContent.setStyle("-fx-background-color: rgba(15, 23, 42, 0.6);");
        VBox.setVgrow(elementsContent, Priority.ALWAYS);

        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("📦 Project Elements");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");
        HBox.setHgrow(title, Priority.ALWAYS);

        Button addButton = createStyledButton("+ Add Element", "#7c3aed", "#6b21a8");
        addButton.setOnAction(e -> handleAddElementToProject(project, elementsContent));

        Button refreshButton = createStyledButton("🔄 Refresh", "#a78bfa", "#7c3aed");
        refreshButton.setOnAction(e -> {
            loadProjectElements(project, elementsContent);
            refreshCostBreakdown(project, costContent);
        });

        header.getChildren().addAll(title, addButton, refreshButton);

        // ScrollPane with Grid of Element Cards
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle(
                "-fx-background: transparent;" +
                        "-fx-background-color: transparent;" +
                        "-fx-border-color: transparent;"
        );
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        FlowPane elementsGrid = new FlowPane();
        elementsGrid.setId("elementsGrid");
        elementsGrid.setHgap(20);
        elementsGrid.setVgap(20);
        elementsGrid.setPadding(new Insets(10));
        elementsGrid.setStyle("-fx-background-color: transparent;");

        scrollPane.setContent(elementsGrid);

        elementsContent.getChildren().addAll(header, scrollPane);
        elementsTab.setContent(elementsContent);
        styleTab(elementsTab, "#a78bfa"); // Light purple theme

        // Add tabs to TabPane
        subTabPane.getTabs().addAll(costTab, elementsTab);

        content.getChildren().add(subTabPane);

        // Load existing elements
        loadProjectElements(project, elementsContent);

        // Calculate and set initial subtotal
        refreshCostBreakdown(project, costContent);

        return content;
    }

    // Helper method to refresh cost breakdown (now works with costContent VBox containing the panel)
    private void refreshCostBreakdown(Project project, VBox costContent) {
        try {
            List<ProjectElement> elements = elementService.getElementsByProject(project.getId());
            java.math.BigDecimal subtotal = java.math.BigDecimal.ZERO;

            for (ProjectElement element : elements) {
                // Use custom price if set, otherwise use storage item price
                java.math.BigDecimal price = element.getCustomPrice();
                if (price == null && element.getStorageItem() != null) {
                    price = element.getStorageItem().getPrice();
                }

                if (price != null) {
                    java.math.BigDecimal quantity = new java.math.BigDecimal(element.getQuantityNeeded());
                    subtotal = subtotal.add(price.multiply(quantity));
                }
            }

            // Update the breakdown panel
            com.magictech.modules.sales.ui.CostBreakdownPanel panel =
                (com.magictech.modules.sales.ui.CostBreakdownPanel) costContent.lookup("#costBreakdownPanel");
            if (panel != null) {
                panel.setElementsSubtotal(subtotal);
            }
        } catch (Exception ex) {
            System.err.println("Error refreshing cost breakdown: " + ex.getMessage());
        }
    }

    // ==================== ADD ELEMENT TO PROJECT (FROM SALES MODULE) ====================
    private void handleAddElementToProject(Project project, VBox contentPane) {
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
                        "-fx-control-inner-background: #0f172a;"
        );

        // Product Name Column
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
                    setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
                }
            }
        });

        // Manufacture Column
        TableColumn<StorageItem, String> mfgCol = new TableColumn<>("🏭 Manufacture");
        mfgCol.setPrefWidth(180);
        mfgCol.setCellValueFactory(new PropertyValueFactory<>("manufacture"));
        mfgCol.setCellFactory(col -> new TableCell<>() {
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

        // Code Column
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

        // Available Quantity Column
        TableColumn<StorageItem, String> qtyCol = new TableColumn<>("📊 Available");
        qtyCol.setPrefWidth(120);
        qtyCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                } else {
                    StorageItem storageItem = getTableView().getItems().get(getIndex());
                    int qty = storageItem.getQuantity();
                    setText(String.valueOf(qty));

                    String color = qty > 0 ? "#22c55e" : "#ef4444";
                    setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 14px;");
                }
            }
        });

        // Action Column
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

                addButton.setOnAction(e -> {
                    StorageItem selectedItem = getTableView().getItems().get(getIndex());
                    handleAddStorageItemToProjectDialog(selectedItem, project, contentPane, dialogStage);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : addButton);
            }
        });

        storageTable.getColumns().addAll(nameCol, mfgCol, codeCol, qtyCol, actionCol);

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

        Button closeButton = createStyledButton("✗ Close", "#6b7280", "#4b5563");
        closeButton.setOnAction(e -> dialogStage.close());

        mainLayout.getChildren().addAll(titleLabel, storageTable, closeButton);

        Scene scene = new Scene(mainLayout, 1200, 700);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }

    // ==================== ADD STORAGE ITEM TO PROJECT (AVAILABILITY CHECK) ====================
    private void handleAddStorageItemToProjectDialog(StorageItem storageItem, Project project, VBox contentPane, Stage parentDialog) {
        Dialog<Integer> quantityDialog = new Dialog<>();
        quantityDialog.setTitle("📦 Specify Quantity");
        quantityDialog.setHeaderText("Add: " + storageItem.getProductName());

        // Style header
        if (quantityDialog.getDialogPane().lookup(".header-panel") != null) {
            quantityDialog.getDialogPane().lookup(".header-panel").setStyle(
                    "-fx-background-color: #1e293b; -fx-text-fill: white;"
            );
        }

        ButtonType confirmButtonType = new ButtonType("✓ Check Availability", ButtonBar.ButtonData.OK_DONE);
        quantityDialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));
        grid.setStyle("-fx-background-color: #0f172a;");

        // Quantity spinner
        Label neededLabel = new Label("Quantity Needed:*");
        neededLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        Spinner<Integer> quantitySpinner = new Spinner<>(1, 999999, 1);
        quantitySpinner.setEditable(true);
        quantitySpinner.setPrefWidth(150);
        quantitySpinner.setStyle("-fx-background-color: #1e293b;");
        quantitySpinner.getEditor().setStyle("-fx-text-fill: white; -fx-background-color: #1e293b;");

        // Custom Price field (Sales can freely edit price)
        Label priceLabel = new Label("Custom Price:*");
        priceLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        TextField priceField = new TextField();
        // Pre-fill with storage item price, but allow editing
        BigDecimal defaultPrice = storageItem.getPrice() != null ? storageItem.getPrice() : BigDecimal.ZERO;
        priceField.setText(defaultPrice.toString());
        priceField.setPromptText("Enter price per unit");
        priceField.setPrefWidth(150);
        priceField.setStyle(
                "-fx-background-color: #1e293b;" +
                        "-fx-text-fill: #22c55e;" + // Green to indicate editable
                        "-fx-font-weight: bold;" +
                        "-fx-prompt-text-fill: #9ca3af;"
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
                        "-fx-prompt-text-fill: #9ca3af;"
        );

        grid.add(neededLabel, 0, 0);
        grid.add(quantitySpinner, 1, 0);
        grid.add(priceLabel, 0, 1);
        grid.add(priceField, 1, 1);
        grid.add(notesLabel, 0, 2);
        grid.add(notesField, 1, 2);

        quantityDialog.getDialogPane().setContent(grid);
        quantityDialog.getDialogPane().setStyle("-fx-background-color: #0f172a;");

        quantityDialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                return quantitySpinner.getValue();
            }
            return null;
        });

        quantityDialog.showAndWait().ifPresent(requestedQty -> {
            int availableQty = storageItem.getQuantity();

            if (requestedQty > availableQty) {
                // NOT AVAILABLE
                Alert notAvailableAlert = new Alert(Alert.AlertType.ERROR);
                notAvailableAlert.setTitle("❌ Insufficient Stock");
                notAvailableAlert.setHeaderText("Cannot allocate " + requestedQty + " units");
                notAvailableAlert.setContentText(
                        "Requested: " + requestedQty + " units\n" +
                                "Available: " + availableQty + " units\n" +
                                "Status: ❌ INSUFFICIENT STOCK"
                );
                notAvailableAlert.showAndWait();
                return;
            }

            // Parse custom price
            BigDecimal customPrice;
            try {
                customPrice = new BigDecimal(priceField.getText());
            } catch (NumberFormatException e) {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("❌ Invalid Price");
                errorAlert.setHeaderText("Invalid price format");
                errorAlert.setContentText("Please enter a valid numeric price.");
                errorAlert.showAndWait();
                return;
            }

            // AVAILABLE - Show confirmation with price
            Alert availableAlert = new Alert(Alert.AlertType.CONFIRMATION);
            availableAlert.setTitle("✅ Stock Available");
            availableAlert.setHeaderText("Confirm Addition");
            availableAlert.setContentText(
                    "Item: " + storageItem.getProductName() + "\n" +
                            "Requested: " + requestedQty + " units\n" +
                            "Custom Price: $" + customPrice + " per unit\n" +
                            "Total: $" + customPrice.multiply(new BigDecimal(requestedQty)) + "\n" +
                            "Status: ✅ AVAILABLE\n\n" +
                            "Add to project?"
            );

            availableAlert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    // Save to database
                    Task<ProjectElement> saveTask = new Task<>() {
                        @Override
                        protected ProjectElement call() {
                            ProjectElement element = new ProjectElement();
                            element.setProject(project);
                            element.setStorageItem(storageItem);
                            element.setQuantityNeeded(requestedQty);
                            element.setCustomPrice(customPrice); // Set the custom price
                            element.setNotes(notesField.getText());
                            element.setAddedBy(currentUser != null ? currentUser.getUsername() : "system");

                            // Sales module adds directly without approval (instant approval)
                            // This automatically sets status to APPROVED, allocates quantity, and deducts from storage
                            ProjectElement saved = elementService.createElementDirectly(element);

                            return saved;
                        }
                    };

                    saveTask.setOnSucceeded(event -> {
                        Platform.runLater(() -> {
                            loadProjectElements(project, contentPane);
                            refreshCostBreakdown(project, contentPane); // ✅ Refresh cost breakdown
                            showSuccess("✓ Element added! Quantity deducted from storage.");
                            parentDialog.close();
                        });
                    });

                    saveTask.setOnFailed(event ->
                            showError("Failed: " + saveTask.getException().getMessage())
                    );

                    new Thread(saveTask).start();
                }
            });
        });
    }

    // ==================== DELETE PROJECT ELEMENT ====================
    private void handleDeleteProjectElement(ProjectElement element, Project project, VBox contentPane) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Remove Element");
        confirm.setHeaderText("Remove this element from project?");

        String itemName = element.getStorageItem() != null ?
                element.getStorageItem().getProductName() : "Unknown Item";
        int returnQty = element.getQuantityAllocated() != null ?
                element.getQuantityAllocated() : 0;

        confirm.setContentText(
                "Item: " + itemName + "\n" +
                        "Allocated: " + returnQty + " units\n\n" +
                        "This will:\n" +
                        "• Remove element from project\n" +
                        "• Return " + returnQty + " units to storage\n\n" +
                        "Continue?"
        );

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Task<Void> deleteTask = new Task<>() {
                    @Override
                    protected Void call() {
                        // Return quantity to storage
                        if (element.getStorageItem() != null && returnQty > 0) {
                            StorageItem storageItem = element.getStorageItem();
                            int currentQty = storageItem.getQuantity();
                            int newQty = currentQty + returnQty;

                            storageItem.setQuantity(newQty);
                            storageService.updateItem(storageItem.getId(), storageItem);
                        }

                        // Delete element
                        elementService.deleteElement(element.getId());

                        return null;
                    }
                };

                deleteTask.setOnSucceeded(event -> {
                    loadProjectElements(project, contentPane);
                    showSuccess("✓ Element removed and " + returnQty + " units returned to storage");
                });

                deleteTask.setOnFailed(event ->
                        showError("Delete failed: " + deleteTask.getException().getMessage())
                );

                new Thread(deleteTask).start();
            }
        });
    }

    // ==================== LOAD PROJECT ELEMENTS (SYNCHRONIZED WITH PROJECTS MODULE) ====================
    private void loadProjectElements(Project project, VBox contentPane) {
        Task<List<ProjectElement>> loadTask = new Task<>() {
            @Override
            protected List<ProjectElement> call() {
                return elementService.getElementsByProject(project.getId());
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<ProjectElement> elements = loadTask.getValue();

            FlowPane elementsGrid = (FlowPane) contentPane.lookup("#elementsGrid");
            if (elementsGrid == null) return;

            Platform.runLater(() -> {
                elementsGrid.getChildren().clear();

                if (elements.isEmpty()) {
                    // Show empty state
                    VBox emptyState = new VBox(20);
                    emptyState.setAlignment(Pos.CENTER);
                    emptyState.setPadding(new Insets(100));

                    Label emptyIcon = new Label("📦");
                    emptyIcon.setStyle("-fx-font-size: 64px;");

                    Label emptyText = new Label("No Elements Added Yet");
                    emptyText.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.6); -fx-font-size: 18px;");

                    Label emptyHint = new Label("Click '+ Add Element' to add items from storage");
                    emptyHint.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.4); -fx-font-size: 14px;");

                    emptyState.getChildren().addAll(emptyIcon, emptyText, emptyHint);
                    elementsGrid.getChildren().add(emptyState);
                } else {
                    // Create cards for each element
                    for (ProjectElement element : elements) {
                        VBox card = createProjectElementCard(element, project, contentPane);
                        elementsGrid.getChildren().add(card);
                    }
                }
            });
        });

        loadTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                showError("Failed to load elements: " + loadTask.getException().getMessage());
            });
        });

        new Thread(loadTask).start();
    }

    private VBox createProjectElementCard(ProjectElement element, Project project, VBox contentPane) {
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
        String itemName = element.getStorageItem() != null ?
                element.getStorageItem().getProductName() : "Unknown Item";

        Label itemNameLabel = new Label("📦 " + itemName);
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
        String status = element.getStatus() != null ? element.getStatus() : "Pending";
        Label statusLabel = new Label(status);
        String statusColor = switch (status) {
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

        deleteButton.setOnAction(e -> handleDeleteProjectElement(element, project, contentPane));

        card.getChildren().addAll(itemNameLabel, separator, quantityBox, statusLabel, notesBox, deleteButton);
        return card;
    }


    // ==================== WORKFLOW WIZARD TAB ====================
    private VBox createWorkflowWizardTab(Project project) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: rgba(15, 23, 42, 0.6);");
        VBox.setVgrow(content, Priority.ALWAYS);

        // Header
        Label title = new Label("🔄 8-Step Workflow Wizard");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

        Label subtitle = new Label("Complete each step sequentially to move the project through the workflow");
        subtitle.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");

        VBox headerBox = new VBox(10, title, subtitle);
        headerBox.setPadding(new Insets(0, 0, 20, 0));

        // Load workflow for this project
        try {
            // Get or create workflow
            java.util.Optional<com.magictech.modules.sales.entity.ProjectWorkflow> workflowOpt =
                workflowService.getWorkflowByProjectId(project.getId());

            com.magictech.modules.sales.entity.ProjectWorkflow workflow;
            if (workflowOpt.isPresent()) {
                workflow = workflowOpt.get();
            } else {
                // Create new workflow
                workflow = workflowService.createWorkflow(project.getId(), currentUser);
            }

            // Get all step completions
            java.util.List<com.magictech.modules.sales.entity.WorkflowStepCompletion> completions =
                stepService.getAllSteps(workflow.getId());

            // Workflow Status Summary
            VBox summaryBox = new VBox(15);
            summaryBox.setPadding(new Insets(25));
            summaryBox.setStyle("-fx-background-color: rgba(30, 41, 59, 0.6); -fx-background-radius: 12px;");

            // Current step indicator
            HBox currentStepBox = new HBox(15);
            currentStepBox.setAlignment(Pos.CENTER_LEFT);

            Label currentStepIcon = new Label("📍");
            currentStepIcon.setStyle("-fx-font-size: 28px;");

            Label currentStepLabel = new Label("Current Step: " + workflow.getCurrentStep() + " of 8");
            currentStepLabel.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");

            currentStepBox.getChildren().addAll(currentStepIcon, currentStepLabel);

            // Progress indicator
            HBox progressBox = new HBox(10);
            progressBox.setAlignment(Pos.CENTER_LEFT);

            for (int i = 1; i <= 8; i++) {
                Label stepDot = new Label("●");
                if (i < workflow.getCurrentStep()) {
                    stepDot.setStyle("-fx-text-fill: #10b981; -fx-font-size: 20px;"); // Green - completed
                } else if (i == workflow.getCurrentStep()) {
                    stepDot.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 24px;"); // Amber - current
                } else {
                    stepDot.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 16px;"); // Gray - pending
                }
                progressBox.getChildren().add(stepDot);
            }

            // Step details
            GridPane stepsGrid = new GridPane();
            stepsGrid.setHgap(20);
            stepsGrid.setVgap(12);
            stepsGrid.setPadding(new Insets(15, 0, 0, 0));

            String[] stepNames = {
                "Site Survey",
                "Selection & Design (Presales)",
                "Bank Guarantee (Finance)",
                "Missing Items Approval",
                "Project Execution",
                "Project Completion",
                "QA After-Sales Check",
                "Storage Analysis"
            };

            for (int i = 0; i < stepNames.length; i++) {
                final int stepNum = i + 1;

                // Find completion for this step
                com.magictech.modules.sales.entity.WorkflowStepCompletion completion = null;
                for (com.magictech.modules.sales.entity.WorkflowStepCompletion c : completions) {
                    if (c.getStepNumber() == stepNum) {
                        completion = c;
                        break;
                    }
                }

                // Step number badge
                Label stepBadge = new Label(String.valueOf(stepNum));
                if (completion != null && completion.getCompletedAt() != null) {
                    stepBadge.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; " +
                        "-fx-padding: 5 10; -fx-background-radius: 50%; -fx-font-weight: bold;");
                } else if (stepNum == workflow.getCurrentStep()) {
                    stepBadge.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; " +
                        "-fx-padding: 5 10; -fx-background-radius: 50%; -fx-font-weight: bold;");
                } else {
                    stepBadge.setStyle("-fx-background-color: #4b5563; -fx-text-fill: white; " +
                        "-fx-padding: 5 10; -fx-background-radius: 50%; -fx-font-weight: bold;");
                }

                // Step name
                Label stepName = new Label(stepNames[i]);
                stepName.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

                // Status icon
                Label statusIcon = new Label();
                if (completion != null && completion.getCompletedAt() != null) {
                    statusIcon.setText("✓");
                    statusIcon.setStyle("-fx-text-fill: #10b981; -fx-font-size: 18px; -fx-font-weight: bold;");
                } else if (stepNum == workflow.getCurrentStep()) {
                    statusIcon.setText("⏳");
                    statusIcon.setStyle("-fx-font-size: 16px;");
                } else {
                    statusIcon.setText("○");
                    statusIcon.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 16px;");
                }

                stepsGrid.add(stepBadge, 0, i);
                stepsGrid.add(stepName, 1, i);
                stepsGrid.add(statusIcon, 2, i);
            }

            summaryBox.getChildren().addAll(currentStepBox, progressBox, stepsGrid);

            // Action buttons
            HBox actionBox = new HBox(15);
            actionBox.setAlignment(Pos.CENTER);
            actionBox.setPadding(new Insets(25, 0, 0, 0));

            Button openWorkflowBtn = createStyledButton("🔄 Open Full Workflow Wizard", "#8b5cf6", "#7c3aed");
            final com.magictech.modules.sales.entity.ProjectWorkflow finalWorkflow = workflow;
            openWorkflowBtn.setOnAction(e -> handleWorkflowDialogOpen(project, finalWorkflow));

            Button refreshBtn = createStyledButton("🔄 Refresh Status", "#6366f1", "#4f46e5");
            refreshBtn.setOnAction(e -> openProjectDetails(project));

            actionBox.getChildren().addAll(openWorkflowBtn, refreshBtn);

            content.getChildren().addAll(headerBox, summaryBox, actionBox);

        } catch (Exception ex) {
            ex.printStackTrace();
            Label errorLabel = new Label("Error loading workflow: " + ex.getMessage());
            errorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 14px;");
            content.getChildren().addAll(headerBox, errorLabel);
        }

        return content;
    }

    // Handle opening workflow dialog
    private void handleWorkflowDialogOpen(Project project,
                                          com.magictech.modules.sales.entity.ProjectWorkflow workflow) {
        try {
            WorkflowDialog dialog = new WorkflowDialog(project, currentUser, workflowService, stepService, siteSurveyRepository, sizingPricingRepository, bankGuaranteeRepository);
            dialog.showAndWait();

            // Refresh the project details after dialog closes
            openProjectDetails(project);
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Failed to open workflow dialog: " + ex.getMessage());
        }
    }

    // ==================== SIMPLIFIED PDF TAB ====================
    private VBox createSimplePDFTab(Project project) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: rgba(15, 23, 42, 0.6);");
        VBox.setVgrow(content, Priority.ALWAYS);

        // Header with upload button
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("📄 Contract Document");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Button uploadBtn = createStyledButton("📁 Upload PDF", "#3b82f6", "#2563eb");
        uploadBtn.setOnAction(e -> handlePDFUpload(project, content));

        Button downloadBtn = createStyledButton("📥 Download PDF", "#22c55e", "#16a34a");
        downloadBtn.setOnAction(e -> handlePDFDownload(project));

        Button deleteBtn = createStyledButton("🗑️ Delete PDF", "#ef4444", "#dc2626");
        deleteBtn.setOnAction(e -> handlePDFDelete(project, content));

        header.getChildren().addAll(titleLabel, uploadBtn, downloadBtn, deleteBtn);

        // PDF Preview area
        StackPane previewArea = new StackPane();
        previewArea.setId("pdfPreview");
        VBox.setVgrow(previewArea, Priority.ALWAYS);
        previewArea.setStyle(
                "-fx-background-color: #334155;" +
                        "-fx-border-color: rgba(59, 130, 246, 0.6);" +
                        "-fx-border-width: 3;" +
                        "-fx-border-radius: 8;"
        );

        Label placeholderLabel = new Label("📁\n\nNo PDF uploaded yet\n\nClick 'Upload PDF' to add a contract document");
        placeholderLabel.setStyle(
                "-fx-text-fill: rgba(255, 255, 255, 0.6);" +
                        "-fx-font-size: 18px;" +
                        "-fx-text-alignment: center;"
        );
        placeholderLabel.setAlignment(Pos.CENTER);

        previewArea.getChildren().add(placeholderLabel);

        content.getChildren().addAll(header, previewArea);

        // Load existing PDF if available
        loadExistingPDF(project, previewArea);

        return content;
    }

    // ==================== PDF UPLOAD ====================
    private void handlePDFUpload(Project project, VBox contentPane) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select PDF Contract");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );

        File selectedFile = fileChooser.showOpenDialog(contentPane.getScene().getWindow());

        if (selectedFile != null) {
            Task<Void> uploadTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    // Read PDF file
                    byte[] pdfBytes = java.nio.file.Files.readAllBytes(selectedFile.toPath());

                    // Get or create sales order
                    List<SalesOrder> orders = salesOrderService.getOrdersByProject(project.getId());
                    SalesOrder order;

                    if (orders.isEmpty()) {
                        order = new SalesOrder("PROJECT");
                        order.setProjectId(project.getId());
                        order.setCreatedBy(currentUser != null ? currentUser.getUsername() : "system");
                        order = salesOrderService.createSalesOrder(order);
                    } else {
                        order = orders.get(0);
                    }

                    // Save or update contract with PDF
                    var existingContract = salesContractService.getContractBySalesOrderId(order.getId());

                    if (existingContract.isPresent()) {
                        SalesContract contract = existingContract.get();
                        contract.setTitle("Contract for " + project.getProjectName());
                        contract.setRequirements(selectedFile.getName()); // Store filename
                        // Note: You'll need to add a field in SalesContract to store the actual PDF bytes
                        // contract.setPdfData(pdfBytes);
                        contract.setUpdatedBy(currentUser != null ? currentUser.getUsername() : "system");
                        salesContractService.updateContract(contract.getId(), contract);
                    } else {
                        SalesContract contract = new SalesContract();
                        contract.setSalesOrder(order);
                        contract.setTitle("Contract for " + project.getProjectName());
                        contract.setRequirements(selectedFile.getName());
                        // contract.setPdfData(pdfBytes);
                        contract.setCreatedBy(currentUser != null ? currentUser.getUsername() : "system");
                        salesContractService.createContract(contract);
                    }

                    return null;
                }
            };

            uploadTask.setOnSucceeded(e -> {
                showSuccess("✓ PDF uploaded successfully!");
                StackPane previewArea = (StackPane) contentPane.lookup("#pdfPreview");
                if (previewArea != null) {
                    displayPDFPreview(selectedFile, previewArea);
                }
            });

            uploadTask.setOnFailed(e -> {
                showError("Failed to upload PDF: " + uploadTask.getException().getMessage());
            });

            new Thread(uploadTask).start();
        }
    }

    // ==================== PDF DOWNLOAD ====================
    private void handlePDFDownload(Project project) {
        Task<File> downloadTask = new Task<>() {
            @Override
            protected File call() throws Exception {
                List<SalesOrder> orders = salesOrderService.getOrdersByProject(project.getId());
                if (!orders.isEmpty()) {
                    var contractOpt = salesContractService.getContractBySalesOrderId(orders.get(0).getId());
                    if (contractOpt.isPresent()) {
                        // You'll need to retrieve the PDF bytes from the contract
                        // byte[] pdfBytes = contractOpt.get().getPdfData();
                        // Save to temp file and return
                        // For now, just show a message
                        throw new Exception("PDF retrieval not yet implemented");
                    }
                }
                throw new Exception("No PDF found for this project");
            }
        };

        downloadTask.setOnSucceeded(e -> {
            // Open file chooser to save
            showSuccess("✓ PDF ready for download");
        });

        downloadTask.setOnFailed(e -> {
            showError("No PDF available for download");
        });

        new Thread(downloadTask).start();
    }

    // ==================== PDF DELETE ====================
    private void handlePDFDelete(Project project, VBox contentPane) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete PDF");
        confirm.setHeaderText("Delete this contract PDF?");
        confirm.setContentText("This action cannot be undone!");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Delete PDF from database
                Task<Void> deleteTask = new Task<>() {
                    @Override
                    protected Void call() {
                        List<SalesOrder> orders = salesOrderService.getOrdersByProject(project.getId());
                        if (!orders.isEmpty()) {
                            var contractOpt = salesContractService.getContractBySalesOrderId(orders.get(0).getId());
                            if (contractOpt.isPresent()) {
                                SalesContract contract = contractOpt.get();
                                contract.setRequirements("");
                                // contract.setPdfData(null);
                                salesContractService.updateContract(contract.getId(), contract);
                            }
                        }
                        return null;
                    }
                };

                deleteTask.setOnSucceeded(e -> {
                    showSuccess("✓ PDF deleted");
                    StackPane previewArea = (StackPane) contentPane.lookup("#pdfPreview");
                    if (previewArea != null) {
                        showPlaceholder(previewArea);
                    }
                });

                new Thread(deleteTask).start();
            }
        });
    }

    // ==================== PDF PREVIEW ====================
    private void displayPDFPreview(File pdfFile, StackPane previewArea) {
        Task<javafx.scene.image.Image> renderTask = new Task<>() {
            @Override
            protected javafx.scene.image.Image call() throws Exception {
                // Render first page of PDF as preview
                try (org.apache.pdfbox.pdmodel.PDDocument document =
                             org.apache.pdfbox.pdmodel.PDDocument.load(pdfFile)) {
                    org.apache.pdfbox.rendering.PDFRenderer renderer =
                            new org.apache.pdfbox.rendering.PDFRenderer(document);
                    java.awt.image.BufferedImage bufferedImage = renderer.renderImageWithDPI(0, 150);

                    // Convert to JavaFX Image
                    java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                    javax.imageio.ImageIO.write(bufferedImage, "png", out);
                    java.io.ByteArrayInputStream in =
                            new java.io.ByteArrayInputStream(out.toByteArray());
                    return new javafx.scene.image.Image(in);
                }
            }
        };

        renderTask.setOnSucceeded(e -> {
            javafx.scene.image.Image previewImage = renderTask.getValue();
            previewArea.getChildren().clear();

            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setStyle("-fx-background-color: transparent;");
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);

            javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(previewImage);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            imageView.fitWidthProperty().bind(scrollPane.widthProperty().subtract(40));

            scrollPane.setContent(imageView);
            previewArea.getChildren().add(scrollPane);
        });

        renderTask.setOnFailed(e -> {
            Label errorLabel = new Label("❌\n\nFailed to load PDF preview");
            errorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 16px; -fx-text-alignment: center;");
            errorLabel.setAlignment(Pos.CENTER);
            previewArea.getChildren().clear();
            previewArea.getChildren().add(errorLabel);
        });

        new Thread(renderTask).start();
    }

    private void loadExistingPDF(Project project, StackPane previewArea) {
        // Load existing PDF if available
        Task<String> loadTask = new Task<>() {
            @Override
            protected String call() {
                List<SalesOrder> orders = salesOrderService.getOrdersByProject(project.getId());
                if (!orders.isEmpty()) {
                    var contractOpt = salesContractService.getContractBySalesOrderId(orders.get(0).getId());
                    if (contractOpt.isPresent()) {
                        return contractOpt.get().getRequirements(); // Returns filename
                    }
                }
                return null;
            }
        };

        loadTask.setOnSucceeded(e -> {
            String filename = loadTask.getValue();
            if (filename != null && !filename.isEmpty()) {
                Label infoLabel = new Label("📄\n\n" + filename + "\n\nPDF is stored in database");
                infoLabel.setStyle(
                        "-fx-text-fill: white;" +
                                "-fx-font-size: 16px;" +
                                "-fx-text-alignment: center;"
                );
                infoLabel.setAlignment(Pos.CENTER);
                previewArea.getChildren().clear();
                previewArea.getChildren().add(infoLabel);
            }
        });

        new Thread(loadTask).start();
    }

    private void showPlaceholder(StackPane previewArea) {
        previewArea.getChildren().clear();
        Label placeholderLabel = new Label("📁\n\nNo PDF uploaded yet\n\nClick 'Upload PDF' to add a contract document");
        placeholderLabel.setStyle(
                "-fx-text-fill: rgba(255, 255, 255, 0.6);" +
                        "-fx-font-size: 18px;" +
                        "-fx-text-alignment: center;"
        );
        placeholderLabel.setAlignment(Pos.CENTER);
        previewArea.getChildren().add(placeholderLabel);
    }

    // Customer details view - navigates within the same container (no new window)
    private void openCustomerDetails(Customer customer) {
        VBox mainLayout = new VBox(20);
        mainLayout.setPadding(new Insets(30));
        mainLayout.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(mainLayout, Priority.ALWAYS);

        // Header with back button
        HBox headerBox = new HBox(20);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(0, 0, 10, 0));

        Button backBtn = createStyledButton("← Back to Sales Dashboard", "#6b7280", "#4b5563");
        backBtn.setOnAction(e -> returnToSalesDashboard());

        Label headerLabel = new Label("👤 " + customer.getName());
        headerLabel.setStyle("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: bold;");
        HBox.setHgrow(headerLabel, Priority.ALWAYS);

        headerBox.getChildren().addAll(backBtn, headerLabel);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        // Contract PDF Tab
        Tab contractsTab = new Tab("📄 Contract PDF");
        contractsTab.setContent(createSimplePDFTabForCustomer(customer));
        styleTab(contractsTab, "#f59e0b");

        // Cost Breakdown & Order Items Tab (replacing Fast Selling)
        Tab costBreakdownTab = new Tab("💰 Cost Breakdown");
        costBreakdownTab.setContent(createCustomerCostBreakdownTab(customer));
        styleTab(costBreakdownTab, "#7c3aed");

        tabPane.getTabs().addAll(contractsTab, costBreakdownTab);

        mainLayout.getChildren().addAll(headerBox, tabPane);

        // Replace the current view with customer details
        mainContainer.getChildren().clear();
        mainContainer.getChildren().add(mainLayout);
    }

    // Return to sales dashboard view
    private void returnToSalesDashboard() {
        mainContainer.getChildren().clear();
        mainContainer.getChildren().add(dashboardScreen);
        loadDashboardData(); // Refresh the data
    }


    private VBox createSimplePDFTabForCustomer(Customer customer) {
        // Similar to project PDF tab but for customer
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: rgba(15, 23, 42, 0.6);");
        VBox.setVgrow(content, Priority.ALWAYS);

        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("📄 Customer Contract Document");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Button uploadBtn = createStyledButton("📁 Upload PDF", "#3b82f6", "#2563eb");
        // Similar upload logic for customer

        header.getChildren().addAll(titleLabel, uploadBtn);

        StackPane previewArea = new StackPane();
        previewArea.setId("pdfPreview");
        VBox.setVgrow(previewArea, Priority.ALWAYS);
        previewArea.setStyle(
                "-fx-background-color: #334155;" +
                        "-fx-border-color: rgba(59, 130, 246, 0.6);" +
                        "-fx-border-width: 3;" +
                        "-fx-border-radius: 8;"
        );

        showPlaceholder(previewArea);

        content.getChildren().addAll(header, previewArea);
        return content;
    }

    // ==================== CUSTOMER COST BREAKDOWN TAB ====================
    private VBox createCustomerCostBreakdownTab(Customer customer) {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: rgba(15, 23, 42, 0.6);");
        VBox.setVgrow(content, Priority.ALWAYS);

        // Create TabPane for Cost Breakdown and Order Items
        TabPane subTabPane = new TabPane();
        subTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(subTabPane, Priority.ALWAYS);

        // ============ TAB 1: Cost Breakdown ============
        Tab costTab = new Tab("💰 Cost Breakdown");
        VBox costContent = new VBox(20);
        costContent.setPadding(new Insets(30));
        costContent.setStyle("-fx-background-color: rgba(15, 23, 42, 0.6);");

        com.magictech.modules.sales.ui.CustomerCostBreakdownPanel breakdownPanel =
            new com.magictech.modules.sales.ui.CustomerCostBreakdownPanel();
        breakdownPanel.setId("customerCostBreakdownPanel");

        // Load existing breakdown
        try {
            List<com.magictech.modules.sales.entity.CustomerCostBreakdown> breakdowns =
                customerCostBreakdownService.getBreakdownsByCustomer(customer.getId());
            if (!breakdowns.isEmpty()) {
                // Load the most recent breakdown
                breakdownPanel.loadBreakdown(breakdowns.get(0));
            }
        } catch (Exception ex) {
            System.err.println("Error loading cost breakdown: " + ex.getMessage());
        }

        // Set save callback
        breakdownPanel.setOnSave(breakdown -> {
            try {
                breakdown.setCustomerId(customer.getId());
                String username = (currentUser != null) ? currentUser.getUsername() : "unknown";
                if (breakdown.getId() == null) {
                    customerCostBreakdownService.createBreakdown(breakdown, username);
                } else {
                    customerCostBreakdownService.updateBreakdown(breakdown.getId(), breakdown, username);
                }
                showSuccess("Cost breakdown saved successfully!");
            } catch (Exception ex) {
                showError("Failed to save breakdown: " + ex.getMessage());
            }
        });

        costContent.getChildren().add(breakdownPanel);
        costTab.setContent(costContent);
        styleTab(costTab, "#eab308"); // Gold theme

        // ============ TAB 2: Order Items ============
        Tab orderItemsTab = new Tab("📦 Order Items");
        VBox orderItemsContent = createCustomerOrderItemsTabContent(customer, breakdownPanel);
        orderItemsTab.setContent(orderItemsContent);
        styleTab(orderItemsTab, "#a78bfa"); // Light purple theme

        // Add tabs to TabPane
        subTabPane.getTabs().addAll(costTab, orderItemsTab);

        content.getChildren().add(subTabPane);

        return content;
    }

    // ==================== ORDER ITEMS TAB CONTENT ====================
    private VBox createCustomerOrderItemsTabContent(Customer customer, com.magictech.modules.sales.ui.CustomerCostBreakdownPanel breakdownPanel) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: rgba(15, 23, 42, 0.6);");
        VBox.setVgrow(content, Priority.ALWAYS);

        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("📦 Order Items");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Button addItemsBtn = createStyledButton("+ Add Items", "#22c55e", "#16a34a");
        addItemsBtn.setOnAction(e -> handleAddItemsToCustomerWithPanel(customer, content, breakdownPanel));

        Button clearBtn = createStyledButton("🗑 Clear All", "#ef4444", "#dc2626");
        clearBtn.setOnAction(e -> handleClearCustomerOrderWithPanel(customer, content, breakdownPanel));

        header.getChildren().addAll(titleLabel, addItemsBtn, clearBtn);

        TableView<OrderItemRow> itemsTable = new TableView<>();
        itemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(itemsTable, Priority.ALWAYS);
        itemsTable.setStyle(
                "-fx-background-color: rgba(15, 23, 42, 0.9);" +
                        "-fx-control-inner-background: rgba(15, 23, 42, 0.95);" +
                        "-fx-border-color: rgba(34, 197, 94, 0.4);" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 8;"
        );

        TableColumn<OrderItemRow, String> nameCol = new TableColumn<>("Product Name");
        nameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().productName));
        styleTableColumn(nameCol);

        TableColumn<OrderItemRow, String> qtyCol = new TableColumn<>("Quantity");
        qtyCol.setPrefWidth(100);
        qtyCol.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().quantity)));
        styleTableColumn(qtyCol);

        TableColumn<OrderItemRow, String> priceCol = new TableColumn<>("Unit Price");
        priceCol.setPrefWidth(120);
        priceCol.setCellValueFactory(cellData ->
                new SimpleStringProperty("$" + cellData.getValue().unitPrice.setScale(2, RoundingMode.HALF_UP).toString())
        );
        styleTableColumn(priceCol);

        TableColumn<OrderItemRow, String> totalCol = new TableColumn<>("Total");
        totalCol.setPrefWidth(130);
        totalCol.setCellValueFactory(cellData -> {
            BigDecimal total = cellData.getValue().unitPrice.multiply(new BigDecimal(cellData.getValue().quantity));
            return new SimpleStringProperty("$" + total.setScale(2, RoundingMode.HALF_UP).toString());
        });
        totalCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold; -fx-font-size: 14px; -fx-alignment: CENTER-RIGHT;");
                }
            }
        });

        TableColumn<OrderItemRow, Void> deleteCol = new TableColumn<>("Action");
        deleteCol.setPrefWidth(100);
        deleteCol.setCellFactory(col -> new TableCell<>() {
            private final Button deleteBtn = new Button("🗑️");

            {
                deleteBtn.setStyle(
                        "-fx-background-color: #ef4444;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 8 16;" +
                                "-fx-background-radius: 6;" +
                                "-fx-cursor: hand;"
                );

                deleteBtn.setOnAction(e -> {
                    OrderItemRow row = getTableView().getItems().get(getIndex());

                    // Return items to storage IMMEDIATELY
                    Task<Void> returnTask = new Task<>() {
                        @Override
                        protected Void call() {
                            try {
                                StorageItem item = storageService.getItemById(row.storageItemId).orElse(null);
                                if (item != null) {
                                    int newQuantity = item.getQuantity() + row.quantity;
                                    item.setQuantity(newQuantity);
                                    storageService.updateItem(item.getId(), item);
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                            return null;
                        }
                    };

                    returnTask.setOnSucceeded(event -> {
                        getTableView().getItems().remove(row);
                        updateCustomerCostBreakdownPanel(getTableView(), breakdownPanel);
                        showSuccess("✓ Item removed and returned to storage!");
                    });

                    returnTask.setOnFailed(event -> {
                        showError("Failed to return item to storage!");
                    });

                    new Thread(returnTask).start();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
            }
        });

        itemsTable.getColumns().addAll(nameCol, qtyCol, priceCol, totalCol, deleteCol);

        content.getChildren().addAll(header, itemsTable);

        // Load existing order items
        loadExistingCustomerOrderItems(customer, itemsTable, breakdownPanel);

        return content;
    }

    // ==================== CUSTOMER ORDER HELPER METHODS ====================

    /**
     * Update cost breakdown panel based on table items
     */
    private void updateCustomerCostBreakdownPanel(TableView<OrderItemRow> itemsTable,
                                                   com.magictech.modules.sales.ui.CustomerCostBreakdownPanel breakdownPanel) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItemRow row : itemsTable.getItems()) {
            BigDecimal itemTotal = row.unitPrice.multiply(new BigDecimal(row.quantity));
            subtotal = subtotal.add(itemTotal);
        }
        breakdownPanel.setItemsSubtotal(subtotal);
    }

    /**
     * Load existing customer order items
     */
    private void loadExistingCustomerOrderItems(Customer customer, TableView<OrderItemRow> itemsTable,
                                                com.magictech.modules.sales.ui.CustomerCostBreakdownPanel breakdownPanel) {
        // TODO: Load from database if there's an existing order
        // For now, start with empty table
        itemsTable.setItems(FXCollections.observableArrayList());
        updateCustomerCostBreakdownPanel(itemsTable, breakdownPanel);
    }

    /**
     * Handle adding items to customer order with panel update
     */
    private void handleAddItemsToCustomerWithPanel(Customer customer, VBox orderTabContent,
                                                    com.magictech.modules.sales.ui.CustomerCostBreakdownPanel breakdownPanel) {
        Stage dialogStage = new Stage();
        dialogStage.setTitle("📦 Select Storage Items");
        dialogStage.initModality(Modality.APPLICATION_MODAL);

        VBox mainLayout = new VBox(20);
        mainLayout.setPadding(new Insets(30));
        mainLayout.setStyle("-fx-background-color: linear-gradient(to bottom right, #1e293b, #0f172a);");

        Label titleLabel = new Label("Select Items from Storage");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

        TableView<StorageItem> storageTable = new TableView<>();
        storageTable.setPrefHeight(500);
        storageTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        storageTable.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.6);" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: rgba(139, 92, 246, 0.3);" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 12;"
        );

        // Storage table columns
        TableColumn<StorageItem, String> manufactureCol = new TableColumn<>("Manufacture");
        manufactureCol.setCellValueFactory(new PropertyValueFactory<>("manufacture"));
        manufactureCol.setPrefWidth(200);
        styleTableColumn(manufactureCol);

        TableColumn<StorageItem, String> nameCol = new TableColumn<>("Product Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        nameCol.setPrefWidth(300);
        styleTableColumn(nameCol);

        // Availability column (NO QUANTITIES SHOWN - Sales should not see actual stock numbers)
        TableColumn<StorageItem, String> availabilityCol = new TableColumn<>("Availability");
        availabilityCol.setCellValueFactory(data -> {
            StorageItem item = data.getValue();
            boolean available = item.getQuantity() != null && item.getQuantity() > 0;
            return new javafx.beans.property.SimpleStringProperty(available ? "✅ AVAILABLE" : "❌ OUT OF STOCK");
        });
        availabilityCol.setPrefWidth(150);
        styleTableColumn(availabilityCol);

        TableColumn<StorageItem, BigDecimal> priceCol = new TableColumn<>("Unit Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceCol.setPrefWidth(150);
        styleTableColumn(priceCol);

        storageTable.getColumns().addAll(manufactureCol, nameCol, availabilityCol, priceCol);

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

        // Buttons
        Button addBtn = createStyledButton("Add Selected Items", "#22c55e", "#16a34a");
        addBtn.setPrefHeight(50);
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setOnAction(e -> {
            StorageItem selected = storageTable.getSelectionModel().getSelectedItem();
            if (selected != null && selected.getQuantity() > 0) {
                // Show quantity dialog
                showQuantityDialog(selected, customer, orderTabContent, breakdownPanel);
                dialogStage.close();
            } else {
                showWarning("Please select an item with available quantity");
            }
        });

        Button cancelBtn = createStyledButton("Cancel", "#6b7280", "#4b5563");
        cancelBtn.setPrefHeight(50);
        cancelBtn.setMaxWidth(Double.MAX_VALUE);
        cancelBtn.setOnAction(e -> dialogStage.close());

        mainLayout.getChildren().addAll(titleLabel, storageTable, addBtn, cancelBtn);

        Scene scene = new Scene(mainLayout, 800, 700);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }

    /**
     * Show quantity dialog for adding items
     */
    private void showQuantityDialog(StorageItem item, Customer customer, VBox orderTabContent,
                                    com.magictech.modules.sales.ui.CustomerCostBreakdownPanel breakdownPanel) {
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Enter Quantity");
        dialog.setHeaderText("How many units of " + item.getProductName() + " do you need?");

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        // Don't expose actual quantity - allow sales to enter any number
        Spinner<Integer> spinner = new Spinner<>(1, 9999, 1);
        spinner.setEditable(true);
        dialog.getDialogPane().setContent(spinner);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                return spinner.getValue();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(quantity -> {
            // Check availability internally (Sales doesn't see actual numbers)
            if (item.getQuantity() == null || item.getQuantity() < quantity) {
                showError("❌ Insufficient stock available for " + item.getProductName() +
                         "\n\nRequested: " + quantity + " units\nPlease reduce the quantity or contact Storage team.");
                return;
            }

            // Deduct from storage
            Task<Void> deductTask = new Task<>() {
                @Override
                protected Void call() {
                    item.setQuantity(item.getQuantity() - quantity);
                    storageService.updateItem(item.getId(), item);
                    return null;
                }
            };

            deductTask.setOnSucceeded(e -> {
                // Find the items table in orderTabContent
                TableView<OrderItemRow> itemsTable = findItemsTableInContent(orderTabContent);
                if (itemsTable != null) {
                    // Add to order
                    OrderItemRow newRow = new OrderItemRow(
                        item.getId(),
                        item.getProductName(),
                        quantity,
                        item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO
                    );
                    itemsTable.getItems().add(newRow);

                    // Update cost breakdown
                    updateCustomerCostBreakdownPanel(itemsTable, breakdownPanel);

                    showSuccess("✓ " + quantity + " units of " + item.getProductName() + " added to order!");
                }
            });

            new Thread(deductTask).start();
        });
    }

    /**
     * Find items table in content
     */
    private TableView<OrderItemRow> findItemsTableInContent(VBox content) {
        for (javafx.scene.Node node : content.getChildren()) {
            if (node instanceof TableView) {
                return (TableView<OrderItemRow>) node;
            }
        }
        return null;
    }

    /**
     * Handle clearing customer order with panel update
     */
    private void handleClearCustomerOrderWithPanel(Customer customer, VBox orderTabContent,
                                                    com.magictech.modules.sales.ui.CustomerCostBreakdownPanel breakdownPanel) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Clear All Items");
        confirmAlert.setHeaderText("Are you sure you want to clear all items?");
        confirmAlert.setContentText("This will return all items to storage.");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                TableView<OrderItemRow> itemsTable = findItemsTableInContent(orderTabContent);
                if (itemsTable != null) {
                    // Return items to storage
                    Task<Void> returnTask = new Task<>() {
                        @Override
                        protected Void call() {
                            for (OrderItemRow row : itemsTable.getItems()) {
                                try {
                                    StorageItem item = storageService.getItemById(row.storageItemId).orElse(null);
                                    if (item != null) {
                                        item.setQuantity(item.getQuantity() + row.quantity);
                                        storageService.updateItem(item.getId(), item);
                                    }
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                            return null;
                        }
                    };

                    returnTask.setOnSucceeded(e -> {
                        itemsTable.getItems().clear();
                        updateCustomerCostBreakdownPanel(itemsTable, breakdownPanel);
                        showSuccess("✓ All items cleared and returned to storage!");
                    });

                    new Thread(returnTask).start();
                }
            }
        });
    }

    // ==================== ORDERS TAB (Keep existing implementation) ====================
    private VBox createProjectOrdersTabRedesigned(Project project) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: rgba(15, 23, 42, 0.6);");
        VBox.setVgrow(content, Priority.ALWAYS);

        // Header
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("💰 Project Pricing & Cost Breakdown");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Button addItemsBtn = createStyledButton("+ Add Items", "#22c55e", "#16a34a");
        addItemsBtn.setOnAction(e -> handleAddItemsToProject(project, content));

        Button clearBtn = createStyledButton("🗑 Clear All", "#ef4444", "#dc2626");
        clearBtn.setOnAction(e -> handleClearProjectOrder(project, content));

        header.getChildren().addAll(titleLabel, addItemsBtn, clearBtn);

        // Items Table
        TableView<OrderItemRow> itemsTable = new TableView<>();
        itemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(itemsTable, Priority.ALWAYS);
        itemsTable.setStyle(
                "-fx-background-color: rgba(15, 23, 42, 0.9);" +
                        "-fx-control-inner-background: rgba(15, 23, 42, 0.95);" +
                        "-fx-border-color: rgba(59, 130, 246, 0.4);" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 8;"
        );

        TableColumn<OrderItemRow, String> nameCol = new TableColumn<>("Product Name");
        nameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().productName));
        styleTableColumn(nameCol);

        TableColumn<OrderItemRow, String> qtyCol = new TableColumn<>("Quantity");
        qtyCol.setPrefWidth(100);
        qtyCol.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().quantity)));
        styleTableColumn(qtyCol);

        TableColumn<OrderItemRow, String> priceCol = new TableColumn<>("Unit Price");
        priceCol.setPrefWidth(120);
        priceCol.setCellValueFactory(cellData ->
                new SimpleStringProperty("$" + cellData.getValue().unitPrice.setScale(2, RoundingMode.HALF_UP).toString())
        );
        styleTableColumn(priceCol);

        TableColumn<OrderItemRow, String> totalCol = new TableColumn<>("Total");
        totalCol.setPrefWidth(130);
        totalCol.setCellValueFactory(cellData -> {
            BigDecimal total = cellData.getValue().unitPrice.multiply(new BigDecimal(cellData.getValue().quantity));
            return new SimpleStringProperty("$" + total.setScale(2, RoundingMode.HALF_UP).toString());
        });
        totalCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold; -fx-font-size: 14px; -fx-alignment: CENTER-RIGHT;");
                }
            }
        });

        TableColumn<OrderItemRow, Void> deleteCol = new TableColumn<>("Action");
        deleteCol.setPrefWidth(100);
        deleteCol.setCellFactory(col -> new TableCell<>() {
            private final Button deleteBtn = new Button("🗑️");

            {
                deleteBtn.setStyle(
                        "-fx-background-color: #ef4444;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 8 16;" +
                                "-fx-background-radius: 6;" +
                                "-fx-cursor: hand;"
                );

                deleteBtn.setOnAction(e -> {
                    OrderItemRow row = getTableView().getItems().get(getIndex());

                    // ✅ CRITICAL FIX: Return items to storage IMMEDIATELY
                    Task<Void> returnTask = new Task<>() {
                        @Override
                        protected Void call() {
                            try {
                                StorageItem item = storageService.getItemById(row.storageItemId).orElse(null);
                                if (item != null) {
                                    int newQuantity = item.getQuantity() + row.quantity;
                                    item.setQuantity(newQuantity);
                                    storageService.updateItem(item.getId(), item);
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                            return null;
                        }
                    };

                    returnTask.setOnSucceeded(event -> {
                        getTableView().getItems().remove(row);

                        // Find the parent VBox to update cost breakdown
                        VBox parentVBox = findParentVBox(getTableView());
                        if (parentVBox != null) {
                            updateCostBreakdown(parentVBox, getTableView());
                        }

                        showSuccess("✓ Item removed and returned to storage!");
                    });

                    returnTask.setOnFailed(event -> {
                        showError("Failed to return item to storage!");
                    });

                    new Thread(returnTask).start();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
            }
        });

        itemsTable.getColumns().addAll(nameCol, qtyCol, priceCol, totalCol, deleteCol);

        // Cost Breakdown Section
        VBox costBreakdownSection = createCostBreakdownSection(itemsTable);

        // Save Button
        Button saveOrderBtn = createStyledButton("💾 Save Order & Calculate Final Price", "#8b5cf6", "#7c3aed");
        saveOrderBtn.setPrefHeight(50);
        saveOrderBtn.setMaxWidth(Double.MAX_VALUE);
        saveOrderBtn.setOnAction(e -> handleSaveProjectOrder(project, itemsTable, costBreakdownSection));

        content.getChildren().addAll(header, itemsTable, costBreakdownSection, saveOrderBtn);

        // Load existing order
        loadExistingProjectOrder(project, itemsTable, costBreakdownSection);

        return content;
    }

    // Add this method anywhere in your class (good place: after styleTab method, around line 1050)

    private <S, T> void styleTableColumn(TableColumn<S, T> column) {
        column.setStyle("-fx-alignment: CENTER; -fx-text-fill: white;");
        column.setCellFactory(col -> new TableCell<S, T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    // Set dark background for empty cells to avoid white-on-white
                    setStyle("-fx-background-color: rgba(30, 41, 59, 0.3); -fx-text-fill: transparent;");
                } else {
                    setText(item.toString());
                    // Dark background with white text for filled cells
                    setStyle("-fx-background-color: rgba(30, 41, 59, 0.6); -fx-text-fill: white; -fx-alignment: CENTER;");
                }
            }
        });
    }

    private VBox createCustomerOrdersTabRedesigned(Customer customer) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: rgba(15, 23, 42, 0.6);");
        VBox.setVgrow(content, Priority.ALWAYS);

        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("💰 Fast Selling - Customer Order");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Button addItemsBtn = createStyledButton("+ Add Items", "#22c55e", "#16a34a");
        addItemsBtn.setOnAction(e -> handleAddItemsToCustomer(customer, content));

        Button clearBtn = createStyledButton("🗑 Clear All", "#ef4444", "#dc2626");
        clearBtn.setOnAction(e -> handleClearCustomerOrder(customer, content));

        header.getChildren().addAll(titleLabel, addItemsBtn, clearBtn);

        TableView<OrderItemRow> itemsTable = new TableView<>();
        itemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(itemsTable, Priority.ALWAYS);
        itemsTable.setStyle(
                "-fx-background-color: rgba(15, 23, 42, 0.9);" +
                        "-fx-control-inner-background: rgba(15, 23, 42, 0.95);" +
                        "-fx-border-color: rgba(34, 197, 94, 0.4);" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 8;"
        );

        TableColumn<OrderItemRow, String> nameCol = new TableColumn<>("Product Name");
        nameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().productName));
        styleTableColumn(nameCol);

        TableColumn<OrderItemRow, String> qtyCol = new TableColumn<>("Quantity");
        qtyCol.setPrefWidth(100);
        qtyCol.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().quantity)));
        styleTableColumn(qtyCol);

        TableColumn<OrderItemRow, String> priceCol = new TableColumn<>("Unit Price");
        priceCol.setPrefWidth(120);
        priceCol.setCellValueFactory(cellData ->
                new SimpleStringProperty("$" + cellData.getValue().unitPrice.setScale(2, RoundingMode.HALF_UP).toString())
        );
        styleTableColumn(priceCol);

        TableColumn<OrderItemRow, String> totalCol = new TableColumn<>("Total");
        totalCol.setPrefWidth(130);
        totalCol.setCellValueFactory(cellData -> {
            BigDecimal total = cellData.getValue().unitPrice.multiply(new BigDecimal(cellData.getValue().quantity));
            return new SimpleStringProperty("$" + total.setScale(2, RoundingMode.HALF_UP).toString());
        });
        totalCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold; -fx-font-size: 14px; -fx-alignment: CENTER-RIGHT;");
                }
            }
        });

        TableColumn<OrderItemRow, Void> deleteCol = new TableColumn<>("Action");
        deleteCol.setPrefWidth(100);
        deleteCol.setCellFactory(col -> new TableCell<>() {
            private final Button deleteBtn = new Button("🗑️");

            {
                deleteBtn.setStyle(
                        "-fx-background-color: #ef4444;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 8 16;" +
                                "-fx-background-radius: 6;" +
                                "-fx-cursor: hand;"
                );

                deleteBtn.setOnAction(e -> {
                    OrderItemRow row = getTableView().getItems().get(getIndex());

                    // ✅ CRITICAL FIX: Return items to storage IMMEDIATELY
                    Task<Void> returnTask = new Task<>() {
                        @Override
                        protected Void call() {
                            try {
                                StorageItem item = storageService.getItemById(row.storageItemId).orElse(null);
                                if (item != null) {
                                    int newQuantity = item.getQuantity() + row.quantity;
                                    item.setQuantity(newQuantity);
                                    storageService.updateItem(item.getId(), item);
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                            return null;
                        }
                    };

                    returnTask.setOnSucceeded(event -> {
                        getTableView().getItems().remove(row);

                        // Find the parent VBox to update cost breakdown
                        VBox parentVBox = findParentVBox(getTableView());
                        if (parentVBox != null) {
                            updateCostBreakdown(parentVBox, getTableView());
                        }

                        showSuccess("✓ Item removed and returned to storage!");
                    });

                    returnTask.setOnFailed(event -> {
                        showError("Failed to return item to storage!");
                    });

                    new Thread(returnTask).start();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
            }
        });

        itemsTable.getColumns().addAll(nameCol, qtyCol, priceCol, totalCol, deleteCol);

        VBox costBreakdownSection = createCostBreakdownSection(itemsTable);

        Button saveOrderBtn = createStyledButton("💾 Save Order & Calculate Final Price", "#8b5cf6", "#7c3aed");
        saveOrderBtn.setPrefHeight(50);
        saveOrderBtn.setMaxWidth(Double.MAX_VALUE);
        saveOrderBtn.setOnAction(e -> handleSaveCustomerOrder(customer, itemsTable, costBreakdownSection));

        content.getChildren().addAll(header, itemsTable, costBreakdownSection, saveOrderBtn);

        loadExistingCustomerOrder(customer, itemsTable, costBreakdownSection);

        return content;
    }

    // ==================== ADD PROJECT/CUSTOMER DIALOGS ====================
    /**
     * Add new project - Always uses workflow mode (8-step process)
     * No selling mode dialog needed - Projects submodule always uses workflow
     */
    private void handleAddProject() {
        // Directly create project with workflow - no mode selection dialog
        Dialog<Project> dialog = new Dialog<>();
        dialog.setTitle("🏗️ Create New Project");
        dialog.setHeaderText("Enter Project Details - 8-Step Workflow Will Start");

        ButtonType createButtonType = new ButtonType("Create & Start Workflow", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField();
        nameField.setPromptText("Project name");

        TextField locationField = new TextField();
        locationField.setPromptText("Location");

        Label infoLabel = new Label("⚠️ After creation, the 8-step workflow wizard will open automatically.");
        infoLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-style: italic;");

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Location:"), 0, 1);
        grid.add(locationField, 1, 1);
        grid.add(infoLabel, 0, 2, 2, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                Project project = new Project();
                project.setProjectName(nameField.getText());
                project.setProjectLocation(locationField.getText());
                project.setCreatedBy(currentUser != null ? currentUser.getUsername() : "system");
                return project;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(project -> {
            Task<Project> saveTask = new Task<>() {
                @Override
                protected Project call() {
                    return projectService.createProject(project);
                }
            };

            saveTask.setOnSucceeded(e -> {
                Project savedProject = saveTask.getValue();
                loadProjects(projectsListView);

                // Show success message first, wait for user to click OK
                showSuccess("✓ Project created successfully!");

                // Then open workflow dialog after user acknowledges
                openWorkflowDialog(savedProject);
            });

            saveTask.setOnFailed(e -> {
                showError("Failed to create project: " + saveTask.getException().getMessage());
            });

            new Thread(saveTask).start();
        });
    }

    /**
     * Open the 8-step workflow dialog
     */
    private void openWorkflowDialog(Project project) {
        try {
            // Validate currentUser before opening workflow
            if (currentUser == null) {
                showError("Error: Current user is not set. Please logout and login again.");
                System.err.println("ERROR: currentUser is null in SalesStorageController.openWorkflowDialog");
                return;
            }

            WorkflowDialog workflowDialog = new WorkflowDialog(
                project,
                currentUser,
                workflowService,
                stepService,
                siteSurveyRepository,
                sizingPricingRepository,
                bankGuaranteeRepository
            );
            workflowDialog.showAndWait();

            // Refresh projects list after workflow dialog closes
            loadProjects(projectsListView);
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Failed to open workflow: " + ex.getMessage());
        }
    }

    /**
     * Create a test site survey request to demonstrate notification system
     * This creates a pending request that PROJECT team will see
     */
    private void handleCreateTestSiteSurveyRequest() {
        // Check if there are any projects first
        if (projectsListView == null || projectsListView.getItems().isEmpty()) {
            showWarning("Please create a project first before creating a site survey request");
            return;
        }

        // Let user select a project
        ChoiceDialog<Project> choiceDialog = new ChoiceDialog<>(
            projectsListView.getItems().get(0),
            projectsListView.getItems()
        );
        choiceDialog.setTitle("Select Project");
        choiceDialog.setHeaderText("Create Test Site Survey Request");
        choiceDialog.setContentText("Select project for site survey:");

        choiceDialog.showAndWait().ifPresent(selectedProject -> {
            Task<Void> createTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    // Create the site survey request through the workflow service
                    // This will send a notification to PROJECT team members
                    projectService.requestSiteSurvey(
                        selectedProject.getId(),
                        currentUser.getUsername(),
                        currentUser.getId(),
                        "PROJECT",  // assigned to PROJECT team
                        "HIGH",     // priority
                        "TEST REQUEST - Site survey needed for project demonstration"
                    );

                    System.out.println("✅ Site survey request created for project: " + selectedProject.getProjectName());
                    System.out.println("📢 Notification should be sent to PROJECT team members");

                    return null;
                }
            };

            createTask.setOnSucceeded(e -> {
                showSuccess("Site survey request sent to Project Team. Waiting for their response...\n\n" +
                    "PROJECT team members should now see a notification.");
                loadProjects(projectsListView);
            });

            createTask.setOnFailed(e -> {
                Throwable ex = createTask.getException();
                showError("Failed to create site survey request: " + ex.getMessage());
                ex.printStackTrace();
            });

            new Thread(createTask).start();
        });
    }

    private void handleDeleteProject(Project project) {
        // Confirmation dialog
        javafx.scene.control.Alert confirmDialog = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Delete Project");
        confirmDialog.setHeaderText("Delete Project: " + project.getProjectName());
        confirmDialog.setContentText("Are you sure you want to delete this project? This action cannot be undone.\n\nAll project elements and associated data will be removed.");

        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Task<Void> deleteTask = new Task<>() {
                    @Override
                    protected Void call() {
                        projectService.deleteProject(project.getId());
                        return null;
                    }
                };

                deleteTask.setOnSucceeded(e -> {
                    showSuccess("✓ Project deleted successfully!");
                    // Refresh the projects list instead of navigating away
                    loadProjects(projectsListView);
                });

                deleteTask.setOnFailed(e -> {
                    showError("Failed to delete project: " + deleteTask.getException().getMessage());
                });

                new Thread(deleteTask).start();
            }
        });
    }

    private void handleAddCustomer() {
        Dialog<Customer> dialog = new Dialog<>();
        dialog.setTitle("👤 Create New Customer");
        dialog.setHeaderText("Enter Customer Details");

        ButtonType createButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField();
        nameField.setPromptText("Customer name");

        TextField emailField = new TextField();
        emailField.setPromptText("Email (optional)");

        TextField phoneField = new TextField();
        phoneField.setPromptText("Phone");

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Email:"), 0, 1);
        grid.add(emailField, 1, 1);
        grid.add(new Label("Phone:"), 0, 2);
        grid.add(phoneField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                Customer customer = new Customer();
                customer.setName(nameField.getText());
                customer.setEmail(emailField.getText().isEmpty() ? null : emailField.getText());
                customer.setPhone(phoneField.getText());
                customer.setCreatedBy(currentUser != null ? currentUser.getUsername() : "system");
                return customer;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(customer -> {
            Task<Customer> saveTask = new Task<>() {
                @Override
                protected Customer call() {
                    return customerService.createCustomer(customer);
                }
            };

            saveTask.setOnSucceeded(e -> {
                showSuccess("✓ Customer created!");
                loadCustomers(customersListView);
            });

            new Thread(saveTask).start();
        });
    }

    private void handleEditCustomer(Customer customer) {
        Dialog<Customer> dialog = new Dialog<>();
        dialog.setTitle("✏️ Edit Customer");
        dialog.setHeaderText("Edit Customer Details");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField(customer.getName());
        nameField.setPromptText("Customer name");

        TextField emailField = new TextField(customer.getEmail() != null ? customer.getEmail() : "");
        emailField.setPromptText("Email (optional)");

        TextField phoneField = new TextField(customer.getPhone() != null ? customer.getPhone() : "");
        phoneField.setPromptText("Phone");

        TextField addressField = new TextField(customer.getAddress() != null ? customer.getAddress() : "");
        addressField.setPromptText("Address (optional)");

        TextField companyField = new TextField(customer.getCompany() != null ? customer.getCompany() : "");
        companyField.setPromptText("Company (optional)");

        grid.add(new Label("Name:*"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Email:"), 0, 1);
        grid.add(emailField, 1, 1);
        grid.add(new Label("Phone:"), 0, 2);
        grid.add(phoneField, 1, 2);
        grid.add(new Label("Address:"), 0, 3);
        grid.add(addressField, 1, 3);
        grid.add(new Label("Company:"), 0, 4);
        grid.add(companyField, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                if (nameField.getText().trim().isEmpty()) {
                    showError("Customer name is required");
                    return null;
                }

                customer.setName(nameField.getText().trim());
                customer.setEmail(emailField.getText().trim().isEmpty() ? null : emailField.getText().trim());
                customer.setPhone(phoneField.getText().trim().isEmpty() ? null : phoneField.getText().trim());
                customer.setAddress(addressField.getText().trim().isEmpty() ? null : addressField.getText().trim());
                customer.setCompany(companyField.getText().trim().isEmpty() ? null : companyField.getText().trim());
                customer.setUpdatedBy(currentUser != null ? currentUser.getUsername() : "system");
                return customer;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updatedCustomer -> {
            Task<Customer> updateTask = new Task<>() {
                @Override
                protected Customer call() {
                    return customerService.updateCustomer(updatedCustomer.getId(), updatedCustomer);
                }
            };

            updateTask.setOnSucceeded(e -> {
                showSuccess("✓ Customer updated successfully!");
                loadCustomers(customersListView);
            });

            updateTask.setOnFailed(e -> {
                showError("Failed to update customer: " + updateTask.getException().getMessage());
            });

            new Thread(updateTask).start();
        });
    }

    private void handleDeleteCustomer(Customer customer) {
        if (showConfirmation("Delete Customer",
                "Are you sure you want to delete customer: " + customer.getName() + "?\n\n" +
                "This will soft-delete the customer (can be restored from database).")) {
            Task<Void> deleteTask = new Task<>() {
                @Override
                protected Void call() {
                    customerService.deleteCustomer(customer.getId());
                    return null;
                }
            };

            deleteTask.setOnSucceeded(e -> {
                showSuccess("✓ Customer deleted successfully!");
                loadCustomers(customersListView);
            });

            deleteTask.setOnFailed(e -> {
                showError("Failed to delete customer: " + deleteTask.getException().getMessage());
            });

            new Thread(deleteTask).start();
        }
    }

    // ==================== EXCEL EXPORT ====================

    private void handleExportProjects() {
        List<Project> allProjects = projectsListView.getItems();

        if (allProjects.isEmpty()) {
            showWarning("No projects to export");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Projects to Excel");
        fileChooser.setInitialFileName("projects_export_" +
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx")
        );

        File file = fileChooser.showSaveDialog(mainContainer.getScene().getWindow());

        if (file != null) {
            Task<File> exportTask = new Task<>() {
                @Override
                protected File call() throws Exception {
                    return salesExcelExportService.exportProjectsToExcel(allProjects, file.getAbsolutePath());
                }
            };

            exportTask.setOnSucceeded(e -> {
                showSuccess("✓ Exported " + allProjects.size() + " project(s) to Excel!\nFile: " + file.getName());
            });

            exportTask.setOnFailed(e -> {
                showError("Export failed: " + exportTask.getException().getMessage());
                exportTask.getException().printStackTrace();
            });

            new Thread(exportTask).start();
        }
    }

    private void handleExportCustomers() {
        List<Customer> allCustomers = customersListView.getItems();

        if (allCustomers.isEmpty()) {
            showWarning("No customers to export");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Customers to Excel");
        fileChooser.setInitialFileName("customers_export_" +
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx")
        );

        File file = fileChooser.showSaveDialog(mainContainer.getScene().getWindow());

        if (file != null) {
            Task<File> exportTask = new Task<>() {
                @Override
                protected File call() throws Exception {
                    return salesExcelExportService.exportCustomersToExcel(allCustomers, file.getAbsolutePath());
                }
            };

            exportTask.setOnSucceeded(e -> {
                showSuccess("✓ Exported " + allCustomers.size() + " customer(s) to Excel!\nFile: " + file.getName());
            });

            exportTask.setOnFailed(e -> {
                showError("Export failed: " + exportTask.getException().getMessage());
                exportTask.getException().printStackTrace();
            });

            new Thread(exportTask).start();
        }
    }

    // ==================== DATA LOADING ====================
    private void loadDashboardData() {
        // Data loading handled by individual list views
    }

    private void loadProjects(ListView<Project> listView) {
        Task<List<Project>> loadTask = new Task<>() {
            @Override
            protected List<Project> call() {
                return projectService.getAllProjects();
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<Project> projects = loadTask.getValue();
            Platform.runLater(() -> listView.getItems().setAll(projects));
        });

        new Thread(loadTask).start();
    }

    private void loadCustomers(ListView<Customer> listView) {
        Task<List<Customer>> loadTask = new Task<>() {
            @Override
            protected List<Customer> call() {
                return customerService.getAllCustomers();
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<Customer> customers = loadTask.getValue();
            listView.setItems(FXCollections.observableArrayList(customers));
        });

        new Thread(loadTask).start();
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
                        "-fx-cursor: hand;"
        );

        button.setOnMouseEntered(e -> button.setStyle(
                "-fx-background-color: " + hoverColor + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 12 24;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        ));

        button.setOnMouseExited(e -> button.setStyle(
                "-fx-background-color: " + normalColor + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 12 24;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        ));

        return button;
    }

    private void styleTab(Tab tab, String color) {
        tab.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 16px;" +
                        "-fx-font-weight: bold;"
        );
    }

    private void navigateToDashboard() {
        if (backgroundPane != null) {
            backgroundPane.stopAnimation();
        }
        com.magictech.core.ui.SceneManager.getInstance().showMainDashboard();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public void immediateCleanup() {
        if (backgroundPane != null) {
            backgroundPane.stopAnimation();
            backgroundPane = null;
        }
        System.out.println("✓ SalesStorageController cleanup completed");
    }

    // ==================== COST BREAKDOWN SECTION ====================
    private VBox createCostBreakdownSection(TableView<OrderItemRow> itemsTable) {
        VBox section = new VBox(20);
        section.setPadding(new Insets(25));
        section.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.8);" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: rgba(139, 92, 246, 0.6);" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 12;"
        );

        Label sectionTitle = new Label("💵 Cost Breakdown");
        sectionTitle.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");

        GridPane grid = new GridPane();
        grid.setHgap(30);
        grid.setVgap(15);
        grid.setPadding(new Insets(15, 0, 0, 0));

        // Items Subtotal (auto-calculated)
        Label subtotalLbl = createCostLabel("Items Subtotal:");
        Label subtotalValue = createCostValueLabel("$0.00");
        subtotalValue.setId("subtotalValue");

        // Tax
        Label taxLbl = createCostLabel("Tax:");
        TextField taxField = createCostTextField("0.00");
        taxField.setId("taxField");
        taxField.textProperty().addListener((obs, old, newVal) -> updateTotalFromFields(grid, itemsTable));

        // Sale Discount
        Label discountLbl = createCostLabel("Sale Discount:");
        TextField discountField = createCostTextField("0.00");
        discountField.setId("discountField");
        discountField.textProperty().addListener((obs, old, newVal) -> updateTotalFromFields(grid, itemsTable));

        // Crew Cost
        Label crewLbl = createCostLabel("Crew Cost:");
        TextField crewField = createCostTextField("0.00");
        crewField.setId("crewField");
        crewField.textProperty().addListener((obs, old, newVal) -> updateTotalFromFields(grid, itemsTable));

        // Additional Materials
        Label materialsLbl = createCostLabel("Additional Materials:");
        TextField materialsField = createCostTextField("0.00");
        materialsField.setId("materialsField");
        materialsField.textProperty().addListener((obs, old, newVal) -> updateTotalFromFields(grid, itemsTable));

        // Total (auto-calculated)
        Label totalLbl = createCostLabel("TOTAL PROJECT COST:");
        totalLbl.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");
        Label totalValue = createCostValueLabel("$0.00");
        totalValue.setId("totalValue");
        totalValue.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 28px; -fx-font-weight: bold;");

        grid.add(subtotalLbl, 0, 0);
        grid.add(subtotalValue, 1, 0);
        grid.add(taxLbl, 0, 1);
        grid.add(taxField, 1, 1);
        grid.add(discountLbl, 0, 2);
        grid.add(discountField, 1, 2);
        grid.add(crewLbl, 0, 3);
        grid.add(crewField, 1, 3);
        grid.add(materialsLbl, 0, 4);
        grid.add(materialsField, 1, 4);

        javafx.scene.shape.Line separator = new javafx.scene.shape.Line();
        separator.setEndX(600);
        separator.setStroke(javafx.scene.paint.Color.web("rgba(139, 92, 246, 0.6)"));
        separator.setStrokeWidth(2);

        HBox totalBox = new HBox(30);
        totalBox.setAlignment(Pos.CENTER_RIGHT);
        totalBox.setPadding(new Insets(10, 0, 0, 0));
        totalBox.getChildren().addAll(totalLbl, totalValue);

        section.getChildren().addAll(sectionTitle, grid, separator, totalBox);
        section.setUserData(grid);

        return section;
    }

    // ==================== ADD ITEMS TO PROJECT ====================
    private void handleAddItemsToProject(Project project, VBox orderTabContent) {
        Stage dialogStage = new Stage();
        dialogStage.setTitle("📦 Select Storage Items");
        dialogStage.initModality(Modality.APPLICATION_MODAL);

        VBox mainLayout = new VBox(20);
        mainLayout.setPadding(new Insets(30));
        mainLayout.setStyle("-fx-background-color: linear-gradient(to bottom right, #1e293b, #0f172a);");

        Label titleLabel = new Label("Select Items from Storage");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

        TableView<StorageItem> storageTable = new TableView<>();
        storageTable.setPrefHeight(500);
        storageTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        storageTable.setStyle(
                "-fx-background-color: #0f172a;" +
                        "-fx-control-inner-background: #0f172a;"
        );

        TableColumn<StorageItem, String> nameCol = new TableColumn<>("Product Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        styleTableColumn(nameCol);

        TableColumn<StorageItem, String> mfgCol = new TableColumn<>("Manufacture");
        mfgCol.setPrefWidth(200);
        mfgCol.setCellValueFactory(new PropertyValueFactory<>("manufacture"));
        styleTableColumn(mfgCol);

        TableColumn<StorageItem, String> codeCol = new TableColumn<>("Code");
        codeCol.setPrefWidth(140);
        codeCol.setCellValueFactory(new PropertyValueFactory<>("code"));
        styleTableColumn(codeCol);

        TableColumn<StorageItem, String> availCol = new TableColumn<>("Status");
        availCol.setPrefWidth(150);
        availCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                } else {
                    StorageItem storageItem = getTableView().getItems().get(getIndex());
                    boolean isAvailable = storageItem.getQuantity() > 0;

                    setText(isAvailable ? "✅ AVAILABLE" : "❌ OUT OF STOCK");
                    setStyle(isAvailable ?
                            "-fx-text-fill: #22c55e; -fx-font-weight: bold; -fx-alignment: CENTER;" :
                            "-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-alignment: CENTER;"
                    );
                }
            }
        });

        TableColumn<StorageItem, Void> actionCol = new TableColumn<>("Action");
        actionCol.setPrefWidth(120);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button addBtn = new Button("+ Add");

            {
                addBtn.setStyle(
                        "-fx-background-color: #22c55e;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 8 20;" +
                                "-fx-background-radius: 6;" +
                                "-fx-cursor: hand;"
                );

                addBtn.setOnAction(e -> {
                    StorageItem selectedItem = getTableView().getItems().get(getIndex());
                    handleAddItemWithQuantityDialog(selectedItem, project, orderTabContent, dialogStage);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    StorageItem storageItem = getTableView().getItems().get(getIndex());
                    addBtn.setDisable(storageItem.getQuantity() == 0);
                    setGraphic(addBtn);
                }
            }
        });

        storageTable.getColumns().addAll(nameCol, mfgCol, codeCol, availCol, actionCol);

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

        Button closeButton = createStyledButton("✗ Close", "#6b7280", "#4b5563");
        closeButton.setPrefHeight(45);
        closeButton.setOnAction(e -> dialogStage.close());

        mainLayout.getChildren().addAll(titleLabel, storageTable, closeButton);

        Scene scene = new Scene(mainLayout, 1200, 700);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }

    // ==================== ADD ITEM WITH QUANTITY DIALOG ====================
    private void handleAddItemWithQuantityDialog(StorageItem storageItem, Project project, VBox orderTabContent, Stage parentDialog) {
        Dialog<OrderItemRow> dialog = new Dialog<>();
        dialog.setTitle("📦 Specify Quantity & Price");
        dialog.setHeaderText("Add: " + storageItem.getProductName());
        styleDialogHeader(dialog);

        ButtonType checkAvailabilityBtn = new ButtonType("✓ Check Availability", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(checkAvailabilityBtn, ButtonType.CANCEL);

        GridPane grid = createDialogGrid();

        // Quantity
        Label qtyLbl = createFieldLabel("Quantity Needed:*");
        Spinner<Integer> qtySpinner = new Spinner<>(1, 999999, 1);
        qtySpinner.setEditable(true);
        qtySpinner.setPrefWidth(200);
        qtySpinner.setStyle("-fx-background-color: #1e293b;");
        qtySpinner.getEditor().setStyle("-fx-text-fill: white; -fx-background-color: #1e293b; -fx-font-size: 14px;");

        // Price AUTO-FILLED from storage
        Label priceLbl = createFieldLabel("Unit Price:");
        BigDecimal itemPrice = storageItem.getPrice() != null ? storageItem.getPrice() : BigDecimal.ZERO;
        String priceValue = "$" + itemPrice.setScale(2, RoundingMode.HALF_UP).toString();

        Label priceDisplayLabel = new Label(priceValue);
        priceDisplayLabel.setStyle(
                "-fx-background-color: rgba(34, 197, 94, 0.2);" +
                        "-fx-text-fill: #22c55e;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 18px;" +
                        "-fx-border-color: rgba(34, 197, 94, 0.5);" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 4;" +
                        "-fx-padding: 10;" +
                        "-fx-min-width: 200;"
        );

        Label priceHint = new Label("(Automatically from storage database)");
        priceHint.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.6); -fx-font-size: 11px; -fx-font-style: italic;");

        grid.add(qtyLbl, 0, 0);
        grid.add(qtySpinner, 1, 0);
        grid.add(priceLbl, 0, 1);
        grid.add(priceDisplayLabel, 1, 1);
        grid.add(priceHint, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setStyle("-fx-background-color: #0f172a;");

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == checkAvailabilityBtn) {
                int quantity = qtySpinner.getValue();

                if (quantity <= 0) {
                    Platform.runLater(() -> showWarning("Quantity must be greater than zero!"));
                    return null;
                }

                BigDecimal price = storageItem.getPrice() != null ? storageItem.getPrice() : BigDecimal.ZERO;

                return new OrderItemRow(
                        storageItem.getId(),
                        storageItem.getProductName(),
                        quantity,
                        price
                );
            }
            return null;
        });

        dialog.showAndWait().ifPresent(orderRow -> {
            int availableQty = storageItem.getQuantity();

            if (orderRow.quantity > availableQty) {
                Alert notAvailableAlert = new Alert(Alert.AlertType.ERROR);
                notAvailableAlert.setTitle("❌ Insufficient Stock");
                notAvailableAlert.setHeaderText("Cannot allocate " + orderRow.quantity + " units");
                notAvailableAlert.setContentText(
                        "Requested: " + orderRow.quantity + " units\n" +
                                "Available: " + availableQty + " units\n" +
                                "Status: ❌ INSUFFICIENT STOCK"
                );
                notAvailableAlert.showAndWait();
                return;
            }

            // ✅ CRITICAL FIX: Reduce storage quantity IMMEDIATELY in database
            Task<Boolean> allocateTask = new Task<>() {
                @Override
                protected Boolean call() {
                    try {
                        StorageItem item = storageService.getItemById(storageItem.getId()).orElse(null);
                        if (item != null) {
                            int newQuantity = item.getQuantity() - orderRow.quantity;
                            item.setQuantity(newQuantity);
                            storageService.updateItem(item.getId(), item);
                            return true;
                        }
                        return false;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                }
            };

            allocateTask.setOnSucceeded(e -> {
                Boolean success = allocateTask.getValue();
                if (success) {
                    @SuppressWarnings("unchecked")
                    TableView<OrderItemRow> itemsTable = (TableView<OrderItemRow>)
                            orderTabContent.getChildren().stream()
                                    .filter(node -> node instanceof TableView)
                                    .findFirst()
                                    .orElse(null);

                    if (itemsTable != null) {
                        itemsTable.getItems().add(orderRow);
                        updateCostBreakdown(orderTabContent, itemsTable);
                        parentDialog.close();
                        showSuccess("✓ Item allocated! Storage updated: " + (availableQty - orderRow.quantity) + " remaining");
                    }
                } else {
                    showError("Failed to allocate stock from storage!");
                }
            });

            allocateTask.setOnFailed(e -> {
                showError("Failed to allocate stock: " + allocateTask.getException().getMessage());
            });

            new Thread(allocateTask).start();
        });
    }

    // ==================== UPDATE COST BREAKDOWN ====================
    private void updateCostBreakdown(VBox orderTabContent, TableView<OrderItemRow> itemsTable) {
        VBox costSection = (VBox) orderTabContent.getChildren().stream()
                .filter(node -> node instanceof VBox && node.getUserData() instanceof GridPane)
                .findFirst()
                .orElse(null);
        if (costSection == null) return;

        GridPane grid = (GridPane) costSection.getUserData();

        BigDecimal subtotal = itemsTable.getItems().stream()
                .map(row -> row.unitPrice.multiply(new BigDecimal(row.quantity)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Label subtotalValue = (Label) grid.lookup("#subtotalValue");
        if (subtotalValue != null) {
            subtotalValue.setText("$" + subtotal.setScale(2, RoundingMode.HALF_UP).toString());
        }

        updateTotalFromFields(grid, itemsTable);
    }

    private VBox findParentVBox(javafx.scene.Node node) {
        javafx.scene.Parent parent = node.getParent();
        while (parent != null) {
            if (parent instanceof VBox) {
                return (VBox) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    private void updateTotalFromFields(GridPane grid, TableView<OrderItemRow> itemsTable) {
        try {
            BigDecimal subtotal = itemsTable.getItems().stream()
                    .map(row -> row.unitPrice.multiply(new BigDecimal(row.quantity)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            TextField taxField = (TextField) grid.lookup("#taxField");
            TextField discountField = (TextField) grid.lookup("#discountField");
            TextField crewField = (TextField) grid.lookup("#crewField");
            TextField materialsField = (TextField) grid.lookup("#materialsField");

            BigDecimal tax = parseBigDecimal(taxField.getText());
            BigDecimal discount = parseBigDecimal(discountField.getText());
            BigDecimal crew = parseBigDecimal(crewField.getText());
            BigDecimal materials = parseBigDecimal(materialsField.getText());

            BigDecimal total = subtotal
                    .add(tax)
                    .subtract(discount)
                    .add(crew)
                    .add(materials);

            Label totalValue = (Label) grid.lookup("#totalValue");
            if (totalValue != null) {
                totalValue.setText("$" + total.setScale(2, RoundingMode.HALF_UP).toString());
            }

        } catch (Exception e) {
            System.err.println("Error calculating total: " + e.getMessage());
        }
    }

    // ==================== SAVE PROJECT ORDER ====================
    private void handleSaveProjectOrder(Project project, TableView<OrderItemRow> itemsTable, VBox costSection) {
        if (itemsTable.getItems().isEmpty()) {
            showWarning("Please add items to the order first");
            return;
        }

        GridPane grid = (GridPane) costSection.getUserData();

        Task<Void> saveTask = new Task<>() {
            @Override
            protected Void call() {
                List<SalesOrder> orders = salesOrderService.getOrdersByProject(project.getId());
                SalesOrder order;

                if (orders.isEmpty()) {
                    order = new SalesOrder("PROJECT");
                    order.setProjectId(project.getId());
                    order.setCreatedBy(currentUser != null ? currentUser.getUsername() : "system");
                    order = salesOrderService.createSalesOrder(order);
                } else {
                    order = orders.get(0);
                }

                // ✅ CRITICAL FIX: Return old items to storage before clearing
                List<SalesOrderItem> existingItems = salesOrderService.getOrderItems(order.getId());
                for (SalesOrderItem item : existingItems) {
                    try {
                        StorageItem storageItem = storageService.getItemById(item.getStorageItemId()).orElse(null);
                        if (storageItem != null) {
                            // Return old quantity back to storage
                            int newQuantity = storageItem.getQuantity() + item.getQuantity();
                            storageItem.setQuantity(newQuantity);
                            storageService.updateItem(storageItem.getId(), storageItem);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    salesOrderService.removeItemFromOrder(item.getId());
                }

                // ✅ Now allocate new items (they're already allocated when added, so just save to order)
                for (OrderItemRow row : itemsTable.getItems()) {
                    SalesOrderItem item = new SalesOrderItem(row.storageItemId, row.quantity, row.unitPrice);
                    salesOrderService.addItemToOrder(order.getId(), item);
                }

                TextField taxField = (TextField) grid.lookup("#taxField");
                TextField discountField = (TextField) grid.lookup("#discountField");
                TextField crewField = (TextField) grid.lookup("#crewField");
                TextField materialsField = (TextField) grid.lookup("#materialsField");

                order.setTax(parseBigDecimal(taxField.getText()));
                order.setSaleDiscount(parseBigDecimal(discountField.getText()));
                order.setCrewCost(parseBigDecimal(crewField.getText()));
                order.setAdditionalMaterials(parseBigDecimal(materialsField.getText()));
                order.setUpdatedBy(currentUser != null ? currentUser.getUsername() : "system");

                salesOrderService.updateSalesOrder(order.getId(), order);

                return null;
            }
        };

        saveTask.setOnSucceeded(e -> showSuccess("✓ Order saved successfully! Storage database updated."));
        saveTask.setOnFailed(e -> showError("Failed to save: " + saveTask.getException().getMessage()));

        new Thread(saveTask).start();
    }

    // ==================== LOAD EXISTING PROJECT ORDER ====================
    private void loadExistingProjectOrder(Project project, TableView<OrderItemRow> itemsTable, VBox costSection) {
        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() {
                List<SalesOrder> orders = salesOrderService.getOrdersByProject(project.getId());
                if (orders.isEmpty()) return null;

                SalesOrder order = orders.get(0);
                List<SalesOrderItem> items = salesOrderService.getOrderItems(order.getId());

                Platform.runLater(() -> {
                    // ✅ FIX: Just load items, don't allocate again (they're already allocated)
                    for (SalesOrderItem item : items) {
                        StorageItem storageItem = storageService.getItemById(item.getStorageItemId()).orElse(null);
                        if (storageItem != null) {
                            itemsTable.getItems().add(new OrderItemRow(
                                    storageItem.getId(),
                                    storageItem.getProductName(),
                                    item.getQuantity(),
                                    item.getUnitPrice()
                            ));
                        }
                    }

                    GridPane grid = (GridPane) costSection.getUserData();
                    TextField taxField = (TextField) grid.lookup("#taxField");
                    TextField discountField = (TextField) grid.lookup("#discountField");
                    TextField crewField = (TextField) grid.lookup("#crewField");
                    TextField materialsField = (TextField) grid.lookup("#materialsField");

                    if (taxField != null) taxField.setText(order.getTax().toString());
                    if (discountField != null) discountField.setText(order.getSaleDiscount().toString());
                    if (crewField != null) crewField.setText(order.getCrewCost().toString());
                    if (materialsField != null) materialsField.setText(order.getAdditionalMaterials().toString());

                    updateCostBreakdown(costSection.getParent() instanceof VBox ? (VBox) costSection.getParent() : null, itemsTable);
                });

                return null;
            }
        };

        new Thread(loadTask).start();
    }

    @Override
    protected void showSuccess(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText(message);

            // Style the alert
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setStyle("-fx-background-color: #1e293b;");
            dialogPane.lookup(".content.label").setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

            alert.showAndWait();
        });
    }

    @Override
    protected void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Operation Failed");
            alert.setContentText(message);

            // Style the alert
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setStyle("-fx-background-color: #1e293b;");
            dialogPane.lookup(".content.label").setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
            if (dialogPane.lookup(".header-panel") != null) {
                dialogPane.lookup(".header-panel").setStyle("-fx-background-color: #991b1b; -fx-text-fill: white;");
            }

            alert.showAndWait();
        });
    }

    @Override
    protected void showWarning(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning");
            alert.setHeaderText(null);
            alert.setContentText(message);

            // Style the alert
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setStyle("-fx-background-color: #1e293b;");
            dialogPane.lookup(".content.label").setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

            alert.showAndWait();
        });
    }


    // ==================== CLEAR PROJECT ORDER ====================
    private void handleClearProjectOrder(Project project, VBox orderTabContent) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Clear Order");
        confirm.setHeaderText("Clear all items from this order?");
        confirm.setContentText("This will return all items to storage!");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                @SuppressWarnings("unchecked")
                TableView<OrderItemRow> itemsTable = (TableView<OrderItemRow>)
                        orderTabContent.getChildren().stream()
                                .filter(node -> node instanceof TableView)
                                .findFirst()
                                .orElse(null);

                if (itemsTable != null) {
                    // ✅ CRITICAL FIX: Return all items to storage before clearing
                    Task<Void> returnAllTask = new Task<>() {
                        @Override
                        protected Void call() {
                            for (OrderItemRow row : itemsTable.getItems()) {
                                try {
                                    StorageItem item = storageService.getItemById(row.storageItemId).orElse(null);
                                    if (item != null) {
                                        int newQuantity = item.getQuantity() + row.quantity;
                                        item.setQuantity(newQuantity);
                                        storageService.updateItem(item.getId(), item);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            return null;
                        }
                    };

                    returnAllTask.setOnSucceeded(e -> {
                        itemsTable.getItems().clear();
                        updateCostBreakdown(orderTabContent, itemsTable);
                        showSuccess("✓ Order cleared and all items returned to storage!");
                    });

                    returnAllTask.setOnFailed(e -> {
                        showError("Failed to return items to storage!");
                    });

                    new Thread(returnAllTask).start();
                }
            }
        });
    }

    // ==================== UTILITY METHODS ====================
    private BigDecimal parseBigDecimal(String text) {
        if (text == null || text.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(text.trim());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private Label createCostLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.9); -fx-font-size: 15px; -fx-font-weight: bold;");
        return label;
    }

    private Label createCostValueLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: white; -fx-font-size: 17px; -fx-font-weight: bold;");
        return label;
    }

    private TextField createCostTextField(String prompt) {
        TextField field = new TextField(prompt);
        field.setPrefWidth(180);
        field.setStyle(
                "-fx-background-color: #1e293b;" +
                        "-fx-text-fill: white;" +
                        "-fx-prompt-text-fill: #9ca3af;" +
                        "-fx-border-color: rgba(139, 92, 246, 0.3);" +
                        "-fx-border-radius: 4;" +
                        "-fx-padding: 10;" +
                        "-fx-font-size: 14px;"
        );
        return field;
    }

    private void styleDialogHeader(Dialog<?> dialog) {
        if (dialog.getDialogPane().lookup(".header-panel") != null) {
            dialog.getDialogPane().lookup(".header-panel").setStyle(
                    "-fx-background-color: #1e293b; -fx-text-fill: white;"
            );
        }
        if (dialog.getDialogPane().lookup(".header-panel .label") != null) {
            dialog.getDialogPane().lookup(".header-panel .label").setStyle("-fx-text-fill: white;");
        }
    }

    private GridPane createDialogGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));
        grid.setStyle("-fx-background-color: #0f172a;");
        return grid;
    }

    private Label createFieldLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
        return label;
    }

    private void handleAddItemsToCustomer(Customer customer, VBox orderTabContent) {
        // Reuse the same storage items dialog - pass null for project since it's customer
        Stage dialogStage = new Stage();
        dialogStage.setTitle("📦 Select Storage Items");
        dialogStage.initModality(Modality.APPLICATION_MODAL);

        VBox mainLayout = new VBox(20);
        mainLayout.setPadding(new Insets(30));
        mainLayout.setStyle("-fx-background-color: linear-gradient(to bottom right, #1e293b, #0f172a);");

        Label titleLabel = new Label("Select Items from Storage");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

        TableView<StorageItem> storageTable = new TableView<>();
        storageTable.setPrefHeight(500);
        storageTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        storageTable.setStyle(
                "-fx-background-color: #0f172a;" +
                        "-fx-control-inner-background: #0f172a;"
        );

        TableColumn<StorageItem, String> nameCol = new TableColumn<>("Product Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        styleTableColumn(nameCol);

        TableColumn<StorageItem, String> mfgCol = new TableColumn<>("Manufacture");
        mfgCol.setPrefWidth(200);
        mfgCol.setCellValueFactory(new PropertyValueFactory<>("manufacture"));
        styleTableColumn(mfgCol);

        TableColumn<StorageItem, String> codeCol = new TableColumn<>("Code");
        codeCol.setPrefWidth(140);
        codeCol.setCellValueFactory(new PropertyValueFactory<>("code"));
        styleTableColumn(codeCol);

        TableColumn<StorageItem, String> availCol = new TableColumn<>("Status");
        availCol.setPrefWidth(150);
        availCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                } else {
                    StorageItem storageItem = getTableView().getItems().get(getIndex());
                    boolean isAvailable = storageItem.getQuantity() > 0;

                    setText(isAvailable ? "✅ AVAILABLE" : "❌ OUT OF STOCK");
                    setStyle(isAvailable ?
                            "-fx-text-fill: #22c55e; -fx-font-weight: bold; -fx-alignment: CENTER;" :
                            "-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-alignment: CENTER;"
                    );
                }
            }
        });

        TableColumn<StorageItem, Void> actionCol = new TableColumn<>("Action");
        actionCol.setPrefWidth(120);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button addBtn = new Button("+ Add");

            {
                addBtn.setStyle(
                        "-fx-background-color: #22c55e;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 8 20;" +
                                "-fx-background-radius: 6;" +
                                "-fx-cursor: hand;"
                );

                addBtn.setOnAction(e -> {
                    StorageItem selectedItem = getTableView().getItems().get(getIndex());
                    handleAddItemWithQuantityDialogForCustomer(selectedItem, customer, orderTabContent, dialogStage);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    StorageItem storageItem = getTableView().getItems().get(getIndex());
                    addBtn.setDisable(storageItem.getQuantity() == 0);
                    setGraphic(addBtn);
                }
            }
        });

        storageTable.getColumns().addAll(nameCol, mfgCol, codeCol, availCol, actionCol);

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

        Button closeButton = createStyledButton("✗ Close", "#6b7280", "#4b5563");
        closeButton.setPrefHeight(45);
        closeButton.setOnAction(e -> dialogStage.close());

        mainLayout.getChildren().addAll(titleLabel, storageTable, closeButton);

        Scene scene = new Scene(mainLayout, 1200, 700);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }

    // ==================== ADD ITEM DIALOG FOR CUSTOMER ====================
    private void handleAddItemWithQuantityDialogForCustomer(StorageItem storageItem, Customer customer, VBox orderTabContent, Stage parentDialog) {
        Dialog<OrderItemRow> dialog = new Dialog<>();
        dialog.setTitle("📦 Specify Quantity & Price");
        dialog.setHeaderText("Add: " + storageItem.getProductName());
        styleDialogHeader(dialog);

        ButtonType checkAvailabilityBtn = new ButtonType("✓ Check Availability", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(checkAvailabilityBtn, ButtonType.CANCEL);

        GridPane grid = createDialogGrid();

        Label qtyLbl = createFieldLabel("Quantity Needed:*");
        Spinner<Integer> qtySpinner = new Spinner<>(1, 999999, 1);
        qtySpinner.setEditable(true);
        qtySpinner.setPrefWidth(200);
        qtySpinner.setStyle("-fx-background-color: #1e293b;");
        qtySpinner.getEditor().setStyle("-fx-text-fill: white; -fx-background-color: #1e293b; -fx-font-size: 14px;");

        Label priceLbl = createFieldLabel("Unit Price:");
        BigDecimal itemPrice = storageItem.getPrice() != null ? storageItem.getPrice() : BigDecimal.ZERO;
        String priceValue = "$" + itemPrice.setScale(2, RoundingMode.HALF_UP).toString();

        Label priceDisplayLabel = new Label(priceValue);
        priceDisplayLabel.setStyle(
                "-fx-background-color: rgba(34, 197, 94, 0.2);" +
                        "-fx-text-fill: #22c55e;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 18px;" +
                        "-fx-border-color: rgba(34, 197, 94, 0.5);" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 4;" +
                        "-fx-padding: 10;" +
                        "-fx-min-width: 200;"
        );

        Label priceHint = new Label("(Automatically from storage database)");
        priceHint.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.6); -fx-font-size: 11px; -fx-font-style: italic;");

        grid.add(qtyLbl, 0, 0);
        grid.add(qtySpinner, 1, 0);
        grid.add(priceLbl, 0, 1);
        grid.add(priceDisplayLabel, 1, 1);
        grid.add(priceHint, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setStyle("-fx-background-color: #0f172a;");

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == checkAvailabilityBtn) {
                int quantity = qtySpinner.getValue();

                if (quantity <= 0) {
                    Platform.runLater(() -> showWarning("Quantity must be greater than zero!"));
                    return null;
                }

                BigDecimal price = storageItem.getPrice() != null ? storageItem.getPrice() : BigDecimal.ZERO;

                return new OrderItemRow(
                        storageItem.getId(),
                        storageItem.getProductName(),
                        quantity,
                        price
                );
            }
            return null;
        });

        dialog.showAndWait().ifPresent(orderRow -> {
            int availableQty = storageItem.getQuantity();

            if (orderRow.quantity > availableQty) {
                Alert notAvailableAlert = new Alert(Alert.AlertType.ERROR);
                notAvailableAlert.setTitle("❌ Insufficient Stock");
                notAvailableAlert.setHeaderText("Cannot allocate " + orderRow.quantity + " units");
                notAvailableAlert.setContentText(
                        "Requested: " + orderRow.quantity + " units\n" +
                                "Available: " + availableQty + " units\n" +
                                "Status: ❌ INSUFFICIENT STOCK"
                );
                notAvailableAlert.showAndWait();
                return;
            }

            // ✅ CRITICAL FIX: Reduce storage quantity IMMEDIATELY in database
            Task<Boolean> allocateTask = new Task<>() {
                @Override
                protected Boolean call() {
                    try {
                        StorageItem item = storageService.getItemById(storageItem.getId()).orElse(null);
                        if (item != null) {
                            int newQuantity = item.getQuantity() - orderRow.quantity;
                            item.setQuantity(newQuantity);
                            storageService.updateItem(item.getId(), item);
                            return true;
                        }
                        return false;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                }
            };

            allocateTask.setOnSucceeded(e -> {
                Boolean success = allocateTask.getValue();
                if (success) {
                    @SuppressWarnings("unchecked")
                    TableView<OrderItemRow> itemsTable = (TableView<OrderItemRow>)
                            orderTabContent.getChildren().stream()
                                    .filter(node -> node instanceof TableView)
                                    .findFirst()
                                    .orElse(null);

                    if (itemsTable != null) {
                        itemsTable.getItems().add(orderRow);
                        updateCostBreakdown(orderTabContent, itemsTable);
                        parentDialog.close();
                        showSuccess("✓ Item allocated! Storage updated: " + (availableQty - orderRow.quantity) + " remaining");
                    }
                } else {
                    showError("Failed to allocate stock from storage!");
                }
            });

            allocateTask.setOnFailed(e -> {
                showError("Failed to allocate stock: " + allocateTask.getException().getMessage());
            });

            new Thread(allocateTask).start();
        });
    }

    // ==================== SAVE CUSTOMER ORDER ====================
    private void handleSaveCustomerOrder(Customer customer, TableView<OrderItemRow> itemsTable, VBox costSection) {
        if (itemsTable.getItems().isEmpty()) {
            showWarning("Please add items to the order first");
            return;
        }

        GridPane grid = (GridPane) costSection.getUserData();

        Task<Void> saveTask = new Task<>() {
            @Override
            protected Void call() {
                List<SalesOrder> orders = salesOrderService.getOrdersByCustomer(customer.getId());
                SalesOrder order;

                if (orders.isEmpty()) {
                    order = new SalesOrder("CUSTOMER");
                    order.setCustomer(customer);
                    order.setCreatedBy(currentUser != null ? currentUser.getUsername() : "system");
                    order = salesOrderService.createSalesOrder(order);
                } else {
                    order = orders.get(0);
                }

                List<SalesOrderItem> existingItems = salesOrderService.getOrderItems(order.getId());
                for (SalesOrderItem item : existingItems) {
                    salesOrderService.removeItemFromOrder(item.getId());
                }

                for (OrderItemRow row : itemsTable.getItems()) {
                    SalesOrderItem item = new SalesOrderItem(row.storageItemId, row.quantity, row.unitPrice);
                    salesOrderService.addItemToOrder(order.getId(), item);
                }

                TextField taxField = (TextField) grid.lookup("#taxField");
                TextField discountField = (TextField) grid.lookup("#discountField");
                TextField crewField = (TextField) grid.lookup("#crewField");
                TextField materialsField = (TextField) grid.lookup("#materialsField");

                order.setTax(parseBigDecimal(taxField.getText()));
                order.setSaleDiscount(parseBigDecimal(discountField.getText()));
                order.setCrewCost(parseBigDecimal(crewField.getText()));
                order.setAdditionalMaterials(parseBigDecimal(materialsField.getText()));
                order.setUpdatedBy(currentUser != null ? currentUser.getUsername() : "system");

                salesOrderService.updateSalesOrder(order.getId(), order);

                return null;
            }
        };

        saveTask.setOnSucceeded(e -> showSuccess("✓ Customer order saved successfully!"));
        saveTask.setOnFailed(e -> showError("Failed to save: " + saveTask.getException().getMessage()));

        new Thread(saveTask).start();
    }

    // ==================== LOAD EXISTING CUSTOMER ORDER ====================
    private void loadExistingCustomerOrder(Customer customer, TableView<OrderItemRow> itemsTable, VBox costSection) {
        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() {
                List<SalesOrder> orders = salesOrderService.getOrdersByCustomer(customer.getId());
                if (orders.isEmpty()) return null;

                SalesOrder order = orders.get(0);
                List<SalesOrderItem> items = salesOrderService.getOrderItems(order.getId());

                Platform.runLater(() -> {
                    for (SalesOrderItem item : items) {
                        StorageItem storageItem = storageService.getItemById(item.getStorageItemId()).orElse(null);
                        if (storageItem != null) {
                            itemsTable.getItems().add(new OrderItemRow(
                                    storageItem.getId(),
                                    storageItem.getProductName(),
                                    item.getQuantity(),
                                    item.getUnitPrice()
                            ));
                        }
                    }

                    GridPane grid = (GridPane) costSection.getUserData();
                    TextField taxField = (TextField) grid.lookup("#taxField");
                    TextField discountField = (TextField) grid.lookup("#discountField");
                    TextField crewField = (TextField) grid.lookup("#crewField");
                    TextField materialsField = (TextField) grid.lookup("#materialsField");

                    if (taxField != null) taxField.setText(order.getTax().toString());
                    if (discountField != null) discountField.setText(order.getSaleDiscount().toString());
                    if (crewField != null) crewField.setText(order.getCrewCost().toString());
                    if (materialsField != null) materialsField.setText(order.getAdditionalMaterials().toString());

                    updateCostBreakdown(costSection.getParent() instanceof VBox ? (VBox) costSection.getParent() : null, itemsTable);
                });

                return null;
            }
        };

        new Thread(loadTask).start();
    }

    // ==================== CLEAR CUSTOMER ORDER ====================
    private void handleClearCustomerOrder(Customer customer, VBox orderTabContent) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Clear Order");
        confirm.setHeaderText("Clear all items from this order?");
        confirm.setContentText("This action cannot be undone!");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                @SuppressWarnings("unchecked")
                TableView<OrderItemRow> itemsTable = (TableView<OrderItemRow>)
                        orderTabContent.getChildren().stream()
                                .filter(node -> node instanceof TableView)
                                .findFirst()
                                .orElse(null);

                if (itemsTable != null) {
                    itemsTable.getItems().clear();
                    updateCostBreakdown(orderTabContent, itemsTable);
                    showSuccess("✓ Order cleared");
                }
            }
        });
    }

    private static class OrderItemRow {
        private final Long storageItemId;
        private final String productName;
        private final int quantity;
        private final BigDecimal unitPrice;

        public OrderItemRow(Long storageItemId, String productName, int quantity, BigDecimal unitPrice) {
            this.storageItemId = storageItemId;
            this.productName = productName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }
    }

    // ==================== NOTIFICATION LISTENER ====================

    /**
     * Register a notification listener for workflow updates.
     * Auto-refreshes the project details sidebar when workflow notifications arrive.
     */
    private void registerWorkflowNotificationListener() {
        if (notificationListenerService == null) {
            System.err.println("⚠️ NotificationListenerService not available, auto-refresh disabled");
            return;
        }

        // Create the listener
        notificationListener = notification -> {
            // Check if this is a workflow-related notification
            boolean isWorkflowNotification =
                "SITE_SURVEY_COMPLETED".equals(notification.getAction()) ||
                "SIZING_PRICING_COMPLETED".equals(notification.getAction()) ||
                "BANK_GUARANTEE_COMPLETED".equals(notification.getAction()) ||
                "STEP_COMPLETED".equals(notification.getAction()) ||
                "WORKFLOW_ADVANCED".equals(notification.getAction());

            if (!isWorkflowNotification) {
                return; // Not a workflow notification, ignore
            }

            // Check if we have a project details view open
            if (currentlyDisplayedProject == null) {
                System.out.println("📊 Workflow notification received but no project details open, skipping refresh");
                return;
            }

            // Check if the notification is for the currently displayed project
            Long notificationProjectId = notification.getEntityId();
            if (notificationProjectId != null &&
                notificationProjectId.equals(currentlyDisplayedProject.getId())) {

                System.out.println("\n┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓");
                System.out.println("┃  🔄 AUTO-REFRESHING PROJECT DETAILS SIDEBAR          ┃");
                System.out.println("┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫");
                System.out.println("┃  Notification: " + notification.getTitle());
                System.out.println("┃  Action: " + notification.getAction());
                System.out.println("┃  Project: " + currentlyDisplayedProject.getProjectName());
                System.out.println("┃  Project ID: " + currentlyDisplayedProject.getId());
                System.out.println("┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛\n");

                // Refresh on JavaFX thread
                javafx.application.Platform.runLater(() -> {
                    try {
                        openProjectDetails(currentlyDisplayedProject);
                        System.out.println("✅ Project details sidebar auto-refreshed successfully");
                    } catch (Exception ex) {
                        System.err.println("❌ Error auto-refreshing project details: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                });
            }
        };

        // Register the listener
        notificationListenerService.addListener(notificationListener);
        System.out.println("✅ Workflow notification listener registered for auto-refresh");
    }

    /**
     * Cleanup method - unregister notification listener
     */
    @Override
    public void cleanup() {
        if (notificationListenerService != null && notificationListener != null) {
            notificationListenerService.removeListener(notificationListener);
            System.out.println("🧹 Workflow notification listener unregistered");
        }
        currentlyDisplayedProject = null;
    }
}