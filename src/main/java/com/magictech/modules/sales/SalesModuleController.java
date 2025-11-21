package com.magictech.modules.sales;

import com.magictech.core.auth.User;
import com.magictech.core.module.BaseModuleController;
import com.magictech.modules.sales.entity.*;
import com.magictech.modules.sales.service.*;
import com.magictech.modules.projects.entity.Project;
import com.magictech.modules.projects.service.ProjectService;
import com.magictech.modules.storage.entity.StorageItem;
import com.magictech.modules.storage.service.StorageService;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Unified Sales Module Controller
 * Single entry point with internal tabs for:
 * - Sales Orders (Projects)
 * - Customers
 */
@Component
public class SalesModuleController extends BaseModuleController {

    @Autowired private CustomerService customerService;
    @Autowired private SalesOrderService salesOrderService;
    @Autowired private ProjectService projectService;
    @Autowired private StorageService storageService;
    @Autowired private com.magictech.modules.sales.service.SalesExcelExportService salesExcelExportService;
    @Autowired private com.magictech.modules.sales.service.ProjectCostBreakdownService costBreakdownService;
    @Autowired private com.magictech.modules.sales.service.CustomerCostBreakdownService customerCostBreakdownService;

    private com.magictech.core.ui.components.DashboardBackgroundPane backgroundPane;
    private TabPane mainTabPane;
    private User currentUser;

    // Sales Orders Tab
    private ListView<Project> projectsListView;
    private VBox salesOrdersContent;

    // Customers Tab
    private ListView<Customer> customersListView;
    private VBox customersContent;

    @Override
    public void refresh() {
        loadSalesOrdersData();
        loadCustomersData();
    }

    @Override
    protected void setupUI() {
        StackPane stackRoot = new StackPane();
        backgroundPane = new com.magictech.core.ui.components.DashboardBackgroundPane();

        VBox mainLayout = new VBox(20);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setStyle("-fx-background-color: transparent;");

        // Header
        HBox header = createHeader();

        // Main TabPane with two tabs
        mainTabPane = new TabPane();
        mainTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        mainTabPane.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(mainTabPane, Priority.ALWAYS);

        // Tab 1: Sales Orders (Projects)
        Tab salesOrdersTab = new Tab("🏗️ Sales Orders");
        salesOrdersContent = createSalesOrdersTab();
        salesOrdersTab.setContent(salesOrdersContent);
        styleTab(salesOrdersTab);

        // Tab 2: Customers
        Tab customersTab = new Tab("👥 Customers");
        customersContent = createCustomersTab();
        customersTab.setContent(customersContent);
        styleTab(customersTab);

        mainTabPane.getTabs().addAll(salesOrdersTab, customersTab);

        mainLayout.getChildren().addAll(header, mainTabPane);
        stackRoot.getChildren().addAll(backgroundPane, mainLayout);

        BorderPane root = getRootPane();
        root.setCenter(stackRoot);
        root.setStyle("-fx-background-color: transparent;");
    }

