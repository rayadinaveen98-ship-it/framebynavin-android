# Backlot — Play foreground-service and full-screen intent evidence script (DRAFT)

Do not submit fabricated videos. Run on a physical Android 14/15/16 device with test data and record a genuine screen capture, in app, including system permission dialogs and notification trays.

| Service | Legitimate feature shown | Recording steps | Interruption effect to disclose |
| --- | --- | --- | --- |
| `AlarmRingingService` | Creator's scheduled, audible alarm | Create test project with Alarm, permit notifications as needed, wait for scheduled time, show sound/vibration, Done/Snooze and stop | Audio may be delayed or cancelled; notification must stay actionable |
| `VoiceReminderService` | User-set Creator TTS reminder | Create voice reminder and select 3 repeats, show voice audio, chosen background track, notification and user Done/Snooze | TTS may not start if voice/language unavailable, or may stop; ordinary notification fallback |
| `PersonalPlaybackService` | Separate Personal audio reminder | Create Personal voice test, confirm private notes aren't read, show notification, listen, Stop/Done/Snooze | Device power management or denial may defer/mute sound, but reminder must remain actionable |

## Full-screen alerts review
1. Show how Backlot (a broader creator organizer, **not automatically a qualifying core alarm app**) requests special full-screen alert access, if present.
2. Deny or revoke FSI and prove ordinary user-visible notification/screen-lock fallback. Do not claim an exemption without Play adjudication.
3. Compare phone locked vs unlocked on supported OS versions without routing unexpected content into system overlays.
4. Prepare explanation and real demonstration video for Play Console if permission retained. Alternative is to remove FSI after separate owner acceptance and full Android regression QA.

## Record actual evidence
Device model/Android version; permissions granted/denied; event time vs scheduled time; background restrictions; UI screens shown; audio output and dismiss paths; test account ID redacted. Keep any screen capture private until appropriately reviewed. This is a developer QA script, not proof of permissions compliance.
