# Backlot UX09-02 — Android offline-first Project/Idea sync source audit & staged implementation gate

**Status:** SOURCE AUDIT / PROPOSED ARCHITECTURE. **No Android production code changed, no APK built, no live Lab writes, no merge.** Date: 2026-11-17.

## Authoritative verified baseline
- Protected original V337 P0 stable and separately owner-tested Backlot Lab V337 remain untouched.
- UX09-01E closed E01–E16 on Backlot Lab `vasleafrmsovdbdlrcsw`: owner-run real signed two-user Project/Idea REST contract passed; independently verified deletion of both disposable QA accounts, no residual account or sync records, original Google identity plus creator profile/snapshot/head preserved.
- Versioned Lab migration `20261117141908 backlot_ux09_01_creator_project_idea_records` remains installed and tested. Four `backlot_sync_internal` tables RLS-protected, two `public` SECURITY INVOKER RPCs only. This server contract supports `creator_project` and `creator_idea` only.
- UX09-01E evidence: `docs/qa/UX09_01E_HOSTED_SYNTHETIC_WRITE_TEST_GATE_20261117.md` in parent PR #435. Both PRs remain draft/unmerged.

## Verified code inventory (from exact parent HEAD `13b923ec359de8ed5f37365e68b65ae98b0b5015`)

| Current component | Source | Impact |
|---|---|---|
| Canonical Creator projects | `app/src/main/java/com/framebynavin/app/data/TaskStore.kt` | Entire task list is persisted in DataStore `creator_v0` / `tasks_json`; `tasks_json_last_good` fallback; `CreatorDataGate` and task mutation mutex; export/import and local recovery |
| Canonical Creator Ideas | `app/src/main/java/com/framebynavin/app/data/IdeaVaultStore.kt` | DataStore `idea_vault_v09`, `ideas_json_last_good`; generation-aware writes and audio file ownership, local-and-remote audio fields |
| Models | `data/CreatorTask.kt`, `data/IdeaVault.kt` | Project has reminders, workflow/stage, publication, local file paths/voice, content DNA and many fields; Idea has audio local path/transcript and trash state. 16KB server JSON payload ceiling must be enforced, not truncate records |
| Local transaction/restore generation | `data/CreatorDataGate.kt` | Serializes writers, holds restore recovery fence; restore can invalidate queued optimistic actions; do not bypass |
| UI write coordinator | `data/CreatorViewModel.kt` | Existing channel batch queue/generation; only triggers cloud background worker after successfully settled writes; success is not proof of durable individual entity delta |
| Existing cloud account/session | `cloud/CloudSyncManager.kt` + `CloudLocalStore.kt` | Google sign-in, serialized account transitions, encrypted local session storage, refreshed sessions, generation checks on account switch |
| Existing cloud HTTP | `cloud/CloudApiClient.kt` | Supports older snapshot RPCs (`get_creator_sync_head`, `push_creator_sync_snapshot`, `delete_creator_sync_data`), not new `backlot_ux09_*_v1` adapters |
| Existing snapshot sync path | `cloud/CreatorCloudSyncManager.kt` | Local-first versioned whole-snapshot backup/sync and explicit conflict resolution/restore; currently protects against overwrites; **do not silently run concurrently with new per-entity sync** |
| Existing background worker | `cloud/CreatorCloudSyncWorker.kt` | WorkManager 6-hour periodic and sooner one-time job; currently drives snapshot + Personal cloud sync. Avoid same unique work name for new engine |
| Existing Room | `personal/spaces/V289SpacesDatabase.kt` | Room 2.8.5 already installed but this private Spaces database is separate and should not become UX09 outbox |
| Android dependencies | `app/build.gradle.kts` | Room runtime/KTX/compiler and DataStore already available. No new paid service or dependency justified for the first isolated stage |

