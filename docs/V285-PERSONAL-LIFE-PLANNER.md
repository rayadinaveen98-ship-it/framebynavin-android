# Backlot V285 — Personal Life Planner (21-day cycle)

**Draft / owner testing only.** Dates are local to the user's Android device.
Planned cycle: **November 5–25, 2026**, inclusive (21 consecutive days).
Baseline: V284 Diet & Fitness, a separate personal reminder namespace and owner preview signing.

## Purpose

A calmer Personal Reminders workspace that helps the owner honor health, a
creative career, reading, rest, skin care and learning. **This plan does not
publish to YouTube, create Creator projects, or silently rewrite diet reminders.**

## Daily rhythm (suggested, adjustable)

| Time | Block | Alert design |
|---|---|---|
| 07:00 | Wake + hydrate; weigh on scheduled days | Existing V284 |
| 07:15 | Strength or mobility (weekday-specific) | Existing V284 |
| 08:00 | Shower / morning skincare | Optional gentle notification |
| 08:15 | Breakfast (weekday-specific menu) | Existing V284 |
| 08:50 | Today's specific YouTube production goal | Optional one-time notification |
| 09:00–11:00 | Scriptwriting and film research | Optional gentle notification |
| 11:00 | Fruit | Existing V284 |
| 11:15–13:15 | Voiceover or video editing | Optional gentle notification |
| 13:30 | Lunch | Existing V284 |
| 14:00–15:00 | Nap / recovery | Optional gentle notification |
| 15:15–16:25 | Sound, visual refinements, thumbnails | Optional gentle notification |
| 16:30 | Snack | Existing V284 |
| 16:45 | Sunscreen if outdoors in daylight | Optional gentle notification Mon–Sat |
| 17:00 | Evening walk (Mon–Sat, duration varies) | Existing V284 |
| 17:45–19:15 | Game dev Tue/Thu/Sat or flexible overflow | Optional gentle notification |
| 19:30 | Dinner (weekday-specific menu) | Existing V284 |
| 20:00–21:15 | Light creative work + next-day prep | Optional gentle notification Mon–Sat |
| 21:30 | Night skincare: cleanser, niacinamide, moisturizer | Optional gentle notification |
| 22:00–22:40 | Novel reading | Optional gentle notification |
| 22:40–23:00 | Wind down, go to sleep | **User must edit** V284's 22:15 reminder if uninterrupted reading is desired |

**Sleep reminder conflict:** The existing V284 22:15 *Voice* reminder can
interrupt 22:00–22:40 reading. The planner warns and **never overwrites the
owner's saved record**. Recommended manual change: 22:40 wind-down.

**Nap:** Suggested one hour. A two-hour nap is a flexible option, not a
mandatory repeat; consider a shorter or roughly 90-minute nap if groggy or if
night sleep suffers.

**Niacinamide:** Exact serum strength not confirmed. Avoid assuming 5% vs 10%.
Start gradually if tolerated. No midday extra face wash is mandated.

## Dated 21-day YouTube focus

| Date | Main deliverable | Secondary |
|---|---|---|
| Nov 05 Thu | Relaunch video argument, script outline and visual evidence | Organize assets |
| Nov 06 Fri | Record VO; finish launch edit/thumbnail/metadata | Prep social posts; no upload before Nov 7 |
| Nov 07 Sat | **First new FrameByNavin voiceover video goes live** | Instagram/X teaser and comments |
| Nov 08 Sun | Analyze launch feedback; next flagship outline | Weekly reset and rest |
| Nov 09 Mon | Research Nov 15 scene/filmmaking breakdown | Game-learning block |
| Nov 10 Tue | Script flagship with shot/sound examples | Game prototype |
| Nov 11 Wed | Record VO and assemble flagship rough cut | Audio refinements |
| Nov 12 Thu | Script, VO and rough edit Friday review | Review thumbnail; optional game block |
| Nov 13 Fri | **Filmmaker POV Movie Review** | Commentary-led short/Reel |
| Nov 14 Sat | Finalize Nov 15 flagship and quality checks | Observe filmmaking craft |
| Nov 15 Sun | **Flagship original filmmaking breakdown** | Weekly progress review |
| Nov 16 Mon | Research and outline next Friday review | Game learning |
| Nov 17 Tue | Draft review and visual plan | Game prototype |
| Nov 18 Wed | VO + first review edit | Outline Nov 22 flagship |
| Nov 19 Thu | Polish review audio, subtitles, metadata | Game only if ready |
| Nov 20 Fri | **Filmmaker POV Movie Review** | Commentary-led Reel |
| Nov 21 Sat | Finalize Nov 22 flagship and quality checks | Calm evening |
| Nov 22 Sun | **Flagship filmmaking breakdown** | Weekly rest and review |
| Nov 23 Mon | Research Nov 27 review | Game learning |
| Nov 24 Tue | VO and rough cut for Nov 27 review | Game prototype |
| Nov 25 Wed | Polish review editing and sound | 21-day reflection |

Nov 7 is fixed first publication, Fridays Nov 13/20 are reviews.
Sundays Nov 15/22 are **proposed** next flagship dates; actual publish
timestamps are not silently committed as Creator deadlines.

## Implementation gates

- [x] Separate V285 branch based on V284 successful owner-test source.
- [x] 21-day local preview with date navigation.
- [x] Ten stable, idempotent recurring routines, selectable by category.
- [x] Optional one-time 08:50 local focus prompts only for future dates.
- [x] No automatic installation, overwrite, cloud modification, or Creator writes.
- [x] Warn about 10:15 PM sleep vs 10 PM reading.
- [x] Kotlin unit tests covering dates, launch, category selection, no duplicates,
  repeat rules, and missed-date protection.
- [x] UI test drafts for preview/cancel and bedtime conflict.
- [ ] GitHub Actions compile and Android 15 emulator tests **PASS**.
- [ ] Verify owner APK version 285, SHA-256 and preview signature.
- [ ] Physical-phone tests with owner, no uninstall/reinstallation or data reset.
- [ ] Reliability UX next: compact Today view, Day/Week tabs, manual Done/Snooze
  indicators, permission-health and late/missed reasons.
- [ ] Phase next: real 21-day progress log and rescheduling without Creator writes,
  data export/import and cross-device personal history validation.
- [ ] Final Play readiness: app data handling, policy, signing, privacy and OEM QA.

## Android safeguards

Use existing PersonalReminderStore DataStore transaction, PersonalDeliveryCoordinator
lease / replay guards, per-reminder snooze, and Android permissions. All new
routine and daily-focus alerts are NORMAL notifications; no abuse of full-screen
intent. Exact alarm requests are limited to legitimate critical reminders in
existing V284; Android permission denial must degrade gracefully.
Google Play eligibility for full-screen intents and USE_EXACT_ALARM remains
a **separate** store policy review.

Backups, creator projects, publications, and Personal reminders remain distinct.
A generated preview APK is not an official Play Store release.
