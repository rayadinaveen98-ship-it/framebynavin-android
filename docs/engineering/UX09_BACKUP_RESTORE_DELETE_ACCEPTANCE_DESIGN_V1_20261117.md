# Backlot UX09 — Sync vs Backup vs Restore vs Delete: founder acceptance design

**Date:** 2026-11-17. **PROPOSED**, not Android implementation, hosted database migration, cloud operation, APK or production activation.
**Founder direction:** Fresh Lab account, no old Supabase migration. No silent cloud/local erasure. Existing Settings Backup/Restore controls previously removed by founder MUST NOT silently return.
**Live source baseline:** owner-tested Lab V337 SHA `4ba1263124d118d3917f988228aec2248068466f`. UX01–08/UX10F source is separately staged, not yet integrated.

## Four independent product guarantees

| Feature | User promise | Critical acceptance |
|---|---|---|
| **1. Automatic Sync** | On a second Google-signed-in device, supported account data converges when online, with offline local work preserved | Per-record revisions, durable outbox, account fencing, conflict handling, tombstones and truthful per-domain status; no automatic loss or cross-account overwrite |
| **2. Account Cloud Backup** | A recoverable account-owned copy exists independently of the first phone | Exact coverage manifest, versioned source evidence, integrity/hash on server read-back, optional private encrypted media, retention/recovery policy; not just successful apply/pull receipts |
| **3. Guided Restore/Recovery** | A new install can safely recover all *covered* data after login, without destroying local work | Identity re-check, backup preview, domain count/diff, conflict review, schema compatibility, atomic local apply or rollback escrow, device-local alarm re-authorization and human-verifiable result |
| **4. Data & Account Deletion** | The user can erase local data, clear cloud data, or close Backlot account as three different intentional actions | Scoped confirmation/reauthentication, durable deletion receipt, verified table and private Storage cleanup, Auth identity termination and JWT/session handling; no Google account deletion |

## 1. Synchronization

- Default: local write immediately; durable pending queue; sync on eligible connectivity, foreground and bounded scheduled worker. This is best-effort latency, not guaranteed real-time.
- Same-account records in two devices reconcile by entity ID/revision; on simultaneous conflict, show explicit comparison/selection. Server op-id prevents duplicate writes and deletion tombstone prevents offline resurrection.
- Supported-domain states appear separately; Creator success does NOT turn a failed Personal, photo or Challenge recovery into a green overall badge.
- Device alarm permission/OS scheduler, custom `content://` music and lock-screen playback remain local. On new device, generate local schedules from approved synced reminder definitions after permission check; never sync PendingIntents.

## 2. Backup (must be proven separately from Sync)

- Scope manifest: Projects, Ideas, Creator tasks/stages/content, Creator Reminders, Personal Reminders/completions, Reminder Groups, single-activity Challenge and exact Done receipts, My Spaces/logs/notes/routines, relevant event/history IDs; record explicit excluded fields and device-only settings.
- Snapshot/changes must support **recovering a lost phone**, not only resolving conflicts while the original is online. The candidate design chooses an encrypted, versioned backup/recovery manifest or equally auditable reconstructable account history, retention and checksum. The exact server media/storage model and recovery passphrase flow require review, NOT selected in UX09-01.
- Private Photo Journal, Best Frame media, original Voice Idea recordings and custom music need **explicit user opt-in and separate secure storage/cost decisions**. Don't upload photos just because the account is signed in; don't call backup "complete" if private media excluded.
- A returned HTTP 200 is not sufficient: verify server revision, integrity and actual recovery of a synthetic fixture. Automatic status: `Saved on this phone`, `Sync pending`, `Some data synced`, `Backup recovery verified`, or `Needs review`. Display source scope, last successful verified recovery point.
- Never disable `PersonalCloudIncompleteChallengeCoverage` until the new Challenge receipt coverage and restore have been proven. The Lab V337 existing snapshot remains an optional limited format during transition, not a complete backup.

## 3. Restore

