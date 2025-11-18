#!/bin/bash

# Notification System Test Script
# Tests the notification API endpoints and verifies functionality

BASE_URL="http://localhost:8085/api/notifications"

echo "========================================"
echo "MagicTech Notification System Test"
echo "========================================"
echo ""

# Health check
echo "1. Testing notification service health..."
curl -s "${BASE_URL}/health" | jq '.'
echo ""

# Create a test notification for MASTER role
echo "2. Creating test notification for MASTER role..."
curl -s -X POST "${BASE_URL}/test/role/MASTER" | jq '.'
echo ""

# Create a test notification for SALES role
echo "3. Creating test notification for SALES role..."
curl -s -X POST "${BASE_URL}/test/role/SALES" | jq '.'
echo ""

# Create a custom notification for MASTER role
echo "4. Creating custom notification for MASTER role..."
curl -s -X POST "${BASE_URL}/role/MASTER" \
  -H "Content-Type: application/json" \
  -d '{
    "module": "STORAGE",
    "type": "INVENTORY_LOW",
    "title": "⚠️ Low Inventory Alert",
    "message": "Several items are running low on stock. Please review and reorder.",
    "priority": "HIGH"
  }' | jq '.'
echo ""

# Get unread count for admin user (userId=1, role=MASTER)
echo "5. Getting unread notification count for admin..."
curl -s "${BASE_URL}/user/1/role/MASTER/unread-count" | jq '.'
echo ""

# Get all notifications for admin user
echo "6. Getting all notifications for admin..."
curl -s "${BASE_URL}/user/1/role/MASTER" | jq '.'
echo ""

echo "========================================"
echo "Test Complete!"
echo "========================================"
echo ""
echo "If the application is running, you should see notification popups"
echo "slide in from the top-right corner of the dashboard."
echo ""
echo "To mark a notification as read:"
echo "  curl -X PUT ${BASE_URL}/{notificationId}/read"
echo ""
echo "To mark all as read:"
echo "  curl -X PUT ${BASE_URL}/user/1/role/MASTER/read-all"
echo ""
