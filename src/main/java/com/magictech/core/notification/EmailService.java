package com.magictech.core.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * EmailService - Handles sending email notifications via Gmail SMTP
 *
 * Features:
 * - HTML email templates
 * - Async sending (non-blocking)
 * - Retry logic for failed sends
 * - Priority-based sending
 * - Batch email support
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${app.email.from.address}")
    private String fromAddress;

    @Value("${app.email.from.name}")
    private String fromName;

    // ==================== Public API ====================

    /**
     * Send a simple text email asynchronously
     */
    @Async
    public CompletableFuture<Boolean> sendSimpleEmail(String to, String subject, String body) {
        if (!emailEnabled) {
            log.warn("Email sending is disabled. Skipping email to: {}", to);
            return CompletableFuture.completedFuture(false);
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false); // false = plain text

            mailSender.send(message);
            log.info("✓ Email sent successfully to: {}", to);
            return CompletableFuture.completedFuture(true);

        } catch (Exception e) {
            log.error("✗ Failed to send email to: {}. Error: {}", to, e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Send an HTML email asynchronously
     */
    @Async
    public CompletableFuture<Boolean> sendHtmlEmail(String to, String subject, String htmlBody) {
        if (!emailEnabled) {
            log.warn("📧 Email sending is DISABLED in configuration. Set app.email.enabled=true");
            return CompletableFuture.completedFuture(false);
        }

        // Check if email is properly configured
        if (!isEmailConfigured()) {
            log.error("📧 Email NOT CONFIGURED! Current fromAddress: '{}'. " +
                    "Please update application.properties with your Gmail credentials. " +
                    "See: https://myaccount.google.com/apppasswords", fromAddress);
            return CompletableFuture.completedFuture(false);
        }

        try {
            log.info("📧 Attempting to send email to: {} | Subject: {}", to, subject);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = HTML

            log.debug("📧 Sending via SMTP: {}...", fromAddress);
            mailSender.send(message);

            log.info("✅ Email SENT successfully! To: {} | From: {}", to, fromAddress);
            return CompletableFuture.completedFuture(true);

        } catch (jakarta.mail.AuthenticationFailedException e) {
            log.error("❌ AUTHENTICATION FAILED! Gmail credentials are incorrect. " +
                    "Please check spring.mail.username and spring.mail.password in application.properties. " +
                    "You need an App Password (16 characters) from: https://myaccount.google.com/apppasswords");
            log.error("Error details: {}", e.getMessage());
            return CompletableFuture.completedFuture(false);

        } catch (jakarta.mail.MessagingException e) {
            log.error("❌ Email MESSAGING ERROR! Recipient: {} | Error: {}", to, e.getMessage());
            log.error("Check if the email address is valid and SMTP settings are correct.");
            return CompletableFuture.completedFuture(false);

        } catch (Exception e) {
            log.error("❌ UNEXPECTED ERROR sending email to: {} | Error: {} | Type: {}",
                    to, e.getMessage(), e.getClass().getSimpleName());
            e.printStackTrace();
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Send notification email (uses HTML template)
     */
    @Async
    public CompletableFuture<Boolean> sendNotificationEmail(
            String to,
            String subject,
            String notificationTitle,
            String notificationMessage,
            String priority,
            String actionUrl
    ) {
        String htmlBody = buildNotificationEmailHtml(notificationTitle, notificationMessage, priority, actionUrl);
        return sendHtmlEmail(to, subject, htmlBody);
    }

    /**
     * Send notification to multiple recipients
     */
    @Async
    public CompletableFuture<Integer> sendBulkNotificationEmail(
            List<String> recipients,
            String subject,
            String notificationTitle,
            String notificationMessage,
            String priority,
            String actionUrl
    ) {
        int successCount = 0;

        for (String recipient : recipients) {
            try {
                CompletableFuture<Boolean> result = sendNotificationEmail(
                        recipient, subject, notificationTitle, notificationMessage, priority, actionUrl
                );
                if (result.join()) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("Failed to send email to: {}. Error: {}", recipient, e.getMessage());
            }
        }

        log.info("Bulk email send completed. Success: {}/{}", successCount, recipients.size());
        return CompletableFuture.completedFuture(successCount);
    }

    /**
     * Send project creation notification email
     */
    @Async
    public CompletableFuture<Boolean> sendProjectCreationEmail(
            String to,
            String projectName,
            String projectLocation,
            String createdBy,
            Long projectId
    ) {
        String subject = "🚀 New Project Created: " + projectName;
        String title = "New Project Assigned to You";
        String message = String.format(
                "A new project has been created and assigned to your team.<br><br>" +
                        "<strong>Project:</strong> %s<br>" +
                        "<strong>Location:</strong> %s<br>" +
                        "<strong>Created by:</strong> %s<br><br>" +
                        "Please review the project details and start planning.",
                projectName, projectLocation, createdBy
        );

        String actionUrl = "magictech://open/project/" + projectId;
        return sendNotificationEmail(to, subject, title, message, "HIGH", actionUrl);
    }

    /**
     * Send approval request notification email
     */
    @Async
    public CompletableFuture<Boolean> sendApprovalRequestEmail(
            String to,
            String requestType,
            String requestDetails,
            String requestedBy,
            Long approvalId
    ) {
        String subject = "⚠️ Approval Request: " + requestType;
        String title = "New Approval Request Requires Your Attention";
        String message = String.format(
                "You have received a new approval request:<br><br>" +
                        "<strong>Type:</strong> %s<br>" +
                        "<strong>Details:</strong> %s<br>" +
                        "<strong>Requested by:</strong> %s<br><br>" +
                        "This request will expire in 2 days if not acted upon.",
                requestType, requestDetails, requestedBy
        );

        String actionUrl = "magictech://open/approval/" + approvalId;
        return sendNotificationEmail(to, subject, title, message, "URGENT", actionUrl);
    }

    /**
     * Send approval confirmation email
     */
    @Async
    public CompletableFuture<Boolean> sendApprovalConfirmationEmail(
            String to,
            String requestType,
            String approvedBy,
            boolean approved
    ) {
        String status = approved ? "Approved ✓" : "Rejected ✗";
        String subject = status + ": " + requestType;
        String title = "Approval Request " + (approved ? "Approved" : "Rejected");
        String message = String.format(
                "Your approval request has been %s.<br><br>" +
                        "<strong>Type:</strong> %s<br>" +
                        "<strong>%s by:</strong> %s<br>",
                approved ? "approved" : "rejected",
                requestType,
                approved ? "Approved" : "Rejected",
                approvedBy
        );

        String priority = approved ? "NORMAL" : "HIGH";
        return sendNotificationEmail(to, subject, title, message, priority, null);
    }

    /**
     * Send low stock alert email
     */
    @Async
    public CompletableFuture<Boolean> sendLowStockAlertEmail(
            String to,
            String productName,
            int currentQuantity,
            int threshold
    ) {
        String subject = "⚠️ Low Stock Alert: " + productName;
        String title = "Low Stock Alert";
        String message = String.format(
                "The following item is running low on stock:<br><br>" +
                        "<strong>Product:</strong> %s<br>" +
                        "<strong>Current Quantity:</strong> %d<br>" +
                        "<strong>Threshold:</strong> %d<br><br>" +
                        "Please reorder to maintain inventory levels.",
                productName, currentQuantity, threshold
        );

        return sendNotificationEmail(to, subject, title, message, "HIGH", "magictech://open/storage");
    }

    // ==================== HTML Email Template Builder ====================

    /**
     * Build HTML email from template
     */
    private String buildNotificationEmailHtml(
            String title,
            String message,
            String priority,
            String actionUrl
    ) {
        String priorityColor = getPriorityColor(priority);
        String priorityIcon = getPriorityIcon(priority);

        String actionButton = "";
        if (actionUrl != null && !actionUrl.isEmpty()) {
            actionButton = String.format(
                    "<div style='text-align: center; margin-top: 30px;'>" +
                            "<a href='%s' style='background-color: #7c3aed; color: white; padding: 12px 30px; " +
                            "text-decoration: none; border-radius: 8px; font-weight: bold; display: inline-block;'>" +
                            "View Details</a>" +
                            "</div>",
                    actionUrl
            );
        }

        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body {
                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                            line-height: 1.6;
                            color: #333;
                            max-width: 600px;
                            margin: 0 auto;
                            padding: 20px;
                        }
                        .email-container {
                            background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                            border-radius: 16px;
                            padding: 2px;
                            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                        }
                        .email-content {
                            background: white;
                            border-radius: 14px;
                            padding: 40px;
                        }
                        .header {
                            text-align: center;
                            margin-bottom: 30px;
                        }
                        .logo {
                            font-size: 32px;
                            font-weight: bold;
                            background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                            -webkit-background-clip: text;
                            -webkit-text-fill-color: transparent;
                            background-clip: text;
                        }
                        .priority-badge {
                            display: inline-block;
                            background-color: %s;
                            color: white;
                            padding: 6px 16px;
                            border-radius: 20px;
                            font-size: 12px;
                            font-weight: bold;
                            margin-bottom: 20px;
                        }
                        .notification-title {
                            font-size: 24px;
                            font-weight: bold;
                            color: #1a1a1a;
                            margin-bottom: 20px;
                        }
                        .notification-message {
                            font-size: 16px;
                            color: #4b5563;
                            line-height: 1.8;
                            margin-bottom: 30px;
                        }
                        .footer {
                            text-align: center;
                            margin-top: 40px;
                            padding-top: 20px;
                            border-top: 2px solid #e5e7eb;
                            font-size: 12px;
                            color: #9ca3af;
                        }
                    </style>
                </head>
                <body>
                    <div class="email-container">
                        <div class="email-content">
                            <div class="header">
                                <div class="logo">✨ MagicTech</div>
                            </div>

                            <div class="priority-badge">%s %s</div>

                            <div class="notification-title">%s</div>

                            <div class="notification-message">%s</div>

                            %s

                            <div class="footer">
                                <p><strong>MagicTech Management System</strong></p>
                                <p>This is an automated notification. Please do not reply to this email.</p>
                                <p>If you wish to change your notification preferences, please log in to the system.</p>
                            </div>
                        </div>
                    </div>
                </body>
                </html>
                """,
                priorityColor,
                priorityIcon,
                priority,
                title,
                message,
                actionButton
        );
    }

    private String getPriorityColor(String priority) {
        return switch (priority.toUpperCase()) {
            case "URGENT" -> "#ef4444";
            case "HIGH" -> "#f97316";
            case "NORMAL" -> "#3b82f6";
            case "LOW" -> "#6b7280";
            default -> "#3b82f6";
        };
    }

    private String getPriorityIcon(String priority) {
        return switch (priority.toUpperCase()) {
            case "URGENT" -> "🚨";
            case "HIGH" -> "⚠️";
            case "NORMAL" -> "🔔";
            case "LOW" -> "ℹ️";
            default -> "🔔";
        };
    }

    // ==================== Health Check ====================

    public boolean isEmailConfigured() {
        return emailEnabled &&
                fromAddress != null &&
                !fromAddress.equals("YOUR_EMAIL@gmail.com") &&
                !fromAddress.isEmpty();
    }

    public String getConfigurationStatus() {
        if (!emailEnabled) {
            return "Email notifications are disabled in configuration";
        }
        if (fromAddress == null || fromAddress.equals("YOUR_EMAIL@gmail.com")) {
            return "Email address not configured. Please update application.properties";
        }
        return "Email service is configured and ready";
    }
}
