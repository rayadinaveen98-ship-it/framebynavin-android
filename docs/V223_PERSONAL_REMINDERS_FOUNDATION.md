# Backlot V223 — Personal Reminders, foundation only

**Status:** Storage/model/recurrence are implemented on a draft feature branch. **No Personal UI, alarms, voice announcements, delivery receivers, notification actions, backup integration or widgets are enabled by this change.**

## Product contract
A Personal reminder is never a creator project. It has its own durable ID, type, timezone, recurrence and alert preference. It is local-first and needs no account or network. Creator Home, Project Pulse, creator insights, YouTube analytics, Google Calendar sync and creator cloud backup cannot see these records. No new permissions.

## Implemented components
- `personal/PersonalReminder.kt` — immutable independent entity, explicit PERSONAL domain identity, scheduling fields, checked invariants, per-occurrence acknowledgement watermark and optional user snooze field.
- `personal/PersonalRecurrence.kt` — deterministic *strictly-after* next scheduled instant using stored wall-clock local date and timezone: one-time, daily, weekdays, selected days, month-end clipping, custom every N days. Explicit 2026 DST gap rule is Java Time's default next-valid-local-time resolution. It does not schedule alarms yet; that is deliberate.
- `personal/PersonalReminderCodec.kt` — versioned JSON envelope, PERSONAL-only source verification, SHA-256 integrity, duplicate-ID and maximum-size checks; cannot import CreatorTask.
- `personal/PersonalReminderStore.kt` — separate Android Preferences DataStore `backlot_personal_reminders_v1`, one mutation mutex, atomic saves, read-only validated last-good revision, explicit backup preview/replacement API. Corruption blocks writes rather than silently emptying user records. It does not change any creator store or backup file.
- JVM tests verify all recurrence classes, leap-year Feb 29, DST and stored timezone; Android instrumentation verifies actual DataStore persistence, edit/reopen, backup import, last-good revision, checksum rejection and creator-task isolation.

## Next engineering phases (not yet implemented)
1. **Occurrence journal & scheduling adapter:** distinct `personal:` PendingIntent identities and notification IDs; stable generation tokens, no CreatorTask or Project Pulse writes. Never reschedule/claim a stale alarm, and preserve exact alarm fallbacks.
2. **Personal action receiver:** separate Done / Snooze / Reschedule / Dismiss semantics and distinct per-occurrence audit state. No response must never be treated as Done. Recurring Done advances the single occurrence, not the series. Backed by durable rollback-aware write transactions.
3. **Recovery:** boot, package upgrade, time/timezone change, Android exact-alarm permission change, duplicate/deferred notifications and WorkManager reconciliation; validate lockscreen and inexact fallback.
4. **UI:** Creator | Personal toggle in Reminders Hub, minimal What / When / How form, recurrence editor, history, privacy settings and accessibility.
5. **Backup:** opt-in PERSONAL export/import separate from creator cloud backup, atomic restore and optional explicit future sync. No personal title/notes on lockscreen by default.
6. **Widgets:** replacement widgets only after personal router/action receiver are production reliable.
7. **Device matrix and Play policy** before public release.

## Release gates
- Existing personalStable **V221.5 240**, **V222 241**, **V222.1 242** remain unchanged.
- Foundation doesn't register receivers, notifications, broadcast intents or new alarms.
- No upcoming user-facing personal-reminders feature should be claimed until end-to-end device tests succeed.
- This PR builds with JVM tests, Android lint and on-device DataStore isolation tests. Do not merge into the stable branch or publish yet.
