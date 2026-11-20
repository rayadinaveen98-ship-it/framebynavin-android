# Backlot UX11-D — Premium Sign-up and Log-in Presentation

Date: 2026-11-20
Status: SOURCE-STAGED ONLY, draft/unmerged; no Kotlin compile, Gradle, unit/lint/instrumented/physical QA, APK or release.
Founder sequence: Source-stage each approved change; integrate and test only after explicit ALL DONE.

## 1. Source of truth, baseline and earlier approved intent

- Approved main and stable/backlot-ux01-08-ux10-owner-accepted-20261120 are both commit abbe88984bac16cfafbd990d9cb00ed54cfdd07e.
- This UX11-D branch is based on UX11-C draft PR #456 at f56f79a00fec2ac4e7c33ddf22bc37fa822fff61, stacked on UX11-B #455, UX11-A #454, RX01-F #453 through RX01-A #448. Do not merge UX09.
- Existing verified source is app/src/main/java/com/framebynavin/app/ui/V23AccountOnboarding.kt. It owns Credential Manager Google ID-token authentication, CloudCreatorAccountPolicy routes (SIGN_IN_REQUIRED, RETURNING_CREATOR, CREATOR_ID_REQUIRED), username claiming and Continue Without Account callback.
- Earlier approved user intent: welcome -> Create account / Log in / Use without account, Google sign-up/login, new accounts finish Backlot display name and unique username, then saved companion + old five-page Creator/Both setup (restored in UX11-C) or Personal-first route. A returning complete account resumes without repeating setup.
- Do not invent email/password, new security providers or promise complete data backup/restore/sync. Sign-up vs Log-in is a PRESENTATION choice, NOT a new Google identity backend.

## 2. Exact user-facing design

### Welcome / account choice (SIGN_IN_REQUIRED, initial entryMode CHOOSE)

- Static original deep ink-black cinematic backdrop, subtle red/amber studio optics, crisp BACKLOT logo and BUILT FOR CREATORS lockup.
- Main headline: A space for what you create.
- Primary brand CTA: CREATE ACCOUNT -> SIGN_UP page.
- Secondary CTA: LOG IN -> LOG_IN page.
- Third CTA: USE WITHOUT ACCOUNT -> original local continuation callback.
- Brief truthful data notice: optional Google, local mode, supported cloud recovery only, no automatic two-device sync.

### Create account / SIGN_UP

- Headline: Start your Backlot.
- Back to all options, and switch to Log in.
- Primary SIGN UP WITH GOOGLE invokes the exact existing CredentialManager -> Google ID token -> CloudSyncManager.completeGoogleSignIn path. No new identity check or guaranteed new-account creation from choosing Sign Up.
- After Google: if the canonical account policy identifies a new/incomplete creator identity, show the existing CREATOR_ID_REQUIRED registration form, now given premium visual style; if recognized returning, proceed through the original returned-account callback instead.

### Log in / LOG_IN

- Headline: Welcome back.
- Back to all options, and switch to Create account.
- Primary LOG IN WITH GOOGLE invokes THE SAME existing Google callback. The authenticated account identity/profile decides whether returning or needs registration.
- No password fields, new password reset, new OAuth clients or secondary session store.

### New creator identity / CREATOR_ID_REQUIRED

- Headline: Make it yours.
- Explicit Google Connected state only if real CloudCreatorAccountPolicy has a session and still needs identity.
- Preserve existing displayName/username state and field value transformation (display name max 40; username lowercased, letters/digits/underscore, max 24; 3-24 length before submission).
- Actual uniqueness, identity and account record writes remain exclusively managed by the original manager.claimUsername(username, displayName) callback. The UI does not claim success before CloudOperationResult.Success.
- Continue into UX11-C restored Creator setup or existing Personal-first flow via unchanged parent onComplete state machine.

### Returning Creator

- Show branded returning screen with a non-claiming progress indicator. The source logic still calls finishReturning() based on real policy plus cached/server state.
- No unconditional profile restore, blind backup overwrite, automatic future sync or new account creation.

## 3. Exact isolated files

1. NEW app/src/main/java/com/framebynavin/app/ui/UX11PremiumAccountEntry.kt
   - Native Compose layout, original still ambient Canvas backing, three entry intents, separate branded account entry/registration/returning screens, accessible min 48dp targets, responsive scroll/IME/safeDrawing, explicit error copy, deterministic static backdrop. No external image/plugin/paid service.
   - All buttons are bound to passed callbacks; no decorative/inert entry options. Google labels reflect whether the user approached from Create or Log in, but do not change credential security semantics.
