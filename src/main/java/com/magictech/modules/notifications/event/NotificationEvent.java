package com.magictech.modules.notifications.event;

import com.magictech.modules.notifications.entity.NotificationPriority;
import com.magictech.modules.notifications.entity.NotificationType;
import org.springframework.context.ApplicationEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Event published when a notification should be created
 */
public class NotificationEvent extends ApplicationEvent {

    private final List<Long> userIds;
    private final String title;
    private final String message;
    private final NotificationType type;
    private final NotificationPriority priority;
    private final String moduleSource;
    private final Long referenceId;
    private final String referenceType;
    private final String actionUrl;

    private NotificationEvent(Builder builder) {
        super(builder.source);
        this.userIds = builder.userIds;
        this.title = builder.title;
        this.message = builder.message;
        this.type = builder.type;
        this.priority = builder.priority;
        this.moduleSource = builder.moduleSource;
        this.referenceId = builder.referenceId;
        this.referenceType = builder.referenceType;
        this.actionUrl = builder.actionUrl;
    }

    public List<Long> getUserIds() {
        return userIds;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public NotificationType getType() {
        return type;
    }

    public NotificationPriority getPriority() {
        return priority;
    }

    public String getModuleSource() {
        return moduleSource;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public static class Builder {
        private final Object source;
        private List<Long> userIds = new ArrayList<>();
        private String title;
        private String message;
        private NotificationType type = NotificationType.INFO;
        private NotificationPriority priority = NotificationPriority.NORMAL;
        private String moduleSource;
        private Long referenceId;
        private String referenceType;
        private String actionUrl;

        public Builder(Object source) {
            this.source = source;
        }

        public Builder userIds(List<Long> userIds) {
            this.userIds = userIds;
            return this;
        }

        public Builder userId(Long userId) {
            this.userIds = List.of(userId);
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder type(NotificationType type) {
            this.type = type;
            return this;
        }

        public Builder priority(NotificationPriority priority) {
            this.priority = priority;
            return this;
        }

        public Builder moduleSource(String moduleSource) {
            this.moduleSource = moduleSource;
            return this;
        }

        public Builder referenceId(Long referenceId) {
            this.referenceId = referenceId;
            return this;
        }

        public Builder referenceType(String referenceType) {
            this.referenceType = referenceType;
            return this;
        }

        public Builder actionUrl(String actionUrl) {
            this.actionUrl = actionUrl;
            return this;
        }

        public NotificationEvent build() {
            return new NotificationEvent(this);
        }
    }
}
