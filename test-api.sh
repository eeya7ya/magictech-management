#!/bin/bash

# MagicTech API Testing Script
# Make executable: chmod +x test-api.sh
# Run: ./test-api.sh

BASE_URL="http://localhost:8080/api/auth"

echo "========================================"
echo "MagicTech API Testing Script"
echo "========================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test 1: Health Check
echo -e "${BLUE}Test 1: Health Check${NC}"
echo "GET $BASE_URL/health"
curl -s -X GET "$BASE_URL/health" | python3 -m json.tool
echo ""
echo ""

# Test 2: Login with default admin
echo -e "${BLUE}Test 2: Login (admin/admin123)${NC}"
echo "POST $BASE_URL/login"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')
echo "$LOGIN_RESPONSE" | python3 -m json.tool
echo ""
echo ""

# Test 3: Login with wrong password
echo -e "${BLUE}Test 3: Login with wrong password${NC}"
echo "POST $BASE_URL/login"
curl -s -X POST "$BASE_URL/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"wrongpass"}' | python3 -m json.tool
echo ""
echo ""

# Test 4: Get all users
echo -e "${BLUE}Test 4: Get all users${NC}"
echo "GET $BASE_URL/users"
curl -s -X GET "$BASE_URL/users" | python3 -m json.tool
echo ""
echo ""

# Test 5: Get user statistics
echo -e "${BLUE}Test 5: Get user statistics${NC}"
echo "GET $BASE_URL/stats"
curl -s -X GET "$BASE_URL/stats" | python3 -m json.tool
echo ""
echo ""

# Test 6: Create new user
echo -e "${BLUE}Test 6: Create new user (testuser)${NC}"
echo "POST $BASE_URL/users"
curl -s -X POST "$BASE_URL/users" \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"test123","role":"SALES"}' | python3 -m json.tool
echo ""
echo ""

# Test 7: Get users by role
echo -e "${BLUE}Test 7: Get MASTER role users${NC}"
echo "GET $BASE_URL/users/role/MASTER"
curl -s -X GET "$BASE_URL/users/role/MASTER" | python3 -m json.tool
echo ""
echo ""

# Test 8: Login with new user
echo -e "${BLUE}Test 8: Login with new user (testuser)${NC}"
echo "POST $BASE_URL/login"
curl -s -X POST "$BASE_URL/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"test123"}' | python3 -m json.tool
echo ""
echo ""

echo "========================================"
echo "Testing Complete!"
echo "========================================"