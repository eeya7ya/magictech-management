# Notification System Documentation

## Overview

The MagicTech Management System now includes a **comprehensive notification system** with multiple delivery channels:

- ✅ **In-app notifications** (notification center with bell icon)
- ✅ **Desktop notifications** (OS-level system tray alerts)
- ✅ **Email notifications** (via Gmail SMTP)
- ✅ **Real-time updates** (auto-polling every 5 seconds)
- ✅ **Event-driven architecture** (publish notifications from anywhere)

---

## Quick Start

### 1. **Basic Usage: Create a Notification**

```java
@Autowired
private NotificationService notificationService;

// Simple notification
notificationService.createNotification(
    userId,
    "New Sales Order",
    "Order #12345 has been created successfully",
    NotificationType.NEW_SALES_ORDER,
    NotificationPriority.HIGH
);
```

### 2. **Advanced Usage: With Module Context**

```java
notificationService.createNotification(
    userId,
    "Project Assigned",
    "You have been assigned to Project XYZ",
    NotificationType.PROJECT_ASSIGNED,
    NotificationPriority.NORMAL,
    "PROJECTS",              // module source
    projectId,               // reference ID
    "Project",               // reference type
    "/projects/" + projectId // action URL
);
```

### 3. **Event-Driven Usage: Publish Notifications**

```java
@Autowired
private ApplicationEventPublisher eventPublisher;

// Publish notification event (handled asynchronously)
NotificationEvent event = new NotificationEvent.Builder(this)
    .userId(userId)
    .title("Low Stock Alert")
    .message("Product XYZ is running low on stock")
    .type(NotificationType.LOW_STOCK_ALERT)
    .priority(NotificationPriority.HIGH)
    .moduleSource("STORAGE")
    .referenceId(storageItemId)
    .referenceType("StorageItem")
    .build();

eventPublisher.publishEvent(event);
```

### 4. **Notify Multiple Users**

```java
// Notify all users with MASTER role
notificationService.notifyUsersByRole(
    "MASTER",
    "System Alert",
    "Database backup completed successfully",
    NotificationType.SYSTEM_ALERT,
    NotificationPriority.NORMAL
);

// Notify all active users
notificationService.notifyAllUsers(
    "Maintenance Notice",
    "System will be down for maintenance at 2 AM",
    NotificationType.SYSTEM_ALERT,
    NotificationPriority.HIGH
);
```

---

## Configuration

### Email Configuration (Gmail)

Update `application.properties`:

```properties
# Gmail SMTP Settings
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=YOUR_EMAIL@gmail.com
spring.mail.password=YOUR_APP_PASSWORD  # Generate at https://myaccount.google.com/apppasswords

# Notification Settings
app.notifications.enabled=true
app.notifications.email.enabled=true
app.notifications.desktop.enabled=true
```

### Notification Priorities

| Priority | Email | Desktop | Color  | Use Case                          |
|----------|-------|---------|--------|-----------------------------------|
| LOW      | ❌     | ❌       | Green  | Minor updates, info messages      |
| NORMAL   | ❌     | ✅       | Blue   | Regular notifications             |
| HIGH     | ✅     | ✅       | Orange | Important events, low stock       |
| URGENT   | ✅     | ✅       | Red    | Critical alerts, system errors    |

---

## Notification Types

### Sales Module
- `NEW_SALES_ORDER` - New order created
- `ORDER_STATUS_CHANGED` - Order status updated
- `ORDER_CANCELLED` - Order cancelled

### Storage Module
- `LOW_STOCK_ALERT` - Item quantity below threshold
- `OUT_OF_STOCK` - Item completely out of stock
- `ITEM_ADDED` - New item added
- `ITEM_UPDATED` - Item details updated

### Projects Module
- `NEW_PROJECT` - New project created
- `PROJECT_ASSIGNED` - User assigned to project
- `TASK_ASSIGNED` - Task assigned to user
- `TASK_COMPLETED` - Task marked complete
- `PROJECT_DEADLINE` - Project deadline approaching
- `PROJECT_STATUS_CHANGED` - Project status updated

### System Notifications
- `SYSTEM_ALERT` - General system alerts
- `USER_MENTION` - User mentioned in comment
- `REMINDER` - Scheduled reminder
- `INFO` - General information
- `WARNING` - Warning message
- `ERROR` - Error notification
- `SUCCESS` - Success confirmation

---

## Integration Examples

### Example 1: Sales Order Created

```java
@Service
public class SalesService {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    public SalesOrder createOrder(SalesOrder order) {
        // Save order
        order = salesOrderRepository.save(order);

        // Notify all MASTER users
        List<User> masterUsers = userRepository.findByRoleAndActiveTrue(UserRole.MASTER);
        for (User user : masterUsers) {
            notificationService.createNotification(
                user.getId(),
                "New Sales Order #" + order.getId(),
                String.format("Order for %s - Total: $%.2f",
                    order.getCustomer().getName(),
                    order.getTotalAmount()),
                NotificationType.NEW_SALES_ORDER,
                NotificationPriority.NORMAL,
                "SALES",
                order.getId(),
                "SalesOrder",
                "/sales/orders/" + order.getId()
            );
        }

        return order;
    }
}
```

