# Backlot RC12 — Build268 Android full-screen-permission fallback

**Date:** 2026-10-30. **Type:** Focused Android permission/resilience fix with fresh device QA, **not Play compliance approval**.

## Why this change

In Build267, both `AlarmRingingService.buildNotification` and `VoiceReminderService.buildNotification` unconditionally attached `setFullScreenIntent(fullScreen, true)`. Android 14+ treats this as user-controlled special app access and Google Play only automatically grants it to qualifying apps whose core purpose is alarms/calls. Backlot's creator organizer product cannot assume auto-grant.

- [Official Android NotificationManager.canUseFullScreenIntent reference](https://developer.android.com/reference/android/app/NotificationManager#canUseFullScreenIntent())
- [Play foreground service and full-screen intent disclosure requirements](https://support.google.com/googleplay/android-developer/answer/13392821)
- [Android high-priority notification fallback documentation](https://developer.android.com/reference/android/app/Notification.Builder#setFullScreenIntent(android.app.PendingIntent,%20boolean))

## Exact changed behavior

- `V268FullScreenReminderAccess.allowed(context)` returns **true for Android ≤13**, and on Android14+ returns `NotificationManager.canUseFullScreenIntent()`, failing **closed** to no full-screen launch on query errors.
- `V268FullScreenReminderAccess.configure(builder, surface, allowed)` attaches the full-screen intent **only** when access is available.
- Both Creator Alarm and Voice notifications **always** retain `.setContentIntent(surface)`, `CATEGORY_ALARM`, `PRIORITY_MAX`, `FOREGROUND_SERVICE_IMMEDIATE`, `setOngoing(true)`, unchanged action target and distinct channel IDs. If FSI is denied, the user can still open the actionable notification. Actual heads-up display remains subject to user notification permission, channel settings, Android policies and device behavior.
- No background activity starts itself from the helper. No change to Personal reminders, audio loops, scheduling or notification channels.
- Version code **268**, version name `2.15.5-rc12-permission-fallback-preview`. Same package ID `com.framebynavin.app`, same public preview signer for device testing only. **No production signer** is provided. Build267 remains separately QA-green and available as rollback *source/artifact*, but downgrading an installed Android package normally requires special handling and could risk data: do **not** advise uninstall/downgrade to users.

## Automated gates

1. Full Gradle JVM tests, lint, debug/test APK and full owner signed-preview APK; verify **exact stable full Build267 SHA-256** `02827066650f7f725ef712c7a2119f20746fec5198fd1b7cfdc0be709e8d0d7d`, old→new signing-certificate match, Build267→268 package identity and checksums.
2. Android 11, 15 and 16 emulator tests for denied fullscreen policy, preserved notification content/tap target, granted full-screen path, OS capability check, frozen Home slideshow, companion, Voice/Alarm cinematic intro, Personal reminders and private Personal backup.
3. On all three Android versions, **real** Build267→268 debug-signer in-place upgrade; seed Creator and Personal data in Build267, install Build268 over it **without uninstall or clear**, verify surviving data and safe cold launch.
4. All CI APKs and personal data fixtures are disposable; no real accounts, private signing key, owner phone data or Play Console changes.

A green gate means **engineering preview validated**, not production ready. Requires owner device acceptance (lock-screen alerts with FSI permitted/denied; notifications enabled/disabled, exact alarm permission; doze and manufacturer battery restrictions) and Google Play FSI/FGS policy eligibility declaration. Existing unresolved blockers: [#174](https://github.com/rayadinaveen98-ship-it/framebynavin-android/issues/174), [#180](https://github.com/rayadinaveen98-ship-it/framebynavin-android/issues/180), [#185](https://github.com/rayadinaveen98-ship-it/framebynavin-android/issues/185), [#187](https://github.com/rayadinaveen98-ship-it/framebynavin-android/issues/187).

**Do not merge into unprotected main or publish via Play before owner and administrator approval.**
