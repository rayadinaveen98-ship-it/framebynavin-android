# Backlot UX07B — Personal My Day certification matrix, AFTER UX10

**State:** DEFERRED TEST PLAN, **NOT RUN**. No Gradle build, CI, UI instrumentation, device capture, APK, or release claim.
**Source parent:** UX07A PR #410, head `d9ba477b279a94b33dd4d367cd27ce3271af4511`.
**Protect:** V337 P0 source `815ba69775f75b6e5f25bb838c2c17b4f09c203e`, unchanged; UX06F corrected Snooze occurrence validation, not yet merged with every accepted P0 fix.
**Founder sequencing:** one cross-stage UX01–UX10 campaign at the end, including UX06F F01–F30 and UX07A P01–P38.

## 1. UX07B deferred matrix

| ID | Priority | Condition / action | Expected |
|---|---|---|---|
| B01 | P0 | Upgrade saved Personal reminders with ONCE, daily, weekly, custom and hourly repeat | No saved record changes, reschedule, duplicate alarms or new store writes |
| B02 | P1 | My Day, two recurring active reminders due at different times | Exactly source-backed upcoming sorted rows, no fabricated routine |
| B03 | P1 | My Day, 3+ recurring due reminders | At most two preview rows; full agenda still shows all original records |
| B04 | P1 | One-off plus recurring, paused plus active | Preview excludes ONCE and paused; all still visible in original appropriate agenda scope |
| B05 | P1 | Only ONCE reminders / no recurring | Quiet, correct empty rhythm state; no imaginary daily routine |
| B06 | P1 | Pending occurrence now overdue in Personal owner | Original due time remains truthful, no false Done/notification-seen claim |
| B07 | P0 | Tap preview saved rhythm row | Original Personal editor opens exact record; cancelling is nonmutating |
| B08 | P0 | Tap NEW from rhythm | Original four-step Personal editor opens, retains validation and source-owned save |
| B09 | P0 | Tap 21-day challenges shortcut | Existing Challenge manager opens, exact receipts, voice v3 and Snooze behaviour unchanged |
| B10 | P0 | Tap Reminder Groups shortcut | Existing group list/edit/link/export/import and confirmed Pause All/Resume All remain |
| B11 | P0 | No Spaces database / first-run profile | My Day safe preview displays empty state without creating database |
| B12 | P0 | Saved Spaces and archived spaces | Existing read-only preview shows real counts only, no photo bytes loaded or sync implied |
| B13 | P0 | Tap exact featured Space on My Day | Opens original Space detail with same ID, never unrelated fallback |
| B14 | P0 | Tap My Spaces overview on preview after exact Home deep link | Opens overview, not stale previously selected Space |
| B15 | P1 | Leave preview-selected Space, edit linked reminder and reopen Spaces | Stale preview focus cleared; existing Space editing and reminder linking preserved |
| B16 | P0 | Existing V288 Done and Snooze controls, all consent states | Exact original Personal coordinator, tokens and result gates; no duplicate completion |
| B17 | P0 | 21-day challenge alert enabled but no Personal reminders | Agenda retains Challenge row and manual Done semantics; V288 copy makes no cross-domain claim |
| B18 | P1 | Challenge store missing, loading or failing | Shortcut remains available, count omitted when unknown; original error shown without false zero |
| B19 | P1 | Personal load error or corrupt record | New rhythm/Space preview not presented as successful empty account; data never reset |
| B20 | P1 | Original Today/Needs Action/Next 7 Days/All scope toggles, Activity focus | Exact original selection reset; bulk deletion cannot include hidden unrelated items |
| B21 | P1 | Settings/My Routines and Home Space entry | Original routes, controls and test tags remain; repeat/notification meaning unchanged |
| B22 | P1 | 320dp width, 200% font, phone landscape, tablet | Rhythm labels and button targets readable/scrollable; next action and deep controls remain reachable |
| B23 | P1 | TalkBack, keyboard, switch access, reduced motion | Action rows and shortcuts labeled; decorative art not focusable; no animations gate response |
| B24 | P1 | Both Home and Personal My Day composed at once | Shared `V340HomeSpacesEntry` tags don't yield ambiguous visible UI semantics or break older Home tests |
| B25 | P1 | Recompose/process death while Spaces preview active | Focus and back navigation are safe, no outdated ID misrouting or duplicate DB write |
| B26 | P0 | Upgrade to integrated UX10 candidate with owner data and V337 P0 fixes | All 22 baseline P0 changed paths reconciled; no lost Snooze or restore recovery |
| B27 | P1 | Refresh device time/date and change timezone | Upcoming rhythm due labels reflect the canonical recurrence/zone, no rescheduling |
| B28 | P1 | Denied notifications / FSI/exact alarm / background restrictions | Existing truthful permission warning and user controls remain; no delivery guarantee |
| B29 | P1 | Private photos in Spaces | My Day does not retrieve, serialize, leak or present photo content in a generic card |
| B30 | P1 | Cancel editor / exit My Day / reopen | No reminder, challenge, Space or group records mutated without an explicit action |

## 2. Unit test source staged, not executed

`app/src/test/java/com/framebynavin/app/ui/UX07BRhythmPolicyTest.kt` contains three deferred cases: exclude paused/once, deterministic first two recurring rows, and an actual pending due instant without any Done mutation. Execute with the complete JVM suite **after UX10**, not as an interim release-certification substitute. Add Android instrumentation/screenshot cases for exact-ID Space focus and Home coexistence at the integrated gate.

## 3. Original control retention

Before claiming the integrated candidate ready, check old tags/hooks:
- `v288-personal-toolbar-back`, `v306-personal-open-activity`, `v288-personal-toggle-settings`, `v2232-personal-add`, `v2232-personal-space-switcher`, `v289-open-spaces`.
- `v288-next-done`, `v288-next-snooze`, `v2232-personal-count`, `v285-agenda-scope-selector`, `ux06b-personal-focus-rail`.
- `v338-personal-select-toggle`, `v338-delete-selected`, `v338-bulk-delete-confirm`, `v2232-personal-empty-add`, `v337-challenge-load-error`.
- `v295-routines-new`, `v328-routines-open-challenges`, `v298-routines-open-groups`, `v295-routines-open-spaces`, `v340-home-spaces-entry`.

New UX07B tags:`ux07b-personal-rhythm`, `ux07b-new-routine`, `ux07b-rhythm-reminder-{index}`, `ux07b-rhythm-empty`, `ux07b-open-challenges`, `ux07b-open-groups`, `ux07b-myday-spaces-preview`.

## 4. Stop-the-line gates

Do NOT promote if: a Personal reminder is overwritten/duplicated, a confirmation is invented, any original Done/Snooze/Delete/Group Pause/Resume path disappears, a Space ID opens a different Space, private photo bytes leak, existing Space data is reset, UX06F P0 Snooze fix is missing, challenge v3 restore loses receipts, or Android lockscreen personal privacy regresses.

Until actual integrated CI/unit/instrumented and physical tests run, all B01–B30 remain **UNVERIFIED**, even if the static PR diff is accurate. **Executed: 0 / 30**.

**Next:** UX07C source-stage polish of the existing My Spaces screens while maintaining this UI-only UX07B parent and retaining the complete storage/privacy/action contract.
