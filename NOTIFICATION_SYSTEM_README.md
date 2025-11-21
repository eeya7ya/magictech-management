# Pub/Sub Messaging and Notification System

## Overview

This document describes the real-time pub/sub messaging system implemented for the MagicTech Management System. The system enables real-time notifications between different modules using Redis as the message broker and PostgreSQL for notification persistence.

## Architecture

The notification system follows a **publisher-subscriber (pub/sub)** pattern:

```
┌──────────────┐         ┌──────────┐         ┌──────────────┐
│ Sales Module │ ──────> │  Redis   │ ──────> │Projects Module│
│  (Publisher) │         │ Pub/Sub  │         │ (Subscriber)  │
└──────────────┘         └──────────┘         └──────────────┘
                              │
                              ├──────────────> Storage Module
                              ├──────────────> Pricing Module
                              └──────────────> Maintenance Module

                         ┌──────────────┐
                         │  PostgreSQL  │
                         │ (Persistence)│
                         └──────────────┘
```

### Key Components

1. **Redis Pub/Sub**: Real-time message delivery between modules
2. **PostgreSQL**: Notification history and missed notifications storage
3. **Device Registration**: Track online/offline status of connected devices
4. **JavaFX Popup**: Visual notification display in the UI
5. **Heartbeat System**: Monitor device connectivity

## Prerequisites

### 1. Redis Installation

#### On Ubuntu/Debian:
```bash
sudo apt update
sudo apt install redis-server
sudo systemctl start redis-server
sudo systemctl enable redis-server
```

#### On macOS:
```bash
brew install redis
brew services start redis
```

#### On Windows:
Download from: https://github.com/microsoftarchive/redis/releases
Or use WSL/Docker

#### Verify Redis is Running:
```bash
redis-cli ping
# Should respond with: PONG
```

### 2. PostgreSQL Configuration

The notification tables will be created automatically on first run via Hibernate DDL auto-update.

## Configuration

### Redis Configuration (`application.properties`)

```properties
# Redis Configuration
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.password=
spring.data.redis.timeout=2000
spring.data.redis.database=0

# Connection pool settings
spring.data.redis.lettuce.pool.max-active=8
spring.data.redis.lettuce.pool.max-idle=8
spring.data.redis.lettuce.pool.min-idle=0
spring.data.redis.lettuce.pool.max-wait=-1ms

# Device heartbeat configuration (in seconds)
magictech.device.heartbeat.timeout=300
```

### Module Channels

Each module has its own dedicated Redis channel:

- **Sales Module**: `sales_notifications`
- **Projects Module**: `projects_notifications`
- **Storage Module**: `storage_notifications` (subscribes to ALL channels)
- **Maintenance Module**: `maintenance_notifications`
- **Pricing Module**: `pricing_notifications`
- **Broadcast Channel**: `all_notifications` (all modules receive)

## Notification Workflow

### Scenario 1: Project Creation from Sales

```
Sales Module                    Projects Module
     │                               │
     ├─ Create Project              │
     │                               │
     ├─ Push to Project Table       │
     │                               │
     ├─ Publish Notification ──────>├─ Receive Notification
     │   (Redis: projects_notifications)
     │                               │
     │                               ├─ Show Popup
     │                               │  "New Project Created"
     │                               │
     │                               ├─ Save to DB
```

### Scenario 2: Confirmation Request from Projects

```
Projects Module                 Sales Module
     │                               │
     ├─ Need Additional Elements    │
     │                               │
     ├─ Request Confirmation ───────>├─ Receive Notification
     │   (Redis: sales_notifications)
     │                               │
     │                               ├─ Show Popup
     │                               │  "Confirmation Requested"
     │                               │
     │                               ├─ Save to DB
```

### Scenario 3: Project Completion

