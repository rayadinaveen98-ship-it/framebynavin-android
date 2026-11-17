# Backlot UX08D — Daily Brief source and interaction certification AFTER UX10

**Date:** 2026-11-17.
**Status:** DEFERRED QA DESIGN — **0 tests executed**; no Gradle/JVM, CI, emulator, instrumentation, physical device, APK, merge or release.
**Exact parent:** UX08C draft PR #419 at `d5d36ae335d8b04288bd956858ad6cfe5995d043`.
**Protected owner stable:** V337 P0 `815ba69775f75b6e5f25bb838c2c17b4f09c203e` unchanged; accepted 19-commit/22-path P0 corrective delta remains an integration blocker.
**Founder workflow:** Stage approved UX01–UX10 sources then run ONE integrated campaign with all prior source tests, owner Android regression and actual physical acceptance, including accessibility/privacy/reliability.

| ID | Priority | Source or user action | Required device/source observation |
|---|---|---|---|
| D01 | P0 | Open Daily Brief via Quick Actions | Original overlay and Back; no change to Home or Creator project data |
| D02 | P0 | Daily Brief deep link / widget entry | Original POverlay Daily Brief works without duplicate screen |
| D03 | P0 | VM projects are not loaded yet | Shows loading, **not** a verified empty project queue or zero counters |
| D04 | P0 | Upstream Creator task read fails after loaded flag set | Failure not silently treated as verified empty; typed owner-read limitation tracked separately |
| D05 | P1 | Loaded truly empty tasks and Weekly Plan | Authentic empty state with available Weekly Plan and Calendar routes |
| D06 | P1 | No active projects but genuine weekly-plan slot upcoming | Focus empty, upcoming slot shown; no fictional project created |
| D07 | P0 | Active CRITICAL project with high urgency | Original `CreatorPriorityEngine.rankActive` chooses focus; no reshuffle by UI |
| D08 | P0 | Original focus project tap | Opens the exact canonical saved project, not a cloned or new project |
| D09 | P0 | Multiple overdue active Creator projects | Genuine past deadlines counted, earliest shown, no task status change |
| D10 | P0 | Focus project is among overdue | Focus visible once in primary card, still included in actual overdue count |
| D11 | P0 | Project due later today but not overdue | Separate due-later section, correct device-local deadline, original project ID |
| D12 | P0 | Focus project due later today | No duplicate task in secondary section, total still includes it |
| D13 | P1 | Deadline passed earlier today | Counted both in DUE DATE TODAY and PAST DEADLINE as original engine; explanation honest |
| D14 | P0 | DONE/SKIPPED/archived task with past due date | Not labelled overdue active project or automatically reopened |
| D15 | P0 | Critical active project with no deadline | Original ranking/nudge may focus, no fabricated due time |
| D16 | P1 | Reminder time passed but project not overdue | Original context nudge source and action preserved, no false delivery claim |
| D17 | P1 | Same project has nudge and belongs to overdue/today group | No repeated secondary row; focus remains primary only |
| D18 | P1 | 12 overdue projects / many nudges | Lists bounded to first 3/3 plus honest count/truncation; source records intact |
| D19 | P0 | Real future project deadline shown in next 7 dates | Exact actual time, project source label and original project navigation |
| D20 | P0 | Weekly template has no materialized project | Weekly-plan preview label and original Weekly Plan destination, no project minted on view |
| D21 | P0 | Weekly occurrence already represented by saved project ID key | Calendar engine shows canonical project once; no duplicate scheduled entity |
| D22 | P0 | Materialized weekly project versus template navigation | Project opens project, template opens Weekly Plan; never swapped |
| D23 | P1 | Completed project appears in original future calendar | Preserves original completed status, not falsely a new task to do |
| D24 | P1 | Upcoming events tomorrow through horizon end | Correct local 7-date horizon, today excluded from secondary Coming Up list |
| D25 | P1 | More than 6 genuine upcoming project/plan items | Shows first 6 chronologically, original total and View Calendar for rest |
| D26 | P1 | Seven-day window empty but many projects overdue | Brief explains upcoming empty; does not call whole account clear |
| D27 | P1 | Creator reminderCount enabled within next 24h | Counts configured original project times only; no claim that Android delivered |
| D28 | P0 | Personal reminder, Challenge alarm, Space journal changes | No Creator deadline/XP/brief project fabricated; privacy boundary intact |
| D29 | P0 | Open Brief, scroll, tap calendar/back | No Creator reward, stage completion, publication, project write or reminder scheduling |
| D30 | P0 | Source project rescheduled in original editor, reopen Brief | New due time reflected; no stale copy or second alarm |
| D31 | P1 | Calendar and Weekly Plan routes from Brief | Original POverlay destinations, original edit/save/disable controls available there |
| D32 | P1 | Repeated open/close, config rotation and background | No duplicate collectors/work, no UI crash, correct source snapshot after reopening |
| D33 | P1 | Time zone or midnight crossed while screen open | Date and due grouping refreshed appropriately or documented lifecycle limitation explicitly addressed |
| D34 | P1 | 320dp, 200% font, tablet/landscape, TalkBack | Primary focus, due/overdue sections and action buttons readable/reachable |
| D35 | P1 | Large library: 5k projects, 500 weekly templates | Original bounded list responsive; no ANR, data writes, duplicate tasks or heavy repeated decode |
| D36 | P0 | Source data format after display only | No new migration, DataStore keys, Room schema, backup codec or server request |
| D37 | P0 | Gradle/JVM compile plus new six source tests | Exact candidate SHA/JVM results, no Kotlin syntax or Compose overload regressions |
| D38 | P0 | Accepted V337 P0 lineage reconciled | All 22 accepted corrective paths accounted for; original Snooze/ledger/restore device checks pass |
| D39 | P0 | Owner physical device signs off integrated UX01–UX10 APK | Exact SHA/APK hash, test matrix evidence, real-screen checks, founder approval |
| D40 | P0 | Production release promotion decision | No unresolved P0, explicit release authorization, no paid provider or new credentials |

