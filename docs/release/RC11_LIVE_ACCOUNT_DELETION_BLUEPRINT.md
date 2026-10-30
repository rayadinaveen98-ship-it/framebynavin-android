# Backlot RC11 — live account-data inventory and safe deletion implementation blueprint

**Audit date:** 2026-10-30 (Asia/Kolkata). **No deletion was performed.** This is the user's connected Supabase project `kukkqgpzxnfanynbddiw` (project name `FrameByNavin Creator OS`), matched to the Android source's Supabase URL. Only schema, function definitions, bucket metadata and security-advisor metadata were inspected. **No user identities, recordings, record payloads or tokens were read**.

**Scope:** Documentation + CI only on a new draft branch based on RC10 commit `42ea7905b482c975d950fc988c08587aacbf1814`. Build267 application source and APK remain unchanged. No database migration, Edge Function, storage deletion, account deletion, private signer, policy publication, Play form submission or merge to `main`.

## Live Supabase facts — verified via metadata-only reads

| Component | Current observed behavior | Deletion implication |
| --- | --- | --- |
| `public.creator_profiles` | RLS enabled, user_id foreign key to `auth.users` with **ON DELETE CASCADE** | Contains profile/username/avatar metadata; **not** removed by current sync-data deletion function |
| `public.creator_sync_snapshots` | RLS enabled, user_id foreign key to `auth.users` with **ON DELETE CASCADE** | Stores Creator snapshot payloads; linked heads require careful deletion order |
| `public.creator_sync_heads` | RLS enabled, user_id foreign key to `auth.users` with **ON DELETE CASCADE**; composite reference to snapshots with **ON DELETE RESTRICT** | Delete heads **before** snapshot history; do not rely on FK cascade ordering alone |
| `public.delete_creator_sync_data()` | **INVOKER** function using `auth.uid()`; deletes matching heads then snapshots only | **Not a complete app-account deletion**. Does not remove profile, Auth user or storage objects |
| Supabase Auth | Users represented by `auth.users`; client uses Google ID token through Supabase Auth | Full account termination requires server-side trusted Auth Admin deletion, not publicly callable client RPC |
| Private bucket `creator-voice-ideas` | Exists; `public=false`; owner-oriented RLS policies for Storage objects; Android object paths begin `<auth-user-id>/...` | Enumerate own objects and remove through **Supabase Storage API** before Auth deletion; metadata SQL deletion alone is insufficient |
| Public bucket `v131-build-assets` | Exists; `public=true`; appears to hold product/review assets, not documented as individual Creator voice recordings | Review whether any user-controlled assets exist; do not indiscriminately purge shared public assets |
| Security advisor | One warning: **leaked password protection disabled** | Review under Auth settings; password authentication may be unused in Google-only login, but decide/document rather than ignore |
| Current Android `CloudSyncActivity` | Google sign-in / Creator cloud backup / optional Drive backup flows; no verified full-account deletion screen | A clearly named in-app account deletion request route is still required |
| Existing Android `CloudApiClient.deleteCreatorSyncData()` | Calls `/rest/v1/rpc/delete_creator_sync_data` | Must not relabel this as Delete Account |

