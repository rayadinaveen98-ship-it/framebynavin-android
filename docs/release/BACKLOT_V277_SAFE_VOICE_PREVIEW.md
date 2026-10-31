# Backlot V277 — Reminder sound-preview hardening (DRAFT)

Based on full owner-preview Build271 source SHA `40134107ffc882f28d46d4674bb1763a82a22a1b`; existing independently verified Build271 package, owner device state and release signing are unchanged.

## What is improved

- Existing Personal Voice and Creator Project Advanced Voice music editor gains a **Test voice + music** action that actually speaks a fixed generic sample through Android TextToSpeech while the selected original piano or user-picked Files song plays softly underneath.
- Sample text is fixed and never reads, uploads or sends the user's private Personal title, Personal notes or Creator tasks. Real reminders still perform their existing three utterances and occurrence/notification lifecycle; this editor preview speaks exactly once.
- Preview supports original piano, custom SAF music or silence. Voice/music volume ducking uses the same `ReminderBackingMusic` implementation as actual reminders.
- The existing Background Music preview is now time-bounded to 30 seconds. Both previews stop on explicit Stop, audio callback completion, editor disposal or Activity/Screen ON_STOP. TextToSpeech engine is shut down, generation numbers invalidate late callbacks and all pending timers are cancelled to prevent continuing speech after leaving the editor.
- No background service, notification, file copies, new Android permission, database/codec, backup format, cloud account write or live app version/signing change.

## Release gates

- Source-level guard and full Android build/lint/JUnit plus Android11 instrumentation check for sample TTS start-stop, button interaction, music-only playback and UI disable while No Music.
- Real physical device listening (language/voice availability, actual audio focus and volume balance) still required.
- This is NOT distributed as a new APK from this branch. Review and release a higher versionCode only after emulator QA and owner acceptance of existing Build271.

Parent integrated owner preview: PR #219; independently verified 17/17 tests on both Android11 and Android16 for original Build271: https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/41196725502
