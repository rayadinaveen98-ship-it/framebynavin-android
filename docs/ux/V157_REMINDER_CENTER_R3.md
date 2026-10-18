# Backlot V157 — Reminder Center R3

**Baseline:** V156 R2, commit `401312ebb65af20e846a1b2297e709451d2f5490`. V154–V156 source and APKs remain unchanged.

## Product intent
Make Reminders a creator-friendly command center for the next check-in, not another copy of the project editor. Every quick action should affect only reminder scheduling, never publication or project completion.

## Delivered
- Full-screen Reminder Center organized into **Needs Attention**, **Today (rolling 24h)**, **Snoozed / Paused**, and **Upcoming**.
- Clear alert times and stage context, simple counts, and permissions/precise-timing readiness warning retained from V155.
- Direct **Snooze** (respecting the user's default snooze minutes), **Change Time**, and overflow **Pause 2 hours** / **Edit Project** / **Turn off reminders**.
- A confirmation step before turning off one or multiple reminders. Long-press bulk selection, select-all, delete-selected and New Project are retained.
- Explicit late-or-scheduled Snooze policy: if an alert is in the future, postpone from its scheduled time; if it has passed, postpone from now. Do not silently bring future alerts forward. Clamp snoozes to a future deadline.
- Smart reminders state that Snooze also shifts their final selected Smart target.
- A time picker warns when a selected reminder time is not after now or is beyond a future project deadline.
- All direct writes run through serialized `CreatorViewModel.updateTask`, with `ReminderActionSafety.fingerprint` rechecked against the authoritative task to reject stale cards and old actions.
- Existing `ReminderActionSafety` and `ProjectPulseEngine` methods are reused for Snooze and Pause. Project status, workflow index, progress, completion and publication fields are not edited.
- **No schema changes**: the editor, backups, permission behavior, smart policy and V156 five-plan UI remain intact.

## Limitations / follow-up
- Today means *next 24 hours*, not strict calendar midnight; copy reflects that.
- A paused reminder is clamped to the project deadline if the deadline is sooner; the Pause option is suppressed in that case.
- Unconfigured stage-based reminders link to project edit to pick a first/next reminder time. R4 can supply a dedicated lightweight next-check-in sheet.
- R3 does not claim an individual Android alarm reached the OS; the Settings warning is permission readiness, not an SLA.
- Real-device QA is necessary for OEM background restrictions, stacked concurrent notifications, lock-screen focus, 100–200% font scale, accessibility and upgrade from V156. This is a development-signed preview, not a Play Store release.

## CI acceptance
- V154–V157 source smoke, JUnit, lint, instrumentation compile, APK assemble/upload, targeted emulator UI tests and V155 recovery tests.
- Unit policy verifies grouping has no duplicates, stale fingerprints are rejected, Snooze delays from scheduled time, and no project completion/publication mutation.
- Existing V132 UI tests still verify card tap opens the editor, long-press selects, batch confirmation and Add action.

## On-phone acceptance
- [ ] Open Reminders; find Today / Needs Attention / Snoozed-Paused / Upcoming, in the right categories.
- [ ] Snooze an alert scheduled tomorrow: it must move *later than tomorrow*, not to now + 10 minutes.
- [ ] Snooze a late alert: it must move forward from now.
- [ ] Tap Change Time; pick a time outside project deadline; receive clear warning; pick a valid time and confirm it persists.
- [ ] Pause a stage check-in for 2 hours with enough time before deadline.
- [ ] Turn off one reminder and several via long-press; project data and progress stay unchanged.
- [ ] Tap card title to edit project; use overflow to edit project; select/deselect with long press.
- [ ] Confirm Smart timing remains explicit; no sudden late ringing chain after Snooze/Change Time.
- [ ] Check notification permission and exact timing warnings with permissions denied.
- [ ] Upgrade from V156 without losing projects, ideas, reminders, backups and settings.
