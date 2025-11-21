# Add Notification System with Approval Workflow

## Summary

Implements a complete notification system with real-time messaging, missed notification recovery, and approval workflows for cross-module operations.

### ✅ LATEST UPDATES

#### 1. **Security: Role-Based Approval Notifications** 🔒 (NEW)
- Approval notifications now **only visible to authorized users**
- **Authorized roles:** SALES, STORAGE, MASTER
- **Blocked roles:** PROJECTS, MAINTENANCE, PRICING, CLIENT
- Prevents unauthorized users from seeing/approving project elements
- Enhanced security logging for authorization checks

#### 2. **Security: Force Dismiss on Logout** 🚪 (NEW)
- All popups **immediately dismissed** when user logs out
- Prevents approval notifications from persisting across user sessions
- No information leakage between users
- Added `dismissImmediately()` method for instant popup closure

#### 3. **Persistent Approval Notifications** 🔔
- Approval notifications now **stay visible until Accept/Reject is clicked**
- No auto-dismiss for approval requests (user must take action)
- Orange border indicator shows it's a persistent notification
- Regular notifications still auto-dismiss after 5 seconds

#### 4. **Notification Sound** 🔊
- Pleasant two-tone beep plays on all notifications
- Smooth fade in/out envelope for non-intrusive audio
- 50% volume for comfortable listening
- Fallback to system beep if sound file missing

#### 5. **Critical Timestamp Bug Fix** 🐛
Fixed timestamp bug that prevented missed notifications from loading. The issue was that `registerDevice()` was updating the `lastSeen` timestamp to NOW, then when querying for missed notifications, it would use this NEW timestamp instead of the OLD one, finding zero results. Now properly captures and uses the previous lastSeen timestamp.

### Features Implemented

#### 1. **Real-time Notification System**
- Redis pub/sub for instant notifications across modules
- PostgreSQL persistence for notification history
- Device registration and heartbeat monitoring
- Module-specific and broadcast notification channels

#### 2. **Missed Notification Recovery**
- Only shows notifications since last logout (not on every login)
- First-time login skips historical notifications
- Prevents duplicate notifications on repeated logins

#### 3. **Approval Workflow for Project Elements**
- When Projects module adds storage element → automatic notification to Sales
- Notification includes **Accept/Reject buttons** directly in popup
- Sales users can approve/reject with one click
- Project elements start with `PENDING_APPROVAL` status
- After approval → status changes to `APPROVED`
- After rejection → status changes to `REJECTED` with reason

#### 4. **Role-Based Notifications**
- Sales module: Receives approval requests for project elements
- Projects module: Receives project creation notifications
- Pricing module: Receives project completion notifications
- Storage/Master: Receives ALL notifications across modules

### Technical Details

**New Components:**
- `NotificationService` - Publishes notifications to Redis and PostgreSQL
- `NotificationListenerService` - Subscribes to Redis channels
- `NotificationManager` - UI integration for JavaFX application
- `NotificationPopup` - Animated notification popup with action buttons
- `DeviceRegistrationService` - Tracks online/offline devices

**Database Tables:**
- `notifications` - Stores all notification history
- `device_registrations` - Tracks device status and last seen time

**Workflow:**
```
Projects adds element → Notification saved to DB → Published to Redis
                                                 ↓
                     Sales user online? → Show popup immediately
                     Sales user offline? → Store for later
                                                 ↓
                     Sales logs in → Load missed notifications → Show popup with Accept/Reject buttons
                                                 ↓
                     Sales clicks Accept → Element status = APPROVED
```

### Testing Instructions

#### Test 1: Real-time Notifications
1. Login as **yahya** (MASTER)
2. Open **Sales** module
3. Create a new project
4. In another window, login as different user in **Projects** module
5. **Should see notification immediately**

#### Test 2: Missed Notifications
1. Login as **yahya** (MASTER)
2. Open **Sales** module
3. Create a new project
4. **Logout**
5. Login as **mosa** (SALES)
6. **Should see the missed notification popup**

#### Test 3: Approval Workflow
1. Login as **yahya** (MASTER)
2. Open **Projects** module
3. Open any project
4. **Add a storage element** (e.g., "NVR", quantity: 5)
5. **Logout**
6. Login as **mosa** (SALES)
7. **Should see approval notification with Accept/Reject buttons**
8. Click **Accept** → Element is approved
9. Go back to Projects module → Element status should be "APPROVED"

#### Test 4: No Duplicate Notifications
1. Login as **mosa** (SALES)
2. See notification
3. **Logout and login again**
4. **Should NOT see the same notification again**

