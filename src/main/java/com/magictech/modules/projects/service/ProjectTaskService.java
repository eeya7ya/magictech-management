package com.magictech.modules.projects.service;

import com.magictech.modules.projects.entity.ProjectTask;
import com.magictech.modules.projects.repository.ProjectTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ProjectTaskService {

    @Autowired
    private ProjectTaskRepository repository;

    public List<ProjectTask> getTasksByProject(Long projectId) {
        return repository.findByProjectIdAndActiveTrue(projectId);
    }

    public ProjectTask createTask(ProjectTask task) {
        return repository.save(task);
    }

    public ProjectTask updateTask(Long id, ProjectTask updated) {
        ProjectTask existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        existing.setTaskTitle(updated.getTaskTitle());
        existing.setTaskDetails(updated.getTaskDetails());
        existing.setIsCompleted(updated.getIsCompleted());
        existing.setPriority(updated.getPriority());
        existing.setDueDate(updated.getDueDate());

        if (updated.getIsCompleted() && existing.getCompletedAt() == null) {
            existing.setCompletedAt(LocalDateTime.now());
        }

        return repository.save(existing);
    }

    public void toggleTaskCompletion(Long id, String username) {
        ProjectTask task = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        task.setIsCompleted(!task.getIsCompleted());

        if (task.getIsCompleted()) {
            task.setCompletedAt(LocalDateTime.now());
            task.setCompletedBy(username);
        } else {
            task.setCompletedAt(null);
            task.setCompletedBy(null);
        }

        repository.save(task);
    }

    public void deleteTask(Long id) {
        repository.deleteById(id);
    }

    public long getCompletedCount(Long projectId) {
        return repository.countByProjectIdAndIsCompletedAndActiveTrue(projectId, true);
    }

    public long getTotalCount(Long projectId) {
        return repository.countByProjectIdAndActiveTrue(projectId);
    }

    /**
     * Alias method for getTasksByProject - for compatibility
     */
    public List<ProjectTask> getProjectTasks(Long projectId) {
        return getTasksByProject(projectId);
    }

    /**
     * Get count of pending tasks for a project
     */
    public long getPendingTaskCount(Long projectId) {
        return repository.countByProjectIdAndIsCompletedAndActiveTrue(projectId, false);
    }

    /**
     * Find all tasks for a project (alias for getTasksByProject)
     * Used by ProjectExecutionWizard
     */
    public List<ProjectTask> findByProjectId(Long projectId) {
        return getTasksByProject(projectId);
    }
}