# Backlot Trust Foundation T — execution and coverage register (9 November 2026)

**Owner direction:** start the next locked phase after Phase 3; do not skip the remaining Phase 3F integrated Android/owner acceptance checks. This document describes evidence, not an owner-approved release. Implement on isolated review branches. No device data reset, auto restore, or production signing change.

## Sequence
1. **T1 universal backup:** a single explicit **Back Up** action with a verified, encrypted, owner-scoped complete snapshot and honest coverage manifest. Keep the previous good cloud revision until the next is durably verified.
2. **T2 reviewed restore:** authenticated snapshot inventory, source account/date, per-domain conflicts, validated bytes, pre-restore recovery escrow, deliberate owner approval, and crash-safe commit. No automatic destructive merge.
3. **T3 delete cloud backup:** delete only selected remote Backlot copies and metadata after server confirmation, leaving local records intact.
4. **T4 Backlot account deletion:** separate authenticated deletion flow with verifiable server/local order, partial-failure recovery and correct external-token claims.

## Coverage reality — do NOT label current cloud sync "universal"

| Owner domain | Existing verified/review code path | Current trust gap |
| --- | --- | --- |
| Creator tasks, Ideas, scripts | Creator cloud/backup and existing workspace stores | Not unified with Personal encrypted photo archive, history, Focus and owner restoration |
| Focus 2.0 | Creator backup versioned scope on Phase 3 branches | Needs full inventory/recovery equivalence and a live two-device proof |
| Personal reminders, Done receipts | `PersonalBackupTransfer.exportForCloud` and v2 history codec | Older cloud path is Personal-only, not a universal archive |
| 21-day ONE-activity challenges | `Phase1HabitCodec` + `Phase1HabitStore`; encrypted Personal ZIP **v2** source and verification preview | Old `PersonalCloudSyncManager` still hard-blocks local challenge data; no live cross-device challenge restore transaction |
| Legacy V297 planner | V297 codec + encrypted ZIP | Keep separate from the new one-activity store, including all old progress |
| Reminder Groups | V298 codec + encrypted ZIP | Not covered by old Personal Sync Now; links must be checked before restore |
| Spaces, journals, links, private JPEGs | V300 encrypted ZIP with staged AES-GCM photo byte integrity verification | Not covered by old Personal Sync Now; no approved cloud photo revision/restore |
| Alarm device leases / custom local music URI / Android permissions | Device-specific; stripped from portable reminder export | Explicit exclusions and post-owner-consent reschedule required |
| Spark and other configuration, notification settings, media, account identity | Mixed stores | Discover every source, manifest and restore policy before declaring universal coverage |

## T1A — legacy remote-downgrade safety: implemented in review branch

The existing legacy Personal sync path **must not overwrite a remotely stored richer v2 cloud bundle**, even when the current phone has zero habits. That would delete the newer cloud bundle's challenges and exact Done timestamps.

- `PersonalCloudLegacyRemoteGate.requireLegacyOnly` decodes and validates the supported remote format, then rejects *any* new v2 bundle with a precise non-success outcome before a write, replacement, reconciliation success, or keep-phone override.
- Check at **Sync Now**, **Keep This Phone**, **Use Cloud Copy/restore**, and cloud conflict preview boundaries. Hash verification remains mandatory; unsupported and malformed formats fail closed.
- Preserve the existing guard that refuses **outgoing legacy Personal sync when local one-activity challenges exist**.
- Tests: old legacy compatibility; valid and corrupt new-format rejection; unchanged exact walking receipt; Android DataStore non-mutation; encrypted v2 ZIP receipt coverage.
- This does **not** enable remote v2 restore, does not change cloud API endpoints and does not present a "full backup" success UI.

## T1B/T1C — next implementation gates

**T1B inventory and verified snapshot manifest**
- Enumerate *all* persisted domains from live stores, distinguish zero records from unreadable/missing data and unknown versions; refuse partial "complete" snapshots.
- Determine cloud ownership/backend object model, account identity, manifest version, timestamp, per-domain byte hashes/counts, encryption key lifecycle, old revision/rollback guarantees, retention and data budget.
- Include photo bytes and voice/media scope or show explicit unsupported backup exclusions. Single archive/export and Creator backup cannot silently masquerade as one transaction.
- Use stable snapshot locks, no mutation and strict size limits before producing any remote backup payload.

**T1C explicit Back Up, separate restore**
- Implement authenticated, encrypted, all-domain server upload and commit; verify object bytes/hash and copy metadata after durable remote acknowledgment; retain previous verified revision.
- Show accurate only-after-verification status and backup history. Account mismatch, offline, stale CAS and incomplete photo inventory fail without overwriting a good copy.
- Run device A→device B evidence via phone/emulator, with receipt instants and photo bytes. Never enable live Restore Backup until T2's staged owner-reviewed coordinator passes.

## T1B2 — source implementation: verified media byte proofs and saved settings (UNTESTED)

The current T1B2 implementation on the isolated review branch adds read-only
SHA-256 proofs from actual files, not merely their Room/photo filenames:

- **Personal Space original JPEGs** — bounded streaming rehash of every
  `V290SpacePhotoEntity` referenced original, matching its exact SHA-256,
  checking the canonical private folder, disallowing symlinks/traversal,
  refusing any missing, untracked or partial private photo file. The actual
  byte reads run under the existing Personal stable-snapshot lock. A valid
  **zero-file inventory** requires an actual empty photo directory/no orphans.
