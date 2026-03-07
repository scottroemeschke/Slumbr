#!/usr/bin/env bash
#
# Generate translated strings.xml files using Claude Code CLI.
# Usage: bash scripts/translate-strings.sh [locale ...]
# Example: bash scripts/translate-strings.sh es de ja fr
#
# Skips locales whose translation is already up-to-date with the source.
# Delete a locale's .source-hash file to force regeneration.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SOURCE_FILE="$PROJECT_ROOT/app/src/main/res/values/strings.xml"

# ~95% of world speakers (by total speakers, excluding English as source)
DEFAULT_LOCALES=(
  # 80% tier
  zh-rCN   # Mandarin Chinese (~1.1B)
  hi       # Hindi (~600M)
  es       # Spanish (~550M)
  ar       # Arabic (~370M)
  fr       # French (~300M)
  bn       # Bengali (~270M)
  pt-rBR   # Brazilian Portuguese (~260M)
  ru       # Russian (~250M)
  id       # Indonesian (~200M)
  # 95% tier
  ur       # Urdu (~230M)
  pa       # Punjabi (~150M)
  ja       # Japanese (~125M)
  de       # German (~130M)
  fa       # Persian (~110M)
  sw       # Swahili (~100M)
  vi       # Vietnamese (~85M)
  ta       # Tamil (~85M)
  tr       # Turkish (~85M)
  it       # Italian (~85M)
  ko       # Korean (~80M)
  ms       # Malay (~80M)
  th       # Thai (~70M)
  pl       # Polish (~45M)
  uk       # Ukrainian (~45M)
)

LOCALES=("${@:-${DEFAULT_LOCALES[@]}}")

if [[ ! -f "$SOURCE_FILE" ]]; then
  echo "ERROR: Source strings.xml not found at $SOURCE_FILE" >&2
  exit 1
fi

if ! command -v claude &>/dev/null; then
  echo "ERROR: claude CLI not found. Install Claude Code first." >&2
  exit 1
fi

SOURCE_CONTENT="$(cat "$SOURCE_FILE")"
SOURCE_HASH="$(sha256sum "$SOURCE_FILE" | cut -d' ' -f1)"

# Validate that source strings.xml has translation metadata before proceeding.
# Run generate-metadata.sh first if this fails.
validate_metadata() {
  local has_issues=false

  # Check xliff namespace
  if ! echo "$SOURCE_CONTENT" | grep -q 'xliff'; then
    echo "ERROR: Source strings.xml missing xliff namespace declaration." >&2
    has_issues=true
  fi

  # Check that strings with format placeholders have xliff:g tags
  if echo "$SOURCE_CONTENT" | grep -P '%[0-9]*\$?[sdfu]' | grep -v 'xliff:g' | grep -q '<string'; then
    echo "ERROR: Source strings.xml has format placeholders not wrapped in xliff:g tags." >&2
    has_issues=true
  fi

  # Check for XML comments (translator context)
  if ! echo "$SOURCE_CONTENT" | grep -q '<!--'; then
    echo "ERROR: Source strings.xml has no XML comments for translator context." >&2
    has_issues=true
  fi

  if [[ "$has_issues" == true ]]; then
    echo "" >&2
    echo "Run 'bash scripts/generate-metadata.sh' first to add translation metadata." >&2
    exit 1
  fi
}

validate_metadata

locale_to_language() {
  case "$1" in
    zh-rCN) echo "Simplified Chinese" ;;
    hi)     echo "Hindi" ;;
    es)     echo "Spanish" ;;
    ar)     echo "Arabic" ;;
    fr)     echo "French" ;;
    bn)     echo "Bengali" ;;
    pt-rBR) echo "Brazilian Portuguese" ;;
    ru)     echo "Russian" ;;
    id)     echo "Indonesian" ;;
    ur)     echo "Urdu" ;;
    pa)     echo "Punjabi" ;;
    ja)     echo "Japanese" ;;
    de)     echo "German" ;;
    fa)     echo "Persian" ;;
    sw)     echo "Swahili" ;;
    vi)     echo "Vietnamese" ;;
    ta)     echo "Tamil" ;;
    tr)     echo "Turkish" ;;
    it)     echo "Italian" ;;
    ko)     echo "Korean" ;;
    ms)     echo "Malay" ;;
    th)     echo "Thai" ;;
    pl)     echo "Polish" ;;
    uk)     echo "Ukrainian" ;;
    nl)     echo "Dutch" ;;
    *)      echo "$1" ;;
  esac
}

is_up_to_date() {
  local output_dir="$1"
  local hash_file="$output_dir/.source-hash"
  if [[ -f "$hash_file" && -f "$output_dir/strings.xml" ]]; then
    local stored_hash
    stored_hash="$(cat "$hash_file")"
    [[ "$stored_hash" == "$SOURCE_HASH" ]]
  else
    return 1
  fi
}

