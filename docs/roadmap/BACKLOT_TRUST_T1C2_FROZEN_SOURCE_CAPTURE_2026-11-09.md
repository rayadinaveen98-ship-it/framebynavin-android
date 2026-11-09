# Backlot Trust T1C2 — frozen source capture implementation
9 November 2026 • isolated code review • source tests queued, NOT run

## What this code ACTUALLY does

\`TrustT1C2OwnerSourceCapture.freezePartialReadOnly()\` obtains the
CreatorDataGate transaction and then the Personal stable-snapshot mutation
gate in that **single lock order**. Both are held while it reads real owner
records and copies actual bytes into temporary private files:

- Creator projects plus legacy Script Studio sidecars via new genuinely
  read-only \`TaskStore.exportJsonForTrustReadOnly\` and
  \`ScriptStudioStore.loadForTrustReadOnly\`. The existing TaskStore.load
  opportunistically seeds workflow timeline; T1C2 explicitly avoids it.
- Idea Vault data plus local Voice Idea originals, with orphan/in-progress/
  remote-only recording checks by \`TrustVoiceIdeaInventory\`.
- Focus 2.0 active session and recorded history.
- Personal v2 reminder/Done envelope, legacy V297 challenges,
  Reminder Groups, Spaces/journal/links, **single-activity 21-day receipts
  preserving exact completion instants**, and all recorded original JPEGs.
- Personal portable reminders explicitly remove local alarm delivery leases,
  device-specific music permissions and custom-music URIs. This matches the
  existing \`PersonalBackupTransfer.exportForCloud\` policy. The original
  reminder count/order and exact completion receipts are verified unchanged.
- Active Best Frames original media; these also supply the Home slideshow.
  Missing, corrupted or symlinked originals fail closed.

\`TrustSourceFreezer\` independently computes a SHA-256 while copying each
part to private temporary storage, flushes it to disk and rehashes the
frozen copy, refusing changed expected hashes, missing files, symlinks,
out-of-root files, duplicate IDs, invalid source count/size, zero-progress
streams and incomplete copies. Frozen source references are usable as inputs
to **T1C1 AES-GCM staging only**. Callers must close them immediately and
remove plaintext after an encrypted verification.

Both locks are held for the data capture, and Creator restore generation
is checked before and after. A second read of key Creator and Personal
payloads detects drift within the scope. This is a stronger local copying
protocol than T1B4's hash-only report.

## Known limitations — hard release blockers

**The result is STILL PARTIAL, not a complete Backlot backup.**

1. Creator config, timeline/pulse/postpublish, weekly slots, Spark/visual and
   notification settings are not yet frozen by this particular coordinator.
   Full CreatorMedia excludes retired/rollback hero frames, unreferenced
   recordings and external URI media. The capture fails on orphan Voice Idea
   bytes rather than deleting or silently skipping them.
2. Not every source in the app is guaranteed to obey CreatorDataGate and
   Personal mutation lock, notably independent SharedPreferences/external
   media producers. The order and scope do not yet prove one GLOBAL atomic
   Backlot revision. This must be audited before T1C is called universal.
3. This copies temporary PLAINTEXT originals into app-private cache. The
   current \`close()\` cleans them on normal return/exception, but abrupt
   process/power death may leave temporary plaintext files until recovery.
   **T1C3 must implement interruption journaling, stale staging cleanup
   on app startup and stricter temp file exposure/recovery policy before
   any owner-facing action is exposed.**
4. T1C1 encryption uses owner ID asserted by the caller, NOT authenticated
   CloudSession binding. Passphrase/key recovery and device switch still
   require verified account-scoped design and security testing.
5. T1C2 does not call a network API, move/merge/restore/delete records,
   schedule alarms, install an APK, merge a branch or declare backup success.
   \`canClaimCompleteBackup\` and \`canUploadAsUniversalBackup\` remain false.
6. Heavy file capture while holding locks needs explicit owner-driven UI,
   progress/cancellation, ANR and size/space feasibility review. It must
   NEVER run as a silent startup/on-every-edit job.
7. Phase 3F V324 integration/owner approval and Trust challenge cloud
   blocker #327 remain separate and unresolved.

## Evidence and next gates

- Authored pure JVM \`TrustFrozenSourceFilesTest\`: freeze immutability after
  live original changes, checksum mismatch, symlink/traversal/external file
  refusal, duplicate IDs and cleanup after failure.
- Authored actual Android 15 \`TrustT1C2OwnerCaptureDeviceTest\`: read genuine
  Creator/Personal app stores on a disposable emulator, copy frozen bytes,
  encrypt via T1C1, verify decrypted source identity and make sure no
  Creator/Personal data changed. Added to
  \`tools/qa/backlot_trust_t1_stage_android15.sh\`.
- **Tests are currently only source**, queued for the single explicit
  \`qa/backlot-trust-t1-stage-*\` milestone workflow per owner request.
- T1C3 must provide bounded, journaled local encrypted backup creation and
  verified readback, no lingering plaintext, durable recovery and exact
  complete data coverage before any owner-facing Back Up.
- T1C4 must implement immutable authenticated cloud revision upload, CAS,
  previous-good retention and verified readback, with no overwrite to the
  legacy Personal or Creator cloud record.
- T1D independent full JVM/Android 11/15, signed in-place APK upgrade,
  real owner device and two-device receipt/photo evidence is essential.
  T2 reviewed restoration, T3/T4 deletion stay separate.

This document records code state, not passed tests or shipping capability.
