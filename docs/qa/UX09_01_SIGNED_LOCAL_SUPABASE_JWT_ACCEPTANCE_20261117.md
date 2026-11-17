# Backlot UX09-01 — Actual local Supabase Auth / JWT and REST API security gate

**Date:** November 17, 2026.
**Purpose:** Execute real GoTrue sign-up / signed access-token checks on a disposable local CLI stack (PostgREST), not fake JWT claims or admin SQL. Uses only throwaway synthetic identities.
**VERIFIED EXECUTION:** GitHub Actions [#44093822510](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/44093822510) `completed / success`, job `134075602445`, exact tested source SHA `ec225aaa7e055aa1f604f7966ab6e96600c3bb7b`. CLI `v2.122.1` (v2.stable), local GoTrue `v2.197.0`, PostgREST `v16.5`. Local container resources were acquired successfully despite transient registry rate-limit warnings. No hosted project was linked.

## Actual executable source

- `.github/workflows/backlot-ux09-01-local-supabase-gotrue-contract.yml`: read-only checkout, pinned v2-compatible Supabase CLI, fresh temp project, local startup with unnecessary services excluded. No linked hosted project, Supabase PAT, app secrets or auth keys.
- `tools/qa/backlot_ux09_01_local_supabase_gotrue.sh`: localhost-only (port 54321) and local DB (port 54322) guard; execute real draft SQL on a disposable Supabase database, authenticate two new GoTrue identities and get **issued** JWTs, then run local API tests without printing tokens.
- `tools/qa/backlot_ux09_01_disposable_auth_matrix.sh`: existing real signed-JWT REST calls using the original two authenticated test users; Creator Project and Idea create, duplicate retry, changed-ID rejection, conflict detection, isolated equal UUIDs, pull pagination, edit/tombstone, unsupported domain rejection and non-resurrection.
- Private schema guard: request `Accept-Profile:backlot_sync_internal` directly through REST and require 404/406. Even if RLS protects tenant ownership, allowing generic direct REST writes would bypass CAS idempotency/tombstone integrity. The intended Data API surface is public RPC only, NOT internal tables.
- Unauthenticated/invalid bearer token RPC requests must fail 401/403 (or 404 if permissions conceal endpoints).

## Acceptance list (16 local GoTrue/REST checks passed in run #44093822510)

| ID | Guarantee | Evidence requirement |
|---|---|---|
| JWT01 | Local Supabase CLI v2-compatible stack starts | Actual local GoTrue/PostgREST Docker services, no hosted network project |
| JWT02 | Correct actual SQL schema compiles on Supabase Postgres | Not mock `auth.uid`, internal RLS and RPC grants pass |
| JWT03 | Two distinct user signups yield real signed access JWT | Issued by local GoTrue, no hand-signed JWT |
| JWT04 | Two user JWT subjects differ | `/auth/v1/user` returns distinct IDs |
| JWT05 | Account A Project create/update/delete | CAS exact revision semantics preserved |
| JWT06 | Idempotent duplicate operation ID and tampered retry rejected | Same request receipt returned; changed data 4xx |
| JWT07 | Account B cannot observe Account A's Project/Idea | Signed account-scoped pull checks / absence of foreign titles |
| JWT08 | B cannot update an A-owned entity by matching ID | Conflict with B revision 0, no cross-account state leak |
| JWT09 | A and B may each own identical UUID | RLS scopes ownership; independent sequences |
| JWT10 | Tombstone remains in A's change stream and old client cannot resurrect | Deleted record version persists and stale create conflicts |
| JWT11 | Exact cursor pagination in signed REST | First page and `has_more` consistent with full stream |
| JWT12 | Private schema direct REST requests inaccessible | `Accept-Profile` 404 or 406 |
| JWT13 | Missing/invalid Auth bearer cannot run sync RPC | Strict non-2xx 401/403/404 refusal |
| JWT14 | Private media/Personal domain not silently accepted | Unsupported domain request 4xx |
| JWT15 | Creator Idea round trip works under true JWT | Data in account A only |
| JWT16 | No live Lab schema, account, personal media or stored data changed | Before/after hosted `backlot_sync_internal` absent |

## Exact CI log evidence

```text
PASS: real local Supabase schema and static privilege assertions
PASS: two distinct locally signed GoTrue access tokens issued
PASS: authenticated A create / exact idempotent retry
PASS: operation UUID reuse with different data is rejected
PASS: revision CAS / account separation
PASS: two accounts never exchange payloads
PASS: updated records / tombstone / 1-row page / cursor / B unaffected
PASS: disallowed Personal/private-media domain rejected
PASS: Creator Idea domain applies and pulls
PASS: actual local GoTrue JWTs used for two-account PostgREST test
PASS: private sync tables NOT exposed to REST API
PASS: missing/invalid Supabase access tokens cannot call sync RPCs
UX09-01 SIGNED LOCAL GOTRUE + POSTGREST PASSED; NO HOSTED LAB CHANGES
```

**Meaning of PASS:** real signed local Supabase Auth access tokens exercised the source-backed RPCs in disposable GoTrue/PostgREST, with ordinary account privileges. It did NOT involve the configured Google identity provider, the owner-tested Lab Android app, hosted RLS settings or account deletion.

## Limitations and next gates

- This still is **not Google Sign-In integration testing**, which uses native Google ID-token exchange with Supabase; disposable local email/password GoTrue is used to obtain properly signed Supabase access JWTs without real Google or user accounts.
- This still is **not Android two-device sync**, offline WorkManager, cross-branch source reconciliation, full Backup/Restore, Personal Challenge sync or Backlot account deletion.
- Real GoTrue/RLS success does not prove that hosted Data API settings expose only `public`; hosted schema inspection is required before rollout, with an explicit architecture review.
- The local CLI startup may fail because of unrelated container/image/tool changes; report that as a test infrastructure failure, not a database-security failure.
- No hosted migration, paid organization upgrade, CI repository secret, signed APK, protected V337 change or project data erase is authorized by this test.

**Current gate result:** Local signed-JWT GoTrue/PostgREST tests PASS. Next requires review of exact hosted private-schema exposure and no-write migration plan, founder approval for any actual hosted DDL, and separate Android/Backup/Restore/Delete implementation. No migration or Android change was made.
