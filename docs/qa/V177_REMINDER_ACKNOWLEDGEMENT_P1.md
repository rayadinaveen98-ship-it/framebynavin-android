# Backlot V177 — Reminder Redesign Phase 1: Acknowledgement Evidence

**Base:** V176 New Project candidate. **Status:** Draft preview. No stable/main merge.
**Goal:** record *attempts and explicit responses* without inferring that alerts were seen/heard, rescheduling unanswered occurrences, or changing user data.

## Durable observation states
- **ATTEMPTED:** a valid authority token was issued and dispatch was attempted; no successful device surface claim.
- **NOTIFICATION_POSTED:** Android accepted a posted notification. Not a read receipt or proof it was displayed to the creator.
- **VOICE_ACTIVE:** voice foreground service started. Not proof TTS audio was heard.
- **VOICE_SPEAKING:** Android TTS invoked its utterance-start callback.
- **VOICE_PLAYBACK_FINISHED:** final configured TTS utterance completed, but no creator action has been received.
- **ALARM_ACTIVE:** alarm foreground service started its sound/vibration routines, not proof the creator heard it.
- **UNANSWERED:** a voice/alarm timeout occurred without accepted action, a newer stage replaced an unresolved stage, or an Android notification remained posted for at least 20 minutes with no explicit response **when periodic/startup reconciliation happens**.
- **DELIVERY_FAILED:** attempted delivery/fallback failed. A subsequent fallback notification may still be posted.
- **RESPONDED:** a creator action was accepted and committed by TaskStore (stage done, working, snooze, pause, dismiss or reschedule). Only the authoritative action-receiver path can set this after it commits.

The journal stores only task ID, occurrence token, data generation, channel, state, timestamps and a bounded reason/action code. No titles, notes, audio, creator name or content details are serialized. Stored in a separate SharedPreferences file; maximum 12 recent occurrence records per project. No data-schema change or migration.

## Stale action and reply safety
- `begin` requires the existing ReminderOccurrenceStore token and current task fingerprint.
- A new authorized occurrence supersedes a still-unanswered previous stage without guessing a user response.
- Updates apply only to the latest token in the current data generation.
- `RESPONDED` is terminal: delayed timeout callbacks cannot undo a committed Snooze/Working/Done.
- An `UNANSWERED` occurrence can become `RESPONDED` if the same valid action is accepted later.
- Only the existing ActionReceiver records an accepted response **after TaskStore mutation/reward recovery has completed**; journal failures cannot cancel a valid action.
- A notification posted 20 minutes ago can become `UNANSWERED` during existing app-start/WorkManager reconciliation; this **does not** schedule additional alerts.
- An already completed one-shot or a project whose occurrence is no longer authoritative is not surfaced as actively awaiting response.
- Generation filtering blocks stale data after backup restore. Explicit `clearAll` and `clearTask` are available for a later coordinated cleanup.

## Not included in Phase 1
- No retries, escalation changes, additional WorkManager cadence, notification retry scheduler, or new Reminders Center UI.
- No modifications to TTS repetitions/intervals, alarm ringing duration, snooze defaults, quiet hours or Android permission requests.
- No claim that a notification was opened/read or the alarm sound was audible.
- No automatic reactivation of missed reminders after token retirement.
- End-to-end delivery on specific OEM devices remains unverified.

## Automated tests
1. V161–V176 inherited source-preservation scripts and new V177 source checks.
2. JVM transition tests for terminal response, timeout, duplicate events and voice fallback.
3. Android 11 emulator test class `V177ReminderAcknowledgementInstrumentedTest`: durable journal, stale tokens, superseded Smart stage, grace period and response precedence.
4. Existing `V159ReminderActionIsolationTest` and `V155ReminderRecoveryIsolationInstrumentedTest` run alongside V177.
5. JVM tests, Android lint, androidTest compilation, candidate debug APK assembly/upload.
6. **Emulator runtime must pass; compilation does not count.** Device sign-off is separate.

## Real device sign-off
- Back up existing Backlot data and verify compatible signing before any in-place preview upgrade. **Do not uninstall a data-bearing app.**
- Notification due -> posted; no response -> periodic recovery eventually marks unanswered. Confirm no new audible retry occurred.
- Voice begins -> TTS start/finish -> service timeout, no response -> unanswered. Check user action takes precedence and TTS-unavailable fallback is recorded.
- Alarm starts/vibrates -> timeout -> unanswered; a completed Snooze, Done, Working or Dismiss remains RESPONDED even if old callbacks arrive.
- Smart SOFT -> VOICE -> ALARM: earlier unanswered stages retained in journal without duplicate response; later stages continue as originally defined.
- Rotate/resume/boot and disabled notification/exact alarm/TTS availability; verify no false acknowledgements or data loss.
- Signed in-place upgrade, missing notification/full-screen permissions, and project restore require physical-device acceptance before stable release.

## Next in roadmap
Phase 2 voice/alarm lifecycle and durations; Phase 3 bounded no-response retries; Phase 4 controls; Phase 5 unanswered Reminders Center; Phase 6 device reliability.
