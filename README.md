# Backlot Android

Backlot is an Android-first, offline-first Creator OS built with Kotlin and Jetpack Compose. It is designed to help creators capture ideas, turn them into projects, execute through reminders/workflows, and connect published work back to channel insights.

## Current stable version

The current verified stable checkpoint is **Backlot V149 / RC26 – Supabase Voice Reminder Fix**.

- `versionCode`: `149`
- binary `versionName`: `2.0.0-rc26-supabase-voice-reminder-fix`
- stable branch: `stable/backlot-v149`
- manually verified application source head: `ac4b7b3ff961d7af39bcdfa3b88c3ffe17a2bed1`
- successful Android APK workflow run for that source: `36799359846`
- workflow result: `success`

The binary version name is intentionally preserved so the stable checkpoint identifies the exact application build that completed real-device verification. The stable designation is carried by this Git checkpoint rather than by changing the tested application payload after verification.

V149/RC26 stabilizes the Supabase-backed identity and voice-reminder path. Its main changes include:

- prefer the connected/cached Google account first name for spoken reminders, with creator-profile and generic fallbacks;
- accept Google `given_name` metadata as an additional identity source;
- prevent duplicate delivery of the same reminder occurrence from restarting speech;
- freeze the complete reminder sentence before speaking and wait for TTS completion before any configured repeat;
- keep manual Replay as an explicit forced restart path;
- harden reminder scheduling, secure backup/restore, settings and creator-state reliability tests added during the V149 hardening pass.

The connected Supabase project used by Backlot is active and the reminder identity path remains resilient to temporary backend unavailability by using cached authenticated identity where available.

## Build and verification

GitHub Actions is the authoritative Android build environment. The main workflow is `.github/workflows/android-apk.yml`.

For V149 it runs:

1. unit tests;
2. Android lint;
3. instrumentation-test APK compilation;
4. debug APK assembly;
5. artifact upload.

The manually verified application source head `ac4b7b3ff961d7af39bcdfa3b88c3ffe17a2bed1` completed workflow run `36799359846` successfully.

## Release status

**Stable checkpoint approved on October 1, 2026 after successful real-device/manual verification.**

The previously pending Supabase + Google first-name reminder path and uninterrupted voice-reminder playback checks are complete and working as expected on the verified build.

The following remain ongoing product/beta validation areas rather than blockers for the V149 stable checkpoint:

- broader offline/online sync and account-switch stress coverage;
- backup/restore coverage across more device/account combinations;
- YouTube Insights metric comparison against YouTube Studio over additional channels/data sets;
- onboarding usability testing with creators who have not previously used Backlot.

Preserve an independent backup before using development builds with irreplaceable creator data. New feature work should branch from `stable/backlot-v149` (or the corresponding updated `main` checkpoint once merged) rather than modifying this stable branch directly.