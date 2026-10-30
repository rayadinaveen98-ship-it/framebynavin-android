# Backlot RC30 — privileged Storage delete-only boundary: real local integration

**Status: DRAFT / NOT DEPLOYED / NOT A PRODUCTION SECURITY GUARANTEE.** The actual Backlot app is still frozen at verified Build270. No new APK, Supabase SQL migration, hosted production deletion, publicly deployed worker, Play Store submission or paid infrastructure.

## Specific RC29 issue addressed

RC29 verified user-JWT Storage writes are denied after account fencing using RESTRICTIVE RLS, but also demonstrated a raw service-role Storage **POST still succeeds**. This is by design because service credentials bypass RLS. RC30 does **not** pretend to revoke the service key or fix arbitrary direct administrative operations. Instead it reduces the trusted deletion worker's privileged Storage capability to **DELETE only**, bound to a private durable account-deletion receipt and an already active deletion fence.

## Concrete code changes

1. New `supabase/account-deletion/rc30-local/privileged_storage_delete_only.py`: hardcoded bucket identifier (actual Backlot `creator-voice-ideas`, optional explicitly local RC22 alias). The only exposed method is `remove_objects_for_receipt(receipt_id, names)`. It loads owner/stage/status from trusted private server records, requires the deletion lock, validates each name as a canonical path under exactly that owner, rejects mixed-account, malformed, duplicate, oversized batches, completed/non-Storage stages, invalid receipts and unexpected bucket. Privileged Storage HTTP transport is callback-held by trusted worker, not passed by callers.
2. RC26 trusted local deletion worker now routes every Storage object removal through this single-purpose delete-only adapter. Previously it directly called the privileged Storage API. The durable authenticated request/fence/status state from RC26–28 remains unchanged; Auth deletion remains last.
3. New `rc30-local/test_delete_only_boundary.py` tests 14 independent fail-closed cases offline, checking cross-account denial, no external call on rejection, path traversal, missing fence, wrong stage, completed receipts and absence of upload/upsert/copy/move methods.
4. New `rc30-local/test_real_delete_only_worker.py` tests the adapter through real local Supabase Auth + Storage API with two synthetic users. After A is fenced, direct raw service-key upload to A is deliberately performed and observed, followed by real DELETE-only removal of exactly A's objects; B's files and Auth survive. Completing the already-fenced receipt invokes the refactored RC26 worker, verifying the full cleanup still reaches actual Auth deletion.
5. CI runs as public repository standard GitHub Actions on a disposable local Supabase CLI stack, with strict loopback target checks and exact changed-file allowlist, no secrets or billable hosted resources.

## Explicit remaining launch blockers — DO NOT OVERCLAIM

- This code runs in the *local test worker* and deliberately does NOT prevent direct privileged Storage writes by other tools, keys, dashboard integrations, S3 access keys or unconnected services. A bad actor holding a raw admin key remains capable of bypassing all RLS. Admin credential distribution and external privileged writer inventory must be resolved; do not put the service key in any client.
- The adapter is deliberately not a network-exposed API, production worker service or credential vault. For production, use a reviewed least-privilege deployment boundary with no privileged upload method or externally supplied arbitrary URL, and separate the deletion worker's administrative capabilities by process/service. Direct admin Storage operations outside it must be eliminated or controlled.
- Storage may have signed URLs, resumable TUS or S3 uploads in flight; app capabilities and production provider settings must be audited. Bulk list keyset pagination, retry leases, concurrency after deleting objects, stale JWTs, legal retention, new-user fences, real production Creator schema migration/backfill and hardened Google reauthentication remain STOP gates.
- Supabase advises against changing the internal `storage` schema, so this phase does not add intrusive triggers to `storage.objects`, change S3 internals or delete Storage metadata with SQL. Actual object bytes are deleted solely through the Storage API.
- The external public privacy/deletion website remains draft and not capable of accepting requests; Android Data & Privacy still correctly states deletion is not available. Keep this status until complete verified production account deletion and policy review.

## Release objective

Finish the production service and external account-deletion flow in a small number of integrated phases; do not keep adding unrelated test candidates. Block Play signing/publishing until #174 phone acceptance, #180 private signer, #185 protected main and #187 real Play deletion compliance are explicitly cleared.

References: https://supabase.com/docs/guides/storage/schema/design ; https://supabase.com/docs/guides/storage/security/access-control ; https://supabase.com/docs/guides/auth/managing-user-data
