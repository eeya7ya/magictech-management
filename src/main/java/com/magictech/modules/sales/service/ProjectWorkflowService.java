package com.magictech.modules.sales.service;

import com.magictech.core.auth.User;
import com.magictech.modules.projects.entity.Project;
import com.magictech.modules.projects.repository.ProjectRepository;
import com.magictech.modules.sales.entity.*;
import com.magictech.modules.sales.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Main workflow orchestration service
 * Coordinates the 8-step project workflow
 */
@Service
@Transactional
public class ProjectWorkflowService {

    @Autowired
    private ProjectWorkflowRepository workflowRepository;

    @Autowired
    private WorkflowStepService stepService;

    @Autowired
    private WorkflowNotificationService notificationService;

    @Autowired
    private ExcelStorageService excelStorageService;

    @Autowired
    private SiteSurveyExcelService siteSurveyExcelService;

    @Autowired
    private SiteSurveyDataRepository siteSurveyRepository;

    @Autowired
    private SizingPricingDataRepository sizingPricingRepository;

    @Autowired
    private BankGuaranteeDataRepository bankGuaranteeRepository;

    @Autowired
    private ProjectCostDataRepository projectCostRepository;

    @Autowired
    private MissingItemRequestRepository missingItemRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private com.magictech.core.auth.UserRepository userRepository;

    @Autowired
    private com.magictech.modules.projects.service.SiteSurveyRequestService siteSurveyRequestService;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Autowired
    private com.magictech.core.messaging.service.NotificationService coreNotificationService;

    @Autowired
    private com.magictech.modules.projects.repository.ProjectElementRepository projectElementRepository;

    /**
     * Create new workflow for a project
     */
    public ProjectWorkflow createWorkflow(Long projectId, User salesUser) {
        // Check if workflow already exists
        if (workflowRepository.existsByProjectIdAndActiveTrue(projectId)) {
            throw new RuntimeException("Workflow already exists for this project");
        }

        ProjectWorkflow workflow = new ProjectWorkflow();
        workflow.setProjectId(projectId);
        workflow.setCreatedBy(salesUser.getUsername());
        workflow.setCreatedById(salesUser.getId());
        workflow.setCurrentStep(1);
        workflow.setStatus(ProjectWorkflow.WorkflowStatusType.IN_PROGRESS);

        workflow = workflowRepository.save(workflow);

        // Create all 8 step completion records
        stepService.createStepsForWorkflow(workflow);

        return workflow;
    }

    /**
     * Get workflow by project ID
     */
    public Optional<ProjectWorkflow> getWorkflowByProjectId(Long projectId) {
        return workflowRepository.findByProjectIdAndActiveTrue(projectId);
    }

    /**
     * Get workflow by ID
     */
    public Optional<ProjectWorkflow> getWorkflowById(Long workflowId) {
        return workflowRepository.findById(workflowId);
    }

    /**
     * Get all workflows for a user
     */
    public List<ProjectWorkflow> getUserWorkflows(User user) {
        return workflowRepository.findByCreatedByIdAndActiveTrue(user.getId());
    }

    /**
     * Get all active workflows
     */
    public List<ProjectWorkflow> getAllActiveWorkflows() {
        return workflowRepository.findByActiveTrue();
    }

