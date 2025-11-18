package com.magictech.modules.pricing;

import com.magictech.core.module.BaseModuleController;
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
import javafx.scene.layout.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Pricing Controller with tab structure
 * Tab 1: Price Management (price-only editing)
 * Tab 2: Analytics Dashboard (read-only)
 */
@Component
public class PricingController extends BaseModuleController {

    @Autowired
    private StorageService storageService;

    @Autowired
    private AnalyticsService analyticsService;

    private enum ActiveView { PRICING, ANALYTICS }
    private ActiveView currentView = ActiveView.PRICING;

    private TableView<StorageItemViewModel> pricingTable;
    private ScrollPane analyticsView;
    private StackPane tableContainer;
    private TextField searchField;
    private Button editPriceButton, refreshButton;
    private Button pricingTabButton, analyticsTabButton;
    private com.magictech.core.ui.components.DashboardBackgroundPane backgroundPane;
    private ProgressIndicator loadingIndicator;
    private Label selectedCountLabel;

    private ObservableList<StorageItemViewModel> pricingItems;
    private FilteredList<StorageItemViewModel> filteredPricing;
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
        pricingItems = FXCollections.observableArrayList();
        filteredPricing = new FilteredList<>(pricingItems, p -> true);
        pricingTable.setItems(filteredPricing);
        loadPricingData();
    }

    @Override
    public void refresh() {
        if (currentView == ActiveView.PRICING) {
            loadPricingData();
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

        Label titleLabel = new Label("💰 Pricing Module");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 32px; -fx-font-weight: bold;");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Button backButton = new Button("← Back");
        backButton.setStyle("-fx-background-color: #1f2937; -fx-text-fill: white; -fx-font-size: 14px; " +
                "-fx-font-weight: 600; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        backButton.setOnAction(e -> com.magictech.core.ui.SceneManager.getInstance().showMainDashboard());

        headerBar.getChildren().addAll(titleLabel, backButton);

        HBox subtitleBar = new HBox();
        subtitleBar.setAlignment(Pos.CENTER);
        subtitleBar.setPadding(new Insets(12, 30, 12, 30));
        subtitleBar.setStyle("-fx-background-color: rgba(20, 30, 45, 0.4); -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-width: 0 0 1 0;");

        Label subtitleLabel = new Label("Price Management • Business Analytics Dashboard");
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
        pricingTable = createPricingTable();
        analyticsView = createAnalyticsView();

        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setVisible(false);
        loadingIndicator.setMaxSize(60, 60);

        tableContainer.getChildren().addAll(pricingTable, loadingIndicator);
        analyticsView.setVisible(false);

        VBox.setVgrow(tableContainer, Priority.ALWAYS);

        content.getChildren().addAll(tabSwitcher, toolbar, tableContainer);
        return content;
    }

    private HBox createTabSwitcher() {
        HBox tabBox = new HBox(10);
        tabBox.setAlignment(Pos.CENTER_LEFT);
        tabBox.setPadding(new Insets(0, 0, 10, 0));

        pricingTabButton = createTabButton("💰 Price Management", true);
        pricingTabButton.setOnAction(e -> switchToPricingView());

        analyticsTabButton = createTabButton("📊 Analytics Dashboard", false);
        analyticsTabButton.setOnAction(e -> switchToAnalyticsView());

        tabBox.getChildren().addAll(pricingTabButton, analyticsTabButton);
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

    private void switchToPricingView() {
        currentView = ActiveView.PRICING;
        tableContainer.getChildren().clear();
        tableContainer.getChildren().addAll(pricingTable, loadingIndicator);
        pricingTable.setVisible(true);
        analyticsView.setVisible(false);
        updateTabButtonStyle(pricingTabButton, true);
        updateTabButtonStyle(analyticsTabButton, false);
        editPriceButton.setVisible(true);
        editPriceButton.setManaged(true);
        pricingTable.refresh();
        updateSelectedCount();
        System.out.println("✓ Switched to Price Management");
    }

    private void switchToAnalyticsView() {
        currentView = ActiveView.ANALYTICS;
        tableContainer.getChildren().clear();
        tableContainer.getChildren().addAll(analyticsView, loadingIndicator);
        analyticsView.setVisible(true);
        pricingTable.setVisible(false);
        updateTabButtonStyle(pricingTabButton, false);
        updateTabButtonStyle(analyticsTabButton, true);
        editPriceButton.setVisible(false);
        editPriceButton.setManaged(false);
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
            filteredPricing.setPredicate(item -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String lower = newVal.toLowerCase();
                return item.getProductName().toLowerCase().contains(lower) || item.getManufacture().toLowerCase().contains(lower);
            });
        });

        editPriceButton = new Button("💵 Edit Price");
        editPriceButton.setStyle("-fx-background-color: #eab308; -fx-text-fill: white; -fx-font-size: 14px; " +
                "-fx-font-weight: 600; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        editPriceButton.setOnAction(e -> handleEditPrice());

        refreshButton = new Button("🔄 Refresh");
        refreshButton.setStyle("-fx-background-color: #6b7280; -fx-text-fill: white; -fx-font-size: 14px; " +
                "-fx-font-weight: 600; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        refreshButton.setOnAction(e -> refresh());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        toolbar.getChildren().addAll(searchField, spacer, editPriceButton, refreshButton);
        return toolbar;
    }

    private TableView<StorageItemViewModel> createPricingTable() {
        TableView<StorageItemViewModel> table = new TableView<>();
        table.setStyle("-fx-background-color: rgba(30, 41, 59, 0.5); -fx-background-radius: 12; -fx-border-color: rgba(234, 179, 8, 0.3); -fx-border-radius: 12; -fx-border-width: 2;");
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        buildPricingColumns(table);
        return table;
    }

    private void buildPricingColumns(TableView<StorageItemViewModel> table) {
        TableColumn<StorageItemViewModel, Boolean> selectCol = new TableColumn<>();
        selectCol.setPrefWidth(40);
        selectCol.setMaxWidth(40);
        selectCol.setMinWidth(40);

        CheckBox selectAll = new CheckBox();
        selectAll.setOnAction(e -> {
            boolean selected = selectAll.isSelected();
            filteredPricing.forEach(item -> selectionMap.get(item).set(selected));
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

        selectCol.setCellFactory(col -> {
            CheckBoxTableCell<StorageItemViewModel, Boolean> cell = new CheckBoxTableCell<>();
            cell.setAlignment(Pos.CENTER);
            return cell;
        });

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
                    handleEditPrice();
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

    private void loadPricingData() {
        Task<List<StorageItem>> loadTask = new Task<>() {
            @Override
            protected List<StorageItem> call() {
                return storageService.getAllItems();
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<StorageItem> items = loadTask.getValue();
            pricingItems.clear();
            selectionMap.clear();

            for (StorageItem entity : items) {
                StorageItemViewModel vm = convertToViewModel(entity);
                pricingItems.add(vm);
                selectionMap.put(vm, new SimpleBooleanProperty(false));
            }

            System.out.println("✓ Loaded " + items.size() + " items for pricing");
        });

        loadTask.setOnFailed(e -> showError("Failed to load items: " + loadTask.getException().getMessage()));

        new Thread(loadTask).start();
    }

    private void handleEditPrice() {
        List<StorageItemViewModel> selected = selectionMap.entrySet().stream().filter(e -> e.getValue().get()).map(Map.Entry::getKey).collect(Collectors.toList());

        if (selected.isEmpty()) {
            showWarning("Please select at least one item to edit price");
            return;
        }

        if (selected.size() == 1) {
            editSinglePrice(selected.get(0));
        } else {
            editBulkPrice(selected);
        }
    }

    private void editSinglePrice(StorageItemViewModel item) {
        Dialog<BigDecimal> dialog = new Dialog<>();
        dialog.setTitle("💵 Edit Price");
        dialog.setHeaderText("Edit price for: " + item.getProductName());

        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField priceField = new TextField(item.getPrice().toString());
        priceField.setPromptText("Enter new price");

        grid.add(new Label("Current Price:"), 0, 0);
        grid.add(new Label(String.format("$%.2f", item.getPrice())), 1, 0);
        grid.add(new Label("New Price:"), 0, 1);
        grid.add(priceField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                try {
                    return new BigDecimal(priceField.getText());
                } catch (NumberFormatException e) {
                    showError("Invalid price format");
                    return null;
                }
            }
            return null;
        });

        Optional<BigDecimal> result = dialog.showAndWait();
        result.ifPresent(newPrice -> {
            Task<Void> updateTask = new Task<>() {
                @Override
                protected Void call() {
                    StorageItem entity = storageService.getItemById(item.getId()).orElseThrow(() -> new RuntimeException("Item not found"));
                    entity.setPrice(newPrice);
                    storageService.updateItem(item.getId(), entity);
                    return null;
                }
            };

            updateTask.setOnSucceeded(e -> {
                item.setPrice(newPrice);
                pricingTable.refresh();
                showSuccess("✓ Price updated successfully!");
            });

            updateTask.setOnFailed(e -> showError("Failed to update price: " + updateTask.getException().getMessage()));

            new Thread(updateTask).start();
        });
    }

    private void editBulkPrice(List<StorageItemViewModel> items) {
        Dialog<BigDecimal> dialog = new Dialog<>();
        dialog.setTitle("💵 Bulk Edit Price");
        dialog.setHeaderText("Set same price for " + items.size() + " selected items");

        ButtonType saveBtn = new ButtonType("Save All", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField priceField = new TextField();
        priceField.setPromptText("Enter price for all selected items");

        grid.add(new Label("Selected Items:"), 0, 0);
        grid.add(new Label(String.valueOf(items.size())), 1, 0);
        grid.add(new Label("New Price:"), 0, 1);
        grid.add(priceField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                try {
                    return new BigDecimal(priceField.getText());
                } catch (NumberFormatException e) {
                    showError("Invalid price format");
                    return null;
                }
            }
            return null;
        });

        Optional<BigDecimal> result = dialog.showAndWait();
        result.ifPresent(newPrice -> {
            Task<Void> updateTask = new Task<>() {
                @Override
                protected Void call() {
                    for (StorageItemViewModel item : items) {
                        StorageItem entity = storageService.getItemById(item.getId()).orElseThrow(() -> new RuntimeException("Item not found: " + item.getId()));
                        entity.setPrice(newPrice);
                        storageService.updateItem(item.getId(), entity);
                    }
                    return null;
                }
            };

            updateTask.setOnSucceeded(e -> {
                items.forEach(item -> item.setPrice(newPrice));
                pricingTable.refresh();
                showSuccess("✓ Prices updated for " + items.size() + " items!");
            });

            updateTask.setOnFailed(e -> showError("Failed to update prices: " + updateTask.getException().getMessage()));

            new Thread(updateTask).start();
        });
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

    public void immediateCleanup() {
        if (backgroundPane != null) {
            backgroundPane.stopAnimation();
            backgroundPane = null;
        }
        pricingTable = null;
        analyticsView = null;
        selectionMap.clear();
        System.out.println("✓ Pricing controller cleaned up");
    }
}
