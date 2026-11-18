# Backlot UX08B — Progress Journey post-UX10 certification

**Date:** 2026-11-17. **Status:** ALL DEFERRED / **0 EXECUTED**. No Kotlin/Compose compilation, JVM suite, Android instrumentation, accessibility audit, physical APK or release.
**Exact parent:** UX08A docs draft PR #417, head `3dd611a5caf2084bfc56e46624d80d5b5c2a2e2c`.
**Accepted stable:** V337 P0 `815ba69775f75b6e5f25bb838c2c17b4f09c203e`, unchanged; its 19-commit/22-path correction lineage requires reconciliation before integrated device signoff.
**Owner sequencing:** after UX10, full integrated stage tests and physical certification on exact signed candidate. Authoring test source now is **not** a PASS.

| ID | Priority | Condition / action | Required pass observation |
|---|---|---|---|
| B01 | P0 | Open Journey from Quick Actions | Original overlay opens/closes, no change to project/idea/reminder/XP |
| B02 | P0 | Open Journey from Home progress strip | Same exact read-only view, Home controls unchanged |
| B03 | P0 | Workflow timeline load IO exception | Non-destructive unavailable warning; available saved project/idea evidence still visible |
| B04 | P0 | Workflow timeline JSON corrupt but owner currently swallows decoder error | Detect unresolved owner gap; **do not** certify full history integrity before separately approved typed error contract |
| B05 | P1 | Empty tasks/ideas/timeline fully loaded | Genuine empty copy, no invented logged work or streak |
| B06 | P1 | Upstream Creator VM tasks/ideas still loading | Don't misrepresent incomplete parent state as proven complete history |
| B07 | P0 | Personal toggle never pressed | Private source not collected and no Personal events or names displayed |
| B08 | P0 | Opt Personal ON, completion journal genuinely empty | Zero accepted Personal receipts, Creator history unaffected |
| B09 | P0 | Opt Personal ON and journal unreadable | Explicit private source unavailable; no false zero or source data deletion |
| B10 | P0 | Opt Personal ON then OFF | Private cached history no longer projected; no private row left in Creator history |
| B11 | P0 | Rotate/process recreate with Personal previously ON | Starts OFF after recreation, no private data shown without fresh opt-in |
| B12 | P0 | Personal accepted Done vs Snooze/Dismiss/posted reminder | Only genuine confirmed `PersonalCompletionJournal` accepted Done shown, not alert delivery or navigation |
| B13 | P1 | Multiple Personal Done receipts with same stable key synthetic data | No duplicate private event; original store never rewritten |
| B14 | P1 | History with real Creator project created/stage moved/completed/published events | Actual event kinds, source labels, original timestamps and stable IDs |
| B15 | P0 | Historical project source deleted/unavailable | Row remains safe history, no wrong Creator project opens |
| B16 | P0 | Existing real project event tapped | Opens exact original project, no attempt to rewrite milestone |
| B17 | P0 | Existing idea event tapped | Opens original Idea Vault, no new idea or XP |
| B18 | P0 | Personal Done history row tapped | Cannot open a Creator project or display original private reminder title |
| B19 | P1 | Week mode previous/next across month/year rollover | Moves 7 calendar days, updates visible month/selection correctly |
| B20 | P1 | Month mode previous/next across year, February and leap year | Moves exactly one month, focuses first day with correct calendar grid |
| B21 | P1 | Today action from earlier date/week/month | Returns selected day/month to true local Today, preserving viewer privacy |
| B22 | P1 | Week selected day in previous/next month | Full year/month correct and target day remains reachable |
| B23 | P1 | Journey recent 10 vs 101+ real events | Exactly most recent 100 shown, explicit truthful truncation notice, original older records preserved |
| B24 | P1 | Seven-day/selected-month events span two time zones or DST | Real date grouping, timestamp display, no invented scheduled date |
| B25 | P1 | Personal date of actual accepted Done crosses midnight | Uses accepted confirmation timestamp, no erroneous reminder scheduled time |
| B26 | P1 | Date/month state malformed after process restore | Safe UI-local fallback, no app crash or source write |
| B27 | P1 | Home preview timeline source unavailable | Existing Home remains usable; honest warning; opening Journey still works |
| B28 | P1 | Home preview source truly empty | Original compact empty/no-preview behavior or truthful explanatory state |
| B29 | P1 | Progress summary multiple events same day | Counts events separately and day once; Creator and private Personal distinct |
| B30 | P1 | No timeline event but task/idea source-backed actual event | Shows original available source; doesn't fabricate timeline activity |
| B31 | P0 | Creator XP and rewards after opening calendar/event | No new `CreatorRewardLedgerEntry`, XP, stage celebration or achievement |
| B32 | P0 | New private progress from UX07E and Space/Challenge | Challenge target dates, Space notes/photos and manual logs not silently imported into Creator timeline |
| B33 | P1 | 320dp, 200% font, landscape, tablet and TalkBack | Date labels/controls, 49dp cells, visible source tag, timestamp, event navigation reachable |
| B34 | P1 | Reduce motion, app backgrounds, Android 15 notification delivery | Journey does not animate or schedule alarms, no keyguard/private data leakage |
| B35 | P0 | Owner-installed integrated APK on accepted V337 profile | P0 Snooze/ledger/restore fixes preserved with exact APK hash and CI SHA |
| B36 | P1 | Original event source re-ordered/corrected | Read-only projection preserves unique event keys, chronology and no new persistence |
| B37 | P1 | Period with no recorded activity, timeline partially unavailable | Does not falsely certify fully empty history; unavailable data explained |
| B38 | P1 | Switch Week / Month / Journey repeatedly during loading | No stale wrong event, no crash, no deletion or duplicate collector |
| B39 | P1 | Big imported timeline 1k+ events / slow device | Bounded UI list, acceptable scrolling/no ANR, truthful old history count |
| B40 | P0 | P0 V337 common-ancestor reconciliation and full campaign | 22 accepted P0 paths accounted for; all relevant original regressions PASS |

