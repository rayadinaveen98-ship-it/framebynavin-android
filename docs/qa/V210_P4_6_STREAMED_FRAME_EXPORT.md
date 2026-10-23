# V210 P4.6 — Streaming original Best Frames ZIP export

**Branch:** `feature/backlot-v210-p4-streamed-frame-export` on top of integrated draft P4.5 / PR #99. **Candidate build 237** `2.8.7-v210-streamed-export-preview`, same Android app ID and personal-preview signer. Do not merge to frozen stable.

## Why

V209–V210 portable ZIP restore and preview are now memory-bounded for large original media, but `CreatorBackupManager.createPortableBackup()` still called `snapshot()`, which allocated all original images as Base64 text in one `JSONArray`. Near the advertised 100 MiB media limit, Base64 size may exceed 133 MiB plus duplicate strings and bitmap bytes — potentially fatal on a ~192 MiB heap.

## Change

- `createPortableBackup()` now calls `snapshot(includeHero = false)` and `PortableBestFrameMedia.prepareFromFiles`.
- For each original frame, the encoder validates bounded file sizes, safe canonical app-private paths, decoded image dimensions and SHA-256; it writes the *legacy canonical* JSON object prefix into a digest and streams the original image as `Base64.NO_WRAP` through `Base64OutputStream` directly into a SHA-256 sink using bounded buffers.
- A real `JSONObject` containing the same field order with empty data supplies the prefix and closing bytes, avoiding assumptions about future Android JSON escaping. The resulting original-array SHA-256 is recorded unchanged in the existing compact media manifest; the ZIP schema and verification rules do **not** change.
- Media files are re-hashed after digesting, failing if modified while the export was reading.
- Empty collections remain exactly `[]`, preserving V208/V209 behavior.
- The previous `PortableBestFrameMedia.prepare(originalFrames, ...)` helper and legacy raw JSON paths are untouched; only the portable ZIP export path switches to streamed file checksum construction.

## Acceptance

1. New `V210StreamedFramesExportTest` on a disposable Android 11 emulator compares `originalSha256` with **CreatorHeroArchive.export()** (the historical canonical representation) across three different valid image files, verifies compact envelope and source media, and checks empty-list backward compatibility. Require `OK (2 tests)`.
2. Run the entire inherited integrated P4.5 suite on the same exact commit: 671 JVM unit tests, whole Android regression matrix, >32 MiB two-image export / preview / restore, tampered journal, cross-process recovery, actual in-flight force-stop and I/O short-write / truncated ZIP rejection.
3. Verify app ID, preview signing, APK SHA-256 and versionCode **237**. No new APK until all exact-head CI jobs pass and checksum is verified.

## Remaining risks / explicit non-claims

- The new code bounds image bytes during **portable export checksum computation**; it does **not** make the legacy raw JSON/cloud image snapshot memory safe, change the media limit or guarantee 100 MiB on low-RAM hardware.
- **Preview verification** still reads up to one 20 MiB image into memory to reconstruct its canonical JSON record. Test with large real pictures on low-memory Android devices.
- This is a production runtime change, so it must remain a **draft preview** until the exact-head full regression suite passes. Physical power interruption, actual disk-full, 65–100 MiB real photo sets, low RAM and Storage Access Framework file-provider issues remain stable-release blockers.
