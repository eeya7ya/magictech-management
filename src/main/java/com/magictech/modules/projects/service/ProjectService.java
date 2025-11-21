package com.magictech.modules.projects.service;

import com.magictech.core.messaging.constants.NotificationConstants;
import com.magictech.core.messaging.dto.NotificationMessage;
import com.magictech.core.messaging.service.NotificationService;
import com.magictech.modules.projects.entity.Project;
import com.magictech.modules.projects.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Project Service - Business Logic Layer
 * Handles all project-related operations
 */
@Service
@Transactional
public class ProjectService {

    @Autowired
    private ProjectRepository repository;

    @Autowired
    private NotificationService notificationService;

    /**
     * Get all active projects
     */
    public List<Project> getAllProjects() {
        List<Project> projects = repository.findByActiveTrue();
        System.out.println("📊 Loading projects: Found " + projects.size() + " active projects");
        return projects;
    }

    /**
     * Get all projects including deleted
     */
    public List<Project> getAllProjectsIncludingDeleted() {
        return repository.findAll();
    }

    /**
     * Get project by ID
     */
    public Optional<Project> getProjectById(Long id) {
        return repository.findById(id);
    }

    /**
     * Create new project
     */
    public Project createProject(Project project) {
        if (project.getDateAdded() == null) {
            project.setDateAdded(LocalDateTime.now());
        }
        project.setActive(true);
        Project savedProject = repository.save(project);

        // Publish notification about new project creation (exclude sender to avoid echo)
        try {
            NotificationMessage message = new NotificationMessage.Builder()
                .type(NotificationConstants.TYPE_SUCCESS)
                .module(NotificationConstants.MODULE_PROJECTS)
                .action(NotificationConstants.ACTION_CREATED)
                .entityType(NotificationConstants.ENTITY_PROJECT)
                .entityId(savedProject.getId())
                .title("New Project Created")
                .message(String.format("Project '%s' has been created successfully", savedProject.getProjectName()))
                .priority(NotificationConstants.PRIORITY_HIGH)
                .createdBy(savedProject.getCreatedBy() != null ? savedProject.getCreatedBy() : "Unknown")
                .excludeSender(true)  // Don't send back to creator - they already have instant feedback
                .build();

            notificationService.publishNotification(message);
            System.out.println("✅ Published notification for new project (excluded sender): " + savedProject.getProjectName());
        } catch (Exception e) {
            System.err.println("❌ Failed to publish project creation notification: " + e.getMessage());
            e.printStackTrace();
        }

        return savedProject;
    }

    /**
     * Update existing project
     */
    public Project updateProject(Long id, Project updatedProject) {
        Optional<Project> existingProject = repository.findById(id);

        if (existingProject.isPresent()) {
            Project project = existingProject.get();
            String oldStatus = project.getStatus();

            project.setProjectName(updatedProject.getProjectName());
            project.setProjectLocation(updatedProject.getProjectLocation());
            project.setDateOfIssue(updatedProject.getDateOfIssue());
            project.setDateOfCompletion(updatedProject.getDateOfCompletion());
            project.setStatus(updatedProject.getStatus());
            project.setNotes(updatedProject.getNotes());
            project.setLastUpdated(LocalDateTime.now());

            Project saved = repository.save(project);

            // Publish notification about project update (exclude sender to avoid echo)
            try {
                NotificationMessage message = new NotificationMessage.Builder()
                    .type(NotificationConstants.TYPE_INFO)
                    .module(NotificationConstants.MODULE_PROJECTS)
                    .action(NotificationConstants.ACTION_UPDATED)
                    .entityType(NotificationConstants.ENTITY_PROJECT)
                    .entityId(saved.getId())
                    .title("Project Updated")
                    .message(String.format("Project '%s' has been updated", saved.getProjectName()))
                    .priority(NotificationConstants.PRIORITY_MEDIUM)
                    .createdBy(saved.getCreatedBy() != null ? saved.getCreatedBy() : "Unknown")
                    .excludeSender(true)  // Don't send back to updater - they already have instant feedback
                    .build();

                notificationService.publishNotification(message);
                System.out.println("✅ Published notification for updated project (excluded sender): " + saved.getProjectName());
            } catch (Exception e) {
                System.err.println("❌ Failed to publish project update notification: " + e.getMessage());
                e.printStackTrace();
            }

            return saved;
        }

        throw new RuntimeException("Project not found with id: " + id);
    }

