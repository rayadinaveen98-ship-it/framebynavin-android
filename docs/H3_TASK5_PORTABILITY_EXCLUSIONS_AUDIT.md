# Backlot H3 Task 5 — Unified Backup Portability & Exclusions Audit

**Scope:** `framebynavin-android`, feature branch `feature/backlot-postcw6-reliability-unified-20261113`, audited on **2026-11-13**. This is a source-coverage audit, **not** a successful Android build, device test, or release authorization. Do not modify stable V336/CW6 or enable Restore All on the strength of this document.

## Decision

**Source audit completed. The existing Creator + Personal archive formats remain unchanged.** Preserve schema v10 Creator and v2 selected Personal. Do not add new data domains, secretly wipe external file references, treat external content-provider URIs as copied media, or normalize the exact ORIGINAL encrypted rollback escrow.

The existing `V341UnifiedBackupManager` exports an encrypted outer ZIP with **two independently verified inner parts**: `PortableCreatorBackupManager` and `V300PersonalArchiveSource`. Exports read these sources sequentially, **not** at one simultaneous app-wide transaction instant. The outer ZIP is authenticated with the owner's password and the Personal inner section has its own authenticated encryption. The app's original/target restore protocol must independently prove current state at commit.

## Verified source coverage matrix

| Domain | Canonical implementation | Export/restore coverage and limitations |
| --- | --- | --- |
| Creator projects, task workflow, content workspace, scripts, timeline | `CreatorBackupManager` v10 schema, `TaskStore`, `CreatorFocusSessionStore` | Records included. Imported project `workspace.assets.location` is **only a URI/text reference**, not source video, audio, attachment, or device access grant. |
| Creator ideas, weekly schedule, preferences, Smart configuration, post-publish and rewards | `CreatorBackupManager` 10 fixed included-section keys | Supported JSON included, schema and checksum validated; no provider credentials or alarm ledger bundled. |
| Creator YouTube links, milestones, publish checkpoints, Pulse and workflow timeline, automation preferences | `CreatorBackupManager` manifest sections | Owner-created project/workflow metadata included; OAuth credentials, active cloud session, remote account connection and derived analytics cache excluded. User must reconnect provider accounts. |
| Creator Focus sessions | `CreatorFocusSessionStore.exportPortableJson` | History included. A running session is exported as paused with its source-device elapsed realtime/boot anchor cleared; import also refuses a foreign running anchor. |
| Creator Voice Idea audio | `PortableBackupArchive`, `PortableCreatorBackupManager`, `V365`–`V373` | App-owned referenced media bytes packaged separately, mapped to Idea IDs and validated with SHA-256. Device-local output filename may change on import; original and imported bytes remain subject to H3 recovery proof. |
| Best Frames | `PortableBestFrameMedia`, `CreatorHeroArchive` | Ordered original image bytes packaged and checked separately, not just the inline metadata. Creator **cloud-only** snapshots may omit originals and are not a substitute for the portable/unified ZIP. |
| Creator custom alarm sound | `TaskStore.alarmSoundUri` and `CreatorBackupManager.BackupPreview.deviceOnlyAlarmSoundReferenceCount` | URI selection string may be retained in Creator JSON, but its audio bytes and SAF grant are **not** packaged. UI now warns and counts these references; owner should reselect on another device. |
| Personal reminders and exact Done history | `PersonalCompletionBackupCodec`, `PersonalBackupTransfer.exportForCloud` | Definitions, recurrence, explicit history and timestamps preserved. Selected portable snapshot resets live Android pending/presented/follow-up/snooze/auto-retry delivery leases. These are **not** a portable proof of pending alarms. |
| Personal custom audio | `PersonalBackupTransfer.exportForCloud`, `V382UnifiedPortableSelectedPolicy` | Selected Personal custom music is normalized to PIANO with blank URI; source-device music bytes/permissions are not bundled. The new verifier rejects a selected unified archive still carrying a custom document URI. |
| Personal legacy challenges, reminder groups | `V297ChallengeCodec`, `V298ReminderGroupCodec` | Both JSON sections included; links are verified against the included Personal IDs. |
| Personal 21-day single-activity challenges | `Phase1HabitCodec`, `V300EncryptedPersonalArchive` v2 | Definitions and exact per-day confirmed Done instants included. Unified **selected** archive with missing single-activity v2 section fails closed. No automatic creation of a missing 21-day record from notification history. |
| Personal Spaces, links, journals, explicit completion receipts | `V303SpaceReceiptArchiveCodec` | Space metadata, linked reminders and entries included and identity-validated. |
| Private Space JPEGs | `V300PersonalArchiveSource`, `V300EncryptedPersonalArchive` | Every tracked app-owned photo is encrypted in the Personal ZIP, checksum validated. Missing/untracked/orphan/partial files **block a complete export**; never silently omit or delete them. Original recovery photos must remain intact. |

