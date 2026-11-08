# Backlot improvement master plan

Planning baseline: V183 / 2.3.3 Stable Personal, reviewed source commit `aa9e1aab939b7b075b50f9d5a952c6fb86b901eb`. Revision 2, 18 October 2026: expanded after an end-to-end source review of Insights, including its four active tabs, authorization, API queries, cache ownership, metrics, revenue, reach, retention, recommendations, project links, publication observations and optional AI. This is a proposed implementation plan, not a claim that changes have shipped. Physical-device and authorized live-data validation remain required. The Home slideshow requirement below is unchanged.

## Product goal

Help a solo creator capture thoughts, continue meaningful work, finish outputs and learn from publication with less administrative effort. Preserve the cinematic identity and the familiar project structure. Improve existing systems before adding parallel ones.

## Execution-order decision — 8 November 2026

**Owner direction:** Keep the historical phase numbers and deferred scope, but move broad **Phase 2 — Readability and interaction / whole-app visual polish** to the *end of feature development*, just before Phase 7 release hardening. Once the frozen reminder-focused Phase 1 is fully accepted, the next implementation is **Phase 3 — Home & Daily Experience**, followed by Phase 4 (Creator Workspace), Phase 5 (Planning/Automation), Phase 6A–6C (Insights), Phase 2 (whole-app visual polish), and Phase 7 (release/recovery). Phase 8 is an optional post-launch pilot/iteration operating phase, not an already approved feature release.

**Source-of-truth distinction:** This master plan's old Phase 0/1A/1B analyses still inform data-safety and analytics work but do *not* imply completion of today's dedicated reminder Phase 1. The frozen Phase 1 source is PR #329 / issue #317; challenge cloud Sync Now coverage remains issue #327. **No Phase 3 code, merge, APK version increment, release promotion, or Home visual redesign is authorized merely by documenting this future phase.** Owner will review Phase 3 real-device references before visual implementation, and Phase 1 safety/acceptance gates cannot be silently skipped.

## Locked Insights: professional minimal-content standard (19 October 2026)

This requirement was reconfirmed before V187 implementation. It applies to Overview, Content, Revenue, Creator, video detail, analytics drills, creator recommendations and cross-surface analytics presentations.

- Visible sections must be identified by short, useful names, not explanatory introductions. Use clear labels such as Views, Watch Time, Revenue, RPM, Top Videos, Projects and Retention.
- No heading descriptions, motivational fillers, generic teaching copy, repetitive observations, redundant metrics or technically oriented prose on ordinary screens.
- One authoritative presentation of each metric per context. Show more detail only after intentional drill-down, not through progressively smaller text.
- Clean, consistent type, scale, card dimensions and spacing. Never shrink essential labels or metric values to fit excess content.
- An exception is mandatory for concise **functional** notices: permission/consent, data freshness, partial/unavailable/loading/error status, irreversible actions and recovery options. The UI must not show false zeroes or conceal uncertainty merely to appear minimal.
- Chart axes, periods, metric units and comparison baselines remain accessible; minimal copy must not erase information required to interpret data correctly. Accessibility/assistive labels may contain descriptions not rendered visually.
- In the future an optional Settings → Guide may carry definitions, detailed behavior and help articles. Do not build this extra destination during V187–V190 unless separately approved.
- Capture and review every changed screen and sub-screen during V190. Enforce the same rule when new features or copy are added, including the Home analytics cards. Do not modify the existing custom Home slideshow while revising Insights.

Phase sequence: **V187** core report evidence and safe deeper-data commit; **V188** revenue/reach reliability; **V189** complete content and video analysis; **V190** all-screens minimal-content redesign; **V191** release QA with live Studio parity and physical-device acceptance. Treat every version as a candidate, never an automatically stable release.

## Locked Home slideshow requirement

Custom images and their automatic slideshow remain a first-class Home feature.

- Put an accessible photo/collections button beside the greeting. Add the same management destination under Settings → Appearance → Home slideshow.
- With no images, show a compact greeting/header and the normal cards immediately underneath. Do not reserve an empty banner or an extra row for the add button.
- When images are successfully added, insert the slideshow beneath the greeting in normal vertical layout. All following cards move down naturally. It must not overlay cards, require enabling a separate switch, or replace creator work.
- Keep greeting outside the image so it remains consistently legible. Reuse the existing image import, local storage and slideshow behavior where safe.
- One image is static; multiple images advance automatically with a gentle transition. Preserve automatic playback as the default for multiple images; offer pause and respect reduced-motion preferences.
- Management: add/change, reorder and remove individual images, plus confirmed remove-all. Make append versus replace explicit before modifying an existing collection.
- Existing custom images survive the upgrade. Failed or canceled import preserves the previous collection. Missing or corrupt images do not block Home or leave a permanent blank slideshow.
- The slideshow has a bounded height and sensible image cropping across phone sizes. Decode images efficiently and suspend background animation when Home is not visible.
- Both entry points manage the same collection. Determine and disclose whether locally copied slideshow images are included in portable backup; implement inclusion with a versioned manifest if absent and approved within the backup work package.

Acceptance matrix: zero, one and multiple images; add, cancel, import failure, reorder, removal, missing file, relaunch, upgrade, offline use, light/dark themes, large font and reduced motion. No card overlap or unexpected reserved space.

