package com.magictech.core.module;

import com.magictech.core.auth.User;
import javafx.scene.layout.BorderPane;

/**
 * Base abstract controller class for all module controllers.
 * Provides common functionality and structure for module implementations.
 */
public abstract class BaseModuleController {

    protected BorderPane rootPane;
    protected User currentUser;
    protected ModuleConfig config;

    /**
     * Initialize the module with user and configuration
     */
    public void initialize(User user, ModuleConfig config) {
        this.currentUser = user;
        this.config = config;
        setupUI();
        loadData();
    }

    /**
     * Setup the user interface components
     * To be implemented by each specific module
     */
    protected abstract void setupUI();

    /**
     * Load initial data for the module
     * To be implemented by each specific module
     */
    protected abstract void loadData();

    /**
     * Refresh the module data
     */
    public abstract void refresh();

    /**
     * Clean up resources when module is closed
     */
    public void cleanup() {
        // Default implementation - can be overridden
        System.out.println("Cleaning up module: " + config.getName());
    }

    /**
     * Get the root pane of this module
     */
    public BorderPane getRootPane() {
        if (rootPane == null) {
            rootPane = new BorderPane();
            rootPane.getStyleClass().add("module-root");
        }
        return rootPane;
    }

    /**
     * Show success message
     */
    protected void showSuccess(String message) {
        showAlert("Success", message, javafx.scene.control.Alert.AlertType.INFORMATION);
    }

    /**
     * Show error message
     */
    protected void showError(String message) {
        showAlert("Error", message, javafx.scene.control.Alert.AlertType.ERROR);
    }

    /**
     * Show warning message
     */
    protected void showWarning(String message) {
        showAlert("Warning", message, javafx.scene.control.Alert.AlertType.WARNING);
    }

    /**
     * Generic alert method
     */
    private void showAlert(String title, String message, javafx.scene.control.Alert.AlertType type) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}