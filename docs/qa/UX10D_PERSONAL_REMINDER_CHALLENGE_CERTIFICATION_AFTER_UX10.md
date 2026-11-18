# Backlot UX10D — Personal/Spaces/Challenges/Reminders certification AFTER owner-approved integration

**Date:** 2026-11-17.
**STATUS: DEFERRED QA SPECIFICATION ONLY — 0/40 checks executed; 0/10 new source tests executed.**
No Kotlin/Gradle/Compose build, JVM/CI, Android instrumentation, physical-device screenshots, signed APK, merge or release.
**Exact stacked parent:** UX10C draft PR #425 HEAD `04d3dfae46e6f2b9280ee4775cd89066dcf2c1d6`.
**Protected stable:** accepted V337 P0 HEAD `815ba69775f75b6e5f25bb838c2c17b4f09c203e`, unchanged, with a separate 19-commit / 22-path protective delta needing reviewed reconciliation.
**UX09:** user-deferred multi-device sync, not done. No account/server/project/schema/secret changes in UX10D.

| ID | Priority | Actual scenario | Required pass evidence |
|---|---|---|---|
| D01 | P0 | Personal Reminder hub from Home/Quick Actions | Original Personal Today editor and source routes, no duplicate store |
| D02 | P0 | Reminder hub Personal/Creator switch | Same original callbacks, no wrong-domain reminder/privacy leak |
| D03 | P1 | Reminder tabs at 320dp, 200% font, light/dark | >=56dp target, readable labels, active state distinct from neutral |
| D04 | P0 | Personal four-step composer Purpose/Schedule/Alert/Review | Exact original validation, back-to-visited-step only, no skip-forward |
| D05 | P1 | Composer scope chips and labels | Original source counts, 72dp+ targets, 2-line text, horizontally reachable |
| D06 | P0 | Personal purpose option tap | Same original selected purpose without writing/saving merely on preview |
| D07 | P1 | Personal purpose at 2.0 font scale | 220dp scrollable cards readable, selected accent, original purpose values |
| D08 | P0 | Personal reminder saved / cancelled | Same exact ID/timing, no unintended Save on Cancel |
| D09 | P1 | Personal Save/Cancel at 320dp/2x font | Stacked full-width 56dp buttons; original test tags/disabled saving state |
| D10 | P0 | Personal Today overdue/not-due/paused row | Real PersonalAgendaPolicy and delivery-health source, no false Done |
| D11 | P0 | Personal Done from reminder row | Invokes original ACTION_DONE delivery-token owner only once |
| D12 | P0 | Personal Snooze from row | Invokes original ACTION_SNOOZE with original duration/id; real alarm rearm checked |
| D13 | P1 | Personal Done/Snooze at 320dp and 200% font | Adaptive stacked >=56dp actions; both accessible, no clipped label |
| D14 | P0 | Cancel, repeated tapping, stale response, missed occurrence | Original token safety / idempotent response, no duplicate completion |
| D15 | P0 | Creator reminders selection/edit/snooze/pause/reschedule | Original recipient IDs and callback flows unchanged |
| D16 | P1 | Creator reminder list at large text | No 9sp label, active/selected red and neutral boundaries distinct |
| D17 | P0 | Creator/Personal full-screen alert on locked Android 15 | Original notification token, permission, exact-alarm and wake lifecycle work |
| D18 | P0 | Original voice 3x / music / custom song / voice+music parity | Correct original playback policy; UX10D does not claim it rebuilt audio |
| D19 | P0 | 21-day single-activity Challenge first create | Exactly one genuine repeated activity, no seeded entries/completions |
| D20 | P0 | 21-day Challenge start date/today/strict/flexible | Original timezone/day-number logic and receipt date unchanged |
| D21 | P1 | Challenge at 393dp and large text | Four-column 21-date grid, no empty row stretch or wrong day ID |
| D22 | P1 | Challenge wide tablet normal font | Seven-column grid with all twenty-one dates, original source state |
| D23 | P1 | Tap day 1/7/14/21 with TalkBack | Accurate day status description and original day-detail navigation |
| D24 | P0 | Confirm Done, Undo, Pause, Resume Challenge | No automatic streak/XP from merely viewing, canonical receipt state |
| D25 | P0 | Challenge 10-minute Snooze and missed alerts | Original Phase1HabitAlarmPlan/ledger/rearm and Snooze rules unchanged |
| D26 | P1 | Challenge Done/Undo/Snooze buttons 2x font | Buttons >=56dp, original visibility/disabled/archived guards |
| D27 | P0 | Legacy V297 plan/day edit, export/import/merge | Original saved 21 days and import conflict protection intact |
| D28 | P1 | Legacy Challenge Previous/Next at 320dp | Stacked controls, first/last disabled, correct 0..20 state |
| D29 | P0 | My Spaces list/open/archive/delete and link routing | Exact Room Space ID, no linked Personal reminder/notes loss |
| D30 | P1 | Space progress at 320/393dp | Three real metrics stack, true log/day/link counts, no invented Done |
| D31 | P1 | Space progress at 800dp normal font | Three actual metrics remain side-by-side, readable captions |
| D32 | P0 | Private Space notes, manual log and photo actions | Existing private data, undo/recovery and confirmation semantics preserved |
| D33 | P0 | Photo Journal user-selected photo add/remove/recover interrupted | Metadata stripping and private local storage protected; no cloud upload |
| D34 | P1 | Photo Journal privacy notice and Add at 200% font | Clear device-local/cloud-exclusion wording and >=56dp Add |
| D35 | P0 | Reminder Groups edit/delete and membership linking | Same exact IDs, group loaded/read-error and empty states |
| D36 | P0 | Group Pause All/Resume All confirmations | Original bulk guard, stale-link validation and confirmation before mutation |
| D37 | P1 | Group action controls at 320dp/2x font | Four original buttons 1/row; all tags and enabled states intact |
| D38 | P1 | Personal/Creator/Challenge UI high contrast, reduced motion, TalkBack, tablet | No invisible warning/selected state, photo-title leakage or inaccessible action |
| D39 | P0 | Accepted V337 P0 reconciling alarms/ledger/full-screen/snooze/restore | 19 accepted commits and 22 affected paths accounted for on exact candidate |
| D40 | P0 | Full candidate Kotlin/JVM/CI/instrumented + founder physical APK acceptance | Evidence tied to exact merged candidate SHA and APK hash, founder release signoff |

