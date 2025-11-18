package com.magictech.core.approval;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for PendingApproval entity
 */
@Repository
public interface PendingApprovalRepository extends JpaRepository<PendingApproval, Long> {

    /**
     * Find pending approvals for a specific approver role
     */
    List<PendingApproval> findByApproverRoleAndStatusOrderByCreatedAtDesc(String approverRole, String status);

    /**
     * Find pending approvals by project
     */
    List<PendingApproval> findByProjectIdAndStatusOrderByCreatedAtDesc(Long projectId, String status);

    /**
     * Find all pending approvals (status = PENDING)
     */
    List<PendingApproval> findByStatusOrderByCreatedAtDesc(String status);

    /**
     * Find expired approvals (need auto-rejection)
     */
    @Query("SELECT p FROM PendingApproval p WHERE p.status = 'PENDING' AND p.expiresAt < :now")
    List<PendingApproval> findExpiredApprovals(@Param("now") LocalDateTime now);

    /**
     * Count pending approvals for a role
     */
    long countByApproverRoleAndStatus(String approverRole, String status);

    /**
     * Find approvals requested by a specific user
     */
    List<PendingApproval> findByRequestedByOrderByCreatedAtDesc(String requestedBy);

    /**
     * Find approvals by type
     */
    List<PendingApproval> findByTypeAndStatusOrderByCreatedAtDesc(String type, String status);

    /**
     * Find approvals by storage item
     */
    List<PendingApproval> findByStorageItemIdAndStatusOrderByCreatedAtDesc(Long storageItemId, String status);

    /**
     * Find all approvals for a project (any status)
     */
    List<PendingApproval> findByProjectIdOrderByCreatedAtDesc(Long projectId);
}
