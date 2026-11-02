# Backlot Build 283 — Release Candidate 1 Stability Gates

**Status:** Engineering preview; **NOT Play Store ready**. Last audited 2026-11-02.

## Scope and frozen baseline

- Freeze Build 283 runtime features. Only fixes to reliability, security, data integrity, performance, or policy compliance are admissible until release.
- Android package: `com.framebynavin.app`; `versionCode = 283`; preview `versionName = 2.17.9-rc10-creator-auto-retry-owner-test`; `targetSdk = 36`.
- Feature source baseline: `c2bc5f23cd0befcb61863b3edbb3b4230bb740fe`, with CI-only changes on draft PR #243. Verify any further source change against this baseline; re-run its affected gates.
- The `personalStable` preview APK uses the **public development certificate**. This key is **not** the Play upload certificate. Never represent an owner APK as a production binary.
- No destructive data reset, reinstall, or migration is authorized by this document.

## Evidence and open items

| Gate | Evidence as of audit | State |
|---|---|---|
| Debug Kotlin, unit tests, debug APK and instrumentation APK | Workflow 41419431433: success | PASS |
| Android 11 targeted emulator tests | Workflow 41419431433: Creator retry 3, Personal retry 2, unified Personal cloud 3, Personal snooze 2 | PASS (10 tests) |
| Standalone `lintDebug` | Workflow 41422459238: `BUILD SUCCESSFUL`, report artifact ID 13133745643 | PASS with warnings |
| Lint report triage | 307 Warnings, 1 Information, 0 Errors. Large clusters: UseKtx 175, HardcodedText 30, GradleDependency 18, SmallSp 18. Review relevant correctness warnings; not a zero-warning result. | OPEN |
| Non-debuggable preview APK | Workflow 41463961020 completed successfully with independent build/test/emulator jobs; owner artifact ID 13154465574; APK SHA-256 `1e0786828718db560b5a75c2f7ef8a2670dde00972ddd921325830cd44005cfe` and APK Signature Scheme v2 verified against the public preview signer. | PASS (preview only) |
| Production signed AAB | Private Play upload key is not configured; public preview signer is prohibited in production | BLOCKED |
| Owner device reliability, fresh-device restore, signing migration | Owner acceptance and migration not proven | BLOCKED |
| Data safety, in-app and web account deletion, permissions policy review | In-app account deletion explicitly unavailable in preview | BLOCKED |

**CI lint treatment:** Full `lintDebug` passed on the same application source. The split CI workflow now excludes **only** `lintVitalPersonalStable` while packaging a non-production owner-test APK to avoid repeated 47-minute timeouts. Do not extend this exclusion to `release` or `bundleRelease`; Play upload remains gated on full release-specific validation.

## Preview APK signer compatibility evidence

Local verified APKs: `Backlot-Build282-PersonalAutoRetry-Owner-Test.apk` and `Backlot-Build283-Developer-Test.apk`. The APK Signature Scheme v2 signing block in both was parsed and the first embedded X.509 signing certificate hashed with SHA-256:

```
SHA-256: F0:DD:EC:78:91:72:EC:9F:83:A0:37:A9:5B:14:E9:02:93:12:6F:61:93:6D:8B:AB:05:6D:3A:57:E0:D4:72:A0
Subject: CN=FrameByNavin Dev, O=FrameByNavin, C=IN
```

**Result:** Build 282 owner-preview and Build 283 developer-preview identities match. This only proves signing compatibility for these two checked artifacts. It does **not** verify safe migration of task, Personal reminder, attachments, or cloud data, and it does **not** approve the public preview signing key for Google Play. The Build 283 **non-debuggable owner APK** must be independently checked on artifact completion before being distributed.

## Phase 1: Release candidate and CI — acceptance criteria

1. Exact frozen-source build produces a **non-debuggable, prototype-signed owner-test APK**. Check checksum, APK v2+ signature validity, package ID, versionCode, and the public signer fingerprint against prior owner previews.
2. Unit tests and Android 11 emulator tests pass on the **same source revision** as owner artifact.
3. `lintDebug` passes; review and classify actionable correctness/security/performance warnings. `bundleRelease` must later pass release Lint without excluded release tasks.
4. Record SHA-256 and provenance for every distributed artifact. No sandbox/download link from a filename guess.
5. Keep draft PR and preview variants distinct from stable `main`; no automatic Play publication.

