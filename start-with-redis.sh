#!/bin/bash

echo "=========================================="
echo "MagicTech Management System Startup"
echo "=========================================="

# Function to check if Redis is running
check_redis() {
    redis-cli ping > /dev/null 2>&1
    return $?
}

# Function to start Redis
start_redis() {
    echo "Starting Redis server..."
    redis-server --daemonize yes --port 6379
    sleep 2

    if check_redis; then
        echo "✓ Redis server started successfully"
        return 0
    else
        echo "✗ Failed to start Redis server"
        return 1
    fi
}

# Check if Redis is already running
if check_redis; then
    echo "✓ Redis is already running"
else
    echo "✗ Redis is not running"

    # Try to start Redis
    if ! start_redis; then
        echo ""
        echo "ERROR: Could not start Redis server"
        echo "Please start Redis manually: redis-server --daemonize yes"
        exit 1
    fi
fi

# Show Redis info
echo ""
echo "Redis Information:"
echo "  Version: $(redis-cli INFO server | grep 'redis_version:' | cut -d':' -f2 | tr -d '\r')"
echo "  Port: 6379"
echo "  Status: Running"

echo ""
echo "=========================================="
echo "Starting Application..."
echo "=========================================="
echo ""

# Start the application
mvn spring-boot:run