## Delivery rules

1. Establish one canonical source and keep a verified baseline. No destructive data reset, forced reinstall or silent replacement of creator choices.
2. One bounded phase per candidate. Every candidate includes source commit, version, change notes, test results and installable APK with artifact/signing information.
3. Extend existing data stores and components. Do not introduce a second project, publication, calendar or reminder authority.
4. Distinguish proposed state, recoverable draft, locally saved data, cloud backup and verified publication.
5. Use focused behavior tests and device checks. Static guards supplement these; they do not prove usability or runtime reliability.
6. Preserve original guided project creation and keep Quick Create separate.
7. Changes to appearance are not permission to discard user data, personal images, recordings, schedules or project history.
8. Capture current screens before visual implementation and compare the changed screens afterward. Source findings do not substitute for visual QA.
9. Insights data correctness is a release gate before redesigned analytics or new recommendation behavior ships. Home and general usability improvements need not wait for all advanced analytics features.
10. Use the same verified Insights data and metric contracts on Home, video reviews and recommendations. A correct Insights tab is insufficient if another surface still shows stale or differently calculated values.

## Insights review: what changed this plan

### Evidence boundaries

This review traces the actual V183 entry point (`V11InsightsScreen` → `V172InsightsBody`), not an assumed future dashboard. Active tabs are Overview, Content, Revenue and Creator. It covers source implementation, existing test expectations and current official YouTube documentation. No Google consent, live channel request, revenue request, Reporting job creation or AI generation was performed. No Android UI was executed or newly screenshotted. Findings below distinguish confirmed source behavior from failure scenarios requiring runtime reproduction. Existing tests were inspected, not rerun in this deeper review.

### What already works and should be retained

- Core channel results are published before the deeper audience/reach work finishes. Optional reporting should not blank ordinary Insights.
- Core cache requests have channel and creator-data-generation authority, reject older requests, preserve manual project links on disconnect and include dedicated lifecycle tests.
- The app fetches actual prior-period reports for comparison; those values are not fabricated from a trend.
- Reach aggregation weights CTR by impressions and prefers newer revisions for a reported day rather than adding both revisions together.
- Foundation datasets have ready/empty/pending/unavailable states, although these distinctions are not consistently carried through every metric and UI.
- Publication observations retain actual capture age and mark late captures, a valuable foundation for truthful comparisons.
- Project-linked reviews retain script/process/packaging context. Optional AI has explicit sharing controls and a preview step; ordinary video review works without AI.

### Confirmed findings and consequences

| ID | Source behavior | Product consequence | Required direction |
|---|---|---|---|
| I01 | Base snapshots store requested start/end dates; absent report fields/rows commonly become numeric zero | Requested coverage can look fully available; a missing metric can look like poor performance | Explicit requested/observed coverage and metric availability; real zero remains distinct from unavailable |
| I02 | Main queries end two days before Pacific today; revenue uses device-local yesterday and an independent period selector | Similar period names can represent different dates; month-start handling can query the current unprocessed day | Shared reporting calendar, explicit dataset dates and a defined month-start state |
| I03 | Core models omit engaged views while the deeper foundation fetches them separately | Core ratios/comparisons cannot reliably choose or expose the relevant denominator | Carry views, engaged views and applicable definitions through the main model and comparisons |
| I04 | Base client fetches top 10 videos and latest 20 uploads; Content displays up to 12 positive-view ranked items | This is not a complete video library or full-channel content-type analysis | Searchable/paginated catalog, explicit coverage and on-demand retrieval outside the initial sample |
| I05 | Recent baseline is an arithmetic average of positive period-view rows, without age/format matching | An older breakout and a just-published Short can receive an apparently comparable multiple | Same-format and comparable-age cohorts; robust baseline, sample count and an insufficient-data state |
| I06 | Format groups are based on linked project metadata/title fallback and the limited loaded video sample | Editorial category and actual platform format can be conflated; unlinked videos are omitted | Separate platform format from creator/editorial category; show linked/sample coverage |
| I07 | Revenue RPM is estimatedRevenue × 1,000 / views; engaged views are absent; zero views returns zero RPM | This computed ratio can differ from Studio RPM, particularly for Shorts; undefined rates look real | Validate applicable numerator/denominator per format and period, disclose calculated ratios, use unavailable for undefined rates |
| I08 | Optional ad metrics fall back to zero and optional content/trend failures become empty lists | API failure, genuinely zero revenue and no available data can look similar | Independent dataset health and retry; no zero CPM/ad-impressions claim from a failed query |
| I09 | Revenue store is keyed by period with a separate owner marker; UI nonce guards display after save, rather than binding the write to a channel/generation | Delayed monetary responses and account/disconnect races need stronger isolation | Channel-bound authorization verification and atomic generation-aware commits across revenue, foundation and reach |
| I10 | Pulse compares counter differences using a current interval of 18–30 hours and a previous interval selected against an older 40–58-hour sample | Unequal-duration intervals can produce a false momentum percentage | Comparable durations or normalized rates; show real times, freshness and correction states |
| I11 | Pulse clamps negative counter changes to zero, uses only sampled videos and sometimes calls the result today's growth | Counter corrections and incomplete attribution are hidden; it is not Studio realtime | Label measured counter changes accurately, preserve correction information and expose sampling limits |
| I12 | Retention is fetched for at most three sampled videos; individual failures can aggregate into an EMPTY retention state | Other videos lack review evidence; an unavailable report can look like no retention activity | On-demand per-video retention; per-video health, timestamps and retry |
| I13 | Reach reports are read from a single page and at most 60 daily reports are imported per sync; summary uses requested dates without coverage counts | A 90-day summary can include only partial reach coverage while looking complete | Pagination, resumable imports, actual covered dates/day counts and revised-report reconciliation |
| I14 | Missed publication targets can all be populated from the same later snapshot; watch/subscriber fields retain a rolling analytics-window meaning | A late observation must not become an exact 30m/2h/24h growth curve or lifetime watch history | Show actual capture time, metric window and late quality; exclude incomparable historical points |
| I15 | Signals use fixed confidence values and thresholds; aggregate average-duration changes can trigger opening/pacing advice | Observations can overstate explanation; a Shorts/long-form mix shift may explain an average change | Evidence/sample-based strength, format controls, cautious hypotheses and no calibrated-probability claim |
| I16 | Some evidence drilldowns resolve a signal by video title; the Insights opportunity CTA invokes a generic create callback | Duplicate titles can select wrong evidence; suggested context is lost on action | Stable IDs and contextual actions: open video/project/idea or prefill a proposed experiment |
| I17 | Creator summary counts raw tasks/links, groups a bottleneck by content type, and mixes 30-day completion with broader ratios | Archived/irrelevant work and incomparable periods can distort process advice | Defined active/archive/publication filters, period labels and workflow-stage bottlenecks |
| I18 | Overview trend bars impose an 8% minimum height; chart points are evenly spaced by row; Canvas interaction has no equivalent date/value controls in the inspected function | Zero or missing dates can be visually misleading; chart interaction may exclude assistive users | Accurate zero/gaps/date spacing, readable labels and accessible list/navigation alternative |
| I19 | Creative-pattern and video-review memoization does not include the foundation refresh revision; saved AI reports are keyed by video without an evidence-content fingerprint | Newly arrived deeper data may not update all dependent cards; saved interpretations can become outdated | Shared observable state/revisions; evidence fingerprint, range and generated-at status on saved interpretations |
| I20 | Deep analytics is nested inside a small video dialog and requires a linked project; audience overview largely shows dataset status; many labels are 6.8–9sp | Valuable data is difficult to discover/read; unrelated project setup becomes a barrier to inspecting video data | Dedicated video detail, actionable Audience view, analysis available before linking and readable typography |

