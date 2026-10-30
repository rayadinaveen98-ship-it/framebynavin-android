# Backlot RC21 — Storage HTTP contract and live-policy read-only audit

**Scope: CI-only local HTTP contract and a read-only inventory of the live Backlot Supabase project on 30 October 2026. NOT a deployed account deletion backend, not a real Supabase Storage API staging test, no live mutations, and no user data read.**

## Safe project inventory

Supabase project discovery returned three existing projects: the Backlot cloud project (`kukkqgpzxnfanynbddiw`, active), CineRelay (`dnqaejljfzwhsainpdxb`, active), and Movie Newsroom (`figvtzzcavidygsykgfn`, inactive). **None** is a designated isolated Backlot staging project. No billable staging project was provisioned, and another product's project must not be repurposed as a destructive Backlot test.

## Confirmed production Storage gap

Using read-only PostgreSQL policy catalog inspection (`pg_policies`), the private `creator-voice-ideas` bucket has four authenticated-user policies on `storage.objects`: SELECT, INSERT, UPDATE and DELETE. Their ownership check restricts the first object-path segment to `auth.uid()`. This guards cross-account file access.

**However, INSERT and UPDATE do not currently check any server-controlled account-deletion/fence state.** A user could retain a valid session and keep uploading or overwriting Voice Idea files while a future account-deletion worker removes older files. The RC20 PostgreSQL *mock* write-fence is not installed in real Supabase and cannot address this by itself. Production deletion remains unsafe to enable.

Supabase currently reports no deployed Edge Functions in the Backlot project. The existing Creator sync-data deletion RPC only deletes sync heads and snapshots; it is not a complete account deletion path.

## What the RC21 tests actually exercise

1. `storage_http_contract.py` implements a tiny, strict **localhost-only** HTTP client, with paths resembling the Supabase Auth `GET /auth/v1/user`, Storage `POST/PUT /storage/v1/object/<bucket>/...`, authenticated object reads, and `POST /storage/v1/object/copy` / `move` interfaces.
2. Its target validator rejects ALL hosted Supabase URLs, including production and other projects, and rejects loopback origins with unexpected path/query/credentials; an HTTP redirect cannot escape to a hosted project.
3. `test_storage_http_contract.py` runs an in-process **synthetic local HTTP server**, never a hosted Supabase server. Two fake accounts receive distinct fake bearer tokens. The server simulates owner-scoped files and a server-side deletion lock.
4. The 22 tests cover two validated identities, bad tokens, cross-owner writes and reads, normal uploads, replacements, upsert, copy, move, path escaping, bounded payload, redirect rejection, deletion fencing and sibling-account continuity.
5. Existing RC17/18 31 security tests remain part of CI; production Build269, signing checks, Supabase real database, app screens and `main` are unchanged.

## What a real Supabase staging test must add next

- Obtain explicit owner approval for a **separate designated staging project** and its potential cost, or provision a verified disposable local Supabase stack with Auth and Storage services. Never use the three existing application projects for these experiments.
- Create two *synthetic staging Auth users* and a private `creator-voice-ideas` bucket, with a true staging-only deletion-fence table, immutable verified user ownership, a trusted server API and a durable queue.
- Implement and review actual Storage `INSERT`/`UPDATE` RLS policies that incorporate the deletion state in a *trusted, race-safe* way. Validate that RLS checks on upsert, move and copy include both source and destination objects and any Storage API service-role bypass.
- Ensure the actual Creator snapshot RPC, refresh/session tokens, and privileged writes cannot bypass the same deletion fence. The existing Android cloud voice adapter's `x-upsert: true` path particularly requires update-policy checking.
- Test the **real** Auth and Storage endpoints, including a stale-but-unexpired user JWT, multipart/signed and resumable upload if supported, DELETE through the Storage API, storage listing pagination, partial failures and retry to completion. Direct SQL modification of `storage.objects` is forbidden.
- Prove operation ordering and a durable completion receipt after Auth deletion. Implement owner-verified web and Android deletion-request routes only after the real backend works. Review exact Play Data Safety and privacy-policy wording.
- Supabase's 12 October 2026 notice schedules the removal of certain older TLS 1.2 ciphers from HTTP APIs starting 10 November 2026. Keep Android's network stack and HTTP clients supported rather than weakening TLS.

## Explicit non-goals

**Do not merge this as a production account-deletion feature.** These HTTP tests run against a local synthetic server, *not* Supabase's actual Storage implementation. No key, Auth user, Voice Idea, bucket or policy in the connected production project was changed. No new APK is necessary. Release remains blocked by #174 #180 #185 #187.

Official documentation:
- https://supabase.com/docs/guides/storage/security/access-control
- https://supabase.com/docs/guides/storage/schema/design
- https://supabase.com/docs/guides/auth
- https://supabase.com/changelog?types=breaking-change