- **Active Best Frames / Home slideshow originals** — `CreatorHeroArchive`
  current ordering and `PortableBestFrameMedia.prepareFromFiles` validate
  each image; the active files are independently streamed and hashed. The
  Home slideshow uses these **same** originals, so both categories reference
  the same verified bytes. Retired/rollback files and voice idea recordings
  remain outside this limited proof.
- **Creator timeline** — read-only export of Workflow Timeline, Project Pulse
  history and Post Publish checkpoint data under Creator data gate. The broad
  history family remains incomplete until all publishing/YouTube sources are
  enumerated.
- **Weekly schedule** — `WeeklyScheduleStore.inspectSavedForInventory()`
  reads the actual persisted slot payload without `loadOrSeed`, upgrades or
  data writes. If there is no saved source, the category remains pending.
- **Settings** — a narrowly whitelisted, read-only
  `creator_os_settings_v1` preference subset, plus the saved slideshow
  pause flag if present. We explicitly do NOT invoke
  `CreatorOsSettingsStore` during inventory because its constructor can
  write a guided-tour migration. Notification/Spark and other configuration
  domains remain pending.

Each proof yields only a count, byte count and SHA-256; no filenames, photos,
notes, private auth tokens or user media are uploaded. The inventory cannot
authorize a cloud upload or declare a full backup. It does NOT yet achieve
atomic Creator+Personal snapshot consistency. All tests in this incremental
branch are source-only and deliberately deferred to the explicit T1 phase gate
per the approved batched QA cadence.

**T1B2 remaining scope:** complete and safely export Idea voice recordings,
Creator script-linked external media, Hero rollback originals policy, missing
local preferences (including Spark, alerts and notification settings),
large-file inventory consistency across process death, and a versioned
owner-scoped full domain manifest. T1C still needs encryption, durable cloud
commit, backup history, prior-good retention and device-A/B evidence.

## T1B3 — Voice Idea originals and Spark/reminder preference subsets (SOURCE ONLY)

This isolated increment is **implemented in code but has not yet run the
T1 stage-end test gate**. The earlier Creator/Personal read-only inventory is
preserved; all cloud upload, local restore and deletion actions remain disabled.

- **Local Voice Ideas:** `TrustVoiceIdeaInventory` checks every record marked
  `IdeaCaptureType.VOICE`, canonical direct child under `files/voice_ideas`,
  valid finalized filename, bounded original bytes (32 MiB/file,
  256 MiB total), and real file SHA-256. Two independent byte reads detect
  truncated/replaced originals. If the Idea has a content-addressed
  `storage://creator-voice-ideas/...` reference, its SHA must match the
  observed local recording. Cloud metadata alone never proves availability.
  If a Voice Idea lacks local bytes, contains an unfinished take, has an
  unreferenced finalized file or references an external/document path, the
  category stays **PENDING**. No orphan cleanup and no local file mutation.
  This proves inspected local bytes only; no encrypted archive/upload yet.
- **Spark identity and tips:** `TrustSparkPreferenceInventory` reads
  `backlot_character_identity_v151` (including historical Frame/Navi/Cute)
  plus `backlot_companion_suggestions_v2212` preferences. It strictly
  validates dismissal JSON without invoking guide initialization/migration.
  Only count/length/SHA leave the reader; absence means PENDING.
  Spark conversation/transient voice/notification state remains a separate
  unproven part of `SPARK_PREFERENCES`.
- **Creator reminder escalation:** `TrustCreatorEscalationInventory` reads
  `smart_escalation_config_v102` without calling its migrating `get()`.
  Every saved notification→voice→alarm timing is validated as a bounded
  integer. Corruption or unsupported keys fail closed rather than being
  silently replaced with defaults. Full `NOTIFICATION_PREFERENCES` remains
  PENDING until every Personal/Creator source is inventoried.
- **Deliberate nonportable exclusions:** `CreatorMusicLocalStore` is
  device-only by design. App-private custom Android SAF document URIs,
  picked music permissions and OS exact alarm/notification grants must
  be reselected or reauthorized on a second phone. Their content is never
  represented by a fake portable music backup.
- **Tests authored and queued, not executed:** six pure JVM Voice Idea
  byte/orphan/path cases; Android15 Spark preference integrity and Smart
  escalation migration-avoidance cases; both registered in the one
  `qa/backlot-trust-t1-stage-*` stage-gate workflow.

### Unresolved owner-backup dependencies (T1B completion and T1C)

1. Enumerate and safely handle remaining mixed data: Creator publishing/AI
   settings not yet covered in whole, Spark conversation state, notification
   preferences, orphan/retired Best Frames and other active voice/media paths.
2. Design final *single* encrypted payload manifest with exact source-revision
   capture across distinct CreatorDataGate and Personal Space/SQLite locks;
   hashes from earlier inventory reads are not a frozen cross-store revision.
3. T1C requires immutable encrypted owner-scoped server uploads, atomic
   last-known-good revision retention, post-upload readback checksum, durable
   backup history and honest UI. It must not reuse legacy reminder-only Sync Now.
4. T2 still requires a separate owner-approved multi-domain recovery journal
   and staged restore. No auto-restore based solely on a green inventory.
5. Full T1 stage gate + version-signed Android V322→future owner APK
   preservation and phone↔emulator evidence remain release prerequisites.

## Remaining owner acceptance and release gates

- Phase 3F signed V322→V324 upgrade Android11/15 + device visual acceptance remains separate.
- No silent overwrite, no `pm clear`, no failed archive interpreted as empty.
- Challenge and Spaces metadata/receipts are privacy-sensitive; no leaking names in logs.
- `#327` remains OPEN until actual cloud challenge end-to-end coverage succeeds.
- Source branch and CI are NOT a verified installable APK or an owner-visible Back Up feature.
