# Slumbr — Product Requirements

## Problem

Existing sleep sound apps are closed-source, ad-riddled, and often require subscriptions for basic features. Users want a simple, free, battery-efficient app that plays soothing noise all night without interruption.

## Target User

Anyone who uses noise to sleep, focus, or mask environmental sounds. Prioritizes simplicity — wants to tap one button and forget about it.

## Core Concepts

| Concept | Definition |
|---------|-----------|
| Noise Type | A color of noise (white, pink, brown, etc.) with distinct spectral characteristics |
| Audio Engine | Algorithmic PCM generator — no audio files, generates samples in real-time |
| Foreground Service | Android service that keeps playback alive when app is backgrounded or screen is off |
| Fade | Gradual volume ramp on start/stop to avoid jarring transitions |

## V1 — Core Sleep Sounds

### V1.0 Foundation (Done)
- Project scaffold with Gradle, Kotlin, Jetpack Compose
- CI/workflow tooling (Linear, agents, skills)
- Audio engine with AudioTrack streaming
- Noise generators: white, pink, brown
- 2-second linear fade in/out
- Foreground service with notification controls
- Minimal dark UI with noise type cards and volume slider
- Unit tests for noise generators

### V1.1 Timer & Sleep Controls
- Sleep timer (15m, 30m, 1h, 2h, custom) with gradual fade-out
- Timer display in UI and notification
- Auto-stop after timer expires

### V1.2 Additional Noise Types
- Blue noise (high-frequency emphasis)
- Violet noise (steeper high-frequency emphasis)
- Grey noise (perceptually flat)

### V1.3 Mixing
- Play multiple noise types simultaneously
- Per-noise volume control
- Save/load presets

### V1.4 Polish
- Custom app icon (proper mipmap PNGs across densities)
- Animated waveform visualization (subtle, battery-conscious)
- Haptic feedback on tap
- Landscape support

### V1 Non-Goals
- Streaming/network audio
- Nature sounds or non-noise audio
- User accounts or cloud sync
- Social features
- Monetization of any kind

## V2 — Enhanced Experience

### V2.1 Nature & Ambient Sounds
- Rain, ocean waves, wind, thunder
- Layerable with noise types
- Asset-based or procedural generation

### V2.2 Favorites & History
- Mark noise configurations as favorites
- Quick-launch from notification or widget

### V2.3 Home Screen Widget
- One-tap play/stop widget
- Shows current playing state

### V2.4 Alarm Integration
- Gentle wake-up: gradual fade-out of noise + optional alarm sound
- Integration with system alarm

### V2 Non-Goals
- Music streaming
- Podcast playback
- Social sharing
- In-app purchases

## V3+ — Future / Stretch

- Wear OS companion
- Sleep tracking integration (Health Connect)
- Binaural beats
- Custom noise profile editor (EQ curves)
- Tasker/automation integration
- F-Droid distribution
