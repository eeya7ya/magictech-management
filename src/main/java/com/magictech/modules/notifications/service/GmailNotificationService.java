package com.magictech.modules.notifications.service;

import com.magictech.core.auth.User;
import com.magictech.core.auth.UserRepository;
import com.magictech.modules.notifications.entity.Notification;
import com.magictech.modules.notifications.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for sending email notifications via Gmail
 */
@Service
public class GmailNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(GmailNotificationService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Value("${spring.mail.from:noreply@magictech.com}")
    private String fromEmail;

    @Value("${app.notifications.email.enabled:false}")
    private boolean emailNotificationsEnabled;

    /**
     * Send email notification asynchronously
     */
    @Async
    public void sendEmailNotification(Notification notification) {
        if (!emailNotificationsEnabled) {
            logger.debug("Email notifications disabled, skipping notification {}", notification.getId());
            notification.setIsSentEmail(true); // Mark as sent to avoid retry
            notificationRepository.save(notification);
            return;
        }

        if (mailSender == null) {
            logger.warn("MailSender not configured, cannot send email for notification {}", notification.getId());
            return;
        }

        try {
            Optional<User> userOpt = userRepository.findById(notification.getUserId());
            if (userOpt.isEmpty()) {
                logger.error("User {} not found for notification {}", notification.getUserId(), notification.getId());
                return;
            }

            User user = userOpt.get();
            String userEmail = user.getEmail();

            if (userEmail == null || userEmail.isBlank()) {
                logger.warn("User {} has no email configured, skipping email notification", user.getId());
                notification.setIsSentEmail(true); // Mark as sent to avoid retry
                notificationRepository.save(notification);
                return;
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(userEmail);
            message.setSubject(notification.getType().getIcon() + " " + notification.getTitle());
            message.setText(buildEmailBody(notification, user));

            mailSender.send(message);

            // Mark as sent
            notification.setIsSentEmail(true);
            notificationRepository.save(notification);

            logger.info("Sent email notification {} to {}", notification.getId(), userEmail);

        } catch (Exception e) {
            logger.error("Failed to send email for notification {}: {}",
                        notification.getId(), e.getMessage(), e);
        }
    }

    /**
     * Build email body from notification
     */
    private String buildEmailBody(Notification notification, User user) {
        StringBuilder body = new StringBuilder();

        body.append("Hello ").append(user.getUsername()).append(",\n\n");
        body.append(notification.getMessage()).append("\n\n");

        if (notification.getModuleSource() != null) {
            body.append("Module: ").append(notification.getModuleSource()).append("\n");
        }

        if (notification.getReferenceType() != null && notification.getReferenceId() != null) {
            body.append("Reference: ").append(notification.getReferenceType())
                .append(" #").append(notification.getReferenceId()).append("\n");
        }

        if (notification.getActionUrl() != null) {
            body.append("\nView details: ").append(notification.getActionUrl()).append("\n");
        }

        body.append("\n---\n");
        body.append("MagicTech Management System\n");
        body.append("Priority: ").append(notification.getPriority().getDisplayName()).append("\n");
        body.append("Time: ").append(notification.getCreatedAt()).append("\n");

        return body.toString();
    }

    /**
     * Send test email
     */
    public boolean sendTestEmail(String toEmail) {
        if (!emailNotificationsEnabled || mailSender == null) {
            logger.warn("Email not configured, cannot send test email");
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("🧪 MagicTech Test Email");
            message.setText("This is a test email from MagicTech Management System.\n\n" +
                          "Email notifications are working correctly!");

            mailSender.send(message);
            logger.info("Sent test email to {}", toEmail);
            return true;

        } catch (Exception e) {
            logger.error("Failed to send test email to {}: {}", toEmail, e.getMessage(), e);
            return false;
        }
    }
}
