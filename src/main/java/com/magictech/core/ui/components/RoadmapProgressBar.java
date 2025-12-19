package com.magictech.core.ui.components;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Modern roadmap-style progress bar for wizard/workflow UIs
 *
 * Features:
 * - Completed steps: Darker green with checkmark
 * - Current step: Glowing/pulsing animation
 * - Future steps: Light blue/grey
 * - Road/path-like connectors between steps
 *
 * Design inspired by modern project tracking UIs
 */
public class RoadmapProgressBar extends HBox {

    private final List<String> stepTitles;
    private final List<StackPane> stepNodes = new ArrayList<>();
    private final List<Region> connectors = new ArrayList<>();
    private int currentStep = 1;
    private List<Boolean> completedSteps;

    private Timeline glowAnimation;

    // Colors for the roadmap theme
    private static final String COMPLETED_COLOR = "#065f46";      // Dark green
    private static final String COMPLETED_GRADIENT_START = "#059669";
    private static final String COMPLETED_GRADIENT_END = "#047857";

    private static final String CURRENT_COLOR = "#3b82f6";         // Blue glow
    private static final String CURRENT_GRADIENT_START = "#60a5fa";
    private static final String CURRENT_GRADIENT_END = "#2563eb";

    private static final String FUTURE_COLOR = "#94a3b8";          // Light slate
    private static final String FUTURE_GRADIENT_START = "#cbd5e1";
    private static final String FUTURE_GRADIENT_END = "#94a3b8";

    private static final String CONNECTOR_COMPLETED = "#10b981";   // Green line
    private static final String CONNECTOR_PENDING = "#e2e8f0";     // Light grey line

    /**
     * Create a new RoadmapProgressBar
     * @param stepTitles List of step titles/names
     */
    public RoadmapProgressBar(List<String> stepTitles) {
        this.stepTitles = stepTitles;
        this.completedSteps = new ArrayList<>();
        for (int i = 0; i < stepTitles.size(); i++) {
            completedSteps.add(false);
        }

        setAlignment(Pos.CENTER);
        setSpacing(0);
        setPadding(new Insets(20, 40, 20, 40));
        setStyle("-fx-background-color: linear-gradient(to bottom, rgba(15, 23, 42, 0.8), rgba(30, 41, 59, 0.9));" +
                 "-fx-background-radius: 16;" +
                 "-fx-border-radius: 16;" +
                 "-fx-border-color: rgba(100, 116, 139, 0.3);" +
                 "-fx-border-width: 1;");

        buildProgressBar();
    }

    private void buildProgressBar() {
        getChildren().clear();
        stepNodes.clear();
        connectors.clear();

        for (int i = 0; i < stepTitles.size(); i++) {
            // Create step node
            StackPane stepNode = createStepNode(i + 1, stepTitles.get(i));
            stepNodes.add(stepNode);
            getChildren().add(stepNode);

            // Add connector if not last step
            if (i < stepTitles.size() - 1) {
                Region connector = createConnector(i);
                connectors.add(connector);
                getChildren().add(connector);
            }
        }

        updateStepStyles();
        startGlowAnimation();
    }

    private StackPane createStepNode(int stepNum, String title) {
        StackPane container = new StackPane();
        container.setAlignment(Pos.CENTER);

        VBox stepBox = new VBox(8);
        stepBox.setAlignment(Pos.CENTER);
        stepBox.setMinWidth(80);
        stepBox.setMaxWidth(100);

        // Circle node
        StackPane circleContainer = new StackPane();
        circleContainer.setAlignment(Pos.CENTER);
        circleContainer.setMinSize(50, 50);
        circleContainer.setMaxSize(50, 50);

        // Background circle with gradient
        Circle bgCircle = new Circle(24);
        bgCircle.setId("bg-circle-" + stepNum);

        // Inner circle for layered effect
        Circle innerCircle = new Circle(20);
        innerCircle.setId("inner-circle-" + stepNum);

        // Step number or checkmark
        Label numLabel = new Label(String.valueOf(stepNum));
        numLabel.setId("num-label-" + stepNum);
        numLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        numLabel.setTextFill(Color.WHITE);

        circleContainer.getChildren().addAll(bgCircle, innerCircle, numLabel);

        // Step title (shortened)
        String shortTitle = getShortTitle(title);
        Label titleLabel = new Label(shortTitle);
        titleLabel.setId("title-label-" + stepNum);
        titleLabel.setFont(Font.font("System", FontWeight.MEDIUM, 10));
        titleLabel.setTextFill(Color.web("#94a3b8"));
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(90);
        titleLabel.setAlignment(Pos.CENTER);

        // Tooltip for full title
        Tooltip.install(circleContainer, new Tooltip("Step " + stepNum + ": " + title));

        stepBox.getChildren().addAll(circleContainer, titleLabel);
        container.getChildren().add(stepBox);

        return container;
    }

