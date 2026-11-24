# Backlot Lab R2 — Android 15 owner-authorized QA request

Date: 2026-11-24.

The targeted Kotlin compile on repaired R2 source passed in GitHub Actions run 45309746036. Continue the approved R2 verification campaign with a **single** isolated disposable Lab build and actual Android 15 first-run navigation test.

Frozen application source: R2 baseline `1577669bc328153a365661a7f3d36fb1558e891c` plus exactly the two diagnosed Kotlin fixes in `V144SettingsHub.kt` and `V407ReminderAlertReadiness.kt`. CI must refuse other application changes or backend changes.

Build only `:app:assembleDebug :app:assembleDebugAndroidTest -PbacklotLab=true` once. Preserve the same debug/test APK pair for two repeatable **disposable emulator** navigations: welcome → local account choice → companion → five-page Creator setup → Creator Home → Personal-default Reminder Hub → Creator/Personal switching with no onboarding loop.

Stop the campaign on failure; keep build/OS logs for exact-stage diagnosis. Do not silently rerun a long Gradle build. This is not device-owner testing or full UX09 certification.

No owner APK, original package, Supabase migration/data mutation, full JVM suite, merge, deletion, paid service, stable promotion, or release is authorized.