## New source-test file, not executed

`app/src/test/java/com/framebynavin/app/ui/UX08BProgressPolicyTest.kt` stages **five** pure policy tests:
- accurate recorded event count, unique recorded days and private/Creator separation
- month filtering by actual display date
- exact 7-day Week vs month-start Month navigation
- exact available source navigation gating and private Done nonclickable
- meaningful event labels.

Separate original `ProgressJourneyEngine`, `CreatorWorkflowTimelineStore`, `CreatorRewardStore`, Creator VM integration and Personal privacy tests are still required. Additional instrumented tests should validate actual Home→Journey route and process recreation of opt-in.

## Known source limitations and no-go

- The original `CreatorWorkflowTimelineStore.load` **swallows decode errors into `emptyList()`**. UX08B catches propagated load exceptions but cannot conclusively detect pre-swallowed corruption; do not claim recovered timeline integrity or automatically clear data.
- UI itself does not authorize server sync, new Personal journal backup, persistent XP, challenge receipt or owner data reset.
- `includePersonal` is ephemeral/private by default and source projection only. It is not a stored opt-in setting or always-on analytics feed.
- 100-event Journey cap remains, now disclosed; older source records aren't deleted.
- A stored-source read failure, import mismatch or accepted V337 P0 regression is **NO GO** for final stable promotion until separately resolved.
- Future UX08C celebration must be driven by genuinely newly credited Creator rewards; never infer award from Journey visits, historical events or imported backups.

**Current executed count: 0 / 40.** UX08B can be SOURCE-STAGED upon exact isolated draft diff review; source alone is not proven compilable or release-ready.

**Next:** UX08C owner-event reward and selected-companion ~3-second celebration design, with separate Personal milestones decision gate, then UX08D Daily Brief, UX08E Content Calendar, UX08F Quick Actions integration. Consolidated tests remain post-UX10.
