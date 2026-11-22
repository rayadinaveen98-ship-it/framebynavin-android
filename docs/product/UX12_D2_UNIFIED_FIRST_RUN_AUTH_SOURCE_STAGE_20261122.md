# Backlot UX12 D2 — Unified first run and focused authentication

Date: 2026-11-22
State: **SOURCE STAGED / NOT TESTED / NOT MERGED / NOT RELEASED**.
Owner-approved workflow: stage all UX12 features first; do NOT run per-stage CI, lint, unit/instrumentation/device tests or build an APK until founder explicitly says "Done bro, start next steps and give APK."

## Provenance and strict scope

- Repository: `rayadinaveen98-ship-it/framebynavin-android`
- Stable `main`: `abbe88984bac16cfafbd990d9cb00ed54cfdd07e` at source review (not modified).
- Draft RX01/UX11 integration PR #460: head `5fb8c288c0ceea70db03504b0188cddddd6873d0` (not modified).
- UX12 D1 draft PR #461: source head `6eeee834124f55f16072b785bb37af3f6a536cb1` at D2 branch cut.
- D2 branch: `feature/backlot-ux12-d2-unified-auth-routing-20261122`, stacked on D1 (not main).
- No auth backend, cloud, DataStore reminder storage, alarm scheduling, completion receipts, companion art, or theme asset mutations authorized here.

## User problem

1. Remove the first-run "Make Backlot yours" Creator / Personal / Both decision entirely.
2. Keep the initial account choice screen, where a user can choose Create account, Log in or continue locally.
3. After choosing Sign-up or Sign-in, show only the corresponding Google authentication CTA, an accessible Back action, feedback and relevant account explanation. Do not repeat the opposite account action or local continuation in these focused screens.
4. Avoid forcing Personal-first users through five-page Creator setup. Preserve Creator setup when Creator work is chosen, and maintain returning/legacy routes without erasing preferences.

## Changes made

### `FrameByNavinV101BApp.kt`

- Removed the `V393FirstRunFocusScreen` composition, making `V23AccountOnboarding` the first interactive route for accounts that have not yet completed onboarding.
- Adds `v393UnifiedEntry` persisted via the existing V393 prefs object, only on a genuinely fresh profile with no account onboarding, Creator setup or saved legacy focus.
- Preserves existing legacy focus reading; does not infer or save a fake PERSONAL focus.
- Preserves the original companion-selection gate and original five-page `V18CreatorOnboarding`.
- Routes **new unified entrants** to Personal-first readiness and the existing permission/Personal guide (with previous first-run and completion flags); Creator setup is presented when Creator work is explicitly entered, rather than automatically before Personal.
- Treats Creator tab, composer/Quick Capture and Creator reminder hub navigation as contextual Creator requests when Creator profile is incomplete.
- Creator's existing fifth permissions page marks the duplicate permission guide complete, as before; no duplicate tour after setup.

### `V393FirstRunQuickStart.kt`

- Keeps V393 legacy `CREATOR`, `PERSONAL`, `BOTH` enum and stored preferences for upgrade compatibility.
- Adds `beginUnifiedEntry()` and `unifiedEntryStarted()` using a new boolean preference `ux12_unified_entry` alongside the existing UX02 first-run flag; synchronous commit is required.
- Removes the obsolete `V393FirstRunFocusScreen` composable (not merely hides it).
- Adds pure `UX12D2FirstRunRoutePolicy.shouldShowCreatorSetup()`; legacy Creator/Both and pre-UX12 incomplete null-focus profiles retain their old Creator setup behavior; legacy Personal and new unified entrants access it contextually.

### `UX11PremiumAccountEntry.kt`

- Account choice (CHOOSE) remains: Create account, Log in, Continue locally.
- Focused Sign-up/Sign-in display only **Back** and the correct **Google** CTA. No switch-mode button, local continuation, or redundant disclosure in the focused view.
- The same existing Credential Manager/Google identity flow and backend routing are used for both intents; authentication distinguishes returning profiles and new accounts rather than labels alone.

## Critical behavior matrix to verify once all stages are staged

| Starting condition | Expected path |
|---|---|
| Brand-new local user | Welcome → Account choice → Local → Spark/Mira → Permission guide → Personal-first guide → Personal hub |
| Brand-new Google account | Welcome → Account choice → Sign up → Google → profile claim → companion → Personal-ready setup |
| Existing Google account | Welcome → Log in → Google → existing identity, no new Creator/Personal/Both gate or reset |
| Legacy selected Creator/Both, incomplete profile | Original five-page Creator setup; preference remains intact |
| Legacy selected Personal, incomplete Creator profile | Personal access; contextual Creator setup on Creator work |
| Older incomplete profile, no focus flag, not UX12 unified | Preserve historical Creator setup, do not silently re-route |
| New unified profile, enters Ideas / Create / Creator reminder hub | Original five-page Creator setup, with all prior values and validation |
| Creator setup completed after unified entry | No duplicate permissions tour; existing guided Creator tour eligibility preserved |
| First-run interrupted or Activity recreated | Persist account/onboarding state; no repeated mode gate; no loops |
| Google credential cancelled, network fails or claim rejected | Error visible, no account fabricated, Retry/Back available |
| Saved reminder opens via external launch | Bypass decorative first-run gate where historical route allows; no alarm/notification modifications |

## Deferred, mandatory final QA

- **No tests were executed as part of D2 staging.** Unit, instrumentation and physical-device cases above are acceptance criteria, not passed results.
- Add/assert policy tests for unified new vs legacy Creator/Both/Personal/no-focus routes.
- Compose UI tests: focused Sign-up and Sign-in lack all redundant actions; account chooser still contains all three; Back returns without authentication.
- Android authentication real-account checks: returning vs new Google user, local data preservation, interrupted credential flow and failed username claim.
- Device/size: compact Android phones, owner's Redmi Pad 2, font scale 1.5/2.0, screen reader navigation, landscape/IME.
- Regression: approved logo/welcome D1, existing companion selection, original five-page Creator wizard, Personal-first permissions and guide, authenticated restoration, no new unintended screens.
- Full integrated UX12 build/test/lint/APK only when founder authorizes final QA; P0 Personal Reminder reliability remains a separate mandatory release gate.

## Engineering cautions

- This stage edits the root Compose navigation gate; only subsequent physical/CI verification can certify correctness.
- The unified flag is intentionally separate from the legacy focus enum. Migration is deliberately non-destructive.
- The first-run on-boarding marker is recorded asynchronously by a Compose `LaunchedEffect`. Final QA must include very rapid local continuation, lifecycle interruption and save-failure cases.
- Local-only onboarding does not imply cloud backup/sync functionality.
- Do not create per-stage push/PR test runs. Future stage branches should stack on this branch, with final QA only after explicit approval.
