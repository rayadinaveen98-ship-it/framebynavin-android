# Backlot UX10A — Shared typography, density and quiet surface certification

**Date:** 2026-11-17
**Stage:** SOURCE-STAGED ONLY / **ALL TESTS DEFERRED (0 EXECUTED)**.
**Exact UX08F parent:** PR #422 `47e785f8a598862154813bfe2ba409e7a5de73ed`.
**Owner-accepted V337 P0:** `815ba69775f75b6e5f25bb838c2c17b4f09c203e`; unchanged, 19 accepted stable correction commits / 22 paths still require reconciliation.
**UX09:** Explicitly deferred by founder. A final all-feature cross-device release cannot claim it has been delivered.
**Do not claim:** Gradle/Kotlin/Compose compilation, test run, Android screenshot, real-device accessibility, release APK or stable promotion.

| ID | Severity | Physical/source scenario | Pass requirement |
|---|---|---|---|
| A01 | P0 | Start Backlot with saved Director's Cut setting | Same palette/logo/brand colors and preference ID, no reset |
| A02 | P0 | Switch to Backlot Light and relaunch | Correct approved light palette and bars, no loss of preferences |
| A03 | P0 | Install over old retired theme preference value | Original normalization succeeds without theme crash |
| A04 | P1 | Compare Material bodySmall/labelMedium/labelSmall against local text | Consistent 10–13sp readable minimum; no unstyled third-party default |
| A05 | P1 | Open Quick Actions at 360dp/normal font | Same 12 shortcuts and overdue actions, no hiding of New Project/Quick Capture |
| A06 | P1 | Open Quick Actions at 320dp | Both primary actions stack, no horizontal clipping |
| A07 | P1 | Quick Actions at 200% font scale | Text wraps, 56dp minimum target respected, scroll remains reachable |
| A08 | P1 | Quick Actions / bottom-sheet Android back | Same original dismissal; no duplicate modal |
| A09 | P0 | Overdue +30 min/New time/Skip from Quick Actions | UX08F original callback and Skip confirmation semantics preserved |
| A10 | P1 | Creator Quick Actions neutral shortcut surfaces | Hairlines less visually dominant, still discoverable in dark/light |
| A11 | P1 | Creator Progress card, normal font scale | All 31 formerly tiny labels legible at minimum 10sp |
| A12 | P1 | Creator Progress at 200% font | No overlap of level, XP, achievement counts or momentum |
| A13 | P0 | Creator XP ledger + achievement data | No credit, source key, point, award or stored reward changed |
| A14 | P1 | Creator Progress achievement card list | Real labels, counts and unlock status remain readable after text increase |
| A15 | P1 | Creator Progress Light theme surfaces | Quiet outlines remain distinguishable, selected/XP accent intact |
| A16 | P1 | Daily Brief headings, support copy and project cards | Common hierarchy, tappable rows >=48dp, original actions |
| A17 | P0 | Daily Brief real focus/overdue/today source groups | No changed task order, duplicate project route or invented completion |
| A18 | P1 | Daily Brief metric cards at 200% font | 10sp+ labels can wrap, no collision or unreadable cutoff |
| A19 | P1 | Content Calendar Month grid normal font | Seven columns, source dots, actual selected day visible |
| A20 | P1 | Content Calendar Month grid 200%/320dp | Date targets 56dp min height, no horizontal scrollbar trap or clipped day count |
| A21 | P1 | Content Calendar Agenda chip list | >=48dp chip height, original 14-day anchoring, no truncated tap targets |
| A22 | P0 | Week/Month/Agenda source navigation | Same future bound and stable recurrence identities, no task materialization |
| A23 | P1 | Content Calendar dark/light selected date | Strong selected border remains while neutral cards use quiet outlines |
| A24 | P1 | Content Calendar source date counts and paging | Accurate 80-item page buttons, counts and original destinations |
| A25 | P1 | Progress Journey day events and labels | Common 18dp gutter/23sp page title, actual event chronology |
| A26 | P0 | Progress Journey Personal OFF | Private accepted Done history absent without fresh opt-in |
| A27 | P1 | Progress Journey active selected-day state | Retains visible 1dp semantic selection border |
| A28 | P1 | Progress Journey historical event cards | Less heavy neutral outlines, still tappable only when original source exists |
| A29 | P0 | Real Creator reward with companion celebration | No animation source/XP/card owner modified by UX10A |
| A30 | P0 | Personal/21-day reminders and Android 15 full-screen response | Original controls, alarm tokens, voice/song and Snooze unaffected |
| A31 | P1 | Light theme contrast at low brightness/daylight | Muted support text and low-alpha borders remain visually distinguishable |
| A32 | P1 | Android TalkBack traverses Quick Actions and source cards | Semantic buttons, descriptions and logical focus order unchanged |
| A33 | P1 | Android font 1.0 / 1.5 / 2.0 and display size adjustments | No fixed-size text clipping, unsafe ellipsis or invisible controls |
| A34 | P1 | System high contrast/reduced motion | Actual selected/focus state remains discoverable, no forced animation |
| A35 | P1 | Tablet portrait/landscape, foldable/reflow where possible | Gutters, typography and wide-card layout preserve hierarchy |
| A36 | P1 | Saved 5k projects, many Calendar entries | No additional computational query or ANR introduced by tokens |
| A37 | P0 | Original Room/DataStore/backup data and restore | No new schema, missing record, preference migration or writes |
| A38 | P0 | Cloud/Auth/Supabase configuration diff | Zero UX10A edits; UX09 remains deferred and not misrepresented |
| A39 | P0 | Cumulative stacked integration + V337 accepted P0 lineage | Reviewed 19/22 correction inventory, no lost Android restore/snooze/ledger fixes |
| A40 | P0 | Integrated signed APK on owner's physical Android after authorized final build | SHA/hash, full UX01–08+10 and P0 results, owner signoff before release |

## Authored source policy test

`app/src/test/java/com/framebynavin/app/ui/theme/BacklotUX10TokenContractTest.kt` declares **5 source tests, 0 executed**, covering 48dp targets, 10sp support/eyebrow minimum, consistent density, quiet vs active border widths and original two selectable appearances.

## Scope and coverage truth

This is UX10A only. Design consistency work on first-run/onboarding/Home/navigation, project composer/Content Workspace, Personal/Challenge/reminder, settings/insights, notification/widgets and all future UX09 sync screens is scheduled for UX10B–F and not claimed complete.

No UI screenshot captured, no comparison of dark/light in physical app, no actual accessibility pass. The 10sp floor is an intention, not an accessibility certification; if high font scale clips action/metric cards, change adaptive layout rather than shrink back to unreadable text.

**Current executed:** 0/40 user acceptance scenarios; 0/5 source tests. **NO GO** for stable release until tests, accepted V337 P0 corrections, explicit owner approval and release checks actually pass.
