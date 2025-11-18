package com.magictech.core.module;

import com.magictech.core.auth.UserRole;

import java.util.Arrays;
import java.util.List;

/**
 * Module Configuration
 * Defines metadata and permissions for each module
 */
public class ModuleConfig {

    private String name;
    private String displayName;
    private String description;
    private String icon;
    private String colorScheme;
    private List<UserRole> allowedRoles;
    private boolean requiresAuth;

    // Private constructor - use factory methods
    private ModuleConfig() {
        this.requiresAuth = true; // Default to requiring authentication
    }

    // Builder pattern for easy configuration
    public static class Builder {
        private ModuleConfig config = new ModuleConfig();

        public Builder name(String name) {
            config.name = name;
            return this;
        }

        public Builder displayName(String displayName) {
            config.displayName = displayName;
            return this;
        }

        public Builder description(String description) {
            config.description = description;
            return this;
        }

        public Builder icon(String icon) {
            config.icon = icon;
            return this;
        }

        public Builder colorScheme(String colorScheme) {
            config.colorScheme = colorScheme;
            return this;
        }

        public Builder allowedRoles(UserRole... roles) {
            config.allowedRoles = Arrays.asList(roles);
            return this;
        }

        public Builder requiresAuth(boolean requiresAuth) {
            config.requiresAuth = requiresAuth;
            return this;
        }

        public ModuleConfig build() {
            return config;
        }
    }

    // Factory method for Storage Module
    public static ModuleConfig createStorageConfig() {
        return new Builder()
                .name("storage")
                .displayName("Storage Management")
                .description("Manage inventory, documents, and resources")
                .icon("📦")
                .colorScheme("module-red")
                .allowedRoles(UserRole.MASTER, UserRole.STORAGE)
                .build();
    }

    // Factory method for Sales Module (for future implementation)
    public static ModuleConfig createSalesConfig() {
        return new Builder()
                .name("sales")
                .displayName("Sales Team Module")
                .description("Manage sales operations and customer relationships")
                .icon("🛒")
                .colorScheme("module-blue")
                .allowedRoles(UserRole.MASTER, UserRole.SALES)
                .build();
    }

    // Factory method for Maintenance Module (for future implementation)
    public static ModuleConfig createMaintenanceConfig() {
        return new Builder()
                .name("maintenance")
                .displayName("Maintenance Team Module")
                .description("Handle maintenance requests and equipment tracking")
                .icon("🔧")
                .colorScheme("module-green")
                .allowedRoles(UserRole.MASTER, UserRole.MAINTENANCE)
                .build();
    }

    // Factory method for Projects Module (for future implementation)
    public static ModuleConfig createProjectsConfig() {
        return new Builder()
                .name("projects")
                .displayName("Projects Team Module")
                .description("Coordinate projects and track progress")
                .icon("📁")
                .colorScheme("module-purple")
                .allowedRoles(UserRole.MASTER, UserRole.PROJECTS)
                .build();
    }

    // Factory method for Pricing Module (for future implementation)
    public static ModuleConfig createPricingConfig() {
        return new Builder()
                .name("pricing")
                .displayName("Pricing Module")
                .description("Configure pricing models and manage quotes")
                .icon("💰")
                .colorScheme("module-orange")
                .allowedRoles(UserRole.MASTER, UserRole.PRICING)
                .build();
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getIcon() {
        return icon;
    }

    public String getColorScheme() {
        return colorScheme;
    }

    public List<UserRole> getAllowedRoles() {
        return allowedRoles;
    }

    public boolean isRequiresAuth() {
        return requiresAuth;
    }

    /**
     * Check if a user role is allowed to access this module
     */
    public boolean isRoleAllowed(UserRole role) {
        if (allowedRoles == null || allowedRoles.isEmpty()) {
            return true; // If no roles specified, allow all
        }
        return allowedRoles.contains(role);
    }

    @Override
    public String toString() {
        return "ModuleConfig{" +
                "name='" + name + '\'' +
                ", displayName='" + displayName + '\'' +
                ", allowedRoles=" + allowedRoles +
                '}';
    }
}