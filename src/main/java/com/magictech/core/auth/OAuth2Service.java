package com.magictech.core.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.magictech.core.util.EncryptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing OAuth2 authentication with Google
 * Handles token storage, refresh, and validation
 */
@Service
@Transactional
public class OAuth2Service {

    @Autowired
    private UserOAuth2TokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret:}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri:http://localhost:8085/oauth2/callback/google}")
    private String redirectUri;

    private static final String PROVIDER_GOOGLE = "GOOGLE";
    private static final List<String> SCOPES = Arrays.asList(
            "openid",
            "profile",
            "email",
            "https://www.googleapis.com/auth/gmail.send"
    );

    /**
     * Get Google OAuth2 authorization URL for user to consent
     * @param userId User ID requesting authorization
     * @return Authorization URL to redirect user to
     */
    public String getAuthorizationUrl(Long userId) {
        try {
            if (clientId == null || clientId.isEmpty()) {
                throw new RuntimeException("OAuth2 client ID not configured. Please set GOOGLE_CLIENT_ID environment variable.");
            }

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    clientId,
                    clientSecret,
                    SCOPES
            )
            .setAccessType("offline") // Request refresh token
            .setApprovalPrompt("force") // Force consent screen to get refresh token
            .build();

            return flow.newAuthorizationUrl()
                    .setRedirectUri(redirectUri)
                    .setState(String.valueOf(userId)) // Pass user ID in state for callback
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate authorization URL", e);
        }
    }

    /**
     * Exchange authorization code for access token and save to database
     * @param userId User ID
     * @param authorizationCode Authorization code from Google callback
     * @return Success status
     */
    public boolean exchangeCodeForToken(Long userId, String authorizationCode) {
        try {
            if (clientId == null || clientId.isEmpty()) {
                throw new RuntimeException("OAuth2 not configured");
            }

            // Exchange code for tokens
            GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeFlow.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    clientId,
                    clientSecret,
                    SCOPES
            ).build()
            .newTokenRequest(authorizationCode)
            .setRedirectUri(redirectUri)
            .execute();

            // Get user's Google email
            String googleEmail = getUserEmailFromToken(tokenResponse.getAccessToken());

            // Update user's email in database
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));

            if (user.getEmail() == null || user.getEmail().isEmpty()) {
                user.setEmail(googleEmail);
                userRepository.save(user);
            }

            // Save or update token
            UserOAuth2Token token = tokenRepository.findByUserIdAndProvider(userId, PROVIDER_GOOGLE)
                    .orElse(new UserOAuth2Token(userId, PROVIDER_GOOGLE));

            // Encrypt tokens before storing
            token.setAccessToken(EncryptionUtil.encrypt(tokenResponse.getAccessToken()));
            token.setRefreshToken(EncryptionUtil.encrypt(tokenResponse.getRefreshToken()));
            token.setTokenType(tokenResponse.getTokenType());
            token.setScope(String.join(",", SCOPES));
            token.setEmail(googleEmail);

            // Calculate expiration time
            if (tokenResponse.getExpiresInSeconds() != null) {
                token.setExpiresAt(LocalDateTime.now().plusSeconds(tokenResponse.getExpiresInSeconds()));
            }

            token.setActive(true);
            tokenRepository.save(token);

            System.out.println("✓ OAuth2 token saved for user " + userId + " (" + googleEmail + ")");
            return true;

        } catch (Exception e) {
            System.err.println("✗ Failed to exchange authorization code: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get valid access token for user (refreshes if needed)
     * @param userId User ID
     * @return Decrypted access token or null if not available
     */
    public String getValidAccessToken(Long userId) {
        try {
            Optional<UserOAuth2Token> tokenOpt = tokenRepository.findByUserIdAndProviderAndActiveTrue(
                    userId, PROVIDER_GOOGLE);

            if (tokenOpt.isEmpty()) {
                return null;
            }

            UserOAuth2Token token = tokenOpt.get();

            // Check if token needs refresh
            if (token.needsRefresh()) {
                System.out.println("🔄 Token expired for user " + userId + ", refreshing...");
                if (!refreshAccessToken(userId)) {
                    return null;
                }
                // Reload token after refresh
                token = tokenRepository.findByUserIdAndProviderAndActiveTrue(userId, PROVIDER_GOOGLE)
                        .orElse(null);
                if (token == null) {
                    return null;
                }
            }

            // Decrypt and return
            return EncryptionUtil.decrypt(token.getAccessToken());

        } catch (Exception e) {
            System.err.println("✗ Failed to get valid access token: " + e.getMessage());
            return null;
        }
    }

    /**
     * Refresh access token using refresh token
     * @param userId User ID
     * @return Success status
     */
    public boolean refreshAccessToken(Long userId) {
        try {
            UserOAuth2Token token = tokenRepository.findByUserIdAndProviderAndActiveTrue(userId, PROVIDER_GOOGLE)
                    .orElseThrow(() -> new RuntimeException("Token not found for user: " + userId));

            String refreshToken = EncryptionUtil.decrypt(token.getRefreshToken());

            if (refreshToken == null || refreshToken.isEmpty()) {
                System.err.println("✗ No refresh token available for user " + userId);
                return false;
            }

            // Request new access token
            GoogleTokenResponse response = new GoogleRefreshTokenRequest(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    refreshToken,
                    clientId,
                    clientSecret
            ).execute();

            // Update token
            token.setAccessToken(EncryptionUtil.encrypt(response.getAccessToken()));

            if (response.getExpiresInSeconds() != null) {
                token.setExpiresAt(LocalDateTime.now().plusSeconds(response.getExpiresInSeconds()));
            }

            tokenRepository.save(token);
            System.out.println("✓ Token refreshed for user " + userId);
            return true;

        } catch (Exception e) {
            System.err.println("✗ Failed to refresh token: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Check if user has valid OAuth2 token configured
     * @param userId User ID
     * @return True if user has active token
     */
    public boolean isUserAuthenticated(Long userId) {
        try {
            Optional<UserOAuth2Token> tokenOpt = tokenRepository.findByUserIdAndProviderAndActiveTrue(
                    userId, PROVIDER_GOOGLE);
            return tokenOpt.isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Revoke OAuth2 token (disconnect Google account)
     * @param userId User ID
     * @return Success status
     */
    public boolean revokeToken(Long userId) {
        try {
            Optional<UserOAuth2Token> tokenOpt = tokenRepository.findByUserIdAndProvider(userId, PROVIDER_GOOGLE);

            if (tokenOpt.isPresent()) {
                UserOAuth2Token token = tokenOpt.get();
                token.setActive(false);
                tokenRepository.save(token);
                System.out.println("✓ OAuth2 token revoked for user " + userId);
                return true;
            }

            return false;
        } catch (Exception e) {
            System.err.println("✗ Failed to revoke token: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get user's Google email from access token
     * @param accessToken Access token
     * @return Email address
     */
    private String getUserEmailFromToken(String accessToken) {
        try {
            // Call Google UserInfo API to get email
            java.net.URL url = new java.net.URL("https://www.googleapis.com/oauth2/v2/userinfo");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == 200) {
                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream()));
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Parse JSON response (simple parsing)
                String json = response.toString();
                int emailStart = json.indexOf("\"email\": \"") + 10;
                int emailEnd = json.indexOf("\"", emailStart);
                return json.substring(emailStart, emailEnd);
            }

            return null;
        } catch (Exception e) {
            System.err.println("✗ Failed to get user email from token: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get OAuth2 token info for user
     * @param userId User ID
     * @return Token info or null
     */
    public UserOAuth2Token getTokenInfo(Long userId) {
        return tokenRepository.findByUserIdAndProviderAndActiveTrue(userId, PROVIDER_GOOGLE)
                .orElse(null);
    }

    /**
     * Check if OAuth2 is properly configured
     * @return True if configured
     */
    public boolean isOAuth2Configured() {
        return clientId != null && !clientId.isEmpty()
                && clientSecret != null && !clientSecret.isEmpty();
    }
}
