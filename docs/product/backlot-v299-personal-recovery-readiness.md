# Backlot V299 — Personal Recovery Readiness (read-only)

**Status: development draft. Not a backup, not cloud sync, not an APK release.**

## What this actually does

Backup & Recovery now has one explicitly initiated **CHECK LOCAL BACKUP COVERAGE** action. It uses an isolated `V299PersonalRecoveryPreflight` scanner to count existing Personal reminders and completion receipts, challenges, Reminder Groups, Spaces, journal entries and Space links. It verifies the SHA-256 integrity of every **recorded private Space JPEG file** with bounded 8 KiB streaming reads (12 MiB maximum per JPEG), rather than trusting a database photo row as proof the bytes exist.

- Spaces SQLite is opened using `SQLiteDatabase.OPEN_READONLY`; the scanner never invokes Room.open or a migration.
- If the Spaces database does not exist, an existing private photo directory is an error, not an unexplained zero.
- v1 Spaces databases without photos are supported; v2 photo metadata is read and validated with existing policy. Each corresponding private photo file is verified against its recorded checksum.
- DataStore reads are validated by the existing stores. Corrupt or unsupported data yields an **Unavailable / unknown** result; never a fake green zero or automatic repair.
- Results clearly distinguish **locally present** from **cloud-covered**. Local groups, challenges, Space notes/links and private photo files are explicitly excluded from Sync Now.
- This is a *verification of local state*, not a verified backup copy or a full photo directory audit. Only database-referenced photo rows are checked, not unreferenced/orphan JPEGs.
- No personal titles, notes, images or content paths are put in logs or shown in the readiness summary.
- No account login or network access is needed. No settings, reminder, Creator, Space, photo or cloud data is mutated.

## Boundaries and remaining work

- The Personal Recovery scanner **does not export or import an archive**. It does not make cloud sync whole-app, verify a stored cloud snapshot, or prove a new device can restore everything.
- A complete portable, verifiable backup with restoration and collision-safe multi-domain sequencing must be designed and independently tested, including photo bytes; do not ship an export presented as recoverable until import is proven.
- This feature's purpose is to prevent destructive actions while verifying coverage before designing the complete recovery system.
- Existing V294 signed owner build remains frozen. No V299 APK is approved without exact-commit unit, synthetic Android 15, safe signed-upgrade and physical-device tests.

## QA

- `V299RecoveryCountsTest`: 5 tests for unverified photo counts, explicit unprotected domains and no false claims of cloud backup.
- `V288PersonalOsUiTest`: one extra emulator test ensuring the button is opt-in, no Personal reminder records/history change, and no Spaces database is created by the scan.
- `V294VisualAuditDeviceTest`: Android 15 screenshot #13 from the synthetic, explicit owner check.
- CI source allowlist rejects all changes to cloud sync implementation, Creator and reminder storage, alarm/notification delivery and Android manifest.