## Hard architecture risks — DO NOT implement away implicitly
1. **DataStore ↔ Room non-atomic two-store writes**. A local edit must never appear saved while its intended sync operation is silently lost. An app-level mutex is not a cross-store power-loss transaction. Before implementation, design a durable write-ahead/recovery mechanism with deterministically replayable operations. Evaluate either (A) fully room-owned canonical Creator entities + outbox with one transaction and explicit migration, or (B) recoverable write-ahead intent before existing DataStore save, with after-crash local hash/epoch reconciliation. Proposal B minimizes V337 disruption but needs carefully demonstrated crash/restart safety.
2. **Whole-snapshot legacy sync vs individual-entity delta convergence.** Both must not write the same canonical Project/Idea data in parallel. Initially isolate new engine behind Lab-only disabled-by-default feature gate; preserve old backup pathways unchanged. Define explicit handoff/migration with tests before enabling.
3. **Complete model coverage vs 16KB server payload cap.** CreatorTask/Idea are rich mutable objects. The server contract permits JSON `object` up to 16,384 bytes. Reject oversize with safe local retention; never strip fields silently or sync absolute local audio paths, secret tokens, private file locations or system-specific reminder alarms.
4. **Stable IDs & deterministic codecs.** Existing CreatorTask.id and CreatorIdea.id are strings; server requires UUID. Audit all ID creation and old imported data before choosing migration/mapping. No lossy UUID mapping or collision.
5. **Deletion semantics.** Distinguish soft-trash, archive and actual deletion. Server tombstone is irreversible under current protocol; never translate an Idea trash action or Project archive into permanent tombstone unless the app's semantics match. Protect against stale resurrection.
6. **Cross-device side effects** (scheduled local reminders, notifications, voice media). Receiving content must not silently schedule alarms with old device-specific permissions or file URI; explicit local rehydration plan required.
7. **Identity and privacy fences.** Every outbox/event/cursor belongs to a Supabase `auth.uid()` account and local account generation. On logout and account switch, suspend draining, never upload another person's local data, and keep guest/local-only work. No service-role key in the APK. Clear cross-user cached remote data only through verified scoped cleanup.
8. **Restore and backup.** `CreatorDataGate` and V344 recovery journal are safety-critical. Invalidate in-flight operations on restore epoch changes. New live sync must not silently replace older backup/restore/delete semantics. No full account deletion claim at this stage.

## UX09-02 implementation stages (proposal; founder major-direction gate remains)
**02A — Contract/codec + durable state design (source only).**
- Define supported *narrow* Creator Project/Idea fields vs unsynced device-local state, stable UUID and canonical JSON encoding, sensitive path/audio exclusions, max payload checks and schema fingerprint.
- Introduce versioned outbox envelope contract: `accountId`, `domain`, `entityId`, `operationId` UUID, `expectedRevision`, `localGeneration`, `canonicalPayloadHash`, `tombstone`, `state`, `retryCount`, `nextAttemptMillis`, `cursor`; no tokens/passwords.
- Design crash-consistent local transaction or recovery algorithm and verify it with *synthetic* filesystem fault injection before wiring into the real stores.
- Tests: malformed payload, old IDs, model roundtrip, field coverage and 16KB overflow, replay, recovery across crash windows, account epoch/restore fence.

**02B — Isolated Lab-only RPC adapter and worker.**
- Add two typed RPC calls `backlot_ux09_apply_v1` and `backlot_ux09_pull_v1` reusing `CloudSyncManager.withFreshSession`; reject unexpected server schema, 400/401/403/409 and transient 429/5xx with safe backoff.
- Unique `WorkManager` chain with network constraint, idempotent per-account outbox ordering, retry/backoff and **feature gate OFF by default**, not coexisting active with old snapshot writer.
- Tests: no network/offline, 401 refresh, logout mid-flight, kill/restart, duplicated worker instances, cursor advance only after durable local apply.

**02C — Canonical writer integration in isolated Lab builds.**
- Source audit all TaskStore/IdeaVaultStore mutation entry points and restore/import/capture/deletion paths. Route a minimal, precise test slice through robust journal; never replace working V337 screens.
- Existing media/voice/reminder side effects device-local and excluded until separately scoped.
- Build dedicated CI APK and emulator tests; no automatic V337 stable promotion.

**02D — Lab staged acceptance.**
- With disposable accounts: A online writes ↔ B receives; B offline changes; reconnection; same-entity conflict without silent loss; account switch; permission-denied and network loss; restore interference; larger data/scale; battery/background constraints. Physical devices mandatory.
- Keep decisions to publish/backfill/delete/migrate and stable merge separately reviewed.

## Acceptance gate for moving to 02A code
- Approved supported-field mapping and lossless ID policy; explicit old snapshot ↔ new per-entity handoff.
- Chosen and tested durable local journal architecture, not just an in-memory queue.
- Guaranteed no impact to protected V337/accepted P0 paths; exact changed-file budget, CI plan and rollback.
- No hosted schema changes anticipated for 02A. No Android APK until integration tests finish.

**Current recommendation:** begin 02A with a **non-destructive code-only, Lab-off default** contract/codec/crash-safety prototype; do not activate new sync automatically until certified. This document audits architecture only; no production implementation has been performed.
