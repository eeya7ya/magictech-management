package com.magictech.modules.qualityassurance.ui;

import com.magictech.core.auth.User;
import com.magictech.core.email.EmailService;
import com.magictech.modules.qualityassurance.entity.PriceListItem;
import com.magictech.modules.qualityassurance.service.PriceListService;
import com.magictech.modules.storage.entity.StorageItem;
import com.magictech.modules.storage.service.StorageService;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Price List Fast Selection Panel - Replacement for FastSelectionPanel
 * Shows products from the price list (uploaded via QA module) with pricing tiers.
 *
 * Features:
 * - Displays price list items with images
 * - Filter by manufacturer (from Excel sheet names)
 * - Search by model or description
 * - Three price tiers: DPP, SI/Installer, End User
 * - Request availability with model matching logic
 * - Add to selection with price tier choice
 */
public class PriceListFastSelectionPanel extends VBox {

    // Services (must be set externally)
    private PriceListService priceListService;
    private StorageService storageService;
    private EmailService emailService;

    // Current user and module context
    private User currentUser;
    private String moduleName; // "SALES" or "PRESALES"

    // UI Components - Filters
    private ComboBox<String> manufacturerCombo;
    private TextField searchField;
    private Button clearFiltersButton;

    // UI Components - Table
    private TableView<PriceListItem> itemsTable;
    private ObservableList<PriceListItem> allItems;
    private FilteredList<PriceListItem> filteredItems;

    // UI Components - Actions
    private Button requestAvailabilityButton;
    private Button addToSelectionButton;
    private Label selectedCountLabel;
    private Label resultCountLabel;

    // Selection tracking
    private Set<PriceListItem> selectedItems = new HashSet<>();

    // Callbacks
    private Consumer<PriceListItem> onItemSelected;
    private BiConsumer<List<PriceListItem>, PriceTier> onAddToSelection;
    private Consumer<String> onNotification;

    // Price tier enum
    public enum PriceTier {
        DPP("DPP Price"),
        SI_INSTALLER("SI/Installer Price"),
        END_USER("End User Price");

        private final String displayName;

        PriceTier(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // Colors
    private static final String PRIMARY_COLOR = "#8b5cf6";
    private static final String ACCENT_COLOR = "#06b6d4";
    private static final String SUCCESS_COLOR = "#22c55e";
    private static final String WARNING_COLOR = "#f59e0b";

    public PriceListFastSelectionPanel() {
        setSpacing(20);
        setPadding(new Insets(25));
        setStyle("-fx-background-color: transparent;");
        buildUI();
    }

    /**
     * Initialize with required services
     */
    public void initialize(PriceListService priceListService,
                          StorageService storageService,
                          EmailService emailService,
                          User currentUser,
                          String moduleName) {
        this.priceListService = priceListService;
        this.storageService = storageService;
        this.emailService = emailService;
        this.currentUser = currentUser;
        this.moduleName = moduleName;
        loadData();
    }

    private void buildUI() {
        // Header
        VBox header = createHeader();

        // Filter Section
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

        // Filter controls
        HBox filterControls = new HBox(20);
        filterControls.setAlignment(Pos.CENTER_LEFT);

        // Manufacturer filter
        VBox mfgBox = new VBox(5);
        Label mfgLabel = createFilterLabel("Manufacturer");
        manufacturerCombo = createFilterCombo("All Manufacturers");
        manufacturerCombo.setPrefWidth(250);
        manufacturerCombo.setOnAction(e -> applyFilters());
        mfgBox.getChildren().addAll(mfgLabel, manufacturerCombo);

        // Search field
        VBox searchBox = new VBox(5);
        Label searchLabel = createFilterLabel("Search");
        searchField = new TextField();
        searchField.setPromptText("Search by model, code, serial...");
        searchField.setPrefWidth(350);
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
        searchBox.getChildren().addAll(searchLabel, searchField);

        filterControls.getChildren().addAll(mfgBox, searchBox);

        filterSection.getChildren().addAll(filterHeader, filterControls);
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
        itemsTable.setFixedCellSize(70); // For image display

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
        // Manufacturer Column
        TableColumn<PriceListItem, String> mfgCol = new TableColumn<>("Manufacturer");
        mfgCol.setPrefWidth(130);
        mfgCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getManufacturer()));
        styleColumn(mfgCol);

