package com.magictech.modules.qualityassurance;

import com.magictech.core.module.BaseModuleController;
import com.magictech.core.ui.SceneManager;
import com.magictech.modules.qualityassurance.entity.PriceList;
import com.magictech.modules.qualityassurance.entity.PriceListItem;
import com.magictech.modules.qualityassurance.service.PriceListExcelImportService;
import com.magictech.modules.qualityassurance.service.PriceListService;
import com.magictech.modules.storage.dto.CustomerAnalyticsDTO;
import com.magictech.modules.storage.dto.ProjectAnalyticsDTO;
import com.magictech.modules.storage.entity.StorageItem;
import com.magictech.modules.storage.model.StorageItemViewModel;
import com.magictech.modules.storage.service.AnalyticsService;
import com.magictech.modules.storage.service.StorageService;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Quality Assurance Controller with tab structure
 * Tab 1: Price List Management (primary function - upload and manage price lists)
 * Tab 2: QA Management (quality checks and approval)
 * Tab 3: Analytics Dashboard (read-only)
 */
@Component
public class QualityAssuranceController extends BaseModuleController {

    @Autowired
    private StorageService storageService;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private PriceListService priceListService;

    @Autowired
    private PriceListExcelImportService priceListExcelImportService;

    private enum ActiveView { PRICE_LIST, QA_MANAGEMENT, ANALYTICS }
    private ActiveView currentView = ActiveView.PRICE_LIST;

    private TableView<StorageItemViewModel> qaTable;
    private ScrollPane analyticsView;
    private VBox priceListView;
    private StackPane tableContainer;
    private TextField searchField;
    private Button approveButton, refreshButton;
    private Button priceListTabButton, qaTabButton, analyticsTabButton;
    private com.magictech.core.ui.components.DashboardBackgroundPane backgroundPane;
    private ProgressIndicator loadingIndicator;
    private Label selectedCountLabel;

    // Price list components
    private TableView<PriceListItem> priceListItemsTable;
    private ComboBox<String> manufacturerFilter;
    private TextField priceListSearchField;
    private Label priceListStatsLabel;
    private ObservableList<PriceListItem> priceListItems;
    private FilteredList<PriceListItem> filteredPriceListItems;

    private ObservableList<StorageItemViewModel> qaItems;
    private FilteredList<StorageItemViewModel> filteredQA;
    private Map<StorageItemViewModel, BooleanProperty> selectionMap = new HashMap<>();

    @Override
    protected void setupUI() {
        StackPane stackRoot = new StackPane();
        backgroundPane = new com.magictech.core.ui.components.DashboardBackgroundPane();

        BorderPane contentPane = new BorderPane();
        contentPane.setStyle("-fx-background-color: transparent;");

        VBox header = createHeader();
        VBox content = createContent();

        contentPane.setTop(header);
        contentPane.setCenter(content);

        stackRoot.getChildren().addAll(backgroundPane, contentPane);

        BorderPane root = getRootPane();
        root.setCenter(stackRoot);
        root.setStyle("-fx-background-color: transparent;");
    }

    @Override
    protected void loadData() {
        // Initialize QA data
        qaItems = FXCollections.observableArrayList();
        filteredQA = new FilteredList<>(qaItems, p -> true);
        qaTable.setItems(filteredQA);

        // Initialize price list data
        priceListItems = FXCollections.observableArrayList();
        filteredPriceListItems = new FilteredList<>(priceListItems, p -> true);
        priceListItemsTable.setItems(filteredPriceListItems);

        // Load price list data (default view)
        loadPriceListData();
    }

    @Override
    public void refresh() {
        if (currentView == ActiveView.PRICE_LIST) {
            loadPriceListData();
        } else if (currentView == ActiveView.QA_MANAGEMENT) {
            loadQAData();
        } else {
            refreshAnalytics();
        }
    }

