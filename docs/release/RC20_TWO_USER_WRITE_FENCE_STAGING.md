# Backlot RC20 — Two-user write-fence RLS rehearsal

**Engineering scope:** Disposable PostgreSQL 17 staging simulation only. **No new Supabase project**, no production database changes, no Edge Function deployment, no actual Storage HTTP API, no Android app code changes, no account deletion.

## Safety-critical finding

RC19 established a durable deletion-request queue but its `fence_recorded` marker did **not** stop subsequent Creator writes or Supabase Storage uploads. RC20 separately demonstrates how account-scoped RLS eligibility and an advisory-lock protocol can isolate a deleting user's writes at the PostgreSQL layer.

## What RC20 really tests

- Two distinct synthetic actors, modeled by PostgreSQL `NOLOGIN` roles. `actor_id()` derives from `current_user`, not user-supplied JSON metadata, JWT claim strings or caller arguments.
- A private `backlot_rc20_ci.fences` table has one immutable row for each synthetic account. Users may read only their own fence but cannot update it or invoke the admin-only `mark_deleting` function.
- The mock Creator projects table and *separate simulated voice-object metadata table* enforce SELECT, INSERT and UPDATE RLS policies.
- An authenticated user's **INSERT and UPDATE** are accepted only for its own UUID and when `write_allowed()` proves the account is not blocked. After setting A's fence, A cannot create or modify either kind of data. B's operations remain unaffected.
- `write_allowed()` takes a transaction-scoped **shared advisory lock** per user; the trusted admin's `mark_deleting()` takes the corresponding exclusive lock. A real concurrent writer holds the shared lock until commit, so the new fence waits for in-flight writes before becoming effective.
- The script refuses execution unless the database is literally `backlot_rc20_ephemeral`; CI creates this disposable DB and fake roles in a GitHub-hosted PostgreSQL container, never the connected live Supabase project.
- RC17/18's **31** offline deletion tests and RC19's queue evidence remain independent.

## Limits that matter

1. **This is NOT a real Supabase Storage API test.** `voice_object_metadata_mock` is a standalone table, not `storage.objects`. Supabase advises using the Storage API for actual object mutations; RLS SQL on a mock table cannot prove upload/upsert/copy/move enforcement in a live bucket. Do not run these statements on live `storage.objects`.
2. **This is NOT live Supabase JWT authentication.** SQL roles simulate validated users; production must use genuine server-validated Supabase Auth, `auth.uid()` in actual RLS, session revocation and fresh reauthentication. The code is not a drop-in production migration.
3. **Only tested writers participate in this lock protocol.** A service-role client or direct privileged writer can bypass RLS. The real Creator sync RPC, all Storage upload/upsert paths, workers and background services must be identified, instrumented and tested before any real deletion is accepted.
4. Release proof requires two real staging Auth identities, real private Storage object bytes, API upload/update/copy/move attempts from a stale session after fence, cleanup via Storage API, and verification that account B is untouched. Production secrets must never reach the APK.
5. A durable job/queue, release signing and privacy deletion site are **not** production-complete merely because this exercise passes. See #174 #180 #185 #187.

## Release rule

Keep draft; do not merge this SQL as a Supabase migration or announce that deletion is live. No chargeable Supabase project was provisioned. A production adapter and actual Storage API / Auth staging remain the next separate gate.

Official references:
- https://supabase.com/docs/guides/database/postgres/row-level-security
- https://supabase.com/docs/guides/storage/security/access-control
- https://supabase.com/docs/guides/storage/schema/design
