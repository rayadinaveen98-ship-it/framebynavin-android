# Backlot Point 8B — Living Voice Orb & Single-Field Capture

**Founder approval:** The previous reactive dot field felt too inert. The double-box capture flow was clumsy. Owner approved a visibly living/breathing orb, one primary idea field, optional notes, and elegant exclusive Speak-to-text vs Record-original controls. Preserve the Backlot Creator-first identity and existing data.

**Status:** SOURCE STAGED / UNCOMPILED / UNTESTED / NOT DEVICE VERIFIED / NO APK.

## Repo and gate
- rayadinaveen98-ship-it/framebynavin-android
- Branch: feature/backlot-v337-point08b-living-voice-orb-source-after-p8-20261123
- P8 base SHA: 1631103e933937cf0e7b64dfafcaaacad899aa34
- Accepted V337 baseline SHA: 815ba69775f75b6e5f25bb838c2c17b4f09c203e
- Main unchanged, stable unchanged, no CI/JVM/Gradle/tests/APK/merge/release.

## Preserved behavior
- Existing IdeaVoiceTranscriber and Android SpeechRecognizer remain authoritative for microphone permission, Telugu/English/Auto language, partial/final text and lifecycle cleanup.
- Original VoiceIdeaRecorderInput remains authoritative for actual saved original audio. No new recording store, remote upload, retention or synthesis system.
- Final recognized transcripts append to the same existing idea title or Quick Capture title via existing callback. Edit Idea continues dictating into its optional notes (label explicitly states this).
- Original FC13ReactiveDotField untouched for other clients including Spark; this new UI is only for V117 dictation.
- Original saved audio, source link, create/edit/save/draft, Quick Capture recovery and audio deletion paths remain in their original stores/owners.

## Point 8B staged changes
1. P8BLivingVoiceOrb.kt: Canvas-rendered fluid nucleus, clearly breathing while idle (~3.2s cycle), subtle surrounding particles and outward rings; stronger speech-reactive expansion from actual non-calibrated Android recognizer RMS. No fake audio simulation in app, no content changes, no UI image generation.
2. Disables visual looping effects for Android animator-off, low-RAM or battery-saver modes, while preserving dictation tap and text status. No strobe/flashing/fake live signals.
3. V117VoiceQuickIdeaUi.kt: removes previous two-step "open dot field then tap" interaction and second bordered partial-transcript panel. Living orb is shown as soon as Speak-to-text is selected. Partial text is a small read-only hint. Final text calls original parent onTranscript callback. Real idle/listening/permission/error/transcript-added feedback is presented.
4. P8BVoiceModeSwitch.kt: exclusive Speak-to-text vs Record-original tabs/chips. Tapping the selected mode closes it; switching away during active original recording is blocked. A saved recording remains attached when mode is changed.
5. V09IdeaVaultUi.kt: removes the nested double-border around the original main idea field. New Idea has ONE primary writing box, plus optional "Add more notes" hidden until selected. Old prompt labels and similar-idea guidance still work. The real original recorder remains available and its Save action cannot proceed while recording is active.
6. V14QuickCaptureUi.kt: same voice tabs with original quick-capture-speak / quick-capture-record tags retained, recorded-file draft recovery and durable save callbacks untouched, optional notes folded unless a recovered draft has notes.
7. Edit Idea: saved notes expanded by default so no user text is hidden silently; empty notes collapsed with Add more notes, and dictation into optional notes is folded and labeled. Original voice playback and edit footer preserved.
8. Added P8BLivingVoiceOrbPolicyTest.kt as unexecuted source tests only. No per-update QA, compiles or APK.

## Deferred consolidated QA (must NOT run yet)
- Compile all P1–P8B Kotlin/Compose sources, check imports, modifier semantics, animation API and old Spark component references.
- Android phones at 320dp/360dp/420dp, font scale 1/1.5/2, screen reader, landscape/tablet and reduced motion.
- Orb at idle: motion visibly breathes. With mic permission denied: no listening claim, readable permission error; mic not activated before tap. With motion disabled: static aesthetic, operable control.
- On real speech (quiet, loud, silence), core energy reacts to real SpeechRecognizer RMS. Language Auto/Telugu/English selections remain honored; restart/destroy handles lifecycle.
- New Idea: one editable main field, optional notes, dictation writes only after final results, partial hint never changes persisted content, saved audio separate from transcribed words.
- Quick Capture: draft persistence and recovery after process death, record start/pause/stop/save, mode switch locked while recording, original audio retained when switching, create remains exactly-once.
- Edit Idea: previous notes visible, optional notes can collapse/expand, speaking writes into notes, playback original audio unchanged.
- Archive, restore, search, convert to project and unrelated Creator Home, Personal Home, Reminder Hub, settings and P6a onboarding still operate normally.
- Staged test source is not evidence. The full suite and physical owner APK are gated until the user says all points are staged and explicitly authorizes consolidated testing/build.

**Point 9 — Settings and Appearance: NOT STARTED.**
