# Backlot Build272 Play deletion integration bridge — disabled and data-safe

**31 October 2026 | Draft only | no live account deletion**.

This PR integrates the existing source-reviewed in-app deletion UI from PR #211 with the *real, fully QA-tested* Backlot Build272 source, while preserving current app version and installer bytes. It is not an updated distributable APK, store release, API activation, production credential change or Supabase migration.

## Implemented

- A separate typed `BacklotAccountDeletionApi` uses a fresh, same-user Creator cloud session, explicit bearer user token, fail-closed response schema validation, network response size limit, no HTTP redirects and no service-role credentials.
- `BacklotDeletionReleaseGate.LIVE_REQUESTS_ENABLED = false` remains a compile-time constant. The HTTP request code requires the gate and **never reaches production** in this build. Do not set it true until deletion flow is live/tested, cloud Storage is fenced, real authentication is validated and owner approves.
- Data & Privacy preserves the existing truthful disabled message; only an explicitly reviewed future release could render the confirm/request UI. No false 'account deleted' success, forced sign-out, local wipe or user-data migration.
- Android unit/Compose and UI tests validate strict response states, exact confirmation phrase, and non-functional deletion status on the existing Privacy screen.
- Branch is based on PR #223 Play preflight + complete Build272 owner code, without modifying Personal Reminders, CreatorTask/backup, Android package/version/signer, release site or actual Supabase schemas/services.

## Production critical path

1. Choose an accessible public developer support email address and approved free Cloudflare Pages URL. Never invent one; DO NOT publish private owner credentials.
2. A Google Play-compliant external web deletion resource may use a functional, staffed support email or request form; it cannot simply display a disabled preview. The user must be able to request account and associated data deletion without reinstalling the app: https://support.google.com/googleplay/android-developer/answer/13327111
3. For manual deletion, define a verified same-account identification process and authorized staff workflow that removes Creator cloud snapshots/heads/profiles, app-owned Voice Ideas from Storage, and Supabase Auth identity. Make no claim of automated completion or fixed turnaround until operational. Never delete external Google/YouTube accounts or local Personal records through this cloud workflow.
4. Existing PR #208/#209 backend automation is promising but not deployed: confirm service RLS, Storage policy behavior and worker/Auth-last cleanup on a disposable synthetic account before any production enablement. Live Backlot Supabase inspection: 3 Creator public tables have RLS, zero Edge functions as of October 31.
5. Rehearse physical-device migration from publicly prototype-signed Build272 to private Play signer; never uninstall without verified separate Creator and Personal backup exports. Owner must choose side-by-side package ID versus supervised same-ID transition.
6. Protect `main`, obtain Play Console developer status and approved listing/permissions declarations, confirm real support contact, produce production-signed AAB without uploading signer to public repo, and run Play internal / closed tests as required.

## This bridge MUST remain draft
Do not merge into main or upload to Play. Only code and synthetic Android QA. Release blocker tickets: #174, #180, #185 and #187.
