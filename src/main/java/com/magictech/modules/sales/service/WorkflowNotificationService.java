package com.magictech.modules.sales.service;

import com.magictech.core.auth.User;
import com.magictech.core.auth.UserRepository;
import com.magictech.core.auth.UserRole;
import com.magictech.core.messaging.constants.NotificationConstants;
import com.magictech.core.messaging.dto.NotificationMessage;
import com.magictech.core.messaging.service.NotificationService;
import com.magictech.modules.projects.entity.Project;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for sending workflow-related notifications
 * Integrates with existing notification system
 */
@Service
public class WorkflowNotificationService {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Step 1: Notify Project module about site survey request
     */
    public void notifySiteSurveyRequest(Project project, User salesUser) {
        List<User> projectUsers = userRepository.findByRoleAndActiveTrue(UserRole.PROJECTS);

        for (User projectUser : projectUsers) {
            NotificationMessage message = new NotificationMessage.Builder()
                .title("Site Survey Request")
                .message(String.format("Sales person %s is requesting a site survey for project '%s'",
                    salesUser.getUsername(), project.getProjectName()))
                .type(NotificationConstants.TYPE_INFO)
                .module(NotificationConstants.MODULE_SALES)
                .targetModule(NotificationConstants.MODULE_PROJECTS)
                .entityType(NotificationConstants.ENTITY_PROJECT)
                .entityId(project.getId())
                .priority(NotificationConstants.PRIORITY_HIGH)
                .createdBy(salesUser.getUsername())
                .build();

            notificationService.publishNotification(message);
        }
    }

    /**
     * Step 1: Notify Sales user that site survey is completed
     */
    public void notifySiteSurveyCompleted(Project project, User projectUser, User salesUser) {
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("📢 CREATING SITE SURVEY COMPLETION NOTIFICATION");
        System.out.println("   Project: " + project.getProjectName());
        System.out.println("   Completed by: " + projectUser.getUsername() + " (PROJECT team)");
        System.out.println("   Notifying: " + salesUser.getUsername() + " (SALES user)");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        NotificationMessage message = new NotificationMessage.Builder()
            .title("Site Survey Completed")
            .message(String.format("Project team member %s has completed the site survey for project '%s'",
                projectUser.getUsername(), project.getProjectName()))
            .type(NotificationConstants.TYPE_SUCCESS)
            .module(NotificationConstants.MODULE_PROJECTS)
            .targetModule(NotificationConstants.MODULE_SALES)
            .entityType(NotificationConstants.ENTITY_PROJECT)
            .entityId(project.getId())
            .priority(NotificationConstants.PRIORITY_HIGH)
            .createdBy(projectUser.getUsername())
            .build();

        System.out.println("📨 Notification message details:");
        System.out.println("   Title: " + message.getTitle());
        System.out.println("   Type: " + message.getType());
        System.out.println("   Module: " + message.getModule());
        System.out.println("   Target Module: " + message.getTargetModule());
        System.out.println("   Priority: " + message.getPriority());

        notificationService.publishNotification(message);

        System.out.println("✅ Notification published successfully!\n");
    }

    /**
     * Step 2: Notify Presales module about selection and design request
     */
    public void notifyPresalesSelectionDesign(Project project, User salesUser) {
        List<User> presalesUsers = userRepository.findByRoleAndActiveTrue(UserRole.PRESALES);

        for (User presalesUser : presalesUsers) {
            NotificationMessage message = new NotificationMessage.Builder()
                .title("Selection & Design Request")
                .message(String.format("Sales person %s needs selection and design for project '%s'",
                    salesUser.getUsername(), project.getProjectName()))
                .type(NotificationConstants.TYPE_INFO)
                .module(NotificationConstants.MODULE_SALES)
                .targetModule(NotificationConstants.MODULE_PRESALES)
                .entityType(NotificationConstants.ENTITY_PROJECT)
                .entityId(project.getId())
                .priority(NotificationConstants.PRIORITY_HIGH)
                .createdBy(salesUser.getUsername())
                .build();

            notificationService.publishNotification(message);
        }
    }

    /**
     * Step 2: Notify Sales user that presales work is completed
     */
    public void notifyPresalesCompleted(Project project, User presalesUser, User salesUser) {
        NotificationMessage message = new NotificationMessage.Builder()
            .title("Sizing & Pricing Completed")
            .message(String.format("Presales team member %s has completed sizing and pricing for project '%s'",
                presalesUser.getUsername(), project.getProjectName()))
            .type(NotificationConstants.TYPE_SUCCESS)
            .module(NotificationConstants.MODULE_PRESALES)
            .targetModule(NotificationConstants.MODULE_SALES)
            .entityType(NotificationConstants.ENTITY_PROJECT)
            .entityId(project.getId())
            .priority(NotificationConstants.PRIORITY_HIGH)
            .createdBy(presalesUser.getUsername())
            .build();

        notificationService.publishNotification(message);
    }

