package com.magictech.core.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing OAuth2 tokens
 */
@Repository
public interface UserOAuth2TokenRepository extends JpaRepository<UserOAuth2Token, Long> {

    /**
     * Find OAuth2 token by user ID
     */
    Optional<UserOAuth2Token> findByUserId(Long userId);

    /**
     * Find OAuth2 token by user ID and provider
     */
    Optional<UserOAuth2Token> findByUserIdAndProvider(Long userId, String provider);

    /**
     * Find OAuth2 token by user ID and provider (only active)
     */
    Optional<UserOAuth2Token> findByUserIdAndProviderAndActiveTrue(Long userId, String provider);

    /**
     * Check if user has OAuth2 token configured
     */
    boolean existsByUserIdAndActiveTrue(Long userId);

    /**
     * Delete OAuth2 token by user ID
     */
    void deleteByUserId(Long userId);

    /**
     * Delete OAuth2 token by user ID and provider
     */
    void deleteByUserIdAndProvider(Long userId, String provider);
}