**References:** [Supabase User Management / deleting users](https://supabase.com/docs/guides/auth/managing-user-data), [Storage API object deletion](https://supabase.com/docs/guides/storage/management/delete-objects), [Google Play account-deletion requirement](https://support.google.com/googleplay/android-developer/answer/13327111).

## What the actual app sends off device

- Optional Google sign-in: Google ID token to Supabase Auth; profile metadata and account tokens handled in client auth workflow.
- Optional Creator cloud sync: project and idea snapshots to Supabase tables; app-owned voice-idea recordings to private `creator-voice-ideas` bucket, under user-scoped paths. Device-local path values are sanitized before uploading snapshots; content remains user-associated Creator data.
- Separate YouTube OAuth read-only analytics/monetary scope with Google; optional Google Calendar provider entries; Firebase AI video evidence and prompt analysis when invoked. These require an additional SDK/runtime privacy/retention check before Data Safety claims.
- Separate local Personal Reminder domain. Its exported portable JSON includes reminder titles/notes and is **not encrypted**. Creator cloud deletion alone does not erase unrelated local Personal Reminders or externally exported files.
- Google account itself is **not** deleted when the Backlot Supabase application account is deleted; provider permissions may require separate disconnect/revoke guidance.

## Proposed app-account deletion journey — **NOT IMPLEMENTED**

1. **Discoverable entry:** `Backup & Recovery → Account → Delete Backlot account and cloud data`, with a separate **Sign out** option and **Delete only Creator cloud backup** action. These actions must not share misleading wording or callbacks.
2. **Backup-first checkpoint:** Explain Creator project + media backup, separate private Personal JSON, external source media and independent recovery. Offer export before deletion, but do not create an endless or coercive obstacle.
3. **Plain-language effect preview:** App account, Creator profile, Creator cloud snapshots, voice recordings, Google-linked Backlot session vs unchanged Google account; local projects and Personal Reminders should not be wiped unexpectedly. Clearly distinguish request submission from completed deletion.
4. **Secure confirmation:** Authenticate current account via fresh server-validated session/reauth, with a deliberate two-stage confirmation. Never accept a client-supplied `user_id` as authority; resolve it from verified credentials on the server. Rate-limit abuse and prevent deleting another user's account.
5. **Request and status:** Create a deletion request with a stable opaque request ID and statuses `requested`, `identity_verified`, `processing`, `completed`, `failed_needs_support` in a secured, server-side internal queue. Avoid marking success on HTTP 202/queue acceptance.
6. **Freeze account-specific writes:** Fence off automatic Creator snapshot uploads and media writes once deletion is accepted. Use server-controlled state, not user-editable JWT `user_metadata`.
7. **Server-side processing:** Enumerate the user's objects under the verified user-prefix in private voice Storage. Delete using the **Storage API** (not SQL on `storage.objects`). Handle pagination, retries and completion checks; quarantine unrelated shared assets.
8. **Database cleanup:** Delete Creator sync **heads first**, then Creator snapshot history, then Creator profile (or confirm explicit FK cascades in a disposable test). Delete only records authenticated to the target user.
9. **Auth termination:** From a trusted server with a private admin credential (never APK or public repo), delete the Supabase Auth user, verify termination and revoke any relevant refresh capability. Existing stateless access JWTs may persist until expiry; choose short expiry or sensitive-operation session validation as documented by Supabase.
10. **Completion evidence:** Verify **zero remaining user-scoped private voice objects, profile, heads, snapshots and Auth identity** with service-only status readback. Return completion to the requestor through a verified channel. Disclose documented retention exceptions and processor backups as policy requires.
11. **Recovery/failure:** Partial deletion errors must remain visible in an internal retry queue with operator alerts. Repeated requests must be idempotent. Never report deletion complete while artifacts remain.
12. **External web request:** Publish a real public `/delete-account` page that **does not require reinstalling the app**; allow verified account deletion requests by email/form/secure link with identity checks, and the same backend processing. The precise URL, verified support route and retention SLA are not yet supplied.

### Required isolation and tests before implementation approval

- Test with **synthetic accounts only** on a **nonproduction clone/staging project**; no live users, snapshots or media.
- Two synthetic users: attempt cross-account deletion / wrong prefix and require denial; ensure sibling user unaffected.
- Storage pagination, transient failures, repeated requests, interrupted processing, and the `creator_sync_heads` snapshot FK restrict relationship.
- Verify opt-out does not touch Personal local reminders or external manually exported backups.
- Validate web requests without app installation and in-app account settings route.
- Test JWT access until expiry vs session revocation, and available support escalation.
- Re-run affected app + migration QA if code changes; **new Build number required** (do not silently mutate verified Build267).
- Recheck Supabase RLS, security advisors and app-account deletion source if backend schema changes.

## Privacy policy release requirements

`docs/release/RC11_PRIVACY_POLICY_OWNER_REVIEW_DRAFT.md` is a **review draft**, not a published policy. Before publication the owner must supply: verified public support contact, legal developer identity, privacy-policy host and deletion-request URL, actual backend/service retention and exception terms, third-party processor/security details, and user-request handling process.

**Do not publish placeholder copy, assert cloud data is encrypted at rest without verification, guarantee deletion time without an operational SLA, or claim a live deletion endpoint exists.**

## Release gates remaining

- [x] RC6/RC7 Build267 automated engineering tests green; RC8 signing mismatch proof green; RC9 PR provenance green; RC10 static Play inventory green.
- [x] RC11 connected Supabase **read-only** schema/Storage/old deletion-function inspection.
- [ ] Stage/develop server-validated deletion pipeline, authenticated and web request routes, complete data deletion with verifiable tests.
- [ ] Owner/legal sign-off public policy, retention, support and Data Safety form.
- [ ] Production private signer and **owner-approved** preview-to-Play migration [#180](https://github.com/rayadinaveen98-ship-it/framebynavin-android/issues/180).
- [ ] Protected `main` [#185](https://github.com/rayadinaveen98-ship-it/framebynavin-android/issues/185), physical-phone acceptance, FSI/FGS special permission review [#187](https://github.com/rayadinaveen98-ship-it/framebynavin-android/issues/187), release approval [#174](https://github.com/rayadinaveen98-ship-it/framebynavin-android/issues/174).

**RC11 does not authorize live deletion, source migration, Play submission or a main merge.**
