# Backlot Personal + Creator V273 — custom voice backing music (DRAFT)

Based on the V271 three-voice-announcement + original piano draft branch at commit `f98a24b58de9429f8efd2225dfdb6e1179fffda4`. This is NOT a full owner APK, Play Store release, Supabase change or account-deletion change.

## Implemented

- Shared `ReminderBackingMusic` plays one of: offline original piano, user-selected local audio (`content://` from Android Documents/Files SAF), or no background music. Custom file loops quietly under TTS; audio volume ducks when a sentence is spoken and recovers between announcements.
- A reusable Compose picker in **both Personal Voice** and **Project Advanced Reminder Settings**; users can choose an audio document through the system Files UI, replace it, select piano/no music, hear the background-music preview and stop preview.
- Uses ACTION_OPEN_DOCUMENT / `ActivityResultContracts.OpenDocument` with a persisted **read-only** URI permission when the system document provider supports it. No `READ_MEDIA_AUDIO`, broad gallery access, custom online streaming or third-party sound library. A chosen audio file must remain accessible on the device. If it is moved, missing or denied, the original piano acts as fallback while the TTS and actionable notification remain.
- Personal sound selection and URI belong to a separate Personal reminder and its v1 JSON codec. Optional new keys are defaulted when importing old backups. After restoring onto a different device, reselect the audio file because SAF permissions do not migrate with the JSON.
- Creator/Project music choice is stored **only in device-local private SharedPreferences** keyed by Creator task ID, deliberately outside TaskStore, Creator backups, YouTube data and Cloud Sync. Project music is saved only after the project creation/edit operation succeeds. Cloud-synced Creator content does not contain a local file URI.
- Playback tears down in existing Done/Snooze/Stop paths. No imported personal record gets converted to CreatorTask.

## Not claimed

- The music preview button plays the **background only**; it is not yet a combined TTS+music rehearsal.
- No physical-device audio audition, audio focus/interruption/accessibility/Doze acceptance yet; Android device/engine restrictions apply.
- Different URI providers, audio formats and revoked permissions can be device-specific; the existing notification remains actionable if media fails.
- Does not yet implement the proposed self-care step-by-step editor, hourly schedules (separate tested V272 PR #215), widget previews or integration of all branches.
- The existing Build270 APK, version/signature, real user data and Play Console remain unchanged; no unreviewed merge, Google authentication or hosting expense.

## Mandatory QA

1. GitHub Actions: JUnit mode/URI checks, full Android debug compile/lint, Android test compile, Android11 Personal export/import compatibility and local Creator music preferences.
2. Merge preview branches only after owner-approved integration review. Android11/16 emulator and physical owner Android tests should include three audible TTS announcements while backing loops, custom file permission after app restart, background playback stop on Done/Snooze, missing song fallback, screen/device rotation, simultaneous reminder conflict, Doze/notification permissions, backup import.
3. Future release migrates `versionCode` and APK signer carefully; never uninstall current user app without separate Creator and Personal exports.

No paid API/service required for this source-level development.
