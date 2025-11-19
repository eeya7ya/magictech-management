package com.magictech.modules.sales;

import com.magictech.core.auth.User;
import com.magictech.core.module.BaseModuleController;
import com.magictech.modules.sales.entity.*;
import com.magictech.modules.sales.service.*;
import com.magictech.modules.sales.model.*;
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
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Customer Management Controller - FULLY REDESIGNED
 * Matches ProjectsStorageController structure exactly
 * Features:
 * - Customer selection screen
 * - Customer workspace with tabs (Schedule, Tasks, Elements, Documents)
 * - PDF document management with real backend storage
 * - Availability-based element selection (NO QUANTITIES SHOWN)
 * - Comprehensive Excel export
 */
@Component
public class CustomerManagementController extends BaseModuleController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerScheduleService scheduleService;

    @Autowired
    private CustomerTaskService taskService;

    @Autowired
    private CustomerNoteService noteService;

    @Autowired
    private CustomerElementService elementService;

    @Autowired
    private CustomerDocumentService documentService;

    @Autowired
    private StorageService storageService;

    @Autowired
    private ComprehensiveExcelExportService excelExportService;

    // UI Components
    private com.magictech.core.ui.components.DashboardBackgroundPane backgroundPane;
    private StackPane mainContainer;
    private VBox customerSelectionScreen;
    private BorderPane customerWorkspaceScreen;
    private ProgressIndicator loadingIndicator;

    // Selected Customer
    private Customer selectedCustomer;
    private Label customerTitleLabel;

    // Tab Content Containers
    private TabPane tabPane;
    private VBox scheduleTabContent;
    private VBox tasksTabContent;
    private VBox elementsTabContent;
    private VBox documentsTabContent;

    // Schedule Data
    private ObservableList<CustomerScheduleViewModel> scheduleItems;
    private TableView<CustomerScheduleViewModel> scheduleTable;

    // Tasks Data
    private ObservableList<CustomerTaskViewModel> taskItems;
    private VBox tasksContainer;
    private TextArea importantNotesArea;

    // Elements Data
    private ObservableList<CustomerElementViewModel> elementItems;
    private FlowPane elementsGrid;

    // Documents Data
    private ObservableList<CustomerDocument> documentItems;
    private ListView<CustomerDocument> documentsListView;

    // Current User
    private User currentUser;

    @Override
    public void refresh() {
        if (selectedCustomer != null) {
            loadScheduleData();
            loadTasksData();
            loadElementsData();
            loadDocumentsData();
        }
    }

    @Override
    protected void setupUI() {
        StackPane stackRoot = new StackPane();
        backgroundPane = new com.magictech.core.ui.components.DashboardBackgroundPane();

        mainContainer = new StackPane();

        customerSelectionScreen = createCustomerSelectionScreen();
        customerWorkspaceScreen = createCustomerWorkspaceScreen();

        mainContainer.getChildren().add(customerSelectionScreen);
        customerWorkspaceScreen.setVisible(false);

        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setVisible(false);
        loadingIndicator.setMaxSize(60, 60);

        mainContainer.getChildren().addAll(customerWorkspaceScreen, loadingIndicator);

        stackRoot.getChildren().addAll(backgroundPane, mainContainer);

        BorderPane root = getRootPane();
        root.setCenter(stackRoot);
        root.setStyle("-fx-background-color: transparent;");
    }

    @Override
    protected void loadData() {
        // Data loads when customer is selected
    }

    // ==================== CUSTOMER SELECTION SCREEN ====================

    private VBox createCustomerSelectionScreen() {
        VBox screen = new VBox(30);
        screen.setAlignment(Pos.CENTER);
        screen.setPadding(new Insets(50));
        screen.setStyle("-fx-background-color: transparent;");

        // Header
        VBox header = new VBox(15);
        header.setAlignment(Pos.CENTER);

        Label iconLabel = new Label("👥");
        iconLabel.setFont(new Font(64));

        Label titleLabel = new Label("Customer Management");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 32px; -fx-font-weight: bold;");

        Label subtitleLabel = new Label("Select a customer to manage schedule, tasks, elements, and documents");
        subtitleLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 16px;");

        header.getChildren().addAll(iconLabel, titleLabel, subtitleLabel);

        // Customers Card
        VBox customersCard = new VBox(20);
        customersCard.setMaxWidth(700);
        customersCard.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.6);" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: rgba(139, 92, 246, 0.3);" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 16;" +
                        "-fx-padding: 30;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 20, 0, 0, 8);"
        );

        Label selectLabel = new Label("SELECT YOUR CUSTOMER:");
        selectLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        // Customer ListView
        ListView<Customer> customerListView = new ListView<>();
        customerListView.setPrefHeight(400);
        customerListView.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-background-radius: 8;" +
                        "-fx-selection-bar: rgba(139, 92, 246, 0.8);" +
                        "-fx-selection-bar-non-focused: rgba(139, 92, 246, 0.5);"
        );

        // Custom cell factory with hover and selection effects
        customerListView.setCellFactory(lv -> new ListCell<Customer>() {
            @Override
            protected void updateItem(Customer customer, boolean empty) {
                super.updateItem(customer, empty);
                if (empty || customer == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    VBox cellContent = new VBox(5);
                    cellContent.setPadding(new Insets(10));

                    Label nameLabel = new Label("👤 " + customer.getName());
                    nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

                    Label detailsLabel = new Label(
                            "📧 " + (customer.getEmail() != null ? customer.getEmail() : "No email") +
                                    " • 📞 " + (customer.getPhone() != null ? customer.getPhone() : "No phone")
                    );
                    detailsLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 13px;");

                    cellContent.getChildren().addAll(nameLabel, detailsLabel);
                    setGraphic(cellContent);

                    // Hover effect
                    setOnMouseEntered(e -> {
                        if (!isSelected()) {
                            setStyle("-fx-background-color: rgba(139, 92, 246, 0.2); -fx-background-radius: 8;");
                        }
                    });

                    setOnMouseExited(e -> {
                        if (!isSelected()) {
                            setStyle("-fx-background-color: transparent;");
                        }
                    });
                }
            }
        });

        // Load customers
        loadCustomersList(customerListView);

        // Selection listener
        customerListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && !customerListView.getSelectionModel().isEmpty()) {
                Customer selected = customerListView.getSelectionModel().getSelectedItem();
                openCustomerWorkspace(selected);
            }
        });

        // Buttons
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button openButton = new Button("Open Customer");
        openButton.setStyle(
                "-fx-background-color: linear-gradient(to right, #8b5cf6, #6366f1);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 12 30;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        );
        openButton.setOnAction(e -> {
            Customer selected = customerListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                openCustomerWorkspace(selected);
            } else {
                showWarning("Please select a customer first");
            }
        });

        Button newButton = new Button("+ New Customer");
        newButton.setStyle(
                "-fx-background-color: rgba(34, 197, 94, 0.8);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 12 30;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        );
        newButton.setOnAction(e -> handleNewCustomer(customerListView));

        Button exportButton = new Button("📊 Export All");
        exportButton.setStyle(
                "-fx-background-color: rgba(59, 130, 246, 0.8);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 12 30;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        );
        exportButton.setOnAction(e -> handleExportAllCustomers());

        buttonBox.getChildren().addAll(openButton, newButton, exportButton);

        customersCard.getChildren().addAll(selectLabel, customerListView, buttonBox);
        screen.getChildren().addAll(header, customersCard);

        return screen;
    }

    private void loadCustomersList(ListView<Customer> listView) {
        Task<List<Customer>> loadTask = new Task<>() {
            @Override
            protected List<Customer> call() {
                return customerService.getAllCustomers();
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<Customer> customers = loadTask.getValue();
            listView.getItems().setAll(customers);
        });

        loadTask.setOnFailed(e -> {
            showError("Failed to load customers: " + loadTask.getException().getMessage());
        });

        new Thread(loadTask).start();
    }

    private void handleNewCustomer(ListView<Customer> listView) {
        Dialog<Customer> dialog = new Dialog<>();
        dialog.setTitle("New Customer");
        dialog.setHeaderText("Create New Customer");

        // Apply dark theme
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white;");

        // Form fields
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
                Customer saved = customerService.createCustomer(customer);
                showSuccess("Customer created successfully!");
                loadCustomersList(listView);
            } catch (Exception ex) {
                showError("Failed to create customer: " + ex.getMessage());
            }
        });
    }

    // ==================== CUSTOMER WORKSPACE SCREEN ====================

    private BorderPane createCustomerWorkspaceScreen() {
        BorderPane workspace = new BorderPane();
        workspace.setStyle("-fx-background-color: transparent;");

        // Top: Customer Info & Back Button
        HBox topBar = new HBox(20);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(20));
        topBar.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.8);" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: rgba(139, 92, 246, 0.3);" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-width: 2;"
        );

        Button backButton = new Button("← Back to Customers");
        backButton.setStyle(
                "-fx-background-color: rgba(239, 68, 68, 0.8);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 10 20;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        );
        backButton.setOnAction(e -> returnToCustomerSelection());

        customerTitleLabel = new Label();
        customerTitleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button exportCustomerButton = new Button("📊 Export Customer Data");
        exportCustomerButton.setStyle(
                "-fx-background-color: rgba(59, 130, 246, 0.8);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 10 20;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        );
        exportCustomerButton.setOnAction(e -> handleExportCustomer());

        topBar.getChildren().addAll(backButton, customerTitleLabel, spacer, exportCustomerButton);

        // Center: Tabs
        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color: transparent;");

        Tab scheduleTab = new Tab("📅 Schedule");
        scheduleTabContent = new VBox();
        scheduleTab.setContent(scheduleTabContent);

        Tab tasksTab = new Tab("✅ Tasks");
        tasksTabContent = new VBox();
        tasksTab.setContent(tasksTabContent);

        Tab elementsTab = new Tab("📦 Elements");
        elementsTabContent = new VBox();
        elementsTab.setContent(elementsTabContent);

        Tab documentsTab = new Tab("📄 Documents");
        documentsTabContent = new VBox();
        documentsTab.setContent(documentsTabContent);

        tabPane.getTabs().addAll(scheduleTab, tasksTab, elementsTab, documentsTab);

        VBox centerContainer = new VBox(20, topBar, tabPane);
        centerContainer.setPadding(new Insets(20));
        workspace.setCenter(centerContainer);

        return workspace;
    }

    private void openCustomerWorkspace(Customer customer) {
        this.selectedCustomer = customer;
        this.currentUser = super.currentUser;

        // Update title
        customerTitleLabel.setText("👤 " + customer.getName());

        // Setup tabs
        setupScheduleTab();
        setupTasksTab();
        setupElementsTab();
        setupDocumentsTab();

        // Load data
        loadScheduleData();
        loadTasksData();
        loadElementsData();
        loadDocumentsData();

        // Switch screens
        customerSelectionScreen.setVisible(false);
        customerWorkspaceScreen.setVisible(true);
    }

    private void returnToCustomerSelection() {
        selectedCustomer = null;
        customerWorkspaceScreen.setVisible(false);
        customerSelectionScreen.setVisible(true);
    }

    // ==================== SCHEDULE TAB ====================

    private void setupScheduleTab() {
        scheduleTabContent.getChildren().clear();
        scheduleTabContent.setPadding(new Insets(20));
        scheduleTabContent.setSpacing(15);
        scheduleTabContent.setStyle("-fx-background-color: rgba(30, 41, 59, 0.6); -fx-background-radius: 12;");

        // Toolbar
        HBox toolbar = new HBox(15);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Button addButton = new Button("+ Add Schedule");
        addButton.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 8;");
        addButton.setOnAction(e -> handleAddSchedule());

        Button editButton = new Button("✏️ Edit");
        editButton.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 8;");
        editButton.setOnAction(e -> handleEditSchedule());

        Button deleteButton = new Button("🗑️ Delete");
        deleteButton.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 8;");
        deleteButton.setOnAction(e -> handleDeleteSchedule());

        toolbar.getChildren().addAll(addButton, editButton, deleteButton);

        // Table
        scheduleTable = new TableView<>();
        scheduleTable.setStyle("-fx-background-color: transparent;");
        scheduleItems = FXCollections.observableArrayList();
        scheduleTable.setItems(scheduleItems);

        TableColumn<CustomerScheduleViewModel, String> taskCol = new TableColumn<>("Task Name");
        taskCol.setCellValueFactory(new PropertyValueFactory<>("taskName"));
        taskCol.setPrefWidth(250);

        TableColumn<CustomerScheduleViewModel, LocalDate> startCol = new TableColumn<>("Start Date");
        startCol.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        startCol.setPrefWidth(150);

        TableColumn<CustomerScheduleViewModel, LocalDate> endCol = new TableColumn<>("End Date");
        endCol.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        endCol.setPrefWidth(150);

        scheduleTable.getColumns().addAll(taskCol, startCol, endCol);
        VBox.setVgrow(scheduleTable, Priority.ALWAYS);

        scheduleTabContent.getChildren().addAll(toolbar, scheduleTable);
    }

    private void loadScheduleData() {
        if (selectedCustomer == null) return;

        Task<List<CustomerSchedule>> loadTask = new Task<>() {
            @Override
            protected List<CustomerSchedule> call() {
                return scheduleService.getCustomerSchedules(selectedCustomer.getId());
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<CustomerSchedule> schedules = loadTask.getValue();
            scheduleItems.clear();
            schedules.forEach(s -> scheduleItems.add(new CustomerScheduleViewModel(s)));
        });

        new Thread(loadTask).start();
    }

    private void handleAddSchedule() {
        // Similar to ProjectSchedule dialog
        showSuccess("Add Schedule feature - to be implemented");
    }

    private void handleEditSchedule() {
        showSuccess("Edit Schedule feature - to be implemented");
    }

    private void handleDeleteSchedule() {
        CustomerScheduleViewModel selected = scheduleTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Please select a schedule item first");
            return;
        }

        if (showConfirmation("Delete Schedule", "Are you sure you want to delete this schedule?")) {
            try {
                scheduleService.deleteSchedule(selected.getId());
                showSuccess("Schedule deleted successfully");
                loadScheduleData();
            } catch (Exception ex) {
                showError("Failed to delete schedule: " + ex.getMessage());
            }
        }
    }

    // ==================== TASKS TAB ====================

    private void setupTasksTab() {
        tasksTabContent.getChildren().clear();
        tasksTabContent.setPadding(new Insets(20));
        tasksTabContent.setSpacing(15);
        tasksTabContent.setStyle("-fx-background-color: rgba(30, 41, 59, 0.6); -fx-background-radius: 12;");

        // Toolbar
        HBox toolbar = new HBox(15);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Button addButton = new Button("+ Add Task");
        addButton.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 8;");
        addButton.setOnAction(e -> handleAddTask());

        toolbar.getChildren().add(addButton);

        // Tasks container
        tasksContainer = new VBox(10);
        ScrollPane scrollPane = new ScrollPane(tasksContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Notes area
        Label notesLabel = new Label("📝 Important Notes:");
        notesLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        importantNotesArea = new TextArea();
        importantNotesArea.setPromptText("Add important notes here...");
        importantNotesArea.setPrefRowCount(4);
        importantNotesArea.setWrapText(true);

        Button saveNotesButton = new Button("💾 Save Notes");
        saveNotesButton.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 8;");
        saveNotesButton.setOnAction(e -> handleSaveNotes());

        VBox notesBox = new VBox(10, notesLabel, importantNotesArea, saveNotesButton);

        tasksTabContent.getChildren().addAll(toolbar, scrollPane, notesBox);
    }

    private void loadTasksData() {
        if (selectedCustomer == null) return;

        Task<List<CustomerTask>> loadTask = new Task<>() {
            @Override
            protected List<CustomerTask> call() {
                return taskService.getCustomerTasks(selectedCustomer.getId());
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<CustomerTask> tasks = loadTask.getValue();
            tasksContainer.getChildren().clear();

            for (CustomerTask task : tasks) {
                tasksContainer.getChildren().add(createTaskCard(task));
            }

            // Load notes
            List<CustomerNote> notes = noteService.getCustomerNotes(selectedCustomer.getId());
            if (!notes.isEmpty()) {
                importantNotesArea.setText(notes.get(0).getNoteContent());
            }
        });

        new Thread(loadTask).start();
    }

    private VBox createTaskCard(CustomerTask task) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle(
                "-fx-background-color: rgba(51, 65, 85, 0.8);" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-color: " + (task.getIsCompleted() ? "#22c55e" : "#ef4444") + ";" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 8;"
        );

        CheckBox completedBox = new CheckBox(task.getTaskTitle());
        completedBox.setSelected(task.getIsCompleted());
        completedBox.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        completedBox.setOnAction(e -> {
            task.setIsCompleted(completedBox.isSelected());
            taskService.updateTask(task);
            loadTasksData();
        });

        Label detailsLabel = new Label(task.getTaskDetails() != null ? task.getTaskDetails() : "");
        detailsLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.8); -fx-font-size: 14px;");
        detailsLabel.setWrapText(true);

        card.getChildren().addAll(completedBox, detailsLabel);
        return card;
    }

    private void handleAddTask() {
        showSuccess("Add Task feature - to be implemented");
    }

    private void handleSaveNotes() {
        if (selectedCustomer == null) return;

        try {
            List<CustomerNote> existingNotes = noteService.getCustomerNotes(selectedCustomer.getId());

            if (existingNotes.isEmpty()) {
                String noteContent = importantNotesArea.getText();
                String createdBy = currentUser != null ? currentUser.getUsername() : "system";
                noteService.createNote(selectedCustomer, noteContent, createdBy);
            } else {
                CustomerNote note = existingNotes.get(0);
                String newContent = importantNotesArea.getText();
                noteService.updateNote(note.getId(), newContent);
            }

            showSuccess("Notes saved successfully!");
        } catch (Exception ex) {
            showError("Failed to save notes: " + ex.getMessage());
        }
    }

    // ==================== ELEMENTS TAB (AVAILABILITY ONLY - NO QUANTITIES) ====================

    private void setupElementsTab() {
        elementsTabContent.getChildren().clear();
        elementsTabContent.setPadding(new Insets(20));
        elementsTabContent.setSpacing(15);
        elementsTabContent.setStyle("-fx-background-color: rgba(30, 41, 59, 0.6); -fx-background-radius: 12;");

        // Toolbar
        HBox toolbar = new HBox(15);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Button addButton = new Button("+ Add Elements");
        addButton.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 8;");
        addButton.setOnAction(e -> handleAddElements());

        Label infoLabel = new Label("ℹ️ Availability Status Only (Quantities Hidden for Sales)");
        infoLabel.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 14px;");

        toolbar.getChildren().addAll(addButton, infoLabel);

        // Elements grid
        elementsGrid = new FlowPane();
        elementsGrid.setHgap(15);
        elementsGrid.setVgap(15);
        elementsGrid.setPadding(new Insets(10));

        ScrollPane scrollPane = new ScrollPane(elementsGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        elementsTabContent.getChildren().addAll(toolbar, scrollPane);
    }

    private void loadElementsData() {
        if (selectedCustomer == null) return;

        Task<List<CustomerElement>> loadTask = new Task<>() {
            @Override
            protected List<CustomerElement> call() {
                return elementService.getCustomerElements(selectedCustomer.getId());
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<CustomerElement> elements = loadTask.getValue();
            elementsGrid.getChildren().clear();

            for (CustomerElement element : elements) {
                elementsGrid.getChildren().add(createElementCard(element));
            }
        });

        new Thread(loadTask).start();
    }

    private VBox createElementCard(CustomerElement element) {
        VBox card = new VBox(10);
        card.setPrefWidth(250);
        card.setPadding(new Insets(15));

        // Check availability
        boolean available = element.getStorageItem() != null &&
                element.getStorageItem().getQuantity() != null &&
                element.getStorageItem().getQuantity() > 0;

        card.setStyle(
                "-fx-background-color: rgba(51, 65, 85, 0.8);" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-color: " + (available ? "#22c55e" : "#ef4444") + ";" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 8;"
        );

        // Availability badge
        Label availabilityLabel = new Label(available ? "✅ AVAILABLE" : "❌ OUT OF STOCK");
        availabilityLabel.setStyle(
                "-fx-background-color: " + (available ? "#22c55e" : "#ef4444") + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 5 10;" +
                        "-fx-background-radius: 4;"
        );

        // Product name
        Label nameLabel = new Label(element.getStorageItem() != null ?
                element.getStorageItem().getProductName() : "Unknown Product");
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        nameLabel.setWrapText(true);

        // Needed quantity (this is okay to show - it's what customer needs)
        Label neededLabel = new Label("Needed: " + element.getQuantityNeeded());
        neededLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.8); -fx-font-size: 13px;");

        // Status
        Label statusLabel = new Label("Status: " + (element.getStatus() != null ? element.getStatus() : "PENDING"));
        statusLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 12px;");

        // Delete button
        Button deleteBtn = new Button("🗑️");
        deleteBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 4;");
        deleteBtn.setOnAction(e -> handleDeleteElement(element));

        card.getChildren().addAll(availabilityLabel, nameLabel, neededLabel, statusLabel, deleteBtn);
        return card;
    }

    private void handleAddElements() {
        if (selectedCustomer == null) return;

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Add Elements to Customer");

        VBox dialogContent = new VBox(20);
        dialogContent.setPadding(new Insets(20));
        dialogContent.setStyle("-fx-background-color: #1e293b;");

        Label titleLabel = new Label("Select Storage Items (Availability Check)");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        // Load all storage items
        TableView<StorageItem> itemsTable = new TableView<>();
        itemsTable.setPrefHeight(400);

        TableColumn<StorageItem, Boolean> selectCol = new TableColumn<>("Select");
        selectCol.setPrefWidth(60);
        Map<StorageItem, SimpleBooleanProperty> selectionMap = new HashMap<>();

        selectCol.setCellFactory(col -> new CheckBoxTableCell<>(index -> {
            StorageItem item = itemsTable.getItems().get(index);
            selectionMap.putIfAbsent(item, new SimpleBooleanProperty(false));
            return selectionMap.get(item);
        }));

        TableColumn<StorageItem, String> nameCol = new TableColumn<>("Product Name");
        nameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getProductName()));
        nameCol.setPrefWidth(250);

        TableColumn<StorageItem, String> availabilityCol = new TableColumn<>("Availability");
        availabilityCol.setCellValueFactory(data -> {
            boolean available = data.getValue().getQuantity() != null && data.getValue().getQuantity() > 0;
            return new javafx.beans.property.SimpleStringProperty(available ? "✅ AVAILABLE" : "❌ OUT OF STOCK");
        });
        availabilityCol.setPrefWidth(150);

        itemsTable.getColumns().addAll(selectCol, nameCol, availabilityCol);

        // Load items
        Task<List<StorageItem>> loadTask = new Task<>() {
            @Override
            protected List<StorageItem> call() {
                return storageService.findAllActive();
            }
        };

        loadTask.setOnSucceeded(e -> {
            itemsTable.getItems().setAll(loadTask.getValue());
        });

        new Thread(loadTask).start();

        // Buttons
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button addBtn = new Button("Add Selected");
        addBtn.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 8;");
        addBtn.setOnAction(e -> {
            List<StorageItem> selectedItems = selectionMap.entrySet().stream()
                    .filter(entry -> entry.getValue().get())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            if (selectedItems.isEmpty()) {
                showWarning("Please select at least one item");
                return;
            }

            // Add elements
            for (StorageItem item : selectedItems) {
                String createdBy = currentUser != null ? currentUser.getUsername() : "system";
                elementService.createElement(selectedCustomer, item, 1, createdBy);
            }

            showSuccess("Elements added successfully!");
            loadElementsData();
            dialog.close();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #6b7280; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 8;");
        cancelBtn.setOnAction(e -> dialog.close());

        buttonBox.getChildren().addAll(addBtn, cancelBtn);

        dialogContent.getChildren().addAll(titleLabel, itemsTable, buttonBox);

        Scene dialogScene = new Scene(dialogContent, 600, 500);
        dialog.setScene(dialogScene);
        dialog.showAndWait();
    }

    private void handleDeleteElement(CustomerElement element) {
        if (showConfirmation("Delete Element", "Remove this element from customer?")) {
            try {
                elementService.deleteElement(element.getId());
                showSuccess("Element removed successfully");
                loadElementsData();
            } catch (Exception ex) {
                showError("Failed to remove element: " + ex.getMessage());
            }
        }
    }

    // ==================== DOCUMENTS TAB (REAL PDF STORAGE) ====================

    private void setupDocumentsTab() {
        documentsTabContent.getChildren().clear();
        documentsTabContent.setPadding(new Insets(20));
        documentsTabContent.setSpacing(15);
        documentsTabContent.setStyle("-fx-background-color: rgba(30, 41, 59, 0.6); -fx-background-radius: 12;");

        // Toolbar
        HBox toolbar = new HBox(15);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Button uploadButton = new Button("📤 Upload Document");
        uploadButton.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 8;");
        uploadButton.setOnAction(e -> handleUploadDocument());

        Button viewButton = new Button("👁️ View");
        viewButton.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 8;");
        viewButton.setOnAction(e -> handleViewDocument());

        Button deleteButton = new Button("🗑️ Delete");
        deleteButton.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 8;");
        deleteButton.setOnAction(e -> handleDeleteDocument());

        toolbar.getChildren().addAll(uploadButton, viewButton, deleteButton);

        // Documents ListView
        documentsListView = new ListView<>();
        documentsListView.setPrefHeight(400);
        documentsListView.setStyle("-fx-background-color: rgba(51, 65, 85, 0.6);");
        documentItems = FXCollections.observableArrayList();
        documentsListView.setItems(documentItems);

        documentsListView.setCellFactory(lv -> new ListCell<CustomerDocument>() {
            @Override
            protected void updateItem(CustomerDocument doc, boolean empty) {
                super.updateItem(doc, empty);
                if (empty || doc == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox cellContent = new VBox(5);
                    cellContent.setPadding(new Insets(10));

                    Label nameLabel = new Label("📄 " + doc.getDocumentName());
                    nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

                    Label detailsLabel = new Label(
                            "Category: " + doc.getCategory() +
                                    " • Size: " + doc.getFileSizeFormatted() +
                                    " • Uploaded: " + doc.getDateUploaded().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    );
                    detailsLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 12px;");

                    cellContent.getChildren().addAll(nameLabel, detailsLabel);
                    setGraphic(cellContent);
                }
            }
        });

        VBox.setVgrow(documentsListView, Priority.ALWAYS);

        documentsTabContent.getChildren().addAll(toolbar, documentsListView);
    }

    private void loadDocumentsData() {
        if (selectedCustomer == null) return;

        Task<List<CustomerDocument>> loadTask = new Task<>() {
            @Override
            protected List<CustomerDocument> call() {
                return documentService.getCustomerDocuments(selectedCustomer.getId());
            }
        };

        loadTask.setOnSucceeded(e -> {
            documentItems.setAll(loadTask.getValue());
        });

        new Thread(loadTask).start();
    }

    private void handleUploadDocument() {
        if (selectedCustomer == null) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Document to Upload");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File file = fileChooser.showOpenDialog(getRootPane().getScene().getWindow());
        if (file == null) return;

        // Ask for category
        ChoiceDialog<String> categoryDialog = new ChoiceDialog<>("CONTRACT",
                "CONTRACT", "QUOTATION", "INVOICE", "ID_PROOF", "OTHER");
        categoryDialog.setTitle("Document Category");
        categoryDialog.setHeaderText("Select document category");
        categoryDialog.setContentText("Category:");

        String category = categoryDialog.showAndWait().orElse("OTHER");

        // Upload
        Task<CustomerDocument> uploadTask = new Task<>() {
            @Override
            protected CustomerDocument call() throws Exception {
                return documentService.saveDocument(
                        selectedCustomer,
                        file,
                        category,
                        "Uploaded via UI",
                        currentUser != null ? currentUser.getUsername() : "system"
                );
            }
        };

        uploadTask.setOnSucceeded(e -> {
            showSuccess("Document uploaded successfully!");
            loadDocumentsData();
        });

        uploadTask.setOnFailed(e -> {
            showError("Failed to upload document: " + uploadTask.getException().getMessage());
        });

        new Thread(uploadTask).start();
    }

    private void handleViewDocument() {
        CustomerDocument selected = documentsListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Please select a document first");
            return;
        }

        try {
            File file = documentService.downloadDocument(selected.getId());

            if (selected.getDocumentType().equalsIgnoreCase("PDF")) {
                showPDFPreview(file, selected.getDocumentName());
            } else {
                showSuccess("Document downloaded to: " + file.getAbsolutePath());
            }
        } catch (Exception ex) {
            showError("Failed to view document: " + ex.getMessage());
        }
    }

    private void showPDFPreview(File pdfFile, String documentName) {
        Stage pdfStage = new Stage();
        pdfStage.setTitle("PDF Preview: " + documentName);

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #1e293b;");

        Label titleLabel = new Label("📄 " + documentName);
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(600);

        VBox pagesBox = new VBox(10);
        pagesBox.setAlignment(Pos.CENTER);
        scrollPane.setContent(pagesBox);

        // Load PDF pages
        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try (PDDocument document = PDDocument.load(pdfFile)) {
                    PDFRenderer renderer = new PDFRenderer(document);
                    for (int page = 0; page < document.getNumberOfPages(); page++) {
                        BufferedImage image = renderer.renderImageWithDPI(page, 150);
                        File tempImageFile = File.createTempFile("pdf_page_", ".png");
                        ImageIO.write(image, "PNG", tempImageFile);

                        int pageNum = page;
                        Platform.runLater(() -> {
                            javafx.scene.image.Image fxImage = new javafx.scene.image.Image(tempImageFile.toURI().toString());
                            javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(fxImage);
                            imageView.setPreserveRatio(true);
                            imageView.setFitWidth(700);

                            VBox pageBox = new VBox(5);
                            pageBox.setAlignment(Pos.CENTER);
                            Label pageLabel = new Label("Page " + (pageNum + 1));
                            pageLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
                            pageBox.getChildren().addAll(pageLabel, imageView);

                            pagesBox.getChildren().add(pageBox);
                        });
                    }
                }
                return null;
            }
        };

        loadTask.setOnFailed(e -> {
            showError("Failed to load PDF: " + loadTask.getException().getMessage());
            pdfStage.close();
        });

        new Thread(loadTask).start();

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: #6b7280; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 8;");
        closeBtn.setOnAction(e -> pdfStage.close());

        content.getChildren().addAll(titleLabel, scrollPane, closeBtn);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        Scene scene = new Scene(content, 800, 700);
        pdfStage.setScene(scene);
        pdfStage.show();
    }

    private void handleDeleteDocument() {
        CustomerDocument selected = documentsListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Please select a document first");
            return;
        }

        if (showConfirmation("Delete Document", "Are you sure you want to delete this document?")) {
            try {
                documentService.deleteDocument(selected.getId());
                showSuccess("Document deleted successfully");
                loadDocumentsData();
            } catch (Exception ex) {
                showError("Failed to delete document: " + ex.getMessage());
            }
        }
    }

    // ==================== EXPORT FUNCTIONALITY ====================

    private void handleExportAllCustomers() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export All Customers");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        fileChooser.setInitialFileName("All_Customers_Export_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx");

        File file = fileChooser.showSaveDialog(getRootPane().getScene().getWindow());
        if (file == null) return;

        Task<Void> exportTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                List<Customer> allCustomers = customerService.getAllCustomers();
                excelExportService.exportAllCustomers(allCustomers, file.getAbsolutePath());
                return null;
            }
        };

        exportTask.setOnSucceeded(e -> {
            showSuccess("All customers exported successfully!");
        });

        exportTask.setOnFailed(e -> {
            showError("Export failed: " + exportTask.getException().getMessage());
        });

        new Thread(exportTask).start();
    }

    private void handleExportCustomer() {
        if (selectedCustomer == null) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Customer Data");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        fileChooser.setInitialFileName("Customer_" + selectedCustomer.getName().replaceAll("[^a-zA-Z0-9]", "_") +
                "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx");

        File file = fileChooser.showSaveDialog(getRootPane().getScene().getWindow());
        if (file == null) return;

        Task<Void> exportTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                excelExportService.exportSingleCustomer(selectedCustomer.getId(), file.getAbsolutePath());
                return null;
            }
        };

        exportTask.setOnSucceeded(e -> {
            showSuccess("Customer data exported successfully!\n\nIncludes: Elements, Pricing, Schedule, Tasks, Documents, and Notes.");
        });

        exportTask.setOnFailed(e -> {
            showError("Export failed: " + exportTask.getException().getMessage());
        });

        new Thread(exportTask).start();
    }

    @Override
    public void cleanup() {
        if (backgroundPane != null) {
            backgroundPane.stopAnimation();
        }

        selectedCustomer = null;
        scheduleItems = null;
        taskItems = null;
        elementItems = null;
        documentItems = null;
    }
}
