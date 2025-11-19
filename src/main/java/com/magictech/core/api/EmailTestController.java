package com.magictech.core.api;

import com.magictech.core.notification.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Email Test Controller - Test and diagnose email configuration
 *
 * Endpoints:
 * - GET  /api/email/status  - Check email configuration status
 * - POST /api/email/test    - Send a test email
 */
@RestController
@RequestMapping("/api/email")
@CrossOrigin(origins = "*")
public class EmailTestController {

    private static final Logger log = LoggerFactory.getLogger(EmailTestController.class);

    @Autowired
    private EmailService emailService;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${app.email.from.address}")
    private String fromAddress;

    @Value("${spring.mail.host}")
    private String mailHost;

    @Value("${spring.mail.port}")
    private int mailPort;

    @Value("${spring.mail.username}")
    private String mailUsername;

    /**
     * Check email configuration status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getEmailStatus() {
        log.info("📧 Email configuration status check requested");

        Map<String, Object> status = new HashMap<>();

        // Check if email is enabled
        status.put("emailEnabled", emailEnabled);
        status.put("emailConfigured", emailService.isEmailConfigured());
        status.put("configurationMessage", emailService.getConfigurationStatus());

        // Configuration details (masked)
        Map<String, String> config = new HashMap<>();
        config.put("mailHost", mailHost);
        config.put("mailPort", String.valueOf(mailPort));
        config.put("fromAddress", fromAddress);
        config.put("username", mailUsername);
        config.put("isPlaceholder", fromAddress.equals("YOUR_EMAIL@gmail.com") ? "YES ⚠️" : "NO ✓");
        status.put("configuration", config);

        // Provide setup instructions if not configured
        if (!emailService.isEmailConfigured()) {
            status.put("setupInstructions", getSetupInstructions());
        }

        return ResponseEntity.ok(status);
    }

    /**
     * Send a test email
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> sendTestEmail(
            @RequestBody Map<String, String> request
    ) {
        String toEmail = request.get("email");

        if (toEmail == null || toEmail.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email address is required in request body as 'email'"));
        }

        log.info("📧 Test email requested to: {}", toEmail);

        // Check if email is configured
        if (!emailService.isEmailConfigured()) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
                    .body(Map.of(
                            "error", "Email is not configured properly",
                            "message", emailService.getConfigurationStatus(),
                            "setupInstructions", getSetupInstructions()
                    ));
        }

        try {
            // Send test email
            CompletableFuture<Boolean> result = emailService.sendNotificationEmail(
                    toEmail,
                    "🧪 MagicTech Email Test",
                    "Email Configuration Test",
                    "Congratulations! Your email notification system is working correctly.<br><br>" +
                            "<strong>From:</strong> " + fromAddress + "<br>" +
                            "<strong>SMTP Host:</strong> " + mailHost + ":" + mailPort + "<br><br>" +
                            "You can now receive notifications from MagicTech Management System.",
                    "NORMAL",
                    null
            );

            // Wait for async result (with timeout)
            boolean success = result.get(10, java.util.concurrent.TimeUnit.SECONDS);

            if (success) {
                log.info("✓ Test email sent successfully to: {}", toEmail);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Test email sent successfully! Check your inbox.",
                        "sentTo", toEmail
                ));
            } else {
                log.error("✗ Failed to send test email to: {}", toEmail);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of(
                                "success", false,
                                "error", "Email sending failed. Check server logs for details."
                        ));
            }

        } catch (Exception e) {
            log.error("✗ Exception while sending test email: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Exception: " + e.getMessage(),
                            "details", e.getClass().getSimpleName()
                    ));
        }
    }

    /**
     * Get Gmail setup instructions
     */
    private Map<String, Object> getSetupInstructions() {
        Map<String, Object> instructions = new HashMap<>();

        instructions.put("step1", "Go to your Google Account: https://myaccount.google.com/security");
        instructions.put("step2", "Enable 2-Step Verification (if not already enabled)");
        instructions.put("step3", "Go to App Passwords: https://myaccount.google.com/apppasswords");
        instructions.put("step4", "Select 'Mail' as the app and generate a password");
        instructions.put("step5", "Copy the 16-character password");
        instructions.put("step6", "Update application.properties with your Gmail and app password");
        instructions.put("step7", "Restart the application");

        instructions.put("configLocation", "src/main/resources/application.properties");
        instructions.put("requiredProperties", Map.of(
                "spring.mail.username", "your-email@gmail.com",
                "spring.mail.password", "your-16-character-app-password",
                "app.email.from.address", "your-email@gmail.com"
        ));

        return instructions;
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Email Test API",
                "emailConfigured", String.valueOf(emailService.isEmailConfigured())
        ));
    }
}
