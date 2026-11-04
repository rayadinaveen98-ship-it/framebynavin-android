# Backlot V286 — Personal Reminder Reliability Center

**Development / QA owner preview only.** This is built on the unmerged V285
planner and the passing V284 owner-test baseline.

## User-facing behavior

A compact **Reminder Delivery Health** panel now appears in Personal
Reminders, below the 21-day plan preview. It reads the current Android device's
settings. The panel NEVER claims a reminder has appeared just because its
settings look correct.

Signals include:
- Notification runtime permission, notification global setting and channel
  enablement. Android can deny these separately.
- Special exact alarm availability. If denied, the existing scheduler falls
  back to Android inexact alarms and a reminder may arrive later.
- Full-screen permission ONLY when at least one explicit Personal ALARM is
  active. Voice and ordinary notifications never acquire new full-screen
  eligibility.
- Battery saver status and ordinary battery-management advisory. A device may
  still defer alarms with permissions granted.

All statuses distinguish confirmed **blocked**, **allowed**, and **unknown**.
Unknown is never mislabeled "denied"; allowed never means "delivered".

## Actions

- Open Android **app notification settings** on an explicit tap.
- Open **Alarms & reminders** special access on an explicit tap, if missing.
- Open **full-screen intent** settings only when there is an active ALARM and the
  Android 14+ special access is unavailable.
- **Recheck Reminder Scheduling** manually invokes the existing Personal
  reconciliation, with unchanged record IDs, due dates, snoozes, and user
  completion history. Reconciliation doesn't prove successful OS delivery.

All settings-launch failures fall back to Android app details instead of
crashing. No request for a battery optimization exception is made automatically.

## Automated checks

- JVM policy tests: notification-denial blocker, inexact fallback, alarm-only
  FSI warning, battery-saver advisory, unknown access, no-guarantee summary.
- Android 15 UI: read-only empty state and manual recheck controls for ALARM.
- V285 dated planner + 21-day regression tests remain required.
- V284 diet/fitness reminder tests and existing private notification checks
  remain required.
- Owner preview APK version 286 and SHA-256/signature identity checks.

## Owner acceptance still required

1. Install owner preview in place over correctly signed Backlot without
   uninstalling. Take a backup first.
2. Disable notifications and verify blocked status. Restore them and return
   to Backlot; status should refresh on resume.
3. Toggle Alarms & reminders special access and verify "may arrive later"
   when exact alarm access is denied.
4. With a Personal ALARM enabled, toggle Android 14+ full-screen permission;
   verify fallback to heads-up/tappable notification on denial.
5. Enable battery saver; verify non-false status and test lock-screen delivery.
6. Tap RECHECK; verify reminders remain unchanged, old Done/Snooze tokens are
   not replayed, and no duplicate notifications are posted.
7. Repeat after reboot and across a second device/cloud restore. Do not claim
   cloud-sync success without actual testing.

## Next phases

- Nap-end wake option with adjustable 60/90/120-minute duration, owner choice
  between normal notification and a genuine alarm.
- A single day timeline with next-action cards and one-tap explicit Done/Snooze;
  completion history remains the source of truth.
- User-controlled quiet modes and noncritical reminder batching; Android
  recommends inexact alarms for ordinary non-time-critical nudges.
- Move personally tailored plan content into a private local profile instead of
  embedding it indefinitely in the public GitHub source.

## Platform references

- https://developer.android.com/develop/background-work/services/alarms
- https://source.android.com/docs/core/permissions/fsi-limits
- https://support.google.com/googleplay/android-developer/answer/13392821

**Never promote this preview as Play-ready** without signing, policy, physical
device, accessibility, user data, permission and background reliability review.