```
Projects Module                 Storage Module    Pricing Module
     │                               │                 │
     ├─ Complete Project            │                 │
     │                               │                 │
     ├─ Mark as COMPLETED           │                 │
     │                               │                 │
     ├─ Publish Notification ───────┼────────────────>├─ Receive
     │   (Redis: storage_notifications & pricing_notifications)
     │                               │                 │
     │                               ├─ Show Popup    ├─ Show Popup
     │                               │  "Project       │  "Project
     │                               │   Completed"    │   Completed"
     │                               │                 │
     │                               ├─ Save to DB    ├─ Save to DB
```

## Usage

### 1. Sending Notifications (from Services)

#### Example: Notify when project is created (Sales Service)

```java
@Service
public class SalesOrderService {

    @Autowired
    private NotificationService notificationService;

    public SalesOrder pushToProjectTable(Long orderId, String pushedBy) {
        // ... business logic ...

        // Send notification
        notificationService.notifyProjectCreated(
            order.getProjectId(),
            "Project from Sales Order #" + orderId,
            pushedBy
        );

        return order;
    }
}
```

#### Example: Request confirmation (Project Service)

```java
@Service
public class ProjectService {

    @Autowired
    private NotificationService notificationService;

    public Project requestConfirmation(Long projectId, String requestedBy, String reason) {
        Project project = repository.findById(projectId).orElseThrow(...);

        // Send notification to Sales
        notificationService.notifyConfirmationRequested(
            projectId,
            project.getProjectName(),
            requestedBy
        );

        return project;
    }
}
```

#### Example: Complete project (Project Service)

```java
public Project completeProject(Long projectId, String completedBy) {
    Project project = repository.findById(projectId).orElseThrow(...);

    project.setStatus("COMPLETED");
    project = repository.save(project);

    // Notify Storage and Pricing modules
    notificationService.notifyProjectCompleted(
        projectId,
        project.getProjectName(),
        completedBy
    );

    return project;
}
```

### 2. Custom Notifications

For custom notification scenarios:

```java
@Autowired
private NotificationService notificationService;

public void sendCustomNotification() {
    NotificationMessage message = new NotificationMessage.Builder()
        .type(NotificationConstants.TYPE_INFO)
        .module(NotificationConstants.MODULE_SALES)
        .action(NotificationConstants.ACTION_CREATED)
        .entityType(NotificationConstants.ENTITY_SALES_ORDER)
        .entityId(123L)
        .title("Custom Notification")
        .message("This is a custom notification message")
        .targetModule(NotificationConstants.MODULE_PROJECTS)
        .priority(NotificationConstants.PRIORITY_HIGH)
        .createdBy("username")
        .build();

    notificationService.publishNotification(message);
}
```

## Database Schema

### Notifications Table

```sql
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    notification_type VARCHAR(50) NOT NULL,
    module VARCHAR(50) NOT NULL,
    action VARCHAR(50),
    entity_type VARCHAR(50),
    entity_id BIGINT,
    title VARCHAR(200) NOT NULL,
    message TEXT,
    target_device_id VARCHAR(100),
    target_module VARCHAR(50),
    read_status BOOLEAN NOT NULL DEFAULT false,
    timestamp TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    priority VARCHAR(20),
    metadata TEXT,
    active BOOLEAN NOT NULL DEFAULT true
);
```

### Device Registrations Table

```sql
CREATE TABLE device_registrations (
    id BIGSERIAL PRIMARY KEY,
    device_id VARCHAR(100) NOT NULL UNIQUE,
    device_name VARCHAR(200),
    module_type VARCHAR(50) NOT NULL,
    user_id BIGINT,
    username VARCHAR(100),
    status VARCHAR(20) NOT NULL,
    last_heartbeat TIMESTAMP,
    registered_at TIMESTAMP NOT NULL,
    last_seen TIMESTAMP,
    ip_address VARCHAR(50),
    hostname VARCHAR(200),
    application_version VARCHAR(50),
    subscribed_channels TEXT,
    active BOOLEAN NOT NULL DEFAULT true
);
```

## Monitoring and Debugging

### Check Redis Connection

```bash
redis-cli ping
```

### Monitor Redis Channels

