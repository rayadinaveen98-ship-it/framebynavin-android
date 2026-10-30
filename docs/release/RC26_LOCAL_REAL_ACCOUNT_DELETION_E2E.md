# Backlot RC26 — actual complete local account deletion integration, ₹0

**Release status: NOT LIVE.** This PR proves the deletion sequencing against an isolated **real local** Supabase Auth/PostgREST/Storage stack, not against any user's account or connected hosted database. It makes no Android APK changes and does not connect the website's external deletion page. Keep PR draft.

## Why this is different from the earlier tests

- RC17–RC18 defined pure deletion/queue behavior, RC19 checked a real PostgreSQL job queue, RC20–24 rehearsed fencing and storage authorization. **RC26 actually calls the real local Supabase Storage API to remove physical object bytes and calls the real Auth Admin API to delete an Auth user, after deleting owned Creator data.**
- Two distinct synthetic email/password Auth users are created by local Auth, with real signed JWTs and a private local bucket. All SQL/HTTP targets are loopback-only via the existing RC22 harness. User A is deleted, user B must remain untouched.
- User identity is verified through `GET /auth/v1/user`; caller-supplied account IDs are never trusted. Confirmation phrase `DELETE MY BACKLOT ACCOUNT` is required. Anonymous/invalid bearer and service-role masquerading as account owner are rejected.
- A private, durable `backlot_rc26_ci.requests` receipt is inserted in the **same transaction** that sets account A's persistent write fence. Request repeats are idempotent per Auth identity. Completion survives Auth deletion because the receipt has no FK to `auth.users`.
- The privileged **local** worker removes A's owned voice objects using `DELETE /storage/v1/object/{bucket}` (Storage API); SQL only enumerates metadata read-only and never deletes from `storage.objects`. After verification, it deletes A's Creator sync heads, snapshots, profile and synthetic projects through PostgREST, deletes Auth user last via Admin API, and verifies no A rows or Storage objects remain.
- A deliberate injected failure after the fence must retain the receipt and existing data; a retry completes. A further retry becomes idempotent. B's files, Creator rows and Auth account remain. No production key/data is used.

## Remaining production blockers — do NOT treat local success as launch

1. **Not a deployed or publicly callable service.** The code is a trusted local Python worker test; the website remains a non-operational draft. A real authenticated in-app endpoint, working external deletion-request flow, durable server-side worker/leases, rate limiting and recent reauthentication must be implemented and reviewed.
2. **Production schema mismatch.** RC24's `creator_*` tables are abbreviated simulation copies. Production RLS and actual write-fence migration must preserve the real Creator RPCs, relationships, and existing users; cannot just apply the local SQL.
3. **Privileged Storage upload bypass remains.** Service-role clients bypass Storage RLS; all administrative writers and signed/resumable uploads need a real universal fence. Supabase-issued JWTs may remain usable until expiry after Auth deletion, so sensitive operations require session checks and write fence.
4. **Concurrency.** The RC26 local test is one worker in one process. Before production, handle concurrent request deduplication, worker leases, crash recovery at every stage, bounded paginated object listing, and fresh object uploads between storage cleanup and Auth deletion.
5. **Account-specific contact/privacy.** Confirm approved support contact, legal retention exceptions, verify third-party SDK data and privacy wording. Publish working external HTTPS deletion page and in-app request only when service is live; a static status page alone is insufficient for Google Play.
6. **Hardware/release.** Build270 remains untouched. Owner real phone restore, private Play signing migration, protected main, Play Data Safety/FSI/FGS requirements remain #174, #180, #185, #187.

## Safe boundary

- The GitHub Actions runner refuses private/paid run contexts and checks the base Build270 source is unmodified.
- Local Supabase CLI uses public-repo free GitHub Actions Docker; no hosted Supabase project, production migration, Edge Function deployment, new paid app service or user data deletion.
- Fixture `rc26-local/receipt.sql` guards `current_database='postgres'`, verified `current_user='postgres'` and explicitly set `backlot.rc22.ephemeral_only=true`. This is not a production migration.
- Real local Storage removal is destructive **only to the two synthetic temporary accounts**, with no real user's information in CI.

## Source references

- https://supabase.com/docs/guides/auth/managing-user-data — deleting Auth user does not invalidate already-issued JWTs, and requires owned Storage objects removed first
- https://supabase.com/docs/guides/storage/management/delete-objects — delete bytes through the Storage API, not SQL
- https://support.google.com/googleplay/android-developer/answer/13327111 — in-app and out-of-app account deletion requirements

**Keep PR draft, do not deploy the local fixture, do not merge `main`, do not claim that visiting the Backlot website submits a deletion request.**