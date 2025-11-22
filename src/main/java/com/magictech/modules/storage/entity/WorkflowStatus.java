package com.magictech.modules.storage.entity;

/**
 * Workflow Status Enum
 * Tracks the lifecycle stage of storage items through different modules
 * Enables workflow management between Presales → Sales → QA → Finance → Projects
 */
public enum WorkflowStatus {
    DRAFT("Draft", "Initial state - not yet in workflow"),
    PRESALES("Presales", "In presales phase - quotations and initial customer contact"),
    SALES("Sales", "In sales phase - confirmed orders and pricing"),
    QUALITY_CHECK("Quality Check", "In quality assurance - verification and approval"),
    APPROVED("Approved", "Approved by quality assurance"),
    FINANCE("Finance", "In finance processing - invoicing and payments"),
    INVOICED("Invoiced", "Invoice created and sent"),
    PAID("Paid", "Payment received"),
    PROJECT("Project", "Assigned to project execution"),
    STORAGE("Storage", "In physical storage/inventory"),
    MAINTENANCE("Maintenance", "In maintenance phase"),
    COMPLETED("Completed", "Workflow completed"),
    CANCELLED("Cancelled", "Workflow cancelled");

    private final String displayName;
    private final String description;

    WorkflowStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Get next logical workflow status
     */
    public WorkflowStatus getNextStatus() {
        return switch (this) {
            case DRAFT -> PRESALES;
            case PRESALES -> SALES;
            case SALES -> QUALITY_CHECK;
            case QUALITY_CHECK -> APPROVED;
            case APPROVED -> FINANCE;
            case FINANCE -> INVOICED;
            case INVOICED -> PAID;
            case PAID -> PROJECT;
            case PROJECT -> STORAGE;
            case STORAGE -> COMPLETED;
            default -> this; // COMPLETED, CANCELLED, MAINTENANCE stay as-is
        };
    }

    /**
     * Check if status can transition to another status
     */
    public boolean canTransitionTo(WorkflowStatus target) {
        // Can always cancel
        if (target == CANCELLED) {
            return this != COMPLETED && this != CANCELLED;
        }

        // Can't transition from terminal states
        if (this == COMPLETED || this == CANCELLED) {
            return false;
        }

        // Can always move to maintenance
        if (target == MAINTENANCE) {
            return true;
        }

        // Check forward transitions
        return target.ordinal() > this.ordinal();
    }
}
