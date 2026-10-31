# Backlot external account deletion — exact-origin browser bridge (DRAFT)

**NOT LIVE. NO WEB SITE CONNECTED. NO ACCOUNTS DELETED.**

This branch is based on the disabled in-app deletion UX PR #211 (commit `84b99abbf0f3e32215111d084bd34928fa645ce4`). The existing Build270 preview APK and package, hosted Supabase Auth/DB/Storage, privacy website, `main` and Play Console stay unchanged.

## Specific blocker addressed

The RC28 Edge request handler rejected ALL browser `Origin` headers, so an independently hosted web account-deletion request would fail browser preflight even if the backend was later approved. Browser JavaScript also cannot set `Content-Length` manually, while RC28 required the header for POST.

This patch introduces an explicitly approved **single HTTPS web origin**, browser preflight for only GET/POST with `Authorization`, and streaming JSON body length enforcement independent of `Content-Length`. The native Android bearer route remains unchanged. Unknown browser origins get 403 with no CORS headers. No wildcard CORS, ambient cookies, server session, privileged `service_role` key, user-id request input, or administrative worker route is introduced. A browser Origin is **not** authorization: each request independently verifies the bearer against Supabase Auth and the owner-scoped deletion RPC.

## Default-off security gates

In addition to the existing four required production server flags, the Edge entrypoint now requires both `BACKLOT_DELETION_WEB_OAUTH_REVIEWED=true` and `BACKLOT_DELETION_WEB_ORIGIN_APPROVED=true` and a single exact HTTPS `BACKLOT_DELETION_WEB_ORIGIN` (no query, credentials, path, or trailing slash) to grant *any* browser origin access.

These flags are **NOT set on hosted Supabase**. No public site is configured. Without web approval, native approved JWT requests can use the already gated API, but the browser path stays 403. No feature or deletion API activation is authorized by these tests.

## To make this a real Google Play-compliant external request

- Choose, approve and publish a real HTTPS privacy/account-deletion website, official developer contact and accurate final privacy/disclosure/retention text.
- Implement browser Google/Supabase OAuth with **PKCE**, approved exact redirect URLs, refreshed authenticated session, and a genuinely fresh/revalidated sign-in before destructive request. No tokens in query fragments, URLs, logs or third-party analytics.
- Verify the browser obtains the same verified Backlot account identity as Android and forwards only a short-lived user bearer to this endpoint. Add no `service_role` client credentials. Test request and status from an actual browser through the real approved domain.
- Finalize production-safe account deletion: private durable queue, stop all user and privileged Storage writes during cleanup (including S3/signed/resumable/in-flight uploads), staged Creator/Storage/Auth deletion, retries and status after Auth is removed, session revocation, legal retention review.
- Verify public external account deletion works even after the app is uninstalled. The existing `release-site/delete-account/index.html` correctly says deletion is unavailable and must not be presented as a live Play-compliant account deletion URL before the service works.
- Resolve Android preview-to-private-Play signing migration (#180), physical-owner acceptance (#174), main protection/integration review (#185), Play privacy/permissions/app-content policy (#187).

## CI

The dedicated GitHub Actions workflow tests the real Deno entrypoint typecheck, native HTTP behavior, exact approved origin, CORS preflight, malicious origins and headers, payload limit with and without Content-Length, disabled service, and real disposable Supabase Auth→RPC→worker integration. Free public-repo actions and local containers only; no live project credentials.
