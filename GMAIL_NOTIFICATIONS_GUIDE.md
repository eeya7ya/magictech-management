# Gmail Sign-In & Notifications Guide

**MagicTech Management System - Gmail Integration**

Last Updated: 2025-11-20

---

## Table of Contents

1. [Overview](#overview)
2. [Gmail Sign-In Setup](#gmail-sign-in-setup)
3. [User Guide: How to Link Your Gmail Account](#user-guide-how-to-link-your-gmail-account)
4. [Developer Guide: Sending Notifications](#developer-guide-sending-notifications)
5. [API Endpoints](#api-endpoints)
6. [Troubleshooting](#troubleshooting)

---

## Overview

The MagicTech Management System now supports **Gmail Sign-In** and **automatic email notifications**. Users can link their Gmail accounts to:

- **Sign in using Google OAuth2** (in addition to traditional username/password)
- **Receive email notifications** for important events (projects, orders, approvals, etc.)
- **Send emails** to other users directly from the system

### Key Features

✅ **Secure OAuth2 authentication** - No password storage, uses Google's secure token system
✅ **Automatic email notifications** - Users receive emails for in-app notifications
✅ **Easy-to-use API** - Simple methods to send notifications to users
✅ **Role-based notifications** - Send notifications to all users with a specific role
✅ **Desktop & Web support** - Works in both JavaFX desktop app and REST API

---

## Gmail Sign-In Setup

### Prerequisites

1. **Google Cloud Project** with Gmail API enabled
2. **OAuth2 Credentials** (Client ID and Client Secret)
3. **Redirect URI** configured: `http://localhost:8085/api/oauth2/callback`

### Configuration Steps

#### 1. Create Google Cloud Project

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project (or select existing)
3. Name it "MagicTech Management System"

#### 2. Enable Gmail API

1. Go to **APIs & Services** → **Enable APIs and Services**
2. Search for "Gmail API"
3. Click **Enable**

#### 3. Create OAuth2 Credentials

1. Go to **APIs & Services** → **Credentials**
2. Click **Create Credentials** → **OAuth client ID**
3. Choose **Web application**
4. Set **Name**: `MagicTech Management System`
5. Add **Authorized redirect URIs**:
   - `http://localhost:8085/api/oauth2/callback`
   - `http://localhost:8085/oauth2/callback/google`
6. Click **Create**
7. **Copy the Client ID and Client Secret**

#### 4. Configure Application

Set environment variables (recommended):

```bash
export GOOGLE_CLIENT_ID="your_client_id_here"
export GOOGLE_CLIENT_SECRET="your_client_secret_here"
export ENCRYPTION_KEY="your_32_character_encryption_key"
```

Or update `application.properties`:

```properties
spring.security.oauth2.client.registration.google.client-id=your_client_id_here
spring.security.oauth2.client.registration.google.client-secret=your_client_secret_here
app.security.encryption.key=your_32_character_encryption_key
```

#### 5. Restart Application

```bash
mvn spring-boot:run
```

---

## User Guide: How to Link Your Gmail Account

### Method 1: Sign In with Google (Desktop App)

1. **Open MagicTech Management System**
2. On the login screen, click **"🔐 Sign in with Google"**
3. Enter your MagicTech username when prompted
4. **Browser will open** → Sign in to your Google account
5. **Grant permissions** to MagicTech (allow sending emails)
6. **Wait for confirmation** → You'll be automatically logged in
7. **Done!** Your Gmail account is now linked

### Method 2: Link Gmail After Login (Web/Desktop)

1. **Log in** to MagicTech with your username and password
2. Go to **Settings** or **Profile**
3. Click **"Link Gmail Account"**
4. Follow the OAuth flow in your browser
5. **Done!** You can now receive and send email notifications

### How to Check if Gmail is Linked

- Look for **"📧 Gmail Connected"** badge in your profile
- Or check via API: `GET /api/oauth2/status?userId=YOUR_ID`

### How to Unlink Gmail

- Go to **Settings** → **"Disconnect Gmail"**
- Or use API: `POST /api/oauth2/revoke?userId=YOUR_ID`

---

## Developer Guide: Sending Notifications

### Using NotificationHelper (Recommended)

The `NotificationHelper` class provides simple methods to send notifications to users. Users who have linked their Gmail accounts will **automatically receive email notifications**.

#### 1. Inject NotificationHelper

```java
@Autowired
private NotificationHelper notificationHelper;
```

#### 2. Send Notification to a Specific User

```java
// Normal priority
notificationHelper.notifyUser(userId, "Task Complete", "Your task has been completed successfully");

// By username
notificationHelper.notifyUser("john", "New Message", "You have a new message from admin");

// Urgent priority
notificationHelper.notifyUserUrgent(userId, "Critical Alert", "Immediate action required!");

// High priority
notificationHelper.notifyUserHigh(userId, "Important Update", "Please review the new project details");
```

#### 3. Send Notification to All Users with a Role

```java
// Normal priority
notificationHelper.notifyRole(UserRole.SALES, "New Order", "A new sales order has been created");

// Urgent priority
notificationHelper.notifyRoleUrgent(UserRole.MASTER, "System Alert", "Server maintenance in 10 minutes");

// High priority
notificationHelper.notifyRoleHigh(UserRole.PROJECTS, "Deadline Approaching", "Project deadline is tomorrow");
```

#### 4. Send Module-Specific Notifications

```java
// Project update
notificationHelper.notifyProjectUpdate(userId, projectId, "Project Updated", "Project XYZ has been updated");

// Sales order
notificationHelper.notifySalesOrder(userId, orderId, "Order Confirmed", "Order #12345 has been confirmed");

// Storage alert
notificationHelper.notifyStorageAlert(UserRole.STORAGE, itemId, "Low Stock Alert", "Item ABC is running low");
```

#### 5. Send Direct Email (Without In-App Notification)

```java
// Send email to external address
notificationHelper.sendEmailDirect(fromUserId, "external@example.com", "Hello", "This is a test email");

// Send email to another user in the system
notificationHelper.sendEmailToUser(fromUserId, toUserId, "Message", "Hi, this is a message for you");
```

#### 6. Utility Methods

```java
// Check if user has Gmail linked
boolean hasGmail = notificationHelper.hasGmailLinked(userId);
boolean hasGmail = notificationHelper.hasGmailLinked("john");

// Get users by role
List<User> salesUsers = notificationHelper.getUsersByRole(UserRole.SALES);

// Get unread notification count
long unreadCount = notificationHelper.getUnreadCount(userId, UserRole.SALES);

// Mark all as read
notificationHelper.markAllAsRead(userId, UserRole.SALES);
```

### Example: Send Notification When Project is Created

```java
@Service
public class ProjectService {

    @Autowired
    private NotificationHelper notificationHelper;

    @Transactional
    public Project createProject(Project project, User creator) {
        // Save project
        Project saved = projectRepository.save(project);

        // Notify all PROJECTS role users
        notificationHelper.notifyRole(
            UserRole.PROJECTS,
            "🚀 New Project Created: " + project.getProjectName(),
            "A new project has been created by " + creator.getUsername() +
            ". Location: " + project.getProjectLocation()
        );

        // Notify MASTER role with HIGH priority
        notificationHelper.notifyRoleHigh(
            UserRole.MASTER,
            "New Project Requires Approval",
            "Project '" + project.getProjectName() + "' has been created and requires your approval."
        );

        return saved;
    }
}
```

### Example: Send Notification for Low Stock

```java
@Service
public class StorageService {

    @Autowired
    private NotificationHelper notificationHelper;

    public void checkLowStockItems() {
        List<StorageItem> lowStockItems = findLowStockItems();

        for (StorageItem item : lowStockItems) {
            notificationHelper.notifyStorageAlert(
                UserRole.STORAGE,
                item.getId(),
                "⚠️ Low Stock Alert: " + item.getProductName(),
                "Current quantity: " + item.getQuantity() + ". Please reorder."
            );
        }
    }
}
```

---

## API Endpoints

### OAuth2 Authentication

#### 1. Check OAuth2 Configuration Status

```bash
GET /api/oauth2/config/status
```

Response:
```json
{
  "configured": true
}
```

#### 2. Initiate OAuth2 Flow (Redirect to Google)

```bash
GET /api/oauth2/authorize?userId=123
```

This redirects the user to Google consent screen.

#### 3. OAuth2 Callback (Automatic)

```bash
GET /api/oauth2/callback?code=xxx&state=userId
```

This is called automatically by Google after user grants permission.

#### 4. Check User OAuth2 Status

```bash
GET /api/oauth2/status?userId=123
```

Response:
```json
{
  "authenticated": true,
  "userId": 123,
  "email": "john@gmail.com",
  "provider": "GOOGLE",
  "expiresAt": "2025-11-21T10:30:00"
}
```

#### 5. Revoke OAuth2 Token (Disconnect Gmail)

```bash
POST /api/oauth2/revoke?userId=123
```

Response:
```json
{
  "success": true,
  "message": "Google account disconnected successfully"
}
```

#### 6. Refresh Access Token

```bash
POST /api/oauth2/refresh?userId=123
```

Response:
```json
{
  "success": true,
  "message": "Token refreshed successfully"
}
```

### Notification API

#### 1. Get Unread Notifications

```bash
GET /api/notifications/unread?userId=123&role=SALES
```

#### 2. Mark Notification as Read

```bash
POST /api/notifications/{notificationId}/read
```

#### 3. Mark All as Read

```bash
POST /api/notifications/mark-all-read?userId=123&role=SALES
```

#### 4. Get Notification Count

```bash
GET /api/notifications/count?userId=123&role=SALES
```

---

## Troubleshooting

### Issue: "Gmail Sign-In is not configured"

**Solution**: Set up OAuth2 credentials in Google Cloud Console and configure environment variables.

```bash
export GOOGLE_CLIENT_ID="your_client_id_here"
export GOOGLE_CLIENT_SECRET="your_client_secret_here"
```

### Issue: "User has not linked their Google account"

**Solution**: User needs to link their Gmail account first:
1. Click "Sign in with Google" on login screen, OR
2. Go to Settings and click "Link Gmail Account"

### Issue: OAuth2 timeout during sign-in

**Solution**:
- Make sure the redirect URI in Google Cloud Console matches: `http://localhost:8085/api/oauth2/callback`
- Check that port 8085 is not blocked by firewall
- Try again and complete the OAuth flow within 2 minutes

### Issue: Emails not being sent

**Possible Causes**:
1. User hasn't linked Gmail account → Link Gmail first
2. OAuth2 token expired → Token auto-refreshes, but may need to re-authenticate
3. Gmail API quota exceeded → Check Google Cloud Console quotas
4. User disabled email notifications → Check notification preferences

**Solution**: Check OAuth2 status:
```bash
curl http://localhost:8085/api/oauth2/status?userId=123
```

### Issue: "Failed to refresh token"

**Solution**:
- Re-authenticate by going through OAuth flow again
- Revoke and re-link Gmail account

### Issue: Desktop app can't open browser

**Solution**:
- Make sure Java has permissions to open browser
- On Linux, install `xdg-open`
- On macOS, make sure system browser is configured
- Alternatively, manually copy the authorization URL and paste in browser

---

## Security Best Practices

1. **Never commit credentials** to Git
   - Use environment variables
   - Add `.env` files to `.gitignore`

2. **Use HTTPS in production**
   - Update redirect URI to `https://your-domain.com/api/oauth2/callback`

3. **Rotate encryption keys regularly**
   - Update `ENCRYPTION_KEY` environment variable

4. **Limit OAuth2 scopes**
   - Only request necessary permissions (currently: `gmail.send`)

5. **Enable 2FA for admin accounts**
   - Protect master accounts with Google 2-Factor Authentication

---

## Support

For questions or issues:
- Check application logs: `logs/application.log`
- Enable DEBUG logging: Set `logging.level.com.magictech=DEBUG` in `application.properties`
- Contact system administrator

---

**Happy Notifying! 📧**

© 2025 MagicTech Management System
