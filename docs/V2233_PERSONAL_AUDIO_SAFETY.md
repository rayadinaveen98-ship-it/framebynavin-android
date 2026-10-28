# Backlot V223.3 — Personal Voice and Alarm (draft)

## Implemented (engineering preview)
- **Independent PersonalPlaybackService** using mediaPlayback foreground-service type already in Backlot's manifest. Receives only immutable Personal id, scheduled occurrence, fire time and rotating lease UUID; verifies DataStore posted token, paused state, alert mode and age before playing. No creator TaskStore, stage checks or creator alarm/voice service reused.
- **VOICE:** Android on-device TTS, one short utterance containing the title only (never notes), stop when speech completes or 35 seconds, whichever is first. Missing voice engine/language/FGS permission: retain existing private notification and user actions.
- **ALARM:** Android default alarm ringtone using async MediaPlayer preparation, controlled looping and vibration. Hard safety timeout 60 seconds; no implied Done/automated Dismiss after audio ends. Fallback is a notification.
- **Private notification:** Personal title hidden on lock-screen, generic publicVersion, independent notification channel. Personal music foreground channel is silent and generic. Existing Done/Snooze/Dismiss action tokens continue to work even after sound stops. Sound stops on an actual action, edit, pause, delete and service destruction. Concurrent Personal alarms share a single player: displaced reminder remains actionable via its own notification.
- **PersonalAlertActivity** is opened only by user tapping a voice/alarm notification, with no unsolicited full-screen intent. It rechecks current posted token on read, closes stale tokens and provides large Done, Snooze 10min, Dismiss controls. A 100-point blue/violet/coral glow wave distinguishes personal voice vs alarm. No lockscreen bypass, no creator-stage actions.

## Important limitations
- Background media-playback FGS may be refused by Android 12+ or OEM restriction. This release does **not** claim guaranteed voice/alarm sound, and does not silently mark a reminder delivered, acknowledged or completed based on audio playback. Notification fallback remains.
- Device DND, ringtone mute, battery optimization and TTS availability may prevent audible output. Automated emulators cannot validate actual speaker loudness or wake-up behavior. No automatic full-screen UI; Play Store full-screen-intent policy is not assumed applicable to this combined creator/personal app.
- One Personal audio service exists. If multiple alarms arrive together the newest audible presentation replaces the earlier one; the earlier notification remains actionable and uncompleted.
- No new permissions added; existing FOREGROUND_SERVICE_MEDIA_PLAYBACK is reused. No Play Store release or stable-branch merge.
- No private title/notes in lockscreen publicVersion or foreground notification. Alert screen appears after user action. Personal notes are never spoken.
- Sound timeout is not answer timeout. Reminder remains in personal journal and actionable on notification afterward.

## Automated gates
- JVM admission policy: posted token, not paused, matching id, exact original occurrence and fire time, no stale/early/overdue sound.
- Android UI: particle wave + real response buttons + no private content on invalid token.
- Existing V223.2 UI creation, actual receiver/notification/DONE, V223.1 action replay protection, V223 Personal backup, V222 projects cold-start regression.
- New manifest service and activity must be non-exported, Personal service must have mediaPlayback type.

## Next before physical-owner preview
Validate Android 11, 13, 15/16 with notification/exact-timing permissions, Doze and locked screen; actual alarm arrival via AlarmManager (not just direct receiver), background foreground-service start failure, response while sound plays, voice language availability, ringtone/vibration/DND, task-killed app, reboot, time changes and battery policies. Only then claim reliable Personal voice/alarm UX. Review visual contrast and motion accessibility on compact and tablet screens.
