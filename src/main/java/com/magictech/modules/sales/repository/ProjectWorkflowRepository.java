package com.magictech.modules.sales.repository;

import com.magictech.modules.sales.entity.ProjectWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectWorkflowRepository extends JpaRepository<ProjectWorkflow, Long> {

    Optional<ProjectWorkflow> findByProjectIdAndActiveTrue(Long projectId);

    List<ProjectWorkflow> findByActiveTrue();

    List<ProjectWorkflow> findByCreatedByIdAndActiveTrue(Long createdById);

    List<ProjectWorkflow> findByStatusAndActiveTrue(ProjectWorkflow.WorkflowStatusType status);

    List<ProjectWorkflow> findByCurrentStepAndActiveTrue(Integer currentStep);

    boolean existsByProjectIdAndActiveTrue(Long projectId);
}
