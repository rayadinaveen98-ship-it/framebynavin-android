# Backlot V295 — Universal Personal OS roadmap

> **Baseline**: verified V294 Wave2 signed owner QA candidate `1231b858480a8677e744019d3f59e56e45616f36`. V295 must remain isolated until signed in-place preservation, full-screen reminder, unit, and owner acceptance gates pass.

## Direction
One user's personal schedule must never become everybody's default. Backlot ships **structures**, but the user creates **content, times, and goals**. A reminder is scheduled once by the existing Personal delivery system and is *referenced* by Spaces, Plans, and Spark. A rendered notification does not prove it was seen or completed.

## Phase 1 — universal foundation (implemented on V295 draft branch; QA pending)
- Remove V285 hardcoded Nov 5–25, 2026 YouTube/filmmaking board, V284 fixed diet/fitness setup, V287 2 PM nap preferences, and duplicate Device reliability card **from visible Personal Settings**. Their historical implementation and saved reminders remain untouched, to preserve owner edits, acknowledgements and data.
- Replace with `My Routines`: a quiet, user-owned Daily Rhythm list computed from existing Personal reminders and a **New routine reminder** action leading to the existing validated editor. No fake 21-day builder or automatic sample installs.
- Optional starter suggestion chips remain editable and may NOT replace user's chosen title, time, repeat, or alert. Generic wording only.
- My Spaces: use existing FK-backed reminder link table to expose Link/Unlink and edit existing reminders. Never write to the schedule when linking; removing a Space never deletes underlying Personal reminders. Read only user-confirmed Done receipts as the completion count; no inferred success from Android notification posting.
- My Spaces: separate **Notes** list and Add Note flow from Check-ins/Metrics and Photo Journal, using existing V289 NOTE entry records. Notes remain device-private; all data is owner-owned.
- Preserve current Spaces metadata export and disclose explicitly that Backlot cloud sync still does not include Spaces or private photo bytes.

## Phase 2 — truly universal planners (not implemented yet)
Create durable, editable, user-defined plan records with tests/migrations and backup support, before displaying new plan builders:
- **21-day Reset**: name, start date, 21 editable daily activities, optional reminders, clear finish/archive state; day 1 starts at *user selected* date, not a code constant. Steps can be left blank, skipped, changed or reordered. No automatic scheduling without preview+confirmation.
- **Daily Rhythm**: user-created routine items plus a synchronized projection of saved recurring Personal reminders. Editing a reminder affects a single authoritative schedule.
- **Reminder Groups**: user-defined group names, colors/icons, membership via references; a reminder may appear in multiple groups; deleting a group only removes references.
- **Diet & Fitness**, **Rest & Sleep**: universal blank/grouped starters; no hardcoded recipes, weight targets or sleep schedules. Optional examples are neutral, editable and always require Save.
- Existing V284/V285 managed reminders are grandfathered as user-owned records, not reinstalled or rewritten.

## Phase 3 — personal reminder composer
Four-step progressive UI:
1. Purpose, editable title, optional note.
2. Date, repeat; reveal days/hourly window only after selection.
3. Notification/Voice/Alarm; reveal speech/music/snooze/auto-retry only for chosen type and when Advanced is opened.
4. Review, optional Space/Group link, explicit Save.
Keep all editor state when navigating Back, accessible 48dp tap targets, keyboard-safe sticky actions, motion-reduced transition, precise existing schedule validation. Edit existing reminder may jump directly to a step.

## Phase 4 — activity and Spark
- Dedicated local-first Activity Center stores/references true Creator reminder acknowledgements, Personal confirmed Done, scheduling permission issues, project deadlines, and important changes. Do NOT equate sent with seen/heard; action buttons re-validate latest occurrence token.
- Compact Creator Reminder screen shows nothing when pending count zero, otherwise a single labelled action row; full history moves to Activity Center, not Settings.
- Spark becomes an optional friendly launcher and top 3 prioritizer of **multiple real evidence-backed** items, with a separate See all page and a non-Spark activity affordance for users who disable companion.
- Privacy: personal reminder titles hidden from broad previews unless opted in; quiet hours, deduplication, stale dismissal and read/unread distinct from Done.

## Release gates
- Source boundary check: no unintended changes to Android manifest, cloud, alarm, Personal reminder codec/model, Creator project store, or photos.
- Android 15: 10 screens including universal Personal Settings, 20 Settings/Personal interactions, 10 Spaces tests; no ANR, no blank screens, real legible typography.
- Link/unlink cannot schedule/cancel anything, mutate a reminder, or fabricate Done; standalone Note can be saved without a check-in.
- Full in-place **V294 → V295** signed matching-certificate upgrade with real synthetic owner records and photo SHA256; clone existing V292→V294 tests, with *no clear after seed*.
- New plans and Groups eventually require explicit metadata export/cloud-scope review; avoid silent data-loss claims.
- Physical owner tests: full-screen alarms with screen locked, 3x voice, custom music ducking, Snooze, cloud restore scope, media and background reliability.
- Never distribute APK from this draft branch until all gates succeed and user approves testing.
