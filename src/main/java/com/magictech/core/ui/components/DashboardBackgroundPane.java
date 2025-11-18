package com.magictech.core.ui.components;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

/**
 * Professional dashboard background with subtle animated gradients
 * Designed to be clean and not distract from content
 */
public class DashboardBackgroundPane extends Pane {

    private final Canvas canvas;
    private final AnimationTimer animationTimer;
    private double time = 0;
    private final DashboardGradientConfig config;

    public DashboardBackgroundPane() {
        this(DashboardGradientConfig.PROFESSIONAL_DARK);
    }

    public DashboardBackgroundPane(DashboardGradientConfig config) {
        this.config = config;
        this.canvas = new Canvas();

        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());

        canvas.widthProperty().addListener(obs -> draw());
        canvas.heightProperty().addListener(obs -> draw());

        getChildren().add(canvas);

        if (config.animated) {
            animationTimer = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    time += 0.003; // Slower for dashboard
                    draw();
                }
            };
            animationTimer.start();
        } else {
            animationTimer = null;
            draw();
        }
    }

    private void draw() {
        double width = canvas.getWidth();
        double height = canvas.getHeight();

        if (width == 0 || height == 0) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);

        // Base gradient background
        drawBaseGradient(gc, width, height);

        // Subtle animated accent orbs
        if (config.useAccentOrbs) {
            drawAccentOrbs(gc, width, height);
        }

        // Optional grid pattern overlay
        if (config.useGridPattern) {
            drawGridPattern(gc, width, height);
        }
    }

    private void drawBaseGradient(GraphicsContext gc, double width, double height) {
        // Diagonal gradient from top-left to bottom-right
        LinearGradient baseGradient = new LinearGradient(
                0, 0, width, height,
                false, CycleMethod.NO_CYCLE,
                new Stop(0, config.baseColorTop),
                new Stop(0.5, config.baseColorMid),
                new Stop(1, config.baseColorBottom)
        );

        gc.setFill(baseGradient);
        gc.fillRect(0, 0, width, height);
    }

    private void drawAccentOrbs(GraphicsContext gc, double width, double height) {
        // Top-left accent (subtle)
        double x1 = width * 0.15 + Math.sin(time * 0.4) * width * 0.05;
        double y1 = height * 0.15 + Math.cos(time * 0.3) * height * 0.05;
        drawSubtleOrb(gc, x1, y1, width * 0.35, config.accentColor1);

        // Top-right accent
        double x2 = width * 0.85 + Math.cos(time * 0.35) * width * 0.05;
        double y2 = height * 0.2 + Math.sin(time * 0.45) * height * 0.05;
        drawSubtleOrb(gc, x2, y2, width * 0.3, config.accentColor2);

        // Bottom-center accent
        double x3 = width * 0.5 + Math.sin(time * 0.5) * width * 0.08;
        double y3 = height * 0.85 + Math.cos(time * 0.4) * height * 0.05;
        drawSubtleOrb(gc, x3, y3, width * 0.4, config.accentColor3);

        // Additional subtle accent
        if (config.useExtraAccent) {
            double x4 = width * 0.3 + Math.cos(time * 0.6) * width * 0.06;
            double y4 = height * 0.6 + Math.sin(time * 0.35) * height * 0.06;
            drawSubtleOrb(gc, x4, y4, width * 0.25, config.accentColor4);
        }
    }

    private void drawSubtleOrb(GraphicsContext gc, double x, double y, double radius, Color color) {
        // Main orb with very subtle opacity
        RadialGradient gradient = new RadialGradient(
                0, 0, x, y, radius,
                false, CycleMethod.NO_CYCLE,
                new Stop(0, color),
                new Stop(0.6, Color.color(color.getRed(), color.getGreen(), color.getBlue(), color.getOpacity() * 0.3)),
                new Stop(1, Color.TRANSPARENT)
        );

        gc.setFill(gradient);
        gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);

        // Soft glow layers (less intense than login)
        for (int i = 1; i <= 2; i++) {
            double scale = 1 + (i * 0.15);
            Color glowColor = Color.color(
                    color.getRed(),
                    color.getGreen(),
                    color.getBlue(),
                    color.getOpacity() * (0.15 / i)
            );

            RadialGradient glow = new RadialGradient(
                    0, 0, x, y, radius * scale,
                    false, CycleMethod.NO_CYCLE,
                    new Stop(0, glowColor),
                    new Stop(1, Color.TRANSPARENT)
            );

            gc.setFill(glow);
            gc.fillOval(x - radius * scale, y - radius * scale,
                    radius * 2 * scale, radius * 2 * scale);
        }
    }

    private void drawGridPattern(GraphicsContext gc, double width, double height) {
        // Subtle grid lines for professional look
        gc.setStroke(Color.color(1, 1, 1, 0.02));
        gc.setLineWidth(1);

        double gridSize = 50;

        // Vertical lines
        for (double x = 0; x < width; x += gridSize) {
            gc.strokeLine(x, 0, x, height);
        }

        // Horizontal lines
        for (double y = 0; y < height; y += gridSize) {
            gc.strokeLine(0, y, width, y);
        }
    }

    public void stopAnimation() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
    }

    public void startAnimation() {
        if (animationTimer != null && config.animated) {
            animationTimer.start();
        }
    }

    /**
     * Dashboard gradient configuration presets
     */
    public static class DashboardGradientConfig {
        final Color baseColorTop;
        final Color baseColorMid;
        final Color baseColorBottom;
        final Color accentColor1;
        final Color accentColor2;
        final Color accentColor3;
        final Color accentColor4;
        final boolean animated;
        final boolean useAccentOrbs;
        final boolean useGridPattern;
        final boolean useExtraAccent;

        public DashboardGradientConfig(
                Color baseColorTop, Color baseColorMid, Color baseColorBottom,
                Color accentColor1, Color accentColor2, Color accentColor3, Color accentColor4,
                boolean animated, boolean useAccentOrbs, boolean useGridPattern, boolean useExtraAccent) {

            this.baseColorTop = baseColorTop;
            this.baseColorMid = baseColorMid;
            this.baseColorBottom = baseColorBottom;
            this.accentColor1 = accentColor1;
            this.accentColor2 = accentColor2;
            this.accentColor3 = accentColor3;
            this.accentColor4 = accentColor4;
            this.animated = animated;
            this.useAccentOrbs = useAccentOrbs;
            this.useGridPattern = useGridPattern;
            this.useExtraAccent = useExtraAccent;
        }

        // Professional dark theme (matches your current design)
        public static final DashboardGradientConfig PROFESSIONAL_DARK = new DashboardGradientConfig(
                Color.rgb(15, 20, 28),                    // Very dark blue-gray
                Color.rgb(18, 24, 35),                    // Dark blue-gray
                Color.rgb(12, 17, 26),                    // Darker blue-gray
                Color.rgb(139, 92, 246, 0.12),           // Purple accent (subtle)
                Color.rgb(59, 130, 246, 0.10),           // Blue accent
                Color.rgb(99, 102, 241, 0.08),           // Indigo accent
                Color.rgb(236, 72, 153, 0.08),           // Pink accent
                true,                                     // Animated
                true,                                     // Use accent orbs
                false,                                    // No grid pattern
                true                                      // Use extra accent
        );

        // Clean minimal (very subtle)
        public static final DashboardGradientConfig CLEAN_MINIMAL = new DashboardGradientConfig(
                Color.rgb(17, 24, 39),
                Color.rgb(20, 27, 42),
                Color.rgb(15, 22, 37),
                Color.rgb(139, 92, 246, 0.08),
                Color.rgb(59, 130, 246, 0.06),
                Color.rgb(99, 102, 241, 0.05),
                Color.rgb(236, 72, 153, 0.05),
                true,
                true,
                false,
                false
        );

        // Professional with grid
        public static final DashboardGradientConfig PROFESSIONAL_GRID = new DashboardGradientConfig(
                Color.rgb(15, 20, 28),
                Color.rgb(18, 24, 35),
                Color.rgb(12, 17, 26),
                Color.rgb(139, 92, 246, 0.10),
                Color.rgb(59, 130, 246, 0.08),
                Color.rgb(99, 102, 241, 0.07),
                Color.rgb(236, 72, 153, 0.07),
                true,
                true,
                true,                                     // With grid pattern
                true
        );

        // Static (no animation for performance)
        public static final DashboardGradientConfig STATIC_PROFESSIONAL = new DashboardGradientConfig(
                Color.rgb(15, 20, 28),
                Color.rgb(18, 24, 35),
                Color.rgb(12, 17, 26),
                Color.rgb(139, 92, 246, 0.12),
                Color.rgb(59, 130, 246, 0.10),
                Color.rgb(99, 102, 241, 0.08),
                Color.rgb(236, 72, 153, 0.08),
                false,                                    // Not animated
                true,
                false,
                true
        );

        // Warm theme
        public static final DashboardGradientConfig WARM_PROFESSIONAL = new DashboardGradientConfig(
                Color.rgb(20, 18, 25),
                Color.rgb(25, 22, 30),
                Color.rgb(18, 16, 23),
                Color.rgb(251, 146, 60, 0.10),           // Orange
                Color.rgb(239, 68, 68, 0.08),            // Red
                Color.rgb(234, 179, 8, 0.07),            // Yellow
                Color.rgb(249, 115, 22, 0.08),           // Deep orange
                true,
                true,
                false,
                true
        );

        // Cool theme
        public static final DashboardGradientConfig COOL_PROFESSIONAL = new DashboardGradientConfig(
                Color.rgb(12, 20, 28),
                Color.rgb(15, 24, 35),
                Color.rgb(10, 18, 26),
                Color.rgb(6, 182, 212, 0.12),            // Cyan
                Color.rgb(59, 130, 246, 0.10),           // Blue
                Color.rgb(14, 165, 233, 0.08),           // Sky
                Color.rgb(99, 102, 241, 0.08),           // Indigo
                true,
                true,
                false,
                true
        );
    }
}