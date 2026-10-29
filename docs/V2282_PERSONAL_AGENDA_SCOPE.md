# Backlot V228.2 — Personal Agenda v1

Stacked on V228.1 for safe full-app upgrades, not merged to main.

## Scope
- Add read-only Personal Agenda categories: Needs attention, Today, Tomorrow, Upcoming, No upcoming occurrence, Paused.
- "Needs attention" only uses actual unacknowledged posted tokens, recorded missed occurrence, or due-time from an unposted pending lease; it does **not** mark reminders delivered, missed or complete merely because time passed.
- Time comparisons use the viewer's device timezone. Persisted reminder recurrence remains in the user's recorded timezone and is never altered by rendering.
- Stable order by due time/title/id; single category per record; all existing CRUD/snooze/done callbacks unchanged.
- Re-evaluate local date categorization every 60 seconds while screen is composed, and on Activity resume, without scheduling new alarms or background jobs.

## Not included
Delivery policies, reminder escalation, speech playback, notification sounds, data model/codec, backup, permissions, reboot recovery. Those require their own device QA and follow-up slices.

## Acceptance gates
Run all existing Personal foundations plus new V2282PersonalAgendaPolicyTest, compile lint/build same package, Android 11/15 UI smoke, verify no missing records, no stale "missed" claims, no data or CreatorTask writes. Keep draft until evidence passes.
