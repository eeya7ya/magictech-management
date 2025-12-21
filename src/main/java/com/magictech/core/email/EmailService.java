package com.magictech.core.email;

import com.magictech.core.auth.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Properties;

/**
 * Service for sending emails via SMTP.
 * Uses database-stored settings for SMTP configuration,
 * allowing users to configure email from the UI.
 */
@Service
public class EmailService {

    @Autowired
    private EmailSettingsService settingsService;

    /**
     * Check if email service is properly configured (from database settings)
     */
    public boolean isConfigured() {
        return settingsService.isConfigured();
    }

    /**
     * Get the active email settings
     */
    public Optional<EmailSettings> getSettings() {
        return settingsService.getActiveSettings();
    }

    /**
     * Get configuration status message
     */
    public String getConfigurationStatus() {
        Optional<EmailSettings> settings = settingsService.getActiveSettings();
        if (settings.isEmpty()) {
            return "No email settings configured. Click 'Email Settings' to set up.";
        }
        EmailSettings s = settings.get();
        if (s.getSmtpHost() == null || s.getSmtpHost().isEmpty()) {
            return "SMTP host not configured";
        }
        if (s.getUsername() == null || s.getUsername().isEmpty()) {
            return "Email username not configured";
        }
        if (s.getPassword() == null || s.getPassword().isEmpty()) {
            return "Email password not configured";
        }
        return "Email service is configured (" + s.getProvider() + ")";
    }

    /**
     * Get the configured mail host
     */
    public String getMailHost() {
        return settingsService.getActiveSettings()
                .map(EmailSettings::getSmtpHost)
                .orElse("Not configured");
    }

    /**
     * Create a JavaMailSender from database settings
     */
    private JavaMailSender createMailSender(EmailSettings settings) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(settings.getSmtpHost());
        mailSender.setPort(settings.getSmtpPort() != null ? settings.getSmtpPort() : 587);
        mailSender.setUsername(settings.getUsername());
        mailSender.setPassword(settings.getPassword());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");

        if (Boolean.TRUE.equals(settings.getUseTls())) {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }

        if (Boolean.TRUE.equals(settings.getUseSsl())) {
            props.put("mail.smtp.ssl.enable", "true");
        }

        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");
        props.put("mail.debug", "false");

