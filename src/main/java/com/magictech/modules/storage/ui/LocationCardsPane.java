package com.magictech.modules.storage.ui;

import com.magictech.modules.storage.service.StorageLocationService.LocationSummary;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.List;
import java.util.function.Consumer;

/**
 * Card-based view for storage locations
 * Simple, clean grid of clickable location cards
 */
public class LocationCardsPane extends VBox {

    private FlowPane cardsContainer;
    private Consumer<LocationSummary> onLocationClick;
    private Consumer<Void> onTotalClick;

    public LocationCardsPane() {
        setupUI();
    }

    private void setupUI() {
        setSpacing(20);
        setPadding(new Insets(20));
        setStyle("-fx-background-color: transparent;");
        setAlignment(Pos.TOP_CENTER);

        // Title section
        VBox titleBox = new VBox(5);
        titleBox.setAlignment(Pos.CENTER);

        Label iconLabel = new Label("📦");
        iconLabel.setFont(new Font(36));

        Label titleLabel = new Label("Storage Locations");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: bold;");

        Label subtitleLabel = new Label("Click on a location to view its inventory");
        subtitleLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.6); -fx-font-size: 14px;");

        titleBox.getChildren().addAll(iconLabel, titleLabel, subtitleLabel);

        // Cards container with FlowPane for responsive grid
        cardsContainer = new FlowPane();
        cardsContainer.setHgap(20);
        cardsContainer.setVgap(20);
        cardsContainer.setAlignment(Pos.CENTER);
        cardsContainer.setPadding(new Insets(20));

        // Wrap in ScrollPane
        ScrollPane scrollPane = new ScrollPane(cardsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle(
            "-fx-background: transparent;" +
            "-fx-background-color: transparent;" +
            "-fx-border-color: transparent;"
        );
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Total Sheet button at bottom
        HBox totalButtonContainer = createTotalButton();

        getChildren().addAll(titleBox, scrollPane, totalButtonContainer);
    }

    private HBox createTotalButton() {
        HBox buttonContainer = new HBox();
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setPadding(new Insets(10, 0, 0, 0));

        VBox totalCard = new VBox(5);
        totalCard.setAlignment(Pos.CENTER);
        totalCard.setPadding(new Insets(15, 40, 15, 40));
        totalCard.setStyle(
            "-fx-background-color: linear-gradient(to right, #22c55e, #16a34a);" +
            "-fx-background-radius: 12;" +
            "-fx-cursor: hand;"
        );

        Label iconLabel = new Label("📊");
        iconLabel.setFont(new Font(24));

        Label textLabel = new Label("VIEW TOTAL SHEET");
        textLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label subLabel = new Label("All items across all locations");
        subLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.8); -fx-font-size: 12px;");

        totalCard.getChildren().addAll(iconLabel, textLabel, subLabel);

        // Hover effects
        totalCard.setOnMouseEntered(e -> totalCard.setStyle(
            "-fx-background-color: linear-gradient(to right, #16a34a, #15803d);" +
            "-fx-background-radius: 12;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(34, 197, 94, 0.5), 15, 0, 0, 5);"
        ));

        totalCard.setOnMouseExited(e -> totalCard.setStyle(
            "-fx-background-color: linear-gradient(to right, #22c55e, #16a34a);" +
            "-fx-background-radius: 12;" +
            "-fx-cursor: hand;"
        ));

        totalCard.setOnMouseClicked(e -> {
            if (onTotalClick != null) {
                onTotalClick.accept(null);
            }
        });

