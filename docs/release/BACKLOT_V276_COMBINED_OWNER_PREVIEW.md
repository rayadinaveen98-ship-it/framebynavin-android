# Backlot V276 owner-preview integration — candidate build 271

**DRAFT / synthetic QA until both CI jobs are green and a real owner-device trial succeeds.**

This branch is based on V273 custom local music PR #216, with source-level integration of verified V272 hourly schedules, new V274 Self-Care category UX and V275 real widget previews. No Play release, public website, live Supabase or real user records were changed.

## Included in one full Android package

- Existing whole Backlot Android app features remain. Personal voice reminders now attempt three full TTS announcements with a bounded watchdog; Projects retain per-project 1-3 repeats. Original piano audio is dynamically synthesized and played via a temporary local WAV/MediaPlayer; no borrowed music sampled.
- Personal and Project Voice: system Files picker supports local audio backing, persistent read grants, preview, piano / custom / off, 3x TTS ducking and missing-file fallback. Project song reference stays private device-only, never in CreatorTask or synced backups. Personal song references stay in Personal local backup; restore on another device requires reselecting the file.
- Personal recurrence: every 1-24 hours, active window, weekdays/weekends, every-N-days combinations, next five scheduled occurrences; old options remain. Hourly stale notification recovery prevents prematurely dropping an unanswered current alert.
- Personal Self-Care: horizontal optional category gallery, non-destructive title suggestions, guided sections, plan summary; purpose only in Personal storage and checksum-protected export, old v1 files import as CUSTOM.
- Widget Manager: read-only actual layout previews for the three modern widgets, with synthetic data and private Personal title protection.

## Signing and install constraints

Integrated candidate versionCode=271, versionName=2.16.0-rc26-reminder-experience-owner-preview. Its `personalStable` APK is signed with the checked-in **public prototype signer**, matching previous prototype Backlot owner previews. It is NOT safe for Google Play production distribution.

Only after full compile, lint, unit tests and synthetic Android11/Android16 instrumentation pass may the APK be offered as a **test update**, with an explicit warning to independently export Creator and Personal backups. Never uninstall Build270 or wipe data to install. If the installed signature differs, STOP and do not uninstall; we must resolve migration without losing data.

## Remaining acceptance requirements

- Actual phone listening for three TTS announcements and piano/custom music balance in presence of audio focus, missed documents, lock screen, power saver, Done/Snooze and simultaneous alerts.
- Real exact-time/reboot recovery of hourly schedules with changing timezone and 30-minute intervals; Android OEM power policies can delay alerts. Hourly schedule is not guaranteed to ring at every exact clock slot.
- Test widget display on target Samsung launcher and other OEMs; direct pin API only requests placement, never auto-pins.
- Small phone and tablet text scaling, Personal Editor keyboard-safe footer, actual backup/restore/relaunch.
- Resolve prior Play Store policy gates and private signing separately; no automatic production merge.

This branch must remain isolated from main and hosted services until owner approves a stable release.
