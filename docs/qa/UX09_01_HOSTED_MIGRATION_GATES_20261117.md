# Backlot UX09-01 — PG17 and hosted Lab migration gates (no hosted DDL)

**2026-11-17.** This is an acceptance plan, not authority to deploy. Current verified host is Backlot Lab `vasleafrmsovdbdlrcsw` with PostgreSQL 17.12. Original V337 and owner-tested Lab baseline remain untouched.

| ID | Gate | Acceptance | Current state |
|---|---|---|---|
| M01 | Read-only hosted project check | Lab project active, PG17.12 | Verified read-only |
| M02 | Existing app tables | Exactly four original, no UX09 tables | Verified read-only |
| M03 | Existing RLS contracts | Existing 4/4/3/3 owner policy coverage unchanged | Verified read-only |
| M04 | Existing migration lineage | Exactly two previous Lab migrations recorded | Verified read-only |
| M05 | Schema collision check | No backlot_sync_internal | Verified read-only |
| M06 | RPC collision check | No backlot_ux09_apply_v1/pull_v1 public objects | Verified read-only |
| M07 | Anonymous public CREATE | Anon cannot CREATE in public | Verified read-only |
| M08 | Disposal PG16 SQL contract | RLS/CAS/serial source and same/different entity races | Passed previous CI |
| M09 | GoTrue local signed JWT | Two accounts separate in local PostgREST | Passed previous CI |
| M10 | Private schema local REST exclusion | Accept-Profile private schema denied | Passed previous CI |
| M11 | PG17 SQL and mock RLS | Run unchanged contract in disposable Postgres17 | Pending new CI |
| M12 | PG17 concurrent CAS | Same entity only 1 APPLIED, distinct entities 2 APPLIED | Pending new CI |
| M13 | PG17 internal schema public RPC count | Four internal tables, two expected public RPCs | Pending new CI |
| M14 | Hosted Data API control-plane config | Internal schema not in Exposed Schemas list | Pending manual verification |
| M15 | Exact versioned migration | Use actual CLI migration new + checksum/diff | Pending approval |
| M16 | Hosted migration no data import | All current app objects, private voice and Auth untouched | Pending approval |
| M17 | Hosted migration role/RLS grants | 10 owner policies, authenticated invoker, anon denied | Pending hosted deployment |
| M18 | Hosted post-deploy REST forbidden schema | Real JWT receives 404/406 for private schema | Pending hosted deployment |
| M19 | Emergency safe-disable | Revoke new sync RPC EXECUTE only, retain data | Source staged, unexecuted |
| M20 | Android/backup/recovery/deletion scope | No green badges, no account deletion, no app cutover | Protected, future UX09 work |

**Execution evidence boundary:** Previous PG16/signed-local-GoTrue jobs passed against UX09-01 code PR #430. The new PG17 job must be read AFTER it completes and tied to the exact tested SHA; no earlier job proves PG17. The Data API Exposed Schemas configuration is NOT verifiable from `current_setting('pgrst.db_schemas')` in the SQL connection. Confirm it in the Backlot Lab dashboard before and after any authorized migration.

**No migration applied:** The proposal is in `supabase/prototypes`, NOT `supabase/migrations`. An actual migration must be generated with the verified Supabase CLI on a disposable environment, reviewed, and separately approved before hosted SQL. The emergency RPC disable file is unexecuted and preserves data.

**Four product guarantees remain independent:** (1) account-scoped automatic Sync; (2) verified recoverable Cloud Backup; (3) guided fresh-device Restore; (4) cloud-data and Backlot-account Delete. The reviewed Project/Idea sync API does NOT complete any of these end-to-end. Local-only erase remains separate and previous Backup/Restore Settings controls must not reappear.

**STOP:** hosted DDL with unreviewed public schema exposure, default client-wide admin keys, deletion of legacy/personal data, disabling the incomplete-Challenge backup guard, merging UX into protected V337, creating paid resources, or calling an unbuilt Android APK stable.
