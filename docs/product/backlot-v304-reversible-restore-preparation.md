# Backlot V304 — Reversible encrypted restore preparation

**DRAFT • NO live restore • NO owner APK release • no change to cloud Sync Now.**

## Why V304 exists

V300–V303 now authenticate an encrypted Personal archive, compare the archive against the phone, detect conflicting identities, and preserve verified Space Done receipts. Applying it would still require multiple independent mutations: Personal reminders + actual Done receipts (DataStore), challenges (DataStore), groups (DataStore), Space metadata and photo rows (SQLite/Room), private JPEG files and Android alarm leases. These cannot honestly be called one atomic transaction without tested recovery semantics.

V304 provides a bounded, private, **non-destructive PREPARE phase**. It is not a writer for any live data and does not mean that restoration is available. It can be invoked only after an explicit owner-requested preparation; no current screen invokes it.

## Implemented

- `V304RestorePlanPolicy` compares a locally verified Personal snapshot against a fully authenticated incoming V300 archive through V301's dry-run engine. A conflicting ID, corrupt/unverified local photo record or an absent owner request aborts before any new private data is stored.
- Both the current phone state and incoming archive are fingerprinted across Personal, challenges, groups, Spaces, and sorted photo metadata. The active-device fingerprint is rechecked before preparing and after copying; if Personal data changes during copying, the prepared copy is discarded, not activated.
- `V304RestorePreparationStore` creates a **random UUID private subdirectory separate from live photos**, with a journal that transitions `PREPARING` → `READY` using a checksum-verified, fsynced checkpoint. Only anonymous, bounded photo part names, sizes and SHA-256 checksums are included; owner titles, notes and account information are not stored in the journal.
- Each staged JPEG is streamed in 16 KiB chunks and verified against its previously authenticated SHA-256/size before the atomic per-file rename inside the *preparation directory*. The photo count is capped at 500, per-file bytes at 12 MiB and total at 500 MiB. An extra file, altered photo, damaged journal or interrupted `PREPARING` state **cannot be called READY**.
- `V304ArchivePreparationCoordinator` orchestrates local inventory, complete V300 archive authentication, zero-conflict planning, isolated copies, fresh local revision verification and reinspection. It never invokes existing restore methods, database inserts, alarm scheduling, cloud sync or live-photo installer.
- On a normal preparation error, isolated files are removed; on a process crash, incomplete private preparation can remain and must be explicitly reviewed or discarded. The read-only `inspectInventory` method counts ready, interrupted and unrecognized private preparations **without deleting files or exposing private filenames**. No automatic deletion of active photos or owner data is possible. An explicit `discardPreparation` method removes only a UUID-named directory under the private preparation root, not any live storage.
- The prepped JPEGs are *app-private but not independently encrypted at rest*, like existing Backlot photo-journal files; the user-selected ZIP remains AES-256-GCM encrypted. No progress or recovery success is claimed from preparation.

## Tests and gates

- **10 JVM tests:** consent requirement, valid private byte staging + cleanup, corrupted source, corrupted staged photo, extra files, tampered journal, changed live fingerprint, path-traversal protection, conflicting record rejection, and interrupted-preparation inventory that never deletes files.
- **1 Android 15 instrumentation test** on a disposable emulator: prepares a synthetic authenticated JPEG inside the isolated app cache, verifies and discards it, and asserts that real Personal reminder records and the presence/absence of the Spaces DB do not change.
- Existing source boundary allows only V304's new Personal code, while existing Creator, reminders/alarms, cloud engine, Room schema, manifest and signing remain frozen.
- V303 receipt-provenance and earlier V295–V302 tests are inherited; exact-head JVM, Android 15 and signed-upgrade tests must pass separately.

## Still required for a usable RESTORE button

1. Owner consent and exact conflict resolution per record (preserve phone-only records by default), including Space/photo ID collisions and existing Done receipts.
2. **Crash-safe live commit/rollback** across multiple DataStores, Room tables, photo byte paths and Android delivery, with a durable independent pre-import recovery copy and both interruption and replay tests.
3. Revalidate the phone's live revisions immediately before every commit; old dry-run/preparation results cannot authorize a later write.
4. Post-restore verification of every record/file, safe alarm rearming and full recovery from process death at each checkpoint.
5. Owner-device SAF export/import, signed in-place upgrade, representative journals/long photos, and final Play Store readiness review.

**This draft intentionally cannot call a prepared transaction "restored" or report a whole-app backup. V294 remains the frozen signed owner-test baseline.**
