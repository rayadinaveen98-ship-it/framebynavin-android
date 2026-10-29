# Backlot V229.4 — Personal follow-up handoff safety (Build 261 preview)

## Why
An optional one-shot Personal follow-up can pass its pre-notification lease checks, but the user may Done, Dismiss, Snooze, Edit, Pause or Delete the reminder while Android's NotificationManager is updating the card. The first DataStore claim cannot atomically protect an OS notification. An old follow-up could overwrite the visible action buttons for a newer posted lease.

## Bounded change
- Keep existing 30/60/120-minute opt-in policy, single follow-up receipt, device-local quiet hours (10 PM–8 AM), recovery grace, core fire alarms, voice playback and Creator reminders unchanged.
- After notification handoff, re-read the posted lease **and the current follow-up opt-in setting**.
- If the old follow-up is invalid: cancel the stale notification when no valid current posted lease remains; otherwise restore the current posted Personal reminder's normal **silent** actionable card with current immutable action tokens.
- Never replay Voice/Alarm audio, rotate occurrence tokens, mark Done/Dismissed, reschedule any alarm or write a new reminder during repair.
- Pure JVM regression checks cover deleted, paused, no-posted, old-posted and newly-posted records.
- Full Build 261 PR CI repeats all previous JVM/lint and Android 11/15 reminder/Spark UI checks plus signing-compatibility against verified Build 260.

## Limits and physical device acceptance
NotificationManager and DataStore cannot be made one atomic Android operation; this is a **best-effort post-handoff repair**. A subsequent user change can still race the restoration, and a brief audible notification may occur before an opt-out is observed. The authoritative action receivers still reject stale lease tokens. On-device QA must validate notifications and action labels during Done/Snooze/opt-out, Android 11/15 OEM Doze, notification permissions, repeat scheduling and upgrade without data loss. Do not merge or label as stable until complete.
