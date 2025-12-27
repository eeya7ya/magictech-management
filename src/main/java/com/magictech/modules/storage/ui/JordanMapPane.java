package com.magictech.modules.storage.ui;

import com.magictech.modules.storage.service.StorageLocationService.LocationSummary;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Interactive Jordan Map component with REAL Jordan borders
 * Features: Zoom in/out, pan, clickable location pins
 */
public class JordanMapPane extends StackPane {

    // Map dimensions
    private static final double MAP_WIDTH = 600;
    private static final double MAP_HEIGHT = 700;

    // Zoom settings - enhanced for better control
    private static final double MIN_ZOOM = 0.4;
    private static final double MAX_ZOOM = 6.0;
    private static final double ZOOM_STEP = 0.15;
    private double currentZoom = 1.0;

    // Pin sizing constants for dynamic scaling
    private static final double BASE_PIN_SCALE = 1.0;
    private static final double MIN_PIN_SCALE = 0.5;  // Minimum pin size at max zoom out
    private static final double MAX_PIN_SCALE = 1.2;  // Maximum pin size at max zoom in

    // Components
    private Pane mapContainer;
    private Group mapGroup;
    private ScrollPane scrollPane;
    private List<LocationPin> pins = new ArrayList<>();
    private List<Timeline> animations = new ArrayList<>();

    // Callbacks
    private Consumer<LocationSummary> onLocationClick;
    private Consumer<Void> onTotalClick;

    // Drag handling
    private double lastMouseX, lastMouseY;

    public JordanMapPane() {
        setupMap();
    }

    private void setupMap() {
        setStyle("-fx-background-color: transparent;");

        // Main wrapper
        VBox mainWrapper = new VBox(15);
        mainWrapper.setAlignment(Pos.CENTER);
        mainWrapper.setPadding(new Insets(20));
        mainWrapper.setStyle(
            "-fx-background-color: linear-gradient(to bottom, rgba(30, 41, 59, 0.9), rgba(15, 23, 42, 0.95));" +
            "-fx-background-radius: 20;" +
            "-fx-border-color: rgba(99, 102, 241, 0.4);" +
            "-fx-border-radius: 20;" +
            "-fx-border-width: 2;"
        );

        // Title
        HBox titleBar = createTitleBar();

        // Map area with scroll/zoom
        StackPane mapArea = createMapArea();
        VBox.setVgrow(mapArea, Priority.ALWAYS);

        // Zoom controls
        HBox zoomControls = createZoomControls();

        // Total button
        HBox totalButton = createTotalButton();

        mainWrapper.getChildren().addAll(titleBar, mapArea, zoomControls, totalButton);
        getChildren().add(mainWrapper);
    }

    private HBox createTitleBar() {
        HBox titleBar = new HBox(15);
        titleBar.setAlignment(Pos.CENTER);

        Label iconLabel = new Label("🗺️");
        iconLabel.setFont(new Font(28));

        VBox titleBox = new VBox(2);
        titleBox.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Jordan Storage Locations Map");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

        Label subtitleLabel = new Label("Click on a location to view its storage • Use mouse wheel to zoom");
        subtitleLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.6); -fx-font-size: 12px;");

        titleBox.getChildren().addAll(titleLabel, subtitleLabel);
        titleBar.getChildren().addAll(iconLabel, titleBox);

