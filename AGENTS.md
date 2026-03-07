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

## Jetpack Compose UI Principles

This project follows modern Compose-first patterns. **Always use context7 to look up current Compose/Material 3 APIs before writing UI code** — Compose evolves rapidly and stale patterns cause real bugs.

### Animation
- Use **declarative animations** (`animateColorAsState`, `animateFloatAsState`, `AnimatedVisibility`, `Crossfade`) driven by state changes — never tick-based updates from background threads
- Compose's animation system runs at render frame rate (60fps+). Driving UI color/size from a `StateFlow` updated on a background thread (e.g., 10fps audio chunks) causes visible stutter
- **Decouple visual animations from engine state.** Audio fade and UI fade should share duration/direction but each be handled by its own system (audio: coroutine gain ramp, UI: Compose `tween`/`spring`)
- Use `tween()` for predictable time-based transitions, `spring()` for physics-based feel
- Match animation durations to the real-world effect they represent (e.g., fade-out button color matches audio fade-out time)

### State & Recomposition
- Hoist state up — Composables should be stateless where possible, receiving state via parameters
- Use `StateFlow` + `collectAsStateWithLifecycle()` for ViewModel → UI state
- Use `remember` and `derivedStateOf` to minimize unnecessary recomposition
- **Never** flow high-frequency updates (audio samples, timers) through ViewModel state — use Compose-local animation or `LaunchedEffect` instead
- Prefer `val` over `var` in state classes; use `data class` with `copy()` for updates

### Material 3
- Use Material 3 components and theming (dynamic color where appropriate)
- Follow M3 color roles: `primary`, `surfaceContainer`, `onSurface`, etc.
- Use `MaterialTheme.typography` and `MaterialTheme.colorScheme` — never hardcode colors/fonts

### Reference
- **Official Compose docs**: developer.android.com/develop/ui/compose
- **Animation guide**: developer.android.com/develop/ui/compose/animation/introduction
- **State guide**: developer.android.com/develop/ui/compose/state
- **Side effects**: developer.android.com/develop/ui/compose/side-effects
- **Use context7** (`resolve-library-id` → `query-docs`) to look up current API signatures before using any Compose API you're not 100% sure about

## Audio Engine Design Principles

- Generate noise algorithmically (PCM) — no audio file assets
- Use `AudioTrack` in streaming mode for low-latency, low-memory playback
- All noise generation runs on a background coroutine, never the UI thread
- Target-gain model: `stop()` sets target to 0, playback loop naturally fades out per-sample
- Fade in: 2s linear ramp. Fade out: 5s linear ramp. Smooth per-sample gain interpolation
- Seamless noise switching: swap `NoiseGenerator` atomically, no restart or volume gap
- Per-noise-type perceptual gain factors compensate for spectral energy distribution and phone speaker frequency response
- Buffer size balances latency (fade responsiveness) vs stability (no underruns) — currently ~500ms

## Battery Optimization

- Foreground Service with `FOREGROUND_SERVICE_MEDIA_PLAYBACK` type
- Minimal wake locks — only audio-related
- No network usage during playback
- No unnecessary sensor or GPS access
- Buffer size tuned to balance latency vs CPU wake-ups (~500ms chunks written every ~100ms)

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