    /**
     * STEP 1: Process Site Survey - Sales does it himself
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void processSiteSurveySales(Long workflowId, byte[] excelFile, String fileName,
                                       User salesUser) throws Exception {
        ProjectWorkflow workflow = getWorkflowById(workflowId)
            .orElseThrow(() -> new RuntimeException("Workflow not found"));

        validateStepCanStart(workflow, 1);

        Project project = projectRepository.findById(workflow.getProjectId())
            .orElseThrow(() -> new RuntimeException("Project not found"));

        // Validate Excel file
        if (!siteSurveyExcelService.isValidExcelFile(excelFile, fileName)) {
            throw new IllegalArgumentException("Invalid Excel file. Please upload a valid .xlsx or .xls file.");
        }

        // Parse Excel with comprehensive image and data extraction
        String parsedData = siteSurveyExcelService.parseExcelToJson(excelFile, fileName);

        SiteSurveyData surveyData = new SiteSurveyData();
        surveyData.setProjectId(workflow.getProjectId());
        surveyData.setWorkflowId(workflowId);
        surveyData.setExcelFile(excelFile);
        surveyData.setFileName(fileName);
        surveyData.setFileSize((long) excelFile.length);
        surveyData.setMimeType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        surveyData.setParsedData(parsedData);
        surveyData.setSurveyDoneBy("SALES");
        surveyData.setSurveyDoneByUser(salesUser.getUsername());
        surveyData.setSurveyDoneByUserId(salesUser.getId());
        surveyData.setUploadedBy(salesUser.getUsername());
        surveyData.setUploadedById(salesUser.getId());

        siteSurveyRepository.save(surveyData);

        System.out.println("✅ Site survey data saved for project: " + project.getProjectName());

        // CRITICAL FIX: Flush site survey to database FIRST
        entityManager.flush();

        // Complete step
        WorkflowStepCompletion step = stepService.getStep(workflowId, 1)
            .orElseThrow(() -> new RuntimeException("Step not found"));

        System.out.println("🔧 DEBUG: Before completeStep - Step 1 completed status: " + step.getCompleted());

        stepService.completeStep(step, salesUser);

        System.out.println("🔧 DEBUG: After completeStep - Step 1 completed status: " + step.getCompleted());

        // CRITICAL FIX: Flush step completion immediately
        entityManager.flush();

        System.out.println("✅ Workflow step 1 marked as completed and flushed to database");

        // Move to next step
        advanceToNextStep(workflow, salesUser);

        // CRITICAL FIX: Flush workflow advancement
        entityManager.flush();

        System.out.println("➡️ Workflow advanced to step " + workflow.getCurrentStep());

        // Notify Projects team that Sales has completed the site survey
        notificationService.notifySiteSurveyCompletedBySales(project, salesUser);

        System.out.println("🔔 Notification sent to Projects team about site survey completion");

        // NOTE: Step 2 (Selection & Design) is now OPTIONAL
        // User will be prompted to choose whether to request from Presales or skip
        System.out.println("✅ Step 1 completed. User will now choose whether to request from Presales in Step 2.");
    }

    /**
     * STEP 1: Process Site Survey with ZIP file - Sales does it himself
     * Alternative to Excel upload - stores ZIP archive instead
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void processSiteSurveySalesWithZip(Long workflowId, byte[] zipFile, String fileName,
                                              User salesUser) throws Exception {
        ProjectWorkflow workflow = getWorkflowById(workflowId)
            .orElseThrow(() -> new RuntimeException("Workflow not found"));

        validateStepCanStart(workflow, 1);

        Project project = projectRepository.findById(workflow.getProjectId())
            .orElseThrow(() -> new RuntimeException("Project not found"));

        // Validate ZIP file
        if (!fileName.toLowerCase().endsWith(".zip")) {
            throw new IllegalArgumentException("Invalid file. Please upload a valid .zip file.");
        }

        SiteSurveyData surveyData = new SiteSurveyData();
        surveyData.setProjectId(workflow.getProjectId());
        surveyData.setWorkflowId(workflowId);
        // Store as ZIP instead of Excel
        surveyData.setZipFile(zipFile);
        surveyData.setZipFileName(fileName);
        surveyData.setZipFileSize((long) zipFile.length);
        surveyData.setZipMimeType("application/zip");
        surveyData.setFileType("ZIP");
        surveyData.setSurveyDoneBy("SALES");
        surveyData.setSurveyDoneByUser(salesUser.getUsername());
        surveyData.setSurveyDoneByUserId(salesUser.getId());
        surveyData.setUploadedBy(salesUser.getUsername());
        surveyData.setUploadedById(salesUser.getId());

        siteSurveyRepository.save(surveyData);

        System.out.println("✅ Site survey ZIP data saved for project: " + project.getProjectName());

        entityManager.flush();

        // Complete step
        WorkflowStepCompletion step = stepService.getStep(workflowId, 1)
            .orElseThrow(() -> new RuntimeException("Step not found"));

        stepService.completeStep(step, salesUser);
        entityManager.flush();

        System.out.println("✅ Workflow step 1 marked as completed (ZIP upload)");

        // Move to next step
        advanceToNextStep(workflow, salesUser);
        entityManager.flush();

        System.out.println("➡️ Workflow advanced to step " + workflow.getCurrentStep());

        // Notify Projects team
        notificationService.notifySiteSurveyCompletedBySales(project, salesUser);

        System.out.println("✅ Step 1 completed with ZIP file upload.");
    }

    /**
     * STEP 1: Request site survey from Project team
     */
    public void requestSiteSurveyFromProject(Long workflowId, User salesUser) {
        ProjectWorkflow workflow = getWorkflowById(workflowId)
            .orElseThrow(() -> new RuntimeException("Workflow not found"));

        validateStepCanStart(workflow, 1);

        Project project = projectRepository.findById(workflow.getProjectId())
            .orElseThrow(() -> new RuntimeException("Project not found"));

        // Mark step as needing external action
        WorkflowStepCompletion step = stepService.getStep(workflowId, 1)
            .orElseThrow(() -> new RuntimeException("Step not found"));
        stepService.markNeedsExternalAction(step, "PROJECT");

        // Create site survey request entity in database
        // This is critical so PROJECT team can see and fulfill the request
        siteSurveyRequestService.createRequest(
            project.getId(),
            salesUser.getUsername(),
            salesUser.getId(),
            "PROJECT",  // assigned to PROJECT team
            "HIGH",     // default priority
            "Site survey requested via workflow by Sales team"
        );

        // Send notification to project team
        notificationService.notifySiteSurveyRequest(project, salesUser);
    }

