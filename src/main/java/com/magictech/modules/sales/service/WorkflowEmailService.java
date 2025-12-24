package com.magictech.modules.sales.service;

import com.magictech.core.auth.User;
import com.magictech.core.email.EmailException;
import com.magictech.core.email.EmailService;
import com.magictech.modules.projects.entity.Project;
import com.magictech.modules.sales.entity.WorkflowStepCompletion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for sending workflow-related email notifications.
 * Handles email notifications for workflow step assignments and completions.
 * Emails are sent using the recipient's SMTP configuration.
 */
@Service
public class WorkflowEmailService {

    @Autowired
    private EmailService emailService;

    @Autowired
    private WorkflowStepService workflowStepService;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Send email notification when a step is assigned to a user.
     * Uses the RECIPIENT's SMTP configuration to send the email.
     *
     * @param step The workflow step being assigned
     * @param assignedUser The user being assigned (email recipient)
     * @param assignedBy The user making the assignment
     * @param project The project this workflow belongs to
     * @return true if email was sent successfully, false otherwise
     */
    public boolean sendStepAssignmentEmail(WorkflowStepCompletion step, User assignedUser,
                                           User assignedBy, Project project) {
        if (!emailService.isUserEmailConfigured(assignedUser)) {
            System.out.println("Cannot send assignment email - recipient " +
                             assignedUser.getUsername() + " has no SMTP configured");
            return false;
        }

        String subject = getAssignmentEmailSubject(step, project);
        String htmlContent = buildAssignmentEmailHtml(step, assignedUser, assignedBy, project);

        try {
            // Send FROM system using recipient's configured SMTP
            // This sends TO the assignedUser's email using their own SMTP settings
            emailService.sendEmailFromUser(assignedUser, assignedUser.getEmail(), subject, htmlContent);

            // Mark email as sent on the step
            workflowStepService.markEmailSent(step);

            System.out.println("Assignment email sent to " + assignedUser.getEmail() +
                             " for step " + step.getStepNumber());
            return true;
        } catch (EmailException e) {
            System.err.println("Failed to send assignment email: " + e.getMessage());
            return false;
        }
    }

    /**
     * Send email notification when a step is completed.
     * Notifies the workflow owner that the assigned user has completed their work.
     *
     * @param step The completed workflow step
     * @param completedBy The user who completed the step
     * @param workflowOwner The owner of the workflow (to receive notification)
     * @param project The project
     * @return true if email was sent successfully
     */
    public boolean sendStepCompletionEmail(WorkflowStepCompletion step, User completedBy,
                                           User workflowOwner, Project project) {
        if (!emailService.isUserEmailConfigured(workflowOwner)) {
            System.out.println("Cannot send completion email - owner " +
                             workflowOwner.getUsername() + " has no SMTP configured");
            return false;
        }

        String subject = getCompletionEmailSubject(step, project);
        String htmlContent = buildCompletionEmailHtml(step, completedBy, workflowOwner, project);

        try {
            emailService.sendEmailFromUser(workflowOwner, workflowOwner.getEmail(), subject, htmlContent);
            System.out.println("Completion email sent to " + workflowOwner.getEmail() +
                             " for step " + step.getStepNumber());
            return true;
        } catch (EmailException e) {
            System.err.println("Failed to send completion email: " + e.getMessage());
            return false;
        }
    }

    // ============================================================
    // EMAIL SUBJECT GENERATORS
    // ============================================================

    private String getAssignmentEmailSubject(WorkflowStepCompletion step, Project project) {
        String stepName = workflowStepService.getStepDisplayName(step.getStepNumber());
        return switch (step.getStepNumber()) {
            case 1 -> "New Site Survey Request - " + project.getProjectName();
            case 2 -> "Sizing/Design Required - " + project.getProjectName();
            case 3 -> "Bank Guarantee Required - " + project.getProjectName();
            case 4 -> "Missing Items Check - " + project.getProjectName();
            case 6 -> "Project Ready for Execution - " + project.getProjectName();
            case 7 -> "After-Sales Support Required - " + project.getProjectName();
            case 8 -> "Project Ready for Final Review - " + project.getProjectName();
            default -> stepName + " - " + project.getProjectName();
        };
    }

