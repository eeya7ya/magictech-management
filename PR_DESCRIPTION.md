# Add Notification System with Approval Workflow

## Summary

Implements a complete notification system with real-time messaging, missed notification recovery, and approval workflows for cross-module operations.

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

### Files Changed

**Core Notification System:**
- `NotificationManager.java` - UI integration and missed notification loading
- `NotificationService.java` - Notification publishing and storage
- `NotificationListenerService.java` - Redis subscription handling
- `NotificationPopup.java` - Popup UI with approval buttons
- `DeviceRegistrationService.java` - Device tracking

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

### Future Enhancements

- [ ] Notification center/history view
- [ ] Mark as read functionality
- [ ] Notification preferences per user
- [ ] Email notifications for critical events
- [ ] Push notifications to mobile devices

---

**Ready for review and testing!** 🚀
