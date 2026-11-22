# Backlot RX03 — Offline Speaker Identity Studio (pre-D8 stage)

**Date:** 2026-11-22  
**Status:** SOURCE STAGED ONLY — NOT COMPILED, TESTED, OWNER-AUDITIONED, MERGED, APK-BUILT, RELEASED, OR QUALITY-CERTIFIED.

## Provenance / change control

- Source parent: `feature/backlot-rx02-custom-entrance-media-20261122` at `a732205ec24ee643f40b1b0bf79797bed6fb3db2`.
- New isolated stacked branch: `feature/backlot-rx03-local-voice-identity-studio-20261122`.
- Protected main/stable at project baseline: `abbe88984bac16cfafbd990d9cb00ed54cfdd07e`. Do not merge, release, deploy or alter stable/main.
- The founder explicitly deferred Gradle, lint, unit/emulator/physical tests and APK until one cumulative D8 authorization. **No tests or builds were triggered by this stage.**
- Paid neural voice provider, billing, subscriptions, private voice recordings/voice clones, cloud integration, model redistribution and new paid service **NOT APPROVED**.
- Personal Reminder long-idle / retries / Snooze P0 certification remains separately open.

## Actual source-state diagnosis

`VoicePersonaEngine.kt` has eight existing `VoicePersona` values and profiles, but their current speech production is Android TextToSpeech plus pitch, rate, and different text. `selectDeviceVoice` previously picked indexed local Android speaker IDs, falling back to last available index. When only one suitable installed speaker exists all eight styles may share one human sound. This stage does **not** falsely claim eight new original AI speakers.

Creator `VoiceReminderService` supplies `task.voicePersona`. `V403VoicePreviewPlayer` owns process-local samples; `V140VoiceStudioPicker` owns the current eight persona choices. Personal `PersonalPlaybackService` and Challenge audio are separate functional owners; no unapproved audio redesign was applied to them.

## Implemented in source

1. **`RX03VoiceIdentityRegistry.kt` (new)**
   - Queries actual Android TTS offline `Voice` entries only (`!isNetworkConnectionRequired`) in the device language. Sorts by quality and latency, deduplicates stable name/locale pairs.
   - Saves an optional **actual Android speaker ID**, the **default TTS engine package** and the **voice language tag** for each existing Creator `VoicePersona`, in a separate app-private SharedPreferences file.
   - On subsequent use, speaker overrides are accepted only when the same engine, speaker ID and language still exist locally. Missing voice or changed engine falls back to established local speech style. No user content or voice recordings are sent externally.
   - Does not migrate/change project voice enum values; restore-original resets only this optional override. Save reports persistent-write failure rather than claiming success.

2. **`VoicePersonaEngine.kt`**
   - Extends `apply(tts, persona)` with backward-compatible optional context and preview override. Existing 8 pitch/rate profiles, labels, descriptions, scripted Creator reminder wording and fallbacks remain.
   - If the user explicitly selects an installed offline speaker for a persona, it is used *before* that persona's modulation. When no override exists, the current device voice selection path applies.
   - Prevents the original fallback from actively choosing a completely unrelated language if no matching installed locale appears.
   - Adds one identical benchmark phrase for speaker-to-speaker audio comparisons. This is a studio preview only, not a runtime reminder phrase.

3. **`V403VoicePreviewPlayer.kt`**
   - Preserves one active preview, 12-second timeout and generation gating (stale initialization never speaks).
   - Supports transient trial of a specific installed offline speaker and benchmark narration without saving. Regular persona preview remains as it was.

