# Backlot UX09-02A-O — pure ordered Project/Idea offline-operation chain

**Date:** 2026-11-17. **Status: CODE STAGED; Gradle/CI verification pending.**

## Parent evidence
- UX09-01E hosted Lab Project/Idea apply/pull contract E01-E16 passed and disposable QA account cleanup independently verified. Existing test records remain empty; owner Google user preserved.
- UX09-02A journal prototype PR #437 completed focused JVM compile/tests on `688c734...`, Actions run #44133359413.
- Top-level field coverage PR #438 passed Actions #44138949808; nested coverage PR #439 passed Actions #44140299063 (21 models/22 spans).
- Protected original V337 and original production backend are NOT changed by any of those isolated drafts.

## Exact change scope
- `app/src/main/java/com/framebynavin/app/cloud/ux09/Ux09OfflineChainPolicy.kt` — **pure** immutable local-edit chain and remote CAS request state machine. Only one account/domain/entity per chain. No production caller or cloud connection.
- `app/src/test/java/com/framebynavin/app/cloud/ux09/Ux09OfflineChainPolicyTest.kt` — 13 synthetic offline tests, added alongside existing 18 journal/contract tests.
- `.github/workflows/backlot-ux09-02a-offline-chain.yml` — focused `:app:testDebugUnitTest`, no APK/hosted backend.
- This document. No Kotlin source outside the new `cloud.ux09` package changed.

## Guarantees implemented in the *pure state machine only*
1. Local drafts are an explicit FIFO list with stable UUID identity and full-entity before/after hashes; rejects missing predecessor, duplicate edit IDs, noncanonical/oversized/sensitive JSON and edits after queued tombstones.
2. One prepared immutable `Ux09Intent` at a time; its first `expectedRevision` is the last acknowledged server revision, not a guessed revision. Later edits are only prepared after the previous exact operation has acknowledged.
3. Uncertain HTTP success: `retryExact` returns the same operation UUID, expected revision and payload. It is *not* a network adapter; future callers must durably persist the prepared state BEFORE sending HTTP.
4. Server acknowledgement is checked for matching operation ID, next revision, monotonic observed sequence, and matching tombstone status before dropping the first local edit.
5. Remote conflict **freezes** the queue and retains all queued drafts; never auto-merges/overwrites user work. Remote permanent tombstone prohibits future resurrection of the same entity UUID.
6. Account/restore generation must match the prepared request. A malformed recovered chain or forged prepared body rejects without clearing pending data.

## Critical limitations: NOT FOR REAL USER DATA
- In-memory immutable state is **NOT durable**. This only establishes deterministic ordering decisions. The plaintext experimental `Ux09DiskIntentJournal` still does not securely persist the chained state, commit across DataStore writes or tolerate all Android power-loss windows.
- Second same-entity offline edits are *still blocked* in the original file journal. This policy is separate until integrated with a secure per-entity durable store. **Do not claim offline editing works in real Backlot.**
- No cloud I/O, WorkManager, Auth integration, data migration, Project/Idea codec, large content handling, multi-device test or account deletion.
- No user data, production Auth, Supabase Lab SQL or Storage modified.
- Real deletion vs reversible Idea trash still requires explicit mapping and protected media handling.
- Legacy whole-snapshot backup and new entity delta writer cannot be enabled simultaneously without handoff and rollback tests.

## Acceptance and next gate
- CI success on exact PR head for **31 targeted JVM tests** (18 original offline contract/journal tests + 13 new chain tests).
- Confirm `Ux09LabFeatureGate.ENABLED=false`, no cloud clients/workers in `cloud.ux09`, no stable source modifications.
- Then proceed to separate **UX09-02A-S** encrypted durable persistence + process-death rollback/fault-injection and full typed domain-codec design, not to app live integration.
- PR stays DRAFT and UNMERGED until comprehensive work is reviewed. No APK, merge, paid resource or release.
