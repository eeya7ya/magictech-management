package com.magictech.modules.sales.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Main workflow tracker for project lifecycle
 * Tracks the 8-step sequential workflow for "Sell as New Project"
 */
@Entity
@Table(name = "project_workflows")
public class ProjectWorkflow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false, unique = true)
    private Long projectId;

    @Column(name = "current_step", nullable = false)
    private Integer currentStep = 1; // Steps 1-8

    @Column(name = "workflow_status", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private WorkflowStatusType status = WorkflowStatusType.IN_PROGRESS;

    @Column(name = "created_by", length = 100, nullable = false)
    private String createdBy; // Sales user "x"

    @Column(name = "created_by_id")
    private Long createdById;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt;

    @Column(name = "last_updated_by", length = 100)
    private String lastUpdatedBy;

    @Column(nullable = false)
    private Boolean active = true;

    // Workflow step completion flags
    @Column(name = "step1_site_survey_completed")
    private Boolean step1Completed = false;

    @Column(name = "step2_selection_design_completed")
    private Boolean step2Completed = false;

    @Column(name = "step3_bank_guarantee_completed")
    private Boolean step3Completed = false;

    @Column(name = "step4_missing_item_completed")
    private Boolean step4Completed = false;

    @Column(name = "step5_tender_acceptance_completed")
    private Boolean step5Completed = false;

    @Column(name = "step6_project_finished_completed")
    private Boolean step6Completed = false;

    @Column(name = "step7_after_sales_completed")
    private Boolean step7Completed = false;

    @Column(name = "step8_completion_completed")
    private Boolean step8Completed = false;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.lastUpdatedAt = LocalDateTime.now();
        if (this.active == null) this.active = true;
        if (this.currentStep == null) this.currentStep = 1;
        if (this.status == null) this.status = WorkflowStatusType.IN_PROGRESS;
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastUpdatedAt = LocalDateTime.now();
    }

    // Enum for workflow status
    public enum WorkflowStatusType {
        IN_PROGRESS,
        COMPLETED,
        REJECTED,
        ON_HOLD
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Integer getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(Integer currentStep) {
        this.currentStep = currentStep;
    }

    public WorkflowStatusType getStatus() {
        return status;
    }

    public void setStatus(WorkflowStatusType status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Long getCreatedById() {
        return createdById;
    }

    public void setCreatedById(Long createdById) {
        this.createdById = createdById;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public String getLastUpdatedBy() {
        return lastUpdatedBy;
    }

    public void setLastUpdatedBy(String lastUpdatedBy) {
        this.lastUpdatedBy = lastUpdatedBy;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getStep1Completed() {
        return step1Completed;
    }

    public void setStep1Completed(Boolean step1Completed) {
        this.step1Completed = step1Completed;
    }

    public Boolean getStep2Completed() {
        return step2Completed;
    }

    public void setStep2Completed(Boolean step2Completed) {
        this.step2Completed = step2Completed;
    }

    public Boolean getStep3Completed() {
        return step3Completed;
    }

    public void setStep3Completed(Boolean step3Completed) {
        this.step3Completed = step3Completed;
    }

    public Boolean getStep4Completed() {
        return step4Completed;
    }

    public void setStep4Completed(Boolean step4Completed) {
        this.step4Completed = step4Completed;
    }

    public Boolean getStep5Completed() {
        return step5Completed;
    }

    public void setStep5Completed(Boolean step5Completed) {
        this.step5Completed = step5Completed;
    }

    public Boolean getStep6Completed() {
        return step6Completed;
    }

    public void setStep6Completed(Boolean step6Completed) {
        this.step6Completed = step6Completed;
    }

    public Boolean getStep7Completed() {
        return step7Completed;
    }

    public void setStep7Completed(Boolean step7Completed) {
        this.step7Completed = step7Completed;
    }

    public Boolean getStep8Completed() {
        return step8Completed;
    }

    public void setStep8Completed(Boolean step8Completed) {
        this.step8Completed = step8Completed;
    }

    // Helper method to check if step is completed
    public boolean isStepCompleted(int stepNumber) {
        return switch (stepNumber) {
            case 1 -> Boolean.TRUE.equals(step1Completed);
            case 2 -> Boolean.TRUE.equals(step2Completed);
            case 3 -> Boolean.TRUE.equals(step3Completed);
            case 4 -> Boolean.TRUE.equals(step4Completed);
            case 5 -> Boolean.TRUE.equals(step5Completed);
            case 6 -> Boolean.TRUE.equals(step6Completed);
            case 7 -> Boolean.TRUE.equals(step7Completed);
            case 8 -> Boolean.TRUE.equals(step8Completed);
            default -> false;
        };
    }

    // Helper method to mark step as completed
    public void markStepCompleted(int stepNumber) {
        switch (stepNumber) {
            case 1 -> step1Completed = true;
            case 2 -> step2Completed = true;
            case 3 -> step3Completed = true;
            case 4 -> step4Completed = true;
            case 5 -> step5Completed = true;
            case 6 -> step6Completed = true;
            case 7 -> step7Completed = true;
            case 8 -> {
                step8Completed = true;
                this.status = WorkflowStatusType.COMPLETED;
                this.completedAt = LocalDateTime.now();
            }
        }
    }
}
