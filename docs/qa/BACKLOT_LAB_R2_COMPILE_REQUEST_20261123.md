# Backlot Lab R2 — targeted Kotlin repair verification

Date: 2026-11-24

Root cause: 164 compiler diagnostics confined to `V144SettingsHub.kt` (158) and `V407ReminderAlertReadiness.kt` (6), first reported by GitHub Actions run 45233124177.

Scope: One isolated repair candidate based on immutable R2 `1577669bc328153a365661a7f3d36fb1558e891c`; only those two production Kotlin paths may differ. Source-contract QA re-verifies the Lab package/backend switch and approved first-run/Settings/Reminder flows. Target: only `:app:compileDebugKotlin -PbacklotLab=true`.

Repairs: remove duplicate malformed Settings markup while retaining the quiet P10 navigation and functional Lab settings search; remove unused private legacy voice-preview helper; replace retired `V336DirectorPalette` references with existing Backlot theme colors in readiness disclosure.

Prohibited: change original/stable source, merge/release, run unrelated JVM suites or Android emulators, package an APK, migrate/read/change Supabase data, edit signing keys, or auto-promote stable.
