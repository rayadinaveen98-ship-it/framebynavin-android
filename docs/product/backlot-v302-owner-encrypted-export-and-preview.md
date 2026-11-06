# Backlot V302 — Owner-initiated encrypted Personal export and no-write preview

**Status: draft / development only. Not a release-approved APK and NOT a restored-data guarantee.**

## Purpose and entry point

In the existing `PersonalBackupActivity`, V302 adds one compact **Encrypted Personal Archive** panel below the existing Personal reminder-only recovery options. The owner explicitly enters an archive passphrase (minimum 12 characters), then selects one of two Android Storage Access Framework actions:

- **EXPORT & VERIFY FILE** — choose a destination document (ZIP), export a new Personal-only encrypted V300 archive, close the output, reopen and SHA-256 check **every saved byte against the digest of the actual written ZIP**, then reopen and authenticate the complete encrypted archive including all domain checksums and private photo JPEG hashes. Only after ALL checks pass can the UI say the encrypted Personal copy was saved and verified. On any failure, warn that a partial file may exist at the chosen destination; existing records are not changed.
- **CHECK A FILE — NO RESTORE** — choose an existing encrypted ZIP, decrypt/authenticate it, inspect current local Personal records using V301 read-only inventory, and show new/identical/conflicting/phone-only record counts. No file or app record is written, merged, replaced, rescheduled or deleted.

The passphrase is not stored in DataStore, a saved-instance Bundle, an account, cloud, or the archive. It is kept only in ephemeral UI state while a picker/export is active and explicitly zeroed when the callback completes, the picker is cancelled, the screen is disposed or the document picker fails. Memory-managed Kotlin strings cannot guarantee secure physical erasure; do not enter an account password. A forgotten passphrase cannot be recovered.

## Exact scope / limitations

Encrypted archive contents: Personal reminder definitions and real Done history, owner-defined 21-day challenges, Reminder Groups, Personal Spaces metadata, Space notes and journal entries that the existing codec can represent, Space reminder links, and recorded app-private photo-journal JPEG bytes. The V300 archive uses per-entry authenticated AES-256-GCM with a PBKDF2-derived key and explicit bounds. The outer ZIP format may reveal size and part count but not private filenames and record titles.

**This is not one-tap all-app backup.** Creator projects, ideas, Voice Idea recordings, original Best Frames, cloud access tokens and custom reminder songs remain outside the new Personal archive. The existing Creator portable backup and Personal reminder-only JSON recovery are unchanged. Existing Sync Now coverage has not expanded. The V289 Spaces metadata format intentionally fails rather than silently omitting unsupported confirmed-Done Space journal entries. An untracked/missing private JPEG, corrupt file, stale linked reminder or changed/corrupt datastore aborts the attempted export or preview.

A reopened SAF document can be slow or unavailable on some providers. V302 reopens the original exported document twice: first as a bounded complete ZIP byte-for-byte integrity check, then for full authenticated archive verification. A document provider that refuses reopening causes a failure message rather than false success. The saved file remains the owner's responsibility; partial documents must be deleted manually at the chosen location. Neither the preview nor the verified export means that a new phone can restore the bundle: **no V300/V301 restore writer is enabled**.

## Verification

- `V302OwnerArchiveFingerprintTest`: 3 tests for exact saved-file integrity, truncated/modified provider writes and empty/oversized saved files.
- `V288PersonalOsUiTest`: three additional Android 15 emulator tests for password-gated actions, no automatic import, and an encrypted export/authenticated preview roundtrip from the actual isolated Personal stores without changing reminders.
- `V294VisualAuditDeviceTest`: one additional synthetic screenshot of the blank encrypted archive panel and its disabled controls, bringing totals to 14 visual tests, 29 Settings/Personal UI tests.
- Full existing unit/data preservation, source allowlist and Android 15 UI checks continue.
- Device acceptance still needs an actual SAF provider roundtrip, long/private photo journals, unavailable provider, wrong password, existing local conflicts, process interruption, storage exhaustion and signed in-place upgrade from V294.

No V302 test code or screenshot uses owner data, real passwords, cloud login or physical device photos. The frozen signed V294 owner preview remains untouched.

## Remaining release blocker

There is **NO import/restore action**. Before a usable restore, we must prove a crash-safe cross-DataStore/Room/photo write protocol, ownership/conflict resolution, rollback, and post-restore reminder reconciliation. In particular, no UI should say “fully backed up” or “restore available” for the entire Backlot app. The V302 review PR is deliberately draft.
