# Backlot V180 — Reminder Redesign Phase 4: Creator Controls & UX

**Base:** V179 quiet no-response follow-ups (V178 voice/alarm lifecycle and V177 tracking).
**State:** Draft preview only. No stable/main merge or TaskStore migration.

## What was added

1. **Per-project timing controls** for voice repeat attempts (1–3), pause between attempts (5–60 seconds) and ringing auto-stop (30–300 seconds) within New Project > Reminders > Advanced, Quick Create > reminder choices > Advanced, and Edit Project > Advanced. These directly update the existing persisted `PProjectDraft` fields, which were previously invisible.
2. **Global unanswered follow-ups toggle** inside Settings > Reminders, clearly stating the maximum of two quiet follow-ups. Switch Off cancels queued follow-up jobs immediately. Scheduled workers read the persisted setting before and during execution; On allows subsequent reminder occurrences to use the V179 bounded policy.
3. **Backups and restores:** The toggle is stored in `CreatorOsSettings` and exported/imported with existing settings. Old backups without this field default to enabled to preserve V179's previous behavior. No project schema change is needed.
4. **Clearer explanations** for when playback ends, which settings belong to the project, and that Android background delivery may run late.
5. **Existing Settings defaults remain**: Snooze 5–30 minutes and default ringing timeout 30–300 seconds. Per-project edits override the default for that project only, leaving unrelated projects unchanged.

## Automated acceptance
- Inherited V161–V179 source contracts and V180 source checks.
- JVM timing policy tests; Android UI-test compilation of selectable timing chips.
- Android lint and APK build; artifact integrity and checksum.
- Android 11 emulator runtime for timing picker, and V179/V178/V177 response/token tests, if the emulator job passes.

## Physical-device acceptance (required before stable)
- Full 7-step and Quick Create paths: select voice repeat 1, 2, 3; gaps including 5 and 60 seconds; alarm 30, 45 and 300 seconds; create and reopen project, verify each value persisted.
- Existing-project editor: change ONLY one timing value and verify overdue due dates, progress, stage, voice and snooze state remain correct per accepted V171 safety logic.
- Settings: change global Snooze preference (5–30 min), confirm next Snooze action uses new value; default alarm auto-stop applies to future projects only.
- Settings follow-up switch: turn off with a queued worker and verify no follow-up arrives. Turn on and verify a new unanswered occurrence gets no more than two discreet follow-ups; verify old backup import behavior.
- Verify voice ends after last sentence, alarm stops at selected deadline, notification remains actionable, and Smart escalation is not interrupted.
- Android 11, 13+, 14+, OEM background limits, disabled notifications, quiet hours, battery optimization, reboot, rapid response at timeout, and permission loss.
- Verify compatible signing and app data backup before in-place upgrade. Do not uninstall the user's main installation.

## Known limits
- Follow-ups are WorkManager-delayed check-ins, **not exact-time alarms**; Android may defer them.
- The setting is global. Per-project follow-up frequency/opt-out can be considered in a future UX phase, but should not be added without a durable preference design.
- The new timing controls do not yet add audio simulation of a full reminder in the editor; voice persona preview already exists.
- Pending unanswered reminders central dashboard/response history belongs to Phase 5.
- Passing CI does not constitute physical-phone alarm or TTS playback verification.
