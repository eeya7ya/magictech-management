package com.magictech.core.ui.controllers;

import com.magictech.core.auth.AuthenticationService;
import com.magictech.core.auth.User;
import com.magictech.core.auth.UserRole;
import com.magictech.core.email.EmailService;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * User Management Controller - Modernized Version
 * Admin interface for managing users and roles
 * Only accessible to MASTER role users
 *
 * Features:
 * - Modular component-based UI architecture
 * - Real-time search/filter functionality
 * - Status filter (All/Active/Inactive)
 * - Responsive user count binding
 * - Role-based badge styling
 */
@Component
public class UserManagementController {

    // ================================
    // STYLING CONSTANTS - Centralized UI styling
    // ================================
    private static final class Styles {
        // Colors
        static final String BG_PRIMARY = "#1e293b";
        static final String BG_SECONDARY = "#0f172a";
        static final String BG_ACCENT = "#8b5cf6";
        static final String TEXT_PRIMARY = "#ffffff";
        static final String TEXT_SECONDARY = "rgba(255, 255, 255, 0.7)";
        static final String SUCCESS = "#86efac";
        static final String ERROR = "#fca5a5";

        // Button styles
        static final String BTN_PRIMARY = "-fx-background-color: #8b5cf6; -fx-text-fill: white; " +
                "-fx-font-size: 14px; -fx-font-weight: 600; -fx-padding: 10 20; " +
                "-fx-background-radius: 8; -fx-cursor: hand;";
        static final String BTN_SECONDARY = "-fx-background-color: #475569; -fx-text-fill: white; " +
                "-fx-font-size: 14px; -fx-font-weight: 600; -fx-padding: 10 20; " +
                "-fx-background-radius: 8; -fx-cursor: hand;";
        static final String BTN_SUCCESS = "-fx-background-color: #10b981; -fx-text-fill: white; " +
                "-fx-font-size: 12px; -fx-padding: 5 10; -fx-background-radius: 6; -fx-cursor: hand;";
        static final String BTN_DANGER = "-fx-background-color: #ef4444; -fx-text-fill: white; " +
                "-fx-font-size: 12px; -fx-padding: 5 10; -fx-background-radius: 6; -fx-cursor: hand;";
        static final String BTN_INFO = "-fx-background-color: #3b82f6; -fx-text-fill: white; " +
                "-fx-font-size: 12px; -fx-padding: 5 10; -fx-background-radius: 6; -fx-cursor: hand;";
        static final String BTN_WARNING = "-fx-background-color: #f59e0b; -fx-text-fill: white; " +
                "-fx-font-size: 12px; -fx-padding: 5 10; -fx-background-radius: 6; -fx-cursor: hand;";

        // Input styles
        static final String INPUT_FIELD = "-fx-background-color: #0f172a; -fx-text-fill: white; " +
                "-fx-prompt-text-fill: rgba(255, 255, 255, 0.5); -fx-pref-width: 250; " +
                "-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #334155; -fx-border-width: 1;";
        static final String SEARCH_FIELD = "-fx-background-color: #334155; -fx-text-fill: white; " +
                "-fx-prompt-text-fill: rgba(255, 255, 255, 0.5); -fx-pref-width: 200; " +
                "-fx-background-radius: 20; -fx-padding: 8 15;";
    }

    @Autowired
    private AuthenticationService authService;

    @Autowired
    private EmailService emailService;

    // UI Components
    private TableView<UserViewModel> userTable;
    private ObservableList<UserViewModel> users;
    private FilteredList<UserViewModel> filteredUsers;
    private Stage dialogStage;
    private TextField searchField;
    private ComboBox<String> statusFilter;
    private Label countLabel;

