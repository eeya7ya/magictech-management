package com.magictech.modules.storage.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object for Project Analytics
 * Used in Storage Module's Analytics Dashboard
 */
public class ProjectAnalyticsDTO {
    private Long projectId;
    private String projectName;
    private String projectLocation;
    private String status;
    private LocalDate dateOfIssue;
    private LocalDate dateOfCompletion;
    private Integer elementsCount;
    private BigDecimal totalCost;
    private Integer durationDays;
    private String createdBy;

    // Constructors
    public ProjectAnalyticsDTO() {
    }

    public ProjectAnalyticsDTO(Long projectId, String projectName, String status,
                              LocalDate dateOfIssue, LocalDate dateOfCompletion) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.status = status;
        this.dateOfIssue = dateOfIssue;
        this.dateOfCompletion = dateOfCompletion;
    }

    // Getters and Setters
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

    public String getProjectLocation() {
        return projectLocation;
    }

    public void setProjectLocation(String projectLocation) {
        this.projectLocation = projectLocation;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
        // Calculate duration if both dates are present
        if (dateOfIssue != null && dateOfCompletion != null) {
            this.durationDays = (int) java.time.temporal.ChronoUnit.DAYS.between(dateOfIssue, dateOfCompletion);
        }
    }

    public Integer getElementsCount() {
        return elementsCount;
    }

    public void setElementsCount(Integer elementsCount) {
        this.elementsCount = elementsCount;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public Integer getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(Integer durationDays) {
        this.durationDays = durationDays;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
