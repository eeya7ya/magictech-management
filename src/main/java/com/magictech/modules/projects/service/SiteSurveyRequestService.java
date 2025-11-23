package com.magictech.modules.projects.service;

import com.magictech.core.messaging.constants.NotificationConstants;
import com.magictech.core.messaging.dto.NotificationMessage;
import com.magictech.core.messaging.service.NotificationService;
import com.magictech.modules.projects.entity.Project;
import com.magictech.modules.projects.entity.SiteSurveyRequest;
import com.magictech.modules.projects.repository.ProjectRepository;
import com.magictech.modules.projects.repository.SiteSurveyRequestRepository;
import com.magictech.modules.sales.entity.SiteSurveyData;
import com.magictech.modules.sales.repository.SiteSurveyDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Site Survey Request Service
 * Handles business logic for site survey requests
 */
@Service
@Transactional
public class SiteSurveyRequestService {

    @Autowired
    private SiteSurveyRequestRepository requestRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private SiteSurveyDataRepository surveyDataRepository;

    @Autowired
    private NotificationService notificationService;

    /**
     * Create a new site survey request
     */
    public SiteSurveyRequest createRequest(Long projectId, String requestedBy, Long requestedById,
                                          String assignedTo, String priority, String notes) {
        // Get project
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));

        // Update project
        project.setSiteSurveyRequested(true);
        project.setSiteSurveyRequestDate(LocalDateTime.now());
        project.setSiteSurveyRequestedBy(requestedBy);
        projectRepository.save(project);

        // Create request
        SiteSurveyRequest request = new SiteSurveyRequest();
        request.setProjectId(projectId);
        request.setProjectName(project.getProjectName());
        request.setRequestedBy(requestedBy);
        request.setRequestedById(requestedById);
        request.setAssignedTo(assignedTo);
        request.setPriority(priority != null ? priority : "MEDIUM");
        request.setNotes(notes);
        request.setStatus("PENDING");

        SiteSurveyRequest savedRequest = requestRepository.save(request);

        // Send notification
        sendSurveyRequestNotification(savedRequest, project);

        System.out.println("✅ Site survey request created for project: " + project.getProjectName());
        return savedRequest;
    }

    /**
     * Get all pending site survey requests
     */
    public List<SiteSurveyRequest> getPendingRequests() {
        return requestRepository.findByStatusAndActiveTrueOrderByRequestDateDesc("PENDING");
    }

    /**
     * Get all site survey requests
     */
    public List<SiteSurveyRequest> getAllRequests() {
        return requestRepository.findByActiveTrue();
    }

    /**
     * Get site survey request by ID
     */
    public Optional<SiteSurveyRequest> getRequestById(Long id) {
        return requestRepository.findById(id);
    }

    /**
     * Get site survey request by project ID
     */
    public Optional<SiteSurveyRequest> getRequestByProjectId(Long projectId) {
        return requestRepository.findByProjectIdAndActiveTrue(projectId);
    }

    /**
     * Get site survey requests assigned to a user
     */
    public List<SiteSurveyRequest> getRequestsAssignedTo(String assignedTo) {
        return requestRepository.findByAssignedToAndActiveTrueOrderByRequestDateDesc(assignedTo);
    }

    /**
     * Update site survey request
     */
    public SiteSurveyRequest updateRequest(Long id, String assignedTo, String priority,
                                          String status, String notes) {
        SiteSurveyRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Site survey request not found with id: " + id));

        if (assignedTo != null) request.setAssignedTo(assignedTo);
        if (priority != null) request.setPriority(priority);
        if (status != null) request.setStatus(status);
        if (notes != null) request.setNotes(notes);

        return requestRepository.save(request);
    }

    /**
     * Complete site survey request (when survey data is uploaded)
     */
    public SiteSurveyRequest completeRequest(Long requestId, Long surveyDataId,
                                            String completedBy, Long completedById) {
        SiteSurveyRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Site survey request not found with id: " + requestId));

        request.setStatus("COMPLETED");
        request.setSurveyDataId(surveyDataId);
        request.setCompletionDate(LocalDateTime.now());
        request.setCompletedBy(completedBy);
        request.setCompletedById(completedById);

        SiteSurveyRequest savedRequest = requestRepository.save(request);

        // Send completion notification
        sendSurveyCompletionNotification(savedRequest);

        System.out.println("✅ Site survey request completed for project: " + request.getProjectName());
        return savedRequest;
    }

    /**
     * Cancel site survey request
     */
    public SiteSurveyRequest cancelRequest(Long id, String reason) {
        SiteSurveyRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Site survey request not found with id: " + id));

        request.setStatus("CANCELLED");
        request.setNotes(request.getNotes() + "\nCancellation reason: " + reason);

        // Update project
        Project project = projectRepository.findById(request.getProjectId()).orElse(null);
        if (project != null) {
            project.setSiteSurveyRequested(false);
            projectRepository.save(project);
        }

        return requestRepository.save(request);
    }

    /**
     * Delete site survey request
     */
    public void deleteRequest(Long id) {
        SiteSurveyRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Site survey request not found with id: " + id));

        request.setActive(false);
        requestRepository.save(request);
    }

    /**
     * Check if project has active site survey request
     */
    public boolean hasActiveRequest(Long projectId) {
        Optional<SiteSurveyRequest> request = requestRepository.findByProjectIdAndActiveTrue(projectId);
        return request.isPresent() && "PENDING".equals(request.get().getStatus());
    }

    /**
     * Get count of pending requests
     */
    public long getPendingRequestCount() {
        return requestRepository.countByStatusAndActiveTrue("PENDING");
    }

    /**
     * Search site survey requests
     */
    public List<SiteSurveyRequest> searchRequests(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllRequests();
        }
        return requestRepository.searchRequests(searchTerm.trim());
    }

    /**
     * Get survey data for a request
     */
    public Optional<SiteSurveyData> getSurveyDataForRequest(Long requestId) {
        SiteSurveyRequest request = requestRepository.findById(requestId).orElse(null);
        if (request != null && request.getSurveyDataId() != null) {
            return surveyDataRepository.findById(request.getSurveyDataId());
        }
        return Optional.empty();
    }

    // Private helper methods

    private void sendSurveyRequestNotification(SiteSurveyRequest request, Project project) {
        try {
            NotificationMessage message = new NotificationMessage.Builder()
                .type(NotificationConstants.TYPE_INFO)
                .module(NotificationConstants.MODULE_PROJECTS)
                .action("SITE_SURVEY_REQUESTED")
                .entityType("SITE_SURVEY_REQUEST")
                .entityId(request.getId())
                .title("Site Survey Requested")
                .message(String.format("Site survey requested for project '%s' by %s",
                        project.getProjectName(), request.getRequestedBy()))
                .targetModule(NotificationConstants.MODULE_SALES)  // Notify sales team
                .priority(request.getPriority().equals("URGENT") ?
                         NotificationConstants.PRIORITY_HIGH : NotificationConstants.PRIORITY_MEDIUM)
                .createdBy(request.getRequestedBy())
                .excludeSender(true)
                .build();

            notificationService.publishNotification(message);
            System.out.println("✅ Site survey request notification sent to SALES module");
        } catch (Exception e) {
            System.err.println("❌ Failed to send site survey request notification: " + e.getMessage());
        }
    }

    private void sendSurveyCompletionNotification(SiteSurveyRequest request) {
        try {
            NotificationMessage message = new NotificationMessage.Builder()
                .type(NotificationConstants.TYPE_SUCCESS)
                .module(NotificationConstants.MODULE_PROJECTS)
                .action("SITE_SURVEY_COMPLETED")
                .entityType("SITE_SURVEY_REQUEST")
                .entityId(request.getId())
                .title("Site Survey Completed")
                .message(String.format("Site survey completed for project '%s' by %s",
                        request.getProjectName(), request.getCompletedBy()))
                .targetModule(NotificationConstants.MODULE_PROJECTS)  // Notify project team
                .priority(NotificationConstants.PRIORITY_MEDIUM)
                .createdBy(request.getCompletedBy())
                .excludeSender(false)
                .build();

            notificationService.publishNotification(message);
            System.out.println("✅ Site survey completion notification sent to PROJECTS module");
        } catch (Exception e) {
            System.err.println("❌ Failed to send site survey completion notification: " + e.getMessage());
        }
    }
}
