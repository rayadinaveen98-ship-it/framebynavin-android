# V210 P4.2 — Process boundary recovery acceptance (test only)

**Source base:** green V210 P4.1 commit `a23a6c6588ffb7687dc2e0d97e4abf47edd24409`, build 236. No production source, version, signer, backup schema, runtime permissions or UI changes.

## Why

P4.1 proved the pending compact-restore journal can recover in the **same process**. P4.2 adds a separate-process test by force-stopping the disposable Android emulator target package between writing a crash-consistent journal and invoking startup recovery.

## Procedure

The new `V210ProcessBoundaryRecoveryTest` has two named test methods intentionally run via **two separate** `adb shell am instrument` commands, in explicit order.

1. **preparePendingRecoveryAcrossProcessBoundary** creates an emulator baseline, installs a valid tiny Best Frame, saves its SHA-256-bound original file reference and writes a compact metadata recovery copy plus the actual `pending.json` marker. It switches visible image order to empty, simulating the point after the restored order was published but before the pending marker was cleared. It deliberately does not clean up, so the on-disk journal survives process exit.
2. CI issues `adb shell am force-stop com.framebynavin.app`. This kills any currently running target process; the next instrumentation starts a fresh target app process.
3. **recoverPendingAfterForceStopInFreshProcess** reads the persisted fixture, calls `CreatorBackupManager.recoverPendingRestore()` if pending remains, and checks restored image order and checksum, cleared marker and idempotent second recovery. If app startup already recovered before instrumentation begins, it still requires the correct recovered state.
4. The second method resets the emulator baseline and removes only the test's own staging files in a `finally` block. No non-disposable device may run this suite.

The workflow **also** runs every inherited regression: 671 JVM tests, Android 11 UI/backup/reminder suites, >32 MiB image export→preview→restore, and the P4.1 tamper-checkpoint test.

## What this cannot prove

This test force-stops the app **after** the prepare method has returned; it does not kill the app in the middle of an actual restore transaction, power off a real phone, simulate filesystem power loss, enforce low free storage or validate a near-100 MiB real-photo library. These are remaining acceptance requirements before stable promotion.

**Release rule:** No stable merge or user-data migration based solely on this check; build 236 remains personal preview. See [draft PR #94](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/94) and [issue #93](https://github.com/rayadinaveen98-ship-it/framebynavin-android/issues/93).