    private String getShortTitle(String title) {
        // Shorten long titles for display
        if (title.length() > 15) {
            return title.substring(0, 12) + "...";
        }
        return title;
    }

    private Region createConnector(int beforeStepIndex) {
        Region connector = new Region();
        connector.setId("connector-" + beforeStepIndex);
        connector.setPrefHeight(4);
        connector.setMinHeight(4);
        connector.setMaxHeight(4);
        connector.setPrefWidth(40);
        connector.setMinWidth(30);
        HBox.setHgrow(connector, Priority.ALWAYS);
        connector.setStyle("-fx-background-color: " + CONNECTOR_PENDING + ";" +
                          "-fx-background-radius: 2;");

        return connector;
    }

    /**
     * Update the current step and refresh styles
     * @param step Current step number (1-indexed)
     */
    public void setCurrentStep(int step) {
        this.currentStep = step;
        updateStepStyles();
    }

    /**
     * Mark a step as completed
     * @param stepNum Step number (1-indexed)
     * @param completed Whether the step is completed
     */
    public void setStepCompleted(int stepNum, boolean completed) {
        if (stepNum > 0 && stepNum <= completedSteps.size()) {
            completedSteps.set(stepNum - 1, completed);
            updateStepStyles();
        }
    }

    /**
     * Set all completion states at once
     * @param completionStates List of boolean completion states
     */
    public void setCompletionStates(List<Boolean> completionStates) {
        for (int i = 0; i < Math.min(completionStates.size(), completedSteps.size()); i++) {
            completedSteps.set(i, completionStates.get(i));
        }
        updateStepStyles();
    }

