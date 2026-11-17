# Backlot UX09-01 — PostgreSQL 17.12 exact-version compatibility PASS

**2026-11-17 | EXECUTED AND VERIFIED in disposable GitHub Actions. No hosted Lab SQL.**

## GitHub Actions proof

- **Run:** [#44096576969](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/44096576969)
- **Job:** `134085157395` — `completed / success`.
- **Exact tested source SHA:** `ce064327ee2fd23e07ef5521fed44ba00422f9a1`.
- **Services:** isolated `postgres:17` Docker image running **PostgreSQL 17.12** (also Backlot Lab hosted server major/minor 17.12), test-only database `backlot_ux09_disposable` and synthetic `auth.uid()` fixture.
- **No remote project URL, secret, service_role key, real user records or project migration was used.**
- **Test SQL:** same `supabase/prototypes/ux09_01_project_idea_sync_contract_v1.sql` previously proven on PG16 and in local signed GoTrue+PostgREST CI. The branch's PG17 workflow does not modify the prototype.
- The reused test runner emits one older literal `PG16 schema compilation` status line even when connected to PostgreSQL 17; the actual GitHub Docker service startup log and `server_version_num` assertion prove this job used **PG17.12**, not 16.

### Exact proof excerpts

```text
Confirmed synthetic PostgreSQL 17 instance
NOTICE: UX09-01 static disposable role/RLS/RPC assertions passed
NOTICE: PASS: disposable SQL scenarios for CAS, idempotency, RLS, 2 accounts, tombstones, cursor and two domains
PASS: two actual concurrent PostgreSQL sessions serialize same-account CAS
PASS: two concurrent independent writes commit distinct, gap-free account sequences
NOTICE: PASS: disposable PostgreSQL 17 private schema and RPC surface
LOG: starting PostgreSQL 17.12
```

## What is genuinely demonstrated

- Candidate SQL compiles and executes in disposable **PostgreSQL 17.12**.
- Synthetic role ownership/privileges, RLS policies, `SECURITY INVOKER` and public RPC count are consistent.
- Repeated operation ID and conflicting revision do not silently overwrite a saved Project/Idea.
- Deleted records yield tombstones; an old create-0 cannot silently resurrect them.
- Concurrent writes to same record allow exactly one success, while concurrent writes to distinct records allow both in unique ordered account-change sequences.
- Original Android/Creator/Personal data is not contacted or changed.

## What remains unverified

This new PG17 job uses *mock* auth claims (as designed). Separately, [local Supabase signed GoTrue JWT REST run #44093822510](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/44093822510) passed on the tested server SQL. **Neither** proves hosted Backlot Lab PostgreSQL security settings, Google Android sign-in for this new RPC, deployed migration, cross-device synchronization, complete Backup, guided Restore or Backlot Auth Account Deletion.

**Live Backlot Lab still has its original 4 public app tables / 2 historical migrations, with no `backlot_sync_internal` schema.** The current migration-readiness PR is not a live migration. Owner approval and Dashboard Data API schema exposure review are still blocking gates.

**Conclusion:** UX09-01 PostgreSQL-17.12 schema/concurrency compatibility **PASS**, no hosted authorization implied.
