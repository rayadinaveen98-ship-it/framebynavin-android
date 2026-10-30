# Backlot RC18 — deletion request queue, idempotent worker and crash recovery

**Status:** Offline-only typed queue/worker contract and mock persistence tests. This is NOT a deployed database, live Edge Function, authenticated deletion endpoint, functioning external account-deletion page, or production rollout.

## What changed

- `supabase/account-deletion/deletion_queue.ts` models submission by a server-verified account, the immutable global request ID binding, isolated owner-only status reads, an atomic worker lease, stage checkpointing, bounded exponential retry and the five-attempt `needs_support` state.
- Destructive stages are ordered: persistent write fence -> private voice Storage API removal -> Creator heads -> Creator snapshots -> profile -> zero-data check -> Auth deletion -> Auth absence check -> durable completion receipt.
- `supabase/account-deletion/deletion_queue_test.ts` exercises idempotency, competing workers, ownership isolation, malformed/duplicate Storage listings, retry delays, stale lease rejection, support escalation and crash-after-Auth recovery using synthetic in-memory objects.
- RC17's standalone deletion-ordering security core and its 15 previous tests are retained unchanged.

## Why a durable request is important

Supabase Auth deletion can succeed while the processing worker crashes before recording its result. The persisted request must survive independently of `auth.users` foreign-key cascade and be resumable by a service-only worker that no longer requires the deleted user's JWT. The Auth removal operation must be idempotent and must check verified absence rather than assuming an HTTP status means success.

## Mandatory production implementation contract (NOT implemented)

1. **Persistent, private queue:** A non-exposed PostgreSQL schema/table with unique opaque request ID and immutable `user_id` UUID; service-only access, no `anon`/`authenticated` insert/update/delete privileges, and RLS enabled as defense in depth. An authenticated request endpoint must verify the user's Supabase session server-side and enforce independent recent step-up before creating the record.
2. **Atomic lease claim:** Use transactional compare-and-set on row status, expiry and lease token, preferably a transaction with `FOR UPDATE SKIP LOCKED` or equivalent. Every stage advance, defer and completion update must validate *both* request ID, current stage and the current lease token. Handle stale leases and worker death. Mock lease tests do not prove SQL atomicity.
3. **Write fence:** The database and private Storage upload paths must deny new owner writes after a deletion request is accepted, using server-controlled state. Ensure that active tokens cannot race deletion, and do not use `user_metadata` for authorization. A fence that affects only database tables is insufficient.
4. **Service adapters:** Must run only on trusted backend with securely stored private credentials; never place service role, admin key or private Storage secrets in the APK, web JS, GitHub repo, PR logs or client error messages.
5. **Storage API:** Enumerate owner-prefixed objects with bounded pagination; remove through Supabase Storage API, not direct SQL changes to `storage.objects`. Reject sibling prefixes, weird path segments and duplicate pages. Apply independent operator handling beyond bounded safety limits.
6. **Identity and completion:** Bind deletion requests to a server-verified authenticated owner, not a user-supplied target ID. Delete Auth last, verify absence, and persist a receipt detached from Auth FK lifecycle. Never mark success simply because deletion was enqueued.
7. **Operations:** Add rate limiting, retries, durable error categories, worker telemetry (without personal data), operator escalation, a retention schedule and nontechnical end-user status wording. The current in-memory fake is not a production durable store.
8. **Staging verification:** Use two synthetic accounts on a nonproduction Supabase project. Test cross-account attacks, reauth expiry, crash after each stage, storage write races, simultaneous workers, restart after Auth deletion, retries, `needs_support`, public web requests and complete account-scope cleanup.
9. **Owner policy and product decisions:** Publish accurate privacy policy and externally reachable deletion-request URL only after the backend and owner contact are verified; finish Play Data Safety and permission declarations. Make real Android UI only after secure endpoint exists.

## Safety and evidence

Production Build269 Kotlin/Compose, package ID, signer, Android UI, Supabase live database, migrations and deployed Edge Functions are unchanged from RC17. CI enforces the narrow diff scope and runs RC17 and RC18 tests without any network privileges or service-role keys. Live deletion is not enabled.

**No merge to `main`, no live deployment, no account deletion and no Play submission** are authorized by RC18. Open blockers: #174 (owner acceptance), #180 (signer migration), #185 (protected main), #187 (Play/privacy/deletion).
