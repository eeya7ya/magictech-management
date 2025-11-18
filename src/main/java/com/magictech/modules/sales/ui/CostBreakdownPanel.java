package com.magictech.modules.sales.ui;

import com.magictech.modules.sales.entity.ProjectCostBreakdown;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Consumer;

/**
 * Reusable Cost Breakdown UI component with purple theme
 * Auto-calculates total based on formula:
 * Total = elements_total + (elements_total * tax_rate) - (elements_total * sale_offer_rate) +
 *         installation_cost + licenses_cost + additional_cost
 */
public class CostBreakdownPanel extends VBox {

    private static final String PURPLE_COLOR = "#7c3aed";
    private static final String DARK_PURPLE = "#6b21a8";
    private static final String LIGHT_PURPLE = "#a78bfa";

    // UI Components
    private Label elementsSubtotalValue;
    private TextField taxRateField;
    private TextField saleOfferRateField;
    private TextField installationField;
    private TextField licensesField;
    private TextField additionalField;
    private Label taxAmountValue;
    private Label discountAmountValue;
    private Label totalCostValue;

    // Data
    private BigDecimal elementsSubtotal = BigDecimal.ZERO;
    private ProjectCostBreakdown breakdown;

    // Callback for save action
    private Consumer<ProjectCostBreakdown> onSaveCallback;

    public CostBreakdownPanel() {
        buildUI();
        setupListeners();
    }

