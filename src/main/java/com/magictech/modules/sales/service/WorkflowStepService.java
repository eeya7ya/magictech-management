package com.magictech.modules.sales.service;

import com.magictech.core.auth.User;
import com.magictech.core.auth.UserRole;
import com.magictech.modules.sales.entity.ProjectWorkflow;
import com.magictech.modules.sales.entity.WorkflowStepCompletion;
import com.magictech.modules.sales.entity.WorkflowStepCompletion.AssignmentStatus;
import com.magictech.modules.sales.repository.WorkflowStepCompletionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing individual workflow step completions
 * Uses optimized batch queries to prevent N+1 query problem
 */
@Service
@Transactional
public class WorkflowStepService {

    @Autowired
    private WorkflowStepCompletionRepository stepRepository;

    /**
     * Create all 8 step completion records for a new workflow
     * Each step is configured with its target role for assignment
     */
    public void createStepsForWorkflow(ProjectWorkflow workflow) {
        String[] stepNames = {
            "Site Survey Check",
            "Selection & Design Check",
            "Bank Guarantee Check",
            "Missing Item Check",
            "Tender Acceptance Check",
            "Project Team Finished Check",
            "After Sales Check",
            "Completion"
        };

        // Target roles for each step
        String[] targetRoles = {
            UserRole.PROJECTS.name(),           // Step 1: Site Survey -> Project Manager
            UserRole.PRESALES.name(),           // Step 2: Selection & Design -> Presales
            UserRole.FINANCE.name(),            // Step 3: Bank Guarantee -> Finance
            UserRole.STORAGE.name(),            // Step 4: Missing Items -> Storage
            null,                                // Step 5: Tender Acceptance -> Self (Sales decision)
            UserRole.PROJECTS.name(),           // Step 6: Project Execution -> Projects
            UserRole.QUALITY_ASSURANCE.name(),  // Step 7: After Sales -> QA
            UserRole.MASTER.name()              // Step 8: Completion -> Master approval
        };

        for (int i = 1; i <= 8; i++) {
            WorkflowStepCompletion step = new WorkflowStepCompletion();
            step.setWorkflowId(workflow.getId());
            step.setProjectId(workflow.getProjectId());
            step.setStepNumber(i);
            step.setStepName(stepNames[i - 1]);
            step.setCompleted(false);
            step.setTargetRole(targetRoles[i - 1]);
            step.setAssignmentStatus(AssignmentStatus.PENDING_ASSIGNMENT);

            stepRepository.save(step);
        }
    }

    /**
     * Get step by workflow and step number
     * Queries database directly to ensure fresh data within transactions
     */
    public Optional<WorkflowStepCompletion> getStep(Long workflowId, Integer stepNumber) {
        return stepRepository.findByWorkflowIdAndStepNumberAndActiveTrue(workflowId, stepNumber);
    }

    /**
     * Get all steps for a workflow
     * Uses optimized batch query to fetch all steps in one database round trip
     */
    public List<WorkflowStepCompletion> getAllSteps(Long workflowId) {
        return stepRepository.findByWorkflowIdOptimized(workflowId);
    }

    /**
     * Mark step as completed
     */
    public void completeStep(WorkflowStepCompletion step, User user) {
        step.setCompleted(true);
        step.setCompletedBy(user.getUsername());
        step.setCompletedById(user.getId());
        step.setCompletedAt(LocalDateTime.now());
        stepRepository.save(step);
    }

    /**
     * Force save step completion (for explicit persistence)
     */
    public void forceStepSave(WorkflowStepCompletion step) {
        stepRepository.save(step);
        stepRepository.flush();
    }

    /**
     * Save step changes
     */
    public WorkflowStepCompletion save(WorkflowStepCompletion step) {
        return stepRepository.save(step);
    }

    /**
     * Mark step as requiring external action
     */
    public void markNeedsExternalAction(WorkflowStepCompletion step, String externalModule) {
        step.setNeedsExternalAction(true);
        step.setExternalModule(externalModule);
        step.setExternalActionCompleted(false);
        stepRepository.save(step);
    }

    /**
     * Mark external action as completed
     */
    public void completeExternalAction(WorkflowStepCompletion step, User externalUser) {
        step.setExternalActionCompleted(true);
        step.setExternalCompletedBy(externalUser.getUsername());
        step.setExternalCompletedAt(LocalDateTime.now());
        stepRepository.save(step);
    }

    /**
     * Add rejection reason (for step 5)
     */
    public void addRejectionReason(WorkflowStepCompletion step, String reason, User user) {
        step.setRejectionReason(reason);
        step.setCompleted(true);
        step.setCompletedBy(user.getUsername());
        step.setCompletedById(user.getId());
        step.setCompletedAt(LocalDateTime.now());
        stepRepository.save(step);
    }

    /**
     * Mark step as delayed (for step 6)
     */
    public void markStepDelayed(WorkflowStepCompletion step, LocalDateTime expectedDate) {
        step.setIsDelayed(true);
        step.setExpectedCompletionDate(expectedDate);
        stepRepository.save(step);
    }

    /**
     * Mark danger alarm sent (for step 6)
     */
    public void markDangerAlarmSent(WorkflowStepCompletion step) {
        step.setDangerAlarmSent(true);
        stepRepository.save(step);
    }

    /**
     * Add notes to step
     */
    public void addNotes(WorkflowStepCompletion step, String notes) {
        step.setNotes(notes);
        stepRepository.save(step);
    }

