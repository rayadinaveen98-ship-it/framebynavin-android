# Backlot V158 — Reminder Delivery R4

**Based on V157** development branch commit `d77f789b3c22a950030078cd355a7d91c4654adf`. V154–V157 remain unchanged.

## Product goals
Make incoming Voice and Alarm reminders easier to understand and respond to, and allow quiet check-ins overnight without silently canceling, delaying or disabling any reminder.

## Implemented
- Full-screen reminder screens present three immediate controls: **STEP DONE** (only for managed stage check-ins), **I'M WORKING**, and **SNOOZE**. Less common actions Pause, Change Time, Replay Voice and Dismiss For Now appear under **MORE OPTIONS**.
- The expanded actions explain likely check-in timing per selected plan, using conditional wording rather than Android delivery guarantees. "Dismiss for now" does not mark a project complete.
- Quiet Hours are **off by default**. The Settings > Notifications & Reminders page lets the creator turn them on and choose an evening start (8–11 PM) and morning end (6–9 AM), default 10 PM to 8 AM.
- Quiet hours use local device time, work across midnight, and use a dedicated low-importance Android notification channel. Eligible gentle check-ins are delivered on time but without app-triggered sound or vibration. Existing Android channel customization or device DND may still affect presentation.
- Explicit Voice reminders, ringing Alarm reminders and all non-gentle Smart escalation stages are intentionally exempt from Quiet Hours. A creator explicitly choosing them should expect interruptions; copy in Settings warns about this.
- Quiet Hours settings persist locally, are included in existing exported/restored settings JSON, and old backups default quiet hours to off. No task schema or scheduling time changes.
- TTS now checks device-language availability. If speech-language resources are unavailable, it attempts an actionable notification fallback rather than silently behaving as if it had spoken.
- All V155 R1 recovery, V156 plan selection and V157 Reminder Center actions remain unchanged.

## Automated testing
- Unit tests: opt-in default, overnight boundaries, end-exclusive semantics, daytime windows, timezone behavior, 12-hour labels, plan-specific follow-up copy.
- Instrumentation: settings backup import/export including legacy data, primary check-in UI, More Options, Pause/Reschedule/Replay/Dismiss behavior.
- CI: smoke, unit, Android lint, instrumentation compilation, APK build/artifact, Android 35 emulator targeted tests.

## Limitations and careful claims
- This is **discreet delivery**, not deferred delivery: soft notifications remain visible at their scheduled time and are not queued to morning. Android may further suppress notifications with its own DND or channel settings.
- Quiet Hours do not prevent explicit ringing alarms or Smart escalation; they also do not mute all system notifications or non-project automation notifications.
- Some OEMs limit exact scheduling, full-screen notifications and TTS languages independently of Backlot.
- No voice-language picker yet; speech uses the current device locale and falls back on failure.
- More Options remains scrollable and is not covered by physical-device accessibility acceptance until manually tested.
- Android emulator tests are useful but do not prove reliability on all physical Android devices.

## Phone acceptance
- [ ] Install V158 on top of V157 without data loss.
- [ ] Settings > Notifications & Reminders > Quiet Hours defaults off, shows 10 PM–8 AM, can be toggled and adjusted.
- [ ] During configured quiet hours, normal notifications are silent but visible and actionable.
- [ ] During quiet hours, explicit Voice/Alarm and later Smart escalation still interrupt (test only with intentional sample reminders).
- [ ] Open Voice and Alarm check-in screens; Step Done/Working/Snooze easily reachable.
- [ ] More Options expands; Pause, Change Time, Dismiss For Now and Replay Voice (Voice screen) work correctly.
- [ ] Dismiss does not mark a project DONE; publication/Stage Done safety remains intact.
- [ ] Voice language resources unavailable -> actionable notification fallback (may require device TTS package settings).
- [ ] Repeat under screen lock, low-power mode, Android 13+ permissions, 200% font scale and TalkBack.
