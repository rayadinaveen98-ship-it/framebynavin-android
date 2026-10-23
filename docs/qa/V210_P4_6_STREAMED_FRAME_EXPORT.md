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

1. New `V210StreamedFramesExportTest` on a disposable Android 11 emulator compares `originalSha256` with **CreatorHeroArchive.export()** (the historical canonical representation) across three different valid image files, verifies compact envelope and source media, and checks empty-list backward compatibility. Require `OK (3 tests)`.
2. Run the entire inherited integrated P4.5 suite on the same exact commit: 671 JVM unit tests, whole Android regression matrix, >32 MiB two-image export / preview / restore, tampered journal, cross-process recovery, actual in-flight force-stop and I/O short-write / truncated ZIP rejection.
3. Verify app ID, preview signing, APK SHA-256 and versionCode **237**. No new APK until all exact-head CI jobs pass and checksum is verified.

## Remaining risks / explicit non-claims

- The new code bounds image bytes during **portable export checksum computation**; it does **not** make the legacy raw JSON/cloud image snapshot memory safe, change the media limit or guarantee 100 MiB on low-RAM hardware.
- **Preview verification** still reads up to one 20 MiB image into memory to reconstruct its canonical JSON record. Test with large real pictures on low-memory Android devices.
- This is a production runtime change, so it must remain a **draft preview** until the exact-head full regression suite passes. Physical power interruption, actual disk-full, 65–100 MiB real photo sets, low RAM and Storage Access Framework file-provider issues remain stable-release blockers.

## Failed first emulator result and compatibility correction (23 Oct 2026)

Initial build 237 at commit `c60aa7c` **compiled successfully but FAILED both emulator runs** ([PR #39904747671](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/39904747671) and [push #39904703426](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/39904703426)). The real in-flight restore fixture called the new file-streaming ZIP export, then `verifyCompactPreview` rejected the reconstructed JSON with `IllegalArgumentException: Best Frames image order or content integrity check failed`. The correct behavior is to reject the mismatch; this is a hard stop, not a CI flake.

Correction: replace the `Base64OutputStream` digest with repeated calls to **the same** `Base64.encode(bytes, offset, length, Base64.NO_WRAP)` method used by legacy export, using **8190-byte chunks** (exact multiple of three). This ensures no Base64 padding is produced except after the final input chunk; the pieces concatenate to exactly one whole-file Base64 encoding, with no full-image Base64 string in memory. The source file is checked for unexpected truncation/extension during encoding and SHA-256 is verified after reading.

The regression now has **three** cases: three ordinary images, a >20 KiB PNG spanning multiple 8190-byte chunk boundaries, and zero images. CI runs `V210StreamedFramesExportTest` before the live-stop regression to fail quickly at the origin of any checksum mismatch. Full >32 MiB media and integrated V210 recovery/I/O suites remain mandatory. This correction is **unverified until exact-head CI is green**; prior build 236 is the last usable preview, and no build 237 APK should be distributed on the strength of its build job alone.

## Root cause confirmed from emulator tests (23 Oct 2026)

Exact-head run [39930842455](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/39930842455) compiled, passed JVM/lint/signature verification, then **failed 2/3** `V210StreamedFramesExportTest` cases. Both normal and multi-chunk fixtures produced SHA-256 values different from the legacy `CreatorHeroArchive.export()` JSON string. The zero-frame case passed.

Android's `org.json.JSONStringer` escapes each forward slash in a quoted JSON string (`/` becomes `\\/`). The legacy Base64 is *quoted JSON*, whereas the streaming digest originally hashed the raw Base64 characters. Aligned 8190-byte chunks fixed Base64 padding, but not JSON's solidus escaping. Merely skipping the escape would incorrectly reject good ZIPs later in preview and restore.

Fix committed on the **draft** P4.6 branch: probe the Android `JSONObject` serializer for solidus escaping and add the escape byte in the SHA-256 stream before each Base64 slash, leaving original bytes and ZIP/media unchanged. The >20 KiB test now asserts that the fixture actually contains an escaped slash. This preserves bounded processing and tests compatibility against the legacy exporter, not only the new exporter itself.

Exact-head validation is mandatory: [run 39934794873](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/39934794873) was pending when this note was written. **Do not treat build 237 as tested, stable or installable until the run is green, artifact SHA verified and the release gates above are satisfied.**
