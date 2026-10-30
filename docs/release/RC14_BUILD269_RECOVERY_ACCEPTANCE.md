# Backlot RC14 — Build269 backup and recovery acceptance

**Scope:** QA-only verification of the frozen RC13 Build269 preview. Absolutely no app-source, UI, APK, version, signing, manifest, permissions or backend modifications.

## Immutable evidence

- Source commit: `7351e4583d08bfa5352c09322a357124fcc801ff`.
- RC13 seven-job success: https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/41031529526
- Reuse `Backlot-RC13-Test-APKs` from that exact successful run; the RC14 workflow does NOT build a new app binary.
- Verified RC13 full owner APK SHA-256: `2e658567fdfc29acab63b1da1d7ac29c804de865899a7c4441c3e231085884fa`.

## RC14 verification

Run **nine existing Android instrumentation tests**, on three isolated, disposable emulators (Android 11/API30, Android 15/API35 and Android 16/API36):

| Suite | Tests | Focus |
| --- | ---: | --- |
| V208PortableBackupIntegrityTest | 3 | Voice archive integrity, rejected orphan/corrupt media |
| V209BackupCoverageTest | 2 | Original Best Frames and external project references |
| V210BackupIoFailureTest | 2 | Interrupted exports and truncated ZIP reject without Creator data loss |
| V226PersonalBackupTransferTest | 2 | Personal recovery, stale action-token prevention, Creator/Personal separation |

These tests use CI-emulator-only gating. No real owner data, cloud credentials or Play credentials are used.
An additional source-invariance job requires only this documentation file and one workflow to differ from frozen RC13.

## Owner-phone acceptance (manual; not performed by CI)

1. Do not uninstall or clear the installed Backlot. Record its app version and signature; if signatures differ, stop rather than uninstall.
2. Export Creator backup to a separate protected location. Require the app's successful readback/checksum confirmation. Project attachments remain links; export source media separately. Creator exports are unencrypted.
3. Separately export Personal Reminders; verify the saved file using the app and protect its unencrypted reminder titles and notes.
4. On a disposable secondary profile/device, review both restore previews and test the two imports. Restores replace data; never rehearse a destructive restore on the owner's phone.
5. On the owner's phone, confirm cold-start project loading, editor buttons with the keyboard open, Home video ordering, slideshow tap controls, companion, voice and alarm screens, Creator and Personal reminders, device permissions and rearming.
6. Record only nonsensitive outcome evidence; do not upload personal backup archives, tokens or private recordings to GitHub.

## Explicit non-release blockers

- #174 owner acceptance and consolidated release approval.
- #180 safe migration from public prototype signing key to private Play identity.
- #185 protect `main` and audit the large stacked-PR integration.
- #187 Play full-screen/foreground-service eligibility, Data Safety, public app privacy policy, secure in-app and web account deletion.

RC14 is a draft verification step only. Do not merge, uninstall, wipe data or claim Play-ready status.
