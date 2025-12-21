package com.magictech.core.email;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for sending emails via SMTP.
 * Provides methods for sending simple text emails and HTML emails,
 * as well as test email functionality.
 */
@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${magictech.mail.from:noreply@magictech.com}")
    private String fromAddress;

    @Value("${magictech.mail.from-name:MagicTech Management System}")
    private String fromName;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    /**
     * Check if email service is properly configured
     */
    public boolean isConfigured() {
        return mailSender != null &&
               mailHost != null && !mailHost.isEmpty() &&
               mailUsername != null && !mailUsername.isEmpty();
    }

    /**
     * Get configuration status message
     */
    public String getConfigurationStatus() {
        if (mailSender == null) {
            return "Mail sender not initialized";
        }
        if (mailHost == null || mailHost.isEmpty()) {
            return "SMTP host not configured (spring.mail.host)";
        }
        if (mailUsername == null || mailUsername.isEmpty()) {
            return "SMTP username not configured (spring.mail.username)";
        }
        return "Email service is configured";
    }

    /**
     * Send a simple text email
     *
     * @param to Recipient email address
     * @param subject Email subject
     * @param text Email body (plain text)
     * @throws EmailException if sending fails
     */
    public void sendSimpleEmail(String to, String subject, String text) throws EmailException {
        if (!isConfigured()) {
            throw new EmailException("Email service is not configured: " + getConfigurationStatus());
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
            System.out.println("Email sent successfully to: " + to);
        } catch (Exception e) {
            throw new EmailException("Failed to send email to " + to + ": " + e.getMessage(), e);
        }
    }

    /**
     * Send an HTML email
     *
     * @param to Recipient email address
     * @param subject Email subject
     * @param htmlContent Email body (HTML)
     * @throws EmailException if sending fails
     */
    public void sendHtmlEmail(String to, String subject, String htmlContent) throws EmailException {
        if (!isConfigured()) {
            throw new EmailException("Email service is not configured: " + getConfigurationStatus());
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            System.out.println("HTML email sent successfully to: " + to);
        } catch (MessagingException e) {
            throw new EmailException("Failed to send HTML email to " + to + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new EmailException("Failed to send HTML email to " + to + ": " + e.getMessage(), e);
        }
    }

    /**
     * Send a test email to verify email configuration
     *
     * @param to Recipient email address
     * @return TestEmailResult containing success status and details
     */
    public TestEmailResult sendTestEmail(String to) {
        TestEmailResult result = new TestEmailResult();
        result.setRecipient(to);
        result.setTimestamp(LocalDateTime.now());

        if (!isConfigured()) {
            result.setSuccess(false);
            result.setMessage("Email service is not configured: " + getConfigurationStatus());
            return result;
        }

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
                            SMTP Host: %s
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
            """.formatted(timestamp, to, mailHost);

        try {
            sendHtmlEmail(to, subject, htmlContent);
            result.setSuccess(true);
            result.setMessage("Test email sent successfully to " + to);
            result.setSmtpHost(mailHost);
        } catch (EmailException e) {
            result.setSuccess(false);
            result.setMessage(e.getMessage());
            result.setSmtpHost(mailHost);
        }

        return result;
    }

    /**
     * Send a welcome email to a new user
     *
     * @param to Recipient email address
     * @param username The user's username
     * @throws EmailException if sending fails
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
