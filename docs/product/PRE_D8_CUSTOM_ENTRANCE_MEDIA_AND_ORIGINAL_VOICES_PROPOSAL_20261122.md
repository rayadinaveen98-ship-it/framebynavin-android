# Backlot — Pre-D8 Requests: Custom Reminder Entrance Media & Truly Distinct Voices

2026-11-22 — **PROPOSED DESIGN + VERIFIED SOURCE AUDIT ONLY. NOT IMPLEMENTED OR APPROVED ARCHITECTURE.**

## Ownership, process and baseline

Founder requested before D8:
1. A gallery-selected image, <=15-second video, or compatible live/motion photo as a **BACKGROUND ONLY** for the double-tap entrance, configurable in Settings. Text, microphone/alarm symbol, double-tap gesture, main response screen, all existing reminder actions and audio must remain unchanged.
2. Solutions to make eight Backlot voices truly distinct rather than roughly the same device voice with pitch/rate modulation, including original voice options.

The source of truth is the D7 review-closure head `b0d667565bd1c27546be895a6956edde245ded75`, on `feature/backlot-ux12-d7-review-closure-20261122`, descended from D6/D7 and RX01/UX11. `main` is protected. UX09 excluded. This plan is documented on an isolated **planning** branch; no source edit, Gradle, tests, CI dispatch, APK, credential, new paid service or merge is approved by this document.

### Current source inspection

- `RX01ReminderExperienceGate.kt` owns the verified occurrence, actual double-tap gate and distinction between ENTRANCE and MAIN.
- `RX01FluidReminderBackdrop.kt` draws the existing three default entrance palettes and three quieter main palettes, independent of audio/response.
- `RX01ReminderOpticHero.kt` owns the original microphone/alarm/combined icon and all text; preserve this exact foreground.
- `RX01ReminderScenePrefs.kt` persists per-domain RX01 toggle, with V394/V391 compatibility. `RX01ReminderSceneSettings.kt` owns the existing Settings entry and three domain choices.
- `VoicePersonaEngine.kt` currently exposes eight profiles but picks from the phone's installed offline Android TextToSpeech voices and adjusts pitch/speed, then builds persona-specific reminder text. It does not deliver eight guaranteed distinct speaker identities.
- `V140VoiceStudioPicker.kt` previews/selects the eight existing IDs. `V403VoicePreviewPlayer.kt` and `VoiceReminderService.kt` use Android TTS. Personal/Challenge have their own playback ownership; a new engine must not assume Creator's service owns them.
- Current Gradle source has `androidx.activity:activity-compose:1.9.3` and no androidx.media3 dependency; Media3 is a proposed additional open-source dependency, not already installed.
- Original claim levels: source present, D8 build/physical tests not started; Personal Reminder P0 reliability remains open.

## Feature A: Custom media for double-tap ENTRANCE only

### User behavior

Settings → Appearance & sound → Reminder scenes → Custom entrance background.
Per-domain choice Creator / Personal / 21-day Challenge. Default is current RX01 fluid identity, separately retain all V391/V394 legacy selections. Each domain can select a locally imported still photo, compatible animated image or video segment trimmed to a maximum of 15 seconds. Preview, Replace, Reset, and optional crop/reframe. The custom scene affects only the **ENTRANCE background**, not MAIN background nor any overlay content.

- Pick only the media selected via Android system Photo Picker (`ActivityResultContracts.PickVisualMedia`); no permission to browse every image/media file.
- Import to app-private local storage before committing preference. Validate MIME/container, real decode, dimensions, duration, max output size, corruption and storage available. Atomic replacement of last valid file, cleanup orphaned imported files, protect the original gallery media.
- If video >15 sec, allow user to choose a <=15-sec segment and export/copy it via Media3 Transformer. Video muted (prefer no audio track in derived copy), hardware decode compatible MP4/AVC at modest resolution/fps, silent loop. If decoding fails, pause, low-memory, low-power, keyguard or screen-off: use poster/static/stock original fallback; never block alarm playback or gesture. Release player immediately on hide/exit; never stream/upload.
- For image: avoid original resolution decompression crashes, copy and resize preserving suitable visual quality, centerCrop with positioning controls. Original selection never mutated. High contrast scrim applied *behind* unchanged entrance icon/text to maintain accessibility.
- Motion Photo may contain JPEG still+video (Android Media3 has compatible motion-photo support). iOS Live Photo / HEIC+MOV and third-party system live wallpapers are not interchangeable files; the UI must explain that if Android cannot extract the motion media, the user should select/export its video or use the still. Do not promise direct import of all running live wallpapers.
- Privacy: non-public files never uploaded; do not show personal user gallery media behind a **locked** alert without a deliberate policy change, default to generic RX01 scene before unlock. Preserve already existing Personal/Challenge keyguard protection, no new FSI eligibility or permission.
- Layout: image/video is a strictly decorative, touch-transparent layer below `RX01ReminderEntranceHero` and gate's gesture surface; preserved existing icon, label, 'DOUBLE TAP TO OPEN', semantics, phase transition and MAIN backdrop. Never auto-advance after 15s; 15s refers to video asset maximum and it loops during ENTRANCE.
- Compatibility: low-end API26+, Android13–16, Redmi Pad 2, battery saver, no-animations, poor decoder, process recreation, missing/revoked URI, rotated aspect ratio, locked phone, 2x font, TalkBack. Preserve no phantom user completion or delivery claim.

