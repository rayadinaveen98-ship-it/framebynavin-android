# UX06B — Reminder Center / Composer Certification Gate (post-UX10 only)

**Status:** DEFERRED, NOT RUN; source-staging acceptance checklist only.
**Protected stable:** `stable/backlot-v337-premiere-owner-testing-20261114` unchanged.
**Parent:** UX06A draft PR #404 at e0ba5bf6eb5d2e2f5fa5ce31049efd9d4f597a9f.
**Scope:** Shared presentation-only V406 controls; current Creator and Personal/Challenge hub navigation; Personal composer step progress; reuse of existing Creator Quick/Full wizard and Personal Editor.

## Invariants

- No new reminder record is created when a navigation, composer progress, Add menu, chip or scope tile is tapped. Actual writes occur only in existing validated owner save callbacks.
- The Creator attention rail uses ReminderCenterPolicy groups. Personal attention uses V337UnifiedPersonalAgenda and the source-namespace keys. Challenges with Alert Off are not shown as scheduled.
- No filter, Activity deep link or screen switch may silently count Done/Working/Answered or change a scheduled reminder's time.
- Category selection is presentation-only; original list filters, Activity Center, My Spaces, Challenge, Idea Vault and full editor functionality remain available.
- A change in search, scope or attention-only filter clears stale/hidden bulk selection before any delete confirmation.
- Search result counts reflect the search-matching Creator groups. A notification deep link remains an exact ID until the owner explicitly selects another view; then the focus is cleared.
- Opening a Personal or Challenge deep link cancels any formerly sticky attention-only filter.
- Previous Personal composer steps are reachable without wiping typed values. Future steps are not clickable until validation allows progress. Editing an existing reminder remains on its existing all-fields route.
- No additional Android permissions, cloud sync, TTS/music behavior, database changes, migrations, backup changes, billing or hosted API modifications.

## Test matrix

| Test | Scenario | Required result |
|---|---|---|
| B01 | Creator empty first launch; existing nonempty tasks | Empty state remains actionable; no misleading zero cards; safe New Project |
| B02 | Creator Add Quick / Full | Routes to original V20 quick and full workflows; no duplicate project; Cancel returns safely |
| B03 | Creator search with multi-word result, count tiles | Scope counts match filtered groups; search text retained on deliberate scope choice |
| B04 | Creator Needs action / Next 24h / Later / History | Correct existing V219 filter or saved response-history section; no new state written |
| B05 | Creator activity notification deep link + rail click | Exact project focus until owner navigates; full view restored after selection |
| B06 | Creator long-press selection, scope change, delete confirmation | Hidden selections cleared; no accidental cancellation of nonvisible reminders |
| B07 | Creator menu Pause, Turn Off, Edit, Snooze, Change Time | Existing actions, policy bounds and reminder records unchanged |
| B08 | Personal empty state; active Personal only; Challenge only | No synthetic schedule; correct entry routes and challenge visibility |
| B09 | Personal scope Today / Seven Days / All vs new scope shortcuts | Counts match existing V337 agenda; attention preserved under Today/Week |
| B10 | Personal Needs action | Only existing Needs Attention section; no false missed/delivered/Done inference |
| B11 | Personal mixed Personal + Challenge with same raw ID | Namespace separation preserved, no duplicate false collision |
| B12 | Personal Alert Off challenge and archived/paused/finished challenge | Correct non-scheduled/All/Paused classification, never invented alert |
| B13 | Personal selected reminder + attention filter switch | Hidden bulk-delete tokens cleared; cannot delete unshown records |
| B14 | Personal exact reminder and challenge deep links following attention filter | Target remains visible via existing original owner route |
| B15 | Personal Add → New Reminder / 21-Day Challenges | Existing Personal editor/challenge manager opens, no false quick creation |
| B16 | Personal four-step composer on new reminder | Earlier steps can be revisited; no future-step skip; input survives revisits |
| B17 | Personal editing an existing reminder | Full-field route, existing linked Space/Group, schedule values unchanged |
| B18 | Personal invalid title, schedule, recurrence, interval, quiet alert | Existing validation blocks invalid save and displays helpful error |
| B19 | Personal fully valid save, permission-denied fallback, link write failure | Exactly one owner-created record; correct "saved but attention needed" distinction |
| B20 | Creator/Personal view transitions while OS time crosses midnight | Labels/counts update only from actual store/time; no miscount across timezone |
| B21 | 320dp compact Android, 200% font, landscape/tablet, IME | Scrollable tiles and composer progress; no clipped vital buttons or hidden exit |
| B22 | TalkBack, focus order, announcement of selected scope, gesture targets | Scope announced as selected/unselected; progress only earlier steps navigable; >=48dp target |
| B23 | Screen rotation/process recreation while editor open | Current wizard fields, progress step, filters and editor intent preserved appropriately |
| B24 | V337 restore/upgrade and source-staged backup round trip | No modification to canonical records, completion or long-term reminder settings |
| B25 | Multiple simultaneous alarms while Reminder Center is open | Navigation must not cancel, acknowledge, duplicate or reschedule them |

## Evidence required for final certification

1. Exact cumulative candidate SHA/CI run, compiled Kotlin and no Android manifest or data-store regression.
2. Screenshot/video at compact phone, normal phone, tablet, light/dark, font-scale and TalkBack.
3. Before/after checksums of original Creator, Personal, Challenge and Idea fixture records.
4. Reproducible automated tests and a real-device acceptance report for B01–B25 and existing UX06A R01–R35.
5. Owner confirmation that the navigation density, Create menus and progress affordances are intuitive.
6. Explicit defect/fix tracking; a document, code diff or test tag is not proof of a passing test.

**Important limitations:** B01–B25 are not executed in this stage, and no cross-domain aggregated All Reminders timeline or standalone Quick Personal create was implemented. Those remain product options requiring a separately reviewed adapter and safe write contract.