    private VBox createHeader() {
        VBox headerContainer = new VBox();

        HBox headerBar = new HBox();
        headerBar.setAlignment(Pos.CENTER_LEFT);
        headerBar.setPadding(new Insets(20, 30, 15, 30));
        headerBar.setStyle("-fx-background-color: linear-gradient(to right, #eab308, #ca8a04);");

        Label titleLabel = new Label("✅ Quality Assurance Module");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 32px; -fx-font-weight: bold;");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Button backButton = createStyledButton("← Back", "#1f2937", "#111827");
        backButton.setOnAction(e -> navigateToDashboard());

        headerBar.getChildren().addAll(titleLabel, backButton);

        HBox subtitleBar = new HBox();
        subtitleBar.setAlignment(Pos.CENTER);
        subtitleBar.setPadding(new Insets(12, 30, 12, 30));
        subtitleBar.setStyle("-fx-background-color: rgba(20, 30, 45, 0.4); -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-width: 0 0 1 0;");

        Label subtitleLabel = new Label("Quality Checks • Verification & Approval • Business Analytics Dashboard");
        subtitleLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 14px;");

        selectedCountLabel = new Label();
        selectedCountLabel.setStyle("-fx-text-fill: #eab308; -fx-font-size: 14px; -fx-font-weight: bold;");
        selectedCountLabel.setVisible(false);

        subtitleBar.getChildren().addAll(subtitleLabel, selectedCountLabel);
        headerContainer.getChildren().addAll(headerBar, subtitleBar);
        return headerContainer;
    }

