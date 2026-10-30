# Backlot V226 — Backup Safety Foundation (full-app engineering preview)

## Existing state and problem
- Creator portable backups already had a checksum-bound archive, ZIP media/Best Frames validation, atomic restore journal, and CI emulator tests for truncation, I/O errors, and post-image-commit process death. This phase deliberately **does not replace** that mature Creator format.
- Creator backup UI verifies the copy written through the Android document provider by reopening it. Some providers cannot immediately reopen a newly created document. Without the provider's actual error logs, we cannot conclude this explains the owner's past failed backup; a clearer actionable verification warning is now shown.
- Personal reminder JSON codec and DataStore read/last-good revision already existed, but there was **no end-to-end user-facing export/restore**. Importing records with a foreign active PendingIntent lease could produce stale notifications or misleading action identities. The single DataStore previous revision rotates away when the scheduler creates its next lease.

## V226 implemented
- Dedicated non-exported Personal Backup & Restore screen reachable from Backlot Settings → Backup & Restore. Creator backups, Drive Vault and YouTube never receive Personal titles, notes, occurrence tokens, or events.
- Separate Personal .json archive with the existing versioned schema + SHA-256 integrity checks, bounded 8 MB document-provider reading, explicit full-file read-back and byte-for-byte SHA-256 verification before claiming export success.
- Preview verifies all records and reports counts before approval; restores re-open/revalidate the selected file and ensure it has not changed since preview. Zero-record backups show a prominent destructive-replacement warning.
- Approved import changes only PersonalReminderStore in an atomic DataStore edit. Old PendingIntent / presented tokens and local snooze leases are cleared; previously acknowledged and missed occurrence journal timestamps are retained. Personal audio, notifications, and alarms for replaced IDs are stopped/cancelled best-effort; the Personal scheduler reconciles future reminders; new My Day widget refresh requested.
- **Durable undo**: before every approved Personal replacement, retain the EXACT previous Personal snapshot in an app-private, atomic, checksum-validated pre-restore recovery file. Keep up to five; expose a separate, user-confirmed recovery flow. The file is written within the DataStore mutation before committing the new primary; failure aborts replacement. This avoids losing the old data when the scheduler later rotates DataStore's single last-good revision.
- Clear post-commit warning on partial cleanup/scheduling failure; never claim the entire restore failed once data replacement was committed.
- Existing Creator Settings backup error copy distinguishes cloud/local storage-provider readback from corrupted backup and guides the user to a local Files/Downloads provider. It still **never** labels an unverified export as successful.
- Full app versionCode 248 / V226, in-place developer-signed upgrade from V225 247 (prototype signer; NOT Play Store production signing).

## Safety boundaries
- Personal archive is **not encrypted**. UI explicitly warns the user to store it privately.
- Do not automatically replace Personal reminders as a side effect of Creator restore, cloud sync, account sign-in or app upgrade.
- Do not silently restore one previous revision when the current primary is corrupted: require explicit owner choice.
- Device-specific alarm leases are never trusted after import, even if importing onto the same phone.
- During a crash after atomic DataStore commit but before re-arming, the updated primary persists and the old pre-restore copy remains. Existing boot/startup/hourly Personal reconcile can re-arm later; old action tokens fail lease validation.

## CI acceptance
- JVM: clean device leases, idempotent reset, Unicode and oversize SAF source.
- Android disposable API 30 emulator: preview, imported lease invalidation, real Personal DataStore restore + explicit undo, CreatorTask store stays bit-for-bit unchanged, malformed/cross-domain data rejected without mutation.
- Existing V225 22 Android checks: Creator recovery isolation, V224.1 widgets, Personal recovery and notifications, UI, Creator Projects cold start.
- Creator backup real corruption/short-write tests from V210; V209 archive coverage (all executed on disposable CI emulator only).
- Gradle unit suite, lint, debug/instrumentation APK and full personalStable APK; check exact package/versionCode and prototype signer against V225.

## Still pending
- Confirm the owner's actual Android backup error using in-app reproduce and storage provider diagnostic context. Never claim its root cause from source code alone.
- Physical phone SAF (Files, Google Drive, OEM Files), large personal data, reminder re-arm across reboot, battery policy, native restore and encrypted export decisions.
- Production APK/AAB signing, Play permission declarations, backup support, privacy language. PR stays draft until tested.
