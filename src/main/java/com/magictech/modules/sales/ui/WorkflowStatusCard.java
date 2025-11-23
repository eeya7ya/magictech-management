package com.magictech.modules.sales.ui;

import com.magictech.modules.projects.entity.Project;
import com.magictech.modules.sales.entity.ProjectWorkflow;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.format.DateTimeFormatter;

/**
 * Visual card component showing workflow status
 */
public class WorkflowStatusCard extends VBox {

    private final ProjectWorkflow workflow;
    private final Project project;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    public WorkflowStatusCard(ProjectWorkflow workflow, Project project) {
        this.workflow = workflow;
        this.project = project;

        buildUI();
    }

    private void buildUI() {
        setSpacing(10);
        setPadding(new Insets(15));
        setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-border-color: #e0e0e0; -fx-border-radius: 10; -fx-border-width: 1;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        setPrefWidth(350);

        // Header
        HBox header = createHeader();

        // Progress section
        VBox progressSection = createProgressSection();

        // Details section
        GridPane details = createDetailsSection();

        // Status badge
        HBox statusBadge = createStatusBadge();

        getChildren().addAll(header, statusBadge, progressSection, details);

        // Hover effect
        setOnMouseEntered(e -> setStyle(getStyle() + "-fx-border-color: #3498db; -fx-cursor: hand;"));
        setOnMouseExited(e -> setStyle(getStyle().replace("-fx-border-color: #3498db;", "-fx-border-color: #e0e0e0;")
                                                    .replace("-fx-cursor: hand;", "")));
    }

    private HBox createHeader() {
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("📋");
        icon.setFont(Font.font(20));

        Label projectName = new Label(project.getProjectName());
        projectName.setFont(Font.font("System", FontWeight.BOLD, 16));
        projectName.setTextFill(Color.web("#2c3e50"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label stepLabel = new Label("Step " + workflow.getCurrentStep() + "/8");
        stepLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        stepLabel.setTextFill(Color.web("#7f8c8d"));

        header.getChildren().addAll(icon, projectName, spacer, stepLabel);
        return header;
    }

    private HBox createStatusBadge() {
        HBox badge = new HBox();
        badge.setAlignment(Pos.CENTER);
        badge.setPadding(new Insets(5, 10, 5, 10));

        String statusText;
        String badgeColor;

        switch (workflow.getStatus()) {
            case COMPLETED -> {
                statusText = "✓ Completed";
                badgeColor = "#27ae60";
            }
            case REJECTED -> {
                statusText = "✗ Rejected";
                badgeColor = "#e74c3c";
            }
            case ON_HOLD -> {
                statusText = "⏸ On Hold";
                badgeColor = "#f39c12";
            }
            default -> {
                statusText = "⏳ In Progress";
                badgeColor = "#3498db";
            }
        }

        badge.setStyle("-fx-background-color: " + badgeColor + "; -fx-background-radius: 15;");

        Label statusLabel = new Label(statusText);
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 11));
        statusLabel.setTextFill(Color.WHITE);

        badge.getChildren().add(statusLabel);
        return badge;
    }

    private VBox createProgressSection() {
        VBox section = new VBox(5);

        Label progressLabel = new Label("Overall Progress");
        progressLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        progressLabel.setTextFill(Color.web("#7f8c8d"));

        double progress = workflow.getCurrentStep() / 8.0;
        ProgressBar progressBar = new ProgressBar(progress);
        progressBar.setPrefWidth(Double.MAX_VALUE);
        progressBar.setStyle("-fx-accent: #3498db;");

        Label percentLabel = new Label(String.format("%.0f%% Complete", progress * 100));
        percentLabel.setFont(Font.font("System", FontWeight.BOLD, 11));
        percentLabel.setTextFill(Color.web("#27ae60"));

        section.getChildren().addAll(progressLabel, progressBar, percentLabel);
        return section;
    }

    private GridPane createDetailsSection() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(10, 0, 0, 0));

        // Created by
        Label createdByLabel = new Label("Created by:");
        createdByLabel.setFont(Font.font("System", FontWeight.NORMAL, 11));
        createdByLabel.setTextFill(Color.web("#7f8c8d"));

        Label createdByValue = new Label(workflow.getCreatedBy());
        createdByValue.setFont(Font.font("System", FontWeight.BOLD, 11));

        // Created at
        Label createdAtLabel = new Label("Started:");
        createdAtLabel.setFont(Font.font("System", FontWeight.NORMAL, 11));
        createdAtLabel.setTextFill(Color.web("#7f8c8d"));

        Label createdAtValue = new Label(workflow.getCreatedAt().format(DATE_FORMATTER));
        createdAtValue.setFont(Font.font("System", FontWeight.NORMAL, 11));

        // Last updated
        Label updatedLabel = new Label("Last Updated:");
        updatedLabel.setFont(Font.font("System", FontWeight.NORMAL, 11));
        updatedLabel.setTextFill(Color.web("#7f8c8d"));

        Label updatedValue = new Label(
            workflow.getLastUpdatedAt() != null ?
                workflow.getLastUpdatedAt().format(DATE_FORMATTER) :
                "-"
        );
        updatedValue.setFont(Font.font("System", FontWeight.NORMAL, 11));

        grid.add(createdByLabel, 0, 0);
        grid.add(createdByValue, 1, 0);
        grid.add(createdAtLabel, 0, 1);
        grid.add(createdAtValue, 1, 1);
        grid.add(updatedLabel, 0, 2);
        grid.add(updatedValue, 1, 2);

        return grid;
    }

    public ProjectWorkflow getWorkflow() {
        return workflow;
    }

    public Project getProject() {
        return project;
    }
}
