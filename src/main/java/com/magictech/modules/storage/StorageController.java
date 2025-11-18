package com.magictech.modules.storage;

import com.magictech.core.auth.User;
import com.magictech.core.module.BaseModuleController;
import com.magictech.core.ui.SceneManager;
import com.magictech.modules.projects.ProjectDetailViewController;
import com.magictech.modules.projects.entity.Project;
import com.magictech.modules.projects.model.ProjectViewModel;
import com.magictech.modules.projects.service.ProjectService;
import com.magictech.modules.storage.dto.CustomerAnalyticsDTO;
import com.magictech.modules.storage.dto.ProjectAnalyticsDTO;
import com.magictech.modules.storage.entity.StorageItem;
import com.magictech.modules.storage.model.StorageItemViewModel;
import com.magictech.modules.storage.service.StorageService;
import com.magictech.modules.storage.service.ExcelImportService;
import com.magictech.modules.storage.service.ExcelExportService;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Storage Module - MASTER CONTROL with Dual Tables
 * Tab 1: Storage Items Table (with Price & Quantity)
 * Tab 2: Projects Table (project management)
 */
@Component
public class StorageController extends BaseModuleController {

    @Autowired
    private StorageService storageService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ExcelImportService excelImportService;

    @Autowired
    private ExcelExportService excelExportService;

    @Autowired
    private com.magictech.modules.storage.service.AnalyticsService analyticsService;

    // Active table tracker
    private enum ActiveTable { STORAGE, ANALYTICS }
    private ActiveTable currentTable = ActiveTable.STORAGE;

    // UI Components
    private TableView<StorageItemViewModel> storageTable;
    private ScrollPane analyticsView;
    private StackPane tableContainer;
    private TextField searchField;
    private Button addButton, editButton, deleteButton, refreshButton;
    private Button importButton, exportButton, columnsButton, openProjectButton;
    private Button storageTabButton, projectsTabButton;
    private com.magictech.core.ui.components.DashboardBackgroundPane backgroundPane;
    private ProgressIndicator loadingIndicator;
    private Label selectedCountLabel;
    private CheckBox selectAllCheckbox;

    // Storage Data
    private ObservableList<StorageItemViewModel> storageItems;
    private FilteredList<StorageItemViewModel> filteredStorage;
    private Map<StorageItemViewModel, BooleanProperty> storageSelectionMap = new HashMap<>();

    @Override
    protected void setupUI() {
        StackPane stackRoot = new StackPane();
        backgroundPane = new com.magictech.core.ui.components.DashboardBackgroundPane();

        BorderPane contentPane = new BorderPane();
        contentPane.setStyle("-fx-background-color: transparent;");

        VBox header = createHeader();
        contentPane.setTop(header);

        VBox content = createMainContent();
        contentPane.setCenter(content);

        stackRoot.getChildren().addAll(backgroundPane, contentPane);

        BorderPane root = getRootPane();
        root.setCenter(stackRoot);
        root.setStyle("-fx-background-color: transparent;");
    }

    @Override
    protected void loadData() {
        // Initialize Storage
        storageItems = FXCollections.observableArrayList();
        filteredStorage = new FilteredList<>(storageItems, p -> true);
        storageTable.setItems(filteredStorage);

        // Load storage data
        loadStorageData();
    }