        return titleBar;
    }

    private StackPane createMapArea() {
        StackPane mapArea = new StackPane();
        mapArea.setStyle("-fx-background-color: rgba(20, 30, 50, 0.5); -fx-background-radius: 15;");
        mapArea.setPadding(new Insets(10));

        // Create the map container
        mapContainer = new Pane();
        mapContainer.setPrefSize(MAP_WIDTH, MAP_HEIGHT);
        mapContainer.setMinSize(MAP_WIDTH, MAP_HEIGHT);

        // Create map group for zooming
        mapGroup = new Group();

        // Draw the real Jordan map
        drawJordanMap();

        mapContainer.getChildren().add(mapGroup);

        // Wrap in scroll pane for panning when zoomed
        scrollPane = new ScrollPane(mapContainer);
        scrollPane.setPannable(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle(
            "-fx-background: transparent;" +
            "-fx-background-color: transparent;" +
            "-fx-border-color: transparent;"
        );
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        // Mouse wheel zoom
        scrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.getDeltaY() > 0) {
                zoomIn();
            } else {
                zoomOut();
            }
            event.consume();
        });

        mapArea.getChildren().add(scrollPane);
        return mapArea;
    }

    private void drawJordanMap() {
        // Accurate Jordan border from GeoJSON coordinates (Natural Earth data)
        // Source: https://github.com/johan/world.geo.json
        // Coordinates converted from [lon, lat] to SVG coordinates
        // Bounds: lon [34.92, 39.20], lat [29.20, 33.38]
        // Scale: 120 pixels per degree, offset for centering

        // Jordan SVG path - accurate geographic border
        String jordanPathData =
            // Point 1: Northwest (near Umm Qais) [35.545665, 32.393992]
            "M 125 148 " +
            // Point 2: [35.719918, 32.709192]
            "L 146 110 " +
            // Point 3: [36.834062, 32.312938]
            "L 279 158 " +
            // Point 4: Northeast corner (Syria/Iraq) [38.792341, 33.378686]
            "L 514 30 " +
            // Point 5: East (Iraq border) [39.195468, 32.161009]
            "L 563 176 " +
            // Point 6: [39.004886, 32.010217]
            "L 540 194 " +
            // Point 7: [37.002166, 31.508413]
            "L 300 254 " +
            // Point 8: [37.998849, 30.5085]
            "L 419 374 " +
            // Point 9: [37.66812, 30.338665]
            "L 379 395 " +
            // Point 10: [37.503582, 30.003776]
            "L 360 435 " +
            // Point 11: [36.740528, 29.865283]
            "L 268 452 " +
            // Point 12: [36.501214, 29.505254]
            "L 240 495 " +
            // Point 13: Southernmost (near Aqaba) [36.068941, 29.197495]
            "L 188 532 " +
            // Point 14: Aqaba west [34.956037, 29.356555]
            "L 54 513 " +
            // Point 15: Aqaba tip [34.922603, 29.501326]
            "L 50 495 " +
            // Point 16: Dead Sea south [35.420918, 31.100066]
            "L 110 303 " +
            // Point 17: Dead Sea middle [35.397561, 31.489086]
            "L 107 257 " +
            // Point 18: Dead Sea north [35.545252, 31.782505]
            "L 125 222 " +
            // Close path back to start
            "L 125 148 Z";

        SVGPath jordanBorder = new SVGPath();
        jordanBorder.setContent(jordanPathData);

        // Jordan fill with gradient
        LinearGradient jordanFill = new LinearGradient(
            0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#1e3a5f", 0.8)),
            new Stop(0.5, Color.web("#1e293b", 0.9)),
            new Stop(1, Color.web("#0f172a", 0.85))
        );
        jordanBorder.setFill(jordanFill);
        jordanBorder.setStroke(Color.web("#6366f1", 0.9));
        jordanBorder.setStrokeWidth(3);
        jordanBorder.setStrokeLineCap(StrokeLineCap.ROUND);
        jordanBorder.setStrokeLineJoin(StrokeLineJoin.ROUND);

        // Glow effect
        DropShadow glow = new DropShadow();
        glow.setColor(Color.web("#6366f1", 0.6));
        glow.setRadius(20);
        glow.setSpread(0.2);
        jordanBorder.setEffect(glow);

        mapGroup.getChildren().add(jordanBorder);

        // Add Dead Sea
        drawDeadSea();

        // Add major geographical labels (Jordan cities only)
        addGeographicalLabels();

        // Note: Neighboring country labels removed per user request

        // Add compass
        addCompass();

        // Add scale indicator
        addScaleIndicator();
    }

    private void drawDeadSea() {
        // Dead Sea shape - positioned along western border (between points 16-18)
        SVGPath deadSea = new SVGPath();
        deadSea.setContent(
            "M 115 230 " +
            "Q 100 260 105 290 " +
            "Q 108 310 115 330 " +
            "Q 125 310 120 290 " +
            "Q 118 260 115 230 Z"
        );
        deadSea.setFill(Color.web("#0ea5e9", 0.5));
        deadSea.setStroke(Color.web("#0ea5e9", 0.7));
        deadSea.setStrokeWidth(2);

        // Dead Sea label
        Text deadSeaLabel = new Text("Dead Sea");
        deadSeaLabel.setX(40);
        deadSeaLabel.setY(280);
        deadSeaLabel.setFill(Color.web("#0ea5e9", 0.8));
        deadSeaLabel.setFont(Font.font("Arial", FontWeight.BOLD, 10));

        // Gulf of Aqaba - at the southern tip (around point 15)
        SVGPath aqabaGulf = new SVGPath();
        aqabaGulf.setContent(
            "M 45 500 " +
            "L 40 530 " +
            "L 55 520 " +
            "Q 50 510 45 500 Z"
        );
        aqabaGulf.setFill(Color.web("#0ea5e9", 0.6));
        aqabaGulf.setStroke(Color.web("#0ea5e9", 0.8));
        aqabaGulf.setStrokeWidth(1);

        Text aqabaLabel = new Text("Gulf of\nAqaba");
        aqabaLabel.setX(5);
        aqabaLabel.setY(545);
        aqabaLabel.setFill(Color.web("#0ea5e9", 0.7));
        aqabaLabel.setFont(Font.font("Arial", 9));

        mapGroup.getChildren().addAll(deadSea, deadSeaLabel, aqabaGulf, aqabaLabel);
    }

    private void addGeographicalLabels() {
        // City labels - positioned based on actual GeoJSON coordinates
        // Amman: approximately [35.93, 31.95] -> SVG ~(170, 201)

        Text ammanRegion = new Text("AMMAN");
        ammanRegion.setX(170);
        ammanRegion.setY(200);
        ammanRegion.setFill(Color.web("#ffffff", 0.25));
        ammanRegion.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        // Wadi Rum: approximately [35.4, 29.6] -> SVG ~(107, 483)
        Text wadiRum = new Text("Wadi Rum");
        wadiRum.setX(120);
        wadiRum.setY(470);
        wadiRum.setFill(Color.web("#f59e0b", 0.4));
        wadiRum.setFont(Font.font("Arial", FontWeight.NORMAL, 10));

        // Madaba: approximately [35.8, 31.72] -> SVG ~(155, 229)
        Text madaba = new Text("Madaba");
        madaba.setX(140);
        madaba.setY(240);
        madaba.setFill(Color.web("#ffffff", 0.15));
        madaba.setFont(Font.font("Arial", FontWeight.NORMAL, 9));

        // Kerak: approximately [35.7, 31.18] -> SVG ~(143, 294)
        Text kerak = new Text("Kerak");
        kerak.setX(135);
        kerak.setY(310);
        kerak.setFill(Color.web("#ffffff", 0.15));
        kerak.setFont(Font.font("Arial", FontWeight.NORMAL, 9));

        // Petra: approximately [35.44, 30.33] -> SVG ~(112, 396)
        Text petra = new Text("Petra");
        petra.setX(130);
        petra.setY(400);
        petra.setFill(Color.web("#ffffff", 0.15));
        petra.setFont(Font.font("Arial", FontWeight.NORMAL, 9));

        // Aqaba: approximately [35.0, 29.53] -> SVG ~(59, 492)
        Text aqaba = new Text("Aqaba");
        aqaba.setX(60);
        aqaba.setY(505);
        aqaba.setFill(Color.web("#ffffff", 0.15));
        aqaba.setFont(Font.font("Arial", FontWeight.NORMAL, 9));

        // Irbid: approximately [35.85, 32.55] -> SVG ~(161, 129)
        Text irbid = new Text("Irbid");
        irbid.setX(155);
        irbid.setY(130);
        irbid.setFill(Color.web("#ffffff", 0.15));
        irbid.setFont(Font.font("Arial", FontWeight.NORMAL, 9));

        // Jerash: approximately [35.9, 32.28] -> SVG ~(167, 161)
        Text jerash = new Text("Jerash");
        jerash.setX(165);
        jerash.setY(165);
        jerash.setFill(Color.web("#ffffff", 0.15));
        jerash.setFont(Font.font("Arial", FontWeight.NORMAL, 9));

        mapGroup.getChildren().addAll(ammanRegion, wadiRum, madaba, kerak, petra, aqaba, irbid, jerash);
    }

    private void addNeighborLabels() {
        // Neighboring countries - positioned outside the new border shape
        Text syria = new Text("SYRIA");
        syria.setX(200);
        syria.setY(60);
        syria.setFill(Color.web("#64748b", 0.5));
        syria.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        Text iraq = new Text("IRAQ");
        iraq.setX(480);
        iraq.setY(120);
        iraq.setFill(Color.web("#64748b", 0.5));
        iraq.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        Text saudiArabia = new Text("SAUDI ARABIA");
        saudiArabia.setX(300);
        saudiArabia.setY(480);
        saudiArabia.setFill(Color.web("#64748b", 0.5));
        saudiArabia.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        Text israel = new Text("ISRAEL");
        israel.setX(20);
        israel.setY(350);
        israel.setFill(Color.web("#64748b", 0.5));
        israel.setFont(Font.font("Arial", FontWeight.BOLD, 11));

        Text palestine = new Text("WEST\nBANK");
        palestine.setX(50);
        palestine.setY(200);
        palestine.setFill(Color.web("#64748b", 0.4));
        palestine.setFont(Font.font("Arial", 9));

        mapGroup.getChildren().addAll(syria, iraq, saudiArabia, israel, palestine);
    }

    private void addCompass() {
        Group compass = new Group();

        // Compass circle
        Circle compassBg = new Circle(30);
        compassBg.setFill(Color.web("#1e293b", 0.8));
        compassBg.setStroke(Color.web("#6366f1", 0.6));
        compassBg.setStrokeWidth(2);

        // N indicator
        Text nLabel = new Text("N");
        nLabel.setX(-6);
        nLabel.setY(-12);
        nLabel.setFill(Color.web("#ef4444"));
        nLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        // Arrow
        Polygon arrow = new Polygon(0, -20, -6, -5, 6, -5);
        arrow.setFill(Color.web("#ef4444"));

        // S, E, W labels
        Text sLabel = new Text("S");
        sLabel.setX(-5);
        sLabel.setY(22);
        sLabel.setFill(Color.web("#94a3b8", 0.7));
        sLabel.setFont(Font.font("Arial", 10));

        Text eLabel = new Text("E");
        eLabel.setX(15);
        eLabel.setY(5);
        eLabel.setFill(Color.web("#94a3b8", 0.7));
        eLabel.setFont(Font.font("Arial", 10));

        Text wLabel = new Text("W");
        wLabel.setX(-24);
        wLabel.setY(5);
        wLabel.setFill(Color.web("#94a3b8", 0.7));
        wLabel.setFont(Font.font("Arial", 10));

        compass.getChildren().addAll(compassBg, arrow, nLabel, sLabel, eLabel, wLabel);
        compass.setTranslateX(550);
        compass.setTranslateY(50);

        mapGroup.getChildren().add(compass);
    }

    private void addScaleIndicator() {
        Group scale = new Group();

        // Scale bar
        Rectangle scaleBar = new Rectangle(80, 6);
        scaleBar.setFill(Color.web("#ffffff", 0.8));
        scaleBar.setStroke(Color.web("#1e293b"));
        scaleBar.setStrokeWidth(1);
        scaleBar.setArcWidth(3);
        scaleBar.setArcHeight(3);

        // Scale divisions
        Line div1 = new Line(0, 0, 0, 8);
        div1.setStroke(Color.web("#1e293b"));
        Line div2 = new Line(40, 0, 40, 8);
        div2.setStroke(Color.web("#1e293b"));
        Line div3 = new Line(80, 0, 80, 8);
        div3.setStroke(Color.web("#1e293b"));

        Text scaleLabel = new Text("0     50    100 km");
        scaleLabel.setY(20);
        scaleLabel.setFill(Color.web("#94a3b8", 0.8));
        scaleLabel.setFont(Font.font("Arial", 9));

        scale.getChildren().addAll(scaleBar, div1, div2, div3, scaleLabel);
        scale.setTranslateX(500);
        scale.setTranslateY(580);

        mapGroup.getChildren().add(scale);
    }

    private HBox createZoomControls() {
        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(5));

        Button zoomInBtn = new Button("+");
        styleZoomButton(zoomInBtn);
        zoomInBtn.setOnAction(e -> zoomIn());

        Button zoomOutBtn = new Button("−");
        styleZoomButton(zoomOutBtn);
        zoomOutBtn.setOnAction(e -> zoomOut());

        Button resetBtn = new Button("⟲ Reset");
        resetBtn.setStyle(
            "-fx-background-color: rgba(99, 102, 241, 0.3);" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 12px;" +
            "-fx-padding: 8 15;" +
            "-fx-background-radius: 6;" +
            "-fx-cursor: hand;"
        );
        resetBtn.setOnAction(e -> resetZoom());

        Label zoomLabel = new Label("Zoom: 100%");
        zoomLabel.setId("zoomLabel");
        zoomLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 12px;");

        controls.getChildren().addAll(zoomOutBtn, zoomLabel, zoomInBtn, resetBtn);
        return controls;
    }

    private void styleZoomButton(Button btn) {
        btn.setStyle(
            "-fx-background-color: rgba(99, 102, 241, 0.5);" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 18px;" +
            "-fx-font-weight: bold;" +
            "-fx-min-width: 40px;" +
            "-fx-min-height: 40px;" +
            "-fx-background-radius: 20;" +
            "-fx-cursor: hand;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(
            "-fx-background-color: rgba(99, 102, 241, 0.8);" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 18px;" +
            "-fx-font-weight: bold;" +
            "-fx-min-width: 40px;" +
            "-fx-min-height: 40px;" +
            "-fx-background-radius: 20;" +
            "-fx-cursor: hand;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
            "-fx-background-color: rgba(99, 102, 241, 0.5);" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 18px;" +
            "-fx-font-weight: bold;" +
            "-fx-min-width: 40px;" +
            "-fx-min-height: 40px;" +
            "-fx-background-radius: 20;" +
            "-fx-cursor: hand;"
        ));
    }

    private void zoomIn() {
        if (currentZoom < MAX_ZOOM) {
            currentZoom += ZOOM_STEP;
            applyZoom();
        }
    }

    private void zoomOut() {
        if (currentZoom > MIN_ZOOM) {
            currentZoom -= ZOOM_STEP;
            applyZoom();
        }
    }

    private void resetZoom() {
        currentZoom = 1.0;
        applyZoom();
        scrollPane.setHvalue(0.5);
        scrollPane.setVvalue(0.5);
    }

    private void applyZoom() {
        mapGroup.setScaleX(currentZoom);
        mapGroup.setScaleY(currentZoom);

        // Update zoom label
        Label zoomLabel = (Label) lookup("#zoomLabel");
        if (zoomLabel != null) {
            zoomLabel.setText(String.format("Zoom: %.0f%%", currentZoom * 100));
        }

        // Adjust container size for scrolling
        double newWidth = MAP_WIDTH * currentZoom;
        double newHeight = MAP_HEIGHT * currentZoom;
        mapContainer.setPrefSize(newWidth, newHeight);
        mapContainer.setMinSize(newWidth, newHeight);

        // Dynamic pin scaling: pins get smaller when zoomed out, larger when zoomed in
        // This prevents overlap at low zoom levels and maintains visibility at high zoom
        double pinScale = calculatePinScale(currentZoom);
        for (LocationPin pin : pins) {
            pin.applyDynamicScale(pinScale, currentZoom);
        }
    }

    /**
     * Calculate pin scale based on zoom level
     * At zoom 1.0, pins are normal size
     * At min zoom, pins are smaller to prevent overlap
     * At max zoom, pins are slightly larger for better visibility
     */
    private double calculatePinScale(double zoom) {
        if (zoom <= 1.0) {
            // When zoomed out, scale down pins progressively
            // At MIN_ZOOM (0.4), pins should be at MIN_PIN_SCALE (0.5)
            // At zoom 1.0, pins should be at BASE_PIN_SCALE (1.0)
            double t = (zoom - MIN_ZOOM) / (1.0 - MIN_ZOOM);
            return MIN_PIN_SCALE + t * (BASE_PIN_SCALE - MIN_PIN_SCALE);
        } else {
            // When zoomed in, pins stay relatively same size but can be slightly larger
            // At zoom 1.0, pins are at BASE_PIN_SCALE
            // At MAX_ZOOM (6.0), pins are at MAX_PIN_SCALE (1.2)
            double t = (zoom - 1.0) / (MAX_ZOOM - 1.0);
            return BASE_PIN_SCALE + t * (MAX_PIN_SCALE - BASE_PIN_SCALE);
        }
    }

    private HBox createTotalButton() {
        HBox buttonContainer = new HBox();
        buttonContainer.setAlignment(Pos.CENTER);

        VBox totalCard = new VBox(5);
        totalCard.setAlignment(Pos.CENTER);
        totalCard.setPadding(new Insets(12, 35, 12, 35));
        totalCard.setStyle(
            "-fx-background-color: linear-gradient(to right, #22c55e, #16a34a);" +
            "-fx-background-radius: 10;" +
            "-fx-cursor: hand;"
        );

        Label iconLabel = new Label("📊");
        iconLabel.setFont(new Font(20));

        Label textLabel = new Label("VIEW TOTAL SHEET");
        textLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label subLabel = new Label("All items across all locations");
        subLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.8); -fx-font-size: 11px;");

        totalCard.getChildren().addAll(iconLabel, textLabel, subLabel);

        totalCard.setOnMouseEntered(e -> totalCard.setStyle(
            "-fx-background-color: linear-gradient(to right, #16a34a, #15803d);" +
            "-fx-background-radius: 10;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(34, 197, 94, 0.5), 15, 0, 0, 5);"
        ));

        totalCard.setOnMouseExited(e -> totalCard.setStyle(
            "-fx-background-color: linear-gradient(to right, #22c55e, #16a34a);" +
            "-fx-background-radius: 10;" +
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
     * Set locations to display on the map
     */
    public void setLocations(List<LocationSummary> locations) {
        // Clear existing pins
        pins.forEach(pin -> mapGroup.getChildren().remove(pin));
        pins.clear();
        stopAnimations();

        for (LocationSummary location : locations) {
            if (location.getMapX() != null && location.getMapY() != null) {
                // Convert percentage to actual map coordinates
                double x = (location.getMapX() / 100.0) * MAP_WIDTH;
                double y = (location.getMapY() / 100.0) * MAP_HEIGHT;

                LocationPin pin = new LocationPin(location, x, y);
                pin.setOnMouseClicked(e -> {
                    if (onLocationClick != null) {
                        onLocationClick.accept(location);
                    }
                });
                pins.add(pin);
                mapGroup.getChildren().add(pin);
            }
        }

        startPinAnimations();
    }

    private void startPinAnimations() {
        for (int i = 0; i < pins.size(); i++) {
            LocationPin pin = pins.get(i);

            Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(pin.scaleXProperty(), 1.0),
                    new KeyValue(pin.scaleYProperty(), 1.0)),
                new KeyFrame(Duration.millis(600),
                    new KeyValue(pin.scaleXProperty(), 1.08),
                    new KeyValue(pin.scaleYProperty(), 1.08)),
                new KeyFrame(Duration.millis(1200),
                    new KeyValue(pin.scaleXProperty(), 1.0),
                    new KeyValue(pin.scaleYProperty(), 1.0))
            );
            pulse.setCycleCount(Timeline.INDEFINITE);
            pulse.setDelay(Duration.millis(i * 100));
            pulse.play();
            animations.add(pulse);
        }
    }

    private void stopAnimations() {
        animations.forEach(Timeline::stop);
        animations.clear();
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
     * Location pin component with dynamic scaling support
     */
    private class LocationPin extends VBox {
        private LocationSummary location;
        private Circle pulseRing;
        private VBox labelBox;
        private Label nameLabel;
        private Label countLabel;
        private double currentPinScale = 1.0;
        private double currentMapZoom = 1.0;

        public LocationPin(LocationSummary location, double x, double y) {
            this.location = location;

            setAlignment(Pos.CENTER);
            setSpacing(3);
            setLayoutX(x - 30);
            setLayoutY(y - 50);
            setCursor(Cursor.HAND);

            // Pin visual
            StackPane pinStack = new StackPane();

            // Outer pulse ring
            pulseRing = new Circle(20);
            pulseRing.setFill(Color.TRANSPARENT);
            pulseRing.setStroke(Color.web(location.getColor(), 0.3));
            pulseRing.setStrokeWidth(2);

            // Pin marker (teardrop shape)
            SVGPath pinShape = new SVGPath();
            pinShape.setContent("M 0 -20 C -12 -20 -18 -10 -18 0 C -18 12 0 28 0 28 C 0 28 18 12 18 0 C 18 -10 12 -20 0 -20 Z");
            pinShape.setFill(Color.web(location.getColor()));
            pinShape.setStroke(Color.WHITE);
            pinShape.setStrokeWidth(2);

            DropShadow shadow = new DropShadow();
            shadow.setColor(Color.web(location.getColor(), 0.7));
            shadow.setRadius(10);
            shadow.setSpread(0.3);
            pinShape.setEffect(shadow);

            // Inner circle for icon
            Circle innerCircle = new Circle(8);
            innerCircle.setFill(Color.WHITE);
            innerCircle.setTranslateY(-8);

            // Icon
            Label iconLabel = new Label(location.getIcon());
            iconLabel.setStyle("-fx-font-size: 10px;");
            iconLabel.setTranslateY(-8);

            pinStack.getChildren().addAll(pulseRing, pinShape, innerCircle, iconLabel);

            // Label below pin
            labelBox = new VBox(1);
            labelBox.setAlignment(Pos.CENTER);

            nameLabel = new Label(location.getLocationName().replace(" Storage", ""));
            nameLabel.setStyle(
                "-fx-text-fill: white;" +
                "-fx-font-size: 10px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-color: rgba(0, 0, 0, 0.75);" +
                "-fx-padding: 2 6;" +
                "-fx-background-radius: 4;"
            );

            countLabel = new Label(location.getItemCount() + " items");
            countLabel.setStyle(
                "-fx-text-fill: " + location.getColor() + ";" +
                "-fx-font-size: 9px;" +
                "-fx-font-weight: bold;"
            );

            labelBox.getChildren().addAll(nameLabel, countLabel);
            getChildren().addAll(pinStack, labelBox);

            // Tooltip
            Tooltip tooltip = new Tooltip(
                location.getLocationName() + "\n" +
                "📍 " + location.getCity() + "\n" +
                "📦 " + location.getItemCount() + " unique items\n" +
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

            // Hover effects - account for dynamic scaling
            setOnMouseEntered(e -> {
                double effectiveScale = (currentPinScale / currentMapZoom) * 1.2;
                setScaleX(effectiveScale);
                setScaleY(effectiveScale);
                toFront();
            });

            setOnMouseExited(e -> {
                applyDynamicScale(currentPinScale, currentMapZoom);
            });

            // Pulse animation for ring
            Timeline ringPulse = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(pulseRing.scaleXProperty(), 1.0),
                    new KeyValue(pulseRing.scaleYProperty(), 1.0),
                    new KeyValue(pulseRing.opacityProperty(), 0.5)),
                new KeyFrame(Duration.seconds(1.5),
                    new KeyValue(pulseRing.scaleXProperty(), 2.0),
                    new KeyValue(pulseRing.scaleYProperty(), 2.0),
                    new KeyValue(pulseRing.opacityProperty(), 0.0))
            );
            ringPulse.setCycleCount(Timeline.INDEFINITE);
            ringPulse.play();
            animations.add(ringPulse);
        }

        /**
         * Apply dynamic scaling based on zoom level
         * Pins get smaller when zoomed out, preventing overlap
         */
        public void applyDynamicScale(double pinScale, double mapZoom) {
            this.currentPinScale = pinScale;
            this.currentMapZoom = mapZoom;

            // Calculate effective scale: counteract map zoom and apply pin scale
            double effectiveScale = pinScale / mapZoom;
            setScaleX(effectiveScale);
            setScaleY(effectiveScale);

            // Adjust label visibility based on zoom
            // Hide labels when very zoomed out to reduce clutter
            if (mapZoom < 0.7) {
                labelBox.setVisible(false);
            } else if (mapZoom < 1.0) {
                // Show only count, hide name when moderately zoomed out
                labelBox.setVisible(true);
                nameLabel.setVisible(false);
                countLabel.setVisible(true);
            } else {
                // Show full labels when zoomed in
                labelBox.setVisible(true);
                nameLabel.setVisible(true);
                countLabel.setVisible(true);
            }
        }
    }
}
