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

        // Complete step
        WorkflowStepCompletion step = stepService.getStep(workflowId, 1)
            .orElseThrow(() -> new RuntimeException("Step not found"));

        System.out.println("🔧 DEBUG: Before completeStep - Step 1 completed status: " + step.getCompleted());

        stepService.completeStep(step, salesUser);

        System.out.println("🔧 DEBUG: After completeStep - Step 1 completed status: " + step.getCompleted());

        // CRITICAL FIX: Flush to database to ensure completion is persisted before advancing
        entityManager.flush();

        System.out.println("✅ Workflow step 1 marked as completed and flushed to database");

        // VERIFICATION: Re-fetch from database to confirm it was saved
        Optional<WorkflowStepCompletion> verifyStep = stepService.getStep(workflowId, 1);
        if (verifyStep.isPresent()) {
            System.out.println("✅ VERIFICATION: Step 1 completed status in DB: " + verifyStep.get().getCompleted());
            System.out.println("✅ VERIFICATION: Step 1 completed by: " + verifyStep.get().getCompletedBy());
            System.out.println("✅ VERIFICATION: Step 1 completed at: " + verifyStep.get().getCompletedAt());
        } else {
            System.out.println("❌ VERIFICATION FAILED: Could not re-fetch step 1 from database!");
        }

        // Notify Projects team that Sales has completed the site survey
        notificationService.notifySiteSurveyCompletedBySales(project, salesUser);

        System.out.println("🔔 Notification sent to Projects team about site survey completion");

        // Move to next step
        advanceToNextStep(workflow, salesUser);

        // CRITICAL FIX: Flush workflow advancement to database
        entityManager.flush();

        System.out.println("➡️ Workflow advanced to next step and flushed to database");
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

        // Mark external action as completed
        WorkflowStepCompletion step = stepService.getStep(workflowId, 1)
            .orElseThrow(() -> new RuntimeException("Step not found"));

        System.out.println("🔧 DEBUG: Before completeStep (PROJECT) - Step 1 completed status: " + step.getCompleted());

        stepService.completeExternalAction(step, projectUser);
        stepService.completeStep(step, projectUser);

        System.out.println("🔧 DEBUG: After completeStep (PROJECT) - Step 1 completed status: " + step.getCompleted());

        // CRITICAL FIX: Flush to database to ensure completion is persisted before advancing
        entityManager.flush();

        System.out.println("✅ Workflow step 1 marked as completed and flushed to database");

        // VERIFICATION: Re-fetch from database to confirm it was saved
        Optional<WorkflowStepCompletion> verifyStep = stepService.getStep(workflowId, 1);
        if (verifyStep.isPresent()) {
            System.out.println("✅ VERIFICATION (PROJECT): Step 1 completed status in DB: " + verifyStep.get().getCompleted());
            System.out.println("✅ VERIFICATION (PROJECT): Step 1 completed by: " + verifyStep.get().getCompletedBy());
            System.out.println("✅ VERIFICATION (PROJECT): Step 1 completed at: " + verifyStep.get().getCompletedAt());
        } else {
            System.out.println("❌ VERIFICATION FAILED (PROJECT): Could not re-fetch step 1 from database!");
        }

        // Notify sales user
        User salesUser = getUserById(workflow.getCreatedById());
        notificationService.notifySiteSurveyCompleted(project, projectUser, salesUser);

        System.out.println("🔔 Notification sent to Sales user: " + salesUser.getUsername() +
                         " about site survey completion for project: " + project.getProjectName());

        // Move to next step
        advanceToNextStep(workflow, projectUser);

        // CRITICAL FIX: Flush workflow advancement to database
        entityManager.flush();

        System.out.println("➡️ Workflow advanced to next step and flushed to database");
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
        // CRITICAL FIX: Clear entity manager cache and re-fetch workflow
        entityManager.clear();

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

        notificationService.notifyPresalesSelectionDesign(project, salesUser);

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
        sizingData.setParsedData(parsedData);
        sizingData.setUploadedBy(presalesUser.getUsername());
        sizingData.setUploadedById(presalesUser.getId());

        sizingPricingRepository.save(sizingData);

        System.out.println("✅ Sizing/Pricing data saved for project: " + project.getProjectName());

        WorkflowStepCompletion step = stepService.getStep(workflowId, 2)
            .orElseThrow(() -> new RuntimeException("Step not found"));
        stepService.completeExternalAction(step, presalesUser);
        stepService.completeStep(step, presalesUser);

        // CRITICAL FIX: Flush to database to ensure completion is persisted before advancing
        entityManager.flush();

        System.out.println("✅ Workflow step 2 marked as completed and flushed to database");

        User salesUser = getUserById(workflow.getCreatedById());
        notificationService.notifyPresalesCompleted(project, presalesUser, salesUser);

        System.out.println("🔔 Notification sent to Sales user about presales completion");

        advanceToNextStep(workflow, presalesUser);

        // CRITICAL FIX: Flush workflow advancement to database
        entityManager.flush();

        System.out.println("➡️ Workflow advanced to next step and flushed to database");
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

        String parsedData = excelStorageService.parseExcelFile(excelFile);

        BankGuaranteeData guaranteeData = new BankGuaranteeData();
        guaranteeData.setProjectId(workflow.getProjectId());
        guaranteeData.setWorkflowId(workflowId);
        guaranteeData.setExcelFile(excelFile);
        guaranteeData.setFileName(fileName);
        guaranteeData.setFileSize((long) excelFile.length);
        guaranteeData.setParsedData(parsedData);
        guaranteeData.setUploadedBy(financeUser.getUsername());
        guaranteeData.setUploadedById(financeUser.getId());

        bankGuaranteeRepository.save(guaranteeData);

        WorkflowStepCompletion step = stepService.getStep(workflowId, 3)
            .orElseThrow(() -> new RuntimeException("Step not found"));
        stepService.completeExternalAction(step, financeUser);
        stepService.completeStep(step, financeUser);

        User salesUser = getUserById(workflow.getCreatedById());
        notificationService.notifyBankGuaranteeCompleted(project, financeUser, salesUser);

        advanceToNextStep(workflow, financeUser);
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
}
