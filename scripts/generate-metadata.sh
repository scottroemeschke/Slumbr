#!/usr/bin/env bash
#
# Add translation metadata to strings.xml files.
# Adds xliff:g tags for format placeholders and XML comments for translator context.
# Smart: skips files that already have complete metadata.
#
# Usage: bash scripts/generate-metadata.sh [path/to/strings.xml ...]
# Default: app/src/main/res/values/strings.xml

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
DEFAULT_FILES=("$PROJECT_ROOT/app/src/main/res/values/strings.xml")

FILES=("${@:-${DEFAULT_FILES[@]}}")

if ! command -v claude &>/dev/null; then
  echo "ERROR: claude CLI not found. Install Claude Code first." >&2
  exit 1
fi

# Check if a strings.xml has complete metadata:
# 1. All format placeholders (%s, %d, etc.) wrapped in xliff:g tags
# 2. XML comments above string entries
has_complete_metadata() {
  local file="$1"
  local content
  content="$(cat "$file")"

  # Check if any format placeholders exist without xliff:g wrapping
  # Look for %s, %d, %1$s etc. that are NOT inside an xliff:g tag
  if echo "$content" | grep -P '%[0-9]*\$?[sdfu]' | grep -v 'xliff:g' | grep -q '<string'; then
    return 1
  fi

  # Check if there are XML comments (at least one <!-- above a <string)
  if ! echo "$content" | grep -q '<!--'; then
    return 1
  fi

  # Count strings (excluding translatable="false") vs comments
  local string_count comment_count
  string_count="$(echo "$content" | grep -c '<string ' || true)"
  comment_count="$(echo "$content" | grep -c '<!--' || true)"

  # Every string should have a comment; allow some slack (at least 80%)
  if [[ "$string_count" -gt 0 && "$comment_count" -lt $(( string_count * 80 / 100 )) ]]; then
    return 1
  fi

  return 0
}

add_metadata() {
  local file="$1"
  local content
  content="$(cat "$file")"

  echo "  Adding metadata..."

  local prompt
  prompt="$(cat <<PROMPT
You are an Android localization expert. Add translation metadata to this strings.xml file.

Rules:
1. Output ONLY the modified XML — no explanation, no markdown fences, no extra text.
2. Add the xliff namespace to <resources> if not already present:
   xmlns:xliff="urn:oasis:names:tc:xliff:document:1.2"
3. Wrap ALL format placeholders (%s, %d, %1\$s, etc.) in xliff:g tags:
   <xliff:g id="descriptive_id" example="realistic_example">%s</xliff:g>
   - id: describe what the placeholder represents (e.g., "noise_type" for a noise name)
   - example: provide a realistic example value (e.g., "Brown" for a noise type)
4. Add a brief XML comment above EACH <string> element explaining:
   - Where this string appears in the app (notification, button, screen title, etc.)
   - Any context a translator needs (e.g., "this is a button label, keep it short")
5. Do NOT change any string values, names, or attributes.
6. Do NOT remove or modify existing metadata that is already correct.
7. The app is Slumbr — a sleep sound generator with brown/white/pink noise.

Current strings.xml:
$content
PROMPT
)"

  local result
  result="$(claude -p "$prompt" --model claude-sonnet-4-6 2>/dev/null)"

  echo "$result" > "$file"
  echo "  Updated: $file"
}

for file in "${FILES[@]}"; do
  echo "--- $(basename "$(dirname "$file")")/strings.xml ---"

  if [[ ! -f "$file" ]]; then
    echo "  ERROR: File not found: $file" >&2
    continue
  fi

  if has_complete_metadata "$file"; then
    echo "  SKIP: Metadata already complete."
  else
    add_metadata "$file"
  fi

  echo ""
done

echo "Done."
