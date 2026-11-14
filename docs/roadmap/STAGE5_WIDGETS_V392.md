# Stage 5 — Replacement Home-Screen Widgets (V392)

Status: **implemented in source only; combined build and tests pending owner-requested final gate**.

## Retired widget migration
- All 12 old V224 / Creator widget providers remain unregistered (Stage 2).
- The new family declares exactly **three Android widget providers**:
  1. Today Hub (4x2) — active Creator projects, non-paused Personal reminder count, upcoming reminders today.
  2. Next Reminder (2x2) — next time and source among Creator, Personal and 21-Day challenge.
  3. Challenge Pulse (2x2) — active 21-day challenge day and confirmed progress.
- Uses fresh RemoteViews layouts with consistent dark glass, warm ivory, red/gold cinematic palette, rounded card background and easy refresh affordances.
- No new paid service or dependency.

## Security and correctness
- No Personal title or notes, no journal content, no private challenge activity text on the launcher.
- Widget taps open the appropriate existing screen; they never directly complete, dismiss, snooze, create or edit a reminder.
- A failing/corrupt Personal, Creator or Challenge data read is represented as unavailable, not fabricated empty data.
- AppWidgetProvider uses bounded goAsync instead of detached broadcast jobs.
- Manual refresh, Android periodic 30-minute update and best-effort post-commit refresh notifications.
- Existing canonical storage, occurrence IDs, scheduling, and data reset semantics are unchanged.

## Upgrade caveat
Android launchers vary in how they handle existing pinned widgets whose providers are removed. The deprecated widgets may disappear or show stale placeholders after upgrade; the new three must be added separately. The migration must never erase app data.

## Final combined QA gate
- Check actual Android launcher picker contains only the new three Backlot widgets.
- Add, resize, force-stop, reboot, refresh, and test while data stores are initially empty, populated and corrupted.
- Validate small 2x2 widgets, large fonts, reduced motion, TalkBack, dark/light launcher appearance and widget contrast.
- Verify all tap routes and exact challenge focus, and confirm zero automatic Done/Snooze mutations.
- Confirm project changes, Personal edits and Challenge progress update pinned widgets without battery-intensive polling.
- Verify malformed/missing data labels unavailable instead of zero.
- Clean obsolete source/resources only after compiling new widgets and auditing all references; do not remove in-app Quick Idea functionality.