## Actual Build282 → Build283 upgrade test

- Draft [PR #246](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/246) adds an upgrade test harness compiled against the **Build282 API**.
- [Run 41479960679](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/41479960679) started on 2026-11-02: installs the exact signed Build282 owner APK, seeds Creator and Personal data, then installs the exact verified Build283 owner APK **without deleting data** and asserts both persisted records and settings.
- **Status: PENDING** until the actual upgrade test passes. Preview signer compatibility only establishes that an in-place upgrade should be permitted; it is not proof of lossless migration.

## Phase 2: Reminder reliability — owner-device matrix

Test on physical Android devices and API 30/33/34/35/36 emulator scenarios where feasible:
- Creator Voice/Alarm, Personal Voice/Alarm: foreground, background, screen locked, unlocked, Doze/battery restrictions; default-denied and granted permissions.
- Configured 1/2/3 voice repeats, gap, mixed/default/custom music levels, end of playback, Done/Snooze, no replay after Done.
- Bounded opt-in re-alerts (Creator + Personal) with 5/10/15/30-minute intervals, 1–3 additional attempts, OFF by default, no old occurrence replay, no duplicate alarm after recovery.
- Reboot, timezone/time changes, app update, revoked exact-alarm/full-screen rights and notification fallback. Force-stop limitations must be explained, not misrepresented as guaranteed delivery.
- Repeat on a Samsung device with battery restrictions. Collect actual observed result and log for each case, never infer it passed from unit/emulator tests.

## Phase 3: Backups, cloud sync and safe upgrades

- Create Creator projects, stages, reminders, ideas, progress and Personal reminders with completion histories on Device A. Sync once, restore on disposable Device B, compare content and times field-by-field. Verify clear conflict choices, and repeat after account switching.
- Document that original Best Frames, custom music, external attachments and device-only reminder tokens do not all sync; do not claim full byte-for-byte media restore without tests.
- Verify same-preview-signer **in-place** upgrade keeps both domains; never uninstall the installed preview as a migration step.
- Play release's different certificate requires explicit owner-approved migration plan (Issue #180); no assumption of same-package in-place installation across different signing keys.

## Phase 4: Policy, privacy and security

- Implement working **in-app and web account deletion** that removes linked account records and applicable cloud backups, with accurate retention exceptions and identity/session checks. Local reset, sign out, or deleting only one backup is not account deletion.
- Audit permissions: `SCHEDULE_EXACT_ALARM`, `USE_FULL_SCREEN_INTENT`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, audio and calendar access; verify eligibility and user-visible fallbacks before release.
- Review SDKs and endpoints (Google Sign-In, Firebase, AI, YouTube), data collection, encrypted transport, data deletion, App Check/abuse protection, and security exceptions. Publish true Data safety declarations and a reachable privacy policy.
- Complete localization, accessibility, smallest display, battery, crash/ANR, and 16 KB page-size testing.

## Phase 5 and 6: Private release AAB and Google Play

- Provision private production signing credentials **outside git**; verify certificate is different from the repository's public test certificate. Choose and test data-safe signer migration **before** changing users' package path.
- Build and inspect a release AAB, with no development-only debugging/test credentials, and perform final app-bundle release Lint, native-library compatibility, staged-install and Play pre-launch checks.
- Prepare store description, screenshots, contact, privacy-policy URL, Data safety, App access instructions and accurate permissions declarations.
- Internal test, then closed-testing/production-access gates when applicable to the owner's Play developer account. Release via staged rollout only after measured crash/ANR and critical reminder data integrity gates.

## Release stop-ship rule

Any reproducible lost Personal/Creator reminder, silent cloud overwrite, duplicate post-acknowledgement alert, incorrect restore, privacy/deletion failure, production signer leak, or blocking Play policy finding is a **P0/P1** issue and stops release. Physical-device QA and Play Console eligibility are not certified by this document.
