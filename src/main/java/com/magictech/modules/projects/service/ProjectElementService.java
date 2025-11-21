package com.magictech.modules.projects.service;

import com.magictech.core.messaging.service.NotificationService;
import com.magictech.modules.projects.entity.ProjectElement;
import com.magictech.modules.projects.repository.ProjectElementRepository;
import com.magictech.modules.storage.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProjectElementService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectElementService.class);

    @Autowired
    private ProjectElementRepository elementRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private StorageService storageService;

    /**
     * Create element from Projects module - REQUIRES APPROVAL from Sales
     */
    @Transactional
    public ProjectElement createElement(ProjectElement element) {
        // Set status to pending approval
        if (element.getStatus() == null || element.getStatus().isEmpty()) {
            element.setStatus("PENDING_APPROVAL");
        }

        // Save the element
        ProjectElement savedElement = elementRepository.save(element);

        // Send approval notification to Sales module
        try {
            String projectName = element.getProject() != null ? element.getProject().getProjectName() : "Unknown Project";
            String itemName = element.getStorageItem() != null ? element.getStorageItem().getProductName() : "Unknown Item";

            notificationService.notifyElementApprovalRequest(
                savedElement.getId(),
                element.getProject().getId(),
                projectName,
                itemName,
                element.getQuantityNeeded(),
                element.getAddedBy()
            );

            logger.info("Sent approval notification for project element: {}", savedElement.getId());

        } catch (Exception e) {
            logger.error("Failed to send approval notification: {}", e.getMessage(), e);
            // Don't fail the entire operation if notification fails
        }

        return savedElement;
    }

    /**
     * Create element from Sales module - INSTANT APPROVAL (no notification needed)
     * Sales module has permission to add elements directly without approval workflow.
     */
    @Transactional
    public ProjectElement createElementDirectly(ProjectElement element) {
        // Set status to APPROVED immediately (Sales has permission)
        element.setStatus("APPROVED");
        element.setAllocatedDate(LocalDateTime.now());
        element.setQuantityAllocated(element.getQuantityNeeded());

        // Save the element
        ProjectElement savedElement = elementRepository.save(element);

        // Deduct quantity from storage immediately
        if (element.getStorageItem() != null && element.getQuantityNeeded() != null && element.getQuantityNeeded() > 0) {
            try {
                storageService.deductQuantity(element.getStorageItem().getId(), element.getQuantityNeeded());
                logger.info("Sales directly added element {} - deducted {} units from storage",
                        savedElement.getId(), element.getQuantityNeeded());
            } catch (Exception e) {
                logger.error("Failed to deduct quantity from storage: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to update storage quantity: " + e.getMessage());
            }
        }

        logger.info("Project element {} created directly by Sales (no approval needed)", savedElement.getId());

        return savedElement;
    }

    @Transactional(readOnly = true)
    public List<ProjectElement> getElementsByProject(Long projectId) {
        return elementRepository.findByProjectIdAndActiveTrue(projectId);
    }

    /**
     * ✅ NEW METHOD - Add this to your service
     */
    @Transactional(readOnly = true)
    public ProjectElement getElementById(Long id) {
        ProjectElement element = elementRepository.findById(id).orElse(null);

        if (element != null && element.getStorageItem() != null) {
            // Force initialization of StorageItem within transaction
            element.getStorageItem().getProductName();
            element.getStorageItem().getQuantity();
            element.getStorageItem().getId();
        }

        return element;
    }

    @Transactional
    public ProjectElement updateElement(Long id, ProjectElement updatedElement) {
        ProjectElement existing = elementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Element not found"));

        existing.setQuantityNeeded(updatedElement.getQuantityNeeded());
        existing.setQuantityAllocated(updatedElement.getQuantityAllocated());
        existing.setNotes(updatedElement.getNotes());
        existing.setStatus(updatedElement.getStatus());

        return elementRepository.save(existing);
    }

    @Transactional
    public void deleteElement(Long id) {
        ProjectElement element = elementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Element not found"));
        element.setActive(false);
        elementRepository.save(element);
    }

    /**
     * Approve a pending project element (called by Sales module).
     * This will:
     * 1. Set element status to "APPROVED"
     * 2. Keep element in project (active = true)
     * 3. Deduct quantity from storage database
     */
    @Transactional
    public ProjectElement approveElement(Long id, String approvedBy) {
        ProjectElement element = elementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Element not found: " + id));

        // Log current status for debugging
        logger.warn("Attempting to approve element {} with current status: '{}'", id, element.getStatus());

        // Accept both PENDING_APPROVAL and legacy statuses (Allocated, ALLOCATED, Pending, etc.)
        String currentStatus = element.getStatus();
        boolean isPending = currentStatus != null && (
            currentStatus.equals("PENDING_APPROVAL") ||
            currentStatus.equalsIgnoreCase("ALLOCATED") ||
            currentStatus.equalsIgnoreCase("Pending")
        );

        if (!isPending) {
            String errorMsg = String.format("Cannot approve element %d - current status is '%s' (expected PENDING_APPROVAL or similar)",
                id, currentStatus);
            logger.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // Update element status
        element.setStatus("APPROVED");
        element.setAllocatedDate(LocalDateTime.now());
        element.setQuantityAllocated(element.getQuantityNeeded()); // Allocate the full quantity

        // Deduct quantity from storage
        if (element.getStorageItem() != null && element.getQuantityNeeded() != null && element.getQuantityNeeded() > 0) {
            try {
                storageService.deductQuantity(element.getStorageItem().getId(), element.getQuantityNeeded());
                logger.info("Deducted {} units of item {} from storage for project element {}",
                        element.getQuantityNeeded(), element.getStorageItem().getId(), id);
            } catch (Exception e) {
                logger.error("Failed to deduct quantity from storage: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to update storage quantity: " + e.getMessage());
            }
        }

        logger.info("Project element {} approved by {}", id, approvedBy);

        return elementRepository.save(element);
    }

    /**
     * Reject a pending project element (called by Sales module).
     * This will:
     * 1. Remove element from project (soft delete: active = false)
     * 2. Return quantity back to storage database
     * 3. Set status to "REJECTED" for logging
     */
    @Transactional
    public ProjectElement rejectElement(Long id, String rejectedBy, String reason) {
        ProjectElement element = elementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Element not found: " + id));

        // Log current status for debugging
        logger.warn("Attempting to reject element {} with current status: '{}'", id, element.getStatus());

        // Accept both PENDING_APPROVAL and legacy statuses
        String currentStatus = element.getStatus();
        boolean isPending = currentStatus != null && (
            currentStatus.equals("PENDING_APPROVAL") ||
            currentStatus.equalsIgnoreCase("ALLOCATED") ||
            currentStatus.equalsIgnoreCase("Pending")
        );

        if (!isPending) {
            String errorMsg = String.format("Cannot reject element %d - current status is '%s' (expected PENDING_APPROVAL or similar)",
                id, currentStatus);
            logger.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // Update element status
        element.setStatus("REJECTED");
        if (reason != null && !reason.isEmpty()) {
            element.setNotes(element.getNotes() != null ?
                element.getNotes() + "\n\nRejection reason: " + reason : "Rejection reason: " + reason);
        }

        // Return quantity back to storage
        if (element.getStorageItem() != null && element.getQuantityNeeded() != null && element.getQuantityNeeded() > 0) {
            try {
                storageService.addQuantity(element.getStorageItem().getId(), element.getQuantityNeeded());
                logger.info("Returned {} units of item {} to storage from rejected project element {}",
                        element.getQuantityNeeded(), element.getStorageItem().getId(), id);
            } catch (Exception e) {
                logger.error("Failed to return quantity to storage: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to update storage quantity: " + e.getMessage());
            }
        }

        // Remove element from project (soft delete)
        element.setActive(false);

        logger.info("Project element {} rejected by {}: {} and removed from project", id, rejectedBy, reason);

        return elementRepository.save(element);
    }

    /**
     * Alias method for getElementsByProject - for compatibility
     */
    @Transactional(readOnly = true)
    public List<ProjectElement> getProjectElements(Long projectId) {
        return getElementsByProject(projectId);
    }

    /**
     * Get count of elements for a project
     */
    @Transactional(readOnly = true)
    public long getElementCount(Long projectId) {
        return elementRepository.countByProjectIdAndActiveTrue(projectId);
    }
}










