    package com.magictech.modules.storage.base;

    import com.magictech.core.module.BaseModuleController;
    import com.magictech.core.ui.SceneManager;
    import com.magictech.modules.storage.config.ModuleStorageConfig;
    import com.magictech.modules.storage.entity.StorageItem;
    import com.magictech.modules.storage.model.StorageItemViewModel;
    import com.magictech.modules.storage.service.StorageService;
    import javafx.animation.FadeTransition;
    import javafx.animation.Interpolator;
    import javafx.animation.ScaleTransition;
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
    import javafx.scene.control.cell.PropertyValueFactory;
    import javafx.scene.layout.*;
    import javafx.scene.text.Font;
    import javafx.util.Duration;
    import org.springframework.beans.factory.annotation.Autowired;

    import java.math.BigDecimal;
    import java.time.LocalDateTime;
    import java.time.format.DateTimeFormatter;
    import java.util.*;
    import java.util.stream.Collectors;

    /**
     * Base Storage Module Controller - UPDATED WITH DARK THEME + ANIMATED BACKGROUND
     * Shared functionality for all storage-based modules (Sales, Maintenance, Projects, Pricing)
     */
    public abstract class BaseStorageModuleController extends BaseModuleController {

        @Autowired
        protected StorageService storageService;

        protected TableView<StorageItemViewModel> storageTable;
        protected TextField searchField;
        protected VBox emptyStateContainer;
        protected Button addButton, editButton, deleteButton, refreshButton;
        protected ProgressIndicator loadingIndicator;
        protected Label selectedCountLabel;
        protected CheckBox selectAllCheckbox;
        protected com.magictech.core.ui.components.DashboardBackgroundPane backgroundPane; // ✅ ADDED

        protected ObservableList<StorageItemViewModel> storageItems;
        protected FilteredList<StorageItemViewModel> filteredItems;
        protected Map<StorageItemViewModel, BooleanProperty> selectionMap = new HashMap<>();

        protected ModuleStorageConfig moduleConfig;

        /**
         * Set module-specific configuration
         */
        protected abstract ModuleStorageConfig getModuleConfig();

        /**
         * Get module color scheme for header
         */
        protected abstract String getHeaderColor();

        /**
         * Get module icon
         */
        protected abstract String getModuleIcon();

        @Override
        protected void setupUI() {
            moduleConfig = getModuleConfig();

            // ✅ CREATE STACK WITH ANIMATED BACKGROUND
            StackPane stackRoot = new StackPane();
            backgroundPane = new com.magictech.core.ui.components.DashboardBackgroundPane();

            BorderPane contentPane = new BorderPane();
            contentPane.setStyle("-fx-background-color: transparent;");

            VBox header = createModuleHeader();
            contentPane.setTop(header);

            VBox content = createMainContent();
            contentPane.setCenter(content);

            // ✅ ADD BACKGROUND FIRST, THEN CONTENT
            stackRoot.getChildren().addAll(backgroundPane, contentPane);

            getRootPane().setCenter(stackRoot);
            getRootPane().setStyle("-fx-background-color: transparent;");

            playEntranceAnimation();
        }

        @Override
        protected void loadData() {
            storageItems = FXCollections.observableArrayList();
            filteredItems = new FilteredList<>(storageItems, p -> true);
            storageTable.setItems(filteredItems);
            loadDataFromDatabase();
        }


        protected VBox createModuleHeader() {
            VBox headerContainer = new VBox();

            HBox headerBar = new HBox();
            headerBar.setAlignment(Pos.CENTER_LEFT);
            headerBar.setSpacing(20);
            headerBar.setPadding(new Insets(20, 30, 20, 30));
            headerBar.setStyle(
                    "-fx-background-color: " + getHeaderColor() + ";" +
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
            Label iconLabel = new Label(getModuleIcon());
            iconLabel.setFont(new Font(32));
            Label titleLabel = new Label(moduleConfig.getDisplayName());
            titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 26px; -fx-font-weight: bold;");
            titleBox.getChildren().addAll(iconLabel, titleLabel);
            HBox.setHgrow(titleBox, Priority.ALWAYS);

            Label userLabel = new Label("👤 " + (currentUser != null ? currentUser.getUsername() : "User"));
            userLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.9); -fx-font-size: 14px;");

            headerBar.getChildren().addAll(backButton, titleBox, userLabel);

            HBox subtitleBar = new HBox(20);
            subtitleBar.setAlignment(Pos.CENTER);
            subtitleBar.setPadding(new Insets(12, 30, 12, 30));
            subtitleBar.setStyle(
                    "-fx-background-color: rgba(20, 30, 45, 0.4);" +
                            "-fx-border-color: rgba(255, 255, 255, 0.1);" +
                            "-fx-border-width: 0 0 1 0;"
            );

            String subtitle = moduleConfig.isUseAvailabilityStatus()
                    ? "Shared Storage Database • Availability-Based View"
                    : "Shared Storage Database • Full Access";

            Label subtitleLabel = new Label(subtitle);
            subtitleLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 14px;");

            selectedCountLabel = new Label();
            selectedCountLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 14px; -fx-font-weight: bold;");
            selectedCountLabel.setVisible(false);

            subtitleBar.getChildren().addAll(subtitleLabel, selectedCountLabel);

            headerContainer.getChildren().addAll(headerBar, subtitleBar);
            return headerContainer;
        }

        protected VBox createMainContent() {
            VBox content = new VBox(20);
            content.setPadding(new Insets(30));
            content.setStyle("-fx-background-color: transparent;");

            HBox toolbar = createToolbar();
            StackPane tableContainer = new StackPane();

            storageTable = createStorageTable();
            emptyStateContainer = createEmptyState();

            loadingIndicator = new ProgressIndicator();
            loadingIndicator.setVisible(false);
            loadingIndicator.setMaxSize(60, 60);

            tableContainer.getChildren().addAll(storageTable, emptyStateContainer, loadingIndicator);
            VBox.setVgrow(tableContainer, Priority.ALWAYS);

            content.getChildren().addAll(toolbar, tableContainer);
            return content;
        }

        protected HBox createToolbar() {
            HBox toolbar = new HBox(12);
            toolbar.setAlignment(Pos.CENTER_LEFT);
            toolbar.setPadding(new Insets(0, 0, 15, 0));

            addButton = createStyledButton("+ Add", "#22c55e", "#16a34a");
            addButton.setOnAction(e -> handleAddItem());

            editButton = createStyledButton("✏️ Edit", "#3b82f6", "#2563eb");
            editButton.setOnAction(e -> handleEditItem());
            editButton.setDisable(true);

            deleteButton = createStyledButton("🗑️ Delete", "#ef4444", "#dc2626");
            deleteButton.setOnAction(e -> handleBulkDelete());
            deleteButton.setDisable(true);

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

            toolbar.getChildren().addAll(addButton, editButton, deleteButton, refreshButton, spacer, searchField);
            return toolbar;
        }

        protected Button createStyledButton(String text, String bgColor, String hoverColor) {
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

        protected TableView<StorageItemViewModel> createStorageTable() {
            TableView<StorageItemViewModel> table = new TableView<>();
            table.setStyle(
                    "-fx-background-color: rgba(30, 41, 59, 0.5);" +
                            "-fx-background-radius: 12;" +
                            "-fx-border-color: rgba(139, 92, 246, 0.3);" +
                            "-fx-border-radius: 12;" +
                            "-fx-border-width: 2;"
            );
            table.setEditable(true);
            buildTableColumns(table);
            return table;
        }

        protected void buildTableColumns(TableView<StorageItemViewModel> table) {
            table.getColumns().clear();
            table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

            // Checkbox column
            TableColumn<StorageItemViewModel, Boolean> selectCol = new TableColumn<>();
            selectCol.setPrefWidth(40);
            selectCol.setMaxWidth(40);
            selectCol.setMinWidth(40);
            selectCol.setResizable(false);
            selectCol.setEditable(true);

            selectAllCheckbox = new CheckBox();
            selectAllCheckbox.setOnAction(e -> {
                boolean selectAll = selectAllCheckbox.isSelected();
                for (StorageItemViewModel item : filteredItems) {
                    selectionMap.get(item).set(selectAll);
                }
                updateSelectedCount();
            });
            selectCol.setGraphic(selectAllCheckbox);

            selectCol.setCellValueFactory(cellData -> {
                StorageItemViewModel item = cellData.getValue();
                return selectionMap.computeIfAbsent(item, k -> {
                    BooleanProperty prop = new SimpleBooleanProperty(false);
                    prop.addListener((obs, oldVal, newVal) -> {
                        updateSelectedCount();
                        Platform.runLater(this::updateSelectAllCheckbox);
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

            // Add columns based on module configuration
            for (ModuleStorageConfig.ColumnConfig colConfig : moduleConfig.getColumnConfigs()) {
                TableColumn<StorageItemViewModel, String> column = createTableColumn(colConfig);
                table.getColumns().add(column);
            }

            // Row factory for selection highlighting
            table.setRowFactory(tv -> createStyledTableRow());
        }

        protected TableColumn<StorageItemViewModel, String> createTableColumn(ModuleStorageConfig.ColumnConfig colConfig) {
            TableColumn<StorageItemViewModel, String> column = new TableColumn<>(colConfig.getDisplayLabel());
            column.setPrefWidth(colConfig.getWidth());
            column.setMinWidth(colConfig.getWidth() - 20);

            switch (colConfig.getColumnName()) {
                case "id":
                    column.setCellValueFactory(cellData ->
                            new SimpleStringProperty(String.valueOf(cellData.getValue().getId())));
                    break;
                case "manufacture":
                    column.setCellValueFactory(new PropertyValueFactory<>("manufacture"));
                    break;
                case "productName":
                    column.setCellValueFactory(new PropertyValueFactory<>("productName"));
                    break;
                case "code":
                    column.setCellValueFactory(new PropertyValueFactory<>("code"));
                    break;
                case "serialNumber":
                    column.setCellValueFactory(new PropertyValueFactory<>("serialNumber"));
                    break;
                case "quantity":
                    column.setCellValueFactory(cellData ->
                            new SimpleStringProperty(String.valueOf(cellData.getValue().getQuantity())));
                    column.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold;");
                    break;
                case "availabilityStatus":
                    column.setCellValueFactory(new PropertyValueFactory<>("availabilityStatus"));
                    column.setCellFactory(col -> new TableCell<StorageItemViewModel, String>() {
                        @Override
                        protected void updateItem(String item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty || item == null) {
                                setText(null);
                                setStyle("");
                            } else {
                                setText(item);
                                if (item.contains("✅")) {
                                    setStyle("-fx-alignment: CENTER; -fx-text-fill: #22c55e; -fx-font-weight: bold;");
                                } else {
                                    setStyle("-fx-alignment: CENTER; -fx-text-fill: #ef4444; -fx-font-weight: bold;");
                                }
                            }
                        }
                    });
                    break;
                case "price":
                    column.setCellValueFactory(cellData -> {
                        BigDecimal price = cellData.getValue().getPrice();
                        return new SimpleStringProperty(price != null ? String.format("$%.2f", price.doubleValue()) : "$0.00");
                    });
                    column.setCellFactory(col -> new TableCell<StorageItemViewModel, String>() {
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
                    break;
                case "dateAdded":
                    column.setCellValueFactory(new PropertyValueFactory<>("dateAdded"));
                    break;
            }

            String alignment = colConfig.getAlignment();
            if (!colConfig.getColumnName().equals("price") && !colConfig.getColumnName().equals("availabilityStatus")) {
                column.setStyle("-fx-alignment: " + alignment + "; -fx-padding: 0 10 0 10;");
            }

            return column;
        }

        protected TableRow<StorageItemViewModel> createStyledTableRow() {
            TableRow<StorageItemViewModel> row = new TableRow<StorageItemViewModel>() {
                @Override
                protected void updateItem(StorageItemViewModel item, boolean empty) {
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
        }

        protected void updateRowStyle(TableRow<StorageItemViewModel> row, boolean isSelected) {
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

        protected VBox createEmptyState() {
            VBox emptyState = new VBox(20);
            emptyState.setAlignment(Pos.CENTER);
            emptyState.setPadding(new Insets(50));
            emptyState.setStyle(
                    "-fx-background-color: rgba(30, 41, 59, 0.3);" +
                            "-fx-background-radius: 12;" +
                            "-fx-border-color: rgba(139, 92, 246, 0.2);" +
                            "-fx-border-radius: 12;" +
                            "-fx-border-width: 2;" +
                            "-fx-border-style: dashed;"
            );

            Label iconLabel = new Label(getModuleIcon());
            iconLabel.setFont(new Font(64));

            Label titleLabel = new Label("No Items Available");
            titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");

            Label messageLabel = new Label("Click '+ Add' to start adding items");
            messageLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.6); -fx-font-size: 14px;");

            emptyState.getChildren().addAll(iconLabel, titleLabel, messageLabel);
            emptyState.setVisible(false);
            return emptyState;
        }

        protected void updateSelectAllCheckbox() {
            if (filteredItems.isEmpty()) {
                selectAllCheckbox.setSelected(false);
                selectAllCheckbox.setIndeterminate(false);
                return;
            }

            long selectedCount = selectionMap.values().stream()
                    .filter(BooleanProperty::get)
                    .count();

            if (selectedCount == 0) {
                selectAllCheckbox.setSelected(false);
                selectAllCheckbox.setIndeterminate(false);
            } else if (selectedCount == filteredItems.size()) {
                selectAllCheckbox.setSelected(true);
                selectAllCheckbox.setIndeterminate(false);
            } else {
                selectAllCheckbox.setSelected(false);
                selectAllCheckbox.setIndeterminate(true);
            }
        }

        protected void updateSelectedCount() {
            long count = selectionMap.values().stream()
                    .filter(BooleanProperty::get)
                    .count();

            Platform.runLater(() -> {
                if (count > 0) {
                    selectedCountLabel.setText("✓ " + count + " item(s) selected");
                    selectedCountLabel.setVisible(true);
                    deleteButton.setDisable(false);
                    editButton.setDisable(count != 1);
                } else {
                    selectedCountLabel.setVisible(false);
                    deleteButton.setDisable(true);
                    editButton.setDisable(true);
                }
            });
        }

        protected List<StorageItemViewModel> getSelectedItems() {
            return selectionMap.entrySet().stream()
                    .filter(entry -> entry.getValue().get())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }

        protected void loadDataFromDatabase() {
            showLoading(true);

            Task<List<StorageItem>> loadTask = new Task<>() {
                @Override
                protected List<StorageItem> call() {
                    return storageService.getAllItems();
                }
            };

            loadTask.setOnSucceeded(e -> {
                List<StorageItem> items = loadTask.getValue();
                storageItems.clear();
                selectionMap.clear();

                for (StorageItem entity : items) {
                    StorageItemViewModel vm = convertToViewModel(entity);
                    storageItems.add(vm);
                    BooleanProperty prop = new SimpleBooleanProperty(false);
                    prop.addListener((obs, oldVal, newVal) -> updateSelectedCount());
                    selectionMap.put(vm, prop);
                }

                updateEmptyState();
                showLoading(false);
                System.out.println("✓ Loaded " + items.size() + " items for " + moduleConfig.getDisplayName());
            });

            loadTask.setOnFailed(e -> {
                showLoading(false);
                showError("Failed to load data: " + loadTask.getException().getMessage());
            });

            new Thread(loadTask).start();
        }

        protected StorageItemViewModel convertToViewModel(StorageItem entity) {
            StorageItemViewModel vm = new StorageItemViewModel();
            vm.setId(entity.getId());
            vm.setManufacture(entity.getManufacture() != null ? entity.getManufacture() : "");
            vm.setProductName(entity.getProductName());
            vm.setCode(entity.getCode() != null ? entity.getCode() : "");
            vm.setSerialNumber(entity.getSerialNumber() != null ? entity.getSerialNumber() : "");
            vm.setQuantity(entity.getQuantity() != null ? entity.getQuantity() : 0);
            vm.setPrice(entity.getPrice() != null ? entity.getPrice() : BigDecimal.ZERO);
            vm.setDateAdded(entity.getDateAdded().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            vm.updateAvailabilityStatus();
            return vm;
        }

        protected StorageItem convertToEntity(StorageItemViewModel vm) {
            StorageItem entity = new StorageItem();
            entity.setManufacture(vm.getManufacture());
            entity.setProductName(vm.getProductName());
            entity.setCode(vm.getCode());
            entity.setSerialNumber(vm.getSerialNumber());
            entity.setQuantity(vm.getQuantity());
            entity.setPrice(vm.getPrice());
            entity.setCreatedBy(currentUser != null ? currentUser.getUsername() : "system");
            return entity;
        }

        protected void showLoading(boolean show) {
            Platform.runLater(() -> {
                loadingIndicator.setVisible(show);
                storageTable.setDisable(show);
                addButton.setDisable(show);
            });
        }

        protected void updateEmptyState() {
            Platform.runLater(() -> {
                boolean isEmpty = storageItems.isEmpty();
                emptyStateContainer.setVisible(isEmpty);
                storageTable.setVisible(!isEmpty);
            });
        }

        protected void handleSearch(String searchText) {
            filteredItems.setPredicate(item -> {
                if (searchText == null || searchText.isEmpty()) {
                    return true;
                }

                String lower = searchText.toLowerCase();
                return (item.getManufacture() != null && item.getManufacture().toLowerCase().contains(lower)) ||
                        (item.getProductName() != null && item.getProductName().toLowerCase().contains(lower)) ||
                        (item.getCode() != null && item.getCode().toLowerCase().contains(lower)) ||
                        (item.getSerialNumber() != null && item.getSerialNumber().toLowerCase().contains(lower));
            });
        }

        protected abstract void handleAddItem();
        protected abstract void handleEditItem();
        protected abstract void handleBulkDelete();

        // Add this method to BaseStorageModuleController class (at the end, before the closing brace)
        public void cleanup() {
            if (backgroundPane != null) {
                backgroundPane.stopAnimation();
                System.out.println("✓ " + getModuleConfig().getDisplayName() + " background animation stopped");
            }
        }

        // REPLACE the handleBack() method:
        protected void handleBack() {
            if (backgroundPane != null) {
                backgroundPane.stopAnimation();
            }
            // ✅ Direct navigation - loading overlay handles the transition
            SceneManager.getInstance().showMainDashboard();
        }

        // ADD this new method (at the end of the class):
        public void immediateCleanup() {
            if (backgroundPane != null) {
                backgroundPane.stopAnimation();
                backgroundPane = null;
            }
            System.out.println("✓ " + getModuleConfig().getDisplayName() + " cleaned up immediately");
        }

        @Override
        public void refresh() {
            selectionMap.clear();
            if (selectAllCheckbox != null) {
                selectAllCheckbox.setSelected(false);
            }
            loadDataFromDatabase();
        }

        private void playEntranceAnimation() {
            // Animation disabled - loading overlay handles all transitions
        }
    }