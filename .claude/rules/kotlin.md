---
description: Kotlin and Android conventions — always apply
globs:
  - "**/*.kt"
  - "**/*.kts"
---

# Kotlin & Android

- Kotlin idioms over Java patterns — use `data class`, `sealed class`, `when`, extension functions
- Prefer `val` over `var`, immutable collections over mutable
- Nullable types only when genuinely nullable — avoid `!!`, use safe calls and `let`/`run`
- Coroutines for async — never block the main thread
- Use `StateFlow` / `SharedFlow` for reactive state, not `LiveData` (Compose-first)
- Compose: stateless composables where possible, hoist state up
- Compose: use `remember` and `derivedStateOf` to minimize recomposition
- Compose animations: use declarative APIs (`animateColorAsState`, `animateFloatAsState`, etc.) — never drive UI visuals from background thread StateFlow ticks
- Decouple visual animations from engine/service state — match timing but let each system own its animation
- Material 3 theming — dynamic color where appropriate
- Android resources: strings in `strings.xml`, dimensions in `dimens.xml`
- Follow Android lifecycle — don't leak contexts or hold activity references in services
