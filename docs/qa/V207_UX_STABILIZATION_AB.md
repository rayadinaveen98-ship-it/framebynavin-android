# Backlot V207 — UX stabilization, A/B

VersionCode **227**, `2.7.7-v207-fullscreen-voice-preview`.
Base **V206**, PR remains draft pending device acceptance.

## A — Stop Home appearing through fullscreen Settings
The current shared Activity renders Home content under Settings. Previously `V144SettingsHub` applied `WindowInsets.safeDrawing` to its own opaque `Surface`, leaving top/bottom strips where Home could be visible on edge-to-edge Samsung devices.

Settings now paints an opaque `Surface(Modifier.fillMaxSize())` across the Activity root; only an inner `Box` receives drawing and keyboard insets. The nested Settings section pages remain scrollable and safe-area-aware. Home, projects, onboarding, notifications and task data are unchanged.

## B — Compact voice in New/Edit Project
Replaced three inline `V140VoiceStudioPicker` usages in advanced reminder settings:
- Both quick and advanced New Project wizard paths.
- Existing project editor/reminder configuration path.

A compact card shows the selected `VoicePersona` label and a fast Preview action. Tapping the card opens a scrollable single-column dialog with **all eight existing persona values**, a per-row TTS Preview, selected state and explicit Close/Done. A selection calls the *same* `onSelected` callback and then closes the dialog. Preview calls the existing `onPreview` and does not change the persona. Global Settings retains its existing default voice picker for now.

No changes to persona enum IDs, text-to-speech engine/pitch/rate, repeat intervals, alarm timeout, default inheritance, persistence, migration, Play package ID, signing or scheduled reminders.

## Required checks
- `tools/check_v207_fullscreen_voice.py` inherits every V200–V206 brand/reminder guard and verifies background/insets and all 3 replacement call sites.
- New Android `V207FullscreenVoiceUiTest`: fullscreen bounds to all Activity edges, preview isolation and select callback, and no-save dismissal.
- Inherited clean Gradle unit tests, lint, debug/instrumentation APK, personalStable signer/package/version/APK-resource verification and emulator regressions.
- On Samsung, install in-place from V206; verify full-screen Settings top/bottom while scrolling, dark/light and large-font settings, back navigation and open-from-project Settings detour; selected voice and previews in Quick New Project, Advanced New Project and Existing Project.
- Real-device screenshots, UX confirmation, and green exact-SHA CI required to promote.

## Next V207 phases (not implemented here)
C. Simplified first-run account/setup, preserve returning-cloud recovery.
D. Measured tutorial target bounds and readable guide.
E. Revenue reporting coverage truth and streamlined dashboard with verified-only totals.
F. Integrated acceptance and stable release candidate.