2. app/src/main/java/com/framebynavin/app/ui/V23AccountOnboarding.kt
   - Retain original Credential Manager, CloudSyncManager, nonce/provider configuration and completeGoogleSignIn, refreshCreatorIdentity, claimUsername, finishReturning and onContinueLocally.
   - Replace ONLY old generic when(route) presentation with UX11PremiumAccountEntry, UX11PremiumAccountIdentity or UX11PremiumAccountReturning.
   - Add local UI entryMode (CHOOSE, SIGN_UP, LOG_IN); switch/back actions clear stale screen errors. One small reliability fix: in-flight busy is remember, not rememberSaveable, so a destroyed Activity cannot restore a permanently disabled CONNECTING state without an active coroutine.
3. This plan and acceptance document.

## 4. Routing/state matrix

| Starting condition | User action | Expected authorized effect |
|---|---|---|
| Unauthenticated first run | Create account | Open distinct premium SIGN_UP page, no network/auth write yet |
| Unauthenticated first run | Log in | Open premium LOG_IN page, no network/auth write yet |
| Unauthenticated first run | Use without account | Execute exact original parent onContinueLocally callback |
| Either Google page | Continue with Google | Existing Credential Manager Google session, id-token backend response; show real errors/retry |
| Google returns existing profile | Returning route | Existing callback resumes saved account, no forced setup |
| Google returns no registered username | Creator ID required | Name and username form, then source-owned claimUsername |
| Username claim success | Complete account | Existing V23 onComplete and parent journey routing, not a second signup flow |
| Google cancelled/failed/offline | Error | Existing error message shown inline, retry enabled after operation |
| Activity recreated | Entry intent | Restore visual choice if possible but never save an in-flight Google busy flag; no duplicate submit |
| Local-only user | New first run | Existing local profile/setup route; no forced Google or implied cloud backup |
| User with earlier chosen Creator/Personal focus | Completes account | Keep same focus and preserved companion/setup routing; no new mandatory gate |
| Returning signed-in owner with incomplete local setup | Finish | Preserve original profile and recovery guards; do not force account recreation |

## 5. Security, accessibility and reliability

- The Auth UI never reads raw Google tokens or credentials, never persists them in Compose state or logs, and never changes CloudConfig, Supabase URLs, OAuth callback/client ID, JWT refresh, account claims, server schema, local encryption, error source or backend.
- No password fields or email address capture. Google choice may select any device account; do not imply an unauthenticated chooser "Log in" can prove an existing user until the verified account-policy response.
- No new permission or manifest changes; Android window/screen privacy handling remains unchanged.
- Static background is intentional on auth (GPU-efficient, stable during Google overlay, reduced-motion safe), and the screen still presents a strong premium dark-red Backlot brand.
- safeDrawing, keyboard IME scroll and minimum touch targets applied to the new UI; no hidden form submit, false loading completion, back button bypass or inaccessible guest option.
- Avoid absolute backup claims. Original Drive recovery gate still controls any restore review after account sign-in; UX09 two-device auto sync is not integrated.

## 6. Planned acceptance tests AFTER explicit ALL DONE (NOT performed)

1. Kotlin/Compose compile of both files and full final stack; lint, all relevant JVM tests and emulator login-screen render.
2. First-run Creator/Personal/Both initial account choice; three clickable paths correct; Back and mode switches; no account created merely by choosing Create.
3. Google Credential Manager login/signup on real Android device using configured Lab backend, success, cancel, no Google account, bad token, rejected auth, offline and retry; never show username form for verified returning profile.
4. New account claim uniqueness, 3–24 lowercase rule, missing username, failed claim, duplicates and retry; no false account-success UI.
5. Returning login with previously completed Creator setup returns Home, preserving local and server-backed identity; partial setup/resume, existing recovery review and Continue Without Account.
6. Confirm no Google client IDs, secrets, backend environment or cloud-sync behavior changed in UX11-D.
7. 320dp / 200% font / tablets, TalkBack, switch control, keyboard/IME, safe insets and screen rotation; brand readability and button hierarchy.
8. Parent UX11-C original five-page setup and UX11-A/B screens still behave correctly after full integration; preserve all RX01 reminders.
9. Source identity, full QA APK SHA and physical testing before owner signs off any Play publishing.

Next: UX11-E Personal Sanctuary Home source stage, UX11-F numeric-free Personal reminder setup, then founder ALL DONE triggers reconciled integration/tests/APK. No main or stable mutation before the explicit gate.
