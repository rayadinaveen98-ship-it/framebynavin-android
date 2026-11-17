# Backlot UX09-02A — disconnected Android offline intent contract + crash-recovery prototype

**Date:** 2026-11-17  
**Status:** Source-staged, JVM CI pending. **NOT FEATURE-COMPLETE**, NOT enabled, NOT an APK.

## Scope (exactly four implementation files plus this document)
1. `app/src/main/java/com/framebynavin/app/cloud/ux09/Ux09SyncContract.kt`
   - Frozen `Ux09LabFeatureGate.ENABLED = false` ensures no authorized app entry point exists for this prototype.
   - Two narrow wire domains only: `creator_project` / `creator_idea`; not Personal, Spaces, Reminders, Challenge, media, backups or Auth deletion.
   - Typed per-account `Ux09Intent`: accountId/epoch, restore epoch, entity UUID, operation UUID, expected remote revision, hashes of entire local before/after entity, canonical JSON payload, explicit tombstone.
   - Fail-closed canonical JSON ordering, SHA256 digest, UUID validation, 16,384-byte payload cap, nested owner/token/device-path guard; allowlisted Supabase `apply_v1` parameter names.
   - **IMPORTANT:** This is a transport envelope *not* the finished CreatorTask/CreatorIdea domain codec. No fields are silently stripped; payloads exceeding server capacity are refused and retained locally.
2. `app/src/main/java/com/framebynavin/app/cloud/ux09/Ux09DiskIntentJournal.kt`
   - PREPARED → READY explicit journal with fsync(temp), atomic same-directory rename and directory fsync (no weak fallback).
   - Restart recovery decisions: NOT_SAVED (full local entity still equals before hash), READY_TO_RETRY (full local entity equals desired hash), NEEDS_MANUAL_RECONCILIATION (unexpected change/corruption), WRONG_ACCOUNT_OR_RESTORE (session/restore epochs changed).
   - Buffered writes not used; journal file contains checked SHA256. Pending entries and incomplete temp files are retained, not purged; stale/mismatched operation UUIDs fail closed. No outbox drain, cloud call, cursor updates, delete or legacy snapshot integration.
   - Prototype is plain-text JSON and **must never handle real user data** until a separately verified app-private storage and encryption threat model is approved. OS power-loss guarantees, Android directory fsync support, and cross-process locking remain untested; JVM restart simulation is not equivalent to physical power-cut certification.
3. `app/src/test/java/com/framebynavin/app/cloud/ux09/Ux09OfflineContractAndJournalTest.kt` — 17 offline JVM test cases.
4. `.github/workflows/backlot-ux09-02a-contract-unit.yml` — focused Gradle unit CI. No Supabase secrets, hosted requests, Android APK, app distribution or data writes.

## Additional 02A fail-closed fix: unresolved same-entity operations
- **Source change:** `Ux09DiskIntentJournal.prepare()` refuses to journal a second operation targeting the same account, domain and entity while the first remains unresolved. Idempotent repeat of the exact same operation ID remains allowed; unrelated entities and accounts remain independent.
- **Reason:** Both offline edits could otherwise carry stale `expectedRevision=0` and one would be incorrectly presumed safe. Remote operation UUID and expected revision must never be rewritten after uncertain delivery.
- **Unit coverage:** `secondOfflineEditToSameEntityFailsClosedUntilRevisionChainingExists()` verifies second edit refused, previous intent retained and unrelated entity still queueable.
- **Product limitation:** This is a deliberately restrictive safeguard in the **disconnected test prototype**, not acceptable finished offline-edit UX. A separate, durable per-entity operation-chain or safe unsent coalescing design must be implemented and crash-tested before any real app writer is connected. No existing edits are blocked because the new journal has no production caller.

## Explicitly untouched
- `TaskStore`, `IdeaVaultStore`, `CreatorViewModel`, `CreatorDataGate` including V344 restore journal, existing `CloudSyncManager`, legacy `CreatorCloudSyncManager`, WorkManager and all user-facing UI.
- Original stable V337, owner-tested Lab V337, any original/backend Supabase configuration, UX01–08/10 draft work, account deletion and media. No paid dependency.
- All existing runtime wiring: there are **zero consumers of these UX09 classes** in production app source. No automatic sync of any kind has been activated.

## Source-backed blockers and next contract decisions
- **Canonical ID incompatibility:** existing `CreatorTask.id` and `CreatorIdea.id` are strings and old records may not be canonical UUID. Never invent a lossy conversion; separately audit historical imported IDs and encode stable mapping with conflict handling.
- **Model completeness:** CreatorTask includes workflow, notes, content DNA, workspace and device-specific alarms; Idea includes rich audio metadata, transcript, status and reversible trash. An all-fields semantic mapping is required; raw local URIs/tokens must not leave device, but dropping meaningful fields is unacceptable.
- **Cross-store atomicity:** a file journal cannot magically make DataStore and Room writes atomic. A signed-off call-site ordering/recovery protocol and actual process death/fault-injection tests are mandatory before integrating with `TaskStore` or `IdeaVaultStore`.
- **Legacy snapshot writer collision:** existing whole-snapshot worker has independent backup/restore semantics. Never enable new per-item write sync concurrently with it without explicit migration/handoff and conflict policy.
- **No physical validation:** Android 14/15 filesystem/keystore, owner sign-in/account switch, restore generation changes and two-device offline/online tests remain outside this phase.

## Acceptance gates
1. New CI workflow must complete successfully on the exact PR head, with all focused JVM tests green; until then label **SOURCE-STAGED**.
2. Inspect Kotlin source for duplicate UUID reuse, journal corruption, unfinished temp handling, null JSON, oversize content, account generation mismatch and stale full-entity hash.
3. Before Phase 02B: define **lossless full model mapping**, stable ID compatibility, maximum payload migration strategy, durable encrypted app-private state, crash consistency, and snapshot-writer handoff.
4. Keep the PR **draft and unmerged**; no host writes, APK releases, production app changes, or auto-sync claims without separate review.

**Goal:** prove that a proposed local intent cannot be lost, mistaken for a successfully saved edit, or transferred across account generations in deterministic offline tests. This is an experimental primitive, not a release-ready outbox.
