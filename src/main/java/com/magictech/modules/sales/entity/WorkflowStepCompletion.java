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

    // ============================================================
    // ASSIGNMENT FIELDS - Who is assigned to complete this step
    // ============================================================

    @Column(name = "assigned_user_id")
    private Long assignedUserId;

    @Column(name = "assigned_username", length = 100)
    private String assignedUsername;

    @Column(name = "assigned_user_email", length = 150)
    private String assignedUserEmail;

    @Column(name = "assigned_by_user_id")
    private Long assignedByUserId;

    @Column(name = "assigned_by_username", length = 100)
    private String assignedByUsername;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "email_sent_at")
    private LocalDateTime emailSentAt;

    @Column(name = "email_sent")
    private Boolean emailSent = false;

    @Column(name = "assignment_status", length = 30)
    @Enumerated(EnumType.STRING)
    private AssignmentStatus assignmentStatus = AssignmentStatus.PENDING_ASSIGNMENT;

    // Target role for this step (e.g., PROJECTS, PRESALES, FINANCE)
    @Column(name = "target_role", length = 50)
    private String targetRole;

    // ============================================================
    // HOLD FIELDS - For holding project execution after tender acceptance
    // ============================================================

    @Column(name = "is_on_hold")
    private Boolean isOnHold = false;

    @Column(name = "hold_reason", columnDefinition = "TEXT")
    private String holdReason;

    @Column(name = "held_at")
    private LocalDateTime heldAt;

    @Column(name = "held_by_username", length = 100)
    private String heldByUsername;

    @Column(name = "unhold_at")
    private LocalDateTime unholdAt;

    @Column(name = "unhold_by_username", length = 100)
    private String unholdByUsername;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt;

    @Column(nullable = false)
    private Boolean active = true;

    // Assignment Status Enum
    public enum AssignmentStatus {
        PENDING_ASSIGNMENT,  // Not yet assigned to anyone
        ASSIGNED,            // Assigned to a user, waiting for them to start
        IN_PROGRESS,         // User has started working on it
        COMPLETED,           // Step completed
        REJECTED             // Step was rejected
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.lastUpdatedAt = LocalDateTime.now();
        if (this.active == null) this.active = true;
        if (this.completed == null) this.completed = false;
        if (this.needsExternalAction == null) this.needsExternalAction = false;
        if (this.assignmentStatus == null) this.assignmentStatus = AssignmentStatus.PENDING_ASSIGNMENT;
        if (this.emailSent == null) this.emailSent = false;
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

    // ============================================================
    // ASSIGNMENT GETTERS AND SETTERS
    // ============================================================

    public Long getAssignedUserId() {
        return assignedUserId;
    }

    public void setAssignedUserId(Long assignedUserId) {
        this.assignedUserId = assignedUserId;
    }

    public String getAssignedUsername() {
        return assignedUsername;
    }

    public void setAssignedUsername(String assignedUsername) {
        this.assignedUsername = assignedUsername;
    }

    public String getAssignedUserEmail() {
        return assignedUserEmail;
    }

    public void setAssignedUserEmail(String assignedUserEmail) {
        this.assignedUserEmail = assignedUserEmail;
    }

    public Long getAssignedByUserId() {
        return assignedByUserId;
    }

    public void setAssignedByUserId(Long assignedByUserId) {
        this.assignedByUserId = assignedByUserId;
    }

    public String getAssignedByUsername() {
        return assignedByUsername;
    }

    public void setAssignedByUsername(String assignedByUsername) {
        this.assignedByUsername = assignedByUsername;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public LocalDateTime getEmailSentAt() {
        return emailSentAt;
    }

    public void setEmailSentAt(LocalDateTime emailSentAt) {
        this.emailSentAt = emailSentAt;
    }

    public Boolean getEmailSent() {
        return emailSent;
    }

    public void setEmailSent(Boolean emailSent) {
        this.emailSent = emailSent;
    }

    public AssignmentStatus getAssignmentStatus() {
        return assignmentStatus;
    }

    public void setAssignmentStatus(AssignmentStatus assignmentStatus) {
        this.assignmentStatus = assignmentStatus;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    // ============================================================
    // HOLD GETTERS AND SETTERS
    // ============================================================

    public Boolean getIsOnHold() {
        return isOnHold;
    }

    public void setIsOnHold(Boolean isOnHold) {
        this.isOnHold = isOnHold;
    }

    public String getHoldReason() {
        return holdReason;
    }

    public void setHoldReason(String holdReason) {
        this.holdReason = holdReason;
    }

    public LocalDateTime getHeldAt() {
        return heldAt;
    }

    public void setHeldAt(LocalDateTime heldAt) {
        this.heldAt = heldAt;
    }

    public String getHeldByUsername() {
        return heldByUsername;
    }

    public void setHeldByUsername(String heldByUsername) {
        this.heldByUsername = heldByUsername;
    }

    public LocalDateTime getUnholdAt() {
        return unholdAt;
    }

    public void setUnholdAt(LocalDateTime unholdAt) {
        this.unholdAt = unholdAt;
    }

    public String getUnholdByUsername() {
        return unholdByUsername;
    }

    public void setUnholdByUsername(String unholdByUsername) {
        this.unholdByUsername = unholdByUsername;
    }

    /**
     * Helper method to put this step on hold
     */
    public void holdProject(String reason, String username) {
        this.isOnHold = true;
        this.holdReason = reason;
        this.heldAt = LocalDateTime.now();
        this.heldByUsername = username;
        this.unholdAt = null;
        this.unholdByUsername = null;
    }

    /**
     * Helper method to release this step from hold
     */
    public void unholdProject(String username) {
        this.isOnHold = false;
        this.unholdAt = LocalDateTime.now();
        this.unholdByUsername = username;
    }

    /**
     * Helper method to check if this step is assigned to someone
     */
    public boolean isAssigned() {
        return assignedUserId != null && assignmentStatus != AssignmentStatus.PENDING_ASSIGNMENT;
    }

    /**
     * Helper method to assign a user to this step
     */
    public void assignUser(Long userId, String username, String email, Long assignedById, String assignedByName) {
        this.assignedUserId = userId;
        this.assignedUsername = username;
        this.assignedUserEmail = email;
        this.assignedByUserId = assignedById;
        this.assignedByUsername = assignedByName;
        this.assignedAt = LocalDateTime.now();
        this.assignmentStatus = AssignmentStatus.ASSIGNED;
    }

    /**
     * Helper method to mark email as sent
     */
    public void markEmailSent() {
        this.emailSent = true;
        this.emailSentAt = LocalDateTime.now();
    }
}
