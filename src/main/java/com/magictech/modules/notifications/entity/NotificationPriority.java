package com.magictech.modules.notifications.entity;

/**
 * Priority levels for notifications
 */
public enum NotificationPriority {
    LOW("Low", "#4CAF50"),           // Green
    NORMAL("Normal", "#2196F3"),     // Blue
    HIGH("High", "#FF9800"),         // Orange
    URGENT("Urgent", "#F44336");     // Red

    private final String displayName;
    private final String color;

    NotificationPriority(String displayName, String color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColor() {
        return color;
    }

    public boolean shouldSendEmail() {
        return this == HIGH || this == URGENT;
    }

    public boolean shouldSendDesktop() {
        return this == NORMAL || this == HIGH || this == URGENT;
    }
}
