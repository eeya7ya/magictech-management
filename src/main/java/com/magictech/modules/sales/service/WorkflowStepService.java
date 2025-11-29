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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing individual workflow step completions
 * Implements in-memory caching to prevent N+1 queries and duplicate concurrent queries
 */
@Service
@Transactional
public class WorkflowStepService {

    @Autowired
    private WorkflowStepCompletionRepository stepRepository;

    /**
     * In-memory cache for workflow steps
     * Key: workflowId, Value: Map of stepNumber -> WorkflowStepCompletion
     * Thread-safe to handle concurrent access from UI refresh operations
     */
    private final Map<Long, Map<Integer, WorkflowStepCompletion>> stepsCache = new ConcurrentHashMap<>();

    /**
     * Cache timestamp for invalidation (steps older than 5 seconds are refreshed)
     */
    private final Map<Long, LocalDateTime> cacheTimestamps = new ConcurrentHashMap<>();
    private static final long CACHE_VALIDITY_SECONDS = 5;

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
     * Get step by workflow and step number (with caching)
     * Uses in-memory cache to avoid duplicate database queries
     */
    public Optional<WorkflowStepCompletion> getStep(Long workflowId, Integer stepNumber) {
        // Check if cache is valid
        if (isCacheValid(workflowId)) {
            Map<Integer, WorkflowStepCompletion> workflowSteps = stepsCache.get(workflowId);
            if (workflowSteps != null && workflowSteps.containsKey(stepNumber)) {
                return Optional.of(workflowSteps.get(stepNumber));
            }
        }

        // Cache miss or invalid - load all steps and cache them
        loadAndCacheAllSteps(workflowId);

        // Return the requested step from cache
        Map<Integer, WorkflowStepCompletion> workflowSteps = stepsCache.get(workflowId);
        if (workflowSteps != null && workflowSteps.containsKey(stepNumber)) {
            return Optional.of(workflowSteps.get(stepNumber));
        }

        return Optional.empty();
    }

    /**
     * Get all steps for a workflow (with caching)
     * Returns cached steps if available and valid, otherwise loads from database
     */
    public List<WorkflowStepCompletion> getAllSteps(Long workflowId) {
        // Check if cache is valid
        if (isCacheValid(workflowId)) {
            Map<Integer, WorkflowStepCompletion> workflowSteps = stepsCache.get(workflowId);
            if (workflowSteps != null && !workflowSteps.isEmpty()) {
                return List.copyOf(workflowSteps.values());
            }
        }

        // Cache miss or invalid - load all steps and cache them
        return loadAndCacheAllSteps(workflowId);
    }

    /**
     * Load all steps for a workflow from database and cache them
     * Uses optimized repository method to fetch all steps in one query
     */
    private List<WorkflowStepCompletion> loadAndCacheAllSteps(Long workflowId) {
        // Use optimized query to fetch all steps in one database round trip
        List<WorkflowStepCompletion> steps = stepRepository.findByWorkflowIdOptimized(workflowId);

        // Build step map for O(1) lookup by step number
        Map<Integer, WorkflowStepCompletion> stepMap = new ConcurrentHashMap<>();
        for (WorkflowStepCompletion step : steps) {
            stepMap.put(step.getStepNumber(), step);
        }

        // Update cache
        stepsCache.put(workflowId, stepMap);
        cacheTimestamps.put(workflowId, LocalDateTime.now());

        return steps;
    }

    /**
     * Check if cache is valid (not expired)
     */
    private boolean isCacheValid(Long workflowId) {
        LocalDateTime timestamp = cacheTimestamps.get(workflowId);
        if (timestamp == null) {
            return false;
        }
        return LocalDateTime.now().minusSeconds(CACHE_VALIDITY_SECONDS).isBefore(timestamp);
    }

    /**
     * Invalidate cache for a workflow (called when steps are modified)
     */
    public void invalidateCache(Long workflowId) {
        stepsCache.remove(workflowId);
        cacheTimestamps.remove(workflowId);
    }

    /**
     * Invalidate all caches (useful for testing or bulk operations)
     */
    public void clearAllCaches() {
        stepsCache.clear();
        cacheTimestamps.clear();
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

        // Invalidate cache since step data changed
        invalidateCache(step.getWorkflowId());
    }

    /**
     * Force save step completion (for explicit persistence)
     */
    public void forceStepSave(WorkflowStepCompletion step) {
        stepRepository.save(step);
        stepRepository.flush();

        // Invalidate cache since step data changed
        invalidateCache(step.getWorkflowId());
    }

    /**
     * Mark step as requiring external action
     */
    public void markNeedsExternalAction(WorkflowStepCompletion step, String externalModule) {
        step.setNeedsExternalAction(true);
        step.setExternalModule(externalModule);
        step.setExternalActionCompleted(false);
        stepRepository.save(step);

        // Invalidate cache since step data changed
        invalidateCache(step.getWorkflowId());
    }

    /**
     * Mark external action as completed
     */
    public void completeExternalAction(WorkflowStepCompletion step, User externalUser) {
        step.setExternalActionCompleted(true);
        step.setExternalCompletedBy(externalUser.getUsername());
        step.setExternalCompletedAt(LocalDateTime.now());
        stepRepository.save(step);

        // Invalidate cache since step data changed
        invalidateCache(step.getWorkflowId());
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

        // Invalidate cache since step data changed
        invalidateCache(step.getWorkflowId());
    }

    /**
     * Mark step as delayed (for step 6)
     */
    public void markStepDelayed(WorkflowStepCompletion step, LocalDateTime expectedDate) {
        step.setIsDelayed(true);
        step.setExpectedCompletionDate(expectedDate);
        stepRepository.save(step);

        // Invalidate cache since step data changed
        invalidateCache(step.getWorkflowId());
    }

    /**
     * Mark danger alarm sent (for step 6)
     */
    public void markDangerAlarmSent(WorkflowStepCompletion step) {
        step.setDangerAlarmSent(true);
        stepRepository.save(step);

        // Invalidate cache since step data changed
        invalidateCache(step.getWorkflowId());
    }

    /**
     * Add notes to step
     */
    public void addNotes(WorkflowStepCompletion step, String notes) {
        step.setNotes(notes);
        stepRepository.save(step);

        // Invalidate cache since step data changed
        invalidateCache(step.getWorkflowId());
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
        WorkflowStepCompletion updated = stepRepository.save(step);

        // Invalidate cache since step data changed
        invalidateCache(step.getWorkflowId());

        return updated;
    }

    /**
     * Delete step (soft delete)
     */
    public void deleteStep(WorkflowStepCompletion step) {
        step.setActive(false);
        stepRepository.save(step);

        // Invalidate cache since step data changed
        invalidateCache(step.getWorkflowId());
    }
}
