package com.magictech.modules.sales.ui;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magictech.core.auth.User;
import com.magictech.modules.sales.entity.QuotationDesign;
import com.magictech.modules.sales.service.QuotationDesignService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;

/**
 * QuotationDesignEditorPanel - Reusable PDF Editor Component
 *
 * Features:
 * - Upload PDF from file
 * - Preview PDF pages with navigation
 * - Click to add text annotations
 * - Rich text editing (font, size, color, bold, italic)
 * - Save annotations to database
 * - Download PDF with annotations burned in
 * - Version history
 */
public class QuotationDesignEditorPanel extends VBox {

    // Services
    private QuotationDesignService quotationService;
    private User currentUser;
    private String moduleSource;
    private String entityType;
    private Long entityId;

    // State
    private QuotationDesign currentQuotation;
    private byte[] currentPdfData;
    private List<Map<String, Object>> annotations = new ArrayList<>();
    private int currentPage = 0;
    private int totalPages = 0;
    private boolean editMode = false;

    // UI Components
    private StackPane pdfViewerPane;
    private ImageView pdfImageView;
    private Pane annotationOverlay;
    private Label pageLabel;
    private Button prevPageBtn;
    private Button nextPageBtn;
    private ComboBox<String> versionCombo;
    private Label statusLabel;
    private VBox toolsPanel;

    // Annotation editing state
    private int selectedAnnotationIndex = -1;
    private List<StackPane> annotationNodes = new ArrayList<>();

    // Text editing tools
    private ComboBox<String> fontFamilyCombo;
    private ComboBox<Integer> fontSizeCombo;
    private ColorPicker colorPicker;
    private ToggleButton boldToggle;
    private ToggleButton italicToggle;

    // Callbacks
    private Consumer<QuotationDesign> onSaveCallback;
    private Consumer<String> onStatusCallback;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final float RENDER_DPI = 150f;

    public QuotationDesignEditorPanel() {
        setSpacing(0);
        setStyle("-fx-background-color: transparent;");
    }

    /**
     * Initialize the panel
     */
    public void initialize(QuotationDesignService quotationService, User currentUser,
                           String moduleSource, String entityType, Long entityId) {
        this.quotationService = quotationService;
        this.currentUser = currentUser;
        this.moduleSource = moduleSource;
        this.entityType = entityType;
        this.entityId = entityId;

        buildUI();
        loadCurrentQuotation();
    }

