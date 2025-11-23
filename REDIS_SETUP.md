# Redis Setup for MagicTech Management System

## Overview

The MagicTech Management System uses **Redis** for real-time pub/sub notifications between modules. Redis enables:

- ✅ Real-time notifications when sales orders are pushed to projects
- ✅ Site survey request notifications between Sales and Projects teams
- ✅ Workflow step completion alerts
- ✅ Approval requests from Projects to Sales
- ✅ Item allocation notifications
- ✅ Project completion broadcasts

**Note**: The application will work WITHOUT Redis, but real-time notifications will be disabled. All other features (storage, sales, projects, etc.) will function normally.

---

## Installation Instructions

### Ubuntu / Debian Linux

```bash
# Install Redis
sudo apt-get update
sudo apt-get install redis-server

# Start Redis service
sudo systemctl start redis

# Enable Redis to start on boot
sudo systemctl enable redis

# Verify Redis is running
redis-cli ping
# Should return: PONG
```

### macOS

```bash
# Install Redis using Homebrew
brew install redis

# Start Redis service
brew services start redis

# Verify Redis is running
redis-cli ping
# Should return: PONG
```

### Windows

1. **Download Redis for Windows**:
   - Visit: https://github.com/microsoftarchive/redis/releases
   - Download the latest `.msi` installer (e.g., `Redis-x64-3.0.504.msi`)

2. **Install Redis**:
   - Run the installer
   - Keep default settings (port 6379)
   - Install as a Windows service (recommended)

3. **Start Redis**:
   - Open Services (Win+R, type `services.msc`)
   - Find "Redis" service
   - Right-click → Start

4. **Verify Redis is running**:
   ```bash
   # Open Command Prompt
   redis-cli ping
   # Should return: PONG
   ```

### Docker (Cross-Platform)

```bash
# Pull Redis image
docker pull redis:latest

# Run Redis container
docker run --name magictech-redis -d -p 6379:6379 redis:latest

# Verify Redis is running
docker exec -it magictech-redis redis-cli ping
# Should return: PONG
```

---

## Configuration

Redis configuration is in `/src/main/resources/application.properties`:

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
```

**Change only if**:
- Redis is on a different host (change `spring.data.redis.host`)
- Redis is on a different port (change `spring.data.redis.port`)
- Redis has password authentication (set `spring.data.redis.password`)

---

## Notification System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                   MagicTech Application                      │
│                                                              │
│  ┌──────────┐      ┌──────────┐       ┌──────────┐         │
│  │  Sales   │      │ Projects │       │ Storage  │         │
│  │  Module  │      │  Module  │       │  Module  │         │
│  └────┬─────┘      └─────┬────┘       └─────┬────┘         │
│       │                   │                   │              │
│       │  Publish          │  Subscribe        │              │
│       │  Notification     │  to Channels      │              │
│       └───────────────────┼───────────────────┘              │
│                           │                                  │
│                  ┌────────▼────────┐                        │
│                  │ NotificationMgr │                        │
│                  └────────┬────────┘                        │
└───────────────────────────┼─────────────────────────────────┘
                            │
                  ┌─────────▼──────────┐
                  │   Redis Pub/Sub    │
                  │  (Port 6379)       │
                  └────────────────────┘

Channels:
  - sales_notifications
  - projects_notifications
  - storage_notifications
  - all_notifications (broadcast)
```

### How It Works

1. **Publishing Notifications**:
   ```java
   // In SalesOrderService.java
   notificationService.publishNotification(
       NotificationMessage.builder()
           .module("sales")
           .action("created")
           .entityType("project")
           .title("New Project Created")
           .message("Sales order pushed to project")
           .build()
   );
   ```

2. **Subscribing to Channels**:
   - **MASTER** users: Subscribe to ALL channels (see everything)
   - **STORAGE** module: Subscribes to ALL channels
   - Other modules: Subscribe only to their specific channel

3. **Displaying Notifications**:
   - `NotificationManager` receives messages from Redis
   - `NotificationPopup` displays visual notification in JavaFX UI
   - Notifications are stored in PostgreSQL for persistence
   - Users can see missed notifications on next login

---

## Verifying Notification System

### 1. Check Redis Connection

