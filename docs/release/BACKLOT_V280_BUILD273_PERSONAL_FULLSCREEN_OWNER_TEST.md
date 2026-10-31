# Backlot Build273 — Personal ALARM full-screen owner test (DRAFT)
Date: 31 October 2026

VersionCode: 273
VersionName: 2.16.2-rc28-personal-alarm-fullscreen-owner-test
Application ID: com.framebynavin.app

This is a complete Backlot Android app test preview based on tested Build272, with only the targeted RCA #226 changes from PR #227 and version metadata. **It is not a Google Play production release.**

Changes:
- The original Personal ALARM notification now requests a full-screen intent **only with Android permission**. No automatic FSI for Personal VOICE, normal notifications, or follow-ups.
- When the Android lockscreen is present, the Personal ALARM Activity can show a generic animated screen with no title, notes or actionable buttons until the screen is unlocked.
- When permission is absent, the existing ordinary notification with Done/Snooze/Dismiss and tap-to-open remains.
- A user-driven Android14+ Full-screen alert Settings shortcut appears in the Personal ALARM editor if special permission is unavailable.
- Corrects stale Personal VOICE copy from once to three announced repetitions.
- Maintains all existing Creator, Personal, Cloud and backup formats. No database migrations, no new permissions, no Play signing change.

Mandatory QA before owner testing:
- Full Android Gradle assemble `personalStable`, JUnit, lint and Android instrumentation APK.
- Unit tests for FSI eligibility; Android notification builder tests for allowed/denied and VOICE/NOTIFICATION; lockscreen Compose redaction; all 20 previous integration tests.
- Exact Build272 owner APK checksum SHA256 `e4f0dc03730986490ac03b0d4a7daa77a4a8ecd03076ddabdcfdc439b413cdc2` and signer certificate comparison to Build273.
- Android11 phone and Android16 tablet instrumentation expected 24/24 cases each; this is *not* proof that a physical Samsung device will grant FSI.

Owner device checks AFTER green CI:
1. Back up independent Personal and Creator exports before updating, then install as update OVER existing Backlot without uninstall/clear data. Cancel if Android reports mismatched signature.
2. Select Personal ALARM and schedule a disposable test alert in a few minutes. Phone locked and screen off: full-screen generic alarm when permission granted. Unlock: title and Done/Snooze. On unlocked screen Android generally displays heads-up notification.
3. On Android14+ deny Backlot Full-screen alerts in Settings: alarm must fall back to actionable notification; optionally tap the user-driven Editor Settings link to enable.
4. Verify Personal VOICE and NOTIFICATION still do not hijack full-screen. Existing notification tap opens screen. Verify repeated voice announcements and audio.
5. Test old tokens, snooze, follow-up, lockscreen title privacy, foreground app transitions, DND, Samsung-specific notification category/lockscreen settings and exact alarm settings.

Play Store caveat: Backlot is a creator productivity app, not necessarily a qualifying core alarm app for auto-granting `USE_FULL_SCREEN_INTENT`. Explicitly review Play Console policy before release; never misrepresent eligibility or force a permission grant.

Official Android guide: https://source.android.com/docs/core/permissions/fsi-limits
Google Play FSI policy: https://support.google.com/googleplay/android-developer/answer/13392821
