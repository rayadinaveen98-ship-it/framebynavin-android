# Backlot V210 P4.5 — Integrated reliability acceptance

**Preview only:** versionCode 236 (`2.8.6-v210-compact-restore-preview`), package ID and signer unchanged. Source stack: draft PR #94 (memory-safe image restoration), PR #95 (tamper and journal recovery), PR #96 (separate-process checkpoint recovery), PR #97 (actual in-flight force-stop), and PR #98 (export write error and truncated ZIP). **No new production runtime feature or stable release.**

## Why the integrated branch exists

Each P4.3 and P4.4 acceptance passed separately on Android 11, but their effects on the same emulator were not tested together. This branch combines P4.4's `V210BackupIoFailureTest` with P4.3's green live restore-interruption implementation (commit `ed24eacd4359913c6023d9a4720790001d0efaf9`), preserving the complete inherited suite.

**Combined CI must prove all of these in the same fresh emulator:**
1. 671 JVM unit tests, app ID and APK signing/hash, Android lint and full historical Android regression matrix.
2. Large Best Frames archive: canonical JSON over 32 MiB, export → preview → restore → original-image SHA-256 → original-reference recovery.
3. Tampered pending recovery journal: fail-closed, preserve marker, then successful recovery and idempotence.
4. Separate Android process recovery: prepare fixture in one instrumentation, force-stop target app, recover in another process.
5. Live restore interruption: execute *real* `restorePortableCompact`; pause with the new images published and recovery journal active, force-stop app mid-transaction; recover old images and creator metadata on a different process.
6. Deliberate short-write during actual portable backup export: fail without mutating creator data. Corrupted/truncated ZIP rejected by preview and restore without mutating creator data.

**Release stop-ship:** Tests (5) and (6) use disposable Android 11 emulator and a synthetic output-stream short write. They do not establish safety under physical power loss, true app-private disk exhaustion, older raw-JSON/cloud image restore, or near 100 MiB on low-RAM phones. Keep draft, leave `stable/backlot-v195-5` untouched, and never market full 100 MiB support before real-device evidence.

**Workflow:** `.github/workflows/backlot-v210-p4-integrated-acceptance.yml`. Require **both PR and push** exact-head runs green; do not interpret partial build green as completed acceptance.
