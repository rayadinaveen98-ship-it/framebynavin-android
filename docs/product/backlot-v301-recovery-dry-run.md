# Backlot V301 — read-only encrypted Personal archive conflict preview

**DRAFT. Never replace or delete app data based on this report. No restore has been implemented.**

## Objective

Build the dry-run that should precede an explicitly approved, non-destructive restore: decrypt the **entire** selected encrypted V300 archive, authenticate all ZIP entries and JPEG bytes, read the existing phone's records, and compare all eight Personal domains by stable identity. The result must not show private titles, journal notes or full paths and must never change the phone.

## Implementation

- `V301RestoreDryRun` classifies incoming records in each domain as **new**, **identical**, or **conflicting** with an existing record having the same stable ID; it also counts records existing **only** on the phone. Domains: Personal reminders, actual Done receipts (reminder+occurrence), challenges, groups, Spaces, Space reminder links, journal entries and private photo records.
- Same ID with different data always causes a conflict, **never a silent overwrite**. An absent record on the archive does not mean the local record should be deleted.
- Incoming linked reminders must exist inside the authenticated archive; incoming photo metadata must reference a Space in the archive. Duplicate IDs and bad cross-domain references fail before producing a report.
- `V301LocalRecoveryReader` reads actual Personal, challenge and group DataStores; it opens the existing Spaces v1/v2 SQLite file **read-only**, never creates or migrates a missing Room database. It reads Space metadata, reminders links, journal rows, photos and checks the existing per-record photo bytes through the V299 preflight. It separately refuses missing or untracked/orphan photo files. If an existing Space contains data the V289 metadata codec cannot faithfully represent (for example confirmed-Done journal receipts), preview fails closed instead of hiding the records.
- `V301ArchiveConflictPreview.check` stages and authenticates the complete archive, calculates the conflict report against the local reader, and wipes temporary decrypted photo files on exit. It has no call to Room inserts, alarm code, Personal restore, Creator restore or cloud sync.
- All report fields are counts and generic explanations. Passwords and private file paths are not stored in reports or error messages. A developer or future UI caller must close its selected-file InputStream and clear the passphrase buffer.
- **`approvedToWrite` is always false**. Even a zero-conflict report is not permission to restore.

## Tests and boundaries

- Eleven unit tests cover empty target, same-ID/same-value, same-ID/different-value, phone-only records, exact Done receipt collisions, group collisions, missing local photo verification, photo metadata conflicts, orphan references, inconsistent diff arithmetic, and portable comparison of reminders with differing device-local alarm leases.
- One Android emulator check asserts the reader never changes Personal reminders or creates a Spaces database. Synthetic screenshot count remains 13; Settings/Personal UI test count rises to 26.
- Existing V295–V300 storage, notification, alarms, cloud sync, manifest, signing and Creator backup implementation remains unchanged. CI allows only the three new V301 files and the non-writing UI test.
- V300's encrypted archive itself is **not user-facing** and does not have an approved export or restore button.

## Remaining blockers for usable complete recovery

1. A real user-reviewed, transactional restore sequence: retain a verified pre-import recovery point; owner can preview conflicts and choose what to retain; never replace records silently; restore files and metadata safely across multiple stores.
2. A process-death/crash-testable journal and rollback for interrupted cross-DataStore/SQLite/photo operations; revalidate the current live revision at the moment of actual import.
3. A genuine owner-selected export document round-trip: persist, close, reopen, authenticate, verify ALL entries, then only claim "verified copy."
4. Preserve custom music/audio or clearly disclose exclusion; handle legacy confirmed Done Space journal receipts without inventing progress.
5. Signable owner QA APK, install-over-baseline migration and device-level reminder behavior tests.

**The V294 owner test APK is frozen. V301 is not Play Store ready and no restored data has been claimed.**
