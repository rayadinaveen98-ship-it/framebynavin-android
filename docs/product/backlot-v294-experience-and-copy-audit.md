# Backlot V294 — Whole-App Experience, Copy and Clean-Data Audit

**Status:** INITIAL SOURCE REVIEW COMPLETE; FULL SCREEN/TEXT INVENTORY PENDING. **Document-only planning branch. No application behavior, storage or UI changed.**
**Code baseline:** V292 consolidated signed-owner preview, `69c9e34e66910a871a63853f99c23e716ba0e381`.
**Product direction:** *Quiet cinema*: premium creator tooling with a calm, unambiguous personal workspace. One system, not a collection of visually inconsistent sub-apps.

## Executive decisions

1. **Do not redesign individual screens in isolation.** Inventory all reachable views and all user-visible strings first. A Kotlin UI file does not necessarily represent a live screen; routes and entry points are authoritative.
2. **Action over explanation.** Each surface gets one clear purpose and usually one primary action. Move secondary controls to context menus, expandable details, or Settings. Avoid duplicate labels, tall introduction cards and irrelevant technical narration.
3. **Global Settings, contextual preferences.** App-wide settings are neutral and apply consistently, rather than centering one creator profile or one Personal Reminders workflow. Creator-specific and Personal-specific options belong in named subsections only when necessary.
4. **No demo data on a new installation.** Real datasets start empty. Starter *templates* and contextual suggestions are optional tools, not automatically created user records. Never delete or alter existing owner data merely because its title looks like a sample.
5. **Preserve honest, prominent warnings where consequential.** Backup exclusions, plaintext exports, photo-locality, full-screen permissions, destructive confirmations and sync conflicts must remain findable; relocate/shorten, never conceal or overpromise.
6. **No owner-phone reset, uninstall, database wipe or restore tests in this cleanup.** Data migrations, sample detection and upgrades are tested on disposable emulators; real data remains untouched.

## Verified findings from V292 source (initial pass)

| Finding | Evidence / location | Proposed direction |
|---|---|---|
| Main navigation has five roots: Today, Ideas, Create, Calendar, Insights, plus many fullscreen overlays | `FrameByNavinV101BApp.kt`: `PTab`, `POverlay` | Keep a simple primary shell; audit overlays and duplicate routes before consolidating |
| Personal Spaces exists but is opened by an **unlabelled sparkle icon** in Personal > My day | `V2232PersonalReminderHub.kt` around `v289-open-spaces` | Explicit **My Spaces** labelled entry; no feature-critical icon-only discovery |
| Settings begins with **My Profile** and several deeply nested creator-oriented descriptions | `V144SettingsHub.kt`, `V167SettingsNavigationPolicy.kt` | Rename to **Account**, separate general defaults from creator workflow settings |
| Repeated explanatory microcopy sits under ordinary actions | `V144SettingsHub.kt`, `V289PersonalSpacesScreen.kt`, `V288PersonalTodayCard.kt` | Reduce to concise action labels + conditional just-in-time help |
| Very small user-facing text exists (e.g. 7.5sp, 8.5sp, 9sp) | `V133CreatorHome.kt`, `V18TodayScreen.kt`, `V144SettingsHub.kt` | Enforce shared typography roles and test Android large-font scaling |
| Some About, Data and Personal screens show long repeating scope disclosures | `CloudSyncActivity.kt`, `V289PersonalSpacesScreen.kt`, Settings | Consolidate safety information while preserving all materially necessary backup limitations |
| Fresh installations already avoid auto-creating a weekly creator schedule | `WeeklySchedule.defaultSlots() = emptyList()`, `WeeklyScheduleStore.loadOrSeed()` | Preserve; regression-test first-run empty state |
| Exact untouched historical `Frame Breakdown` demo may be retired | `V230LegacyStarterProjectPolicy.isUntouchedDemo()` | Preserve **modified** examples, real projects and imported data; audit all other initializers |
| A collector also filters legacy post-publish tasks | `CreatorViewModel.kt` references `CreatorPostPublishEngine.isLegacyTask()` | **HIGH-RISK REVIEW:** check all migration predicates before editing or broadening cleanup |
| V292 cloud sync explicitly excludes Spaces, journal metadata, private JPEGs and local music | `V290CloudCoverageDisclosure.kt`, `V292LocalBackupInventory.kt` | Keep exact recovery boundary clear in Backup & Data; do not imply one-tap full recovery |
| Current Personal Spaces contains on-device photo journal, manual check-ins, measurements and metadata-only export | `V289PersonalSpacesScreen.kt`, `V290PersonalPhotoSection.kt` | Simplify layout and copy without changing ownership/privacy or backups |