- Suggested UX: First launch -> Google sign-in -> detect covered remote state -> **Recover from Backlot** with real metadata/date/count preview, `Start fresh` alternative and `Review conflicts` when this device already has local work. Existing legacy Backup/Restore Settings tiles remain absent.
- Perform a read-only dry run: account ID and integrity checks, schema/version/size/manifest checks, cross-record references, local vs remote diff and unsupported media disclosures.
- Only after explicit review, apply in a resilient local transaction/checkpoint with rollback escrow and idempotent restore ID. Preserve all valid local data unless explicitly included in a reviewed replacement.
- Challenge Done proof, record revision/tombstone and logged creator achievement must not be fabricated on import. Exact alarm and notification permissions must be rechecked and scheduled on THIS phone; custom device URI may require reselection.
- Result page: restored counts by domain, excluded media, pending downloads, permission warnings, errors with retry/resume. Sign-in success alone is NOT restore confirmation.

## 4. Deletion must have three clear meanings

**A. Erase local device data (ALREADY EXISTS):** Advanced -> Start fresh on this phone -> explicit confirmation -> Android app-private data clearing. Cloud/Backlot Auth account remains. Keep this functionality untouched.

**B. Delete cloud data (NOT IMPLEMENTED):** Authenticated owner requests server-side removal of all sync entities/change logs, backup revisions/history, media manifests/storage objects, Creator+Personal old snapshots, app-scoped account profile metadata; account can remain active if founder chooses. Repeated requests idempotent. Device A/B queued old operations must not repopulate cloud: server-level per-account generation/deletion fence and clients must drop/review obsolete outboxes.

**C. Delete Backlot account (NOT IMPLEMENTED):** After recent reauthentication and explicit irreversible confirmation, a server-held privileged operation deletes Backlot-owned tables, backup history and approved Storage objects, revokes or invalidates sessions using appropriate Auth mechanisms, then removes Supabase Auth identity via an admin-only interface. **No service_role/secret on Android, no pretending `delete_creator_sync_data` means account closure.** Handle stale access JWTs and asynchronous object delete reconciliation. Do not delete the user's Google account. A validated retention policy must explain any narrow legal/security exceptions.

### Proposed server execution boundary

- Evaluate a tightly scoped backend deletion handler with admin privilege isolated from Android (Supabase Edge Function or existing trusted server only after price/security approval). This is a future architecture decision, NOT implicit permission to introduce paid services.
- Use short-lived server-side deletion request ID, per-account authorization, idempotency key, final proof/audit receipt free of deleted content; reconcile storage object failures before claiming complete deletion.
- For login after deletion: do not restore old outbox or cached private records automatically. Confirm account generation and user intent to start a new account.
- Delete operations are irreversible; auth schema cascades must be reviewed for all new tables, including revision changes, tombstones and backup/voice storage. Account Auth deletion alone is not adequate evidence that Storage bytes were removed.

## 5. Separate implementation acceptance stages

| Stage | Deliverable | State |
|---|---|---|
| UX09-01 | Disposable account-scoped Creator Project/Idea CAS + tombstone + delta contract | Source-only, untested |
| UX09-02 | Android durable outbox + Creator/Idea adapter and two-phone testing | Pending |
| UX09-03/04 | Reminders, Groups, Challenge receipts, Spaces, all approved Personal and Creator state | Pending |
| UX09-05A | Backup coverage manifest + encrypted/reconstructable account recovery | Pending |
| UX09-05B | Guided verified new-device restore and rollback | Pending |
| UX09-06A | Fully scoped cloud data deletion and anti-resurrection account fence | Pending |
| UX09-06B | Verified Backlot Auth account closure + media cleanup, optional identity re-verification | Pending |
| UX09-07 | Live full coverage, concurrency, RLS, offline, two-device and Android alarm acceptance | Pending |
| Final | Lab V337 P0+all UX integration, build/QA APK, founder device approval, release | Pending |

**Founder decision gates still open:** covered sensitive media (upload opt-in vs local-only), account removal confirmation/reauth UX, offline sync latency, conflict resolution, backup retention and any new service cost. No sensitive media automatically synced by this document.

**Stop-the-line:** backup success without full source-scope manifest and restore proof; account delete button that only logs out; orphaned private Storage; post-delete old client resurrection; silent conflict overwrite; wrong-account data exposure; resumed OS alarms without local permission; missing V337 protected P0.
