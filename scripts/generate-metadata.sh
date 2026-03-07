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

XLIFF_NS='xmlns:xliff="urn:oasis:names:tc:xliff:document:1.2"'

validate_xml() {
  python3 -c "
import xml.etree.ElementTree as ET, sys
try:
    ET.fromstring(sys.stdin.read())
except ET.ParseError as e:
    print(f'XML parse error: {e}', file=sys.stderr)
    sys.exit(1)
" < "$1"
}

# Check if a strings.xml has complete metadata:
# 1. xliff namespace declared
# 2. All format placeholders (%s, %d, etc.) wrapped in xliff:g tags
# 3. Every <string> has a preceding XML comment
has_complete_metadata() {
  local file="$1"
  local content
  content="$(cat "$file")"

  # Require explicit xliff namespace declaration
  if ! echo "$content" | grep -qF "$XLIFF_NS"; then
    return 1
  fi

  # Check if any format placeholders exist without xliff:g wrapping
  if echo "$content" | grep -P '%[0-9]*\$?[sdfu]' | grep -v 'xliff:g' | grep -q '<string'; then
    return 1
  fi

  # Every <string> must have a comment — require comment_count >= string_count
  local string_count comment_count
  string_count="$(echo "$content" | grep -c '<string ' || true)"
  comment_count="$(echo "$content" | grep -c '<!--' || true)"

  if [[ "$string_count" -gt 0 && "$comment_count" -lt "$string_count" ]]; then
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

  local result tmp_file
  tmp_file="$(mktemp)"
  result="$(claude -p "$prompt" --model claude-sonnet-4-6 2>/dev/null)"

  if [[ -z "$result" ]]; then
    rm -f "$tmp_file"
    echo "  ERROR: Claude returned empty output. Original file preserved." >&2
    return 1
  fi

  echo "$result" > "$tmp_file"

  if ! validate_xml "$tmp_file"; then
    echo "  ERROR: Claude output is not valid XML. Original file preserved." >&2
    echo "  Bad output saved to: $tmp_file" >&2
    return 1
  fi

  mv "$tmp_file" "$file"
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
