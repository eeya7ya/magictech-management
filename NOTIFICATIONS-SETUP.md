# Notification System Setup Guide

## Overview

The MagicTech Management System uses **Redis Pub/Sub** for real-time notifications between modules. This guide explains how to set up and troubleshoot the notification system.

## Architecture

```
┌─────────────┐                  ┌─────────────┐
│   Module A  │ ──── Publish ──→ │             │
│  (Publisher)│                  │    Redis    │
└─────────────┘                  │   Pub/Sub   │
                                 │             │
┌─────────────┐                  │   Server    │
│   Module B  │ ←── Subscribe ── │             │
│ (Subscriber)│                  └─────────────┘
└─────────────┘
```

### How It Works

1. **Publisher** (e.g., Sales module) creates a project
2. **NotificationService** saves notification to PostgreSQL
3. **NotificationService** publishes message to Redis channels
4. **NotificationListenerService** receives message from Redis
5. **NotificationManager** displays notification in UI

---

## ⚠️ CRITICAL REQUIREMENT: Redis Must Be Running

The notification system **WILL NOT WORK** if Redis is not running!

### Symptoms of Redis Not Running

- ✅ Application starts successfully
- ✅ UI works normally
- ✅ You see "Published notification..." in logs
- ❌ **But notifications never appear** in other modules
- ❌ No "Dispatched notification to X listeners" in logs

---

## Installation & Setup

### Linux / macOS

#### 1. Install Redis

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install redis-server
```

**macOS (Homebrew):**
```bash
brew install redis
```

#### 2. Start Redis

**Option A: Using the startup script (Recommended)**
```bash
./start-with-redis.sh
```

**Option B: Manual start**
```bash
redis-server --daemonize yes --port 6379
```

**Option C: System service**
```bash
sudo systemctl start redis-server
sudo systemctl enable redis-server  # Auto-start on boot
```

#### 3. Verify Redis is Running

```bash
redis-cli ping
# Should return: PONG
```

### Windows

#### 1. Install Redis

**Option A: Native Windows (Recommended for development)**
Download from: https://github.com/microsoftarchive/redis/releases
- Download `Redis-x64-*.zip`
- Extract to `C:\Redis`
- Add `C:\Redis` to PATH

**Option B: Windows Subsystem for Linux (WSL)**
```bash
wsl --install
# Then follow Linux instructions
```

**Option C: Docker**
```bash
docker run -d -p 6379:6379 --name redis redis:latest
```

#### 2. Start Redis

**Option A: Using the startup script (Recommended)**
```cmd
start-with-redis.bat
```

**Option B: Manual start**
```cmd
redis-server --port 6379
```

#### 3. Verify Redis is Running

```cmd
redis-cli ping
REM Should return: PONG
```

---

## Running the Application

### Option 1: Use the Startup Scripts (Recommended)

These scripts automatically check and start Redis before launching the application:

**Linux/macOS:**
```bash
./start-with-redis.sh
```

**Windows:**
```cmd
start-with-redis.bat
```

### Option 2: Manual Start

**Step 1: Start Redis**
```bash
redis-server --daemonize yes
```

**Step 2: Start Application**
```bash
mvn spring-boot:run
```

---

## Troubleshooting

### Problem: Notifications Not Working

**Diagnosis Steps:**

1. **Check if Redis is running**
   ```bash
   redis-cli ping
   ```
   - ✅ Returns `PONG` → Redis is running
   - ❌ Returns error → Redis is NOT running (see fix below)

2. **Check Redis connection in application**
   Look for this in application logs:
   ```
   INFO ... : Subscribed to channel: all_notifications
   INFO ... : Subscribed to channel: projects_notifications
   ```
   - ✅ If you see these → Connection OK
   - ❌ If you see errors → Check Redis configuration

3. **Check if messages are being published**
   Look for this in application logs:
   ```
   DEBUG ... : Published to channel all_notifications: ...
   ```
   - ✅ If you see this → Publishing works
   - ❌ If missing → Check NotificationService

4. **Check if messages are being received**
   Look for this in application logs:
   ```
   DEBUG ... : Received message on channel ...
   INFO ... : Dispatched notification to X listeners
   ```
   - ✅ If you see this → System working!
   - ❌ If missing → Redis not running or connection issue

### Fix: Start Redis

```bash
# Linux/macOS
redis-server --daemonize yes --port 6379

# Windows
redis-server --port 6379

# Docker
docker run -d -p 6379:6379 --name redis redis:latest
```

### Problem: "Connection refused" Error

**Cause:** Redis is not running or is running on different port

**Solution:**
1. Check if Redis is running: `redis-cli ping`
2. Check configured port in `application.properties`:
   ```properties
   spring.data.redis.host=localhost
   spring.data.redis.port=6379
   ```
3. Start Redis on correct port: `redis-server --port 6379`

### Problem: Redis Starts But Crashes

**Possible Causes:**
- Port 6379 already in use
- Insufficient permissions
- Corrupted Redis data files

**Solutions:**

1. **Check if port is in use:**
   ```bash
   # Linux/macOS
   netstat -an | grep 6379
   lsof -i :6379

   # Windows
   netstat -an | findstr 6379
   ```

2. **Use different port:**
   ```bash
   redis-server --port 6380
   ```
   Then update `application.properties`:
   ```properties
   spring.data.redis.port=6380
   ```

3. **Clear Redis data (development only):**
   ```bash
   redis-cli FLUSHALL
   ```

### Problem: Slow or Delayed Notifications

**Causes:**
- Network latency
- Redis overloaded
- Too many subscriptions

**Solutions:**
1. Check Redis performance: `redis-cli INFO stats`
2. Monitor Redis memory: `redis-cli INFO memory`
3. Increase Redis timeout in `application.properties`:
   ```properties
   spring.data.redis.timeout=5000
   ```

---

## Testing Notifications

### Manual Test Script

```bash
# Terminal 1: Subscribe to test channel
redis-cli SUBSCRIBE test_notifications

