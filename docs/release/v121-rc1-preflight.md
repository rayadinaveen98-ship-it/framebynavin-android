# FrameByNavin v121 — RC1 Preflight

Version: `2.0.0-rc1-preflight`  
Version code: `121`

This milestone is release-candidate preparation only. It does not add product features.

## Play submission blocker fixed

As of 31 August 2026, Google Play requires new Android phone/tablet apps and app updates to target Android 16 / API 36 or higher. The v120 baseline still targeted API 35, so it was not eligible for a normal Play submission in September 2026.

v121 moves FrameByNavin to:

- `compileSdk = 36`
- `targetSdk = 36`

The project remains on AGP 8.9.2, which is new enough for API 36, and Gradle 8.11.1 / JDK 17.

## Android 16 compatibility preflight

The RC build gate checks that:

- the main activity explicitly uses `enableEdgeToEdge()`;
- the manifest does not use the removed Android 16 edge-to-edge opt-out;
- application source does not use legacy `onBackPressed` or raw `KEYCODE_BACK` interception that would break predictive-back behavior when targeting API 36;
- the full debug and release lint/build suites pass while targeting API 36.

Physical Android 16 testing is still required before production readiness.

## Production signing path prepared

Production signing material is never committed to GitHub.

The Android build now supports an optional production signing configuration supplied only through Gradle properties or environment variables:

- `FRAMEBYNAVIN_RELEASE_STORE_FILE`
- `FRAMEBYNAVIN_RELEASE_STORE_PASSWORD`
- `FRAMEBYNAVIN_RELEASE_KEY_ALIAS`
- `FRAMEBYNAVIN_RELEASE_KEY_PASSWORD`

When all four are present, the release APK/AAB use that signing configuration. When they are absent, CI continues compiling the release variant unsigned so release-only code cannot rot.

Windows preflight helper:

`scripts/release-signing-preflight.ps1`

The helper prints the release certificate SHA-1/SHA-256 fingerprints via `keytool`, then builds the release APK and AAB. Those fingerprints must be registered in Google/Firebase before production-signed Google sign-in, YouTube OAuth and Play Integrity App Check validation.

## Play bundle gate

v121 adds `bundleRelease` to CI. Every validated preflight build must therefore produce both:

- installable debug-signed QA APK;
- release AAB suitable for final signing/distribution preparation.

The CI verification package records SHA-256 hashes for the QA APK, release APK artifact and release AAB.

## Security/data gates carried forward

v121 keeps all v120 hardening in force:

- production Supabase authorization review and privilege hardening;
- Android Keystore AES/GCM session storage;
- account-generation fencing;
- process-only, account-bound Drive tokens cleared on account transition/sign-out;
- Drive `appDataFolder` scope;
- backup payload SHA-256 validation;
- durable pre-restore recovery journal;
- reminder completion / Finish Project regression safety;
- release Play Integrity App Check provider with debug provider isolated to debug builds;
- no copied Firebase API keys or Supabase secret/service-role keys in application source.

## Still open before RC1 production-ready

The following cannot be truthfully closed by repository CI alone:

1. create/confirm the real upload/production signing path (prefer Play App Signing);
2. register final release SHA-1/SHA-256 fingerprints;
3. test Google sign-in and YouTube authorization from the production-signed build;
4. validate Firebase AI through Play Integrity App Check on that production-signed build, then enable enforcement;
5. exercise reminders/permissions on physical Android 13, 14, 15 and 16 devices where available;
6. run destructive backup/restore tests on a real installation, including process death and corrupted data;
7. exercise real Drive empty/stale/malformed/wrong-account recovery paths.

No new feature work should bypass these gates.