    private VBox createHeader() {
        VBox headerContainer = new VBox();

        // Top bar
        HBox headerBar = new HBox();
        headerBar.setAlignment(Pos.CENTER_LEFT);
        headerBar.setSpacing(20);
        headerBar.setPadding(new Insets(20, 30, 20, 30));
        headerBar.setStyle(
                "-fx-background-color: linear-gradient(to right, #ef4444, #dc2626);" +
                        "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 15, 0, 0, 3);"
        );

        Button backButton = new Button("← Back");
        backButton.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.2);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 20;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        );
        backButton.setOnAction(e -> handleBack());

        HBox titleBox = new HBox(15);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        Label iconLabel = new Label("📦");
        iconLabel.setFont(new Font(32));
        Label titleLabel = new Label("Storage Management");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 26px; -fx-font-weight: bold;");
        titleBox.getChildren().addAll(iconLabel, titleLabel);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Label userLabel = new Label("👤 " + (currentUser != null ? currentUser.getUsername() : "User"));
        userLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.9); -fx-font-size: 14px;");

        headerBar.getChildren().addAll(backButton, titleBox, userLabel);

        // Subtitle bar
        HBox subtitleBar = new HBox(20);
        subtitleBar.setAlignment(Pos.CENTER);
        subtitleBar.setPadding(new Insets(12, 30, 12, 30));
        subtitleBar.setStyle(
                "-fx-background-color: rgba(20, 30, 45, 0.4);" +
                        "-fx-border-color: rgba(255, 255, 255, 0.1);" +
                        "-fx-border-width: 0 0 1 0;"
        );

        Label subtitleLabel = new Label("Master Control • Storage Management + Business Analytics");
        subtitleLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 14px;");

        selectedCountLabel = new Label();
        selectedCountLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 14px; -fx-font-weight: bold;");
        selectedCountLabel.setVisible(false);

        subtitleBar.getChildren().addAll(subtitleLabel, selectedCountLabel);

        headerContainer.getChildren().addAll(headerBar, subtitleBar);
        return headerContainer;
    }

    private VBox createMainContent() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: transparent;");

        // TAB SWITCHER
        HBox tabSwitcher = createTabSwitcher();

        HBox toolbar = createToolbar();

        tableContainer = new StackPane();
        storageTable = createStorageTable();
        analyticsView = createAnalyticsView();

        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setVisible(false);
        loadingIndicator.setMaxSize(60, 60);

        // Show storage table by default
        tableContainer.getChildren().addAll(storageTable, loadingIndicator);
        analyticsView.setVisible(false);

        VBox.setVgrow(tableContainer, Priority.ALWAYS);

        content.getChildren().addAll(tabSwitcher, toolbar, tableContainer);
        return content;
    }

    private HBox createTabSwitcher() {
        HBox tabBox = new HBox(10);
        tabBox.setAlignment(Pos.CENTER_LEFT);
        tabBox.setPadding(new Insets(0, 0, 10, 0));

        storageTabButton = createTabButton("📦 Storage Management", true);
        storageTabButton.setOnAction(e -> switchToStorageTable());

        projectsTabButton = createTabButton("📊 Analytics Dashboard", false);
        projectsTabButton.setOnAction(e -> switchToAnalyticsView());

        tabBox.getChildren().addAll(storageTabButton, projectsTabButton);
        return tabBox;
    }

    private Button createTabButton(String text, boolean active) {
        Button btn = new Button(text);
        updateTabButtonStyle(btn, active);
        return btn;
    }

    private void updateTabButtonStyle(Button btn, boolean active) {
        if (active) {
            btn.setStyle(
                    "-fx-background-color: linear-gradient(to right, #ef4444, #dc2626);" +
                            "-fx-text-fill: white;" +
                            "-fx-font-size: 14px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-padding: 12 24;" +
                            "-fx-background-radius: 8;" +
                            "-fx-border-color: rgba(239, 68, 68, 0.6);" +
                            "-fx-border-width: 2;" +
                            "-fx-border-radius: 8;" +
                            "-fx-cursor: hand;" +
                            "-fx-effect: dropshadow(gaussian, rgba(239, 68, 68, 0.4), 10, 0, 0, 3);"
            );
        } else {
            btn.setStyle(
                    "-fx-background-color: rgba(30, 41, 59, 0.6);" +
                            "-fx-text-fill: rgba(255, 255, 255, 0.7);" +
                            "-fx-font-size: 14px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-padding: 12 24;" +
                            "-fx-background-radius: 8;" +
                            "-fx-border-color: rgba(239, 68, 68, 0.3);" +
                            "-fx-border-width: 1;" +
                            "-fx-border-radius: 8;" +
                            "-fx-cursor: hand;"
            );
        }

        btn.setOnMouseEntered(e -> {
            if (!active) {
                btn.setStyle(
                        "-fx-background-color: rgba(30, 41, 59, 0.8);" +
                                "-fx-text-fill: white;" +
                                "-fx-font-size: 14px;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 12 24;" +
                                "-fx-background-radius: 8;" +
                                "-fx-border-color: rgba(239, 68, 68, 0.5);" +
                                "-fx-border-width: 1;" +
                                "-fx-border-radius: 8;" +
                                "-fx-cursor: hand;"
                );
            }
        });

        btn.setOnMouseExited(e -> updateTabButtonStyle(btn, active));
    }

    private void switchToStorageTable() {
        currentTable = ActiveTable.STORAGE;

        tableContainer.getChildren().clear();
        tableContainer.getChildren().addAll(storageTable, loadingIndicator);
        storageTable.setVisible(true);
        analyticsView.setVisible(false);

        updateTabButtonStyle(storageTabButton, true);
        updateTabButtonStyle(projectsTabButton, false);

        // Show storage-specific buttons
        importButton.setVisible(true);
        importButton.setManaged(true);
        exportButton.setVisible(true);
        exportButton.setManaged(true);
        columnsButton.setVisible(true);
        columnsButton.setManaged(true);
        openProjectButton.setVisible(false);
        openProjectButton.setManaged(false);
        addButton.setVisible(true);
        addButton.setManaged(true);
        editButton.setVisible(true);
        editButton.setManaged(true);
        deleteButton.setVisible(true);
        deleteButton.setManaged(true);

        storageTable.refresh();
        updateSelectedCount();

        System.out.println("✓ Switched to Storage Table");
    }

    private void switchToAnalyticsView() {
        currentTable = ActiveTable.ANALYTICS;

        tableContainer.getChildren().clear();
        tableContainer.getChildren().addAll(analyticsView, loadingIndicator);
        analyticsView.setVisible(true);
        storageTable.setVisible(false);

        updateTabButtonStyle(storageTabButton, false);
        updateTabButtonStyle(projectsTabButton, true);

        // Hide all edit buttons (analytics is read-only)
        importButton.setVisible(false);
        importButton.setManaged(false);
        exportButton.setVisible(false);
        exportButton.setManaged(false);
        columnsButton.setVisible(false);
        columnsButton.setManaged(false);
        openProjectButton.setVisible(false);
        openProjectButton.setManaged(false);
        addButton.setVisible(false);
        addButton.setManaged(false);
        editButton.setVisible(false);
        editButton.setManaged(false);
        deleteButton.setVisible(false);
        deleteButton.setManaged(false);

        storageSelectionMap.forEach((item, prop) -> prop.set(false));
        selectedCountLabel.setVisible(false);

        // Refresh analytics data
        refreshAnalytics();

        System.out.println("✓ Switched to Analytics Dashboard");
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0, 0, 15, 0));

        addButton = createStyledButton("+ Add", "#22c55e", "#16a34a");
        addButton.setOnAction(e -> handleAdd());

        openProjectButton = createStyledButton("📂 Open", "#6366f1", "#4f46e5");
        openProjectButton.setOnAction(e -> handleOpenProject());
        openProjectButton.setDisable(true);
        openProjectButton.setVisible(false);
        openProjectButton.setManaged(false);

        editButton = createStyledButton("✏️ Edit", "#3b82f6", "#2563eb");
        editButton.setOnAction(e -> handleEdit());
        editButton.setDisable(true);

        deleteButton = createStyledButton("🗑️ Delete", "#ef4444", "#dc2626");
        deleteButton.setOnAction(e -> handleDelete());
        deleteButton.setDisable(true);

        importButton = createStyledButton("📤 Import", "#f59e0b", "#d97706");
        importButton.setOnAction(e -> handleExcelImport());

        exportButton = createStyledButton("📥 Export", "#8b5cf6", "#7c3aed");
        exportButton.setOnAction(e -> handleExcelExport());

        columnsButton = createStyledButton("⚙️ Columns", "#6366f1", "#4f46e5");
        columnsButton.setOnAction(e -> {
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Column Management");
            info.setHeaderText("Feature Coming Soon");
            info.setContentText("Column management coming soon!");
            info.showAndWait();
        });

        refreshButton = createStyledButton("↻ Refresh", "#10b981", "#059669");
        refreshButton.setOnAction(e -> refresh());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        searchField = new TextField();
        searchField.setPromptText("🔍 Search...");
        searchField.setPrefWidth(280);
        searchField.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.6);" +
                        "-fx-text-fill: white;" +
                        "-fx-prompt-text-fill: rgba(255, 255, 255, 0.5);" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-color: rgba(139, 92, 246, 0.3);" +
                        "-fx-border-radius: 8;" +
                        "-fx-border-width: 1;" +
                        "-fx-padding: 8 12;"
        );
        searchField.textProperty().addListener((obs, old, newVal) -> handleSearch(newVal));

        toolbar.getChildren().addAll(addButton, openProjectButton, editButton, deleteButton,
                importButton, exportButton, columnsButton, refreshButton, spacer, searchField);
        return toolbar;
    }

    private Button createStyledButton(String text, String bgColor, String hoverColor) {
        Button button = new Button(text);
        button.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 18;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.2), 5, 0, 0, 2);"
        );

        button.setOnMouseEntered(e -> button.setStyle(
                "-fx-background-color: " + hoverColor + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 18;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 8, 0, 0, 3);"
        ));

        button.setOnMouseExited(e -> button.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 18;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.2), 5, 0, 0, 2);"
        ));

        return button;
    }

    // ==================== STORAGE TABLE ====================

    private TableView<StorageItemViewModel> createStorageTable() {
        TableView<StorageItemViewModel> table = new TableView<>();
        table.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.5);" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: rgba(239, 68, 68, 0.3);" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-width: 2;"
        );
        table.setEditable(true);
        buildStorageColumns(table);
        return table;
    }

    private void buildStorageColumns(TableView<StorageItemViewModel> table) {
        table.getColumns().clear();
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        // Checkbox
        TableColumn<StorageItemViewModel, Boolean> selectCol = new TableColumn<>();
        selectCol.setPrefWidth(40);
        selectCol.setMaxWidth(40);
        selectCol.setMinWidth(40);
        selectCol.setResizable(false);
        selectCol.setEditable(true);

        CheckBox storageSelectAll = new CheckBox();
        storageSelectAll.setOnAction(e -> {
            boolean selectAll = storageSelectAll.isSelected();
            for (StorageItemViewModel item : filteredStorage) {
                storageSelectionMap.get(item).set(selectAll);
            }
        });
        selectCol.setGraphic(storageSelectAll);

        selectCol.setCellValueFactory(cellData -> {
            StorageItemViewModel item = cellData.getValue();
            return storageSelectionMap.computeIfAbsent(item, k -> {
                BooleanProperty prop = new SimpleBooleanProperty(false);
                prop.addListener((obs, oldVal, newVal) -> {
                    updateSelectedCount();
                    Platform.runLater(() -> storageTable.refresh());
                });
                return prop;
            });
        });

        selectCol.setCellFactory(col -> {
            CheckBoxTableCell<StorageItemViewModel, Boolean> cell = new CheckBoxTableCell<>();
            cell.setAlignment(Pos.CENTER);
            cell.setEditable(true);
            return cell;
        });

        table.getColumns().add(selectCol);

        // ID
        TableColumn<StorageItemViewModel, String> idCol = new TableColumn<>("ID");
        idCol.setPrefWidth(60);
        idCol.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getId())));
        idCol.setStyle("-fx-alignment: CENTER;");
        table.getColumns().add(idCol);

        // Manufacture
        TableColumn<StorageItemViewModel, String> mfgCol = new TableColumn<>("Manufacture");
        mfgCol.setPrefWidth(150);
        mfgCol.setCellValueFactory(new PropertyValueFactory<>("manufacture"));
        mfgCol.setStyle("-fx-alignment: CENTER-LEFT; -fx-padding: 0 10 0 10;");
        table.getColumns().add(mfgCol);

        // Product Name
        TableColumn<StorageItemViewModel, String> productCol = new TableColumn<>("Product Name");
        productCol.setPrefWidth(200);
        productCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        productCol.setStyle("-fx-alignment: CENTER-LEFT; -fx-padding: 0 10 0 10;");
        table.getColumns().add(productCol);

        // Code
        TableColumn<StorageItemViewModel, String> codeCol = new TableColumn<>("Code");
        codeCol.setPrefWidth(120);
        codeCol.setCellValueFactory(new PropertyValueFactory<>("code"));
        codeCol.setStyle("-fx-alignment: CENTER;");
        table.getColumns().add(codeCol);

        // Serial Number
        TableColumn<StorageItemViewModel, String> serialCol = new TableColumn<>("Serial Number");
        serialCol.setPrefWidth(150);
        serialCol.setCellValueFactory(new PropertyValueFactory<>("serialNumber"));
        serialCol.setStyle("-fx-alignment: CENTER;");
        table.getColumns().add(serialCol);

        // Quantity (SHOWN in Storage module)
        TableColumn<StorageItemViewModel, String> qtyCol = new TableColumn<>("Quantity");
        qtyCol.setPrefWidth(100);
        qtyCol.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getQuantity())));
        qtyCol.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold;");
        table.getColumns().add(qtyCol);

        // Price (SHOWN in Storage module)
        TableColumn<StorageItemViewModel, String> priceCol = new TableColumn<>("Price");
        priceCol.setPrefWidth(120);
        priceCol.setCellValueFactory(cellData -> {
            BigDecimal price = cellData.getValue().getPrice();
            return new SimpleStringProperty(String.format("$%.2f", price.doubleValue()));
        });
        priceCol.setCellFactory(col -> new TableCell<StorageItemViewModel, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER-RIGHT; -fx-padding: 0 15 0 0; " +
                            "-fx-text-fill: #22c55e; -fx-font-weight: bold; -fx-font-size: 13px;");
                }
            }
        });
        table.getColumns().add(priceCol);

        // Row factory with selection highlighting
        table.setRowFactory(tv -> {
            TableRow<StorageItemViewModel> row = new TableRow<StorageItemViewModel>() {
                @Override
                protected void updateItem(StorageItemViewModel item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setStyle("");
                    } else {
                        BooleanProperty selected = storageSelectionMap.get(item);
                        if (selected != null) {
                            selected.addListener((obs, oldVal, newVal) -> updateStorageRowStyle(this, newVal));
                            updateStorageRowStyle(this, selected.get());
                        }
                    }
                }
            };
            return row;
        });
    }

    private void updateStorageRowStyle(TableRow<StorageItemViewModel> row, boolean isSelected) {
        if (isSelected) {
            row.setStyle(
                    "-fx-background-color: rgba(34, 197, 94, 0.2);" +
                            "-fx-border-color: rgba(34, 197, 94, 0.6);" +
                            "-fx-border-width: 0 0 0 4;"
            );
        } else {
            int index = row.getIndex();
            if (index % 2 == 0) {
                row.setStyle("-fx-background-color: rgba(30, 41, 59, 0.3);");
            } else {
                row.setStyle("-fx-background-color: rgba(15, 23, 42, 0.4);");
            }
        }
    }

    // ==================== ANALYTICS DASHBOARD ====================

    private ScrollPane createAnalyticsView() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle(
                "-fx-background: transparent;" +
                        "-fx-background-color: transparent;"
        );

        VBox content = new VBox(30);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: transparent;");

        // Title
        Label title = new Label("📊 Business Analytics Dashboard");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: bold;");

        Label subtitle = new Label("Comprehensive business insights and performance metrics");
        subtitle.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 14px;");

        // Metrics Cards
        HBox metricsCards = new HBox(20);
        metricsCards.setAlignment(Pos.CENTER);
        metricsCards.setId("metricsCards");

        // Projects Analytics Table
        VBox projectsSection = new VBox(15);
        Label projectsTitle = new Label("📁 Projects Analytics");
        projectsTitle.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");

        TableView<com.magictech.modules.storage.dto.ProjectAnalyticsDTO> projectsAnalyticsTable = new TableView<>();
        projectsAnalyticsTable.setId("projectsAnalyticsTable");
        projectsAnalyticsTable.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.5);" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: rgba(99, 102, 241, 0.3);" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-width: 2;"
        );
        projectsAnalyticsTable.setPrefHeight(300);

        // Project columns
        TableColumn<com.magictech.modules.storage.dto.ProjectAnalyticsDTO, String> pNameCol = new TableColumn<>("Project Name");
        pNameCol.setPrefWidth(200);
        pNameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getProjectName()));

        TableColumn<com.magictech.modules.storage.dto.ProjectAnalyticsDTO, String> pStatusCol = new TableColumn<>("Status");
        pStatusCol.setPrefWidth(120);
        pStatusCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getStatus()));

        TableColumn<com.magictech.modules.storage.dto.ProjectAnalyticsDTO, String> pDurationCol = new TableColumn<>("Duration (days)");
        pDurationCol.setPrefWidth(120);
        pDurationCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getDurationDays() != null ? String.valueOf(data.getValue().getDurationDays()) : "N/A"
        ));

        TableColumn<com.magictech.modules.storage.dto.ProjectAnalyticsDTO, String> pElementsCol = new TableColumn<>("Elements");
        pElementsCol.setPrefWidth(100);
        pElementsCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getElementsCount() != null ? String.valueOf(data.getValue().getElementsCount()) : "0"
        ));

        TableColumn<com.magictech.modules.storage.dto.ProjectAnalyticsDTO, String> pCostCol = new TableColumn<>("Total Cost");
        pCostCol.setPrefWidth(120);
        pCostCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getTotalCost() != null ? String.format("$%.2f", data.getValue().getTotalCost()) : "$0.00"
        ));

        projectsAnalyticsTable.getColumns().addAll(pNameCol, pStatusCol, pDurationCol, pElementsCol, pCostCol);

        projectsSection.getChildren().addAll(projectsTitle, projectsAnalyticsTable);

        // Customer Analytics Table
        VBox customersSection = new VBox(15);
        Label customersTitle = new Label("👥 Customer Analytics");
        customersTitle.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");

        TableView<com.magictech.modules.storage.dto.CustomerAnalyticsDTO> customersAnalyticsTable = new TableView<>();
        customersAnalyticsTable.setId("customersAnalyticsTable");
        customersAnalyticsTable.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.5);" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: rgba(34, 197, 94, 0.3);" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-width: 2;"
        );
        customersAnalyticsTable.setPrefHeight(300);

        // Customer columns
        TableColumn<com.magictech.modules.storage.dto.CustomerAnalyticsDTO, String> cNameCol = new TableColumn<>("Customer Name");
        cNameCol.setPrefWidth(200);
        cNameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getCustomerName()));

        TableColumn<com.magictech.modules.storage.dto.CustomerAnalyticsDTO, String> cEmailCol = new TableColumn<>("Email");
        cEmailCol.setPrefWidth(180);
        cEmailCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getEmail()));

        TableColumn<com.magictech.modules.storage.dto.CustomerAnalyticsDTO, String> cOrdersCol = new TableColumn<>("Orders");
        cOrdersCol.setPrefWidth(100);
        cOrdersCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getOrdersCount() != null ? String.valueOf(data.getValue().getOrdersCount()) : "0"
        ));

        TableColumn<com.magictech.modules.storage.dto.CustomerAnalyticsDTO, String> cSalesCol = new TableColumn<>("Total Sales");
        cSalesCol.setPrefWidth(120);
        cSalesCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getTotalSales() != null ? String.format("$%.2f", data.getValue().getTotalSales()) : "$0.00"
        ));

        customersAnalyticsTable.getColumns().addAll(cNameCol, cEmailCol, cOrdersCol, cSalesCol);

        customersSection.getChildren().addAll(customersTitle, customersAnalyticsTable);

        content.getChildren().addAll(title, subtitle, metricsCards, projectsSection, customersSection);
        scrollPane.setContent(content);

        return scrollPane;
    }

    private void refreshAnalytics() {
        Platform.runLater(() -> {
            try {
                // Get metrics
                com.magictech.modules.storage.service.AnalyticsService.BusinessMetricsDTO metrics =
                    analyticsService.getBusinessMetrics();

                // Update metrics cards
                HBox metricsCards = (HBox) analyticsView.lookup("#metricsCards");
                if (metricsCards != null) {
                    metricsCards.getChildren().clear();
                    metricsCards.getChildren().addAll(
                        createMetricCard("Projects", String.valueOf(metrics.getTotalProjects()), "#6366f1"),
                        createMetricCard("Completed", String.valueOf(metrics.getCompletedProjects()), "#22c55e"),
                        createMetricCard("Active", String.valueOf(metrics.getActiveProjects()), "#f59e0b"),
                        createMetricCard("Customers", String.valueOf(metrics.getTotalCustomers()), "#8b5cf6"),
                        createMetricCard("Revenue", String.format("$%.0f", metrics.getTotalRevenue()), "#ef4444")
                    );
                }

                // Load projects analytics
                List<com.magictech.modules.storage.dto.ProjectAnalyticsDTO> projectAnalytics =
                    analyticsService.getProjectAnalytics();
                TableView<com.magictech.modules.storage.dto.ProjectAnalyticsDTO> projectsTable =
                    (TableView<com.magictech.modules.storage.dto.ProjectAnalyticsDTO>) analyticsView.lookup("#projectsAnalyticsTable");
                if (projectsTable != null) {
                    projectsTable.getItems().setAll(projectAnalytics);
                }

                // Load customers analytics
                List<com.magictech.modules.storage.dto.CustomerAnalyticsDTO> customerAnalytics =
                    analyticsService.getCustomerAnalytics();
                TableView<com.magictech.modules.storage.dto.CustomerAnalyticsDTO> customersTable =
                    (TableView<com.magictech.modules.storage.dto.CustomerAnalyticsDTO>) analyticsView.lookup("#customersAnalyticsTable");
                if (customersTable != null) {
                    customersTable.getItems().setAll(customerAnalytics);
                }

                System.out.println("✓ Analytics refreshed successfully");
            } catch (Exception ex) {
                System.err.println("Error refreshing analytics: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
    }

    private VBox createMetricCard(String label, String value, String color) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20));
        card.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.6);" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: " + color + ";" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 12;" +
                        "-fx-min-width: 150px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 10, 0, 0, 3);"
        );

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 32px; -fx-font-weight: bold;");

        Label labelText = new Label(label);
        labelText.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 14px;");

        card.getChildren().addAll(valueLabel, labelText);
        return card;
    }

    // ==================== DATA LOADING ====================

    private void loadStorageData() {
        Task<List<StorageItem>> loadTask = new Task<>() {
            @Override
            protected List<StorageItem> call() {
                return storageService.getAllItems();
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<StorageItem> items = loadTask.getValue();
            storageItems.clear();
            storageSelectionMap.clear();

            for (StorageItem entity : items) {
                StorageItemViewModel vm = convertStorageToViewModel(entity);
                storageItems.add(vm);
                BooleanProperty prop = new SimpleBooleanProperty(false);
                prop.addListener((obs, oldVal, newVal) -> {
                    updateSelectedCount();
                    Platform.runLater(() -> storageTable.refresh());
                });
                storageSelectionMap.put(vm, prop);
            }

            System.out.println("✓ Loaded " + items.size() + " storage items");
        });

        loadTask.setOnFailed(e -> {
            showError("Failed to load storage items: " + loadTask.getException().getMessage());
        });

        new Thread(loadTask).start();
    }

    private StorageItemViewModel convertStorageToViewModel(StorageItem entity) {
        StorageItemViewModel vm = new StorageItemViewModel();
        vm.setId(entity.getId());
        vm.setManufacture(entity.getManufacture() != null ? entity.getManufacture() : "");
        vm.setProductName(entity.getProductName());
        vm.setCode(entity.getCode() != null ? entity.getCode() : "");
        vm.setSerialNumber(entity.getSerialNumber() != null ? entity.getSerialNumber() : "");
        vm.setQuantity(entity.getQuantity() != null ? entity.getQuantity() : 0);
        vm.setPrice(entity.getPrice() != null ? entity.getPrice() : BigDecimal.ZERO);
        vm.setDateAdded(entity.getDateAdded().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        return vm;
    }

    private ProjectViewModel convertProjectToViewModel(Project entity) {
        ProjectViewModel vm = new ProjectViewModel();
        vm.setId(entity.getId());
        vm.setProjectName(entity.getProjectName());
        vm.setProjectLocation(entity.getProjectLocation() != null ? entity.getProjectLocation() : "");
        vm.setDateOfIssue(entity.getDateOfIssue() != null ?
                entity.getDateOfIssue().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "");
        vm.setDateOfCompletion(entity.getDateOfCompletion() != null ?
                entity.getDateOfCompletion().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "");
        vm.setStatus(entity.getStatus() != null ? entity.getStatus() : "Planning");
        vm.setDateAdded(entity.getDateAdded().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        return vm;
    }

    // ==================== HANDLERS ====================

    private void handleAdd() {
        if (currentTable == ActiveTable.STORAGE) {
            handleAddStorageItem();
        } else {
            handleAddProject();
        }
    }

    private void handleEdit() {
        if (currentTable == ActiveTable.STORAGE) {
            handleEditStorageItem();
        } else {
            handleEditProject();
        }
    }

    private void handleDelete() {
        if (currentTable == ActiveTable.STORAGE) {
            handleDeleteStorageItems();
        } else {
            handleDeleteProjects();
        }
    }

    private void handleSearch(String searchText) {
        if (currentTable == ActiveTable.STORAGE) {
            filteredStorage.setPredicate(item -> {
                if (searchText == null || searchText.isEmpty()) return true;
                String lower = searchText.toLowerCase();
                return item.getManufacture().toLowerCase().contains(lower) ||
                        item.getProductName().toLowerCase().contains(lower) ||
                        item.getCode().toLowerCase().contains(lower) ||
                        item.getSerialNumber().toLowerCase().contains(lower);
            });
        } else {
            filteredProjects.setPredicate(item -> {
                if (searchText == null || searchText.isEmpty()) return true;
                String lower = searchText.toLowerCase();
                return item.getProjectName().toLowerCase().contains(lower) ||
                        item.getProjectLocation().toLowerCase().contains(lower) ||
                        item.getStatus().toLowerCase().contains(lower);
            });
        }
    }

    private void updateSelectedCount() {
        long count;
        if (currentTable == ActiveTable.STORAGE) {
            count = storageSelectionMap.values().stream().filter(BooleanProperty::get).count();
        } else {
            count = projectSelectionMap.values().stream().filter(BooleanProperty::get).count();
        }

        Platform.runLater(() -> {
            if (count > 0) {
                selectedCountLabel.setText("✓ " + count + " item(s) selected");
                selectedCountLabel.setVisible(true);
                deleteButton.setDisable(false);
                editButton.setDisable(count != 1);
                openProjectButton.setDisable(currentTable != ActiveTable.PROJECTS || count != 1);
            } else {
                selectedCountLabel.setVisible(false);
                deleteButton.setDisable(true);
                editButton.setDisable(true);
                openProjectButton.setDisable(true);
            }
        });
    }

    // ==================== STORAGE CRUD ====================

    private void handleAddStorageItem() {
        Dialog<StorageItemViewModel> dialog = createStorageDialog(null);
        Optional<StorageItemViewModel> result = dialog.showAndWait();

        result.ifPresent(vm -> {
            Task<StorageItem> saveTask = new Task<>() {
                @Override
                protected StorageItem call() {
                    StorageItem entity = new StorageItem();
                    entity.setManufacture(vm.getManufacture());
                    entity.setProductName(vm.getProductName());
                    entity.setCode(vm.getCode());
                    entity.setSerialNumber(vm.getSerialNumber());
                    entity.setQuantity(vm.getQuantity());
                    entity.setPrice(vm.getPrice());
                    entity.setCreatedBy(currentUser != null ? currentUser.getUsername() : "system");
                    return storageService.createItem(entity);
                }
            };

            saveTask.setOnSucceeded(e -> {
                StorageItem saved = saveTask.getValue();
                StorageItemViewModel savedVM = convertStorageToViewModel(saved);
                Platform.runLater(() -> {
                    storageItems.add(savedVM);
                    storageSelectionMap.put(savedVM, new SimpleBooleanProperty(false));
                    showSuccess("✓ Storage item created!");
                });
            });

            saveTask.setOnFailed(e -> showError("Failed to save: " + saveTask.getException().getMessage()));
            new Thread(saveTask).start();
        });
    }

    private void handleEditStorageItem() {
        List<StorageItemViewModel> selected = storageSelectionMap.entrySet().stream()
                .filter(e -> e.getValue().get())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (selected.isEmpty()) {
            showWarning("Please select an item to edit");
            return;
        }
        if (selected.size() > 1) {
            showWarning("Please select only ONE item to edit");
            return;
        }

        StorageItemViewModel item = selected.get(0);
        Dialog<StorageItemViewModel> dialog = createStorageDialog(item);
        Optional<StorageItemViewModel> result = dialog.showAndWait();

        result.ifPresent(updated -> {
            Task<StorageItem> updateTask = new Task<>() {
                @Override
                protected StorageItem call() {
                    StorageItem entity = new StorageItem();
                    entity.setManufacture(updated.getManufacture());
                    entity.setProductName(updated.getProductName());
                    entity.setCode(updated.getCode());
                    entity.setSerialNumber(updated.getSerialNumber());
                    entity.setQuantity(updated.getQuantity());
                    entity.setPrice(updated.getPrice());
                    return storageService.updateItem(item.getId(), entity);
                }
            };

            updateTask.setOnSucceeded(e -> {
                Platform.runLater(() -> {
                    item.setManufacture(updated.getManufacture());
                    item.setProductName(updated.getProductName());
                    item.setCode(updated.getCode());
                    item.setSerialNumber(updated.getSerialNumber());
                    item.setQuantity(updated.getQuantity());
                    item.setPrice(updated.getPrice());
                    storageTable.refresh();
                    showSuccess("✓ Storage item updated!");
                });
            });

            updateTask.setOnFailed(e -> showError("Update failed: " + updateTask.getException().getMessage()));
            new Thread(updateTask).start();
        });
    }

    private void handleDeleteStorageItems() {
        List<StorageItemViewModel> selected = storageSelectionMap.entrySet().stream()
                .filter(e -> e.getValue().get())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (selected.isEmpty()) {
            showWarning("Please select items to delete");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("⚠️ DELETE");
        confirm.setHeaderText("DELETE " + selected.size() + " storage item(s)?");
        confirm.setContentText("This action cannot be undone!");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Task<Void> deleteTask = new Task<>() {
                @Override
                protected Void call() {
                    List<Long> ids = selected.stream().map(StorageItemViewModel::getId).collect(Collectors.toList());
                    storageService.deleteItems(ids);
                    return null;
                }
            };

            deleteTask.setOnSucceeded(e -> {
                Platform.runLater(() -> {
                    for (StorageItemViewModel item : selected) {
                        storageItems.remove(item);
                        storageSelectionMap.remove(item);
                    }
                    showSuccess("✓ Deleted " + selected.size() + " storage item(s)");
                });
            });

            deleteTask.setOnFailed(e -> showError("Delete failed: " + deleteTask.getException().getMessage()));
            new Thread(deleteTask).start();
        }
    }

    private void handleExcelImport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Excel");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls")
        );

        File file = fileChooser.showOpenDialog(rootPane.getScene().getWindow());

        if (file != null) {
            Task<List<StorageItem>> importTask = new Task<>() {
                @Override
                protected List<StorageItem> call() throws Exception {
                    List<StorageItem> items = excelImportService.importFromExcel(file);
                    List<StorageItem> saved = new ArrayList<>();
                    for (StorageItem item : items) {
                        item.setCreatedBy(currentUser != null ? currentUser.getUsername() : "system");
                        saved.add(storageService.createItem(item));
                    }
                    return saved;
                }
            };

            importTask.setOnSucceeded(e -> {
                List<StorageItem> saved = importTask.getValue();
                Platform.runLater(() -> {
                    for (StorageItem entity : saved) {
                        StorageItemViewModel vm = convertStorageToViewModel(entity);
                        storageItems.add(vm);
                        storageSelectionMap.put(vm, new SimpleBooleanProperty(false));
                    }
                    showSuccess("✓ Imported " + saved.size() + " items!");
                });
            });

            importTask.setOnFailed(e -> showError("Import failed: " + importTask.getException().getMessage()));
            new Thread(importTask).start();
        }
    }

    private void handleExcelExport() {
        if (storageItems.isEmpty()) {
            showWarning("No items to export!");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Excel");
        fileChooser.setInitialFileName("storage_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel", "*.xlsx")
        );

        File file = fileChooser.showSaveDialog(rootPane.getScene().getWindow());

        if (file != null) {
            Task<File> exportTask = new Task<>() {
                @Override
                protected File call() throws Exception {
                    return excelExportService.exportToExcel(storageService.getAllItems(), file.getAbsolutePath());
                }
            };

            exportTask.setOnSucceeded(e -> showSuccess("✓ Exported " + storageItems.size() + " items!"));
            exportTask.setOnFailed(e -> showError("Export failed: " + exportTask.getException().getMessage()));
            new Thread(exportTask).start();
        }
    }

    // ==================== PROJECT CRUD ====================

    private void handleAddProject() {
        Dialog<ProjectViewModel> dialog = createProjectDialog(null);
        Optional<ProjectViewModel> result = dialog.showAndWait();

        result.ifPresent(vm -> {
            Task<Project> saveTask = new Task<>() {
                @Override
                protected Project call() {
                    Project entity = new Project();
                    entity.setProjectName(vm.getProjectName());
                    entity.setProjectLocation(vm.getProjectLocation());
                    if (!vm.getDateOfIssue().isEmpty()) {
                        entity.setDateOfIssue(LocalDate.parse(vm.getDateOfIssue()));
                    }
                    if (!vm.getDateOfCompletion().isEmpty()) {
                        entity.setDateOfCompletion(LocalDate.parse(vm.getDateOfCompletion()));
                    }
                    entity.setStatus(vm.getStatus());
                    entity.setCreatedBy(currentUser != null ? currentUser.getUsername() : "system");
                    return projectService.createProject(entity);
                }
            };

            saveTask.setOnSucceeded(e -> {
                Project saved = saveTask.getValue();
                ProjectViewModel savedVM = convertProjectToViewModel(saved);
                Platform.runLater(() -> {
                    projectItems.add(savedVM);
                    projectSelectionMap.put(savedVM, new SimpleBooleanProperty(false));
                    showSuccess("✓ Project created!");
                });
            });

            saveTask.setOnFailed(e -> showError("Failed to save: " + saveTask.getException().getMessage()));
            new Thread(saveTask).start();
        });
    }

    private void handleEditProject() {
        List<ProjectViewModel> selected = projectSelectionMap.entrySet().stream()
                .filter(e -> e.getValue().get())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (selected.isEmpty()) {
            showWarning("Please select a project to edit");
            return;
        }
        if (selected.size() > 1) {
            showWarning("Please select only ONE project to edit");
            return;
        }

        ProjectViewModel project = selected.get(0);
        Dialog<ProjectViewModel> dialog = createProjectDialog(project);
        Optional<ProjectViewModel> result = dialog.showAndWait();

        result.ifPresent(updated -> {
            Task<Project> updateTask = new Task<>() {
                @Override
                protected Project call() {
                    Project entity = new Project();
                    entity.setProjectName(updated.getProjectName());
                    entity.setProjectLocation(updated.getProjectLocation());
                    if (!updated.getDateOfIssue().isEmpty()) {
                        entity.setDateOfIssue(LocalDate.parse(updated.getDateOfIssue()));
                    }
                    if (!updated.getDateOfCompletion().isEmpty()) {
                        entity.setDateOfCompletion(LocalDate.parse(updated.getDateOfCompletion()));
                    }
                    entity.setStatus(updated.getStatus());
                    return projectService.updateProject(project.getId(), entity);
                }
            };

            updateTask.setOnSucceeded(e -> {
                Platform.runLater(() -> {
                    project.setProjectName(updated.getProjectName());
                    project.setProjectLocation(updated.getProjectLocation());
                    project.setDateOfIssue(updated.getDateOfIssue());
                    project.setDateOfCompletion(updated.getDateOfCompletion());
                    project.setStatus(updated.getStatus());
                    projectsTable.refresh();
                    showSuccess("✓ Project updated!");
                });
            });

            updateTask.setOnFailed(e -> showError("Update failed: " + updateTask.getException().getMessage()));
            new Thread(updateTask).start();
        });
    }

    private void handleDeleteProjects() {
        List<ProjectViewModel> selected = projectSelectionMap.entrySet().stream()
                .filter(e -> e.getValue().get())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (selected.isEmpty()) {
            showWarning("Please select projects to delete");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("⚠️ DELETE");
        confirm.setHeaderText("DELETE " + selected.size() + " project(s)?");
        confirm.setContentText("This action cannot be undone!");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Task<Void> deleteTask = new Task<>() {
                @Override
                protected Void call() {
                    List<Long> ids = selected.stream().map(ProjectViewModel::getId).collect(Collectors.toList());
                    projectService.deleteProjects(ids);
                    return null;
                }
            };

            deleteTask.setOnSucceeded(e -> {
                Platform.runLater(() -> {
                    for (ProjectViewModel item : selected) {
                        projectItems.remove(item);
                        projectSelectionMap.remove(item);
                    }
                    showSuccess("✓ Deleted " + selected.size() + " project(s)");
                });
            });

            deleteTask.setOnFailed(e -> showError("Delete failed: " + deleteTask.getException().getMessage()));
            new Thread(deleteTask).start();
        }
    }

    // ==================== DIALOGS ====================

    private Dialog<StorageItemViewModel> createStorageDialog(StorageItemViewModel existing) {
        Dialog<StorageItemViewModel> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Storage Item" : "Edit Storage Item");

        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        TextField mfgField = new TextField(existing != null ? existing.getManufacture() : "");
        mfgField.setPromptText("Manufacture");

        TextField nameField = new TextField(existing != null ? existing.getProductName() : "");
        nameField.setPromptText("Product Name (Required)");

        TextField codeField = new TextField(existing != null ? existing.getCode() : "");
        codeField.setPromptText("Code");

        TextField serialField = new TextField(existing != null ? existing.getSerialNumber() : "");
        serialField.setPromptText("Serial Number");

        Spinner<Integer> qtySpinner = new Spinner<>(0, 999999,
                existing != null ? existing.getQuantity() : 0);
        qtySpinner.setEditable(true);

        TextField priceField = new TextField(
                existing != null && existing.getPrice() != null ?
                        existing.getPrice().toString() : "0.00");
        priceField.setPromptText("Price");

        grid.add(new Label("Manufacture:"), 0, 0);
        grid.add(mfgField, 1, 0);
        grid.add(new Label("Product Name:*"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Code:"), 0, 2);
        grid.add(codeField, 1, 2);
        grid.add(new Label("Serial Number:"), 0, 3);
        grid.add(serialField, 1, 3);
        grid.add(new Label("Quantity:"), 0, 4);
        grid.add(qtySpinner, 1, 4);
        grid.add(new Label("Price:"), 0, 5);
        grid.add(priceField, 1, 5);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn && !nameField.getText().trim().isEmpty()) {
                StorageItemViewModel vm = new StorageItemViewModel();
                vm.setManufacture(mfgField.getText().trim());
                vm.setProductName(nameField.getText().trim());
                vm.setCode(codeField.getText().trim());
                vm.setSerialNumber(serialField.getText().trim());
                vm.setQuantity(qtySpinner.getValue());
                try {
                    vm.setPrice(new BigDecimal(priceField.getText().trim()));
                } catch (Exception e) {
                    vm.setPrice(BigDecimal.ZERO);
                }
                return vm;
            }
            return null;
        });

        return dialog;
    }

    private Dialog<ProjectViewModel> createProjectDialog(ProjectViewModel existing) {
        Dialog<ProjectViewModel> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Project" : "Edit Project");

        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        grid.setPrefWidth(500);

        TextField nameField = new TextField(existing != null ? existing.getProjectName() : "");
        nameField.setPromptText("Project Name (Required)");

        TextField locationField = new TextField(existing != null ? existing.getProjectLocation() : "");
        locationField.setPromptText("Project Location");

        DatePicker issueDate = new DatePicker();
        if (existing != null && !existing.getDateOfIssue().isEmpty()) {
            issueDate.setValue(LocalDate.parse(existing.getDateOfIssue()));
        }

        DatePicker completionDate = new DatePicker();
        if (existing != null && !existing.getDateOfCompletion().isEmpty()) {
            completionDate.setValue(LocalDate.parse(existing.getDateOfCompletion()));
        }

        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("Planning", "In Progress", "On Hold", "Completed");
        statusBox.setValue(existing != null ? existing.getStatus() : "Planning");

        grid.add(new Label("Project Name:*"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Location:"), 0, 1);
        grid.add(locationField, 1, 1);
        grid.add(new Label("Date of Issue:"), 0, 2);
        grid.add(issueDate, 1, 2);
        grid.add(new Label("Date of Completion:"), 0, 3);
        grid.add(completionDate, 1, 3);
        grid.add(new Label("Status:"), 0, 4);
        grid.add(statusBox, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn && !nameField.getText().trim().isEmpty()) {
                ProjectViewModel vm = new ProjectViewModel();
                vm.setProjectName(nameField.getText().trim());
                vm.setProjectLocation(locationField.getText().trim());
                vm.setDateOfIssue(issueDate.getValue() != null ?
                        issueDate.getValue().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "");
                vm.setDateOfCompletion(completionDate.getValue() != null ?
                        completionDate.getValue().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "");
                vm.setStatus(statusBox.getValue());
                return vm;
            }
            return null;
        });

        return dialog;
    }

    // ==================== LIFECYCLE ====================

    private void handleBack() {
        if (backgroundPane != null) {
            backgroundPane.stopAnimation();
        }
        SceneManager.getInstance().showMainDashboard();
    }

    public void immediateCleanup() {
        if (backgroundPane != null) {
            backgroundPane.stopAnimation();
            backgroundPane = null;
        }
        System.out.println("✓ Storage master controller cleaned up");
    }

    @Override
    public void refresh() {
        storageSelectionMap.clear();
        projectSelectionMap.clear();
        loadStorageData();
        loadProjectsData();
    }
}