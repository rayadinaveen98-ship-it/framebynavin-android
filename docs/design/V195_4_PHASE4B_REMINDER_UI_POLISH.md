# Backlot V195.4 Phase 4B — Reminder UI polish

Based on approved V195.4 Phase 4A Spark expression prototype.

- Keep the user-approved V195.3 particle field, mode-specific Spark, manual reveal, sound and alarm services, reminder state transitions.
- Tighten information hierarchy: brief ALARM / VOICE label, due time, Spark, prominent title, small contextual metadata, optional supporting line only if genuinely distinct.
- Remove "UP NEXT" card and "WHAT HAPPENS NEXT" instructional text from reminder screens; preserve underlying responses and follow-up behaviors.
- Primary prominent solid action: I'M WORKING (normal reminder) or STEP DONE (stage check-in); keep secondary I'M WORKING for stages.
- Snooze always visible; More Options houses pause, reschedule, replay and dismiss, plus notes only when present. No hidden action is deleted.
- Compact height strategy below 720dp, stacked Snooze/More below 390dp width; content scrolls if large font or small screen.
- Stable test tags and 48dp+ touch target sizes; preserve previously existing action wiring.
- New pure layout policy tests and 3 Android emulator interaction suites, plus all inherited safety regressions.

Not in scope: full-screen Android permissions, TTS, particle tuning, new raster character art, notification delivery, stable release promotion. Verify contrast, TalkBack and scroll accessibility on Samsung hardware before declaring stable.
