package com.magictech.modules.projects.service;

import com.magictech.core.messaging.service.NotificationService;
import com.magictech.modules.projects.entity.ProjectElement;
import com.magictech.modules.projects.repository.ProjectElementRepository;
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
     */
    @Transactional
    public ProjectElement approveElement(Long id, String approvedBy) {
        ProjectElement element = elementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Element not found: " + id));

        if (!"PENDING_APPROVAL".equals(element.getStatus())) {
            throw new IllegalStateException("Element is not pending approval");
        }

        element.setStatus("APPROVED");
        element.setAllocatedDate(LocalDateTime.now());

        logger.info("Project element {} approved by {}", id, approvedBy);

        return elementRepository.save(element);
    }

    /**
     * Reject a pending project element (called by Sales module).
     */
    @Transactional
    public ProjectElement rejectElement(Long id, String rejectedBy, String reason) {
        ProjectElement element = elementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Element not found: " + id));

        if (!"PENDING_APPROVAL".equals(element.getStatus())) {
            throw new IllegalStateException("Element is not pending approval");
        }

        element.setStatus("REJECTED");
        if (reason != null && !reason.isEmpty()) {
            element.setNotes(element.getNotes() != null ?
                element.getNotes() + "\n\nRejection reason: " + reason : "Rejection reason: " + reason);
        }

        logger.info("Project element {} rejected by {}: {}", id, rejectedBy, reason);

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