Proposed staged implementation only AFTER founder signs off on scope/architecture: new media model, import/validation/trim pipeline, app-private store + scene preference, safe entrance-only renderer, Settings selector/preview, source-only QA matrix. Existing RX01 and reminder ownership unchanged; D8 deferred final compile/test/APK policy retained.

**Reference:** https://developer.android.com/training/data-storage/shared/photo-picker ; https://developer.android.com/media/media3/transformer/transformations ; https://developer.android.com/media/media3/exoplayer/supported-formats

## Feature B: Original voices, not eight pitch presets

### Current gap

`VoicePersonaEngine.apply` runs `selectDeviceVoice()`, then preset `tts.setPitch` and `tts.setSpeechRate`. Candidate voice availability/quality depends on the device. On devices with 1 appropriate voice, eight profiles share that same speaker. The term 'eight Backlot delivery styles' in Settings is honest; 'eight independent neural voices' is not yet true.

### Options requiring a founder choice

**Option 1 — Installed-device/offline TTS enhancement (zero API costs):** expose actual installed voices (including other Android system engines where lawful), choose by stable voice ID/language, quality test and only label distinct voices if actually present; optional independent on-device neural voice packs (e.g. licensable Piper models) may improve identity at cost of larger downloads, battery and inconsistent locale/device coverage. Licensing of *each* model and speaker, especially commercial redistribution, must be audited first. Not guaranteed 8 distinct voices on all phones, especially Telugu.

**Option 2 — Cloud neural preset speaker voices:** run a small opt-in listening benchmark (same text, English / Telugu / mixed), with 8 truly distinct speaker identities rather than pitch variants. Compare Google Cloud Chirp 3 HD (lists te-IN) and ElevenLabs V4/Turbo (lists Telugu among supported languages). Prices are per character and/or plan; never activate billing or put API key in the APK without founder consent. Generate ahead of scheduled alarms and cache privately; never make successful speech depend on the network at alert time; retain explicit offline Android fallback and truthful fallback message. API calls may transmit reminder text; require affirmative consent, privacy disclosure, secure backend token, retention limits and deletion controls. Prevent duplicate requests via content+voice+locale hash and bounded cache.

**Option 3 — Backlot-licensed original identity voices:** audition/hire distinct professional voice actors with explicit license for app commercial use and potentially consented synthetic voice generation, or commission licensed bespoke voices through a provider. A small fixed set of recorded phrases alone cannot speak arbitrary new reminder titles; an approved dynamic synthesis model or segmented synthesis is still required. Avoid cloning another person's identifiable voice without their informed consent, and never falsely claim the voices are human recordings when they are generated.

### Recommended production architecture (NOT APPROVED YET)

A pluggable `ReminderSpeechProvider` contract, with `AndroidSystemSpeech` as backward-compatible offline fallback and optional `NeuralSpeech` as explicit user-selected quality tier, with exact original eight `VoicePersona` IDs and settings state preserved. New voices can use separate stable provider+voice IDs without coercing old IDs to unrelated speakers. Prepare and validate audio **on creation/edit/schedule**, not during an active alert; securely cache with expiry/storage budget and deterministic invalidation on text/lang/voice changes; preserve user privacy, no automatic remote upload. Existing Creator, Personal and Challenge owners must continue to own alarms, repeat/snooze/receipts/notifications; use separate adapter integration in approved source stage. A voice preview should compare the SAME sample content for all voices; support Telugu and English if chosen, with transliteration/pronunciation handling and offline-safe behavior.

### Decision gate

1. Media: scope sufficiently specified; founder can approve staging an isolated feature before D8. Paid services: **none for media**.
2. Voice: choose offline-first no subscription (quality varies), cloud neural paid/consent, or commissioned Backlot originals (upfront and/or recurring expense). Recommend first listening samples/benchmark and cost cap. No voice provider, billing, third-party recording or training is approved yet.
3. Gate: Source-only staging until all approved pre-D8 feature additions are ready. Do not trigger unit/lint/emulator/API build or APK before founder's explicit final-stage authorization. Keep D8 held and P0 Personal Reminder safety certification open.

**External sources:** Android TextToSpeech https://developer.android.com/reference/android/speech/tts/TextToSpeech ; ElevenLabs https://elevenlabs.io/docs/overview/capabilities/text-to-speech ; Google Chirp https://docs.cloud.google.com/text-to-speech/docs/chirp3-hd ; prices https://elevenlabs.io/pricing/api and https://cloud.google.com/text-to-speech/pricing .
