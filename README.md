# Backlot Android

Backlot is an Android-first, offline-first Creator OS built with Kotlin and Jetpack Compose. It is designed to help creators capture ideas, turn them into projects, execute through reminders/workflows, and connect published work back to channel insights.

## Current candidate

The current verified installable candidate is **Backlot V149**:

- `versionCode`: `149`
- `versionName`: `2.0.0-rc26-supabase-voice-reminder-fix`
- active branch: `feature/v149-supabase-voice-reminder-fix`
- RC26 source head used for the verified APK: `9bb512db3f87669d7da3bef1501a154df177735c`
- successful Android APK workflow run: `36710758713`
- generated artifact: `Backlot-v149-rc26-Supabase-Voice-Reminder-Fix`

RC26 is a reliability candidate rather than a new feature release. Its main changes are:

- prefer the connected/cached Google account first name for spoken reminders, with creator-profile and generic fallbacks;
- accept Google `given_name` metadata as an additional identity source;
- prevent duplicate delivery of the same reminder occurrence from restarting speech;
- freeze the complete reminder sentence before speaking and wait for TTS completion before any configured repeat;
- keep manual Replay as an explicit forced restart path.

The connected Supabase project used by Backlot is currently active and healthy. Voice reminder identity is intentionally resilient to temporary backend unavailability by using cached authenticated identity where available.

## Build and verification

GitHub Actions is the authoritative Android build environment. The main workflow is `.github/workflows/android-apk.yml`.

For the current V149 branch it runs:

1. unit tests;
2. Android lint;
3. instrumentation-test APK compilation;
4. debug APK assembly;
5. artifact upload.

The RC26 APK workflow completed successfully. The obsolete one-off `Insights V2 Compile Fix` workflow from the earlier V145 work has been removed from the current branch so it no longer creates misleading failed checks.

## Current release-readiness status

RC26 has passed the repository Android build pipeline, but it should still be treated as a release candidate until real-device verification is completed for the updated reminder paths and broader beta-readiness checks are finished.

Priority verification still includes:

- real-device confirmation of Google first-name reminder greeting;
- long reminder speech without mid-sentence restart or overlap;
- offline/online sync and account-switch stress testing;
- backup/restore integrity checks;
- YouTube Insights metric comparison against YouTube Studio;
- onboarding usability with creators who have not previously used Backlot.

Preserve an independent backup before using development candidates with irreplaceable creator data.
