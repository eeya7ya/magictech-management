package com.magictech.core.auth;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity to store OAuth2 tokens for users
 * Enables each user to authenticate with their own Google account
 * for sending emails via Gmail API
 */
@Entity
@Table(name = "user_oauth2_tokens")
public class UserOAuth2Token {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider; // GOOGLE, MICROSOFT, etc.

    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken; // Encrypted

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken; // Encrypted

    @Column(name = "token_type", length = 50)
    private String tokenType; // Usually "Bearer"

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "scope", columnDefinition = "TEXT")
    private String scope; // Permissions granted

    @Column(name = "email", length = 100)
    private String email; // Google account email

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Boolean active = true;

    public UserOAuth2Token() {
        this.createdAt = LocalDateTime.now();
        this.active = true;
    }

    public UserOAuth2Token(Long userId, String provider) {
        this.userId = userId;
        this.provider = provider;
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
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Check if token is expired
    public boolean isExpired() {
        if (expiresAt == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(expiresAt);
    }

    // Check if token needs refresh (expires in less than 5 minutes)
    public boolean needsRefresh() {
        if (expiresAt == null) {
            return false;
        }
        return LocalDateTime.now().plusMinutes(5).isAfter(expiresAt);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "UserOAuth2Token{" +
                "id=" + id +
                ", userId=" + userId +
                ", provider='" + provider + '\'' +
                ", email='" + email + '\'' +
                ", expiresAt=" + expiresAt +
                ", active=" + active +
                '}';
    }
}
