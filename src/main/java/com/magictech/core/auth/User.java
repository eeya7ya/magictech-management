package com.magictech.core.auth;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Email(message = "Please provide a valid email address")
    @Column(unique = true, length = 100)
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "photo_path")
    private String photoPath;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(nullable = false)
    private Boolean active = true;

    // SMTP Email Settings (per-user)
    @Column(name = "smtp_provider", length = 50)
    private String smtpProvider; // gmail, outlook, hotmail, custom

    @Column(name = "smtp_host", length = 255)
    private String smtpHost;

    @Column(name = "smtp_port")
    private Integer smtpPort = 587;

    @Column(name = "smtp_password", length = 255)
    private String smtpPassword; // App password for SMTP

    @Column(name = "smtp_configured")
    private Boolean smtpConfigured = false;

    public User() {
        this.createdAt = LocalDateTime.now();
        this.active = true;
    }

    public User(String username, String password, UserRole role) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.createdAt = LocalDateTime.now();
        this.active = true;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.active == null) {
            this.active = true;
        }
        System.out.println("✓ User @PrePersist: " + username + " | Active: " + active + " | Role: " + role);
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        System.out.println("✓ User @PreUpdate: " + username + " | Active: " + active);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public boolean hasAccess(UserRole requiredRole) {
        return this.role == UserRole.MASTER || this.role == requiredRole;
    }

    // SMTP Getters and Setters
    public String getSmtpProvider() {
        return smtpProvider;
    }

    public void setSmtpProvider(String smtpProvider) {
        this.smtpProvider = smtpProvider;
    }

    public String getSmtpHost() {
        return smtpHost;
    }

    public void setSmtpHost(String smtpHost) {
        this.smtpHost = smtpHost;
    }

    public Integer getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(Integer smtpPort) {
        this.smtpPort = smtpPort;
    }

    public String getSmtpPassword() {
        return smtpPassword;
    }

    public void setSmtpPassword(String smtpPassword) {
        this.smtpPassword = smtpPassword;
    }

    public Boolean getSmtpConfigured() {
        return smtpConfigured;
    }

    public void setSmtpConfigured(Boolean smtpConfigured) {
        this.smtpConfigured = smtpConfigured;
    }

    /**
     * Check if user has complete SMTP configuration
     */
    public boolean hasSmtpConfigured() {
        return smtpConfigured != null && smtpConfigured &&
               email != null && !email.isEmpty() &&
               smtpHost != null && !smtpHost.isEmpty() &&
               smtpPassword != null && !smtpPassword.isEmpty();
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', role=" + role + ", active=" + active + "}";
    }
}