    /**
     * STEP 1: Project team submits site survey
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void submitSiteSurveyFromProject(Long workflowId, byte[] excelFile, String fileName,
                                            User projectUser) throws Exception {
        ProjectWorkflow workflow = getWorkflowById(workflowId)
            .orElseThrow(() -> new RuntimeException("Workflow not found"));

        Project project = projectRepository.findById(workflow.getProjectId())
            .orElseThrow(() -> new RuntimeException("Project not found"));

        // Validate Excel file
        if (!siteSurveyExcelService.isValidExcelFile(excelFile, fileName)) {
            throw new IllegalArgumentException("Invalid Excel file. Please upload a valid .xlsx or .xls file.");
        }

        // Parse Excel with comprehensive image and data extraction
        String parsedData = siteSurveyExcelService.parseExcelToJson(excelFile, fileName);

        SiteSurveyData surveyData = new SiteSurveyData();
        surveyData.setProjectId(workflow.getProjectId());
        surveyData.setWorkflowId(workflowId);
        surveyData.setExcelFile(excelFile);
        surveyData.setFileName(fileName);
        surveyData.setFileSize((long) excelFile.length);
        surveyData.setMimeType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        surveyData.setParsedData(parsedData);
        surveyData.setSurveyDoneBy("PROJECT");
        surveyData.setSurveyDoneByUser(projectUser.getUsername());
        surveyData.setSurveyDoneByUserId(projectUser.getId());
        surveyData.setUploadedBy(projectUser.getUsername());
        surveyData.setUploadedById(projectUser.getId());

        siteSurveyRepository.save(surveyData);

        System.out.println("✅ Site survey data saved for project: " + project.getProjectName());

        // CRITICAL FIX: Flush site survey to database FIRST
        entityManager.flush();

        // Mark external action as completed
        WorkflowStepCompletion step = stepService.getStep(workflowId, 1)
            .orElseThrow(() -> new RuntimeException("Step not found"));

        System.out.println("🔧 DEBUG: Before completeStep (PROJECT) - Step 1 completed status: " + step.getCompleted());

        stepService.completeExternalAction(step, projectUser);
        stepService.completeStep(step, projectUser);

        System.out.println("🔧 DEBUG: After completeStep (PROJECT) - Step 1 completed status: " + step.getCompleted());

        // CRITICAL FIX: Flush step completion immediately
        entityManager.flush();

        System.out.println("✅ Workflow step 1 marked as completed and flushed to database");

        // Move to next step
        advanceToNextStep(workflow, projectUser);

        // CRITICAL FIX: Flush workflow advancement
        entityManager.flush();

        System.out.println("➡️ Workflow advanced to step " + workflow.getCurrentStep());

        // Notify sales user
        User salesUser = getUserById(workflow.getCreatedById());
        notificationService.notifySiteSurveyCompleted(project, projectUser, salesUser);

        System.out.println("🔔 Notification sent to Sales user: " + salesUser.getUsername() +
                         " about site survey completion for project: " + project.getProjectName());

        // NOTE: Step 2 (Selection & Design) is now OPTIONAL
        // User will be prompted to choose whether to request from Presales or skip
        System.out.println("✅ Step 1 completed. User will now choose whether to request from Presales in Step 2.");
    }

    /**
     * STEP 2: Mark selection & design as not needed
     */
    public void markSelectionDesignNotNeeded(Long workflowId, User salesUser) {
        ProjectWorkflow workflow = getWorkflowById(workflowId)
            .orElseThrow(() -> new RuntimeException("Workflow not found"));

        validateStepCanStart(workflow, 2);

        WorkflowStepCompletion step = stepService.getStep(workflowId, 2)
            .orElseThrow(() -> new RuntimeException("Step not found"));
        stepService.addNotes(step, "Marked as not needed by " + salesUser.getUsername());
        stepService.completeStep(step, salesUser);

        advanceToNextStep(workflow, salesUser);
    }

    /**
     * STEP 2: Request selection & design from Presales
     */
    public void requestSelectionDesignFromPresales(Long workflowId, User salesUser) {
        ProjectWorkflow workflow = getWorkflowById(workflowId)
            .orElseThrow(() -> new RuntimeException("Workflow not found"));

        System.out.println("🔍 DEBUG requestSelectionDesignFromPresales:");
        System.out.println("   Workflow ID: " + workflow.getId());
        System.out.println("   Current step: " + workflow.getCurrentStep());
        System.out.println("   Step 1 completed (workflow flag): " + workflow.getStep1Completed());

        // Check if step 1 is completed
        Optional<WorkflowStepCompletion> step1Opt = stepService.getStep(workflowId, 1);
        if (step1Opt.isPresent()) {
            WorkflowStepCompletion step1 = step1Opt.get();
            System.out.println("   Step 1 completed (step table): " + step1.getCompleted());
            System.out.println("   Step 1 ID: " + step1.getId());
            System.out.println("   Step 1 completed at: " + step1.getCompletedAt());
            System.out.println("   Step 1 completed by: " + step1.getCompletedBy());
        } else {
            System.out.println("   ❌ Step 1 NOT FOUND in database!");
        }

        // CRITICAL: If step 1 is not completed, don't validate - just fail with clear message
        if (step1Opt.isEmpty() || !Boolean.TRUE.equals(step1Opt.get().getCompleted())) {
            throw new RuntimeException(
                "Step 1 (Site Survey) is not completed yet. Please ensure the site survey is uploaded and saved before requesting from Presales.\n\n" +
                "Current workflow state:\n" +
                "- Workflow current step: " + workflow.getCurrentStep() + "\n" +
                "- Step 1 completed: " + (step1Opt.isPresent() ? step1Opt.get().getCompleted() : "NOT FOUND") + "\n\n" +
                "This usually means the site survey upload failed or wasn't saved to the database."
            );
        }

        validateStepCanStart(workflow, 2);

        Project project = projectRepository.findById(workflow.getProjectId())
            .orElseThrow(() -> new RuntimeException("Project not found"));

        WorkflowStepCompletion step = stepService.getStep(workflowId, 2)
            .orElseThrow(() -> new RuntimeException("Step not found"));
        stepService.markNeedsExternalAction(step, "PRESALES");

        // CRITICAL FIX: Flush to ensure presales notification is persisted
        entityManager.flush();

        notificationService.notifyPresalesSelectionDesign(workflowId, project, salesUser);

        System.out.println("✅ Selection & Design request sent to Presales team");
    }

