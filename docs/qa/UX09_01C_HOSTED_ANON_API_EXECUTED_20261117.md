# Backlot Lab UX09-01C — live anonymous REST security negatives (executed)

Date: 2026-11-17. This records execution against **Backlot Lab only** (`vasleafrmsovdbdlrcsw`), not the original Backlot service or an Android APK.

## Precondition
- Founder reported saving Dashboard Data API selections for the two new functions (`public.backlot_ux09_apply_v1`, `public.backlot_ux09_pull_v1`). Earlier screenshots showed selected schemas only `public` and `graphql_public`; `backlot_sync_internal` must stay private.
- These dashboard selections are user-attested; the connected database tool cannot independently read the Dashboard control-plane selection. Live HTTP negative probes below **did** run against the real API.
- UX09-01 hosted Lab migration previously installed exactly once, version `20261117141908`. New `backlot_sync_internal` has four tables and ten authenticated owner policies; both new public RPCs are SECURITY INVOKER, anon EXECUTE denied, authenticated EXECUTE granted.
- Supabase publishable API key already checked in for Backlot Lab client is used only to identify public Lab API calls. No service-role/admin token, bearer sign-in token, private auth credential, or original-production project endpoint.

## Independent executed CI evidence

[GitHub Actions #44104089711](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/44104089711), source PR #433, live HTTPS-only negative probe, job `134111612899` **completed/success**.

- `GET /rest/v1/entities` with `Accept-Profile: backlot_sync_internal`: **HTTP 406 / PGRST106**. Private Data API schema denied.
- Anonymous `GET /rest/v1/rpc/backlot_ux09_pull_v1?p_after_sequence=0&p_limit=1`: **HTTP 401 / 42501**. Anonymous execution denied (function route present enough to return the SQL privilege denial).
- Deliberately invalid Bearer token against the read-only pull route: **rejected**. Do not treat this as proof of signed-user Auth.
- All requests are read-only GET. No new Auth users created and no SQL writes, Android changes, database changes, RLS edits or original production calls.

## Independent connected database post-HTTP recheck

- Internal sync rows: `account_sequences=0`, `entities=0`, `changes=0`, `applied_operations=0`.
- Installed UX09 migration occurrences = `1`.
- Original four Lab public application tables still exist.
- Security advisor again returns only the previously known `auth_leaked_password_protection` warning: https://supabase.com/docs/guides/auth/password-security#password-strength-and-leaked-password-protection

## Scope and next gates

**VERIFIED:** live anonymous denial, invalid JWT denial, private REST schema not exposed, no state mutation, existing Lab schema preserved.

**NOT VERIFIED:** actually signed hosted JWT accepted by the new RPCs, two-account ownership isolation through hosted HTTP, Creator project/idea CAS and idempotency through hosted APIs, live Android Google OAuth/WorkManager, automatic two-device sync, Personal/Spaces/Challenges, full Backup/Restore and Auth/data Delete.

1. Prepare a separate **hosted signed-test-users acceptance gate** with two disposable Lab-only Auth identities and an explicit safe cleanup plan. Never use owner Google account JWT or log credentials/tokens. Do not create persistent test accounts without cleanup capability.
2. Validate authenticated API function visibility in Dashboard (0/9 was pre-toggle state); do not expose any `backlot_sync_internal` tables, grant anonymous execution, or enable original seven RPCs indiscriminately.
3. Next UX09-02: canonical local entity/codec inventory and durable offline queue source only under an isolated draft; Android CI and real device testing remain mandatory.
4. Preserve `stable/backlot-v337-p0-owner-accepted-20261116` and `stable/backlot-lab-v337-owner-tested-20261117`; do not merge cumulative UX drafts or release from this branch.

**Draft PR; no production release/merge.**
