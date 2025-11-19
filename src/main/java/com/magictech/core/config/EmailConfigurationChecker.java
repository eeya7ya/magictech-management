package com.magictech.core.config;

import com.magictech.core.notification.EmailService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Email Configuration Checker - Validates email configuration on startup
 *
 * This component runs automatically when the application starts and:
 * 1. Checks if email is properly configured
 * 2. Displays clear warnings if not configured
 * 3. Provides setup instructions
 */
@Component
public class EmailConfigurationChecker {

    private static final Logger log = LoggerFactory.getLogger(EmailConfigurationChecker.class);

    @Autowired
    private EmailService emailService;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${app.email.from.address}")
    private String fromAddress;

    @Value("${spring.mail.username}")
    private String mailUsername;

    @Value("${spring.mail.host}")
    private String mailHost;

    @Value("${spring.mail.port}")
    private int mailPort;

    /**
     * Run email configuration check on application startup
     */
    @PostConstruct
    public void checkEmailConfiguration() {
        log.info("========================================");
        log.info("📧 EMAIL CONFIGURATION CHECK");
        log.info("========================================");

        if (!emailEnabled) {
            log.warn("⚠️  Email notifications are DISABLED");
            log.warn("   Set app.email.enabled=true to enable email notifications");
            log.info("========================================");
            return;
        }

        if (!emailService.isEmailConfigured()) {
            displayConfigurationWarning();
        } else {
            displayConfigurationSuccess();
        }

        log.info("========================================");
    }

    /**
     * Display warning if email is not configured
     */
    private void displayConfigurationWarning() {
        log.error("❌ EMAIL NOT CONFIGURED!");
        log.error("");
        log.error("   Current Configuration:");
        log.error("   • Host: {}", mailHost);
        log.error("   • Port: {}", mailPort);
        log.error("   • Username: {}", mailUsername);
        log.error("   • From Address: {}", fromAddress);
        log.error("");

        if (fromAddress.equals("YOUR_EMAIL@gmail.com")) {
            log.error("   ⚠️  You are using PLACEHOLDER values!");
            log.error("");
            log.error("   📋 GMAIL SETUP INSTRUCTIONS:");
            log.error("   ┌─────────────────────────────────────────────────────");
            log.error("   │ 1. Go to: https://myaccount.google.com/security");
            log.error("   │ 2. Enable 2-Step Verification (if not enabled)");
            log.error("   │ 3. Go to: https://myaccount.google.com/apppasswords");
            log.error("   │ 4. Create an App Password for 'Mail'");
            log.error("   │ 5. Copy the 16-character password");
            log.error("   │");
            log.error("   │ 6. Edit: src/main/resources/application.properties");
            log.error("   │    Replace:");
            log.error("   │      spring.mail.username=YOUR_EMAIL@gmail.com");
            log.error("   │      spring.mail.password=YOUR_APP_PASSWORD_HERE");
            log.error("   │      app.email.from.address=YOUR_EMAIL@gmail.com");
            log.error("   │");
            log.error("   │    With your actual Gmail and app password:");
            log.error("   │      spring.mail.username=yourname@gmail.com");
            log.error("   │      spring.mail.password=abcd efgh ijkl mnop");
            log.error("   │      app.email.from.address=yourname@gmail.com");
            log.error("   │");
            log.error("   │ 7. Restart the application");
            log.error("   │");
            log.error("   │ 8. Test email: curl -X POST http://localhost:8085/api/email/test \\");
            log.error("   │                  -H \"Content-Type: application/json\" \\");
            log.error("   │                  -d '{{\"email\":\"your-email@gmail.com\"}}'");
            log.error("   └─────────────────────────────────────────────────────");
            log.error("");
            log.error("   📍 Quick Test API:");
            log.error("      GET  http://localhost:8085/api/email/status");
            log.error("      POST http://localhost:8085/api/email/test");
        }

        log.error("");
        log.error("   ⚠️  Notifications will be stored but NO EMAILS will be sent!");
    }

    /**
     * Display success message if email is configured
     */
    private void displayConfigurationSuccess() {
        log.info("✅ Email Configuration: OK");
        log.info("");
        log.info("   Configuration:");
        log.info("   • Host: {}", mailHost);
        log.info("   • Port: {}", mailPort);
        log.info("   • From: {}", fromAddress);
        log.info("");
        log.info("   Status: Ready to send email notifications");
        log.info("");
        log.info("   📍 Test Email API:");
        log.info("      GET  http://localhost:8085/api/email/status");
        log.info("      POST http://localhost:8085/api/email/test");
    }
}
