# Backlot V287 — Optional Afternoon Nap Wake-up

**Draft / not Play Store release.** Based on V286 reliability-center source; V285 and V286 QA must pass independently.

The 2 PM nap *start* remains V285's optional NORMAL notification. V287
offers a separately opted-in, genuinely audible nap-*end* `ALARM` with one
stable Personal ID: `life21-v1-nap-wake`.

## Durations

| Choice | Nap begins | Wake-up alarm | 3:15 PM editing block |
|---|---|---|---|
| 60 min (default) | 2:00 PM | 3:00 PM | Preserved |
| 90 min | 2:00 PM | 3:30 PM | Overlap warning |
| 120 min | 2:00 PM | 4:00 PM | Overlap warning |

None are installed merely by viewing the UI. A confirmation dialog lists
the time, duration, Android limitations, and the existing reminder update
behavior. Config can be customized by editing the resulting Personal item.

Nap-end alarm default:
- Daily, LOCAL wall-clock, handled by existing Personal Delivery Coordinator.
- Alarm audio with the system's per-reminder configuration; full-screen
  requires granted access on supported Android versions.
- Manual snooze 5 minutes, no automatic retry, 60-second alarm duration.
- Existing notification delivery fallback remains when exact permission
  cannot be granted. No claim of guaranteed ringing under device limits.

## Data safeguards

- Never duplicate the same wake alarm on a changed duration.
- On a confirmed update, reset stale delivery leases and cancel the prior
  PendingIntent before reconciliation.
- Preserve separate Personal completion-history journal, existing non-nap
  Personal reminders, Creator projects, YouTube content and cloud data.
- If nap setup is saved **at or after 2:00 PM**, the new daily schedule starts
  tomorrow. This avoids a surprise wake alarm today for an already-missed nap.
- Reapplying an active, unchanged duration is idempotent; doesn't invalidate
  existing Snooze or acknowledgement.
- No new paid APIs, cloud services, or app permission requests.

## Tests and release blockers

- 6 JVM tests: 60/90/120, conflicts, setup before/after 2 PM, stable ID, alarm
  and snooze configuration, isolation from V284/V285 reminders.
- 2 Android 15 Compose UI tests: preview, overlap warning, cancel without
  installation, no duplicate-install button for saved active reminder.
- Full V286, V285, V284 regression suite remains required by CI.
- Owner APK version 287, SHA256, package identity and signer verification.
- Owner phone must test alarm audio/notification, real snooze, locked-screen
  presentation, Android exact-alarm denial and two-device backup/restore.

**Not implemented:** One-time ad hoc nap timer starting immediately. This
feature schedules a regular **2 PM** nap, not a dynamic stopwatch. Automatic
changes to the 3:15 editing block are deliberately not made.