4. **`RX03LocalVoiceIdentityStudio.kt` (new)**
   - Collapsible `Settings → Voice → Real Speaker Identities` area, with eight existing persona tabs, real inventory count/labels for the phone's default TTS engine and current language, same-sentence audition for each actual offline speaker, optional `Use speaker` per persona and `Reset`.
   - The surface labels local voices as installed speaker IDs, not eight new/backlot-exclusive voices. Voice availability can vary across phones, locale, OS and installed TTS engine.
   - UI only changes a persona-speaker mapping when user explicitly taps `Use Speaker`; opening Settings or playing a preview does not save or change actual reminder voices.
   - Speech engine scan closes with the Settings surface; listen player remains separately one-at-a-time and is shut down when the identity panel closes.

5. **`VoiceReminderService.kt`** and **`V144SettingsHub.kt`**
   - Creator audio now passes app context to `VoicePersonaEngine.apply` so any explicit saved offline voice assignment is used in actual live Creator reminder speech.
   - Voice Settings hosts the new optional studio below the existing eight persona cards; copy explains that an underlying speaker override can affect existing projects that use that same persona, but their chosen persona values are unchanged.
   - All original Creator TTS callback listeners, wake locks, service response watchdog, Repeat/Snooze/Done/Play/notification pipelines are untouched by this source stage.

## What RX03 explicitly DOES NOT provide

- It does not generate eight distinct speaker timbres on a phone that provides only one. Eight styles are retained; the underlying audible identities are limited by the local engine.
- It is **not** an original or proprietary Backlot neural voice model; no training, model asset, license or audio synthesis backend exists yet.
- It does not replace Personal or 21-Day Challenge's independent existing speech engines; their voice quality needs a separately scoped, owner-approved unified provider contract.
- It has not been auditioned in Telugu/English; quality, pronouncing Telugu words in English, en-IN/te-IN locale support and mixed-language phrases need real listening evaluation.
- It does not add paid cloud TTS, guarantee commercial rights for third-party downloadable models, or promise cross-device speaker portability.
- It cannot prove that two separate Android voice IDs represent acoustically distinct individuals. Identity must be evaluated by ear or provider metadata, not guessed from a numeric count.

## D8 acceptance matrix (authored now, DO NOT EXECUTE YET)

1. Compile Android Kotlin source and imports with exact integrated Gradle dependencies, SDK26–36 compatibility, compose side-effects, full Settings scroll and voice inventory resource disposal.
2. Voice Studio: scan available offline voices on physical Android 14/15/16 and tablet with one, several, zero or missing language; error path, slow TTS initialization, quick navigation away, engine switch, remote-network voice rejection; no automatic TTS requests during screen close.
3. Same-sentence preview: one speaker at a time, no stale sample after changing persona, no repeated speech after closing, own preview volume only; good accessibility and 2× font.
4. Explicit speaker selection persists across process restart for the exact Android engine/voice/locale; switching voice engine or uninstalling voice falls back to prior profile without crashing. Reset restores original; old users have no migration or default voice change.
5. Creator real occurrence: explicit persona-speaker choice affects synthesis when chosen while preserving callback/retry/watchdog/Repeat/Snooze/Done/notification state; a failed voice ID is NOT a delivered/heard audio receipt.
6. Personal and 21-Day playback unaffected. Regress actual audio on Redmi Pad 2 and OEM Android; P0 unattended Personal Voice failures need independently certified long-idle/auto-retry/Snooze tests.
7. Private strings/recordings never leave the phone in RX03; no billing/API key/session/user-credential/network permission additions.
8. Founder must listen to a fixed English/Telugu sample set and decide if next product stage should license original synthesized speakers and/or commission actors, subject to provider rights, privacy, offline caching/fallback and cost approval. Do not call RX03 alone the "perfect eight voices" solution.

## Roadmap

- D0–D7 + review closure: source staged.
- RX02 custom entrance media + unified Reminder Appearance: source staged, D8 not run.
- **RX03 Offline Speaker Identity Studio: source staged, NOT voice-quality accepted.**
- **RX04 truly original premium voices: provider/voice audition and cost/privacy/license approval pending; NO engine installed, no code for remote API, no paid activation.**
- D8 integrated tests and Lab APK: held until founder explicit final command.
