package com.magictech.modules.storage;

import com.magictech.core.auth.User;
import com.magictech.core.auth.UserRole;
import com.magictech.core.module.BaseModuleController;
import com.magictech.core.ui.SceneManager;
import com.magictech.modules.storage.entity.StorageItem;
import com.magictech.modules.storage.entity.StorageLocation;
import com.magictech.modules.storage.entity.StorageItemLocation;
import com.magictech.modules.storage.model.StorageItemLocationViewModel;
import com.magictech.modules.storage.service.StorageService;
import com.magictech.modules.storage.service.StorageLocationService;
import com.magictech.modules.storage.service.StorageLocationService.LocationSummary;
import com.magictech.modules.storage.service.StorageItemLocationService;
import com.magictech.modules.storage.service.ExcelImportService;
import com.magictech.modules.storage.ui.LocationCardsPane;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Storage Module Controller - Advanced Multi-Location Storage Management
 *
 * Features:
 * - Road Map View: Interactive Jordan map with storage location pins
 * - Location Sheet View: Items at a specific storage location
 * - Total Sheet View: All items across all locations
 * - Navigation with breadcrumb and return path
 */
@Component
public class StorageController extends BaseModuleController {

    @Autowired
    private StorageService storageService;

    @Autowired
    private StorageLocationService locationService;

    @Autowired
    private StorageItemLocationService itemLocationService;

    @Autowired
    private ExcelImportService excelImportService;

    // View modes
    private enum ViewMode { CARDS, LOCATION_SHEET, TOTAL_SHEET }
    private ViewMode currentViewMode = ViewMode.CARDS;

    // Current location being viewed (for LOCATION_SHEET mode)
    private LocationSummary currentLocation;

    // Navigation history for breadcrumb
    private Stack<ViewMode> navigationHistory = new Stack<>();

    // UI Components
    private StackPane mainContainer;
    private LocationCardsPane locationCards;
    private VBox locationSheetView;
    private VBox totalSheetView;
    private HBox breadcrumbBar;
    private Label breadcrumbLabel;

    // Table components
    private TableView<StorageItemLocationViewModel> itemTable;
    private ObservableList<StorageItemLocationViewModel> tableItems;
    private FilteredList<StorageItemLocationViewModel> filteredItems;
    private Map<StorageItemLocationViewModel, BooleanProperty> selectionMap = new HashMap<>();

    // Toolbar components
    private TextField searchField;
    private Button addButton, editButton, deleteButton, refreshButton;
    private Button transferButton, importButton, exportButton;
    private Label selectedCountLabel;
    private CheckBox selectAllCheckbox;
    private ProgressIndicator loadingIndicator;

    // Background
    private com.magictech.core.ui.components.DashboardBackgroundPane backgroundPane;

    @Override
    protected void setupUI() {
        // Initialize default locations if needed
        locationService.initializeDefaultLocations();

        StackPane stackRoot = new StackPane();
        backgroundPane = new com.magictech.core.ui.components.DashboardBackgroundPane();

        BorderPane contentPane = new BorderPane();
        contentPane.setStyle("-fx-background-color: transparent;");

        VBox header = createHeader();
        contentPane.setTop(header);

        mainContainer = new StackPane();
        mainContainer.setStyle("-fx-background-color: transparent;");

        // Create all views
        locationCards = createCardsView();
        locationSheetView = createSheetView(false);
        totalSheetView = createSheetView(true);

        // Initially show cards view
        mainContainer.getChildren().add(locationCards);

        contentPane.setCenter(mainContainer);

        stackRoot.getChildren().addAll(backgroundPane, contentPane);

        BorderPane root = getRootPane();
        root.setCenter(stackRoot);
        root.setStyle("-fx-background-color: transparent;");
    }

    @Override
    protected void loadData() {
        loadCardsData();
    }

    // ==================== HEADER ====================

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

        // Breadcrumb bar
        breadcrumbBar = new HBox(10);
        breadcrumbBar.setAlignment(Pos.CENTER_LEFT);
        breadcrumbBar.setPadding(new Insets(12, 30, 12, 30));
        breadcrumbBar.setStyle(
            "-fx-background-color: rgba(20, 30, 45, 0.6);" +
            "-fx-border-color: rgba(255, 255, 255, 0.1);" +
            "-fx-border-width: 0 0 1 0;"
        );

        breadcrumbLabel = new Label("📦 Storage Locations");
        breadcrumbLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        selectedCountLabel = new Label();
        selectedCountLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 14px; -fx-font-weight: bold;");
        selectedCountLabel.setVisible(false);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        breadcrumbBar.getChildren().addAll(breadcrumbLabel, spacer, selectedCountLabel);