    /**
     * STEP 2: Presales submits sizing and pricing
     */
    public void submitSizingPricing(Long workflowId, byte[] excelFile, String fileName,
                                   User presalesUser) throws Exception {
        ProjectWorkflow workflow = getWorkflowById(workflowId)
            .orElseThrow(() -> new RuntimeException("Workflow not found"));

        Project project = projectRepository.findById(workflow.getProjectId())
            .orElseThrow(() -> new RuntimeException("Project not found"));

        String parsedData = excelStorageService.parseExcelFile(excelFile);

        SizingPricingData sizingData = new SizingPricingData();
        sizingData.setProjectId(workflow.getProjectId());
        sizingData.setWorkflowId(workflowId);
        sizingData.setExcelFile(excelFile);
        sizingData.setFileName(fileName);
        sizingData.setFileSize((long) excelFile.length);
        sizingData.setMimeType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        sizingData.setParsedData(parsedData);
        sizingData.setUploadedBy(presalesUser.getUsername());
        sizingData.setUploadedById(presalesUser.getId());

        sizingPricingRepository.save(sizingData);

        System.out.println("✅ Sizing/Pricing data saved for project: " + project.getProjectName());

        // CRITICAL FIX: Flush sizing data to database FIRST
        entityManager.flush();

        WorkflowStepCompletion step = stepService.getStep(workflowId, 2)
            .orElseThrow(() -> new RuntimeException("Step not found"));
        stepService.completeExternalAction(step, presalesUser);
        stepService.completeStep(step, presalesUser);

        // CRITICAL FIX: Flush step completion immediately
        entityManager.flush();

        System.out.println("✅ Workflow step 2 marked as completed and flushed to database");

        advanceToNextStep(workflow, presalesUser);

        // CRITICAL FIX: Flush workflow advancement
        entityManager.flush();

        System.out.println("➡️ Workflow advanced to step " + workflow.getCurrentStep());

        User salesUser = getUserById(workflow.getCreatedById());
        notificationService.notifyPresalesCompleted(project, presalesUser, salesUser);

        System.out.println("🔔 Notification sent to Sales user about presales completion");

        // NOTE: Step 3 (Bank Guarantee) is now OPTIONAL
        // User will be prompted to choose whether to request bank guarantee or skip
        System.out.println("✅ Step 2 completed. User will now choose whether to request bank guarantee in Step 3.");
    }

    /**
     * STEP 3: Mark bank guarantee as not needed
     */
    public void markBankGuaranteeNotNeeded(Long workflowId, User salesUser) {
        ProjectWorkflow workflow = getWorkflowById(workflowId)
            .orElseThrow(() -> new RuntimeException("Workflow not found"));

        validateStepCanStart(workflow, 3);

        WorkflowStepCompletion step = stepService.getStep(workflowId, 3)
            .orElseThrow(() -> new RuntimeException("Step not found"));
        stepService.addNotes(step, "Marked as not needed by " + salesUser.getUsername());
        stepService.completeStep(step, salesUser);

        advanceToNextStep(workflow, salesUser);
    }

    /**
     * STEP 3: Request bank guarantee from Finance
     */
    public void requestBankGuarantee(Long workflowId, User salesUser) {
        ProjectWorkflow workflow = getWorkflowById(workflowId)
            .orElseThrow(() -> new RuntimeException("Workflow not found"));

        validateStepCanStart(workflow, 3);

        Project project = projectRepository.findById(workflow.getProjectId())
            .orElseThrow(() -> new RuntimeException("Project not found"));

        WorkflowStepCompletion step = stepService.getStep(workflowId, 3)
            .orElseThrow(() -> new RuntimeException("Step not found"));
        stepService.markNeedsExternalAction(step, "FINANCE");

        notificationService.notifyBankGuaranteeRequest(project, salesUser);
    }

    /**
     * STEP 3: Finance submits bank guarantee
     */
    public void submitBankGuarantee(Long workflowId, byte[] excelFile, String fileName,
                                   User financeUser) throws Exception {
        ProjectWorkflow workflow = getWorkflowById(workflowId)
            .orElseThrow(() -> new RuntimeException("Workflow not found"));

        Project project = projectRepository.findById(workflow.getProjectId())
            .orElseThrow(() -> new RuntimeException("Project not found"));

        // Parse bank guarantee data (simple parsing like site survey)
        String parsedData;
        try {
            parsedData = excelStorageService.parseExcelFile(excelFile);
        } catch (Exception e) {
            System.err.println("Warning: Failed to parse bank guarantee Excel: " + e.getMessage());
            parsedData = "{}"; // Empty JSON if parsing fails
        }

        BankGuaranteeData guaranteeData = new BankGuaranteeData();
        guaranteeData.setProjectId(workflow.getProjectId());
        guaranteeData.setWorkflowId(workflowId);
        guaranteeData.setExcelFile(excelFile);
        guaranteeData.setFileName(fileName);
        guaranteeData.setFileSize((long) excelFile.length);
        guaranteeData.setMimeType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        guaranteeData.setParsedData(parsedData);
        guaranteeData.setUploadedBy(financeUser.getUsername());
        guaranteeData.setUploadedById(financeUser.getId());

        bankGuaranteeRepository.save(guaranteeData);
        System.out.println("✅ Bank guarantee data saved for project: " + project.getProjectName());

        // CRITICAL FIX: Flush bank guarantee to database FIRST
        entityManager.flush();

        // Complete external action and step
        WorkflowStepCompletion step = stepService.getStep(workflowId, 3)
            .orElseThrow(() -> new RuntimeException("Step not found"));
        stepService.completeExternalAction(step, financeUser);
        stepService.completeStep(step, financeUser);

        // Flush step completion
        entityManager.flush();
        System.out.println("✅ Workflow step 3 marked as completed and flushed to database");

        // Advance to next step
        advanceToNextStep(workflow, financeUser);
        entityManager.flush();
        System.out.println("➡️ Workflow advanced to step " + workflow.getCurrentStep());

        // Notify sales user
        User salesUser = getUserById(workflow.getCreatedById());
        notificationService.notifyBankGuaranteeCompleted(project, financeUser, salesUser);
        System.out.println("🔔 Notification sent to Sales user about bank guarantee completion");
    }

