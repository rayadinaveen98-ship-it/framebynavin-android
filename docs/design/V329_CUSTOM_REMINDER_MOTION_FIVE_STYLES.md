# Backlot V329 · Custom Reminder Motion (approved V2 directions)

## Scope lock
Presentation only. No changes to alarm/voice services, notification posting,
reminder scheduling, response actions, personal privacy, snooze, replay,
reschedule, music, or creator/personal reminder storage.

## Five native motion styles
- Crimson Singularity — orbital embers, plasma rings
- Liquid Frequency — reflective wave ribbons, fluid light
- Obsidian Bloom — floating ruby facets
- Signal Architecture — scanning grid / concentric signal rings
- Nocturne Constellation — subtle microstars and linked lights

All five are procedural Compose Canvas implementations. The mic/clock silhouettes
are composed of moving dots based on the existing V195 glyph geometry, without
an icon sticker.

## Independent user customization
- Creator Alarm
- Creator Voice
- Personal Alarm
- Personal Voice

Each can pick any style and adjust glow intensity, particle density, and motion
speed independently. The choices persist locally in a separate SharedPreferences
store. They do **not** change stored reminder records or system delivery settings.

- Creator: Settings > Appearance & sound > Appearance > Reminder motion
- Personal: Personal reminders > Settings > Reminder motion

Settings contain a live native preview; changing a slider saves immediately.

## Interaction and safety
- Single ordinary tap: transient energy ripple; no navigation
- Double ordinary tap: reveal the exact pre-existing reminder action surface
- Assistive technology semantics: single accessibility activation to open controls
- Animations disabled in Android: bypass decorative gate and show controls
- Battery saver / low-RAM: fewer particles, reduced effect count and 20fps cap
- Personal keyguard still shows the existing locked, redacted Personal screen
- Personal notification-only alert: unchanged, no new gate
- Reminder watchdogs remain active: no new timers or delivery side effects

## Files changed
- V329ReminderVisualPrefs.kt (new visual-only model and local persistence)
- V329ReminderMotionScene.kt (new native Canvas worlds and tap behavior)
- V329ReminderVisualSettings.kt (new chooser / live preview)
- ReminderCheckInScreen.kt (Creator entry substitution only)
- PersonalAlertActivity.kt (Personal entry gate before unchanged V2233 UI)
- V144SettingsHub.kt and V2232PersonalReminderHub.kt (existing settings integration)

## Verification
Source implementation and unit + device persistence tests are committed.
A Kotlin/Android build and real Android visual/gesture QA are required before
promoting to stable. Keep V328 owner release untouched until V329 passes.

### Owner acceptance
1. Pick different styles for all four contexts; close/reopen settings and confirm.
2. Open each reminder: single tap should pulse, double tap should reveal.
3. Confirm all previous Done/Working/Snooze/Pause/Reschedule/Dismiss/Replay buttons,
   callback effects, service watchdogs and alarms remain identical.
4. Confirm Personal locked alarms are redacted, notification-only alerts unchanged.
5. Toggle Android reduced-motion / Battery Saver; verify controls remain accessible.
6. Verify low-end device fps, density and orientation; visual stage should not
   delay alarm playback or post new notifications.
