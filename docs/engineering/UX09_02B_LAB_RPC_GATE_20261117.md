# Backlot UX09-02B — Signed Lab Project/Idea RPC Adapter (DISCONNECTED)

**Status:** Source-staged, exact-head CI pending. **Not enabled, merged, shipped or tested live.** Date: 2026-11-17.

## Motivation and verified server basis
Backlot Lab project `vasleafrmsovdbdlrcsw` has a deployed and independently signed/tested `backlot_ux09_apply_v1` + `backlot_ux09_pull_v1` contract supporting **Creator Projects and Ideas only**, with account-RLS, idempotent operation IDs, compare-and-swap revisions, sequence-ordered change history and tombstones. UX09-01E E01–E16 live hosted acceptance and final disposable-account deletion passed. No new Lab credentials or QA users are required for this source stage.

Previous disconnected Android engine: journal PR #437 and field audit PRs #438–439 passed CI. Operation-chain PR #440 passed 31 focus tests. Encrypted store #441 and file lease #442 remained under exact-head CI at branch creation.

## Exact six-file implementation
1. `app/src/main/java/com/framebynavin/app/cloud/ux09rpc/Ux09LabRpcContract.kt` (NEW): strict apply/pull response parsing, known wire domains, exact response shapes, bounded page size, monotonic event sequence and matching cursor, authenticated operation revision/identity/tombstone checks, canonical JSON and local-path filtering.
2. `app/src/main/java/com/framebynavin/app/cloud/CloudApiClient.kt` (ADDITIVE): exactly two **internal** method entrypoints for the known `/rest/v1/rpc/backlot_ux09_apply_v1` and `pull_v1` endpoints, reusing the existing authenticated HTTP request, publishable-key configuration, no-store cache policy and error path. Both invoke the **disabled runtime feature gate and hardcoded Lab-only endpoint refusal guard before any HTTP**. Apply requires `session.userId == intent.accountId`.
3. `app/src/main/java/com/framebynavin/app/cloud/ux09rpc/Ux09LabSignedRpcClient.kt` (NEW): uses `CloudSyncManager.withFreshSession` to fence signed account transitions before/after RPC I/O; exposes typed `apply()`/`pull()` only. **No production caller**; no legacy snapshot change.
4. `app/src/test/java/com/framebynavin/app/cloud/ux09rpc/Ux09LabRpcContractTest.kt` (NEW): 16 synthetic JVM tests, covering original-production backend rejection, applied/conflict shape, identity and revision mismatches, pagination bounds, strict sequence/cursor ordering, tombstones, overfetch, unknown domain, unsafe local links and reserved payload keys. No credentials, no web or Lab writes.
5. `.github/workflows/backlot-ux09-02b-lab-rpc.yml` (NEW): focused `:app:testDebugUnitTest` and no-activation/static Lab guard; no Android APK.
6. This doc (SIX files including documentation).

## Hard limitations
- NO TaskStore/IdeaVaultStore, Content Workspace, DataStore, WorkManager, UI, existing cloud backup, restore, deletion or V344 recovery integration. No automatic sync; `Ux09LabFeatureGate.ENABLED=false`.
- Parsing authenticated responses is necessary but insufficient: a future **transactional local apply + cursor commit** must validate canonical IDs, preserve local-only reminder/media fields and never ack events before local persistence.
- The backend's 16KiB payload ceiling still prevents universal restoration of rich scripts/assets/versions. Nothing is truncated; larger-content design/migration and privacy review remain mandatory.
- The signed session client is intentionally not called by any current app flow. A real Lab-only sync-worker gate is a separate future PR with authenticated physical device tests and account/restore and old-snapshot handoff.
- Existing Backlot Lab migration, permissions, all original production and stable V337/Lab V337 build sources untouched. No new users, credentials, backend writes or paid infrastructure.
- CI source-only tests cannot certify Android OAuth session transitions, network cancellation, live pagination, physical account logout or cross-device conflict behavior.

## Acceptance and next gates
1. PR must pass exact-head focused Gradle JVM tests. No missed source safety checks.
2. Parent sealed-store and lease PRs must separately pass before any durable worker is connected.
3. Design and test **lossless portable full Project/Idea codecs** (including linked IDs, scripts >16KiB, local paths and reversible Idea trash), account-private persistence rollback/checkpoint, DataStore crash atomicity and legacy backup handoff.
4. Only then make an isolated Lab APK, test two signed Android devices, offline edits, conflicts, restart, restore and deletion with a real user-visible status/errors contract. Never silently activate this adapter on original production.
5. Keep this PR DRAFT/UNMERGED; explicit integration/release gate after certification.

**Intent:** move toward the Lab Android integration without prematurely touching real user work or original production.
