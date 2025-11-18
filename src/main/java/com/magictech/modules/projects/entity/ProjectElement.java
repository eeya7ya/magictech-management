package com.magictech.modules.projects.entity;

import com.magictech.modules.storage.entity.StorageItem;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Project Element Entity
 * Links storage items to projects (which items are allocated to which project)
 *
 * ✅ FIXED: Changed StorageItem fetch type to EAGER to prevent LazyInitializationException
 */
@Entity
@Table(name = "project_elements")
public class ProjectElement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    // ✅ CRITICAL FIX: Changed from LAZY to EAGER
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "storage_item_id", nullable = false)
    private StorageItem storageItem;

    @Column(name = "quantity_needed", nullable = false)
    private Integer quantityNeeded = 0;

    @Column(name = "quantity_allocated")
    private Integer quantityAllocated = 0;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "status", length = 50)
    private String status; // "Pending", "Allocated", "In Use", "Returned"

    @Column(name = "added_date", nullable = false)
    private LocalDateTime addedDate;

    @Column(name = "allocated_date")
    private LocalDateTime allocatedDate;

    @Column(name = "added_by", length = 100)
    private String addedBy;

    @Column(nullable = false)
    private Boolean active = true;

    // Constructors
    public ProjectElement() {
        this.addedDate = LocalDateTime.now();
        this.active = true;
        this.status = "Pending";
        this.quantityNeeded = 0;
        this.quantityAllocated = 0;
    }

    public ProjectElement(Project project, StorageItem storageItem, Integer quantityNeeded) {
        this();
        this.project = project;
        this.storageItem = storageItem;
        this.quantityNeeded = quantityNeeded;
    }

    @PrePersist
    protected void onCreate() {
        if (addedDate == null) addedDate = LocalDateTime.now();
        if (active == null) active = true;
        if (status == null) status = "Pending";
        if (quantityNeeded == null) quantityNeeded = 0;
        if (quantityAllocated == null) quantityAllocated = 0;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public StorageItem getStorageItem() { return storageItem; }
    public void setStorageItem(StorageItem storageItem) { this.storageItem = storageItem; }

    public Integer getQuantityNeeded() { return quantityNeeded; }
    public void setQuantityNeeded(Integer quantityNeeded) { this.quantityNeeded = quantityNeeded; }

    public Integer getQuantityAllocated() { return quantityAllocated; }
    public void setQuantityAllocated(Integer quantityAllocated) {
        this.quantityAllocated = quantityAllocated;
    }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getAddedDate() { return addedDate; }
    public void setAddedDate(LocalDateTime addedDate) { this.addedDate = addedDate; }

    public LocalDateTime getAllocatedDate() { return allocatedDate; }
    public void setAllocatedDate(LocalDateTime allocatedDate) {
        this.allocatedDate = allocatedDate;
    }

    public String getAddedBy() { return addedBy; }
    public void setAddedBy(String addedBy) { this.addedBy = addedBy; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    @Override
    public String toString() {
        return "ProjectElement{" +
                "id=" + id +
                ", quantityNeeded=" + quantityNeeded +
                ", quantityAllocated=" + quantityAllocated +
                ", status='" + status + '\'' +
                '}';
    }
}