## Exported vs excluded: exact semantics

`CreatorBackupManager` v10 fixed manifest declares:

- **Included Creator sections (15):** `tasks`, `ideas`, `weeklySchedule`, `settings`, `smartEscalationConfig`, `postPublish`, `rewards`, `personalFrames`, `youtubeProjectLinks`, `youtubeMilestones`, `youtubePublishCheckpoints`, `projectPulseHistory`, `workflowStageTimeline`, `automationPreferences`, `creatorFocusSessions`. (canonical JSON/manifest sections, not separate encrypted files).
- **Explicitly excluded Creator runtime sections (8):** `accountCredentials`, `oauthTokens`, `cloudSessions`, `youtubeAnalyticsCache`, `reminderOccurrenceTokens`, `alarmDeliveryLedger`, `activeSmartSessions`, `activeMediaServices`.

*Do not mistake exclusion of alarm runtime tokens for exclusion of reminder definitions or actual Done records.* A saved Creator alarm URI, workspace `content://` asset reference and YouTube project link are user metadata, not proof of transferable external binary content.

Additional device-only state not captured as owned media: Android exact-alarm and notification permissions, persisted SAF grants, OS notification channels, third-party login sessions, running services, unrelated local files, and playback on a device without the source media. Exclusion is a disclosure requirement, **not** a reason to reject an otherwise valid Creator record.

## H3 Task 5 source improvements

1. `V382UnifiedPortableSelectedPolicy` rejects any selected unified Personal snapshot containing live source-device leases, custom music URI, or a missing v2 single-activity challenge section. It is used by `V300PersonalArchiveSource.export` and during `V341UnifiedBackupManager.export/verify/inspectRestore` selected-file verification.
2. **Original recovery escrow is deliberately exempt.** Original escrow must preserve the owner's exact live state, including its last known delivery receipts, for rollback/fingerprint comparison. Never rewrite original ZIP bytes for portability.
3. `CreatorBackupManager.BackupPreview` counts externally referenced Creator custom alarm sound selections independently of unbundled workspace attachment references. `V341UnifiedBackupPanel` explicitly warns about both, without claiming the sound or attachment was copied.
4. Added pure `V382UnifiedPortableSelectedPolicyTest` for no-permission receipt, malformed/foreign lease denial, custom URI denial and complete 21-day domain; expanded CI-emulator-only `V209BackupCoverageTest` for source-device alarm reference count versus absent physical audio entry.
5. The archive formats, encryption and owner data were not migrated or reset.

## Failure handling and residual risks

- **Never auto-clean up** private Voice Idea or Space photo orphans created during an interrupted recovery. If original / target media cannot be proven byte-for-byte, final H3 verification must remain blocked for explicit owner review. A later selected export may also refuse these unknown private photo files. This is safer than a purported complete backup that silently loses them.
- **No automatic external-attachment relocation:** preserving a project URI string does not grant access to the original phone's file. Keep source footage and other third-party media backups separately.
- **Device reconfiguration required:** reauthenticate YouTube/cloud providers, grant Android reminder/notification permissions, and reselect custom sound documents where desired.
- **Release validation pending:** the large H3 recovery/release source (Tasks 1–4) and Task 5 improvements still require full Gradle compilation, fresh-device restores, interrupted-write/crash tests, permission handling, exact reminder/21-day delivery tests, and media SHA comparisons in Package J. No source commit is a certified working APK.

## Fixed next work (no extra stages)

- **H3 Task 5:** source audit and exclusion disclosures completed; Android test execution pending Package J.
- **Package I:** unified app-data delete/reset with verified non-destructive owner approval, clear scope and crash safety; do not erase encrypted owner escrow or personal media implicitly.
- **Package J:** compile, fix the integration, run emulator + physical device source/target and interrupted recovery QA, check release signing and only then deliver a verified integrated APK. **Keep Restore All disabled until that QA passes.**

**Source anchors:** `CreatorBackupManager.kt`, `PortableCreatorBackupManager.kt`, `PortableBackupArchive.kt`, `PortableBestFrameMedia.kt`, `V341UnifiedBackupManager.kt`, `V300PersonalArchiveSource.kt`, `V300EncryptedPersonalArchive.kt`, `PersonalBackupTransfer.kt`, `PersonalCompletionBackupCodec.kt`, `V303SpaceReceiptArchiveCodec.kt`, `Phase1HabitCodec.kt`, `V382UnifiedPortableSelectedPolicy.kt`, `V341UnifiedBackupPanel.kt`.
