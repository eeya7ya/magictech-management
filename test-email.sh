#!/bin/bash

# ================================================
# MagicTech Email Configuration Test Script
# ================================================
# This script helps you test and diagnose email configuration

BASE_URL="http://localhost:8085/api/email"

echo "================================================"
echo "📧 MagicTech Email Configuration Tester"
echo "================================================"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

# Check if server is running
echo "1️⃣  Checking if server is running..."
if curl -s "${BASE_URL}/health" > /dev/null 2>&1; then
    print_success "Server is running"
else
    print_error "Server is NOT running!"
    echo ""
    echo "Please start the application first:"
    echo "  mvn spring-boot:run"
    echo ""
    exit 1
fi

echo ""
echo "2️⃣  Checking email configuration status..."
echo ""

# Get email status
STATUS_RESPONSE=$(curl -s "${BASE_URL}/status")
echo "$STATUS_RESPONSE" | jq '.' 2>/dev/null || echo "$STATUS_RESPONSE"

echo ""
echo "================================================"

# Check if configured
IS_CONFIGURED=$(echo "$STATUS_RESPONSE" | jq -r '.emailConfigured' 2>/dev/null)

if [ "$IS_CONFIGURED" = "false" ]; then
    print_error "Email is NOT configured!"
    echo ""
    print_info "Follow these steps to configure Gmail:"
    echo ""
    echo "  1. Go to: https://myaccount.google.com/security"
    echo "  2. Enable 2-Step Verification"
    echo "  3. Go to: https://myaccount.google.com/apppasswords"
    echo "  4. Create an App Password for 'Mail'"
    echo "  5. Update src/main/resources/application.properties:"
    echo ""
    echo "     spring.mail.username=your-email@gmail.com"
    echo "     spring.mail.password=your-16-char-app-password"
    echo "     app.email.from.address=your-email@gmail.com"
    echo ""
    echo "  6. Restart the application"
    echo ""
    exit 1
fi

print_success "Email is configured!"
echo ""

# Ask user if they want to send a test email
echo "3️⃣  Would you like to send a test email? (y/n)"
read -r SEND_TEST

if [ "$SEND_TEST" = "y" ] || [ "$SEND_TEST" = "Y" ]; then
    echo ""
    echo "Enter recipient email address:"
    read -r RECIPIENT_EMAIL

    if [ -z "$RECIPIENT_EMAIL" ]; then
        print_error "No email address provided. Exiting."
        exit 1
    fi

    echo ""
    print_info "Sending test email to: $RECIPIENT_EMAIL"
    echo ""

    TEST_RESPONSE=$(curl -s -X POST "${BASE_URL}/test" \
        -H "Content-Type: application/json" \
        -d "{\"email\":\"$RECIPIENT_EMAIL\"}")

    echo "$TEST_RESPONSE" | jq '.' 2>/dev/null || echo "$TEST_RESPONSE"

    echo ""

    SUCCESS=$(echo "$TEST_RESPONSE" | jq -r '.success' 2>/dev/null)
    if [ "$SUCCESS" = "true" ]; then
        print_success "Test email sent successfully!"
        echo ""
        print_info "Check your inbox: $RECIPIENT_EMAIL"
        echo ""
        echo "If you don't see the email:"
        echo "  • Check spam/junk folder"
        echo "  • Wait a few minutes (Gmail can be slow)"
        echo "  • Check application logs for errors"
    else
        print_error "Failed to send test email!"
        echo ""
        ERROR=$(echo "$TEST_RESPONSE" | jq -r '.error' 2>/dev/null)
        if [ -n "$ERROR" ] && [ "$ERROR" != "null" ]; then
            echo "Error: $ERROR"
        fi
        echo ""
        echo "Check application logs for more details"
    fi
else
    print_info "Skipping test email"
fi

echo ""
echo "================================================"
echo "📍 Available Endpoints:"
echo "================================================"
echo ""
echo "  GET  ${BASE_URL}/health"
echo "       → Check if email API is running"
echo ""
echo "  GET  ${BASE_URL}/status"
echo "       → Get detailed email configuration status"
echo ""
echo "  POST ${BASE_URL}/test"
echo "       → Send a test email"
echo "       Example:"
echo "         curl -X POST ${BASE_URL}/test \\"
echo "           -H 'Content-Type: application/json' \\"
echo "           -d '{\"email\":\"your-email@gmail.com\"}'"
echo ""
echo "================================================"
echo "Done!"
echo "================================================"
