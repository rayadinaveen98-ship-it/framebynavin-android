# FrameByNavin v120 — Production Security & Data Audit

Version: `2.0.0-beta2.2-production-security-data-audit`  
Version code: `120`

This milestone is release hardening only. It does not add product features.

## Production Supabase authorization review

The production Supabase project was reviewed against the schema actually used by FrameByNavin.

### Verified production facts

- `public.creator_profiles` is the only FrameByNavin table in the public schema.
- Row Level Security is enabled on `public.creator_profiles`.
- Authenticated row policies restrict SELECT, INSERT, UPDATE and DELETE to `auth.uid() = user_id`.
- `claim_creator_username(text, text)` derives the target identity from `auth.uid()` and does not accept a caller-supplied user id.
- The username RPC rejects unauthenticated requests, validates the username, uses a fixed `public` search path, and relies on the unique normalized-username index for collision protection.
- Creator projects, ideas, scripts and other private creator-work payloads are not stored in Supabase. Supabase is used for account/Creator ID identity only.

### Finding discovered during the audit

Postgres default table grants were broader than the app needs. In particular, `anon` and `authenticated` had table-level privileges such as `TRUNCATE`, `REFERENCES` and `TRIGGER`. RLS protects row-oriented operations, but `TRUNCATE` is not a normal row-level operation and must not be available to an application client role.

### Production fix applied

Migration `harden_creator_profiles_privileges` was applied to production and is source-controlled at:

`supabase/migrations/20260912034000_harden_creator_profiles_privileges.sql`

After the migration:

- `anon` has no table privileges on `public.creator_profiles`.
- `authenticated` has only `SELECT`, `INSERT`, `UPDATE`, `DELETE`.
- `authenticated` no longer has `TRUNCATE`, `REFERENCES` or `TRIGGER`.
- `claim_creator_username` remains executable by `authenticated` and administrative `service_role`, but not by `anon`/`public`.

The production schema was queried again after the migration to verify these grants.

## Account and Google Drive boundaries

### Supabase session

- Supabase access and refresh tokens are stored locally using Android Keystore backed AES/GCM encryption.
- Account transitions are serialized through `CloudAccountGate` so stale work cannot adopt a newer account generation.
- Cached creator profiles are accepted only when their `userId` matches the currently authenticated session.

### Google Drive Vault

- Creator backup snapshots use the Google Drive `appDataFolder` scope rather than normal visible Drive file access.
- Drive access tokens are process-memory only and are not written to disk.
- A token is scoped in memory to the Google account email that authorized it.
- v120 additionally clears all cached Drive authorization whenever Google/Supabase account ownership changes and when the user signs out. A new account must therefore authorize its own Drive vault.
- Drive restore verifies the snapshot SHA-256 before importing creator data.
- An empty local phone cannot silently create an empty snapshot over meaningful vault history.

## Local creator data durability

The app's own backup system remains authoritative for creator-data serialization:

- versioned backup schema;
- SHA-256 payload integrity validation;
- serialized writes via `CreatorDataGate`;
- durable pre-restore recovery journal;
- automatic rollback/recovery path if restore fails or process death interrupts a restore;
- reminder rescheduling after restore;
- account authentication tokens excluded from creator backups.

## Automated v120 gates

CI must keep all of the following green before v120 can be promoted to validated staging:

- full debug unit tests;
- debug and release lint;
- debug Android-test APK compilation;
- debug installable APK build;
- unsigned release APK compilation;
- release non-debuggable invariant;
- Play Integrity provider in release and debug App Check provider only in debug;
- cleartext disabled and Android auto-backup disabled;
- only the Android launcher component exported;
- Android Keystore AES/GCM session storage invariant;
- Drive token process-only and account-transition clearing invariants;
- source-controlled production Supabase grant-hardening migration;
- backup journal/checksum invariants;
- existing reminder-completion, Finish Project, Gemini Video Autopsy, Voice Quick Idea and Opportunity Engine regression gates.

## Still requires production/device validation

v120 does **not** declare FrameByNavin 2.0 production-ready. These gates require a production-signed build or physical-device exercise and remain open:

- production/Play signing path and final SHA-1/SHA-256 fingerprints;
- Google sign-in and YouTube OAuth from the production-signed install;
- Firebase AI with Play Integrity App Check from the production-signed install, followed by enforcement;
- Android 13/14/15 reminder and permission matrix;
- destructive backup/restore tests including process death and corrupted snapshots on a real install;
- real Google Drive wrong-account, stale-backup and malformed-backup recovery exercises.

No new feature work should bypass these release gates.
