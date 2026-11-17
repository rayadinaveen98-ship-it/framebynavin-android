# Backlot UX10A — Design System Foundation and Creator Screen Polish

**Date:** 2026-11-17
**Stage:** UX10A **SOURCE-STAGED / DRAFT ONLY**. No Kotlin/Compose compile, Gradle/JVM suite, Android instrumentation, screenshot comparison, tablet/physical device evidence, APK, merge or release.
**Stack base:** UX08F draft PR #422 at `47e785f8a598862154813bfe2ba409e7a5de73ed`.
**Accepted stable V337:** `815ba69775f75b6e5f25bb838c2c17b4f09c203e` unchanged.
**Founder ordering change (2026-11-17):** UX10 is **NOW**; UX09 cloud/multi-device sync is **DEFERRED**, not skipped or considered done. No backend or Supabase cutover/migration authorized by UX10.
**Global release gate:** accepted V337 P0 19-commit/22-path corrective reconciliation remains pending. A final cloud-complete production release cannot be claimed until deferred UX09 also completes and receives its own verification.

## 1. Design audit and strong product choice

### Verified source foundation

- Original Director's Cut dark appearance and Backlot Light appearance are already centralized in `ui/theme/FrameByNavinTheme.kt`. Both use the same approved cinematic/REC-red/ivory/secondary-gold language and UI shape profile. Legacy theme preference IDs are retained for safe normalization. **UX10A preserves all these identities, storage keys and choices**.
- The theme already exposes compatible semantic color aliases (`CinemaBlack`, `CinemaSurface`, `ProjectorIvory`, `RecRed`) and spacing/profile tokens. Hard-coded local fonts/borders in many individual Composables are the actual consistency debt. Do not add another color theme or a disconnected card library.
- Source-grounded density findings: `V22CreatorRewardsUi.kt` contained **31** directly declared font sizes below 10sp; `V15ContextUi.kt` contained **5**. These are too small for a reliable premium information hierarchy at 200% accessibility scaling. The current theme did not explicitly define bodySmall/labelMedium/labelSmall to lock in legible Material fallbacks.
- Previously staged UX08B–F improvements have local 12–18dp shapes, varying 1dp neutral borders and inconsistent page gutters 18 vs 20dp. On both dark and light appearances heavy outlines can visually compete with the content.
- Essential UI elements **must retain visible boundaries**: selected calendar dates, destructive confirmation, alert severity, focus and active states; an invisible border is not appropriate for all widgets. The goal is calmer chrome **without eliminating semantic state**.
- Across ~215 UI files, do NOT mechanically replace every `BorderStroke` or hard-code text to a single size. Distinct reminder/notification/lockscreen, Personal/Challenge, camera/media, companion artwork and accessibility semantics each require screen-owner review.

### Chosen design language

1. **Typography hierarchy:** expressive page title ~23sp, section headline ~14sp, body ~13sp, supporting copy ~11sp, minimum decorative eyebrow ~10sp. Android user font scaling remains enabled; these tokens are not a hardcoded accessibility cap.
2. **Content density:** ~18dp default gutter (16dp compact), 16dp section rhythm, modest 6dp row rhythm, medium 14dp surface radius, 16dp primary card radius, 12dp compact accents.
3. **Visual affordance:** calm neutral card hairlines at **0.75dp + 32% semantic border alpha**, selected/action border retains **1dp + stronger semantic contrast**. Do not change original alarm, destructive error, selection or lockscreen focus visuals in a generic sweep.
4. **Targets:** new shared minimum **48dp**, preferred navigation/shortcut row **56dp**; density never means tiny, inaccurate or untappable controls.
5. **Contrast:** use current theme semantic colors from source, not fixed screen-specific off-white/black overrides. Existing light/dark palette mappings survive unchanged.
6. **No gratuitous motion:** UX08C 3-second approved Spark/Mira celebration and personal/reminder motion remain source-authoritative; UX10 work must audit reduced-motion and interruptions, not add new loops.

## 2. Exact UX10A implementation scope

| File | Source-stage change | Invariants |
|---|---|---|
| `app/src/main/java/com/framebynavin/app/ui/theme/BacklotUX10Tokens.kt` (NEW) | Shared sizes for typography, touch, card density and quiet/active outlines | Read-only presentation constants/functions; no preference/DB/storage |
| `app/src/main/java/com/framebynavin/app/ui/theme/FrameByNavinTheme.kt` | Adds explicit accessible Material `bodySmall`, `labelMedium` and `labelSmall` to existing Director's Cut typography | Original palettes, theme IDs, profile, Android bars and legacy preference read remain untouched |
| `app/src/main/java/com/framebynavin/app/ui/V161QuickActionsHubUi.kt` | Uses shared page title/gutter/section rhythm, 56dp shortcut rows, 112dp prominent capture buttons and quiet neutral borders | Original 12 shortcut tags, three overdue callbacks, Skip confirmation, 48dp minimum controls intact |
| `app/src/main/java/com/framebynavin/app/ui/UX08DDailyBriefPresentation.kt` | Consistent 56dp source-linked project rows, compact card surface treatment, readable support labels | Original priority/CreatorTask actions unchanged |
| `app/src/main/java/com/framebynavin/app/ui/V282ProgressJourneyUi.kt` | Consistent page gutter/title and less heavy outline on historical events | Private Personal Done opt-in, original chronology, 49dp selected calendar cells unchanged |
| `app/src/main/java/com/framebynavin/app/ui/UX08ECalendarNavigationUi.kt` | 56dp Week/Month date targets, 48dp Agenda chips and visible selected indicator | Period navigation, recurrence keys, 90-day horizon and source counts unchanged |
| `app/src/main/java/com/framebynavin/app/ui/V22CreatorRewardsUi.kt` | Raised 31 sub-10sp hard-coded labels to shared 10sp eyebrow, softened seven neutral outlines | Original XP ledger, achievement thresholds, weekly momentum and events unchanged |
| `app/src/main/java/com/framebynavin/app/ui/V15ContextUi.kt` | Raised 5 sub-10sp labels in Daily Brief/Calendar to 10sp, softened two neutral borders | Existing Calendar source, 80-row paging, all callbacks and buttons unchanged |
| `app/src/test/java/com/framebynavin/app/ui/theme/BacklotUX10TokenContractTest.kt` (NEW) | **5 authored / 0 executed** contract tests for touch sizing, typography ordering, density, boundary contrast and existing two selectable themes | Source tests are not Compose/physical visual validation |
| `docs/qa/UX10A_GLOBAL_DESIGN_FOUNDATION_CERTIFICATION.md` | Deferred device/design/accessibility/regression matrix | Documentation, not a test PASS |

