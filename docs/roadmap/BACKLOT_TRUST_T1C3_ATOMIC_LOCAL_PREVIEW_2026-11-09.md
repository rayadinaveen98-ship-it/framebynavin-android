# Backlot Trust T1C3 — encrypted atomic LOCAL PREVIEW (source review)

> **T1C3B update:** MainActivity-startup temporary staging cleanup,
> a minimal interrupted-attempt journal and a shared process/OS lock are now
> implemented as source on a later stacked branch. See
> [T1C3B startup recovery notes](./BACKLOT_TRUST_T1C3B_STARTUP_RECOVERY_2026-11-09.md).
> The original T1C3A limitations below describe the previous implementation.
> Real account binding, global completeness and full Android testing are still
> missing.

9 November 2026 • isolated draft only • not compiled, QA not executed, no APK

## Implemented

`TrustT1C3LocalEncryptedPreview.createPartialReview()` is an internal,
explicitly-invoked, **PARTIAL** local review coordinator. It does NOT have
a Back Up button, background scheduler, cloud connection, authenticated
account binding, restore, or data-deletion UI.

- Captures owner-owned bytes from T1C2 under Creator→Personal locks into
  disposable plaintext files; does not change project, reminder, challenge,
  Space, photo or recording data.
- Encrypts all staged parts through T1C1 AES-GCM into a **new Android
  AtomicFile** in an app-private, no-backup, pending folder. Never overwrites
  the previous verified review object.
- Uses a flush-only stream wrapper so T1C1's ZipOutputStream may close its
  wrapper but does **not** close AtomicFile's underlying descriptor before
  `AtomicFile.finishWrite()` has performed sync and atomic rename.
- Reopens the encrypted pending file, authenticates all source frames,
  owner assertion and passphrase, and checks each part against its
  independently frozen id, record count, exact byte length and SHA-256.
- Computes encrypted SHA-256 and total bytes before an atomic same-volume
  promotion to a new `verified/verified-UUID.enc` file. Existing encrypted
  reviewed objects are retained; no replace. At most three are permitted
  before further creation is refused rather than silently deleting the
  owner's history.
- `verifyPartialReview()` reopens a particular promoted object and
  independently decrypts/rechecks it into disposable private staging.
  Wrong owner assertion/passphrase is rejected without changing records.
- Before an explicit new preview, cleans only validly named abandoned
  `pending` candidates, T1C2 plaintext temporary directories and T1C1
  decrypted readback directories. Rejects unknown file types, symlinks or
  unexpected paths rather than blindly traversing other cache files.
- `TrustTemporaryStageRegistry` protects in-progress T1C1/T1C2 temporary
  directories from the same-process cleanup sweep. All coordinator
  instances serialize preview operations on a common mutex.
- Returns `TrustLocalPartialPreviewReceipt` with encrypted byte count,
  encrypted SHA, number of verified source parts, UUID and review timestamp.
  `isUniversalBackup`, `canUploadToCloud`, `canRestoreOwnerData`
  are **all hard-false**, including after successful verification.

## Important limitations

**This is not crash-safe universal recovery**. The atomic encrypted preview
survives a normal successful process restart, but source capture still
creates temporary plaintext under app-private cache; process/power failure
can leave those files until the **next explicit** preview operation. There
is **no automatic startup sweep yet**, no cross-process staging lock, no
secure erase guarantee and no autonomous recovery journal. These must be
resolved before any real owner-facing Back Up button is enabled.

This preview covers only the source subset frozen by T1C2; missing broad
notification/Spark settings, external media and retired assets remain
unproven. Independent producers outside the existing gates prevent a
universal atomic completeness claim. Cloud account identity is an
assertion from caller arguments, **not authenticated provider login**.
No server-side owner encryption-key recovery, cloud revision retention or
read-after-write confirmation exists.

A lost passphrase may make the encrypted preview unrecoverable. Never
advertise account-driven key recovery until T1C4 explicitly implements
and verifies it. The `noBackupFilesDir` location deliberately avoids
mistaking this experimental partial local preview for an OS cloud backup.

## QA planned, NOT executed

- `TrustT1C3LocalEncryptedPreviewDeviceTest` runs on disposable CI
  emulator, verifies the local encrypted file, reopens it through a fresh
  coordinator, rejects wrong owner/password, preserves an older verified
  copy after failure, sweeps crashed pending and plaintext fixtures and
  confirms Creator/Personal data is unchanged.
- Added to the **single explicit Trust T1 milestone**
  `tools/qa/backlot_trust_t1_stage_android15.sh`, rather than starting a
  full emulator run for each incremental commit.
- The older T1A/T1B/T1C1/T1C2 tests also remain queued. Source has not
  been compiled or verified by Android; no production readiness implied.
- Before shipping: power/process-crash injection at every AtomicFile stage,
  cleanup recovery at app startup, concurrent account/device switch,
  disk exhaustion, malformed archives, oversized media, all-source coverage,
  retained previous-good snapshots, signed V322 upgrade and physical owner
  device acceptance. Never use destructive test fixtures on user's phone.

## Next tasks

1. **T1C3B:** reliable startup cleanup/recovery journal with safe concurrency
   and a persistent encrypted-preview registry; gap-free domain capture and
   owner-key handling. No false success UI.
2. **T1C4:** authenticated owner-scoped immutable cloud object versions with
   compare-and-swap, encrypted upload/readback, previous-good retention and
   account-switch protection. Must not replace legacy Creator/Personal sync.
3. **T1D:** one complete unit, Android 11/15, multidevice, interruption,
   real-owner signed-upgrade and media/challenge receipt QA matrix.
4. **T2:** owner-previewed and explicitly approved restore only after all
   source, recovery and conflict gates pass. T3/T4 deletion later.

Related issue #344, challenge cloud blocker #327, parent PR #350.