    /**
     * STEP 4: Mark no missing items
     */
    public void markNoMissingItems(Long workflowId, User salesUser) {
        ProjectWorkflow workflow = getWorkflowById(workflowId)
            .orElseThrow(() -> new RuntimeException("Workflow not found"));

        validateStepCanStart(workflow, 4);

        WorkflowStepCompletion step = stepService.getStep(workflowId, 4)
            .orElseThrow(() -> new RuntimeException("Step not found"));
        stepService.addNotes(step, "No missing items - marked by " + salesUser.getUsername());
        stepService.completeStep(step, salesUser);

        advanceToNextStep(workflow, salesUser);
    }

    /**
     * STEP 4: Complete step with elements added from storage
     * Called when Sales adds elements to the project and clicks "Complete Step 4"
     * Notifies Presales about the element changes
     */
    public void completeStep4WithElements(Long workflowId, User salesUser) {
        System.out.println("📦 STEP 4: Completing with elements added by " + salesUser.getUsername());

        ProjectWorkflow workflow = getWorkflowById(workflowId)
            .orElseThrow(() -> new RuntimeException("Workflow not found"));

        validateStepCanStart(workflow, 4);

        Project project = projectRepository.findById(workflow.getProjectId())
            .orElseThrow(() -> new RuntimeException("Project not found"));

        // Get the elements that were added to this project
        java.util.List<com.magictech.modules.projects.entity.ProjectElement> elements =
            projectElementRepository.findByProjectIdAndActiveTrue(project.getId());

        int elementCount = elements.size();
        System.out.println("   Project: " + project.getProjectName());
        System.out.println("   Elements count: " + elementCount);

        // Mark step 4 as completed
        WorkflowStepCompletion step = stepService.getStep(workflowId, 4)
            .orElseThrow(() -> new RuntimeException("Step not found"));
        stepService.addNotes(step, "Elements added by " + salesUser.getUsername() +
            " - Total elements: " + elementCount);
        stepService.completeStep(step, salesUser);

        // Send notification to Presales about the element changes
        notifyPresalesAboutElementChanges(project, salesUser, elementCount);

        // Advance to next step
        advanceToNextStep(workflow, salesUser);

        System.out.println("✅ STEP 4: Completed and advanced to Step 5");
    }

    /**
     * Send notification to Presales team about element changes made by Sales
     */
    private void notifyPresalesAboutElementChanges(Project project, User salesUser, int elementCount) {
        System.out.println("📢 Sending notification to Presales about element changes");

        com.magictech.core.messaging.dto.NotificationMessage message =
            new com.magictech.core.messaging.dto.NotificationMessage.Builder()
                .title("Project Elements Updated")
                .message(String.format(
                    "Sales team member %s has updated elements for project '%s'. " +
                    "Total elements: %d. Please review the changes.",
                    salesUser.getUsername(), project.getProjectName(), elementCount))
                .type(com.magictech.core.messaging.constants.NotificationConstants.TYPE_INFO)
                .module(com.magictech.core.messaging.constants.NotificationConstants.MODULE_SALES)
                .targetModule(com.magictech.core.messaging.constants.NotificationConstants.MODULE_PRESALES)
                .entityType(com.magictech.core.messaging.constants.NotificationConstants.ENTITY_PROJECT)
                .entityId(project.getId())
                .priority(com.magictech.core.messaging.constants.NotificationConstants.PRIORITY_HIGH)
                .createdBy(salesUser.getUsername())
                .build();

        // Use the core notification service to publish
        coreNotificationService.publishNotification(message);

        System.out.println("✅ Notification sent to Presales team");
    }

    /**
     * STEP 4: Submit missing item request
     */
    public void submitMissingItemRequest(Long workflowId, MissingItemRequest request, User salesUser) {
        ProjectWorkflow workflow = getWorkflowById(workflowId)
            .orElseThrow(() -> new RuntimeException("Workflow not found"));

        validateStepCanStart(workflow, 4);

        Project project = projectRepository.findById(workflow.getProjectId())
            .orElseThrow(() -> new RuntimeException("Project not found"));

        request.setProjectId(workflow.getProjectId());
        request.setWorkflowId(workflowId);
        request.setRequestedBy(salesUser.getUsername());
        request.setRequestedById(salesUser.getId());

        missingItemRepository.save(request);

        WorkflowStepCompletion step = stepService.getStep(workflowId, 4)
            .orElseThrow(() -> new RuntimeException("Step not found"));
        stepService.markNeedsExternalAction(step, "MASTER_SALES_MANAGER");

        String itemDetails = request.getItemName() + " (Qty: " + request.getQuantityNeeded() + ")";
        notificationService.notifyMissingItemRequest(project, salesUser, itemDetails);
    }

