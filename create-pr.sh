#!/bin/bash

# Create Pull Request for Notification System
gh pr create \
  --title "Add Notification System with Approval Workflow" \
  --body "$(cat PR_DESCRIPTION.md)" \
  --head claude/maven-build-config-01G4pWU2AnnpH8pHS7hKfqVH

echo "Pull request created successfully!"
