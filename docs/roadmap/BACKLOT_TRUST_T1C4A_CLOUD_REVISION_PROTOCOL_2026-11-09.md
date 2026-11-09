# Backlot Trust T1C4A — immutable cloud revisions, owner binding and proof-of-readback

**Date:** 9 November 2026  
**Status:** isolated SOURCE REVIEW — no cloud API calls, SQL applied, bucket created, account logged in, full test run or APK.  
**Scope:** the first T1C4 increment; **NOT** a functioning universal cloud backup.

## Existing groundwork and exact boundary

Backlot already implements Google/Supabase identity via `CloudSyncManager.withFreshSession` with account-generation fencing, Creator JSON sync, and a **separate** encrypted Personal Archive V2 path using immutable private object uploads, download verification and a compare-and-swap head. Those paths have different schemas and coverage; they cannot be renamed "universal backup" or overwritten by the new path.

The new `TrustT1C4CloudRevisionPolicy` defines an isolated **new** protocol, without calling either legacy cloud API, installing a bucket, or changing any actual user data.

- Future private, authenticated, non-public storage bucket: `backlot-trust-universal-v1` (proposal, **not created**).
- New immutable object key: `<auth.uid()>/revisions/v1-<uuid>.enc`. Never reuse an object key, never upsert.
- Metadata: remote revision number, exact new object path, encrypted ciphertext SHA-256, encrypted byte count, creation time. **No private filenames, notes, project names, key material or passphrase**.
- Verify actual streamed readback bytes locally, not provider-supplied checksums. Require the readback proof before acknowledging the new head. A successful HTTP response is NOT durability proof.
- Compare-and-swap head based on **both previous revision and previous SHA/path**, rejecting different-device changes. CAS is *server-side atomic*, not a client read/overwrite race.
- After CAS, fetch the exact head again; only matching next revision/path/SHA/byte count counts as a confirmed remote revision.
- Never delete the previous remote object during upload, a conflict, a retry or after CAS. Recovery history and retention rules are separate reviewed operations.
- All old Creator/Personal cloud sync paths remain untouched.

## Required two-phase server protocol (NOT YET IMPLEMENTED)

1. Obtain a fresh authenticated account via `CloudSyncManager.withFreshSession`; use the session's **verified user UUID**, not a user-editable email or asserted owner string. Bind encrypted payload to the same exact UUID. Account switching must fence every step.
2. Gate on a **proved complete, immutable full-owner source manifest**. Current `TrustFrozenSourceFiles` / `TrustLocalPartialPreviewReceipt` hard-reject universal upload. T1B/T1C still omit some notification/Spark/media configuration and unguarded external producers; they must be resolved first.
3. Independently verify the local encrypted file and derive SHA/bytes. Reject excess total bytes and quota issues before opening a network request.
4. Fetch validated current remote head. Upload ciphertext stream to a **new** private owner-only key with upsert explicitly disabled.
5. Download that same new object through authenticated private storage, stream bytes through SHA validation, and fully verify the AES-GCM encrypted manifest. A claimed checksum from metadata is insufficient.
6. Perform a **single server-atomic CAS** from the verified previous head to the new verified object. On conflict, return a review-required status; do not delete the new object or overwrite someone else's revision.
7. Fetch the new head again and check exact revision/path/SHA/byte count. Until then user UI cannot say “backup complete.” Do not automatically update any previously verified backup head on network failure.
8. Store an authenticated, reviewable history. Never auto-restore or auto-delete old versions; T2 and T3 remain separate user-authorized operations.

## Server/storage security requirements (design gate; nothing deployed)

- Private storage bucket only. `storage.objects` RLS **INSERT** and **SELECT** may be granted only when `bucket_id` matches the new private bucket and the first path segment equals `(select auth.uid())::text`; explicit deny for upsert/update/delete in normal client role. Do not permit public/anon read or reveal signed URLs in logs.
- New `trust_backup_heads_v1` and `trust_backup_history_v1` must have `user_id uuid`, `enable row level security`, explicit `SELECT` owner predicates `user_id = (select auth.uid())`, and no public/anon grants.
- A client must not get arbitrary REST `UPDATE` privilege on the head: that could bypass CAS. Exact server-side claim authorization, SQL role semantics, `auth.uid()` check, search path, function-execute grants, old-head comparison and transaction lock must be reviewed before deploying. Do not introduce an unreviewed `SECURITY DEFINER` RPC or put a service role key in Android.
- A former user's phone must not access another owner's path even if a UUID is guessed. The server, not just Android string validation, enforces ownership.
- Use cost controls and free-tier quota checks (encrypted original files may be large). Never provision a paid bucket, compute, or subscription automatically.
- Restrict key recovery separately; the current passphrase is not stored. A returned object SHA alone is not evidence that a lost key can be recovered.

## Tests committed but not executed

Pure JVM `TrustT1C4CloudRevisionContractTest` checks owner path validation, unsafe paths, genuine streamed ciphertext, tampering/truncation/extra data, remote head conflicts, exact CAS acknowledgement and rejection of partial local previews. The remote-readback proof has a private constructor and cannot be created from an HTTP status alone.

**Next T1C4B** implements a private transport adapter using actual Supabase authenticated streaming upload and download, and a reviewed, owner-only database head+history CAS backend. Before any DB migration: inspect deployment schema, review RLS/security advisers, test with disposable accounts and simulate races. This PR intentionally contains **no migration under `supabase/migrations`**, no bucket creation, and no production endpoint calls.

**T1D** remains the one comprehensive JVM + Android 11/15 + signed upgrade + simulated network/kill + two-device proof gate. Full success is not claimed. #327 and Trust #344 stay open.
