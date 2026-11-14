# Backlot Stage 5 — Replacement Android home widgets

Status: **implemented in source; final combined build and device QA pending**.

## New widget family (exactly three providers)
1. **Today Hub** (4×2 nominal): Creator active-work / due count, privacy-safe Personal count, Today and Personal app routes.
2. **Next Reminder** (2×2 nominal): earliest future Creator, Personal, or active Challenge alert, with time and generic source label but NO private title.
3. **Challenge Pulse** (2×2 nominal): confirmed days (0–21) and current challenge day, click opens original Challenge page inside Backlot.

The three original layouts use a consistent Backlot dark/card style, red interactive accents, independent rounded gradients, responsive text fallback and read-only RemoteViews. XML provider metadata uses OS 30-minute update cadence and resize handling.

## Architecture / privacy
- Exactly 3 V392 AppWidgetProvider declarations; all 12 previous registrations remain absent.
- Broadcasts use bounded goAsync() foreground receiver lifetime, 9-second timeout and safe error logs, not a permanently running service.
- Widget content is read-only from Creator tasks, PersonalReminderStore, and Phase1HabitStore. Missing/corrupt Personal or Challenge data is shown as "Open to check", not silently treated as zero.
- Source namespace and instance identity are never confused with the CreatorTask database. Challenge widget deep-links into the challenge-specific screen; no Done or Snooze state is written from the widget.
- New Settings gallery shows ACTUAL RemoteViews sample layouts (explicitly labeled example content), reads pinned counts, and requests Android pinning where supported. Never claims placement succeeded before launcher confirmation.
- V224 and prior provider classes/layouts stay unregistered and dormant for now; clean up obsolete source and update old widget tests during final integrated QA without breaking other app call sites. No user record deletion.

## Final QA gate
- Confirm manifest registers only V392 family after all stages.
- Android 12/13/14/15: picker, add, resize, remove, rotating device, process kill, reboot, disabled notifications, fontScale 1.5 and tablets.
- Verify update on Creator save, Personal edit/snooze and 21-day Done; ensure failed DataStore read displays unavailable, never fabricated zero.
- Verify all PendingIntents route correctly, including challenge-specific deep link. No Personal title, notes or images leak to launcher RemoteViews.
- Verify changing app appearance does not restart retired widget updater and new widgets keep matching design.
- Resource/legacy test cleanup is an explicit final integration action; do not claim tests pass before they run.