No other Kotlin files modified. In particular, no change to `CreatorViewModel`, workflow writers, original Personal/Challenge/Spaces storage, `V406ReminderExperienceUi`, `V131LaunchGate`, `CloudConfig`, Supabase, backup/restore, reminders/AlarmManager, widgets, manifests, app release or accepted stable.

## 3. Next UX10 source waves (each separately scoped/owner audited)

The founder explicitly moved UX10 before UX09. To avoid adding visual debt while pretending 215 screen files are already certified, UX10 is divided into bounded audits and implementation packages:

| Substage | Scope | Acceptance / protection |
|---|---|---|
| **UX10A** | Shared density/typography/surface tokens and Creator progress/Quick Actions/Calendar/Journey/Brief | CURRENT, draft source stage |
| **UX10B** | First launch, signed-out / onboarding / setup, Home dashboard and bottom/top navigation | Preserve approved brand/Spark/Mira, saved onboarding, permission flow, empty/loading/Back; audit screen sizes |
| **UX10C** | Project composer/details, stage transitions, advanced Content Workspace and Idea Vault | No changes to stage mutations, publication, XP, content/asset writer, autosave or draft ownership |
| **UX10D** | Personal Spaces/Groups/Journal, Personal/Creator/21-day Reminder screens, voice/music/snooze presentation | Never interfere with full-screen answer, confirmation, tokens, lockscreen, Android 15 readiness, child/privacy data |
| **UX10E** | Insights, Creator Progress continuation, Settings, notifications center/widgets, cloud/offline/error states | Preserve every settings control; avoid surfacing UX09 as complete; no backend or auth migration |
| **UX10F** | Cross-screen continuity, small/tablet/landscape, TalkBack/reduced motion and UX10 final integration docs | Deferred unit/Compose/physical certification and exact V337 P0 merge strategy; no unverified APK claims |

This table is a **plan**, not proof that UX10B–F are already implemented, audited or approved individually. The design rules are stable: keep approved theme/characters, retain original buttons and semantics, fix hierarchy before effects, make empty/error/loading readable and distinguish proposal from implementation.

## 4. Test sequencing now that UX09 is postponed

Previously the founder approved one consolidated full test after all ten features, to avoid repeated tests. With UX10 now ahead of the still-unfinished UX09:

- Continue authoring **UX10 per-stage test matrices/source tests** while source-staging and perform source-level GitHub ancestry/diff checks, not claim a compiled state.
- **After UX10F** perform an integration-readiness review and make the best scope decision explicitly. A *visual/UX* device pre-certification may be useful; a production candidate still needs accepted V337 P0 integration.
- **Final release-quality cross-device sync acceptance can only occur after UX09** is implemented and integrated. Running a full final release campaign before UX09 would either omit the requested auto-sync capability or need a second campaign. Do not falsely mark UX09 done to satisfy the stage count.
- A founder-approved **non-sync-specific stable build** could be a separate release decision, provided it has full relevant P0/performance/visual/Android acceptance and clear public feature limitations. No such promotion is currently approved.

## 5. Important unresolved findings / stop the line

- 215 UI filenames is **repository size**, not 215 individually inspected/visually validated screens. UX10A sampled concrete high-use source owners; thorough coverage will be tracked as the next owner-scoped waves advance.
- The 10sp minimum does not itself certify 200% font scaling; small Creator reward cards, three metric columns and 7-column date grids may clip. Explicit Android screenshots/TalkBack are required, and split/wrap controls rather than shrinking fonts back to 8sp.
- 0.75dp borders on light theme may be too subtle. Test both light/dark, sunlight/low contrast and keyboard focus. Do not globally remove selected/error/focus outlines.
- Original Creator/Personal database and V337 owner stable are protected; accepted P0 changes remain divergent from UX source branch.
- Existing UX08B timeline decode false-empty risk, UX08D/E Weekly Plan typed-read gap, UX08C reward animation physical evidence and UX08F Skip owner race remain open. UX10 visuals do not fix them.
- UX09 new Supabase project configuration, domain parity, RLS and conflict rules remain NOT STARTED and DEFERRED, with no secret/server/account changes.

**Stage result:** UX10A implementable source diff, 5 authored test cases, no runtime evidence; pending exact stacked draft review and owner continuation to UX10B.
