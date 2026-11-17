# Backlot UX08F — Quick Actions + cumulative UX08 certification AFTER UX10

**Date:** 2026-11-17
**Status:** **DEFERRED QA MATRIX — 0 TESTS EXECUTED**. No Kotlin/Gradle compile, JVM, CI, Compose instrumentation, emulator, physical Android APK, merge or release.
**Exact base:** UX08E draft PR #421, HEAD `a1c13694bd0ce2a546b7912c780b00a137f6732f`.
**Protected owner stable:** `815ba69775f75b6e5f25bb838c2c17b4f09c203e` V337 P0, not modified.
**Separate blocker:** V337 stable accepted 19 corrective commits across 22 paths outside UX draft lineage — reconcile before the integrated release candidate and actual owner physical certification.
**Founder-approved testing:** stage all UX01–UX10 sources; execute one full integrated Gradle/JVM/UI/Android/physical campaign AFTER UX10, never invent a pass.

| ID | Priority | Action / scenario | Required pass evidence |
|---|---|---|---|
| F01 | P0 | Open Quick Actions floating control from Home | Original fully expanded bottom sheet, dismissal and Back without nested second navigator |
| F02 | P0 | New Project shortcut | Original wizard, same `quick-action-new-project` route, no ghost save/XP on open |
| F03 | P0 | Quick Capture shortcut | Original capture and successful saved idea owner, cancel creates nothing |
| F04 | P1 | Projects loading on initial app entry | No misleading 0 past-deadline count; other original shortcuts still reachable |
| F05 | P1 | True loaded empty Creator projects | Correct 0 source summary with no overdue action card |
| F06 | P0 | PLANNED / WORKING, DONE, SKIPPED and archived tasks mixed | Counts only original active statuses; never labels completed project overdue |
| F07 | P0 | Ten overdue active Creator projects | Correct real total, first four earliest by actual deadline, more via Daily Brief |
| F08 | P0 | Original project +30 min action | `CreatorViewModel.publishLate` moves exact project due +30min and reconciles original Android schedule; no extra task |
| F09 | P0 | Original New time action | Original date/time picker, future validation, exact `rescheduleDeadline`; cancel makes no write |
| F10 | P0 | Tap Skip overdue project | Opens confirmation first, no task or alarm mutation before confirmation |
| F11 | P0 | Cancel Skip dialog / system Back | Project remains as-is, original alerts intact |
| F12 | P0 | Confirm eligible Skip | Exact active original ID marked SKIPPED once, original Creator alerts disabled, original project retained |
| F13 | P0 | Task rescheduled/completed/archived/deleted while Skip dialog visible | Confirm disabled or recheck fails; no stale source write |
| F14 | P0 | Duplicate/skewed ID or changed owner while Skip dialog visible | Fail closed; do not skip wrong task or alter another account |
| F15 | P0 | Quick Actions closes/reopens mid-Skip | Pending ephemeral confirmation not silently committed or replayed |
| F16 | P0 | Daily Brief shortcut | Original UX08D overlay, genuine priority/overdue/today/calendar data, source actions preserved |
| F17 | P0 | Content Calendar shortcut | Original UX08E overlay, Week/Month/Agenda future planning, correct Project vs Weekly route |
| F18 | P0 | Idea Vault shortcut | Original ideas tab, no duplicate capture/XP; live ready count |
| F19 | P0 | Weekly Plan shortcut | Original weekly slot editor/enable/save/delete/reset, recurrence identity preserved |
| F20 | P0 | Automation shortcut | Original automation workspace and background scheduling controls; no new defaults |
| F21 | P0 | Progress Journey shortcut | Original UX08B Week/Month/Journey recorded history and explicit Personal opt-in |
| F22 | P0 | Creator Progress shortcut | Original ledger XP/level/achievement details and non-replayed UX08C companion feedback |
| F23 | P0 | Reminders shortcut | Original Creator and Personal hub/notification response tokens, no duplicate alarm |
| F24 | P0 | Trending Moment shortcut | Original release-day planning, no new CreatorTask on mere viewing |
| F25 | P0 | Settings shortcut | Original Account/Preferences/Permissions and still-gated backup/restore surfaces reachable |
| F26 | P1 | All 12 legacy `quick-action-*` semantics/test tags | One intended visible shortcut per tag; no accidental duplicate nav destination or missing action |
| F27 | P1 | Quick Actions overdue + task mutation while full-screen Reminder arrives | Reminder response wins; no alert/action interception, no stale skip |
| F28 | P1 | 320dp width and 200% font scale | Primary actions stack; all 12 links reachable; actions 48dp+, no clipped/hard-bordered tiny inputs |
| F29 | P1 | Tablet/landscape, system nav bars, keyboard and TalkBack | Logical focus order, readable subtle shortcut borders, labels/action meaning accessible |
| F30 | P1 | 5k Creator projects, 1k ideas | Bounded overdue cards, source count accurate, no ANR or needless recreation of app sources |
| F31 | P0 | Read Quick Actions several times | No new XP/achievements, reminders, cloud writes, posts or projects just from opening |
| F32 | P0 | Creator rewards imported/reconciled and Quick Actions Creator Progress opened | No false XP, no historical companion celebration, correct real stored ledger |
| F33 | P0 | Private Personal/Challenge/Spaces records | No personal title/photo/note leaked into Creator Quick Actions summary or XP label |
| F34 | P1 | UX08B workflow timeline source corrupt/unavailable | Original known false-empty decoder gap documented and addressed before final history-integrity certification |
| F35 | P1 | UX08D/E Weekly Plan source unavailable | Honest partial state; no false "all source data loaded" claim; typed-read owner gate disposition |
| F36 | P0 | UX08E 80-row pages + original weekly occurrence keys | Every source item reachable, no duplicate materialized Creator project/plan task |
| F37 | P0 | UX09 cloud sync plan review | Accurate existing snapshot vs real per-record sync inventory; no unapproved Supabase switch, RLS/schema write or paid service |
| F38 | P0 | P0 V337 accepted 19-commit/22-path delta fully integrated | Snooze/ledger/readiness/restore fixes and original Android 15 instrumented regression preserved |
| F39 | P0 | Combined all UX01–UX10 Kotlin, JVM, CI, instrumentation, accessibility and Android tests | Real PASS logs from same exact SHA, unresolved P0 count 0, explicit P1 disposition |
| F40 | P0 | Owner installed signed APK on exact Android physical device | APK hash/source SHA, owner functional and visual signoff, explicit release authorization before promoting stable |

