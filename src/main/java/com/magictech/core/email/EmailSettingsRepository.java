package com.magictech.core.email;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for EmailSettings entity.
 */
@Repository
public interface EmailSettingsRepository extends JpaRepository<EmailSettings, Long> {

    /**
     * Find the active email settings
     */
    Optional<EmailSettings> findByIsActiveTrue();

    /**
     * Find settings by provider
     */
    Optional<EmailSettings> findByProvider(String provider);
}
