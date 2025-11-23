package com.magictech.modules.projects.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Site Survey Request Entity
 * Tracks site survey requests for projects
 * Links projects with site survey data
 */
@Entity
@Table(name = "site_survey_requests")
public class SiteSurveyRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "project_name", length = 200)
    private String projectName;

    @Column(name = "requested_by", length = 100, nullable = false)
    private String requestedBy;

    @Column(name = "requested_by_id")
    private Long requestedById;

    @Column(name = "request_date", nullable = false)
    private LocalDateTime requestDate;

    @Column(name = "status", length = 50)
    private String status; // "PENDING", "COMPLETED", "CANCELLED"

    @Column(name = "assigned_to", length = 100)
    private String assignedTo; // Who should do the survey (SALES or PROJECT team member)

    @Column(name = "assigned_to_id")
    private Long assignedToId;

    @Column(name = "priority", length = 20)
    private String priority; // "LOW", "MEDIUM", "HIGH", "URGENT"

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "survey_data_id")
    private Long surveyDataId; // Links to SiteSurveyData when uploaded

    @Column(name = "completion_date")
    private LocalDateTime completionDate;

    @Column(name = "completed_by", length = 100)
    private String completedBy;

    @Column(name = "completed_by_id")
    private Long completedById;

    // Standard metadata fields
    @Column(name = "date_added", nullable = false)
    private LocalDateTime dateAdded;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(nullable = false)
    private Boolean active = true;

    // Constructors
    public SiteSurveyRequest() {
        this.dateAdded = LocalDateTime.now();
        this.requestDate = LocalDateTime.now();
        this.active = true;
        this.status = "PENDING";
        this.priority = "MEDIUM";
    }

    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        if (dateAdded == null) {
            dateAdded = LocalDateTime.now();
        }
        if (requestDate == null) {
            requestDate = LocalDateTime.now();
        }
        if (active == null) {
            active = true;
        }
        if (status == null) {
            status = "PENDING";
        }
        if (priority == null) {
            priority = "MEDIUM";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
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

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public Long getRequestedById() {
        return requestedById;
    }

    public void setRequestedById(Long requestedById) {
        this.requestedById = requestedById;
    }

    public LocalDateTime getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDateTime requestDate) {
        this.requestDate = requestDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public Long getAssignedToId() {
        return assignedToId;
    }

    public void setAssignedToId(Long assignedToId) {
        this.assignedToId = assignedToId;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getSurveyDataId() {
        return surveyDataId;
    }

    public void setSurveyDataId(Long surveyDataId) {
        this.surveyDataId = surveyDataId;
    }

    public LocalDateTime getCompletionDate() {
        return completionDate;
    }

    public void setCompletionDate(LocalDateTime completionDate) {
        this.completionDate = completionDate;
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

    public LocalDateTime getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(LocalDateTime dateAdded) {
        this.dateAdded = dateAdded;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "SiteSurveyRequest{" +
                "id=" + id +
                ", projectId=" + projectId +
                ", projectName='" + projectName + '\'' +
                ", status='" + status + '\'' +
                ", priority='" + priority + '\'' +
                ", requestDate=" + requestDate +
                ", active=" + active +
                '}';
    }
}
