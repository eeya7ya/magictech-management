package com.magictech.core.auth;

public enum UserRole {
    MASTER("Master", "Full system access"),
    SALES("Sales Team", "Sales module access"),
    MAINTENANCE("Maintenance Team", "Maintenance module access"),
    PROJECTS("Project Manager", "Projects module full access"),
    PROJECT_SUPPLIER("Project Supplier", "Projects module - same as Project Manager"),
    PRICING("Pricing Team", "Pricing module access"),
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