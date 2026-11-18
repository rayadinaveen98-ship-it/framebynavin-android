# Backlot UX09-02D — Account-fenced, manually initiated Lab-only remote preview

Date: 2026-11-18
Status: SOURCE-STAGED IN ISOLATED DRAFT ONLY; exact-head CI, Android emulator, physical testing, and any promotion remain PENDING.

## Source and ancestry

- Parent: UX09-02C PR #444 at exact head `a963bcfd8a600406029ebe6c1caff576b4ac0d92`, which passed focused Android 15 and codec CI.
- UX09-01E hosted security/write gate previously passed for Projects/Ideas on **Backlot Lab ONLY** `vasleafrmsovdbdlrcsw`, and disposable QA accounts/records were independently confirmed cleaned.
- The cumulative UX01–08 + UX10 physically testing candidate is **separate** PR #445 at `abbe88984bac16cfafbd990d9cb00ed54cfdd07e`; it does NOT contain UX09. Do not merge into or replace it while owner testing is under way.
- Protected owner-tested Lab stable V337 and original production remain unchanged.

## Bounded user-visible QA slice

**Lab Settings → Advanced → Lab sync readiness (read-only)** retains the offline canonical Project/Idea readiness check. An additional explicit button **CHECK SIGNED CLOUD (READ-ONLY)**:
1. Requires `BuildConfig.BACKLOT_LAB`, exact Lab endpoint, and existing signed-in Google/Supabase user session.
2. Calls `CloudSyncManager.withFreshSession` to fence account switches before/after the request. Auth token refresh can update the stored **session**, not Project/Idea data.
3. Uses a **new read-only-only** HTTP facade to call the already-tested owner-scoped `backlot_ux09_pull_v1` RPC with `afterSequence=0` and `limit=20`.
4. Strictly validates the signed response with `Ux09LabRpcContract.parsePull`, including ordering, tombstones, UUIDs, payload safety and server cursor.
5. Displays only aggregated first-page Project/Idea change counts, tombstone count and whether further pages exist. No private titles, text, IDs, tokens, files or URLs reach UI state or logs. The first page is never called the total library.
6. Does not persist cursor, import/apply changes, save user records, create/update cloud records, create outbox entries, acknowledge tombstones, run any background sync, sign anyone out or change provider configuration.
7. The existing `Ux09LabFeatureGate.ENABLED=false` remains enforced on the normal write and pull RPCs; no automatic sync activation.

All network activity is explicitly initiated by a QA button. A signed read may fail for no session/network/auth and shows a generic safe message.

## Execution and acceptance gates

- One isolated new draft PR stacked only on UX09-02C, no protected branch or Supabase database edits.
- Source guards prove write feature remains disabled, Lab check and owner-generation fences present, and the preview has no canonical source-store mutation.
- Full Android Lab app/test compilation, full JVM regression suite (including pure preview negative tests), lint, package and public dev signer checks.
- Android 15 synthetic non-destructive app install/foreground process smoke. This is **not** a signed two-device data test.
- User may finish testing PR #445 APK without this branch impacting the installed app.

## Hard blockers before true automatic sync, backup, restore and deletion

- Durable canonical-save + encrypted outbox **atomicity/recovery** across DataStore/Room and process death, anti-rollback/KeyStore-loss checks, and rigorous offline multi-edit CAS conflict handling.
- End-to-end signed apply/pull of source-certified payloads, bounded and lossless larger records, UUID legacy migration, dual-device conflict/receive UI, and no concurrent old snapshot writer.
- Safe per-device reminder/media rehydration and Personal/Spaces/Challenge/Journals/creator domain coverage, immutable Done/XP receipts.
- Complete encrypted backup coverage, lossless restore dry-run + rollback, and verified server Auth + local account deletion (with all private data and dependencies) remain unimplemented. No account deletion will be executed in QA.
- No Play release or stable promotion until physical owner acceptance, separate scope review, and signed cross-device tests.

## Current verdict

The signed preview is a **manual QA diagnostic, not a sync engine**. The new CI is required before any claim of verified Android behavior. Keep PR DRAFT / UNMERGED.
