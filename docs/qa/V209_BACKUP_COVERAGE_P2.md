# Backlot V209 P2 — Truthful backup coverage and restore preview

**Scope:** Narrow functional/UI disclosure and validation-readout update, based on V209 P1.
**Candidate version:** `2.8.3-v209-backup-coverage-preview`, versionCode `233`.
**Package/signing:** unchanged `com.framebynavin.app` and `personalStable` prototype certificate for non-destructive personal in-place update.
**Stable reference:** frozen V195.5, unchanged; V208 preview remains separate.

## Verified coverage from V208 source

| Creator data | Portable backup behavior | Recovery limitations |
| --- | --- | --- |
| Projects, workflow stages, scripts, checklists, deliverables, titles and publication history | Included in `tasks` JSON; legacy script sidecar is folded into export | External file references within workspace do not carry bytes |
| Ideas, metadata and links to projects | Included in `ideas` | Voice Idea local paths need portable media packaging |
| Finalized, readable app-owned Voice Idea recordings | Included as actual ZIP media entries, SHA-256 verified before successful preview and again for restore | Referenced missing/broken recordings reject export; working/unsaved recordings are excluded |
| Home personal Best Frames / slideshow source images | Included in core `personalFrames` JSON as Base64, image-decoded and SHA-256 checked | Max 10 frames, <=20 MB per image, <=100 MB total; missing active frame rejects export |
| Project research assets (image/video/audio/document imports) | Workspace asset *location strings*, labels/kind/notes only | No file bytes, no new read grants, may not open on a second device |
| Schedule, core reminder settings and user profile | Included in core JSON | Pending OS alarms are re-created; notification state/permissions require Android re-grant |
| YouTube project links, milestones and publish checkpoints | Included in core JSON | OAuth tokens and rebuildable analytic caches excluded |
| Theme, launcher icon, companion choice, launch sound | Not included in portable export | Device preference remains as-is on restore/new device may default |
| Google sign-in/session, Calendar account linkage | Excluded | Reauthorization needed; sensitive tokens never exported |

## Shipped P2 safeguards

1. `CreatorBackupManager.BackupPreview` now exposes `personalFrameCount`,
   `personalFramesIncluded`, and `externalProjectAssetCount` on successful
   validation. This is **derived from the same validated JSON** with no extra
   file access or schema migration. Blank/absent asset references are not counted.
2. Restore confirmation shows **Best Frames included** and **Project
   attachments: references only** so a valid backup cannot imply copied footage.
3. Export confirmation and Settings plainly state that physical project
   attachments are excluded and originals must be kept separately.
4. All old backup schemas remain supported. No changes to checksum,
   archive format, restore writes, app permissions or account/companion stores.
5. New tests use an isolated CI emulator, add a fixture with a real 1-pixel PNG
   in the Best Frames archive and an invalid `content://` project-asset
   reference, and confirm only the former contains actual media bytes.
6. No user data is accessed by this change; the emulator test restores its
   original fixture during cleanup.

## Limitations still requiring their own approved package

- True *portable packaging and restore* for project-file attachments requires
  a versioned asset manifest, streaming size limits, file provenance and
  permission checks, safe path rewriting, a shared rollback transaction and
  explicit user consent. Do not naively copy `content://` Uris into ZIPs.
- Existing personal export uses checksums, **not encryption/authentication**.
  Avoid claiming it is safe to share. Design authenticated encryption with
  usable recovery before public distribution.
- The cloud snapshot path differs: device-local media locations are sanitized;
  Voice Idea media are handled by a separate authenticated transport. Project
  attachments still remain location references. Drive snapshots should never
  be presented as a full phone-image backup.
- Test compatibility with interrupted export, file provider readback,
  corrupted images, max media sizes, original/second device URI availability
  and 200% font scaling on a disposable device.

## Stop-ship and rollout

**Stop ship** on failing exact-SHA CI, failed Backup/V208/V209 emulator suite,
missing project links or Best Frames, lost Voice Idea media, changed package
signer, or destructive restore. Never reinstall/clear a populated Backlot
device to test new-user setup.

Release P2 as a *preview* only after green exact-head build, APK signing/hash
verification, emulator acceptance and explicit personal-device review.