# Terminal 2: Publish test message
redis-cli PUBLISH test_notifications "Hello World"
```

If you see the message in Terminal 1, Redis pub/sub is working!

### Test in Application

1. Start application with Redis running
2. Open **Sales** module
3. Create a new project
4. Check logs for:
   ```
   Published notification for new project: <name>
   Published to channel all_notifications: New Project Created
   Published to channel projects:created:project: New Project Created
   ```
5. Open **Projects** module
6. You should see the new project

---

## Configuration

### application.properties

```properties
# Redis Configuration
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.password=              # Empty for no password
spring.data.redis.timeout=2000           # 2 seconds
spring.data.redis.database=0

# Connection pool
spring.data.redis.lettuce.pool.max-active=8
spring.data.redis.lettuce.pool.max-idle=8
spring.data.redis.lettuce.pool.min-idle=0

# Device heartbeat (for online/offline detection)
magictech.device.heartbeat.timeout=300   # 5 minutes
```

### Redis Server Configuration

Create `/etc/redis/redis.conf` (Linux) or `C:\Redis\redis.windows.conf` (Windows):

```conf
# Basic settings
port 6379
bind 127.0.0.1
protected-mode yes

# Persistence (optional for notifications)
save 900 1
save 300 10
save 60 10000

# Memory management
maxmemory 256mb
maxmemory-policy allkeys-lru

# Logging
loglevel notice
logfile /var/log/redis/redis-server.log
```

---

## Notification Channels

The system uses these Redis channels:

| Channel | Purpose | Subscribers |
|---------|---------|-------------|
| `all_notifications` | Broadcast to all modules | Storage (MASTER) |
| `sales_notifications` | Sales module messages | Sales module |
| `projects_notifications` | Projects module messages | Projects module |
| `storage_notifications` | Storage module messages | Storage module |
| `maintenance_notifications` | Maintenance messages | Maintenance module |
| `pricing_notifications` | Pricing messages | Pricing module |
| `projects:created:project` | Project created event | Projects module |
| `projects:updated:project` | Project updated event | Projects module |

---

## Best Practices

### Development

1. **Always start Redis before application**
   - Use the provided startup scripts
   - Or add Redis start to your IDE run configuration

2. **Monitor Redis in development**
   ```bash
   redis-cli MONITOR
   ```
   This shows all commands in real-time

3. **Clear test data periodically**
   ```bash
   redis-cli FLUSHDB
   ```

### Production

1. **Use Redis as a system service**
   ```bash
   sudo systemctl enable redis-server
   sudo systemctl start redis-server
   ```

2. **Set up Redis authentication**
   ```properties
   spring.data.redis.password=your-secure-password
   ```

3. **Use Redis persistence**
   - Configure RDB snapshots or AOF
   - Backup Redis data regularly

4. **Monitor Redis health**
   - Set up alerts for high memory usage
   - Monitor pub/sub channels
   - Track connection count

5. **Consider Redis clustering** for high availability

---

## Quick Reference

### Start Redis
```bash
# Linux/macOS
redis-server --daemonize yes

# Windows
redis-server

# Docker
docker run -d -p 6379:6379 redis
```

### Stop Redis
```bash
# Linux/macOS
redis-cli shutdown

# Windows
redis-cli shutdown

# Docker
docker stop redis
```

### Check Redis Status
```bash
redis-cli ping                    # Should return PONG
redis-cli INFO server            # Server information
redis-cli CLIENT LIST            # Connected clients
redis-cli PUBSUB CHANNELS        # Active channels
redis-cli PUBSUB NUMSUB <channel> # Subscribers to channel
```

### Useful Redis Commands
```bash
redis-cli MONITOR               # Watch all commands
redis-cli INFO stats            # Statistics
redis-cli INFO memory           # Memory usage
redis-cli SLOWLOG get 10        # Slow commands
redis-cli FLUSHDB               # Clear current database
redis-cli FLUSHALL              # Clear all databases
```

---

## Support

If you continue to have issues:

1. Check Redis logs:
   - Linux: `/var/log/redis/redis-server.log`
   - Windows: `C:\Redis\redis-log.txt`

2. Check application logs for:
   - Connection errors
   - Subscription errors
   - Publishing errors

3. Verify network connectivity:
   ```bash
   telnet localhost 6379
   ```

4. Test with minimal Redis client:
   ```bash
   redis-cli -h localhost -p 6379 ping
   ```

---

## Summary

✅ **Redis MUST be running for notifications to work**

✅ **Use startup scripts** to automatically start Redis

✅ **Verify with** `redis-cli ping` before running application

✅ **Monitor logs** to confirm messages are published and received

✅ **Test manually** with `redis-cli SUBSCRIBE` and `PUBLISH`

---

Last Updated: 2025-11-21
