package com.magictech.core.ui.controllers;

import com.magictech.core.auth.AuthenticationService;
import com.magictech.core.auth.User;
import com.magictech.core.auth.UserRole;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

/**
 * User Management Controller
 * Admin interface for managing users and roles
 * Only accessible to MASTER role users
 */
@Component
public class UserManagementController {

    @Autowired
    private AuthenticationService authService;

    private TableView<UserViewModel> userTable;
    private ObservableList<UserViewModel> users;
    private Stage dialogStage;

    /**
     * Show user management dialog
     */
    public void showUserManagement(Stage owner) {
        dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(owner);
        dialogStage.initStyle(StageStyle.DECORATED);
        dialogStage.setTitle("User Management - Admin Panel");

        VBox root = createMainLayout();

        Scene scene = new Scene(root, 900, 600);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        
        dialogStage.setScene(scene);
        loadUsers();
        dialogStage.showAndWait();
    }

    private VBox createMainLayout() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #1e293b;");

        // Header
        HBox header = createHeader();
        
        // Toolbar
        HBox toolbar = createToolbar();
        
        // User table
        userTable = createUserTable();
        VBox.setVgrow(userTable, Priority.ALWAYS);
        
        root.getChildren().addAll(header, toolbar, userTable);
        return root;
    }

    private HBox createHeader() {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("👥");
        icon.setStyle("-fx-font-size: 32px;");

        VBox titleBox = new VBox(5);
        Label title = new Label("User Management");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        
        Label subtitle = new Label("Manage system users and assign roles");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: rgba(255, 255, 255, 0.7);");
        
        titleBox.getChildren().addAll(title, subtitle);
        header.getChildren().addAll(icon, titleBox);

        return header;
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Button addUserBtn = new Button("➕ Add New User");
        addUserBtn.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; " +
                "-fx-font-size: 14px; -fx-font-weight: 600; -fx-padding: 10 20; " +
                "-fx-background-radius: 8; -fx-cursor: hand;");
        addUserBtn.setOnAction(e -> showAddUserDialog());

        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.setStyle("-fx-background-color: #475569; -fx-text-fill: white; " +
                "-fx-font-size: 14px; -fx-font-weight: 600; -fx-padding: 10 20; " +
                "-fx-background-radius: 8; -fx-cursor: hand;");
        refreshBtn.setOnAction(e -> loadUsers());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label infoLabel = new Label("Total Users: ");
        infoLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 14px;");

        Label countLabel = new Label("0");
        countLabel.setStyle("-fx-text-fill: #8b5cf6; -fx-font-weight: bold; -fx-font-size: 14px;");
        countLabel.textProperty().bind(javafx.beans.binding.Bindings.size(
                userTable != null && userTable.getItems() != null ? 
                userTable.getItems() : FXCollections.observableArrayList()
        ).asString());

        toolbar.getChildren().addAll(addUserBtn, refreshBtn, spacer, infoLabel, countLabel);
        return toolbar;
    }

    private TableView<UserViewModel> createUserTable() {
        TableView<UserViewModel> table = new TableView<>();
        table.setStyle("-fx-background-color: #0f172a; -fx-text-fill: white;");
        
        users = FXCollections.observableArrayList();
        table.setItems(users);

        // ID Column
        TableColumn<UserViewModel, Long> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(60);
        idCol.setStyle("-fx-alignment: CENTER;");

        // Username Column
        TableColumn<UserViewModel, String> usernameCol = new TableColumn<>("Username");
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        usernameCol.setPrefWidth(150);

        // Role Column
        TableColumn<UserViewModel, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(new PropertyValueFactory<>("roleDisplay"));
        roleCol.setPrefWidth(180);
        roleCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; " +
                            "-fx-padding: 4 12; -fx-background-radius: 12; -fx-font-size: 12px; " +
                            "-fx-font-weight: 600;");
                    setGraphic(badge);
                }
            }
        });

        // Last Login Column
        TableColumn<UserViewModel, String> lastLoginCol = new TableColumn<>("Last Login");
        lastLoginCol.setCellValueFactory(new PropertyValueFactory<>("lastLoginFormatted"));
        lastLoginCol.setPrefWidth(180);

        // Status Column
        TableColumn<UserViewModel, Boolean> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("active"));
        statusCol.setPrefWidth(100);
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label status = new Label(item ? "✅ Active" : "❌ Inactive");
                    status.setStyle("-fx-text-fill: " + (item ? "#86efac" : "#fca5a5") + "; " +
                            "-fx-font-weight: 600;");
                    setGraphic(status);
                }
            }
        });

        // Actions Column
        TableColumn<UserViewModel, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(200);
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("✏️ Edit");
            private final Button toggleBtn = new Button("🔄");
            private final HBox container = new HBox(8, editBtn, toggleBtn);

            {
                editBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; " +
                        "-fx-font-size: 12px; -fx-padding: 5 10; -fx-background-radius: 6; -fx-cursor: hand;");
                toggleBtn.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; " +
                        "-fx-font-size: 12px; -fx-padding: 5 10; -fx-background-radius: 6; -fx-cursor: hand;");
                container.setAlignment(Pos.CENTER);

                editBtn.setOnAction(e -> {
                    UserViewModel user = getTableView().getItems().get(getIndex());
                    showEditUserDialog(user);
                });

                toggleBtn.setOnAction(e -> {
                    UserViewModel user = getTableView().getItems().get(getIndex());
                    toggleUserStatus(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    UserViewModel user = getTableView().getItems().get(getIndex());
                    toggleBtn.setText(user.isActive() ? "❌ Deactivate" : "✅ Activate");
                    setGraphic(container);
                }
            }
        });

        table.getColumns().addAll(idCol, usernameCol, roleCol, lastLoginCol, statusCol, actionsCol);
        return table;
    }

    private void loadUsers() {
        users.clear();
        List<User> allUsers = authService.getAllUsers();
        for (User user : allUsers) {
            users.add(new UserViewModel(user));
        }
    }

    private void showAddUserDialog() {
        Dialog<UserFormData> dialog = new Dialog<>();
        dialog.setTitle("Add New User");
        dialog.setHeaderText("Create a new user account");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogPane.setStyle("-fx-background-color: #1e293b;");

        GridPane grid = createUserForm(null);
        dialogPane.setContent(grid);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                TextField usernameField = (TextField) grid.lookup("#usernameField");
                PasswordField passwordField = (PasswordField) grid.lookup("#passwordField");
                ComboBox<UserRole> roleCombo = (ComboBox<UserRole>) grid.lookup("#roleCombo");

                return new UserFormData(
                        usernameField.getText(),
                        passwordField.getText(),
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
        dialog.setHeaderText("Update user information");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogPane.setStyle("-fx-background-color: #1e293b;");

        GridPane grid = createUserForm(userVM);
        dialogPane.setContent(grid);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                PasswordField passwordField = (PasswordField) grid.lookup("#passwordField");
                ComboBox<UserRole> roleCombo = (ComboBox<UserRole>) grid.lookup("#roleCombo");

                String newPassword = passwordField.getText();
                return new UserFormData(
                        userVM.getUsername(),
                        newPassword.isEmpty() ? null : newPassword,
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
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));
        grid.setStyle("-fx-background-color: #1e293b;");

        int row = 0;

        // Username field
        Label usernameLabel = new Label("Username:");
        usernameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: 600;");
        TextField usernameField = new TextField();
        usernameField.setId("usernameField");
        usernameField.setPromptText("Enter username");
        usernameField.setStyle("-fx-background-color: #0f172a; -fx-text-fill: white; " +
                "-fx-prompt-text-fill: rgba(255, 255, 255, 0.5); -fx-pref-width: 250;");
        
        if (existingUser != null) {
            usernameField.setText(existingUser.getUsername());
            usernameField.setDisable(true);
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
        passwordField.setStyle("-fx-background-color: #0f172a; -fx-text-fill: white; " +
                "-fx-prompt-text-fill: rgba(255, 255, 255, 0.5); -fx-pref-width: 250;");

        grid.add(passwordLabel, 0, row);
        grid.add(passwordField, 1, row);
        row++;

        // Role selection
        Label roleLabel = new Label("Role:");
        roleLabel.setStyle("-fx-text-fill: white; -fx-font-weight: 600;");
        ComboBox<UserRole> roleCombo = new ComboBox<>();
        roleCombo.setId("roleCombo");
        roleCombo.getItems().addAll(UserRole.values());
        roleCombo.setStyle("-fx-background-color: #0f172a; -fx-text-fill: white;");
        roleCombo.setPrefWidth(250);
        
        if (existingUser != null) {
            roleCombo.setValue(existingUser.getRole());
        } else {
            roleCombo.setValue(UserRole.STORAGE);
        }

        grid.add(roleLabel, 0, row);
        grid.add(roleCombo, 1, row);

        return grid;
    }

    private void createUser(UserFormData data) {
        if (data.username == null || data.username.trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Username cannot be empty");
            return;
        }

        if (data.password == null || data.password.trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Password cannot be empty");
            return;
        }

        if (data.role == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please select a role");
            return;
        }

        try {
            User newUser = new User();
            newUser.setUsername(data.username.trim());
            newUser.setPassword(data.password);
            newUser.setRole(data.role);
            newUser.setActive(true);

            authService.createUser(newUser);
            showAlert(Alert.AlertType.INFORMATION, "Success", "User created successfully");
            loadUsers();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to create user: " + e.getMessage());
        }
    }

    private void updateUser(Long userId, UserFormData data) {
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

            authService.updateUser(user);
            showAlert(Alert.AlertType.INFORMATION, "Success", "User updated successfully");
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
                showAlert(Alert.AlertType.INFORMATION, "Success", "User activated");
            } else {
                authService.deactivateUser(user.getId());
                showAlert(Alert.AlertType.INFORMATION, "Success", "User deactivated");
            }
            
            loadUsers();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to update user status: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white;");
        
        alert.showAndWait();
    }

    // ViewModel for UserTable
    public static class UserViewModel {
        private final LongProperty id = new SimpleLongProperty();
        private final StringProperty username = new SimpleStringProperty();
        private final ObjectProperty<UserRole> role = new SimpleObjectProperty<>();
        private final StringProperty roleDisplay = new SimpleStringProperty();
        private final ObjectProperty<java.time.LocalDateTime> lastLogin = new SimpleObjectProperty<>();
        private final StringProperty lastLoginFormatted = new SimpleStringProperty();
        private final BooleanProperty active = new SimpleBooleanProperty();

        public UserViewModel(User user) {
            this.id.set(user.getId());
            this.username.set(user.getUsername());
            this.role.set(user.getRole());
            this.roleDisplay.set(user.getRole().getDisplayName());
            this.lastLogin.set(user.getLastLogin());
            
            if (user.getLastLogin() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                this.lastLoginFormatted.set(user.getLastLogin().format(formatter));
            } else {
                this.lastLoginFormatted.set("Never");
            }
            
            this.active.set(user.getActive());
        }

        // Getters
        public Long getId() { return id.get(); }
        public String getUsername() { return username.get(); }
        public UserRole getRole() { return role.get(); }
        public String getRoleDisplay() { return roleDisplay.get(); }
        public String getLastLoginFormatted() { return lastLoginFormatted.get(); }
        public Boolean isActive() { return active.get(); }

        // Properties
        public LongProperty idProperty() { return id; }
        public StringProperty usernameProperty() { return username; }
        public ObjectProperty<UserRole> roleProperty() { return role; }
        public StringProperty roleDisplayProperty() { return roleDisplay; }
        public StringProperty lastLoginFormattedProperty() { return lastLoginFormatted; }
        public BooleanProperty activeProperty() { return active; }
    }

    // Form data holder
    private static class UserFormData {
        final String username;
        final String password;
        final UserRole role;

        UserFormData(String username, String password, UserRole role) {
            this.username = username;
            this.password = password;
            this.role = role;
        }
    }
}
