# Backlot V305 — Independent, encrypted pre-commit rollback escrow

**DRAFT / NO LIVE RESTORE. This code cannot approve a commit, import data or roll back active Personal storage.**

## Why

V304 can stage an imported archive's JPEG bytes, but a multi-domain restore also needs an independently preserved copy of **what was already on the phone**, including existing notes, genuine Done receipts and JPEGs. Without that copy, a crash after changing the first DataStore but before SQLite/photos could cause permanent partial data loss.

V305 builds that independent recovery *copy* as a separate safety prerequisite, rather than falsely claiming that preparing new data is enough to roll it back.

## Scope of implementation

- `V305RollbackEscrowCoordinator`: requires explicit owner request and **reads all currently existing Personal data** with V301's read-only and photo-integrity preflight before writing anything. It creates an encrypted V300 archive from the phone's OWN existing Personal, challenges, groups, Space journal metadata, real Done receipts and JPEG bytes. It does **not** back up the incoming archive.
- `V305RollbackMatchPolicy`: reopens the newly generated encrypted archive and requires exact semantic identity with the original local snapshot for all eight V301 domains: reminders after normalization of nonportable device leases, actual Done receipts, challenges, Reminder Groups, Spaces, Space links, journal entries and photo metadata. The archive verifier also checks every private photo byte hash. No domain or phone-only record may be omitted.
- `V305RollbackEscrowStore`: stores a ZIP under app-private persistent `filesDir/backlot-personal-rollback-escrow-v1/<random-uuid>/archive.zip`, **not cache**, with an independently fsynced output and SHA-256 equality check between bytes actually emitted and bytes reopened on disk. ZIP payload uses V300 PBKDF2-derived AES-256-GCM with a new random salt and entry nonces.
- The escrow receives its own compact journal containing only random transaction ID, encrypted ZIP SHA-256/byte count and baseline data fingerprint. Owner notes, photo names, reminder titles, and passphrases never appear in the journal.
- The archive is authenticated twice, before sealing and after sealing; the phone is read again and checked against the original full snapshot. A changed live source aborts and discards the new copy, without touching live records.
- On normal failure, incomplete escrow creation is cleaned up; on a process crash, incomplete private folders remain isolated. The read-only `inventory` reports **sealed candidates** versus incomplete/unrecognized folders without deleting or decrypting them automatically. Only a password-verified reopen may call an escrow valid; a local checksum alone is not authentication.
- After a process death, an owner must **re-enter the recovery passphrase** to authenticate a saved escrow. If the passphrase is lost, this encrypted escrow cannot be recovered. App-private encryption protects content; it does not promise unattended or guaranteed automatic rollback.
- Even a password-verified escrow has `canWriteLiveOwnerData = false`. There is no code path to apply it to DataStore/Room, install photos or reschedule alarms.

## Tests

- 10 JVM tests verify exact byte survival and reopening, consent requirement, authentication failure, truncated writer cleanup, altered archived bytes, corrupt and extra journal files, crash-incomplete folders remaining for manual review, path traversal, missing local records, and unverified source refusal.
- 1 Android 15 emulator integration test exercises real Backlot Personal source export, AES archive authentication, wrong passphrase rejection, reopen after creation and explicit discard. It asserts that the Personal DataStore and Spaces DB presence remain unchanged.
- 6 additional pure barrier unit tests verify that the incoming V304 prepared archive and V305 encrypted old-data escrow match the SAME current phone baseline, refusing stale fingerprints, corrupt local integrity, partial preparations or invalid escrow metadata. A read-only `V305RestoreCommitBarrier` rereads current data, checks all staged photo hashes and authenticates the original ZIP with the owner's password; even success returns `authorizedToCommit = false`.
- CI source allowlist permits only these two new production Personal files, with all existing cloud, alarm, Creator storage, manifest and Room schema code frozen.

## Limits — not yet crash-safe COMMIT

- The ZIP file is written and fsynced and its content is authenticated, but Android filesystem behavior, **directory-entry fsync durability on abrupt power loss**, actual free-space exhaustion and backup retention on uninstall still need device-level checks.
- V305 uses an *owner-supplied passphrase*. Automatic rollback without owner intervention after a process death needs a separately protected device key or another well-tested, consented escrow recovery path.
- Existing original custom reminder song bytes and Creator data are still outside the Personal-only archive. No action must say *whole Backlot backed up*.
- A usable restore requires a durable write-ahead commit/rollback journal with transaction checkpoints, revalidation immediately before live writes, repair of partial multi-DataStore/Room/photo changes, version-safe alarm reconciliation and repeated crash/restart tests.
- V304/V305 are private safety building blocks; they are not wired into Personal Backup UI. V302 owner export and read-only preview remain the ONLY enabled ZIP actions.

**Do not merge, advertise or distribute as a signed restore-ready APK until exact-head CI, in-place upgrade, owner phone provider roundtrip and every restore checkpoint are independently proven. V294 remains frozen.**
