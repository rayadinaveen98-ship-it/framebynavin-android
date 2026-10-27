# Backlot V195.1 Phase 1 — alarm and voice reliability

## Findings and changes
- Both reminder foreground services already used Android notification full-screen intents, but readiness did not inspect the Android 14+ FSI grant or channel importance. This change adds a visible one-line warning for blocked grants, with a user-tap route to the corresponding Android Settings page; checks refresh on return.
- Sound ending must not imply a creator response. Previously both services actively closed an already-visible reminder Activity as audio stopped or speech completed; now services preserve that Activity and their quiet actionable notification fallback.
- To avoid leaving the phone awake indefinitely, the alarm/voice Activities retain their watchdog/fallback logic, now bounded to the selected/worst-case playback window plus a 45s response grace. Existing occurrence ownership and snooze/dismiss actions remain untouched.
- No new storage schema, overlay or forced activity launch, backend change, or alarm/voice audio timing change.

## Platform constraints
- Even with FSI permitted, Android may display a persistent heads-up alert rather than automatically replace an app while the device is actively in use (Android 13+). An off/locked display with full FSI grant and high-importance channel is the intended automatic full-screen path.
- On Android 14+, full-screen access may be denied by default. App must not silently claim access; users grant this in OS settings.
- If exact alarms/notifications are blocked or channels demoted, the UI reports the first blocker. Stage check-in does not override Android notification restrictions.

## Physical acceptance still required
- Samsung One UI Android 14–16, lockscreen on/off and unlocked in-use; exact timing and FSI grants enabled/disabled; notification channel muted; default voice TTS available/unavailable; 1–3 voice repeats; 30–300s alarm duration; Smart escalation and overlapping occurrences; silent fallback; no duplicate follow-ups; response actions after sound stops.
- V195 particle design remains unchanged in this reliability phase; full-screen bokeh and distinct blinking glyphs are planned for Phase 2.
- Preview branch is draft and stacked on V195; leave stable main unchanged.
