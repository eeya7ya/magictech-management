package com.magictech.modules.storage.service;

import com.magictech.core.auth.User;
import com.magictech.core.email.EmailException;
import com.magictech.core.email.EmailService;
import com.magictech.modules.storage.entity.AvailabilityRequest;
import com.magictech.modules.storage.entity.AvailabilityRequest.RequestStatus;
import com.magictech.modules.storage.entity.StorageItem;
import com.magictech.modules.storage.repository.AvailabilityRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing availability requests from Sales/Presales to Storage.
 * Handles creation, response, and email notification for requests.
 */
@Service
@Transactional
public class AvailabilityRequestService {

    @Autowired
    private AvailabilityRequestRepository requestRepository;

    @Autowired
    private StorageService storageService;

    @Autowired
    private EmailService emailService;

    /**
     * Create a new availability request
     */
    public AvailabilityRequest createRequest(
            StorageItem item,
            int quantityRequested,
            String reason,
            Long projectId,
            String projectName,
            Long customerId,
            String customerName,
            User requester,
            String requesterModule) {

        AvailabilityRequest request = new AvailabilityRequest();
        request.setStorageItemId(item.getId());
        request.setItemName(item.getProductName());
        request.setItemManufacture(item.getManufacture());
        request.setItemCode(item.getCode());
        request.setQuantityRequested(quantityRequested);
        request.setRequestReason(reason);
        request.setProjectId(projectId);
        request.setProjectName(projectName);
        request.setCustomerId(customerId);
        request.setCustomerName(customerName);
        request.setRequesterModule(requesterModule);
        request.setRequesterUsername(requester.getUsername());
        request.setRequesterEmail(requester.getEmail());
        request.setStatus(RequestStatus.PENDING);

        return requestRepository.save(request);
    }

    /**
     * Create a simple availability request (without project/customer context)
     */
    public AvailabilityRequest createSimpleRequest(
            StorageItem item,
            int quantityRequested,
            String reason,
            User requester,
            String requesterModule) {

        return createRequest(item, quantityRequested, reason, null, null, null, null, requester, requesterModule);
    }

    /**
     * Get all pending requests (for Storage module)
     */
    public List<AvailabilityRequest> getPendingRequests() {
        return requestRepository.findByStatusInAndActiveTrue(
                Arrays.asList(RequestStatus.PENDING, RequestStatus.IN_REVIEW));
    }

    /**
     * Get all active requests
     */
    public List<AvailabilityRequest> getAllActiveRequests() {
        return requestRepository.findByActiveTrueOrderByCreatedAtDesc();
    }

    /**
     * Get requests by status
     */
    public List<AvailabilityRequest> getRequestsByStatus(RequestStatus status) {
        return requestRepository.findByStatusAndActiveTrueOrderByCreatedAtDesc(status);
    }

    /**
     * Get requests by requester
     */
    public List<AvailabilityRequest> getRequestsByRequester(String username) {
        return requestRepository.findByRequesterUsernameAndActiveTrueOrderByCreatedAtDesc(username);
    }

    /**
     * Get requests by module
     */
    public List<AvailabilityRequest> getRequestsByModule(String module) {
        return requestRepository.findByRequesterModuleAndActiveTrueOrderByCreatedAtDesc(module);
    }

    /**
     * Count pending requests
     */
    public long countPendingRequests() {
        return requestRepository.countPendingRequests();
    }

    /**
     * Find request by ID
     */
    public Optional<AvailabilityRequest> findById(Long id) {
        return requestRepository.findById(id);
    }

    /**
     * Mark request as in review
     */
    public AvailabilityRequest markAsInReview(Long requestId, String reviewerUsername) {
        AvailabilityRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found: " + requestId));

        request.setStatus(RequestStatus.IN_REVIEW);
        request.setRespondedBy(reviewerUsername);

        return requestRepository.save(request);
    }

    /**
     * Respond to a request (by Storage team)
     */
    public AvailabilityRequest respondToRequest(
            Long requestId,
            RequestStatus responseStatus,
            Integer availableQuantity,
            String responseMessage,
            User responder) {

        AvailabilityRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found: " + requestId));

        request.setStatus(responseStatus);
        request.setAvailableQuantity(availableQuantity);
        request.setResponseMessage(responseMessage);
        request.setRespondedBy(responder.getUsername());
        request.setRespondedAt(LocalDateTime.now());

        return requestRepository.save(request);
    }

    /**
     * Respond and send email notification
     */
    public AvailabilityRequest respondAndNotify(
            Long requestId,
            RequestStatus responseStatus,
            Integer availableQuantity,
            String responseMessage,
            User responder) throws EmailException {

        AvailabilityRequest request = respondToRequest(
                requestId, responseStatus, availableQuantity, responseMessage, responder);

        // Send email notification if requester has email
        if (request.getRequesterEmail() != null && !request.getRequesterEmail().isEmpty()) {
            sendResponseEmail(request, responder);
            request.setEmailSent(true);
            request.setEmailSentAt(LocalDateTime.now());
            request = requestRepository.save(request);
        }

        return request;
    }