    /**
     * Get all pending external actions for a module
     */
    public List<WorkflowStepCompletion> getPendingExternalActions(String moduleName) {
        return stepRepository.findByExternalModuleAndExternalActionCompletedAndActiveTrue(
            moduleName, false
        );
    }

    /**
     * Get all delayed steps
     */
    public List<WorkflowStepCompletion> getDelayedSteps() {
        return stepRepository.findByIsDelayedAndActiveTrue(true);
    }

    /**
     * Check if step can be started (previous step must be completed)
     */
    public boolean canStartStep(Long workflowId, Integer stepNumber) {
        if (stepNumber == 1) {
            return true; // First step can always start
        }

        // Check if previous step is completed
        Optional<WorkflowStepCompletion> previousStep = getStep(workflowId, stepNumber - 1);
        return previousStep.isPresent() && Boolean.TRUE.equals(previousStep.get().getCompleted());
    }

    /**
     * Update step
     */
    public WorkflowStepCompletion updateStep(WorkflowStepCompletion step) {
        return stepRepository.save(step);
    }

    /**
     * Delete step (soft delete)
     */
    public void deleteStep(WorkflowStepCompletion step) {
        step.setActive(false);
        stepRepository.save(step);
    }

    // ============================================================
    // ASSIGNMENT METHODS - For role-based workflow routing
    // ============================================================

    /**
     * Assign a user to a workflow step
     * @param step The step to assign
     * @param assignedUser The user being assigned to complete this step
     * @param assignedBy The user making the assignment
     */
    public void assignUserToStep(WorkflowStepCompletion step, User assignedUser, User assignedBy) {
        step.assignUser(
            assignedUser.getId(),
            assignedUser.getUsername(),
            assignedUser.getEmail(),
            assignedBy.getId(),
            assignedBy.getUsername()
        );
        stepRepository.save(step);
    }

    /**
     * Get all steps assigned to a specific user
     */
    public List<WorkflowStepCompletion> getStepsAssignedToUser(Long userId) {
        return stepRepository.findByAssignedUserIdAndActiveTrue(userId);
    }

    /**
     * Get all pending (not completed) steps assigned to a user
     */
    public List<WorkflowStepCompletion> getPendingStepsForUser(Long userId) {
        return stepRepository.findPendingStepsForUser(userId);
    }

    /**
     * Get project IDs where a user is assigned to at least one step
     */
    public List<Long> getAssignedProjectIdsForUser(Long userId) {
        return stepRepository.findProjectIdsAssignedToUser(userId);
    }

    /**
     * Get workflow IDs where a user is assigned to at least one step
     */
    public List<Long> getAssignedWorkflowIdsForUser(Long userId) {
        return stepRepository.findWorkflowIdsAssignedToUser(userId);
    }

    /**
     * Count pending steps for a user (for badge/notification count)
     */
    public long countPendingStepsForUser(Long userId) {
        return stepRepository.countPendingStepsForUser(userId);
    }

    /**
     * Get steps that need assignment for a specific role
     */
    public List<WorkflowStepCompletion> getUnassignedStepsForRole(String roleName) {
        return stepRepository.findUnassignedStepsForRole(roleName);
    }

    /**
     * Mark step as in progress (when assigned user starts working on it)
     */
    public void markStepInProgress(WorkflowStepCompletion step) {
        step.setAssignmentStatus(AssignmentStatus.IN_PROGRESS);
        stepRepository.save(step);
    }

    /**
     * Complete step and mark assignment as completed
     */
    public void completeStepWithAssignment(WorkflowStepCompletion step, User completedBy) {
        step.setCompleted(true);
        step.setCompletedBy(completedBy.getUsername());
        step.setCompletedById(completedBy.getId());
        step.setCompletedAt(LocalDateTime.now());
        step.setAssignmentStatus(AssignmentStatus.COMPLETED);
        stepRepository.save(step);
    }

    /**
     * Mark email notification as sent for a step
     */
    public void markEmailSent(WorkflowStepCompletion step) {
        step.markEmailSent();
        stepRepository.save(step);
    }

    /**
     * Get the target role for a specific step number
     */
    public UserRole getTargetRoleForStep(int stepNumber) {
        return switch (stepNumber) {
            case 1 -> UserRole.PROJECTS;           // Site Survey -> Project Manager
            case 2 -> UserRole.PRESALES;           // Selection & Design -> Presales
            case 3 -> UserRole.FINANCE;            // Bank Guarantee -> Finance
            case 4 -> UserRole.STORAGE;            // Missing Items -> Storage
            case 5 -> null;                        // Tender Acceptance -> Self
            case 6 -> UserRole.PROJECTS;           // Project Execution -> Projects
            case 7 -> UserRole.QUALITY_ASSURANCE;  // After Sales -> QA
            case 8 -> UserRole.MASTER;             // Completion -> Master
            default -> null;
        };
    }

    /**
     * Get step name for display
     */
    public String getStepDisplayName(int stepNumber) {
        return switch (stepNumber) {
            case 1 -> "Site Survey";
            case 2 -> "Selection & Design";
            case 3 -> "Bank Guarantee";
            case 4 -> "Missing Item Request";
            case 5 -> "Tender Acceptance";
            case 6 -> "Project Execution";
            case 7 -> "After-Sales Support";
            case 8 -> "Completion";
            default -> "Unknown Step";
        };
    }

    /**
     * Check if a step requires user assignment
     */
    public boolean stepRequiresAssignment(int stepNumber) {
        // Step 5 (Tender Acceptance) is self-handled by Sales, no assignment needed
        return stepNumber != 5;
    }
}
