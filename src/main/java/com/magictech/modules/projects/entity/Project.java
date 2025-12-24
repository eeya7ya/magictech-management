package com.magictech.modules.projects.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Project Entity - Main Projects Table
 * Acts as the entry point for all project-related data
 * Internal analysis, schedules, and calculations will be linked to this table from other modules
 */
@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_name", nullable = false, length = 200)
    private String projectName;

    @Column(name = "project_location", length = 300)
    private String projectLocation;

    @Column(name = "date_of_issue")
    private LocalDate dateOfIssue;

    @Column(name = "date_of_completion")
    private LocalDate dateOfCompletion;

    // Metadata fields - Owner tracking for role-based visibility
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_by_id")
    private Long createdById;

    @Column(name = "date_added", nullable = false)
    private LocalDateTime dateAdded;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(nullable = false)
    private Boolean active = true;

    // Status field for project state
    @Column(length = 50)
    private String status; // e.g., "Planning", "In Progress", "Completed", "On Hold"

    // Notes/Description field
    @Column(columnDefinition = "TEXT")
    private String notes;

    // Site survey request flag
    @Column(name = "site_survey_requested")
    private Boolean siteSurveyRequested = false;

    @Column(name = "site_survey_request_date")
    private LocalDateTime siteSurveyRequestDate;

    @Column(name = "site_survey_requested_by", length = 100)
    private String siteSurveyRequestedBy;

    // Constructors
    public Project() {
        this.dateAdded = LocalDateTime.now();
        this.active = true;
        this.status = "Planning";
    }

    public Project(String projectName, String projectLocation, LocalDate dateOfIssue, LocalDate dateOfCompletion) {
        this();
        this.projectName = projectName;
        this.projectLocation = projectLocation;
        this.dateOfIssue = dateOfIssue;
        this.dateOfCompletion = dateOfCompletion;
    }

    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        if (dateAdded == null) {
            dateAdded = LocalDateTime.now();
        }
        if (active == null) {
            active = true;
        }
        if (status == null) {
            status = "Planning";
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

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectLocation() {
        return projectLocation;
    }

    public void setProjectLocation(String projectLocation) {
        this.projectLocation = projectLocation;
    }

    public LocalDate getDateOfIssue() {
        return dateOfIssue;
    }

    public void setDateOfIssue(LocalDate dateOfIssue) {
        this.dateOfIssue = dateOfIssue;
    }

    public LocalDate getDateOfCompletion() {
        return dateOfCompletion;
    }

    public void setDateOfCompletion(LocalDate dateOfCompletion) {
        this.dateOfCompletion = dateOfCompletion;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Boolean getSiteSurveyRequested() {
        return siteSurveyRequested;
    }

    public void setSiteSurveyRequested(Boolean siteSurveyRequested) {
        this.siteSurveyRequested = siteSurveyRequested;
    }

    public LocalDateTime getSiteSurveyRequestDate() {
        return siteSurveyRequestDate;
    }

    public void setSiteSurveyRequestDate(LocalDateTime siteSurveyRequestDate) {
        this.siteSurveyRequestDate = siteSurveyRequestDate;
    }

    public String getSiteSurveyRequestedBy() {
        return siteSurveyRequestedBy;
    }

    public void setSiteSurveyRequestedBy(String siteSurveyRequestedBy) {
        this.siteSurveyRequestedBy = siteSurveyRequestedBy;
    }

    @Override
    public String toString() {
        return "Project{" +
                "id=" + id +
                ", projectName='" + projectName + '\'' +
                ", location='" + projectLocation + '\'' +
                ", status='" + status + '\'' +
                ", dateOfIssue=" + dateOfIssue +
                ", dateOfCompletion=" + dateOfCompletion +
                ", active=" + active +
                '}';
    }
}