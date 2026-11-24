# Backlot Lab R2 — S6 Verified Owner Preview Request
Date: 2026-11-24. Founder continued after successful Stage 5 QA, for the planned Stage 6 owner-preview gate; no stable promotion authorized.

## Verified evidence
- Repaired Kotlin compile SUCCESS: run 45337219034; source app commit `379a3412ca84bf4fe70671fe14cec0bb56b0d316`.
- Three focused policy test classes SUCCESS: run 45340227725.
- Android15 Lab disposable debug APK and AndroidTest APK assembled with exact app tree: run 45343275501.
- Android15 original five-page Creator first-run → Home → Personal-first Reminder Hub → Creator/Personal navigation twice: 2/2 success in run 45343275501.
- Prior preview run 45315344548 failed in `:app:lintVitalAnalyzePersonalStable` from 2 GiB Gradle heap exhaustion; source Kotlin compilation succeeded. This is not permission to disable Lint.

## Authorized scope
- Exact same `app` source tree as repaired, compiled and Android15-tested commit `379a3412ca84bf4fe70671fe14cec0bb56b0d316`, no new feature changes.
- Build **one** non-debuggable `personalStable` Backlot Lab APK (`com.framebynavin.app.lab`, versionCode 337) with the **existing checked-in prototype signing identity**, not Play production release key.
- Keep full vital Android Lint enabled. Gradle heap raised to 5 GiB with in-process Kotlin to avoid parallel heavy daemon; one worker; bounded run.
- Before publishing, validate signed certificate, Lab package and label, non-debuggable manifest, source identity and SHA-256 APK.
- On any failure, stop. Diagnose specific gate; avoid unrelated source changes or arbitrary full-pipeline reruns.
- Physical tablet acceptance of background/media/audio/lock-screen and 18-scenario matrix remains pending. Do NOT promote stable or publish to Play. No Supabase writes or resets.
