# 📧 Email Notification Setup Guide

## Overview

MagicTech Management System includes a comprehensive email notification system that automatically sends emails when important events occur:

- **Project Creation**: When Sales creates a project → Email to Projects team
- **Element Approval Requests**: When Projects adds elements → Email to Sales team
- **Approval Confirmations**: When Sales approves/rejects → Email to requester
- **Low Stock Alerts**: When inventory is low → Email to Storage team
- **System Notifications**: Important system events → Email to relevant users

## ⚠️ Important: Gmail Configuration Required

The email system is **fully implemented** but requires **Gmail App Password** configuration.

### Why You're Not Receiving Emails

If you added your email but aren't receiving notifications, it's because:

1. **Placeholder values are still in `application.properties`**
2. **Gmail App Password not configured** (regular password won't work!)
3. **Email service safety check** prevents sending with placeholder values

## 🔧 Setup Instructions

### Step 1: Enable Gmail App Password

1. **Go to Google Account Security**
   - Visit: https://myaccount.google.com/security

2. **Enable 2-Step Verification** (if not already enabled)
   - Click "2-Step Verification"
   - Follow the setup wizard
   - This is **REQUIRED** for App Passwords

3. **Generate App Password**
   - Visit: https://myaccount.google.com/apppasswords
   - Select "Mail" as the app
   - Select "Other (Custom name)" as the device
   - Enter "MagicTech Management System"
   - Click "Generate"

4. **Copy the 16-character password**
   - It will look like: `abcd efgh ijkl mnop`
   - **IMPORTANT**: Save this password - you won't see it again!

### Step 2: Configure Application

1. **Open Configuration File**
   ```bash
   nano src/main/resources/application.properties
   ```

2. **Update Email Settings**

   Find these lines (around line 77-91):
   ```properties
   # Email Configuration
   spring.mail.host=smtp.gmail.com
   spring.mail.port=587
   spring.mail.username=YOUR_EMAIL@gmail.com              # ← CHANGE THIS
   spring.mail.password=YOUR_APP_PASSWORD_HERE            # ← CHANGE THIS
   spring.mail.properties.mail.smtp.auth=true
   spring.mail.properties.mail.smtp.starttls.enable=true
   spring.mail.properties.mail.smtp.starttls.required=true
   spring.mail.properties.mail.smtp.connectiontimeout=5000
   spring.mail.properties.mail.smtp.timeout=5000
   spring.mail.properties.mail.smtp.writetimeout=5000

   # Email notification settings
   app.email.enabled=true
   app.email.from.name=MagicTech Management System
   app.email.from.address=YOUR_EMAIL@gmail.com            # ← CHANGE THIS
   ```

3. **Replace with your credentials**
   ```properties
   spring.mail.username=yourname@gmail.com
   spring.mail.password=abcd efgh ijkl mnop
   app.email.from.address=yourname@gmail.com
   ```

   **Example (Real Configuration):**
   ```properties
   spring.mail.username=john.doe@gmail.com
   spring.mail.password=wxyz abcd efgh mnop
   app.email.from.address=john.doe@gmail.com
   ```

4. **Save and Close**
   - Press `Ctrl+O` to save
   - Press `Ctrl+X` to exit

### Step 3: Add Email Addresses to Users

Users must have email addresses in their profiles to receive notifications.

**Option A: Via Database**
```sql
-- Update user emails directly in PostgreSQL
UPDATE users SET email = 'user1@gmail.com', phone_number = '+1234567890' WHERE username = 'admin';
UPDATE users SET email = 'sales@gmail.com' WHERE role = 'SALES';
UPDATE users SET email = 'projects@gmail.com' WHERE role = 'PROJECTS';
```

**Option B: Via REST API**
```bash
curl -X PUT http://localhost:8085/api/auth/users/1 \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@gmail.com",
    "phoneNumber": "+1234567890"
  }'
```

**Option C: Via UI** (if User Management screen exists)
- Go to User Management
- Edit user profile
- Add email address
- Save

### Step 4: Restart Application

```bash
# Stop current application (Ctrl+C if running)

# Restart
mvn spring-boot:run
```

**Look for startup logs:**
```
========================================
📧 EMAIL CONFIGURATION CHECK
========================================
✅ Email Configuration: OK

   Configuration:
   • Host: smtp.gmail.com
   • Port: 587
   • From: yourname@gmail.com

   Status: Ready to send email notifications
```

If you see **❌ EMAIL NOT CONFIGURED!**, check your `application.properties` again.

### Step 5: Test Email Sending

**Option A: Using Test Script** (Recommended)
```bash
./test-email.sh
```

**Option B: Manual cURL**
```bash
# Check configuration status
curl http://localhost:8085/api/email/status | jq

# Send test email
curl -X POST http://localhost:8085/api/email/test \
  -H "Content-Type: application/json" \
  -d '{"email":"your-email@gmail.com"}'
```

**Option C: Create a Real Notification**

1. Log in as Sales user
2. Create a new project
3. Check Projects team email inbox
4. You should receive a beautiful HTML email!

## 🎨 Email Features

### Beautiful HTML Templates
- Gradient background design
- Priority-based color coding:
  - 🚨 **URGENT** - Red
  - ⚠️ **HIGH** - Orange
  - 🔔 **NORMAL** - Blue
  - ℹ️ **LOW** - Gray

### Smart Delivery
- **Asynchronous sending** - Doesn't block application
- **User preferences** - Respects quiet hours and notification settings
- **Retry logic** - Automatic retry on temporary failures
- **Priority filtering** - Users can disable low-priority emails

### Notification Types
- `PROJECT_CREATED` - New project assigned
- `PROJECT_COMPLETED` - Project finished
- `APPROVAL_REQUEST` - Requires approval
- `APPROVAL_CONFIRMED` - Request approved
- `APPROVAL_REJECTED` - Request rejected
- `LOW_STOCK_ALERT` - Inventory low
- `ELEMENT_ADDED` - Project element added

## 🧪 Testing Endpoints

### 1. Health Check
```bash
curl http://localhost:8085/api/email/health
```

**Response:**
```json
{
  "status": "UP",
  "service": "Email Test API",
  "emailConfigured": "true"
}
```

### 2. Configuration Status
```bash
curl http://localhost:8085/api/email/status | jq
```

**Response (Configured):**
```json
{
  "emailEnabled": true,
  "emailConfigured": true,
  "configurationMessage": "Email service is configured and ready",
  "configuration": {
    "mailHost": "smtp.gmail.com",
    "mailPort": "587",
    "fromAddress": "yourname@gmail.com",
    "username": "yourname@gmail.com",
    "isPlaceholder": "NO ✓"
  }
}
```

**Response (NOT Configured):**
```json
{
  "emailEnabled": true,
  "emailConfigured": false,
  "configurationMessage": "Email address not configured...",
  "configuration": {
    "mailHost": "smtp.gmail.com",
    "mailPort": "587",
    "fromAddress": "YOUR_EMAIL@gmail.com",
    "username": "YOUR_EMAIL@gmail.com",
    "isPlaceholder": "YES ⚠️"
  },
  "setupInstructions": { ... }
}
```

### 3. Send Test Email
```bash
curl -X POST http://localhost:8085/api/email/test \
  -H "Content-Type: application/json" \
  -d '{"email":"recipient@gmail.com"}'
```

**Response (Success):**
```json
{
  "success": true,
  "message": "Test email sent successfully! Check your inbox.",
  "sentTo": "recipient@gmail.com"
}
```

**Response (Not Configured):**
```json
{
  "error": "Email is not configured properly",
  "message": "Email address not configured...",
  "setupInstructions": { ... }
}
```

## 🔍 Troubleshooting

### Issue 1: "Email NOT CONFIGURED" Error

**Symptom:** Logs show:
```
📧 Email NOT CONFIGURED! Current fromAddress: 'YOUR_EMAIL@gmail.com'
```

**Solution:**
- You're still using placeholder values
- Update `application.properties` with real Gmail credentials
- Restart application

### Issue 2: "AUTHENTICATION FAILED" Error

**Symptom:** Logs show:
```
❌ AUTHENTICATION FAILED! Gmail credentials are incorrect
```

**Solutions:**
1. **Using regular password instead of App Password**
   - Regular Gmail passwords don't work!
   - You MUST use a 16-character App Password
   - Generate one at: https://myaccount.google.com/apppasswords

2. **2-Step Verification not enabled**
   - App Passwords require 2-Step Verification
   - Enable at: https://myaccount.google.com/security

3. **Incorrect App Password**
   - Copy the password exactly as shown
   - Include spaces: `abcd efgh ijkl mnop`
   - Or remove all spaces: `abcdefghijklmnop`

### Issue 3: Not Receiving Emails

**Possible Causes:**

1. **User has no email address**
   ```sql
   -- Check user emails
   SELECT id, username, email, role FROM users;

   -- Update if missing
   UPDATE users SET email = 'user@gmail.com' WHERE username = 'admin';
   ```

2. **Check spam folder**
   - Gmail might filter automated emails
   - Mark as "Not Spam" to whitelist

3. **Notification preferences disabled**
   ```sql
   -- Check user preferences
   SELECT * FROM notification_preferences WHERE user_id = 1;

   -- Enable all notifications
   UPDATE notification_preferences SET email_enabled = true WHERE user_id = 1;
   ```

4. **Email delivery is async**
   - Wait 10-30 seconds
   - Check application logs for errors

### Issue 4: SMTP Connection Timeout

**Symptom:**
```
Connection timeout: smtp.gmail.com:587
```

**Solutions:**
1. **Firewall blocking port 587**
   ```bash
   # Test SMTP connection
   telnet smtp.gmail.com 587
   ```

2. **Corporate network restrictions**
   - Some networks block SMTP
   - Try from different network
   - Or use VPN

3. **Gmail blocking your IP**
   - Too many failed attempts
   - Wait 1 hour and try again
   - Or visit: https://accounts.google.com/DisplayUnlockCaptcha

### Issue 5: Emails Going to Spam

**Solutions:**
1. **Add sender to contacts**
   - Add your Gmail to recipient's contacts

2. **Mark as "Not Spam"**
   - Find email in spam folder
   - Click "Not Spam"
   - Gmail will learn

3. **Set up SPF/DKIM** (Advanced)
   - For production use
   - Requires domain configuration

## 📊 Monitoring Email Delivery

### Check Application Logs

```bash
# Watch logs in real-time
tail -f logs/spring.log

# Search for email activity
grep "📧" logs/spring.log
grep "✅ Email SENT" logs/spring.log
grep "❌" logs/spring.log
```

**Successful send:**
```
📧 Attempting to send email to: user@gmail.com | Subject: 🔔 New Project Created
✅ Email SENT successfully! To: user@gmail.com | From: yourname@gmail.com
```

**Failed send:**
```
📧 Email NOT CONFIGURED! Current fromAddress: 'YOUR_EMAIL@gmail.com'
```

### Database Monitoring

```sql
-- Check notifications created
SELECT id, title, type, target_role, created_at, is_read
FROM notifications
ORDER BY created_at DESC
LIMIT 10;

-- Check user preferences
SELECT u.username, np.email_enabled, np.quiet_hours_start, np.quiet_hours_end
FROM users u
LEFT JOIN notification_preferences np ON u.id = np.user_id;
```

## 🔐 Security Best Practices

### DO:
✅ Use Gmail App Passwords (16-character)
✅ Keep credentials in `.gitignore`
✅ Use environment variables in production
✅ Enable 2-Step Verification
✅ Regularly rotate App Passwords

### DON'T:
❌ Use regular Gmail password
❌ Commit credentials to Git
❌ Share App Passwords
❌ Disable 2-Step Verification
❌ Use same password for multiple apps

### Production Deployment

For production, use environment variables:

```properties
# application.properties (production)
spring.mail.username=${GMAIL_USERNAME}
spring.mail.password=${GMAIL_APP_PASSWORD}
app.email.from.address=${GMAIL_FROM_ADDRESS}
```

Set via environment:
```bash
export GMAIL_USERNAME=yourname@gmail.com
export GMAIL_APP_PASSWORD=your-app-password
export GMAIL_FROM_ADDRESS=yourname@gmail.com
```

Or via `.env` file (with proper .gitignore):
```bash
GMAIL_USERNAME=yourname@gmail.com
GMAIL_APP_PASSWORD=your-app-password
GMAIL_FROM_ADDRESS=yourname@gmail.com
```

## 📞 Support

### Still Having Issues?

1. **Check startup logs** for configuration warnings
2. **Run test script**: `./test-email.sh`
3. **Check API status**: `curl http://localhost:8085/api/email/status`
4. **Review this guide** - most issues are covered above
5. **Check Gmail settings** - verify 2-Step and App Passwords

### Quick Checklist

- [ ] 2-Step Verification enabled on Gmail
- [ ] App Password generated (16 characters)
- [ ] `application.properties` updated with real credentials
- [ ] Application restarted after config changes
- [ ] Startup logs show "✅ Email Configuration: OK"
- [ ] Users have valid email addresses in database
- [ ] Test email API returns success
- [ ] Test email received in inbox (check spam too!)

## 🎉 Success!

Once configured, you should see:

1. **Startup logs:**
   ```
   ✅ Email Configuration: OK
   Status: Ready to send email notifications
   ```

2. **Test email response:**
   ```json
   {"success": true, "message": "Test email sent successfully!"}
   ```

3. **Beautiful HTML emails in inbox** with gradient backgrounds and action buttons!

---

**Last Updated:** 2025-11-19
**Version:** 1.0
**Contact:** MagicTech Development Team
