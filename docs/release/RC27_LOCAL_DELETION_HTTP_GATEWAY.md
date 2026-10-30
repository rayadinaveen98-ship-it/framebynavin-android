# Backlot RC27 — real HTTP request gateway → local Supabase deletion worker

**Status: local integration only, NOT deployed or Play-compliant.** The user requires free tooling only. No real Backlot account, production Supabase policy, public website or Build270 app is changed. This branch descends from verified RC26 #205 (`0beae7666d5b3189778823c82f1fa0f4ec7d530d`).

## Genuine integration delivered

- `rc27-local/http_request_gateway.py`: **HTTP request API**, listening exclusively on `127.0.0.1` and a random port. POST `/v1/account/deletion/requests` requires a signed-in **real local Supabase Auth** bearer and exact confirmation. GET `/v1/account/deletion/requests/current` returns only the currently Auth-verified user's private receipt. `GET /health` explicitly says not deployed.
- Owner identity comes from the real local Auth `GET /auth/v1/user`, **never from request body/query/headers**. The API denies missing/forged bearer, service-role misuse, cross-origin/Host spoofing, unknown or duplicate request fields, oversized/malformed input, unknown methods, untrusted redirects and query parameters. Does not set cookies or CORS wildcard.
- HTTP request is limited to scheduling: an **atomic private receipt + database write fence**. **No privileged delete action is exposed through HTTP.** The internal trusted RC26 worker is invoked separately by test harness, deletes Storage bytes via real Storage API, Creator records via PostgREST and Auth account last. The real `rc27-local/test_gateway_e2e.py` exercises this entire route with 2 synthetic local Auth users, injected worker interruption, retry and cross-account isolation.
- All HTTP status replies use `Cache-Control: no-store`, no user email or account ID. No bearer values or private notes are logged. Private completion receipts live in a schema unavailable to ordinary Auth/PostgREST clients.
- Exact file change allowlist, unchanged Build270/package/private Play signer, and public GitHub-hosted free runner are rechecked in CI.

## Why this is NOT a production endpoint

**Critical production blockers remain.** The server uses the local RC26 worker's test utility, which requires `psql` against a loopback PostgreSQL fixture and a synthetic Creator schema. It is neither deployed nor hosted. Its session verification does **not** prove *recent* Google OAuth reauthentication (a refreshed JWT isn't equivalent to reauth), there is no production web login/session flow, no worker lease/claim, no rate-limit or abuse mitigation, no TLS reverse proxy, no deployable TypeScript Edge Function or storage writer inventory. Auth deletion does not instantly invalidate issued access JWTs. The account deletion worker must be integrated with a durable trusted service and all app/storage write paths must be locked and tested. Never expose RC26 `resume_internal` as a public endpoint.

**Website not connected:** `release-site/delete-account/index.html` from PR #204 intentionally remains draft and non-operational, because linking a public page to a localhost service does NOT satisfy Google Play's external deletion-path requirement. A real public website must enable request submission without app reinstallation, with authenticated account ownership and an approved support/identity recovery path. Both app and web need owner-approved UX before launch.

## Specific next stage to reach launch

1. Review production Supabase schema and design an actual migrations stack compatible with existing `creator_profiles`, `creator_sync_heads`, `creator_sync_snapshots`, private `creator-voice-ideas` bucket, real user accounts and rollback. No migrations have been deployed.
2. Implement deployable server identity/reauth and worker with external web/app route, authenticated request receipt, real delete fence across app writes, privileged Storage abuse protection, queue leasing, paging and deterministic completion.
3. Test a totally isolated production-like environment with two synthetic users, in-flight writes, crash after each step, JWT refresh/revoke and signatures; **no real user destruction** until safeguards verified.
4. Publish an actual functioning external deletion page with verified support contact, final privacy policy and truthful retention plan; add discoverable in-app deletion request. Build a fresh APK for changed app code and rerun full acceptance.
5. Resolve #174 physical phone acceptance, #180 private Play signer migration and #185 protected `main`; prepare Play Data Safety/FSI/FGS declarations. Keep all change PRs draft until approved.

**No paid infrastructure or hosting is provisioned. No Play approval is asserted.**

References: https://supabase.com/docs/guides/auth/managing-user-data ; https://supabase.com/docs/guides/storage/management/delete-objects ; https://support.google.com/googleplay/android-developer/answer/13327111
