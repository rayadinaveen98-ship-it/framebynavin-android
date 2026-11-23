# Backlot Point 6 — Enter Creator OS · Source Staging Ledger

**Approval:** Founder approved continuation to Point 6 after accepting the in-chat Point 5 screen preview. Continue source staging only. No Gradle, Kotlin/JVM, device QA, CI, APK, merge into main/stable, release or Point 7.

**Status:** SOURCE-STAGED CANDIDATE / UNCOMPILED / UNTESTED / NOT DEVICE ACCEPTED

## Authoritative baseline
- Repo: rayadinaveen98-ship-it/framebynavin-android
- Cumulative branch: feature/backlot-v337-point06-creator-entry-source-after-p5-20261123
- Point 5 parent: 6336390ed9b59e46f69ecb2e7550b8bc71b7de3d
- Founder accepted stable V337: 815ba69775f75b6e5f25bb838c2c17b4f09c203e. Stable/main are unchanged.
- No rejected V338 merge, hosted/backend/data modifications.

## Verified source and rationale
- Quick Creator Setup finishes with CreatorOsSettingsStore.completeCreatorSetup and immediately opens the quick first-project composer.
- Legacy five-page Creator onboarding also durably finishes through completeCreatorSetup and existing guided tour policy.
- V131LaunchGate already handles P3 splash independently.
- The historic recurring onboarding bug must not be recreated: the new animation is not tied to app launch or tab changes.

## Staged behavior
1. Add P6CreatorEntryTransition.kt: 2,300ms minimal dark handoff; BACKLOT small label, CREATOR OS typography, thin red line, Your studio is ready. No particle field, logo reconstruction, complex effects or companion redraw.
2. On a VERIFIED successful Creator setup save (only) and no external launch, show exactly once. Quick setup defers the EXISTING quick first-project composer until the transition closes. Older five-page setup uses TODAY destination without extra onboarding mutations.
3. Overlay is composed LAST above all existing shell, coach and first-run surfaces. No Activity, session, backup, permission or cloud changes.
4. Transient in-memory destination is NOT Saveable; process death/relaunch uses previously persisted setup and does not replay.
5. Existing P3 launch, P4 account choice, P5 companion, Creator guided tour, Personal, reminder scheduling, direct actions and notification paths remain intact.
6. Regular timing ends with a short 220ms dissolve into the already-composed Creator shell. Back/tap may skip; finishOnce protects against duplicate callback.
7. Reduced-motion mode shows the static identity and completes after 180ms, rather than running animations.

## Deferred QA and negative cases (do not execute yet)
- Compile-only Kotlin/Compose verification then targeted P6CreatorEntryPolicyTest when founder opens consolidated QA.
- Quick Creator: setup save -> transition -> first-project composer, no double open on repeated tap/back.
- Legacy Creator: completed setup -> transition -> Today and any existing guided tour. Never repeat setup.
- Returning account and completed local setup: no transition on startup, navigating tabs, sign in, or changing companion.
- Personal first-run: no Creator animation until actual Creator setup finishes.
- Widget/deep link targets: bypass transition and preserve intended action.
- Force stop/rotation during transition: safe recovery from durable setup without replay or data loss.
- Visual QA: reduced motion, large fonts, TalkBack, immersive layout, low-end phone animation, status/nav bars.
- Data/privacy regression: no changes to credentials, recovery, alarms, notifications, app database or existing cloud sync.
- Only approved consolidated QA and owner physical acceptance can authorize build, merge, release.

**Point 7 not started.**

## Superseded by founder-approved P6a unified first run (2026-11-23)

Point 6's original QUICK_PROJECT quick-setup destination is retired by P6a. The five-page V18CreatorOnboarding is now the single wizard for new users. After the P6 creator transition, return to Creator Home; the original guided tour is manually replayable from Settings only. Preserve the rest of the P6 code and its one-shot timing. See BACKLOT_P6A_UNIFIED_CREATOR_FIRST_SOURCE_STAGE_20261123.md.
