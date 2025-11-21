#!/bin/bash

echo "==================================="
echo "Testing Redis Pub/Sub"
echo "==================================="

# Test 1: Basic ping
echo ""
echo "Test 1: Redis Ping"
redis-cli ping

# Test 2: Subscribe and publish test
echo ""
echo "Test 2: Pub/Sub Test"
echo "Publishing message to test_channel..."

# Start subscriber in background
timeout 5 redis-cli SUBSCRIBE test_channel > /tmp/redis_sub_output.txt 2>&1 &
SUBSCRIBER_PID=$!

# Wait for subscriber to be ready
sleep 1

# Publish a test message
redis-cli PUBLISH test_channel "Hello from Redis!" > /dev/null

# Wait for message to be received
sleep 1

# Check if message was received
echo "Subscriber output:"
cat /tmp/redis_sub_output.txt

# Cleanup
kill $SUBSCRIBER_PID 2>/dev/null
rm -f /tmp/redis_sub_output.txt

echo ""
echo "Test 3: Check Redis connection info"
redis-cli INFO server | grep "redis_version\|os\|tcp_port"

echo ""
echo "Test 4: Check active connections"
redis-cli CLIENT LIST | head -5

echo ""
echo "==================================="
echo "Redis is ready for use!"
echo "==================================="
