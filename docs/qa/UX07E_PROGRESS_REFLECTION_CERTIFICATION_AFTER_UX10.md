# Backlot UX07E — Personal progress and 21-day reflection certification AFTER UX10

**Date:** 2026-11-17.
**State:** DEFERRED ACCEPTANCE TEST SPEC, **0 tests executed**. No Gradle compilation, JVM tests, CI run, emulator/instrumentation, user-device QA, APK or release.
**Exact source parent:** UX07D draft PR #414 at `3246059706a2cc433278249fad10700142bcaa3a`.
**Owner-stable accepted P0:** V337 `815ba69775f75b6e5f25bb838c2c17b4f09c203e` remains untouched, with full accepted P0 lineage still an open final integration blocker.
**Execution agreement:** one consolidated UX01–UX10 campaign after source staging, including prior UX06F F01–F30, UX07A P01–P38, UX07B B01–B30, UX07C C01–C32 and UX07D D01–D36.

| ID | Priority | Action / condition | Expected |
|---|---|---|---|
| E01 | P0 | Personal completion journal source fails to read | Em dash/unavailable for Personal counts, never successful zero or data reset |
| E02 | P0 | 21-day Challenge store fails, Personal journal remains valid | Personal counts still real; Challenge counts unavailable, not invented |
| E03 | P1 | Both owners load truly empty | All four counts show truthful zero, never an invented streak |
| E04 | P0 | Accepted Personal DONE at 23:59 / 00:00 display zone | Date assigned by actual accepted `doneAtMillis`, not notification time |
| E05 | P0 | Personal Snooze/Dismiss/posted notification without actual DONE | Does not contribute to Done count or activity-day tally |
| E06 | P0 | Multiple accepted Personal Done receipts on same local day | Count occurrences individually; calendar active day once |
| E07 | P0 | Duplicate Personal completion stable key in synthetic projection | No duplicate count; original source codec rejects bad raw journals |
| E08 | P1 | Completed Personal occurrence older than seven-day window | Excluded, even if the reminder remains active today |
| E09 | P1 | Personal Done receipt timestamp in the future | Excluded until its timestamp is eligible; no future success |
| E10 | P0 | Challenge flexible catch-up confirms prior target day today | Count selected target day in the seven-day Challenge date window; receipt still has real current confirmation instant |
| E11 | P0 | Challenge STRICT mode only Today allowed | UI doesn't reveal new backfill permission or mutate source rule |
| E12 | P0 | Challenge notes saved without confirmations | Note count rises; confirmed-day count stays unchanged |
| E13 | P0 | Challenge confirmation without any day note | Confirmed-day count rises independently; note count unchanged |
| E14 | P0 | Original Challenge Undo Day, then inspect progress | Confirmation count decreases; note stays saved unless owner explicitly changes it |
| E15 | P0 | Challenge archived then opened in Personal | Historical real notes/receipts visible read-only, no action unlock |
| E16 | P1 | Challenge paused or alert OFF | Original confirmation history remains; not treated as scheduled/delivered alert |
| E17 | P1 | Challenge not started yet | Future target dates not counted as completed; original calendar future status preserved |
| E18 | P1 | Challenge ended last month | Only target dates within window count; no new next-alarm promised |
| E19 | P1 | Two 21-day Challenges with overlapping dates | Both distinct owner-confirmed daily receipts counted, no merged record or fake one-challenge global streak |
| E20 | P1 | Challenge zone differs from device zone | Target date uses each challenge's saved zone, Personal Done uses screen display zone |
| E21 | P1 | Time zone and DST transition | Seven calendar day boundaries correct; no elapsed 7×24h assumption |
| E22 | P0 | Challenge Day note listed with no receipt | Shows Note saved / no Done; tap opens exact original selected day |
| E23 | P0 | Challenge day has note AND receipt | Shows separate confirmation and opening day shows actual recorded timestamp |
| E24 | P1 | 3 vs 4+ day notes | Preview maximum three by target date; all older notes remain accessible via 21-day calendar |
| E25 | P1 | Saved notes with multilingual text / long notes | Ellipsis/scroll, original note bytes unchanged |
| E26 | P0 | Tap a reflection in archived Challenge | Original archive read-only dialog, no write or hidden catch-up |
| E27 | P0 | Save note via original day dialog, then revisit progress | Notes persist in Phase1HabitStore; receipt history remains untouched |
| E28 | P0 | Confirm/Undo from existing main challenge dashboard | Original owner calls, scheduling and AlarmManager reconciliation unchanged |
| E29 | P0 | Existing Challenge linked Space or Group | Linking is still ID-only; neither source-owned Done/notes nor Space manual logs are duplicated |
| E30 | P1 | My Day open, then tap progress Challenges | Exact existing manager; doesn't silently create/start a challenge |
| E31 | P1 | Personal/Creator tab switch; Progress Journey include-Personal off | No new Personal journal data in Creator Analytics/Progress by default |
| E32 | P0 | App locked; notification and full-screen alerts | No challenge notes, Space photos or private titles leaked by new progress UI |
| E33 | P1 | 320dp / 200% font / landscape / tablet | Four progress values readable; reflection rows, calendar, original buttons reachable |
| E34 | P1 | TalkBack/switch access/reduced motion | Four domains spoken distinctly, note-vs-confirmed clear, reflection tap target navigates to right day |
| E35 | P1 | Date/time changes while My Day stays open | Preview recalculates with original agenda clock and doesn’t retain a stale wrong seven-day window |
| E36 | P0 | V337 P0-approved baseline reconciled into integrated UX01–10 candidate | No lost ledger/restore protections or P0 Snooze millisecond comparison fix |
| E37 | P0 | Existing 21-day v1/v2 upgrade to v3, backup/rollback | Voice/music legacy OFF defaults and original exact Done receipts retained; new view doesn't write format |
| E38 | P1 | Internal personal UI screenshot / logging and app background | No private photo/Day note recorded in debug telemetry or user-unapproved upload |
| E39 | P1 | Very long Personal completion history (up to 5,000) and 21×N Challenge dates | Read-only projection remains responsive, no excess allocations or extra backend calls |
| E40 | P1 | Opening original Challenge from old Activity/agenda deep link | Original exact ID focus and all old management controls preserved |

## Deferred test-source evidence

`app/src/test/java/com/framebynavin/app/ui/UX07EPersonalProgressPolicyTest.kt` stages **six** source tests for unavailable-vs-zero counts, accepted Personal done-time grouping, Challenge target-day flexible catch-up, note-vs-confirmed truth, and bounded note previews. These files are **not executed** until post-UX10. Device UI, restoration and true Android delivery/response tests are additionally required.

## Stop-the-line failures

- Notes, photos, opening a Space, sharing a Group or receiving an alert counted as confirmed activity.
- A failed data read shown as zero or trigger for reset/overwrite.
- Flexible catch-up backdates the original confirmation timestamp or STRICT mode gains backfill.
- UX06F corrected Challenge Snooze target mismatch regresses, or accepted V337 P0 ledger/restore changes are lost.
- Challenge notes or private media surface on keyguard, public telemetry, Creator default analytics or unwanted cloud sync.
- Owner day/receipt action or rollback flow replaced by the UI-only view; unsafe merge, data deletion or release without owner signoff.

**Current evidence:** 0/40 UX07E cases run; only repository source commits and exact diff inspection are recorded. Source-coded unit tests are **pending**.

**UX07F handoff:** cumulative static integration audit, P0 branch divergence, all original button/action preservation, and UX08 Quick Actions implementation brief. Do not merge or certify before post-UX10.
