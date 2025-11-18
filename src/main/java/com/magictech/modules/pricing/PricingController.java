package com.magictech.modules.pricing;

import com.magictech.core.module.BaseModuleController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Pricing Module Controller
 * Handles price lists, dynamic pricing, discount management
 */
public class PricingController extends BaseModuleController {

    private VBox contentArea;

    @Override
    protected void setupUI() {
        VBox header = createHeader();

        contentArea = new VBox(20);
        contentArea.setPadding(new Insets(30));
        contentArea.setAlignment(Pos.TOP_CENTER);

        createWelcomeContent();

        getRootPane().setTop(header);
        getRootPane().setCenter(contentArea);
    }

    private VBox createHeader() {
        VBox header = new VBox(10);
        header.setPadding(new Insets(20, 30, 20, 30));
        header.setStyle("-fx-background-color: linear-gradient(to right, #eab308, #ca8a04);");

        Label title = new Label("Pricing Module");
        title.setFont(Font.font("System", FontWeight.BOLD, 24));
        title.setStyle("-fx-text-fill: white;");

        Label subtitle = new Label("Price Lists | Dynamic Pricing | Discount Management");
        subtitle.setFont(Font.font("System", 14));
        subtitle.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.9);");

        header.getChildren().addAll(title, subtitle);
        return header;
    }

    private void createWelcomeContent() {
        Label welcome = new Label("Welcome to Pricing Module");
        welcome.setFont(Font.font("System", FontWeight.BOLD, 20));
        welcome.setStyle("-fx-text-fill: #1e293b;");

        Label info = new Label("Manage pricing and discounts");
        info.setFont(Font.font("System", 14));
        info.setStyle("-fx-text-fill: #64748b;");

        VBox featureBox = new VBox(15);
        featureBox.setPadding(new Insets(30));
        featureBox.setAlignment(Pos.CENTER_LEFT);
        featureBox.setMaxWidth(600);
        featureBox.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        String[] features = {
                "✓ Price Lists",
                "✓ Dynamic Pricing",
                "✓ Discount Management",
                "✓ Payment Tracking"
        };

        for (String feature : features) {
            Label featureLabel = new Label(feature);
            featureLabel.setFont(Font.font("System", 15));
            featureLabel.setStyle("-fx-text-fill: #475569;");
            featureBox.getChildren().add(featureLabel);
        }

        Button actionButton = new Button("Manage Pricing");
        actionButton.setFont(Font.font("System", FontWeight.BOLD, 14));
        actionButton.setStyle("-fx-background-color: #eab308; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-padding: 12 24 12 24; -fx-cursor: hand;");
        actionButton.setOnAction(e -> showSuccess("Pricing management features coming soon!"));

        contentArea.getChildren().addAll(welcome, info, featureBox, actionButton);
    }

    @Override
    protected void loadData() {
        System.out.println("Loading pricing data for user: " + currentUser.getUsername());
    }

    @Override
    public void refresh() {
        System.out.println("Refreshing pricing module data");
        loadData();
    }
}