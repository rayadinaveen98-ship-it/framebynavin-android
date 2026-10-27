# Backlot V197 — Stable candidate RC1

Date: 2026-10-21 (IST)
Source: `feature/backlot-v197-6-e2e-reliability-gates` at `46f2c9c6fc1e4e24e15b2cee9b93d149d4dd349d`, plus RC-only packaging/documentation/verification changes.
RC branch: `release/backlot-v197-stable-rc1`.
RC versionCode: **217**. versionName: **2.6.7-v197-stable-rc1**.
App ID: `com.framebynavin.app`.
Packaging: non-debuggable `personalStable` personal sideload (NOT Play production signing).

## Acceptance evidence

- V197.5 build and Android emulator: both succeeded, [GitHub Actions #39555927381](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/39555927381).
- V197.6 build and Android emulator: both succeeded, [GitHub Actions #39555932495](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/39555932495). Runs all inherited UI regression suites and 25 added end-to-end tests for idea conversions, publication recovery, backups, project persistence and reminder independence.
- User reported the V197.6 installed preview was working well on their physical phone. That is general device acceptance, **not** an exhaustive verification of 200% font, offline restores, alarm delivery on all OEMs, or data recovery.
- Verified V195.5 baseline APK SHA-256: `b8fe7af47ab3dca1ad91a2881511559670ec1db7718addec912e0c6464622949` (matches earlier recorded stable hash).
- Verified V197.6 preview APK SHA-256: `95192406cd58467b350fa22bf651b4ca727a6db1f4ba5ee342c9e738adbc99bc`.
- Both APK v2 certificates independently inspected from actual APK signing blocks. Certificate DER SHA-256: `f0ddec789172ec9f83a037a95b14e90293126f61936d8bab056d3a57e0d472a0` for **both**.
- RC1 pipeline enforces this exact signer, package identity, increasing versionCode, JVM/Android tests, lint and non-debuggable personal APK assembly. Rebuilt RC SHA-256 is recorded in CI artifact.

## Branch policy

- `stable/backlot-v195-5` is unchanged and remains the rollback baseline until candidate sign-off.
- This RC source is based on the user-accepted V197.6 feature code. **No new feature or data-schema changes**; only version metadata, signer-verification utility, CI workflow and release documentation.
- Draft RC must pass CI and Android emulator from the **exact RC head** before any promotion.
- All stacked development PRs remain unmerged. Do not force-push main or stable, and do not merge RC into main bypassing review.

## Personal release acceptance

1. Check RC1 CI green (source guards, unit tests, lint, APK assembly, signer/package/version and full emulator).
2. Verify RC APK artifact hash and certificate against the accepted V195.5 baseline.
3. Non-destructive phone update check; never uninstall a populated build to get around a signature mismatch. Verify that saved projects, ideas, reminders, voice notes, publication history, app settings and local backups persist.
4. Verify Quick Actions, Calendar, Weekly Plan, accessibility navigation, and the reminder alarm/notification acknowledgement flow on the device. Android notification permissions and OEM power restrictions may affect delivery.
5. On approval, create a distinct immutable `stable/backlot-v197` branch/tag from the verified candidate. Preserve original stable branch and full provenance. RC is not promoted automatically.

## Important limitation

The certificate is the **public prototype developer signing certificate** for personal sideload compatibility. It is not secure production/Play release signing. The verification script checks certificate *identity*, not signature cryptography; Android verifies APK signatures during installation. A CI emulator is not a substitute for real handset and backup-restore acceptance.
