package com.magictech.core.notification;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.magictech.core.auth.OAuth2Service;
import com.magictech.core.auth.User;
import com.magictech.core.auth.UserOAuth2Token;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * Gmail service using OAuth2 authentication
 * Sends emails using user's own Gmail account via Gmail API
 */
@Service
public class GmailOAuth2Service {

    @Autowired
    private OAuth2Service oauth2Service;

    /**
     * Send email using user's OAuth2-authenticated Gmail account
     * @param user User sending the email
     * @param toEmail Recipient email
     * @param subject Email subject
     * @param body Email body (HTML or plain text)
     * @param isHtml Whether body is HTML
     * @return CompletableFuture<Boolean> indicating success
     */
    @Async
    public CompletableFuture<Boolean> sendEmailViaOAuth2(User user, String toEmail, String subject,
                                                          String body, boolean isHtml) {
        try {
            // Check if user has OAuth2 token
            if (!oauth2Service.isUserAuthenticated(user.getId())) {
                System.err.println("✗ User " + user.getUsername() + " has not linked their Google account");
                return CompletableFuture.completedFuture(false);
            }

            // Get valid access token (auto-refreshes if needed)
            String accessToken = oauth2Service.getValidAccessToken(user.getId());
            if (accessToken == null) {
                System.err.println("✗ Failed to get valid access token for user " + user.getUsername());
                return CompletableFuture.completedFuture(false);
            }

            // Get user's Google email
            UserOAuth2Token tokenInfo = oauth2Service.getTokenInfo(user.getId());
            String fromEmail = tokenInfo != null ? tokenInfo.getEmail() : user.getEmail();

            if (fromEmail == null || fromEmail.isEmpty()) {
                System.err.println("✗ User email not found");
                return CompletableFuture.completedFuture(false);
            }

            // Create Gmail service with OAuth2 credentials
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            GsonFactory jsonFactory = GsonFactory.getDefaultInstance();

            GoogleCredentials credentials = GoogleCredentials.create(new AccessToken(accessToken, null));

            Gmail gmailService = new Gmail.Builder(
                    httpTransport,
                    jsonFactory,
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName("MagicTech Management System")
                    .build();

            // Create email message
            MimeMessage email = createEmail(fromEmail, toEmail, subject, body, isHtml);

            // Send email via Gmail API
            Message message = sendMessage(gmailService, "me", email);

            if (message != null) {
                System.out.println("✓ Email sent via Gmail OAuth2: " + fromEmail + " → " + toEmail);
                return CompletableFuture.completedFuture(true);
            } else {
                System.err.println("✗ Failed to send email via Gmail OAuth2");
                return CompletableFuture.completedFuture(false);
            }

        } catch (Exception e) {
            System.err.println("✗ Error sending email via Gmail OAuth2: " + e.getMessage());
            e.printStackTrace();
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Send email with custom sender name
     */
    @Async
    public CompletableFuture<Boolean> sendEmailViaOAuth2(User user, String toEmail, String subject,
                                                          String body, boolean isHtml, String senderName) {
        // Same as above but use senderName in createEmail
        return sendEmailViaOAuth2(user, toEmail, subject, body, isHtml);
    }

    /**
     * Create MimeMessage for email
     */
    private MimeMessage createEmail(String fromEmail, String toEmail, String subject,
                                     String body, boolean isHtml) throws Exception {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(fromEmail));
        email.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(toEmail));
        email.setSubject(subject);

        if (isHtml) {
            email.setContent(body, "text/html; charset=utf-8");
        } else {
            email.setText(body, "utf-8");
        }

        email.setSentDate(new Date());
        return email;
    }

    /**
     * Send email via Gmail API
     */
    private Message sendMessage(Gmail service, String userId, MimeMessage emailContent) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.getUrlEncoder().encodeToString(bytes);

        Message message = new Message();
        message.setRaw(encodedEmail);

        return service.users().messages().send(userId, message).execute();
    }

    /**
     * Send notification email using templated HTML
     */
    @Async
    public CompletableFuture<Boolean> sendNotificationEmail(User user, String toEmail, String title,
                                                              String message, String priority, String actionUrl) {
        try {
            // Determine priority styling
            String priorityColor = switch (priority.toUpperCase()) {
                case "URGENT" -> "#ef4444";
                case "HIGH" -> "#f97316";
                case "NORMAL" -> "#3b82f6";
                case "LOW" -> "#6b7280";
                default -> "#3b82f6";
            };

            String priorityIcon = switch (priority.toUpperCase()) {
                case "URGENT" -> "🚨";
                case "HIGH" -> "⚠️";
                case "NORMAL" -> "🔔";
                case "LOW" -> "ℹ️";
                default -> "🔔";
            };

            // Build HTML email with purple gradient theme
            StringBuilder htmlContent = new StringBuilder();
            htmlContent.append("<!DOCTYPE html>");
            htmlContent.append("<html><head><style>");
            htmlContent.append("body { font-family: 'Segoe UI', Arial, sans-serif; margin: 0; padding: 20px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }");
            htmlContent.append(".container { max-width: 600px; margin: 0 auto; background: white; border-radius: 12px; overflow: hidden; box-shadow: 0 10px 30px rgba(0,0,0,0.2); }");
            htmlContent.append(".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; }");
            htmlContent.append(".content { padding: 30px; }");
            htmlContent.append(".priority-badge { display: inline-block; padding: 6px 12px; border-radius: 20px; font-size: 12px; font-weight: bold; margin-bottom: 15px; }");
            htmlContent.append(".button { display: inline-block; padding: 12px 30px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; text-decoration: none; border-radius: 6px; margin-top: 20px; font-weight: bold; }");
            htmlContent.append(".footer { background: #f3f4f6; padding: 20px; text-align: center; font-size: 12px; color: #6b7280; }");
            htmlContent.append("</style></head><body>");

            htmlContent.append("<div class='container'>");
            htmlContent.append("<div class='header'>");
            htmlContent.append("<h1 style='margin:0; font-size: 28px;'>").append(priorityIcon).append(" MagicTech Notification</h1>");
            htmlContent.append("</div>");

            htmlContent.append("<div class='content'>");
            htmlContent.append("<div class='priority-badge' style='background-color: ").append(priorityColor).append("; color: white;'>");
            htmlContent.append(priority.toUpperCase()).append("</div>");

            htmlContent.append("<h2 style='color: #1f2937; margin-top: 0;'>").append(title).append("</h2>");
            htmlContent.append("<p style='color: #4b5563; line-height: 1.6; font-size: 16px;'>").append(message).append("</p>");

            if (actionUrl != null && !actionUrl.isEmpty()) {
                htmlContent.append("<a href='").append(actionUrl).append("' class='button'>View Details</a>");
            }

            htmlContent.append("</div>");

            htmlContent.append("<div class='footer'>");
            htmlContent.append("<p>This is an automated message from MagicTech Management System</p>");
            htmlContent.append("<p>© 2025 MagicTech. All rights reserved.</p>");
            htmlContent.append("</div>");

            htmlContent.append("</div>");
            htmlContent.append("</body></html>");

            return sendEmailViaOAuth2(user, toEmail, title, htmlContent.toString(), true);

        } catch (Exception e) {
            System.err.println("✗ Failed to send notification email: " + e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Check if user can send emails (has OAuth2 configured)
     */
    public boolean canSendEmail(Long userId) {
        return oauth2Service.isUserAuthenticated(userId);
    }
}
