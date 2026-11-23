# Backlot Point 9B — Settings Interior Organization (2026-11-23)

**Founder authorization:** "ok bro go ahead" in the continuation of the Settings-inside audit. Scope is the five Personalization subpages, without changing their data owners or advancing Point 10.

**Status: SOURCE-STAGED / NOT COMPILED / NOT TESTED / NO APK / NO MERGE / NOT DEVICE ACCEPTED.**

## Authority and baseline

- Repository: `rayadinaveen98-ship-it/framebynavin-android`
- Isolated cumulative source branch: `feature/backlot-v337-point09b-settings-interiors-source-20261123`
- Parent P9 commit: `ea5cd07b5bec14a33b16c6a21c49ed85d15e1f43`
- Owner-accepted stable V337: `815ba69775f75b6e5f25bb838c2c17b4f09c203e`, unchanged.
- Production `main` unchanged. No Gradle, JVM, UI, emulator, device, CI, signed APK, merge or release executed.
- No destructive data migration, key reset, backend changes, cloud uploads or paid dependencies.

## Approved scope implemented as source

### 1. App Appearance
- `V127AppearanceSettings.kt`: tighten theme copy and retain only existing supported Director's Cut / Backlot Light; remove launch sound from appearance.
- `V133AppIconPicker`: retain the single approved brand icon display and legacy alias compatibility. Do not invent fake alternate icons or alter launcher selection keys.
- Both theme cards now show simplified recognizable Creator Home mini-previews (Backlot, Today, Up next) instead of only abstract lines. These are illustrative compositions, not Android screenshots; device acceptance is still required.

### 2. Reminder Backgrounds
- `P1GalleryBackgroundSettings.kt`: reorder source tabs to Creator first, Personal second, 21-Day third; Creator is the default view.
- Replace competing separate Original / Gallery cards with one clear Background Source panel.
- Preserve one chooser, custom media type detail, full-screen voice/alarm preview, per-scope private media, reset, contextual crop/zoom/position, and existing test tags.
- Rename ambiguous Mic / Clock to Voice / Alarm, remove developer-facing "Source-stage notice" wording, and fold detailed media-format/privacy guidance behind a clearly labeled disclosure while keeping local-only status visible.
- For non-Creator flows, explicitly disclose that new full-screen visual attachment is pending integration; do NOT represent the preview as a delivered reminder.
- P10 built-in immersive replacement backgrounds are not included.

### 3. Companion
- `V144GuideSystem.kt`: settings copy focuses on saved vs preview selection; smaller hero portrait.
- Fold Hello/Thinking/Celebrate audition poses behind optional expression preview; first-run companion choice unchanged.
- `V144SettingsHub.kt`: separate Companion Presence controls from character selection.
- Preserve saved Spark/Mira identity, legacy preference handling, suggestions toggle, underlying artwork, and all callback guards.

### 4. Voice & Sound
- `V140VoiceStudioPicker.kt`: optional `compact` mode for Settings only; shows selected voice and Change Voice instead of all eight cards at once. Other callers retain existing uncollapsed behavior.
- `V144SettingsHub.kt`: reminder voice, reminder music, app launch sound, then advanced offline speaker lab. The lab retains its own explicit expansion.
- `VisualExperiencePrefs.launchSoundEnabled` and existing update callback move to Voice & Sound; no new preferences.
- Existing voice previews, device offline voice identities, English/Telugu audition, and guarded apply-music-to-all confirmation remain owned by their existing stores.

### 5. Home Photos
- `V167SettingsNavigationPolicy.kt`: add subordinate HOME_PHOTOS destination under Personalization.
- `V144SettingsHub.kt`: enter an in-Settings Home Photos page rather than redirecting to Today.
- `V175BestFramesUi.kt`: opt-in `settingsPhotoManager` mode reuses the existing `V18CinematicHomeHero` editor state and `V18HomeHeroStore` (including pickers, reorder, remove-one confirmation, replace, pause/play and remove-all confirmation). Settings displays a contained manager; live Home rendering remains in the default mode.
- Original `onOpenHomeSlideshow` callback in Settings API is retained for caller compatibility, though the Settings button no longer invokes it. Home overflow Gallery still uses its existing path.
- No cloned photo repository, parallel album, overwrite migration or new media permission.

## Deferred consolidated verification — DO NOT RUN YET
1. Compile all P1–P9B Kotlin/Compose sources, including the conditional in `V18CinematicHomeHero`; examine syntax/import resolution and regressions.
2. Verify nav back / system back for every nested Personalization page; no Settings-to-Today surprise when opening Home Photos; returning from Android media picker remains on Settings.
3. Appearance: both themes, approved canonical icon and launch sound moved to Voice & Sound; no old Cinematic/Classic/Premiere selectors.
4. Reminder Backgrounds: Creator-first initial tab and Voice/Alarm labels, per-scope isolated saved gallery assets, full-screen preview, import, reset, crop and privacy; check pending Personal/21-Day live host caveat.
5. Companion: Spark/Mira preview only until confirmation; legacy choice, expression toggle, suggestions, same poses/artwork and no altered first-run path.
6. Voice: collapsed/expanded eight styles, preview, offline lab, sound/microphone availability, no unexpected voice or project music overwrites.
7. Home Photos: empty + one photo + many photo flows, add/replace/reorder/remove, cancel confirmation, pause/play, process restart and source equality between Home and Settings.
8. Small phones, 1.5x–2x font, screen readers, touch targets, reduced motion, dark/light, tablets, permission denial and Activity lifecycle.
9. Confirm no unexpected onboarding reroutes, lost data, changes to Creator/Personal reminder triggers, notifications, snooze/music/voice or account/sync.
10. Only on founder's explicit consolidated QA/APK instruction: run targeted checks and integrated device validation. Do not run per-point builds or tests.

## Remaining boundaries
- P9B is a repository source stage, not a certified UX or implemented on a physical device.
- P10 (three immersive built-in reminder visual replacements) remains separate and not started.
- Voice music persistence semantics are unchanged: the existing global apply action can update existing Creator projects, with confirmation.
- App-theme preview remains a representation, not a verified actual screenshot. Real-device UI captures and polish decisions are deferred.