    /**
     * Step 3: Notify Finance module about bank guarantee request
     */
    public void notifyBankGuaranteeRequest(Project project, User salesUser) {
        List<User> financeUsers = userRepository.findByRoleAndActiveTrue(UserRole.FINANCE);

        for (User financeUser : financeUsers) {
            NotificationMessage message = new NotificationMessage.Builder()
                .title("Bank Guarantee Request")
                .message(String.format("Sales person %s needs bank guarantee processing for project '%s'",
                    salesUser.getUsername(), project.getProjectName()))
                .type(NotificationConstants.TYPE_INFO)
                .module(NotificationConstants.MODULE_SALES)
                .targetModule(NotificationConstants.MODULE_FINANCE)
                .entityType(NotificationConstants.ENTITY_PROJECT)
                .entityId(project.getId())
                .priority(NotificationConstants.PRIORITY_HIGH)
                .createdBy(salesUser.getUsername())
                .build();

            notificationService.publishNotification(message);
        }
    }

    /**
     * Step 3: Notify Sales user that bank guarantee is completed
     */
    public void notifyBankGuaranteeCompleted(Project project, User financeUser, User salesUser) {
        NotificationMessage message = new NotificationMessage.Builder()
            .title("Bank Guarantee Completed")
            .message(String.format("Finance team member %s has completed bank guarantee for project '%s'",
                financeUser.getUsername(), project.getProjectName()))
            .type(NotificationConstants.TYPE_SUCCESS)
            .module(NotificationConstants.MODULE_FINANCE)
            .targetModule(NotificationConstants.MODULE_SALES)
            .entityType(NotificationConstants.ENTITY_PROJECT)
            .entityId(project.getId())
            .priority(NotificationConstants.PRIORITY_HIGH)
            .createdBy(financeUser.getUsername())
            .build();

        notificationService.publishNotification(message);
    }

    /**
     * Step 4: Notify MASTER and SALES_MANAGER about missing item request
     */
    public void notifyMissingItemRequest(Project project, User salesUser, String itemDetails) {
        // Notify all MASTER users
        List<User> masterUsers = userRepository.findByRoleAndActiveTrue(UserRole.MASTER);
        for (User masterUser : masterUsers) {
            NotificationMessage message = new NotificationMessage.Builder()
                .title("Missing Item Approval Required")
                .message(String.format("Sales person %s requires approval for missing item in project '%s': %s",
                    salesUser.getUsername(), project.getProjectName(), itemDetails))
                .type(NotificationConstants.TYPE_WARNING)
                .module(NotificationConstants.MODULE_SALES)
                .targetModule(NotificationConstants.MODULE_ALL)
                .entityType(NotificationConstants.ENTITY_PROJECT)
                .entityId(project.getId())
                .priority(NotificationConstants.PRIORITY_URGENT)
                .createdBy(salesUser.getUsername())
                .build();

            notificationService.publishNotification(message);
        }

        // Notify all SALES_MANAGER users
        List<User> salesManagers = userRepository.findByRoleAndActiveTrue(UserRole.SALES_MANAGER);
        for (User salesManager : salesManagers) {
            NotificationMessage message = new NotificationMessage.Builder()
                .title("Missing Item Approval Required")
                .message(String.format("Sales person %s requires approval for missing item in project '%s': %s",
                    salesUser.getUsername(), project.getProjectName(), itemDetails))
                .type(NotificationConstants.TYPE_WARNING)
                .module(NotificationConstants.MODULE_SALES)
                .targetModule(NotificationConstants.MODULE_SALES)
                .entityType(NotificationConstants.ENTITY_PROJECT)
                .entityId(project.getId())
                .priority(NotificationConstants.PRIORITY_URGENT)
                .createdBy(salesUser.getUsername())
                .build();

            notificationService.publishNotification(message);
        }
    }

    /**
     * Step 4: Notify Sales user that missing item is approved
     */
    public void notifyMissingItemApproved(Project project, User approver, User salesUser) {
        NotificationMessage message = new NotificationMessage.Builder()
            .title("Missing Item Approved")
            .message(String.format("%s has approved the missing item request for project '%s'",
                approver.getUsername(), project.getProjectName()))
            .type(NotificationConstants.TYPE_SUCCESS)
            .module(NotificationConstants.MODULE_SALES)
            .targetModule(NotificationConstants.MODULE_SALES)
            .entityType(NotificationConstants.ENTITY_PROJECT)
            .entityId(project.getId())
            .priority(NotificationConstants.PRIORITY_HIGH)
            .createdBy(approver.getUsername())
            .build();

        notificationService.publishNotification(message);
    }

