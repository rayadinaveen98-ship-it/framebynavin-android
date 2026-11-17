# Backlot UX07F → UX08 — Quick Actions, Progress Journey, Achievements, Daily Brief and Content Calendar

**Prepared:** 2026-11-17. **Stage:** UX07F approved scope handoff; this document does not claim UX08 work is implemented or tested.  
**Exact source anchor:** UX07E draft PR #415 `08ee9ad1acb1e48cd406ee5cff8958815eb69572`.  
**Protected stable:** V337 P0 `815ba69775f75b6e5f25bb838c2c17b4f09c203e`, not merged with this staging lineage.  
**Owner decision:** improve the EXISTING valuable Quick Actions, Progress Journey, Creator Progress, Personal achievements, Daily Brief and Content Calendar rather than simplify away features; chosen companion celebrates an **actual credited** action for about **3 seconds** with polished animation, fades away, with optional reduced motion/accessibility support. No new service fees or silent cloud account work.

## 1. Verified source owners and existing baseline

| Product area | Existing real source / UI | Confirmed behaviour or guard |
|---|---|---|
| Quick Actions hub | `ui/V161QuickActionsHubUi.kt` | Existing New Project, Quick Capture, unresolved publish-late action choices (+30 min / new time / Skip), Daily Brief, Idea Vault, Content Calendar, Weekly Plan, Automation, Trending Moment, Reminders, Progress Journey, Creator Progress, Settings; preserve callback tags and routes |
| Progress Journey | `ui/V282ProgressJourneyUi.kt`, `ui/V282ProgressHomeStrip.kt`, `data/ProgressJourneyEngine.kt` | Source-built Creator project/idea/workflow timeline; month/week/day navigation and events; separate optional Personal accepted-Done overlay (OFF by default). **A calendar is not an achievement engine** |
| Creator XP and rewards | `data/CreatorRewards.kt`, `data/CreatorRewardsV22.kt`, `data/CreatorRewardReconciliation.kt`, `ui/V22CreatorRewardsUi.kt` | Local reward ledger with event keys, reconciliation, levels/traits/weekly momentum, achievements, recent reward history and original XP toast; no points for simple views |
| Stage celebration | `ui/V2284StageCelebrationPolicy.kt` | Only genuine `PROJECT_STAGE_COMPLETED` ledger evidence should initiate specific stage celebration; other reward events need independently verified rules before celebration |
| Companion | `ui/V2214HomeCompanion.kt`, `ui/V217LiveCompanionCharacter.kt`, selected companion presentation controls and current Spark/Mira assets | Preserve owner-selected companion, character consistency, accessibility, focus behaviour and existing suggestions; no unapproved third character or new asset store |
| Daily Brief | `data/CreatorDailyBrief.kt` with current calling route | Actual daily due/publish context plus calendar items; show real eligibility and sources, don't invent earnings, publication or scheduled obligations |
| Content Calendar | `data/CreatorContentCalendar.kt`, `data/CreatorCalendarNavigationPolicy.kt` | Merges real Creator project dates and recurring weekly slots, de-duplicates scheduled occurrence keys, groups by date. Do not schedule a second real project or alter publish times simply by viewing |
| Personal authentic progress | UX07B–E read-only My Day, Spaces, Group and Challenge progress; `PersonalCompletionJournal`, `Phase1HabitChallenge.confirmations`, Room manual entries | Three independently owned progress sources. UX08 Personal achievements must be explicit source-aware, idempotent, private by default and **must not call journal viewing success** |

The baseline must be audited again in UX08A with actual caller/route counts before a single button is moved. Names above are source-verified; this document is a **handoff** rather than runtime validation.

## 2. Proposed UX08 source stages (no expansion beyond approved point 08)

### UX08A — Quick Actions, reward and timeline owner inventory [documentation/protection]

- Trace all `V161QuickActionsHubUi` callbacks into Home/Creator, Progress Journey, Creator Progress, Daily Brief, Content Calendar, Week Planner, Automations and Personal.
- Map actual saved data writers and event keys; inspect reusable UI/theme, companion selection, animation flags and existing attendance of 3-second overlays.
- Build exact button matrix, privacy/accessibility and source-dedup guards, no changes to other owners.

**Exit:** file-by-file action/writer inventory and isolated UX08B source brief.

### UX08B — Progress Journey advancement [read-only presentation]

- Improve day/week/month navigation, history chronology, milestones and meaningful event detail from existing `ProgressJourneyEngine` sources.
- Filter/source-label creator vs opted-in Personal correctly; preserve OFF-by-default Personal setting and allow undo/toggle with no new event write.
- Add empty/error/read-only states and compact quantitative summaries only if supported by actual past events. Restore exact project/idea deep links and selected dates.
- Do not merge Activity Center, Content Calendar or Challenge history into Creator events without explicit owner-specified mapping.

**Exit:** better event navigation without changing authoritativeness or duplicated counts.

### UX08C — Creator + Personal achievements, chosen companion celebration [bounded new presentation and exact-event rules]

