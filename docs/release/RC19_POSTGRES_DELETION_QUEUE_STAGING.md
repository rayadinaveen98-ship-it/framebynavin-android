# Backlot RC19 — Real PostgreSQL deletion-queue staging verification

**Status:** Ephemeral database engineering test, not an online Supabase staging project, deployed deletion service, Android app change, or production migration.

## Implementation

- `supabase/account-deletion/rc19-staging/queue.sql` creates a private schema **only** when `current_database() = 'backlot_rc19_ephemeral'`. It refuses production database names and requires a CI-only worker role.
- A non-exposed `backlot_rc19_ci.requests` table stores immutable opaque request IDs, user UUID binding, stage/status, bounded attempts, retry times, lease tokens, expiry and a completion receipt. No FK to Auth is created, so requests can survive Auth termination.
- RLS + FORCE RLS are enabled on the queue; only the test worker is granted schema/table/function access. `anon` and `authenticated` receive no privileges. Functions run `SECURITY INVOKER`, with explicit empty search path.
- SQL functions enforce idempotent submit/owner binding, atomic `UPDATE .. RETURNING` lease claims, leased stage compare-and-set, bounded retry, support escalation, and completion only at the final stage with an unexpired token.
- SQL includes a `fence_recorded` marker, **not a real Creator DB/Storage write fence**. Real upload fencing and a fresh server-verified user session/reauth are required in a future trusted adapter.
- `test_queue.py` drives real PostgreSQL connections, tests eight concurrent client processes racing for the same work item, verifies a single winner, expired/stale leases, owner-only reads, role and RLS isolation, rerunnable requests, bounded retries, Auth-independent receipts and support escalation.

## CI boundary

GitHub Actions spins up a fresh `postgres:17` service, sets an ephemeral fake password for **that container only**, installs the schema into a database literally named `backlot_rc19_ephemeral`, and runs the Python client script. It does not use a connected Supabase project, service-role key, public OAuth token, existing account, live Storage bucket, or create a billed staging project. No database files or credentials are published as artifacts.

RC17's 15 pure deletion safety tests and RC18's 16 queue mock tests run alongside the real PostgreSQL contract tests. Automated source checks refuse changes outside these RC19 files, preserving the frozen Build269 production app and RC16 signing gate.

## Not yet implemented / remaining blockers

1. **Real remote staged account integration:** Staging Supabase Auth, Database and Storage with two fresh synthetic users and separate consent/cost review. No owner-approved isolated Supabase staging project exists from this task.
2. **Production write fence:** Privileged SQL, RLS policies, and Storage upload denial for a deleting user (including existing JWT/refresh sessions and any SDK bypass); the queue's `fence_recorded` flag alone does not enforce this.
3. **Live deletion adapters:** Scoped Supabase Storage list/remove with pagination, Creator heads-before-snapshots and profile removal, Supabase Auth Admin deletion after verification and client-owned request status.
4. **Reliable worker operations:** Durable operator-visible errors, server-side reauth/step-up, customer-safe request ID/status, rate limiting, abuse prevention, lease expiration and recovery, token revocation.
5. **Public, functioning app and web paths:** User-account deletion flow, externally accessible deletion-request page, verified privacy policy, owner contact and retention terms. Build269 correctly reports full deletion as unavailable.
6. **Play release:** Owner-phone testing, private production signer, audited stacked PR integration, protected `main`, Play declarations (#174, #180, #185, #187).

## Precise success interpretation

A green RC19 run proves **PostgreSQL queue mechanics under concurrency and roles in a disposable CI environment**. It does not certify a deployed backend, actual cloud deletion, full privacy compliance, or safe real-account removal. RC19 must remain a draft and must never be used as a production migration directly.
