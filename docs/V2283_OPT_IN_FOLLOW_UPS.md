# V228.3 — Opt-in bounded Personal Follow-ups

This work is stacked on draft V228.2, not merged into stable. Development Build 255 keeps app ID and signing identity.

### Actual behavior
- **OFF by default.** User can opt in from Personal Reminders and choose a 30/60/120-minute interval.
- At most **one** optional follow-up for each **posted but unanswered** occurrence, using a durable receipt in the Personal DataStore. No forced extra exact-alarm permission.
- Quiet hours **22:00–08:00 device-local**. A due nudge during quiet time is shifted to 08:00 only if still within the original occurrence's recovery grace.
- If Android delivers more than 15 minutes late, suppress the nudge instead of surprise-alerting later. No posting if not inside the posted current lease, paused, already responded, or recovery expired.
- Same Personal notification with **Done / Snooze / Dismiss** actions, same private-on-lock-screen policy. No CreatorTask or YouTube connection.
- Changes to opt-in settings trigger a reconciliation and cancel any old follow-up if switched off. An independent receiver checks current settings and the stored token again.
- Optional preference corruption fails closed and must not block normal reminders. Users can continue to receive original reminders with follow-ups off.

### Safe upgrade and backup
Adds optional `followUpSentToken` within Personal reminder schema 1; decoder accepts old records without the key. No existing record is deleted. Only one lease token is stored, never a history of alerts.

### Known limitations / next device QA
Android may delay inexact alarms and may block notification permission or audio. A scheduled follow-up is **not** proof of delivery. Real physical-device tests must exercise Doze, notification settings, reboot, user actions racing a follow-up, alarm playback and upgrade from Build 254. Keep PR draft until Android 11/15 tests and owner QA pass.
