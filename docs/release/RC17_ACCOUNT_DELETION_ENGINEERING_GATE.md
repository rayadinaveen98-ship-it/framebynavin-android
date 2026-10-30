# Backlot RC17 — account deletion security groundwork (offline only)

**Status:** Security-core prototype and offline automated tests. **NOT** an active account-deletion feature, Edge Function, deployed database migration, privacy policy, deletion-request page, or Play eligibility certification.

## What was verified read-only against the connected Backlot Supabase project on 30 Oct 2026

- Three user-data tables under `public`: `creator_profiles`, `creator_sync_heads`, `creator_sync_snapshots`.
- The existing invoker RPC `delete_creator_sync_data()` deletes `creator_sync_heads` then `creator_sync_snapshots` for `auth.uid()` only. It **does not** delete account profiles, Supabase Auth users, or private voice-media files.
- The app writes user-scoped voice recordings to the **private** `creator-voice-ideas` Storage bucket with paths beginning `<auth-user-uuid>/...`. Storage files must be deleted through Storage API, not metadata SQL.
- A public `v131-build-assets` bucket also exists, but it is not the user-scoped voice bucket; **never indiscriminately delete shared product assets**.
- Supabase had no deployed Edge Functions at inspection time. No live user identity or content was read, and no database mutation or deletion occurred.

## What this branch implements

`supabase/account-deletion/deletion_core.ts` contains a pure, dependency-injected deletion ordering function. It cannot access Supabase on its own and requires a future trusted server adapter. Its verified contract requires:

1. Exact `DELETE BACKLOT` confirmation and a valid opaque request ID.
2. The server-verified authenticated user ID must match the requested account; a fresh server-verified reauthentication timestamp must be within five minutes. **Never trust client-provided reauthentication times or user metadata.**
3. A durable lock/fence that prevents concurrent Creator snapshot writes and Storage uploads must be established before any removal. The production adapter must actually implement and prove this, including RLS/Storage enforcement.
4. Paginated deletion through the Storage API of only paths under the verified account ID, with bounded batches, strict path validation and a fail-closed limit.
5. Delete sync heads before snapshot history (foreign-key restriction), then Creator profile; verify zero Creator rows and zero voice media.
6. Delete Supabase Auth **last**, verify identity no longer exists, and only then record completed in an independent durable request store.
7. On any error, do not report success. The production system will need resumable requests, durable failure reporting, rate limits and operator recovery.

Fifteen offline Deno tests exercise success, sibling-account protection, old reauthentication, wrong confirmation, tampered storage paths, duplicate/oversized storage listings, interrupted cleanup, order, leftover data and Auth-deletion failures. They use synthetic identities only.

## Blocking work before connecting to production

- [ ] Build and test persistent account deletion requests with status receipts independent of `auth.users`, retries, operator support and documented retention.
- [ ] Implement a production adapter on an isolated/nonproduction Supabase project, with real read-only identity binding, fresh reauth/step-up, rate limiting, RLS and Storage write fence, and Storage API list/remove pagination. Never store a service-role key in APK, browser or public repo.
- [ ] Add web requests usable without the installed Android app; securely prove account ownership. Externally hosted deletion-request page, privacy policy, support identity/contact and owner-approved SLA are **not yet available**.
- [ ] Prove at least two synthetic accounts on staging: attempts to delete another account fail, interrupted deletion safely resumes, storage failures keep Auth intact, and no uploads can race the deletion lock.
- [ ] Add real Android in-app deletion request UI and status screen only after a live backend is approved. Build269 explicitly and correctly says full deletion is unavailable.
- [ ] Review processor/deletion scope (Google/YouTube OAuth, Supabase, Firebase, local Personal records, external exported files); validate legal wording and Play Data Safety.
- [ ] Finish physical phone acceptance, private production signing, protected main and Play review (#174, #180, #185, #187).

## Why no live deployment?

Account deletion is irreversible, and the current live project has no durable deletion request queue or write fence. Publishing a destructive endpoint before those systems and verified staging tests exist could delete an authenticated user while leaving surviving cloud media or causing a partial failure. This PR deliberately modifies **no** production Android app source or database configuration.

**Release status remains blocked.** No claim of completed account deletion, working public deletion URL, production signing or Play approval is made.
