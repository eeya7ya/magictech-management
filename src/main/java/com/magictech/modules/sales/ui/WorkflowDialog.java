package com.magictech.modules.sales.ui;

import com.magictech.core.auth.User;
import com.magictech.modules.projects.entity.Project;
import com.magictech.modules.sales.entity.MissingItemRequest;
import com.magictech.modules.sales.entity.ProjectWorkflow;
import com.magictech.modules.sales.entity.WorkflowStepCompletion;
import com.magictech.modules.sales.service.ProjectWorkflowService;
import com.magictech.modules.sales.service.WorkflowStepService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Main workflow dialog for 8-step project lifecycle
 * Shown as popup when creating "Sell as New Project"
 */
public class WorkflowDialog extends Stage {

    private final ProjectWorkflowService workflowService;
    private final WorkflowStepService stepService;
    private final Project project;
    private final User currentUser;
    private ProjectWorkflow workflow;

    private VBox mainContainer;
    private HBox progressBar;
    private VBox stepContainer;
    private Button nextButton;
    private Button backButton;
    private Button closeButton;

    private int currentStep = 1;
    private final String[] stepTitles = {
        "Site Survey Check",
        "Selection & Design Check",
        "Bank Guarantee Check",
        "Missing Item Check",
        "Tender Acceptance Check",
        "Project Team Finished Check",
        "After Sales Check",
        "Completion"
    };

    public WorkflowDialog(Project project, User currentUser,
                          ProjectWorkflowService workflowService,
                          WorkflowStepService stepService) {
        this.project = project;
        this.currentUser = currentUser;
        this.workflowService = workflowService;
        this.stepService = stepService;

        initStyle(StageStyle.UTILITY);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Project Workflow - " + project.getProjectName());

        createWorkflow();
        buildUI();
        loadCurrentStep();
    }

    private void createWorkflow() {
        // Create workflow if doesn't exist
        Optional<ProjectWorkflow> existing = workflowService.getWorkflowByProjectId(project.getId());
        if (existing.isPresent()) {
            workflow = existing.get();
            currentStep = workflow.getCurrentStep();
        } else {
            workflow = workflowService.createWorkflow(project.getId(), currentUser);
            currentStep = 1;
        }
    }

    private void buildUI() {
        mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(20));
        mainContainer.setStyle("-fx-background-color: #f5f5f5;");
        mainContainer.setPrefWidth(800);
        mainContainer.setPrefHeight(600);

        // Header
        Label headerLabel = new Label("Project Workflow");
        headerLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        headerLabel.setTextFill(Color.web("#2c3e50"));

        Label projectLabel = new Label("Project: " + project.getProjectName());
        projectLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        projectLabel.setTextFill(Color.web("#7f8c8d"));

        VBox header = new VBox(5, headerLabel, projectLabel);
        header.setAlignment(Pos.CENTER);

        // Progress bar
        progressBar = createProgressBar();

        // Step container
        stepContainer = new VBox(15);
        stepContainer.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 20;");

        // Buttons
        HBox buttonBox = createButtonBox();

        mainContainer.getChildren().addAll(header, progressBar, stepContainer, buttonBox);