I09 is an identified isolation gap and test requirement, not a claim that a particular user's revenue has been mixed. I18 is a source-level accessibility risk, not a completed TalkBack audit. I15 describes heuristic confidence, not a claim that percentages are displayed everywhere.

### Metric meaning verified against official documentation

YouTube's report query can stop before the requested end date when not all requested metrics are available. A successful fetch therefore does not establish complete requested coverage. Public views and engaged views have distinct definitions; the existing metric-boundary contract should be propagated through the app rather than removed. YouTube's current revenue guidance also distinguishes engaged-view handling, so Backlot's views-only monetary ratio must not promise Studio parity without denominator/numerator reconciliation. Per-video subscriber reports also cover a narrower attribution scope than channel totals; totals should not be forced to reconcile by inventing a residual video attribution.

References:
- [Report query, date coverage and pagination](https://developers.google.com/youtube/analytics/reference/reports/query)
- [Metric definitions and subscriber attribution](https://developers.google.com/youtube/analytics/metrics)
- [YouTube explanation of views and engaged views](https://blog.youtube/inside-youtube/engaged-views-youtube-explained/)
- [Revenue, RPM and CPM explanations](https://support.google.com/youtube/answer/9314357?hl=en)
- [Reporting API reach report dimensions and metrics](https://developers.google.com/youtube/reporting/v1/reports/channel_reports)

### Revised Insights product structure

Retain four top-level destinations, with familiar names where possible:

| Destination | Main question | Contents |
|---|---|---|
| Overview | What changed in my channel? | Explicit channel/range/coverage, readable primary metrics, prior comparison, accurate trend, a few evidence-linked observations |
| Content | Which videos and formats are working? | Search, filters, pagination, data coverage, comparable video performance and a dedicated video detail |
| Revenue | What did I earn and what affects it? | Estimated totals, explicit dates/currency, transparent rates, format breakdown, partial/error states and permission controls |
| Creator → Learning & Progress | What should I improve in my next project? | Distinct production progress and publishing outcomes, linked reviews, cautious patterns and saved experiments/learnings |

Audience details belong in an accessible screen from Overview: traffic sources, subscribed status, geography, devices and retention, each with availability and sample context. Video detail should offer Results, Reach, Retention and Linked Project/Review sections without requiring a project link to view available platform data. An unavailable section explains why and offers the appropriate retry or waiting state; it does not disappear into a misleading zero.

Optional AI is secondary to the deterministic review. Show the actual information selected for sharing in understandable terms; remove provider/build configuration from the ordinary creator flow. Preserve consent and deletion controls, identify older saved analysis and require no AI subscription for the core learning loop. AI should not claim it examined imagery or a full script unless that evidence was actually supplied and supported.

## Phase 0 — Establish the release baseline

Purpose: ensure all further work starts from the correct app and protects existing creator data.

Work:
- Recheck latest branch, main commit, CI status and installer provenance at implementation time.
- Reconcile README, CURRENT_STABLE and the release record around the current version, while preserving historical checkpoints.
- Inventory active routes, stores, drafts, images, recordings, publication records, schedules and backup scope.
- Create representative test data and an export/restore baseline; record known gaps separately from new regressions.
- Prepare device QA for the user's Samsung phone and a modern Android emulator, with notification/permission differences covered. Retain existing older-Android regression coverage.

Exit: reproducible baseline; release records agree; old-data upgrade and recovery path documented; fresh reference screens captured. If a runtime surface is unavailable, identify that gap and do not declare its check passed.

## Phase 1A — Protect creative input and consistency

Purpose: remove the most consequential risks before changing the visual hierarchy.

Work:
- Quick Capture: recoverable draft, explicit save state and safe exit when text/audio exists. Recover original recordings after interruption and avoid premature cleanup.
- Idea conversion: one recoverable/idempotent operation across project creation, idea linking, reminder configuration and reward recording. Prefer durable transaction/recovery semantics appropriate to existing stores.
- Show reminder behavior before conversion; inherit an explicit preference rather than silently applying Guided.
- Display conversion success only after durable acceptance. Failed writes keep the user's input available with a retry path.
- Preserve recoverable deletion and distinguish it from future permanent erasure.
- Replace misleading combined progress with named workflow, checklist and publication facts. Use one meaning for each status throughout the app.

Exit: interruption/failure tests leave one project, one link and one reward; capture text/audio survive restart; no automatic stage or publication completion from reminder acknowledgement.

## Phase 1B — Insights trust foundation

Purpose: make data interpretation and ownership reliable before expanding the dashboard.

Work:
- Define one metric contract including origin, channel, unit, definition/denominator, requested and observed coverage, checked-at time, availability and processing status.
- Preserve actual zero, missing, unavailable, partial and pending as separate states. Do not infer complete dates from successful request parameters.
- Align reporting calendars and explain independent revenue periods. Handle first-of-month, midnight, timezone and late-report boundaries.
- Carry engaged views where supported; validate monetary ratio semantics. Label any custom calculated rate honestly and explain limits relative to Studio.
- Apply channel/generation/request authority at the commit boundary for every derived dataset, not only the core cache. Validate the monetary token's actual channel before accepting its data.
- Make each dataset refresh/retry independently and retain clearly labeled last-good data. Ensure silent authorization cancellation, rapid range changes and failures cannot leave a stuck loading state.
- Invalidate dependent Home, creative-pattern and review state when deeper data changes; prevent stale responses from repopulating disconnected/restored caches.

Exit: mocked and instrumented contract tests cover partial reports, empty success, real zero, permission denial, expiry, rate limits, cancellation, restore, account switch and delayed responses. Compare authorized live results with Studio using the same channel, metric definition, timezone and coverage; document expected differences instead of forcing equal numbers.

## Phase 2 — Readability and interaction foundation

Purpose: make the app comfortable on an actual phone without replacing its identity.

Work:
- Expand the recent New Project sizing foundation into shared text, spacing and action patterns across active screens.
- Start with 14–16sp body text, 12–14sp supporting text and approximately 48dp minimum touch targets, then validate rendered results. These are design targets, not proof of accessibility.
- Essential information must not depend on 7–9sp labels. Let important copy wrap and containers grow.
- Fix keyboard/insets, scrolling, long names, Telugu/English content, empty states and error states.
- Standardize primary/secondary/destructive actions and consistent Back/Close behavior.
- Remove internal release-history language and unnecessary technical explanations from product copy.
- Verify contrast, TalkBack labels/focus order, large text, reduced motion and light/dark themes with actual rendered screens.

Exit: compact-screen and 1.5×/2× font journeys remain usable; controls have clear labels; no inaccessible essential actions or clipped fields in tested configurations.

## Phase 3 — Home & Daily Experience (owner-reviewed scope, 8 November 2026)

**Status: documented / NOT IMPLEMENTED.** Start implementation only after today's frozen reminder Phase 1 release decision and data-safety issues have explicit disposition. No silent changes to installed Build 320/321, no premature production merge. This section supersedes the older Phase 3 summary for future implementation.

### Product goal and current Build 321 evidence

Home must be a **launchpad for real work**, not an oversized command dashboard. One primary "Continue" action must open the project and relevant current working context; Focus is a separate optional and durable work session; project status and stage completion are not disguised as duplicate CTAs. Preserve the cinematic Director's Cut black/ivory/REC-red identity and existing authored data.

Owner's Build 321 screenshot and confirmed source audit:
- `V18TodayScreen.PNextMoveCard` expanded details show "WHY THIS MOVE", "SIGNALS", internal "PRIORITY 1228", redundant "START THIS MOVE", two alternatives and "HIDE REASONING". The large card pushes the Reminder Hub down and the bottom navigation is near lower content.
- The large "START THIS MOVE" and separate "WORKING/START" button both dispatch `onStart(selected.id)` to `CreatorViewModel.startTask`; if already WORKING, this often produces **no user-visible navigation or progress**, though existing reminder scheduling/working-quiet behavior may be refreshed. **Remove redundant Home CTAs, not the underlying state/quiet-period behavior.**
- "FOCUS" sets `focusTaskId` and opens `PFocusScreen`: it immediately runs a 25-minute in-memory Compose timer; leaving destroys the timing state. Its "STEP DONE" calls `advanceWorkflowWithJourney` regardless of elapsed time. Timer completion itself does not confirm creative work.
- The internal priority score is useful to *rank*, not to show as an unexplained user metric. `CreatorPriorityEngine` already exposes meaningful urgency and reasons; existing `openProject(id)` can route to a specific project while preserving the CreatorTask source of truth.

### Frozen functional decisions / action contract

| Current affordance | Phase 3 disposition | User-visible outcome |
| --- | --- | --- |
| "START THIS MOVE" inside expanded Next Move | **Remove duplicate CTA** | One primary Home **Continue** action below the selected project opens its actual work context. |
| Separate "START/WORKING" Home button | **Remove button** | Show `READY` / `IN PROGRESS` as a non-clickable status only. Starting a planned project, when needed for reminder quiet-period logic, stays an **explicit** project workflow action. |
| "FOCUS" | **Keep, rework** | Open an optional focus setup/session with explicit Start/Pause/Resume/Finish, durable state and honest time accounting. No automatic project-stage advancement. |
| "MARK STEP DONE" / "MARK PUBLISHED" below Home card | **Move to project workflow** | Explicit confirmation in the relevant stage. Publishing must retain existing separate publication evidence and must never be inferred from timer completion. |
| "WHY THIS MOVE" / "SIGNALS" | **Compact** | Default one useful action plus a short human-readable rationale; expand for explanation only if requested. |
| "PRIORITY 1228" score | **Remove from UI** | Keep internal ranking calculation; display meaningful Overdue / Due Today / Critical / In Progress labels without false precision. |
| "ALSO NOW" | **De-emphasize** | At most two other relevant projects, behind an optional disclosure, each tapping through to a real project. |

### Implementation 3A — Home action cleanup (first candidate)

- Replace dual Start buttons with a **single Continue** CTA and a passive current project status. Don't create a second project status store.
- Condense Next Move to current stage/action, honest due state, and one-line reason; explanation/alternatives reveal only on tap. Do not expose numerical ranking scores.
- Remove redundant Home **Mark Step Done**; preserve explicit completion and `CreatorPublicationEngine` semantics within Projects.
- Guarantee every CTA either navigates or supplies accurate save/error feedback. A repeated tap must not enqueue duplicate project mutations, stage rewards or reminders.
- **Gate:** on planned/working/overdue sample projects, no duplicate Start; correct status, due date and task selected; project and reminder data unchanged by merely opening Home.

### Implementation 3B — Continue Work deep navigation

- Reuse existing project route `openProject(id)` / `externalStudioId`; first deliver a verified **direct project opening** and return to Home with the same selected project.
- Add a **stage-aware destination mapping** only when a real existing section/action exists (research, script, production, publication). Otherwise fall back visibly to the project overview, never to an unrelated editor or empty screen.
- Navigating into a PLANNED project must **not** silently mark it WORKING. Provide an explicit "Begin project" workflow action when required; preserve `startTask`'s current smart-reminder/working quiet-period semantics and recheck scheduling, failures and idempotence.
- Preserve current project, editor drafts, workflow stage and Home carousel selection through app navigation, cold start and optional detours.
- **Gate:** correct project and stage for each content type; planned, working, archived, completed, and missing project states have sensible fallbacks; Back returns correctly and never advances a stage or reports publication.

### Implementation 3C — Focus 2.0 (durable work sessions)

- Focus is **optional** for the selected project/stage. Explicit Start; optional 15/25/45/60-minute preset selection (owner can approve final UI). Pause/Resume/Finish/Discard are distinct and understandable.
- Maintain a single active session authority with project ID, stage ID, start/last-active timestamps, accumulated active duration, pause state and completion/abandonment outcome. Prefer existing persistence infrastructure; schema migration/rollback must be additive, and every new durable session must have a defined export/restore scope before release.
- Recover after navigating away, screen rotation, app process death or reboot; calculate elapsed time from durable timestamps (not one-second Compose decrements). Handle clock changes, suspended app and timer expiration without fabricated minutes, duplicate notifications or unintended project edits.
- Completion shows an **honest session summary** (actual active time), not a project accomplishment. Never call `advanceWorkflow` merely because a timer ends. If a stage really is complete, user can separately confirm inside the project workspace.
- Do not add background alarm/foreground-service behavior without explicit permission/reliability and battery-policy review. User may finish/discard a session with no stage or project completion.
- **Gate:** start/pause/resume/finish/discard, rotation, background, process death, reboot, second device backup/restore if synchronized, and simultaneous action tests; no phantom elapsed time or auto Done.

### Implementation 3D — Smart Next Move clarity

- Keep `CreatorPriorityEngine` as read-only ranking authority; do not create conflicting "AI decision" status or arbitrary urgency score.
- Show the **recommended next real action** and a short, truthful reason grounded in deadline, priority and stage. Overdue/Today labels derive from valid stored timestamps; outdated `dueLabel` alone is not proof of an active deadline.
- Provide optional "Why this?" and "Other projects" disclosures. Support choosing another existing project without editing a user's priorities. When a recommendation goes stale after edits, recompute from current `CreatorTask` data, not cached screen copy.
- **Gate:** planned/working/overdue/critical/archived/completed combinations, equal-score ties, missing/stale due dates, no projects, multiple projects, and delayed state reload do not show impossible instructions or numeric internal scores.

### Implementation 3E — Home hierarchy and preserved features

Keep the approved Home order while reducing excessive card height:
1. Greeting + compact Spark; existing slideshow photo-management control.
2. Slideshow **only if images exist**, using the *locked Home slideshow requirements* earlier in this document, with no empty banner.
3. Optional connected Top 3 video results above Up Next as currently established; never show fake zero metrics if disconnected.
4. One Up Next project + compact Next Move/Continue; optional Focus secondary.
5. Reminder Hub with real active/needs-attention state, not a duplicate Home alerts list.
6. Existing creator progress strip, Weekly Focus and performance highlights as secondary/collapsed information.

- Preserve existing images, slide order/playback/reduced-motion, Spark, Top 3, Idea Vault / plus navigation, projects, reminders and home quick capture.
- Check user screenshot's apparent bottom-nav overlap against **real** Android 15 navigation bars, keyboard and content scroll; fix inset/scroll clearance only when reproduced. Do not blindly increase padding.
- Retain actual completed/working status and accurate loading/empty/error/offline states.
- **Gate:** no lost buttons, hidden required controls or unexpected vertical blank areas on compact displays; Reminder Hub remains accessible; current selected project persists through recompositions.

### Implementation 3F — Test, design approval and owner APK

**Design before implementation:** Capture baseline Build 321 native Home screen(s), document before/after interaction map, present 2–3 *actual Android implementation-feasible* Home states for owner review (collapsed, "Why this?" expanded, active Focus). **Do not independently lock new visual decisions** before the owner approves. No separate showpiece-only prototype as the final product.

Each bounded candidate needs its own exact source SHA, screenshots, tests, signed-owner-APK provenance and recoverable update plan. Specific QA:
- Buttons: Continue, Focus, Why this, Other projects, Home project switch, Reminder Hub, Back, project Begin/Finish all navigate or give verified feedback; no dead or duplicated CTA.
- Persistence: Focus timing and session history after pause/force stop/rotation/reboot; project stage and publication remain unchanged unless explicitly confirmed; Smart reminder working-quiet logic preserved.
- Layout: Android 11/15, narrow screen, 1.5x/2x fonts, English/Telugu mixed text, light/dark semantic Director's Cut, gesture/3-button nav, keyboard and TalkBack.
- Preserved modules: zero/one/multiple Home images, slideshow editor/restore, Spark, Top 3 disconnected/connected, offline Home, Personal/Creator Reminder Hub and Spaces, and Build 321-to-next **in-place signed** upgrade without clearing existing owner data.
- Security/recovery: verify backup scope for new focus session data; fail closed if incomplete rather than claiming full cloud coverage. Preserve all Phase 1 challenge, Space, photo, and Personal restore protection.
- Final owner approval on a **physical phone**. Test evidence may justify a review APK but not a production/Play release.

**Controlled release sequence:** 3A+3B (Home action/Continue) -> owner navigation acceptance -> 3C (Focus durability) -> 3D (recommendation logic) -> 3E (hierarchy/slideshow preservation) -> 3F regression/owner APK and sign-off. If 3A/3B reveal a common structural blocker, fix the smallest root cause rather than adding features to unrelated screens.

### Phase 3 exit criteria

A returning creator can select a real active project and **reach the relevant working context in one intentional Continue tap** (with documented overview fallback); no dead Home CTAs or raw internal scores; optional Focus survives interruption and records only actual session activity; Home remains compact, readable and fully usable with existing slideshow/Spark/Insights/reminders; old and new data survive an in-place upgrade. All updated screens have approved real Android screenshot evidence and passing functional/device acceptance.

**Out of scope:** whole-app Phase 2 visual overhaul (explicitly deferred until after Phase 6), new recommendation engines or speculative AI, new Spaces architecture, new Focus gamification, social features, redesigned reminders unrelated to Home, automatic publication, and any Phase 1 sync bypass.

## Phase 4 — Projects and the creator workspace

Purpose: make project work feel continuous rather than a set of separate tools.

Work:
- Retain guided New Project and separate Quick Create; use visible, editable defaults and a clear final summary.
- Preserve creation drafts across Settings detours, navigation and interruption.
- Make the project hub show current stage, meaningful production status, next action and save status.
- Connect brief/research, script, production and publishing through consistent project context. Keep one authoritative script and publication model.
- Clarify local saved, unsaved/recoverable and cloud-backed states. Avoid cloud imagery implying a backup that has not occurred.
- Improve long-script editing and draft recovery; ensure visual notes, references and original voice ideas remain accessible.
- Add a few editable templates using existing template infrastructure: cinematic analysis, general commentary/education and Short/Reel. Do not hardcode film-specific prompts for every creator.

Exit: a creator develops a real project, switches workspaces, returns later and resumes the correct draft; edits and existing data survive upgrade, failure and restore.

## Phase 5 — Calendar, reminders, Weekly Plan and Automation

Purpose: make commitments understandable and realistic while preserving the working reminder engine.

Work:
- Calendar clearly distinguishes real projects, recurring-slot previews and recorded publication.
- Weekly Plan defines recurring slots; Automation creates eligible projects; Calendar displays their dates. Explain this near first use.
- Preserve confirmation for generation and schedule changes. Editing/disabling a template does not silently rewrite existing work.
- Show intended background action, last confirmed run and failure/retry state separately.
- Retain acknowledgement authority, bounded quiet follow-ups and quiet hours. Explain defaults and per-project overrides simply.
- Add a modest optional workload view using creator-provided estimates/availability; warn about overload without moving deadlines automatically. Do not block the core phase on a complex scheduling engine.

Exit: no duplicate generated projects; no phantom notifications or posting claims; existing reminder regressions pass; device matrix verifies locked screen, reboot, quiet hours, overlapping alerts and permission changes.

## Phase 6A — Content coverage and fair performance comparisons

Purpose: turn a limited sample into an understandable analysis experience.

Work:
- Keep quick initial results, then support searchable/paginated video coverage with explicit loaded counts and retrieval state. Load missing video details on demand instead of hiding them because they are absent from top/recent samples.
- Separate actual YouTube format, project editorial category and publication age. Avoid presenting title inference as authoritative format metadata.
- Add comparable-age/format baselines using robust statistics, visible sample counts and sufficient-evidence checks. Clearly distinguish current-period contribution from first-day/first-week performance.
- Correct the pulse's interval comparison, stale-data labels and counter-correction handling; do not imply it is an official realtime feed.
- Preserve late publication observations as late; show metric coverage and actual age. Do not backfill exact historical checkpoints from later cumulative totals.
- Correct zero/missing chart rendering and provide accessible chart controls/data lists. Repair creator summary filtering and workflow-stage bottleneck meaning.

Exit: synthetic fixtures for mixed Shorts/long videos, unequal video ages, a breakout, zero baselines, net subscriber losses, sparse dates, archived projects and sparse links produce understandable comparisons without unsupported conclusions.

## Phase 6B — Audience, reach, retention and revenue workspace

Purpose: make deeper datasets useful and independently dependable.

Work:
- Expose readable Audience and dedicated video-detail destinations. Project linking enriches review but does not gate ordinary video metrics.
- Fetch retention on demand for the selected video; show per-video availability and freshness rather than treating every failure as empty.
- Paginate/resume reach report imports, track covered dates, reconcile revised reports and show partial 90-day coverage honestly. Preserve impression-weighted aggregation.
- Keep estimated revenue, ad revenue, playback CPM, monetized playbacks and calculated rates distinct; show dataset failure/permission states without invented zero values.
- Search/sort top earners with explicit dataset coverage. Provide clear currency choice/cache identity and confirmed channel context.
- Test report latency without promising immediate availability. Do not add upload/post/delete permissions to enable analysis.

Exit: a selected video opens available results/CTR/retention even if not linked or in the initial sample; partial reach and optional revenue failure do not corrupt totals or blank unrelated Insights.

## Phase 6C — Publishing, evidence-based guidance and learning

Purpose: close the loop from finishing work to improving the next output.

Work:
- Make one long video plus related Shorts/Reels easy to manage with separate readiness, deadlines and publication records.
- Distinguish Prepare to Publish, Record Publication and external posting. Preserve explicit publication evidence and correction history.
- Remove processing/finalized wording contradictions throughout Insights.
- Show reporting coverage, checked-at time, missing data and processing status consistently. Audit requested versus actual returned coverage before claiming completeness.
- Keep Shorts and long-form comparisons appropriately separate and communicate limited samples.
- Add a concise review: intended outcome, observed result, what worked and one change to try next. Reuse it in future projects rather than generating ungrounded advice.
- Keep in-app XP separate from YouTube results and meaningful creative outcomes.
- Route recommendations by stable IDs and carry their evidence into the correct existing video, idea or project. A new experiment opens with editable source context rather than an empty unrelated composer.
- Replace arbitrary confidence impressions with explainable evidence strength, comparable samples and limitations. Do not blame pacing based only on a mixed-format average-duration change.
- Preserve AI opt-in and preview; fingerprint/version its evidence and visibly mark stale saved reports. Keep ordinary review independent of AI and remove engineering configuration text from the creator flow.

Exit: publication of a long video leaves unfinished derivatives active; analytics wording matches data; one recorded learning can inform the next project; disconnected/offline states remain useful.

## Phase 7 — Settings, recovery and whole-app acceptance

Purpose: make daily use and upgrades dependable.

Work:
- Refine Settings around profile, appearance/Home images, reminders, planning, connections and data/recovery. Keep one destination per underlying setting.
- Show backup scope and last confirmed backup time. Explain external-file availability and any excluded media.
- Test backup/restore with voice recordings, scripts, personal Home images, schedules, reminders and publication history.
- Validate local-only use, sign-out/account changes, offline work, reconnect and failed backup. Keep local work safe.
- Run the full creator journey and visual/accessibility matrix; resolve regressions before stable promotion.
- Keep personal-install stability and public production readiness separate. Do not change package/signing identity casually; public distribution needs a dedicated migration and production configuration work package.

Exit: verified in-place upgrade, recovery drill, current CI and device sign-off. Deliver a stable candidate only after its own artifact is tested.

## Pilot and success measurement

After the core improvements, run a small observed pilot with 5–10 unfamiliar solo creators. This is proposed research, not existing evidence.

Proposed acceptance targets to validate:
- First saved idea within 60–90 seconds without an account.
- Returning creator can identify the next action within roughly five seconds.
- Typical Continue action reaches the relevant editor in one or two taps.
- No loss in the defined capture/script interruption matrix.
- No duplicate conversion or generation in the defined recovery tests.
- No overlap or blank reserved banner in the slideshow state matrix.
- All essential tasks remain usable in the defined large-text/device matrix.

Measure first useful project, resumed work, recorded publication, useful learning, reminder fatigue and successful recovery. Avoid optimizing for time spent in the app, notification volume or XP alone. Exact targets and instrumentation should be adjusted after observing real usage.

## First implementation package

Start with Phase 0, then bounded Phase 1A and 1B candidates. Deliver Phase 2 and Phase 3 as the first substantial visible improvement, including the preserved slideshow. Continue through Phase 4 and 5, then Phase 6A, 6B and 6C as separately reviewable creator/Insights improvements, and finish with Phase 7. Correct high-priority data/ownership defects early even if their related advanced screen ships later. Do not combine all changes into one unreviewable APK.

## Insights release acceptance matrix

1. Connection: no account, canceled consent, channel A/B switch, separate monetary consent, expired token, revoked permission and disconnected offline cache.
2. Requests: rapid 7D/28D/90D changes, missing requested cache, overlapping refreshes, screen leave/return, late result after restore and an unsuccessful optional dataset.
3. Dates: IST/Pacific day difference, first day of month, processing lag, missing report days, current versus prior-period coverage and 90-day partial reach.
4. Metrics: known zero versus missing, engaged/public definitions, undefined monetary denominator, negative subscriber changes, supported report combinations and API-versus-Studio scope differences.
5. Catalog/comparisons: more than 20 uploads, a linked video outside the initial sample, duplicate titles, Shorts versus long-form, different ages, one breakout and insufficient comparable examples.
6. Reach/retention: first Reporting job pending, paginated reports, revised day replacement, missing days, zero impressions, selected-video retention absent/failed/available and independent retry.
7. Pulse/checkpoints: unequal sample intervals, no previous baseline, negative counter adjustment, stale samples, a late first observation and a video outside the rolling analytics window.
8. Creator/recommendations: archived/deleted projects, broken links, per-output publication, stable-ID drilldowns, actionable idea/project context and no automatic strategy claims from weak evidence.
9. AI: disabled, consent preview, canceled/failed generation, changed evidence, older cached analysis, deletion and preserved ordinary review without sharing.
10. UX: long Telugu/English titles, font scaling, dark/light themes, chart zeroes/gaps, TalkBack navigation and no loading indicator that remains after completion/cancellation.

## Insights source map

Paths below are beneath `app/src/main/java/com/framebynavin/app/` in the pinned review baseline.

- `ui/V11YouTubeInsights.kt`, `ui/V172InsightsUi.kt`: active route, tabs, loading, linking, charts and dialogs.
- `youtube/YouTubeApiClient.kt`, `YouTubeAnalyticsModels.kt`, `YouTubeAnalyticsQueryPolicy.kt`: queries, sample limits, parsing and date contracts.
- `youtube/YouTubeAnalyticsStore.kt`, `YouTubeAuthorization.kt`: core request authority and consent scopes.
- `youtube/YouTubeInsightEngine.kt`, `YouTubeContentClassifier.kt`, `YouTube24HourPulse.kt`: baselines, grouping, creator summary and counter-derived signals.
- `youtube/YouTubeRevenue.kt`, `ui/V144YouTubeRevenueIntegration.kt`, `ui/V144YouTubeRevenueUi.kt`: monetary queries, owner handling, dates, rates and presentation.
- `youtube/YouTubeInsightsFoundation.kt`, `YouTubeReachReporting.kt`, `ui/V20InsightsFoundationUi.kt`: deeper reports, health, sample coverage and importer.
- `youtube/YouTubePublishCheckpointsV2.kt`, `YouTubeMilestoneSnapshots.kt`: timing and observation semantics.
- `ui/V20InsightsDrilldownUi.kt`, `V20VideoPostmortemUi.kt`, `V20OpportunityEngineUi.kt`, `V20CreativeIntelligenceUi.kt`: evidence, interpretation, navigation and refresh dependencies.
- `data/CreatorCreativeIntelligence.kt`, `CreatorVideoPostmortem.kt`, `CreatorAiReportStore.kt`, `ui/V20GeminiIntelligenceUi.kt`: correlations, project context and optional AI interpretation.
- Existing JVM and instrumented tests cover useful parts of these paths, including core cache lifecycle, query windows, reach revision handling, content classification and checkpoint quality. Their presence does not establish complete live/API/device acceptance for the revised matrix.

Version numbers and dates are assigned after checking current repository state and bounding the implementation work. No invented delivery deadline is part of this plan.
