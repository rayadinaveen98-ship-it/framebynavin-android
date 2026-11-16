# Backlot — Refinement #09: Progress Journey, Creator/Personal Achievements, Daily Brief and Content Calendar

**Record date:** 2026-11-17  
**State:** RESEARCHED / PROPOSED for founder review; **NOT IMPLEMENTED, TESTED, MERGED or RELEASED**.  
**Scope:** Quick Actions: Progress Journey, Creator Progress + rewards, Daily Brief, Content Calendar; companion celebration and privacy-safe Personal progress.  
**Code source:** Frozen stable V337 `678502f4626150ea1945b81eed1593a362377454` (`rayadinaveen98-ship-it/framebynavin-android`). Read-only source audit; no live user records, APK/device screenshots, new CI, physical audio/animation tests. Founder requested documentation and planning before implementation.  
**Dependencies:** #02 first-run, #03 personalized identity, #06 Content Workspace 3.0 format/stage evidence, #07 selected-companion motion, #08 Personal Home/My Spaces and personal data provenance.  
**Product intent:** Help people see what happened, celebrate actual progress, know what to do today, and plan what is ahead. Four perspectives with one evidence model; no duplication or fake "completed"/"published" events.

## Executive assessment

Backlot has meaningful foundations: an auditable Progress Journey, deduplicated/append-only Creator reward ledger, six achievements and five inferred traits, a contextual creator Daily Brief, and a two-week Content Calendar that distinguishes actual projects from virtual weekly-plan slots. Their functionality is currently shallow in **exploration and decision utility**. A rewarding event can trigger a short stage pulse + reward aura/toast, but **the selected companion is not integrated**. Personal confirmed Done can optionally appear in Progress Journey (private labels only) but **there is no independent Personal achievements presentation**.

**Recommendation:** Upgrade each screen progressively; add a **three-second chosen-companion celebration** on *confirmed saved* reward events; create a separate optional **Personal Milestones** system using truthful Personal receipts, Space journals and actual Challenge confirmations, **not mandatory XP/streak punishment**. Do not replace the Creator reward engine or combine unrelated Personal/Creator stores.

## 1. Existing Quick Actions navigation

`V161QuickActionsHub` groups New Project and Quick Capture, Daily Brief/Idea Vault, Content Calendar/Weekly Plan/Automation, other utilities, Progress Journey and Creator Progress. It retains existing callbacks; `FrameByNavinV101BApp` uses `POverlay.PROGRESS_JOURNEY`, `POverlay.CREATOR_PROGRESS`, `POverlay.DAILY_BRIEF`, `POverlay.CALENDAR`. Preserve Quick Actions structure and exact domain navigation. Incrementally improve action subtitles and visible status only if necessary; no global Home redesign.

## 2. Progress Journey — verified and proposed

**Current source:** `ProgressJourneyEngine.kt` + `V282ProgressJourneyUi.kt`.
- Read-only events: project created, stage *entered via transition*, project marked done, manually marked published, idea captured, and optional **Personal reminder confirmed Done**.
- Creator timeline read from `CreatorWorkflowTimelineStore`; project/idea timestamps from authoritative saved records; no synthetic achievements from notification delivery, seeds, schedule or opening the app.
- UI has **Compact week**, **Month**, **Journey** grouped chronology (up to **100 events** in Journey), active-day markers, selected-day details, and navigates to idea vault/project. No deep link to exact stage or private Personal item.
- **Include Personal completions** switch is OFF by default; private Personal titles never appear in this creator view. A completion is shown as a generic **Personal reminder completed**. History currently device/local-based and not included in some backup exports.
- Limits: only active/not-active day indicator, no per-type filters, few source-aware details, stage transition entered != full work duration; old events may have incomplete evidence. `JOURNEY` truncates to recent 100 rather than paged full history. Visible results can change across local-time timezone; respect original timestamps.
**Plan:**
1. Three distinct perspectives: **Activity calendar**, **Milestone timeline** and **Progress insights**. Keep accessible week/month navigation; add a genuine *year overview* only if events remain efficient and paginated.
2. Typed chips (Ideas, Projects, Stage work, Published, Personal opt-in), optional project/platform/format filters. Show small honest daily summaries with actual count and category, not fabricated work hours.
3. Historical details show event time, source, actual action (created / stage changed / marked as published), and context. Open exact project/current stage when supported; otherwise open project and never fake inaccessible historic editor.
4. Weekly/monthly comparisons: # ideas captured, # stages advanced, # projects finished, # owner-confirmed published; optional personal Done counts, but **not** a total "wellbeing score". Explain when old history is incomplete.
5. Learn-more cards for key milestones (first recorded idea, release, evidence-backed stage) derived from sources; do not turn missing log into false zero.
6. Personal data opt-in remains OFF by default; in shared screenshots/export, strip private labels/photo paths and offer explicit privacy preview. If Personal 21-day or Space activity added, use their own durable source receipts and user consent.
7. Keep Journey read-only; schedule and deadlines remain in Content Calendar. Avoid storing duplicate journey rows unless a versioned canonical activity feed is explicitly approved.

