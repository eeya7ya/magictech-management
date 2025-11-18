package com.magictech.core.ui.components;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

/**
 * Modular animated gradient background component
 * Creates smooth, flowing color gradients similar to modern web designs
 */
public class GradientBackgroundPane extends Pane {

    private final Canvas canvas;
    private final AnimationTimer animationTimer;
    private double time = 0;

    // Gradient configuration
    private final GradientConfig config;

    public GradientBackgroundPane() {
        this(GradientConfig.PURPLE_BLUE); // Default theme
    }

    public GradientBackgroundPane(GradientConfig config) {
        this.config = config;
        this.canvas = new Canvas();

        // Make canvas resize with pane
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());

        // Redraw when size changes
        canvas.widthProperty().addListener(obs -> draw());
        canvas.heightProperty().addListener(obs -> draw());

        getChildren().add(canvas);

        // Start animation if enabled
        if (config.animated) {
            animationTimer = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    time += 0.005;
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

        // Clear canvas
        gc.clearRect(0, 0, width, height);

        // Draw base gradient
        gc.setFill(config.baseColor);
        gc.fillRect(0, 0, width, height);

        // Draw animated gradient orbs
        drawGradientOrbs(gc, width, height);
    }

    private void drawGradientOrbs(GraphicsContext gc, double width, double height) {
        // Multiple gradient orbs for color mixing effect

        // Orb 1 - Top Left
        double x1 = width * 0.2 + Math.sin(time * 0.5) * width * 0.1;
        double y1 = height * 0.2 + Math.cos(time * 0.3) * height * 0.1;
        drawOrb(gc, x1, y1, width * 0.6, config.color1);

        // Orb 2 - Top Right
        double x2 = width * 0.8 + Math.cos(time * 0.4) * width * 0.1;
        double y2 = height * 0.3 + Math.sin(time * 0.6) * height * 0.1;
        drawOrb(gc, x2, y2, width * 0.5, config.color2);

        // Orb 3 - Bottom Center
        double x3 = width * 0.5 + Math.sin(time * 0.7) * width * 0.15;
        double y3 = height * 0.7 + Math.cos(time * 0.4) * height * 0.1;
        drawOrb(gc, x3, y3, width * 0.55, config.color3);

        // Optional 4th orb for more complexity
        if (config.useAccentOrb) {
            double x4 = width * 0.3 + Math.cos(time * 0.6) * width * 0.1;
            double y4 = height * 0.6 + Math.sin(time * 0.5) * height * 0.1;
            drawOrb(gc, x4, y4, width * 0.4, config.accentColor);
        }
    }

    private void drawOrb(GraphicsContext gc, double x, double y, double radius, Color color) {
        RadialGradient gradient = new RadialGradient(
                0, 0, x, y, radius,
                false, CycleMethod.NO_CYCLE,
                new Stop(0, color),
                new Stop(1, Color.TRANSPARENT)
        );

        gc.setFill(gradient);
        gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);

        // Add blur effect by drawing multiple layers with decreasing opacity
        for (int i = 1; i <= 3; i++) {
            double scale = 1 + (i * 0.1);
            Color fadedColor = Color.color(
                    color.getRed(),
                    color.getGreen(),
                    color.getBlue(),
                    color.getOpacity() * (0.3 / i)
            );

            RadialGradient blurGradient = new RadialGradient(
                    0, 0, x, y, radius * scale,
                    false, CycleMethod.NO_CYCLE,
                    new Stop(0, fadedColor),
                    new Stop(1, Color.TRANSPARENT)
            );

            gc.setFill(blurGradient);
            gc.fillOval(x - radius * scale, y - radius * scale,
                    radius * 2 * scale, radius * 2 * scale);
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
     * Gradient configuration presets
     */
    public static class GradientConfig {
        final Color baseColor;
        final Color color1;
        final Color color2;
        final Color color3;
        final Color accentColor;
        final boolean animated;
        final boolean useAccentOrb;

        public GradientConfig(Color baseColor, Color color1, Color color2,
                              Color color3, Color accentColor,
                              boolean animated, boolean useAccentOrb) {
            this.baseColor = baseColor;
            this.color1 = color1;
            this.color2 = color2;
            this.color3 = color3;
            this.accentColor = accentColor;
            this.animated = animated;
            this.useAccentOrb = useAccentOrb;
        }

        // Preset: Purple & Blue (Your current theme)
        public static final GradientConfig PURPLE_BLUE = new GradientConfig(
                Color.rgb(15, 23, 42),                    // Dark base
                Color.rgb(139, 92, 246, 0.4),            // Purple
                Color.rgb(59, 130, 246, 0.35),           // Blue
                Color.rgb(99, 102, 241, 0.3),            // Indigo
                Color.rgb(236, 72, 153, 0.25),           // Pink accent
                true,                                     // Animated
                true                                      // Use accent orb
        );

        // Preset: Orange & Red (Solar theme inspired)
        public static final GradientConfig SOLAR_WARM = new GradientConfig(
                Color.rgb(20, 20, 30),                   // Dark base
                Color.rgb(251, 146, 60, 0.45),          // Orange
                Color.rgb(239, 68, 68, 0.35),           // Red
                Color.rgb(234, 179, 8, 0.3),            // Yellow
                Color.rgb(249, 115, 22, 0.3),           // Deep orange
                true,
                true
        );

        // Preset: Green & Teal (Nature/Eco theme)
        public static final GradientConfig ECO_GREEN = new GradientConfig(
                Color.rgb(17, 24, 39),                   // Dark base
                Color.rgb(34, 197, 94, 0.4),            // Green
                Color.rgb(20, 184, 166, 0.35),          // Teal
                Color.rgb(59, 130, 246, 0.3),           // Blue
                Color.rgb(16, 185, 129, 0.3),           // Emerald
                true,
                true
        );

        // Preset: Pink & Purple (Vibrant theme)
        public static final GradientConfig VIBRANT_PINK = new GradientConfig(
                Color.rgb(24, 24, 27),                   // Dark base
                Color.rgb(236, 72, 153, 0.45),          // Pink
                Color.rgb(168, 85, 247, 0.4),           // Purple
                Color.rgb(244, 114, 182, 0.35),         // Hot pink
                Color.rgb(192, 132, 252, 0.3),          // Light purple
                true,
                true
        );

        // Preset: Cyan & Blue (Tech/Modern theme)
        public static final GradientConfig TECH_CYAN = new GradientConfig(
                Color.rgb(12, 17, 29),                   // Dark base
                Color.rgb(6, 182, 212, 0.45),           // Cyan
                Color.rgb(59, 130, 246, 0.4),           // Blue
                Color.rgb(14, 165, 233, 0.35),          // Sky blue
                Color.rgb(99, 102, 241, 0.3),           // Indigo
                true,
                true
        );

        // Preset: Static (no animation)
        public static final GradientConfig STATIC_PURPLE = new GradientConfig(
                Color.rgb(15, 23, 42),
                Color.rgb(139, 92, 246, 0.35),
                Color.rgb(59, 130, 246, 0.3),
                Color.rgb(99, 102, 241, 0.25),
                Color.rgb(236, 72, 153, 0.2),
                false,                                    // Not animated
                true
        );
    }
}