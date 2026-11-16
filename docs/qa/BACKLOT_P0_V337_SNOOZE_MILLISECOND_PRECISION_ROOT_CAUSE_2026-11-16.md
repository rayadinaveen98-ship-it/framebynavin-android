# Backlot V337 — Snooze redelivery millisecond precision correction
Date: 2026-11-16

## Scope & protected baseline
- Frozen owner-reference branch: `stable/backlot-v337-premiere-owner-testing-20261114` SHA `678502f4626150ea1945b81eed1593a362377454`.
- This is a **separate draft implementation candidate**, not a stable update, signed APK, merge, or shipping approval.
- No owner devices, live cloud data, challenge records, notification permission settings, or backup/restore surfaces are mutated by this PR.

## Evidence and confirmed source defect
- Independent Android 15 CI actual Snooze acceptance from draft PR #387, run 43842141044: naturally fired snoozed OS occurrence appeared, but `NOTIFICATION_POSTED` did not; assertion failed. This run by itself does **not** prove the reason for rejection, because it lacked stage-level diagnosis.
- Frozen V337 source `Phase1HabitChallenge.snoozeToday()` creates `snoozedUntil = at.plusSeconds(600)`, preserving sub-millisecond nanoseconds.
- `Phase1HabitCodec` persists and restores the complete ISO-8601 `Instant` precision.
- `Phase1HabitAlarmScheduler.reconcile()` converts the target to `toEpochMilli()`, and the receiver obtains the same millisecond integer from its PendingIntent.
- Frozen V337 `Phase1HabitOccurrencePolicy.current()` uses strict `Instant` equality of the unrounded persisted time with the millisecond-rounded OS target. Typical sub-millisecond Snooze values therefore fail stale-occurrence validation, even when the OS-fired PendingIntent matches the requested Snooze.
- After receiver records `OS_FIRED`, failed stale-occurrence validation follows the `STALE_IGNORED` branch instead of posting. Correlation with the exact failure log remains pending PR #387 rerun.

## Narrow fix
Only compare the calculated expected occurrence's `toEpochMilli()` to the exact `scheduledAt` PendingIntent millisecond epoch, retaining all other bounds, paused/opt-out gating, and the challenge identity check.
- Do not round or rewrite stored owner Snooze values.
- Do not loosen validation to a time window or match adjacent milliseconds.
- No alarm timing, notification construction, service, DataStore, completion semantics, or existing notification channel is modified.

## Verification and release gates
1. Exact-head full JVM regression, no JVM exclusions, with two new deterministic precision and stale-safety tests; archive XML and source SHA.
2. Review actual PR #387 `STALE_IGNORED`/other stage diagnostics when its rerun finishes.
3. Integrate only after both the independent Snooze unit gate and the corrected Android 15 contract in PR #385 have credible results. Rerun full JVM and real Android 15 10-minute Snooze acceptance **on the combined candidate SHA**, not on the isolated branches alone.
4. Preserve pending full instrumentation suite compilation, locked-screen, voice/music, real-device, OEM idle, and upgrade/rollback checks.
5. Founder release approval before shipping; never move frozen V337/main from green isolated CI.

## Untouched
Existing challenge data and Done receipts; V337 stable branch and main; live cloud; Backup/Restore Settings removal; Advanced > Erase local data; icons, widgets, companions, and app visual stages.
