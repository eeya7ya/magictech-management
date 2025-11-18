package com.magictech.modules.projects.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Project Task Entity
 * Manages checklist tasks for each project
 * ✅ UPDATED: Now includes link to schedule items
 */
@Entity
@Table(name = "project_tasks")
public class ProjectTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "task_title", nullable = false, length = 300)
    private String taskTitle;

    @Column(name = "task_details", columnDefinition = "TEXT")
    private String taskDetails;

    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted = false;

    @Column(name = "priority", length = 20)
    private String priority; // "Low", "Medium", "High", "Critical"

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "completed_by", length = 100)
    private String completedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    // ✅ NEW FIELD: Link to schedule task
    @Column(name = "schedule_task_name", length = 200)
    private String scheduleTaskName;

    @Column(nullable = false)
    private Boolean active = true;

    // Constructors
    public ProjectTask() {
        this.createdAt = LocalDateTime.now();
        this.active = true;
        this.isCompleted = false;
    }

    public ProjectTask(Project project, String taskTitle) {
        this();
        this.project = project;
        this.taskTitle = taskTitle;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (active == null) active = true;
        if (isCompleted == null) isCompleted = false;
    }

    @PreUpdate
    protected void onUpdate() {
        if (isCompleted && completedAt == null) {
            completedAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public String getTaskTitle() { return taskTitle; }
    public void setTaskTitle(String taskTitle) { this.taskTitle = taskTitle; }

    public String getTaskDetails() { return taskDetails; }
    public void setTaskDetails(String taskDetails) { this.taskDetails = taskDetails; }

    public Boolean getIsCompleted() { return isCompleted; }
    public void setIsCompleted(Boolean isCompleted) { this.isCompleted = isCompleted; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public String getCompletedBy() { return completedBy; }
    public void setCompletedBy(String completedBy) { this.completedBy = completedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    // ✅ NEW: Schedule Task Name getter/setter
    public String getScheduleTaskName() { return scheduleTaskName; }
    public void setScheduleTaskName(String scheduleTaskName) {
        this.scheduleTaskName = scheduleTaskName;
    }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    @Override
    public String toString() {
        return "ProjectTask{" +
                "id=" + id +
                ", taskTitle='" + taskTitle + '\'' +
                ", isCompleted=" + isCompleted +
                ", priority='" + priority + '\'' +
                ", scheduleTaskName='" + scheduleTaskName + '\'' +
                '}';
    }
}