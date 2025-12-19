package com.magictech.modules.sales.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Detailed tracking for each workflow step completion
 * Stores who completed it, when, and additional metadata
 */
@Entity
@Table(name = "workflow_step_completions")
public class WorkflowStepCompletion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workflow_id", nullable = false)
    private Long workflowId;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "step_number", nullable = false)
    private Integer stepNumber; // 1-8

    @Column(name = "step_name", length = 100, nullable = false)
    private String stepName;

    @Column(name = "completed", nullable = false)
    private Boolean completed = false;

    @Column(name = "completed_by", length = 100)
    private String completedBy;

    @Column(name = "completed_by_id")
    private Long completedById;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // For steps requiring external action (pushed to other modules)
    @Column(name = "needs_external_action")
    private Boolean needsExternalAction = false;

    @Column(name = "external_module", length = 50)
    private String externalModule; // PRESALES, FINANCE, PROJECT, QA

    @Column(name = "external_action_completed")
    private Boolean externalActionCompleted = false;

    @Column(name = "external_completed_by", length = 100)
    private String externalCompletedBy;

    @Column(name = "external_completed_at")
    private LocalDateTime externalCompletedAt;

    // For Step 5 - Rejection reason
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // For Step 6 - Time tracking
    @Column(name = "expected_completion_date")
    private LocalDateTime expectedCompletionDate;

    @Column(name = "is_delayed")
    private Boolean isDelayed = false;

    @Column(name = "danger_alarm_sent")
    private Boolean dangerAlarmSent = false;

    // Project execution completion (Step 6 - from Projects team)
    @Column(name = "project_completion_notes", columnDefinition = "TEXT")
    private String projectCompletionNotes;

    @Column(name = "has_issues")
    private Boolean hasIssues = false;

    @Column(name = "external_action_completed_at")
    private LocalDateTime externalActionCompletedAt;

    @Column(name = "external_action_completed_by", length = 100)
    private String externalActionCompletedBy;

    // General notes for this step
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt;

    @Column(nullable = false)
    private Boolean active = true;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.lastUpdatedAt = LocalDateTime.now();
        if (this.active == null) this.active = true;
        if (this.completed == null) this.completed = false;
        if (this.needsExternalAction == null) this.needsExternalAction = false;
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastUpdatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(Long workflowId) {
        this.workflowId = workflowId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Integer getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(Integer stepNumber) {
        this.stepNumber = stepNumber;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
        if (completed && this.completedAt == null) {
            this.completedAt = LocalDateTime.now();
        }
    }

    public String getCompletedBy() {
        return completedBy;
    }

    public void setCompletedBy(String completedBy) {
        this.completedBy = completedBy;
    }

    public Long getCompletedById() {
        return completedById;
    }

    public void setCompletedById(Long completedById) {
        this.completedById = completedById;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Boolean getNeedsExternalAction() {
        return needsExternalAction;
    }

    public void setNeedsExternalAction(Boolean needsExternalAction) {
        this.needsExternalAction = needsExternalAction;
    }

    public String getExternalModule() {
        return externalModule;
    }

    public void setExternalModule(String externalModule) {
        this.externalModule = externalModule;
    }

    public Boolean getExternalActionCompleted() {
        return externalActionCompleted;
    }

    public void setExternalActionCompleted(Boolean externalActionCompleted) {
        this.externalActionCompleted = externalActionCompleted;
        if (externalActionCompleted && this.externalCompletedAt == null) {
            this.externalCompletedAt = LocalDateTime.now();
        }
    }

    public String getExternalCompletedBy() {
        return externalCompletedBy;
    }

    public void setExternalCompletedBy(String externalCompletedBy) {
        this.externalCompletedBy = externalCompletedBy;
    }

    public LocalDateTime getExternalCompletedAt() {
        return externalCompletedAt;
    }

    public void setExternalCompletedAt(LocalDateTime externalCompletedAt) {
        this.externalCompletedAt = externalCompletedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public LocalDateTime getExpectedCompletionDate() {
        return expectedCompletionDate;
    }

    public void setExpectedCompletionDate(LocalDateTime expectedCompletionDate) {
        this.expectedCompletionDate = expectedCompletionDate;
    }

    public Boolean getIsDelayed() {
        return isDelayed;
    }

    public void setIsDelayed(Boolean isDelayed) {
        this.isDelayed = isDelayed;
    }

    public Boolean getDangerAlarmSent() {
        return dangerAlarmSent;
    }

    public void setDangerAlarmSent(Boolean dangerAlarmSent) {
        this.dangerAlarmSent = dangerAlarmSent;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    // Project execution completion getters/setters
    public String getProjectCompletionNotes() {
        return projectCompletionNotes;
    }

    public void setProjectCompletionNotes(String projectCompletionNotes) {
        this.projectCompletionNotes = projectCompletionNotes;
    }

    public Boolean getHasIssues() {
        return hasIssues;
    }

    public void setHasIssues(Boolean hasIssues) {
        this.hasIssues = hasIssues;
    }

    public LocalDateTime getExternalActionCompletedAt() {
        return externalActionCompletedAt;
    }

    public void setExternalActionCompletedAt(LocalDateTime externalActionCompletedAt) {
        this.externalActionCompletedAt = externalActionCompletedAt;
    }

    public String getExternalActionCompletedBy() {
        return externalActionCompletedBy;
    }

    public void setExternalActionCompletedBy(String externalActionCompletedBy) {
        this.externalActionCompletedBy = externalActionCompletedBy;
    }
}