- Audit immutable/idempotent `CreatorRewardLedgerEntry.eventKey`, credited-event type and `CreatorRewardReconciliation`. Only actually persisted credit may start celebration.
- For each eligible event, display chosen compatible companion for **approximately three seconds** with tasteful appearance, joyful/appropriate animation and smooth disappearance. A new screen visit, retry, process reentry, restored ledger or re-compose must **not replay** the celebration or mint XP.
- Carefully define allowed event sources: Creator idea created, authoritative project stage completion, validated publication/checkpoint and other *already credited* Creator ledger events. For Personal, **propose and explicitly review** which actual accepted Personal Done, owner-confirmed Challenge dates, or owner-confirmed manual Space check-ins are eligible; do **not** reward note creation, media browsing, viewing reminder/space, import, dismissal, snooze or attempted alert.
- Personal reward expansion needs a privacy-safe source discriminator and stable event key; it **cannot re-use Creator project XP keys** or leak personal titles. Define retention, rollback, duplicate event handling, anti-spam and whether Personal uses shared vs separate XP BEFORE a persistent schema/write. No paid service.
- Respect user-selected Spark/Mira visual design, Companion off state, reduced motion, screen reader announcement, no auto-playing audio, and a safe queue for overlapping events; no full-screen interaction-blocking celebration on a reminder response.
- Preserve current reward milestones, traits, progress screens and any existing toast path. Do not fabricate achievements from old records during import unless an approved idempotent backfill is explicitly tested.

**Exit:** exact-event rules, user experience and available original owner actions preserved. Approval gate for new persistent Personal rewards.

### UX08D — Daily Brief 2.0 [truthful priority-oriented UI]

- Use actual `CreatorDailyBriefEngine`: today's true due work, meaningful imminent deadlines, genuine creator activity, live but non-invasive reminder context, and source-backed calendar entries.
- Show "Today / Upcoming / Needs review / Plan next" with grouped actionability, no invented deadlines or broadcast claims.
- Make loading, empty, late, timezone and permission-degraded states honest. Deep links into original project, reminder and weekly slots only.
- Do not add always-on remote AI-generated summaries or paid LLM calls without explicit cost review and founder approval.

### UX08E — Content Calendar 2.0 [source-backed planning]

- Preserve `CreatorContentCalendarEngine` schedule-occurrence dedup, project owners and weekly plan. Improve month/week/agenda switching, date selection, planned vs actual states, source tags and edit navigation.
- Support proper empty/missing week-slot handling, timezone rollover, long running content plans, tablet/phone density and low-motion.
- Calendar view does not autonomously modify project publish dates, scheduled Android alarms, analytics or YouTube status.

### UX08F — Quick Actions hierarchy + UX09 handoff [bounded integration]

- Reorganize/hide complexity progressively within `V161QuickActionsHubUi.kt` while retaining every original button (New Project, Quick Capture, late actions, Daily Brief, Idea Vault, Content Calendar, Weekly Plan, Automation, Trending Moment, Reminders, Progress Journey, Creator Progress and Settings).
- Connect UX08B–E to existing navigation destinations and verify Creator/Personal boundaries. Keep compact surfaces and original tokens; comprehensive layout consistency belongs to UX10.
- Static diff, provenance/permission audit, regression matrix and UX09 cross-device auto-sync handoff; **no merge/release**. UX09 migration and the new Supabase project are separately approved but must not be silently implemented as part of UX08.

## 3. Functional acceptance and high-risk scenarios

1. Every Quick Actions button opens the original correct destination or shows its honest disabled/permission state; new UI does not remove existing menus.
2. Progress Journey event dates are real, source-tagged and idempotent; Personal overlay remains opt-in OFF by default.
3. Creator rewards reflect credited ledger events, never navigation/notification; no replay on startup, process restore, renderer re-composition or imported backup.
4. Companion celebration uses the chosen supported character; takes roughly three seconds, exits smoothly and never interrupts full-screen Reminder Done/Snooze controls. No animation required to complete an action.
5. Personal progress and achievements remain private, independent of Creator XP unless the founder approves an exact integration policy; no added payments, cloud upload or analytics feed.
6. Daily Brief and Content Calendar share original project/week schedule sources and avoid duplicate project deadlines.
7. All new presentation UI accessible at 320dp/large-font/TalkBack, tablet and reduced motion.
8. UX08 source history remains stacked on UX07F. V337 P0 ancestor reconciliation and all UX01–UX10 tests deferred, but **not waived**.

## 4. Required decisions before new persistent Personal achievements

**Founder gate, not an immediate question blocking UX08A/B:** choose whether Personal achievements are (A) a separate private points ledger, (B) non-points milestones and celebrations, or (C) a shared Creator/Personal XP profile. Product/safety recommendation to evaluate first: **B**, grounded in original owner confirmations and reversible, with no creator-score contamination or sensitive habit pressure. A decision and data contract are required before new persistent reward keys. Existing Creator rewards may be polished without this decision.

## 5. Change control / status

**UX07F:** read-only source audit and bounded handoff. **UX08A–F: not yet implemented, tested or approved for merge.** Owner-stable V337 remains untouched. Android regression, receipt migration, backup/restore, P0 ledger forensics, UI performance and owner physical certification occur only at the post-UX10 integrated gate. First UX08A may begin on a separate draft based on UX07F once the current draft source ancestry is verified.
