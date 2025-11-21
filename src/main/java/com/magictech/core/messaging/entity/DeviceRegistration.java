package com.magictech.core.messaging.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a registered device in the system.
 * Tracks connected devices for notification routing and status monitoring.
 */
@Entity
@Table(name = "device_registrations")
public class DeviceRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, unique = true, length = 100)
    private String deviceId; // Unique identifier for the device (e.g., MAC address, UUID)

    @Column(name = "device_name", length = 200)
    private String deviceName; // User-friendly device name

    @Column(name = "module_type", nullable = false, length = 50)
    private String moduleType; // sales, projects, storage, maintenance, pricing

    @Column(name = "user_id")
    private Long userId; // Associated user

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // ONLINE, OFFLINE, IDLE

    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "hostname", length = 200)
    private String hostname;

    @Column(name = "application_version", length = 50)
    private String applicationVersion;

    @Column(name = "subscribed_channels", columnDefinition = "TEXT")
    private String subscribedChannels; // JSON array of subscribed channels

    @Column(nullable = false)
    private Boolean active = true;

    // Lifecycle hooks
    @PrePersist
    protected void onCreate() {
        this.registeredAt = LocalDateTime.now();
        this.lastSeen = LocalDateTime.now();
        this.lastHeartbeat = LocalDateTime.now();
        if (this.active == null) {
            this.active = true;
        }
        if (this.status == null) {
            this.status = "ONLINE";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastSeen = LocalDateTime.now();
    }

    // Constructors
    public DeviceRegistration() {
    }

    public DeviceRegistration(String deviceId, String moduleType, Long userId, String username) {
        this.deviceId = deviceId;
        this.moduleType = moduleType;
        this.userId = userId;
        this.username = username;
        this.status = "ONLINE";
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getModuleType() {
        return moduleType;
    }

    public void setModuleType(String moduleType) {
        this.moduleType = moduleType;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(LocalDateTime lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    public LocalDateTime getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getApplicationVersion() {
        return applicationVersion;
    }

    public void setApplicationVersion(String applicationVersion) {
        this.applicationVersion = applicationVersion;
    }

    public String getSubscribedChannels() {
        return subscribedChannels;
    }

    public void setSubscribedChannels(String subscribedChannels) {
        this.subscribedChannels = subscribedChannels;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