## Pure source policy tests authored, NOT executed

`app/src/test/java/com/framebynavin/app/ui/UX10DPersonalPresentationPolicyTest.kt` defines ten deferred tests:
1. 320/393dp Space metric layout and three real metric entries;
2. large-font Space metric layout on tablet;
3. responsive Personal Done/Snooze and group controls;
4. legacy Challenge navigation adaptivity;
5. all twenty-one original Challenge dates retained and readable in four columns;
6. seven-column wide-date grid and four-column large-text fallback;
7. purpose card width 148/220dp responsive;
8. invalid fontScale fails to readable layouts;
9. shared 48dp/56dp touch sizes and semantic selected border contrast;
10. original Personal editor Save/Cancel threshold retained.

**0/10 run.** These are pure presentation policy tests and not Android end-to-end proof. The original Creator/P0/Space/Photo/Group/Challenge stores must also pass their own original regression and security suites under an approved integrated candidate.

## Stop-the-line

Never promote if full-screen audible alert/voice/Snooze, completion ledger, backup/restore or exact Android 15 readiness regresses; if Personal Done is acknowledged without a genuine user response; if private photo/media metadata leaks; if selected group bulk actions operate on stale or missing reminder IDs; if Challenge dates change; if keyboard/focus hides save, cancel or selected warning; if the accepted V337 P0 fixes are missing; or if the postponed UX09 feature is falsely advertised as complete.

**Open known limits:** UX10D focuses on source-owned presentation wrappers, not exhaustive physical review of every deep Personal Settings screen or cinematic reminder/theme effect. The visual QA and cross-screen consistency audits remain pending UX10E/UX10F. UX09 sync remains deferred, not release-ready.

**Current result:** UX10D source stage only. **0/40 scenarios executed, 0/10 new tests executed, 0 APK built.** Next UX10E Settings/Insights/widgets/system states, then UX10F global integration and founder-gated release path.