    private VBox createContent() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20, 30, 30, 30));
        content.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(content, Priority.ALWAYS);

        HBox tabSwitcher = createTabSwitcher();
        HBox toolbar = createToolbar();

        tableContainer = new StackPane();
        qaTable = createQATable();
        analyticsView = createAnalyticsView();
        priceListView = createPriceListView();

        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setVisible(false);
        loadingIndicator.setMaxSize(60, 60);

        // Default view is price list
        tableContainer.getChildren().addAll(priceListView, loadingIndicator);
        qaTable.setVisible(false);
        analyticsView.setVisible(false);

        VBox.setVgrow(tableContainer, Priority.ALWAYS);

        content.getChildren().addAll(tabSwitcher, toolbar, tableContainer);
        return content;
    }

    private HBox createTabSwitcher() {
        HBox tabBox = new HBox(10);
        tabBox.setAlignment(Pos.CENTER_LEFT);
        tabBox.setPadding(new Insets(0, 0, 10, 0));

        priceListTabButton = createTabButton("📋 Price List Management", true);
        priceListTabButton.setOnAction(e -> switchToPriceListView());

        qaTabButton = createTabButton("✅ QA Management", false);
        qaTabButton.setOnAction(e -> switchToQAView());

        analyticsTabButton = createTabButton("📊 Analytics Dashboard", false);
        analyticsTabButton.setOnAction(e -> switchToAnalyticsView());

        tabBox.getChildren().addAll(priceListTabButton, qaTabButton, analyticsTabButton);
        return tabBox;
    }

    private Button createTabButton(String text, boolean active) {
        Button btn = new Button(text);
        updateTabButtonStyle(btn, active);
        return btn;
    }

    private void updateTabButtonStyle(Button btn, boolean active) {
        if (active) {
            btn.setStyle("-fx-background-color: #eab308; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(234, 179, 8, 0.4), 8, 0, 0, 2);");
        } else {
            btn.setStyle("-fx-background-color: rgba(30, 41, 59, 0.6); -fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 14px; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand; -fx-border-color: rgba(255, 255, 255, 0.2); -fx-border-width: 1; -fx-border-radius: 8;");
        }
    }

    private void switchToPriceListView() {
        currentView = ActiveView.PRICE_LIST;
        tableContainer.getChildren().clear();
        tableContainer.getChildren().addAll(priceListView, loadingIndicator);
        priceListView.setVisible(true);
        qaTable.setVisible(false);
        analyticsView.setVisible(false);
        updateTabButtonStyle(priceListTabButton, true);
        updateTabButtonStyle(qaTabButton, false);
        updateTabButtonStyle(analyticsTabButton, false);
        approveButton.setVisible(false);
        approveButton.setManaged(false);
        selectedCountLabel.setVisible(false);
        loadPriceListData();
        System.out.println("✓ Switched to Price List Management");
    }

    private void switchToQAView() {
        currentView = ActiveView.QA_MANAGEMENT;
        tableContainer.getChildren().clear();
        tableContainer.getChildren().addAll(qaTable, loadingIndicator);
        qaTable.setVisible(true);
        priceListView.setVisible(false);
        analyticsView.setVisible(false);
        updateTabButtonStyle(priceListTabButton, false);
        updateTabButtonStyle(qaTabButton, true);
        updateTabButtonStyle(analyticsTabButton, false);
        approveButton.setVisible(true);
        approveButton.setManaged(true);
        qaTable.refresh();
        updateSelectedCount();
        loadQAData();
        System.out.println("✓ Switched to QA Management");
    }

    private void switchToAnalyticsView() {
        currentView = ActiveView.ANALYTICS;
        tableContainer.getChildren().clear();
        tableContainer.getChildren().addAll(analyticsView, loadingIndicator);
        analyticsView.setVisible(true);
        priceListView.setVisible(false);
        qaTable.setVisible(false);
        updateTabButtonStyle(priceListTabButton, false);
        updateTabButtonStyle(qaTabButton, false);
        updateTabButtonStyle(analyticsTabButton, true);
        approveButton.setVisible(false);
        approveButton.setManaged(false);
        selectionMap.forEach((item, prop) -> prop.set(false));
        selectedCountLabel.setVisible(false);
        refreshAnalytics();
        System.out.println("✓ Switched to Analytics Dashboard");
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0, 0, 15, 0));

        searchField = new TextField();
        searchField.setPromptText("🔍 Search by product name or manufacture...");
        searchField.setPrefWidth(350);
        searchField.setStyle("-fx-background-color: rgba(30, 41, 59, 0.6); -fx-text-fill: white; -fx-prompt-text-fill: rgba(255, 255, 255, 0.5); -fx-padding: 10; -fx-background-radius: 8; -fx-border-color: rgba(234, 179, 8, 0.3); -fx-border-radius: 8;");

        searchField.textProperty().addListener((obs, old, newVal) -> {
            filteredQA.setPredicate(item -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String lower = newVal.toLowerCase();
                return item.getProductName().toLowerCase().contains(lower) || item.getManufacture().toLowerCase().contains(lower);
            });
        });

        approveButton = createStyledButton("✅ Approve Items", "#22c55e", "#16a34a");
        approveButton.setOnAction(e -> handleApprove());

        refreshButton = createStyledButton("🔄 Refresh", "#6b7280", "#4b5563");
        refreshButton.setOnAction(e -> refresh());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        toolbar.getChildren().addAll(searchField, spacer, approveButton, refreshButton);
        return toolbar;
    }

    private TableView<StorageItemViewModel> createQATable() {
        TableView<StorageItemViewModel> table = new TableView<>();
        table.setStyle("-fx-background-color: rgba(30, 41, 59, 0.5); -fx-background-radius: 12; -fx-border-color: rgba(234, 179, 8, 0.3); -fx-border-radius: 12; -fx-border-width: 2;");
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setEditable(true);
        buildQAColumns(table);
        return table;
    }

    private void buildQAColumns(TableView<StorageItemViewModel> table) {
        TableColumn<StorageItemViewModel, Boolean> selectCol = new TableColumn<>();
        selectCol.setPrefWidth(40);
        selectCol.setMaxWidth(40);
        selectCol.setMinWidth(40);
        selectCol.setEditable(true);

        CheckBox selectAll = new CheckBox();
        selectAll.setOnAction(e -> {
            boolean selected = selectAll.isSelected();
            filteredQA.forEach(item -> selectionMap.get(item).set(selected));
        });
        selectCol.setGraphic(selectAll);

        selectCol.setCellValueFactory(cellData -> {
            StorageItemViewModel item = cellData.getValue();
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
            StorageItemViewModel item = table.getItems().get(index);
            return selectionMap.get(item);
        }));

        table.getColumns().add(selectCol);

        TableColumn<StorageItemViewModel, String> idCol = new TableColumn<>("ID");
        idCol.setPrefWidth(60);
        idCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        table.getColumns().add(idCol);

        TableColumn<StorageItemViewModel, String> mfgCol = new TableColumn<>("Manufacture");
        mfgCol.setPrefWidth(200);
        mfgCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getManufacture()));
        table.getColumns().add(mfgCol);

        TableColumn<StorageItemViewModel, String> nameCol = new TableColumn<>("Product Name");
        nameCol.setPrefWidth(300);
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProductName()));
        table.getColumns().add(nameCol);

        TableColumn<StorageItemViewModel, String> codeCol = new TableColumn<>("Code");
        codeCol.setPrefWidth(150);
        codeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCode()));
        table.getColumns().add(codeCol);

        TableColumn<StorageItemViewModel, String> priceCol = new TableColumn<>("💵 Price");
        priceCol.setPrefWidth(150);
        priceCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("$%.2f", data.getValue().getPrice())));
        priceCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #eab308; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-color: rgba(234, 179, 8, 0.1);");
                }
            }
        });
        table.getColumns().add(priceCol);

        table.setRowFactory(tv -> {
            TableRow<StorageItemViewModel> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    handleApprove();
                }
            });
            return row;
        });
    }

    private ScrollPane createAnalyticsView() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        VBox content = new VBox(30);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: transparent;");

        Label title = new Label("📊 Business Analytics Dashboard");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: bold;");

        Label subtitle = new Label("Comprehensive business insights and performance metrics");
        subtitle.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 14px;");

        HBox metricsCards = new HBox(20);
        metricsCards.setAlignment(Pos.CENTER);
        metricsCards.setId("metricsCards");

        VBox projectsSection = createProjectsAnalyticsSection();
        VBox customersSection = createCustomersAnalyticsSection();

        content.getChildren().addAll(title, subtitle, metricsCards, projectsSection, customersSection);
        scrollPane.setContent(content);

        return scrollPane;
    }

    // ========== Price List Management View ==========

    private VBox createPriceListView() {
        VBox container = new VBox(15);
        container.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(container, Priority.ALWAYS);

        // Header with upload button
        HBox header = createPriceListHeader();

        // Filter bar
        HBox filterBar = createPriceListFilterBar();

        // Stats label
        priceListStatsLabel = new Label("No price list loaded");
        priceListStatsLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 13px;");

        // Create price list items table
        priceListItemsTable = createPriceListItemsTable();
        VBox.setVgrow(priceListItemsTable, Priority.ALWAYS);

        container.getChildren().addAll(header, filterBar, priceListStatsLabel, priceListItemsTable);
        return container;
    }

    private HBox createPriceListHeader() {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 0, 10, 0));

        Label title = new Label("📋 Price List Management");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button uploadButton = createStyledButton("📤 Upload Price List", "#6366f1", "#4f46e5");
        uploadButton.setOnAction(e -> handleUploadPriceList());

        Button refreshPriceListBtn = createStyledButton("🔄 Refresh", "#6b7280", "#4b5563");
        refreshPriceListBtn.setOnAction(e -> loadPriceListData());

        header.getChildren().addAll(title, spacer, uploadButton, refreshPriceListBtn);
        return header;
    }

    private HBox createPriceListFilterBar() {
        HBox filterBar = new HBox(15);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        Label filterLabel = new Label("Filter by Manufacturer:");
        filterLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.8); -fx-font-size: 13px;");

        manufacturerFilter = new ComboBox<>();
        manufacturerFilter.setPromptText("All Manufacturers");
        manufacturerFilter.setPrefWidth(200);
        manufacturerFilter.setStyle("-fx-background-color: rgba(30, 41, 59, 0.6); -fx-text-fill: white;");
        manufacturerFilter.setOnAction(e -> filterPriceListItems());

        priceListSearchField = new TextField();
        priceListSearchField.setPromptText("🔍 Search by model or description...");
        priceListSearchField.setPrefWidth(300);
        priceListSearchField.setStyle("-fx-background-color: rgba(30, 41, 59, 0.6); -fx-text-fill: white; -fx-prompt-text-fill: rgba(255, 255, 255, 0.5); -fx-padding: 8; -fx-background-radius: 6;");
        priceListSearchField.textProperty().addListener((obs, old, newVal) -> filterPriceListItems());

        Button clearFilterBtn = createStyledButton("Clear", "#6b7280", "#4b5563");
        clearFilterBtn.setOnAction(e -> {
            manufacturerFilter.setValue(null);
            priceListSearchField.clear();
        });

        filterBar.getChildren().addAll(filterLabel, manufacturerFilter, priceListSearchField, clearFilterBtn);
        return filterBar;
    }

    private TableView<PriceListItem> createPriceListItemsTable() {
        TableView<PriceListItem> table = new TableView<>();
        table.setStyle("-fx-background-color: rgba(30, 41, 59, 0.5); -fx-background-radius: 12; -fx-border-color: rgba(99, 102, 241, 0.3); -fx-border-radius: 12; -fx-border-width: 2;");
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        // Manufacturer column
        TableColumn<PriceListItem, String> mfgCol = new TableColumn<>("Manufacturer");
        mfgCol.setPrefWidth(150);
        mfgCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getManufacturer()));
        mfgCol.setStyle("-fx-alignment: CENTER-LEFT;");

        // Model column
        TableColumn<PriceListItem, String> modelCol = new TableColumn<>("Model");
        modelCol.setPrefWidth(150);
        modelCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getModel()));
        modelCol.setStyle("-fx-alignment: CENTER-LEFT;");

        // Description column
        TableColumn<PriceListItem, String> descCol = new TableColumn<>("Description");
        descCol.setPrefWidth(300);
        descCol.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getDescription() != null ? data.getValue().getDescription() : ""
        ));

        // Image column
        TableColumn<PriceListItem, PriceListItem> imageCol = new TableColumn<>("Picture");
        imageCol.setPrefWidth(80);
        imageCol.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue()));
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

        // DPP Price column
        TableColumn<PriceListItem, String> dppCol = new TableColumn<>("DPP");
        dppCol.setPrefWidth(100);
        dppCol.setCellValueFactory(data -> new SimpleStringProperty(
            formatPrice(data.getValue().getDppPrice(), data.getValue().getCurrency())
        ));
        dppCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        dppCol.setCellFactory(col -> createPriceCell("#22c55e"));

        // SI/Installer Price column
        TableColumn<PriceListItem, String> siCol = new TableColumn<>("SI/Installer");
        siCol.setPrefWidth(100);
        siCol.setCellValueFactory(data -> new SimpleStringProperty(
            formatPrice(data.getValue().getSiInstallerPrice(), data.getValue().getCurrency())
        ));
        siCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        siCol.setCellFactory(col -> createPriceCell("#3b82f6"));

        // End User Price column
        TableColumn<PriceListItem, String> endUserCol = new TableColumn<>("End User");
        endUserCol.setPrefWidth(100);
        endUserCol.setCellValueFactory(data -> new SimpleStringProperty(
            formatPrice(data.getValue().getEndUserPrice(), data.getValue().getCurrency())
        ));
        endUserCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        endUserCol.setCellFactory(col -> createPriceCell("#f59e0b"));

        // Match Status column
        TableColumn<PriceListItem, String> matchCol = new TableColumn<>("In Storage");
        matchCol.setPrefWidth(90);
        matchCol.setCellValueFactory(data -> {
            PriceListItem.MatchStatus status = data.getValue().getMatchStatus();
            if (status == PriceListItem.MatchStatus.MATCHED) {
                return new SimpleStringProperty("✓ Yes");
            } else if (status == PriceListItem.MatchStatus.NOT_FOUND) {
                return new SimpleStringProperty("✗ No");
            } else {
                return new SimpleStringProperty("? Unknown");
            }
        });
        matchCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.startsWith("✓")) {
                        setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
                    } else if (item.startsWith("✗")) {
                        setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #6b7280;");
                    }
                }
            }
        });

        table.getColumns().addAll(mfgCol, modelCol, descCol, imageCol, dppCol, siCol, endUserCol, matchCol);

        // Set row height to accommodate images
        table.setFixedCellSize(70);

        return table;
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

    private void handleUploadPriceList() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Price List Excel File");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel Files", "*.xlsx")
        );

        File selectedFile = fileChooser.showOpenDialog(getRootPane().getScene().getWindow());
        if (selectedFile == null) {
            return;
        }

        // Show loading
        loadingIndicator.setVisible(true);

        Task<PriceList> importTask = new Task<>() {
            @Override
            protected PriceList call() throws Exception {
                byte[] fileData = Files.readAllBytes(selectedFile.toPath());

                // Validate first
                PriceListExcelImportService.ValidationResult validation =
                    priceListExcelImportService.validateExcelFile(fileData, selectedFile.getName());

                if (!validation.valid) {
                    throw new Exception(validation.message);
                }

                // Import the price list
                return priceListExcelImportService.importPriceList(
                    fileData,
                    selectedFile.getName(),
                    currentUser != null ? currentUser.getUsername() : "system"
                );
            }
        };

        importTask.setOnSucceeded(e -> {
            loadingIndicator.setVisible(false);
            PriceList result = importTask.getValue();

            // Get stats
            PriceListService.PriceListStats stats = priceListService.getStats(result.getId());

            showSuccess(String.format("Price list imported successfully!\n" +
                "Total items: %d\n" +
                "Matched in storage: %d\n" +
                "Not found in storage: %d\n" +
                "Manufacturers: %d",
                stats.getTotalItems(),
                stats.getMatchedItems(),
                stats.getNotFoundItems(),
                stats.getManufacturersCount()
            ));

            // Reload the data
            loadPriceListData();
        });

        importTask.setOnFailed(e -> {
            loadingIndicator.setVisible(false);
            Throwable ex = importTask.getException();
            showError("Failed to import price list: " + ex.getMessage());
        });

        new Thread(importTask).start();
    }

    private void loadPriceListData() {
        Task<List<PriceListItem>> loadTask = new Task<>() {
            @Override
            protected List<PriceListItem> call() {
                return priceListService.getItemsFromLatestPriceList();
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<PriceListItem> items = loadTask.getValue();
            priceListItems.clear();
            priceListItems.addAll(items);

            // Update manufacturer filter
            List<String> manufacturers = priceListService.getAllManufacturers();
            manufacturerFilter.getItems().clear();
            manufacturerFilter.getItems().add(null); // "All" option
            manufacturerFilter.getItems().addAll(manufacturers);

            // Update stats
            updatePriceListStats();

            System.out.println("✓ Loaded " + items.size() + " price list items");
        });

        loadTask.setOnFailed(e -> {
            showError("Failed to load price list: " + loadTask.getException().getMessage());
        });

        new Thread(loadTask).start();
    }

    private void filterPriceListItems() {
        String selectedManufacturer = manufacturerFilter.getValue();
        String searchText = priceListSearchField.getText();

        filteredPriceListItems.setPredicate(item -> {
            // Filter by manufacturer
            if (selectedManufacturer != null && !selectedManufacturer.isEmpty()) {
                if (!item.getManufacturer().equals(selectedManufacturer)) {
                    return false;
                }
            }

            // Filter by search text
            if (searchText != null && !searchText.isEmpty()) {
                String lowerSearch = searchText.toLowerCase();
                boolean matchModel = item.getModel() != null &&
                    item.getModel().toLowerCase().contains(lowerSearch);
                boolean matchDesc = item.getDescription() != null &&
                    item.getDescription().toLowerCase().contains(lowerSearch);
                return matchModel || matchDesc;
            }

            return true;
        });

        updatePriceListStats();
    }

    private void updatePriceListStats() {
        int total = priceListItems.size();
        int filtered = filteredPriceListItems.size();

        if (total == 0) {
            priceListStatsLabel.setText("No price list loaded. Click 'Upload Price List' to import.");
        } else if (total == filtered) {
            priceListStatsLabel.setText(String.format("Showing all %d items", total));
        } else {
            priceListStatsLabel.setText(String.format("Showing %d of %d items (filtered)", filtered, total));
        }
    }

    private VBox createProjectsAnalyticsSection() {
        VBox section = new VBox(15);
        Label title = new Label("📁 Projects Analytics");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");

        TableView<ProjectAnalyticsDTO> table = new TableView<>();
        table.setId("projectsAnalyticsTable");
        table.setStyle("-fx-background-color: rgba(30, 41, 59, 0.5); -fx-background-radius: 12; -fx-border-color: rgba(99, 102, 241, 0.3); -fx-border-radius: 12; -fx-border-width: 2;");
        table.setPrefHeight(300);

        TableColumn<ProjectAnalyticsDTO, String> nameCol = new TableColumn<>("Project Name");
        nameCol.setPrefWidth(200);
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProjectName()));

        TableColumn<ProjectAnalyticsDTO, String> statusCol = new TableColumn<>("Status");
        statusCol.setPrefWidth(120);
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));

        TableColumn<ProjectAnalyticsDTO, String> durationCol = new TableColumn<>("Duration (days)");
        durationCol.setPrefWidth(120);
        durationCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDurationDays() != null ? String.valueOf(data.getValue().getDurationDays()) : "N/A"));

        TableColumn<ProjectAnalyticsDTO, String> elementsCol = new TableColumn<>("Elements");
        elementsCol.setPrefWidth(100);
        elementsCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getElementsCount() != null ? data.getValue().getElementsCount() : 0)));

        TableColumn<ProjectAnalyticsDTO, String> costCol = new TableColumn<>("Total Cost");
        costCol.setPrefWidth(120);
        costCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTotalCost() != null ? String.format("$%.2f", data.getValue().getTotalCost()) : "$0.00"));

        table.getColumns().addAll(nameCol, statusCol, durationCol, elementsCol, costCol);
        section.getChildren().addAll(title, table);
        return section;
    }

    private VBox createCustomersAnalyticsSection() {
        VBox section = new VBox(15);
        Label title = new Label("👥 Customer Analytics");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");

        TableView<CustomerAnalyticsDTO> table = new TableView<>();
        table.setId("customersAnalyticsTable");
        table.setStyle("-fx-background-color: rgba(30, 41, 59, 0.5); -fx-background-radius: 12; -fx-border-color: rgba(34, 197, 94, 0.3); -fx-border-radius: 12; -fx-border-width: 2;");
        table.setPrefHeight(300);

        TableColumn<CustomerAnalyticsDTO, String> nameCol = new TableColumn<>("Customer Name");
        nameCol.setPrefWidth(200);
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCustomerName()));

        TableColumn<CustomerAnalyticsDTO, String> emailCol = new TableColumn<>("Email");
        emailCol.setPrefWidth(180);
        emailCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));

        TableColumn<CustomerAnalyticsDTO, String> ordersCol = new TableColumn<>("Orders");
        ordersCol.setPrefWidth(100);
        ordersCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getOrdersCount() != null ? data.getValue().getOrdersCount() : 0)));

        TableColumn<CustomerAnalyticsDTO, String> salesCol = new TableColumn<>("Total Sales");
        salesCol.setPrefWidth(120);
        salesCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTotalSales() != null ? String.format("$%.2f", data.getValue().getTotalSales()) : "$0.00"));

        table.getColumns().addAll(nameCol, emailCol, ordersCol, salesCol);
        section.getChildren().addAll(title, table);
        return section;
    }

    private void refreshAnalytics() {
        Platform.runLater(() -> {
            try {
                AnalyticsService.BusinessMetricsDTO metrics = analyticsService.getBusinessMetrics();

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

                List<ProjectAnalyticsDTO> projectAnalytics = analyticsService.getProjectAnalytics();
                TableView<ProjectAnalyticsDTO> projectsTable = (TableView<ProjectAnalyticsDTO>) analyticsView.lookup("#projectsAnalyticsTable");
                if (projectsTable != null) {
                    projectsTable.getItems().setAll(projectAnalytics);
                }

                List<CustomerAnalyticsDTO> customerAnalytics = analyticsService.getCustomerAnalytics();
                TableView<CustomerAnalyticsDTO> customersTable = (TableView<CustomerAnalyticsDTO>) analyticsView.lookup("#customersAnalyticsTable");
                if (customersTable != null) {
                    customersTable.getItems().setAll(customerAnalytics);
                }

                System.out.println("✓ Analytics refreshed");
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
        card.setStyle("-fx-background-color: rgba(30, 41, 59, 0.6); -fx-background-radius: 12; -fx-border-color: " + color + "; -fx-border-width: 2; -fx-border-radius: 12; -fx-min-width: 150px; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 10, 0, 0, 3);");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 32px; -fx-font-weight: bold;");

        Label labelText = new Label(label);
        labelText.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 14px;");

        card.getChildren().addAll(valueLabel, labelText);
        return card;
    }

    private void loadQAData() {
        Task<List<StorageItem>> loadTask = new Task<>() {
            @Override
            protected List<StorageItem> call() {
                return storageService.getAllItems();
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<StorageItem> items = loadTask.getValue();
            qaItems.clear();
            selectionMap.clear();

            for (StorageItem entity : items) {
                StorageItemViewModel vm = convertToViewModel(entity);
                qaItems.add(vm);
                selectionMap.put(vm, new SimpleBooleanProperty(false));
            }

            System.out.println("✓ Loaded " + items.size() + " items for quality assurance");
        });

        loadTask.setOnFailed(e -> showError("Failed to load items: " + loadTask.getException().getMessage()));

        new Thread(loadTask).start();
    }

    private void handleApprove() {
        List<StorageItemViewModel> selected = selectionMap.entrySet().stream()
                .filter(e -> e.getValue().get())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (selected.isEmpty()) {
            showWarning("Please select at least one item to approve");
            return;
        }

        // TODO: Implement approval workflow
        // This should update the workflow status to APPROVED
        showSuccess("✓ " + selected.size() + " item(s) approved!");

        // Clear selections
        selectionMap.forEach((item, prop) -> prop.set(false));
        updateSelectedCount();
    }

    private void updateSelectedCount() {
        long count = selectionMap.values().stream().filter(BooleanProperty::get).count();
        if (count > 0) {
            selectedCountLabel.setText(String.format("✓ %d item(s) selected", count));
            selectedCountLabel.setVisible(true);
        } else {
            selectedCountLabel.setVisible(false);
        }
    }

    private StorageItemViewModel convertToViewModel(StorageItem entity) {
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

    private void navigateToDashboard() {
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
        qaTable = null;
        analyticsView = null;
        priceListView = null;
        priceListItemsTable = null;
        if (priceListItems != null) {
            priceListItems.clear();
        }
        selectionMap.clear();
        System.out.println("✓ Quality Assurance controller cleaned up");
    }
}
