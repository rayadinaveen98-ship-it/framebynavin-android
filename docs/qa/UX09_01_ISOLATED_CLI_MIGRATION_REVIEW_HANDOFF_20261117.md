# Backlot UX09-01 — isolated migration artifact preparation (review gate)

Date: 2026-11-17. **DRAFT / repo-only tooling. No hosted SQL, APK, Android source, merge, or release.**

## Protected sources and scope
- Owner-tested Lab V337 `4ba1263124d118d3917f988228aec2248068466f`, original V337 P0 `815ba69775f75b6e5f25bb838c2c17b4f09c203e`, and staged UX10F `6654c730971309b5047473e2f69292fe799b3f46` remain untouched.
- Parent PR #431 head `07d9e45715ad8607e2cd35e38ba2c6b67f2c7cf5` contains successful PG17.12 and PG16/local-GoTrue tests of the original `supabase/prototypes/ux09_01_project_idea_sync_contract_v1.sql`.
- This stage prepares *only* a CLI-generated review artifact in an isolated temporary project directory. It must never run `supabase db push`, use linked hosted credentials, or replay original V337 production migrations.
- Deliberately keep the prototype in `supabase/prototypes` until a generated filename and exact byte-for-byte SQL are reviewed and separately authorized for Lab.

## Fresh live read-only checks (2026-11-17)
Using the explicitly connected **Navin** Supabase account, project `vasleafrmsovdbdlrcsw`:
- Project `ACTIVE_HEALTHY`, PostgreSQL 17.12.
- Exactly two Lab migration-history versions: `20261117070805` and `20261117070925`.
- Four public Lab app tables are RLS-enabled; `backlot_sync_internal` does not exist; proposed two public sync RPCs do not exist.
- Security advisor warns that leaked-password protection is disabled (assess password-provider launch policy separately).
- The **Data API Exposed Schemas** control-plane configuration cannot be certified from a SQL connection. STOP before any deployment until Dashboard confirms `backlot_sync_internal` is not exposed. After authorized install, test forbidden private-schema REST directly as a signed normal account. This gate is not replaceable by RLS alone.

## New scoped files
1. `tools/qa/backlot_ux09_01_package_cli_migration_review.sh` — uses pinned tested Supabase CLI `2.122.1`; rejects different versions, generates a migration file via `supabase migration new` in a fresh isolated CLI project, copies the exact tested SQL, and compares its bytes/checksum. It never links to a project or runs SQL.
2. `.github/workflows/backlot-ux09-01-isolated-cli-migration-review.yml` — on the PR, publishes a 7-day *REVIEW ONLY* versioned migration artifact; read-only contents permission, no Supabase token/secret, no hosted operations.
3. This acceptance record.

## Review / acceptance gates
1. CI must actually run and pass; the generated artifact must exist. Until then this stage is **SOURCE-STAGED, NOT TESTED**.
2. Review generated CLI migration name, exact SQL hash, transaction, grants, RLS policies, and invoker RPCs. Compare against tested original prototype and ensure no original-app migration entries.
3. Confirm Data API Exposed Schemas in actual Lab Dashboard and zero remote schema collisions.
4. Founder must separately approve **one bounded, versioned, Lab-only hosted DDL operation** after reviewing the exact migration; the present approval to *continue development* does not waive that boundary.
5. On a future authorized deployment: live pre/post metadata and RLS grants, two-account signed JWT negative API tests, no legacy table changes, and non-destructive RPC-disable emergency response. No user data deletion.
6. Only then progress to UX09-02 Android local-first durable outbox, canonical Project/Idea adapters, conflict UI; subsequent Personal/Spaces/Challenges sync, full Backup, Restore and cloud/identity Delete remain distinct testable stages.
7. Later reconcile cumulative UX01–UX10F and all 22 P0 correction paths on a separate integration branch; complete Android CI and owner physical certification before candidate APK promotion.

**Do not auto-merge, deploy, copy service-role credentials to clients, or declare Sync/Backup/Restore/Delete complete.**