These are **source-based preliminary findings**, not claims that all screens have been tested on-device.

## Full route and text inventory — audit queue

Every route requires: entry path, exit/back behavior, data source, one-line purpose, title/CTA hierarchy, literal/dynamic copy, icon labelling, empty/loading/error/success/conflict states, destructive operations, accessibility checks and screenshots.

| Audit group | Known routes/screens and key components | Priority |
|---|---|---|
| Entry and onboarding | Launch/welcome, account gate, creator onboarding, guided tour, first use | P0 |
| App shell | Main bottom navigation, Today/next actions, home slideshow, companion, quick capture | P0 |
| Projects | Create tab, new-project wizard, project details, editing, checklist, publication flow | P0 |
| Ideas | Idea Vault, quick idea, voice ideas, idea-to-project | P1 |
| Planning | Content Calendar, Weekly Plan, Release Day, Daily Brief, automation center | P1 |
| Insights | Insights home, YouTube connect, performance rows, video detail, creator progress | P1 |
| Reminders shell | Creator/Personal switcher, reminder hub, reminder inbox, search/history | P0 |
| Personal Today | Header, next reminder, lists, filters, actionable Done/Snooze | P0 |
| Personal editor | Create/edit, repeat, alarm/voice, music, snooze, validation, save feedback | P0 |
| Personal settings | Routine plans, permission readiness, life planner, optional presets | P1 |
| My Spaces | Discoverable entry, list/empty state, create, goal/edit, archive, activity log | P0 |
| Progress journal | Measurement entry, check-ins, day counts, photo picker, comparison, recovery | P0 |
| Alert runtime | Full-screen alert, voice, music mix, notification states, snooze/Done surfaces | P0 |
| Global Settings | Settings home, navigation, appearance, companion, voice, defaults, planning | P0 |
| Account | Profile, creator details, Google login, identity, sign-out | P1 |
| Backup & data | Cloud sync, conflicts, manual export/import, recovery copies, media exclusions | P0 |
| Platform integrations | Google Calendar, YouTube authorization, local media pickers | P1 |
| Widgets | Widget gallery/preview, setup, quick launch, obsolete-widget migration | P1 |
| Help and about | Tour, app version, permissions help, errors and diagnostic wording | P1 |
| Advanced/danger | Start Fresh, data reset gates, backup warnings, irreversible delete dialogs | P0 |

Inventory **visible strings**, **dynamic/generated strings**, accessibility contentDescriptions, system permission prompts, dialogs, snackbars, empty states, restoration failures, error paths, reminder speeches, widget copy, notification text and external deep-link landing states. Inspect actual reachability; retired versioned source files are archived, not counted as live screens until proven reachable.

## Proposed universal information architecture

**Main tabs:** Today · Ideas · Projects · Calendar · Insights (subject to audit; avoid increasing roots). Keep Reminders a clearly labelled global destination available where needed; don't hide key features behind decorative icons.

**Reminder space:** Creator | Personal. Personal defaults to **My day** with a visibly labelled **My Spaces** action. Distinguish reminders (scheduled prompts) from Spaces (ongoing goals/journals) and avoid duplicating their data.

