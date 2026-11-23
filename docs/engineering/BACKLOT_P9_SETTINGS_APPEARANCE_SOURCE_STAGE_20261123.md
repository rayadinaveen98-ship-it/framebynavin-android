# Backlot Point 9 — Settings & Appearance Cleanup (2026-11-23)

**Founder approval:** "yeah bro approved go ahead" after reviewing and approving P8B Living Voice Orb. The earlier founder request explicitly rejects the old Cinematic/Classic collections in the reminder settings and wants the new approved background direction instead.

**Stage:** SOURCE STAGED CANDIDATE / UNCOMPILED / UNTESTED / NO APK / NO MERGE / NOT DEVICE ACCEPTED.

## Source-of-truth
- repo: rayadinaveen98-ship-it/framebynavin-android
- Cumulative branch: feature/backlot-v337-point09-settings-appearance-source-after-p8b-20261123
- P8B base: 9836d160e997701ad477b62b08e1badc43d7f7cb
- Stable owner-accepted V337: 815ba69775f75b6e5f25bb838c2c17b4f09c203e unchanged.
- Main unchanged. No JVM, Gradle, Compose, emulator, physical device, signing, CI, merge or release.

## Source audit and decision
The active Settings surface is V144SettingsHub (FrameByNavinV101BApp overlay SETTINGS). It presented three overlapping visual controls:
1. V127 app theme picker (two supported modes: Director's Cut + Backlot Light) and app icon/launch sound.
2. V391ReminderSceneSettings, seven retired voice/alarm background selections plus legacy preferences.
3. V394PremiereSettingsSection (nested in V391), experimental complete full-screen alternative with legacy per-kind toggles.
P1GalleryBackgroundLaunchCard already existed in V127 Appearance and stores independent Personal, Creator, 21-day selections; it is the replacement path, not a request to build a fourth parallel renderer.

## P9 changes staged
1. V144SettingsHub Personalization now has a clear hierarchy: App appearance, direct Reminder backgrounds Gallery entry, Companion, Voice & music, Home photos. No "Reminder scenes" card leading to the old picker.
2. V144SettingsHub APPEARANCE no longer mounts V391ReminderSceneSettings; so V394 Premiere controls are not reachable through Settings.
3. V127AppearanceSettings shows only two supported app looks, original icon picker and launch sound; remove duplicate Gallery launcher from its app-theme page. Designer copy differentiates app theme from reminder imagery.
4. P1GalleryBackgroundLaunchCard remains a single direct action on Appearance & sound, offering scope-isolated Personal/Creator/21-day local photos/animated images/silent short videos, preview/crop/reset under the existing P1 data store.
5. ReminderCheckInScreen: retired V394 Premiere conditional early return and V391 scene renderer. It retains the original V337 creator intro/reveal, P1 gallery extension and functional Creator callbacks; one V336 fallback until Point 10's replacement visuals.
6. PersonalAlertActivity: retired V394 alternative screen and V391 scene renderer. It retains original unlock privacy, actionable Done/Snooze/Dismiss and P1 Personal gallery entrance host. V336 fallback until P10.
7. Old persisted v391/v394 preference keys and legacy Kotlin compatibility implementations are NOT erased, migrated, or modified. They are dormant and no longer consulted by the live routes above. Preserve recovery/backup semantics; a later separately gated code-deletion audit can remove unreachable classes after compilation and references are verified.
8. All V336 fallback, P1 media engine, V195 particle entrance, full-screen permissions, music, voice, reminder scheduling, notification actions and Personal data remain intact. P10 is reserved for new immersive built-in reminder visuals, not silently implemented in P9.
9. P9AppearanceScopePolicyTest.kt added as deferred unexecuted source tests.

## Honest scope boundary
The historical "Director" fallback is intentionally retained until the approved P10 visual replacements can be installed and device-tested. Do not claim the original alert backdrop is gone now. The old selectable Cinematic/Classic/Premiere settings and old selections have been deactivated in active screens, NOT deleted from source or from user preference storage. No existing photo/voice data was deleted.

## Deferred consolidated QA acceptance gate
1. Compile all P1-P9 source; verify V394/V391 references have no active screen call sites, and UI has no old collection labels.
2. Fresh and returning local/Google users: Settings > Appearance & sound > App appearance, Gallery, Companion, Voice and Home photos all open correctly and back navigates.
3. Older saved V391 voice/alarm and V394 enabled preferences: no old UI overrides or scene selection on Creator/Personal real alerts. Do not clear stored preferences or user media.
4. Real Creator reminder (voice/alarm): original V195 reveal and all controls, Snooze/Done, notifications, volume and retry remain functional.
5. Real Personal reminder (voice/alarm): locked phone reveals no private data; unlock and response works; P1 media only after valid alert/consent conditions.
6. P1 backgrounds: Personal/Creator/21-day independent local scope selection, image/GIF/HEIF/short silent video/crop/reset, privacy and media decode fallback; no cloud upload.
7. P8B voice orb, P7 Personal Home, P6a onboarding and Idea Vault unchanged.
8. 320dp-420dp, font scaling 1.5x, TalkBack, dark/light, tablet and accessibility contrast.
9. When founder says all points are staged and approves next steps/APK, run targeted compilation and test suites, then consolidated device certification; never per-point runs.

**Point 10 — immersive reminder visual replacements: NOT STARTED.**
