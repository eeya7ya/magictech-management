package com.magictech.modules.sales.service;

import com.magictech.core.auth.User;
import com.magictech.modules.sales.entity.ProjectWorkflow;
import com.magictech.modules.sales.entity.WorkflowStepCompletion;
import com.magictech.modules.sales.repository.WorkflowStepCompletionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing individual workflow step completions
 */
@Service
@Transactional
public class WorkflowStepService {

    @Autowired
    private WorkflowStepCompletionRepository stepRepository;

    /**
     * Create all 8 step completion records for a new workflow
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

        for (int i = 1; i <= 8; i++) {
            WorkflowStepCompletion step = new WorkflowStepCompletion();
            step.setWorkflowId(workflow.getId());
            step.setProjectId(workflow.getProjectId());
            step.setStepNumber(i);
            step.setStepName(stepNames[i - 1]);
            step.setCompleted(false);

            stepRepository.save(step);
        }
    }

    /**
     * Get step by workflow and step number
     */
    public Optional<WorkflowStepCompletion> getStep(Long workflowId, Integer stepNumber) {
        return stepRepository.findByWorkflowIdAndStepNumberAndActiveTrue(workflowId, stepNumber);
    }

    /**
     * Get all steps for a workflow
     */
    public List<WorkflowStepCompletion> getAllSteps(Long workflowId) {
        return stepRepository.findByWorkflowIdAndActiveTrue(workflowId);
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
}