    /**
     * Send response email to requester
     */
    private void sendResponseEmail(AvailabilityRequest request, User responder) throws EmailException {
        String subject = "Availability Response: " + request.getItemName();

        String statusText = switch (request.getStatus()) {
            case AVAILABLE -> "AVAILABLE";
            case PARTIAL -> "PARTIALLY AVAILABLE";
            case UNAVAILABLE -> "NOT AVAILABLE";
            default -> request.getStatus().toString();
        };

        String statusColor = switch (request.getStatus()) {
            case AVAILABLE -> "#22c55e";
            case PARTIAL -> "#f59e0b";
            case UNAVAILABLE -> "#ef4444";
            default -> "#6b7280";
        };

        String projectInfo = "";
        if (request.getProjectName() != null) {
            projectInfo = "<tr><td style=\"padding: 8px; border-bottom: 1px solid #eee;\"><strong>Project:</strong></td>" +
                    "<td style=\"padding: 8px; border-bottom: 1px solid #eee;\">" + request.getProjectName() + "</td></tr>";
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f4f4f4; }
                    .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #ef4444 0%%, #dc2626 100%%); color: white; padding: 20px; border-radius: 10px 10px 0 0; margin: -30px -30px 30px -30px; text-align: center; }
                    h1 { margin: 0; font-size: 24px; }
                    .content { color: #333; line-height: 1.6; }
                    .status-badge { display: inline-block; padding: 8px 16px; border-radius: 20px; color: white; font-weight: bold; font-size: 14px; }
                    .info-table { width: 100%%; border-collapse: collapse; margin: 20px 0; }
                    .info-table td { padding: 8px; border-bottom: 1px solid #eee; }
                    .message-box { background-color: #f8f9fa; border-left: 4px solid #6b7280; padding: 15px; margin: 20px 0; border-radius: 0 5px 5px 0; }
                    .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; color: #666; font-size: 12px; text-align: center; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>📦 Availability Response</h1>
                    </div>
                    <div class="content">
                        <p>Hello <strong>%s</strong>,</p>
                        <p>The Storage team has responded to your availability request:</p>

                        <p style="text-align: center; margin: 20px 0;">
                            <span class="status-badge" style="background-color: %s;">%s</span>
                        </p>

                        <table class="info-table">
                            <tr>
                                <td style="padding: 8px; border-bottom: 1px solid #eee;"><strong>Item:</strong></td>
                                <td style="padding: 8px; border-bottom: 1px solid #eee;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding: 8px; border-bottom: 1px solid #eee;"><strong>Manufacturer:</strong></td>
                                <td style="padding: 8px; border-bottom: 1px solid #eee;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding: 8px; border-bottom: 1px solid #eee;"><strong>Code:</strong></td>
                                <td style="padding: 8px; border-bottom: 1px solid #eee;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding: 8px; border-bottom: 1px solid #eee;"><strong>Requested Qty:</strong></td>
                                <td style="padding: 8px; border-bottom: 1px solid #eee;">%d</td>
                            </tr>
                            <tr>
                                <td style="padding: 8px; border-bottom: 1px solid #eee;"><strong>Available Qty:</strong></td>
                                <td style="padding: 8px; border-bottom: 1px solid #eee; color: %s; font-weight: bold;">%s</td>
                            </tr>
                            %s
                        </table>

                        <div class="message-box">
                            <strong>Storage Team Response:</strong><br>
                            %s
                        </div>

                        <p style="color: #666; font-size: 12px;">
                            Responded by: %s<br>
                            Response time: %s
                        </p>
                    </div>
                    <div class="footer">
                        <p>This is an automated notification from MagicTech Management System.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                request.getRequesterUsername(),
                statusColor,
                statusText,
                request.getItemName(),
                request.getItemManufacture() != null ? request.getItemManufacture() : "-",
                request.getItemCode() != null ? request.getItemCode() : "-",
                request.getQuantityRequested(),
                statusColor,
                request.getAvailableQuantity() != null ? request.getAvailableQuantity().toString() : "N/A",
                projectInfo,
                request.getResponseMessage() != null ? request.getResponseMessage() : "No additional message",
                responder.getUsername(),
                timestamp
        );

        // Try to send from responder's account first, fall back to system email
        if (emailService.isUserEmailConfigured(responder)) {
            emailService.sendEmailFromUser(responder, request.getRequesterEmail(), subject, htmlContent);
        } else {
            emailService.sendHtmlEmail(request.getRequesterEmail(), subject, htmlContent);
        }
    }

    /**
     * Close a request
     */
    public AvailabilityRequest closeRequest(Long requestId, String closedBy) {
        AvailabilityRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found: " + requestId));

        request.setStatus(RequestStatus.CLOSED);
        request.setRespondedBy(closedBy);
        request.setRespondedAt(LocalDateTime.now());

        return requestRepository.save(request);
    }

    /**
     * Soft delete a request
     */
    public void deleteRequest(Long requestId) {
        AvailabilityRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found: " + requestId));

        request.setActive(false);
        requestRepository.save(request);
    }
}
