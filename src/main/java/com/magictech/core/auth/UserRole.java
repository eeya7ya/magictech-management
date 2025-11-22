package com.magictech.core.auth;

public enum UserRole {
    MASTER("Master", "Full system access"),
    PRESALES("Presales Team", "Presales module access - quotations and initial customer contact"),
    SALES("Sales Team", "Sales module access - confirmed orders and pricing"),
    QUALITY_ASSURANCE("Quality Assurance Team", "Quality assurance module access - verification and approval"),
    FINANCE("Finance Team", "Finance module access - invoicing and payments"),
    MAINTENANCE("Maintenance Team", "Maintenance module access"),
    PROJECTS("Project Manager", "Projects module full access"),
    PROJECT_SUPPLIER("Project Supplier", "Projects module - same as Project Manager"),
    STORAGE("Storage Team", "Storage module access"),
    CLIENT("Client", "Limited access");

    private final String displayName;
    private final String description;

    UserRole(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}