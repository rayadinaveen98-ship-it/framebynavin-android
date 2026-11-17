# Backlot UX09-01 — First executed disposable PostgreSQL 16 contract evidence

**Date/time:** 2026-11-17 UTC.  
**Verification:** REAL GitHub Actions run, not source-only inference.
**Exact tested commit:** `19855a6adf75b6d963f878b9da11f45d61ffdf6e` (contains the UX09-01 prototype SQL, mock Auth fixture, synthetic PG scenarios, local-only shell runner and workflow).
**Run:** [GitHub Actions #44089557376](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/44089557376) — `completed`, **`success`**. Job `134060806357` `completed/success`.

## What really ran

The Linux GitHub runner started a **throwaway PostgreSQL 16 Docker service** named `postgres`, with database `backlot_ux09_disposable`, local-only host `127.0.0.1:5432` and synthetic roles/user UUIDs. The runner used `psql 16.15` against PostgreSQL server `16.16`.

The workflow executed `bash tools/qa/backlot_ux09_01_postgres16_test.sh`, which:
1. Refused non-loopback/incorrect database target and existing non-empty schema.
2. Created mock local `auth.uid()` and `auth.jwt()`, synthetic `anon` and `authenticated` roles, and **two fictional user UUIDs**.
3. Compiled and committed the **actual UX09-01 SQL prototype** to that throwaway Postgres16 database (four internal RLS tables and two authenticated sync RPCs).
4. Ran `ux09_01_security_assertions_v1.sql`: owner-bound policy count, RLS, no anonymous table or RPC access, no authenticated DELETE privilege on internal tables.
5. Ran `ux09_01_disposable_pg_scenarios.sql` under mock `authenticated` with two distinct synthetic subjects, covering Creator Project create/edit/delete, Idea create, revision CAS, stable op-id replay, malicious changed-op rejection, tombstone no-resurrection, bounded change cursors, cross-user RLS visibility and rejection of foreign-owner writes.
6. Ran **two independent simultaneous PostgreSQL sessions** using different operation UUIDs against the SAME as-yet-uncreated Creator Idea; exactly one operation was `applied`, the other `conflict`. No double write.

### Exact log excerpts from the successful run

```text
NOTICE: UX09-01 static disposable role/RLS/RPC assertions passed
NOTICE: PASS: disposable SQL scenarios for CAS, idempotency, RLS, 2 accounts, tombstones, cursor and two domains
PASS: disposable PG16 schema compilation + mock-A/B SQL/RLS/CAS scenarios
PASS: two actual concurrent PostgreSQL sessions serialize same-account CAS
IMPORTANT: Real Supabase Auth JWT and REST RLS A/B tests remain pending
```

### Limitations — do NOT overclaim

**What has NOT been proven by this run:**
- It did **not** connect to real Supabase Auth/GoTrue or mint/validate two signed real JWTs. `auth.uid()` and `auth.jwt()` were mock functions, so actual Data API exposure, Google OAuth, RLS JWT A/B and token expiry remain **NOT TESTED**.
- It did **not** apply SQL to hosted Backlot Lab `vasleafrmsovdbdlrcsw`, which remains unchanged.
- It did **not** include actual Backlot Android code, Room/outbox, two phones, background workers, physical Google sign-in, alarm/Snooze, Creator project real schema codecs or approved cross-record dependencies.
- It did **not** implement, exercise or certify complete account-level backup, guided restore, deletion of cloud data or deletion of Supabase Auth accounts.
- It did **not** certify all 40 roadmap QA scenarios, only the test cases explicitly covered by disposable PG source tests. Full signed JWT and two-phone tests remain separate no-go gates.
- User-approved no-old-cloud-data-migration still applies. No previous account records or project data were erased.

## Stronger second execution — current contract SQL and tests

- **GitHub Actions run:** [#44090476691](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/44090476691)
- **Exact tested source SHA:** `da90847ccea9211896ae6c3e7fa4187618067a02`
- **Outcome:** `completed / success`; job `134064013367` successful.
- **Expanded SQL mock-role negative cases:** in addition to cross-account pull and foreign INSERT denial, B's foreign-owner UPDATE has zero affected rows and direct hard DELETE is denied.
- **Additional actual concurrency test:** two concurrent SQL sessions write DIFFERENT Creator Idea IDs under the same synthetic account and **both commit** with distinct, consecutive per-account event sequences. This checks that the account cursor works beyond single-target conflict races.
- **Actual selected GitHub log evidence:**

```text
NOTICE: UX09-01 static disposable role/RLS/RPC assertions passed
NOTICE: PASS: disposable SQL scenarios for CAS, idempotency, RLS, 2 accounts, tombstones, cursor and two domains
PASS: disposable PG16 schema compilation + mock-A/B SQL/RLS/CAS scenarios
PASS: two actual concurrent PostgreSQL sessions serialize same-account CAS
PASS: two concurrent independent writes commit distinct, gap-free account sequences
```

These are again **mock Supabase Auth roles** in disposable Postgres16. Real signed Google/JWT A/B API, hosted Lab migration, Android two-device use, verified whole-account backup/restore and full account deletion all remain unimplemented or untested. The earlier successful run remains in history for provenance.

**Latest executed verdict:** Postgres16 SQL compilation, internal RLS mock-tenant access, CAS/idempotency/tombstones, same-entity race and independent-write sequence ordering **PASS at exact SHA `da90847c...`**. No hosted Backlot Lab migration has been made. Full UX09 is **NOT ready**.

**Verdict:** UX09-01 *Postgres16 contract-level* SQL compilation, mock-role RLS and two-session optimistic concurrency **PASS at exact SHA**. Host migration and Android client remain NOT STARTED. Future code changes require fresh test evidence tied to their own SHA.

**Next safe gate:** standalone Supabase/GoTrue two-user API/RLS negative acceptance in an actual **disposable local** Supabase stack, followed by review of the private schema's Data API exposure, payload codec constraints and authorized hosted migration plan. No production backend action without approval.