    @Override
    protected void loadData() {
        loadSalesOrdersData();
        loadCustomersData();
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

    // ==================== SALES ORDERS TAB ====================

    private VBox createSalesOrdersTab() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: transparent;");

        // Header
        HBox toolbar = new HBox(15);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("🏗️ Project Sales Orders");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Button exportBtn = createStyledButton("📥 Export", "#8b5cf6", "#7c3aed");
        exportBtn.setOnAction(e -> handleExportProjects());

        Button addBtn = createStyledButton("+ New Project", "#3b82f6", "#2563eb");
        addBtn.setOnAction(e -> handleAddProject());

        toolbar.getChildren().addAll(titleLabel, exportBtn, addBtn);

        // Projects ListView
        projectsListView = new ListView<>();
        projectsListView.setPrefHeight(500);
        projectsListView.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.6);" +
                "-fx-control-inner-background: rgba(15, 23, 42, 0.8);"
        );
        VBox.setVgrow(projectsListView, Priority.ALWAYS);

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

        content.getChildren().addAll(toolbar, projectsListView);
        return content;
    }

    private void loadSalesOrdersData() {
        Task<List<Project>> loadTask = new Task<>() {
            @Override
            protected List<Project> call() {
                return projectService.getAllProjects();
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<Project> projects = loadTask.getValue();
            projectsListView.getItems().setAll(projects);
        });

        loadTask.setOnFailed(e -> {
            showError("Failed to load projects: " + loadTask.getException().getMessage());
        });

        new Thread(loadTask).start();
    }

    private void openProjectDetails(Project project) {
        // Open project details in dialog
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Project: " + project.getProjectName());

        VBox content = createProjectDetailsContent(project);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #1e293b;");

        Scene scene = new Scene(scrollPane, 1000, 700);
        dialog.setScene(scene);
        dialog.show();
    }

    private VBox createProjectDetailsContent(Project project) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: #1e293b;");

        // Header
        Label titleLabel = new Label("📋 " + project.getProjectName());
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

        // Order Items Section with CARD LAYOUT
        Label orderItemsLabel = new Label("📦 Order Items");
        orderItemsLabel.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");

        HBox orderItemsToolbar = new HBox(15);
        orderItemsToolbar.setAlignment(Pos.CENTER_LEFT);

        Button addItemsBtn = createStyledButton("+ Add Items", "#22c55e", "#16a34a");
        addItemsBtn.setOnAction(e -> handleAddItemsToProject(project, content));

        Button clearBtn = createStyledButton("🗑 Clear All", "#ef4444", "#dc2626");
        clearBtn.setOnAction(e -> handleClearProjectOrder(project, content));

        orderItemsToolbar.getChildren().addAll(addItemsBtn, clearBtn);

        // CARD-BASED ORDER ITEMS DISPLAY (instead of table)
        FlowPane orderItemsGrid = new FlowPane();
        orderItemsGrid.setHgap(15);
        orderItemsGrid.setVgap(15);
        orderItemsGrid.setPadding(new Insets(10));
        orderItemsGrid.setId("orderItemsGrid");

        // Load order items as cards
        loadOrderItemsAsCards(project, orderItemsGrid);

        content.getChildren().addAll(titleLabel, orderItemsLabel, orderItemsToolbar, orderItemsGrid);
        return content;
    }

    private void loadOrderItemsAsCards(Project project, FlowPane orderItemsGrid) {
        Task<List<SalesOrderItem>> loadTask = new Task<>() {
            @Override
            protected List<SalesOrderItem> call() {
                List<SalesOrder> orders = salesOrderService.getOrdersByProject(project.getId());
                if (orders.isEmpty()) return new ArrayList<>();
                return salesOrderService.getOrderItems(orders.get(0).getId());
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<SalesOrderItem> items = loadTask.getValue();
            orderItemsGrid.getChildren().clear();

            for (SalesOrderItem item : items) {
                orderItemsGrid.getChildren().add(createOrderItemCard(item, orderItemsGrid));
            }

            if (items.isEmpty()) {
                Label emptyLabel = new Label("No items in this order yet");
                emptyLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.5); -fx-font-size: 16px;");
                orderItemsGrid.getChildren().add(emptyLabel);
            }
        });

        new Thread(loadTask).start();
    }

    private VBox createOrderItemCard(SalesOrderItem orderItem, FlowPane parentGrid) {
        VBox card = new VBox(10);
        card.setPrefWidth(280);
        card.setPadding(new Insets(15));
        card.setStyle(
                "-fx-background-color: rgba(51, 65, 85, 0.8);" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: rgba(34, 197, 94, 0.4);" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 12;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 10, 0, 0, 5);"
        );

        // Get storage item details
        StorageItem storageItem = storageService.getItemById(orderItem.getStorageItemId()).orElse(null);

        // Product name
        Label nameLabel = new Label(storageItem != null ? storageItem.getProductName() : "Unknown Product");
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        nameLabel.setWrapText(true);

        // Quantity
        Label qtyLabel = new Label("Quantity: " + orderItem.getQuantity());
        qtyLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.9); -fx-font-size: 14px;");

        // Unit price
        Label priceLabel = new Label("Unit Price: $" + orderItem.getUnitPrice().setScale(2, RoundingMode.HALF_UP));
        priceLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.9); -fx-font-size: 14px;");

        // Total price
        BigDecimal total = orderItem.getUnitPrice().multiply(new BigDecimal(orderItem.getQuantity()));
        Label totalLabel = new Label("Total: $" + total.setScale(2, RoundingMode.HALF_UP));
        totalLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 16px; -fx-font-weight: bold;");

        // Delete button
        Button deleteBtn = new Button("🗑️ Remove");
        deleteBtn.setStyle(
                "-fx-background-color: #ef4444;" +
                "-fx-text-fill: white;" +
                "-fx-padding: 8 15;" +
                "-fx-background-radius: 6;" +
                "-fx-cursor: hand;"
        );
        deleteBtn.setOnAction(e -> handleDeleteOrderItem(orderItem, parentGrid));

        card.getChildren().addAll(nameLabel, qtyLabel, priceLabel, totalLabel, deleteBtn);
        return card;
    }

    private void handleDeleteOrderItem(SalesOrderItem orderItem, FlowPane parentGrid) {
        if (showConfirmation("Remove Item", "Are you sure you want to remove this item from the order?")) {
            Task<Void> deleteTask = new Task<>() {
                @Override
                protected Void call() {
                    // Return to storage
                    StorageItem item = storageService.getItemById(orderItem.getStorageItemId()).orElse(null);
                    if (item != null) {
                        item.setQuantity(item.getQuantity() + orderItem.getQuantity());
                        storageService.updateItem(item.getId(), item);
                    }
                    // Remove from order
                    salesOrderService.removeItemFromOrder(orderItem.getId());
                    return null;
                }
            };

            deleteTask.setOnSucceeded(e -> {
                parentGrid.getChildren().remove(findCardForOrderItem(parentGrid, orderItem));
                showSuccess("Item removed and returned to storage!");
            });

            deleteTask.setOnFailed(e -> {
                showError("Failed to remove item: " + deleteTask.getException().getMessage());
            });

            new Thread(deleteTask).start();
        }
    }

    private VBox findCardForOrderItem(FlowPane grid, SalesOrderItem orderItem) {
        for (javafx.scene.Node node : grid.getChildren()) {
            if (node instanceof VBox) {
                return (VBox) node;
            }
        }
        return null;
    }

    private void handleAddItemsToProject(Project project, VBox content) {
        // Implementation similar to existing SalesStorageController
        showSuccess("Add Items feature - to be connected");
    }

    private void handleClearProjectOrder(Project project, VBox content) {
        showSuccess("Clear Order feature - to be connected");
    }

    private void handleExportProjects() {
        showSuccess("Export Projects feature - to be connected");
    }

    private void handleAddProject() {
        showSuccess("Add Project feature - open Projects module");
    }

    // ==================== CUSTOMERS TAB ====================

    private VBox createCustomersTab() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: transparent;");

        // Header
        HBox toolbar = new HBox(15);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("👥 Customers");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Button exportBtn = createStyledButton("📥 Export", "#8b5cf6", "#7c3aed");
        exportBtn.setOnAction(e -> handleExportCustomers());

        Button addBtn = createStyledButton("+ New Customer", "#22c55e", "#16a34a");
        addBtn.setOnAction(e -> handleAddCustomer());

        toolbar.getChildren().addAll(titleLabel, exportBtn, addBtn);

        // Customers ListView
        customersListView = new ListView<>();
        customersListView.setPrefHeight(500);
        customersListView.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.6);" +
                "-fx-control-inner-background: rgba(15, 23, 42, 0.8);"
        );
        VBox.setVgrow(customersListView, Priority.ALWAYS);

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
                            "-fx-border-color: rgba(139, 92, 246, 0.3);" +
                            "-fx-border-width: 1;" +
                            "-fx-border-radius: 8;" +
                            "-fx-cursor: hand;"
                    );

                    Label nameLabel = new Label("👤 " + customer.getName());
                    nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

                    String details = "📧 " + (customer.getEmail() != null ? customer.getEmail() : "No email") +
                                    " • 📞 " + (customer.getPhone() != null ? customer.getPhone() : "No phone");
                    Label detailsLabel = new Label(details);
                    detailsLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.8); -fx-font-size: 12px;");

                    HBox actionsBox = new HBox(10);
                    actionsBox.setAlignment(Pos.CENTER_LEFT);

                    Button openBtn = new Button("Open");
                    openBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 5 10;");
                    openBtn.setOnAction(e -> {
                        e.consume();
                        openCustomerDetails(customer);
                    });

                    Button editBtn = new Button("✏️ Edit");
                    editBtn.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 5 10;");
                    editBtn.setOnAction(e -> {
                        e.consume();
                        handleEditCustomer(customer);
                    });

                    Button deleteBtn = new Button("🗑️");
                    deleteBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 5 10;");
                    deleteBtn.setOnAction(e -> {
                        e.consume();
                        handleDeleteCustomer(customer);
                    });

                    actionsBox.getChildren().addAll(openBtn, editBtn, deleteBtn);

                    cellBox.getChildren().addAll(nameLabel, detailsLabel, actionsBox);
                    cellBox.setOnMouseClicked(e -> {
                        if (e.getClickCount() == 2) {
                            openCustomerDetails(customer);
                        }
                    });

                    setGraphic(cellBox);
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                }
            }
        });

        content.getChildren().addAll(toolbar, customersListView);
        return content;
    }

    private void loadCustomersData() {
        Task<List<Customer>> loadTask = new Task<>() {
            @Override
            protected List<Customer> call() {
                return customerService.getAllCustomers();
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<Customer> customers = loadTask.getValue();
            customersListView.getItems().setAll(customers);
        });

        loadTask.setOnFailed(e -> {
            showError("Failed to load customers: " + loadTask.getException().getMessage());
        });

        new Thread(loadTask).start();
    }

    private void openCustomerDetails(Customer customer) {
        // Open customer details in dialog
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Customer: " + customer.getName());

        VBox content = createCustomerDetailsContent(customer);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #1e293b;");

        Scene scene = new Scene(scrollPane, 1000, 700);
        dialog.setScene(scene);
        dialog.show();
    }

    private VBox createCustomerDetailsContent(Customer customer) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: #1e293b;");

        // Header
        Label titleLabel = new Label("👤 " + customer.getName());
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

        // Customer info
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(20);
        infoGrid.setVgap(10);

        addInfoRow(infoGrid, 0, "Email:", customer.getEmail() != null ? customer.getEmail() : "N/A");
        addInfoRow(infoGrid, 1, "Phone:", customer.getPhone() != null ? customer.getPhone() : "N/A");
        addInfoRow(infoGrid, 2, "Address:", customer.getAddress() != null ? customer.getAddress() : "N/A");
        addInfoRow(infoGrid, 3, "Company:", customer.getCompany() != null ? customer.getCompany() : "N/A");

        // Order Items Section with CARD LAYOUT
        Label orderItemsLabel = new Label("📦 Order Items");
        orderItemsLabel.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");

        HBox orderItemsToolbar = new HBox(15);
        orderItemsToolbar.setAlignment(Pos.CENTER_LEFT);

        Button addItemsBtn = createStyledButton("+ Add Items", "#22c55e", "#16a34a");
        addItemsBtn.setOnAction(e -> handleAddItemsToCustomer(customer, content));

        Button clearBtn = createStyledButton("🗑 Clear All", "#ef4444", "#dc2626");
        clearBtn.setOnAction(e -> handleClearCustomerOrder(customer, content));

        orderItemsToolbar.getChildren().addAll(addItemsBtn, clearBtn);

        // CARD-BASED ORDER ITEMS DISPLAY (instead of table)
        FlowPane orderItemsGrid = new FlowPane();
        orderItemsGrid.setHgap(15);
        orderItemsGrid.setVgap(15);
        orderItemsGrid.setPadding(new Insets(10));
        orderItemsGrid.setId("orderItemsGrid");

        // Load order items as cards
        loadCustomerOrderItemsAsCards(customer, orderItemsGrid);

        content.getChildren().addAll(titleLabel, infoGrid, orderItemsLabel, orderItemsToolbar, orderItemsGrid);
        return content;
    }

    private void addInfoRow(GridPane grid, int row, String label, String value) {
        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 14px; -fx-font-weight: bold;");

        Label valueNode = new Label(value);
        valueNode.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        grid.add(labelNode, 0, row);
        grid.add(valueNode, 1, row);
    }

    private void loadCustomerOrderItemsAsCards(Customer customer, FlowPane orderItemsGrid) {
        Task<List<SalesOrderItem>> loadTask = new Task<>() {
            @Override
            protected List<SalesOrderItem> call() {
                List<SalesOrder> orders = salesOrderService.getOrdersByCustomer(customer.getId());
                if (orders.isEmpty()) return new ArrayList<>();
                return salesOrderService.getOrderItems(orders.get(0).getId());
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<SalesOrderItem> items = loadTask.getValue();
            orderItemsGrid.getChildren().clear();

            for (SalesOrderItem item : items) {
                orderItemsGrid.getChildren().add(createOrderItemCard(item, orderItemsGrid));
            }

            if (items.isEmpty()) {
                Label emptyLabel = new Label("No items in this order yet");
                emptyLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.5); -fx-font-size: 16px;");
                orderItemsGrid.getChildren().add(emptyLabel);
            }
        });

        new Thread(loadTask).start();
    }

    private void handleAddItemsToCustomer(Customer customer, VBox content) {
        showSuccess("Add Items feature - to be connected");
    }

    private void handleClearCustomerOrder(Customer customer, VBox content) {
        showSuccess("Clear Order feature - to be connected");
    }

    private void handleAddCustomer() {
        Dialog<Customer> dialog = new Dialog<>();
        dialog.setTitle("New Customer");
        dialog.setHeaderText("Create New Customer");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField();
        nameField.setPromptText("Customer Name");
        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        TextField phoneField = new TextField();
        phoneField.setPromptText("Phone");
        TextField addressField = new TextField();
        addressField.setPromptText("Address");
        TextField companyField = new TextField();
        companyField.setPromptText("Company");

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

        dialogPane.setContent(grid);

        ButtonType createButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                if (nameField.getText().trim().isEmpty()) {
                    showError("Customer name is required");
                    return null;
                }

                Customer customer = new Customer();
                customer.setName(nameField.getText().trim());
                customer.setEmail(emailField.getText().trim());
                customer.setPhone(phoneField.getText().trim());
                customer.setAddress(addressField.getText().trim());
                customer.setCompany(companyField.getText().trim());
                customer.setCreatedBy(currentUser != null ? currentUser.getUsername() : "system");

                return customer;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(customer -> {
            try {
                customerService.createCustomer(customer);
                showSuccess("Customer created successfully!");
                loadCustomersData();
            } catch (Exception ex) {
                showError("Failed to create customer: " + ex.getMessage());
            }
        });
    }

    private void handleEditCustomer(Customer customer) {
        // Similar to handleAddCustomer but with pre-filled values
        showSuccess("Edit Customer feature - to be implemented");
        // Reload after edit
        loadCustomersData();
    }

    private void handleDeleteCustomer(Customer customer) {
        if (showConfirmation("Delete Customer",
                "Are you sure you want to delete customer: " + customer.getName() + "?\n\n" +
                "This will soft-delete the customer (can be restored from database).")) {
            try {
                customerService.deleteCustomer(customer.getId());
                showSuccess("Customer deleted successfully!");
                loadCustomersData();
            } catch (Exception ex) {
                showError("Failed to delete customer: " + ex.getMessage());
            }
        }
    }

    private void handleExportCustomers() {
        showSuccess("Export Customers feature - to be connected");
    }

    // ==================== HELPER METHODS ====================

    private Button createStyledButton(String text, String color, String hoverColor) {
        Button button = new Button(text);
        button.setStyle(
                "-fx-background-color: " + color + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 10 20;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;"
        );
        button.setOnMouseEntered(e -> button.setStyle(
                "-fx-background-color: " + hoverColor + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 10 20;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;"
        ));
        button.setOnMouseExited(e -> button.setStyle(
                "-fx-background-color: " + color + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 10 20;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;"
        ));
        return button;
    }

    private void styleTab(Tab tab) {
        tab.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.8);" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 16px;" +
                "-fx-font-weight: bold;"
        );
    }

    @Override
    public void cleanup() {
        if (backgroundPane != null) {
            backgroundPane.stopAnimation();
        }
    }

    private void navigateToDashboard() {
        if (backgroundPane != null) {
            backgroundPane.stopAnimation();
        }
        com.magictech.core.ui.SceneManager.getInstance().showMainDashboard();
    }

    public void immediateCleanup() {
        if (backgroundPane != null) {
            backgroundPane.stopAnimation();
            backgroundPane = null;
        }
        projectsListView = null;
        customersListView = null;
        salesOrdersContent = null;
        customersContent = null;
        mainTabPane = null;
        System.out.println("✓ Sales module controller cleaned up");
    }
}
