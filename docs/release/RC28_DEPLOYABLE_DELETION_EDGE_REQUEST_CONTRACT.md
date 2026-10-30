# Backlot RC28 — deployable Supabase Edge Function request/status contract (NOT deployed)

**Scope:** Real deployable TypeScript entrypoint at `supabase/functions/backlot-account-deletion/index.ts`, default disabled. Real local-only PostgreSQL RPC contract at `supabase/account-deletion/rc28-local/edge_request_rpc_contract.sql` and integration with existing trusted RC26 local deletion worker. This is the first shared user-facing Edge-to-DB request transport, not a production deletion service. Do NOT merge/deploy/activate without owner approval.

## Concrete integration

1. Actual Supabase Edge Function accepts only `POST /functions/v1/backlot-account-deletion` with exact `DELETE MY BACKLOT ACCOUNT` or `GET` to read the caller's own receipt. Supports the standalone Deno local `/` route for CI. No arbitrary user ID, receipt ID or privileged worker command.
2. Supabase Auth `GET /auth/v1/user` verifies JWT against a real Auth instance; the Edge Function relays the **same user JWT + public/anon key** to `public.backlot_request_deletion(p_confirmation text)` or `public.backlot_my_deletion_status()`. No `service_role`/secret keys appear in this endpoint or the compiled Android app.
3. The SQL RPC is `SECURITY DEFINER` with `SET search_path=''`, explicit `auth.uid()`, validated actual `auth.sessions.id` + `created_at within five minutes`, and `REVOKE EXECUTE` from PUBLIC/anon plus `GRANT` to authenticated only. Request + private receipt + server-side DB fence are one transaction; repeated requests for the same account are idempotent. Only owner-scoped receipt fields are returned.
4. The previous RC26 trusted internal worker reads that same receipt from private local DB, deletes the account's Voice files via the real Storage API and DB Creator rows via PostgREST, and deletes Auth identity last. This **is tested locally**, never with a hosted account.
5. Four explicit Edge Function launch gates require literal `true` in runtime environment: `BACKLOT_DELETION_API_ENABLED`, `BACKLOT_DELETION_SCHEMA_REVIEWED`, `BACKLOT_DELETION_STORAGE_FENCE_REVIEWED`, and `BACKLOT_DELETION_OWNER_APPROVED`. They are intentionally **NOT set anywhere in production**. Without all flags the endpoint returns HTTP 503 and cannot access any Supabase account.
6. No browser CORS, redirects, cookies, arbitrary paths or arbitrary user-supplied target IDs. Public website stays a non-operational draft until separate verified web login and reachable HTTPS deletion request path exist.

## CRITICAL: the SQL contract is NOT a production migration

- It references **disposable RC22/24/26 fixture schemas** (`backlot_rc22_ci` and `backlot_rc26_ci`). The script refuses non-local fixture databases. It MUST NOT be applied to the hosted Backlot project. Production will need schema design/backfill, migrations created by the Supabase CLI, user-service-RPC permission review, and guard/Storage policies compatible with the full live schema. Real `creator_sync_snapshots` has more columns than the simplified local fixture.
- Database triggers block database `service_role` rows, but **service-role Storage API writes bypass Storage RLS**. Every privileged writer, signed/resumable upload and concurrent file creation during deletion still requires a real distributed fence. This remains a STOP gate.
- Five-minute Auth session creation is conservative proof of *new sign-in*, not necessarily a high-assurance fresh Google OAuth reauthentication. Real recent OAuth reauth and account recovery, revoke sessions, worker lease, retries/paging and rate limiting remain launch blockers.
- After Auth user deletion the old JWT might remain valid until expiration; status GET can no longer be requested under a deleted identity. External status receipts/identity recovery must be designed before launch.

## Release safety

- Build270 app source, pinned preview signer, `main`, privacy/deletion website, production Supabase database/Storage/Auth and Play Console unchanged.
- CI uses free standard public-repository GitHub Actions Linux runners plus disposable Supabase CLI v2.122.0; Deno v2.5.0. No user data and no paid service.
- Run `deno check` + `deno test` for handler security and the genuine local Edge Function + Auth/SQL RPC + trusted worker with two synthetic users. All run against `127.0.0.1`, no remote Supabase access.
- Do not publish an external deletion request page or claim Play account deletion compliance from this work alone.

Remaining launch tasks: production-compatible migration review, real Storage fence, OAuth reauthentication, durable multiworker locking, public web sign-in/request, in-app link/flows/new APK QA, privacy/contact/retention approval, owner physical-device QA (#174), Play signing (#180), main protection (#185), policy requirements (#187).