translate_locale() {
  local locale="$1"
  local language
  language="$(locale_to_language "$locale")"
  local output_dir="$PROJECT_ROOT/app/src/main/res/values-$locale"
  local output_file="$output_dir/strings.xml"
  local hash_file="$output_dir/.source-hash"

  echo "--- $language ($locale) ---"

  if is_up_to_date "$output_dir"; then
    echo "  SKIP: Translation is up-to-date with source."
    return 0
  fi

  mkdir -p "$output_dir"

  local translate_prompt
  translate_prompt="$(cat <<PROMPT
You are a professional Android app translator. Translate the following strings.xml from English to $language ($locale).

Rules:
1. Output ONLY the translated XML — no explanation, no markdown fences, no extra text.
2. Keep the XML declaration, <resources> tags, all string name attributes, and xliff namespace exactly as-is.
3. Do NOT translate strings with translatable="false".
4. Keep "Slumbr" as-is — it's a brand name.
5. Preserve all xliff:g tags around format placeholders — keep the id and example attributes, only translate surrounding text.
6. Preserve XML comments but translate them to $language so they serve as context for $language-speaking translators.
7. Translate naturally and conversationally, not literally.
8. The app is a sleep sound generator (brown noise, white noise, pink noise, etc.).

Source strings.xml:
$SOURCE_CONTENT
PROMPT
)"

  local translation
  translation="$(claude -p "$translate_prompt" --model claude-sonnet-4-6 2>/dev/null)"

  echo "$translation" > "$output_file"
  echo "  Written: $output_file"

  # Pass 2: Judge
  local verdict
  verdict="$(judge_translation "$locale" "$language" "$translation")"

  # Pass 3: Refine (only if judge found issues)
  if [[ "$verdict" == *"WARNING:"* ]]; then
    echo "  Warnings found, refining..."
    echo "$verdict" | sed 's/^/    /'
    translation="$(refine_translation "$locale" "$language" "$translation" "$verdict")"
    echo "$translation" > "$output_file"
    echo "  Refined: $output_file"
  else
    echo "  OK: No issues, skipping refinement."
  fi

  # Store source hash so we can skip this locale next run
  echo "$SOURCE_HASH" > "$hash_file"
}

judge_translation() {
  local locale="$1"
  local language="$2"
  local translation="$3"

  local judge_prompt
  judge_prompt="$(cat <<PROMPT
You are a translation quality reviewer for an Android app called Slumbr (a sleep sound generator).
Review this $language ($locale) translation of strings.xml.

Check for:
1. ACCURACY: Do translations convey the correct meaning?
2. NATURALNESS: Do they sound natural to a native $language speaker?
3. PLACEHOLDERS: Are all %s/%d placeholders preserved and wrapped in xliff:g tags?
4. XML VALIDITY: Is the XML well-formed with proper escaping?
5. CULTURAL FIT: Are there any culturally inappropriate translations?
6. BRAND: Is "Slumbr" kept untranslated?

Source (English):
$SOURCE_CONTENT

Translation ($language):
$translation

Output ONLY issues found, one per line, prefixed with "WARNING: ".
If everything looks good, output exactly: "OK: No issues found."
Do not output anything else.
PROMPT
)"

  echo "  Judging..." >&2
  local verdict
  verdict="$(claude -p "$judge_prompt" --model claude-sonnet-4-6 2>/dev/null)"
  echo "$verdict"
}

refine_translation() {
  local locale="$1"
  local language="$2"
  local translation="$3"
  local verdict="$4"

  local refine_prompt
  refine_prompt="$(cat <<PROMPT
You are a professional Android app translator. You translated strings.xml to $language ($locale) and a reviewer found some issues.

Your job: produce a final, refined translation that addresses legitimate issues from the review. Use your judgment — not every warning needs action. Ignore nitpicks that would make the translation worse.

Rules:
1. Output ONLY the final XML — no explanation, no markdown fences, no extra text.
2. Keep XML declaration, <resources> tags, all string name attributes, and xliff namespace exactly as-is.
3. Keep "Slumbr" as-is — brand name.
4. Preserve all xliff:g tags around format placeholders with their id and example attributes.
5. Translate XML comments to $language.
6. Keep translations natural and conversational.
7. The app is a sleep sound generator (brown noise, white noise, pink noise, etc.).

Source (English):
$SOURCE_CONTENT

Current translation ($language):
$translation

Reviewer feedback:
$verdict
PROMPT
)"

  claude -p "$refine_prompt" --model claude-sonnet-4-6 2>/dev/null
}

echo "Source: $SOURCE_FILE"
echo "Source hash: $SOURCE_HASH"
echo "Locales: ${LOCALES[*]}"
echo ""

skipped=0
translated=0

for locale in "${LOCALES[@]}"; do
  translate_locale "$locale"
  echo ""
done

echo "Done. Processed ${#LOCALES[@]} locales."
