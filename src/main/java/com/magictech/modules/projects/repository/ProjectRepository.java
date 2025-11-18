package com.magictech.modules.projects.repository;

import com.magictech.modules.projects.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Project Repository - Database Access Layer
 * Provides CRUD operations and custom queries for Project entity
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    /**
     * Find all active projects
     */
    List<Project> findByActiveTrue();

    /**
     * Count active projects
     */
    long countByActiveTrue();

    /**
     * Find projects by status
     */
    List<Project> findByStatusAndActiveTrue(String status);

    /**
     * Find projects by creator
     */
    List<Project> findByCreatedByAndActiveTrue(String createdBy);

    /**
     * Search projects across multiple fields
     */
    @Query("SELECT p FROM Project p WHERE p.active = true AND " +
            "(LOWER(p.projectName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.projectLocation) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.status) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Project> searchProjects(@Param("searchTerm") String searchTerm);

    /**
     * Find projects by name pattern
     */
    List<Project> findByProjectNameContainingIgnoreCaseAndActiveTrue(String projectName);

    /**
     * Find projects by location
     */
    List<Project> findByProjectLocationContainingIgnoreCaseAndActiveTrue(String location);
}