    /**
     * Show user management dialog
     */
    public void showUserManagement(Stage owner) {
        try {
            System.out.println("UserManagementController.showUserManagement() called");

            validateDependencies(owner);

            dialogStage = createDialogStage(owner);
            VBox root = createMainLayout();
            Scene scene = createScene(root);

            dialogStage.setScene(scene);
            loadUsers();

            System.out.println("✓ User management dialog created successfully - showing now...");
            dialogStage.showAndWait();
            System.out.println("✓ User management dialog closed");

        } catch (Exception e) {
            System.err.println("❌ Error in showUserManagement(): " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to show User Management dialog: " + e.getMessage(), e);
        }
    }

    // ================================
    // INITIALIZATION HELPERS
    // ================================

    private void validateDependencies(Stage owner) {
        if (owner == null) {
            throw new IllegalArgumentException("Owner stage cannot be null");
        }
        if (authService == null) {
            throw new IllegalStateException("AuthenticationService is not initialized (Spring autowiring failed)");
        }
    }

    private Stage createDialogStage(Stage owner) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);
        stage.initStyle(StageStyle.DECORATED);
        stage.setTitle("User Management - Admin Panel");
        stage.setMinWidth(800);
        stage.setMinHeight(500);
        return stage;
    }

    private Scene createScene(VBox root) {
        Scene scene = new Scene(root, 950, 650);
        java.net.URL cssUrl = getClass().getResource("/css/styles.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
        return scene;
    }

    // ================================
    // LAYOUT BUILDERS - Modular Components
    // ================================

    private VBox createMainLayout() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: " + Styles.BG_PRIMARY + ";");

        // Build components in correct order for bindings
        HBox header = buildHeaderComponent();
        initializeUserTable();  // Initialize table and users list first
        HBox toolbar = buildToolbarComponent();
        HBox searchBar = buildSearchBarComponent();

        // Table wrapper with styling
        VBox tableWrapper = new VBox(userTable);
        tableWrapper.setStyle("-fx-background-color: " + Styles.BG_SECONDARY + "; " +
                "-fx-background-radius: 12; -fx-padding: 5;");
        VBox.setVgrow(tableWrapper, Priority.ALWAYS);
        VBox.setVgrow(userTable, Priority.ALWAYS);

        root.getChildren().addAll(header, toolbar, searchBar, tableWrapper);
        return root;
    }

    /**
     * Build header component with icon and title
     */
    private HBox buildHeaderComponent() {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        // Icon
        Label icon = new Label("👥");
        icon.setStyle("-fx-font-size: 36px;");

        // Title section
        VBox titleBox = new VBox(5);
        Label title = new Label("User Management");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: " + Styles.TEXT_PRIMARY + ";");

        Label subtitle = new Label("Manage system users, roles, and permissions");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: " + Styles.TEXT_SECONDARY + ";");

        titleBox.getChildren().addAll(title, subtitle);
        header.getChildren().addAll(icon, titleBox);

        return header;
    }

    /**
     * Build toolbar with action buttons and user count
     */
    private HBox buildToolbarComponent() {
        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(10, 0, 10, 0));

        // Add User Button
        Button addUserBtn = new Button("➕ Add New User");
        addUserBtn.setStyle(Styles.BTN_PRIMARY);
        addUserBtn.setOnAction(e -> showAddUserDialog());
        addHoverEffect(addUserBtn, Styles.BTN_PRIMARY, "#7c3aed");

        // Refresh Button
        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.setStyle(Styles.BTN_SECONDARY);
        refreshBtn.setOnAction(e -> loadUsers());
        addHoverEffect(refreshBtn, Styles.BTN_SECONDARY, "#64748b");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // User count display
        HBox countBox = buildUserCountComponent();

        toolbar.getChildren().addAll(addUserBtn, refreshBtn, spacer, countBox);
        return toolbar;
    }

    /**
     * Build user count display component
     */
    private HBox buildUserCountComponent() {
        HBox countBox = new HBox(8);
        countBox.setAlignment(Pos.CENTER_RIGHT);
        countBox.setStyle("-fx-background-color: " + Styles.BG_SECONDARY + "; " +
                "-fx-background-radius: 20; -fx-padding: 8 15;");

        Label infoLabel = new Label("Total Users:");
        infoLabel.setStyle("-fx-text-fill: " + Styles.TEXT_SECONDARY + "; -fx-font-size: 13px;");

        countLabel = new Label("0");
        countLabel.setStyle("-fx-text-fill: " + Styles.BG_ACCENT + "; -fx-font-weight: bold; -fx-font-size: 14px;");
        // Binding will be set after filteredUsers is initialized

        countBox.getChildren().addAll(infoLabel, countLabel);
        return countBox;
    }

    /**
     * Build search and filter bar
     */
    private HBox buildSearchBarComponent() {
        HBox searchBar = new HBox(15);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.setPadding(new Insets(5, 0, 10, 0));

        // Search field
        Label searchIcon = new Label("🔍");
        searchIcon.setStyle("-fx-font-size: 14px;");

        searchField = new TextField();
        searchField.setPromptText("Search users...");
        searchField.setStyle(Styles.SEARCH_FIELD);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        // Status filter
        Label filterLabel = new Label("Status:");
        filterLabel.setStyle("-fx-text-fill: " + Styles.TEXT_SECONDARY + "; -fx-font-size: 13px;");

        statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("All Users", "Active Only", "Inactive Only");
        statusFilter.setValue("All Users");
        statusFilter.setStyle("-fx-background-color: " + Styles.BG_SECONDARY + "; " +
                "-fx-background-radius: 6; -fx-pref-width: 130;");
        statusFilter.setOnAction(e -> applyFilters());

        // Clear filters button
        Button clearBtn = new Button("✕ Clear");
        clearBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + Styles.TEXT_SECONDARY + "; " +
                "-fx-font-size: 12px; -fx-cursor: hand;");
        clearBtn.setOnAction(e -> {
            searchField.clear();
            statusFilter.setValue("All Users");
        });

        searchBar.getChildren().addAll(searchIcon, searchField, filterLabel, statusFilter, clearBtn);
        return searchBar;
    }

    // ================================
    // TABLE COMPONENT
    // ================================

    /**
     * Initialize user table with all columns
     */
    private void initializeUserTable() {
        userTable = new TableView<>();
        userTable.setStyle("-fx-background-color: " + Styles.BG_SECONDARY + ";");
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Initialize data lists
        users = FXCollections.observableArrayList();
        filteredUsers = new FilteredList<>(users, p -> true);
        userTable.setItems(filteredUsers);

        // Bind count label to filtered users size
        Platform.runLater(() -> {
            if (countLabel != null) {
                countLabel.textProperty().bind(
                    javafx.beans.binding.Bindings.size(filteredUsers).asString()
                );
            }
        });

        // Build columns
        userTable.getColumns().addAll(
            buildIdColumn(),
            buildUsernameColumn(),
            buildRoleColumn(),
            buildSmtpStatusColumn(),
            buildLastLoginColumn(),
            buildStatusColumn(),
            buildActionsColumn()
        );

        // Double-click to edit
        userTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && !userTable.getSelectionModel().isEmpty()) {
                showEditUserDialog(userTable.getSelectionModel().getSelectedItem());
            }
        });

        // Empty state placeholder
        Label placeholder = new Label("No users found");
        placeholder.setStyle("-fx-text-fill: " + Styles.TEXT_SECONDARY + "; -fx-font-size: 14px;");
        userTable.setPlaceholder(placeholder);
    }

    private TableColumn<UserViewModel, Long> buildIdColumn() {
        TableColumn<UserViewModel, Long> col = new TableColumn<>("ID");
        col.setCellValueFactory(new PropertyValueFactory<>("id"));
        col.setMinWidth(50);
        col.setMaxWidth(70);
        col.setStyle("-fx-alignment: CENTER;");
        return col;
    }

    private TableColumn<UserViewModel, String> buildUsernameColumn() {
        TableColumn<UserViewModel, String> col = new TableColumn<>("Username");
        col.setCellValueFactory(new PropertyValueFactory<>("username"));
        col.setMinWidth(120);
        col.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label userLabel = new Label("👤 " + item);
                    userLabel.setStyle("-fx-text-fill: white; -fx-font-weight: 600;");
                    setGraphic(userLabel);
                }
            }
        });
        return col;
    }

    private TableColumn<UserViewModel, String> buildRoleColumn() {
        TableColumn<UserViewModel, String> col = new TableColumn<>("Role");
        col.setCellValueFactory(new PropertyValueFactory<>("roleDisplay"));
        col.setMinWidth(150);
        col.setCellFactory(column -> new RoleBadgeCell());
        return col;
    }

    private TableColumn<UserViewModel, String> buildLastLoginColumn() {
        TableColumn<UserViewModel, String> col = new TableColumn<>("Last Login");
        col.setCellValueFactory(new PropertyValueFactory<>("lastLoginFormatted"));
        col.setMinWidth(140);
        col.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: " + Styles.TEXT_SECONDARY + ";");
                }
            }
        });
        return col;
    }

    private TableColumn<UserViewModel, Boolean> buildStatusColumn() {
        TableColumn<UserViewModel, Boolean> col = new TableColumn<>("Status");
        col.setCellValueFactory(new PropertyValueFactory<>("active"));
        col.setMinWidth(100);
        col.setCellFactory(column -> new StatusBadgeCell());
        return col;
    }

    private TableColumn<UserViewModel, Boolean> buildSmtpStatusColumn() {
        TableColumn<UserViewModel, Boolean> col = new TableColumn<>("SMTP");
        col.setCellValueFactory(new PropertyValueFactory<>("smtpConfigured"));
        col.setMinWidth(100);
        col.setCellFactory(column -> new SmtpStatusCell());
        return col;
    }

    private TableColumn<UserViewModel, Void> buildActionsColumn() {
        TableColumn<UserViewModel, Void> col = new TableColumn<>("Actions");
        col.setMinWidth(220);
        col.setCellFactory(column -> new ActionButtonsCell());
        return col;
    }

    // ================================
    // CUSTOM TABLE CELL FACTORIES
    // ================================

    /**
     * Custom cell for role badges with color coding
     */
    private class RoleBadgeCell extends TableCell<UserViewModel, String> {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                Label badge = new Label(item);
                String color = getRoleColor(item);
                badge.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                        "-fx-padding: 4 12; -fx-background-radius: 12; -fx-font-size: 11px; " +
                        "-fx-font-weight: 600;");
                setGraphic(badge);
            }
        }

        private String getRoleColor(String role) {
            if (role == null) return Styles.BG_ACCENT;
            return switch (role.toUpperCase()) {
                case "MASTER", "MASTER ADMIN" -> "#dc2626"; // Red
                case "SALES", "SALES TEAM" -> "#2563eb"; // Blue
                case "PRESALES", "PRESALES TEAM" -> "#7c3aed"; // Purple
                case "FINANCE", "FINANCE TEAM" -> "#059669"; // Green
                case "PROJECTS", "PROJECTS TEAM" -> "#d97706"; // Amber
                case "MAINTENANCE", "MAINTENANCE TEAM" -> "#0891b2"; // Cyan
                case "QUALITY ASSURANCE", "QA TEAM" -> "#be185d"; // Pink
                case "STORAGE", "STORAGE TEAM" -> "#4f46e5"; // Indigo
                default -> Styles.BG_ACCENT;
            };
        }
    }

    /**
     * Custom cell for status badges
     */
    private class StatusBadgeCell extends TableCell<UserViewModel, Boolean> {
        @Override
        protected void updateItem(Boolean item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                HBox statusBox = new HBox(5);
                statusBox.setAlignment(Pos.CENTER_LEFT);

                Label dot = new Label("●");
                dot.setStyle("-fx-text-fill: " + (item ? "#10b981" : "#ef4444") + "; -fx-font-size: 10px;");

                Label text = new Label(item ? "Active" : "Inactive");
                text.setStyle("-fx-text-fill: " + (item ? Styles.SUCCESS : Styles.ERROR) + "; " +
                        "-fx-font-weight: 600; -fx-font-size: 12px;");

                statusBox.getChildren().addAll(dot, text);
                setGraphic(statusBox);
            }
        }
    }

    /**
     * Custom cell for SMTP configuration status
     */
    private class SmtpStatusCell extends TableCell<UserViewModel, Boolean> {
        @Override
        protected void updateItem(Boolean item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
            } else {
                HBox statusBox = new HBox(5);
                statusBox.setAlignment(Pos.CENTER_LEFT);

                boolean configured = item != null && item;

                Label icon = new Label(configured ? "📧" : "❌");
                icon.setStyle("-fx-font-size: 14px;");

                Label text = new Label(configured ? "Configured" : "Not Set");
                text.setStyle("-fx-text-fill: " + (configured ? "#10b981" : "#9ca3af") + "; " +
                        "-fx-font-weight: 600; -fx-font-size: 11px;");

                statusBox.getChildren().addAll(icon, text);
                setGraphic(statusBox);
            }
        }
    }

    /**
     * Custom cell for action buttons
     */
    private class ActionButtonsCell extends TableCell<UserViewModel, Void> {
        private final Button editBtn = new Button("✏️ Edit");
        private final Button testEmailBtn = new Button("📧");
        private final Button toggleBtn = new Button();
        private final Button deleteBtn = new Button("🗑️");
        private final HBox container = new HBox(6);

        public ActionButtonsCell() {
            editBtn.setStyle(Styles.BTN_INFO);
            testEmailBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: white; " +
                    "-fx-font-size: 12px; -fx-padding: 5 8; -fx-background-radius: 6; -fx-cursor: hand;");
            toggleBtn.setStyle(Styles.BTN_WARNING);
            deleteBtn.setStyle(Styles.BTN_DANGER);
            container.setAlignment(Pos.CENTER);

            editBtn.setOnAction(e -> {
                UserViewModel user = getTableView().getItems().get(getIndex());
                showEditUserDialog(user);
            });

            testEmailBtn.setOnAction(e -> {
                UserViewModel user = getTableView().getItems().get(getIndex());
                testUserEmail(user);
            });

            toggleBtn.setOnAction(e -> {
                UserViewModel user = getTableView().getItems().get(getIndex());
                toggleUserStatus(user);
            });

            deleteBtn.setOnAction(e -> {
                UserViewModel user = getTableView().getItems().get(getIndex());
                handleDeleteUser(user);
            });

            container.getChildren().addAll(editBtn, testEmailBtn, toggleBtn, deleteBtn);
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
            } else {
                UserViewModel user = getTableView().getItems().get(getIndex());
                toggleBtn.setText(user.isActive() ? "Deactivate" : "Activate");
                toggleBtn.setStyle(user.isActive() ? Styles.BTN_WARNING : Styles.BTN_SUCCESS);
                setGraphic(container);
            }
        }
    }

    // ================================
    // DATA OPERATIONS
    // ================================

    private void loadUsers() {
        try {
            users.clear();
            List<User> allUsers = authService.getAllUsers();
            for (User user : allUsers) {
                users.add(new UserViewModel(user));
            }
            System.out.println("✓ Loaded " + users.size() + " users");
        } catch (Exception e) {
            System.err.println("❌ Failed to load users: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load users: " + e.getMessage());
        }
    }

    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase().trim();
        String statusValue = statusFilter.getValue();

        Predicate<UserViewModel> predicate = user -> {
            // Search filter
            boolean matchesSearch = searchText.isEmpty() ||
                    user.getUsername().toLowerCase().contains(searchText) ||
                    user.getRoleDisplay().toLowerCase().contains(searchText);

            // Status filter
            boolean matchesStatus = switch (statusValue) {
                case "Active Only" -> user.isActive();
                case "Inactive Only" -> !user.isActive();
                default -> true;
            };

            return matchesSearch && matchesStatus;
        };

        filteredUsers.setPredicate(predicate);
    }

    // ================================
    // DIALOG HANDLERS
    // ================================

    private void showAddUserDialog() {
        Dialog<UserFormData> dialog = new Dialog<>();
        dialog.setTitle("Add New User");
        dialog.setHeaderText("Create a new user account");
        dialog.initOwner(dialogStage);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogPane.setStyle("-fx-background-color: " + Styles.BG_PRIMARY + ";");
        dialogPane.setPrefWidth(400);

        GridPane grid = createUserForm(null);
        dialogPane.setContent(grid);

        // Enable/disable OK button based on validation
        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        okButton.setStyle(Styles.BTN_PRIMARY);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                TextField usernameField = (TextField) grid.lookup("#usernameField");
                PasswordField passwordField = (PasswordField) grid.lookup("#passwordField");
                TextField emailField = (TextField) grid.lookup("#emailField");
                @SuppressWarnings("unchecked")
                ComboBox<UserRole> roleCombo = (ComboBox<UserRole>) grid.lookup("#roleCombo");
                // SMTP fields
                @SuppressWarnings("unchecked")
                ComboBox<String> smtpProviderCombo = (ComboBox<String>) grid.lookup("#smtpProviderCombo");
                TextField smtpHostField = (TextField) grid.lookup("#smtpHostField");
                TextField smtpPortField = (TextField) grid.lookup("#smtpPortField");
                PasswordField smtpPasswordField = (PasswordField) grid.lookup("#smtpPasswordField");

                Integer smtpPort = 587;
                try {
                    smtpPort = Integer.parseInt(smtpPortField.getText().trim());
                } catch (NumberFormatException ignored) {}

                return new UserFormData(
                        usernameField.getText(),
                        passwordField.getText(),
                        emailField.getText(),
                        roleCombo.getValue(),
                        smtpProviderCombo.getValue() != null ? smtpProviderCombo.getValue().toLowerCase() : null,
                        smtpHostField.getText(),
                        smtpPort,
                        smtpPasswordField.getText()
                );
            }
            return null;
        });

        Optional<UserFormData> result = dialog.showAndWait();
        result.ifPresent(this::createUser);
    }

    private void showEditUserDialog(UserViewModel userVM) {
        // Load full user object to get SMTP settings
        Optional<User> userOpt = authService.getUserById(userVM.getId());
        if (userOpt.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "User not found");
            return;
        }
        User fullUser = userOpt.get();

        Dialog<UserFormData> dialog = new Dialog<>();
        dialog.setTitle("Edit User");
        dialog.setHeaderText("Update user information for: " + userVM.getUsername());
        dialog.initOwner(dialogStage);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogPane.setStyle("-fx-background-color: " + Styles.BG_PRIMARY + ";");
        dialogPane.setPrefWidth(400);

        GridPane grid = createUserForm(userVM, fullUser);
        dialogPane.setContent(grid);

        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        okButton.setStyle(Styles.BTN_PRIMARY);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                PasswordField passwordField = (PasswordField) grid.lookup("#passwordField");
                TextField emailField = (TextField) grid.lookup("#emailField");
                @SuppressWarnings("unchecked")
                ComboBox<UserRole> roleCombo = (ComboBox<UserRole>) grid.lookup("#roleCombo");
                // SMTP fields
                @SuppressWarnings("unchecked")
                ComboBox<String> smtpProviderCombo = (ComboBox<String>) grid.lookup("#smtpProviderCombo");
                TextField smtpHostField = (TextField) grid.lookup("#smtpHostField");
                TextField smtpPortField = (TextField) grid.lookup("#smtpPortField");
                PasswordField smtpPasswordField = (PasswordField) grid.lookup("#smtpPasswordField");

                Integer smtpPort = 587;
                try {
                    smtpPort = Integer.parseInt(smtpPortField.getText().trim());
                } catch (NumberFormatException ignored) {}

                String newPassword = passwordField.getText();
                return new UserFormData(
                        userVM.getUsername(),
                        newPassword.isEmpty() ? null : newPassword,
                        emailField.getText(),
                        roleCombo.getValue(),
                        smtpProviderCombo.getValue() != null ? smtpProviderCombo.getValue().toLowerCase() : null,
                        smtpHostField.getText(),
                        smtpPort,
                        smtpPasswordField.getText()
                );
            }
            return null;
        });

        Optional<UserFormData> result = dialog.showAndWait();
        result.ifPresent(data -> updateUser(userVM.getId(), data));
    }

    private GridPane createUserForm(UserViewModel existingUser) {
        return createUserForm(existingUser, null);
    }

    private GridPane createUserForm(UserViewModel existingUser, User fullUser) {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(18);
        grid.setPadding(new Insets(25));

        int row = 0;

        // Username field
        Label usernameLabel = new Label("Username:");
        usernameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: 600;");
        TextField usernameField = new TextField();
        usernameField.setId("usernameField");
        usernameField.setPromptText("Enter username");
        usernameField.setStyle(Styles.INPUT_FIELD);

        if (existingUser != null) {
            usernameField.setText(existingUser.getUsername());
            usernameField.setDisable(true);
            usernameField.setStyle(Styles.INPUT_FIELD + " -fx-opacity: 0.7;");
        }

        grid.add(usernameLabel, 0, row);
        grid.add(usernameField, 1, row);
        row++;

        // Password field
        Label passwordLabel = new Label("Password:");
        passwordLabel.setStyle("-fx-text-fill: white; -fx-font-weight: 600;");
        PasswordField passwordField = new PasswordField();
        passwordField.setId("passwordField");
        passwordField.setPromptText(existingUser != null ? "Leave empty to keep current" : "Enter password");
        passwordField.setStyle(Styles.INPUT_FIELD);

        grid.add(passwordLabel, 0, row);
        grid.add(passwordField, 1, row);
        row++;

        // Role selection
        Label roleLabel = new Label("Role:");
        roleLabel.setStyle("-fx-text-fill: white; -fx-font-weight: 600;");
        ComboBox<UserRole> roleCombo = new ComboBox<>();
        roleCombo.setId("roleCombo");
        roleCombo.getItems().addAll(UserRole.values());
        roleCombo.setStyle("-fx-background-color: " + Styles.BG_SECONDARY + "; -fx-pref-width: 250;");

        if (existingUser != null) {
            roleCombo.setValue(existingUser.getRole());
        } else {
            roleCombo.setValue(UserRole.STORAGE);
        }

        grid.add(roleLabel, 0, row);
        grid.add(roleCombo, 1, row);
        row++;

        // ================================
        // SMTP EMAIL SETTINGS SECTION
        // ================================
        Label smtpSectionLabel = new Label("── SMTP Settings (for sending emails) ──");
        smtpSectionLabel.setStyle("-fx-text-fill: #8b5cf6; -fx-font-weight: bold; -fx-font-size: 12px;");
        grid.add(smtpSectionLabel, 0, row, 2, 1);
        row++;

        // SMTP Email (sender email for SMTP)
        Label emailLabel = new Label("SMTP Email:");
        emailLabel.setStyle("-fx-text-fill: white; -fx-font-weight: 600;");

        TextField emailField = new TextField();
        emailField.setId("emailField");
        emailField.setPromptText("your-email@gmail.com");
        emailField.setStyle(Styles.INPUT_FIELD);

        if (existingUser != null && existingUser.getEmail() != null) {
            emailField.setText(existingUser.getEmail());
        }

        grid.add(emailLabel, 0, row);
        grid.add(emailField, 1, row);
        row++;

        // SMTP Provider
        Label smtpProviderLabel = new Label("Provider:");
        smtpProviderLabel.setStyle("-fx-text-fill: white; -fx-font-weight: 600;");
        ComboBox<String> smtpProviderCombo = new ComboBox<>();
        smtpProviderCombo.setId("smtpProviderCombo");
        smtpProviderCombo.getItems().addAll("Gmail", "Outlook", "Hotmail", "Custom");
        smtpProviderCombo.setStyle("-fx-background-color: " + Styles.BG_SECONDARY + "; -fx-pref-width: 250;");

        // Load existing SMTP provider
        if (fullUser != null && fullUser.getSmtpProvider() != null) {
            String provider = fullUser.getSmtpProvider();
            // Capitalize first letter for display
            String displayProvider = provider.substring(0, 1).toUpperCase() + provider.substring(1).toLowerCase();
            if (smtpProviderCombo.getItems().contains(displayProvider)) {
                smtpProviderCombo.setValue(displayProvider);
            } else {
                smtpProviderCombo.setValue("Custom");
            }
        }

        grid.add(smtpProviderLabel, 0, row);
        grid.add(smtpProviderCombo, 1, row);
        row++;

        // SMTP Host
        Label smtpHostLabel = new Label("SMTP Host:");
        smtpHostLabel.setStyle("-fx-text-fill: white; -fx-font-weight: 600;");
        TextField smtpHostField = new TextField();
        smtpHostField.setId("smtpHostField");
        smtpHostField.setPromptText("e.g., smtp.gmail.com");
        smtpHostField.setStyle(Styles.INPUT_FIELD);

        // Load existing SMTP host
        if (fullUser != null && fullUser.getSmtpHost() != null) {
            smtpHostField.setText(fullUser.getSmtpHost());
        }

        grid.add(smtpHostLabel, 0, row);
        grid.add(smtpHostField, 1, row);
        row++;

        // SMTP Port
        Label smtpPortLabel = new Label("SMTP Port:");
        smtpPortLabel.setStyle("-fx-text-fill: white; -fx-font-weight: 600;");
        TextField smtpPortField = new TextField("587");
        smtpPortField.setId("smtpPortField");
        smtpPortField.setStyle(Styles.INPUT_FIELD + " -fx-pref-width: 100;");

        // Load existing SMTP port
        if (fullUser != null && fullUser.getSmtpPort() != null) {
            smtpPortField.setText(String.valueOf(fullUser.getSmtpPort()));
        }

        grid.add(smtpPortLabel, 0, row);
        grid.add(smtpPortField, 1, row);
        row++;

        // SMTP Password (App Password) with Test SMTP button
        Label smtpPasswordLabel = new Label("App Password:");
        smtpPasswordLabel.setStyle("-fx-text-fill: white; -fx-font-weight: 600;");

        HBox smtpPasswordBox = new HBox(10);
        smtpPasswordBox.setAlignment(Pos.CENTER_LEFT);

        PasswordField smtpPasswordField = new PasswordField();
        smtpPasswordField.setId("smtpPasswordField");
        smtpPasswordField.setStyle(Styles.INPUT_FIELD);
        smtpPasswordField.setPrefWidth(150);

        // Show appropriate prompt based on whether password exists
        if (fullUser != null && fullUser.getSmtpPassword() != null && !fullUser.getSmtpPassword().isEmpty()) {
            smtpPasswordField.setPromptText("Leave empty to keep current");
        } else {
            smtpPasswordField.setPromptText("Gmail/Outlook App Password");
        }

        // Test SMTP Button - sends actual test email using form settings
        Button testSmtpBtn = new Button("Test SMTP");
        testSmtpBtn.setId("testSmtpBtn");
        testSmtpBtn.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; " +
                "-fx-font-size: 11px; -fx-padding: 5 10; -fx-background-radius: 6; -fx-cursor: hand;");
        testSmtpBtn.setOnAction(e -> {
            // Get values from form
            String email = emailField.getText().trim();
            String host = smtpHostField.getText().trim();
            String portStr = smtpPortField.getText().trim();
            String password = smtpPasswordField.getText();

            // Validate required fields
            if (email.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Please enter an email address first");
                return;
            }
            if (!isValidEmail(email)) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Please enter a valid email address");
                return;
            }
            if (host.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Please enter SMTP host");
                return;
            }
            if (password.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Please enter App Password");
                return;
            }

            int port = 587;
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException ignored) {}

            // Test SMTP connection and send actual test email
            testSmtpSettings(email, host, port, password);
        });

        // Add hover effect
        testSmtpBtn.setOnMouseEntered(ev ->
            testSmtpBtn.setStyle("-fx-background-color: #d97706; -fx-text-fill: white; " +
                "-fx-font-size: 11px; -fx-padding: 5 10; -fx-background-radius: 6; -fx-cursor: hand;"));
        testSmtpBtn.setOnMouseExited(ev ->
            testSmtpBtn.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; " +
                "-fx-font-size: 11px; -fx-padding: 5 10; -fx-background-radius: 6; -fx-cursor: hand;"));

        smtpPasswordBox.getChildren().addAll(smtpPasswordField, testSmtpBtn);

        grid.add(smtpPasswordLabel, 0, row);
        grid.add(smtpPasswordBox, 1, row);
        row++;

        // Help text
        Label smtpHelpLabel = new Label("For Gmail: Use App Password (Google Account > Security > App Passwords)");
        smtpHelpLabel.setStyle("-fx-text-fill: " + Styles.TEXT_SECONDARY + "; -fx-font-size: 10px;");
        smtpHelpLabel.setWrapText(true);
        grid.add(smtpHelpLabel, 0, row, 2, 1);

        // Provider selection auto-fills host
        smtpProviderCombo.setOnAction(e -> {
            String provider = smtpProviderCombo.getValue();
            if (provider != null) {
                switch (provider) {
                    case "Gmail" -> smtpHostField.setText("smtp.gmail.com");
                    case "Outlook" -> smtpHostField.setText("smtp.office365.com");
                    case "Hotmail" -> smtpHostField.setText("smtp-mail.outlook.com");
                    case "Custom" -> smtpHostField.setText("");
                }
                smtpPortField.setText("587");
            }
        });

        // Load existing SMTP settings if editing
        if (existingUser != null) {
            // Need to load from database since ViewModel doesn't have SMTP fields
            try {
                Optional<User> userOpt = authService.getUserById(existingUser.getId());
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    if (user.getSmtpProvider() != null) {
                        String provider = user.getSmtpProvider();
                        provider = provider.substring(0, 1).toUpperCase() + provider.substring(1).toLowerCase();
                        if (smtpProviderCombo.getItems().contains(provider)) {
                            smtpProviderCombo.setValue(provider);
                        } else {
                            smtpProviderCombo.setValue("Custom");
                        }
                    }
                    if (user.getSmtpHost() != null) smtpHostField.setText(user.getSmtpHost());
                    if (user.getSmtpPort() != null) smtpPortField.setText(String.valueOf(user.getSmtpPort()));
                    if (user.getSmtpPassword() != null) smtpPasswordField.setText(user.getSmtpPassword());
                }
            } catch (Exception ex) {
                System.err.println("Error loading SMTP settings: " + ex.getMessage());
            }
        } else {
            smtpProviderCombo.setValue("Gmail");
            smtpHostField.setText("smtp.gmail.com");
        }

        return grid;
    }

    /**
     * Simple email validation
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) return false;
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    /**
     * Test SMTP settings by sending an actual test email using the provided settings
     * This tests the form's SMTP configuration before saving
     */
    private void testSmtpSettings(String email, String smtpHost, int smtpPort, String smtpPassword) {
        // Create a proper Stage-based loading dialog (Alert.close() doesn't work properly without buttons)
        Stage loadingStage = new Stage();
        loadingStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        loadingStage.initOwner(dialogStage);
        loadingStage.initStyle(javafx.stage.StageStyle.UNDECORATED);
        loadingStage.setTitle("Testing SMTP Settings");

        VBox loadingContent = new VBox(15);
        loadingContent.setAlignment(Pos.CENTER);
        loadingContent.setPadding(new Insets(30));
        loadingContent.setStyle("-fx-background-color: " + Styles.BG_PRIMARY + "; -fx-background-radius: 10;");

        // Info icon
        Label iconLabel = new Label("\u2139");
        iconLabel.setStyle("-fx-font-size: 32px; -fx-text-fill: #3b82f6;");

        Label titleLabel = new Label("Testing SMTP Settings");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + Styles.TEXT_PRIMARY + ";");

        Label messageLabel = new Label("Sending test email using your SMTP settings...\n\n" +
                "Email: " + email + "\n" +
                "SMTP Host: " + smtpHost + ":" + smtpPort + "\n\n" +
                "Please wait...");
        messageLabel.setStyle("-fx-text-fill: " + Styles.TEXT_PRIMARY + "; -fx-font-size: 13px;");
        messageLabel.setWrapText(true);

        // Progress indicator
        javafx.scene.control.ProgressIndicator progress = new javafx.scene.control.ProgressIndicator();
        progress.setPrefSize(40, 40);

        loadingContent.getChildren().addAll(iconLabel, titleLabel, messageLabel, progress);

        Scene loadingScene = new Scene(loadingContent, 350, 250);
        loadingStage.setScene(loadingScene);
        loadingStage.show();

        // Center on owner
        if (dialogStage != null) {
            loadingStage.setX(dialogStage.getX() + (dialogStage.getWidth() - 350) / 2);
            loadingStage.setY(dialogStage.getY() + (dialogStage.getHeight() - 250) / 2);
        }

        // Test in background thread
        new Thread(() -> {
            try {
                // Create a temporary mail sender with the form's settings
                org.springframework.mail.javamail.JavaMailSenderImpl mailSender = new org.springframework.mail.javamail.JavaMailSenderImpl();
                mailSender.setHost(smtpHost);
                mailSender.setPort(smtpPort);
                mailSender.setUsername(email);
                mailSender.setPassword(smtpPassword);

                java.util.Properties props = mailSender.getJavaMailProperties();
                props.put("mail.transport.protocol", "smtp");
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.starttls.required", "true");
                props.put("mail.smtp.connectiontimeout", "10000");
                props.put("mail.smtp.timeout", "10000");
                props.put("mail.smtp.writetimeout", "10000");

                // Create and send test email
                jakarta.mail.internet.MimeMessage message = mailSender.createMimeMessage();
                org.springframework.mail.javamail.MimeMessageHelper helper =
                    new org.springframework.mail.javamail.MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(email, "MagicTech SMTP Test");
                helper.setTo(email);
                helper.setSubject("MagicTech - SMTP Test Successful!");

                String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                String htmlContent = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <style>
                            body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f4f4f4; }
                            .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; padding: 30px; border-radius: 10px; }
                            .header { background: linear-gradient(135deg, #f59e0b 0%%, #d97706 100%%); color: white; padding: 20px; border-radius: 10px 10px 0 0; margin: -30px -30px 30px -30px; text-align: center; }
                            .success-icon { font-size: 48px; margin-bottom: 10px; }
                            h1 { margin: 0; font-size: 24px; }
                            .content { color: #333; line-height: 1.6; }
                            .info-box { background-color: #fef3c7; border-left: 4px solid #f59e0b; padding: 15px; margin: 20px 0; }
                            .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; color: #666; font-size: 12px; text-align: center; }
                        </style>
                    </head>
                    <body>
                        <div class="container">
                            <div class="header">
                                <div class="success-icon">✉️</div>
                                <h1>SMTP Test Successful!</h1>
                            </div>
                            <div class="content">
                                <p>Congratulations! Your SMTP settings are working correctly.</p>
                                <div class="info-box">
                                    <strong>Test Details:</strong><br>
                                    Email: %s<br>
                                    SMTP Host: %s:%d<br>
                                    Timestamp: %s
                                </div>
                                <p>You can now save your settings and send emails from the application.</p>
                            </div>
                            <div class="footer">
                                <p>MagicTech Management System - SMTP Configuration Test</p>
                            </div>
                        </div>
                    </body>
                    </html>
                    """.formatted(email, smtpHost, smtpPort, timestamp);

                helper.setText(htmlContent, true);
                mailSender.send(message);

                Platform.runLater(() -> {
                    loadingStage.close();
                    showAlert(Alert.AlertType.INFORMATION, "SMTP Test Successful!",
                        "Test email sent successfully!\n\n" +
                        "Email: " + email + "\n" +
                        "SMTP Host: " + smtpHost + ":" + smtpPort + "\n\n" +
                        "Please check your inbox (and spam folder) for the test email.\n\n" +
                        "Your SMTP settings are working correctly!");
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    loadingStage.close();
                    showSmtpErrorDialog(email, smtpHost, smtpPort, e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Show SMTP error dialog with troubleshooting tips
     */
    private void showSmtpErrorDialog(String email, String smtpHost, int smtpPort, String errorMessage) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("SMTP Test Failed");
        alert.setHeaderText("Failed to send test email");
        alert.initOwner(dialogStage);

        String content = "Error: " + errorMessage + "\n\n" +
            "SETTINGS USED:\n" +
            "• Email: " + email + "\n" +
            "• SMTP Host: " + smtpHost + "\n" +
            "• SMTP Port: " + smtpPort + "\n\n" +
            "TROUBLESHOOTING:\n\n" +
            "FOR GMAIL:\n" +
            "1. Enable 2-Factor Authentication\n" +
            "2. Generate App Password at:\n" +
            "   Google Account > Security > App Passwords\n" +
            "3. Use smtp.gmail.com port 587\n\n" +
            "FOR OUTLOOK:\n" +
            "1. Use smtp.office365.com port 587\n" +
            "2. Use your regular password or App Password\n\n" +
            "GENERAL:\n" +
            "• Check firewall isn't blocking port " + smtpPort + "\n" +
            "• Verify internet connection\n" +
            "• Make sure email/password are correct";

        TextArea textArea = new TextArea(content);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefWidth(500);
        textArea.setPrefHeight(350);
        textArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12px; " +
                "-fx-control-inner-background: " + Styles.BG_SECONDARY + "; -fx-text-fill: white;");

        alert.getDialogPane().setContent(textArea);
        alert.getDialogPane().setStyle("-fx-background-color: " + Styles.BG_PRIMARY + ";");
        alert.getDialogPane().setPrefWidth(550);

        alert.showAndWait();
    }

    // ================================
    // CRUD OPERATIONS
    // ================================

    private void createUser(UserFormData data) {
        if (data.username == null || data.username.trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Username cannot be empty");
            return;
        }

        if (data.password == null || data.password.trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Password cannot be empty");
            return;
        }

        if (data.role == null) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Please select a role");
            return;
        }

        // Validate email format if provided
        if (data.email != null && !data.email.trim().isEmpty() && !isValidEmail(data.email.trim())) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Please enter a valid email address");
            return;
        }

        try {
            User newUser = new User();
            newUser.setUsername(data.username.trim());
            newUser.setPassword(data.password);
            newUser.setRole(data.role);
            newUser.setActive(true);

            // Set email if provided
            if (data.email != null && !data.email.trim().isEmpty()) {
                newUser.setEmail(data.email.trim());
            }

            // Set SMTP settings if provided
            if (data.hasSmtpConfig()) {
                newUser.setSmtpProvider(data.smtpProvider);
                newUser.setSmtpHost(data.smtpHost);
                newUser.setSmtpPort(data.smtpPort);
                newUser.setSmtpPassword(data.smtpPassword);
                newUser.setSmtpConfigured(true);
            }

            authService.createUser(newUser);
            showAlert(Alert.AlertType.INFORMATION, "Success", "User '" + data.username + "' created successfully!");
            loadUsers();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to create user: " + e.getMessage());
        }
    }

    private void updateUser(Long userId, UserFormData data) {
        // Validate email format if provided
        if (data.email != null && !data.email.trim().isEmpty() && !isValidEmail(data.email.trim())) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Please enter a valid email address");
            return;
        }

        try {
            Optional<User> userOpt = authService.getUserById(userId);
            if (userOpt.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Error", "User not found");
                return;
            }

            User user = userOpt.get();

            if (data.password != null && !data.password.trim().isEmpty()) {
                user.setPassword(data.password);
            }

            if (data.role != null) {
                user.setRole(data.role);
            }

            // Update email (can be set to null/empty to remove)
            if (data.email != null && !data.email.trim().isEmpty()) {
                user.setEmail(data.email.trim());
            } else {
                user.setEmail(null);
            }

            // Update SMTP settings
            // Always update provider, host, and port if provided
            if (data.smtpProvider != null && !data.smtpProvider.isEmpty()) {
                user.setSmtpProvider(data.smtpProvider);
            }
            if (data.smtpHost != null && !data.smtpHost.isEmpty()) {
                user.setSmtpHost(data.smtpHost);
            }
            if (data.smtpPort != null) {
                user.setSmtpPort(data.smtpPort);
            }

            // Only update password if a new one is provided (leave empty to keep existing)
            if (data.smtpPassword != null && !data.smtpPassword.isEmpty()) {
                user.setSmtpPassword(data.smtpPassword);
            }

            // Calculate smtpConfigured based on all required fields being present
            boolean hasEmail = user.getEmail() != null && !user.getEmail().isEmpty();
            boolean hasHost = user.getSmtpHost() != null && !user.getSmtpHost().isEmpty();
            boolean hasPassword = user.getSmtpPassword() != null && !user.getSmtpPassword().isEmpty();
            user.setSmtpConfigured(hasEmail && hasHost && hasPassword);

            System.out.println("📧 SMTP Update: email=" + hasEmail + ", host=" + hasHost +
                ", password=" + hasPassword + ", configured=" + user.getSmtpConfigured());

            authService.updateUser(user);
            showAlert(Alert.AlertType.INFORMATION, "Success", "User updated successfully!");
            loadUsers();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to update user: " + e.getMessage());
        }
    }

    private void toggleUserStatus(UserViewModel userVM) {
        try {
            Optional<User> userOpt = authService.getUserById(userVM.getId());
            if (userOpt.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Error", "User not found");
                return;
            }

            User user = userOpt.get();
            boolean newStatus = !user.getActive();

            if (newStatus) {
                authService.activateUser(user.getId());
                showAlert(Alert.AlertType.INFORMATION, "Success", "User '" + user.getUsername() + "' activated");
            } else {
                authService.deactivateUser(user.getId());
                showAlert(Alert.AlertType.INFORMATION, "Success", "User '" + user.getUsername() + "' deactivated");
            }

            loadUsers();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to update user status: " + e.getMessage());
        }
    }

    private void handleDeleteUser(UserViewModel userVM) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Delete User");
        confirmAlert.setHeaderText("⚠️ Permanently Delete User");
        confirmAlert.setContentText("Are you sure you want to permanently delete user '" + userVM.getUsername() +
                "'?\n\nThis action cannot be undone!");
        confirmAlert.initOwner(dialogStage);

        DialogPane dialogPane = confirmAlert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: " + Styles.BG_PRIMARY + ";");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                authService.deleteUser(userVM.getId());
                showAlert(Alert.AlertType.INFORMATION, "Success", "User deleted permanently");
                loadUsers();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete user: " + e.getMessage());
            }
        }
    }

    /**
     * Test email for a specific user using their own SMTP settings
     */
    private void testUserEmail(UserViewModel userVM) {
        try {
            Optional<User> userOpt = authService.getUserById(userVM.getId());
            if (userOpt.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Error", "User not found");
                return;
            }

            User user = userOpt.get();

            // Check if user has email configured
            if (user.getEmail() == null || user.getEmail().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "No Email",
                    "User '" + user.getUsername() + "' does not have an email address configured.\n\n" +
                    "Please edit the user and add an email address first.");
                return;
            }

            // Check if user has SMTP configured
            if (!user.hasSmtpConfigured()) {
                showAlert(Alert.AlertType.WARNING, "Email Not Configured",
                    "User '" + user.getUsername() + "' has not configured SMTP email settings.\n\n" +
                    "Please edit the user and configure:\n" +
                    "• Email Provider (Gmail/Outlook)\n" +
                    "• SMTP Host\n" +
                    "• App Password\n\n" +
                    "For Gmail: Use App Password from Google Account > Security > App Passwords");
                return;
            }

            // Show loading indicator
            Alert loadingAlert = new Alert(Alert.AlertType.INFORMATION);
            loadingAlert.setTitle("Testing Email");
            loadingAlert.setHeaderText(null);
            loadingAlert.setContentText("Sending test email for: " + user.getUsername() + "\nTo: " + user.getEmail() + "\nPlease wait...");
            loadingAlert.initOwner(dialogStage);
            loadingAlert.getDialogPane().setStyle("-fx-background-color: " + Styles.BG_PRIMARY + ";");
            loadingAlert.getButtonTypes().clear();
            loadingAlert.show();

            // Send test email in background
            new Thread(() -> {
                try {
                    EmailService.TestEmailResult result = emailService.sendTestEmailForUser(user);

                    Platform.runLater(() -> {
                        loadingAlert.close();

                        if (result.isSuccess()) {
                            showAlert(Alert.AlertType.INFORMATION, "Success",
                                "Test email sent successfully!\n\n" +
                                "User: " + user.getUsername() + "\n" +
                                "Email: " + user.getEmail() + "\n" +
                                "SMTP Host: " + result.getSmtpHost() + "\n\n" +
                                "Please check inbox (and spam folder).");
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Email Test Failed",
                                "Failed to send test email:\n\n" + result.getMessage() + "\n\n" +
                                "Please check SMTP settings for this user.");
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        loadingAlert.close();
                        showAlert(Alert.AlertType.ERROR, "Error", "Failed to test email: " + e.getMessage());
                    });
                }
            }).start();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to test email: " + e.getMessage());
        }
    }

    // ================================
    // UTILITY METHODS
    // ================================

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(dialogStage);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: " + Styles.BG_PRIMARY + ";");

        alert.showAndWait();
    }

    private void addHoverEffect(Button button, String normalStyle, String hoverColor) {
        button.setOnMouseEntered(e ->
            button.setStyle(normalStyle.replace("-fx-background-color: #", "-fx-background-color: " + hoverColor + "; -fx-background-color: #").replace("; -fx-background-color: #" + hoverColor.substring(1), ""))
        );
        button.setOnMouseExited(e -> button.setStyle(normalStyle));
    }

    // ================================
    // VIEW MODEL
    // ================================

    public static class UserViewModel {
        private final LongProperty id = new SimpleLongProperty();
        private final StringProperty username = new SimpleStringProperty();
        private final StringProperty email = new SimpleStringProperty();
        private final ObjectProperty<UserRole> role = new SimpleObjectProperty<>();
        private final StringProperty roleDisplay = new SimpleStringProperty();
        private final ObjectProperty<java.time.LocalDateTime> lastLogin = new SimpleObjectProperty<>();
        private final StringProperty lastLoginFormatted = new SimpleStringProperty();
        private final BooleanProperty active = new SimpleBooleanProperty();
        private final BooleanProperty smtpConfigured = new SimpleBooleanProperty();

        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        public UserViewModel(User user) {
            this.id.set(user.getId());
            this.username.set(user.getUsername());
            this.email.set(user.getEmail() != null ? user.getEmail() : "");
            this.role.set(user.getRole());
            this.roleDisplay.set(user.getRole().getDisplayName());
            this.lastLogin.set(user.getLastLogin());

            if (user.getLastLogin() != null) {
                this.lastLoginFormatted.set(user.getLastLogin().format(FORMATTER));
            } else {
                this.lastLoginFormatted.set("Never");
            }

            this.active.set(user.getActive());
            this.smtpConfigured.set(user.hasSmtpConfigured());
        }

        // Getters
        public Long getId() { return id.get(); }
        public String getUsername() { return username.get(); }
        public String getEmail() { return email.get(); }
        public UserRole getRole() { return role.get(); }
        public String getRoleDisplay() { return roleDisplay.get(); }
        public String getLastLoginFormatted() { return lastLoginFormatted.get(); }
        public Boolean isActive() { return active.get(); }
        public Boolean isSmtpConfigured() { return smtpConfigured.get(); }

        // Properties
        public LongProperty idProperty() { return id; }
        public StringProperty usernameProperty() { return username; }
        public StringProperty emailProperty() { return email; }
        public ObjectProperty<UserRole> roleProperty() { return role; }
        public StringProperty roleDisplayProperty() { return roleDisplay; }
        public StringProperty lastLoginFormattedProperty() { return lastLoginFormatted; }
        public BooleanProperty activeProperty() { return active; }
        public BooleanProperty smtpConfiguredProperty() { return smtpConfigured; }
    }

    // ================================
    // FORM DATA
    // ================================

    private static class UserFormData {
        final String username;
        final String password;
        final String email;
        final UserRole role;
        // SMTP fields
        final String smtpProvider;
        final String smtpHost;
        final Integer smtpPort;
        final String smtpPassword;

        UserFormData(String username, String password, String email, UserRole role) {
            this(username, password, email, role, null, null, null, null);
        }

        UserFormData(String username, String password, String email, UserRole role,
                     String smtpProvider, String smtpHost, Integer smtpPort, String smtpPassword) {
            this.username = username;
            this.password = password;
            this.email = email;
            this.role = role;
            this.smtpProvider = smtpProvider;
            this.smtpHost = smtpHost;
            this.smtpPort = smtpPort;
            this.smtpPassword = smtpPassword;
        }

        boolean hasSmtpConfig() {
            return smtpHost != null && !smtpHost.isEmpty() &&
                   smtpPassword != null && !smtpPassword.isEmpty();
        }
    }
}
