package com.magictech.modules.maintenance;

import com.magictech.modules.storage.base.BaseStorageModuleController;
import com.magictech.modules.storage.config.ModuleStorageConfig;
import com.magictech.modules.storage.entity.StorageItem;
import com.magictech.modules.storage.model.StorageItemViewModel;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Maintenance Storage Controller
 * Shows: ID, Manufacture, Product Name, Code, Serial Number, Availability
 * NO PRICE COLUMN
 */
@Component
public class MaintenanceStorageController extends BaseStorageModuleController {

    @Override
    protected ModuleStorageConfig getModuleConfig() {
        return ModuleStorageConfig.MAINTENANCE;
    }

    @Override
    protected String getHeaderColor() {
        return "linear-gradient(to right, #22c55e, #16a34a)";
    }

    @Override
    protected String getModuleIcon() {
        return "🔧";
    }

    @Override
    protected void handleAddItem() {
        Dialog<StorageItemViewModel> dialog = createItemDialog(null);
        Optional<StorageItemViewModel> result = dialog.showAndWait();

        result.ifPresent(vm -> {
            showLoading(true);

            Task<StorageItem> saveTask = new Task<>() {
                @Override
                protected StorageItem call() {
                    return storageService.createItem(convertToEntity(vm));
                }
            };

            saveTask.setOnSucceeded(e -> {
                StorageItem saved = saveTask.getValue();
                StorageItemViewModel savedVM = convertToViewModel(saved);

                Platform.runLater(() -> {
                    storageItems.add(savedVM);
                    updateEmptyState();
                    showLoading(false);
                    showSuccess("✓ Maintenance item added!");
                    storageTable.scrollTo(savedVM);
                });
            });

            saveTask.setOnFailed(e -> {
                showLoading(false);
                showError("Failed to save: " + saveTask.getException().getMessage());
            });

            new Thread(saveTask).start();
        });
    }

    @Override
    protected void handleEditItem() {
        List<StorageItemViewModel> selected = getSelectedItems();

        if (selected.isEmpty()) {
            showWarning("Please select an item to edit");
            return;
        }

        if (selected.size() > 1) {
            showWarning("Please select only ONE item to edit");
            return;
        }

        StorageItemViewModel item = selected.get(0);
        Dialog<StorageItemViewModel> dialog = createItemDialog(item);
        Optional<StorageItemViewModel> result = dialog.showAndWait();

        result.ifPresent(updated -> {
            showLoading(true);

            Task<StorageItem> updateTask = new Task<>() {
                @Override
                protected StorageItem call() {
                    return storageService.updateItem(item.getId(), convertToEntity(updated));
                }
            };

            updateTask.setOnSucceeded(e -> {
                Platform.runLater(() -> {
                    item.setManufacture(updated.getManufacture());
                    item.setProductName(updated.getProductName());
                    item.setCode(updated.getCode());
                    item.setSerialNumber(updated.getSerialNumber());
                    item.setQuantity(updated.getQuantity());
                    item.updateAvailabilityStatus();
                    storageTable.refresh();
                    showLoading(false);
                    showSuccess("✓ Maintenance item updated!");
                });
            });

            updateTask.setOnFailed(e -> {
                showLoading(false);
                showError("Update failed: " + updateTask.getException().getMessage());
            });

            new Thread(updateTask).start();
        });
    }

    @Override
    protected void handleBulkDelete() {
        List<StorageItemViewModel> selected = getSelectedItems();

        if (selected.isEmpty()) {
            showWarning("Please select items to delete");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete " + selected.size() + " maintenance item(s)?");
        confirm.setContentText("This will remove the items from the inventory.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            showLoading(true);

            Task<Void> deleteTask = new Task<>() {
                @Override
                protected Void call() {
                    List<Long> ids = selected.stream()
                            .map(StorageItemViewModel::getId)
                            .collect(Collectors.toList());
                    storageService.deleteItems(ids);
                    return null;
                }
            };

            deleteTask.setOnSucceeded(e -> {
                Platform.runLater(() -> {
                    storageItems.removeAll(selected);
                    selectionMap.keySet().removeAll(selected);
                    selectAllCheckbox.setSelected(false);
                    updateEmptyState();
                    updateSelectedCount();
                    showLoading(false);
                    showSuccess("✓ Deleted " + selected.size() + " item(s)!");
                });
            });

            deleteTask.setOnFailed(e -> {
                showLoading(false);
                showError("Delete failed: " + deleteTask.getException().getMessage());
            });

            new Thread(deleteTask).start();
        }
    }

    /**
     * Create dialog - NO PRICE FIELD for Maintenance
     */
    private Dialog<StorageItemViewModel> createItemDialog(StorageItemViewModel existing) {
        Dialog<StorageItemViewModel> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Maintenance Item" : "Edit Maintenance Item");
        dialog.initModality(Modality.APPLICATION_MODAL);

        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        grid.setStyle("-fx-background-color: white;");

        TextField mfgField = new TextField(existing != null ? existing.getManufacture() : "");
        mfgField.setPromptText("Manufacture");

        TextField nameField = new TextField(existing != null ? existing.getProductName() : "");
        nameField.setPromptText("Product Name (Required)");

        TextField codeField = new TextField(existing != null ? existing.getCode() : "");
        codeField.setPromptText("Product Code");

        TextField serialField = new TextField(existing != null ? existing.getSerialNumber() : "");
        serialField.setPromptText("Serial Number");

        Spinner<Integer> qtySpinner = new Spinner<>(0, 999999,
                existing != null ? existing.getQuantity() : 0);
        qtySpinner.setEditable(true);

        Label availLabel = new Label();
        availLabel.setStyle("-fx-font-weight: bold;");
        qtySpinner.valueProperty().addListener((obs, old, newVal) -> {
            if (newVal > 0) {
                availLabel.setText("Status: ✅ Available for Maintenance");
                availLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #22c55e;");
            } else {
                availLabel.setText("Status: ❌ Not Available");
                availLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #ef4444;");
            }
        });
        qtySpinner.getValueFactory().setValue(qtySpinner.getValue());  // ✅ BETTER
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
        grid.add(availLabel, 1, 5);

        dialog.getDialogPane().setContent(grid);

        javafx.scene.Node saveButton = dialog.getDialogPane().lookupButton(saveBtn);
        saveButton.setDisable(true);
        nameField.textProperty().addListener((obs, old, n) -> {
            saveButton.setDisable(n.trim().isEmpty());
        });

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                StorageItemViewModel vm = new StorageItemViewModel();
                vm.setManufacture(mfgField.getText().trim());
                vm.setProductName(nameField.getText().trim());
                vm.setCode(codeField.getText().trim());
                vm.setSerialNumber(serialField.getText().trim());
                vm.setQuantity(qtySpinner.getValue());
                vm.setPrice(BigDecimal.ZERO); // Maintenance doesn't manage price
                vm.updateAvailabilityStatus();
                return vm;
            }
            return null;
        });

        return dialog;
    }
}