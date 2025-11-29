package com.magictech.modules.sales.repository;

import com.magictech.modules.sales.entity.ProjectWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectWorkflowRepository extends JpaRepository<ProjectWorkflow, Long> {

    Optional<ProjectWorkflow> findByProjectIdAndActiveTrue(Long projectId);

    /**
     * Optimized query to fetch workflow by project ID with query hint for cacheable results
     * This reduces database round trips when the same workflow is queried multiple times
     */
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    @Query("SELECT w FROM ProjectWorkflow w WHERE w.projectId = :projectId AND w.active = true")
    Optional<ProjectWorkflow> findByProjectIdOptimized(@Param("projectId") Long projectId);

    List<ProjectWorkflow> findByActiveTrue();

    List<ProjectWorkflow> findByCreatedByIdAndActiveTrue(Long createdById);

    List<ProjectWorkflow> findByStatusAndActiveTrue(ProjectWorkflow.WorkflowStatusType status);

    List<ProjectWorkflow> findByCurrentStepAndActiveTrue(Integer currentStep);

    boolean existsByProjectIdAndActiveTrue(Long projectId);
}
