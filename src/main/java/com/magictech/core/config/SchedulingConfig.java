package com.magictech.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration to enable scheduled tasks
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // Spring will automatically scan for @Scheduled methods
}
