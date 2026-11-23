# Backlot Point 4 — Account Entry Source Staging Ledger

**Founder approval:** Explicit approval for one-time initial account choice after welcome, separate minimal Sign in and Sign up, Continue locally only on the initial choice, existing account routing and non-destructive safeguards.

**Status:** SOURCE-STAGED / UNCOMPILED / UNTESTED / NOT USER-ACCEPTED.
**Prohibition:** Do not run tests, build an APK, merge, release, or start Point 5.

## Source of truth
- Repo: rayadinaveen98-ship-it/framebynavin-android
- Branch: feature/backlot-v337-point04-account-entry-source-after-p3-20261123
- Parent cumulative Points 1–3: 1532da18d6d60b03205afbc6901f00bd74dbb764
- Original owner-accepted V337 baseline: 815ba69775f75b6e5f25bb838c2c17b4f09c203e
- Rejected V338 source NOT merged. Stable and main untouched.

## Scope and architecture
- Existing V23AccountOnboarding is restructured into five visual states: CHOICE, SIGN_IN, SIGN_UP, ACCOUNT_REVIEW, CREATOR_ID.
- Choice page appears only for new users without a completed account entry or completed legacy Creator setup, immediately after P3 welcome and before V393FirstRunFocusScreen. It alone contains Continue without account.
- Sign in contains only Back and Google sign-in. If authenticated identity has a complete matching Backlot Creator ID, the existing returning-account completion path resumes. Otherwise a distinct ACCOUNT_REVIEW screen explicitly offers Create account; never silently creates the Creator ID.
- Sign up contains only Back and Continue with Google. Already-completed Google/Backlot accounts resume instead of duplicating the Creator ID. New/incomplete profile needs explicit Creator ID creation after Google identity verification.
- Creator ID form retains existing display name, 3–24 ASCII lowercase letters/digits/underscore rules, server-side uniqueness, existing CloudSyncManager.claimUsername operation and its errors.
- One-time account choice completion uses the existing synchronous accountOnboardingComplete flag. Focus choice no longer marks account entry complete; it only stores the focus. Returning users who already completed local setup are grandfathered through without forced account registration.
- Existing Google CredentialManager, CloudSyncManager.completeGoogleSignIn, CloudCreatorAccountPolicy, profile matching, saved session, recovery Gate, Guide/Spark/Mira selection, Creator setup and app routing remain in place.
- Google identity token exchange may create a provider identity even from the sign-in screen; this UI does NOT promise strict account existence gating on the Supabase identity level. Only an explicit Creator ID can complete Backlot account setup.
- Errors, invalid creator usernames, cancelled sign-in and network refresh failure remain on the relevant screen without erasing entered text or local data.
- The account section in Profile and Settings is not redesigned by Point 4.

## Changed source
- Add P4AccountEntryPolicy.kt for explicit auth intent / screen navigation policy and returning legacy guard.
- Change V23AccountOnboarding.kt only for the visual flow and existing CloudSyncManager integration.
- Change FrameByNavinV101BApp.kt only at first-run account/focus gate and protect completed legacy setup.
- Change V393FirstRunQuickStart.kt copy so account choice comes first.
- Add P4AccountEntryPolicyTest.kt (staged but not run).
- No new hosted auth endpoint, database migration, credentials, SDK or paid service.

## Deferred QA: owner approval required
- QA0: static diff and account/session security review; confirm no production service, scheduler or cloud data modifications.
- QA1: focused Kotlin compile and Compose resource check, then only targeted P4AccountEntryPolicyTest; correct exact faults without re-running unrelated full JVM tests.
- QA2: Android disposable accounts: local choice; Signup to Google to Creator ID; Signin for existing profile; Signin with missing profile enters explicit review; Signup with existing account returns; Google cancel/retry; wrong username server conflict.
- QA3: Recovery and migration: existing full setup without account flag, legacy partial setup, returning session expired/refresh and offline device; no unnecessary account choice after returning.
- QA4: lifecycle/back navigation; keyboard; screen reader, touch targets, font scaling; no secondary local links on Signin/Signup or username page.
- QA5: ensure existing project/idea/personal history, cloud sync and backup account ownership unaffected. Do not erase owner records.
- QA6: owner-approved consolidated QA, signed APK and physical acceptance. Not executed at source stage.

**No CI, JVM tests, device tests, APK builds, merge or release performed by Point 4 staging.**

## Superseded navigation note (P6a, 2026-11-23)

P4 account choice still appears after the P3 welcome, but no longer leads to V393FirstRunFocusScreen. P6a removed that screen and the V393 three-question shortcut entirely. Account choice now leads to P5 companion selection and the original V18 five-page Creator Setup. The P4 authentication and session protections remain unchanged.
