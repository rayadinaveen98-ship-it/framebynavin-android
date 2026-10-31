# Backlot — In-app account deletion request UX, gated for Play readiness

**Status: DRAFT. Not enabled, not merged into `main`, not deployed, not Play-approved.**

This branch starts from reviewed RC30 local deletion-path work (SHA `825768b9cc4c02dc9e541cdb0f139b7ecc82a0ad`). Build270's production app package ID/version code/signer, live Supabase, website, Play Console, and installed owner data remain untouched.

## What the branch actually implements

- A user-scoped, fixed-endpoint Supabase Edge deletion HTTP client: `GET /functions/v1/backlot-account-deletion` to check account-specific status; `POST` with only the exact `DELETE MY BACKLOT ACCOUNT` confirmation to request deletion. Uses the existing publishable key and current signed-in user's bearer session, **never** a `service_role` key or caller-supplied target account ID.
- Reuses `CloudSyncManager.withFreshSession` to prevent stale token/account-generation races and honor existing token refresh.
- Strict HTTP 200/202 handling, no redirects, bounded response body, tight receipt UUID/status/stage parsing, no raw errors/tokens written to a log, no claim of deletion success on a mere 202 request.
- An in-app Data & Privacy UI with separate Creator/Personal backup warning, explicit phrase entry, second confirmation dialog, current request status, and no automatic deletion of local work, Personal Reminders, external files, the owner's Google account or YouTube channel.
- The new content is **not visible and cannot perform a network call** with `BacklotDeletionReleaseGate.LIVE_REQUESTS_ENABLED = false`. The existing honest preview disclaimer remains fully intact.
- Android API 30 automated checks for receipt-response parsing and old Data & Privacy UI, plus JVM/lint/AndroidTest compilation in a zero-extra-cost public GitHub Actions workflow. No hosted Supabase changes.

## Explicit launch gates — NOT satisfied by this work alone

1. Production-safe deletion request schema + durable queue/worker deployed, reviewed, and verified on actual production-shaped disposable DB, with owner approval.
2. Restrictive Voice Idea Storage/user write fence, privileged service-role/S3 upload isolation, signed/resumable in-flight upload audit and Auth session revocation handled.
3. Real recent Google OAuth verification, explicit irreversible confirmation, recovery/retention rules and account status after Auth removal designed.
4. Public external authenticated account-deletion website accepts requests without the app installed, with approved verified developer contact and privacy/retention text.
5. Enable flag in a **future privately reviewed Android release only**, then re-run Android device/permission tests; verify actual Edge flags, in-app/external status, local files, 503/401 transitions and multi-account safety.
6. Resolve signer migration #180, protected `main` review #185, owner device acceptance #174 and Play policy declarations #187.

Do not flip the gate just to make UI screenshot tests pass. A 503 or unsupported external website cannot satisfy Google Play account-deletion policy. Never deploy the older `rc*-local` disposable SQL fixtures to hosted production.

## QA evidence

- PR is stacked on RC30 rather than merged to `main`. The strict CI source gate permits only this client/UI, privacy integration, isolated Android tests, documentation and workflow/scripts.
- No new app APK / AAB claimed as signed or installable in this phase.
