# Backlot UX12 D4 — Practical Personal Reminder Home

**Date:** 2026-11-22  
**Status:** SOURCE STAGED; NOT COMPILED; NOT TESTED; NOT MERGED; NOT RELEASED.  
**Founder workflow:** stage every approved UX12 change before the combined build and Android QA. Do not run per-stage tests, lint, APK builds or device exercises until founder explicitly says **“Done bro, start next steps and give APK.”**

## Continuity and isolation

- Repository: `rayadinaveen98-ship-it/framebynavin-android`.
- Stable `main` at D4 branch creation: `abbe88984bac16cfafbd990d9cb00ed54cfdd07e` (untouched).
- Prior D3 source baseline: `b643b15b7fd263c126b30561f73bea15ca765999` (unchanged).
- D4 staged branch: `feature/backlot-ux12-d4-personal-home-20261122`, stacked on D3.
- No separate PR was opened for D4, to avoid triggering PR-dependent jobs before the founder’s final test command.
- This is a presentation-only redesign for the unlocked in-app Personal workspace. No Android alarm, scheduling, Voice/Alarm playback, automatic repeat, Snooze engine, Supabase/cloud sync, Creator project, Room database, journal/media or account/auth changes.

## Verified initial problem

The prior `UX11EPersonalSanctuaryHero` stacked a large greeting and explanatory paragraph, an additional date, two bordered statistic cards, a large nested `Up Next` card, vertical Done and Snooze controls, an attention sentence and two large bottom actions. Its Personal Hub caller then displayed Rhythm, a verbose Spaces introduction, a separate Progress introduction, a verbose Reminders introduction, and a full agenda. This duplicated headings, produced nested borders and pushed the actionable reminder list down the screen.

## D4 source changes

### 1. Compact Personal home projection

File: `app/src/main/java/com/framebynavin/app/ui/UX11EPersonalSanctuaryHome.kt`

- Keeps `UX11EPersonalSanctuaryHero` call signature and all existing event callbacks.
- Replaces the nested stat and summary-card stacks with one restrained, static warm gradient surface.
- Condenses the greeting, date and two factual progress figures into one small header region. The labels are still sourced from the original read-only `V288TodaySnapshot`.
- Uses **Progress unavailable** when receipt history could not be read, never a fabricated zero.
- Displays the existing `Up next / Needs attention` reminder title and projected time without a new bordered subcard.
- **Done** and **Snooze** appear only when `V288PersonalTodayPolicy.canRespond(next)` permits them, using the exact original `PersonalDeliveryContract.ACTION_DONE` and `ACTION_SNOOZE` codes passed through the existing `onRespond` callback. The delivery coordinator and eligibility remain untouched.
- On narrow Android screens or enlarged font scales, eligible action buttons stack vertically to avoid truncation. The standard layout uses a compact two-button row.
- The **New reminder** CTA still invokes the original editor callback; **My Spaces** still invokes the original Spaces route. Same `busy` state disables actions.
- For an empty next reminder, shows a small approved Spark/Mira `THINK` pose without introducing a new animation engine or altering character assets. No intrusive large mascot or duplicate header portrait. Existing top-level companion/avatar interaction is unchanged.
- Existing accessibility/test tags preserved: `ux11e-personal-sanctuary-hero`, `v288-today-home`, `v288-next-action`, `v288-next-title`, `v288-next-done`, `v288-next-snooze`, `v288-today-confirmed-done`, `ux11e-create-personal-reminder`, `v289-open-spaces`, `ux11e-attention-summary`.
- Canvas is static (not always animating), with zero-size measure guard.
- `UX11ESanctuarySection` retains its existing public-to-module signature but omits the secondary sentence when its detail is intentionally blank.

### 2. Eliminate redundant headings in the Personal Hub

File: `app/src/main/java/com/framebynavin/app/ui/V2232PersonalReminderHub.kt`

- Removes the standalone `Your corners` heading because `V340HomeSpacesEntry` already renders **My Spaces**.
- Removes the standalone `Looking back` heading because `UX07EPersonalProgressOverview` already renders its own seven-day progress heading.
- Replaces verbose `Your reminders` description with a short **Reminders** heading above the original agenda filters.
- Simplifies the separate empty reminder-list explanation to **No saved reminders yet.** The greeting/CTA in the hero remains actionable.
- Keeps existing Rhythm, Spaces, Progress, 21-day Challenge, complete Personal agenda, scope navigation, create/edit/delete, group management, recovery, permissions and follow-up settings; all data and callback implementations remain unchanged.

### 3. Shorter Routine and Progress copy

File: `app/src/main/java/com/framebynavin/app/ui/UX07BPersonalRhythmUi.kt`

- Uses **ROUTINES** with optional factual recurring-count subtitle, no redundant motivational line when there are no recurring reminders.
- Keeps actual recurring reminder items, New, Challenges, and Reminder Groups functionality.

File: `app/src/main/java/com/framebynavin/app/ui/UX07EPersonalProgressUi.kt`

- Shortens the seven-day subtitle and explanatory statement. Preserves important source semantics: Personal Done uses confirmation time; Challenge totals use chosen dates including explicit catch-up; notes do not mark a day Done.
- Does not alter progress computations or stored confirmations.

## Boundaries and next approvals

- Do not redesign complete agenda rows, Spaces Room data or 21-day completion rules under D4.
- Do not make Done/Snooze available merely because a reminder is overdue; only a real eligible delivered occurrence may accept a response.
- Never display an invented achievement or mark an overdue item as delivered.
- Do not replace Spark/Mira with generated assets or reuse face-only crop on a full-body pose.
- D4 is source-staged only: code paths, layout under IME, large font and actual Android behavior have **not** been compiled/tested.

## Deferred cumulative acceptance checklist

1. New Personal profile with zero reminders: small welcome, companion cameo, Create, Spaces, full agenda empty state, no premature Done/Snooze.
2. Existing scheduled reminder not yet presented: correct title/time; Done/Snooze absent.
3. Delivered eligible occurrence: correct Done/Snooze callbacks, repeat/Snooze semantics unchanged, disabled while busy.
4. Overdue without eligible response: wording never implies Android delivery or user failure; review via original agenda.
5. History unavailable: progress displays **unavailable**, not zero; read errors remain visible in the Hub.
6. Several active/paused/archived items: original agenda scope counts, edit/pause/delete and bulk selection intact.
7. My Spaces and linked journal: original navigation and read-only home previews; no new database creation or image-byte leak.
8. Routines, 21-day Challenge, groups and seven-day progress: original callbacks, projection and distinct completion domains.
9. Accessibility and screen fitting: 320–360dp phone, 1.5x and 2.0x font, Redmi Pad 2 tablet, landscape, TalkBack, touch-target sizing, contrast, no clipping.
10. Exact test tags and metrics remain findable by existing instrumentation. Adjust obsolete text expectations in the combined final QA if necessary; do not silently delete meaningful tests.
11. No dependency or effects on Android reminder delivery; outstanding Personal Reminder P0 reliability remains a separate release gate.

**Evidence level:** implementation source staged, without any CI, device, APK or release certification. Continue to D5 in a new stacked branch once the founder says continue.
