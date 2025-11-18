package com.magictech.core.ui.components;

import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

/**
 * Centralized background management for the application
 * Makes it easy to apply consistent backgrounds across all scenes
 */
public class BackgroundManager {

    private static BackgroundManager instance;
    private GradientBackgroundPane.GradientConfig currentTheme;

    private BackgroundManager() {
        // Default theme
        this.currentTheme = GradientBackgroundPane.GradientConfig.PURPLE_BLUE;
    }

    public static BackgroundManager getInstance() {
        if (instance == null) {
            instance = new BackgroundManager();
        }
        return instance;
    }

    /**
     * Wraps any pane with an animated gradient background
     */
    public StackPane wrapWithGradient(Pane content) {
        return wrapWithGradient(content, currentTheme);
    }

    /**
     * Wraps any pane with a specific gradient theme
     */
    public StackPane wrapWithGradient(Pane content, GradientBackgroundPane.GradientConfig theme) {
        StackPane wrapper = new StackPane();

        // Add gradient background
        GradientBackgroundPane background = new GradientBackgroundPane(theme);

        // Add content on top
        wrapper.getChildren().addAll(background, content);

        return wrapper;
    }

    /**
     * Creates a new gradient background with current theme
     */
    public GradientBackgroundPane createBackground() {
        return new GradientBackgroundPane(currentTheme);
    }

    /**
     * Creates a new gradient background with specific theme
     */
    public GradientBackgroundPane createBackground(GradientBackgroundPane.GradientConfig theme) {
        return new GradientBackgroundPane(theme);
    }

    /**
     * Change the global theme
     */
    public void setTheme(GradientBackgroundPane.GradientConfig theme) {
        this.currentTheme = theme;
    }

    public GradientBackgroundPane.GradientConfig getCurrentTheme() {
        return currentTheme;
    }

    /**
     * Available theme presets
     */
    public enum Theme {
        PURPLE_BLUE(GradientBackgroundPane.GradientConfig.PURPLE_BLUE, "Purple & Blue"),
        SOLAR_WARM(GradientBackgroundPane.GradientConfig.SOLAR_WARM, "Solar Warm"),
        ECO_GREEN(GradientBackgroundPane.GradientConfig.ECO_GREEN, "Eco Green"),
        VIBRANT_PINK(GradientBackgroundPane.GradientConfig.VIBRANT_PINK, "Vibrant Pink"),
        TECH_CYAN(GradientBackgroundPane.GradientConfig.TECH_CYAN, "Tech Cyan"),
        STATIC_PURPLE(GradientBackgroundPane.GradientConfig.STATIC_PURPLE, "Static Purple");

        private final GradientBackgroundPane.GradientConfig config;
        private final String displayName;

        Theme(GradientBackgroundPane.GradientConfig config, String displayName) {
            this.config = config;
            this.displayName = displayName;
        }

        public GradientBackgroundPane.GradientConfig getConfig() {
            return config;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}