```bash
# Subscribe to all notification channels
redis-cli psubscribe "*_notifications"
```

### View Published Messages

```bash
redis-cli monitor
```

### Check Database Notifications

```sql
-- View recent notifications
SELECT * FROM notifications ORDER BY timestamp DESC LIMIT 10;

-- View unread notifications for a module
SELECT * FROM notifications
WHERE target_module = 'projects'
  AND read_status = false
  AND active = true;

-- View device registrations
SELECT * FROM device_registrations WHERE active = true;

-- View online devices
SELECT * FROM device_registrations WHERE status = 'ONLINE' AND active = true;
```

### Application Logs

Look for these log entries:
- `✓ Notification system initialized`
- `Published notification: [title] to module: [module]`
- `Received notification: [title]`
- `Device registered successfully`
- `Heartbeat sent successfully`

## Troubleshooting

### Issue: Notifications not received

**Check:**
1. Redis is running: `redis-cli ping`
2. Application logs for subscription errors
3. Channel names match in publisher and subscriber
4. NotificationListenerService is initialized

### Issue: Connection refused to Redis

**Solutions:**
1. Start Redis: `sudo systemctl start redis-server`
2. Check Redis port: Default is 6379
3. Update `application.properties` if Redis is on different host/port

### Issue: Notifications not persisting to database

**Check:**
1. Database connection is working
2. Tables are created (check with `\dt` in psql)
3. Check application logs for database errors

### Issue: Device shows as offline

**Check:**
1. Heartbeat scheduler is running (check logs)
2. Heartbeat timeout setting (default 300 seconds)
3. Device registration was successful

## Performance Considerations

### Redis Connection Pooling

The system uses Lettuce with connection pooling:
- Max active connections: 8
- Max idle connections: 8
- Min idle connections: 0

Adjust in `application.properties` based on load.

### Heartbeat Frequency

- Heartbeat sent every: **60 seconds**
- Device timeout: **300 seconds** (5 minutes)

Devices are marked offline if no heartbeat received within timeout period.

### Notification Popup Behavior

- Auto-dismiss after: **5 seconds**
- Position: **Bottom-right corner**
- Multiple popups: **Queued and displayed sequentially**

## Security Considerations

### Redis Security (Production)

1. **Set Redis password:**
```bash
# In redis.conf
requirepass your_strong_password
```

2. **Update application.properties:**
```properties
spring.data.redis.password=your_strong_password
```

3. **Bind to localhost only:**
```bash
# In redis.conf
bind 127.0.0.1
```

4. **Enable Redis SSL/TLS** for production deployments

### Database Security

- Notifications are persisted in PostgreSQL
- Use proper database authentication
- Implement row-level security if needed
- Regular backups of notification history

## Testing

### Manual Testing

1. **Start Redis:**
```bash
redis-cli ping
```

2. **Run Application:**
```bash
mvn spring-boot:run
```

3. **Create Test Notification:**
- Login as Sales user
- Create a project
- Push to project table
- Check Projects module for notification popup

### Monitor Redis Activity

```bash
# Terminal 1: Monitor all Redis activity
redis-cli monitor

# Terminal 2: Subscribe to specific channel
redis-cli
> SUBSCRIBE projects_notifications
```

## Future Enhancements

Possible improvements:

1. **PostgreSQL LISTEN/NOTIFY**: Add database-level triggers
2. **Notification History UI**: View past notifications in the app
3. **Notification Preferences**: Allow users to customize notification types
4. **Sound Alerts**: Add audio notifications
5. **Email Integration**: Send email for critical notifications
6. **Mobile Push**: Integrate with mobile notification services
7. **Redis Cluster**: Scale to multiple Redis instances
8. **Message Persistence**: Use Redis Streams for guaranteed delivery

## Support

For issues or questions:
1. Check application logs
2. Verify Redis and PostgreSQL are running
3. Review configuration in `application.properties`
4. Check this README for troubleshooting steps

---

**Last Updated:** 2025-11-21
**Version:** 1.0
**Author:** MagicTech Development Team
