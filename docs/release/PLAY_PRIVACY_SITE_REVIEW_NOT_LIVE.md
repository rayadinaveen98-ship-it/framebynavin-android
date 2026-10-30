# Backlot — Play privacy + account-deletion website draft

**Status: NOT PLAY-READY. NO HOSTED DEPLOYMENT.** Built from the frozen, tested Build270 owner-preview commit `9b21e9631c2a19a44c4bb98787387ee10e428b7e`. This PR is a documentation/static-website phase ONLY; it is not the required production account-deletion integration. No APK/package/signing changes, Cloud DB writes, live Storage or Google Play submission. No chargeable resources are created.

## Files

- `release-site/index.html` — entry page; draft/non-live notice.
- `release-site/privacy/index.html` — clear, mobile-responsive, app-specific preview disclosure.
- `release-site/delete-account/index.html` — honest non-operational deletion status and planned deletion scope. **It cannot accept requests.**
- `release-site/assets/backlot.css` — self-hosted styles, no third-party fonts/analytics/iframes.
- `release-site/tests/test_site.py` — broken-link, no external tracking, no fake form, draft marker, and Android-source truthfulness guard.

## Grounded data statements

These are based on current Backlot source, especially `V269DataPrivacyActivity.kt`, `CloudApiClient.kt`, `CreatorCloudVoiceMedia.kt`, manifest permissions, Personal backup implementation, and `RC10_PLAY_POLICY_AND_DATA_FLOW_REVIEW.md`: offline-first projects/reminders; Creator cloud backups and account; user-initiated YouTube/Google Calendar links; optional AI features; voice input; unencrypted exported Personal Reminder JSON; sign-out is not deletion; original external media outside backups.

**Unverified details deliberately not invented:** data retention timeframes, customer support email, developer legal entity/address, provider-specific onward processing and actual account-deletion operational SLA. User's verified support contact and owner approval are required. The site is not an approved privacy policy, and the external deletion page is not yet a working request pathway.

## What Google Play requires to leave draft

1. **Real deletion endpoint:** Accept an authenticated user's request both in-app and from an accessible web resource (web can permit out-of-app users to submit; cannot require re-installing the app). Provide a request receipt, support/reauth path, durable queue/worker, explicit data retention exceptions and actual account+related cloud data removal. A page saying 'unavailable' **does not satisfy** Google Play account deletion.
2. **Trusted server:** Verified Auth user, recent reauth, no privileged write bypass, durable write fence for Creator DB and real Storage API uploads/upserts, scoped paginated Storage deletion via Storage API, DB heads/snapshots/profile removal, Auth last, recheck for residual data; real two-user staged end-to-end deletion and failure recovery. RC17–24 local CI prototypes are NOT deployed services. Never deploy mock test SQL to production.
3. **Owner-verified contact and policy:** Approve a support/privacy mailbox and legal developer label; decide precise retention exceptions and third-party data/SKD statements after real network review. Do not publish a fabricated contact.
4. **External public URL:** Only after all above, publish an approved page (GitHub Pages is available with GitHub Free for public repos) with HTTPS, no sign-in or app install required merely to find the path, and verify its functional request mechanism from an independent device.
5. **In-app discovery:** Add a prominent user-account deletion link/flow from Data & Privacy with correct account identity, confirmation and backup notice; build a new signed app candidate after code modifications. Do not falsely change Build270's honest 'unavailable' copy before service launch.
6. **Play declarations:** Verify all 12 declared permissions, foreground-service/full-screen-intent use, SDK transmission/retention, exact-alarm fallbacks and actual Data Safety answers. Complete the required Play Console URL and metadata once operational.
7. **Release controls:** Protect `main` (#185), owner phone acceptance (#174), choose safe private Play signing strategy (#180), satisfy Play compliance (#187). Keep consolidated PR #203 unmerged until gates clear.

## Free infrastructure

- Site source is already in a public GitHub repository; GitHub Pages can host static pages under standard Free public-repo allowances. **This phase does not turn Pages on or publish a public website.**
- CI can test static pages on free GitHub-hosted standard Ubuntu runners; no custom server, new domain, database, email-service plan or paid Supabase project is needed.
- A functioning deletion endpoint may later require server infrastructure; use available free quota/local integration, avoid new billing, and inform owner before any action that could incur charges.

## References

- Google Play User Data policy: https://support.google.com/googleplay/android-developer/answer/10144311
- Google Play app account deletion FAQ: https://support.google.com/googleplay/android-developer/answer/13327111
- GitHub Pages public repository support: https://docs.github.com/en/pages/getting-started-with-github-pages/what-is-github-pages
- Supabase Storage deletion using Storage API: https://supabase.com/docs/guides/storage/management/delete-objects

## Owner-review acceptance (currently all pending)

- [ ] Developer identity and verified contact approved
- [ ] Final accurate privacy/retention language approved
- [ ] Real authenticated deletion service deployed and verified with full cleanup
- [ ] External web request works after app uninstall
- [ ] In-app deletion request works and reports result
- [ ] Site publicly reachable and linked from app + Play listing
- [ ] Data Safety/permissions declarations accurate and approved

**No web form and no mailto action are intentionally present in this draft, because no verified destination exists.** Do not use these draft pages as final Play Console URLs.
