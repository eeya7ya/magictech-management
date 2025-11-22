package com.magictech.modules.sales.repository;

import com.magictech.modules.sales.entity.WorkflowStepCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowStepCompletionRepository extends JpaRepository<WorkflowStepCompletion, Long> {

    List<WorkflowStepCompletion> findByWorkflowIdAndActiveTrue(Long workflowId);

    List<WorkflowStepCompletion> findByProjectIdAndActiveTrue(Long projectId);

    Optional<WorkflowStepCompletion> findByWorkflowIdAndStepNumberAndActiveTrue(Long workflowId, Integer stepNumber);

    List<WorkflowStepCompletion> findByWorkflowIdAndCompletedAndActiveTrue(Long workflowId, Boolean completed);

    List<WorkflowStepCompletion> findByWorkflowIdAndNeedsExternalActionAndActiveTrue(Long workflowId, Boolean needsExternalAction);

    List<WorkflowStepCompletion> findByExternalModuleAndExternalActionCompletedAndActiveTrue(String externalModule, Boolean completed);

    List<WorkflowStepCompletion> findByIsDelayedAndActiveTrue(Boolean isDelayed);
}