    /**
     * STEP 4: Approve missing item (by MASTER or SALES_MANAGER)
     */
    public void approveMissingItem(Long missingItemId, User approver) {
        MissingItemRequest request = missingItemRepository.findById(missingItemId)
            .orElseThrow(() -> new RuntimeException("Missing item request not found"));

        if (approver.getRole().name().equals("MASTER")) {
            request.setApprovedByMaster(approver.getUsername());
            request.setApprovedByMasterId(approver.getId());
        } else if (approver.getRole().name().equals("SALES_MANAGER")) {
            request.setApprovedBySalesManager(approver.getUsername());
            request.setApprovedBySalesManagerId(approver.getId());
        }

        // Check if fully approved
        if (request.isFullyApproved()) {
            request.setItemDelivered(true);
            request.setDeliveryConfirmedBy(approver.getUsername());
            request.setDeliveryConfirmedAt(LocalDateTime.now());

            // Complete step
            WorkflowStepCompletion step = stepService.getStep(request.getWorkflowId(), 4)
                .orElseThrow(() -> new RuntimeException("Step not found"));
            stepService.completeExternalAction(step, approver);
            stepService.completeStep(step, approver);

            // Notify sales user
            ProjectWorkflow workflow = getWorkflowById(request.getWorkflowId()).orElseThrow();
            Project project = projectRepository.findById(workflow.getProjectId()).orElseThrow();
            User salesUser = getUserById(workflow.getCreatedById());
            notificationService.notifyMissingItemApproved(project, approver, salesUser);

            // Move to next step
            advanceToNextStep(workflow, approver);
        }

        missingItemRepository.save(request);
    }

    /**
     * STEP 5: Mark tender as accepted and push to Project module
     */
    public void markTenderAccepted(Long workflowId, User salesUser) {
        ProjectWorkflow workflow = getWorkflowById(workflowId)
            .orElseThrow(() -> new RuntimeException("Workflow not found"));

        validateStepCanStart(workflow, 5);

        Project project = projectRepository.findById(workflow.getProjectId())
            .orElseThrow(() -> new RuntimeException("Project not found"));

        WorkflowStepCompletion step = stepService.getStep(workflowId, 5)
            .orElseThrow(() -> new RuntimeException("Step not found"));
        stepService.addNotes(step, "Tender accepted");
        stepService.markNeedsExternalAction(step, "PROJECT");

        // Notify project team to start work
        notificationService.notifyProjectStart(project, salesUser);
    }

    /**
     * STEP 5: Mark tender as rejected with reason
     */
    public void markTenderRejected(Long workflowId, String rejectionReason, User salesUser) {
        ProjectWorkflow workflow = getWorkflowById(workflowId)
            .orElseThrow(() -> new RuntimeException("Workflow not found"));

        validateStepCanStart(workflow, 5);

        WorkflowStepCompletion step = stepService.getStep(workflowId, 5)
            .orElseThrow(() -> new RuntimeException("Step not found"));
        stepService.addRejectionReason(step, rejectionReason, salesUser);

        // Mark workflow as rejected
        workflow.setStatus(ProjectWorkflow.WorkflowStatusType.REJECTED);
        workflowRepository.save(workflow);
    }

    /**
     * STEP 5: Project team notifies completion
     */
    public void notifyProjectCompletion(Long workflowId, User projectUser) {
        ProjectWorkflow workflow = getWorkflowById(workflowId)
            .orElseThrow(() -> new RuntimeException("Workflow not found"));

        Project project = projectRepository.findById(workflow.getProjectId())
            .orElseThrow(() -> new RuntimeException("Project not found"));

        WorkflowStepCompletion step = stepService.getStep(workflowId, 5)
            .orElseThrow(() -> new RuntimeException("Step not found"));
        stepService.completeExternalAction(step, projectUser);
        stepService.completeStep(step, projectUser);

        User salesUser = getUserById(workflow.getCreatedById());
        notificationService.notifyProjectCompleted(project, projectUser, salesUser);

        advanceToNextStep(workflow, projectUser);
    }

    /**
     * STEP 6: Confirm project finished and upload project cost
     */
    public void confirmProjectFinished(Long workflowId, byte[] excelFile, String fileName,
                                      User salesUser) throws Exception {
        ProjectWorkflow workflow = getWorkflowById(workflowId)
            .orElseThrow(() -> new RuntimeException("Workflow not found"));

        validateStepCanStart(workflow, 6);

        String parsedData = excelStorageService.parseExcelFile(excelFile);

        ProjectCostData costData = new ProjectCostData();
        costData.setProjectId(workflow.getProjectId());
        costData.setWorkflowId(workflowId);
        costData.setExcelFile(excelFile);
        costData.setFileName(fileName);
        costData.setFileSize((long) excelFile.length);
        costData.setMimeType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        costData.setParsedData(parsedData);
        costData.setUploadedBy(salesUser.getUsername());
        costData.setUploadedById(salesUser.getId());
        costData.setProjectReceivedConfirmation(true);

        projectCostRepository.save(costData);

        WorkflowStepCompletion step = stepService.getStep(workflowId, 6)
            .orElseThrow(() -> new RuntimeException("Step not found"));
        stepService.completeStep(step, salesUser);

        advanceToNextStep(workflow, salesUser);
    }

