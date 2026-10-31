# Backlot Play external account deletion — PKCE web owner preview (DISABLED)

**NOT PUBLISHED · NOT CONNECTED · NOT PLAY-COMPLIANT · NO REAL ACCOUNT REQUESTS.**

This draft is stacked on PR #212, the exact-origin Edge request bridge. It adds a source-level external web account-deletion journey under release-site/deletion-request/ with Google OAuth PKCE, memory-only session, verified Supabase Auth identity, receipt/status read, typed irreversible confirmation and second confirmation. It is NOT linked from the draft public pages or deployed to the internet.

## Safety guards

- The JS config is hard-disabled: enabled=false, approvedOrigin empty and publishableKey empty. The HTML Google sign-in button is disabled as well. Zero real calls when disabled.
- Runtime checks require exact approved HTTPS host, exact /deletion-request/ path, secure Supabase host and owner-approved publishable key.
- PKCE verifier is stored in same-tab sessionStorage only until OAuth callback, expires after five minutes and is removed before code exchange. Auth code is stripped from browser history. Access tokens live only in JavaScript memory, never persisted with refresh tokens. Page reload signs out.
- No client service-role/admin key, arbitrary target user ID, worker route, external tracking, third-party JS/CDN or persistent browser cookies. Requests use explicit user Bearer token, no credentials.
- A user-scoped GET status and POST request only operate through the fixed Edge URL. Server must independently authenticate the user, recent Auth session and owner. A 202 is an accepted request, NOT completed deletion. No deletion request is sent on sign-in.
- First-party JS and CSS, no external fonts or embedded frames. CSP limits outbound traffic to approved Supabase project. Hosting still needs real HTTP security headers and a privacy/data-retention review.
- CI checks SHA-256 PKCE generation, origin restrictions, typed owner confirmation, receipt shape and disabled preview. It does NOT simulate a fully configured real Google provider, actual OAuth redirect allowlist or deployed backend.

## Stop gates before enabling, publishing or submitting to Google Play

1. Owner approves public HTTPS website/domain, actual developer support contact, legally accurate privacy/data retention disclosures and Google Play Data Safety fields.
2. Exact OAuth redirect URI and provider verified in real browser on an isolated test account using PKCE; review account-switching and recent Google reauth, MFA if needed.
3. Deploy production-compatible deletion queue/worker and restrictive Creator + Storage write fences with safely isolated admin credentials; test signed/resumable/S3 and in-flight upload races before allowing any request.
4. Test complete cloud deletion, personal/local data preservation, status after Auth user removal, support recovery and rate limits.
5. Resolve physical owner device acceptance #174, Play signing/migration #180, protected-main integration #185 and policy declaration #187. Enable the site and app path together only after explicit owner approval.

No live data, website host, Play Console or stable Build270 APK has been changed. No paid infrastructure required for this draft.

Official technical references:
- https://supabase.com/docs/guides/auth/sessions/pkce-flow
- https://github.com/supabase/auth/blob/master/openapi.yaml
- https://supabase.com/docs/guides/functions/cors
