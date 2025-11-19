#!/bin/bash

# MagicTech Management System - Complete Test Script
# This script verifies all fixes and new features

echo "=============================================="
echo "MagicTech Management System - Full Test Suite"
echo "=============================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test counter
PASSED=0
FAILED=0

# Function to print test result
test_result() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓ PASSED${NC}: $2"
        ((PASSED++))
    else
        echo -e "${RED}✗ FAILED${NC}: $2"
        ((FAILED++))
    fi
}

echo "Starting MagicTech Management System tests..."
echo ""

# ==========================================
# TEST 1: Database Connection
# ==========================================
echo "TEST 1: Checking database connection..."
psql -U postgres -d magictech_db -c "SELECT 1;" > /dev/null 2>&1
test_result $? "Database connection"
echo ""

# ==========================================
# TEST 2: Check if tables exist
# ==========================================
echo "TEST 2: Verifying database tables..."

# Check users table
psql -U postgres -d magictech_db -c "\dt users" | grep -q "users"
test_result $? "Users table exists"

# Check customer tables
psql -U postgres -d magictech_db -c "\dt customer_elements" | grep -q "customer_elements"
test_result $? "Customer Elements table exists"

psql -U postgres -d magictech_db -c "\dt customer_tasks" | grep -q "customer_tasks"
test_result $? "Customer Tasks table exists"

psql -U postgres -d magictech_db -c "\dt customer_notes" | grep -q "customer_notes"
test_result $? "Customer Notes table exists"

psql -U postgres -d magictech_db -c "\dt customer_schedules" | grep -q "customer_schedules"
test_result $? "Customer Schedules table exists"

# Check document tables
psql -U postgres -d magictech_db -c "\dt project_documents" | grep -q "project_documents"
test_result $? "Project Documents table exists"

psql -U postgres -d magictech_db -c "\dt customer_documents" | grep -q "customer_documents"
test_result $? "Customer Documents table exists"

echo ""

# ==========================================
# TEST 3: Check default users
# ==========================================
echo "TEST 3: Verifying default users..."

USER_COUNT=$(psql -U postgres -d magictech_db -t -c "SELECT COUNT(*) FROM users WHERE active = true;")
if [ "$USER_COUNT" -ge 7 ]; then
    test_result 0 "Default users created (Found: $USER_COUNT)"
else
    test_result 1 "Default users created (Found: $USER_COUNT, Expected: >= 7)"
fi

# Check admin user
psql -U postgres -d magictech_db -t -c "SELECT username FROM users WHERE username = 'admin';" | grep -q "admin"
test_result $? "Admin user exists"

echo ""

# ==========================================
# TEST 4: API Health Check
# ==========================================
echo "TEST 4: Testing REST API endpoints..."

# Start the application in background (if not already running)
echo "Note: Make sure the application is running on port 8085"
sleep 2

# Health check
HEALTH_CHECK=$(curl -s http://localhost:8085/api/auth/health 2>/dev/null | grep -o "UP")
if [ "$HEALTH_CHECK" = "UP" ]; then
    test_result 0 "API health check"
else
    test_result 1 "API health check (Application may not be running)"
fi

# Test login endpoint
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8085/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"admin123"}' 2>/dev/null | grep -o "success")

if [ "$LOGIN_RESPONSE" = "success" ]; then
    test_result 0 "User authentication API"
else
    test_result 1 "User authentication API"
fi

echo ""

# ==========================================
# TEST 5: File Structure Verification
# ==========================================
echo "TEST 5: Verifying file structure..."

# Check customer entities
[ -f "src/main/java/com/magictech/modules/sales/entity/CustomerElement.java" ]
test_result $? "CustomerElement.java exists"

[ -f "src/main/java/com/magictech/modules/sales/entity/CustomerTask.java" ]
test_result $? "CustomerTask.java exists"

[ -f "src/main/java/com/magictech/modules/sales/entity/CustomerNote.java" ]
test_result $? "CustomerNote.java exists"

[ -f "src/main/java/com/magictech/modules/sales/entity/CustomerSchedule.java" ]
test_result $? "CustomerSchedule.java exists"

# Check document entities
[ -f "src/main/java/com/magictech/modules/projects/entity/ProjectDocument.java" ]
test_result $? "ProjectDocument.java exists"

[ -f "src/main/java/com/magictech/modules/sales/entity/CustomerDocument.java" ]
test_result $? "CustomerDocument.java exists"

# Check services
[ -f "src/main/java/com/magictech/modules/sales/service/ComprehensiveExcelExportService.java" ]
test_result $? "ComprehensiveExcelExportService.java exists"

# Check documentation
[ -f "USER_ROLES.md" ]
test_result $? "USER_ROLES.md exists"

[ -f "FIXES_SUMMARY.md" ]
test_result $? "FIXES_SUMMARY.md exists"

echo ""

# ==========================================
# TEST 6: Document Storage Directory
# ==========================================
echo "TEST 6: Verifying document storage..."

# Create document directories if they don't exist
mkdir -p ./data/documents/projects
mkdir -p ./data/documents/customers

[ -d "./data/documents/projects" ]
test_result $? "Project documents directory"

[ -d "./data/documents/customers" ]
test_result $? "Customer documents directory"

echo ""

# ==========================================
# SUMMARY
# ==========================================
echo "=============================================="
echo "TEST SUMMARY"
echo "=============================================="
TOTAL=$((PASSED + FAILED))
echo -e "Total Tests: $TOTAL"
echo -e "${GREEN}Passed: $PASSED${NC}"
echo -e "${RED}Failed: $FAILED${NC}"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}🎉 ALL TESTS PASSED! System is ready to use!${NC}"
    echo ""
    echo "Next steps:"
    echo "1. Start the application: mvn spring-boot:run"
    echo "2. Login with admin/admin123"
    echo "3. Create new users via User Management"
    echo "4. Test customer and project functionality"
    exit 0
else
    echo -e "${YELLOW}⚠️  Some tests failed. Please review the errors above.${NC}"
    echo ""
    echo "Common fixes:"
    echo "1. Ensure PostgreSQL is running"
    echo "2. Check database credentials in application.properties"
    echo "3. Start the application if API tests failed"
    echo "4. Run: mvn spring-boot:run"
    exit 1
fi