    /**
     * Delete single project - PERMANENT DELETE
     */
    @Transactional
    public void deleteProject(Long id) {
        System.out.println("🗑️ PERMANENT DELETE - Removing project ID: " + id);
        repository.deleteById(id);
        repository.flush();
        System.out.println("✓ Project ID " + id + " permanently deleted from database");
    }

    /**
     * Delete multiple projects - PERMANENT DELETE
     */
    @Transactional
    public void deleteProjects(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            System.out.println("⚠️ No IDs provided for deletion");
            return;
        }

        System.out.println("🗑️ PERMANENT DELETE - Removing " + ids.size() + " projects: " + ids);
        repository.deleteAllById(ids);
        repository.flush();
        System.out.println("✓ Successfully PERMANENTLY deleted " + ids.size() + " projects from database");
    }

    /**
     * Search projects across multiple fields
     */
    public List<Project> searchProjects(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllProjects();
        }
        return repository.searchProjects(searchTerm.trim());
    }

    /**
     * Get projects by status
     */
    public List<Project> getProjectsByStatus(String status) {
        return repository.findByStatusAndActiveTrue(status);
    }

    /**
     * Get projects by creator
     */
    public List<Project> getProjectsByUser(String username) {
        return repository.findByCreatedByAndActiveTrue(username);
    }

    /**
     * Get total count of active projects
     */
    public long getTotalProjectCount() {
        return repository.countByActiveTrue();
    }

    /**
     * Check if project exists
     */
    public boolean projectExists(Long id) {
        return repository.existsById(id);
    }

    /**
     * Bulk create projects
     */
    @Transactional
    public List<Project> createBulkProjects(List<Project> projects) {
        projects.forEach(project -> {
            if (project.getDateAdded() == null) {
                project.setDateAdded(LocalDateTime.now());
            }
            project.setActive(true);
        });
        List<Project> savedProjects = repository.saveAll(projects);

        return savedProjects;
    }

    /**
     * Get all project statuses (distinct)
     */
    public List<String> getAllStatuses() {
        return repository.findByActiveTrue().stream()
                .map(Project::getStatus)
                .filter(s -> s != null && !s.isEmpty())
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * Request confirmation from Sales module for additional elements.
     * Sends a notification to Sales module.
     */
    public Project requestConfirmation(Long projectId, String requestedBy, String reason) {
        Project project = repository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));

        project.setLastUpdated(LocalDateTime.now());
        Project savedProject = repository.save(project);

        // Send notification to Sales module
        try {
            notificationService.notifyConfirmationRequested(
                projectId,
                project.getProjectName(),
                requestedBy
            );
            System.out.println("✉️ Confirmation request sent to Sales for project: " + project.getProjectName());
        } catch (Exception e) {
            System.err.println("Failed to send confirmation request notification: " + e.getMessage());
        }

        return savedProject;
    }

    /**
     * Mark project as completed and notify Storage and Pricing modules.
     * Sends notifications to both Storage and Pricing modules for analysis.
     */
    public Project completeProject(Long projectId, String completedBy) {
        Project project = repository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));

        // Update project status to COMPLETED
        project.setStatus("COMPLETED");
        project.setDateOfCompletion(java.time.LocalDate.now());
        project.setLastUpdated(LocalDateTime.now());
        Project savedProject = repository.save(project);

        // Send notifications to Storage and Pricing modules
        try {
            notificationService.notifyProjectCompleted(
                projectId,
                project.getProjectName(),
                completedBy
            );
            System.out.println("✉️ Project completion notifications sent to Storage and Pricing modules for: " + project.getProjectName());
        } catch (Exception e) {
            System.err.println("Failed to send project completion notifications: " + e.getMessage());
        }

        return savedProject;
    }
}