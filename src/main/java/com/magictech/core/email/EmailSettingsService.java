package com.magictech.core.email;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service for managing email settings.
 */
@Service
@Transactional
public class EmailSettingsService {

    @Autowired
    private EmailSettingsRepository repository;

    /**
     * Get the active email settings
     */
    public Optional<EmailSettings> getActiveSettings() {
        return repository.findByIsActiveTrue();
    }

    /**
     * Save or update email settings
     */
    public EmailSettings saveSettings(EmailSettings settings) {
        // Deactivate any existing active settings
        repository.findByIsActiveTrue().ifPresent(existing -> {
            if (!existing.getId().equals(settings.getId())) {
                existing.setIsActive(false);
                repository.save(existing);
            }
        });

        settings.setIsActive(true);
        return repository.save(settings);
    }

    /**
     * Create default settings for a provider
     */
    public EmailSettings createDefaultSettings(String provider) {
        EmailSettings settings = new EmailSettings();
        settings.setProvider(provider);
        settings.setUseTls(true);
        settings.setUseSsl(false);
        settings.setSmtpPort(587);

        switch (provider.toLowerCase()) {
            case "gmail" -> {
                settings.setSmtpHost("smtp.gmail.com");
                settings.setFromName("MagicTech Management System");
            }
            case "outlook" -> {
                settings.setSmtpHost("smtp.office365.com");
                settings.setFromName("MagicTech Management System");
            }
            case "hotmail" -> {
                settings.setSmtpHost("smtp-mail.outlook.com");
                settings.setFromName("MagicTech Management System");
            }
            default -> {
                settings.setSmtpHost("");
                settings.setFromName("MagicTech Management System");
            }
        }

        return settings;
    }

    /**
     * Test if settings are configured
     */
    public boolean isConfigured() {
        return getActiveSettings()
                .map(EmailSettings::isComplete)
                .orElse(false);
    }

    /**
     * Delete settings
     */
    public void deleteSettings(Long id) {
        repository.deleteById(id);
    }
}