## 3. Creator Progress — verified mechanics and proposal

**Current source:** `CreatorRewards.kt`, `CreatorRewardsV22.kt`, `CreatorRewardReconciliation.kt`, `V22CreatorRewardsUi.kt`, `FrameByNavinV101BApp.kt`.
- Stable idempotent `eventKey` prevents duplicate rewards. Rewards include idea captured **+5 XP once per local day**, idea converted to project **+10**, stage completed **+10**, published **+40**, post-publish checkpoints **+5/+10/+15** depending on kind. Existing XP is a *product score*, not revenue or platform performance.
- Every 100 XP advances a level. Weekly momentum totals rewards in current week; six achievement IDs: First Spark, First Publish, Learning Loop, Shipping Rhythm, Creator 100, Creator 500.
- Five traits: Ideator, Builder, Finisher, Learner, Consistent; based on recorded app actions, not on opening app.
- UI shows level, total XP, weekly momentum, next milestone, achievements, traits and recent rewards. Current reward UI is `V127StageCompletionPulse` plus `V127RewardAura` and `V22RewardToast`; `vm.rewardFeedback` clears after **2,200 ms**; no user-selected companion celebration in that overlay.
- Backfill deliberately credits only events with reliable timestamps; older stage transitions cannot be backfilled without trustworthy per-stage completion receipts.

**Three-second selected-companion celebration — concrete storyboard and behavior:**
1. On **new, committed, idempotent reward ledger event**, enqueue `CelebrationEvent` with eventKey, action label, original XP, timestamp, domain. No animation on optimistic tap or history hydration/reconciliation/backfill.
2. **0–300ms**: selected Spark/Mira enters bottom-center or action-safe side anchor via short easing, matching existing approved artwork and actual available poses; never use generic placeholder or hardcoded Spark.
3. **300–2,600ms**: warm character cheer, short celebratory particle/light ring consistent with Backlot; readable label e.g. "Research stage complete · +10 XP". Selected companion remains visible.
4. **2,600–3,000ms**: character fades/moves gently out and disappears; return focus to actual project. Entire visible celebration around **3 seconds**. For OS reduced motion, static acknowledgement for around 3 seconds or dismiss immediately; never block taps.
5. Dismiss/mute animations through Settings; duplicate events collapse by eventKey; queue/coalesce events so several successes do not layer and block workflow. Pause/suppress during active alarm, sensitive Personal alert, critical error, or navigation that would hide actual content. No repeated animation when reopening progress.
6. Existing toast and aura visual layers are integrated/replaced carefully; don't run a second overlapping animation. Preserve reward service/state ownership.
7. Use only approved Spark/Mira asset/pose packs; existing pose art is not proof of full per-pose fidelity; physical visual approval needed before claiming actual 3-second character animation.
8. Text alternatives for animations; no new speech/music playback without settings/cost/license review.

