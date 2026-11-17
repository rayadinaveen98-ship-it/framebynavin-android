# UX09-02A-F — lossless Project/Idea coverage and Android migration gate

**Date:** 2026-11-17  
**Status:** Audited source inventory + read-only test guard **STAGED**. No real sync activated.  
**Parent:** UX09-02A journal PR #437 at verified commit `688c734169a7e1ceea98bd1ef7d4199d7ba95571`; focused Android/JVM CI run #44133359413 **SUCCESS**.  
**Target:** original Backlot V337 remains protected; the previously certified `backlot_ux09_apply_v1`/`pull_v1` contract remains installed only on Backlot Lab.

## Why this stage exists
A passing journal test is not enough to synchronize existing Creator work. The current DataStore export serializes **47** top-level `CreatorTask` properties, **26** `CreatorIdea` properties, and **17** `CreatorContentWorkspace` properties (with additional nested types, stage work, scripts, deliverables and edit histories). Raw full-model upload would leak device-local media paths and can exceed the backend's 16 KiB JSON cap. Silently omitting workspace/voice fields is unacceptable.

We must make omissions visible and prevent subsequent app changes from bypassing this review.

## Executable evidence
- `docs/engineering/UX09_02A_FIELD_COVERAGE_MANIFEST_20261117.json` explicitly partitions **every current top-level persisted model property** into:
  - `portable_candidate`: potentially synced only after domain codec and round-trip tests;
  - `device_local`: stays on the device, never uploaded as-is;
  - `block_until_design`: makes enabling full sync illegal until resolved.
- `tools/qa/backlot_ux09_02a_field_coverage.py` verifies (a) current Kotlin model properties, (b) exact TaskStore/IdeaVaultStore JSON export property sets and (c) classification completeness and uniqueness, and (d) unchanged disabled-feature, payload cap and security sentinels. New/removed/renamed properties cause CI to fail closed. The script's self-tests deliberately introduce an unclassified model field, a renamed JSON key and double classification, expecting all three to be rejected.
- `.github/workflows/backlot-ux09-02a-field-coverage.yml` runs the Python stdlib validator only; no Android build, dependency changes, Supabase credentials, network or hosted data.
- The Python checker covers only these **top-level models + their export keys**, plus workspace top-level fields. It does **not** prove nested codecs preserve all child fields. A future recursive nested model manifest and serialization-roundtrip verification remain required.

## Preliminary classification — NOT final sync activation
| Domain | Key safeguard | Unresolved impact |
|---|---|---|
| Creator Project | Device-local: `alarmSoundUri`, `snoozeCount`, `workingUntilMillis` | Cloud may carry user reminder *intent*, but not device lease, runtime snoozes or local ringtone URI |
| Creator Project workspace | `workspace` blocked; nested `script`, `assets`, `deliverables`, `scriptStudio`, `stageWork`, `outputScripts`, `editVersions`, `stageTransitions` require a lossless plan | Scripts and editing/version history may exceed 16 KiB; assets may contain local file/content URIs; do not truncate or leak |
| Creator Idea | Device-local: `audioLocalPath`, `audioSyncState`, `transcriptionError` | Voice file bytes remain local unless a separately approved private media transfer is implemented |
| Creator Idea | `audioRemoteUrl` and `projectTaskId` blocked | Remote URI trust model and Project ID remapping across existing legacy records are unresolved |
| Creator Idea | `trashedAtMillis` retains reversible trash status | **Never map trash directly to server permanent tombstone**. Restoration must remain possible |
| Both | Existing model IDs are strings, new server expects canonical UUID | Verify migrations, imports, generated IDs, linked Project IDs, collision handling before upload |

All other fields are `portable_candidate`, not cleared for actual transmission. The manifest is a strict *coverage fence* and cannot substitute for semantic domain review or field roundtrip tests.

## Decision record — next architecture gate (proposed, not approved)
1. **Local-first preserves familiar UI:** Do not replace Creator editor UX or existing DataStore stores yet. Continue the isolated write-ahead journal **only as a prototype**, with full before/after encrypted local snapshots and a deterministic recovery procedure before any live call site is integrated. A cross-store save must never be acknowledged as cloud-enqueued until recovery is provably durable.
2. **Durable offline ordering:** Design a per-account, per-entity local edit chain. Prepare a new remote operation with immutable `expectedRevision` only when its predecessor has acknowledged success, or safely coalesce only operations proven never to have been sent. After ambiguous network success, retry the exact same UUID/body. On real conflict, preserve local edits and surface a recoverable conflict rather than delete data.
3. **Full fidelity:** Before real sync, build model-specific versioned codecs with nested round-trip tests. Handle the 16KiB server cap using an explicitly versioned larger-content strategy; reject oversized payloads locally until approved. Do not invent a new hosted migration, Storage subscription, or backend role exposure in this stage.
4. **Privacy/media:** Strictly separate portable metadata from device-specific alarm and media references. Keep original voice recordings local until their independent cloud-media retention/encryption design is approved; do not imply remote restore includes media bytes.
5. **Legacy snapshot handoff:** Existing versioned Creator backup/restore worker must not compete with entity-sync for the same projects/ideas. Build an explicit migration fence, recovery state and rollback plan. Keep V337 and existing data untouched until physical end-to-end tests.

## Acceptance criteria to move beyond 02A-F
- [ ] CI field audit passes on exact branch head; new fields cannot silently appear without review
- [ ] Recursive nested-model manifest, domain codecs and lossless two-way roundtrip for every portable field
- [ ] Stable ID mapping for imported records and linked Ideas/Projects
- [ ] Encrypted journal, process-kill and power-loss recovery, no cross-account replay
- [ ] Multiple offline edits without user-facing block; remote idempotency/conflict handling verified
- [ ] No duplicate competing snapshot writer and no silent restore overwrites
- [ ] Lab emulator + two physical devices with real Account sign-in, offline mode, concurrent edits and conflict/recovery checks
- [ ] Explicit gate for cloud Backup/Restore/Delete and Personal/Spaces/Challenges separate from Project/Idea sync

**Scope:** This branch contains only a field-coverage manifest, read-only validator, CI gate and this design note. No Java/Kotlin Android source, API, original Auth data, Supabase schema, APK or releases changed. Keep PR DRAFT.
