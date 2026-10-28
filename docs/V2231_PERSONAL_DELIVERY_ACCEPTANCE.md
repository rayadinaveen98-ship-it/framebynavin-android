# V223.1 — Personal Delivery engine and response safety

**Feature branch only. NOT user-release-ready.** The V223.1 engine adds private Personal reminders through a separate AlarmManager PendingIntent identity, independent receipt journal, standalone notification channel and action receiver, and boot/time/upgrade reconciliation. There is **no Personal creation interface yet**.

## Guaranteed isolation
- Identity is in `backlot-personal://reminders/...`, with separate classes `PersonalReminderReceiver` and `PersonalReminderActionReceiver`, `exported=false` in the manifest. No creator `ReminderReceiver`, `ReminderActionReceiver`, `CreatorTask`, Project Pulse, rewards, analytics, Google/YouTube or existing widget integration.
- Each occurrence stores an immutable original due instant, mutable snooze/rescheduled fire instant, a rotating UUID token, and distinct `presentedToken`. Actual user responses are DONE, DISMISSED, SNOOZED, RESCHEDULED, never inferred from silence or notification posting.
- Every action atomically checks stored identity + original due instant + issued token + posting evidence and rejects stale/duplicated actions. Done and Dismiss both advance recurrence but **only Done counts as Done**. Snooze and Reschedule keep the original occurrence and rotate token. New schedules are calculated using V223 date/timezone policy.
- Personal lock-screen notifications hide the title via a generic publicVersion. Separate notification channel; no personal note displayed. Notification permission denied -> no false assertion of delivery. Exact-alarm permission unavailable -> inexact fallback. All PendingIntents are immutable and target non-exported app components.
- On reboot/package upgrade/timezone change or exact permission change, recovery reconciles personal scheduled data after creator recovery. Previously posted occurrence is never repeated. Its open notification remains actionable, with a later explicit user action. Recent unpresented overdue occurrences are scheduled again once; outdated occurrences remain uncompleted.
- Currently **only notification presentation is implemented**. Personal VOICE/ALARM choices intentionally fall back to an explicitly labeled regular notification. Dedicated voice/alarm surfaces and their policy compliance are deferred. Do not use fake CreatorTask to reuse creator voice services.

## Risk still requiring work
- Notification-post success and persisted `presentedToken` are separate writes; on process death in that narrow interval a recovery can repost. The `setOnlyAlertOnce` and fixed tag minimize but don't provide exactly-once guarantees. A future delivery journal should close the gap.
- Long-overdue one-time occurrences do not yet move into a visible Missed history until the Personal UX/history phase. Never claim missed records were completed.
- No personal settings, request-permission flow, Quiet Hours policy, lockscreen review, widget quick actions, actual voice/alarm ring screens or user-facing reminder creation yet.
- OS force-stop, OEM battery optimization and Doze timing cannot guarantee exact delivery. Device testing is essential.
- Existing V221.5/V222/V222.1 stable or visual preview remains unchanged. New APK if compiled is **QA only**.

## Acceptance gates
- Android lint, JVM recurrence tests, Android emulator dedicated V2231 ledger + response tests and earlier V223 store+backup, V222 cold-start and V2215 companion interactions.
- Negative stale token, no notification posted, no creator-project mutation; recurring Dismiss ≠ Done, snooze/reschedule rotates token, backup preserves records.
- Actual alarm fire, notifications on unlocked/locked, permissions blocked, reboot and timezone tests are required before this can be user-visible.
