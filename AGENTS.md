# Slumbr — Agent Documentation

> `CLAUDE.md` symlinks here. Always edit `AGENTS.md`.

## Project

Open-source sleep sound generator — brown noise, white noise, pink noise, and more. Native Android app built with Kotlin and Jetpack Compose. Designed for minimal battery impact during extended background playback.

## Development Environment

**At session start**, detect the OS (`uname -s`). On Linux → we're in WSL; on Darwin → native macOS.

**WSL (Linux) workflow:**
- Claude Code runs in WSL; Android builds happen via Gradle CLI
- No emulator in WSL — build APKs, then install on a connected device or Windows-side emulator via `adb`
- When a fix is ready for testing, proactively ask: "Want me to build and push the APK to your device?"

## Stack

| Layer | Technologies |
|-------|-------------|
| UI | Kotlin, Jetpack Compose (Material 3) |
| Audio | Android AudioTrack API (low-level PCM generation) |
| Background | Foreground Service with persistent notification |
| Build | Gradle (Kotlin DSL), AGP 9.x |
| Min SDK | 28 (Android 9.0) |
| Target SDK | 36 (Android 16) |

## Commands

```bash
# Build
./gradlew assembleDebug          # Debug APK
./gradlew assembleRelease        # Release APK
./gradlew installDebug           # Build + install on connected device

# Quality
./gradlew lint                   # Android lint
./gradlew ktlintCheck            # Kotlin lint
./gradlew ktlintFormat           # Kotlin format
./gradlew test                   # Unit tests

# Info
./gradlew dependencies           # Dependency tree
adb devices                      # List connected devices
adb install app/build/outputs/apk/debug/app-debug.apk  # Manual install
```

## Directory Structure

```
Slumbr/
├── app/                          # Main application module
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/dev/ashera/slumbr/
│   │   │   │   ├── ui/           # Compose UI (screens, components, theme)
│   │   │   │   ├── audio/        # Audio generation engine
│   │   │   │   ├── service/      # Foreground service for background playback
│   │   │   │   └── MainActivity.kt
│   │   │   ├── res/              # Resources (icons, strings, etc.)
│   │   │   └── AndroidManifest.xml
│   │   └── test/                 # Unit tests
│   └── build.gradle.kts
├── gradle/                       # Gradle wrapper
├── specs/                        # Feature specifications
├── docs/                         # Documentation
├── build.gradle.kts              # Root build file
├── settings.gradle.kts           # Project settings
└── gradle.properties             # Gradle config
```

## Workflow with Linear

Linear is the single source of truth for project management. Do not use GitHub Issues.

- **Workspace**: Slumbr (`https://linear.app/slumbr`)
- **Team**: SLU — only interact with this team, never others

Full workflow rules: `.claude/rules/linear-workflow.md`

**Hierarchy**: Initiative (human-created) → Project → Milestone (optional) → Issue

**Key conventions:**
- Every non-trivial task gets a Linear issue before work begins
- Specs approved via `spec-planner` → Claude creates Project + Issues in Linear
- PRs reference Linear issues: `Fixes SLU-XX` + issue URL in description
- Issues auto-close on PR merge via GitHub integration
- Priority and due dates are human-set only
- Every issue needs Type + Scope labels; risk labels for non-trivial work
- Flag labels (Breaking Change, Spike, Needs Design) applied when relevant

**Risk-driven reviews** (check issue labels before creating PR):
- **Security: High** → run security review + `/code-review`
- **Tech Risk: High** → invoke oracle agent + `/code-review`
- **Tech Risk: Medium** → run `/code-review`

**Ad-hoc issues**: When discovering bugs, tech debt, or follow-ups during work, create issues in Backlog with appropriate Type label.

---

## Audio Engine Design Principles

- Generate noise algorithmically (PCM) — no audio file assets
- Use `AudioTrack` in streaming mode for low-latency, low-memory playback
- All noise generation runs on a background thread, never the UI thread
- Fade in/out on start/stop (linear ramp over ~2 seconds) to avoid harsh transitions
- Support concurrent noise mixing if multiple types selected

## Battery Optimization

- Foreground Service with `FOREGROUND_SERVICE_MEDIA_PLAYBACK` type
- Minimal wake locks — only audio-related
- No network usage during playback
- No unnecessary sensor or GPS access
- Efficient PCM buffer sizes to balance latency vs CPU wake-ups

---

## Claude Code Configuration

Conventions and standards live in `.claude/rules/`. Subagents in `.claude/agents/`. Skills in `.claude/skills/`.

Team-shared settings: `.claude/settings.json` (committed).
Personal overrides: `.claude/settings.local.json` (gitignored).

**Allowlist policy:**
- Git, gh CLI, Gradle, adb, Kotlin commands — allowed
- Destructive commands (`rm -rf`, `--force`, `--admin`) — denied

### Skills

| Skill | Command | Description |
|-------|---------|-------------|
| `spec-planner` | `/spec-planner` | Dialogue-driven spec development through skeptical questioning |
| `code-review` | `/code-review` | Review current branch changes for bugs, quality, and best practices |
