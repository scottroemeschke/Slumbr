# Translation Workflow

Slumbr uses Claude Code CLI (`claude -p`) to generate and maintain translations for all supported locales.

## Overview

```text
Source strings.xml (English)
        │
        ▼
┌──────────────────┐
│ generate-metadata │  Add xliff:g tags + translator comments
└────────┬─────────┘
         │
         ▼
  Source strings.xml (with metadata)
         │
         ▼
┌──────────────────┐
│ translate-strings │  Translate to each locale via Claude
└────────┬─────────┘
         │
         ├──► Pass 1: Claude translates
         │
         ├──► Pass 2: Claude judges quality
         │
         └──► Pass 3: Claude refines (only if judge found issues)
                │
                ▼
         values-{locale}/strings.xml
```

## Scripts

### `scripts/generate-metadata.sh`

Adds translation metadata to the source `strings.xml`:

- **xliff:g tags** around format placeholders (`%s`, `%d`) with `id` and `example` attributes
- **XML comments** above each string explaining context and usage for translators

Smart: skips files that already have complete metadata.

```bash
# Add metadata to source strings.xml (default)
bash scripts/generate-metadata.sh

# Add metadata to a specific file
bash scripts/generate-metadata.sh app/src/main/res/values/strings.xml
```

### `scripts/translate-strings.sh`

Generates translated `strings.xml` files for target locales.

```bash
# Translate all 24 default locales (~95% of world speakers)
bash scripts/translate-strings.sh

# Translate specific locales
bash scripts/translate-strings.sh es de ja
```

**Pre-flight check**: Fails immediately if source `strings.xml` is missing metadata. Run `generate-metadata.sh` first.

**Smart skipping**: Stores a hash of the source file alongside each translation. On re-run, skips locales whose translation is already up-to-date with the current source. Delete a locale's `.source-hash` file to force regeneration.

**3-pass pipeline**: Each locale goes through translate → judge → refine:
1. **Translate**: Claude generates the initial translation
2. **Judge**: A second Claude pass reviews for accuracy, naturalness, placeholder preservation, XML validity, cultural appropriateness, and brand consistency
3. **Refine** (conditional): If the judge found warnings, a third Claude pass produces a final translation that addresses legitimate issues while ignoring nitpicks. Skipped if judge found no issues.

## Supported Locales (24)

### 80% tier (~80% of world speakers)

| Code | Language | Speakers |
|------|----------|----------|
| zh-rCN | Simplified Chinese | ~1.1B |
| hi | Hindi | ~600M |
| es | Spanish | ~550M |
| ar | Arabic | ~370M |
| fr | French | ~300M |
| bn | Bengali | ~270M |
| pt-rBR | Brazilian Portuguese | ~260M |
| ru | Russian | ~250M |
| id | Indonesian | ~200M |

### 95% tier (additional ~15%)

| Code | Language | Speakers |
|------|----------|----------|
| ur | Urdu | ~230M |
| pa | Punjabi | ~150M |
| ja | Japanese | ~125M |
| de | German | ~130M |
| fa | Persian | ~110M |
| sw | Swahili | ~100M |
| vi | Vietnamese | ~85M |
| ta | Tamil | ~85M |
| tr | Turkish | ~85M |
| it | Italian | ~85M |
| ko | Korean | ~80M |
| ms | Malay | ~80M |
| th | Thai | ~70M |
| pl | Polish | ~45M |
| uk | Ukrainian | ~45M |

## Important: Run Outside Claude Code

These scripts use `claude -p` which **cannot run inside a Claude Code session** — nested sessions will crash. Always run them from a separate terminal.

To let Claude Code review the results after running, pipe output to a temp file:

```bash
# Run from a separate terminal (not inside Claude Code)
bash scripts/generate-metadata.sh > /tmp/metadata-out.txt 2>&1
bash scripts/translate-strings.sh zh-rCN hi > /tmp/translate-out.txt 2>&1
```

Then tell Claude Code the script finished and it can read `/tmp/metadata-out.txt` or `/tmp/translate-out.txt` to verify the output looks correct. This is especially useful after modifying the scripts themselves — have Claude Code review the pipeline output to catch prompt or logic issues.

## Typical Workflow

1. Edit `app/src/main/res/values/strings.xml` (add/change English strings)
2. Run `bash scripts/generate-metadata.sh` from a separate terminal
3. Run `bash scripts/translate-strings.sh` from a separate terminal
4. Optionally have Claude Code read the output files to sanity-check
5. Commit all generated files

## How Android Fallback Works

Android automatically falls back to the default `values/strings.xml` (English) when:
- A locale-specific `values-{locale}/strings.xml` doesn't exist
- A specific string key is missing from the locale file

This means untranslated locales gracefully show English — no crashes.

## Forcing Regeneration

To force a specific locale to regenerate (e.g., after fixing a judge warning):

```bash
rm app/src/main/res/values-es/.source-hash
bash scripts/translate-strings.sh es
```

To regenerate all locales:

```bash
find app/src/main/res/values-*/  -name '.source-hash' -delete
bash scripts/translate-strings.sh
```

## Cost

Each locale requires 2-3 Claude API calls (translate + judge, + refine if needed). Default set of 24 locales = 48-72 calls per full run. Smart skipping avoids unnecessary calls on re-runs.
