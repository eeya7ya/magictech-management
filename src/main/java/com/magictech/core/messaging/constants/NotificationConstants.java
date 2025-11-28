package com.magictech.core.messaging.constants;

/**
 * Constants for notification system.
 */
public class NotificationConstants {

    // Notification Types
    public static final String TYPE_INFO = "INFO";
    public static final String TYPE_WARNING = "WARNING";
    public static final String TYPE_SUCCESS = "SUCCESS";
    public static final String TYPE_ERROR = "ERROR";

    // Module Names
    public static final String MODULE_PRESALES = "presales";
    public static final String MODULE_SALES = "sales";
    public static final String MODULE_QA = "qualityassurance";
    public static final String MODULE_FINANCE = "finance";
    public static final String MODULE_PROJECTS = "projects";
    public static final String MODULE_STORAGE = "storage";
    public static final String MODULE_MAINTENANCE = "maintenance";
    public static final String MODULE_ALL = "ALL";

    // Action Types
    public static final String ACTION_CREATED = "created";
    public static final String ACTION_UPDATED = "updated";
    public static final String ACTION_DELETED = "deleted";
    public static final String ACTION_CONFIRMED = "confirmed";
    public static final String ACTION_COMPLETED = "completed";
    public static final String ACTION_CONFIRMATION_REQUESTED = "confirmation_requested";
    public static final String ACTION_ASSIGNED = "assigned";

    // Entity Types
    public static final String ENTITY_PROJECT = "project";
    public static final String ENTITY_WORKFLOW = "workflow";
    public static final String ENTITY_PRESALES_ITEM = "presales_item";
    public static final String ENTITY_SALES_ORDER = "sales_order";
    public static final String ENTITY_QA_ITEM = "qa_item";
    public static final String ENTITY_FINANCE_ITEM = "finance_item";
    public static final String ENTITY_STORAGE_ITEM = "storage_item";
    public static final String ENTITY_MAINTENANCE_ITEM = "maintenance_item";

    // Priority Levels
    public static final String PRIORITY_LOW = "LOW";
    public static final String PRIORITY_MEDIUM = "MEDIUM";
    public static final String PRIORITY_HIGH = "HIGH";
    public static final String PRIORITY_URGENT = "URGENT";

    // Device Status
    public static final String DEVICE_STATUS_ONLINE = "ONLINE";
    public static final String DEVICE_STATUS_OFFLINE = "OFFLINE";
    public static final String DEVICE_STATUS_IDLE = "IDLE";

    // Redis Channels
    public static final String CHANNEL_PRESALES_NOTIFICATIONS = "presales_notifications";
    public static final String CHANNEL_SALES_NOTIFICATIONS = "sales_notifications";
    public static final String CHANNEL_QA_NOTIFICATIONS = "qa_notifications";
    public static final String CHANNEL_FINANCE_NOTIFICATIONS = "finance_notifications";
    public static final String CHANNEL_PROJECTS_NOTIFICATIONS = "projects_notifications";
    public static final String CHANNEL_STORAGE_NOTIFICATIONS = "storage_notifications";
    public static final String CHANNEL_MAINTENANCE_NOTIFICATIONS = "maintenance_notifications";
    public static final String CHANNEL_ALL_NOTIFICATIONS = "all_notifications";

    // Channel Patterns
    public static final String CHANNEL_PATTERN_MODULE = "%s_notifications"; // e.g., sales_notifications
    public static final String CHANNEL_PATTERN_ACTION = "%s:%s:%s"; // e.g., sales:created:project

    /**
     * Get Redis channel name for a specific module.
     */
    public static String getModuleChannel(String module) {
        return String.format(CHANNEL_PATTERN_MODULE, module);
    }

    /**
     * Get Redis channel name for a specific action.
     */
    public static String getActionChannel(String module, String action, String entityType) {
        return String.format(CHANNEL_PATTERN_ACTION, module, action, entityType);
    }

    /**
     * Get all module channels.
     */
    public static String[] getAllModuleChannels() {
        return new String[] {
            CHANNEL_PRESALES_NOTIFICATIONS,
            CHANNEL_SALES_NOTIFICATIONS,
            CHANNEL_QA_NOTIFICATIONS,
            CHANNEL_FINANCE_NOTIFICATIONS,
            CHANNEL_PROJECTS_NOTIFICATIONS,
            CHANNEL_STORAGE_NOTIFICATIONS,
            CHANNEL_MAINTENANCE_NOTIFICATIONS,
            CHANNEL_ALL_NOTIFICATIONS
        };
    }

    /**
     * Get channels for a specific module (including all_notifications).
     */
    public static String[] getChannelsForModule(String module) {
        return new String[] {
            getModuleChannel(module),
            CHANNEL_ALL_NOTIFICATIONS
        };
    }

    private NotificationConstants() {
        // Prevent instantiation
    }
}
