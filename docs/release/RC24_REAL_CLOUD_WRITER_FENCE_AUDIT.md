# Backlot RC24 — actual cloud writer audit and Creator DB privileged-write guard

**30 October 2026 · Strict ₹0 infrastructure.** Draft only. No live Supabase mutations, no deployed migrations/Edge Functions, no prod accounts or file reads, no Android code changes, no account deletion. This branch tests synthetic data on a disposable real local Supabase Auth/PostgREST/Storage installation.

## Grounded read-only production inventory

- The Android `CloudConfig.kt` contains a `sb_publishable_` key, not a service-role key. `CloudApiClient.kt` sends session.accessToken to the Creator profile, snapshot and sync-only deletion RPCs; `CreatorCloudVoiceMedia.kt` uses the publishable key + user JWT and sends `x-upsert: true` to the private `creator-voice-ideas` bucket.
- The connected Backlot project live PostgreSQL metadata shows **four Creator RPCs**: `claim_creator_username`, `push_creator_sync_snapshot`, `get_creator_sync_head`, `delete_creator_sync_data`. They currently run as `SECURITY INVOKER` and scope to `auth.uid()`; **the first three are not full deletion endpoints**.
- The currently deployed profile, sync-head, sync-snapshot and voice Storage policies check authenticated user ownership but do not check a server-controlled account-deletion fence. `delete_creator_sync_data()` removes sync heads then snapshots only, leaving profile, private Storage objects and Auth intact.
- Old 2026-09-11 migrations mention a creator generation-fence trigger, but a subsequent `20260911_reduce_supabase_to_creator_identity_v20.sql` drops that older design. **Do not assume obsolete migrations describe active safeguards**. RC24 uses the actual read-only live catalog and current Android client.
- No deployed Edge Functions were present in the connected Backlot project during RC21 inspection. No existing privileged Storage write backend was established from the Android source; external/unconnected backend services still need owner inventory.

## What we actually built

1. `rc24-local/audit_cloud_write_paths.py` runs over compiled Android Java/Kotlin sources and fails if privileged Supabase credentials or admin routes are introduced. It checks the known Creator profile, sync and Voice Idea transports use the publishable key + the signed-in account bearer. This is a static guard, not a proof of every future SDK or external service.
2. `rc24-local/creator_write_guard.sql` creates **local-only simplified tables with the same live Creator table names and owner keys**, not a production migration. A private trigger checks a local trusted deletion fence before any Creator profile, head or snapshot INSERT/UPDATE. The trigger also fires for `service_role` database writes, unlike RLS.
3. Its `FOR SHARE` fence row read serializes these Creator database writes with a trusted state UPDATE. It does not block DELETE, which is needed by future authenticated deletion cleanup, nor is it a solution to privileged Storage object uploads.
4. `rc24-local/test_creator_guard.py` creates real local Supabase Auth accounts, authenticates via real JWT, and uses real PostgREST to validate blocked account A vs active account B, including privileged database INSERT/UPDATE bypass attempts and unmodified existing data.
5. Existing RC22's 36 real local Auth/PostgREST/Storage checks run in the same disposable stack. RC17/18's 31 offline deletion tests run in a separate free CI job.

## Critical pending integration / security limits

- This is a **local staging schema with abbreviated Creator columns**, not a drop-in replacement for the deployed `public.creator_*` tables. Exact production SQL must be migrated carefully, preserve existing RPC signatures and foreign keys, backfill fence state for existing accounts without blocking normal use, and be validated against a full copy of the production schema before deployment.
- **Storage `service_role` still bypasses object RLS**. Only strict server credential isolation, a privileged write inventory and **no unfenced admin Storage writes** can close this gap. Do not claim a database Creator trigger globally blocks privileged Storage.
- Real Auth/Storage session refresh, signed/resumable uploads, in-flight object storage writes, cross-user deletion requests, failure replay, durable queue, secure server endpoint, web request form, privacy-policy URL, account deletion itself and Play approval remain unresolved.
- Do not conflate sync-only data deletion with full account deletion. Do not deploy fixture SQL, delete the app, remove owner backups, or merge to `main` without explicit review.

### No-cost references

- https://supabase.com/docs/guides/local-development
- https://supabase.com/docs/guides/database/postgres/row-level-security
- https://supabase.com/docs/guides/storage/security/access-control
- https://supabase.com/docs/guides/storage/schema/design