**Settings home** should be short, stable and user-neutral:
1. **Account** — sign-in, profile, connected identity. Actual user profile belongs *inside* this section, not as the layout's organizing principle.
2. **Appearance** — theme, companion, home slideshow and widget preferences.
3. **Notifications & alarms** — permissions, voice, default sound/music, snooze, quiet hours. Present only applicable options.
4. **Planning & integrations** — weekly automation, Google Calendar and YouTube connections.
5. **Backup & storage** — cloud sync, local exports, recovery and precise supported/excluded data scope.
6. **Privacy & permissions** — current access and control, app-level transparency.
7. **Help & about** — concise help, tour, version.
8. **Advanced** — high-risk device-only data reset behind multi-step confirmation. Keep away from ordinary navigation.

Context-specific app settings (e.g. a reminder's repeat/music) remain in that workflow. Global defaults must not silently overwrite saved custom reminder choices. No forced account-specific welcome, name, public channel, cinema schedule or sample project in generic Settings.

## Copy system and examples

**Voice:** calm, professional, clear, human. Avoid internal version names (`V289`), engineering terms (`delta`, `snapshot`, `migration`), excessive caps, cheerleading, emoji decorations, multiple redundant subtitles or motivational filler. Use user-controlled action verbs and state what actually happens.

| Existing copy/surface | Candidate after audit | Rule |
|---|---|---|
| Icon only (`AutoAwesome`) opens Spaces | **My Spaces** | A feature must be discoverable and accessible |
| `Your habits, skills and little wins — in one place.` | No subtitle, or `Track what matters to you.` only when empty | Don't narrate an ordinary screen |
| `Make room for your progress` + another explanatory paragraph | `No spaces yet` / `Create a space to track a goal.` | Empty state: one sentence and one action |
| `Log what you did. Optional measures are saved only when you choose.` | Field label `What did you do?`; optional `Add measurement` | Input copy does the work |
| `Create your own pictures... metadata removed... current backups...` | `Photos stay on this device and aren't backed up.` | Keep critical risk; shorten and place near Add |
| `Backlot, your way` + `Choose what you'd like to change.` | `Settings` | Remove overlapping heading/intro |
| `My Profile` as Settings organizing card | `Account` | Global settings should be universal |
| `DATA, BACKUP & SYNC` / `Keep your Backlot data recoverable` | `Backup & storage` | Never imply full recoverability |
| `On this phone only: 2 Spaces, ... None included... files not verified` | Keep precise affected counts and exclusions in Backup details | Technical warnings belong where risk decisions happen |

Copy is **proposed**, not merged. Audit must decide if deleting a sentence would obscure a permission, data-loss consequence or action.

## Visual system: design tokens before redesign

- Art direction: cinematic depth from composition and imagery, **not gradients/particles/cards/text on every surface**. Dark graphite/cinematic black baseline, restrained ivory, one principal brand accent and purposeful semantic warning/success colors.
- Typography roles (initial target): page title 22–26sp, section title 18–20sp, body/action 14–16sp, secondary 12–14sp. Remove 7–10sp user-facing microtext where legibility suffers; respect dynamic font scaling. These are audit candidates, not unreviewed blanket replacements.
- Spacing from a single 4dp scale (8/12/16/24/32), consistent card radius, 48dp minimum practical tap target for important actions, visible focus and contrast.
- Hierarchy: one dominant surface/CTA, concise secondary controls, grouped settings rows. No stacked explanatory cards or repeated destination descriptions.
- Reusable primitives: AppHeader, SectionHeader, SettingRow, StatusSummary, DataRow, ActionFooter, EmptyState, ErrorState, CriticalDisclosure and ConfirmDialog. Do not create per-screen variants for the same purpose.
- Personal and Creator retain distinct content models but **share** tokens, labels, action placement and accessibility patterns.
- Preserve intentional motion/companion/playback features where useful, but add consistent reduced-motion/silent options and avoid decorative interruptions during tasks.

## Sample-data removal — safety contract

**Never interpret 'remove preloaded data' as 'erase the owner's existing Backlot data'.** Do **not** call `FreshStartActivity`, `clearApplicationUserData`, `pm clear` on owner phones, mass delete accounts, wipe Room tables, or overwrite cloud copies.

1. Enumerate every data initializer, factory preset, example tile, carousel placeholder, seeded calendar slot, demo profile, fake insight/metric and legacy migration. Mark each **persisted default**, **UI-only placeholder**, **opt-in template**, or **user-owned record**.
2. New installs start with zero projects, ideas, schedules, Spaces, measurements and private photos unless the user creates/imports them. Account/setup and legitimate **behavior defaults** (e.g. snooze, theme) are not fake data and should remain.
3. Existing installs: only remove a persisted historical demo when its **entire stored record** matches an audited known seed byte-for-byte/field-for-field. Preserve edited seeds, new IDs, archived drafts, active alarms and any ambiguous state. Log a migration outcome without exposing private content.
4. Audit existing `CreatorPostPublishEngine.isLegacyTask()` filtering carefully; no blanket heuristic deletion of creator work.
5. Optional Templates stay explicit and do not schedule alerts or create projects merely by browsing.
6. Migration acceptance: V287→new and V291/V292→new upgrades preserve real Creator and Personal records, completion history, Space journals, private JPEGs, reminders and account state; fresh-install records genuinely empty; backup scope disclosures remain correct.

## Audit execution and quality gates

### Phase A — Audit only, no app changes
- Build route map from actual `PTab`, `POverlay`, fullscreen destinations, dialogs, settings destinations and Android activities.
- Extract ALL user-facing copy: `Text`, resources, notification channels/alerts, content descriptions, errors, placeholders, templates, widgets and backend-derived copy. Record source `file:line`, current/target state, reuse/retire decision.
- Capture screenshots on disposable test emulator for empty, populated, offline, permission-denied, error, conflict and success states at standard and large-font settings; include compact and tall displays.
- Audit all sample-data creation/migration paths before touch; create a **data-retention matrix** with field-level provenance.
- Output a sortable screen register, copy catalog and severity-ranked findings (P0 function/safety, P1 clarity/navigation, P2 visual polish). Explicitly mark any unavailable/error-only states not observed.
- Gate: **every reachable production route inventoried, every surfaced string classified, and all destructive/migration paths mapped.**

### Phase B — Single shared design and language system
- Establish types, spacing, colors, accessible focus, navigation semantics, component specs and approved string style. Make one reference screen before multiplying changes.
- Freeze component and art-direction rules in repo; screenshots are reviewed against them. Avoid visual drift across development phases.

### Phase C — Implementation in dependency order
- Settings & navigation architecture, including labelled My Spaces and true global Account/Settings.
- Main Today + Projects/Ideas/Calendar/Insights shell.
- Reminders Creator/Personal and Space journals.
- Details, dialogs, errors, backup/disclosures, widgets, onboarding and remaining reachable screens.
- Remove audited fake data from new-install pathways; only conservative legacy cleanup for upgrade.

### Phase D — Regression and owner acceptance
- CI: unit, Compose/screenshot, performance, accessibility, signed-owner Android 15 and supported in-place-upgrade preservation.
- Confirm original user data unchanged, voice/fullscreen delivery unaffected, content saves correctly, and backup claims precise.
- Validate small/large device and text scaling. Review screenshots, copy and tap targets before signing another owner-preview APK.
- A passing emulator is **not** a claim of owner-phone or Play Store production acceptance.

## Tracked review statuses (initial)

- [x] Locate main five-tab shell, overlay enum, Personal and Spaces entry.
- [x] Inspect Settings home/navigation and sample Personal, Today, Home and Backup copy.
- [x] Identify clean-install empty weekly schedule and exact historical starter demo predicate.
- [x] Identify current backup exclusions and high-risk device reset path.
- [ ] Full reachable-route inventory and screenshot set.
- [ ] Complete user-facing string catalog including speech/notifications.
- [ ] Empty/loading/error/conflict and long-text audit.
- [ ] Data initializer and legacy migration provenance classification.
- [ ] Approved app-wide settings tree, navigation wireframes, copy standard and accessibility pass.
- [ ] No-code-change audit sign-off before UI/data implementation.

**Release boundary:** This document is an audit brief and preliminary evidence, not approval to delete any data, claim every screen was audited, install a new build, or ship to Play.
