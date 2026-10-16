# Backlot V156 — Reminder Setup R2

**Base:** V155 R1 `4ac1dce10b8a232bfc49085b546fef68d671e64e`. V154 and V155 stable preview branches remain unchanged.

## Product goal
A creator can choose reminder help without understanding system-specific alarm, Smart, foreground service, TTS, or escalation details. Offer advanced settings without sacrificing any existing plan, delivery choice, or save compatibility.

## Implementation
- Shared `V156ReminderPlanPicker` in the seven-step wizard, Quick Create and the existing project editor.
- Five existing persisted plans remain OFF / LIGHT / GUIDED / URGENT / CUSTOM; user sees No reminders, Occasional reminders, Keep me on track, Important deadline and Choose my own time.
- Quick Create now exposes all five options in a compact layout rather than just OFF/GUIDED. It is still a separate opt-in shortcut and does not replace the seven-step creation wizard.
- The custom time picker remains plainly visible wherever CUSTOM is selected; its validation still requires a future time on/before the project deadline.
- The existing delivery preferences (AUTO / NOTIFICATION / VOICE / ALARM / SMART), voice controls and custom importance are grouped under a disclosed **Advanced reminder settings** section.
- Plain-language descriptions warn that AUTO may use voice/alarm when risk rises, and URGENT may ring, so loud delivery is not concealed.
- The disclosure summary shows the saved alert preference even while collapsed. Existing explicit choices are not overwritten with AUTO.
- Reminder engine, task schema, serialization, existing V155 recovery policy, notifications, and project persistence paths are unchanged.

## Test gates
- Source smoke: original seven-step wizard remains default; Quick Create separate; Insights/R1 preservation; shared setup used in all three paths; custom time always accessible.
- Unit: all five plans mapped to unique labels, urgent/guided descriptions disclose voice/alarm, explicit preference remains in summary, validation parity.
- Android emulator: select all five plan types in full/quick picker, reveal advanced settings, repeat V155 recovery token isolation tests.
- CI: unit, lint, instrumentation compilation, APK assembly/upload and targeted Android 35 emulator tests.

## On-phone acceptance checklist
- [ ] New Project opens seven-step wizard and shows five options at the reminder step.
- [ ] Quick Create is separate and offers all five plans in a compact control.
- [ ] Choose my own time lets you pick a time without entering advanced settings.
- [ ] Advanced exposes all AUTO/NOTIFICATION/VOICE/ALARM/SMART styles and voice/importance controls.
- [ ] Edit a project created in V155 with a custom Voice/Alarm setting; ensure its choice remains and is visible.
- [ ] Change from CUSTOM to OFF to GUIDED; confirm effective delivery mode and save persistence.
- [ ] Check due-date changes that make custom time invalid; disabled save and clear validation message.
- [ ] Verify the reminder permission warning and link to Settings are unchanged.
- [ ] Install V156 over V155 without data loss. Test reminder fire/snooze after upgrade.
- [ ] Check 100% / 130% / 200% Android font scale, scrolling, TalkBack and landscape.

**Scope:** R2 only. R3 Reminder Center redesign, R4 quiet hours/delivery polish and R5 full physical-device acceptance remain future phases. Passing emulator tests is not proof of timing reliability across Android OEMs.
