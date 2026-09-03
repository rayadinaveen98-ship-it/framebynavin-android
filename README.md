# FrameByNavin Android

## v0.3.0 — Native Reminder Core

The V0 cinematic theme is frozen. This milestone adds the first real Android reminder foundation:

- task-linked reminders with saved exact timestamp
- Android 13+ notification permission setup
- Android 12+ exact-alarm access setup
- `AlarmManager.setExactAndAllowWhileIdle` when exact-alarm access is granted
- high-importance notification channel
- reminder notification survives app closure / lock screen scheduling path
- notification actions: **STARTED**, **SNOOZE 10m**, **DONE**
- action state changes persist back to the local DataStore
- completing/skipping a task cancels its pending alarm
- one-minute / five-minute / fifteen-minute test shortcuts
- custom date + time picker
- priority and notes stored with each creator task

### V0.3 acceptance test

1. Install the APK.
2. Tap the red alarm button above the bottom navigation.
3. Enable Notifications and Allow Exact Alarms if shown.
4. Select a task, choose `1 MIN`, then Set Reminder.
5. Completely close FrameByNavin and lock the phone.
6. The reminder should arrive near the selected exact time.
7. Test STARTED, SNOOZE 10m, and DONE from the notification.

Boot recovery, voice TTS, alarm/full-screen escalation, and smart escalation are intentionally deferred to later milestones.
