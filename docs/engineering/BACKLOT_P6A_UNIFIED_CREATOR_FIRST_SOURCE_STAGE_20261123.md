# Backlot P6a — Unified Creator-first Navigation Correction

**Founder authorization:** Remove old compulsory Creator / Personal / Both focus selection and three-question quick setup. Backlot is Creator OS first; Personal OS is an accessible well-being feature inside the same unified app. Restore original five-page setup for all new creators, no repeated onboarding loops.

**State:** SOURCE STAGED CANDIDATE / UNCOMPILED / UNTESTED / NOT ACCEPTED ON DEVICE.

## Source-of-truth
- Repo rayadinaveen98-ship-it/framebynavin-android
- Cumulative source branch feature/backlot-v337-point06a-unified-creator-first-source-after-p6-20261123
- P6 parent aa5f4b395e9be953011fb655f2d0976fbec42a0b
- Stable V337 accepted 815ba69775f75b6e5f25bb838c2c17b4f09c203e unchanged.
- Main unchanged. No tests, CI, APK, merges or releases.

## Single agreed route
P3 Backlot signature welcome -> P4 account choice (Google sign in / create account / local) -> P5 Spark or Mira -> original V18 five-page Creator Setup -> P6 Enter Creator OS -> existing Creator-first Today Home, with Personal OS features available normally.

- Original setup screens retain content type/secondary modes; platforms; production styles; goals/weekly target; reminder and alert permissions/final review.
- The old V393 focus choice and three-question shortcut are completely removed from active source.
- The V393FirstRunQuickStart Kotlin file is deleted. Its old SharedPreferences keys are NOT deleted, changed or migrated; they simply no longer influence routing.
- P6's obsolete QUICK_PROJECT destination has been retired; completing unified setup lands in normal Creator Home, not a forced project composer.
- Settings remains the explicit way to replay the optional guided tour. Automatic guided tour startup after five-page setup has been disabled; lesson code and stored versions remain.
- Existing V23 account authentication, cloud recovery review, P5 guide selection, V18 five-page UI, CreatorProfile normalized durable setup save, and P6 entrance are reused.
- Completed local profiles bypass account/focus/setup screens; the historical incomplete account-entry flag is repaired without erasing saved user data.
- For an external/widget route, P6 animation is bypassed and targeted destination is not replaced.
- Navigation gates are controlled by durable account/setup/profile completion, not Home tab, Personal usage, Quick Capture, idea creation, or clicks.
- The user can still reach Personal Reminders, spaces and challenges using existing Home and Settings entry points.
- No new backend services, account migration, cloud write, subscription, paid dependency or Personal feature redesign.

## Exact source changes
- Modify FrameByNavinV101BApp.kt: remove v393FirstRunFocus preference reads, automatic Personal redirect, quick three-step branch, auto-guided tour trigger; centralize first-run route and use original V18 wizard for all new creators.
- Add P6aUnifiedFirstRunPolicy.kt to expose ACCOUNT_CHOICE, COMPANION, FIVE_PAGE_SETUP and READY.
- Modify P6CreatorEntryTransition.kt: retire no-longer-used QUICK_PROJECT destination.
- Delete V393FirstRunQuickStart.kt: obsolete selectable focus and three-question screens.
- Add P6aUnifiedFirstRunPolicyTest.kt, SOURCE-STAGED ONLY, never executed.
- Original V18CreatorOnboarding.kt and data stores left unchanged.

## Deferred risk / QA gates
1. Verify Kotlin/Compose compilation, dependency search for removed V393 symbols, and editor state before any suite. Do not run until founder directs.
2. Fresh local install: P3 -> P4 local -> P5 companion -> five wizard pages with all inputs -> P6 once -> Creator Home.
3. New Google user: Signup -> Creator ID -> existing recovery review as needed -> P5 -> V18 -> P6 -> Creator Home.
4. Returning Creator completed setup, historical V393 Personal focus saved, incomplete AccountOnboarding flag: launch Home, no repeated guide/setup.
5. Half-finished five-page wizard: can complete from saved profile as before, no separate focus or quick branch.
6. Personal Reminders, challenges, spaces, Quick Capture, Today/Ideas/Studio/Insights, Settings and Home back navigation: no unexpected setup/guide overlays after completion.
7. On Android restart, process death or repeated taps: no repeated P6, no forced creator composer and no duplicate Guided Tour.
8. Widget/deep link and real notification action continue to their intended target without P6 blocking.
9. Offline/crashed Google sign-in and Data/Privacy recovery remain isolated; preserve account-owned data.
10. Check creator setup page accessibility, font scaling, reduce motion and permission handling.
11. After all approved points have been staged, perform targeted test then consolidated device QA only on founder instruction.

**Point 7 not started; production stable/main unchanged.**
