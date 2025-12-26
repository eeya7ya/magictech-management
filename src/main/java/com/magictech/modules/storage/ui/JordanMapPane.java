package com.magictech.modules.storage.ui;

import com.magictech.modules.storage.service.StorageLocationService.LocationSummary;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Interactive Jordan Map component for storage location visualization
 * Displays an SVG-style map of Jordan with clickable pins for each storage location
 */
public class JordanMapPane extends StackPane {

    private Pane mapContainer;
    private List<LocationPin> pins = new ArrayList<>();
    private Consumer<LocationSummary> onLocationClick;
    private Consumer<Void> onTotalClick;
    private List<Timeline> animations = new ArrayList<>();

    public JordanMapPane() {
        setupMap();
    }

    private void setupMap() {
        setStyle("-fx-background-color: transparent;");
        setPadding(new Insets(20));

        // Create gradient background for map area
        VBox mapWrapper = new VBox(20);
        mapWrapper.setAlignment(Pos.CENTER);
        mapWrapper.setStyle(
            "-fx-background-color: linear-gradient(to bottom, rgba(30, 41, 59, 0.8), rgba(15, 23, 42, 0.9));" +
            "-fx-background-radius: 20;" +
            "-fx-border-color: rgba(99, 102, 241, 0.3);" +
            "-fx-border-radius: 20;" +
            "-fx-border-width: 2;"
        );
        mapWrapper.setPadding(new Insets(30));
        mapWrapper.setMaxWidth(900);
        mapWrapper.setMaxHeight(750);

        // Title
        Label titleLabel = new Label("🗺️ Storage Locations Road Map");
        titleLabel.setStyle(
            "-fx-text-fill: white;" +
            "-fx-font-size: 28px;" +
            "-fx-font-weight: bold;"
        );

        Label subtitleLabel = new Label("Click on a location pin to view its storage sheet");
        subtitleLabel.setStyle(
            "-fx-text-fill: rgba(255, 255, 255, 0.6);" +
            "-fx-font-size: 14px;"
        );

        VBox titleBox = new VBox(5, titleLabel, subtitleLabel);
        titleBox.setAlignment(Pos.CENTER);

        // Map container with Jordan outline
        mapContainer = new Pane();
        mapContainer.setPrefSize(800, 550);
        mapContainer.setMinSize(800, 550);
        mapContainer.setMaxSize(800, 550);

        // Draw Jordan outline
        drawJordanOutline();

        // Create Total button at the bottom
        HBox totalButton = createTotalButton();

        mapWrapper.getChildren().addAll(titleBox, mapContainer, totalButton);
        VBox.setVgrow(mapContainer, Priority.ALWAYS);

        getChildren().add(mapWrapper);
    }

    private void drawJordanOutline() {
        // Simplified Jordan map outline as SVG path
        // Jordan is roughly shaped like an irregular quadrilateral
        // Coordinates normalized to fit 800x550 container

        Path jordanOutline = new Path();

        // Create simplified Jordan shape
        MoveTo start = new MoveTo(350, 10);  // North (near Syria border)

        // Northern border (with Syria)
        LineTo north1 = new LineTo(550, 15);
        LineTo north2 = new LineTo(620, 40);

        // Eastern border (with Iraq/Saudi)
        LineTo east1 = new LineTo(700, 120);
        LineTo east2 = new LineTo(750, 200);
        LineTo east3 = new LineTo(720, 350);

        // Southern tip (Aqaba region)
        LineTo south1 = new LineTo(600, 450);
        LineTo south2 = new LineTo(450, 520);
        LineTo south3 = new LineTo(380, 540);

        // Aqaba Gulf
        LineTo aqaba1 = new LineTo(350, 530);
        LineTo aqaba2 = new LineTo(320, 500);

        // Western border (with Israel/Palestine)
        LineTo west1 = new LineTo(280, 400);
        LineTo west2 = new LineTo(250, 300);
        LineTo west3 = new LineTo(270, 200);
        LineTo west4 = new LineTo(300, 100);
        LineTo west5 = new LineTo(350, 10);

        jordanOutline.getElements().addAll(
            start, north1, north2,
            east1, east2, east3,
            south1, south2, south3,
            aqaba1, aqaba2,
            west1, west2, west3, west4, west5
        );

        jordanOutline.setFill(Color.web("#1e293b", 0.6));
        jordanOutline.setStroke(Color.web("#6366f1", 0.8));
        jordanOutline.setStrokeWidth(3);
        jordanOutline.setStrokeLineCap(StrokeLineCap.ROUND);
        jordanOutline.setStrokeLineJoin(StrokeLineJoin.ROUND);

        // Add glow effect
        DropShadow glow = new DropShadow();
        glow.setColor(Color.web("#6366f1", 0.5));
        glow.setRadius(15);
        glow.setSpread(0.2);
        jordanOutline.setEffect(glow);

        // Add decorative grid lines
        for (int i = 0; i < 8; i++) {
            Line hLine = new Line(50, i * 70 + 30, 750, i * 70 + 30);
            hLine.setStroke(Color.web("#334155", 0.3));
            hLine.setStrokeWidth(1);
            hLine.getStrokeDashArray().addAll(5.0, 5.0);
            mapContainer.getChildren().add(hLine);

            Line vLine = new Line(i * 100 + 50, 10, i * 100 + 50, 540);
            vLine.setStroke(Color.web("#334155", 0.3));
            vLine.setStrokeWidth(1);
            vLine.getStrokeDashArray().addAll(5.0, 5.0);
            mapContainer.getChildren().add(vLine);
        }

        mapContainer.getChildren().add(jordanOutline);

        // Add Dead Sea
        Ellipse deadSea = new Ellipse(260, 280, 20, 50);
        deadSea.setFill(Color.web("#0ea5e9", 0.4));
        deadSea.setStroke(Color.web("#0ea5e9", 0.6));
        deadSea.setStrokeWidth(2);
        mapContainer.getChildren().add(deadSea);

        Label deadSeaLabel = new Label("Dead Sea");
        deadSeaLabel.setLayoutX(225);
        deadSeaLabel.setLayoutY(340);
        deadSeaLabel.setStyle("-fx-text-fill: rgba(14, 165, 233, 0.7); -fx-font-size: 10px;");
        mapContainer.getChildren().add(deadSeaLabel);

        // Add Jordan River
        Path jordanRiver = new Path();
        jordanRiver.getElements().addAll(
            new MoveTo(285, 50),
            new CubicCurveTo(280, 100, 270, 150, 265, 230)
        );
        jordanRiver.setStroke(Color.web("#0ea5e9", 0.5));
        jordanRiver.setStrokeWidth(3);
        jordanRiver.setFill(null);
        mapContainer.getChildren().add(jordanRiver);

        // Add compass
        addCompass();
    }

