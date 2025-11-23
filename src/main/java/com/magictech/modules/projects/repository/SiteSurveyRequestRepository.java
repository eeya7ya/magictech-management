package com.magictech.modules.projects.repository;

import com.magictech.modules.projects.entity.SiteSurveyRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Site Survey Request operations
 */
@Repository
public interface SiteSurveyRequestRepository extends JpaRepository<SiteSurveyRequest, Long> {

    /**
     * Find all active site survey requests
     */
    List<SiteSurveyRequest> findByActiveTrue();

    /**
     * Find site survey request by project ID
     */
    Optional<SiteSurveyRequest> findByProjectIdAndActiveTrue(Long projectId);

    /**
     * Find all site survey requests by project ID (including completed/cancelled)
     */
    List<SiteSurveyRequest> findByProjectId(Long projectId);

    /**
     * Find site survey requests by status
     */
    List<SiteSurveyRequest> findByStatusAndActiveTrue(String status);

    /**
     * Find pending site survey requests
     */
    List<SiteSurveyRequest> findByStatusAndActiveTrueOrderByRequestDateDesc(String status);

    /**
     * Find site survey requests assigned to a user
     */
    List<SiteSurveyRequest> findByAssignedToAndActiveTrueOrderByRequestDateDesc(String assignedTo);

    /**
     * Find site survey requests by priority
     */
    List<SiteSurveyRequest> findByPriorityAndActiveTrueOrderByRequestDateDesc(String priority);

    /**
     * Find site survey requests requested by a user
     */
    List<SiteSurveyRequest> findByRequestedByAndActiveTrueOrderByRequestDateDesc(String requestedBy);

    /**
     * Count active site survey requests
     */
    long countByActiveTrue();

    /**
     * Count pending site survey requests
     */
    long countByStatusAndActiveTrue(String status);

    /**
     * Search site survey requests
     */
    @Query("SELECT s FROM SiteSurveyRequest s WHERE s.active = true AND " +
           "(LOWER(s.projectName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(s.requestedBy) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(s.assignedTo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(s.status) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<SiteSurveyRequest> searchRequests(@Param("searchTerm") String searchTerm);
}
