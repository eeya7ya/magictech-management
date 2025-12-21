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
     * Custom cell for action buttons
     */
    private class ActionButtonsCell extends TableCell<UserViewModel, Void> {
        private final Button editBtn = new Button("✏️ Edit");
        private final Button toggleBtn = new Button();
        private final Button deleteBtn = new Button("🗑️");
        private final HBox container = new HBox(6);

        public ActionButtonsCell() {
            editBtn.setStyle(Styles.BTN_INFO);
            toggleBtn.setStyle(Styles.BTN_WARNING);
            deleteBtn.setStyle(Styles.BTN_DANGER);
            container.setAlignment(Pos.CENTER);

            editBtn.setOnAction(e -> {
                UserViewModel user = getTableView().getItems().get(getIndex());
                showEditUserDialog(user);
            });

            toggleBtn.setOnAction(e -> {
                UserViewModel user = getTableView().getItems().get(getIndex());
                toggleUserStatus(user);
            });

            deleteBtn.setOnAction(e -> {
                UserViewModel user = getTableView().getItems().get(getIndex());
                handleDeleteUser(user);
            });

            container.getChildren().addAll(editBtn, toggleBtn, deleteBtn);
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

                return new UserFormData(
                        usernameField.getText(),
                        passwordField.getText(),
                        emailField.getText(),
                        roleCombo.getValue()
                );
            }
            return null;
        });

        Optional<UserFormData> result = dialog.showAndWait();
        result.ifPresent(this::createUser);
    }

    private void showEditUserDialog(UserViewModel userVM) {
        Dialog<UserFormData> dialog = new Dialog<>();
        dialog.setTitle("Edit User");
        dialog.setHeaderText("Update user information for: " + userVM.getUsername());
        dialog.initOwner(dialogStage);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogPane.setStyle("-fx-background-color: " + Styles.BG_PRIMARY + ";");
        dialogPane.setPrefWidth(400);

        GridPane grid = createUserForm(userVM);
        dialogPane.setContent(grid);

        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        okButton.setStyle(Styles.BTN_PRIMARY);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                PasswordField passwordField = (PasswordField) grid.lookup("#passwordField");
                TextField emailField = (TextField) grid.lookup("#emailField");
                @SuppressWarnings("unchecked")
                ComboBox<UserRole> roleCombo = (ComboBox<UserRole>) grid.lookup("#roleCombo");

                String newPassword = passwordField.getText();
                return new UserFormData(
                        userVM.getUsername(),
                        newPassword.isEmpty() ? null : newPassword,
                        emailField.getText(),
                        roleCombo.getValue()
                );
            }
            return null;
        });

        Optional<UserFormData> result = dialog.showAndWait();
        result.ifPresent(data -> updateUser(userVM.getId(), data));
    }

    private GridPane createUserForm(UserViewModel existingUser) {
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

        // Email field with test button
        Label emailLabel = new Label("Email:");
        emailLabel.setStyle("-fx-text-fill: white; -fx-font-weight: 600;");

        HBox emailBox = new HBox(10);
        emailBox.setAlignment(Pos.CENTER_LEFT);

        TextField emailField = new TextField();
        emailField.setId("emailField");
        emailField.setPromptText("Enter email address");
        emailField.setStyle(Styles.INPUT_FIELD);
        emailField.setPrefWidth(180);

        if (existingUser != null && existingUser.getEmail() != null) {
            emailField.setText(existingUser.getEmail());
        }

        // Test Email Button
        Button testEmailBtn = new Button("Test Email");
        testEmailBtn.setId("testEmailBtn");
        testEmailBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: white; " +
                "-fx-font-size: 11px; -fx-padding: 5 10; -fx-background-radius: 6; -fx-cursor: hand;");
        testEmailBtn.setOnAction(e -> {
            String email = emailField.getText().trim();
            if (email.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Please enter an email address first");
                return;
            }
            if (!isValidEmail(email)) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Please enter a valid email address");
                return;
            }
            sendTestEmail(email);
        });

        // Add hover effect
        testEmailBtn.setOnMouseEntered(e ->
            testEmailBtn.setStyle("-fx-background-color: #047857; -fx-text-fill: white; " +
                "-fx-font-size: 11px; -fx-padding: 5 10; -fx-background-radius: 6; -fx-cursor: hand;"));
        testEmailBtn.setOnMouseExited(e ->
            testEmailBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: white; " +
                "-fx-font-size: 11px; -fx-padding: 5 10; -fx-background-radius: 6; -fx-cursor: hand;"));

        emailBox.getChildren().addAll(emailField, testEmailBtn);

        grid.add(emailLabel, 0, row);
        grid.add(emailBox, 1, row);
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
     * Send a test email to verify the email address works
     */
    private void sendTestEmail(String email) {
        // Show loading indicator
        Alert loadingAlert = new Alert(Alert.AlertType.INFORMATION);
        loadingAlert.setTitle("Sending Test Email");
        loadingAlert.setHeaderText(null);
        loadingAlert.setContentText("Sending test email to: " + email + "\nPlease wait...");
        loadingAlert.initOwner(dialogStage);
        loadingAlert.getDialogPane().setStyle("-fx-background-color: " + Styles.BG_PRIMARY + ";");

        // Remove buttons to prevent closing
        loadingAlert.getButtonTypes().clear();
        loadingAlert.show();

        // Send email in background thread
        new Thread(() -> {
            try {
                EmailService.TestEmailResult result = emailService.sendTestEmail(email);

                Platform.runLater(() -> {
                    loadingAlert.close();

                    if (result.isSuccess()) {
                        showAlert(Alert.AlertType.INFORMATION, "Success",
                            "Test email sent successfully!\n\n" +
                            "Recipient: " + result.getRecipient() + "\n" +
                            "SMTP Host: " + result.getSmtpHost() + "\n\n" +
                            "Please check your inbox (and spam folder).");
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Email Failed",
                            "Failed to send test email:\n\n" + result.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    loadingAlert.close();
                    showAlert(Alert.AlertType.ERROR, "Error",
                        "Error sending test email:\n" + e.getMessage());
                });
            }
        }).start();
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
        }

        // Getters
        public Long getId() { return id.get(); }
        public String getUsername() { return username.get(); }
        public String getEmail() { return email.get(); }
        public UserRole getRole() { return role.get(); }
        public String getRoleDisplay() { return roleDisplay.get(); }
        public String getLastLoginFormatted() { return lastLoginFormatted.get(); }
        public Boolean isActive() { return active.get(); }

        // Properties
        public LongProperty idProperty() { return id; }
        public StringProperty usernameProperty() { return username; }
        public StringProperty emailProperty() { return email; }
        public ObjectProperty<UserRole> roleProperty() { return role; }
        public StringProperty roleDisplayProperty() { return roleDisplay; }
        public StringProperty lastLoginFormattedProperty() { return lastLoginFormatted; }
        public BooleanProperty activeProperty() { return active; }
    }

    // ================================
    // FORM DATA
    // ================================

    private static class UserFormData {
        final String username;
        final String password;
        final String email;
        final UserRole role;

        UserFormData(String username, String password, String email, UserRole role) {
            this.username = username;
            this.password = password;
            this.email = email;
            this.role = role;
        }
    }
}
