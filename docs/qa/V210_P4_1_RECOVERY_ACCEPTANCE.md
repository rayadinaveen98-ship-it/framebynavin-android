# Backlot V210 P4.1 — Interrupted compact restore acceptance

**Scope:** test-only change stacked on draft PR #94. VersionCode **236** remains unchanged; **no runtime source code, application permissions, signing or user data changes**.

## Recovery scenario

The V210 compact restore path writes a SHA-256-bound pre-restore metadata journal and an `originalFrames` checkpoint listing original app-owned image filenames and SHA-256 hashes. The original image files are retained while new files are installed. A crash after publishing the new order but before clearing the marker should recover the original order and original creator metadata on the next application startup.

## New disposable Android 11 test

`V210RecoveryCheckpointTest.incompleteCompactRestoreRecoversPreviousOriginalsAndPreservesJournalOnCorruption`

1. Snapshot the current disposable emulator's creator data, and ensure there is no pending journal.
2. Install a tiny valid Best Frame and capture its original file order/checksum.
3. Produce a valid normal metadata-only (empty `personalFrames`) pre-restore journal, and stage the matching SHA-256-bound original file checkpoint.
4. Switch the visible image order to empty, simulating an interrupted compact restore after its image-order commit.
5. Tamper with the checkpoint SHA-256: verify recovery **fails closed**, doesn't modify the current order and does not discard the pending journal.
6. Repair the on-disk fixture marker to the correctly signed contents and call the same `CreatorBackupManager.recoverPendingRestore()` path used at app startup.
7. Verify the original image order and checksum are restored, the pending marker is cleared, and a second recovery invocation does nothing.
8. Remove only the test-owned fixture and restore the disposable emulator baseline in a `finally` block.

**Limit:** this is an on-disk **simulated interruption**, not a physical process kill, reboot or low-storage filesystem failure. Those cases still require physical-device / deterministic fault-injection testing.

## Gates

- Keep inherited 671 JVM unit tests, the full V210 emulator regression suite, large-image 32 MiB export/preview/restore regression, Android signer/package/fingerprint checks.
- Require the new checkpoint acceptance `OK (1 test)` before treating the *test-only* PR as green.
- Do not merge/promote to stable based solely on this result; low-storage errors, 65–100 MiB real-image collections, real process death and older Base64-heavy raw-JSON/cloud restore remain open.

CI: `.github/workflows/backlot-v210-p4-recovery-acceptance.yml`
