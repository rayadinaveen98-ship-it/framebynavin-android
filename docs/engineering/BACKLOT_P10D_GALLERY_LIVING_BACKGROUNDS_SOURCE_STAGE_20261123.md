# Backlot P10D — Living Reminder Backgrounds + Private Gallery Continuity

**Date:** 2026-11-23  
**Founder authorization:** "ok bro contine" after P10C source-staging. Advance the previously agreed P10D gallery/image/live-photo/video presentation and three-family Settings integration.  
**Status: SOURCE-STAGED / NOT COMPILED / NO TESTS RUN / NO DEVICE QA / NO APK / NO MERGE / NOT RELEASED.**

## Source authority
- Repository: `rayadinaveen98-ship-it/framebynavin-android`.
- Current cumulative branch: `feature/backlot-v337-point10d-gallery-visuals-after-p10c-20261123`.
- Parent: `feature/backlot-v337-point10c-live-screen-source-after-p10b-20261123`, includes P1–P10C source candidates.
- Accepted V337 baseline: `815ba69775f75b6e5f25bb838c2c17b4f09c203e`, unchanged. Production `main` unchanged.
- No additional paid service, network media provider, cloud upload, media scanner, alarm service, notification listener, audio recorder, permission or navigation feature.

## What P10D source now does

### 1. Three built-ins and My Gallery in existing Settings
- `ui/P1GalleryBackgroundSettings.kt`: keeps Creator / Personal / 21-Day tabs (Creator initially shown); keeps Voice / Alarm preview mode, full-screen preview, gallery photo picker, zoom and X/Y crop controls, local media guidance and privacy.
- Renders **Living Bloom**, **Pulse Studio**, **Rise Core** as the only three built-in family choices with static miniature previews; does not resurrect Cinematic/Classic/Premiere, Aperture, Personal Sunrise, Projector Light or the old Backlot Original selector.
- One **My Gallery** card offers private photo/animated image/silent video import, replacement, saved selection, crop controls and **explicit, confirmed removal of the app-private saved media**. It does not delete photos from the user's phone gallery.
- Switching to a built-in family retains the imported media; switching back to My Gallery selects the previously imported item without a new import.
- Avoids running three animated thumbnails: mini family renderers are deliberately static.
- When full-screen preview opens, the inline preview is unmounted so two gallery video players are not running underneath each other.
- Preview artwork uses `P10ReminderSceneBackground` and `P10ReminderIdentityEmblem` instead of old V336 signal art. Preview-only label remains truthful.
- The 21-Day note states the actual capability: the visual appears on a valid **user-opened challenge notification**, not a newly invented automatic full-screen challenge alarm.

### 2. New non-destructive selection policy and migration compatibility
- `P10ReminderVisualContract.kt` adds `P10ReminderBackgroundSource`, `P10ReminderBackgroundPolicy`, `P10ReminderBackgroundStore` with independent per-domain preferences.
- Family selection is stored in the existing P10 family preference file; source selection is stored separately. Gallery media and framing continue in the original P1 private media store.
- On upgrade, an existing valid P1 media file with no saved P10 background-source preference defaults to **My Gallery**. A prior explicit built-in choice remains built-in even when private media is present.
- If gallery source is saved but the file is missing/broken, resolve to the selected family rather than blank or a retired old scene.
- All private gallery media is hidden when locked; source policy is presentation-only, not a new permission gate.
- New P10D background-source unit cases were **written but not run**.

