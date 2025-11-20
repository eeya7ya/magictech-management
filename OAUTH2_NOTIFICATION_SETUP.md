# OAuth2 Notification System - Complete Setup Guide

## 📋 Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Prerequisites](#prerequisites)
4. [Google Cloud Setup](#google-cloud-setup)
5. [Application Configuration](#application-configuration)
6. [Database Schema](#database-schema)
7. [User Authentication Flow](#user-authentication-flow)
8. [API Endpoints](#api-endpoints)
9. [WebSocket Integration](#websocket-integration)
10. [Testing](#testing)
11. [Security Best Practices](#security-best-practices)
12. [Troubleshooting](#troubleshooting)

---

## 🎯 Overview

This system replaces the old **hardcoded Gmail SMTP** approach with a modern **OAuth2-based notification system** where:

- ✅ Each user authenticates with their **own Google account**
- ✅ Emails are sent via **Gmail API** using user's credentials
- ✅ **Real-time notifications** via WebSocket (no polling)
- ✅ **In-app notifications** + **Email notifications**
- ✅ **Secure token storage** with AES encryption
- ✅ **Automatic token refresh** when expired
- ✅ **Fallback to SMTP** if OAuth2 not configured

### What Changed?

| Before (Old System) | After (New System) |
|---------------------|-------------------|
| ❌ Hardcoded Gmail credentials in `application.properties` | ✅ Each user links their own Google account |
| ❌ All emails sent from single account | ✅ Emails sent from user's Gmail account |
| ❌ Gmail API limits apply to single account | ✅ Distributed across all user accounts |
| ❌ Security risk (plain text password) | ✅ OAuth2 tokens encrypted in database |
| ❌ Polling for new notifications (10s interval) | ✅ WebSocket push notifications (instant) |

---

## 🏗️ Architecture

### System Components

```
┌─────────────────────────────────────────────────────────────┐
│                    MagicTech Application                     │
│                                                              │
│  ┌────────────┐      ┌──────────────┐      ┌─────────────┐│
│  │  Modules   │─────>│ Notification │─────>│  WebSocket  ││
│  │  (Sales,   │      │   Service    │      │   Service   ││
│  │  Projects) │      └──────┬───────┘      └──────┬──────┘│
│  └────────────┘             │                     │        │
│                              │                     │        │
│                    ┌─────────▼─────────┐          │        │
│                    │  OAuth2Service    │          │        │
│                    │  - Get tokens     │          │        │
│                    │  - Refresh tokens │          │        │
│                    └─────────┬─────────┘          │        │
│                              │                     │        │
│                    ┌─────────▼─────────┐          │        │
│                    │ GmailOAuth2Service│          │        │
│                    │ - Send via Gmail  │          │        │
│                    │ - User's account  │          │        │
│                    └─────────┬─────────┘          │        │
│                              │                     │        │
└──────────────────────────────┼─────────────────────┼────────┘
                               │                     │
                    ┌──────────▼─────────┐    ┌─────▼──────┐
                    │   Gmail API        │    │  WebSocket │
                    │   (Google)         │    │  Clients   │
                    └────────────────────┘    └────────────┘
```

### Database Schema

**New Tables:**
1. `user_oauth2_tokens` - Stores encrypted OAuth2 tokens per user

**Existing Tables:**
- `notifications` - In-app notifications
- `notification_preferences` - User notification preferences
- `users` - User accounts (already has email field)

### Flow Diagram: Sending Notification

```
Module creates notification
         │
         ▼
NotificationService.createNotification()
         │
         ├─> Save to database (notifications table)
         │
         ├─> Send via WebSocket (real-time push)
         │   └─> All connected clients receive instantly
         │
         └─> Send via Email
             │
             ├─> Check if user has OAuth2 token
             │   │
             │   ├─ YES: Use GmailOAuth2Service
             │   │        └─> Send from user's Gmail account
             │   │
             │   └─ NO:  Fallback to SMTP EmailService
             │            └─> Send from system account
             │
             └─> Result logged
```

---

## 📋 Prerequisites

Before setting up OAuth2 notifications, ensure you have:

- ✅ Google account (Gmail)
- ✅ Access to Google Cloud Console
- ✅ MagicTech application running
- ✅ PostgreSQL database configured
- ✅ Maven 3.6+ and Java 21

---

## ☁️ Google Cloud Setup

### Step 1: Create Google Cloud Project

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Click **"Create Project"** or select existing project
3. Enter project name: **"MagicTech Management System"**
4. Click **"Create"**

### Step 2: Enable Gmail API

1. In the Google Cloud Console, go to **"APIs & Services"** > **"Library"**
2. Search for **"Gmail API"**
3. Click on **Gmail API** in the results
4. Click **"Enable"**
5. Wait for API to be enabled (takes a few seconds)

### Step 3: Create OAuth 2.0 Credentials

1. Go to **"APIs & Services"** > **"Credentials"**
2. Click **"Create Credentials"** > **"OAuth client ID"**

   **If you see "Configure Consent Screen" message:**
   - Click **"Configure Consent Screen"**
   - Select **"Internal"** (if using Google Workspace) or **"External"**
   - Fill in:
     - App name: **"MagicTech Management System"**
     - User support email: Your email
     - Developer contact email: Your email
   - Click **"Save and Continue"**
   - **Scopes**: Click **"Add or Remove Scopes"**
     - Add: `openid`, `profile`, `email`
     - Add: `https://www.googleapis.com/auth/gmail.send`
   - Click **"Save and Continue"**
   - **Test users** (for External type): Add your Gmail addresses
   - Click **"Save and Continue"**

3. Back to **"Credentials"** page, click **"Create Credentials"** > **"OAuth client ID"**

4. Configure OAuth client:
   - **Application type**: Web application
   - **Name**: MagicTech Email Integration
   - **Authorized redirect URIs**: Add these URLs:
     ```
     http://localhost:8085/api/oauth2/callback
     http://localhost:8085/oauth2/callback/google
     ```
     (Add production URLs when deploying)

5. Click **"Create"**

6. **Download credentials**:
   - You'll see a dialog with **Client ID** and **Client Secret**
   - Copy both values (you'll need them in the next step)
   - Click **"Download JSON"** to save credentials file (optional backup)

### Step 4: Note Your Credentials

Save these values securely:
```
Client ID: 1234567890-abc123xyz789.apps.googleusercontent.com
Client Secret: GOCSPX-abc123xyz789
```

---

## ⚙️ Application Configuration

### Option 1: Environment Variables (Recommended for Production)

Set these environment variables:

**Linux/Mac:**
```bash
export GOOGLE_CLIENT_ID="your_client_id_here"
export GOOGLE_CLIENT_SECRET="your_client_secret_here"
export GOOGLE_REDIRECT_URI="http://localhost:8085/api/oauth2/callback"
export ENCRYPTION_KEY="your-32-character-secure-key-1234"
```

**Windows:**
```cmd
set GOOGLE_CLIENT_ID=your_client_id_here
set GOOGLE_CLIENT_SECRET=your_client_secret_here
set GOOGLE_REDIRECT_URI=http://localhost:8085/api/oauth2/callback
set ENCRYPTION_KEY=your-32-character-secure-key-1234
```

**Docker Compose:**
```yaml
environment:
  - GOOGLE_CLIENT_ID=your_client_id_here
  - GOOGLE_CLIENT_SECRET=your_client_secret_here
  - GOOGLE_REDIRECT_URI=http://localhost:8085/api/oauth2/callback
  - ENCRYPTION_KEY=your-32-character-secure-key-1234
```

### Option 2: application.properties (Development Only)

Edit `/src/main/resources/application.properties`:

```properties
# Replace YOUR_GOOGLE_CLIENT_ID_HERE with your actual Client ID
spring.security.oauth2.client.registration.google.client-id=1234567890-abc123xyz789.apps.googleusercontent.com

# Replace YOUR_GOOGLE_CLIENT_SECRET_HERE with your actual Client Secret
spring.security.oauth2.client.registration.google.client-secret=GOCSPX-abc123xyz789

# Redirect URI (update for production)
spring.security.oauth2.client.registration.google.redirect-uri=http://localhost:8085/api/oauth2/callback
```

### Generate Encryption Key

The encryption key is used to encrypt OAuth2 tokens in the database. Generate a secure 32-character key:

**Using OpenSSL:**
```bash
openssl rand -base64 32
```

**Using Python:**
```python
import secrets
print(secrets.token_urlsafe(32))
```

**Or manually create:** Any 32+ character random string:
```
MagicTech2025SecureKey!@#$%^&*
```

---

## 🗄️ Database Schema

The system automatically creates the following table on first run:

### user_oauth2_tokens

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| user_id | BIGINT | Foreign key to users table (UNIQUE) |
| provider | VARCHAR(50) | OAuth provider ("GOOGLE") |
| access_token | TEXT | Encrypted access token |
| refresh_token | TEXT | Encrypted refresh token |
| token_type | VARCHAR(50) | Token type ("Bearer") |
| expires_at | TIMESTAMP | Token expiration time |
| scope | TEXT | Granted permissions |
| email | VARCHAR(100) | Google account email |
| created_at | TIMESTAMP | When token was created |
| updated_at | TIMESTAMP | Last token update |
| active | BOOLEAN | Token active status |

**Encryption:** `access_token` and `refresh_token` are encrypted using AES-128 before storage.

---

## 🔐 User Authentication Flow

### Step-by-Step Process

#### 1. User Clicks "Link Google Account"

From the UI (to be implemented in JavaFX):
```java
// Open browser to OAuth2 authorization URL
String authUrl = "http://localhost:8085/api/oauth2/authorize?userId=" + currentUser.getId();
Desktop.getDesktop().browse(new URI(authUrl));
```

#### 2. Server Redirects to Google

**Endpoint:** `GET /api/oauth2/authorize?userId={userId}`

Server generates Google OAuth URL:
```
https://accounts.google.com/o/oauth2/v2/auth
  ?client_id=YOUR_CLIENT_ID
  &redirect_uri=http://localhost:8085/api/oauth2/callback
  &response_type=code
  &scope=openid profile email https://www.googleapis.com/auth/gmail.send
  &state=123  // userId for callback
  &access_type=offline
  &prompt=consent
```

#### 3. User Approves Permissions

Google shows consent screen:
- **MagicTech Management System** wants to:
  - View your email address
  - View your basic profile info
  - Send email on your behalf

User clicks **"Allow"**

#### 4. Google Redirects Back

Google redirects to:
```
http://localhost:8085/api/oauth2/callback?code=AUTHORIZATION_CODE&state=123
```

#### 5. Server Exchanges Code for Tokens

**Endpoint:** `GET /api/oauth2/callback?code={code}&state={userId}`

Server:
1. Extracts `userId` from `state` parameter
2. Exchanges authorization `code` for tokens:
   ```json
   {
     "access_token": "ya29.a0AfH6SMC...",
     "refresh_token": "1//0gHZzF...",
     "expires_in": 3600,
     "token_type": "Bearer"
   }
   ```
3. Encrypts tokens using `EncryptionUtil`
4. Saves to database:
   ```sql
   INSERT INTO user_oauth2_tokens (user_id, provider, access_token, refresh_token, ...)
   VALUES (123, 'GOOGLE', 'encrypted_access', 'encrypted_refresh', ...)
   ```
5. Redirects to success page:
   ```
   http://localhost:8085/oauth2/success.html?userId=123
   ```

#### 6. User Sees Success Page

Success page shows:
- ✅ Google Account Connected!
- 📧 Emails will now be sent from your Gmail account
- 🔒 Your credentials are securely encrypted
- 🔔 Notifications will be delivered instantly

Page auto-closes after 10 seconds.

---

## 🔌 API Endpoints

### OAuth2 Authentication Endpoints

| Method | Endpoint | Description | Parameters |
|--------|----------|-------------|------------|
| GET | `/api/oauth2/health` | Health check | None |
| GET | `/api/oauth2/config/status` | Check if OAuth2 configured | None |
| GET | `/api/oauth2/authorize` | Start OAuth2 flow (redirects to Google) | `userId` |
| GET | `/api/oauth2/callback` | OAuth2 callback (handles Google redirect) | `code`, `state` |
| GET | `/api/oauth2/status` | Check if user has linked Google account | `userId` |
| POST | `/api/oauth2/revoke` | Disconnect Google account | `userId` |
| POST | `/api/oauth2/refresh` | Manually refresh access token | `userId` |
| POST | `/api/oauth2/test-email` | Test email sending (check auth only) | `userId`, `toEmail` |

### Example Requests

#### Check OAuth2 Configuration Status
```bash
curl http://localhost:8085/api/oauth2/config/status
```

**Response:**
```json
{
  "configured": true
}
```

#### Check User Authentication Status
```bash
curl http://localhost:8085/api/oauth2/status?userId=1
```

**Response (Authenticated):**
```json
{
  "authenticated": true,
  "userId": 1,
  "email": "user@gmail.com",
  "provider": "GOOGLE",
  "expiresAt": "2025-11-21T10:30:00"
}
```

**Response (Not Authenticated):**
```json
{
  "authenticated": false,
  "userId": 1
}
```

#### Revoke OAuth2 Token
```bash
curl -X POST http://localhost:8085/api/oauth2/revoke?userId=1
```

**Response:**
```json
{
  "success": true,
  "message": "Google account disconnected successfully"
}
```

---

## 🌐 WebSocket Integration

### Overview

WebSocket provides **real-time notification delivery** without polling. Notifications are pushed instantly to all connected clients.

### WebSocket Configuration

**Endpoint:** `ws://localhost:8085/ws/notifications`

**Message Broker Destinations:**
- `/topic/notifications/{role}` - Subscribe for role-based notifications
- `/queue/notifications` - User-specific notifications
- `/queue/notifications/count` - Unread count updates

### JavaScript Client Example (Web)

```javascript
// Connect to WebSocket
const socket = new SockJS('http://localhost:8085/ws/notifications');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame);

    // Subscribe to user-specific notifications
    stompClient.subscribe('/user/queue/notifications', function(message) {
        const notification = JSON.parse(message.body);
        console.log('New notification:', notification);
        showNotificationPopup(notification);
    });

    // Subscribe to role-based notifications (e.g., PROJECTS role)
    stompClient.subscribe('/topic/notifications/PROJECTS', function(message) {
        const notification = JSON.parse(message.body);
        console.log('Role notification:', notification);
        showNotificationPopup(notification);
    });

    // Subscribe to unread count updates
    stompClient.subscribe('/user/queue/notifications/count', function(message) {
        const data = JSON.parse(message.body);
        updateBadge(data.count);
    });
});
```

### JavaFX Client Integration (Future Enhancement)

For JavaFX desktop application, use:
- **Tyrus WebSocket Client** (Java WebSocket API)
- Or **Spring WebSocket StompClient**

---

## 🧪 Testing

### 1. Test OAuth2 Configuration

```bash
# Check if OAuth2 is configured
curl http://localhost:8085/api/oauth2/config/status
```

Expected: `{"configured": true}`

### 2. Test OAuth2 Flow (Manual)

1. Open browser to: `http://localhost:8085/api/oauth2/authorize?userId=1`
2. Login with Google account
3. Approve permissions
4. Should redirect to success page
5. Check database:
   ```sql
   SELECT * FROM user_oauth2_tokens WHERE user_id = 1;
   ```

### 3. Test Notification Creation

```bash
# Create test notification
curl -X POST http://localhost:8085/api/notifications/legacy/test/role/MASTER
```

Expected:
- Notification created in database
- Email sent via Gmail OAuth2 (if user has linked account)
- WebSocket notification pushed to connected clients

### 4. Test Email Sending

From within the application, trigger a notification:
```java
notificationService.createRoleNotification(
    "MASTER",           // Target role
    "STORAGE",          // Source module
    "LOW_STOCK",        // Notification type
    "Low Stock Alert",  // Title
    "Item XYZ is running low (Quantity: 5)",  // Message
    currentUser.getUsername()  // Created by
);
```

Check:
- ✅ Notification appears in database
- ✅ Email received in user's inbox (sent from their Gmail)
- ✅ WebSocket notification received (if connected)

### 5. Test Token Refresh

OAuth2 tokens expire after 1 hour. Test automatic refresh:

```java
// Manually trigger token refresh
OAuth2Service oauth2Service = context.getBean(OAuth2Service.class);
boolean success = oauth2Service.refreshAccessToken(userId);
System.out.println("Token refreshed: " + success);
```

---

## 🔒 Security Best Practices

### Production Deployment Checklist

- [ ] **Use environment variables** for OAuth2 credentials (never commit to Git)
- [ ] **Generate strong encryption key** (32+ random characters)
- [ ] **Enable HTTPS** for OAuth2 callbacks (required by Google)
- [ ] **Restrict OAuth redirect URIs** to production domains only
- [ ] **Enable Spring Security** (currently disabled for development)
- [ ] **Implement rate limiting** on OAuth2 endpoints
- [ ] **Add audit logging** for OAuth2 authentication events
- [ ] **Regularly rotate** encryption keys (implement key versioning)
- [ ] **Monitor token expiration** and refresh failures
- [ ] **Implement token revocation** on user logout/account deletion

### OAuth2 Security Features

| Feature | Implementation | Status |
|---------|----------------|--------|
| Token Encryption | AES-128 encryption | ✅ Implemented |
| State Parameter | CSRF protection | ✅ Implemented |
| Refresh Tokens | Automatic token refresh | ✅ Implemented |
| Token Expiration | 1 hour access token validity | ✅ Implemented |
| HTTPS Enforcement | Required for production | ⚠️ Configure in deployment |
| Scope Limitation | Only `gmail.send` permission | ✅ Implemented |

---

## 🔧 Troubleshooting

### Issue 1: "OAuth2 not configured"

**Symptoms:**
- Error message: "OAuth2 not configured. Please set GOOGLE_CLIENT_ID..."
- `/api/oauth2/config/status` returns `{"configured": false}`

**Solution:**
1. Verify environment variables are set:
   ```bash
   echo $GOOGLE_CLIENT_ID
   echo $GOOGLE_CLIENT_SECRET
   ```
2. Or check `application.properties` values are not placeholders
3. Restart application after setting environment variables

### Issue 2: "redirect_uri_mismatch"

**Symptoms:**
- Google OAuth error: "Error 400: redirect_uri_mismatch"

**Solution:**
1. Go to Google Cloud Console > Credentials
2. Edit OAuth 2.0 Client ID
3. Add exact redirect URI: `http://localhost:8085/api/oauth2/callback`
4. Ensure no trailing slash
5. Save and try again

### Issue 3: Tokens Not Refreshing

**Symptoms:**
- Error: "Failed to refresh token"
- Emails stop sending after 1 hour

**Solution:**
1. Check if `refresh_token` is stored in database:
   ```sql
   SELECT refresh_token FROM user_oauth2_tokens WHERE user_id = ?;
   ```
2. If NULL, user needs to re-authenticate (revoke and link again)
3. Ensure OAuth consent screen has `access_type=offline`

### Issue 4: WebSocket Connection Failed

**Symptoms:**
- WebSocket error: "Failed to connect to ws://localhost:8085/ws/notifications"

**Solution:**
1. Check if WebSocket dependency is in `pom.xml`:
   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-websocket</artifactId>
   </dependency>
   ```
2. Verify WebSocketConfig is loaded:
   ```bash
   # Check logs for: "Mapped WebSocket endpoint..."
   ```
3. Try SockJS fallback: `http://localhost:8085/ws/notifications`

### Issue 5: Encryption/Decryption Errors

**Symptoms:**
- Error: "Decryption failed"
- Tokens cannot be decrypted

**Solution:**
1. Check if `ENCRYPTION_KEY` environment variable is set
2. Ensure key is same across restarts (don't change mid-operation)
3. If key changed, users need to re-authenticate (old tokens cannot be decrypted)
4. Verify key is at least 16 characters

### Issue 6: Emails Not Sending

**Symptoms:**
- Notifications created but no emails received

**Debug Steps:**
1. Check if user has linked Google account:
   ```bash
   curl http://localhost:8085/api/oauth2/status?userId=1
   ```
2. Check application logs for errors:
   ```bash
   grep "OAuth2" logs/application.log
   ```
3. Verify user's email address is set in database:
   ```sql
   SELECT email FROM users WHERE id = ?;
   ```
4. Test Gmail API access:
   ```bash
   # Check if Gmail API is enabled in Google Cloud Console
   ```

---

## 📊 Monitoring & Logging

### Log Messages to Watch

**Successful OAuth2 authentication:**
```
✓ OAuth2 token saved for user 123 (user@gmail.com)
```

**Token refresh:**
```
🔄 Token expired for user 123, refreshing...
✓ Token refreshed for user 123
```

**Email sent:**
```
✓ Email sent via Gmail OAuth2: user@gmail.com → recipient@example.com
```

**WebSocket notification:**
```
📤 WebSocket notification sent to user 123
📤 WebSocket notification sent to role: PROJECTS
```

**Fallback to SMTP:**
```
⚠️ OAuth2 email failed for user 123, falling back to SMTP
✓ Email notification sent via SMTP to user: admin (admin@example.com)
```

### Database Queries for Monitoring

**Check active OAuth2 tokens:**
```sql
SELECT u.username, o.email, o.expires_at, o.updated_at
FROM user_oauth2_tokens o
JOIN users u ON o.user_id = u.id
WHERE o.active = true;
```

**Check recent notifications:**
```sql
SELECT module, type, title, created_at, is_read
FROM notifications
ORDER BY created_at DESC
LIMIT 20;
```

**Check users without OAuth2:**
```sql
SELECT u.id, u.username, u.email
FROM users u
LEFT JOIN user_oauth2_tokens o ON u.id = o.user_id
WHERE o.id IS NULL AND u.active = true;
```

---

## 🚀 Next Steps

### For Developers

1. **Build and run application:**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

2. **Link your Google account:**
   - Login to MagicTech as admin
   - Go to Settings > Notifications
   - Click "Link Google Account"

3. **Test notifications:**
   - Create a test notification from any module
   - Check email inbox
   - Verify WebSocket delivery (if connected)

### For System Administrators

1. **Set up Google Cloud project** (see [Google Cloud Setup](#google-cloud-setup))
2. **Configure environment variables** (see [Application Configuration](#application-configuration))
3. **Deploy to production** with HTTPS enabled
4. **Monitor OAuth2 token health** (check database regularly)
5. **Set up backup SMTP** (for fallback when OAuth2 unavailable)

### Future Enhancements

- [ ] **UI for OAuth2 management** in JavaFX application
- [ ] **Bulk user authentication** (admin can initiate for all users)
- [ ] **Microsoft OAuth2 support** (Outlook/Office 365)
- [ ] **Push notifications** via Firebase Cloud Messaging
- [ ] **SMS notifications** via Twilio
- [ ] **Notification templates** (customizable email templates)
- [ ] **Notification scheduler** (send at specific times)
- [ ] **Notification analytics** (delivery rates, open rates)

---

## 📚 Additional Resources

- [Google OAuth2 Documentation](https://developers.google.com/identity/protocols/oauth2)
- [Gmail API Documentation](https://developers.google.com/gmail/api)
- [Spring Boot OAuth2 Guide](https://spring.io/guides/tutorials/spring-boot-oauth2/)
- [WebSocket with Spring](https://spring.io/guides/gs/messaging-stomp-websocket/)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/index.html)

---

**Created:** 2025-11-20
**Version:** 1.0
**Author:** Claude AI Assistant
**Project:** MagicTech Management System
