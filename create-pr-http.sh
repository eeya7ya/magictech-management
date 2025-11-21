#!/bin/bash

# GitHub API endpoint
API_URL="https://api.github.com/repos/eeya7ya/magictech-management/pulls"

# Read PR description
PR_BODY=$(cat /home/user/magictech-management/PR_DESCRIPTION.md)

# PR title
PR_TITLE="Add Notification System with Approval Workflow"

# Create JSON payload
JSON_PAYLOAD=$(jq -n \
  --arg title "$PR_TITLE" \
  --arg body "$PR_BODY" \
  --arg head "claude/maven-build-config-01G4pWU2AnnpH8pHS7hKfqVH" \
  --arg base "main" \
  '{
    title: $title,
    body: $body,
    head: $head,
    base: $base
  }')

# Check if GITHUB_TOKEN is set
if [ -z "$GITHUB_TOKEN" ]; then
  echo "❌ GITHUB_TOKEN environment variable is not set"
  echo ""
  echo "Please create a GitHub Personal Access Token with 'repo' scope:"
  echo "1. Go to: https://github.com/settings/tokens/new"
  echo "2. Select 'repo' scope"
  echo "3. Generate token"
  echo "4. Export it: export GITHUB_TOKEN=your_token_here"
  echo ""
  echo "Then run this script again."
  exit 1
fi

# Create PR using curl
echo "Creating pull request using GitHub API..."
echo ""

RESPONSE=$(curl -s -w "\n%{http_code}" \
  -X POST \
  -H "Accept: application/vnd.github+json" \
  -H "Authorization: Bearer $GITHUB_TOKEN" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  "$API_URL" \
  -d "$JSON_PAYLOAD")

# Extract HTTP status code (last line)
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
# Extract response body (all but last line)
RESPONSE_BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -eq 201 ]; then
  echo "✅ Pull request created successfully!"
  echo ""
  PR_URL=$(echo "$RESPONSE_BODY" | jq -r '.html_url')
  PR_NUMBER=$(echo "$RESPONSE_BODY" | jq -r '.number')
  echo "PR #$PR_NUMBER: $PR_URL"
  echo ""
  echo "$RESPONSE_BODY" | jq '{
    number: .number,
    title: .title,
    html_url: .html_url,
    state: .state,
    user: .user.login,
    created_at: .created_at
  }'
else
  echo "❌ Failed to create pull request (HTTP $HTTP_CODE)"
  echo ""
  echo "Response:"
  echo "$RESPONSE_BODY" | jq '.'
fi