    private String getCompletionEmailSubject(WorkflowStepCompletion step, Project project) {
        String stepName = workflowStepService.getStepDisplayName(step.getStepNumber());
        return stepName + " Completed - " + project.getProjectName();
    }

    // ============================================================
    // EMAIL HTML BUILDERS
    // ============================================================

    private String buildAssignmentEmailHtml(WorkflowStepCompletion step, User assignedUser,
                                            User assignedBy, Project project) {
        String stepName = workflowStepService.getStepDisplayName(step.getStepNumber());
        String timestamp = LocalDateTime.now().format(DATE_FORMAT);
        String actionDescription = getStepActionDescription(step.getStepNumber());
        String headerColor = getStepHeaderColor(step.getStepNumber());

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f4f4f4; }
                    .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { background: %s; color: white; padding: 20px; border-radius: 10px 10px 0 0; margin: -30px -30px 30px -30px; text-align: center; }
                    .step-badge { background: rgba(255,255,255,0.2); padding: 5px 15px; border-radius: 20px; display: inline-block; margin-bottom: 10px; }
                    h1 { margin: 0; font-size: 24px; }
                    .content { color: #333; line-height: 1.6; }
                    .info-box { background-color: #f0f7ff; border-left: 4px solid #2196F3; padding: 15px; margin: 20px 0; border-radius: 0 5px 5px 0; }
                    .action-box { background-color: #fff3e0; border-left: 4px solid #ff9800; padding: 15px; margin: 20px 0; border-radius: 0 5px 5px 0; }
                    .project-details { background-color: #f5f5f5; padding: 15px; border-radius: 5px; margin: 20px 0; }
                    .project-details table { width: 100%%; border-collapse: collapse; }
                    .project-details td { padding: 8px 0; }
                    .project-details td:first-child { font-weight: bold; width: 40%%; }
                    .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; color: #666; font-size: 12px; text-align: center; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="step-badge">Step %d of 8</div>
                        <h1>%s</h1>
                    </div>
                    <div class="content">
                        <p>Hello <strong>%s</strong>,</p>

                        <p><strong>%s</strong> from the Sales team has assigned you a new task for the project below.</p>

                        <div class="action-box">
                            <strong>Action Required:</strong><br>
                            %s
                        </div>

                        <div class="project-details">
                            <h3 style="margin-top: 0;">Project Details</h3>
                            <table>
                                <tr>
                                    <td>Project Name:</td>
                                    <td>%s</td>
                                </tr>
                                <tr>
                                    <td>Location:</td>
                                    <td>%s</td>
                                </tr>
                                <tr>
                                    <td>Assigned By:</td>
                                    <td>%s</td>
                                </tr>
                                <tr>
                                    <td>Assigned At:</td>
                                    <td>%s</td>
                                </tr>
                            </table>
                        </div>

                        <div class="info-box">
                            <strong>Next Steps:</strong><br>
                            Please log in to the MagicTech Management System to view the full project details and complete your assigned task.
                        </div>
                    </div>
                    <div class="footer">
                        <p>This is an automated notification from MagicTech Management System.</p>
                        <p>&copy; MagicTech Management System</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                headerColor,
                step.getStepNumber(),
                stepName,
                assignedUser.getUsername(),
                assignedBy.getUsername(),
                actionDescription,
                project.getProjectName(),
                project.getProjectLocation() != null ? project.getProjectLocation() : "Not specified",
                assignedBy.getUsername(),
                timestamp
            );
    }

    private String buildCompletionEmailHtml(WorkflowStepCompletion step, User completedBy,
                                            User workflowOwner, Project project) {
        String stepName = workflowStepService.getStepDisplayName(step.getStepNumber());
        String timestamp = LocalDateTime.now().format(DATE_FORMAT);
        String completionMessage = getStepCompletionMessage(step.getStepNumber());

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f4f4f4; }
                    .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #4CAF50 0%%, #45a049 100%%); color: white; padding: 20px; border-radius: 10px 10px 0 0; margin: -30px -30px 30px -30px; text-align: center; }
                    .success-icon { font-size: 48px; margin-bottom: 10px; }
                    h1 { margin: 0; font-size: 24px; }
                    .content { color: #333; line-height: 1.6; }
                    .success-box { background-color: #e8f5e9; border-left: 4px solid #4caf50; padding: 15px; margin: 20px 0; border-radius: 0 5px 5px 0; }
                    .project-details { background-color: #f5f5f5; padding: 15px; border-radius: 5px; margin: 20px 0; }
                    .project-details table { width: 100%%; border-collapse: collapse; }
                    .project-details td { padding: 8px 0; }
                    .project-details td:first-child { font-weight: bold; width: 40%%; }
                    .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; color: #666; font-size: 12px; text-align: center; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="success-icon">&#x2705;</div>
                        <h1>Step Completed: %s</h1>
                    </div>
                    <div class="content">
                        <p>Hello <strong>%s</strong>,</p>

                        <p>Good news! <strong>%s</strong> has completed their assigned task.</p>

                        <div class="success-box">
                            <strong>Completed:</strong><br>
                            %s
                        </div>

                        <div class="project-details">
                            <h3 style="margin-top: 0;">Project Details</h3>
                            <table>
                                <tr>
                                    <td>Project Name:</td>
                                    <td>%s</td>
                                </tr>
                                <tr>
                                    <td>Step Completed:</td>
                                    <td>%s (Step %d of 8)</td>
                                </tr>
                                <tr>
                                    <td>Completed By:</td>
                                    <td>%s</td>
                                </tr>
                                <tr>
                                    <td>Completed At:</td>
                                    <td>%s</td>
                                </tr>
                            </table>
                        </div>

                        <p>Please log in to the MagicTech Management System to review and proceed with the next step.</p>
                    </div>
                    <div class="footer">
                        <p>This is an automated notification from MagicTech Management System.</p>
                        <p>&copy; MagicTech Management System</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                stepName,
                workflowOwner.getUsername(),
                completedBy.getUsername(),
                completionMessage,
                project.getProjectName(),
                stepName,
                step.getStepNumber(),
                completedBy.getUsername(),
                timestamp
            );
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    private String getStepActionDescription(int stepNumber) {
        return switch (stepNumber) {
            case 1 -> "Please conduct the site survey and upload the completed Excel sheet with photos.";
            case 2 -> "Please create the sizing and pricing document for this project.";
            case 3 -> "Please prepare and upload the bank guarantee document.";
            case 4 -> "Please review and address any missing items for this project.";
            case 6 -> "The project has been approved. Please begin project execution.";
            case 7 -> "Please provide after-sales support and quality assurance review.";
            case 8 -> "Please review and approve the final project completion.";
            default -> "Please complete your assigned task for this workflow step.";
        };
    }

    private String getStepCompletionMessage(int stepNumber) {
        return switch (stepNumber) {
            case 1 -> "Site survey has been completed and uploaded.";
            case 2 -> "Sizing and pricing document has been prepared.";
            case 3 -> "Bank guarantee document has been uploaded.";
            case 4 -> "Missing items have been reviewed and addressed.";
            case 6 -> "Project execution has been completed.";
            case 7 -> "After-sales support and QA review has been completed.";
            case 8 -> "Final project review has been completed.";
            default -> "The workflow step has been completed.";
        };
    }

    private String getStepHeaderColor(int stepNumber) {
        return switch (stepNumber) {
            case 1 -> "linear-gradient(135deg, #667eea 0%, #764ba2 100%)"; // Purple
            case 2 -> "linear-gradient(135deg, #11998e 0%, #38ef7d 100%)"; // Green
            case 3 -> "linear-gradient(135deg, #ee9ca7 0%, #ffdde1 100%)"; // Pink
            case 4 -> "linear-gradient(135deg, #fc4a1a 0%, #f7b733 100%)"; // Orange
            case 6 -> "linear-gradient(135deg, #2193b0 0%, #6dd5ed 100%)"; // Blue
            case 7 -> "linear-gradient(135deg, #834d9b 0%, #d04ed6 100%)"; // Magenta
            case 8 -> "linear-gradient(135deg, #1e3c72 0%, #2a5298 100%)"; // Navy
            default -> "linear-gradient(135deg, #667eea 0%, #764ba2 100%)";
        };
    }
}
