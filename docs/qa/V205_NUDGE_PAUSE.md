# Backlot V205 — One-tap pause for optional creator nudges

Version: 225 · `2.7.5-v205-nudge-pause-preview` · Draft.

## Change
- Every **optional creator-context nudge** now includes **Pause 7 days**.
- Tapping the action writes a seven-day pause expiry in local preferences and cancels the optional notification. No user account, internet, or external analytics required.
- The existing V205 nudge worker declines to post more creator-context nudges during that pause and resumes automatically once the seven days expire.
- Settings > Reminders shows the paused status and expected resume date while the existing opt-in switch remains on.
- Turning the optional creator-context switch **off then back on** explicitly resumes immediately by clearing the pause expiry.
- Android receiver is non-exported; pending intent is explicit and immutable.
- No modification to actual scheduled project reminders, voice/alarm/Smart escalation, follow-ups, notification channels, data schema, app identity, signing, splash, or V200 branding.

## Verification
- Pure JVM `V205NudgePausePolicyTest`: default unpaused, seven-day exact expiry, boundary and invalid/overflow timestamps.
- Android `V205NudgePauseReceiverTest`: private action stores pause durably; wrong action does nothing; unrelated preference untouched. Tests run only on disposable CI emulator.
- `tools/check_v205_nudge_pause.py`: inherited all V200-V204 branding, permission, local quiet-hours, limit, freshness, deep-link and source contracts plus private action/Settings/resume policy.
- Full exact-SHA Android unit/lint/signing/emulator workflow before installing.
- Physical acceptance: V204→V205 in-place update, Settings paused status, 7-day pause action, app restart survival, manual early resume, current user opt-out, no change to real scheduled reminders, alarms, voice or Smart flows.
- No stable merges until green CI and creator device acceptance.