        return mailSender;
    }

    /**
     * Send an HTML email using database settings
     */
    public void sendHtmlEmail(String to, String subject, String htmlContent) throws EmailException {
        Optional<EmailSettings> settingsOpt = settingsService.getActiveSettings();
        if (settingsOpt.isEmpty() || !settingsOpt.get().isComplete()) {
            throw new EmailException("Email service is not configured: " + getConfigurationStatus());
        }

        EmailSettings settings = settingsOpt.get();
        JavaMailSender mailSender = createMailSender(settings);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String fromAddress = settings.getFromAddress() != null && !settings.getFromAddress().isEmpty()
                    ? settings.getFromAddress()
                    : settings.getUsername();
            String fromName = settings.getFromName() != null && !settings.getFromName().isEmpty()
                    ? settings.getFromName()
                    : "MagicTech Management System";

            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            System.out.println("HTML email sent successfully to: " + to);
        } catch (MessagingException e) {
            throw new EmailException("Failed to send email: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new EmailException("Failed to send email: " + e.getMessage(), e);
        }
    }

    /**
     * Send a test email to verify email configuration
     */
    public TestEmailResult sendTestEmail(String to) {
        TestEmailResult result = new TestEmailResult();
        result.setRecipient(to);
        result.setTimestamp(LocalDateTime.now());

        Optional<EmailSettings> settingsOpt = settingsService.getActiveSettings();
        if (settingsOpt.isEmpty() || !settingsOpt.get().isComplete()) {
            result.setSuccess(false);
            result.setMessage("Email not configured. Please set up email settings first.");
            return result;
        }

        EmailSettings settings = settingsOpt.get();
        result.setSmtpHost(settings.getSmtpHost());

        String subject = "MagicTech - Test Email";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f4f4f4; }
                    .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 20px; border-radius: 10px 10px 0 0; margin: -30px -30px 30px -30px; text-align: center; }
                    .success-icon { font-size: 48px; margin-bottom: 10px; }
                    h1 { margin: 0; font-size: 24px; }
                    .content { color: #333; line-height: 1.6; }
                    .info-box { background-color: #e8f5e9; border-left: 4px solid #4caf50; padding: 15px; margin: 20px 0; border-radius: 0 5px 5px 0; }
                    .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; color: #666; font-size: 12px; text-align: center; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="success-icon">&#x2705;</div>
                        <h1>Email Configuration Test</h1>
                    </div>
                    <div class="content">
                        <p>Hello,</p>
                        <p>This is a test email from the <strong>MagicTech Management System</strong>.</p>
                        <div class="info-box">
                            <strong>Test Details:</strong><br>
                            Timestamp: %s<br>
                            Recipient: %s<br>
                            SMTP Host: %s<br>
                            Provider: %s
                        </div>
                        <p>If you received this email, your email configuration is working correctly!</p>
                    </div>
                    <div class="footer">
                        <p>This is an automated test email. Please do not reply.</p>
                        <p>&copy; MagicTech Management System</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(timestamp, to, settings.getSmtpHost(), settings.getProvider());

        try {
            sendHtmlEmail(to, subject, htmlContent);
            result.setSuccess(true);
            result.setMessage("Test email sent successfully to " + to);
        } catch (EmailException e) {
            result.setSuccess(false);
            result.setMessage(e.getMessage());
        }

        return result;
    }

    /**
     * Send a welcome email to a new user
     */
    public void sendWelcomeEmail(String to, String username) throws EmailException {
        String subject = "Welcome to MagicTech Management System";

        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f4f4f4; }
                    .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 20px; border-radius: 10px 10px 0 0; margin: -30px -30px 30px -30px; text-align: center; }
                    h1 { margin: 0; font-size: 24px; }
                    .content { color: #333; line-height: 1.6; }
                    .highlight { background-color: #f0f4ff; padding: 15px; border-radius: 5px; margin: 20px 0; }
                    .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; color: #666; font-size: 12px; text-align: center; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Welcome to MagicTech!</h1>
                    </div>
                    <div class="content">
                        <p>Hello <strong>%s</strong>,</p>
                        <p>Welcome to the MagicTech Management System! Your account has been created successfully.</p>
                        <div class="highlight">
                            <strong>Your username:</strong> %s
                        </div>
                        <p>You can now log in to the system and start using all available features based on your assigned role.</p>
                        <p>If you have any questions, please contact your system administrator.</p>
                    </div>
                    <div class="footer">
                        <p>&copy; MagicTech Management System</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(username, username);

        sendHtmlEmail(to, subject, htmlContent);
    }

    // ================================================
    // PER-USER EMAIL METHODS
    // ================================================

    /**
     * Create a JavaMailSender from a User's SMTP settings
     */
    private JavaMailSender createMailSenderForUser(User user) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(user.getSmtpHost());
        mailSender.setPort(user.getSmtpPort() != null ? user.getSmtpPort() : 587);
        mailSender.setUsername(user.getEmail());
        mailSender.setPassword(user.getSmtpPassword());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");
        props.put("mail.debug", "false");

        return mailSender;
    }

    /**
     * Check if a specific user has email configured
     */
    public boolean isUserEmailConfigured(User user) {
        return user != null && user.hasSmtpConfigured();
    }

    /**
     * Send an email FROM a specific user's account
     */
    public void sendEmailFromUser(User fromUser, String toEmail, String subject, String htmlContent) throws EmailException {
        if (!isUserEmailConfigured(fromUser)) {
            throw new EmailException("User '" + fromUser.getUsername() + "' has not configured email settings.");
        }

        JavaMailSender mailSender = createMailSenderForUser(fromUser);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromUser.getEmail(), fromUser.getUsername() + " (MagicTech)");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            System.out.println("Email sent from " + fromUser.getEmail() + " to " + toEmail);
        } catch (MessagingException e) {
            throw new EmailException("Failed to send email: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new EmailException("Failed to send email: " + e.getMessage(), e);
        }
    }

    /**
     * Send a test email using a specific user's SMTP settings
     */
    public TestEmailResult sendTestEmailForUser(User user) {
        TestEmailResult result = new TestEmailResult();
        result.setRecipient(user.getEmail());
        result.setTimestamp(LocalDateTime.now());

        if (!isUserEmailConfigured(user)) {
            result.setSuccess(false);
            result.setMessage("Email not configured for user '" + user.getUsername() + "'. Please configure SMTP settings.");
            return result;
        }

        result.setSmtpHost(user.getSmtpHost());

        String subject = "MagicTech - Email Test for " + user.getUsername();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f4f4f4; }
                    .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 20px; border-radius: 10px 10px 0 0; margin: -30px -30px 30px -30px; text-align: center; }
                    .success-icon { font-size: 48px; margin-bottom: 10px; }
                    h1 { margin: 0; font-size: 24px; }
                    .content { color: #333; line-height: 1.6; }
                    .info-box { background-color: #e8f5e9; border-left: 4px solid #4caf50; padding: 15px; margin: 20px 0; border-radius: 0 5px 5px 0; }
                    .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; color: #666; font-size: 12px; text-align: center; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="success-icon">&#x2705;</div>
                        <h1>Email Test Successful!</h1>
                    </div>
                    <div class="content">
                        <p>Hello <strong>%s</strong>,</p>
                        <p>Your email configuration is working correctly!</p>
                        <div class="info-box">
                            <strong>Configuration Details:</strong><br>
                            Username: %s<br>
                            Email: %s<br>
                            SMTP Host: %s<br>
                            Provider: %s<br>
                            Timestamp: %s
                        </div>
                        <p>You can now send emails to other users in the system.</p>
                    </div>
                    <div class="footer">
                        <p>This is an automated test email from MagicTech Management System.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                user.getUsername(),
                user.getUsername(),
                user.getEmail(),
                user.getSmtpHost(),
                user.getSmtpProvider() != null ? user.getSmtpProvider() : "Custom",
                timestamp
            );

        try {
            sendEmailFromUser(user, user.getEmail(), subject, htmlContent);
            result.setSuccess(true);
            result.setMessage("Test email sent successfully! Check your inbox at " + user.getEmail());
        } catch (EmailException e) {
            result.setSuccess(false);
            result.setMessage(e.getMessage());
        }

        return result;
    }

    /**
     * Result object for test email operations
     */
    public static class TestEmailResult {
        private boolean success;
        private String message;
        private String recipient;
        private String smtpHost;
        private LocalDateTime timestamp;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getRecipient() { return recipient; }
        public void setRecipient(String recipient) { this.recipient = recipient; }

        public String getSmtpHost() { return smtpHost; }
        public void setSmtpHost(String smtpHost) { this.smtpHost = smtpHost; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

        @Override
        public String toString() {
            return "TestEmailResult{success=" + success + ", message='" + message + "', recipient='" + recipient + "'}";
        }
    }
}
