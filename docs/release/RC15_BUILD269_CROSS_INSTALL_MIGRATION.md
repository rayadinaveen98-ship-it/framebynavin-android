# Backlot RC15 — Cross-install backup migration rehearsal

**Date:** 30 Oct 2026. **Scope:** synthetic-data CI rehearsal, not a user-device migration or production key decision.

## What is actually tested

One clean CI Android 11 emulator exports a synthetic Creator project (title, script, editing progress, external file URI) plus an original 2x2 PNG Best Frame and a separately created paused Personal Reminder with a note. The actual Backlot portable Creator ZIP and the separate Personal JSON are written by the app. The test checks export validation and records SHA-256 hashes.

CI copies the two synthetic archives through the runner *without uploading them publicly*. A clean Android 16 emulator installs the same application source in a separate Android installation. CI injects the exact bytes into its app-private test fixture directory; a second emulator-gated instrumentation test verifies the hashes, preview, full Creator restore and isolated Personal restore, all key fields, original frame, URI-only external asset, paused Personal schedule and no imported action tokens.

No real user backup, Google account, YouTube token, calendar, voice recording or cloud service is contacted. A dummy content URI proves that **external media file bytes are not carried** and must be transferred separately. The paused Personal reminder never schedules a real notification.

## Precise limits

- This confirms **data portability across independent fresh installations**, not install-time compatibility across differing Android signing certificates. The recipient build is signed for QA, and real Play signing has not been selected.
- No owner physical-device acceptance, OEM alarm reliability or genuine owner backups are tested.
- A clean emulator is intentionally used on both sides; **never uninstall or wipe a real Backlot** to imitate this.
- No production Android app source, manifest, version or signing config changed; the one new Kotlin file is in `app/src/androidTest`, and the workflow builds test APKs exclusively for CI.
- User approval, protected main, Play signing choice, separate export verification, and Play policies are unresolved: #174, #180, #185, #187.

## Acceptance

The workflow must pass exact source-scope checks, Gradle compile/lint/test packaging, Android 11 sender, host SHA-256 verification, and Android 16 recipient restore. A partial result is not green.

## Next steps outside CI

1. On owner's phone, **keep Backlot installed**; export/verify both Creator and Personal backups separately, plus all linked source footage.
2. Select production signing and package ID explicitly before Play builds; prefer a side-by-side app path if the prototype signer cannot be upgraded in place.
3. Test migration on a disposable real device/profile using copies of the verified exports before any change on the owner's device.
4. Confirm physical notifications, alarm/voice screens, startup persistence, and restore UX. Never claim app deletion/Play compliance until the backend and public policy pages genuinely exist.
