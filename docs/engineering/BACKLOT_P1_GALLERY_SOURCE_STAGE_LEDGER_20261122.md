# Backlot P1 Gallery Entrance — Source Staging Ledger

Founder approved Point 1 on 2026-11-22. No Points 2–10 are authorized.
Repository: rayadinaveen98-ship-it/framebynavin-android
Branch: feature/backlot-v337-point01-gallery-entrance-source-20261122
Reliability baseline SHA: 815ba69775f75b6e5f25bb838c2c17b4f09c203e
Rejected V338 source 8b771a5287b92d33cc8341907fc7747447cfd29a was read-only technical reference, never merged.

Approved: local Photo Picker, private photos, GIF/WebP, HEIF, silent looping videos <= 15s and <= 40 MiB, adjustable crop, independent Personal/Creator/21-day selections, preview, replace and reset. No universal third-party wallpaper engine import.
Keep old icon, text, tap mechanics, voice, music, snooze, Done, all reminder scheduling and privacy unchanged.

HOST GAP: V337 Creator has a real V195 ONE-TAP particle entrance; custom media can be underlaid there without changing gestures. V337 Personal has no separate double-tap entrance; V337 21-day uses notifications opening the normal app. Their gallery selections are stored and previewable, NOT active on live alerts. Replacing the host or rerouting PendingIntents is excluded until separately reviewed and explicitly approved.

SOURCE-STAGED, UNTESTED: media store and import limits, image/video renderer and safe fallback, Settings Appearance launcher, gallery import, full-screen preview, crop, isolated Creator background underlay, focused unit cases.
NOT STARTED: source compilation, JVM test run, AndroidTest compile, emulator test, physical-device acceptance, signing, merge or release.

Planned tests after founder QA gate:
QA0 branch diff and production invariants: no unrelated source, manifest, sign-in, reminders delivery, hosted data, credentials, version or signing changes.
QA1 compile Kotlin only; fix exact failure without full JVM restart.
QA2 run P1GalleryImportPolicyTest and only affected existing reminder presentation tests.
QA3 AndroidTest compile and synthetic gallery importer negative cases.
QA4 disposable Android 15: images, GIF/WebP, HEIC, 15s silent mp4 loop, >15s/40MiB rejection, crop persistence, locked/fallback, original Creator icons and unchanged tap behavior.
QA5 Personal/21-day live entrance only after separate approved compatible host integration, then alerts and Done/Snooze regression.
QA6 separate signed owner-preview identity/hash and physical owner acceptance.

No tests or APK builds manually triggered for this source staging. No stable branch or cloud changes.
