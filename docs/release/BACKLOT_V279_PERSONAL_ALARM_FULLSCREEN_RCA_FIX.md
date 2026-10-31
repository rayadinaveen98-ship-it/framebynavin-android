# Backlot Build272 → V279: Personal ALARM fullscreen root-cause fix

**Status: draft / Android QA required / physical handset acceptance required.**
Based on immutable Build272 owner app, preserving `versionCode=272` in the feature branch. This does not alter the previously distributed owner APK, app data, cloud, private signing, account deletion or Play publishing.

## RCA
In Build272, `PersonalReminderNotifications.show` created a HIGH-priority `CATEGORY_REMINDER` notification with **only** `setContentIntent(PersonalAlertActivity)`. The Activity deliberately lacked show-when-locked/turn-screen-on, so users had to tap their notification to see the particle-wave Personal screen. Creator Alarm and Voice instead used a permission-checked `V268FullScreenReminderAccess`.

## Implemented behavior

- A new small pure policy **only permits full-screen intent when `PersonalAlert.ALARM`, the original occurrence (not follow-up), and platform FSI access is granted**. User chooses ALARM in Personal editor; this is not a default for ordinary notifications or VOICE.
- On Android14+ Personal editor, a clear Full-screen alert settings shortcut is offered if Android reports special access denied. The app does not self-grant permission, and explicitly explains the unlocked-phone heads-up behavior.
- Personal ALARM sets `CATEGORY_ALARM` and uses the existing `V268FullScreenReminderAccess.allowed()/configure()`; Android14+ `canUseFullScreenIntent()` denial retains a high-priority notification with a tap-target and Done/Snooze/Dismiss. There is **no direct background Activity launch**, no permission request in the background, and no new permission declaration.
- Personal Alert activity requests Android show-when-locked and turn-screen-on **only for a Personal ALARM PendingIntent**. While keyguard is showing, it displays generic Personal Alarm animation and an unlock prompt, never the private title/notes and never actionable Done/Snooze/Dismiss. Activity monitors resumed/focus changes so unlocked access restores private details and response controls; answer path also checks the keyguard.
- All notification types retain a PRIVATE lock-screen notification with generic publicVersion; the old occurrence token and exact actionable Personal pending intent remain unchanged.
- Corrects misleading `Spoken once` copy to reflect current three voice announcements. Personal VOICE does **not** gain automatic full-screen behavior pending Google Play policy review.
- Synthetic JVM tests cover eligibility vs voice/notification/follow-up/permission denied. Android tests inspect actual built `Notification.fullScreenIntent`, fallback intent and category and that locked Compose UI never renders synthetic private titles/actions. Device tests include existing Personal delivery lease safety.
- The existing Backlot owner Build272 APK remains intact; no user records modified.

## Android / Play limitations

Android14+ user must grant special full-screen access for apps not auto-eligible. Even with granted access, an **unlocked phone normally shows a heads-up notification, not a fullscreen Activity**; locked/off display can launch FSI. A high importance channel must be enabled, and an OEM can impose restrictions. See https://source.android.com/docs/core/permissions/fsi-limits and https://support.google.com/googleplay/android-developer/answer/13392821

Google Play permits automatic FSI grant only for qualifying core alarm or calling apps. Backlot is a broader creator organizer. **Do not claim automatic entitlement, do not publish to Play without truthful permission declaration and consent/denial fallback.**

## Release acceptance
1. Full Android build/lint/JUnit and Android11/16 synthetic instrumentation pass in draft PR.
2. Owner device: backup Creator & Personal separately, install future higher-version prototype-signed test APK OVER Build272, **never uninstall or clear data**.
3. Select Personal ALARM, schedule for a few minutes later. Test phone screen off and locked with Full screen alerts allowed; expect generic animated Personal Alarm view, after unlocking reveal private title and Done/Snooze. Confirm notification remains if access is denied.
4. Screen unlocked should show a heads-up alert; do not promise automatic fullscreen when unlocked. Test Personal VOICE/Notification do NOT automatically take over screen, but clicking notification opens screen.
5. Verify old/stale token cannot control new event; test snooze, follow-up, reboots, Samsung battery restrictions, FSI and exact-alarm special access, DND.
6. Check Play Console restricted permissions before approving production. No release/merge until successful owner acceptance.

## Automatic-launch receipt race safeguard
The receiver posts the Personal notification **before** committing `presentedToken`. An immediate system-launched FSI can open before that write completes. Build272's tap-only screen previously rejected such a record as stale. The new `PersonalAlertPresentationPolicy` explicitly waits up to 10 short checks (150ms apart) for the **same pending occurrence token** to become presented; any rotated, deleted or paused record is rejected immediately. This never acknowledges a reminder automatically, changes user response, or bypasses the original posted-token requirement. Three pure JVM tests cover waiting, successful receipt, stale tokens and timeout bounds.
