# Backlot Trust T1C3B — Main-launch backup staging recovery

9 November 2026 • source review • not compiled or Android-verified • no owner APK

## What is now implemented in source

The previously open T1C3A crash-privacy gap is narrowed by a dedicated,
read-only-to-owner-data **startup recovery for temporary backup artifacts**.

- `TrustT1C3StartupRecovery.recoverOnMainActivityStartup` runs inside
  MainActivity's existing background startup recovery flow **before** normal
  Creator restore and reminder reconciliation. It does not open owner
  databases, restore a backup, delete personal records, change alarm schedules
  or upload anything. If it fails, only a generic warning is logged; the
  essential reminder recovery still proceeds.
- `TrustT1C3OperationGate` holds a process-wide coroutine mutex and an
  OS file lock under the private T1C3 preview directory. Both preview creation/
  verification and startup cleanup participate. This prevents a simultaneous
  preview in another app process being mistaken for abandoned work. T1C2
  standalone/freezer callers do NOT yet use this OS lock; those remain an
  internal, non-owner-facing test and integration limitation.
- `TrustT1C3RecoveryJournal` persists a compact atomic marker containing only
  random attempt UUID and phase CAPTURING/ENCRYPTING/VERIFYING/PROMOTED.
  Owner identity, passphrase, content, paths and filenames are never recorded.
  The state is strictly validated and bounded on recovery.
- When starting a new partial review, T1C3A now records the journal phases
  and uses the same startup cleanup policy. On a successful encrypted
  verification and atomic promotion it clears the journal.
- Recovery preflights **all candidates before deleting any**. It recognizes
  only files directly in `noBackupFilesDir/trust-t1c3-local-review-v1/pending`
  matching the exact AtomicFile attempt pattern, plus two app-cache temporary
  directory prefixes: `trust-t1c2-owner-bytes-...` and
  `backlot-trust-check-...`.
- Recursive preflight uses Java NIO without following symlinks, rejects
  unexpected file types and oversized trees, and won't remove an active
  registered staging directory. It then removes only these abandoned files.
  Unknown pending objects, malicious symlinks and malformed journal contents
  result in a safe failure and leave suspicious files for investigation.
- Previously committed `verified/verified-*.enc` objects are NEVER touched by
  this sweep, even if a process died after promotion but before writing the
  final journal state. Ordinary cache contents and Personal Space original
  JPEGs remain outside the deletion scope.
- Recovery is idempotent: a second clean startup has nothing to remove.

## Limitations that remain before owner-facing Back Up

**THIS IS NOT a complete or release-verified backup system.**

1. The hook runs on **main Activity launch**, not on every possible
   background-service-only process start or immediately after power loss.
   Private plaintext may remain until Backlot's main UI next opens. On flash
   storage, unlinking a file also cannot prove physical secure erasure.
2. Current source stages remain internal only. External call sites that use
   the T1C2 freezer directly do not hold the new OS file lock; a complete
   production transaction contract must cover every source producer.
3. T1C2 remains a **partial** capture: some independent Creator settings,
   full Spark/notification state, external media and retired Hero frames are
   not frozen. The existing Creator and Personal gates are not a verified
   global Backlot transaction across all producers.
4. No authenticated cloud account binding, secure recovery keys,
   durable versioned upload, compare-and-swap, old-good remote retention or
   remote readback exists. The encrypted local preview is intentionally not
   an uploadable universal backup.
5. No feature UI is connected. The receipt still has three hard-false
   capabilities: universal backup, cloud upload, owner restore. No existing
   owner data or release artifacts are modified.
6. No automated test is claimed passed. At the T1 milestone we must test:
   process kills at every journal phase, disk-full and permission failures,
   startup recovery under lock contention, true OS power interruption,
   Android 11/15, owner-signed upgrade, account switching, two-device receipt
   and photo integrity, and preservation of existing reminders.
7. Before enabling a real backup, use a production staging design minimizing
   plaintext-at-rest, plus proper startup/background-service recovery and
   honest user-visible failure states. Don't promise crash *privacy* or
   complete recovery solely on this code.

## Tests written but deferred

`TrustT1C3StartupRecoveryDeviceTest` contains four disposable-emulator
cases: cleanup while preserving verified and unrelated files; active capture
protection; reject untracked/symlink files; and fail closed on corrupt journal.
Registered in `tools/qa/backlot_trust_t1_stage_android15.sh`, which runs
only from explicit `qa/backlot-trust-t1-stage-*` milestone branches.
Existing T1A/T1B/T1C1/T1C2/T1C3A regression tests remain queued.

## Next locked execution

T1C3B release gate: integrate all owner source producers into a consistent
snapshot, minimize/automatically clean temporary plaintext on every process
entry, and verify crash behavior. Then T1C4: real authenticated immutable
cloud revisions/readback/previous-good retention. Then one complete T1D
milestone test and signed owner review, followed separately by T2 owner-
approved and journaled restore. Track under #344; #327 stays open.