```bash
# Connect to Redis CLI
redis-cli

# Monitor all commands
MONITOR

# In another terminal, run the application
# You should see Redis commands being executed
```

### 2. Check Application Logs

When the application starts, you should see:

```
✓ Notification system initialized
Subscribed to all notification channels (MASTER user sees everything)
Device registered successfully
```

If Redis is **NOT** running, you'll see:

```
Warning: Failed to initialize notification system: Connection refused
Note: Notifications require Redis to be running.
      The application will continue to work, but real-time notifications will be disabled.
```

### 3. Test Notification Flow

**Step 1**: Log in as a MASTER user

**Step 2**: Open Sales module

**Step 3**: Create a project order and push to project

**Step 4**: You should see a notification popup:
- 🔔 **New Project Created**
- "Sales order pushed to project"

---

## Troubleshooting

### Issue: "Connection refused" Error

**Cause**: Redis is not running

**Solution**:
```bash
# Linux/macOS
sudo systemctl start redis

# macOS (Homebrew)
brew services start redis

# Docker
docker start magictech-redis

# Windows
# Start Redis service from Services panel
```

---

### Issue: Notifications Not Appearing

**Possible Causes**:

1. **Redis is not running** → Start Redis (see above)

2. **Wrong Redis host/port** → Check `application.properties`:
   ```properties
   spring.data.redis.host=localhost  # Should match your Redis host
   spring.data.redis.port=6379       # Should match your Redis port
   ```

3. **User role restrictions**:
   - Only **MASTER** and **STORAGE** users see notifications from ALL modules
   - Other users see only their module's notifications

4. **Check console logs** for errors:
   ```
   Failed to subscribe to channels: Connection refused
   ```

---

### Issue: Redis Port Already in Use

**Solution 1**: Stop the existing Redis instance
```bash
# Linux
sudo systemctl stop redis

# macOS
brew services stop redis
```

**Solution 2**: Use a different port
Edit `application.properties`:
```properties
spring.data.redis.port=6380  # Use different port
```

Then start Redis on that port:
```bash
redis-server --port 6380
```

---

## Performance Tuning

For production environments with high notification volume:

### 1. Increase Connection Pool

Edit `application.properties`:
```properties
spring.data.redis.lettuce.pool.max-active=20
spring.data.redis.lettuce.pool.max-idle=10
spring.data.redis.lettuce.pool.min-idle=2
```

### 2. Enable Persistence (Optional)

If you want to persist pub/sub history (not required for this app):

```bash
# Edit Redis config
sudo nano /etc/redis/redis.conf

# Enable RDB persistence
save 900 1
save 300 10
save 60 10000
```

### 3. Monitor Redis Performance

```bash
# Monitor real-time stats
redis-cli --stat

# Check memory usage
redis-cli INFO memory

# Check connected clients
redis-cli CLIENT LIST
```

---

## Notification Channels Reference

| Channel Name | Subscribers | Purpose |
|-------------|-------------|---------|
| `sales_notifications` | Sales users, MASTER, STORAGE | Sales order events |
| `projects_notifications` | Projects users, MASTER, STORAGE | Project updates, site surveys |
| `storage_notifications` | Storage users, MASTER | Inventory changes |
| `maintenance_notifications` | Maintenance users, MASTER, STORAGE | Maintenance requests |
| `pricing_notifications` | Pricing users, MASTER, STORAGE | Pricing updates |
| `all_notifications` | MASTER users only | Broadcast channel |

---

## Alternative: Running Without Redis

If you cannot install Redis, the application will still work with these limitations:

✅ **Still Works**:
- All CRUD operations (Create, Read, Update, Delete)
- Sales order management
- Project management
- Storage management
- Site survey uploads
- Excel import/export

❌ **Disabled**:
- Real-time notification popups
- Cross-module event alerts
- Workflow notifications

**Database notifications** are still stored in PostgreSQL, but you won't see real-time popups.

---

## Support

For Redis-related issues:
- Official Redis Documentation: https://redis.io/docs/
- Redis GitHub: https://github.com/redis/redis
- Community Support: https://redis.io/community/

For MagicTech application issues:
- Check application logs in console
- Verify database connection (PostgreSQL)
- Ensure all services are autowired correctly

---

**Last Updated**: 2025-11-23
**Version**: 1.0
**MagicTech Management System**