    private void buildUI() {
        setPadding(new Insets(20));
        setSpacing(15);
        setStyle(String.format(
            "-fx-background-color: linear-gradient(to bottom, %s, %s); " +
            "-fx-background-radius: 10; " +
            "-fx-border-radius: 10; " +
            "-fx-border-color: %s; " +
            "-fx-border-width: 2;",
            "#2a2a2a", "#1a1a1a", PURPLE_COLOR
        ));

        // Title
        Label title = new Label("💰 Project Cost Breakdown");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));
        title.setTextFill(Color.web(LIGHT_PURPLE));

        // Grid for breakdown fields
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(10, 0, 10, 0));

        int row = 0;

        // Elements Subtotal (read-only, auto-calculated)
        grid.add(createLabel("Elements Subtotal:"), 0, row);
        elementsSubtotalValue = createValueLabel("$0.00");
        grid.add(elementsSubtotalValue, 1, row++);

        // Separator
        Separator sep1 = new Separator();
        sep1.setStyle("-fx-background-color: " + PURPLE_COLOR + ";");
        grid.add(sep1, 0, row++, 2, 1);

        // Tax Rate (percentage)
        grid.add(createLabel("Tax Rate (%):"), 0, row);
        taxRateField = createTextField("0.00");
        grid.add(taxRateField, 1, row++);

        // Tax Amount (calculated)
        grid.add(createLabel("Tax Amount:"), 0, row);
        taxAmountValue = createValueLabel("$0.00");
        taxAmountValue.setTextFill(Color.web("#f59e0b")); // Orange for tax
        grid.add(taxAmountValue, 1, row++);

        // Sale Offer (discount percentage)
        grid.add(createLabel("Sale Offer (%):"), 0, row);
        saleOfferRateField = createTextField("0.00");
        grid.add(saleOfferRateField, 1, row++);

        // Discount Amount (calculated)
        grid.add(createLabel("Discount Amount:"), 0, row);
        discountAmountValue = createValueLabel("$0.00");
        discountAmountValue.setTextFill(Color.web("#22c55e")); // Green for discount
        grid.add(discountAmountValue, 1, row++);

        // Separator
        Separator sep2 = new Separator();
        sep2.setStyle("-fx-background-color: " + PURPLE_COLOR + ";");
        grid.add(sep2, 0, row++, 2, 1);

        // Installation Cost
        grid.add(createLabel("Installation Cost:"), 0, row);
        installationField = createTextField("0.00");
        grid.add(installationField, 1, row++);

        // Licenses Cost
        grid.add(createLabel("Licenses Cost:"), 0, row);
        licensesField = createTextField("0.00");
        grid.add(licensesField, 1, row++);

        // Additional Cost
        grid.add(createLabel("Additional Cost:"), 0, row);
        additionalField = createTextField("0.00");
        grid.add(additionalField, 1, row++);

        // Separator
        Separator sep3 = new Separator();
        sep3.setStyle("-fx-background-color: " + LIGHT_PURPLE + "; -fx-min-height: 2;");
        grid.add(sep3, 0, row++, 2, 1);

        // Total Cost (calculated, prominent)
        Label totalLabel = createLabel("TOTAL PROJECT COST:");
        totalLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        totalLabel.setTextFill(Color.web(LIGHT_PURPLE));
        grid.add(totalLabel, 0, row);

        totalCostValue = new Label("$0.00");
        totalCostValue.setFont(Font.font("System", FontWeight.BOLD, 18));
        totalCostValue.setTextFill(Color.web(PURPLE_COLOR));
        grid.add(totalCostValue, 1, row);

        // Save button
        Button saveBtn = new Button("💾 Save Cost Breakdown");
        saveBtn.setStyle(String.format(
            "-fx-background-color: %s; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-font-size: 14px; " +
            "-fx-padding: 10 20 10 20; " +
            "-fx-background-radius: 5; " +
            "-fx-cursor: hand;",
            PURPLE_COLOR
        ));
        saveBtn.setOnAction(e -> handleSave());

        HBox buttonBox = new HBox(saveBtn);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        getChildren().addAll(title, grid, buttonBox);
    }

    private Label createLabel(String text) {
        Label label = new Label(text);
        label.setTextFill(Color.web("#cccccc"));
        label.setFont(Font.font("System", 12));
        return label;
    }

    private Label createValueLabel(String text) {
        Label label = new Label(text);
        label.setTextFill(Color.WHITE);
        label.setFont(Font.font("System", FontWeight.BOLD, 13));
        return label;
    }

    private TextField createTextField(String promptText) {
        TextField field = new TextField();
        field.setPromptText(promptText);
        field.setStyle(
            "-fx-background-color: #2a2a2a; " +
            "-fx-text-fill: white; " +
            "-fx-prompt-text-fill: #666; " +
            "-fx-border-color: " + PURPLE_COLOR + "; " +
            "-fx-border-radius: 5; " +
            "-fx-background-radius: 5; " +
            "-fx-padding: 5;"
        );
        // Only allow numbers and decimal point
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*\\.?\\d*")) {
                field.setText(oldVal);
            }
        });
        return field;
    }

    private void setupListeners() {
        // Recalculate on any field change
        taxRateField.textProperty().addListener((obs, old, newVal) -> recalculate());
        saleOfferRateField.textProperty().addListener((obs, old, newVal) -> recalculate());
        installationField.textProperty().addListener((obs, old, newVal) -> recalculate());
        licensesField.textProperty().addListener((obs, old, newVal) -> recalculate());
        additionalField.textProperty().addListener((obs, old, newVal) -> recalculate());
    }

    /**
     * Set elements subtotal (from project elements)
     */
    public void setElementsSubtotal(BigDecimal subtotal) {
        this.elementsSubtotal = subtotal;
        elementsSubtotalValue.setText("$" + subtotal.setScale(2, RoundingMode.HALF_UP).toString());
        recalculate();
    }

    /**
     * Load existing breakdown data
     */
    public void loadBreakdown(ProjectCostBreakdown breakdown) {
        this.breakdown = breakdown;

        if (breakdown != null) {
            setElementsSubtotal(breakdown.getElementsSubtotal());

            // Convert rates to percentages for display (0.15 → 15.00)
            BigDecimal taxPercent = breakdown.getTaxRate().multiply(new BigDecimal(100));
            BigDecimal offerPercent = breakdown.getSaleOfferRate().multiply(new BigDecimal(100));

            taxRateField.setText(taxPercent.setScale(2, RoundingMode.HALF_UP).toString());
            saleOfferRateField.setText(offerPercent.setScale(2, RoundingMode.HALF_UP).toString());
            installationField.setText(breakdown.getInstallationCost().setScale(2, RoundingMode.HALF_UP).toString());
            licensesField.setText(breakdown.getLicensesCost().setScale(2, RoundingMode.HALF_UP).toString());
            additionalField.setText(breakdown.getAdditionalCost().setScale(2, RoundingMode.HALF_UP).toString());

            recalculate();
        }
    }

    /**
     * Recalculate totals
     */
    private void recalculate() {
        try {
            // Parse input fields
            BigDecimal taxRatePercent = parseDecimal(taxRateField.getText());
            BigDecimal saleOfferPercent = parseDecimal(saleOfferRateField.getText());
            BigDecimal installation = parseDecimal(installationField.getText());
            BigDecimal licenses = parseDecimal(licensesField.getText());
            BigDecimal additional = parseDecimal(additionalField.getText());

            // Convert percentages to rates (15.00 → 0.15)
            BigDecimal taxRate = taxRatePercent.divide(new BigDecimal(100), 4, RoundingMode.HALF_UP);
            BigDecimal offerRate = saleOfferPercent.divide(new BigDecimal(100), 4, RoundingMode.HALF_UP);

            // Calculate tax amount: elements_subtotal * tax_rate
            BigDecimal taxAmount = elementsSubtotal.multiply(taxRate);
            taxAmountValue.setText("$" + taxAmount.setScale(2, RoundingMode.HALF_UP).toString());

            // Calculate discount amount: elements_subtotal * sale_offer_rate
            BigDecimal discountAmount = elementsSubtotal.multiply(offerRate);
            discountAmountValue.setText("$" + discountAmount.setScale(2, RoundingMode.HALF_UP).toString());

            // Calculate total
            BigDecimal total = elementsSubtotal
                    .add(taxAmount)
                    .subtract(discountAmount)
                    .add(installation)
                    .add(licenses)
                    .add(additional);

            totalCostValue.setText("$" + total.setScale(2, RoundingMode.HALF_UP).toString());

        } catch (Exception e) {
            // Ignore parsing errors, keep previous values
        }
    }

    /**
     * Parse BigDecimal from text field
     */
    private BigDecimal parseDecimal(String text) {
        if (text == null || text.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Handle save button click
     */
    private void handleSave() {
        if (breakdown == null) {
            breakdown = new ProjectCostBreakdown();
        }

        // Update breakdown object
        breakdown.setElementsSubtotal(elementsSubtotal);

        // Convert percentages to rates
        BigDecimal taxRatePercent = parseDecimal(taxRateField.getText());
        BigDecimal offerRatePercent = parseDecimal(saleOfferRateField.getText());
        breakdown.setTaxRate(taxRatePercent.divide(new BigDecimal(100), 4, RoundingMode.HALF_UP));
        breakdown.setSaleOfferRate(offerRatePercent.divide(new BigDecimal(100), 4, RoundingMode.HALF_UP));

        breakdown.setInstallationCost(parseDecimal(installationField.getText()));
        breakdown.setLicensesCost(parseDecimal(licensesField.getText()));
        breakdown.setAdditionalCost(parseDecimal(additionalField.getText()));

        // Trigger callback
        if (onSaveCallback != null) {
            onSaveCallback.accept(breakdown);
        }
    }

    /**
     * Set callback for save action
     */
    public void setOnSave(Consumer<ProjectCostBreakdown> callback) {
        this.onSaveCallback = callback;
    }

    /**
     * Get current breakdown data
     */
    public ProjectCostBreakdown getBreakdown() {
        handleSave(); // Ensure data is up to date
        return breakdown;
    }
}
