package com.magictech.modules.storage.ui;

import com.magictech.core.auth.User;
import com.magictech.modules.storage.entity.AvailabilityRequest;
import com.magictech.modules.storage.entity.StorageItem;
import com.magictech.modules.storage.service.AvailabilityRequestService;
import com.magictech.modules.storage.service.StorageService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Fast Selection Panel - Advanced hierarchical filtering UI for item selection.
 * Used by Sales and Presales modules for quick product catalog browsing.
 *
 * Features:
 * - ARM-style cascading filters (System Type -> Manufacturer -> Product)
 * - Clean, elegant UI design
 * - No quantity displayed (availability via request only)
 * - Request availability functionality with email integration
 */
public class FastSelectionPanel extends VBox {

    // Services (must be set externally)
    private StorageService storageService;
    private AvailabilityRequestService availabilityRequestService;

    // Current user and module context
    private User currentUser;
    private String moduleName; // "SALES" or "PRESALES"

    // UI Components - Filters
    private ComboBox<String> systemTypeCombo;
    private ComboBox<String> manufacturerCombo;
    private ComboBox<String> productTypeCombo;
    private TextField searchField;
    private Button clearFiltersButton;

    // UI Components - Table
    private TableView<StorageItem> itemsTable;
    private ObservableList<StorageItem> allItems;
    private FilteredList<StorageItem> filteredItems;

    // UI Components - Actions
    private Button requestAvailabilityButton;
    private Button addToSelectionButton;
    private Label selectedCountLabel;
    private Label resultCountLabel;

    // Selection tracking
    private Set<StorageItem> selectedItems = new HashSet<>();

    // Callbacks
    private Consumer<StorageItem> onItemSelected;
    private Consumer<List<StorageItem>> onAddToSelection;
    private Consumer<AvailabilityRequest> onRequestCreated;

    // Colors
    private static final String PRIMARY_COLOR = "#8b5cf6";
    private static final String ACCENT_COLOR = "#06b6d4";
    private static final String SUCCESS_COLOR = "#22c55e";
    private static final String WARNING_COLOR = "#f59e0b";

    public FastSelectionPanel() {
        setSpacing(20);
        setPadding(new Insets(25));
        setStyle("-fx-background-color: transparent;");
        buildUI();
    }

    /**
     * Initialize with required services
     */
    public void initialize(StorageService storageService,
                          AvailabilityRequestService availabilityRequestService,
                          User currentUser,
                          String moduleName) {
        this.storageService = storageService;
        this.availabilityRequestService = availabilityRequestService;
        this.currentUser = currentUser;
        this.moduleName = moduleName;
        loadData();
    }

    private void buildUI() {
        // Header
        VBox header = createHeader();

        // Filter Section (ARM-style cascading)
        VBox filterSection = createFilterSection();

        // Results Table
        VBox tableSection = createTableSection();
        VBox.setVgrow(tableSection, Priority.ALWAYS);

        // Action Bar
        HBox actionBar = createActionBar();

        getChildren().addAll(header, filterSection, tableSection, actionBar);
    }

    private VBox createHeader() {
        VBox header = new VBox(8);

        Label titleLabel = new Label("🎯 Fast Selection");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label subtitleLabel = new Label("Select products using hierarchical filters. Request availability from Storage team.");
        subtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255, 255, 255, 0.7);");

