# Backlot UX09-02C — First real Creator-store read-only Lab integration

**Date:** 2026-11-18  
**Current status:** IMPLEMENTED IN ISOLATED DRAFT SOURCE; exact-head CI/Android 15 emulator gate **PENDING**.  
**Parent:** draft PR #443 exact `ad3eaa22aaf0bfe650906acccd984208a3ae868f`, whose focused Lab signed RPC gate passed. Prior journal, field-coverage, operation-chain, encrypted queue and OS-file-lock focused suites also passed at their respective exact SHA. All parent PRs remain draft/unmerged.

## Why this is a new milestone
We are moving beyond detached synthetic sync classes and exercising **actual canonical Creator data models and existing serializers**. Before any live cloud request can safely run, a device must identify which real records can be represented *completely* in the current 16,384-byte Project/Idea RPC without truncation, leaking local file paths or causing alarm/media side effects.

This PR creates a **Lab-only manual read-only QA screen**, not a new account-sync, backup or restore workflow. The eventual first end-to-end milestone remains: create a simple Project/Idea on device A, save it durably, authenticate, send safely, receive on device B with correct conflict handling. This PR covers only the **source inspection/compatibility gate** preceding that milestone.

## Exact implementation and original source reuse

1. `app/src/main/java/com/framebynavin/app/cloud/ux09bridge/Ux09CanonicalReadinessPolicy.kt`
   - Explicit audited 47 Project and 26 Idea top-level persisted field names, matched to the existing source-authoritative UX09-02A field manifest in CI.
   - Only admits **simple planned Projects** (no active reminders, schedule, complex Content Workspace, Content DNA, publication/workflow progress, legacy ID or unreviewed link).
   - Only admits **unlinked text Ideas** (no attached voice media, remote media references, active Idea reminders, reversible trash or unmapped Project relationship).
   - An additional read-only call to \`ScriptStudioStore.loadForUnifiedSnapshot(task)\` is performed only for otherwise-ready Projects: if a legacy unmerged Script Studio sidecar carries ANY data—including script text, hooks, revision/status or other metadata—readiness is vetoed as \`LEGACY_SCRIPT_STUDIO\`. Fails closed on sidecar read errors rather than silently ignoring hidden Creator content.
   - Never sends or logs payloads; candidate payloads exist briefly in memory for validation only.
   - The original local persisted record is never modified. A copy of its source-authored JSON has exactly three device-local Project keys or five device-local/unreviewed Idea keys excluded. Everything else remains intact.
   - Must pass the original `TaskStore` / `IdeaVaultStore` decoder **full-model equality** check against the object with only those explicitly local-only fields normalized. Any different data, extra/missing field, unsafe nested file URI, 16KB overflow or noncanonical UUID fails closed, retaining the complete original local record.
   - `Ready` means a *readiness candidate*, **not** authorization to transmit or import.

2. `app/src/main/java/com/framebynavin/app/data/IdeaVaultStore.kt`
   - Exactly two additive internal, pure single-Idea serialization helpers (`encodeRecoveryIdea`, `decodeRecoveryIdea`), both reuse existing private encode/decode and never touch DataStore. Existing `save`, `mutate`, `capture`, media ownership and restore behavior remain unchanged.
   - Project uses existing `TaskStore.encodeRecoveryTask/decodeRecoveryTask` methods; **TaskStore is not modified**.

3. `app/src/main/java/com/framebynavin/app/cloud/ux09bridge/Ux09LabCanonicalReadiness.kt`
   - Actual `TaskStore.tasksFlow.first()` and `IdeaVaultStore.ideasFlow.first()`, inside `CreatorDataGate.transaction`, with restore generation recheck.
   - Explicitly **does not call `CreatorDataGate.readyTransaction`**, because that can repair pending restores and perform writes. Ordinary Creator transaction denies pending V344 combined restore and serializes with concurrent Creator writers. This inspection performs no save, import, deletion, upload, local recovery, snapshot sync or background work.
   - Strict `BuildConfig.BACKLOT_LAB` and exact Lab backend guard; 500-record manual QA cap to prevent blocking other Creator edits. Returns only totals and blocker counts; no content names, IDs, notes, media paths, payloads or tokens to the UI.

4. `app/src/main/java/com/framebynavin/app/cloud/ux09bridge/Ux09LabReadinessActivity.kt`, `app/src/main/java/com/framebynavin/app/ui/V144SettingsHub.kt`, `app/src/main/AndroidManifest.xml`
   - **Backlot Lab → Settings → Advanced → Lab sync readiness (read-only)**. Dedicated nonexported Activity, guarded with `BACKLOT_LAB` before any UI or inspection.
   - Manual check only; shows candidate totals and safe blocker categories; generic error disclosure (never includes record IDs, original private error details or filesystem paths).
   - Does **not** open `CloudSyncActivity` because it can run legacy whole-snapshot sync upon sign-in. Preserves all existing Settings rows, stable app entrypoints and reminders. This new row is invisible in normal V337 builds.

5. `app/src/test/java/com/framebynavin/app/cloud/ux09bridge/Ux09CanonicalReadinessPolicyTest.kt`
   - 20 synthetic JVM tests: 47/26 property fences, file-local exclusions, basic ready states, malformed/extra fields, oversized (>16KiB), remote identity, legacy ID, reminder, voice, trashed Idea, unsafe nested URI and unexpected round-trip change.

6. `app/src/androidTest/java/com/framebynavin/app/cloud/ux09bridge/Ux09CanonicalSourceRoundtripAndroidTest.kt`
   - **Android 15 instrumentation** with synthetic in-memory CreatorTask and CreatorIdea using the actual current `TaskStore` and `IdeaVaultStore` one-record encoders and decoders; no real user data, no DataStore write, no cloud login. Must pass before claiming a first physically testable read-only readiness APK.

7. `.github/workflows/backlot-ux09-02c-lab-readiness.yml`
   - Exact-source static no-cloud/no-write gate; rerun previously verified top-level and nested field manifests; Kotlin policy expected-field parity.
   - Build with `-PbacklotLab=true` into **separate package `com.framebynavin.app.lab`**, not normal production Backlot; compile focused JVM test and Android instrumentation APK; verify public preview signing identity and package metadata.
   - Android 15 emulator executes the 2 real-source synthetic codec tests.
   - CI artifact is a **Lab read-only QA candidate only**, not a promoted stable release, not an assertion that two-device sync/backup/delete is working.

## Known blockers before ANY upload/download
- **Account ownership and durable change outbox:** this read-only bridge does not generate per-record mutation events, save edits into encrypted outbox, replay across process death, publish remote changes, or apply changes back into DataStore. All prior prototype classes are still disconnected; `Ux09LabFeatureGate.ENABLED=false`.
- **Legacy snapshot conflict:** existing `CreatorCloudSyncManager` and `CreatorCloudSyncWorker` perform whole-snapshot sync. No per-entity writer may run concurrently until the explicit owner, migration/handoff and rollback contract is approved and tested.
- **Data completeness:** default/empty workspace pilot is intentionally narrower than complete Backlot. Rich scripts, deliverables, workspace stage histories, linked Ideas, voice recordings, reminders, Personal/Spaces/Challenges and old/non-UUID data are correctly classified as **blocked**, not silently ignored or claimed cloud-backed.
- **Server 16KB JSON cap:** no change to hosted database. A larger-content strategy and exact lossless codecs are required for full Creator sync.
- **Device key/recovery:** previously tested AES-GCM sealed store and OS lock need trusted anti-rollback state, Android keystore/key-loss, backup exclusions, power-loss and DataStore transaction crash tests before live cloud activity.
- **Account deletion, backup/restore:** not implemented or certified by this source-inspection PR.

## Release decision
- DO NOT merge, promote stable V337, replace owner-certified Backlot Lab V337, modify original production Supabase, create user data or enable synchronization.
- After source + Android 15 CI pass, produce a **separate Lab QA APK** for owner inspection of counts only, then separately authorize/implement a bounded Creator simple-record write/read pipeline. Physically verify UI and data integrity before any two-device claims.
- All original user records remain untouched.

**Acceptance criteria:** CI full success at exact PR HEAD including Android 15 real source round-trip, 20 JVM policy tests, Lab package/signing and no-write source guard; no regression to prior P0 baseline. Current status remains unverified until these checks pass.
