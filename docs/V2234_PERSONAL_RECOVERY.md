# Backlot V223.4 — Personal missed-occurrence recovery (draft)

## Problem fixed
V223.3 kept a posted Personal reminder actionable until an explicit user response. When no response arrived, the pending lease could block later recurring occurrences forever. An unposted overdue lease beyond 24 hours was cancelled without clearing its durable state, so future reconciling kept returning to the same stale lease. Recovery could also move the fire time to now + 2s, making a several-hours-old Personal alarm appear new to the voice/alarm admission policy.

## V223.4 contract
- **24-hour explicit grace window:** Do not expire a recently posted reminder. Allow up to 24 hours after the original scheduled occurrence (or a genuine user Snooze/Reschedule time) for an explicit response. For recurring reminders, expire the old unanswered occurrence earlier—two hours before the next scheduled occurrence—when necessary to avoid skipping tomorrow's reminder. OS catch-up re-arming never extends the grace window. Preserve the Personal record and all earlier explicit Done/Dismiss/Snooze history; do not pretend a user answered.
- **Separate missed timestamp:** `lastMissedOccurrenceMillis` is not `lastAcknowledgedOccurrenceMillis` and does not change `lastResponse`. Backward compatible Personal v1 JSON: old records default the new field to 0, and their checksums still validate. No CreatorTask, CreatorBackupManager or creator analytics mutation.
- **Autonomous recovery watchdog:** In addition to startup/reboot reconciliation, schedule a Personal-only inexact AlarmManager recovery wakeup at the calculated grace deadline + 5 seconds. Verify the current immutable token before reconciling. Keep the watchdog armed even when the original posted notification's fire PendingIntent is cancelled. Doze/OEM restrictions may delay it, so this is best-effort rather than a guarantee.
- **Reconcile safely:** Expire only the exact current pending token/occurrence using the existing DataStore mutation lock. Remove stale posted notifications *after* invalidating their old action tokens. Recurring reminders schedule their next future occurrence; one-time reminders retain the visible MISSED marker without fabricating a new occurrence.
- **No late audible resurrection:** For an ordinary delayed delivery, audio age is measured from its original scheduled occurrence even if recovery rotates the fire timestamp. Only a genuine user Snooze/Reschedule can authorize audio at the later chosen fire time.
- **UI:** A subtle MISSED indicator flags unanswered old occurrences even when a recurring next occurrence is scheduled. It is not a Done event or a punitive alarm.
- **No false guarantee:** Android 12–16 background alarms and FGS remain dependent on OS restrictions; this is a recovery-state fix, not real-device certification.

## Verification
- JVM pure one-off 24h grace-boundary, posted/unposted policy, next-day recurrence clearance, OS rearm and user snooze tests.
- JVM delayed-recovery audio admission / explicit user snooze tests.
- Android 11 real DataStore/AlarmManager instrumentation: overdue daily schedules next, independent recovery PendingIntent exists, once remains missed, stale notification action rejected, CreatorTask untouched.
- Preserve previous V223.3 audio/alert/UI, V223.2 editor, V223.1 journal, V223 backup and V222 Home startup tests.

## Remaining
Physical-phone Android 11/13/15/16 notification/permission/Doze/reboot/TTS/DND testing, independent Personal export UI, widget replacement and tablet accessibility. Keep PR draft and do not merge to stable until verified.
