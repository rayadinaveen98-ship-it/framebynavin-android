# Backlot UX11-C — Restore original five-page Creator setup

Date: 2026-11-20
Status: SOURCE-STAGED ONLY, draft, unmerged. No Kotlin compilation, unit tests, lint, instrumentation, emulator, physical QA, APK or stable promotion.
Founder contract: source-stage one at a time; integrate and test only after explicit ALL DONE.

## Authoritative baseline and historical comparison

- Owner-approved main and stable/backlot-ux01-08-ux10-owner-accepted-20261120 remain at abbe88984bac16cfafbd990d9cb00ed54cfdd07e.
- Source branch stacks on UX11-B draft PR #455 at 5eb7e55d2bf3a6690f3dd2368e40e545c3786955; UX11-A #454 and RX01-A..F PRs #448-453 all remain isolated drafts. UX09 stays excluded.
- Verified historical pre-UX02 commit f63e6a1f17101088b859f64d788d7eda95039a81: app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt referenced the existing original V18CreatorOnboarding.
- Compared historical and current app/src/main/java/com/framebynavin/app/ui/V18CreatorOnboarding.kt. The original five-page setup remains in the repository; minor subsequent typography, spacing and safe-inset refinements should remain. The new experience bypassed the old screen by routing new users with saved Creator/Both intent into V393QuickCreatorSetup.

This is a RESTORATION OF THE ORIGINAL REAL RENDERER, not a newly invented setup.

## Original setup, exact five screens

1. What do you create most? — Primary Creator mode and optional secondary modes.
2. Where do you publish? — Supported publishing platforms; at least one required.
3. How do you usually create? — Optional production styles, including SKIP FOR NOW.
4. What matters most right now? — Up to three creator goals, primary focus, weekly publishing target.
5. Reminders & alerts — Notification, precise timing, full-screen and battery restriction controls, then saved profile summary and ENTER CREATOR OS.

The existing V149CreatorOnboardingPolicy, V197OnboardingActions and CreatorProfile normalization/validation are preserved. Production styles and displayName are optional for CreatorProfile.isComplete; the original form does not force nonexistent values.

## Exact change

Only the application routing file app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt was edited:
- Within the existing V151SetupPolicy.needsCreatorSetup gate, use V18CreatorOnboarding for new Creator, new Both and legacy partial Creator setups.
- Retain all existing account-vs-setup gating, companion choice, user mode selection, local/Google account sign-in, cloud recovery guard, setting save via settingsStore.completeCreatorSetup, original per-page permission callbacks, and guided-first-run transitions.
- Retain Personal-only first entry. It does not force Creator setup; its existing separate permission and Personal-first guide still work.
- Because original page 5 already explains the four permissions, after successfully saving a NEW Creator/Both profile call the existing committed V393FirstRunPrefs.markPermissionGuideComplete to prevent immediately showing the same separate permissions guide. If that preference cannot persist, leave the original guide as a safe fallback. Do not claim completion until the canonical CreatorProfile actually saved.
- Keep the V393QuickCreatorSetup source file present (no destructive file removal or data migration); it is no longer the normal first-run Creator route.
- Existing V18 five-page renderer is not replaced or edited, and old partially configured users still get the original flow.

## Expected state matrix, to verify AFTER ALL DONE

| User situation | Expected |
|---|---|
| New Creator | Original focus + account choice + companion choice + old five-page Creator setup |
| New Both | Same old five-page Creator setup, followed by existing guide |
| New Personal-only | Personal-specific path; no compulsory Creator profile setup |
| Existing completed Creator | Bypass all mandatory Creator setup |
| Historical partial Creator with no saved first-run intent | Original five-page profile setup |
| Returning signed-in Google owner with complete local profile | No forced profile reset or duplicate setup |
| Personal owner enters Creator intentionally with incomplete profile | Existing gated Creator setup, never automatic fake profile defaults |
| Save fails or app exits mid-setup | Do not flag complete; original form and retry semantics remain |
| Creator/Both completes original permission page | No duplicate separate permission guide after successful completion |

## Untouched invariants

No change to auth, Google Credential Manager, account storage, profile data class/schema, local account, saved data, backend, UX09, schedules, reminders, alert permissions, notifications, full-screen eligibility, 21-Day Challenge, companion identities or Home UI.
No new architecture or paid dependency.
The old setup is preserved with modern compatible spacing and controls, not reverted to an unsafe old APK.

## Deferred certification, not performed now

1. Full Kotlin/Compose compilation and lint; targeted and existing onboarding navigation unit tests.
2. New Creator and Both follow the correct five pages with backwards movement, validation, optional production styles, goal/weekly constraints, save success/failure.
3. Personal-only stays on the Personal route; returning accounts and complete profiles never re-enter Creator setup.
4. Page 5 permission callbacks still launch their existing Android settings/permission requests; no duplicate permission guide for Creator/Both.
5. Verify name, platforms, style choices, prior profile records and account state survive restart and rollback.
6. Keyboard/IME, TalkBack, 320dp, 200% text, device safe insets and tablet.
7. Reconcile UX11 with RX01 stack only after founder ALL DONE. Use full automated regression, physical QA and a traceable signed test APK before Play consideration.

Next: UX11-D premium sign-in/sign-up source stage, then E Personal Sanctuary Home and F numeric-free Personal reminder wizard.