    /**
     * Step 5: Notify Project module to start work on accepted tender
     */
    public void notifyProjectStart(Project project, User salesUser) {
        List<User> projectUsers = userRepository.findByRoleAndActiveTrue(UserRole.PROJECTS);

        for (User projectUser : projectUsers) {
            NotificationMessage message = new NotificationMessage.Builder()
                .title("Project Start - Tender Accepted")
                .message(String.format("Tender accepted for project '%s'. Please start work.",
                    project.getProjectName()))
                .type(NotificationConstants.TYPE_SUCCESS)
                .module(NotificationConstants.MODULE_SALES)
                .targetModule(NotificationConstants.MODULE_PROJECTS)
                .entityType(NotificationConstants.ENTITY_PROJECT)
                .entityId(project.getId())
                .priority(NotificationConstants.PRIORITY_URGENT)
                .createdBy(salesUser.getUsername())
                .build();

            notificationService.publishNotification(message);
        }
    }

    /**
     * Step 5: Notify Sales user that project is completed
     */
    public void notifyProjectCompleted(Project project, User projectUser, User salesUser) {
        NotificationMessage message = new NotificationMessage.Builder()
            .title("Project Work Completed")
            .message(String.format("Project team has completed work on project '%s'",
                project.getProjectName()))
            .type(NotificationConstants.TYPE_SUCCESS)
            .module(NotificationConstants.MODULE_PROJECTS)
            .targetModule(NotificationConstants.MODULE_SALES)
            .entityType(NotificationConstants.ENTITY_PROJECT)
            .entityId(project.getId())
            .priority(NotificationConstants.PRIORITY_HIGH)
            .createdBy(projectUser.getUsername())
            .build();

        notificationService.publishNotification(message);
    }

    /**
     * Step 6: Notify MASTER about project delay (DANGER alarm)
     */
    public void notifyProjectDelayDanger(Project project, User salesUser, String delayDetails) {
        List<User> masterUsers = userRepository.findByRoleAndActiveTrue(UserRole.MASTER);

        for (User masterUser : masterUsers) {
            NotificationMessage message = new NotificationMessage.Builder()
                .title("⚠️ DANGER: Project Delay")
                .message(String.format("Project '%s' is delayed. Details: %s",
                    project.getProjectName(), delayDetails))
                .type(NotificationConstants.TYPE_ERROR)
                .module(NotificationConstants.MODULE_SALES)
                .targetModule(NotificationConstants.MODULE_ALL)
                .entityType(NotificationConstants.ENTITY_PROJECT)
                .entityId(project.getId())
                .priority(NotificationConstants.PRIORITY_URGENT)
                .createdBy(salesUser.getUsername())
                .build();

            notificationService.publishNotification(message);
        }
    }

    /**
     * Step 7: Notify QA module about after-sales check
     */
    public void notifyQAAfterSalesCheck(Project project, User salesUser) {
        List<User> qaUsers = userRepository.findByRoleAndActiveTrue(UserRole.QUALITY_ASSURANCE);

        for (User qaUser : qaUsers) {
            NotificationMessage message = new NotificationMessage.Builder()
                .title("After-Sales Check Required")
                .message(String.format("Please perform profit/loss analysis for project '%s'",
                    project.getProjectName()))
                .type(NotificationConstants.TYPE_INFO)
                .module(NotificationConstants.MODULE_SALES)
                .targetModule(NotificationConstants.MODULE_QA)
                .entityType(NotificationConstants.ENTITY_PROJECT)
                .entityId(project.getId())
                .priority(NotificationConstants.PRIORITY_MEDIUM)
                .createdBy(salesUser.getUsername())
                .build();

            notificationService.publishNotification(message);
        }
    }

    /**
     * Step 7: Notify Sales user that QA check is completed
     */
    public void notifyQACheckCompleted(Project project, User qaUser, User salesUser) {
        NotificationMessage message = new NotificationMessage.Builder()
            .title("After-Sales Check Completed")
            .message(String.format("Quality Assurance has completed analysis for project '%s'",
                project.getProjectName()))
            .type(NotificationConstants.TYPE_SUCCESS)
            .module(NotificationConstants.MODULE_QA)
            .targetModule(NotificationConstants.MODULE_SALES)
            .entityType(NotificationConstants.ENTITY_PROJECT)
            .entityId(project.getId())
            .priority(NotificationConstants.PRIORITY_MEDIUM)
            .createdBy(qaUser.getUsername())
            .build();

        notificationService.publishNotification(message);
    }

    /**
     * Step 8: Notify about workflow completion
     */
    public void notifyWorkflowCompleted(Project project, User salesUser) {
        NotificationMessage message = new NotificationMessage.Builder()
            .title("✅ Project Workflow Completed")
            .message(String.format("Complete workflow for project '%s' has been finished. All data pushed to storage analysis.",
                project.getProjectName()))
            .type(NotificationConstants.TYPE_SUCCESS)
            .module(NotificationConstants.MODULE_SALES)
            .targetModule(NotificationConstants.MODULE_SALES)
            .entityType(NotificationConstants.ENTITY_PROJECT)
            .entityId(project.getId())
            .priority(NotificationConstants.PRIORITY_MEDIUM)
            .createdBy("SYSTEM")
            .build();

        notificationService.publishNotification(message);
    }
}
