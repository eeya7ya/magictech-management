package com.magictech.modules.sales.ui;

import com.magictech.core.auth.User;
import com.magictech.modules.sales.entity.QuotationTemplate;
import com.magictech.modules.sales.service.QuotationTemplateService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

/**
 * QuotationTemplatePanel - Simple form-based quotation editor.
 *
 * Much simpler than the annotation-based system:
 * - Select a template
 * - Fill in To, From, Date fields
 * - Generate PDF
 *
 * No complex positioning or annotation management needed!
 */
public class QuotationTemplatePanel extends VBox {

    // Services
    private QuotationTemplateService templateService;
    private User currentUser;

    // State
    private QuotationTemplate selectedTemplate;
    private String entityType;
    private Long entityId;

    // UI Components
    private ComboBox<TemplateItem> templateCombo;
    private TextField toField;
    private TextField fromField;
    private DatePicker datePicker;
    private ImageView previewImageView;
    private Label statusLabel;
    private Button generateBtn;
    private Button downloadBtn;
    private StackPane previewPane;

    // Generated PDF data
    private byte[] generatedPdfData;

    // Callbacks
    private Consumer<byte[]> onGenerateCallback;
    private Consumer<String> onStatusCallback;

    private static final float PREVIEW_DPI = 100f;

    public QuotationTemplatePanel() {
        setSpacing(0);
        setStyle("-fx-background-color: transparent;");
    }

    /**
     * Initialize the panel.
     */
    public void initialize(QuotationTemplateService templateService, User currentUser,
                           String entityType, Long entityId) {
        this.templateService = templateService;
        this.currentUser = currentUser;
        this.entityType = entityType;
        this.entityId = entityId;

        buildUI();
        loadTemplates();
    }

    private void buildUI() {
        // Header
        HBox header = createHeader();

        // Main content: Form + Preview
        HBox mainContent = new HBox(20);
        mainContent.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(mainContent, Priority.ALWAYS);

        // Left side: Form
        VBox formPanel = createFormPanel();
        formPanel.setPrefWidth(350);
        formPanel.setMinWidth(300);

        // Right side: Preview
        VBox previewPanel = createPreviewPanel();
        HBox.setHgrow(previewPanel, Priority.ALWAYS);

        mainContent.getChildren().addAll(formPanel, previewPanel);
        mainContent.setPadding(new Insets(15));

        // Status bar
        HBox statusBar = createStatusBar();

        getChildren().addAll(header, mainContent, statusBar);
    }