### Example 2: Low Stock Alert

```java
@Service
public class StorageService {

    @Autowired
    private NotificationService notificationService;

    public void checkLowStock() {
        List<StorageItem> lowStockItems = repository.findByQuantityLessThan(10);

        for (StorageItem item : lowStockItems) {
            // Notify storage and master users
            notificationService.notifyUsersByRole(
                "STORAGE",
                "Low Stock Alert",
                String.format("%s is running low (Qty: %d)",
                    item.getProductName(),
                    item.getQuantity()),
                NotificationType.LOW_STOCK_ALERT,
                NotificationPriority.HIGH
            );
        }
    }
}
```

### Example 3: Project Task Assigned

```java
@Service
public class ProjectService {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public void assignTask(ProjectTask task, Long userId) {
        task.setAssignedUserId(userId);
        projectTaskRepository.save(task);

        // Publish event (async)
        NotificationEvent event = new NotificationEvent.Builder(this)
            .userId(userId)
            .title("New Task Assigned")
            .message("You have been assigned: " + task.getTaskTitle())
            .type(NotificationType.TASK_ASSIGNED)
            .priority(NotificationPriority.NORMAL)
            .moduleSource("PROJECTS")
            .referenceId(task.getId())
            .referenceType("ProjectTask")
            .actionUrl("/projects/" + task.getProjectId() + "/tasks/" + task.getId())
            .build();

        eventPublisher.publishEvent(event);
    }
}
```

---

## REST API Endpoints

### Get User Notifications
```bash
GET /api/notifications/user/{userId}
GET /api/notifications/user/{userId}/unread
GET /api/notifications/user/{userId}/count
GET /api/notifications/user/{userId}/recent?days=7
```

### Manage Notifications
```bash
POST /api/notifications
PUT /api/notifications/{id}/read
PUT /api/notifications/user/{userId}/read-all
DELETE /api/notifications/{id}
DELETE /api/notifications/user/{userId}
```

### Example: Create Notification via API
```bash
curl -X POST http://localhost:8085/api/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "title": "Test Notification",
    "message": "This is a test",
    "type": "INFO",
    "priority": "NORMAL"
  }'
```

---

## UI Integration

### Add Notification Center to Your View

```java
public class MyController extends BaseModuleController {

    @Autowired
    private NotificationCenterPane notificationCenter;

    @Override
    protected void setupUI() {
        // ... your UI setup

        // Add notification center to top toolbar
        HBox toolbar = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        notificationCenter.initialize(currentUser);
        toolbar.getChildren().addAll(titleLabel, spacer, notificationCenter);
    }

    @Override
    public void cleanup() {
        notificationCenter.stopPolling();
        super.cleanup();
    }
}
```

---

## Database Schema

```sql
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    module_source VARCHAR(50),
    reference_id BIGINT,
    reference_type VARCHAR(50),
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP,
    is_sent_email BOOLEAN DEFAULT FALSE,
    is_sent_desktop BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    active BOOLEAN DEFAULT TRUE,
    action_url VARCHAR(500)
);

CREATE INDEX idx_user_read ON notifications(user_id, is_read);
CREATE INDEX idx_created_at ON notifications(created_at);
```

---

## Best Practices

1. **Use appropriate priority levels**
   - Don't spam users with URGENT notifications
   - Reserve HIGH/URGENT for truly important events

2. **Include context in messages**
   - Bad: "Order created"
   - Good: "Order #12345 created for ABC Corp - $5,000"

3. **Set reference IDs and types**
   - Enables future "click to view" functionality
   - Helps with notification filtering

4. **Use module source consistently**
   - Makes it easy to filter notifications by module
   - Helps with debugging

5. **Clean up old notifications**
   - Automatic cleanup runs daily (30-day retention)
   - Users can manually delete notifications

6. **Test email configuration**
   - Use test email API before going to production
   - Verify Gmail app password is correct

---

## Troubleshooting

### Notifications not appearing?
1. Check if user ID is correct
2. Verify database connection
3. Check application logs for errors

### Email not sending?
1. Verify Gmail credentials in `application.properties`
2. Generate Gmail App Password (not regular password)
3. Check `app.notifications.email.enabled=true`
4. Test with: `gmailService.sendTestEmail("test@example.com")`

### Desktop notifications not showing?
1. Check OS notification settings
2. Verify `app.notifications.desktop.enabled=true`
3. Some OS may block Java desktop notifications

### Badge count not updating?
1. Check polling interval (default 5 seconds)
2. Verify `notificationCenter.initialize(user)` was called
3. Ensure cleanup was not called prematurely

---

## Future Enhancements

- [ ] WebSocket for real-time push (instead of polling)
- [ ] Notification preferences per user
- [ ] Notification categories and filtering
- [ ] Mobile push notifications (Firebase)
- [ ] Slack/Teams integration
- [ ] Notification templates
- [ ] Rich notifications with images
- [ ] Notification history and analytics

---

**Created**: 2025-11-20
**Version**: 1.0
**Author**: MagicTech Development Team