    /**
     * STEP 6: Confirm project finished and upload project cost as ZIP file
     */
    public void confirmProjectFinishedWithZip(Long workflowId, byte[] zipFile, String fileName,
                                              User salesUser) throws Exception {
        ProjectWorkflow workflow = getWorkflowById(workflowId)
            .orElseThrow(() -> new RuntimeException("Workflow not found"));

        validateStepCanStart(workflow, 6);

        // Validate ZIP file
        if (!fileName.toLowerCase().endsWith(".zip")) {
            throw new IllegalArgumentException("Invalid file. Please upload a valid .zip file.");
        }

        ProjectCostData costData = new ProjectCostData();
        costData.setProjectId(workflow.getProjectId());
        costData.setWorkflowId(workflowId);
        // Store as ZIP instead of Excel
        costData.setZipFile(zipFile);
        costData.setZipFileName(fileName);
        costData.setZipFileSize((long) zipFile.length);
        costData.setZipMimeType("application/zip");
        costData.setFileType("ZIP");
        costData.setUploadedBy(salesUser.getUsername());
        costData.setUploadedById(salesUser.getId());
        costData.setProjectReceivedConfirmation(true);

        projectCostRepository.save(costData);

        WorkflowStepCompletion step = stepService.getStep(workflowId, 6)
            .orElseThrow(() -> new RuntimeException("Step not found"));
        stepService.completeStep(step, salesUser);

        advanceToNextStep(workflow, salesUser);

        System.out.println("✅ Step 6 completed with ZIP file upload.");
    }

    /**
     * STEP 6: Report project delay
     */
    public void reportProjectDelay(Long workflowId, LocalDateTime expectedDate, String delayDetails,
                                  User salesUser) {
        ProjectWorkflow workflow = getWorkflowById(workflowId)
            .orElseThrow(() -> new RuntimeException("Workflow not found"));

        validateStepCanStart(workflow, 6);

        Project project = projectRepository.findById(workflow.getProjectId())
            .orElseThrow(() -> new RuntimeException("Project not found"));

        WorkflowStepCompletion step = stepService.getStep(workflowId, 6)
            .orElseThrow(() -> new RuntimeException("Step not found"));
        stepService.markStepDelayed(step, expectedDate);
        stepService.markDangerAlarmSent(step);

        // Send DANGER notification to MASTER
        notificationService.notifyProjectDelayDanger(project, salesUser, delayDetails);
    }

    /**
     * STEP 7: Mark after-sales check as not needed
     */
    public void markAfterSalesNotNeeded(Long workflowId, User salesUser) {
        ProjectWorkflow workflow = getWorkflowById(workflowId)
            .orElseThrow(() -> new RuntimeException("Workflow not found"));

        validateStepCanStart(workflow, 7);

        WorkflowStepCompletion step = stepService.getStep(workflowId, 7)
            .orElseThrow(() -> new RuntimeException("Step not found"));
        stepService.addNotes(step, "After-sales check not needed - marked by " + salesUser.getUsername());
        stepService.completeStep(step, salesUser);

        advanceToNextStep(workflow, salesUser);
    }

    /**
     * STEP 7: Request after-sales check from QA
     */
    public void requestAfterSalesCheck(Long workflowId, User salesUser) {
        ProjectWorkflow workflow = getWorkflowById(workflowId)
            .orElseThrow(() -> new RuntimeException("Workflow not found"));

        validateStepCanStart(workflow, 7);

        Project project = projectRepository.findById(workflow.getProjectId())
            .orElseThrow(() -> new RuntimeException("Project not found"));

        WorkflowStepCompletion step = stepService.getStep(workflowId, 7)
            .orElseThrow(() -> new RuntimeException("Step not found"));
        stepService.markNeedsExternalAction(step, "QUALITY_ASSURANCE");

        notificationService.notifyQAAfterSalesCheck(project, salesUser);
    }

    /**
     * STEP 7: QA completes after-sales check
     */
    public void completeAfterSalesCheck(Long workflowId, User qaUser) {
        ProjectWorkflow workflow = getWorkflowById(workflowId)
            .orElseThrow(() -> new RuntimeException("Workflow not found"));

        Project project = projectRepository.findById(workflow.getProjectId())
            .orElseThrow(() -> new RuntimeException("Project not found"));

        WorkflowStepCompletion step = stepService.getStep(workflowId, 7)
            .orElseThrow(() -> new RuntimeException("Step not found"));
        stepService.completeExternalAction(step, qaUser);
        stepService.completeStep(step, qaUser);

        User salesUser = getUserById(workflow.getCreatedById());
        notificationService.notifyQACheckCompleted(project, qaUser, salesUser);

        advanceToNextStep(workflow, qaUser);
    }

    /**
     * STEP 8: Complete workflow and push to storage analysis
     */
    public void completeWorkflow(Long workflowId, User salesUser) {
        ProjectWorkflow workflow = getWorkflowById(workflowId)
            .orElseThrow(() -> new RuntimeException("Workflow not found"));

        validateStepCanStart(workflow, 8);

        Project project = projectRepository.findById(workflow.getProjectId())
            .orElseThrow(() -> new RuntimeException("Project not found"));

        WorkflowStepCompletion step = stepService.getStep(workflowId, 8)
            .orElseThrow(() -> new RuntimeException("Step not found"));
        stepService.completeStep(step, salesUser);

        // Mark workflow as completed
        workflow.setStatus(ProjectWorkflow.WorkflowStatusType.COMPLETED);
        workflow.setCompletedAt(LocalDateTime.now());
        workflow.markStepCompleted(8);
        workflowRepository.save(workflow);

        // Send completion notification
        notificationService.notifyWorkflowCompleted(project, salesUser);

        // TODO: Push all data to storage analysis
        pushToStorageAnalysis(workflow, project);
    }

    /**
     * Push all workflow data to storage analysis
     */
    private void pushToStorageAnalysis(ProjectWorkflow workflow, Project project) {
        // TODO: Implement storage analysis data export
        // This will collect:
        // - Project data
        // - All workflow steps
        // - All uploaded Excel files
        // - Customer data
        // - Timeline information
        // - Cost analysis
        System.out.println("Pushing workflow " + workflow.getId() +
            " to storage analysis for project: " + project.getProjectName());
    }

