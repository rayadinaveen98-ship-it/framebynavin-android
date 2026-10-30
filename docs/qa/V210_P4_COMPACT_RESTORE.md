# Backlot V210 P4 — Compact Original Best Frames Restore

**Candidate:** `2.8.6-v210-compact-restore-preview`, versionCode **236**, same Android package and personal-preview signing. Branch `feature/backlot-v210-p4-compact-restore`, based on green P3.1 commit `aead6f0aae7724028d78c7c015f5c12d2f3f58b8`. **DRAFT — not stable.**

## Why

P3.1 fixed export/preview for a >32 MiB original-image collection. The legacy restore route still called `PortableBestFrameMedia.expand` to create a large Base64 JSON snapshot and `CreatorBackupManager.restore` built a second full-image pre-restore journal. On an Android heap ~192 MiB these operations can cause an OutOfMemoryError. Backup portability means **restore**, not merely export.

## Source changes

- Portable ZIPs with verified Best Frames are now routed to `CreatorBackupManager.restorePortableCompact`, **not** hydrated into a giant Base64 canonical payload. Core metadata is validated under unchanged schema v9 and the same existing checksum/manifest rules.
- `CreatorHeroArchive.importVerifiedFiles` checks each staged source file's bounded size, SHA-256 checksum and bitmap dimensions, copies it via buffered streams into a **new app-owned filename** and verifies the destination before publishing the new Best Frames order. Every original file is retained.
- For a compact-image restore, the pre-restore journal records regular creator metadata with an **empty hero array** plus a small original order-and-SHA-256 checkpoint. The checkpoint is itself covered by the pending marker's SHA-256.
- Pending restore recovery knows how to verify and reattach these saved original files after process interruption, without materializing the previous images as Base64. The old journal format and regular CreatorBackupManager.restore stay supported unchanged.
- Voice Idea recordings remain verified and installed before the creator-data transaction, and their locally installed paths are re-signed into the compact metadata. External project asset URIs remain references only and are **not** copied.
- The older V208/V209 ZIP / JSON core restore path is unchanged; special handling applies only to ZIPs with fully staged Best Frames manifest.

## Why this should be safer

The currently active originals are referenced by checksum-bound in-app filenames. Because import/compact restore only adds new source files and changes the preferences order after verification, both expected failures and process-death recovery can switch back to the exact old files while preserving the creator metadata and reminder schedules. Existing image files are never overwritten.

## Release gates / risks

1. Android build, lint, 671+ unit tests, exact app ID, personal signing and hash validation.
2. Full inherited emulator suite plus extended `V209LargeFrameAcceptanceTest`: two 13 MiB original PNG files yield a canonical JSON larger than 32 MiB; portable **export → validate → restore**, restored checksum comparison, original image-reference recovery, and no memory exhaustion.
3. Backup corruption/ZIP-bomb protections and legacy V208/V209 compatibility suites remain enabled.
4. Real-phone checks: representative photo collections at 35/65/100 MiB, low storage, different RAM classes, app/process kill immediately after journal publication and after image order commit, reboot/pending recovery, voice media and upcoming reminder schedule.
5. **Do not promote to stable until the exact-head CI and on-device gate are green.** The large existing normal raw-JSON/cloud restore path can still use high memory. The test uses synthetic metadata-heavy PNGs, not representative photos.

**Open limits:** temporary restored media may retain orphaned files after a hard process kill; a separate garbage-collection strategy must never delete files referred to by pending recovery journals. Backups are still unencrypted; external project files are not embedded. Do not infer low-memory or near-100-MiB reliability from this one emulator profile.
