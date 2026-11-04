# Backlot V290 — Owner-Device Acceptance Gate

**Status: NOT Play Store ready.** This checklist is for an owner-selected controlled trial of the version 290 preview signed by the existing PUBLIC prototype certificate. No action is authorized on the owner's data by this document.

## 1. Before installing an owner preview
- [ ] Confirm installed app version and capture counts of Creator projects, Personal reminders and completion receipts.
- [ ] Create, store independently and verify Creator and Personal exports/backups. Verify the authenticated cloud account.
- [ ] If Spaces already exist, export the Spaces metadata JSON separately. Space goals/notes are NOT included in the current combined cloud sync.
- [ ] **Private photos are not included in JSON exports, Creator/Personal backups or cloud sync.** Preserve originals outside Backlot. Never uninstall or clear app data to upgrade.
- [ ] Confirm the candidate is the exact verified versionCode 290, application ID com.framebynavin.app, signed with the existing V287/V288 preview certificate.
- [ ] If important data lacks a verified recovery copy, test using a spare device or disposable profile rather than a primary phone.

## 2. Upgrade and legacy data checks
- [ ] Install V290 over V287 or V288, without uninstall, clear-data or reset.
- [ ] Confirm V290 opens as the same application, not a separate install.
- [ ] Cross-check Creator project status/notes, Personal reminder recurrence/time zone, snooze, music settings and completion records.
- [ ] Confirm the uncluttered Today screen and compact Personal Spaces shortcut.
- [ ] Stop on unexpected migration errors. **Do not downgrade in-place:** older Backlot builds may not read the migrated Spaces database.

## 3. Reminder and journal functionality
- [ ] Create a disposable reminder due soon. Test its notification arrives without tapping Backlot first.
- [ ] Exercise voice repeats, background music ducking, Done and Snooze with benign test reminders.
- [ ] Test a benign Alarm/Voice reminder with the screen locked, accounting for Android full-screen permission and notification restrictions.
- [ ] Create a Skincare Space; manually add a dated note, edit its goal, archive/unarchive, reopen, and confirm no Personal or Creator reminder was changed.
- [ ] Do not treat a missed reminder or a notification being shown as confirmed Done.

## 4. Photos and privacy
- [ ] Open system Photo Picker (no broad gallery permission); select a disposable image and review explicit save/privacy warning.
- [ ] Save with an optional note, reopen Backlot and verify the private copy loads; original gallery photo remains.
- [ ] Choose a second disposable photo and verify side-by-side comparison. Calendar-day spacing is not evidence of physical/medical improvement. Wait naturally for 14 days or rely on isolated emulator clock tests.
- [ ] Confirm private photo deletion needs explicit consent and never deletes the original image.
- [ ] Space deletion must be blocked while its private photos remain. Do not simulate app crashes or corrupt files on the primary device.
- [ ] Photos are app-private, not a fully encrypted/synced media vault; uninstall and clear-data can destroy copies.

## 5. Manual Spaces backup
- [ ] Read the plaintext JSON warning and select the destination personally.
- [ ] Export; inspect Space and journal counts. Photos, audio, and reminder definitions are excluded.
- [ ] Attempt import and merge only on a disposable test device/profile with known data and backups. Verify missing linked reminders or conflict prevents partial data writes.
- [ ] Never confuse Spaces metadata backup with complete Backlot backup.

## 6. Cloud and multi-device gaps
- [ ] Creator and Personal cloud syncing must be verified against the correct account with non-destructive conflict choices.
- [ ] **Spaces notes/goals and all private photo bytes are not included in existing one-tap cloud sync.** This release gate stays open until complete coverage is implemented and verified, or the product is explicitly limited to device-only local Spaces with prominent warnings.
- [ ] Perform cross-device restoration only with disposable verified fixtures; do not erase the sole primary copy to test restore.

## 7. Required release gates
- [ ] V290 signed owner-preview build, checksum/signing validation, all unit tests, Personal regression and Spaces/photo Android 15 instrumentation pass.
- [ ] Signed V288-to-V290 in-place upgrade preserves Creator data, Personal reminders and completion records.
- [ ] Physical-device reminder/photo/metadata backup checks pass.
- [ ] Media and Space backup limitations are resolved or consciously scoped with honest user warnings.
- [ ] Play Store release signing uses an owner-controlled PRIVATE upload key, not the repository's public prototype certificate.

For each defect, record app/Android/phone version, permission state, reproduction steps, expected vs actual and a redacted log. Never publish private photos, real reminder notes or account tokens in public GitHub issues.