    private void addCompass() {
        VBox compass = new VBox(2);
        compass.setAlignment(Pos.CENTER);
        compass.setLayoutX(720);
        compass.setLayoutY(30);

        Label nLabel = new Label("N");
        nLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.8); -fx-font-size: 14px; -fx-font-weight: bold;");

        Circle compassCircle = new Circle(15);
        compassCircle.setFill(Color.TRANSPARENT);
        compassCircle.setStroke(Color.web("#6366f1", 0.6));
        compassCircle.setStrokeWidth(2);

        Line compassNeedle = new Line(0, 10, 0, -10);
        compassNeedle.setStroke(Color.web("#ef4444"));
        compassNeedle.setStrokeWidth(2);

        StackPane compassStack = new StackPane(compassCircle, compassNeedle);

        compass.getChildren().addAll(nLabel, compassStack);
        mapContainer.getChildren().add(compass);
    }

    /**
     * Add location pins to the map
     */
    public void setLocations(List<LocationSummary> locations) {
        // Clear existing pins
        pins.forEach(pin -> mapContainer.getChildren().remove(pin));
        pins.clear();
        stopAnimations();

        for (LocationSummary location : locations) {
            if (location.getMapX() != null && location.getMapY() != null) {
                // Convert percentage coordinates to actual positions
                double x = (location.getMapX() / 100.0) * 800;
                double y = (location.getMapY() / 100.0) * 550;

                LocationPin pin = new LocationPin(location, x, y);
                pin.setOnMouseClicked(e -> {
                    if (onLocationClick != null) {
                        onLocationClick.accept(location);
                    }
                });
                pins.add(pin);
                mapContainer.getChildren().add(pin);
            }
        }

        // Start pulse animations
        startPinAnimations();
    }

    private void startPinAnimations() {
        for (int i = 0; i < pins.size(); i++) {
            LocationPin pin = pins.get(i);

            // Staggered pulse animation
            Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(pin.scaleXProperty(), 1.0),
                    new KeyValue(pin.scaleYProperty(), 1.0)),
                new KeyFrame(Duration.millis(500),
                    new KeyValue(pin.scaleXProperty(), 1.1),
                    new KeyValue(pin.scaleYProperty(), 1.1)),
                new KeyFrame(Duration.millis(1000),
                    new KeyValue(pin.scaleXProperty(), 1.0),
                    new KeyValue(pin.scaleYProperty(), 1.0))
            );
            pulse.setCycleCount(Timeline.INDEFINITE);
            pulse.setDelay(Duration.millis(i * 150));
            pulse.play();
            animations.add(pulse);
        }
    }

    private void stopAnimations() {
        animations.forEach(Timeline::stop);
        animations.clear();
    }

    private HBox createTotalButton() {
        HBox buttonContainer = new HBox();
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setPadding(new Insets(10, 0, 0, 0));

        VBox totalCard = new VBox(8);
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
        textLabel.setStyle(
            "-fx-text-fill: white;" +
            "-fx-font-size: 16px;" +
            "-fx-font-weight: bold;"
        );

        Label subLabel = new Label("All items across all locations");
        subLabel.setStyle(
            "-fx-text-fill: rgba(255, 255, 255, 0.8);" +
            "-fx-font-size: 12px;"
        );

        totalCard.getChildren().addAll(iconLabel, textLabel, subLabel);

        // Hover effects
        totalCard.setOnMouseEntered(e -> {
            totalCard.setStyle(
                "-fx-background-color: linear-gradient(to right, #16a34a, #15803d);" +
                "-fx-background-radius: 12;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(34, 197, 94, 0.5), 15, 0, 0, 5);"
            );
        });

        totalCard.setOnMouseExited(e -> {
            totalCard.setStyle(
                "-fx-background-color: linear-gradient(to right, #22c55e, #16a34a);" +
                "-fx-background-radius: 12;" +
                "-fx-cursor: hand;"
            );
        });

        totalCard.setOnMouseClicked(e -> {
            if (onTotalClick != null) {
                onTotalClick.accept(null);
            }
        });

        buttonContainer.getChildren().add(totalCard);
        return buttonContainer;
    }

    public void setOnLocationClick(Consumer<LocationSummary> handler) {
        this.onLocationClick = handler;
    }

    public void setOnTotalClick(Consumer<Void> handler) {
        this.onTotalClick = handler;
    }

    public void cleanup() {
        stopAnimations();
    }

    /**
     * Inner class for location pins on the map
     */
    private class LocationPin extends VBox {
        private LocationSummary location;
        private Circle pinDot;
        private Circle pulseRing;

        public LocationPin(LocationSummary location, double x, double y) {
            this.location = location;

            setAlignment(Pos.CENTER);
            setSpacing(2);
            setLayoutX(x - 25);
            setLayoutY(y - 40);
            setCursor(Cursor.HAND);

            // Pin container
            StackPane pinStack = new StackPane();

            // Pulse ring (animated)
            pulseRing = new Circle(18);
            pulseRing.setFill(Color.TRANSPARENT);
            pulseRing.setStroke(Color.web(location.getColor(), 0.4));
            pulseRing.setStrokeWidth(2);

            // Main pin dot
            pinDot = new Circle(12);
            pinDot.setFill(Color.web(location.getColor()));
            pinDot.setStroke(Color.WHITE);
            pinDot.setStrokeWidth(3);

            // Drop shadow
            DropShadow shadow = new DropShadow();
            shadow.setColor(Color.web(location.getColor(), 0.6));
            shadow.setRadius(10);
            shadow.setSpread(0.3);
            pinDot.setEffect(shadow);

            // Icon in center
            Label iconLabel = new Label(location.getIcon());
            iconLabel.setStyle("-fx-font-size: 10px;");

            pinStack.getChildren().addAll(pulseRing, pinDot, iconLabel);

            // Location name label
            Label nameLabel = new Label(location.getLocationName().replace(" Storage", ""));
            nameLabel.setStyle(
                "-fx-text-fill: white;" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-color: rgba(0, 0, 0, 0.7);" +
                "-fx-padding: 2 6;" +
                "-fx-background-radius: 4;"
            );

            // Item count badge
            Label countLabel = new Label(String.valueOf(location.getItemCount()));
            countLabel.setStyle(
                "-fx-text-fill: white;" +
                "-fx-font-size: 9px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-color: " + location.getColor() + ";" +
                "-fx-padding: 1 5;" +
                "-fx-background-radius: 10;"
            );

            HBox labelBox = new HBox(4, nameLabel, countLabel);
            labelBox.setAlignment(Pos.CENTER);

            getChildren().addAll(pinStack, labelBox);

            // Tooltip
            Tooltip tooltip = new Tooltip(
                location.getLocationName() + "\n" +
                "📍 " + location.getCity() + "\n" +
                "📦 " + location.getItemCount() + " items\n" +
                "🔢 " + location.getTotalQuantity() + " total units"
            );
            tooltip.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.95);" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 12px;" +
                "-fx-padding: 10;" +
                "-fx-background-radius: 8;"
            );
            Tooltip.install(this, tooltip);

            // Hover effects
            setOnMouseEntered(e -> {
                setScaleX(1.15);
                setScaleY(1.15);
                pinDot.setFill(Color.web(location.getColor()).brighter());
            });

            setOnMouseExited(e -> {
                setScaleX(1.0);
                setScaleY(1.0);
                pinDot.setFill(Color.web(location.getColor()));
            });

            // Pulse animation for the ring
            Timeline ringPulse = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(pulseRing.scaleXProperty(), 1.0),
                    new KeyValue(pulseRing.scaleYProperty(), 1.0),
                    new KeyValue(pulseRing.opacityProperty(), 0.6)),
                new KeyFrame(Duration.seconds(1.5),
                    new KeyValue(pulseRing.scaleXProperty(), 1.8),
                    new KeyValue(pulseRing.scaleYProperty(), 1.8),
                    new KeyValue(pulseRing.opacityProperty(), 0.0))
            );
            ringPulse.setCycleCount(Timeline.INDEFINITE);
            ringPulse.play();
            animations.add(ringPulse);
        }
    }
}
