package com.magictech.modules.sales.repository;

import com.magictech.modules.sales.entity.WorkflowStepCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowStepCompletionRepository extends JpaRepository<WorkflowStepCompletion, Long> {

    /**
     * Optimized query to fetch all steps for a workflow, ordered by step number
     * Uses query hint for cacheability to reduce database load
     */
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    @Query("SELECT s FROM WorkflowStepCompletion s WHERE s.workflowId = :workflowId AND s.active = true ORDER BY s.stepNumber ASC")
    List<WorkflowStepCompletion> findByWorkflowIdOptimized(@Param("workflowId") Long workflowId);

    List<WorkflowStepCompletion> findByWorkflowIdAndActiveTrue(Long workflowId);

    List<WorkflowStepCompletion> findByProjectIdAndActiveTrue(Long projectId);

    Optional<WorkflowStepCompletion> findByWorkflowIdAndStepNumberAndActiveTrue(Long workflowId, Integer stepNumber);

    List<WorkflowStepCompletion> findByWorkflowIdAndCompletedAndActiveTrue(Long workflowId, Boolean completed);

    List<WorkflowStepCompletion> findByWorkflowIdAndNeedsExternalActionAndActiveTrue(Long workflowId, Boolean needsExternalAction);

    List<WorkflowStepCompletion> findByExternalModuleAndExternalActionCompletedAndActiveTrue(String externalModule, Boolean completed);

    List<WorkflowStepCompletion> findByIsDelayedAndActiveTrue(Boolean isDelayed);
}
