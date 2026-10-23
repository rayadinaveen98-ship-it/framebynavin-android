# Backlot V210 P4.4 — Short-write and corrupt archive acceptance

**QA-only branch** stacked on green [PR #96](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/96), unchanged V210 personal preview build 236. This does **not** modify production runtime code, package identity, signing, permissions, UI or backup schema. No stable merge.

## Scenario A: backup destination becomes unwritable

`V210BackupIoFailureTest.interruptedPortableExportDoesNotAlterCreatorState` sets up a valid personal Best Frame on a disposable Android 11 emulator and runs actual `PortableCreatorBackupManager.writeBackup` against an OutputStream that raises `IOException` after **64 written bytes**, modelling a short-write/full destination. The export MUST fail (never report complete). It checks the original Best Frames order/checksum, tasks, ideas, weekly schedule, settings, post-publish and rewards remain byte-identical before/after; no pending restore journal appears.

## Scenario B: imported ZIP truncated during copying

`V210BackupIoFailureTest.truncatedPortableZipFailsBeforeChangingCreatorData` exports a real valid portable ZIP (including verified personal image media), truncates it within the first local ZIP header, and requires both preview and restore to **reject it**. The original creator data, image checksum/order and pending recovery marker remain unchanged.

Both tests restore the disposable emulator's original baseline in `finally`. They do not fill the emulator's actual storage.

## Important boundaries

- Injected I/O failure is **not true ENOSPC**: it does not model insufficient app-private free space, failing atomic writes, Android Storage Access Framework persistable URI loss, intermittent output streams or file-system power loss.
- Truncation after a later ZIP entry, missing/corrupted Best Frames manifests, storage pressure during a **restore**, and repeated low-RAM media are separate risks; other existing suites cover some integrity cases.
- Keep inherited 671 JVM tests, the full Android 11 regression matrix, large Best Frames export/preview/restore, P4.1 tamper fail-closed and P4.2 two-process recovery checks.
- Do not promote stable or call backups universally safe based on this synthetic fault injection.

CI: `.github/workflows/backlot-v210-p4-io-failure.yml`.