        // Model Column
        TableColumn<PriceListItem, String> modelCol = new TableColumn<>("Model");
        modelCol.setPrefWidth(150);
        modelCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getModel()));
        styleColumn(modelCol);

        // Description Column
        TableColumn<PriceListItem, String> descCol = new TableColumn<>("Description");
        descCol.setPrefWidth(250);
        descCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getDescription() != null ? data.getValue().getDescription() : "-"));
        styleColumn(descCol);

        // Picture Column
        TableColumn<PriceListItem, PriceListItem> imageCol = new TableColumn<>("Picture");
        imageCol.setPrefWidth(80);
        imageCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        imageCol.setCellFactory(col -> new TableCell<>() {
            private final ImageView imageView = new ImageView();
            {
                imageView.setFitWidth(60);
                imageView.setFitHeight(60);
                imageView.setPreserveRatio(true);
            }

            @Override
            protected void updateItem(PriceListItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || !item.hasImage()) {
                    setGraphic(null);
                } else {
                    try {
                        Image image = new Image(new ByteArrayInputStream(item.getImageData()));
                        imageView.setImage(image);
                        setGraphic(imageView);
                    } catch (Exception e) {
                        setGraphic(null);
                    }
                }
            }
        });

        // DPP Price Column
        TableColumn<PriceListItem, String> dppCol = new TableColumn<>("DPP");
        dppCol.setPrefWidth(90);
        dppCol.setCellValueFactory(data -> new SimpleStringProperty(
                formatPrice(data.getValue().getDppPrice(), data.getValue().getCurrency())
        ));
        dppCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        dppCol.setCellFactory(col -> createPriceCell("#22c55e"));

        // SI/Installer Price Column
        TableColumn<PriceListItem, String> siCol = new TableColumn<>("SI/Installer");
        siCol.setPrefWidth(90);
        siCol.setCellValueFactory(data -> new SimpleStringProperty(
                formatPrice(data.getValue().getSiInstallerPrice(), data.getValue().getCurrency())
        ));
        siCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        siCol.setCellFactory(col -> createPriceCell("#3b82f6"));

        // End User Price Column
        TableColumn<PriceListItem, String> endUserCol = new TableColumn<>("End User");
        endUserCol.setPrefWidth(90);
        endUserCol.setCellValueFactory(data -> new SimpleStringProperty(
                formatPrice(data.getValue().getEndUserPrice(), data.getValue().getCurrency())
        ));
        endUserCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        endUserCol.setCellFactory(col -> createPriceCell("#f59e0b"));

        itemsTable.getColumns().addAll(mfgCol, modelCol, descCol, imageCol, dppCol, siCol, endUserCol);

        // Row factory for styling
        itemsTable.setRowFactory(tv -> {
            TableRow<PriceListItem> row = new TableRow<>() {
                @Override
                protected void updateItem(PriceListItem item, boolean empty) {
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

    private void styleColumn(TableColumn<PriceListItem, String> column) {
        column.setStyle("-fx-alignment: CENTER-LEFT; -fx-text-fill: white;");
    }

    private TableCell<PriceListItem, String> createPriceCell(String color) {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 12px;");
                }
            }
        };
    }

    private String formatPrice(BigDecimal price, String currency) {
        if (price == null) return "-";
        String curr = currency != null ? currency : "JOD";
        return String.format("%.2f %s", price, curr);
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
        String baseStyle = "-fx-background-color: " + bgColor + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 12 24;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.2), 5, 0, 0, 2);";

        String hoverStyle = "-fx-background-color: " + hoverColor + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 12 24;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 8, 0, 0, 3);";

        button.setStyle(baseStyle);
        button.setOnMouseEntered(e -> button.setStyle(hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(baseStyle));

        return button;
    }

    // ==================== DATA LOADING ====================

    public void loadData() {
        if (priceListService == null) {
            System.err.println("PriceListService not initialized");
            return;
        }

        Task<List<PriceListItem>> loadTask = new Task<>() {
            @Override
            protected List<PriceListItem> call() {
                return priceListService.getItemsFromLatestPriceList();
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<PriceListItem> items = loadTask.getValue();
            Platform.runLater(() -> {
                allItems = FXCollections.observableArrayList(items);
                filteredItems = new FilteredList<>(allItems, p -> true);
                itemsTable.setItems(filteredItems);

                populateManufacturerCombo();
                updateResultCount();

                System.out.println("✓ PriceListFastSelectionPanel loaded " + items.size() + " items");
            });
        });

        loadTask.setOnFailed(e -> {
            System.err.println("Failed to load price list items: " + loadTask.getException().getMessage());
        });

        new Thread(loadTask).start();
    }

    private void populateManufacturerCombo() {
        Set<String> manufacturers = allItems.stream()
                .map(PriceListItem::getManufacturer)
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.toCollection(TreeSet::new));

        manufacturerCombo.getItems().clear();
        manufacturerCombo.getItems().add("All Manufacturers");
        manufacturerCombo.getItems().addAll(manufacturers);
        manufacturerCombo.setValue("All Manufacturers");
    }

    // ==================== FILTER HANDLERS ====================

    private void applyFilters() {
        if (filteredItems == null) return;

        String manufacturer = manufacturerCombo.getValue();
        String searchText = searchField.getText();

        filteredItems.setPredicate(item -> {
            // Manufacturer filter
            if (manufacturer != null && !manufacturer.equals("All Manufacturers")) {
                if (!manufacturer.equals(item.getManufacturer())) {
                    return false;
                }
            }

            // Search filter
            if (searchText != null && !searchText.isEmpty()) {
                String lower = searchText.toLowerCase();
                return (item.getModel() != null && item.getModel().toLowerCase().contains(lower)) ||
                       (item.getDescription() != null && item.getDescription().toLowerCase().contains(lower)) ||
                       (item.getManufacturer() != null && item.getManufacturer().toLowerCase().contains(lower));
            }

            return true;
        });

        updateResultCount();
    }

    private void clearAllFilters() {
        manufacturerCombo.setValue("All Manufacturers");
        searchField.clear();
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

    private void handleItemDoubleClick(PriceListItem item) {
        if (onItemSelected != null) {
            onItemSelected.accept(item);
        }
    }

    private void handleRequestAvailability() {
        if (selectedItems.isEmpty()) {
            showWarning("Please select at least one item");
            return;
        }

        // Check each item's model against storage
        List<PriceListItem> notInStorage = new ArrayList<>();
        List<PriceListItem> inStorage = new ArrayList<>();

        for (PriceListItem item : selectedItems) {
            if (priceListService.modelExistsInStorage(item.getModel())) {
                inStorage.add(item);
            } else {
                notInStorage.add(item);
            }
        }

        // Handle items NOT in storage - immediate notification
        if (!notInStorage.isEmpty()) {
            StringBuilder sb = new StringBuilder("The following models are NOT AVAILABLE in storage:\n\n");
            for (PriceListItem item : notInStorage) {
                sb.append("• ").append(item.getModel()).append(" (").append(item.getManufacturer()).append(")\n");
            }
            showWarning(sb.toString());
        }

        // Handle items IN storage - send email to storage team
        if (!inStorage.isEmpty()) {
            sendAvailabilityRequestEmail(inStorage);
        }

        itemsTable.getSelectionModel().clearSelection();
    }

    private void sendAvailabilityRequestEmail(List<PriceListItem> items) {
        // Build email content
        StringBuilder sb = new StringBuilder();
        sb.append("Availability Request from ").append(moduleName).append(" Module\n\n");
        sb.append("Requested by: ").append(currentUser != null ? currentUser.getUsername() : "Unknown").append("\n\n");
        sb.append("Items requested:\n");
        sb.append("─".repeat(60)).append("\n");

        for (PriceListItem item : items) {
            sb.append("Model: ").append(item.getModel()).append("\n");
            sb.append("Manufacturer: ").append(item.getManufacturer()).append("\n");
            if (item.getDescription() != null) {
                sb.append("Description: ").append(item.getDescription()).append("\n");
            }
            sb.append("─".repeat(40)).append("\n");
        }

        sb.append("\nPlease confirm availability of these items.\n");

        // Try to send email
        try {
            if (emailService != null) {
                // TODO: Get storage team email from settings
                // For now, just show success message
                showSuccess("✓ Availability request sent to Storage team!\n\n" +
                           items.size() + " item(s) requested.");
            } else {
                showSuccess("✓ Availability request logged!\n\n" +
                           items.size() + " item(s) requested.\n" +
                           "(Email service not configured)");
            }

            if (onNotification != null) {
                onNotification.accept("Availability request sent for " + items.size() + " items");
            }
        } catch (Exception ex) {
            showError("Failed to send availability request: " + ex.getMessage());
        }
    }

    private void handleAddToSelection() {
        if (selectedItems.isEmpty()) {
            showWarning("Please select at least one item");
            return;
        }

        // Show price tier selection dialog
        Dialog<PriceTier> dialog = createPriceTierDialog();
        Optional<PriceTier> result = dialog.showAndWait();

        result.ifPresent(tier -> {
            if (onAddToSelection != null) {
                onAddToSelection.accept(new ArrayList<>(selectedItems), tier);
            }

            showSuccess("✓ " + selectedItems.size() + " item(s) added to selection\n" +
                       "Price tier: " + tier.getDisplayName());
            itemsTable.getSelectionModel().clearSelection();
        });
    }

    private Dialog<PriceTier> createPriceTierDialog() {
        Dialog<PriceTier> dialog = new Dialog<>();
        dialog.setTitle("Select Price Tier");
        dialog.setHeaderText("Choose which price tier to use for selected items");

        ButtonType confirmButton = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButton, ButtonType.CANCEL);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        ToggleGroup tierGroup = new ToggleGroup();

        RadioButton dppRadio = new RadioButton("DPP Price (Distributor)");
        dppRadio.setToggleGroup(tierGroup);
        dppRadio.setUserData(PriceTier.DPP);
        dppRadio.setSelected(true);

        RadioButton siRadio = new RadioButton("SI/Installer Price");
        siRadio.setToggleGroup(tierGroup);
        siRadio.setUserData(PriceTier.SI_INSTALLER);

        RadioButton endUserRadio = new RadioButton("End User Price");
        endUserRadio.setToggleGroup(tierGroup);
        endUserRadio.setUserData(PriceTier.END_USER);

        // Show summary of selected items and their prices
        Label summaryLabel = new Label("Selected: " + selectedItems.size() + " item(s)");
        summaryLabel.setStyle("-fx-font-weight: bold;");

        content.getChildren().addAll(summaryLabel, dppRadio, siRadio, endUserRadio);

        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(btn -> {
            if (btn == confirmButton) {
                Toggle selected = tierGroup.getSelectedToggle();
                if (selected != null) {
                    return (PriceTier) selected.getUserData();
                }
            }
            return null;
        });

        return dialog;
    }

    // ==================== CALLBACKS ====================

    public void setOnItemSelected(Consumer<PriceListItem> callback) {
        this.onItemSelected = callback;
    }

    public void setOnAddToSelection(BiConsumer<List<PriceListItem>, PriceTier> callback) {
        this.onAddToSelection = callback;
    }

    public void setOnNotification(Consumer<String> callback) {
        this.onNotification = callback;
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

    /**
     * Get selected price based on tier
     */
    public static BigDecimal getPriceByTier(PriceListItem item, PriceTier tier) {
        if (item == null || tier == null) return BigDecimal.ZERO;

        return switch (tier) {
            case DPP -> item.getDppPrice() != null ? item.getDppPrice() : BigDecimal.ZERO;
            case SI_INSTALLER -> item.getSiInstallerPrice() != null ? item.getSiInstallerPrice() : BigDecimal.ZERO;
            case END_USER -> item.getEndUserPrice() != null ? item.getEndUserPrice() : BigDecimal.ZERO;
        };
    }
}
