# Backlot — durable restore write journal (stabilization only)

This completes a **non-writing** prerequisite of the existing full Personal recovery backlog. It is not a new app version and is not wired to an app screen, alarm, cloud restore or data mutation.

The private journal records opaque UUIDs of prepared incoming archive and authenticated local rollback escrow, original/target SHA-256 fingerprints and the current checkpoint. It contains no reminder titles, notes, passwords, photo bytes or custom audio links. Each update syncs its file, atomically renames and syncs its directory. Unfinished .pending files, malformed checksums and unrecognized files fail closed; owner approval is required to create a journal and an earlier journal is never overwritten automatically.

Phases bracket separate writes to Personal+Done, Challenges, Groups, Room Spaces and JPEGs; a fresh process sees recovery required from the first BEFORE stage. No live write is authorized and there is deliberately no completed restore phase. Twelve JVM and one disposable Android emulator regression tests cover interrupted, corrupt and duplicate journals, stage sequencing and restart semantics.

**Still blocking any Restore UI or stable release:** global mutation freeze, independently reauthenticated V300 archive and V305 escrow, live multi-DataStore/Room/JPEG writer, process-death recovery and rollback, full byte checksum reinspection, lossless-vs-portable media handling, local rearm, owner phone QA and Play signing migration. Portable archive does not preserve phone-local music URI/file bytes or alarm lease authority.

## Verified private JPEG installation prerequisite

`StabilizationVerifiedPhotoInstaller` accepts an already authenticated staged
photo and metadata through an **injected sandbox root**. It validates file UUID,
byte count and SHA-256, rejects symlink sources/destinations and cross-root stages,
streams into a fresh fsynced partial, then publishes via **CREATE_NEW** to reserve a destination that cannot
replace an existing JPEG (not a replacing rename). Android denied hard-link
creation in the first Android 15 test, so a verified partial is streamed to
the CREATE_NEW destination and fsynced before the partial is removed. Existing different photos are never
overwritten; exact existing bytes can be safely recognized as idempotent on
replay. An interrupted `.restore.pending` is preserved and prevents
automatic resume, so future owner-driven recovery must review it.

12 new JVM fixture tests and a second disposable Android emulator test are
required. The helper is not connected to a real app restore, Room, Personal
DataStore, media chooser, cloud sync or a Restore button. It does not fix the
missing cross-store global lock, transaction coordinator or rollback writer.