## Six staged policy tests (0 executed)

`app/src/test/java/com/framebynavin/app/ui/UX08FQuickActionsPolicyTest.kt` defines **six** unexecuted pure tests for:
- actual active records vs DONE/SKIPPED/archived;
- chronological first-four overdue preview and true full count without mutation;
- exact original-ID/archived/rescheduled/skipped safety;
- missing, stale or duplicate ID fail-closed;
- zero/exact-now deadlines not counted overdue;
- the 12 original canonical shortcut tag identifiers.

**Tests authored are not tests passed.** UI instrumentation must additionally exercise each real callable shortcut in `FrameByNavinV101BApp.kt`, `PControlCenter` and `V161QuickActionsHubUi.kt`; pure test tags in source do not prove real button routing.

## Cumulative UX08A–F acceptance & final release gates

1. **UX08A:** Existing owner callbacks, dates, XP provenance, 36 deferred cases and no lost Quick Actions surface.
2. **UX08B:** Real Creator events/private Personal opt-in, historical project/idea links, 40 deferred cases and known timeline decoder gap.
3. **UX08C:** Newly credited Creator XP and saved Spark/Mira 3-second nonblocking celebration, 40 deferred cases; Personal persistent reward policy stays founder-gated.
4. **UX08D:** Actual Creator priority, overdue/today/upcoming from original engines, 40 deferred cases; Weekly typed-read state gap.
5. **UX08E:** True original Creator projects/weekly occurrence keys, bounded Week/Month/Agenda/paged results, 40 deferred cases.
6. **UX08F:** Original 12 route/action tags, 3 overdue mutations, explicit Skip confirmation and honest loaded summary, **40 deferred cases**.

**UX08 total authored cross-stage QA requirements: 236 (36 + 40×5), 0 executed.** This total is a document/test-plan count, not count of compiled tests or unique runtime methods. UX07's earlier 176 + 26 scenarios remain a separate cumulative gate.

### NO-GO

No release when: original Creator/Personal/Challenge/Space data lost; accepted P0 alarm/backup/restore fix omitted; duplicate schedules/XP credited from views; stale Skip changes wrong project; Personal data leaked to Creator progress; corrupt read claimed successfully empty; second-device sync falsely represented complete; unapproved credential/backend migration, paid service, silent destructive restore or lack of owner physical APK signoff.

**Current verdict:** UX08F source/QA documentation can be staged as draft; **0/40 UX08F cases run**, **0/6 new tests run**, and no actual stable/merged/app-tested state is asserted. Next **UX09A cloud/domain/coverage architecture inventory** under separate authority and eventual UX10 UI consistency.
