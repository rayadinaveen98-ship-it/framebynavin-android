# Backlot — Build273 Play submission readiness package

Updated: 31 October 2026. Status: **PREPARATION VERIFIED, NOT ELIGIBLE TO SUBMIT OR PUBLISH YET**.

## Verified app baseline
- Full Build273 `com.framebynavin.app`, versionCode 273, targetSdk 36, minSdk 26.
- Android 11 phone and Android 16 tablet integrated CI and full package build green in GitHub Actions run 41206522435. The Build273 package is signed with the PUBLIC prototype preview certificate, NOT private Play signing. Never upload this APK/AAB to Google Play.
- Build273 contains permission-checked Personal Alarm FSI, denied-access notification fallback, and generic private title-free locked presentation; actual physical Android OEM behavior is not verified.
- Candidate branch carries default-disabled in-app account deletion UI and static policy/PKCE website drafts. This integration does not alter account data, Creator/Personal schema, remote Supabase or installed owner APK.

## Public services still missing / hard stop
1. **Google Play Console developer account**: owner has not registered. Play Console sign-up, identity/payment and selection of account type must be done by the owner; do not claim uploaded, reviewed or approved.
2. **Approved developer support email**: not provided. Never publish a fabricated contact or personal data.
3. **Public live HTTPS privacy URL**: Cloudflare Pages Free site source is available, but not deployed or legally approved. Current pages have `noindex,nofollow` and explicit draft notices. Do not submit them as final Play disclosures.
4. **External account deletion service**: web PKCE path exists but is disabled. Google's policy accepts an operational staffed support-email or form route; merely displaying a disabled page is not sufficient.
5. **In-app deletion route**: Android UI is gated OFF; must only be enabled or replaced with a clear manual deletion-request link after verification of actual request handling.
6. **Functional deletion of account and associated data**: trusted workflow must authenticate the requester and remove eligible Supabase Auth account, Creator profiles, sync head/snapshots, app-owned private Voice Idea Storage objects, and prevent concurrent writes. Backend draft PRs #208/#209 have not been deployed. Do not enable destructive endpoints without disposable-account end-to-end validation and explicit approval.
7. **Private signing**: Build273 public preview certificate cannot be a production signing/upload key. Current Gradle `verifyPrivateReleaseSigning` fails closed if an external private keystore isn't provided. Owner must decide final Android application ID and data-preserving migration path BEFORE making a Play listing. Android normally will not update a locally installed prototype-signed app with a different Play signer, even when package name is the same.
8. **Physical owner test**: exporter backup/restore both Personal+Creator, lock screen/FSI allowed and denied, alarm audible, voice repeats, Doze/boot/OEM management, actual widgets and user authentication. CI emulators do not prove Samsung behavior.
9. **Play declarations**: final Data Safety after real SDK/network tracing; targeted FGS mediaPlayback, FSI permission declaration and denial fallback, exact-alarm permission justification, calendar/mic runtime consent, AI/cloud transfer disclosures, content rating, target audience, ads and pricing.
10. **Google OAuth config**: verify Google Sign-In and any Calendar/YouTube OAuth client SHA-1/256 are registered for **the Play app signing certificate** and final package ID (separate from the private upload key certificate).

## Developer-facing materials prepared on this branch
- `docs/play/STORE_LISTING_DRAFT.md`: Play listing copy under review.
- `docs/play/DATA_SAFETY_EVIDENCE.md`: data collection and permission evidence matrix; not a console submission.
- `docs/play/FGS_FSI_REVIEW_SCRIPT.md`: physical demonstration checklist.
- `release-site/`: static site with disabled draft privacy/deletion request flow and zero-network test.
- `V269DataPrivacyActivity`, `V270AccountDeletionContent`, `BacklotAccountDeletionApi`: in-app request UI added behind compile-time `LIVE_REQUESTS_ENABLED = false`.

## Suggested owner release sequence (no paid hosting needed)
A. Approve final support email and choose Free Cloudflare Pages host.
B. Make a *genuinely working* privacy/deletion request site and authenticated in-app request; perform disposable-account complete deletion (table rows + Auth + Storage).
C. Export and test restoring both Personal and Creator independently. Decide permanent app ID and signing/migration path and provision external key.
D. Get Play Console account, submit developer verification and app setup; provide actual package name and OAuth registrations.
E. Build a **private signed Play AAB**, record exact SHA-256 and bundle cert, run internal testing then any closed-testing track Google requires for owner's account.
F. Review store assets/screenshots, User Data and permission declarations, owner approve before pushing production and wait for Google review. This candidate must remain DRAFT until all checks complete.

## Policy references (verified 31 October 2026)
- Android 16 API36 requirement: https://support.google.com/googleplay/android-developer/answer/11926878
- Account deletion in + outside app: https://support.google.com/googleplay/android-developer/answer/13327111
- Full-screen intent special access: https://support.google.com/googleplay/android-developer/answer/16558241
- Foreground-service policies: https://support.google.com/googleplay/android-developer/answer/13392821

This document is a candid release readiness checklist, not an assertion of Play acceptance.