        buttonContainer.getChildren().add(totalCard);
        return buttonContainer;
    }

    /**
     * Set locations to display as cards
     */
    public void setLocations(List<LocationSummary> locations) {
        cardsContainer.getChildren().clear();

        for (LocationSummary location : locations) {
            VBox card = createLocationCard(location);
            cardsContainer.getChildren().add(card);
        }
    }

    private VBox createLocationCard(LocationSummary location) {
        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20, 25, 20, 25));
        card.setPrefWidth(220);
        card.setMinWidth(200);
        card.setCursor(Cursor.HAND);

        String baseColor = location.getColor() != null ? location.getColor() : "#3b82f6";

        card.setStyle(
            "-fx-background-color: rgba(30, 41, 59, 0.8);" +
            "-fx-background-radius: 16;" +
            "-fx-border-color: " + baseColor + ";" +
            "-fx-border-radius: 16;" +
            "-fx-border-width: 2;"
        );

        // Drop shadow
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web(baseColor, 0.3));
        shadow.setRadius(15);
        shadow.setSpread(0.1);
        card.setEffect(shadow);

        // Icon circle
        StackPane iconContainer = new StackPane();
        iconContainer.setPrefSize(60, 60);
        iconContainer.setStyle(
            "-fx-background-color: " + baseColor + ";" +
            "-fx-background-radius: 30;"
        );

        Label iconLabel = new Label(location.getIcon() != null ? location.getIcon() : "📦");
        iconLabel.setStyle("-fx-font-size: 28px;");
        iconContainer.getChildren().add(iconLabel);

        // Location name
        Label nameLabel = new Label(location.getLocationName());
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        nameLabel.setWrapText(true);
        nameLabel.setAlignment(Pos.CENTER);

        // City
        Label cityLabel = new Label("📍 " + (location.getCity() != null ? location.getCity() : ""));
        cityLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.6); -fx-font-size: 12px;");

        // Stats
        HBox statsBox = new HBox(15);
        statsBox.setAlignment(Pos.CENTER);

        VBox itemsBox = new VBox(2);
        itemsBox.setAlignment(Pos.CENTER);
        Label itemsValue = new Label(String.valueOf(location.getItemCount()));
        itemsValue.setStyle("-fx-text-fill: " + baseColor + "; -fx-font-size: 20px; -fx-font-weight: bold;");
        Label itemsLabel = new Label("Items");
        itemsLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.5); -fx-font-size: 10px;");
        itemsBox.getChildren().addAll(itemsValue, itemsLabel);

        VBox unitsBox = new VBox(2);
        unitsBox.setAlignment(Pos.CENTER);
        Label unitsValue = new Label(String.valueOf(location.getTotalQuantity()));
        unitsValue.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 20px; -fx-font-weight: bold;");
        Label unitsLabel = new Label("Units");
        unitsLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.5); -fx-font-size: 10px;");
        unitsBox.getChildren().addAll(unitsValue, unitsLabel);

        statsBox.getChildren().addAll(itemsBox, unitsBox);

        card.getChildren().addAll(iconContainer, nameLabel, cityLabel, statsBox);

        // Hover effects
        card.setOnMouseEntered(e -> {
            card.setStyle(
                "-fx-background-color: rgba(40, 51, 69, 0.95);" +
                "-fx-background-radius: 16;" +
                "-fx-border-color: " + baseColor + ";" +
                "-fx-border-radius: 16;" +
                "-fx-border-width: 3;"
            );
            DropShadow hoverShadow = new DropShadow();
            hoverShadow.setColor(Color.web(baseColor, 0.5));
            hoverShadow.setRadius(25);
            hoverShadow.setSpread(0.2);
            card.setEffect(hoverShadow);
            card.setScaleX(1.05);
            card.setScaleY(1.05);
        });

        card.setOnMouseExited(e -> {
            card.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.8);" +
                "-fx-background-radius: 16;" +
                "-fx-border-color: " + baseColor + ";" +
                "-fx-border-radius: 16;" +
                "-fx-border-width: 2;"
            );
            card.setEffect(shadow);
            card.setScaleX(1.0);
            card.setScaleY(1.0);
        });

        // Click handler
        card.setOnMouseClicked(e -> {
            if (onLocationClick != null) {
                onLocationClick.accept(location);
            }
        });

        return card;
    }

    public void setOnLocationClick(Consumer<LocationSummary> handler) {
        this.onLocationClick = handler;
    }

    public void setOnTotalClick(Consumer<Void> handler) {
        this.onTotalClick = handler;
    }

    public void cleanup() {
        // No animations to stop in card view
    }
}