## Unexecuted source-test file

`app/src/test/java/com/framebynavin/app/ui/UX08DDailyBriefPolicyTest.kt` includes **6** deferred unit-policy test cases:
- real overdue vs later-today active task groups and excluded DONE/SKIPPED/archived
- primary focus not repeated in secondary groups
- real Weekly Plan preview when no matching CreatorTask exists
- source owner key de-duplicates materialized weekly project
- honest empty project+schedule state
- bounded summaries without source mutation.

**Execution:** 0/6 source tests, 0/40 QA matrix checks. Source authoring is not proof of compiled Kotlin, live Android app behaviour, correct delivery or reliability. Full original priority, nudge, week, content calendar and VM tests must also execute after UX10.

## Known integration risks and stop-the-line decisions

- The original `CreatorViewModel.weeklyLoaded` is private and does not offer a typed complete/read-error public UI state. UX08D can gate on `tasksLoaded` but cannot prove that a given empty weekly-plan snapshot is complete. Treat as P1 reliability gap until separately approved owner contract; do not reset data or pretend complete sync.
- The existing `CreatorDailyBriefEngine` uses local calendar-day boundaries; app kept open at midnight requires explicit lifecycle/time-change review.
- Opening Daily Brief or Calendar must never create a project from a weekly preview. Source metadata and original `scheduleOccurrenceKey` belong to the Creator weekly engine.
- UX08C Creator reward feedback remains isolated; no XP from merely opening Daily Brief.
- UX07 Personal Done/Challenge targets/Space notes stay with their own sources and are never inferred from Creator work.
- Existing Creator reminders remain original scheduler owners; a reminder timestamp is not a delivered notification or user-confirmed completion.
- **No-Go:** lost/duplicated task or weekly key, wrong project navigation, false publish/XP, silent data reset, private Personal leak, accepted V337 P0 regression, or release without integrated CI/device and founder signoff.

**Current stage:** source-staged draft, not tested, not merged or released.
**Next:** UX08E Content Calendar 2.0 source improvements, then UX08F Quick Actions integration, followed by UX09/UX10 and consolidated final testing.
