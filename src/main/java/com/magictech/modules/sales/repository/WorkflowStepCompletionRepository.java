package com.magictech.modules.sales.repository;

import com.magictech.modules.sales.entity.WorkflowStepCompletion;
import com.magictech.modules.sales.entity.WorkflowStepCompletion.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowStepCompletionRepository extends JpaRepository<WorkflowStepCompletion, Long> {

    /**
     * Optimized query to fetch all steps for a workflow in a single batch query, ordered by step number
     * This eliminates N+1 query problem when loading all workflow steps
     */
    @Query("SELECT s FROM WorkflowStepCompletion s WHERE s.workflowId = :workflowId AND s.active = true ORDER BY s.stepNumber ASC")
    List<WorkflowStepCompletion> findByWorkflowIdOptimized(@Param("workflowId") Long workflowId);

    List<WorkflowStepCompletion> findByWorkflowIdAndActiveTrue(Long workflowId);

    List<WorkflowStepCompletion> findByProjectIdAndActiveTrue(Long projectId);

    Optional<WorkflowStepCompletion> findByWorkflowIdAndStepNumberAndActiveTrue(Long workflowId, Integer stepNumber);

    List<WorkflowStepCompletion> findByWorkflowIdAndCompletedAndActiveTrue(Long workflowId, Boolean completed);

    List<WorkflowStepCompletion> findByWorkflowIdAndNeedsExternalActionAndActiveTrue(Long workflowId, Boolean needsExternalAction);

    List<WorkflowStepCompletion> findByExternalModuleAndExternalActionCompletedAndActiveTrue(String externalModule, Boolean completed);

    List<WorkflowStepCompletion> findByIsDelayedAndActiveTrue(Boolean isDelayed);

    // ============================================================
    // ASSIGNMENT-RELATED QUERIES
    // ============================================================

    /**
     * Find all steps assigned to a specific user
     */
    List<WorkflowStepCompletion> findByAssignedUserIdAndActiveTrue(Long assignedUserId);

    /**
     * Find all steps assigned to a user with specific status
     */
    List<WorkflowStepCompletion> findByAssignedUserIdAndAssignmentStatusAndActiveTrue(
            Long assignedUserId, AssignmentStatus status);

    /**
     * Find all pending steps assigned to a user (not yet completed)
     */
    @Query("SELECT s FROM WorkflowStepCompletion s WHERE s.assignedUserId = :userId " +
           "AND s.completed = false AND s.active = true ORDER BY s.assignedAt DESC")
    List<WorkflowStepCompletion> findPendingStepsForUser(@Param("userId") Long userId);

    /**
     * Find all steps that need assignment (for a specific workflow)
     */
    List<WorkflowStepCompletion> findByWorkflowIdAndAssignmentStatusAndActiveTrue(
            Long workflowId, AssignmentStatus status);

    /**
     * Find steps by target role (for finding available work for a role)
     */
    List<WorkflowStepCompletion> findByTargetRoleAndAssignmentStatusAndActiveTrue(
            String targetRole, AssignmentStatus status);

    /**
     * Get all workflow IDs where a user is assigned to at least one step
     */
    @Query("SELECT DISTINCT s.workflowId FROM WorkflowStepCompletion s WHERE s.assignedUserId = :userId AND s.active = true")
    List<Long> findWorkflowIdsAssignedToUser(@Param("userId") Long userId);

    /**
     * Get all project IDs where a user is assigned to at least one step
     */
    @Query("SELECT DISTINCT s.projectId FROM WorkflowStepCompletion s WHERE s.assignedUserId = :userId AND s.active = true")
    List<Long> findProjectIdsAssignedToUser(@Param("userId") Long userId);

    /**
     * Count pending steps for a user
     */
    @Query("SELECT COUNT(s) FROM WorkflowStepCompletion s WHERE s.assignedUserId = :userId " +
           "AND s.completed = false AND s.active = true")
    long countPendingStepsForUser(@Param("userId") Long userId);

    /**
     * Find steps by target role that are pending assignment
     */
    @Query("SELECT s FROM WorkflowStepCompletion s WHERE s.targetRole = :role " +
           "AND s.assignmentStatus = 'PENDING_ASSIGNMENT' AND s.active = true")
    List<WorkflowStepCompletion> findUnassignedStepsForRole(@Param("role") String role);
}