    private HBox createHeader() {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(15, 20, 15, 20));
        header.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.8);" +
                "-fx-border-color: rgba(139, 92, 246, 0.3);" +
                "-fx-border-width: 0 0 2 0;"
        );

        Label titleLabel = new Label("📄 Quotation Generator");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");

        Label subtitleLabel = new Label("Fill in the fields and generate your quotation PDF");
        subtitleLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 13px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Template management button
        Button manageTemplatesBtn = new Button("⚙️ Manage Templates");
        manageTemplatesBtn.setStyle(
                "-fx-background-color: #6b7280;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 12px;" +
                "-fx-padding: 8 15;" +
                "-fx-background-radius: 6;" +
                "-fx-cursor: hand;"
        );
        manageTemplatesBtn.setOnAction(e -> showTemplateManager());

        header.getChildren().addAll(titleLabel, subtitleLabel, spacer, manageTemplatesBtn);
        return header;
    }

    private VBox createFormPanel() {
        VBox form = new VBox(15);
        form.setPadding(new Insets(20));
        form.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.9);" +
                "-fx-background-radius: 10;" +
                "-fx-border-color: rgba(139, 92, 246, 0.3);" +
                "-fx-border-radius: 10;" +
                "-fx-border-width: 1;"
        );

        // Section title
        Label formTitle = new Label("📝 Quotation Details");
        formTitle.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        // Template selection
        Label templateLabel = new Label("Template:");
        templateLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.8); -fx-font-size: 13px;");

        templateCombo = new ComboBox<>();
        templateCombo.setPrefWidth(Double.MAX_VALUE);
        templateCombo.setPromptText("Select a template...");
        styleComboBox(templateCombo);
        templateCombo.setOnAction(e -> handleTemplateChange());

        // To field
        Label toLabel = new Label("To (Customer/Company):");
        toLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.8); -fx-font-size: 13px;");

        toField = new TextField();
        toField.setPromptText("Enter customer or company name...");
        styleTextField(toField);
        toField.textProperty().addListener((obs, old, newVal) -> updatePreview());

        // From field
        Label fromLabel = new Label("From:");
        fromLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.8); -fx-font-size: 13px;");

        fromField = new TextField("Magic Technology");
        fromField.setPromptText("Your company name...");
        styleTextField(fromField);
        fromField.textProperty().addListener((obs, old, newVal) -> updatePreview());

        // Date field
        Label dateLabel = new Label("Date:");
        dateLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.8); -fx-font-size: 13px;");

        datePicker = new DatePicker(LocalDate.now());
        datePicker.setPrefWidth(Double.MAX_VALUE);
        datePicker.setStyle(
                "-fx-background-color: #374151;" +
                "-fx-font-size: 13px;"
        );
        datePicker.valueProperty().addListener((obs, old, newVal) -> updatePreview());

        // Separator
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: rgba(139, 92, 246, 0.3);");

        // Action buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        generateBtn = new Button("🔄 Preview");
        generateBtn.setPrefWidth(120);
        generateBtn.setStyle(
                "-fx-background-color: #3b82f6;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 10 20;" +
                "-fx-background-radius: 6;" +
                "-fx-cursor: hand;"
        );
        generateBtn.setOnAction(e -> generatePreview());

        downloadBtn = new Button("📥 Download PDF");
        downloadBtn.setPrefWidth(140);
        downloadBtn.setStyle(
                "-fx-background-color: #22c55e;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 10 20;" +
                "-fx-background-radius: 6;" +
                "-fx-cursor: hand;"
        );
        downloadBtn.setOnAction(e -> downloadPdf());
        downloadBtn.setDisable(true);

        buttonBox.getChildren().addAll(generateBtn, downloadBtn);

        // Help text
        Label helpLabel = new Label("💡 Select a template and fill in the fields.\nClick Preview to see the result.");
        helpLabel.setStyle("-fx-text-fill: #a78bfa; -fx-font-size: 11px;");
        helpLabel.setWrapText(true);

        form.getChildren().addAll(
                formTitle,
                templateLabel, templateCombo,
                toLabel, toField,
                fromLabel, fromField,
                dateLabel, datePicker,
                sep,
                buttonBox,
                helpLabel
        );

        return form;
    }

    private VBox createPreviewPanel() {
        VBox preview = new VBox(10);
        preview.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(preview, Priority.ALWAYS);

        Label previewTitle = new Label("📄 Preview");
        previewTitle.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        // Preview pane with scroll
        previewPane = new StackPane();
        previewPane.setStyle(
                "-fx-background-color: #1e293b;" +
                "-fx-border-color: rgba(139, 92, 246, 0.5);" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 8;"
        );
        VBox.setVgrow(previewPane, Priority.ALWAYS);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);

        previewImageView = new ImageView();
        previewImageView.setPreserveRatio(true);
        previewImageView.setSmooth(true);

        scrollPane.setContent(previewImageView);

        // Placeholder
        VBox placeholder = createPlaceholder();
        placeholder.setId("placeholder");

        previewPane.getChildren().addAll(scrollPane, placeholder);

        preview.getChildren().addAll(previewTitle, previewPane);
        preview.setPadding(new Insets(20, 0, 20, 0));

        return preview;
    }

    private VBox createPlaceholder() {
        VBox placeholder = new VBox(15);
        placeholder.setAlignment(Pos.CENTER);
        placeholder.setStyle("-fx-background-color: rgba(30, 41, 59, 0.95);");

        Label iconLabel = new Label("📄");
        iconLabel.setStyle("-fx-font-size: 48px;");

        Label textLabel = new Label("No template selected");
        textLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 16px;");

        Label hintLabel = new Label("Select a template and fill in the fields to preview");
        hintLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.5); -fx-font-size: 13px;");

        placeholder.getChildren().addAll(iconLabel, textLabel, hintLabel);
        return placeholder;
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox(15);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setPadding(new Insets(10, 20, 10, 20));
        statusBar.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.6);" +
                "-fx-border-color: rgba(139, 92, 246, 0.2);" +
                "-fx-border-width: 1 0 0 0;"
        );

        statusLabel = new Label("Ready - Select a template to begin");
        statusLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 12px;");

        statusBar.getChildren().add(statusLabel);
        return statusBar;
    }

    // ==================== Styling ====================

    private void styleTextField(TextField field) {
        field.setStyle(
                "-fx-background-color: #374151;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-padding: 10;" +
                "-fx-background-radius: 6;"
        );
    }

    private <T> void styleComboBox(ComboBox<T> combo) {
        combo.setStyle(
                "-fx-background-color: #374151;" +
                "-fx-font-size: 13px;"
        );
    }

    // ==================== Data Loading ====================

    private void loadTemplates() {
        Task<List<QuotationTemplate>> task = new Task<>() {
            @Override
            protected List<QuotationTemplate> call() {
                return templateService.findAll();
            }
        };

        task.setOnSucceeded(e -> {
            List<QuotationTemplate> templates = task.getValue();
            templateCombo.getItems().clear();

            for (QuotationTemplate template : templates) {
                templateCombo.getItems().add(new TemplateItem(template));
            }

            // Select default if available
            for (TemplateItem item : templateCombo.getItems()) {
                if (item.template.getIsDefault() != null && item.template.getIsDefault()) {
                    templateCombo.setValue(item);
                    break;
                }
            }

            if (!templates.isEmpty()) {
                setStatus("Loaded " + templates.size() + " template(s)");
            } else {
                setStatus("No templates found - Upload a template to get started");
            }
        });

        task.setOnFailed(e -> setStatus("Failed to load templates"));

        new Thread(task).start();
    }

    // ==================== Event Handlers ====================

    private void handleTemplateChange() {
        TemplateItem selected = templateCombo.getValue();
        if (selected != null) {
            selectedTemplate = selected.template;
            loadTemplatePreview();
            setStatus("Template selected: " + selectedTemplate.getTemplateName());
        }
    }

    private void loadTemplatePreview() {
        if (selectedTemplate == null) return;

        Task<byte[]> task = new Task<>() {
            @Override
            protected byte[] call() throws Exception {
                return templateService.renderTemplatePreview(selectedTemplate.getId(), PREVIEW_DPI);
            }
        };

        task.setOnSucceeded(e -> {
            byte[] pngData = task.getValue();
            Image image = new Image(new ByteArrayInputStream(pngData));
            previewImageView.setImage(image);
            hidePlaceholder();
        });

        task.setOnFailed(e -> setStatus("Failed to load template preview"));

        new Thread(task).start();
    }

    private void updatePreview() {
        // Debounce - could add delay here if needed
        // For now, preview updates on explicit button click
    }

    private void generatePreview() {
        if (selectedTemplate == null) {
            showAlert("No Template", "Please select a template first.");
            return;
        }

        String toValue = toField.getText().trim();
        String fromValue = fromField.getText().trim();
        String dateValue = datePicker.getValue() != null
                ? datePicker.getValue().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
                : "";

        if (toValue.isEmpty()) {
            showAlert("Missing Field", "Please enter a value for the 'To' field.");
            return;
        }

        setStatus("Generating preview...");
        generateBtn.setDisable(true);

        Task<byte[]> task = new Task<>() {
            @Override
            protected byte[] call() throws Exception {
                // Generate PDF
                generatedPdfData = templateService.generateQuotationPdf(
                        selectedTemplate.getId(), toValue, fromValue, dateValue);

                // Render preview
                return templateService.renderQuotationPreview(
                        selectedTemplate.getId(), toValue, fromValue, dateValue, PREVIEW_DPI);
            }
        };

        task.setOnSucceeded(e -> {
            byte[] pngData = task.getValue();
            Image image = new Image(new ByteArrayInputStream(pngData));
            previewImageView.setImage(image);
            hidePlaceholder();

            downloadBtn.setDisable(false);
            generateBtn.setDisable(false);
            setStatus("✓ Preview generated - Ready to download");

            if (onGenerateCallback != null) {
                onGenerateCallback.accept(generatedPdfData);
            }
        });

        task.setOnFailed(e -> {
            generateBtn.setDisable(false);
            setStatus("✗ Failed to generate preview: " + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    private void downloadPdf() {
        if (generatedPdfData == null) {
            showAlert("No PDF", "Please generate a preview first.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Quotation PDF");
        fileChooser.setInitialFileName("quotation_" + toField.getText().replaceAll("[^a-zA-Z0-9]", "_") + ".pdf");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );

        File file = fileChooser.showSaveDialog(getScene().getWindow());
        if (file != null) {
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(generatedPdfData);
                setStatus("✓ PDF saved to: " + file.getName());
            } catch (Exception ex) {
                setStatus("✗ Failed to save PDF: " + ex.getMessage());
            }
        }
    }

    private void showTemplateManager() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Template Manager");
        dialog.setHeaderText("Manage Quotation Templates");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(500);

        // Template list
        ListView<TemplateItem> templateList = new ListView<>();
        templateList.setPrefHeight(200);
        templateList.getItems().addAll(templateCombo.getItems());

        // Upload new template button
        Button uploadBtn = new Button("📤 Upload New Template");
        uploadBtn.setOnAction(e -> {
            uploadNewTemplate();
            // Refresh list
            loadTemplates();
            templateList.getItems().clear();
            templateList.getItems().addAll(templateCombo.getItems());
        });

        // Set as default button
        Button setDefaultBtn = new Button("⭐ Set as Default");
        setDefaultBtn.setOnAction(e -> {
            TemplateItem selected = templateList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                templateService.setAsDefault(selected.template.getId());
                loadTemplates();
                showAlert("Success", "Template set as default.");
            }
        });

        // Delete button
        Button deleteBtn = new Button("🗑️ Delete");
        deleteBtn.setOnAction(e -> {
            TemplateItem selected = templateList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Delete Template");
                confirm.setHeaderText("Delete template '" + selected.template.getTemplateName() + "'?");
                confirm.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        templateService.deleteTemplate(selected.template.getId());
                        loadTemplates();
                        templateList.getItems().remove(selected);
                    }
                });
            }
        });

        HBox buttonBar = new HBox(10);
        buttonBar.getChildren().addAll(uploadBtn, setDefaultBtn, deleteBtn);

        content.getChildren().addAll(
                new Label("Available Templates:"),
                templateList,
                buttonBar
        );

        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    private void uploadNewTemplate() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Template Background");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        File file = fileChooser.showOpenDialog(getScene().getWindow());
        if (file != null) {
            // Get template name
            TextInputDialog nameDialog = new TextInputDialog(file.getName().replaceAll("\\.[^.]+$", ""));
            nameDialog.setTitle("Template Name");
            nameDialog.setHeaderText("Enter a name for this template:");
            nameDialog.setContentText("Name:");

            nameDialog.showAndWait().ifPresent(name -> {
                try {
                    byte[] data = Files.readAllBytes(file.toPath());
                    String type = file.getName().toLowerCase().endsWith(".pdf") ? "PDF" : "IMAGE";

                    templateService.createTemplate(
                            name, data, type, file.getName(),
                            currentUser != null ? currentUser.getUsername() : "system"
                    );

                    loadTemplates();
                    setStatus("✓ Template uploaded: " + name);

                } catch (Exception ex) {
                    setStatus("✗ Failed to upload template: " + ex.getMessage());
                }
            });
        }
    }

    // ==================== Helpers ====================

    private void hidePlaceholder() {
        VBox placeholder = (VBox) previewPane.lookup("#placeholder");
        if (placeholder != null) {
            placeholder.setVisible(false);
        }
    }

    private void showPlaceholder() {
        VBox placeholder = (VBox) previewPane.lookup("#placeholder");
        if (placeholder != null) {
            placeholder.setVisible(true);
        }
    }

    private void setStatus(String message) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            if (onStatusCallback != null) {
                onStatusCallback.accept(message);
            }
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ==================== Public API ====================

    public void setOnGenerateCallback(Consumer<byte[]> callback) {
        this.onGenerateCallback = callback;
    }

    public void setOnStatusCallback(Consumer<String> callback) {
        this.onStatusCallback = callback;
    }

    public byte[] getGeneratedPdfData() {
        return generatedPdfData;
    }

    public void setToValue(String value) {
        toField.setText(value);
    }

    public void setFromValue(String value) {
        fromField.setText(value);
    }

    public void setDate(LocalDate date) {
        datePicker.setValue(date);
    }

    public void refresh() {
        loadTemplates();
    }

    // ==================== Helper Class ====================

    /**
     * Wrapper for ComboBox display.
     */
    private static class TemplateItem {
        final QuotationTemplate template;

        TemplateItem(QuotationTemplate template) {
            this.template = template;
        }

        @Override
        public String toString() {
            String name = template.getTemplateName();
            if (template.getIsDefault() != null && template.getIsDefault()) {
                name += " ⭐";
            }
            return name;
        }
    }
}