    private void buildUI() {
        // Header toolbar
        HBox toolbar = createToolbar();

        // Main content: PDF viewer + tools panel
        HBox mainContent = new HBox(0);
        mainContent.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(mainContent, Priority.ALWAYS);

        // PDF Viewer (left side)
        VBox viewerContainer = createPdfViewer();
        HBox.setHgrow(viewerContainer, Priority.ALWAYS);

        // Tools panel (right side)
        toolsPanel = createToolsPanel();
        toolsPanel.setVisible(false);
        toolsPanel.setManaged(false);

        mainContent.getChildren().addAll(viewerContainer, toolsPanel);

        // Status bar
        HBox statusBar = createStatusBar();

        getChildren().addAll(toolbar, mainContent, statusBar);
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(15);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(15, 20, 15, 20));
        toolbar.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.8);" +
                "-fx-border-color: rgba(139, 92, 246, 0.3);" +
                "-fx-border-width: 0 0 2 0;"
        );

        Label titleLabel = new Label("📄 Quotation Design");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        // Upload button
        Button uploadBtn = createToolButton("📤 Upload PDF", "#3b82f6");
        uploadBtn.setOnAction(e -> handleUpload());

        // Edit mode toggle
        ToggleButton editToggle = new ToggleButton("✏️ Edit Mode");
        editToggle.setStyle(
                "-fx-background-color: #6b7280;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 8 15;" +
                "-fx-background-radius: 6;" +
                "-fx-cursor: hand;"
        );
        editToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            editMode = newVal;
            toolsPanel.setVisible(newVal);
            toolsPanel.setManaged(newVal);
            editToggle.setStyle(
                    "-fx-background-color: " + (newVal ? "#8b5cf6" : "#6b7280") + ";" +
                    "-fx-text-fill: white;" +
                    "-fx-font-size: 13px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 8 15;" +
                    "-fx-background-radius: 6;" +
                    "-fx-cursor: hand;"
            );
            updateAnnotationOverlay();
        });

        // Save button
        Button saveBtn = createToolButton("💾 Save", "#22c55e");
        saveBtn.setOnAction(e -> handleSave());

        // Download button
        Button downloadBtn = createToolButton("📥 Download", "#f59e0b");
        downloadBtn.setOnAction(e -> handleDownload());

        // Reset button
        Button resetBtn = createToolButton("🔄 Reset", "#ef4444");
        resetBtn.setOnAction(e -> handleReset());

        // Version selector
        Label versionLabel = new Label("Version:");
        versionLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");

        versionCombo = new ComboBox<>();
        versionCombo.setPrefWidth(120);
        versionCombo.setStyle(
                "-fx-background-color: #374151;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;"
        );
        versionCombo.setOnAction(e -> handleVersionChange());

        toolbar.getChildren().addAll(
                titleLabel, spacer1,
                uploadBtn, editToggle, saveBtn, downloadBtn, resetBtn,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                versionLabel, versionCombo
        );

        return toolbar;
    }

    private VBox createPdfViewer() {
        VBox container = new VBox(10);
        container.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(container, Priority.ALWAYS);

        // PDF viewer pane with annotation overlay
        pdfViewerPane = new StackPane();
        pdfViewerPane.setStyle(
                "-fx-background-color: #1e293b;" +
                "-fx-border-color: rgba(139, 92, 246, 0.5);" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 8;"
        );
        VBox.setVgrow(pdfViewerPane, Priority.ALWAYS);

        // ScrollPane for PDF image
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);

        // Container for image and overlay
        StackPane imageContainer = new StackPane();
        imageContainer.setAlignment(Pos.TOP_LEFT);

        pdfImageView = new ImageView();
        pdfImageView.setPreserveRatio(true);
        pdfImageView.setSmooth(true);

        annotationOverlay = new Pane();
        annotationOverlay.setStyle("-fx-background-color: transparent;");
        annotationOverlay.setMouseTransparent(false);
        annotationOverlay.setOnMouseClicked(this::handleOverlayClick);

        imageContainer.getChildren().addAll(pdfImageView, annotationOverlay);
        scrollPane.setContent(imageContainer);

        // Placeholder for empty state
        VBox placeholder = createPlaceholder();
        placeholder.setId("placeholder");

        pdfViewerPane.getChildren().addAll(scrollPane, placeholder);

        // Page navigation
        HBox pageNav = createPageNavigation();

        container.getChildren().addAll(pdfViewerPane, pageNav);
        container.setPadding(new Insets(15));

        return container;
    }

    private VBox createPlaceholder() {
        VBox placeholder = new VBox(15);
        placeholder.setAlignment(Pos.CENTER);
        placeholder.setStyle("-fx-background-color: rgba(30, 41, 59, 0.9);");

        Label iconLabel = new Label("📄");
        iconLabel.setStyle("-fx-font-size: 48px;");

        Label textLabel = new Label("No PDF uploaded yet");
        textLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 18px;");

        Label hintLabel = new Label("Click 'Upload PDF' to add a quotation document");
        hintLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.5); -fx-font-size: 14px;");

        placeholder.getChildren().addAll(iconLabel, textLabel, hintLabel);
        return placeholder;
    }

    private HBox createPageNavigation() {
        HBox nav = new HBox(15);
        nav.setAlignment(Pos.CENTER);
        nav.setPadding(new Insets(10, 0, 5, 0));

        prevPageBtn = new Button("◀ Previous");
        prevPageBtn.setStyle(
                "-fx-background-color: #4b5563;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-padding: 8 15;" +
                "-fx-background-radius: 6;" +
                "-fx-cursor: hand;"
        );
        prevPageBtn.setOnAction(e -> navigatePage(-1));
        prevPageBtn.setDisable(true);

        pageLabel = new Label("Page 0 / 0");
        pageLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        nextPageBtn = new Button("Next ▶");
        nextPageBtn.setStyle(
                "-fx-background-color: #4b5563;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-padding: 8 15;" +
                "-fx-background-radius: 6;" +
                "-fx-cursor: hand;"
        );
        nextPageBtn.setOnAction(e -> navigatePage(1));
        nextPageBtn.setDisable(true);

        nav.getChildren().addAll(prevPageBtn, pageLabel, nextPageBtn);
        return nav;
    }

    private VBox createToolsPanel() {
        VBox panel = new VBox(15);
        panel.setPrefWidth(280);
        panel.setPadding(new Insets(15));
        panel.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.95);" +
                "-fx-border-color: rgba(139, 92, 246, 0.5);" +
                "-fx-border-width: 0 0 0 2;"
        );

        Label titleLabel = new Label("✏️ Text Tools");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        // Font family
        Label fontLabel = new Label("Font Family:");
        fontLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.8); -fx-font-size: 12px;");

        fontFamilyCombo = new ComboBox<>(FXCollections.observableArrayList(
                "Helvetica", "Times New Roman", "Courier"
        ));
        fontFamilyCombo.setValue("Helvetica");
        fontFamilyCombo.setPrefWidth(250);
        styleComboBox(fontFamilyCombo);

        // Font size
        Label sizeLabel = new Label("Font Size:");
        sizeLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.8); -fx-font-size: 12px;");

        fontSizeCombo = new ComboBox<>(FXCollections.observableArrayList(
                8, 10, 12, 14, 16, 18, 20, 24, 28, 32, 36, 48, 72
        ));
        fontSizeCombo.setValue(12);
        fontSizeCombo.setPrefWidth(250);
        styleComboBox(fontSizeCombo);

        // Color picker
        Label colorLabel = new Label("Text Color:");
        colorLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.8); -fx-font-size: 12px;");

        colorPicker = new ColorPicker(Color.BLACK);
        colorPicker.setPrefWidth(250);
        colorPicker.setStyle("-fx-background-color: #374151;");

        // Bold/Italic toggles
        HBox styleBox = new HBox(10);
        styleBox.setAlignment(Pos.CENTER_LEFT);

        boldToggle = new ToggleButton("B");
        boldToggle.setStyle(
                "-fx-background-color: #4b5563;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-min-width: 40;" +
                "-fx-min-height: 40;" +
                "-fx-background-radius: 6;"
        );
        boldToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            boldToggle.setStyle(
                    "-fx-background-color: " + (newVal ? "#8b5cf6" : "#4b5563") + ";" +
                    "-fx-text-fill: white;" +
                    "-fx-font-size: 14px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-min-width: 40;" +
                    "-fx-min-height: 40;" +
                    "-fx-background-radius: 6;"
            );
        });

        italicToggle = new ToggleButton("I");
        italicToggle.setStyle(
                "-fx-background-color: #4b5563;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 14px;" +
                "-fx-font-style: italic;" +
                "-fx-min-width: 40;" +
                "-fx-min-height: 40;" +
                "-fx-background-radius: 6;"
        );
        italicToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            italicToggle.setStyle(
                    "-fx-background-color: " + (newVal ? "#8b5cf6" : "#4b5563") + ";" +
                    "-fx-text-fill: white;" +
                    "-fx-font-size: 14px;" +
                    "-fx-font-style: italic;" +
                    "-fx-min-width: 40;" +
                    "-fx-min-height: 40;" +
                    "-fx-background-radius: 6;"
            );
        });

        styleBox.getChildren().addAll(new Label("Style:"), boldToggle, italicToggle);
        ((Label) styleBox.getChildren().get(0)).setStyle("-fx-text-fill: rgba(255, 255, 255, 0.8); -fx-font-size: 12px;");

        // Instructions
        Label instructionLabel = new Label("💡 Click on the PDF to add text");
        instructionLabel.setStyle("-fx-text-fill: #a78bfa; -fx-font-size: 12px; -fx-wrap-text: true;");
        instructionLabel.setWrapText(true);

        // Separator
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: rgba(139, 92, 246, 0.3);");

        // Annotations list
        Label annotationsLabel = new Label("📝 Annotations (" + annotations.size() + ")");
        annotationsLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        annotationsLabel.setId("annotationsLabel");

        ListView<String> annotationsList = new ListView<>();
        annotationsList.setId("annotationsList");
        annotationsList.setPrefHeight(200);
        annotationsList.setStyle(
                "-fx-background-color: #1e293b;" +
                "-fx-control-inner-background: #1e293b;"
        );
        VBox.setVgrow(annotationsList, Priority.ALWAYS);

        // Delete selected annotation button
        Button deleteAnnotationBtn = new Button("🗑️ Delete Selected");
        deleteAnnotationBtn.setStyle(
                "-fx-background-color: #ef4444;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 12px;" +
                "-fx-padding: 8 15;" +
                "-fx-background-radius: 6;" +
                "-fx-cursor: hand;"
        );
        deleteAnnotationBtn.setOnAction(e -> {
            int selected = annotationsList.getSelectionModel().getSelectedIndex();
            if (selected >= 0) {
                deleteAnnotation(selected);
                updateAnnotationsList();
            }
        });

        panel.getChildren().addAll(
                titleLabel,
                fontLabel, fontFamilyCombo,
                sizeLabel, fontSizeCombo,
                colorLabel, colorPicker,
                styleBox,
                instructionLabel,
                sep,
                annotationsLabel,
                annotationsList,
                deleteAnnotationBtn
        );

        return panel;
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

        statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 12px;");

        statusBar.getChildren().add(statusLabel);
        return statusBar;
    }

    private Button createToolButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color: " + color + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 8 15;" +
                "-fx-background-radius: 6;" +
                "-fx-cursor: hand;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(btn.getStyle() + "-fx-opacity: 0.9;"));
        btn.setOnMouseExited(e -> btn.setStyle(btn.getStyle().replace("-fx-opacity: 0.9;", "")));
        return btn;
    }

    private <T> void styleComboBox(ComboBox<T> combo) {
        combo.setStyle(
                "-fx-background-color: #374151;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;"
        );
    }

    // ==================== Event Handlers ====================

    private void handleUpload() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select PDF File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );

        File file = fileChooser.showOpenDialog(getScene().getWindow());
        if (file != null) {
            uploadPdf(file);
        }
    }

    private void uploadPdf(File file) {
        setStatus("Uploading PDF...");

        Task<QuotationDesign> task = new Task<>() {
            @Override
            protected QuotationDesign call() throws Exception {
                byte[] pdfData = Files.readAllBytes(file.toPath());
                return quotationService.uploadPdf(
                        entityType, entityId, pdfData,
                        file.getName(),
                        currentUser != null ? currentUser.getUsername() : "system",
                        moduleSource
                );
            }
        };

        task.setOnSucceeded(e -> {
            currentQuotation = task.getValue();
            currentPdfData = currentQuotation.getPdfData();
            annotations.clear();
            loadVersions();
            renderCurrentPage();
            hidePlaceholder();
            setStatus("✓ PDF uploaded successfully - Version " + currentQuotation.getVersion());
            if (onSaveCallback != null) {
                onSaveCallback.accept(currentQuotation);
            }
        });

        task.setOnFailed(e -> {
            setStatus("✗ Failed to upload PDF: " + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    private void handleSave() {
        if (currentQuotation == null) {
            setStatus("No quotation to save");
            return;
        }

        setStatus("Saving annotations...");

        Task<QuotationDesign> task = new Task<>() {
            @Override
            protected QuotationDesign call() throws Exception {
                String annotationsJson = objectMapper.writeValueAsString(annotations);
                return quotationService.createVersionWithAnnotations(
                        entityType, entityId,
                        annotationsJson,
                        "Updated annotations",
                        currentUser != null ? currentUser.getUsername() : "system"
                );
            }
        };

        task.setOnSucceeded(e -> {
            currentQuotation = task.getValue();
            loadVersions();
            setStatus("✓ Saved as Version " + currentQuotation.getVersion());
            if (onSaveCallback != null) {
                onSaveCallback.accept(currentQuotation);
            }
        });

        task.setOnFailed(e -> {
            setStatus("✗ Failed to save: " + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    private void handleDownload() {
        if (currentQuotation == null || currentPdfData == null) {
            setStatus("No PDF to download");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save PDF");
        fileChooser.setInitialFileName(currentQuotation.getFilename());
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );

        File file = fileChooser.showSaveDialog(getScene().getWindow());
        if (file != null) {
            downloadPdf(file);
        }
    }

    private void downloadPdf(File file) {
        setStatus("Generating PDF with annotations...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String annotationsJson = objectMapper.writeValueAsString(annotations);
                byte[] pdfWithAnnotations = quotationService.generatePdfWithAnnotations(
                        currentPdfData, annotationsJson
                );

                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(pdfWithAnnotations);
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            setStatus("✓ PDF downloaded to: " + file.getName());
        });

        task.setOnFailed(e -> {
            setStatus("✗ Failed to download: " + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    private void handleReset() {
        if (currentQuotation == null) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Reset to Original");
        confirm.setHeaderText("Reset PDF to original?");
        confirm.setContentText("This will remove all annotations and create a new version.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                resetToOriginal();
            }
        });
    }

    private void resetToOriginal() {
        setStatus("Resetting to original...");

        Task<QuotationDesign> task = new Task<>() {
            @Override
            protected QuotationDesign call() throws Exception {
                return quotationService.resetToOriginal(
                        entityType, entityId,
                        currentUser != null ? currentUser.getUsername() : "system"
                );
            }
        };

        task.setOnSucceeded(e -> {
            currentQuotation = task.getValue();
            currentPdfData = currentQuotation.getPdfData();
            annotations.clear();
            loadVersions();
            renderCurrentPage();
            updateAnnotationOverlay();
            setStatus("✓ Reset to original - Version " + currentQuotation.getVersion());
        });

        task.setOnFailed(e -> {
            setStatus("✗ Failed to reset: " + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    private void handleVersionChange() {
        String selected = versionCombo.getValue();
        if (selected == null || currentQuotation == null) return;

        // Parse version number from "Version X (date)"
        try {
            int version = Integer.parseInt(selected.split(" ")[1].split("\\(")[0].trim());
            if (version != currentQuotation.getVersion()) {
                loadVersion(version);
            }
        } catch (Exception e) {
            // Ignore parse errors
        }
    }

    private void loadVersion(int version) {
        setStatus("Loading version " + version + "...");

        Task<QuotationDesign> task = new Task<>() {
            @Override
            protected QuotationDesign call() throws Exception {
                return quotationService.getVersion(entityType, entityId, version)
                        .orElseThrow(() -> new RuntimeException("Version not found"));
            }
        };

        task.setOnSucceeded(e -> {
            currentQuotation = task.getValue();
            currentPdfData = currentQuotation.getPdfData();
            loadAnnotationsFromQuotation();
            renderCurrentPage();
            setStatus("Loaded Version " + version);
        });

        task.setOnFailed(e -> {
            setStatus("✗ Failed to load version: " + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    private void handleOverlayClick(MouseEvent event) {
        if (!editMode || currentPdfData == null) return;

        double x = event.getX();
        double y = event.getY();

        // Show text input dialog
        showTextInputDialog(x, y);
    }

    private void showTextInputDialog(double x, double y) {
        Dialog<Map<String, Object>> dialog = new Dialog<>();
        dialog.setTitle("Add Text Annotation");
        dialog.setHeaderText("Enter text to add at this position");

        // Set dialog buttons
        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        // Create content
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        TextArea textArea = new TextArea();
        textArea.setPromptText("Enter your text here...");
        textArea.setPrefRowCount(3);
        textArea.setWrapText(true);

        content.getChildren().addAll(new Label("Text:"), textArea);
        dialog.getDialogPane().setContent(content);

        // Focus text area
        Platform.runLater(textArea::requestFocus);

        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType && !textArea.getText().trim().isEmpty()) {
                Map<String, Object> annotation = new HashMap<>();
                annotation.put("page", currentPage);
                annotation.put("x", x);
                annotation.put("y", y);
                annotation.put("text", textArea.getText());
                annotation.put("fontSize", fontSizeCombo.getValue());
                annotation.put("fontFamily", fontFamilyCombo.getValue());
                annotation.put("color", toHexString(colorPicker.getValue()));
                annotation.put("bold", boldToggle.isSelected());
                annotation.put("italic", italicToggle.isSelected());
                return annotation;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(annotation -> {
            annotations.add(annotation);
            updateAnnotationOverlay();
            updateAnnotationsList();
            setStatus("Added annotation at (" + (int) x + ", " + (int) y + ")");
        });
    }

    // ==================== Data Loading ====================

    private void loadCurrentQuotation() {
        Task<Optional<QuotationDesign>> task = new Task<>() {
            @Override
            protected Optional<QuotationDesign> call() throws Exception {
                return quotationService.getCurrentVersion(entityType, entityId);
            }
        };

        task.setOnSucceeded(e -> {
            Optional<QuotationDesign> result = task.getValue();
            if (result.isPresent()) {
                currentQuotation = result.get();
                currentPdfData = currentQuotation.getPdfData();
                loadAnnotationsFromQuotation();
                loadVersions();
                renderCurrentPage();
                hidePlaceholder();
                setStatus("Loaded: " + currentQuotation.getFilename() + " (Version " + currentQuotation.getVersion() + ")");
            } else {
                showPlaceholder();
                setStatus("No quotation uploaded yet");
            }
        });

        task.setOnFailed(e -> {
            setStatus("Failed to load quotation");
        });

        new Thread(task).start();
    }

    private void loadAnnotationsFromQuotation() {
        annotations.clear();
        if (currentQuotation != null && currentQuotation.getPdfAnnotations() != null) {
            try {
                annotations = objectMapper.readValue(
                        currentQuotation.getPdfAnnotations(),
                        new TypeReference<>() {}
                );
            } catch (Exception e) {
                annotations = new ArrayList<>();
            }
        }
        updateAnnotationsList();
    }

    private void loadVersions() {
        Task<List<QuotationDesign>> task = new Task<>() {
            @Override
            protected List<QuotationDesign> call() throws Exception {
                return quotationService.getVersionHistory(entityType, entityId);
            }
        };

        task.setOnSucceeded(e -> {
            List<QuotationDesign> versions = task.getValue();
            versionCombo.getItems().clear();

            for (QuotationDesign v : versions) {
                String label = "Version " + v.getVersion();
                if (v.getIsCurrentVersion()) {
                    label += " (current)";
                }
                versionCombo.getItems().add(label);
            }

            if (currentQuotation != null) {
                String currentLabel = "Version " + currentQuotation.getVersion();
                if (currentQuotation.getIsCurrentVersion()) {
                    currentLabel += " (current)";
                }
                versionCombo.setValue(currentLabel);
            }
        });

        new Thread(task).start();
    }

    // ==================== Rendering ====================

    private void renderCurrentPage() {
        if (currentPdfData == null) return;

        Task<Image> task = new Task<>() {
            @Override
            protected Image call() throws Exception {
                byte[] pngData = quotationService.renderPageAsPng(currentPdfData, currentPage, RENDER_DPI);
                totalPages = quotationService.getPageCount(currentPdfData);
                return new Image(new ByteArrayInputStream(pngData));
            }
        };

        task.setOnSucceeded(e -> {
            Image image = task.getValue();
            pdfImageView.setImage(image);

            // Update annotation overlay size
            annotationOverlay.setPrefWidth(image.getWidth());
            annotationOverlay.setPrefHeight(image.getHeight());
            annotationOverlay.setMinWidth(image.getWidth());
            annotationOverlay.setMinHeight(image.getHeight());

            updatePageNavigation();
            updateAnnotationOverlay();
        });

        task.setOnFailed(e -> {
            setStatus("Failed to render page: " + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    private void updatePageNavigation() {
        pageLabel.setText("Page " + (currentPage + 1) + " / " + totalPages);
        prevPageBtn.setDisable(currentPage <= 0);
        nextPageBtn.setDisable(currentPage >= totalPages - 1);
    }

    private void navigatePage(int delta) {
        int newPage = currentPage + delta;
        if (newPage >= 0 && newPage < totalPages) {
            currentPage = newPage;
            renderCurrentPage();
        }
    }

    private void updateAnnotationOverlay() {
        annotationOverlay.getChildren().clear();
        annotationNodes.clear();

        if (!editMode) {
            annotationOverlay.setCursor(Cursor.DEFAULT);
            return;
        }

        annotationOverlay.setCursor(Cursor.CROSSHAIR);

        // Draw annotations for current page
        for (int i = 0; i < annotations.size(); i++) {
            Map<String, Object> ann = annotations.get(i);
            int page = ((Number) ann.getOrDefault("page", 0)).intValue();

            if (page == currentPage) {
                StackPane node = createAnnotationNode(ann, i);
                annotationNodes.add(node);
                annotationOverlay.getChildren().add(node);
            }
        }
    }

    private StackPane createAnnotationNode(Map<String, Object> annotation, int index) {
        double x = ((Number) annotation.get("x")).doubleValue();
        double y = ((Number) annotation.get("y")).doubleValue();
        String text = (String) annotation.getOrDefault("text", "");
        int fontSize = ((Number) annotation.getOrDefault("fontSize", 12)).intValue();
        String color = (String) annotation.getOrDefault("color", "#000000");
        boolean bold = (Boolean) annotation.getOrDefault("bold", false);
        boolean italic = (Boolean) annotation.getOrDefault("italic", false);

        Label label = new Label(text);
        label.setStyle(
                "-fx-text-fill: " + color + ";" +
                "-fx-font-size: " + fontSize + "px;" +
                (bold ? "-fx-font-weight: bold;" : "") +
                (italic ? "-fx-font-style: italic;" : "")
        );
        label.setWrapText(true);
        label.setMaxWidth(400);

        StackPane container = new StackPane(label);
        container.setLayoutX(x);
        container.setLayoutY(y);
        container.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.8);" +
                "-fx-padding: 2 5;" +
                "-fx-border-color: " + (index == selectedAnnotationIndex ? "#8b5cf6" : "rgba(139, 92, 246, 0.3)") + ";" +
                "-fx-border-width: 1;" +
                "-fx-cursor: hand;"
        );

        // Click to select
        container.setOnMouseClicked(e -> {
            e.consume();
            selectedAnnotationIndex = index;
            updateAnnotationOverlay();

            // Update tools panel with annotation properties
            fontFamilyCombo.setValue((String) annotation.getOrDefault("fontFamily", "Helvetica"));
            fontSizeCombo.setValue(fontSize);
            colorPicker.setValue(Color.web(color));
            boldToggle.setSelected(bold);
            italicToggle.setSelected(italic);

            // Select in list
            ListView<String> list = (ListView<String>) toolsPanel.lookup("#annotationsList");
            if (list != null) {
                list.getSelectionModel().select(index);
            }
        });

        return container;
    }

    private void updateAnnotationsList() {
        ListView<String> list = (ListView<String>) toolsPanel.lookup("#annotationsList");
        Label label = (Label) toolsPanel.lookup("#annotationsLabel");

        if (list != null) {
            list.getItems().clear();
            for (int i = 0; i < annotations.size(); i++) {
                Map<String, Object> ann = annotations.get(i);
                String text = (String) ann.getOrDefault("text", "");
                int page = ((Number) ann.getOrDefault("page", 0)).intValue() + 1;
                String preview = text.length() > 30 ? text.substring(0, 30) + "..." : text;
                list.getItems().add("Page " + page + ": " + preview);
            }
        }

        if (label != null) {
            label.setText("📝 Annotations (" + annotations.size() + ")");
        }
    }

    private void deleteAnnotation(int index) {
        if (index >= 0 && index < annotations.size()) {
            annotations.remove(index);
            selectedAnnotationIndex = -1;
            updateAnnotationOverlay();
            setStatus("Annotation deleted");
        }
    }

    // ==================== Helpers ====================

    private void showPlaceholder() {
        VBox placeholder = (VBox) pdfViewerPane.lookup("#placeholder");
        if (placeholder != null) {
            placeholder.setVisible(true);
        }
    }

    private void hidePlaceholder() {
        VBox placeholder = (VBox) pdfViewerPane.lookup("#placeholder");
        if (placeholder != null) {
            placeholder.setVisible(false);
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

    private String toHexString(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    // ==================== Public API ====================

    public void setOnSaveCallback(Consumer<QuotationDesign> callback) {
        this.onSaveCallback = callback;
    }

    public void setOnStatusCallback(Consumer<String> callback) {
        this.onStatusCallback = callback;
    }

    public QuotationDesign getCurrentQuotation() {
        return currentQuotation;
    }

    public List<Map<String, Object>> getAnnotations() {
        return new ArrayList<>(annotations);
    }

    public void refresh() {
        loadCurrentQuotation();
    }
}
