# Backlot Lab R2 S5 — Focused policy test request

Owner continuation after repaired Lab Kotlin compile pass on 2026-11-24.
Completed repair verification: GitHub Actions run 45337219034, jobs 138252042519 and 138252183993 succeeded.
Application source for these unit tests is frozen at repair commit `379a3412ca84bf4fe70671fe14cec0bb56b0d316`.
Run only these source-staged unit test classes:
- `P10ReminderIconStylePolicyTest` (three styles; Living Energy default)
- `P10CreatorLockScreenMediaPolicyTest` (Creator opt-in; private Personal/21-day)
- `ReminderExperienceMixPolicyTest` (speech duck/recovery/fades/alarm gain)
Do NOT run full JVM suite, Android emulator, owner APK, Gradle install, lint, or signing. On failure, inspect exact class/test failure before any retrigger. Protect stable branches and Lab backend data.