        Scene scene = new Scene(mainContainer);
        setScene(scene);
    }

    private HBox createProgressBar() {
        HBox progressContainer = new HBox(5);
        progressContainer.setAlignment(Pos.CENTER);
        progressContainer.setPadding(new Insets(10));

        for (int i = 1; i <= 8; i++) {
            VBox stepBox = createProgressStep(i);
            progressContainer.getChildren().add(stepBox);

            if (i < 8) {
                // Add connector line
                Region connector = new Region();
                connector.setPrefWidth(30);
                connector.setPrefHeight(2);
                connector.setStyle("-fx-background-color: #bdc3c7;");
                progressContainer.getChildren().add(connector);
            }
        }

        return progressContainer;
    }

    private VBox createProgressStep(int stepNum) {
        VBox stepBox = new VBox(5);
        stepBox.setAlignment(Pos.CENTER);

        // Circle
        StackPane circle = new StackPane();
        circle.setPrefSize(40, 40);
        circle.setStyle(getStepStyle(stepNum));

        Label numLabel = new Label(String.valueOf(stepNum));
        numLabel.setTextFill(Color.WHITE);
        numLabel.setFont(Font.font("System", FontWeight.BOLD, 16));

        circle.getChildren().add(numLabel);

        // Step name
        Label nameLabel = new Label("Step " + stepNum);
        nameLabel.setFont(Font.font("System", FontWeight.NORMAL, 10));
        nameLabel.setTextFill(Color.web("#7f8c8d"));

        stepBox.getChildren().addAll(circle, nameLabel);
        return stepBox;
    }

    private String getStepStyle(int stepNum) {
        Optional<WorkflowStepCompletion> stepOpt = stepService.getStep(workflow.getId(), stepNum);

        if (stepOpt.isPresent() && Boolean.TRUE.equals(stepOpt.get().getCompleted())) {
            return "-fx-background-color: #27ae60; -fx-background-radius: 20;"; // Green - completed
        } else if (stepNum == currentStep) {
            return "-fx-background-color: #3498db; -fx-background-radius: 20;"; // Blue - current
        } else if (stepNum < currentStep) {
            return "-fx-background-color: #f39c12; -fx-background-radius: 20;"; // Orange - in progress
        } else {
            return "-fx-background-color: #bdc3c7; -fx-background-radius: 20;"; // Gray - pending
        }
    }

    private HBox createButtonBox() {
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(10));

        backButton = new Button("← Back");
        backButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        backButton.setOnAction(e -> handleBack());

        nextButton = new Button("Next →");
        nextButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        nextButton.setOnAction(e -> handleNext());

        closeButton = new Button("Close");
        closeButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        closeButton.setOnAction(e -> close());

        buttonBox.getChildren().addAll(backButton, nextButton, closeButton);
        return buttonBox;
    }

    private void loadCurrentStep() {
        stepContainer.getChildren().clear();

        // Step title
        Label stepTitle = new Label("Step " + currentStep + ": " + stepTitles[currentStep - 1]);
        stepTitle.setFont(Font.font("System", FontWeight.BOLD, 20));
        stepTitle.setTextFill(Color.web("#2c3e50"));

        stepContainer.getChildren().add(stepTitle);

        // Load step-specific content
        switch (currentStep) {
            case 1 -> loadStep1_SiteSurvey();
            case 2 -> loadStep2_SelectionDesign();
            case 3 -> loadStep3_BankGuarantee();
            case 4 -> loadStep4_MissingItem();
            case 5 -> loadStep5_TenderAcceptance();
            case 6 -> loadStep6_ProjectFinished();
            case 7 -> loadStep7_AfterSales();
            case 8 -> loadStep8_Completion();
        }

        // Update buttons
        backButton.setDisable(currentStep == 1);

        // Update progress bar
        progressBar.getChildren().clear();
        progressBar = createProgressBar();
        mainContainer.getChildren().set(1, progressBar);
    }

    // STEP 1: Site Survey
    private void loadStep1_SiteSurvey() {
        Label question = new Label("Does Project need site survey?");
        question.setFont(Font.font("System", FontWeight.NORMAL, 16));

        Button yesButton = new Button("Yes - I'll do it myself");
        yesButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        yesButton.setOnAction(e -> handleSiteSurveySales());

        Button yesProjectButton = new Button("Yes - Request from Project Team");
        yesProjectButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        yesProjectButton.setOnAction(e -> handleSiteSurveyProject());

        HBox buttons = new HBox(10, yesButton, yesProjectButton);
        buttons.setAlignment(Pos.CENTER);

        stepContainer.getChildren().addAll(question, buttons);
    }

    private void handleSiteSurveySales() {
        // Show file chooser for Excel upload
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Upload Site Survey Excel");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls")
        );

        File file = fileChooser.showOpenDialog(this);
        if (file != null) {
            try {
                byte[] fileData = Files.readAllBytes(file.toPath());
                workflowService.processSiteSurveySales(workflow.getId(), fileData,
                    file.getName(), currentUser);

                showSuccess("Site survey uploaded successfully!");
                refreshWorkflow();
                loadCurrentStep();
            } catch (Exception ex) {
                showError("Failed to upload site survey: " + ex.getMessage());
            }
        }
    }

    private void handleSiteSurveyProject() {
        workflowService.requestSiteSurveyFromProject(workflow.getId(), currentUser);
        showInfo("Site survey request sent to Project Team. Waiting for their response...");
    }

    // STEP 2: Selection & Design
    private void loadStep2_SelectionDesign() {
        Label question = new Label("Does it need a selection and design?");
        question.setFont(Font.font("System", FontWeight.NORMAL, 16));

        Button noButton = new Button("No");
        noButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        noButton.setOnAction(e -> {
            workflowService.markSelectionDesignNotNeeded(workflow.getId(), currentUser);
            showSuccess("Marked as not needed");
            refreshWorkflow();
            loadCurrentStep();
        });

        Button yesButton = new Button("Yes - Request from Presales");
        yesButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        yesButton.setOnAction(e -> {
            workflowService.requestSelectionDesignFromPresales(workflow.getId(), currentUser);
            showInfo("Selection & design request sent to Presales Team");
        });

        HBox buttons = new HBox(10, noButton, yesButton);
        buttons.setAlignment(Pos.CENTER);

        stepContainer.getChildren().addAll(question, buttons);
    }

    // STEP 3: Bank Guarantee
    private void loadStep3_BankGuarantee() {
        Label question = new Label("Does project needs Bank Guarantee?");
        question.setFont(Font.font("System", FontWeight.NORMAL, 16));

        Button noButton = new Button("No");
        noButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        noButton.setOnAction(e -> {
            workflowService.markBankGuaranteeNotNeeded(workflow.getId(), currentUser);
            showSuccess("Marked as not needed");
            refreshWorkflow();
            loadCurrentStep();
        });

        Button yesButton = new Button("Yes - Request from Finance");
        yesButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        yesButton.setOnAction(e -> {
            workflowService.requestBankGuarantee(workflow.getId(), currentUser);
            showInfo("Bank guarantee request sent to Finance Team");
        });

        HBox buttons = new HBox(10, noButton, yesButton);
        buttons.setAlignment(Pos.CENTER);

        stepContainer.getChildren().addAll(question, buttons);
    }

    // STEP 4: Missing Item
    private void loadStep4_MissingItem() {
        Label question = new Label("Is there any missing item?");
        question.setFont(Font.font("System", FontWeight.NORMAL, 16));

        Button noButton = new Button("No");
        noButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        noButton.setOnAction(e -> {
            workflowService.markNoMissingItems(workflow.getId(), currentUser);
            showSuccess("Marked as no missing items");
            refreshWorkflow();
            loadCurrentStep();
        });

        Button yesButton = new Button("Yes - Submit Item Details");
        yesButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        yesButton.setOnAction(e -> showMissingItemDialog());

        HBox buttons = new HBox(10, noButton, yesButton);
        buttons.setAlignment(Pos.CENTER);

        stepContainer.getChildren().addAll(question, buttons);
    }

    private void showMissingItemDialog() {
        Dialog<MissingItemRequest> dialog = new Dialog<>();
        dialog.setTitle("Submit Missing Item Request");
        dialog.setHeaderText("Enter missing item details");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField itemNameField = new TextField();
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPrefRowCount(3);
        TextField quantityField = new TextField();
        TextArea specsArea = new TextArea();
        specsArea.setPrefRowCount(3);
        ComboBox<String> urgencyBox = new ComboBox<>();
        urgencyBox.getItems().addAll("LOW", "MEDIUM", "HIGH", "CRITICAL");
        urgencyBox.setValue("MEDIUM");

        grid.add(new Label("Item Name:*"), 0, 0);
        grid.add(itemNameField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descriptionArea, 1, 1);
        grid.add(new Label("Quantity:"), 0, 2);
        grid.add(quantityField, 1, 2);
        grid.add(new Label("Specifications:"), 0, 3);
        grid.add(specsArea, 1, 3);
        grid.add(new Label("Urgency:"), 0, 4);
        grid.add(urgencyBox, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                MissingItemRequest request = new MissingItemRequest();
                request.setItemName(itemNameField.getText());
                request.setItemDescription(descriptionArea.getText());
                request.setQuantityNeeded(Integer.parseInt(quantityField.getText()));
                request.setItemSpecifications(specsArea.getText());
                request.setUrgencyLevel(urgencyBox.getValue());
                return request;
            }
            return null;
        });

        Optional<MissingItemRequest> result = dialog.showAndWait();
        result.ifPresent(request -> {
            workflowService.submitMissingItemRequest(workflow.getId(), request, currentUser);
            showInfo("Missing item request submitted. Waiting for MASTER/SALES_MANAGER approval...");
        });
    }

    // STEP 5: Tender Acceptance
    private void loadStep5_TenderAcceptance() {
        Label question = new Label("Does the Tender Accepted?");
        question.setFont(Font.font("System", FontWeight.NORMAL, 16));

        Button yesButton = new Button("Yes - Tender Accepted");
        yesButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        yesButton.setOnAction(e -> {
            workflowService.markTenderAccepted(workflow.getId(), currentUser);
            showSuccess("Tender accepted! Project team will be notified to start work.");
        });

        Button noButton = new Button("No - Tender Rejected");
        noButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        noButton.setOnAction(e -> showRejectionDialog());

        HBox buttons = new HBox(10, yesButton, noButton);
        buttons.setAlignment(Pos.CENTER);

        stepContainer.getChildren().addAll(question, buttons);
    }

    private void showRejectionDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Tender Rejection");
        dialog.setHeaderText("Tender Rejected");
        dialog.setContentText("Please explain why the tender was rejected:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(reason -> {
            workflowService.markTenderRejected(workflow.getId(), reason, currentUser);
            showWarning("Tender marked as rejected. Workflow status updated.");
            close();
        });
    }

    // STEP 6: Project Finished
    private void loadStep6_ProjectFinished() {
        Label question = new Label("Does Project team Finished?");
        question.setFont(Font.font("System", FontWeight.NORMAL, 16));

        Button yesButton = new Button("Yes - Upload Project Cost");
        yesButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        yesButton.setOnAction(e -> handleProjectCostUpload());

        Button noButton = new Button("No - Report Delay");
        noButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        noButton.setOnAction(e -> showDelayDialog());

        HBox buttons = new HBox(10, yesButton, noButton);
        buttons.setAlignment(Pos.CENTER);

        stepContainer.getChildren().addAll(question, buttons);
    }

    private void handleProjectCostUpload() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Upload Project Cost Excel");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls")
        );

        File file = fileChooser.showOpenDialog(this);
        if (file != null) {
            try {
                byte[] fileData = Files.readAllBytes(file.toPath());
                workflowService.confirmProjectFinished(workflow.getId(), fileData,
                    file.getName(), currentUser);

                showSuccess("Project cost uploaded successfully!");
                refreshWorkflow();
                loadCurrentStep();
            } catch (Exception ex) {
                showError("Failed to upload project cost: " + ex.getMessage());
            }
        }
    }

    private void showDelayDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Report Project Delay");
        dialog.setHeaderText("Project Delay - DANGER Alert");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        DatePicker expectedDatePicker = new DatePicker();
        TextArea delayDetailsArea = new TextArea();
        delayDetailsArea.setPrefRowCount(5);

        grid.add(new Label("Expected Completion Date:"), 0, 0);
        grid.add(expectedDatePicker, 1, 0);
        grid.add(new Label("Delay Details:"), 0, 1);
        grid.add(delayDetailsArea, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            LocalDateTime expectedDate = expectedDatePicker.getValue().atStartOfDay();
            workflowService.reportProjectDelay(workflow.getId(), expectedDate,
                delayDetailsArea.getText(), currentUser);
            showWarning("DANGER alert sent to MASTER about project delay");
        });
    }

    // STEP 7: After Sales
    private void loadStep7_AfterSales() {
        Label question = new Label("After Sales check?");
        question.setFont(Font.font("System", FontWeight.NORMAL, 16));

        Button noButton = new Button("No");
        noButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        noButton.setOnAction(e -> {
            workflowService.markAfterSalesNotNeeded(workflow.getId(), currentUser);
            showSuccess("Marked as not needed");
            refreshWorkflow();
            loadCurrentStep();
        });

        Button yesButton = new Button("Yes - Request QA Check");
        yesButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        yesButton.setOnAction(e -> {
            workflowService.requestAfterSalesCheck(workflow.getId(), currentUser);
            showInfo("After-sales check request sent to Quality Assurance Team");
        });

        HBox buttons = new HBox(10, noButton, yesButton);
        buttons.setAlignment(Pos.CENTER);

        stepContainer.getChildren().addAll(question, buttons);
    }

    // STEP 8: Completion
    private void loadStep8_Completion() {
        Label question = new Label("Complete the workflow?");
        question.setFont(Font.font("System", FontWeight.NORMAL, 16));

        Label info = new Label("All project data will be pushed to storage analysis.");
        info.setFont(Font.font("System", FontWeight.NORMAL, 14));
        info.setTextFill(Color.web("#7f8c8d"));

        Button completeButton = new Button("✓ Complete Workflow");
        completeButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 15 30;");
        completeButton.setOnAction(e -> {
            workflowService.completeWorkflow(workflow.getId(), currentUser);
            showSuccess("Workflow completed! All data pushed to storage analysis.");
            close();
        });

        VBox content = new VBox(15, question, info, completeButton);
        content.setAlignment(Pos.CENTER);

        stepContainer.getChildren().add(content);
    }

    private void handleNext() {
        if (canMoveToNextStep()) {
            currentStep++;
            loadCurrentStep();
        }
    }

    private void handleBack() {
        if (currentStep > 1) {
            currentStep--;
            loadCurrentStep();
        }
    }

    private boolean canMoveToNextStep() {
        Optional<WorkflowStepCompletion> stepOpt = stepService.getStep(workflow.getId(), currentStep);
        if (stepOpt.isPresent()) {
            return Boolean.TRUE.equals(stepOpt.get().getCompleted());
        }
        return false;
    }

    private void refreshWorkflow() {
        workflow = workflowService.getWorkflowById(workflow.getId()).orElse(workflow);
        currentStep = workflow.getCurrentStep();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
