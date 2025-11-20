package com.magictech.modules.projects.service;

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
    private com.magictech.core.notification.CoreNotificationService notificationService;

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

        // Create notification for new project
        try {
            notificationService.createNotificationWithRelation(
                "PROJECTS",  // targetRole
                "PROJECTS",  // module
                "PROJECT_CREATED",  // type
                "New Project Created",  // title
                String.format("Project '%s' has been created", savedProject.getProjectName()),  // message
                savedProject.getId(),  // relatedId
                "PROJECT",  // relatedType
                "NORMAL",  // priority
                savedProject.getCreatedBy() != null ? savedProject.getCreatedBy() : "System"  // createdBy
            );
            System.out.println("🔔 Notification created for new project: " + savedProject.getProjectName());
        } catch (Exception e) {
            System.err.println("⚠️ Failed to create notification: " + e.getMessage());
            // Don't fail the project creation if notification fails
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

            // If project status changed to "Completed", notify PRICING module
            if (updatedProject.getStatus() != null &&
                updatedProject.getStatus().equalsIgnoreCase("Completed") &&
                !updatedProject.getStatus().equalsIgnoreCase(oldStatus)) {

                notificationService.createNotificationWithRelation(
                    "PRICING",  // targetRole
                    "PRICING",  // module
                    "PROJECT_COMPLETED",  // type
                    "Project Completed",  // title
                    String.format("Project '%s' has been completed and needs pricing finalization", saved.getProjectName()),  // message
                    saved.getId(),  // relatedId
                    "PROJECT",  // relatedType
                    "HIGH",  // priority
                    project.getCreatedBy() != null ? project.getCreatedBy() : "System"  // createdBy
                );

                System.out.println("✓ Pricing notification created for completed project: " + saved.getProjectName());
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

        // Create notifications for all new projects
        try {
            savedProjects.forEach(project -> {
                notificationService.createNotificationWithRelation(
                    "PROJECTS",  // targetRole
                    "PROJECTS",  // module
                    "PROJECT_CREATED",  // type
                    "New Project Created",  // title
                    String.format("Project '%s' has been created", project.getProjectName()),  // message
                    project.getId(),  // relatedId
                    "PROJECT",  // relatedType
                    "NORMAL",  // priority
                    project.getCreatedBy() != null ? project.getCreatedBy() : "System"  // createdBy
                );
            });
            System.out.println("🔔 Notifications created for " + savedProjects.size() + " new projects");
        } catch (Exception e) {
            System.err.println("⚠️ Failed to create notifications: " + e.getMessage());
            // Don't fail the bulk creation if notifications fail
        }

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
}