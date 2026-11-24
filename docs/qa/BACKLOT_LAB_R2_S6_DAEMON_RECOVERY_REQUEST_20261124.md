# Backlot Lab R2 S6 — isolated memory recovery request
Date: 2026-11-24. User said continue; this gate remains the planned owner-preview QA, not a production/stable release.

## Prior verified gates
- Repaired app source SHA `379a3412ca84bf4fe70671fe14cec0bb56b0d316` and previous R2 source ancestry.
- Source fence + debug Kotlin: run `45337219034` SUCCESS.
- Three new policy test classes: run `45340227725` SUCCESS.
- Android 15 Lab debug + AndroidTest APK and TWO genuine first-run journeys: run `45343275501` SUCCESS, no production app/cloud changes.

## Latest blocked S6
- Run `45347700158` source fence PASSED, owner-preview compilation FAILED in `:app:compilePersonalStableKotlin` after approximately 29m52s.
- Root cause: forcing `-Pkotlin.compiler.execution.strategy=in-process` exhausted the shared 5 GiB Gradle JVM heap while Kotlin's FIR dataflow analyzer compiled a large source module. This was BEFORE Android Lint; don't label this a Lint failure.
- Previous older S6 attempt `45315344548` successfully compiled `personalStable` using an 8 GiB external Kotlin daemon but failed `:app:lintVitalAnalyzePersonalStable` when Gradle had only 2 GiB.

## Narrow, evidence-based configuration change
- No Android/Kotlin source, Gradle files, dependencies, backend or signing changes.
- CI only: on the same runner, first run `:app:compilePersonalStableKotlin` with 2 GiB Gradle + separate proven 8 GiB Kotlin daemon, then run `:app:assemblePersonalStable` with 6 GiB Gradle for full vital Lint, reusing compiled outputs. Serial single worker. Both phases bounded, each fails fast with separate diagnostics. Do NOT skip Lint or downgrade it.
- Publish only if `personalStable` is non-debuggable, `com.framebynavin.app.lab`, versionCode 337, known existing preview certificate, and SHA-256 verified. No owner installed APK or stable promotion without owner real-device review.
