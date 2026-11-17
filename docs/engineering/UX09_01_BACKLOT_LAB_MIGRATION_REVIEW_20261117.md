# Backlot UX09-01 — Hosted Lab migration preflight and rollback review

**Date: 2026-11-17. Status: SOURCE-STAGED / HOSTED MIGRATION NOT AUTHORIZED OR APPLIED.**
Founder approved moving Backlot forward with **no old cloud-test-data migration**, preserving both accepted stable variants and the Google-verified Lab package. This record is a REVIEW GATE, not instructions to apply SQL against the live Lab by implication.
- Official V337 stable P0: `815ba69775f75b6e5f25bb838c2c17b4f09c203e` (protected).
- Owner-tested Backlot Lab V337: `4ba1263124d118d3917f988228aec2248068466f` (protected).
- Cumulative UX10F source: `6654c730971309b5047473e2f69292fe799b3f46` (draft/not integrated into Lab).
- UX09-01 tested prototype PR #430 latest code reviewed at `17688134c9b3e286e469d59a8034b1bc6c7698f2`.
- New branch: `feature/backlot-ux09-01-lab-migration-readiness-20261117`, based on exact PR #430 HEAD.
- Backlot Lab hosted project: `vasleafrmsovdbdlrcsw`, India Mumbai, Free-tier project, PostgreSQL **17.12**.
- No paid branch, new Supabase organization, Edge Function, service-role exposure, Realtime upgrade, cloud-data wipe or app-account deletion authorized.

## 1. Read-only direct hosted preflight — VERIFIED

Via Supabase read-only SQL / project metadata on **the actual Backlot Lab**:
- PostgreSQL server `17.12`, project `ACTIVE_HEALTHY`.
- Exactly **4** original public application tables:
  `creator_profiles`, `creator_sync_heads`, `creator_sync_snapshots`, `personal_sync_heads`. All four remain **RLS-enabled**, with respective 4/4/3/3 existing owner policies; no app tables altered.
- Existing versioned migration count **2**:
  `20261117070805 backlot_lab_v337_fresh_install_20261117`,
  `20261117070925 backlot_lab_v337_private_voice_storage_policies`.
- `backlot_sync_internal` **ABSENT**. Both proposed `public.backlot_ux09_apply_v1` and `public.backlot_ux09_pull_v1` **ABSENT**; no object collision.
- Anonymous and authenticated roles do **not** have CREATE privilege on public schema (but do have normal public USAGE); public schema RPC creation remains a privileged migration action.
- `current_setting('pgrst.db_schemas',true)` is NULL when inspected through the SQL session. **This does NOT verify PostgREST's exposed schema setting.** It is a separate control-plane configuration, requiring Dashboard inspection plus a real Data API forbidden-schema test after migration.
- Project advisor currently reports one unrelated **leaked-password protection disabled** warning. Review whether password auth is enabled before launch. This is not evidence of an active RLS vulnerability in the new (not-yet-deployed) schema.

**Source for reproducible read-only query:** `supabase/prototypes/ux09_01_lab_preflight_readonly.sql`. No data payload read is needed; never dump Creator/Personal data in this stage.

## 2. Existing verified isolated tests and version alignment

