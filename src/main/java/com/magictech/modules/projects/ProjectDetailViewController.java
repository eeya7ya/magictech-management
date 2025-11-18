package com.magictech.modules.projects;

import com.magictech.core.ui.SceneManager;
import com.magictech.modules.projects.model.ProjectViewModel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/**
 * Project Detail View - Full Professional Analytics Page
 * Shows project schedule, analytics, charts with black-purple-green theme
 */
public class ProjectDetailViewController {

    private ProjectViewModel project;
    private Stage detailStage;
    private com.magictech.core.ui.components.DashboardBackgroundPane backgroundPane;

    public ProjectDetailViewController(ProjectViewModel project) {
        this.project = project;
    }

    public void show() {
        detailStage = new Stage();
        detailStage.setTitle("Project Details - " + project.getProjectName());

        StackPane root = new StackPane();
        backgroundPane = new com.magictech.core.ui.components.DashboardBackgroundPane();

        BorderPane contentPane = createContent();
        contentPane.setStyle("-fx-background-color: transparent;");

        root.getChildren().addAll(backgroundPane, contentPane);

        Scene scene = new Scene(root, 1400, 900);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

        detailStage.setScene(scene);
        detailStage.setMaximized(true);
        detailStage.show();

        // Cleanup on close
        detailStage.setOnCloseRequest(e -> {
            if (backgroundPane != null) {
                backgroundPane.stopAnimation();
            }
        });
    }

    private BorderPane createContent() {
        BorderPane main = new BorderPane();

        // Header
        VBox header = createHeader();
        main.setTop(header);

        // Main content with tabs
        TabPane tabPane = createTabPane();
        main.setCenter(tabPane);

        return main;
    }

