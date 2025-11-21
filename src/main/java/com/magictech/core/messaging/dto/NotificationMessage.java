package com.magictech.core.messaging.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for notification messages sent through Redis pub/sub.
 */
public class NotificationMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String type; // INFO, WARNING, SUCCESS, ERROR
    private String module; // sales, projects, storage, maintenance, pricing
    private String action; // created, updated, deleted, confirmed, completed
    private String entityType; // project, sales_order, storage_item
    private Long entityId;
    private String title;
    private String message;
    private String targetDeviceId;
    private String targetModule;
    private String priority; // LOW, MEDIUM, HIGH, URGENT
    private String createdBy;
    private String sourceDeviceId; // Device that created this notification
    private boolean excludeSender; // If true, don't send notification back to source device
    private LocalDateTime timestamp;
    private String metadata; // JSON string for additional data

    public NotificationMessage() {
        this.timestamp = LocalDateTime.now();
    }

    public NotificationMessage(String type, String module, String action, String title, String message) {
        this.type = type;
        this.module = module;
        this.action = action;
        this.title = title;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    // Builder pattern
    public static class Builder {
        private final NotificationMessage message;

        public Builder() {
            this.message = new NotificationMessage();
        }

        public Builder type(String type) {
            message.type = type;
            return this;
        }

        public Builder module(String module) {
            message.module = module;
            return this;
        }

        public Builder action(String action) {
            message.action = action;
            return this;
        }

        public Builder entityType(String entityType) {
            message.entityType = entityType;
            return this;
        }

        public Builder entityId(Long entityId) {
            message.entityId = entityId;
            return this;
        }

        public Builder title(String title) {
            message.title = title;
            return this;
        }

        public Builder message(String messageText) {
            message.message = messageText;
            return this;
        }

        public Builder targetDeviceId(String targetDeviceId) {
            message.targetDeviceId = targetDeviceId;
            return this;
        }

        public Builder targetModule(String targetModule) {
            message.targetModule = targetModule;
            return this;
        }

        public Builder priority(String priority) {
            message.priority = priority;
            return this;
        }

        public Builder createdBy(String createdBy) {
            message.createdBy = createdBy;
            return this;
        }

        public Builder metadata(String metadata) {
            message.metadata = metadata;
            return this;
        }

        public Builder sourceDeviceId(String sourceDeviceId) {
            message.sourceDeviceId = sourceDeviceId;
            return this;
        }

        public Builder excludeSender(boolean excludeSender) {
            message.excludeSender = excludeSender;
            return this;
        }

        public NotificationMessage build() {
            return message;
        }
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTargetDeviceId() {
        return targetDeviceId;
    }

    public void setTargetDeviceId(String targetDeviceId) {
        this.targetDeviceId = targetDeviceId;
    }

    public String getTargetModule() {
        return targetModule;
    }

    public void setTargetModule(String targetModule) {
        this.targetModule = targetModule;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getSourceDeviceId() {
        return sourceDeviceId;
    }

    public void setSourceDeviceId(String sourceDeviceId) {
        this.sourceDeviceId = sourceDeviceId;
    }

    public boolean isExcludeSender() {
        return excludeSender;
    }

    public void setExcludeSender(boolean excludeSender) {
        this.excludeSender = excludeSender;
    }

    @Override
    public String toString() {
        return "NotificationMessage{" +
                "type='" + type + '\'' +
                ", module='" + module + '\'' +
                ", action='" + action + '\'' +
                ", title='" + title + '\'' +
                ", message='" + message + '\'' +
                ", targetModule='" + targetModule + '\'' +
                ", sourceDeviceId='" + sourceDeviceId + '\'' +
                ", excludeSender=" + excludeSender +
                ", timestamp=" + timestamp +
                '}';
    }
}