    private void updateStepStyles() {
        for (int i = 0; i < stepNodes.size(); i++) {
            int stepNum = i + 1;
            StackPane stepNode = stepNodes.get(i);
            boolean isCompleted = completedSteps.get(i);
            boolean isCurrent = stepNum == currentStep;

            // Get circle elements
            Circle bgCircle = (Circle) stepNode.lookup("#bg-circle-" + stepNum);
            Circle innerCircle = (Circle) stepNode.lookup("#inner-circle-" + stepNum);
            Label numLabel = (Label) stepNode.lookup("#num-label-" + stepNum);
            Label titleLabel = (Label) stepNode.lookup("#title-label-" + stepNum);

            if (bgCircle == null || innerCircle == null || numLabel == null) continue;

            // Apply styles based on state
            if (isCompleted) {
                // COMPLETED: Dark green with checkmark
                bgCircle.setFill(createGradient(COMPLETED_GRADIENT_START, COMPLETED_GRADIENT_END));
                innerCircle.setFill(Color.web(COMPLETED_COLOR));
                numLabel.setText("✓");
                numLabel.setTextFill(Color.WHITE);
                titleLabel.setTextFill(Color.web("#10b981"));

                // Subtle shadow
                DropShadow shadow = new DropShadow();
                shadow.setColor(Color.web("#059669", 0.5));
                shadow.setRadius(10);
                bgCircle.setEffect(shadow);

            } else if (isCurrent) {
                // CURRENT: Glowing blue
                bgCircle.setFill(createGradient(CURRENT_GRADIENT_START, CURRENT_GRADIENT_END));
                innerCircle.setFill(Color.web(CURRENT_COLOR));
                numLabel.setText(String.valueOf(stepNum));
                numLabel.setTextFill(Color.WHITE);
                titleLabel.setTextFill(Color.WHITE);

                // Glow effect for current step
                DropShadow glow = new DropShadow();
                glow.setColor(Color.web("#3b82f6", 0.8));
                glow.setRadius(20);
                glow.setSpread(0.3);
                bgCircle.setEffect(glow);

            } else {
                // FUTURE: Light slate/blue
                bgCircle.setFill(createGradient(FUTURE_GRADIENT_START, FUTURE_GRADIENT_END));
                innerCircle.setFill(Color.web(FUTURE_COLOR));
                numLabel.setText(String.valueOf(stepNum));
                numLabel.setTextFill(Color.web("#475569"));
                titleLabel.setTextFill(Color.web("#64748b"));

                // No effect
                bgCircle.setEffect(null);
            }

            // Update connector before this step
            if (i > 0) {
                Region connector = connectors.get(i - 1);
                boolean previousCompleted = completedSteps.get(i - 1);
                if (previousCompleted) {
                    connector.setStyle("-fx-background-color: linear-gradient(to right, " +
                                      CONNECTOR_COMPLETED + ", " + CONNECTOR_COMPLETED + ");" +
                                      "-fx-background-radius: 2;");
                } else {
                    connector.setStyle("-fx-background-color: " + CONNECTOR_PENDING + ";" +
                                      "-fx-background-radius: 2;");
                }
            }
        }
    }

    private LinearGradient createGradient(String startColor, String endColor) {
        return new LinearGradient(
            0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web(startColor)),
            new Stop(1, Color.web(endColor))
        );
    }

    private void startGlowAnimation() {
        // Stop existing animation
        if (glowAnimation != null) {
            glowAnimation.stop();
        }

        // Create pulsing glow animation for current step
        glowAnimation = new Timeline(
            new KeyFrame(Duration.ZERO, e -> {
                if (currentStep > 0 && currentStep <= stepNodes.size()) {
                    StackPane stepNode = stepNodes.get(currentStep - 1);
                    Circle bgCircle = (Circle) stepNode.lookup("#bg-circle-" + currentStep);
                    if (bgCircle != null && !completedSteps.get(currentStep - 1)) {
                        DropShadow glow = new DropShadow();
                        glow.setColor(Color.web("#3b82f6", 0.6));
                        glow.setRadius(15);
                        glow.setSpread(0.2);
                        bgCircle.setEffect(glow);
                    }
                }
            }),
            new KeyFrame(Duration.millis(750), e -> {
                if (currentStep > 0 && currentStep <= stepNodes.size()) {
                    StackPane stepNode = stepNodes.get(currentStep - 1);
                    Circle bgCircle = (Circle) stepNode.lookup("#bg-circle-" + currentStep);
                    if (bgCircle != null && !completedSteps.get(currentStep - 1)) {
                        DropShadow glow = new DropShadow();
                        glow.setColor(Color.web("#3b82f6", 1.0));
                        glow.setRadius(25);
                        glow.setSpread(0.4);
                        bgCircle.setEffect(glow);
                    }
                }
            })
        );
        glowAnimation.setCycleCount(Timeline.INDEFINITE);
        glowAnimation.setAutoReverse(true);
        glowAnimation.play();
    }

    /**
     * Stop any animations and clean up
     */
    public void cleanup() {
        if (glowAnimation != null) {
            glowAnimation.stop();
            glowAnimation = null;
        }
    }

    /**
     * Get the number of steps
     */
    public int getStepCount() {
        return stepTitles.size();
    }

    /**
     * Check if a step is completed
     */
    public boolean isStepCompleted(int stepNum) {
        if (stepNum > 0 && stepNum <= completedSteps.size()) {
            return completedSteps.get(stepNum - 1);
        }
        return false;
    }
}
