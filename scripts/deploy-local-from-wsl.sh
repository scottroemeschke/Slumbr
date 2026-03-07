#!/usr/bin/env bash
set -euo pipefail

# Config
DEST_DIR="/mnt/c/Users/Ashera/Projects/slumbr"
APK_SOURCE="app/build/outputs/apk/debug/app-debug.apk"
MAX_APKS=10
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

cd "$PROJECT_ROOT"

# Extract issue ID from branch name (e.g. feature/slu-12-description -> slu-12)
BRANCH="$(git branch --show-current)"
ISSUE_ID="$(echo "$BRANCH" | grep -oiP 'slu-\d+' | head -1 | tr '[:upper:]' '[:lower:]')" || true

if [[ -z "$ISSUE_ID" ]]; then
    echo "Warning: No SLU-XX issue ID found in branch '$BRANCH', using 'dev'"
    ISSUE_ID="dev"
fi

TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
APK_NAME="slumbr-${ISSUE_ID}-${TIMESTAMP}.apk"

# Build
echo "Building debug APK..."
./gradlew assembleDebug --quiet

if [[ ! -f "$APK_SOURCE" ]]; then
    echo "Error: APK not found at $APK_SOURCE"
    exit 1
fi

# Ensure destination exists
mkdir -p "$DEST_DIR"

# Enforce max APK limit (delete oldest first)
EXISTING=($(ls -1t "$DEST_DIR"/slumbr-*.apk 2>/dev/null || true))
DELETE_COUNT=$(( ${#EXISTING[@]} - MAX_APKS + 1 ))

if (( DELETE_COUNT > 0 )); then
    echo "Cleaning up $DELETE_COUNT old APK(s)..."
    for f in "${EXISTING[@]:$(( MAX_APKS - 1 ))}"; do
        echo "  Removing $(basename "$f")"
        rm "$f"
    done
fi

# Copy
cp "$APK_SOURCE" "$DEST_DIR/$APK_NAME"
echo ""
echo "Deployed: $APK_NAME"
echo "     To: C:\\Users\\Ashera\\Projects\\slumbr\\$APK_NAME"
echo ""
echo "To install via ADB from Windows PowerShell:"
echo "  adb install \"C:\\Users\\Ashera\\Projects\\slumbr\\$APK_NAME\""
