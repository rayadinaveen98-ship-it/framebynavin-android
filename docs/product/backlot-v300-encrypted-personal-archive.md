# Backlot V300 — encrypted Personal portable archive foundation

**Status:** draft code, no owner-facing export/import action, no restore writes, no verified APK.

## Actual implementation

- `V300EncryptedPersonalArchive` is a bounded ZIP-container codec with a separate PBKDF2-HMAC-SHA256-derived **AES-256-GCM authenticated encrypted part for every entry**. Passphrase minimum 12 characters; 210,000 PBKDF2 iterations; fresh random salt and 96-bit nonce per encrypted entry. Manifest and all private content remain inside encrypted generic entries, including titles, notes, IDs and media filenames. The outer header contains only format/version, iteration count and random salt. ZIP entry count and rough sizes may remain observable.
- The encrypted manifest contains independently verified portable Personal reminders and completion receipts (using the existing portable Personal cloud codec), custom 21-day challenges, custom Reminder Groups, and Spaces metadata including journal notes and reminder links. The importer validates each existing domain codec and refuses dangling Space/group reminder references.
- Private photo-journal **JPEG bytes** are written as distinct authenticated entries with bounded reads and SHA-256 validation against existing photo metadata. Photo records must belong to Spaces in the same archive. No absolute source paths or original provider URIs are exported.
- A validation-only `stageAndVerify` reads every encrypted entry, checks sizes, hashes, order, passphrase authentication, inner domain integrity and photo metadata, and stages photo files in app-private cache. On any failure, the stage is removed; it NEVER updates Room, DataStore, Android alarms, Creator records, cloud or permanent photos. The calling code MUST close the verified stage to wipe its temporary files.
- `V300PersonalArchiveSource` can read current app Personal DataStore, challenge/group stores and existing Spaces/photos for explicit export; no Spaces database is created when none existed. It offers a verify-and-preview summary; it does not perform restore or present an export UI.
- Limits: 500 photo files, each up to 12 MiB; total recorded photo bytes <=500 MiB; manifest <=24 MiB; no unlimited Base64 or whole-archive decryption buffer. Individual entries are authenticated independently to avoid buffering an entire multi-hundred-MB encrypted archive at once.
- Nine focused unit tests verify a successful encrypted multi-domain/photo roundtrip, absence of plaintext notes, wrong passphrase, tampering, missing required entries, unexpected ZIP entries, missing linked reminders, changed source JPEGs, empty photo sets and short-passphrase rejection.
- `V300RestoreReadinessPolicy` is a non-writing conflict gate. A destination with any existing Personal, challenge, group, Space, journal or photo records cannot be automatically overwritten. Missing/corrupt existing photo bytes block recovery; an empty target is only a candidate, **never an authorization to write**. Five additional tests enforce this. The exporter also rejects canonical-path escapes for private photo files.

## Important exclusions / release gates

This **is not yet a recoverable owner backup feature**. There is no UI to export, no verified file-provider write/reopen cycle, no owner-approved import/merge or photo restoration, and no signed release. Do not remove the old Backlot backup feature or describe any new file as verified recoverable until a complete end-to-end restore passes.

- **Creator projects, ideas, original Best Frames, Voice Idea recordings, and music files** are not in this Personal bundle. The existing Creator portable backup remains separate.
- Personal records are exported using the portable cloud sanitizer: device alarm leases and nonportable custom music paths are dropped; on restore, device permissions and alerts must be reconciled. Custom song bytes are not exported.
- V289 Spaces metadata codec currently refuses confirmed-Done Space journal receipts (rather than forging progress); export fails closed if existing Space metadata cannot be represented exactly. This needs a separately verified, backward-compatible schema before claiming full recovery.
- A missing photo, **untracked/orphan/partially saved private photo file**, or stale dangling group/Space link fails export rather than silently producing an incomplete archive. The exporter compares canonical private photo paths against all existing on-disk files, refusing silent omissions and never showing private paths in errors.
- A password cannot be recovered. Loss of the passphrase renders an archive unreadable; the user must explicitly understand this before a future UI is exposed.
- The temporary decoded JPEGs are in private app cache, **not encrypted at rest while staged**; they are removed on verification/preview exit. Later restore flow must use tightly controlled lifetimes.

## Next required gates

1. Exact-head full JVM unit suite and build; Android 15 synthetic UI/visual regression (includes all prior phases).
2. Add a user-consented export flow with file reopening and exact byte verification, coupled to a tested import/restore preview.
3. Develop non-destructive transactional restore ordering across Personal, challenge/group DataStores, Spaces metadata and private photo files. Test ID collisions, aborted import, process interruption, existing owner data, and rollback.
4. Only then consider combining the separately validated Creator portable archive, or enabling a complete one-action backup.

The V294 owner QA APK remains frozen. V300 is a stacked **draft** on V299 (#297), V298 (#296), V297 (#295), V296 (#294) and V295 (#293).
