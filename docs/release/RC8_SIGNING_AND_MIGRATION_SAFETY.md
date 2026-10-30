# Backlot RC8 — signing transition and data-migration safety

**Release hardening only, 29 October 2026.** The latest approved engineering preview is unchanged **Build267** (versionCode 267), APK hash `02827066650f7f725ef712c7a2119f20746fec5198fd1b7cfdc0be709e8d0d7d`, built from frozen RC6 application source `5b0682140ae19ec698c0c84aa2be8a9939844500`. [RC6 all five jobs passed](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/40922583197) on Android 11/15; [RC7 all five passed](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/40926157504) including Android 16, Build266→267 same-certificate upgrade, and unsigned release AAB structure.

**RC8 changes only docs and a standalone CI workflow.** No app source, package ID, version, manifest, database, backup format, signing identity, phone data, Play listing or `main` branch is changed.

## Security and migration boundary (must not silently cross)

- `app/build.gradle.kts`: `debug` and `personalStable` use `dev-signing/framebynavin-public-debug.jks`; its alias/password and signing materials are in source control. It exists **only** for prototypes. Anyone possessing the publicly distributed keystore may be able to forge a preview-signed update, so it is **not** a production trust root. `release` can accept **independently provisioned private credentials** through environment/Gradle properties; no production credential is configured in CI.
- The shipping preview package ID is `com.framebynavin.app`. An Android package **normally requires signing-certificate continuity** to update over an installed copy. A new Play app-signing identity cannot be assumed to update an APK installed using the prototype signer. A changed upload key is not the same thing as a changed **Play app-signing key**.
- AndroidManifest has `android:allowBackup="false"`. Android system backup is not a data-recovery substitute for the app's two separate exports. **Never uninstall or clear storage without explicit owner acceptance and verified off-device recovery.**
- This is not a reason to reuse a public key for Play, or to make silent package-ID changes. The owner must choose the migration/distribution strategy once its consequences are clear.

## What RC8 tests safely

On an **empty disposable Android 16 emulator**:

1. Install the *exact Build267 debug APK* from the five-job successful RC6 run, whose signer is the public prototype test signer.
2. Write a **synthetic sentinel**, not any user's record, under that installed debug app's private files.
3. Generate a **throwaway one-day RSA test key locally in the short-lived CI worker**, separate from the public prototype certificate and any future real Play key. Re-sign a transient copy of the *same Build267 debug APK*. Verify the two certificate SHA-256 fingerprints differ.
4. Attempt to install it **over** the original package using `adb install -r`. Expect Android to reject it with an update/signature incompatibility error; if it installs, **fail the gate**. Confirm the old app and sentinel still exist after rejection.
5. Reinstall the original same-signer Build267 debug APK in place; verify the synthetic sentinel is preserved. Never run `adb uninstall` or `pm clear`.
6. Upload only audit JSON + sanitized installer logs/certificate fingerprints. **Do not upload the throwaway keystore or re-signed APK.**

This negative test shows why changing a signing identity cannot be treated as a routine upgrade. It does **not** prove any full backup/restore pathway, production Play signing, or Play policy eligibility.

## Verified backup implementation and limitations

### Creator portable backup

- `BackupActivity.kt` exports through `PortableCreatorBackupManager.writeBackup`, then **re-opens and validates** the saved file before reporting success. Archive validation uses checksums, payload lengths, signed-off envelope validation, and explicit media manifests.
- `PortableCreatorBackupManager` bundles app-owned **Voice Idea recordings** and verifies these references; `PortableBestFrameMedia` restores original **Best Frames** in compatible archive exports. It rejects some incomplete/corrupted media backups rather than silently reporting success.
- **External project source-media attachment URIs/references are not equivalent to the original source files**; transfer source videos, audio, photos and project attachments separately where applicable. Verify a full project on a second disposable profile/device.

### Personal Reminder backup

- `PersonalBackupActivity.kt` exports an entirely **separate JSON file**, re-opens the exported URI, verifies the bytes and warns the user that the file **is not encrypted** and contains reminder names and notes. Never upload an unencrypted Personal file into public issue logs, this repository, or a shared test report.
- `PersonalBackupTransfer` previews the complete JSON before any restore; performs a protected local recovery-copy handoff and re-arms device-local scheduling where possible. Restore operation is potentially **replacement**, not a harmless merge: preview counts and test on a disposable profile/device first.
- Creator/Drive sync does not automatically include private Personal Reminders; Android system Auto Backup does not preserve these data.

## Candidate release strategies — owner decision required

| Option | Upside | Tradeoff / requirement |
| --- | --- | --- |
| **A. Different Play package ID**, new private Play signing key | Existing preview can stay installed for side-by-side import and verification before retiring it | New Play listing identity; OAuth/Firebase/Deep Link/app links, widgets and package-specific config must be reviewed; no automatic private app-to-app data sharing |
| **B. Same package ID**, new private Play signing key | Keeps the intended `com.framebynavin.app` Play identity | Existing public-prototype installs cannot normally upgrade in place; user-approved, independently verified Creator and Personal exports, separate source media, and a careful restore after replacement are necessary |
| **C. Delay Play rollout**, keep existing preview branch | No immediate signing migration for current users | Public keystore remains unsuitable for production, Play shipping blocked |

**No strategy is selected in RC8.** Under A or B, prototype users must validate both backup exports **before** any installation change; restore tests require a disposable second profile/device. Selecting a production key, Play listing ID, or deleting an installed app requires explicit owner approval.

## Acceptance / hard gates

- [x] RC6 Build267 five-job matrix green Android11/15, in-place 266→267 green
- [x] RC7 Build267 five-job matrix green Android16, backup/reminder/visual UI, in-place 266→267 green, unsigned AAB structure green
- [ ] RC8 different-signer negative test green on disposable Android16
- [ ] Owner confirms **their actual phone** Build267 icon, slideshow and Voice/Alarm waves and reminder behavior
- [ ] Two export files separately created, re-opened, and verified off device (Personal JSON stays private)
- [ ] Source media / attachment dependencies inventoried and moved separately, not merely URIs
- [ ] Second clean disposable device/profile restore of Creator and Personal succeeds without silently damaging data; schedule rearmed and media verified
- [ ] Owner explicitly selects A, B or C and authorizes the chosen migration test
- [ ] Production private upload key provisioned securely (not this CI, not public prototype key)
- [ ] Play exact/full-screen alarm/foreground-service policy, data safety, listing, privacy and staged-release requirements reviewed independently
- [ ] Complete stacked PR integration audit and explicit owner approval to merge into protected stable/main

Track: [Release gate #174](https://github.com/rayadinaveen98-ship-it/framebynavin-android/issues/174), [Signing-migration blocker #180](https://github.com/rayadinaveen98-ship-it/framebynavin-android/issues/180). RC8 is an **engineering proof**, not approval to merge or publish.
