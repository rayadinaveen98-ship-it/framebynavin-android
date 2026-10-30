# Backlot RC29 — live-schema parity and actual Voice Idea RLS write fence (₹0, local only)

**Draft integration only. No deployment, no migration, no real user deletion, no Play publication.** Frozen Build270 remains unchanged. Source builds on RC28's real Deno Edge Function and private request receipt, verified as of 2026-10-30.

## Why this phase matters

Read-only current live Backlot metadata showed `public.creator_sync_snapshots` has **13 columns** with required `base_content_sha256`, `payload_sha256`, schema/capture/app metadata, counters and device identity. Earlier RC24/28 simplified snapshots did NOT match this full shape. RC29 extends only the disposable local RC24 Creator tables with live columns, CHECKs and constraints, then writes real full-field payloads via PostgREST. The same trigger continues to reject user and privileged database INSERT/UPDATE after a request fences the owner.

Live Supabase `storage.objects` in the actual `creator-voice-ideas` bucket has four permissive ownership policies for SELECT/INSERT/UPDATE/DELETE, but **none fences user writes during account deletion**. Adding another permissive policy is NOT effective: PostgreSQL combines permissive policies with OR. RC29 creates **RESTRICTIVE INSERT and UPDATE policies** for that bucket (AND across applicable policies), using a private verified-owner `SECURITY DEFINER` predicate and a lock (`FOR SHARE`) on the authoritative deletion-fence row. RC28 request RPC writes that row with `FOR UPDATE`, serializing racing database metadata writers with the request lock.

## Exactly what was added

- `supabase/account-deletion/rc29-local/live_shape_and_storage_fence.sql`: a hard-guarded disposable local SQL fixture, exact currently observed Creator snapshot columns and checks, four existing Voice Idea ownership policy shapes and two additional restrictive INSERT/UPDATE policies; no `storage.objects` table mutation/triggers, no production migration.
- `supabase/account-deletion/rc29-local/test_live_shape_storage_fence.py`: actual local Supabase Auth with three synthetic identities (A, B, C); real `creator-voice-ideas` private bucket, signed JWT/Edge POST for A's deletion receipt, user and privileged Creator writes before/after, Storage object upload/upsert/replace/copy/move, missing fence, sibling boundaries and production launch-blocker reproduction.
- Adds a deliberately **over-permissive** local test INSERT policy to ensure RESTRICTIVE policy still prevents A's uploads despite OR'd permissions. The same test verifies B remains functional and third user C with no fence fails closed.
- Reproduces the **unresolved service-role Storage API bypass** by showing that a local privileged request can upload even after A is fenced. There is NO global privileged Storage write fence yet. This is a red **STOP** gate for account deletion production enablement, not evidence that service-role Storage is safe.

## Critical boundaries / limits

- The full-schema exercise matches read-only public Creator table columns and selected CHECK/FKs but **is not a complete replica of production** (other extensions, evolving Supabase schemas, actual RPC function definitions, private service integrations and user data aren't replicated). Audit again immediately before any proposed migration.
- Never execute this SQL on a hosted Supabase project: it explicitly requires the RC22/RC24 disposable fixture. It is NOT a Supabase migration. No existing Storage policy or production database is altered.
- Real Storage service-role writes bypass RLS by design. Stop privileged writers, require a trusted writer gateway, or redesign credentials; test signed URLs, resumable uploads, multipart writes and in-flight uploads. The prototype worker does not automatically protect every real bucket/service.
- Need full production-compatible queue/fence DDL and backfill for all pre-existing users plus automatic registration for new Auth identities; ensure no accidental lockout. Database write-fence plus Storage restriction must deploy as one reviewed transactional plan with rollback and operator monitoring. RC28's recent-session guard is NOT equivalent to high-assurance Google reauthentication.
- Full requested deletion backend with concurrent lease, privileged Storage object removal, secure external web sign-in/request, public privacy/support contact, Android in-app request and owner device acceptance remain blockers #174 #180 #185 #187.

## Safety and cost

Local GitHub Actions only, public-repository standard GitHub runner and local Supabase containers. No newly provisioned hosted project, no billing upgrades. No `main` merge, private-key changes, real Auth deletion, Android APK changes, website deployment or account deletion activation.

Official references:
- https://supabase.com/docs/guides/storage/security/access-control
- https://supabase.com/docs/guides/storage/schema/design
- https://supabase.com/docs/guides/storage/schema/helper-functions
- https://www.postgresql.org/docs/current/ddl-rowsecurity.html
- https://supabase.com/docs/guides/auth/managing-user-data