#### Test 5: Persistent Approval Notifications (NEW)
1. Login as **yahya** (MASTER)
2. Open **Projects** module
3. Add a storage element to any project
4. **Logout**
5. Login as **mosa** (SALES)
6. **Approval notification appears with orange border**
7. **Wait 10 seconds** - notification should **STILL be visible** (not auto-dismiss)
8. Click **Accept** or **Reject** - notification should dismiss
9. **Expected:** Approval notifications persist until action is taken

#### Test 6: Notification Sound 🔊
1. Login as any user
2. Have another user create a notification (or wait for missed notification on login)
3. **Expected:** Hear a pleasant two-tone beep sound
4. Sound should be moderate volume (50%) and not jarring
5. If sound file is missing, system beep should play as fallback

#### Test 7: Security - Role-Based Filtering 🔒 (NEW)
1. Login as **yahya** (PROJECTS role)
2. Open **Projects** module
3. Add a storage element to any project
4. **Logout**
5. Login as **yahya** again (or another PROJECTS user)
6. **Expected:** NO approval notification shown (blocked by role)
7. Check logs: "User yahya (role: PROJECTS) is not authorized to see approval notifications"

#### Test 8: Security - Force Dismiss on Logout 🚪 (NEW)
1. Login as **mosa** (SALES role)
2. Approval notification appears with orange border
3. **DO NOT click Accept/Reject**
4. Click **Logout** button
5. **Expected:** Popup disappears immediately
6. Login as different user (any role)
7. **Expected:** Old notification NOT visible
8. Check logs: "Dismissing X active popup(s)" and "Dismissed popup immediately"

### Bug Fix Details

**Problem:**
When users logged in, they saw zero notifications even when notifications were created while they were offline.

**Root Cause:**
```
1. User logs in → registerDevice() called
2. registerDevice() updates lastSeen = NOW and saves to DB
3. loadMissedNotifications() calls getLastSeenTime()
4. getLastSeenTime() returns the NEW timestamp (just saved)
5. Query: SELECT * FROM notifications WHERE timestamp > NOW
6. Result: 0 notifications (because they were created BEFORE now)
```

**Solution:**
```
1. Capture OLD lastSeen timestamp BEFORE updating device
2. Store it in previousLastSeen instance variable
3. Added getPreviousLastSeen() method
4. NotificationManager now uses getPreviousLastSeen() for queries
5. Query: SELECT * FROM notifications WHERE timestamp > OLD_TIMESTAMP
6. Result: All notifications since last logout ✅
```

### Files Changed

**Core Notification System:**
- `NotificationManager.java` - UI integration and missed notification loading (✅ Fixed timestamp query)
- `NotificationService.java` - Notification publishing and storage
- `NotificationListenerService.java` - Redis subscription handling
- `NotificationPopup.java` - Popup UI with approval buttons (✅ Persistent approval popups + sound)
- `DeviceRegistrationService.java` - Device tracking (✅ Captures previous lastSeen)
- `NotificationSoundGenerator.java` - Utility to generate notification sound (NEW)
- `notification.wav` - Notification sound file (NEW, 35KB)

**Approval Workflow:**
- `ProjectElementService.java` - Sends approval notification on element creation
- `NotificationConstants.java` - Notification type constants
- `NotificationMessage.java` - DTO for notification data

**Database:**
- `Notification.java` - Notification entity
- `DeviceRegistration.java` - Device tracking entity
- `NotificationRepository.java` - Notification queries

**Configuration:**
- `MainApp.java` - Exposed Spring context for non-Spring components

### Notes

- ✅ Notifications are persisted in database
- ✅ Real-time delivery via Redis pub/sub
- ✅ Missed notifications loaded on login
- ✅ No duplicate notifications
- ✅ Approval workflow with Accept/Reject
- ✅ Role-based notification filtering
- ✅ **Persistent approval popups** (stay until user action)
- ✅ **Auto-dismiss regular notifications** (5 seconds)
- ✅ **Notification sound** with pleasant two-tone beep
- ✅ **Visual indicators** (orange border for persistent notifications)
- ✅ **Security: Role-based approval filtering** (SALES, STORAGE, MASTER only)
- ✅ **Security: Force dismiss on logout** (no persistence across sessions)
- ✅ **Security: Enhanced logging** for authorization and cleanup

### Future Enhancements

- [ ] Notification center/history view
- [ ] Mark as read functionality
- [ ] Notification preferences per user
- [ ] Email notifications for critical events
- [ ] Push notifications to mobile devices

---

**Ready for review and testing!** 🚀
