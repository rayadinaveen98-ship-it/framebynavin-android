# Backlot V291 — Controlled Owner-Phone Acceptance

**Status: DRAFT. NOT a Play Store release, not permission to erase owner data.**  
Use only after integrated V291 and signed-owner Android 15 workflows have **both passed**. V291's owner-preview signing certificate is publicly checked into this development repo, so it is strictly for sideload testing, NOT Play signing.

## Before any install

- [ ] Preserve the current working V287 APK, source/versionCode, app signing certificate, and full Creator/Personal counts. Do not assume the user's installed version is V287 without checking.
- [ ] Make **independent**, restorable Creator and Personal backups. Validate contents and timestamps, not only a green Sync Now badge. Personal reminder permissions and local file-based music do not automatically transfer.
- [ ] If the current build already has Personal Spaces: export **plaintext metadata-only JSON** into a user-chosen destination, then reopen/read the file. This does **not** contain private JPEG bytes.
- [ ] If there are private journal photos, retain each original outside Backlot. **No current combined backup preserves private journal photo bytes**, even when metadata succeeds. Never uninstall/clear app data if it contains the only copy.
- [ ] Confirm the artifact was produced by a **successful** V291 full build and signed-owner regression workflow, not merely a successful compile job.
- [ ] Verify application id `com.framebynavin.app`, versionCode **291** and V2 owner-preview certificate fingerprint `f0ddec789172ec9f83a037a95b14e90293126f61936d8bab056d3a57e0d472a0`; verify the supplied APK SHA256 against the workflow's checksum file.
- [ ] If any important data has no independently verified recovery path, use a disposable spare device/profile rather than the primary phone.

## V287/V288/V290 → V291 signed in-place update

- [ ] Use Android's **in-place app update** with matching signer; do **not** uninstall, clear data, reset, or wipe settings first.
- [ ] Confirm app opens with the **same existing** Creator projects and Personal reminders. Check counts and selected notes, not just the home screen.
- [ ] Verify Personal recurrence, time zone, snooze length, reminder type, custom sounds/music visibility, three voice announcements, and previously confirmed Done history.
- [ ] Verify one disposable reminder with phone unlocked and then locked/screen off; cover notification permission, exact-alarm permission, full-screen eligibility and Android/OEM restrictions. Full-screen alerts may require user-granted permissions; do not misreport a heads-up as full-screen.
- [ ] Verify existing Spaces, archived state, journal entries and previously stored private photos remain visible. Compare checksum-verified photo opening where possible.
- [ ] Stop and document errors if any of the above differs. **Do not downgrade via Android install -r** if V291 has migrated local Room data; an older build may not understand the new schema.

## Photo regression and privacy

- [ ] Select a deliberately disposable portrait photo with EXIF 90° orientation from Android's system Photo Picker. Verify it displays upright in the app after saving and reopening.
- [ ] Check another 180° rotated photo; verify pixel orientation, not only an optional EXIF orientation flag.
- [ ] Confirm app requests only the chosen photo and stores a separate app-private JPEG; source gallery item is unchanged.
- [ ] Check the imported copy contains no GPS/camera make/model metadata, and names/paths are not those of the original image.
- [ ] Save note and baseline image, reopen journal, add a later disposable image and review side-by-side comparison. **14 calendar days is a timeline criterion, not evidence of health/appearance improvement**.
- [ ] Confirm delete asks before erasing **only** Backlot's private copy and cannot silently remove the original gallery item or a linked reminder.
- [ ] Do not perform corruption/crash/migration fault injection on the primary phone: those are emulator-only tests.

## Backup honesty and second-device recovery

- [ ] Confirm Cloud UI says **Creator + Personal cloud sync** and accurately explains excluded Spaces/check-in notes/private photo bytes/original Best Frame photos/custom local music.
- [ ] On a spare device/profile, exercise supported Creator+Personal backup/restore without clearing the only primary copy. Check non-destructive account conflict handling.
- [ ] Export Spaces metadata to a chosen document, reopen and verify JSON, then import only on a disposable device/profile. Verify notes and counts; do not assume photo recovery.
- [ ] Confirm private photos are **not** transferred by this action. Store originals separately until a future verified media backup/restore exists.
- [ ] Confirm two-device permissions and voice/music URI reconfiguration requirements are clearly stated.

## Completion / remaining blockers

- [ ] V291 integrated workflow green, including all personal/UI/Space/photo/EXIF tests on Android 15.
- [ ] V291 signed personalStable workflow green, including the matching-ABI 20-domain-test gate.
- [x] Synthetic signed V288→V291 and V290→V291 upgrade CI [41875132585](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/41875132585) **passed November 4**.
- [x] Synthetic signed direct V287→V291 upgrade CI [41876779772](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/41876779772) **passed November 4**.
- [ ] Real owner-device acceptance above signed by the tester. Keep screenshots/logs redacted (never publish photos, notes, accounts, tokens).
- [ ] Product decision on missing complete Spaces/photo backup: implement and verify it before claiming one-tap full Backlot recovery; otherwise explicitly ship only a **device-local** Spaces feature with prominent disclosure.
- [ ] Private owner-controlled Play upload key created **outside** the repo; never use the public preview signing identity for Play.
- [ ] Play listing disclosures, notification/full-screen eligibility, Android 15/16 compatibility and final signed release bundle checks completed.

**No build should be labeled stable, publish-ready, or safe for a primary phone based only on this checklist.**
