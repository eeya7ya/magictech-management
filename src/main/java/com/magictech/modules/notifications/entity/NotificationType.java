package com.magictech.modules.notifications.entity;

/**
 * Notification types for different categories
 */
public enum NotificationType {
    // Sales notifications
    NEW_SALES_ORDER("New Sales Order", "💰"),
    ORDER_STATUS_CHANGED("Order Status Changed", "📝"),
    ORDER_CANCELLED("Order Cancelled", "❌"),

    // Storage/Inventory notifications
    LOW_STOCK_ALERT("Low Stock Alert", "⚠️"),
    OUT_OF_STOCK("Out of Stock", "🚫"),
    ITEM_ADDED("Item Added", "➕"),
    ITEM_UPDATED("Item Updated", "✏️"),

    // Project notifications
    NEW_PROJECT("New Project", "🆕"),
    PROJECT_ASSIGNED("Project Assigned", "👤"),
    TASK_ASSIGNED("Task Assigned", "📋"),
    TASK_COMPLETED("Task Completed", "✅"),
    PROJECT_DEADLINE("Project Deadline", "⏰"),
    PROJECT_STATUS_CHANGED("Project Status Changed", "🔄"),

    // Maintenance notifications
    MAINTENANCE_DUE("Maintenance Due", "🔧"),
    MAINTENANCE_COMPLETED("Maintenance Completed", "✔️"),

    // System notifications
    SYSTEM_ALERT("System Alert", "🔔"),
    USER_MENTION("User Mention", "💬"),
    REMINDER("Reminder", "⏱️"),

    // General
    INFO("Information", "ℹ️"),
    WARNING("Warning", "⚠️"),
    ERROR("Error", "❌"),
    SUCCESS("Success", "✅");

    private final String displayName;
    private final String icon;

    NotificationType(String displayName, String icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }
}
