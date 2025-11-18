package com.magictech.modules.sales.repository;

import com.magictech.modules.sales.entity.ProjectCostBreakdown;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for ProjectCostBreakdown entity
 */
@Repository
public interface ProjectCostBreakdownRepository extends JpaRepository<ProjectCostBreakdown, Long> {

    /**
     * Find cost breakdown by project ID
     */
    Optional<ProjectCostBreakdown> findByProjectId(Long projectId);

    /**
     * Check if cost breakdown exists for project
     */
    boolean existsByProjectId(Long projectId);

    /**
     * Delete cost breakdown by project ID
     */
    void deleteByProjectId(Long projectId);
}
