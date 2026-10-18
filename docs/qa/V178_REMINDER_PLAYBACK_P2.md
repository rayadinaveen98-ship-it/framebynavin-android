# Backlot V178 — Reminder Redesign Phase 2: Voice and Alarm Lifecycle

**Base:** V177 durable acknowledgement journal. **Status:** Draft preview, not stable. No project schema migration or change to existing scheduled alarm times.

## Implemented
- **Voice:** stop foreground speech playback after the final configured utterance, with 1.5 seconds of grace for an in-flight action. The old worst-case window (up to minutes) remains only a maximum watchdog, not the required playback lifetime.
- **TTS initialization stall:** arm the bounded service watchdog *before* asynchronous TextToSpeech initialization; if TTS fails or never starts, the service cannot linger indefinitely.
- **When speaking finishes:** attempt to post a quiet, actionable standard Backlot notification with the **same original occurrence token**. Record unanswered without inventing Done/Snooze/Working, close the voice activity, then stop the foreground service and release the wake lock. There is no automatic voice replay.
- **TTS error/unavailable:** fall back to ordinary notification, close the voice screen promptly and stop the voice service. A missing voice engine does not count as spoken audio.
- **Alarm:** honor the exact existing creator-selected duration (30–300 seconds), then release the media player, vibration and wake lock. Attempt one normal actionable notification with the same token, mark unanswered, close alarm activity. Smart escalation scheduling remains unchanged, including its later CRITICAL stage.
- **Activity watchdogs:** run 3 seconds later than the service's maximum safety window, and provide a final same-token notification handoff if the foreground service was interrupted.
- **Acknowledgement:** V177 terminal creator responses always take precedence over late timeout markers. An unanswered alert remains actionable unless superseded; no project task is silently marked finished.

## No behavior change in this phase
- No new retry scheduler, infinite ringing, extra alarm attempts or escalation after final Smart stage. Those belong to Phase 3.
- No change to stored voice repeats (1–3), voice gaps (5–60 seconds), alarm duration (30–300 seconds, default unchanged), or snooze preferences.
- No removal of original actions, notification channels, full-screen permission handling, or Project Pulse flow.
- No claim that the creator actually *heard* an alarm, *read* a notification or responded just because playback stopped.

## Automated gates
1. Existing V161–V177 source checks, V178 lifecycle check, full JVM unit tests.
2. Android lint, instrumentation-test compilation, APK assembly and artifact checksum.
3. Android 11 emulator runtime: V178 same-token notification handoff, V177 response isolation, V159 duplicate-action/recovery tests.
4. Required creator/physical-device acceptance. Emulator tests exercise the software state transition, not real TTS quality or hardware ring/vibration.

## Real Android acceptance checklist
- Backup and compare signing before updating a data-bearing Backlot installation. Never uninstall simply to avoid a signature mismatch.
- Voice with one/two/three attempts: verify the service releases audio immediately after the final sentence + brief grace, the voice screen closes and the original reminder remains actionable as a notification.
- With missing/unavailable TTS and language pack: fallback notification appears, voice screen does not linger and no false spoken event is asserted.
- Background/cold start/full-screen denied: service watchdog releases resources and the same occurrence remains actionable if permissions allow posting.
- Alarm 30s, 45s, 120s, 300s: stops ringing/vibrating at selected deadline, screen closes, notification preserves Snooze/Working/Done choices; no unsolicited additional loud alert.
- Smart stages: next stage is scheduled as before, old stage notification doesn't steal the new stage's actions.
- Rapid response at timeout boundary: an accepted Snooze/Working/Done is never reverted to UNANSWERED, and an outdated notification cannot affect a newer project occurrence.
- Two overlapping reminders: the displaced reminder remains actionable, no cross-task close or incorrect sound cancellation.
- Boot, Doze, exact-alarm denied, notification permission denied, battery saver and audio focus: confirm graceful failure without leaked foreground service or wake lock.
- Confirm stable project data and reminder schedules survive signed in-place upgrade; protect user data.

## Known limitations
- If Android blocks the ordinary notification handoff, the occurrence is still marked unanswered, but the upcoming Reminders Center work is needed to show it centrally.
- Voice/Alarm actions and per-OEM delivery still require physical-device testing; Google CI emulator is not a substitute.
- Phase 3 will implement bounded unanswered follow-ups, using this journal. No future background work has been started.
