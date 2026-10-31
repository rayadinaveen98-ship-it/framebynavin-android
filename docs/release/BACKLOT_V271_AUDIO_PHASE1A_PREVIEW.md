# Backlot Reminders V271 — Phase 1A audio preview (NOT released)

Based on immutable Build270 Phase1 acceptance source `1810aae9c4a7cd2762388306b63ee19793c66c18`; **no change to user's installed app until a future owner-approved full APK**.

## Source changes

- Fix Personal voice reminders: replace once-only TTS callback with **three complete spoken announcements**, original title frozen for the whole occurrence, ~2.5-second gaps, dedicated 90-second safety watchdog. In particular, the current personal title and notes are never sent to CreatorTask or project systems and personal notes are never read aloud.
- Share an **original offline synthesized piano-like arpeggio** through `ReminderPianoLoop`, with ducked volume while TTS speaks, both for Personal and Creator voice reminders. No licensed music, external file, Internet call, gallery permission, upload or other service dependency.
- The unchanged Creator voice engine already supports configured repeat counts up to three and user-configured gap; it now starts/stops and ducks piano. Project-specific replay limits and foreground service watchdog are preserved.
- Done/Snooze/Edit/Delete stop the existing occurrence via its generation/token and existing service teardown. Both player and TTS are released. TTS failure leaves already posted actionable notification.
- The source branch adds tests for bounded 3x repeat advancement and original deterministic PCM samples.

## What is NOT done yet

- User-selected custom songs/Android file picker and persistent SAF URI permissions; personal/creator music settings and optional No Music mode.
- Actual speaker/TTS/media-focus tests on physical Android devices; audio mix quality and different TTS languages need manual acceptance.
- Personal editor self-care redesign, hourly recurrence, widget previews.
- This branch is NOT an APK distribution branch, Play release, Cloudflare site or production Supabase change.

## Next phase

Phase 1B needs a persisted, per-reminder music selection in Personal and Creator independent stores with schema/backup migration, Android file picker, revoked-file fallback and player ducking/focus handling. Then validate complete mixed voice/media behavior in phone/tablet emulator and physical owner device; only after that proceed to personal hourly scheduler and UI redesign.

## Safety

Existing Build270 `versionCode=270` and public prototype signing stay frozen. No storage models, backup codecs, project records, databases, widget files or scheduling logic touched in 1A. The new audio engine runs only while an admitted reminder is active, and the prior notification fallback remains intact.
