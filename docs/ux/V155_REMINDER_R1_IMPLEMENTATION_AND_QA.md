# Backlot V155 — Reminder reliability R1 (preview)

**Base:** V154 PR #10 `a3e5e3aa2eb638183902b168f2cb1d28159f2c3c`. V154 remains unchanged.

## Scope implemented
- Smart escalation's **final target** remains a strict boundary for escalating stages; an Android-delayed stage after that time becomes a bounded, actionable catch-up notification instead of being discarded. A stale or completed project is rejected.
- The app-resume/boot/periodic reconciliation path uses pending-alarm-only cleanup across modes, so a reminder that is currently shown does not have its occurrence token, voice service or alarm service invalidated just because app recovery ran.
- If a Smart target is expired and no catch-up has been successfully delivered, retire the old Smart stage before offering recovery. On subsequent reconciliations, preserve the recovered notification's action token.
- Ordinary expired one-shot reminders also preserve the action token after initial delivery.
- Text-to-speech initialization/playback failures fall back to a regular actionable notification if Android notification permission permits.
- Reminder Center shows whether system notifications are blocked or precise timing unavailable, and lets the user reach Settings.
- V154 New Project / Quick Create and Insights behavior are retained unchanged.

## Automated checks
- Unit tests exercise Android-late Smart stages, recovery window limits, working quiet window, stale target, completed project and on-time delivery.
- Instrumentation tests exercise cross-mode cancellation isolation and preservation of delivered regular/Smart occurrence tokens across app resume.
- CI requires V155 smoke, full unit tests, lint, instrumentation-test compilation, APK assembly, artifact upload, and a targeted Android 35 emulator test run.

## Known limitations
- **No guarantee of exact timing** on heavily restricted/battery-optimized Android devices. The banner describes permissions, not proof a specific alarm reached the system alarm queue.
- Missed Smart reminders are recovered only within the relevance window (through due time, or up to six hours afterward), using an ordinary notification.
- A blocked notification permission still prevents a visible catch-up notification. No synthetic "delivered" record is written until posting succeeds.
- The work does not yet include R2 simplified setup, R3 Reminder Center redesign, R4 quiet hours/personalization, or R5 full physical-device acceptance.
- Existing full-screen alerts, exact-alarm permissions and default notification category depend on platform and OEM behavior.

## Physical-device acceptance
- [ ] Deliver Notification, Voice, Alarm, Smart at target while app foreground/background/screen-off.
- [ ] During active Voice/Alarm, reopen app: current reminder continues; notification actions remain valid.
- [ ] After Smart final target has passed, force late delivery: one catch-up notification, no late ringing chain.
- [ ] Test no notification permission and no exact-alarm permission; show honest fallback and settings access.
- [ ] Reboot, change timezone/time, and install V155 over V154: no lost projects, ideas, schedules, voice settings or duplicate alarms.
- [ ] Smart snooze, Step Done, I'm Working, Pause, Dismiss, Custom next-stage check-in all retain task/publication safety.
- [ ] Two projects with same reminder time; neither silently removes the other's notification/actions.
- [ ] Test at 100%, 130%, 200% font scales and relevant TalkBack actions.

**Release status:** do not merge to stable or claim production reliability before physical-device testing and signoff.