        headerContainer.getChildren().addAll(headerBar, breadcrumbBar);
        return headerContainer;
    }

    private void updateBreadcrumb() {
        String breadcrumbText;
        switch (currentViewMode) {
            case CARDS:
                breadcrumbText = "📦 Storage Locations";
                break;
            case LOCATION_SHEET:
                breadcrumbText = "📦 Locations  ›  📍 " +
                    (currentLocation != null ? currentLocation.getLocationName() : "Location");
                break;
            case TOTAL_SHEET:
                breadcrumbText = "📦 Locations  ›  📊 Total Sheet (All Locations)";
                break;
            default:
                breadcrumbText = "📦 Storage Locations";
        }
        breadcrumbLabel.setText(breadcrumbText);
    }

    // ==================== CARDS VIEW ====================

    private LocationCardsPane createCardsView() {
        LocationCardsPane cards = new LocationCardsPane();

        // Check if user can manage locations (MASTER or STORAGE roles)
        boolean canManage = currentUser != null &&
            (currentUser.getRole() == UserRole.MASTER || currentUser.getRole() == UserRole.STORAGE);
        cards.setCanManage(canManage);

        cards.setOnLocationClick(location -> {
            currentLocation = location;
            navigateToView(ViewMode.LOCATION_SHEET);
        });

        cards.setOnTotalClick(v -> {
            navigateToView(ViewMode.TOTAL_SHEET);
        });

        // Management callbacks
        cards.setOnAddLocation(v -> handleAddLocation());
        cards.setOnEditLocation(this::handleEditLocation);
        cards.setOnDeleteLocation(this::handleDeleteLocation);

        return cards;
    }

    private void loadCardsData() {
        Task<List<LocationSummary>> loadTask = new Task<>() {
            @Override
            protected List<LocationSummary> call() {
                return locationService.getAllLocationSummaries();
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<LocationSummary> summaries = loadTask.getValue();
            Platform.runLater(() -> {
                locationCards.setLocations(summaries);
                System.out.println("✓ Loaded " + summaries.size() + " storage locations");
            });
        });

        loadTask.setOnFailed(e -> {
            showError("Failed to load locations: " + loadTask.getException().getMessage());
        });

        new Thread(loadTask).start();
    }

    // ==================== SHEET VIEW (Location & Total) ====================

    private VBox createSheetView(boolean isTotalView) {
        VBox sheetView = new VBox(15);
        sheetView.setPadding(new Insets(20));
        sheetView.setStyle("-fx-background-color: transparent;");

        // Back to locations button
        Button backToLocationsButton = createStyledButton("← Back to Locations", "#6366f1", "#4f46e5");
        backToLocationsButton.setOnAction(e -> navigateToView(ViewMode.CARDS));

        // Toolbar
        HBox toolbar = createSheetToolbar(isTotalView);

        // Table
        itemTable = createItemTable(isTotalView);

        // Loading indicator
        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setVisible(false);
        loadingIndicator.setMaxSize(60, 60);

        StackPane tableContainer = new StackPane(itemTable, loadingIndicator);
        VBox.setVgrow(tableContainer, Priority.ALWAYS);

        HBox topBar = new HBox(15);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.getChildren().add(backToLocationsButton);

        sheetView.getChildren().addAll(topBar, toolbar, tableContainer);
        return sheetView;
    }

    private HBox createSheetToolbar(boolean isTotalView) {
        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0, 0, 10, 0));

        // Only show edit buttons for MASTER and STORAGE roles
        boolean canEdit = currentUser != null &&
            (currentUser.getRole() == UserRole.MASTER || currentUser.getRole() == UserRole.STORAGE);

        if (canEdit) {
            addButton = createStyledButton("+ Add Item", "#22c55e", "#16a34a");
            addButton.setOnAction(e -> handleAddItem());

            editButton = createStyledButton("✏️ Edit", "#3b82f6", "#2563eb");
            editButton.setOnAction(e -> handleEditItem());
            editButton.setDisable(true);

            deleteButton = createStyledButton("🗑️ Delete", "#ef4444", "#dc2626");
            deleteButton.setOnAction(e -> handleDeleteItems());
            deleteButton.setDisable(true);

            transferButton = createStyledButton("🔄 Transfer", "#f59e0b", "#d97706");
            transferButton.setOnAction(e -> handleTransferItem());
            transferButton.setDisable(true);

            toolbar.getChildren().addAll(addButton, editButton, deleteButton, transferButton);
        }

        refreshButton = createStyledButton("↻ Refresh", "#10b981", "#059669");
        refreshButton.setOnAction(e -> refresh());

        // Import button - only show for users who can edit
        if (canEdit) {
            importButton = createStyledButton("📤 Import Excel", "#0ea5e9", "#0284c7");
            importButton.setOnAction(e -> handleExcelImport());
            toolbar.getChildren().add(importButton);
        }

        exportButton = createStyledButton("📥 Export", "#8b5cf6", "#7c3aed");
        exportButton.setOnAction(e -> handleExcelExport());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        searchField = new TextField();
        searchField.setPromptText("🔍 Search items...");
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

        toolbar.getChildren().addAll(refreshButton, exportButton, spacer, searchField);
        return toolbar;
    }

    private TableView<StorageItemLocationViewModel> createItemTable(boolean showLocationColumn) {
        TableView<StorageItemLocationViewModel> table = new TableView<>();
        table.setStyle(
            "-fx-background-color: rgba(30, 41, 59, 0.5);" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: rgba(239, 68, 68, 0.3);" +
            "-fx-border-radius: 12;" +
            "-fx-border-width: 2;"
        );
        table.setEditable(true);

        buildTableColumns(table, showLocationColumn);
        return table;
    }

    private void buildTableColumns(TableView<StorageItemLocationViewModel> table, boolean showLocationColumn) {
        table.getColumns().clear();
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        // Checkbox column
        TableColumn<StorageItemLocationViewModel, Boolean> selectCol = new TableColumn<>();
        selectCol.setPrefWidth(40);
        selectCol.setMaxWidth(40);
        selectCol.setMinWidth(40);
        selectCol.setResizable(false);
        selectCol.setEditable(true);

        selectAllCheckbox = new CheckBox();
        selectAllCheckbox.setOnAction(e -> {
            boolean selectAll = selectAllCheckbox.isSelected();
            for (StorageItemLocationViewModel item : filteredItems) {
                selectionMap.get(item).set(selectAll);
            }
        });
        selectCol.setGraphic(selectAllCheckbox);

        selectCol.setCellValueFactory(cellData -> {
            StorageItemLocationViewModel item = cellData.getValue();
            return selectionMap.computeIfAbsent(item, k -> {
                BooleanProperty prop = new SimpleBooleanProperty(false);
                prop.addListener((obs, oldVal, newVal) -> {
                    updateSelectedCount();
                    Platform.runLater(() -> table.refresh());
                });
                return prop;
            });
        });

        selectCol.setCellFactory(col -> new CheckBoxTableCell<>(index -> {
            StorageItemLocationViewModel item = table.getItems().get(index);
            return selectionMap.get(item);
        }));

        table.getColumns().add(selectCol);

        // ID Column
        TableColumn<StorageItemLocationViewModel, String> idCol = new TableColumn<>("ID");
        idCol.setPrefWidth(60);
        idCol.setCellValueFactory(cellData ->
            new SimpleStringProperty(String.valueOf(cellData.getValue().getItemId())));
        idCol.setStyle("-fx-alignment: CENTER;");
        table.getColumns().add(idCol);

        // Location Column (only for Total Sheet)
        if (showLocationColumn) {
            TableColumn<StorageItemLocationViewModel, String> locationCol = new TableColumn<>("Location");
            locationCol.setPrefWidth(140);
            locationCol.setCellValueFactory(new PropertyValueFactory<>("locationName"));
            locationCol.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        setStyle("-fx-alignment: CENTER; -fx-text-fill: #6366f1; -fx-font-weight: bold;");
                    }
                }
            });
            table.getColumns().add(locationCol);
        }

        // Manufacture Column
        TableColumn<StorageItemLocationViewModel, String> mfgCol = new TableColumn<>("Manufacture");
        mfgCol.setPrefWidth(150);
        mfgCol.setCellValueFactory(new PropertyValueFactory<>("manufacture"));
        mfgCol.setStyle("-fx-alignment: CENTER-LEFT; -fx-padding: 0 10 0 10;");
        table.getColumns().add(mfgCol);

        // Product Name Column
        TableColumn<StorageItemLocationViewModel, String> productCol = new TableColumn<>("Product Name");
        productCol.setPrefWidth(200);
        productCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        productCol.setStyle("-fx-alignment: CENTER-LEFT; -fx-padding: 0 10 0 10;");
        table.getColumns().add(productCol);

        // Code Column
        TableColumn<StorageItemLocationViewModel, String> codeCol = new TableColumn<>("Code");
        codeCol.setPrefWidth(120);
        codeCol.setCellValueFactory(new PropertyValueFactory<>("code"));
        codeCol.setStyle("-fx-alignment: CENTER;");
        table.getColumns().add(codeCol);

        // Serial Number Column
        TableColumn<StorageItemLocationViewModel, String> serialCol = new TableColumn<>("Serial Number");
        serialCol.setPrefWidth(130);
        serialCol.setCellValueFactory(new PropertyValueFactory<>("serialNumber"));
        serialCol.setStyle("-fx-alignment: CENTER;");
        table.getColumns().add(serialCol);

        // Quantity Column
        TableColumn<StorageItemLocationViewModel, String> qtyCol = new TableColumn<>("Quantity");
        qtyCol.setPrefWidth(100);
        qtyCol.setCellValueFactory(cellData ->
            new SimpleStringProperty(String.valueOf(cellData.getValue().getQuantity())));
        qtyCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    int qty = Integer.parseInt(item);
                    if (qty <= 0) {
                        setStyle("-fx-alignment: CENTER; -fx-text-fill: #ef4444; -fx-font-weight: bold;");
                    } else if (qty < 10) {
                        setStyle("-fx-alignment: CENTER; -fx-text-fill: #f59e0b; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-alignment: CENTER; -fx-text-fill: #22c55e; -fx-font-weight: bold;");
                    }
                }
            }
        });
        table.getColumns().add(qtyCol);

        // Bin Location Column
        TableColumn<StorageItemLocationViewModel, String> binCol = new TableColumn<>("Bin Location");
        binCol.setPrefWidth(150);
        binCol.setCellValueFactory(new PropertyValueFactory<>("binLocation"));
        binCol.setStyle("-fx-alignment: CENTER;");
        table.getColumns().add(binCol);

        // Price Column
        TableColumn<StorageItemLocationViewModel, String> priceCol = new TableColumn<>("Price");
        priceCol.setPrefWidth(100);
        priceCol.setCellValueFactory(cellData -> {
            BigDecimal price = cellData.getValue().getPrice();
            return new SimpleStringProperty(String.format("$%.2f", price != null ? price.doubleValue() : 0));
        });
        priceCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER-RIGHT; -fx-padding: 0 15 0 0; " +
                            "-fx-text-fill: #22c55e; -fx-font-weight: bold;");
                }
            }
        });
        table.getColumns().add(priceCol);

        // Row factory
        table.setRowFactory(tv -> {
            TableRow<StorageItemLocationViewModel> row = new TableRow<>() {
                @Override
                protected void updateItem(StorageItemLocationViewModel item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setStyle("");
                    } else {
                        BooleanProperty selected = selectionMap.get(item);
                        if (selected != null) {
                            selected.addListener((obs, oldVal, newVal) -> updateRowStyle(this, newVal));
                            updateRowStyle(this, selected.get());
                        }
                    }
                }
            };
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    handleEditItem();
                }
            });
            return row;
        });
    }

    private void updateRowStyle(TableRow<StorageItemLocationViewModel> row, boolean isSelected) {
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

    // ==================== NAVIGATION ====================

    private void navigateToView(ViewMode newMode) {
        // Save current view to history (for back navigation)
        if (currentViewMode != newMode) {
            navigationHistory.push(currentViewMode);
        }

        currentViewMode = newMode;
        updateBreadcrumb();

        Platform.runLater(() -> {
            mainContainer.getChildren().clear();

            switch (newMode) {
                case CARDS:
                    mainContainer.getChildren().add(locationCards);
                    loadCardsData();
                    break;

                case LOCATION_SHEET:
                    // Recreate the sheet view with correct toolbar
                    locationSheetView = createSheetView(false);
                    mainContainer.getChildren().add(locationSheetView);
                    loadLocationSheetData();
                    break;

                case TOTAL_SHEET:
                    // Recreate the sheet view with location column
                    totalSheetView = createSheetView(true);
                    mainContainer.getChildren().add(totalSheetView);
                    loadTotalSheetData();
                    break;
            }
        });
    }

    private void loadLocationSheetData() {
        if (currentLocation == null) return;

        showLoading(true);
        tableItems = FXCollections.observableArrayList();
        filteredItems = new FilteredList<>(tableItems, p -> true);
        itemTable.setItems(filteredItems);
        selectionMap.clear();

        Task<List<StorageItemLocation>> loadTask = new Task<>() {
            @Override
            protected List<StorageItemLocation> call() {
                return itemLocationService.getItemsInLocation(currentLocation.getLocationId());
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<StorageItemLocation> items = loadTask.getValue();
            Platform.runLater(() -> {
                for (StorageItemLocation sil : items) {
                    StorageItemLocationViewModel vm = new StorageItemLocationViewModel(sil);
                    tableItems.add(vm);
                    BooleanProperty prop = new SimpleBooleanProperty(false);
                    prop.addListener((obs, oldVal, newVal) -> updateSelectedCount());
                    selectionMap.put(vm, prop);
                }
                showLoading(false);
                System.out.println("✓ Loaded " + items.size() + " items for " + currentLocation.getLocationName());
            });
        });

        loadTask.setOnFailed(e -> {
            showLoading(false);
            showError("Failed to load items: " + loadTask.getException().getMessage());
        });

        new Thread(loadTask).start();
    }

    private void loadTotalSheetData() {
        showLoading(true);
        tableItems = FXCollections.observableArrayList();
        filteredItems = new FilteredList<>(tableItems, p -> true);
        itemTable.setItems(filteredItems);
        selectionMap.clear();

        Task<List<StorageItemLocation>> loadTask = new Task<>() {
            @Override
            protected List<StorageItemLocation> call() {
                return itemLocationService.getAllItemLocations();
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<StorageItemLocation> items = loadTask.getValue();
            Platform.runLater(() -> {
                for (StorageItemLocation sil : items) {
                    StorageItemLocationViewModel vm = new StorageItemLocationViewModel(sil);
                    tableItems.add(vm);
                    BooleanProperty prop = new SimpleBooleanProperty(false);
                    prop.addListener((obs, oldVal, newVal) -> updateSelectedCount());
                    selectionMap.put(vm, prop);
                }
                showLoading(false);
                System.out.println("✓ Loaded " + items.size() + " total items across all locations");
            });
        });

        loadTask.setOnFailed(e -> {
            showLoading(false);
            showError("Failed to load items: " + loadTask.getException().getMessage());
        });

        new Thread(loadTask).start();
    }

    // ==================== HANDLERS ====================

    private void handleSearch(String searchText) {
        if (filteredItems == null) return;

        filteredItems.setPredicate(item -> {
            if (searchText == null || searchText.isEmpty()) return true;
            String lower = searchText.toLowerCase();
            return (item.getManufacture() != null && item.getManufacture().toLowerCase().contains(lower)) ||
                   (item.getProductName() != null && item.getProductName().toLowerCase().contains(lower)) ||
                   (item.getCode() != null && item.getCode().toLowerCase().contains(lower)) ||
                   (item.getSerialNumber() != null && item.getSerialNumber().toLowerCase().contains(lower)) ||
                   (item.getLocationName() != null && item.getLocationName().toLowerCase().contains(lower));
        });
    }

    private void updateSelectedCount() {
        long count = selectionMap.values().stream()
                .filter(BooleanProperty::get)
                .count();

        Platform.runLater(() -> {
            if (count > 0) {
                selectedCountLabel.setText("✓ " + count + " item(s) selected");
                selectedCountLabel.setVisible(true);
                if (deleteButton != null) deleteButton.setDisable(false);
                if (editButton != null) editButton.setDisable(count != 1);
                if (transferButton != null) transferButton.setDisable(count != 1);
            } else {
                selectedCountLabel.setVisible(false);
                if (deleteButton != null) deleteButton.setDisable(true);
                if (editButton != null) editButton.setDisable(true);
                if (transferButton != null) transferButton.setDisable(true);
            }
        });
    }

    private List<StorageItemLocationViewModel> getSelectedItems() {
        return selectionMap.entrySet().stream()
                .filter(entry -> entry.getValue().get())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private void handleAddItem() {
        if (currentViewMode != ViewMode.LOCATION_SHEET || currentLocation == null) {
            showWarning("Please select a storage location first from the Road Map");
            return;
        }

        Dialog<StorageItemLocationViewModel> dialog = createItemDialog(null);
        Optional<StorageItemLocationViewModel> result = dialog.showAndWait();

        result.ifPresent(vm -> {
            Task<StorageItemLocation> saveTask = new Task<>() {
                @Override
                protected StorageItemLocation call() {
                    // First create or get the storage item
                    StorageItem item = new StorageItem();
                    item.setManufacture(vm.getManufacture());
                    item.setProductName(vm.getProductName());
                    item.setCode(vm.getCode());
                    item.setSerialNumber(vm.getSerialNumber());
                    item.setPrice(vm.getPrice());
                    item.setCreatedBy(currentUser != null ? currentUser.getUsername() : "system");
                    StorageItem savedItem = storageService.createItem(item);

                    // Then add it to the location
                    return itemLocationService.addItemToLocation(
                        savedItem.getId(),
                        currentLocation.getLocationId(),
                        vm.getQuantity(),
                        currentUser != null ? currentUser.getUsername() : "system"
                    );
                }
            };

            saveTask.setOnSucceeded(e -> {
                Platform.runLater(() -> {
                    showSuccess("✓ Item added to " + currentLocation.getLocationName());
                    loadLocationSheetData();
                });
            });

            saveTask.setOnFailed(e -> showError("Failed to add item: " + saveTask.getException().getMessage()));
            new Thread(saveTask).start();
        });
    }

    private void handleEditItem() {
        List<StorageItemLocationViewModel> selected = getSelectedItems();
        if (selected.isEmpty()) {
            showWarning("Please select an item to edit");
            return;
        }
        if (selected.size() > 1) {
            showWarning("Please select only ONE item to edit");
            return;
        }

        StorageItemLocationViewModel item = selected.get(0);
        Dialog<StorageItemLocationViewModel> dialog = createItemDialog(item);
        Optional<StorageItemLocationViewModel> result = dialog.showAndWait();

        result.ifPresent(updated -> {
            Task<Void> updateTask = new Task<>() {
                @Override
                protected Void call() {
                    // Update storage item
                    StorageItem entity = storageService.findById(item.getItemId())
                            .orElseThrow(() -> new RuntimeException("Item not found"));
                    entity.setManufacture(updated.getManufacture());
                    entity.setProductName(updated.getProductName());
                    entity.setCode(updated.getCode());
                    entity.setSerialNumber(updated.getSerialNumber());
                    entity.setPrice(updated.getPrice());
                    storageService.updateItem(entity.getId(), entity);

                    // Update quantity in location
                    itemLocationService.setItemQuantityInLocation(
                        item.getItemId(),
                        item.getLocationId(),
                        updated.getQuantity(),
                        currentUser != null ? currentUser.getUsername() : "system"
                    );
                    return null;
                }
            };

            updateTask.setOnSucceeded(e -> {
                Platform.runLater(() -> {
                    showSuccess("✓ Item updated");
                    refresh();
                });
            });

            updateTask.setOnFailed(e -> showError("Update failed: " + updateTask.getException().getMessage()));
            new Thread(updateTask).start();
        });
    }

    private void handleDeleteItems() {
        List<StorageItemLocationViewModel> selected = getSelectedItems();
        if (selected.isEmpty()) {
            showWarning("Please select items to delete");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("⚠️ DELETE");
        confirm.setHeaderText("Permanently delete " + selected.size() + " item(s) from this location?");
        confirm.setContentText("This will permanently remove these item-location records from the database. This action cannot be undone.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Task<Void> deleteTask = new Task<>() {
                @Override
                protected Void call() {
                    for (StorageItemLocationViewModel vm : selected) {
                        // Use hard delete to actually remove from database
                        itemLocationService.hardRemoveItemFromLocation(vm.getItemId(), vm.getLocationId());
                    }
                    return null;
                }
            };

            deleteTask.setOnSucceeded(e -> {
                Platform.runLater(() -> {
                    showSuccess("✓ Permanently deleted " + selected.size() + " item(s) from database");
                    refresh();
                });
            });

            deleteTask.setOnFailed(e -> showError("Delete failed: " + deleteTask.getException().getMessage()));
            new Thread(deleteTask).start();
        }
    }

    private void handleTransferItem() {
        List<StorageItemLocationViewModel> selected = getSelectedItems();
        if (selected.isEmpty() || selected.size() > 1) {
            showWarning("Please select exactly ONE item to transfer");
            return;
        }

        StorageItemLocationViewModel item = selected.get(0);

        // Show transfer dialog
        Dialog<Long> transferDialog = createTransferDialog(item);
        Optional<Long> result = transferDialog.showAndWait();

        result.ifPresent(targetLocationId -> {
            // Ask for quantity
            TextInputDialog qtyDialog = new TextInputDialog(String.valueOf(item.getQuantity()));
            qtyDialog.setTitle("Transfer Quantity");
            qtyDialog.setHeaderText("How many units to transfer?");
            qtyDialog.setContentText("Quantity (max " + item.getQuantity() + "):");

            Optional<String> qtyResult = qtyDialog.showAndWait();
            qtyResult.ifPresent(qtyStr -> {
                try {
                    int qty = Integer.parseInt(qtyStr);
                    if (qty <= 0 || qty > item.getQuantity()) {
                        showError("Invalid quantity. Must be between 1 and " + item.getQuantity());
                        return;
                    }

                    Task<Void> transferTask = new Task<>() {
                        @Override
                        protected Void call() {
                            itemLocationService.transferItem(
                                item.getItemId(),
                                item.getLocationId(),
                                targetLocationId,
                                qty,
                                currentUser != null ? currentUser.getUsername() : "system"
                            );
                            return null;
                        }
                    };

                    transferTask.setOnSucceeded(e -> {
                        Platform.runLater(() -> {
                            showSuccess("✓ Transferred " + qty + " unit(s)");
                            refresh();
                        });
                    });

                    transferTask.setOnFailed(e -> showError("Transfer failed: " + transferTask.getException().getMessage()));
                    new Thread(transferTask).start();
                } catch (NumberFormatException ex) {
                    showError("Please enter a valid number");
                }
            });
        });
    }

    private void handleExcelExport() {
        if (tableItems == null || tableItems.isEmpty()) {
            showWarning("No items to export!");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export to Excel");
        String prefix = currentViewMode == ViewMode.TOTAL_SHEET ? "all_locations" :
            (currentLocation != null ? currentLocation.getLocationCode() : "storage");
        fileChooser.setInitialFileName(prefix + "_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel", "*.xlsx")
        );

        File file = fileChooser.showSaveDialog(rootPane.getScene().getWindow());
        if (file != null) {
            // TODO: Implement Excel export for item locations
            showSuccess("✓ Export functionality coming soon!");
        }
    }

    /**
     * Handle Excel import - reads items from Excel file and adds them to the current location
     * Expected columns: Manufacture | Product Name | Code | Serial Number | Quantity | Price
     */
    private void handleExcelImport() {
        if (currentViewMode != ViewMode.LOCATION_SHEET || currentLocation == null) {
            showWarning("Please select a storage location first to import items into");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Items from Excel");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File file = fileChooser.showOpenDialog(rootPane.getScene().getWindow());
        if (file == null) {
            return; // User cancelled
        }

        // Show confirmation dialog with expected format
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Import Excel File");
        confirmDialog.setHeaderText("Import items from: " + file.getName());
        confirmDialog.setContentText(
            "Expected Excel format (first row is header):\n\n" +
            "Column A: Manufacture\n" +
            "Column B: Product Name (Required)\n" +
            "Column C: Code\n" +
            "Column D: Serial Number\n" +
            "Column E: Quantity\n" +
            "Column F: Price\n\n" +
            "Items will be added to: " + currentLocation.getLocationName() + "\n\n" +
            "Continue with import?"
        );

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        // Show loading indicator
        showLoading(true);

        Task<List<StorageItem>> importTask = new Task<>() {
            @Override
            protected List<StorageItem> call() throws Exception {
                return excelImportService.importFromExcel(file);
            }
        };

        importTask.setOnSucceeded(e -> {
            List<StorageItem> importedItems = importTask.getValue();

            if (importedItems.isEmpty()) {
                Platform.runLater(() -> {
                    showLoading(false);
                    showWarning("No valid items found in the Excel file.\n" +
                        "Please ensure the file has the correct format and Product Name column is not empty.");
                });
                return;
            }

            // Save imported items to database and add to current location
            Task<Integer> saveTask = new Task<>() {
                @Override
                protected Integer call() {
                    int savedCount = 0;
                    String username = currentUser != null ? currentUser.getUsername() : "system";

                    for (StorageItem item : importedItems) {
                        try {
                            // Set creator
                            item.setCreatedBy(username);

                            // Save the storage item first
                            StorageItem savedItem = storageService.createItem(item);

                            // Add to current location with the quantity from Excel
                            int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
                            itemLocationService.addItemToLocation(
                                savedItem.getId(),
                                currentLocation.getLocationId(),
                                quantity,
                                username
                            );

                            savedCount++;
                        } catch (Exception ex) {
                            System.err.println("Failed to save item: " + item.getProductName() + " - " + ex.getMessage());
                        }
                    }
                    return savedCount;
                }
            };

            saveTask.setOnSucceeded(ev -> {
                int savedCount = saveTask.getValue();
                Platform.runLater(() -> {
                    showLoading(false);
                    if (savedCount > 0) {
                        showSuccess("✓ Successfully imported " + savedCount + " item(s) to " +
                            currentLocation.getLocationName());
                        refresh(); // Reload the table to show new items
                    } else {
                        showError("Failed to import items. Please check the Excel format and try again.");
                    }
                });
            });

            saveTask.setOnFailed(ev -> {
                Platform.runLater(() -> {
                    showLoading(false);
                    showError("Import failed: " + saveTask.getException().getMessage());
                });
            });

            new Thread(saveTask).start();
        });

        importTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                showLoading(false);
                Throwable ex = importTask.getException();
                String message = ex.getMessage();
                if (message == null || message.isEmpty()) {
                    message = ex.getClass().getSimpleName();
                }
                showError("Failed to read Excel file: " + message +
                    "\n\nPlease ensure the file is a valid Excel file (.xlsx or .xls)");
            });
        });

        new Thread(importTask).start();
    }

    // ==================== DIALOGS ====================

    private Dialog<StorageItemLocationViewModel> createItemDialog(StorageItemLocationViewModel existing) {
        Dialog<StorageItemLocationViewModel> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Item to Location" : "Edit Item");

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
                StorageItemLocationViewModel vm = new StorageItemLocationViewModel();
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

    private Dialog<Long> createTransferDialog(StorageItemLocationViewModel item) {
        Dialog<Long> dialog = new Dialog<>();
        dialog.setTitle("Transfer Item");
        dialog.setHeaderText("Transfer '" + item.getProductName() + "' to another location");

        ButtonType transferBtn = new ButtonType("Transfer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(transferBtn, ButtonType.CANCEL);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        Label currentLabel = new Label("From: " + item.getLocationName());
        currentLabel.setStyle("-fx-font-weight: bold;");

        ComboBox<StorageLocation> locationCombo = new ComboBox<>();
        List<StorageLocation> locations = new ArrayList<>(locationService.getAllActiveLocations());
        locations.removeIf(loc -> loc.getId().equals(item.getLocationId())); // Remove current location
        locationCombo.getItems().addAll(locations);
        locationCombo.setPromptText("Select destination location...");
        locationCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(StorageLocation loc, boolean empty) {
                super.updateItem(loc, empty);
                setText(empty || loc == null ? null : loc.getName() + " (" + loc.getCity() + ")");
            }
        });
        locationCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(StorageLocation loc, boolean empty) {
                super.updateItem(loc, empty);
                setText(empty || loc == null ? null : loc.getName());
            }
        });

        content.getChildren().addAll(currentLabel, new Label("To:"), locationCombo);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(btn -> {
            if (btn == transferBtn && locationCombo.getValue() != null) {
                return locationCombo.getValue().getId();
            }
            return null;
        });

        return dialog;
    }

    // ==================== LOCATION MANAGEMENT ====================

    private void handleAddLocation() {
        Dialog<StorageLocation> dialog = createLocationDialog(null);
        Optional<StorageLocation> result = dialog.showAndWait();

        result.ifPresent(location -> {
            Task<StorageLocation> saveTask = new Task<>() {
                @Override
                protected StorageLocation call() {
                    location.setCreatedBy(currentUser != null ? currentUser.getUsername() : "system");
                    return locationService.createLocation(location);
                }
            };

            saveTask.setOnSucceeded(e -> {
                Platform.runLater(() -> {
                    showSuccess("✓ Location '" + location.getName() + "' created successfully");
                    loadCardsData();
                });
            });

            saveTask.setOnFailed(e -> showError("Failed to create location: " + saveTask.getException().getMessage()));
            new Thread(saveTask).start();
        });
    }

    private void handleEditLocation(LocationSummary locationSummary) {
        // Load the full location entity
        Task<Optional<StorageLocation>> loadTask = new Task<>() {
            @Override
            protected Optional<StorageLocation> call() {
                return locationService.findById(locationSummary.getLocationId());
            }
        };

        loadTask.setOnSucceeded(e -> {
            Optional<StorageLocation> locationOpt = loadTask.getValue();
            if (locationOpt.isPresent()) {
                Platform.runLater(() -> {
                    Dialog<StorageLocation> dialog = createLocationDialog(locationOpt.get());
                    Optional<StorageLocation> result = dialog.showAndWait();

                    result.ifPresent(updated -> {
                        Task<StorageLocation> saveTask = new Task<>() {
                            @Override
                            protected StorageLocation call() {
                                return locationService.updateLocation(updated.getId(), updated);
                            }
                        };

                        saveTask.setOnSucceeded(ev -> {
                            Platform.runLater(() -> {
                                showSuccess("✓ Location updated successfully");
                                loadCardsData();
                            });
                        });

                        saveTask.setOnFailed(ev -> showError("Failed to update location: " + saveTask.getException().getMessage()));
                        new Thread(saveTask).start();
                    });
                });
            } else {
                showError("Location not found");
            }
        });

        loadTask.setOnFailed(e -> showError("Failed to load location: " + loadTask.getException().getMessage()));
        new Thread(loadTask).start();
    }

    private void handleDeleteLocation(LocationSummary location) {
        // Check if location has items
        if (location.getItemCount() > 0) {
            Alert warning = new Alert(Alert.AlertType.WARNING);
            warning.setTitle("Cannot Delete");
            warning.setHeaderText("Location has items");
            warning.setContentText("This location has " + location.getItemCount() + " items. " +
                "Please transfer or remove all items before deleting the location.");
            warning.showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Location");
        confirm.setHeaderText("Delete '" + location.getLocationName() + "'?");
        confirm.setContentText("This action cannot be undone.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Task<Void> deleteTask = new Task<>() {
                @Override
                protected Void call() {
                    // Use hard delete to actually remove from database
                    locationService.hardDeleteLocation(location.getLocationId());
                    return null;
                }
            };

            deleteTask.setOnSucceeded(e -> {
                Platform.runLater(() -> {
                    showSuccess("✓ Location permanently deleted from database");
                    loadCardsData();
                });
            });

            deleteTask.setOnFailed(e -> showError("Failed to delete location: " + deleteTask.getException().getMessage()));
            new Thread(deleteTask).start();
        }
    }

    private Dialog<StorageLocation> createLocationDialog(StorageLocation existing) {
        Dialog<StorageLocation> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add New Location" : "Edit Location");

        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField(existing != null ? existing.getName() : "");
        nameField.setPromptText("Location Name (Required)");
        nameField.setPrefWidth(250);

        TextField codeField = new TextField(existing != null ? existing.getCode() : "");
        codeField.setPromptText("Code (e.g., AMM-01)");

        TextField cityField = new TextField(existing != null ? existing.getCity() : "");
        cityField.setPromptText("City");

        TextField addressField = new TextField(existing != null ? existing.getAddress() : "");
        addressField.setPromptText("Full Address");

        // Color picker as ComboBox
        ComboBox<String> colorCombo = new ComboBox<>();
        colorCombo.getItems().addAll(
            "#3b82f6", // Blue
            "#22c55e", // Green
            "#ef4444", // Red
            "#f59e0b", // Orange
            "#8b5cf6", // Purple
            "#06b6d4", // Cyan
            "#ec4899", // Pink
            "#6366f1"  // Indigo
        );
        colorCombo.setValue(existing != null && existing.getColor() != null ? existing.getColor() : "#3b82f6");
        colorCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String color, boolean empty) {
                super.updateItem(color, empty);
                if (empty || color == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Region colorBox = new Region();
                    colorBox.setPrefSize(20, 20);
                    colorBox.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 4;");
                    setGraphic(colorBox);
                    setText(getColorName(color));
                }
            }
        });
        colorCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String color, boolean empty) {
                super.updateItem(color, empty);
                if (empty || color == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Region colorBox = new Region();
                    colorBox.setPrefSize(20, 20);
                    colorBox.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 4;");
                    setGraphic(colorBox);
                    setText(getColorName(color));
                }
            }
        });

        // Icon picker
        ComboBox<String> iconCombo = new ComboBox<>();
        iconCombo.getItems().addAll("📦", "🏭", "🏢", "🏪", "🚚", "✈️", "🚢", "🏠");
        iconCombo.setValue(existing != null && existing.getIcon() != null ? existing.getIcon() : "📦");
        iconCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String icon, boolean empty) {
                super.updateItem(icon, empty);
                if (empty || icon == null) {
                    setText(null);
                } else {
                    setText(icon);
                    setStyle("-fx-font-size: 18px;");
                }
            }
        });

        TextField managerField = new TextField(existing != null ? existing.getManagerName() : "");
        managerField.setPromptText("Manager Name");

        TextField phoneField = new TextField(existing != null ? existing.getPhone() : "");
        phoneField.setPromptText("Phone Number");

        TextField emailField = new TextField(existing != null ? existing.getEmail() : "");
        emailField.setPromptText("Email Address");

        grid.add(new Label("Name:*"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Code:"), 0, 1);
        grid.add(codeField, 1, 1);
        grid.add(new Label("City:"), 0, 2);
        grid.add(cityField, 1, 2);
        grid.add(new Label("Address:"), 0, 3);
        grid.add(addressField, 1, 3);
        grid.add(new Label("Color:"), 0, 4);
        grid.add(colorCombo, 1, 4);
        grid.add(new Label("Icon:"), 0, 5);
        grid.add(iconCombo, 1, 5);
        grid.add(new Label("Manager:"), 0, 6);
        grid.add(managerField, 1, 6);
        grid.add(new Label("Phone:"), 0, 7);
        grid.add(phoneField, 1, 7);
        grid.add(new Label("Email:"), 0, 8);
        grid.add(emailField, 1, 8);

        dialog.getDialogPane().setContent(grid);

        // Disable save button if name is empty
        dialog.getDialogPane().lookupButton(saveBtn).setDisable(nameField.getText().trim().isEmpty());
        nameField.textProperty().addListener((obs, old, newVal) -> {
            dialog.getDialogPane().lookupButton(saveBtn).setDisable(newVal.trim().isEmpty());
        });

        dialog.setResultConverter(btn -> {
            if (btn != null && btn.getButtonData() == ButtonBar.ButtonData.OK_DONE && !nameField.getText().trim().isEmpty()) {
                // Create a new location object to avoid JPA issues with modified detached entities
                StorageLocation location = new StorageLocation();
                // Copy ID from existing if editing
                if (existing != null) {
                    location.setId(existing.getId());
                    location.setDateAdded(existing.getDateAdded());
                    location.setCreatedBy(existing.getCreatedBy());
                    location.setMapX(existing.getMapX());
                    location.setMapY(existing.getMapY());
                    location.setDisplayOrder(existing.getDisplayOrder());
                }
                location.setName(nameField.getText().trim());
                location.setCode(codeField.getText().trim().isEmpty() ? null : codeField.getText().trim());
                location.setCity(cityField.getText().trim().isEmpty() ? null : cityField.getText().trim());
                location.setAddress(addressField.getText().trim().isEmpty() ? null : addressField.getText().trim());
                location.setColor(colorCombo.getValue());
                location.setIcon(iconCombo.getValue());
                location.setManagerName(managerField.getText().trim().isEmpty() ? null : managerField.getText().trim());
                location.setPhone(phoneField.getText().trim().isEmpty() ? null : phoneField.getText().trim());
                location.setEmail(emailField.getText().trim().isEmpty() ? null : emailField.getText().trim());
                // Explicitly set active to true to ensure visibility
                location.setActive(true);
                return location;
            }
            return null;
        });

        return dialog;
    }

    private String getColorName(String hex) {
        return switch (hex) {
            case "#3b82f6" -> "Blue";
            case "#22c55e" -> "Green";
            case "#ef4444" -> "Red";
            case "#f59e0b" -> "Orange";
            case "#8b5cf6" -> "Purple";
            case "#06b6d4" -> "Cyan";
            case "#ec4899" -> "Pink";
            case "#6366f1" -> "Indigo";
            default -> "Custom";
        };
    }

    // ==================== HELPERS ====================

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

    private void showLoading(boolean show) {
        Platform.runLater(() -> {
            if (loadingIndicator != null) {
                loadingIndicator.setVisible(show);
            }
            if (itemTable != null) {
                itemTable.setDisable(show);
            }
        });
    }

    // ==================== LIFECYCLE ====================

    private void handleBack() {
        if (currentViewMode != ViewMode.CARDS) {
            // Go back to cards view
            navigateToView(ViewMode.CARDS);
        } else {
            // Go back to dashboard
            if (backgroundPane != null) {
                backgroundPane.stopAnimation();
            }
            if (locationCards != null) {
                locationCards.cleanup();
            }
            SceneManager.getInstance().showMainDashboard();
        }
    }

    public void immediateCleanup() {
        if (backgroundPane != null) {
            backgroundPane.stopAnimation();
            backgroundPane = null;
        }
        if (locationCards != null) {
            locationCards.cleanup();
            locationCards = null;
        }
        System.out.println("✓ Storage controller cleaned up");
    }

    @Override
    public void refresh() {
        switch (currentViewMode) {
            case CARDS:
                loadCardsData();
                break;
            case LOCATION_SHEET:
                selectionMap.clear();
                loadLocationSheetData();
                break;
            case TOTAL_SHEET:
                selectionMap.clear();
                loadTotalSheetData();
                break;
        }
    }
}
