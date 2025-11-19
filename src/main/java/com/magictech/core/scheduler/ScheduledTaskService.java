package com.magictech.core.scheduler;

import com.magictech.core.approval.PendingApprovalService;
import com.magictech.core.notification.CoreNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduled task service for:
 * - Notification cleanup (every 3 months)
 * - Approval timeout processing (every hour)
 */
@Service
public class ScheduledTaskService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTaskService.class);

    @Autowired
    private CoreNotificationService notificationService;

    @Autowired
    private PendingApprovalService approvalService;

    /**
     * Delete old notifications (older than 3 months)
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldNotifications() {
        try {
            logger.info("Starting scheduled notification cleanup...");
            notificationService.deleteOldNotifications();
            logger.info("Notification cleanup completed successfully");
        } catch (Exception e) {
            logger.error("Error during notification cleanup", e);
        }
    }

    /**
     * Process expired approvals (auto-reject after 2 days)
     * Runs every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour = 3,600,000 ms
    public void processExpiredApprovals() {
        try {
            logger.debug("Checking for expired approvals...");
            approvalService.processExpiredApprovals();
        } catch (Exception e) {
            logger.error("Error processing expired approvals", e);
        }
    }

    /**
     * Manual trigger for notification cleanup (can be called from UI)
     */
    public void triggerNotificationCleanup() {
        logger.info("Manual trigger: Notification cleanup");
        notificationService.deleteOldNotifications();
    }

    /**
     * Manual trigger for approval timeout processing (can be called from UI)
     */
    public void triggerApprovalTimeout() {
        logger.info("Manual trigger: Approval timeout processing");
        approvalService.processExpiredApprovals();
    }
}
