# Backlot — consolidated implementation blueprint (8 November 2026)

**Status: owner-review PLANNING document, not a feature release or permission to merge.**
**Baseline:** owner-preview Build 321 / Phase 1 source c1e10ae8ff34c63c0e0ab41a5215a075dcf97ff9, integration PR #329. Phase 3 detailed plan is in the updated older master roadmap and draft PR #330 / issue #331.
**Input:** the owner's 13-item "Point-by-point analysis and product decisions" document, the existing Backlot master roadmap, the owner's Build 321 Home screenshot/functional observations, and the Nov 8 phase-order decision.
**Ownership:** the user approves visual direction and final Android results; engineering must not invent acceptance, production readiness, or a verified cloud backup without tests.

## 0. Canonical decisions and changes to the old proposal

- The uploaded 13-item document was a set of PRODUCT DECISIONS, not evidence those 13 features shipped. Its earlier four-phase labels are superseded by the current execution order. Do not confuse old "Phase 3 — UX simplification" with today's "Phase 3 — Home & Daily Experience".
- Keep the familiar phase numbers but intentionally execute: **Phase 1 closure -> Phase 3 Home -> Trust Foundation T (early, before broad creator expansion) -> Phase 4 Creator Workspace -> Phase 5 Planning, Reminders & Spark -> Phase 6 Insights -> Phase 2 Whole-App Visual & Accessibility Polish -> Phase 7 Full Release Hardening -> Phase 8 Beta/Launch/Iteration**. Trust Foundation T's challenge-specific guard begins within Phase 1 and new-data coverage is a gate on each release, not a reason to bypass Phase 3.
- Phase 2 is deferred from feature development by explicit owner request. This NEVER excuses unreadable text, broken accessibility, or bad navigation in any current phase: each edited screen must be fit for release before proceeding. Phase 2 is the FINAL CROSS-APP normalization, not the first occasion any visual bugs are fixed.
- Frozen original Phase 1 scope (#317/#329): unified Personal/Creator reminder shell, Director's Cut black/ivory/REC-red, progressive Personal editor, single-activity 21-day challenge, source/data safety, owner APK. No new features disguised as Phase 1 fixes.
- Director's Cut stays authoritative across all stages. The old animation reference's orange/pink/purple/gold palette is aesthetic inspiration only; it is NOT approved as Backlot's permanent UI palette. Prototype motion within black/ivory/red with optional secondary light only after owner approves real Android evidence.
- No silent data wipe, uninstall-to-upgrade, automatic cloud restore, phony completion/publication, fake analytics, deceptive notification, or deadline based on wishful thinking.
- One real source of truth per entity: CreatorTask, PersonalReminder, single-activity challenge store, separate legacy 21-day planner, Space/Group metadata, publication evidence, and cloud archive version manifests. Existing stores must be migrated additively, not replaced or ambiguously conflated.

## 1. Baseline and owner-document traceability

| Owner doc item | What exists at Build 321 | Remaining requirement and planned location |
|---|---|---|
| 1. Unified Personal and Creator reminder language | Shared hub shell and red design, Android QA passed | Owner visual acceptance Phase 1; broad consistency audit Phase 2 |
| 2. One repeated activity for 21 days | Local single-activity receipts, pause/undo, optional alarm/voice/quiet scheduling and emulator QA | Unified Personal Sync Now challenge coverage Phase 1/T; optional Space/Group link, nuanced streak/catch-up behavior reviewed later under Phase 5; do NOT silently lose legacy V297 planner |
| 3. Show Suggestions and simple new Personal editor | Progressive Show/Hide Suggestions, Save behavior, optional links; Android QA | Owner phone check Phase 1, later comprehensive accessibility Phase 2 |
| 4. Notification Center + Spark Assist | Creator/Personal Activity Center, existing reminder actions and Spark digest | Coherent source labels, unread/read, grouping, actionable history, Spark role Phase 5 |
| 5. Premium New Idea launchpad | Text, record, dictate, expandable details and Idea Vault exist | Full premium capture IA and Android design refinement Phase 4 |
| 6. Replace Dictate orb | The source still has FramePulseOrb | Replace with audio-reactive pulse field/ribbon candidate, performance/permissions QA Phase 4 |
| 7. Major project content workspace restructure | Existing workspace with scripts, research, checklists, assets, deliverables | Real modular Overview/Plan/Content/Production/Activity navigation and safe migrations Phase 4 |
| 8. Spark three visual levels | Existing Spark assets/digest/notifications | Small status, medium notification card, expressive full-screen states with non-intrusive rules Phase 5 |
| 9. Personal full-screen reliability | Eligible ALARM uses full-screen intent with Android permission checks; VOICE/quiet are not automatic full-screen | Reliability test matrix, explicit approved behavior, fallbacks Phase 5; never promise forced full-screen on all devices |
| 10. Audio-reactive reference motion | Some existing particle effects; NOT new approved sound-reactive language | Voice capture motion Phase 4, Spark/alert motion Phase 5, consistency Phase 2 |
| 11. Useful Reminder Groups | Group CRUD, linking and optional group selection on new reminder exist | Usable pause/resume-all, progress, source labeling and safe linking without mutation/rearming surprises Phase 5 |
| 12. Universal Backup / Restore / Delete Cloud Backup | Disconnected Creator/Personal paths, v2 encrypted archive and older Sync Now guard | Trust Foundation T: complete versioned snapshot + review/restore + actual cloud-delete UX, verify all domains |
| 13. Complete Backlot account deletion | Data & Privacy explicitly says deletion unavailable in owner preview | Trust Foundation T: authenticated deletion, backend/local/linked data and verifiable partial failure recovery |

Note that the legacy V297 screen is still a per-day planner and says it creates no automatic alerts. The new Phase1Habit challenge is a separate product model. The future transition must retain and distinguish existing V297 data, not treat both as interchangeable or rewrite 21 days of confirmed history.

## 2. Phase 1 — current owner acceptance and safety closure

**Current facts:** Build 321 has passing integrated Android15 run #42477427141 (172 JVM tests, Android device tests, 14 captured screens) and signed Build320->321 upgrade run #42477427145. This is a review APK, NOT owner-approved stable production. PR #329 / issue #317 remain open, and #327 remains open.

**1A. Owner device QA:** compare both reminder hubs, black/red palette and large-font behavior; create/edit/snooze one Personal and Creator reminder; create one single-activity challenge; confirm Done/Undo/Pause, optional alarm and retained data after restart; inspect existing Spaces, Groups and photos. Record phone model, OS, granted permissions, observed defects, screenshots. Do NOT clear owner data.

**1B. Challenge sync completeness:** replace the old Personal Sync Now guard with honest full challenge-inclusive cloud snapshot semantics OR retain the hard block with explicit documented owner decision and independent encrypted v2 protected path. Cover source zone, day dates, exact Done receipts, paused state, alert settings, old V297 and all impacted records. Restore only after preview/consent; crash-safe rollback. This is an early release-safety gate, not Phase 3 scope creep.

**1C. Closure:** fix only accepted blockers, rerun unit/emulator and owner in-place signing checks, capture real screenshot approvals, freeze the result and record which other features remain unfinished. Never mark universal backup or account deletion complete in Phase 1.

**Exit:** approved phone UI, no critical reminder/challenge data loss, sync limitation either solved or clearly accepted without deceptive success UI, signed upgrade and PR decision logged.

## 3. Phase 3 — Home & Daily Experience (NEXT FEATURE DEVELOPMENT)

This phase was owner-scoped and is already documented in Phase 3 issue #331 / PR #330. Do not restart its requirements under an unrelated design system.

### 3A — Remove ineffective actions; simplify decision area
- Remove redundant **START THIS MOVE** CTA and **START/WORKING** Home button (both currently call CreatorViewModel.startTask).
- Make status READY/IN PROGRESS passive; hide raw PRIORITY 1228 ranking score; retain internal priority algorithm. Replace with meaningful Critical, Overdue, Due Today and concise "Why this" reason.
- Remove duplicate Home MARK STEP DONE/PUBLISHED affordance; preserve explicit stage and publication confirmation in actual project workflow.
- No mutation from simply opening Home. Build working-state and reminder quiet-period regression tests.

**Gate:** no dead/redundant CTA, no project accidentally starts or advances, screen remains compact.

### 3B — Continue Work means open actual work
- One primary **Continue** button opens the selected real project using existing routing, then stage-aware sub-destination only when supported (script/research/production/publish). Deterministic project overview fallback.
- PLANNED does not silently become WORKING; explicit Begin action remains in project workflow and preserves working-quiet reminder behavior.
- Back navigation returns to selected Home project, edit drafts survive; missing/archived/done cases have honest recovery.

**Gate:** one intentional tap opens the correct project, with no duplicate update or wrong workspace.

### 3C — Focus 2.0, no fake activity
- Separate optional Focus session, explicit Start/Pause/Resume/Finish/Discard. User-approved 15/25/45/60 minute presets, not mandatory 25m.
- Persist one active session with project/stage identity and monotonic/wall-time-aware elapsed accounting; retain actual active minutes, pause records, and clear restart/reboot recovery semantics.
- Do not call project advance or publication on timer end; optional "Mark step complete" route goes to explicit project confirmation.
- Include focus session history/active state in a verified backup/recovery scope before owner APK. Do not claim full unified cloud coverage if absent.

**Gate:** background, screen rotation, process kill, reboot, pause/resume, clock changes and no double-count/auto-complete.

### 3D — Smart Next Move without mystery ranking
- Keep existing real project priority, overdue and workflow evidence; show one recommendation and human reason. Up to two alternate actual projects behind optional disclosure.
- Recalculate on task edits/load, avoid stale due labels/priority number, preserve manual project choice.

**Gate:** deterministic ranking, missing deadlines, no project, multi-project, stale and archived cases.

### 3E — Home composition, preserve existing assets
- Greeting and compact Spark; custom slideshow only when at least one image, no blank banner or loss of autoplay/reorder/photos.
- Connected Top 3 above Up Next, no fake analytics when not connected.
- One Continue Work block, optional Focus, visible Reminder Hub, compact creator progress, Weekly Focus behind disclosure, existing Ideas and navigation.
- Reproduce and fix any actual floating-nav overlap on gesture and 3-button systems. Maintain scroll/insets/IME and small-screen usability.

**Gate:** zero/one/multi image, offline/connected, phone size, mixed Telugu/English, font scaling, return navigation and no missing button.

### 3F — Android review/QA release
- Baseline and after real emulator captures for collapsed/expanded Home, planned/working and Focus; owner approves visuals before lock.
- Unit + instrumented QA including unchanged legacy reminder, Space, backups and in-place signed APK. No install over Build321 unless verified compatible signer/version.
- Ship a bounded review APK, get real-phone approval, record accepted and known limitations.

**Exit:** Home reliably gets user into meaningful work; optional Focus tracks true time; no redundant/no-op controls; approved screenshots.

## 4. Trust Foundation T — early mandatory data/security work, BEFORE broader Creator Workspace expansion

The original owner document rates universal backup and real account deletion VERY HIGH. Do not leave either until the final Play Store checklist. Challenge-specific protection begins in Phase 1; each newly introduced data domain needs coverage from its own release.

### T1 — Single understandable universal cloud backup
- Distinct normal-user actions: **Back Up**, **Restore Backup**, **Delete Cloud Backup**. Do not call an ambiguous bidirectional merge "Sync Now" if it can resurrect deleted local records.
- Design a versioned, encrypted, owner-scoped manifest with backup timestamp, account identity, per-domain coverage, integrity hashes and exact counts.
- Include Creator projects, Ideas, scripts/voice references, remnant media scope, Personal reminders/completions, both challenge models, Groups, Spaces/photos/journals, schedules, notification settings, Spark preferences and Focus sessions where portable. State exclusions for OS permissions, device-specific music/URI, external media and YouTube token re-authorization.
- Atomic/recoverable cloud snapshot creation, no false success; explicit backup history. Do not delete the previous verified copy until a new one is durable.

### T2 — Reviewed Restore, no unexpected overwrite
- Show date/account/source/contents/exclusions, data conflicts and replace-vs-merge semantics with a clear confirmation. Default must not silently overwrite, merge, or resurrect intentionally deleted records.
- Use pre-restore recovery escrow, migration/version checks, photo byte verification and deliberate user consent. Test interruption between domains, offline, account switch, duplicate data, restore from old versions and missing media.
- A separate manual export remains until verified replacement; never claim manual Google Drive and Creator/Personal backup paths are equivalent without checks.

### T3 — Delete Cloud Backup
- Delete only the selected cloud backup and related media/metadata, including older revisions covered by stated semantics; clearly say local records and user's Google account are unaffected.
- Verify remote deletion and user-visible result; support retry if partially failed. Do not simulate deletion by merely clearing a local pointer.

### T4 — Complete Backlot account deletion
- Separate account deletion from Delete Cloud Backup, sign-out and app-data reset. Require owner re-authentication and confirmation.
- Delete Backlot-owned server data/backup objects/media/profile/associations, revoke service tokens when possible, and clear local account data by explicit policy only after server deletion is confirmed; handle partial failure without claiming success.
- Document external-provider scope, unavoidable retention obligations, audit receipt and privacy policy / Play Data Safety behavior. Do not promise deleting the user's Google account.

**Exit:** verified end-to-end on separate owner test accounts, cloud/backend/phone probes and old-data upgrade; no lost original records; authenticated account deletion works; ready for non-owner testers. If two physical devices are unavailable, use phone + emulator and document the missing physical-to-physical check without claiming it passed.

## 5. Phase 4 — Creator Workspace, Ideas, Dictation and Motion

### 4A — Workspace information architecture and data continuity
- Five project destinations: **Overview** (goal/status/next step/deadline/reminders), **Plan** (outline, milestones, tasks, checklist), **Content** (script, hooks, titles, references, assets), **Production** (shoot/edit/deliverables/review), **Activity** (actual change/alert/history receipts).
- Rehouse existing CreatorContentWorkspace fields, not duplicate them; preserve script edits, references, recording paths and publication records. No automatic steps from reminders or timer.
- Keep full offline use, revision conflicts, long-script support, undo/recovery, trustworthy save state, and Content/Project stage mapping from Phase 3 Continue.

### 4B — Ideas as a creative launchpad
- Keep simple default title/text field and fast Save. Optional Record, Dictate and category/format presets reveal progressively; tags, reminders, links and notes hide until requested.
- Retain the current recoverable idea, voice source, duplicate detection, delete/archive and idea-to-project conversion. Design visually premium without introducing prompts personal to one user or dead capture modes.

### 4C — Replace Dictate orb and create a consistent audio motion language
- Replace the current FramePulseOrb with owner-reviewed **audio-reactive dot field / Pulse Field** (or chosen wave alternative), restrained cinematic glow, predictable listening/permission/error states and reduced-motion fallback.
- Motion reacts to real input amplitude, never fake listening; low-end device performance/battery, microphone permissions, privacy and screen reader controls tested.
- Adapt the reference's calm negative space and breathing geometry to Director's Cut black/ivory/REC-red. Do not copy imagery or add gold, purple, pink as an automatic new theme.

### 4D — Safe creator flow and QA
- Idea -> project -> plan -> script -> production -> publication -> retrospective. Protect original voice recordings and editable script drafts; existing creator stages and publication sources are authoritative.
- Stage-by-stage owner visual review on real Android captures; repository/CI tests of idempotent conversions, interrupted saves, revision history, wrong-project navigation and offline restore.

**Exit:** one complete real creator journey works with a manageable five-zone workspace; no lost research/script/audio data; approved Idea/Dictate visuals.

## 6. Phase 5 — Planning, Reminder Groups, Notification Center and Spark

### 5A — Calendar / Weekly Plan / Automation
- Clear difference among planned slots, created projects, reminders and confirmed publication. No duplicate generation; idempotent project automation; schedule and task changes never silently rewrite user choices.
- Safe explicit recurrence and workload support only where backed by existing scheduling semantics.

### 5B — Reminder Groups as operational routines
- Existing CRUD and optional Add to Group stay; make operational pause/resume-all and real progress aggregation only after defining whether groups mutate linked reminder settings, with confirmation/rollback/rearm tests.
- Link reminders to a Space/Group without changing reminder identity; optional challenge association must not delete the challenge or fabricated Done.
- Groups remain user-defined universal templates, not personally hardcoded content.

### 5C — Unified Activity/Notification Center
- Real separate Personal, Creator, Challenge and System source labels; actionable/attention/read/history states; priority, grouping and per-domain correct Done/Snooze/Open/Pause/Reschedule where meaningful.
- No fake notification counters, duplicated alerts, fabricated completion or invasive nudges. Separate summary notification from actual delivered OS alert and keep lockscreen details private.
- Preserve existing reminder scheduling, voice repeats/snooze semantics, exact alarm and background reliability; test Android OS notification permission and battery policies.

### 5D — Spark three-level system
- **Tiny** home/status icon; **Medium** actionable center avatar; **Full expression** only for qualified urgent reminder, achievement or chosen interactive moments.
- State based on actual receipts: needs attention, missed/pending, Done and 5/21 progress; no pseudo-emotional claims. User can disable sound/motion/activity suggestions.
- Review Sprite/character continuity, visual quality, animations, quiet hours, privacy and accessibility with owner screenshots.

### 5E — Full-screen Personal/Creator alert behavior
- Understand Android restrictions. Explicit alarm plus granted full-screen access may present lock-screen UI; voice/quiet remain notification fallbacks unless a tested, policy-compliant permitted path is approved.
- Test phone locked/unlocked, screen off, reboot, app background/force-stop constraints, Android 11/15, exact-alarm denial, full-screen permission disabled, notification disabled and battery/OEM behavior. Never promise all OS states guarantee takeover.
- Preserve authentic Done, Snooze, Dismiss and bounded voice repeats; an unanswered alert is not a completed habit.

**Exit:** scheduling/reliability proven on physical phone + emulator; group actions are operational without destruction; Spark/Activity Center provides honest, useful behavior with consistent design.

## 7. Phase 6 — Insights & Creator Intelligence

### 6A — Foundational metric truth
- Resolve documented older Phase 1B issues: requested-vs-observed report windows, missing vs zero, date/timezone alignment, request generation/channel ownership, stale caches, and real Studio parity for authorized connection.
- No fabricated dashboards, revenue numbers or fresh-data claims.

### 6B — Complete and comparable content
- Search/pagination beyond top/recent samples; separate Shorts from long videos and editorial category; age-/format-matched comparison, sensible insufficient-data state and honest publication observation timing.

### 6C — Audience, reach, retention and revenue
- Video-level retention and reach when actually available, partial Reporting imports, independent retry and permission states. Correct monetary formulas, channel ownership, currency and real data coverage.

### 6D — Learn and improve production
- Confirmed publication records, long-form plus derivative short links, reflection and one next experiment grounded in observable evidence. Optional consent-based AI with visible data sharing/staleness; offline review still works.

**Exit:** available Insights match source definitions, no false claims from samples or stale responses, and recommendations open relevant actual projects or videos.

## 8. Phase 2 — deferred final cross-app visual/accessibility normalization

- Audit all shipped screens after phases 3–6: consistent Director's Cut black/ivory/REC-red, type, icon family, card/button sizes, error language, motion and Spark presence. Remove unintended gold/purple leftovers without erasing intentional media/content colors.
- 1.5x/2x fonts, TalkBack, contrast, targets, long Telugu/English labels, landscape/narrow devices, animation reduction, keyboard and system gesture insets.
- Compare pre/post **real Android** captures, obtain owner approval; preserve original slideshow images, journals, recordings and saved settings.

**Exit:** no fractured UI language, obstructed core controls or color drift on the entire tested app.

## 9. Phase 7 — Security, recovery, publication readiness

- Re-run universal backup and owner-deletion proof on a release candidate, not by trusting the earlier T milestone; restore both legacy and new data types, confirm media and conflict scope.
- Privacy policy, Android Data Safety form, Play full-screen intent/exact alarm requirements, media/microphone permission, external YouTube consent, offline behavior and minimal collection. Document platform-specific limitations.
- Stable package identity, **production signing transition plan** distinct from publicly exposed owner-preview certificate, signed app bundle, version increments and rollback strategy. Do not assume preview-signature APK is Play release.
- Device regression, cold start, network loss, power/reboot, a11y, performance, disk pressure, backup, account switches, Crash/ANR observation, one-to-two-week acceptance sample when possible.
- No feature creep: fix release blockers only; owner signoff before promotion.

**Exit:** safely installable/upgradeable real release candidate, complete observed user journey and no known high-severity data/security issues.

## 10. Phase 8 — controlled beta, Play launch and operating loop

- Small consented beta after Phase 7 readiness; collect structured UX, delivery/reliability and crash data; tune only what evidence shows.
- Public launch conditioned on Play review/approval, functioning support/privacy/deletion paths and rollback response.
- Iterate on actual first-capture success, Continue navigation, useful completion and recovery—not artificial time spent or spammy reminders. Monetization is a separate opt-in business decision, not a reason to weaken privacy.
- Maintain weekly release records and do not promise background autonomous engineering absent scheduled tools.

## 11. Stage gates and release discipline (apply to EVERY package)

A. **Before coding:** scope card with owner-approved exact changes, old-data impact, interaction map and screen samples. Do not silently revise approved appearance.
B. **During work:** separate GitHub feature branch, tests close to changed logic, migration versioning, fail-closed restore; no second source of truth.
C. **Visual acceptance:** native emulator Android screenshots and core tap paths compared against approved prototype; owner feedback before final UI lock.
D. **Behavioral acceptance:** unit, device, cold start/reboot/process death, true permissions, offline, failure injection, no data loss; do not conflate UI screenshot with passed behavior.
E. **Artifact acceptance:** clean CI, exact commit SHA, test log, signed in-place upgrade baseline, checksum, downloadable APK, explicit non-covered domains, owner physical QA. Do not publish an unverified APK.
F. **Close stage:** update issue checklist and release notes, record known limitations, stop feature creep, and **only then** start next bounded package.

## 12. Immediate next actions

1. Collect owner Build321 physical QA and any screenshots; close original Phase 1 blocker #327 or explicitly accept a non-lossy owner-preview limitation; keep PR #329 draft until owner decision.
2. Present specific native Home 3A/B before/after review and confirm Continue's destination mapping and implicit-start rule. Do not redo entire Home theme.
3. Implement 3A/3B on a NEW branch from a verified integrated baseline, with tests and one owner review candidate; no updates to Build321 branch until Phase 1 disposition.
4. Complete the rest of Phase 3 with Focus backup contract and failure testing. Then execute high-priority Trust Foundation T before broad phase 4 additions.
5. Work through stages 4, 5, 6, deferred 2, 7 and 8 with explicit owner gates.

**Planning refs:** Phase 1 issue #317, sync issue #327, integration PR #329; Phase 3 issue #331 and draft PR #330. All are documentation/source controls, not hidden promises of active background work.
