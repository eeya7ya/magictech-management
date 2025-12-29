package com.magictech.modules.storage.repository;

import com.magictech.modules.storage.entity.AvailabilityRequest;
import com.magictech.modules.storage.entity.AvailabilityRequest.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for AvailabilityRequest entity
 */
@Repository
public interface AvailabilityRequestRepository extends JpaRepository<AvailabilityRequest, Long> {

    // Find all active requests
    List<AvailabilityRequest> findByActiveTrueOrderByCreatedAtDesc();

    // Find pending requests (for Storage module)
    List<AvailabilityRequest> findByStatusAndActiveTrueOrderByCreatedAtDesc(RequestStatus status);

    // Find pending requests - multiple statuses
    @Query("SELECT r FROM AvailabilityRequest r WHERE r.active = true AND r.status IN :statuses ORDER BY r.createdAt DESC")
    List<AvailabilityRequest> findByStatusInAndActiveTrue(@Param("statuses") List<RequestStatus> statuses);

    // Find requests by requester module
    List<AvailabilityRequest> findByRequesterModuleAndActiveTrueOrderByCreatedAtDesc(String requesterModule);

    // Find requests by requester username
    List<AvailabilityRequest> findByRequesterUsernameAndActiveTrueOrderByCreatedAtDesc(String requesterUsername);

    // Find requests for a specific storage item
    List<AvailabilityRequest> findByStorageItemIdAndActiveTrueOrderByCreatedAtDesc(Long storageItemId);

    // Find requests for a specific project
    List<AvailabilityRequest> findByProjectIdAndActiveTrueOrderByCreatedAtDesc(Long projectId);

    // Count pending requests (for badge display)
    @Query("SELECT COUNT(r) FROM AvailabilityRequest r WHERE r.active = true AND r.status = 'PENDING'")
    long countPendingRequests();

    // Count requests by status
    long countByStatusAndActiveTrue(RequestStatus status);
}