**Expand achievement system selectively:**
- Keep IDs, existing XP grants, levels, receipts and unlock times stable for current users.
- Add evidence-based quality milestones such as First Research Evidence, First Reviewed Cut, First Multi-output Project, First 21-Day Learning Loop **only** after #06 stage artifacts and exact completion receipts are implemented. Not every click deserves XP.
- Clear categories: Getting started, Building, Publishing, Learning, Consistency and Craft. Each achievement must state condition, evidence and timestamp; no credit for fake imported records.
- Improve next-achievement navigation (open relevant source), archived milestone shelf, explainable ledger line ("what awarded these XP") and accessible 3-second cameo.
- Anti-farming: stable event keys and authoritative state/reversal policy (cancelled or retracted stage? don't silently regrant or retain impossible claim without a defined source-of-truth rule). Preserve current app's backfill semantics. No purchased XP, leaderboard or exploitative streaks.
- Reward Creator *process* and learning, not views, money or aggressive upload cadence.

## 4. Personal progress & milestones — distinct care-oriented system

- Personal data sources: `PersonalCompletionJournal`, `Phase1HabitStore` with manually confirmed challenge days, `V289SpacesDatabase` manual activity, photos, note records and confirmed linked reminder Done; `V298ReminderGroupStore` group links only. Avoid double counting same Done occurrence in both Group and Space.
- Recommended "Personal Progress" within future Personal Home from #08, not mixed into Creator XP leaderboard. Optional recognition examples: **First Moment for Myself** (first confirmed Personal action), **Made Time to Rest** (user-confirmed rest routine), **A Week of Reflection** (at least one voluntary private note on 7 distinct dates, not 7-day perfect streak), **21-Day Commitment** (completed designated challenge according to its original confirmation policy), **Space Explorer** (first manual Space entry).
- Never award health "improvement" badges from photos, diagnosis, sleep values or unconfirmed notification. A missed day does not subtract points or trigger guilt; users can disable milestones and companion celebration.
- Default remains privacy-respecting; no cross-account restoration promises. If optional Personal celebration is enabled, do not speak/display sensitive titles over locked screen or other users; surface only inside private unlocked Personal UI, or show generic "You made time for yourself".
- Migration: new optional achievements based on provenance receipts, not inferred old values. Compare ability to derive each milestone truthfully before launch. Data export/delete/privacy approach must be defined.

## 5. Daily Brief — actual current scope and redesign

**Current:** `CreatorDailyBriefEngine.build` chooses first ranked active Creator project via `CreatorPriorityEngine`, action and reason, active/due-today/overdue counts, upcoming Creator reminder count (24h), up to four context nudges, up to six upcoming project/weekly items across seven days. `V15DailyBriefScreen` shows metric row, focus card, alerts and linked calendar rows. Does **not** automatically make schedule changes or merge Personal by default.
**Gaps:** single automatically selected focus; no owner-confirmed 1–3 key intentions, no clear readiness/context from #06, no direct planned-vs-actual day review, the header "reminderCount" is a creator-task reminder projection and not the entire Personal/Creator reminder health.
**Plan:**
1. One clean morning brief: "Today matters" with **one suggested focus + two editable priorities** selected by owner; why chosen, deadline, format, stage and honest time, if stored. User may decline and select different project.
2. **Needs attention** ordered by real urgency: due/late project, blocked work, unanswered verified reminder, pending review (when available), not noisy repeated nudges.
3. **Next actions** one-tap Open Project at exact stage / Open Reminder / Review Weekly Plan / Open Content Calendar; no auto-stage completion or rescheduling from merely reading.
4. **Daily wrap-up** lightweight optional review of actual events and deferred items; link back to Progress Journey. Never score a quiet day as failure.
5. Personal section optional, private, defaults to a separate "Personal" card only with owner opt-in and lock-screen redaction. No mixing personal care with deadline guilt.
6. Clear states: "Updated locally at ...", stale/offline; time-zone changes, app resume, missing project, empty day, pending failed sync. No paid AI narrative requirement; a rules-based prioritized brief is sufficient initially.

## 6. Content Calendar — actual current scope and redesign

**Current:** `CreatorContentCalendarEngine.upcoming` builds a 14-day forward agenda of **real project due dates** and **uncreated weekly-schedule occurrences**; de-duplicates template keys already represented by real projects. `V15ContentCalendarScreen` offers horizontal Today/13-day chips, All dates, list grouped by date, and up to six recent overdue projects, with existing onOpenProject or Open Weekly Plan callbacks. It is **not** a visual week/month production calendar nor verified social-platform publishing.
**Plan:**
1. Three calendar views: **Week**, **Month**, **Agenda** with persistent user preference. Filters for platform, format, status, and Creator project vs weekly-plan template. Date changes must update grouping and source links correctly across timezone/reboots.
2. Color/accessibility semantics distinguish **Project deadline**, **Creator reminder**, **Stage milestone** (after #06), **Weekly plan placeholder**, **Published (creator-confirmed)**. No false live publishing confirmation. Ensure text+icon status, not color alone.
3. Present overdue items in a distinct truthful queue even when due date falls before visible 14-day window; on project entry provide safe actions to review/reschedule through existing authorized workflow.
4. Format awareness: e.g. video long-form stage deadlines, Shorts, podcast release, carousel draft/review; do not invent stage dates from a single final due date or force autogenerated schedule without consent.
5. Tap item -> details preview with time/platform/format and clear source, then Open Project or Weekly Plan. A weekly plan placeholder is **not** a stored project, so do not give it a project ID or Done/Publish action.
6. Month screen is read-only planning at first; inline reschedule or drag/drop later only after confirmation, rollback/error, conflicts and reminder rearm tests. Never change schedule by merely dragging in prototype.
7. Calendar integration must preserve existing Google Calendar export/settings semantics, not imply bidirectional sync/import/auto-posting. Avoid third-party paid dependencies.
8. Performance: efficient month aggregation and bounded list, large font, 320dp phones/tablets, orientation, daylight-saving/time-zone tests.

## 7. Competitive/product research (official 2026 documentation)

| App | Verified design strength | Backlot fit |
|---|---|---|
| Todoist Productivity/Karma | Progress views for daily/weekly completions, points, goals and streak visibility; user can change goals | Explainable progression, but avoid pressure-based streaks in Personal: https://www.todoist.com/help/todoist/features/use-the-productivity-view-in-todoist-6S63uAa9 |
| Sunsama | Guided daily planning, prioritized intention, review/undo and end-of-day reflection; celebration controls | Keep Daily Brief actionable and owner-controlled, not generated prose alone: https://help.sunsama.com/docs/usage-guides/daily-planning/ ; https://help.sunsama.com/docs/usage-guides/daily-highlights/ ; https://help.sunsama.com/docs/settings/user-settings/ |
| Buffer | Week/month calendar views and filters; separate drafts/approvals/scheduled/published; explicit publishing states | Multi-format production calendar should be visually clear and honest about statuses: https://support.buffer.com/articles/how-to-use-buffers-calendar-feature-FSSKbH32DN |
| Asana | Calendar vs timeline distinction, milestone/deliverable/dependency model | #06 versioned stages feed optional meaningful milestones, not every stage as a publish event: https://help.asana.com/s/article/plan-and-execute-projects-with-timeline |

These are feature descriptions from publicly documented products, not new hands-on usage studies. No claim that Backlot is unique or already on feature parity.

## 8. Single source-of-truth and privacy design

```text
Quick Actions views (composed read-only)
  ├── Progress Journey: CreatorWorkflowTimeline + Projects/Ideas + Personal receipts opt-in
  ├── Creator Progress: CreatorRewardStore ledger + V22 derived snapshots (separate truth)
  ├── Personal Milestones: explicit opt-in, local Personal receipt/Challenge/Space evidence (new view)
  ├── Daily Brief: PriorityEngine + Creator tasks + actual reminders/nudges (no auto mutations)
  └── Content Calendar: Creator due dates + WeeklyPlan templates + optional #06 stage milestones
```

Never treat future planned items as completed history; never infer actual published content from a calendar slot; never treat unsaved reward animation as a committed XP event. Preserve authentication privacy, no cross-profile reward leak and no fabricated personal health results.

## 9. Delivery work packages and acceptance gates

| Gate | Work | Approval/evidence required |
|---|---|---|
| Q0 | Source baseline, reward/Personal receipt provenance, UX and widget navigation inventory | Frozen V337 evidence, no production changes |
| Q1 | Founder signoff on Progress Journey and brief/calendar wireflows, companion 3s storyboard, Personal achievements philosophy | Design approvals, reduced-motion alternative |
| Q2 | Read-only Journey enhancements, timeline pagination, filters and source navigation | Unit/Compose tests, historic/missing data cases |
| Q3 | Creator celebration controller (confirmed reward event -> chosen Spark/Mira +3s) | Event-key idempotence, timing/queue and reduced-motion device tests |
| Q4 | Opt-in Personal Milestones based on real receipts | Privacy/storage/consent/reinstall migration review and acceptance |
| Q5 | Daily Brief prioritized/editable intentions + wrap-up | No accidental task changes; correctness of due/overdue and recommended next action |
| Q6 | Week/month/agenda Calendar and stage-aware milestones | Platform statuses truthful; DST/timezone/reschedule/reminder regressions |
| Q7 | Integration with #06 Content Workspace and #08 Personal Home; phone/tablet accessibility QA | Complete synthetic project/reminder/Personal journeys and owner signoff |

**Proposed acceptance criteria:**
1. Reopening any Quick Action never awards new XP, changes data or marks a reminder Done.
2. An actual committed reward creates **one** 3-second chosen-companion celebration; no duplicated overlay on app restore, preview or replay.
3. Original 6 achievement IDs, stored ledger, XP grants and leveling remain intact after upgrade; no regression in idempotence/reconciliation/backfill.
4. Personal milestones privacy opt-in; no sensitive titles or image paths in Quick Actions, screenshot/share, lockscreen or cloud data.
5. Progress Journey filters and calendars match actual timestamps, don't count planned tasks and do not hide source limitations.
6. Daily Brief recommendations match up-to-date task data and don't claim edits completed without user confirmation.
7. Calendar slots are visibly templates vs real projects vs creator-confirmed publication; no silent schedule shifts or reminders duplicated.
8. 200% font, TalkBack, reduced motion, screen interruption, process death, offline, 320dp phones, tablet, large history and time zone boundary cases pass on physical device.
9. No unapproved paid API, new content publishing or health analytic cloud upload.
10. All work stays in isolated implementation packages and requires separate merge/release approval.

**Recommendation:** Approve the phased visual/UX+data approach, not a wholesale rewrite. First deliver Journey/Creator celebration and Calendar/Daily Brief actionable improvements, then Personal milestones after explicit privacy design review. Keep the four features distinct but navigable and grounded in one authoritative factual history.