| Evidence | Tested system | Result |
|---|---|---|
| [GitHub PG16 run #44090476691](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/44090476691) | Disposable Postgres16 + synthetic `auth.uid` | PASS: CAS, RLS mock roles, tombstones, account sequence same/different entity races |
| [GitHub current PG16 run #44093822390](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/44093822390) | Same DB contract on updated source | PASS |
| [GitHub local GoTrue run #44093822510](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/44093822510) | Disposable Supabase CLI `v2.122.1`, GoTrue and real signed JWTs, PostgREST | PASS: authentic two-account JWT API isolation, CAS, delete/tombstone, private-schema REST inaccessible |
| **PG17 compatibility job** | Disposable PostgreSQL 17.12, matching hosted server version | **PASS**: [GitHub Actions run #44096576969](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/44096576969), tested SHA `ce064327ee2fd23e07ef5521fed44ba00422f9a1` |
| Hosted Backlot Lab | Not modified | Preflight-only, no application RPCs yet |

**Code coverage caveat:** The prototype is a generic JSON envelope that currently allows only Creator Projects and Ideas. It is NOT yet tied to Backlot's canonical Android codecs or project/idea relational invariants. Do not deploy a real Android writer until those are verified. It does NOT implement Sync/Backup/Restore/Delete for all app domains.

## 3. Exact additive SQL scope and permission invariants

Only the previously CI-tested `supabase/prototypes/ux09_01_project_idea_sync_contract_v1.sql` is considered as the future migration's **source of truth**; do not transcribe, fork or relax it during deployment without rerunning tests.

Candidate additive objects, **none present on hosted Lab today**:
- Internal `backlot_sync_internal` schema **must NOT be exposed on Data REST API**; has owner-scoped authenticated SELECT/INSERT/UPDATE to support invoker functions but no authenticated direct DELETE. RLS always ON, 10 authenticated owner policies.
- Four internal tables: `account_sequences`, `entities`, `changes`, `applied_operations`.
- Two `public` `SECURITY INVOKER` RPCs: `backlot_ux09_apply_v1`, `backlot_ux09_pull_v1`. Explicit `REVOKE ... PUBLIC, anon`; `GRANT EXECUTE ... authenticated`. Both derive account ownership from `auth.uid()` and reject anonymous/no-user calls.
- Project/Idea max payload 16 KiB, optimistic record revision, idempotent op ID, nonresurrectable tombstone and per-account monotonic delta cursor.
- New SQL is transaction-wrapped; legacy Creator/Personal snapshot tables/RPCs, Auth providers, private voice bucket and old backup settings are not modified. There is NO data import from old Supabase.
- Do not add a permanent direct REST route to internal tables, `SECURITY DEFINER` bypass, anonymous grants, service-role key in Android or secret in GitHub.

### Security STOP: actual Data API exposed schemas

Supabase docs explicitly distinguish role `GRANT` and RLS from **which database schemas PostgREST exposes**. Because the security-invoker RPC requires authenticated SQL privileges on the internal tables, **exposing that schema in Data API would let signed-in clients bypass the version/CAS/idempotency function using generic direct REST writes**. Before live migration and after it:
1. Open [Backlot Lab Data API settings](https://supabase.com/dashboard/project/vasleafrmsovdbdlrcsw/integrations/data_api/settings); verify **backlot_sync_internal is NOT in Exposed Schemas** (and explicitly disallow it on any future change).
2. In a controlled post-migration authenticated integration test, request REST with `Accept-Profile: backlot_sync_internal`; require 404/406 as already shown by local signed JWT CI.
3. Independently inspect live `pg_policies` / `information_schema` / `pg_proc.prosecdef` and role grants. Passing RLS alone does not satisfy this API-exposure gate.

## 4. Migration artifact and deploy approval protocol

**Not yet a versioned hosted migration**. `supabase/prototypes/` files are intentionally separate from `supabase/migrations/`; do not make up a timestamped migration ID or declare CI staging a deployment.

When ready and *after explicit founder authorization of the exact destination and scope*:
1. On trusted Supabase CLI environment, discover supported commands using `supabase --version; supabase migration --help; supabase migration new --help` (tested local CLI version 2.122.1).
2. Generate a proper filename via `supabase migration new backlot_ux09_01_creator_entities`; insert byte-for-byte reviewed prototype SQL. Verify checksum and no unrelated SQL / generated artifacts.
3. Apply resulting migration FIRST to a disposable **PG17/Supabase** stack, with the same signed JWT Data API and SQL tests, before any remote DB change. Check the `supabase db push --help` flags against actual CLI, do not infer defaults.
4. Review exactly 2 existing Lab migrations and absence of new schema/functions again; confirm no schema collision and no nested dangerous DDL. Obtain explicit scoped founder go-ahead **for Lab-only hosted DDL**.
5. Apply one versioned transaction with evidence at exact commit SHA, then read-only verify four new RLS tables/10 policies/two invoker RPCs, old four tables unchanged, two previous migration versions intact, new applied migration recorded.
6. Perform signed normal-user positive/negative smoke against Lab **only with disposable account identities**, only after explicit hosted-test approval. No owner private Projects/Personal/media upload. No Android product uses new API until UX09-02 separately approved.
7. Review advisors/permissions and record rollback disable/re-enable/backup result and cost monitoring. No move to original V337/stable or Google Play without separate approval.

## 5. Non-destructive emergency disable is preferable to DROP

`supabase/prototypes/ux09_01_emergency_disable_rpc_v1.sql` is an **unexecuted emergency-only candidate** for authorized incident response. It REVOKEs authenticated EXECUTE on both new RPCs and preserves all internal records, revisions, operation acknowledgements and tombstones.

**Do NOT** use `DROP SCHEMA CASCADE` as an automatic rollback: deleting tombstones or history while a stale offline device holds queued edits can resurrect previously deleted records, and dropping tables also destroys evidence. Emergency disable must be accompanied by accurate Android/Sync status, an incident report and later reviewed re-enable mechanism. The emergency SQL does not terminate an Auth account or delete old Creator snapshot data.

## 6. Sync, Backup, Restore, Delete — do not collapse the promises

- **Sync**: the proposed Project/Idea CAS RPC works locally; no Android outbox, two-device automatic sync, Personal/Challenge/Spaces adapter or per-domain UI yet.
- **Backup**: synchronization changelog is NOT a proven recoverable full-account archive. A future covered-domain manifest, encryption/privacy, integrity verification and device-loss recovery test are separately mandatory.
- **Restore**: no guided new-phone restore, local conflict preview, rollback escrow or OS alarm re-permission test exists yet.
- **Delete**: only record tombstones are modeled. Separate *erase local phone* (already exists) from future *delete cloud Backlot data* and *delete Backlot Auth account + Storage*; neither cloud/account deletion is implemented. Do not advertise `delete_creator_sync_data` as full identity removal.
- Android Google OAuth and physical Lab V337 success do not validate this new integration. UX01–08/UX10F remain unmerged and require Lab V337 protected P0 source reconciliation.

## 7. Explicit go/no-go gate

**PG17 CI PASSED** at the tested SHA. **READY FOR HOSTED MIGRATION REVIEW**, but NOT YET APPROVED TO DEPLOY: the Dashboard Data API schema exposure, actual CLI-generated versioned migration/diff, live security review, reversible-disable protocol and founder's separate Lab-only SQL authorization are still pending.

**NO-GO** if any of these occur: remote schema already exists/collides, `backlot_sync_internal` exposed in Data API, client-side service key, missing `auth.uid` ownership, public RPC with anonymous rights, workflow skips signed-JWT checks, Android claimed backup complete from this prototype, Personal Challenge guard removed, unreviewed paid deployment, or protected V337 modified.

**Owner approval boundary:** this document authorizes nothing on hosted Lab by itself. PG17 tests PASS; NEXT: finish the Dashboard Exposed Schemas review, generate the exact versioned CLI migration, and seek explicit approval for a controlled **Lab-only** DDL change. No account data or cloud records erased.