### 3. One visual compositor between entrance, main, and Settings preview
- New `P10ReminderSceneBackground.kt` composes selected living family or P1 private media in both entrance and main acts. Family-tinted lightweight atmosphere and contrast vignette protect title/button readability.
- `P1EntranceBackdrop.kt` retains the original P1 still/GIF/animated WebP/HEIF/short muted Media3 loop, orientation/crop, locked neutral media gate and failure recovery. Its historical `P1OriginalBackdrop` fallback is replaced by a corresponding **new living family** fallback. Low-power/video poster behavior and video lifecycle release remain unchanged.
- `P10ReminderPresentationHosts.kt` uses the scene compositor for built-in or gallery preview and main. An optional `renderBackground=false` allows an action owner to provide a **single** persistent background layer.
- `ReminderCheckInScreen.kt`: Creator screen now owns a single persistent `P10ReminderSceneBackground` outside its entrance/main crossfade; video does not remount because the entrance changes to the main act. Existing response and reveal callbacks unchanged.
- `P10PersonalReminderExperience.kt` + `personal/PersonalAlertActivity.kt`: similarly host one persistent background for each validated Personal occurrence; `V2233PersonalAlertScreen` can skip its own backdrop when the wrapper provides the shared one. Invalid/loading paths retain their safe normal backdrop. Original Personal receipt, snooze, dismissal and lock privacy remain untouched.
- `P1ChallengeNotificationEntrance` still uses the shared P10 entry and opens the existing normal app destination. No full-screen auto alert created.
- Media files remain app-private. Imported Live Photos may be stored as still images depending on Android picker; moving-photo support must not be falsely guaranteed.

## Precisely what is NOT complete
- **P10E: old renderers, resources, settings and compatibility references not physically deleted yet.** Dormant `V391ReminderSceneEngine`, `V394PremiereExperience`, `V336DirectorReminderVisuals`, unused `V195ReminderParticleScene`, old P1 legacy entrance helper and historical source/test tags still exist. Only P10E's safe reference inventory and cutover cleanup can remove them.
- Motion is a native Compose proposal only; no screenshots, GPU profiles, visual owner approval or physical APK evidence. A source branch is not a verified production experience.
- Existing P1 media import policy: video <=15 seconds and <=40 MiB, muted loops. Unsupported Live Photo animation must be exported as video when needed.
- Do not change the app-wide **Director's Cut / Backlot Light** theme choices; those are unrelated to the removed reminder scenes.

## Deferred QA acceptance (explicit founder consolidated gate only)
1. Source comparison to approved V337 stable, then compile cumulative P1–P10D Compose/Android. Fix exact compile errors without automatically running an entire JVM test suite.
2. Settings: all three scopes, three families, persisted selection, successful/failed gallery import, returning old P1 media, source switching without deleting media, confirmed explicit removal, crop/position persisted and restored.
3. Gallery image/HEIF/animated GIF/WebP/short MP4: permissions, decoder fallback, unsupported content, 15s/40 MiB validation, corruption, privacy and lock-screen handling; check no unexpected uploaded data.
4. Creator Voice/Alarm: exactly one continuous media surface across double-tap, and selected family/background persists through transition; all original Done/Working/Stage Done/Snooze/Replay/Reschedule/Pause/dismiss actions remain correct.
5. Personal Voice/Alarm: exact posted token validation, locked generic stage, no personal title/media/action leakage, one continuous media surface after user double tap, no response actions accepted while locked.
6. 21-Day: valid notification-opened visual with or without saved gallery, no automatic full-screen escalation, paused/archived/stale/out-of-window checks, original challenge navigation.
7. Preview in Settings: inline + full-screen should not hold concurrent video decoders. Picker cancellation, process lifecycle, pause/resume/rotation and media cleanup.
8. Small screen 320–420dp, 1.5x/2x fonts, TalkBack, reduced motion, battery saver, low-RAM/thermal, frame pacing and contrast across light/dark gallery media.
9. P10E source deletion, reference and strings/resources audit, no old visual fallbacks or Cinematic/Classic/Premiere selectable paths, and no deletion of user preferences/media.
10. Only after founder says staging is complete and explicitly approves consolidated tests/APK: scoped compile + focused tests + app/device QA + signed owner preview for aesthetic acceptance.

**No test, compiler, CI, emulator, APK, merge or release was triggered during P10D source staging.**
