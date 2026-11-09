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

## T1B4 — Double-read stability proof and stored routine/appearance preferences (SOURCE REVIEW)

New `TrustInventoryReviewReader.inspectTwiceReadOnly()` performs two
independent, validated Creator+Personal inventory passes and checks the
Creator restore generation before/after. `TrustInventoryReviewPolicy`
rejects any changed domain count, UTF-8 byte length, SHA-256, missing/extra
domain, exclusion or generation. It **independently recalculates** pending and
nonportable domains from `TrustBackupDomain` so two identical fabricated
"all backed up" lists still fail closed. A deterministic length-delimited
UTF-8 fingerprint records the read-only inspection receipt without names,
notes, paths, raw audio, photos or source content.

**Explicit limitation:** two matching observations do not imply a frozen,
single global Creator+Personal revision. This is not a T1C snapshot,
server-backed backup, encryption proof, uploaded archive or safe restore.
`crossStoreAtomicityVerified`, `canClaimCompleteBackup` and
`canStartCloudUpload` all remain **false** by construction.
No app-start background inspection is scheduled; two complete passes are
potentially expensive and only run from an explicit QA/owner action.

Additional real saved sources (read-only, no lazy initialization):
- `TrustRoutinePreferencesInventory` reads exactly persisted
  `creator_automation_preferences_v17` daily-brief/weekly-review/idea-review
  boolean choices. Missing keys remain missing; unknown/invalid keys fail.
  Planner runtime deduplication tokens are excluded as device execution state.
- `TrustAppearancePreferencesInventory` reads saved Director's Cut or other
  recognized theme enum values and launch-sound choice from
  `framebynavin_visual_experience`. No call to visual
  `initialize()`, which can migrate old theme selections on read.
- `DEVICE_AI_SHARING_CONSENT` is explicitly **nonportable**:
  `creator_ai_consent_v104` is device-specific and an imported backup may
  **never** silently re-enable sharing project/private context.

Tests **written but not executed**: six JVM drift/canonicalization/false
coverage scenarios, an Android15 non-mutating Creator+Personal review, and a
disposable-emulator routine/appearance validation test. Both native tests
were added to the single deferred `qa/backlot-trust-t1-stage-*`
integration gate. No owner APK or new cloud controls.

### Remaining before any T1C "complete backup" operation

- Cover or clearly classify broad notification/Spark state and Creator media
  not already verified (orphan/retired images, external URIs, pending voice).
- Stage actual immutable bytes under a documented, lock-safe cross-store
  snapshot protocol. Two independent read passes are not equivalent.
- Design and implement account-bound encryption/key recovery, authenticated
  durable remote upload, revision compare-and-swap, old-good retention and
  post-upload verification without using legacy reminder-only Sync Now.
- Owner-approved restore remains T2, not part of inspection.
- Run the full phase-end JVM/Android regression suite, a signed upgrade proof
  and real owner acceptance before any release.

## T1C1 — streamed encrypted owner-staging format (CODE ONLY)

