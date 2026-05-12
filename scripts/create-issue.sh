#!/bin/bash
# Create GitHub Issue from task in 01-analyst/TASKS.md
# Usage: ./scripts/create-issue.sh [--push]

set -e

REPO="jQwout/ai-editor"
TOKEN="${GITHUB_TOKEN:-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}"
TASKS_FILE="01-analyst/TASKS.md"

# Parse arguments
PUSH=false
while [[ $# -gt 0 ]]; do
  case $1 in
    --push) PUSH=true; shift ;;
    *) echo "Unknown option: $1"; exit 1 ;;
  esac
done

# Check if tasks file exists
if [[ ! -f "$TASKS_FILE" ]]; then
  echo "Error: $TASKS_FILE not found"
  exit 1
fi

# Extract first task (title = first line after ##, body = until next ## or end)
TITLE=$(sed -n '1p' "$TASKS_FILE" | sed 's/^# //' | head -c 200)
BODY=$(sed '1d' "$TASKS_FILE" | head -c 6000)

if [[ -z "$TITLE" ]]; then
  echo "Error: Could not extract task title"
  exit 1
fi

echo "Creating issue: $TITLE"

# Create issue via GitHub API
RESPONSE=$(curl -s -X POST "https://api.github.com/repos/$REPO/issues" \
  -H "Authorization: token $TOKEN" \
  -H "Content-Type: application/json" \
  -d "$(jq -n --arg title "$TITLE" --arg body "$BODY" '{
    title: $title,
    body: $body,
    labels: ["external-task"]
  }')")

ISSUE_URL=$(echo "$RESPONSE" | jq -r '.html_url // empty')
ISSUE_NUM=$(echo "$RESPONSE" | jq -r '.number // empty')

if [[ -z "$ISSUE_URL" || "$ISSUE_URL" == "null" ]]; then
  echo "Error creating issue:"
  echo "$RESPONSE"
  exit 1
fi

echo "Created: $ISSUE_URL"

# Optionally push to remote
if [[ "$PUSH" == "true" ]]; then
  BRANCH="issue-$ISSUE_NUM"
  git checkout -b "$BRANCH" 2>/dev/null || true
  git add "$TASKS_FILE"
  git commit -m "task: $TITLE (issue #$ISSUE_NUM)" 2>/dev/null || echo "Nothing to commit"
  git push -u origin "$BRANCH" 2>/dev/null || echo "Branch already exists"
  echo "Pushed to branch: $BRANCH"
fi

echo "Done. Issue #$ISSUE_NUM"