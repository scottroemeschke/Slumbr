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

# Quality — run ALL of these before pushing (mirrors CI)
./gradlew assembleDebug          # Build
./gradlew test                   # Unit tests
./gradlew detekt                 # Static analysis (method length, complexity, etc.)
./gradlew ktlintCheck            # Kotlin style lint
./gradlew lint                   # Android lint

# Deploy (WSL)
bash scripts/deploy-local-from-wsl.sh  # Build + copy APK to Windows for device install

# Info
./gradlew dependencies           # Dependency tree
adb devices                      # List connected devices
adb install app/build/outputs/apk/debug/app-debug.apk  # Manual install
```

**Before pushing any branch**, always run the CI checks locally: `check-core-boundary.py`, `assembleDebug`, `test`, `detekt`, `ktlintCheck`, `lint`. All must pass.

## Architecture Boundary: `core/` vs `android/`

All code under `dev.ashera.slumbr` is split into two top-level packages:

- **`core/`** — Pure Kotlin. Zero `android.*`/`androidx.*` imports. Domain models, interfaces, audio engine interface, playback logic, DSP. Testable without Android SDK.
- **`android/`** — Platform layer. Implements `core/` interfaces, owns all Android-specific code: Service, Activity, Compose UI, notifications, media session, DI, Intent handling.

**Hard rules:**
- `core/` MUST NOT import from `android.*`, `androidx.*`, or `dev.ashera.slumbr.android.*`
- `android/` depends on `core/`, never the reverse
- Interfaces live in `core/`; Android implementations live in `android/`
- Platform concepts (Intents, Notifications, MediaSession) stay in `android/` — don't leak string constants, Intent parsing, or Android types into `core/`
- Enforced by `scripts/check-core-boundary.py` in CI

**When adding new code**, ask: "Does this need Android?" If no → `core/`. If yes → `android/`, implementing a `core/` interface if the abstraction is useful.

## Directory Structure

```
Slumbr/
├── app/                          # Main application module
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/dev/ashera/slumbr/
│   │   │   │   ├── core/         # Pure Kotlin — no Android imports
│   │   │   │   │   ├── audio/    # AudioEngine interface, generators, DSP
│   │   │   │   │   ├── playback/ # PlaybackCommand, PlaybackState, PlaybackController
│   │   │   │   │   └── system/   # DndStateProvider interface
│   │   │   │   └── android/      # Platform layer — Android implementations
│   │   │   │       ├── audio/    # AudioTrackAudioEngine
│   │   │   │       ├── service/  # SoundService, notifications, media session
│   │   │   │       ├── system/   # AndroidDndStateProvider
│   │   │   │       ├── ui/       # Compose screens, theme
│   │   │   │       ├── di/       # Hilt DI module
│   │   │   │       ├── MainActivity.kt
│   │   │   │       └── SlumbrApplication.kt
│   │   │   ├── res/              # Resources (icons, strings, etc.)
│   │   │   └── AndroidManifest.xml
│   │   └── test/                 # Unit tests (mirrors core/ and android/ structure)
│   └── build.gradle.kts
├── gradle/                       # Gradle wrapper
├── scripts/                      # CI scripts (check-core-boundary.py, etc.)
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
- Fade in: 2s ramp. Fade out: 16s ramp. Smooth per-sample gain interpolation with smoothstep S-curve
- Seamless noise switching: swap `NoiseGenerator` atomically, no restart or volume gap
- Per-noise-type perceptual gain factors compensate for spectral energy distribution and phone speaker frequency response
- 22,050 Hz sample rate, 16-bit PCM — noise needs no high-frequency fidelity or float precision
- ~1 second chunks, ~3 second buffer — minimizes CPU wakes (1/sec) while preventing underruns

## Battery Optimization

- Foreground Service with `FOREGROUND_SERVICE_MEDIA_PLAYBACK` type
- Minimal wake locks — only audio-related
- No network usage during playback
- No unnecessary sensor or GPS access
- ~1 sec chunks with ~3 sec buffer — CPU wakes once per second instead of 10x

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