**Source-only implementation:** \`TrustEncryptedStagingCodec\` is an isolated
streamed AES-256-GCM encrypted ZIP format with anonymous entries,
PBKDF2-HMAC-SHA256 passphrase derivation, encrypted part manifest and asserted
owner ID, per-part actual SHA/size checks and authenticated final frames.
\`stageAndVerify\` validates and temporarily stages *provided* immutable
source bytes; it does not read or mutate live Creator/Personal data, upload to
cloud, verify a signed CloudSession or restore records.

- Declared incomplete by construction: both
  \`canClaimCompleteBackup\` and \`canUploadAsUniversalBackup\` remain false
  even after every encrypted file byte checks out. A cryptographically valid
  **partial snapshot** is still a partial snapshot.
- Original app archive formats and existing Creator/Personal Sync Now paths
  remain unchanged; no separate user-visible Back Up action or APK.
- Eight pure JVM crypto/media/data-integrity cases and one fake-owner Android
  device test were authored and attached to the deferred T1 phase-end suite.
  **They are not yet executed**, and this source is NOT a production-ready
  encryption/security audit.
- [Full T1C staging threat model and release contract](
  ./BACKLOT_TRUST_T1C_ENCRYPTED_STAGING_CONTRACT_2026-11-09.md).
- T1C2 must freeze actual complete owner source bytes under a consistent
  cross-store revision and decide whether existing Creator and encrypted
  Personal archive codecs are nested or consolidated. T1C3 must add an
  encrypted local durable preflight + authenticated account binding. T1C4
  adds versioned private cloud objects, CAS, old-good retention and readback
  confirmation; do not overwrite older legacy cloud heads.
- T1 final QA and owner test, T2 reviewed restore and T3/T4 deletion remain
  strictly pending.

## T1C2 — frozen real owner source bytes under both existing locks (CODE ONLY)

[Exact source/coverage/recovery specification](
  ./BACKLOT_TRUST_T1C2_FROZEN_SOURCE_CAPTURE_2026-11-09.md).

Internal \`TrustT1C2OwnerSourceCapture\` now holds the Creator mutation gate
then the Personal stable-snapshot gate while producing **physical app-private
copies**, with per-file SHA-256, rehash and size checks. It includes Creator
projects/legacy Script Studio (via two new non-seeding read-only exporters),
Idea Vault, Focus history, actual Personal reminders and Done history,
legacy and 21-day challenges with **exact confirmed timestamps**, Groups,
Space metadata/journals/links, every referenced original Space JPEG,
active original Best Frames and verified local Voice Idea recordings.
Unexpected/orphan Voice Idea bytes prevent claiming a successful capture.

Personal reminders use the existing portable sanitizer: device alarm leases,
custom music URIs and nonportable music permission state are stripped
without changing completion history. Creator and portable Personal baseline
documents are reread and compared before releasing the temporary copies.
Missing originals, symlinks, suspicious paths and unexpected checksums
abort and clean the partial snapshot.

The output is deliberately \`TrustFrozenSourceFiles\` with
\`canClaimCompleteBackup = false\` and \`canUploadAsUniversalBackup = false\`.
This is NOT a global atomic revision, complete data coverage, encrypted
durable owner backup, verified account binding, cloud upload or restoration.
Independent preference writers and missing Creator families still require
coverage, and app-private temporary **plaintext** staging needs
interrupted-process/power-loss cleanup before a user-facing feature exists.

Five JVM source-freezer fixture tests + one Android15 native real-source
freeze→T1C1 encryption→readback test are authored but **NOT executed**.
The latter was added to the existing *single phase-end T1 QA gate*, not a
full emulator invocation for each source commit.

T1C3 durable encrypted local coordinator and T1C4 immutable authenticated
cloud CAS/readback remain pending. No existing owner's data, account,
installed APK or cloud snapshot was changed by this source-only commit.

## T1C3A — atomic encrypted LOCAL PARTIAL preview (source only, not tested)

The isolated `TrustT1C3LocalEncryptedPreview` coordinates T1C2 frozen
real-source bytes and T1C1 authenticated AES-GCM ZIP into a **new**
Android `AtomicFile`, performs whole-bundle decrypted byte/hash readback,
then atomically promotes only the validated encrypted object without
overwriting existing reviewed copies. A process-wide mutex protects
separate coordinator instances. The stream wrapper preserves AtomicFile's
descriptor until its final disk sync and rename. Each result has an
opaque review UUID, encrypted SHA-256, size and verified part count.

Known abandoned encrypted pending objects and temporary plaintext copies are
cleaned before the **next explicit** preview; a same-process registry avoids
removing active source/decryption directories. There is NO app-startup recovery
hook, no global multi-process lock, no guaranteed physical secure erase,
and no full owner-domain coverage. Crashes can leave sensitive private
plaintext until a later explicitly initiated cleanup. The encrypted local
files are stored in app-private `noBackupFilesDir`, NOT uploaded to a provider.
No app button, auto job, restoration or user backup-complete message is wired.

**Hard-false in source**: `isUniversalBackup`,
`canUploadToCloud`, and `canRestoreOwnerData`. The provided owner ID
is not authenticated; passphrase loss may make data unrecoverable.
[Threat model and remaining gates](
  ./BACKLOT_TRUST_T1C3_ATOMIC_LOCAL_PREVIEW_2026-11-09.md).

A real Android15 test is authored for encrypted local creation/reopen,
owner/password rejection, preserving old copy after failed attempt,
abandoned staging cleanup and unchanged Creator/Personal stores.
Two simple pure JVM tests cover active-path registry. These are **not run**:
the full Trust T1 QA remains reserved for the explicit milestone gate.
T1C3 startup/recovery hardening, T1C4 real cloud/version/key design, T1D
integration test and T2 owner-reviewed restore all remain pending.

## T1C4A — cloud revision policy + offline two-phase state machine (SOURCE ONLY)

Drafted a **new, separate** universal-backup protocol that cannot overwrite
Creator JSON sync or the existing encrypted Personal Archive V2. No live
bucket, SQL migration, HTTP client, cloud API call, authenticated session,
storage object upload, owner login, APK or restore is wired here.

`TrustT1C4CloudRevisionPolicy` validates canonical UUID account ownership,
immutable owner-prefixed ciphertext paths, SHA-256 and byte size, exact next
revision CAS acknowledgement, remote-head conflicts and **actual streamed
ciphertext downloaded from the server**. The verified-readback token has
a private constructor; a plain HTTP success cannot create it.
`TrustT1C4TwoPhaseState` models the required order of immutable upload,
streamed object readback, server CAS and independently fetched head proof.
State skipping and altered or concurrent revisions fail closed.

Even a transport-verified object is **not** a universal Backlot backup:
`canClaimCompleteBacklotBackup` and `canOfferRestore` stay false. Existing
T1C3 partial snapshots are rejected for live cloud upload. Underlying Creator
and Personal data gaps, key recovery and account-authenticated upload remain
T1C4B/release gates.

Ten pure-JVM test cases were written for owner isolation, changed/truncated
ciphertext, key naming, CAS/old-good protection, ordering and proof
requirements; **NOT run** under our owner-approved phase-end testing cadence.
[Exact T1C4A cloud/security contract](
  ./BACKLOT_TRUST_T1C4A_CLOUD_REVISION_PROTOCOL_2026-11-09.md).

Before enabling a live cloud adapter, provision and security-test a new
**private** authenticated storage bucket with RLS, separate head/history
schema and server-atomic CAS. Avoid direct arbitrary head UPDATE privileges
and never expose service-role credentials in Android. Verify account-switch
fencing, download/readback, retention and zero-spend/quota constraints.
T1D Android 11/15, owner-signed upgrade, two-device data recovery and
exact 21-day Done/JPEG integrity tests still precede any release.

## Remaining owner acceptance and release gates

- Phase 3F signed V322→V324 upgrade Android11/15 + device visual acceptance remains separate.
- No silent overwrite, no `pm clear`, no failed archive interpreted as empty.
- Challenge and Spaces metadata/receipts are privacy-sensitive; no leaking names in logs.
- `#327` remains OPEN until actual cloud challenge end-to-end coverage succeeds.
- Source branch and CI are NOT a verified installable APK or an owner-visible Back Up feature.
