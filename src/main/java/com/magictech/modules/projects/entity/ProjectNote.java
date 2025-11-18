package com.magictech.modules.projects.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Project Note Entity
 * Stores important notes/descriptions for each project
 */
@Entity
@Table(name = "project_notes")
public class ProjectNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "note_title", length = 200)
    private String noteTitle;

    @Column(name = "important_description", columnDefinition = "TEXT")
    private String importantDescription;

    @Column(name = "note_type", length = 50)
    private String noteType; // "General", "Critical", "Warning", "Info"

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(nullable = false)
    private Boolean active = true;

    // Constructors
    public ProjectNote() {
        this.createdAt = LocalDateTime.now();
        this.active = true;
        this.noteType = "General";
    }

    public ProjectNote(Project project, String importantDescription) {
        this();
        this.project = project;
        this.importantDescription = importantDescription;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (active == null) active = true;
        if (noteType == null) noteType = "General";
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public String getNoteTitle() { return noteTitle; }
    public void setNoteTitle(String noteTitle) { this.noteTitle = noteTitle; }

    public String getImportantDescription() { return importantDescription; }
    public void setImportantDescription(String importantDescription) {
        this.importantDescription = importantDescription;
    }

    public String getNoteType() { return noteType; }
    public void setNoteType(String noteType) { this.noteType = noteType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    @Override
    public String toString() {
        return "ProjectNote{" +
                "id=" + id +
                ", noteTitle='" + noteTitle + '\'' +
                ", noteType='" + noteType + '\'' +
                '}';
    }
}