        header.getChildren().addAll(titleLabel, subtitleLabel);
        return header;
    }

    private VBox createFilterSection() {
        VBox filterSection = new VBox(15);
        filterSection.setPadding(new Insets(20));
        filterSection.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.7);" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: rgba(139, 92, 246, 0.3);" +
                "-fx-border-radius: 12;" +
                "-fx-border-width: 1;"
        );

        // Filter header
        HBox filterHeader = new HBox(15);
        filterHeader.setAlignment(Pos.CENTER_LEFT);

        Label filterIcon = new Label("🔍");
        filterIcon.setFont(new Font(18));

        Label filterTitle = new Label("Filter Products");
        filterTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        clearFiltersButton = createStyledButton("↻ Clear All", "#6b7280", "#4b5563");
        clearFiltersButton.setOnAction(e -> clearAllFilters());

        filterHeader.getChildren().addAll(filterIcon, filterTitle, spacer, clearFiltersButton);

        // Cascading Filter Grid - ARM Style
        GridPane filterGrid = new GridPane();
        filterGrid.setHgap(20);
        filterGrid.setVgap(15);

        // Row 0: System Type -> Manufacturer
        Label systemLabel = createFilterLabel("System Type");
        systemTypeCombo = createFilterCombo("All System Types");
        systemTypeCombo.setOnAction(e -> onSystemTypeChanged());

        Label mfgLabel = createFilterLabel("Manufacturer");
        manufacturerCombo = createFilterCombo("All Manufacturers");
        manufacturerCombo.setOnAction(e -> onManufacturerChanged());

        filterGrid.add(systemLabel, 0, 0);
        filterGrid.add(systemTypeCombo, 0, 1);
        filterGrid.add(mfgLabel, 1, 0);
        filterGrid.add(manufacturerCombo, 1, 1);

        // Row 1: Product Type -> Search
        Label productLabel = createFilterLabel("Product Type");
        productTypeCombo = createFilterCombo("All Products");
        productTypeCombo.setOnAction(e -> applyFilters());

        Label searchLabel = createFilterLabel("Search");
        searchField = new TextField();
        searchField.setPromptText("Search by name, code, serial...");
        searchField.setPrefWidth(250);
        searchField.setStyle(
                "-fx-background-color: rgba(15, 23, 42, 0.8);" +
                "-fx-text-fill: white;" +
                "-fx-prompt-text-fill: rgba(255, 255, 255, 0.5);" +
                "-fx-background-radius: 8;" +
                "-fx-border-color: rgba(139, 92, 246, 0.4);" +
                "-fx-border-radius: 8;" +
                "-fx-padding: 10 15;"
        );
        searchField.textProperty().addListener((obs, old, newVal) -> applyFilters());

        filterGrid.add(productLabel, 2, 0);
        filterGrid.add(productTypeCombo, 2, 1);
        filterGrid.add(searchLabel, 3, 0);
        filterGrid.add(searchField, 3, 1);

        // Set column constraints for even distribution
        for (int i = 0; i < 4; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setHgrow(Priority.ALWAYS);
            col.setPercentWidth(25);
            filterGrid.getColumnConstraints().add(col);
        }

        filterSection.getChildren().addAll(filterHeader, filterGrid);
        return filterSection;
    }

    private Label createFilterLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.8); -fx-font-size: 12px; -fx-font-weight: bold;");
        return label;
    }

    private ComboBox<String> createFilterCombo(String placeholder) {
        ComboBox<String> combo = new ComboBox<>();
        combo.setPromptText(placeholder);
        combo.setPrefWidth(200);
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.setStyle(
                "-fx-background-color: rgba(15, 23, 42, 0.8);" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 8;" +
                "-fx-border-color: rgba(139, 92, 246, 0.4);" +
                "-fx-border-radius: 8;"
        );
        return combo;
    }

    private VBox createTableSection() {
        VBox tableSection = new VBox(10);

        // Table header with result count
        HBox tableHeader = new HBox(15);
        tableHeader.setAlignment(Pos.CENTER_LEFT);

        Label tableTitle = new Label("📦 Available Products");
        tableTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

        resultCountLabel = new Label("0 items");
        resultCountLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.6); -fx-font-size: 13px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        selectedCountLabel = new Label("");
        selectedCountLabel.setStyle("-fx-text-fill: " + SUCCESS_COLOR + "; -fx-font-size: 13px; -fx-font-weight: bold;");

        tableHeader.getChildren().addAll(tableTitle, resultCountLabel, spacer, selectedCountLabel);

        // Table
        itemsTable = new TableView<>();
        itemsTable.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.6);" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: rgba(139, 92, 246, 0.3);" +
                "-fx-border-radius: 12;" +
                "-fx-border-width: 1;"
        );
        VBox.setVgrow(itemsTable, Priority.ALWAYS);
        itemsTable.setMinHeight(300);

        buildTableColumns();

        // Row selection handler
        itemsTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        itemsTable.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            updateSelectedItems();
        });

        tableSection.getChildren().addAll(tableHeader, itemsTable);
        return tableSection;
    }

    private void buildTableColumns() {
        // System Type Column
        TableColumn<StorageItem, String> systemCol = new TableColumn<>("System Type");
        systemCol.setPrefWidth(120);
        systemCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getSystemType() != null ? data.getValue().getSystemType() : "-"));
        styleColumn(systemCol);

        // Manufacturer Column
        TableColumn<StorageItem, String> mfgCol = new TableColumn<>("Manufacturer");
        mfgCol.setPrefWidth(130);
        mfgCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getManufacture() != null ? data.getValue().getManufacture() : "-"));
        styleColumn(mfgCol);

        // Product Name Column
        TableColumn<StorageItem, String> nameCol = new TableColumn<>("Product Name");
        nameCol.setPrefWidth(200);
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProductName()));
        styleColumn(nameCol);

        // Code Column
        TableColumn<StorageItem, String> codeCol = new TableColumn<>("Code");
        codeCol.setPrefWidth(100);
        codeCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getCode() != null ? data.getValue().getCode() : "-"));
        styleColumn(codeCol);

        // Serial Number Column
        TableColumn<StorageItem, String> serialCol = new TableColumn<>("Serial Number");
        serialCol.setPrefWidth(120);
        serialCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getSerialNumber() != null ? data.getValue().getSerialNumber() : "-"));
        styleColumn(serialCol);

        // Price Column
        TableColumn<StorageItem, String> priceCol = new TableColumn<>("Price");
        priceCol.setPrefWidth(100);
        priceCol.setCellValueFactory(data -> {
            BigDecimal price = data.getValue().getPrice();
            return new SimpleStringProperty(price != null ? String.format("$%.2f", price) : "-");
        });
        priceCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        styleColumn(priceCol);

        // Note: NO QUANTITY COLUMN - as per requirements

        itemsTable.getColumns().addAll(systemCol, mfgCol, nameCol, codeCol, serialCol, priceCol);

        // Row factory for styling
        itemsTable.setRowFactory(tv -> {
            TableRow<StorageItem> row = new TableRow<>() {
                @Override
                protected void updateItem(StorageItem item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setStyle("");
                    } else {
                        int index = getIndex();
                        if (isSelected()) {
                            setStyle("-fx-background-color: rgba(139, 92, 246, 0.3);");
                        } else if (index % 2 == 0) {
                            setStyle("-fx-background-color: rgba(30, 41, 59, 0.4);");
                        } else {
                            setStyle("-fx-background-color: rgba(15, 23, 42, 0.4);");
                        }
                    }
                }
            };

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    handleItemDoubleClick(row.getItem());
                }
            });

            return row;
        });
    }

    private void styleColumn(TableColumn<StorageItem, String> column) {
        column.setStyle("-fx-alignment: CENTER-LEFT; -fx-text-fill: white;");
    }

    private HBox createActionBar() {
        HBox actionBar = new HBox(15);
        actionBar.setAlignment(Pos.CENTER_RIGHT);
        actionBar.setPadding(new Insets(15, 0, 0, 0));

        // Request Availability Button (Primary action)
        requestAvailabilityButton = createStyledButton("📧 Request Availability", PRIMARY_COLOR, "#7c3aed");
        requestAvailabilityButton.setDisable(true);
        requestAvailabilityButton.setOnAction(e -> handleRequestAvailability());

        // Add to Selection Button (Secondary action)
        addToSelectionButton = createStyledButton("➕ Add to Selection", SUCCESS_COLOR, "#16a34a");
        addToSelectionButton.setDisable(true);
        addToSelectionButton.setOnAction(e -> handleAddToSelection());

        actionBar.getChildren().addAll(requestAvailabilityButton, addToSelectionButton);
        return actionBar;
    }

    private Button createStyledButton(String text, String bgColor, String hoverColor) {
        Button button = new Button(text);
        button.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 12 24;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.2), 5, 0, 0, 2);"
        );

        button.setOnMouseEntered(e -> button.setStyle(
                "-fx-background-color: " + hoverColor + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 12 24;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 8, 0, 0, 3);"
        ));

        button.setOnMouseExited(e -> button.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 12 24;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.2), 5, 0, 0, 2);"
        ));

        return button;
    }

    // ==================== DATA LOADING ====================

    public void loadData() {
        if (storageService == null) {
            System.err.println("StorageService not initialized");
            return;
        }

        Task<List<StorageItem>> loadTask = new Task<>() {
            @Override
            protected List<StorageItem> call() {
                return storageService.findAllActive();
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<StorageItem> items = loadTask.getValue();
            Platform.runLater(() -> {
                allItems = FXCollections.observableArrayList(items);
                filteredItems = new FilteredList<>(allItems, p -> true);
                itemsTable.setItems(filteredItems);

                populateFilterCombos();
                updateResultCount();

                System.out.println("✓ FastSelectionPanel loaded " + items.size() + " items");
            });
        });

        loadTask.setOnFailed(e -> {
            System.err.println("Failed to load items: " + loadTask.getException().getMessage());
        });

        new Thread(loadTask).start();
    }

    private void populateFilterCombos() {
        // Populate System Type combo
        Set<String> systemTypes = allItems.stream()
                .map(StorageItem::getSystemType)
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.toCollection(TreeSet::new));
        systemTypeCombo.getItems().clear();
        systemTypeCombo.getItems().add("All System Types");
        systemTypeCombo.getItems().addAll(systemTypes);
        systemTypeCombo.setValue("All System Types");

        // Populate Manufacturer combo
        Set<String> manufacturers = allItems.stream()
                .map(StorageItem::getManufacture)
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.toCollection(TreeSet::new));
        manufacturerCombo.getItems().clear();
        manufacturerCombo.getItems().add("All Manufacturers");
        manufacturerCombo.getItems().addAll(manufacturers);
        manufacturerCombo.setValue("All Manufacturers");

        // Populate Product Type combo (based on product name patterns)
        Set<String> productTypes = allItems.stream()
                .map(StorageItem::getProductName)
                .filter(s -> s != null && !s.isEmpty())
                .map(this::extractProductType)
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.toCollection(TreeSet::new));
        productTypeCombo.getItems().clear();
        productTypeCombo.getItems().add("All Products");
        productTypeCombo.getItems().addAll(productTypes);
        productTypeCombo.setValue("All Products");
    }

    private String extractProductType(String productName) {
        // Extract main product type from name (e.g., "IP CAM 8MP" -> "IP CAM")
        if (productName == null) return null;
        String[] parts = productName.split("\\s+");
        if (parts.length >= 2) {
            return parts[0] + " " + parts[1];
        }
        return productName;
    }

    // ==================== FILTER HANDLERS ====================

    private void onSystemTypeChanged() {
        String selectedSystem = systemTypeCombo.getValue();

        // Update manufacturer combo based on selected system type
        Set<String> filteredManufacturers = allItems.stream()
                .filter(item -> selectedSystem == null ||
                        selectedSystem.equals("All System Types") ||
                        selectedSystem.equals(item.getSystemType()))
                .map(StorageItem::getManufacture)
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.toCollection(TreeSet::new));

        String currentMfg = manufacturerCombo.getValue();
        manufacturerCombo.getItems().clear();
        manufacturerCombo.getItems().add("All Manufacturers");
        manufacturerCombo.getItems().addAll(filteredManufacturers);

        if (filteredManufacturers.contains(currentMfg)) {
            manufacturerCombo.setValue(currentMfg);
        } else {
            manufacturerCombo.setValue("All Manufacturers");
        }

        applyFilters();
    }

    private void onManufacturerChanged() {
        String selectedSystem = systemTypeCombo.getValue();
        String selectedMfg = manufacturerCombo.getValue();

        // Update product type combo based on selected filters
        Set<String> filteredProducts = allItems.stream()
                .filter(item -> selectedSystem == null ||
                        selectedSystem.equals("All System Types") ||
                        selectedSystem.equals(item.getSystemType()))
                .filter(item -> selectedMfg == null ||
                        selectedMfg.equals("All Manufacturers") ||
                        selectedMfg.equals(item.getManufacture()))
                .map(StorageItem::getProductName)
                .filter(s -> s != null && !s.isEmpty())
                .map(this::extractProductType)
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.toCollection(TreeSet::new));

        String currentProduct = productTypeCombo.getValue();
        productTypeCombo.getItems().clear();
        productTypeCombo.getItems().add("All Products");
        productTypeCombo.getItems().addAll(filteredProducts);

        if (filteredProducts.contains(currentProduct)) {
            productTypeCombo.setValue(currentProduct);
        } else {
            productTypeCombo.setValue("All Products");
        }

        applyFilters();
    }

    private void applyFilters() {
        if (filteredItems == null) return;

        String systemType = systemTypeCombo.getValue();
        String manufacturer = manufacturerCombo.getValue();
        String productType = productTypeCombo.getValue();
        String searchText = searchField.getText();

        filteredItems.setPredicate(item -> {
            // System Type filter
            if (systemType != null && !systemType.equals("All System Types")) {
                if (!systemType.equals(item.getSystemType())) {
                    return false;
                }
            }

            // Manufacturer filter
            if (manufacturer != null && !manufacturer.equals("All Manufacturers")) {
                if (!manufacturer.equals(item.getManufacture())) {
                    return false;
                }
            }

            // Product Type filter
            if (productType != null && !productType.equals("All Products")) {
                String itemProductType = extractProductType(item.getProductName());
                if (!productType.equals(itemProductType)) {
                    return false;
                }
            }

            // Search filter
            if (searchText != null && !searchText.isEmpty()) {
                String lower = searchText.toLowerCase();
                return (item.getProductName() != null && item.getProductName().toLowerCase().contains(lower)) ||
                       (item.getManufacture() != null && item.getManufacture().toLowerCase().contains(lower)) ||
                       (item.getCode() != null && item.getCode().toLowerCase().contains(lower)) ||
                       (item.getSerialNumber() != null && item.getSerialNumber().toLowerCase().contains(lower)) ||
                       (item.getSystemType() != null && item.getSystemType().toLowerCase().contains(lower));
            }

            return true;
        });

        updateResultCount();
    }

    private void clearAllFilters() {
        systemTypeCombo.setValue("All System Types");
        manufacturerCombo.setValue("All Manufacturers");
        productTypeCombo.setValue("All Products");
        searchField.clear();
        populateFilterCombos();
        applyFilters();
    }

    private void updateResultCount() {
        if (filteredItems != null) {
            resultCountLabel.setText(filteredItems.size() + " items");
        }
    }

    private void updateSelectedItems() {
        selectedItems.clear();
        selectedItems.addAll(itemsTable.getSelectionModel().getSelectedItems());

        int count = selectedItems.size();
        if (count > 0) {
            selectedCountLabel.setText("✓ " + count + " selected");
            requestAvailabilityButton.setDisable(false);
            addToSelectionButton.setDisable(false);
        } else {
            selectedCountLabel.setText("");
            requestAvailabilityButton.setDisable(true);
            addToSelectionButton.setDisable(true);
        }
    }

    // ==================== ACTION HANDLERS ====================

    private void handleItemDoubleClick(StorageItem item) {
        if (onItemSelected != null) {
            onItemSelected.accept(item);
        }
    }

    private void handleRequestAvailability() {
        if (selectedItems.isEmpty()) {
            showWarning("Please select at least one item");
            return;
        }

        // Show request dialog
        Dialog<AvailabilityRequestData> dialog = createRequestDialog();
        Optional<AvailabilityRequestData> result = dialog.showAndWait();

        result.ifPresent(data -> {
            for (StorageItem item : selectedItems) {
                try {
                    AvailabilityRequest request = availabilityRequestService.createSimpleRequest(
                            item,
                            data.quantity,
                            data.reason,
                            currentUser,
                            moduleName
                    );

                    if (onRequestCreated != null) {
                        onRequestCreated.accept(request);
                    }
                } catch (Exception ex) {
                    showError("Failed to create request for " + item.getProductName() + ": " + ex.getMessage());
                }
            }

            showSuccess("✓ Availability request(s) sent to Storage team!");
            itemsTable.getSelectionModel().clearSelection();
        });
    }

    private void handleAddToSelection() {
        if (selectedItems.isEmpty()) {
            showWarning("Please select at least one item");
            return;
        }

        if (onAddToSelection != null) {
            onAddToSelection.accept(new ArrayList<>(selectedItems));
        }

        showSuccess("✓ " + selectedItems.size() + " item(s) added to selection");
        itemsTable.getSelectionModel().clearSelection();
    }

    private Dialog<AvailabilityRequestData> createRequestDialog() {
        Dialog<AvailabilityRequestData> dialog = new Dialog<>();
        dialog.setTitle("Request Availability");
        dialog.setHeaderText("Request availability from Storage team");

        ButtonType sendButton = new ButtonType("📧 Send Request", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(sendButton, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        // Items summary
        Label itemsLabel = new Label("Items: " + selectedItems.size() + " selected");
        itemsLabel.setStyle("-fx-font-weight: bold;");

        // Quantity field
        Spinner<Integer> quantitySpinner = new Spinner<>(1, 9999, 1);
        quantitySpinner.setEditable(true);
        quantitySpinner.setPrefWidth(120);

        // Reason field
        TextArea reasonArea = new TextArea();
        reasonArea.setPromptText("Why do you need this item? (Optional)");
        reasonArea.setPrefRowCount(3);
        reasonArea.setPrefWidth(300);

        grid.add(itemsLabel, 0, 0, 2, 1);
        grid.add(new Label("Quantity Needed:"), 0, 1);
        grid.add(quantitySpinner, 1, 1);
        grid.add(new Label("Reason:"), 0, 2);
        grid.add(reasonArea, 0, 3, 2, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == sendButton) {
                return new AvailabilityRequestData(
                        quantitySpinner.getValue(),
                        reasonArea.getText()
                );
            }
            return null;
        });

        return dialog;
    }

    private static class AvailabilityRequestData {
        int quantity;
        String reason;

        AvailabilityRequestData(int quantity, String reason) {
            this.quantity = quantity;
            this.reason = reason;
        }
    }

    // ==================== CALLBACKS ====================

    public void setOnItemSelected(Consumer<StorageItem> callback) {
        this.onItemSelected = callback;
    }

    public void setOnAddToSelection(Consumer<List<StorageItem>> callback) {
        this.onAddToSelection = callback;
    }

    public void setOnRequestCreated(Consumer<AvailabilityRequest> callback) {
        this.onRequestCreated = callback;
    }

    // ==================== ALERTS ====================

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
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

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ==================== REFRESH ====================

    public void refresh() {
        loadData();
    }
}
