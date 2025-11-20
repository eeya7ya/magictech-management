package com.magictech.core.api;

import com.magictech.core.auth.OAuth2Service;
import com.magictech.core.auth.User;
import com.magictech.core.auth.UserOAuth2Token;
import com.magictech.core.auth.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * REST API Controller for OAuth2 authentication
 * Handles Google OAuth2 flow for email integration
 */
@RestController
@RequestMapping("/api/oauth2")
@CrossOrigin(origins = "*")
public class OAuth2Controller {

    @Autowired
    private OAuth2Service oauth2Service;

    @Autowired
    private UserRepository userRepository;

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("service", "OAuth2 Authentication");
        response.put("configured", oauth2Service.isOAuth2Configured());
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    /**
     * Get OAuth2 configuration status
     */
    @GetMapping("/config/status")
    public ResponseEntity<Map<String, Object>> getConfigStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("configured", oauth2Service.isOAuth2Configured());

        if (!oauth2Service.isOAuth2Configured()) {
            response.put("message", "OAuth2 not configured. Please set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET environment variables.");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Initiate OAuth2 authorization flow
     * Redirects user to Google consent screen
     *
     * GET /api/oauth2/authorize?userId=123
     */
    @GetMapping("/authorize")
    public void authorize(@RequestParam Long userId, HttpServletResponse response) throws IOException {
        try {
            if (!oauth2Service.isOAuth2Configured()) {
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                        "OAuth2 not configured. Please set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET.");
                return;
            }

            // Verify user exists
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "User not found: " + userId);
                return;
            }

            // Get authorization URL and redirect
            String authUrl = oauth2Service.getAuthorizationUrl(userId);
            response.sendRedirect(authUrl);

        } catch (Exception e) {
            System.err.println("✗ Error initiating OAuth2 flow: " + e.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Failed to initiate OAuth2 flow: " + e.getMessage());
        }
    }

    /**
     * OAuth2 callback endpoint
     * Google redirects here after user consents
     *
     * GET /api/oauth2/callback?code=xxx&state=userId
     */
    @GetMapping("/callback")
    public void callback(@RequestParam String code,
                         @RequestParam String state,
                         HttpServletResponse response) throws IOException {
        try {
            // State parameter contains userId
            Long userId = Long.parseLong(state);

            // Exchange authorization code for tokens
            boolean success = oauth2Service.exchangeCodeForToken(userId, code);

            if (success) {
                // Redirect back to application with success message
                String redirectUrl = "http://localhost:8085/oauth2/success.html?userId=" + userId;
                response.sendRedirect(redirectUrl);
            } else {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Failed to exchange authorization code for token");
            }

        } catch (Exception e) {
            System.err.println("✗ Error in OAuth2 callback: " + e.getMessage());
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "OAuth2 callback failed: " + e.getMessage());
        }
    }

    /**
     * Check if user has connected their Google account
     *
     * GET /api/oauth2/status?userId=123
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus(@RequestParam Long userId) {
        Map<String, Object> response = new HashMap<>();

        try {
            boolean authenticated = oauth2Service.isUserAuthenticated(userId);
            response.put("authenticated", authenticated);
            response.put("userId", userId);

            if (authenticated) {
                UserOAuth2Token tokenInfo = oauth2Service.getTokenInfo(userId);
                if (tokenInfo != null) {
                    response.put("email", tokenInfo.getEmail());
                    response.put("provider", tokenInfo.getProvider());
                    response.put("expiresAt", tokenInfo.getExpiresAt());
                }
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Disconnect Google account (revoke OAuth2 token)
     *
     * POST /api/oauth2/revoke?userId=123
     */
    @PostMapping("/revoke")
    public ResponseEntity<Map<String, Object>> revokeToken(@RequestParam Long userId) {
        Map<String, Object> response = new HashMap<>();

        try {
            boolean success = oauth2Service.revokeToken(userId);
            response.put("success", success);

            if (success) {
                response.put("message", "Google account disconnected successfully");
            } else {
                response.put("message", "No OAuth2 token found for user");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Manually refresh access token
     *
     * POST /api/oauth2/refresh?userId=123
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(@RequestParam Long userId) {
        Map<String, Object> response = new HashMap<>();

        try {
            boolean success = oauth2Service.refreshAccessToken(userId);
            response.put("success", success);

            if (success) {
                response.put("message", "Token refreshed successfully");
            } else {
                response.put("message", "Failed to refresh token");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Test OAuth2 email sending
     *
     * POST /api/oauth2/test-email
     * Body: { "userId": 123, "toEmail": "test@example.com" }
     */
    @PostMapping("/test-email")
    public ResponseEntity<Map<String, Object>> testEmail(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();

        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            String toEmail = request.get("toEmail").toString();

            // Check if user is authenticated
            if (!oauth2Service.isUserAuthenticated(userId)) {
                response.put("success", false);
                response.put("error", "User has not connected their Google account");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            response.put("success", true);
            response.put("message", "User is authenticated. Use GmailOAuth2Service.sendEmailViaOAuth2() to send.");
            response.put("note", "This endpoint only checks authentication. Actual email sending is done via GmailOAuth2Service.");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