    // Helper methods
    private void validateStepCanStart(ProjectWorkflow workflow, int stepNumber) {
        if (!stepService.canStartStep(workflow.getId(), stepNumber)) {
            throw new RuntimeException("Cannot start step " + stepNumber +
                ". Previous step must be completed first.");
        }
    }

    private void advanceToNextStep(ProjectWorkflow workflow, User user) {
        if (workflow.getCurrentStep() < 8) {
            workflow.setCurrentStep(workflow.getCurrentStep() + 1);
            workflow.setLastUpdatedBy(user.getUsername());
            workflow.markStepCompleted(workflow.getCurrentStep() - 1);
            workflowRepository.save(workflow);
        }
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
    }

    // ==================== PROJECT EXECUTION (Step 6) ====================

    /**
     * Mark project execution as completed successfully by the Projects team
     * This advances the Sales workflow from Step 6 to Step 7
     * @param workflowId The workflow ID
     * @param projectUser The Projects team user who completed the execution
     */
    public void markProjectExecutionCompleted(Long workflowId, User projectUser) {
        System.out.println("🏁 Marking project execution as COMPLETED for workflow " + workflowId);

        ProjectWorkflow workflow = getWorkflowById(workflowId)
            .orElseThrow(() -> new RuntimeException("Workflow not found: " + workflowId));

        // Get Step 6
        WorkflowStepCompletion step6 = stepService.getStep(workflowId, 6)
            .orElseThrow(() -> new RuntimeException("Step 6 not found"));

        // Mark external action completed
        step6.setExternalActionCompleted(true);
        step6.setExternalActionCompletedAt(LocalDateTime.now());
        step6.setExternalActionCompletedBy(projectUser.getUsername());
        step6.setProjectCompletionNotes("Project execution completed successfully by Projects team");

        // Complete the step
        stepService.completeStep(step6, projectUser);

        // Advance to step 7
        workflow.setCurrentStep(7);
        workflow.markStepCompleted(6);
        workflow.setLastUpdatedBy(projectUser.getUsername());
        workflowRepository.save(workflow);

        // Notify Sales team
        notifyProjectExecutionCompleted(workflow, projectUser, true, null);

        System.out.println("✅ Project execution completed. Workflow advanced to Step 7.");
    }

    /**
     * Mark project execution as completed with issues/explanation
     * This advances the Sales workflow but records the issues
     * @param workflowId The workflow ID
     * @param explanation The explanation/issues reported
     * @param projectUser The Projects team user
     */
    public void markProjectExecutionCompletedWithIssues(Long workflowId, String explanation, User projectUser) {
        System.out.println("⚠️ Marking project execution as COMPLETED WITH ISSUES for workflow " + workflowId);

        ProjectWorkflow workflow = getWorkflowById(workflowId)
            .orElseThrow(() -> new RuntimeException("Workflow not found: " + workflowId));

        // Get Step 6
        WorkflowStepCompletion step6 = stepService.getStep(workflowId, 6)
            .orElseThrow(() -> new RuntimeException("Step 6 not found"));

        // Mark external action completed with notes
        step6.setExternalActionCompleted(true);
        step6.setExternalActionCompletedAt(LocalDateTime.now());
        step6.setExternalActionCompletedBy(projectUser.getUsername());
        step6.setProjectCompletionNotes("Project completed with issues: " + explanation);
        step6.setHasIssues(true);

        // Complete the step
        stepService.completeStep(step6, projectUser);

        // Advance to step 7
        workflow.setCurrentStep(7);
        workflow.markStepCompleted(6);
        workflow.setLastUpdatedBy(projectUser.getUsername());
        workflowRepository.save(workflow);

        // Notify Sales team about the issues
        notifyProjectExecutionCompleted(workflow, projectUser, false, explanation);

        System.out.println("✅ Project execution completed with issues. Workflow advanced to Step 7.");
    }

    /**
     * Notify Sales team that project execution is completed
     */
    private void notifyProjectExecutionCompleted(ProjectWorkflow workflow, User projectUser,
                                                  boolean success, String explanation) {
        try {
            Project project = projectRepository.findById(workflow.getProjectId()).orElse(null);
            String projectName = project != null ? project.getProjectName() : "Unknown Project";

            String title = success
                ? "Project Execution Completed"
                : "Project Execution Completed with Issues";

            String message = success
                ? String.format("Project '%s' has been successfully completed by the Projects team (%s). " +
                               "Please proceed with After-Sales check.", projectName, projectUser.getUsername())
                : String.format("Project '%s' has been completed by the Projects team (%s) but with issues: %s",
                               projectName, projectUser.getUsername(), explanation);

            // Use core notification service with NotificationMessage builder
            com.magictech.core.messaging.dto.NotificationMessage notificationMessage =
                new com.magictech.core.messaging.dto.NotificationMessage.Builder()
                    .type(success ? "SUCCESS" : "WARNING")
                    .module("PROJECTS")
                    .action("PROJECT_EXECUTION_COMPLETED")
                    .entityType("WORKFLOW")
                    .entityId(workflow.getId())
                    .title(title)
                    .message(message)
                    .targetModule("SALES")
                    .priority("HIGH")
                    .build();
            coreNotificationService.publishNotification(notificationMessage);

            System.out.println("📨 Notification sent to Sales: " + title);
        } catch (Exception ex) {
            System.err.println("Failed to send notification: " + ex.getMessage());
        }
    }
}
