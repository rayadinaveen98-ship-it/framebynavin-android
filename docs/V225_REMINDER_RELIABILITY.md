# Backlot V225 — Creator late-audio safety and independent reminder recovery

## Audit findings
1. Regular Creator AlarmManager deliveries trusted the stored `EXTRA_EXACT_DELIVERY` boolean as sufficient to start a Voice or Alarm service even if the broadcast arrived hours or days after its original target. The OS may defer delivery in Doze or during device downtime: exact scheduling is not proof of on-time dispatch.
2. The hourly ReminderHealthWorker reconciled only CreatorTask reminders. Personal reminders had startup/boot and a best-effort 24-hour watchdog, but no equivalent WorkManager health pass. Idea reminders were also absent from hourly repair.
3. The boot/timezone/package recovery receiver ran Creator, Idea and Personal recovery sequentially without failure isolation around Creator or Ideas. A thrown exception in an earlier lane prevented Personal recovery.

## V225 changes (complete app preview; no data migrations)
- **Bounded audible Creator dispatch:** For VOICE and ALARM, start audible services only if an exact broadcast arrives within two minutes of its *original* scheduled occurrence (allow up to five seconds early to tolerate clock drift). Inexact delivery, late delivery, and failed audio service attempts use the existing notification fallback instead.
- **Suppress truly stale Creator alerts:** Do not revive a long-expired reminder after the project due-time grace period. If its deadline remains ahead, still provide a recovered *notification*, never surprise audio. Existing MissedReminderRecovery relevance policy is reused.
- **Three independent recovery lanes:** Hourly WorkManager health now attempts Creator, Personal, and Idea reconciliation and retries if any lane fails. Each failure is isolated so the other lanes can still run; coroutine cancellation propagates.
- **Reboot/timezone/exact-permission changes:** The boot receiver also uses the shared lane isolation policy, protects periodic WorkManager registration, and completes its Android goAsync broadcast within a nine-second bounded coroutine.
- **No user action mutation:** None of these recovery paths synthesize Done, Dismiss, Snooze, XP, or Personal acknowledgement. Existing CreatorTask, Ideas and PersonalReminder stores, launcher widgets, and UI remain intact.
- **Version 247** prototype signed, same package com.framebynavin.app and signing identity as V224.1 version 246 for in-place sideload. Not Play Store signed.

## Validation
- JVM unit boundaries: on-time/120s+/inexact/clock early Creator audio gating; stale past-due vs relevant future deadline; independent creator/personal/idea retry flags and cancellation propagation.
- Prior full JVM, lint, debug/instrumentation/personalStable builds and signing check.
- Android 11 emulator: V224.1 widget registry 4, V2234 Personal missed recovery 2, V2233 alert UI 2, V2232 Personal Hub 2, Personal notification 1, V2231 action journal 3, Personal storage 3, V222 cold projects 1, Creator V155 recovery isolation 4. Tests run independently and logs captured.
- Physical-phone Android 11–16 Doze, reboot, exact-alarm permission revocation, notification permission, battery optimization, voice service and lockscreen checks remain mandatory. This engineering PR stays draft until device QA.

## Device review sequence
- Schedule Creator Voice and Alarm one to two minutes ahead and verify appropriate delivery while screen on/off (respecting OS permissions).
- Simulate inexact alarm permission limitations using Android Settings and confirm notification fallback instead of false sound claims.
- Check an old overdue reminder after re-opening the phone; it must not unexpectedly start hours-old speech/alarm. Do not deliberately wait through dangerous real-world reminders for this test.
- Schedule Personal daily reminder, reboot, and check next occurrence and distinct missed vs Done state.
- Check My Day widget counts remain privacy-safe and project/idea data is preserved after in-place upgrade.
