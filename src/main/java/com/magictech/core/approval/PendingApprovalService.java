package com.magictech.core.approval;

import com.magictech.core.notification.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing pending approvals
 */
@Service
@Transactional
public class PendingApprovalService {

    @Autowired
    private PendingApprovalRepository approvalRepository;

    @Autowired
    private NotificationService notificationService;

    /**
     * Create a new approval request for project element addition
     */
    public PendingApproval createProjectElementApproval(Long projectId, Long storageItemId,
                                                        Integer quantity, String requestedBy,
                                                        Long requestedByUserId, String notes) {
        PendingApproval approval = new PendingApproval();
        approval.setType("PROJECT_ELEMENT_ADD");
        approval.setProjectId(projectId);
        approval.setStorageItemId(storageItemId);
        approval.setQuantity(quantity);
        approval.setRequestedBy(requestedBy);
        approval.setRequestedByUserId(requestedByUserId);
        approval.setApproverRole("SALES"); // Sales must approve
        approval.setNotes(notes);

        approval = approvalRepository.save(approval);

        // Create notification for sales team
        String message = requestedBy + " wants to add " + quantity + " item(s) to project. " +
                        "Approval required within 2 days or will auto-reject.";

        notificationService.createNotificationWithRelation(
            "SALES",
            "PROJECTS",
            "ELEMENT_APPROVAL_REQUEST",
            "Approval Required: Project Element Addition",
            message,
            approval.getId(),
            "APPROVAL",
            "HIGH",
            requestedBy
        );

        return approval;
    }

    /**
     * Approve a pending request
     */
    public PendingApproval approveRequest(Long approvalId, String approvedBy, String notes) {
        Optional<PendingApproval> approvalOpt = approvalRepository.findById(approvalId);
        if (approvalOpt.isEmpty()) {
            throw new RuntimeException("Approval request not found");
        }

        PendingApproval approval = approvalOpt.get();

        if (!"PENDING".equals(approval.getStatus())) {
            throw new RuntimeException("Approval request already processed: " + approval.getStatus());
        }

        if (approval.isExpired()) {
            approval.timeout();
            approvalRepository.save(approval);
            throw new RuntimeException("Approval request expired");
        }

        approval.approve(approvedBy, notes);
        approvalRepository.save(approval);

        // Notify requester
        String message = "Your request to add items to project has been approved by " + approvedBy;
        notificationService.createNotificationWithRelation(
            null,
            "SALES",
            "ELEMENT_APPROVED",
            "Request Approved",
            message,
            approval.getId(),
            "APPROVAL",
            "NORMAL",
            approvedBy
        );

        // Set user ID for notification
        if (approval.getRequestedByUserId() != null) {
            List<com.magictech.core.notification.Notification> notifications =
                notificationService.getNotificationsByRelatedEntity(approval.getId(), "APPROVAL");
            for (com.magictech.core.notification.Notification n : notifications) {
                if ("ELEMENT_APPROVED".equals(n.getType())) {
                    n.setUserId(approval.getRequestedByUserId());
                }
            }
        }

        return approval;
    }

    /**
     * Reject a pending request
     */
    public PendingApproval rejectRequest(Long approvalId, String rejectedBy, String reason) {
        Optional<PendingApproval> approvalOpt = approvalRepository.findById(approvalId);
        if (approvalOpt.isEmpty()) {
            throw new RuntimeException("Approval request not found");
        }

        PendingApproval approval = approvalOpt.get();

        if (!"PENDING".equals(approval.getStatus())) {
            throw new RuntimeException("Approval request already processed: " + approval.getStatus());
        }

        approval.reject(rejectedBy, reason);
        approvalRepository.save(approval);

        // Notify requester
        String message = "Your request to add items to project has been rejected by " + rejectedBy +
                        (reason != null ? ". Reason: " + reason : "");
        notificationService.createNotificationWithRelation(
            null,
            "SALES",
            "ELEMENT_REJECTED",
            "Request Rejected",
            message,
            approval.getId(),
            "APPROVAL",
            "NORMAL",
            rejectedBy
        );

        // Set user ID for notification
        if (approval.getRequestedByUserId() != null) {
            List<com.magictech.core.notification.Notification> notifications =
                notificationService.getNotificationsByRelatedEntity(approval.getId(), "APPROVAL");
            for (com.magictech.core.notification.Notification n : notifications) {
                if ("ELEMENT_REJECTED".equals(n.getType())) {
                    n.setUserId(approval.getRequestedByUserId());
                }
            }
        }

        return approval;
    }

    /**
     * Process expired approvals (auto-reject)
     */
    public void processExpiredApprovals() {
        List<PendingApproval> expiredApprovals = approvalRepository.findExpiredApprovals(LocalDateTime.now());

        for (PendingApproval approval : expiredApprovals) {
            approval.timeout();

            // Notify requester about timeout
            String message = "Your request to add items to project has been automatically rejected due to timeout (2 days)";
            notificationService.createNotificationWithRelation(
                null,
                "PROJECTS",
                "ELEMENT_TIMEOUT",
                "Request Timeout",
                message,
                approval.getId(),
                "APPROVAL",
                "NORMAL",
                "SYSTEM"
            );

            // Set user ID for notification
            if (approval.getRequestedByUserId() != null) {
                List<com.magictech.core.notification.Notification> notifications =
                    notificationService.getNotificationsByRelatedEntity(approval.getId(), "APPROVAL");
                for (com.magictech.core.notification.Notification n : notifications) {
                    if ("ELEMENT_TIMEOUT".equals(n.getType())) {
                        n.setUserId(approval.getRequestedByUserId());
                    }
                }
            }
        }

        if (!expiredApprovals.isEmpty()) {
            approvalRepository.saveAll(expiredApprovals);
        }
    }

    /**
     * Get pending approvals for a specific role
     */
    public List<PendingApproval> getPendingApprovals(String approverRole) {
        return approvalRepository.findByApproverRoleAndStatusOrderByCreatedAtDesc(approverRole, "PENDING");
    }

    /**
     * Get all approvals (any status)
     */
    public List<PendingApproval> getAllApprovals(String approverRole) {
        List<PendingApproval> all = approvalRepository.findAll();
        return all.stream()
                .filter(a -> approverRole.equals(a.getApproverRole()))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
    }

    /**
     * Get approval by ID
     */
    public Optional<PendingApproval> getApprovalById(Long id) {
        return approvalRepository.findById(id);
    }

    /**
     * Get pending approvals for a project
     */
    public List<PendingApproval> getPendingApprovalsForProject(Long projectId) {
        return approvalRepository.findByProjectIdAndStatusOrderByCreatedAtDesc(projectId, "PENDING");
    }

    /**
     * Count pending approvals for role
     */
    public long countPendingApprovals(String approverRole) {
        return approvalRepository.countByApproverRoleAndStatus(approverRole, "PENDING");
    }

    /**
     * Get approvals requested by user
     */
    public List<PendingApproval> getApprovalsByRequester(String requestedBy) {
        return approvalRepository.findByRequestedByOrderByCreatedAtDesc(requestedBy);
    }

    /**
     * Delete approval
     */
    public void deleteApproval(Long id) {
        approvalRepository.deleteById(id);
    }
}
