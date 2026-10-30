# Backlot V206 — Legacy icon purge and status-bar brand integrity

Preview 226 / `2.7.6-v206-brand-icon-purge-preview`.
Based on V205's draft branch. Do not promote without exact-SHA CI + real-phone review.

## Root-cause investigation
A Samsung home-screen screenshot shows a small **colored, dark-background gold/crimson frame** just to the right of a YouTube indicator. It resembles Backlot's retired cinematic logo. The screenshot does **not** show the expanded notification shade, channel, notification owner or creation time, so it cannot establish whether this was a currently posted Backlot notification, a notification posted by an older APK, or another system surface.

Verified V205 repository findings:
- A retired `ic_framebynavin_launcher.xml` gold/crimson/black vector still existed in app resources, alongside old `ic_framebynavin_splash.xml`, four unused `backlot_icon_*.xml` variants and `backlot_splash_mark.xml`.
- Primary app icon, round icon and four launcher aliases already target `@mipmap/backlot_v198_launcher`. Its adaptive foreground is `backlot_v198_approved_icon`, with the approved white `backlot_v198_notification_monochrome` as monochrome.
- Confirmed all known notification builders for regular, alarm, voice, idea, automation, next-stage, optional-context and follow-up surfaces select `R.drawable.backlot_v198_notification_monochrome`. The incoming screenshot alone **does not prove an active source-code notification defect**.
- There are foreground services for voice and alarms. Their `NotificationCompat.Builder` paths also use approved white B, not the old colored launcher.

## V206 changes
- Delete seven obsolete source drawable XML assets so they are not packaged or accidentally selected by later notification/UI paths.
- Normalize stale icon-option descriptions but retain old enum names/alias component names and preference keys **for update compatibility**.
- Strengthen CI: check every source notification builder uses canonical small icon; prohibit historical drawable assets/references; parse manifest to check app, round and all alias identities; verify adaptive foreground/monochrome; check white paths only in status icon.
- Preserve app identity `com.framebynavin.app`, user data, launch component aliases, WorkManager contracts, alarm and voice notifications, exact reminder schedule, existing notification IDs/channels and signing.

## Physical-device acceptance
1. Install V206 *over V205* (do not uninstall or clear app data).
2. On Samsung, pull down the shade. **Before clearing anything**, identify the publisher/channel and note its title and time for the colored square shown in the top bar.
3. Note whether the icon belongs to an alert posted **before** upgrading. A pre-update Android notification can preserve its original icon while still present in the shade.
4. Dismiss only an obsolete optional nudge when safe; do **not** blanket-clear the important reminder/voice/alarm channels. Trigger one fresh scheduled ordinary reminder, one alarm, one voice and one optional creator nudge (with opt-in) and inspect notification small icons and actions.
5. Reboot once and validate newly delivered icons, home launcher, app drawer, Settings, widgets, widget add chooser, recent-apps icon and shortcuts.
6. Confirm projects/reminders survive upgrade, notifications reach the same destination, 3-second welcome is unchanged, disabled/quiet hours unchanged.
7. If screenshot icon remains, request expanded notification shade with app name/channel and timestamp to narrow whether it is an Android-cached notification or a separate icon owner.

## Required CI gates
`python3 tools/check_v206_brand_assets.py`; inherited V205 QA, JVM unit, lint, APK packaging/signature checks and Android 11 emulator regressions. Keep PR draft until green CI and creator's real-device approval.