    private VBox createHeader() {
        VBox headerContainer = new VBox();

        // Top bar
        HBox headerBar = new HBox();
        headerBar.setAlignment(Pos.CENTER_LEFT);
        headerBar.setSpacing(20);
        headerBar.setPadding(new Insets(20, 30, 20, 30));
        headerBar.setStyle(
                "-fx-background-color: linear-gradient(to right, #8b5cf6, #7c3aed);" +
                        "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 15, 0, 0, 3);"
        );

        Button backButton = new Button("← Close");
        backButton.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.2);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 20;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        );
        backButton.setOnAction(e -> detailStage.close());

        HBox titleBox = new HBox(15);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        Label iconLabel = new Label("📊");
        iconLabel.setFont(new Font(32));

        VBox titleInfo = new VBox(5);
        Label titleLabel = new Label(project.getProjectName());
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

        Label locationLabel = new Label("📍 " + project.getProjectLocation());
        locationLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.8); -fx-font-size: 14px;");

        titleInfo.getChildren().addAll(titleLabel, locationLabel);
        titleBox.getChildren().addAll(iconLabel, titleInfo);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        // Status badge
        Label statusBadge = new Label(project.getStatus());
        String statusColor = switch (project.getStatus().toLowerCase()) {
            case "completed" -> "#22c55e";
            case "in progress" -> "#3b82f6";
            case "on hold" -> "#f59e0b";
            default -> "#a78bfa";
        };
        statusBadge.setStyle(
                "-fx-background-color: " + statusColor + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 8 20;" +
                        "-fx-background-radius: 20;"
        );

        headerBar.getChildren().addAll(backButton, titleBox, statusBadge);

        // Info bar
        HBox infoBar = new HBox(40);
        infoBar.setAlignment(Pos.CENTER);
        infoBar.setPadding(new Insets(15, 30, 15, 30));
        infoBar.setStyle(
                "-fx-background-color: rgba(20, 30, 45, 0.4);" +
                        "-fx-border-color: rgba(255, 255, 255, 0.1);" +
                        "-fx-border-width: 0 0 1 0;"
        );

        infoBar.getChildren().addAll(
                createInfoCard("🆔 Project ID", String.valueOf(project.getId())),
                createInfoCard("📅 Issue Date", project.getDateOfIssue()),
                createInfoCard("📅 Completion Date", project.getDateOfCompletion()),
                createInfoCard("🎯 Status", project.getStatus())
        );

        headerContainer.getChildren().addAll(headerBar, infoBar);
        return headerContainer;
    }

    private VBox createInfoCard(String label, String value) {
        VBox card = new VBox(5);
        card.setAlignment(Pos.CENTER);

        Label labelText = new Label(label);
        labelText.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.6); -fx-font-size: 12px;");

        Label valueText = new Label(value != null && !value.isEmpty() ? value : "N/A");
        valueText.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        card.getChildren().addAll(labelText, valueText);
        return card;
    }

    private TabPane createTabPane() {
        TabPane tabPane = new TabPane();
        tabPane.setStyle("-fx-background-color: transparent;");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab overviewTab = new Tab("📊 Overview");
        overviewTab.setContent(createOverviewTab());

        Tab scheduleTab = new Tab("📅 Schedule");
        scheduleTab.setContent(createScheduleTab());

        Tab analyticsTab = new Tab("📈 Analytics");
        analyticsTab.setContent(createAnalyticsTab());

        Tab tasksTab = new Tab("✅ Tasks");
        tasksTab.setContent(createTasksTab());

        tabPane.getTabs().addAll(overviewTab, scheduleTab, analyticsTab, tasksTab);

        return tabPane;
    }

    private ScrollPane createOverviewTab() {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: transparent;");

        // Project Summary Cards
        HBox summaryCards = new HBox(20);
        summaryCards.getChildren().addAll(
                createSummaryCard("⏱️ Duration", "180 Days", "#8b5cf6"),
                createSummaryCard("💰 Budget", "$2.5M", "#22c55e"),
                createSummaryCard("👥 Team Size", "25 Members", "#3b82f6"),
                createSummaryCard("📊 Progress", "67%", "#f59e0b")
        );

        // Progress Chart
        VBox progressSection = createProgressChart();

        content.getChildren().addAll(summaryCards, progressSection);
        scroll.setContent(content);
        return scroll;
    }

    private VBox createSummaryCard(String title, String value, String color) {
        VBox card = new VBox(15);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(300);
        card.setPrefHeight(120);
        card.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.6);" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: " + color + ";" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 12;" +
                        "-fx-padding: 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 15, 0, 0, 5);"
        );

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 14px;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 28px; -fx-font-weight: bold;");

        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    private VBox createProgressChart() {
        VBox section = new VBox(15);
        section.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.6);" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: rgba(139, 92, 246, 0.3);" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 12;" +
                        "-fx-padding: 25;"
        );

        Label title = new Label("📊 Project Progress Timeline");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        BarChart<String, Number> barChart = createBarChart();

        section.getChildren().addAll(title, barChart);
        return section;
    }

    private BarChart<String, Number> createBarChart() {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Phase");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Completion %");

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Phase Completion Status");
        chart.setLegendVisible(false);
        chart.setPrefHeight(300);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("Planning", 100));
        series.getData().add(new XYChart.Data<>("Design", 85));
        series.getData().add(new XYChart.Data<>("Development", 60));
        series.getData().add(new XYChart.Data<>("Testing", 30));
        series.getData().add(new XYChart.Data<>("Deployment", 10));

        chart.getData().add(series);

        return chart;
    }

    private ScrollPane createScheduleTab() {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: transparent;");

        // Schedule Table
        TableView<ScheduleItem> scheduleTable = createScheduleTable();

        // Gantt Chart
        VBox ganttSection = createGanttChart();

        content.getChildren().addAll(scheduleTable, ganttSection);
        scroll.setContent(content);
        return scroll;
    }

    private TableView<ScheduleItem> createScheduleTable() {
        TableView<ScheduleItem> table = new TableView<>();
        table.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.6);" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: rgba(34, 197, 94, 0.5);" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 12;"
        );
        table.setPrefHeight(400);

        TableColumn<ScheduleItem, String> taskCol = new TableColumn<>("Task");
        taskCol.setPrefWidth(300);
        taskCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().task));
        taskCol.setStyle("-fx-alignment: CENTER-LEFT; -fx-padding: 0 10;");

        TableColumn<ScheduleItem, String> startCol = new TableColumn<>("Start Date");
        startCol.setPrefWidth(150);
        startCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().startDate));
        startCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<ScheduleItem, String> endCol = new TableColumn<>("End Date");
        endCol.setPrefWidth(150);
        endCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().endDate));
        endCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<ScheduleItem, String> statusCol = new TableColumn<>("Status");
        statusCol.setPrefWidth(120);
        statusCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().status));
        statusCol.setCellFactory(col -> new TableCell<ScheduleItem, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    String color = switch (item) {
                        case "Completed" -> "#22c55e";
                        case "In Progress" -> "#3b82f6";
                        case "Pending" -> "#f59e0b";
                        default -> "#ef4444";
                    };
                    setStyle("-fx-alignment: CENTER; -fx-text-fill: " + color + "; -fx-font-weight: bold;");
                }
            }
        });

        TableColumn<ScheduleItem, String> progressCol = new TableColumn<>("Progress");
        progressCol.setPrefWidth(150);
        progressCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().progress + "%"));
        progressCol.setStyle("-fx-alignment: CENTER;");

        table.getColumns().addAll(taskCol, startCol, endCol, statusCol, progressCol);

        // Sample data
        table.getItems().addAll(
                new ScheduleItem("Site Survey", "2024-01-15", "2024-01-20", "Completed", 100),
                new ScheduleItem("Design Phase", "2024-01-21", "2024-02-15", "Completed", 100),
                new ScheduleItem("Foundation Work", "2024-02-16", "2024-03-30", "In Progress", 75),
                new ScheduleItem("Structural Framework", "2024-04-01", "2024-06-15", "In Progress", 40),
                new ScheduleItem("MEP Installation", "2024-06-16", "2024-08-30", "Pending", 0),
                new ScheduleItem("Interior Finishing", "2024-09-01", "2024-11-15", "Pending", 0),
                new ScheduleItem("Final Inspection", "2024-11-16", "2024-11-30", "Pending", 0)
        );

        return table;
    }

    private VBox createGanttChart() {
        VBox section = new VBox(15);
        section.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.6);" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: rgba(34, 197, 94, 0.5);" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 12;" +
                        "-fx-padding: 25;"
        );

        Label title = new Label("📅 Gantt Chart Timeline");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label placeholder = new Label("Interactive Gantt Chart Coming Soon!");
        placeholder.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.6); -fx-font-size: 16px; -fx-padding: 50;");
        placeholder.setAlignment(Pos.CENTER);

        section.getChildren().addAll(title, placeholder);
        return section;
    }

    private ScrollPane createAnalyticsTab() {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: transparent;");

        HBox charts = new HBox(20);
        charts.getChildren().addAll(
                createPieChartSection(),
                createLineChartSection()
        );

        content.getChildren().add(charts);
        scroll.setContent(content);
        return scroll;
    }

    private VBox createPieChartSection() {
        VBox section = new VBox(15);
        section.setPrefWidth(600);
        section.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.6);" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: rgba(139, 92, 246, 0.3);" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 12;" +
                        "-fx-padding: 25;"
        );

        Label title = new Label("💰 Budget Distribution");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        PieChart pieChart = new PieChart();
        pieChart.getData().addAll(
                new PieChart.Data("Labor", 40),
                new PieChart.Data("Materials", 35),
                new PieChart.Data("Equipment", 15),
                new PieChart.Data("Overhead", 10)
        );
        pieChart.setPrefHeight(350);

        section.getChildren().addAll(title, pieChart);
        return section;
    }

    private VBox createLineChartSection() {
        VBox section = new VBox(15);
        section.setPrefWidth(600);
        section.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.6);" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: rgba(34, 197, 94, 0.5);" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 12;" +
                        "-fx-padding: 25;"
        );

        Label title = new Label("📈 Progress Trend");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        NumberAxis xAxis = new NumberAxis(1, 12, 1);
        xAxis.setLabel("Month");

        NumberAxis yAxis = new NumberAxis(0, 100, 10);
        yAxis.setLabel("Completion %");

        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setLegendVisible(false);
        lineChart.setPrefHeight(350);

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.getData().addAll(
                new XYChart.Data<>(1, 10),
                new XYChart.Data<>(2, 20),
                new XYChart.Data<>(3, 35),
                new XYChart.Data<>(4, 45),
                new XYChart.Data<>(5, 55),
                new XYChart.Data<>(6, 67)
        );

        lineChart.getData().add(series);

        section.getChildren().addAll(title, lineChart);
        return section;
    }

    private ScrollPane createTasksTab() {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: transparent;");

        Label title = new Label("✅ Task Management");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");

        Label placeholder = new Label("Task management interface coming soon!");
        placeholder.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.6); -fx-font-size: 16px;");

        content.getChildren().addAll(title, placeholder);
        scroll.setContent(content);
        return scroll;
    }

    // Helper class for schedule data
    public static class ScheduleItem {
        String task;
        String startDate;
        String endDate;
        String status;
        int progress;

        public ScheduleItem(String task, String startDate, String endDate, String status, int progress) {
            this.task = task;
            this.startDate = startDate;
            this.endDate = endDate;
            this.status = status;
            this.progress = progress;
        }
    }
}