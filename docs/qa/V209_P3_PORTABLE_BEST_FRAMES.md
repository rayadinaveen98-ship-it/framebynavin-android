# Backlot V209 P3 — Portable original Best Frames (draft candidate)

**Scope:** fix [issue #90](https://github.com/rayadinaveen98-ship-it/framebynavin-android/issues/90) without changing the canonical core/cloud backup schema or losing existing Voice Ideas. Based on the green V209 P2 source SHA `396e134935de516dd9aa59bdbcfd53e053ffe3b9`.

- Android personal-preview `2.8.4-v209-best-frames-portable-preview`, versionCode **234**, same package and personalStable signer as previous previews.
- Not stable or commercially signed. Existing V208 and V209 P2 APKs and frozen V195.5 stable branch remain unchanged.

## Root cause

The local Best Frames archive allows up to ten images, 20 MiB per original, up to 100 MiB total. The previous portable ZIP stored Base64 image data inside `backlot.json` and enforced a 32 MiB JSON-entry cap on validation and restore. Large but otherwise valid collections could therefore fail after export.

## New ZIP representation

- The **core backup schema stays v9**. Its regular snapshot still has `personalFrames` inline and can be decoded by existing restore/rollback/cloud workflows.
- During *portable* export only, original images are replaced by a strict portable marker in the ZIP's compact `backlot.json`. The marker records the exact original serialized frame-array SHA-256 and count.
- Original file bytes are copied into `best_frames/frame-0.img`, `frame-1.img`, etc. A versioned `best_frames.tsv` manifest binds their order, safe original filename, actual size and SHA-256.
- `backlot.json` stays limited to 32 MiB. Frame extraction is limited to ten entries, 20 MiB each, 100 MiB combined. The existing Voice Ideas manifest and its 1 GiB media budget remain separate.
- Restore **fully stages** all ZIP media, verifies declared sizes/hashes/order, verifies the original compact JSON manifest and checksum **before re-signing anything**, reconstructs the original canonical Base64 `personalFrames` JSON, compares the original image-array SHA-256 and then invokes the existing core backup schema validator *before any restore mutations*.
- Legacy raw JSON and V208/V209 portable ZIPs without a frame manifest remain supported unchanged. A marker with missing manifest, unexpected image entries, duplicate entries or altered images fails closed.
- External **project attachments remain references only**; this work does not package content URIs or footage and does not offer encryption.

## Critical limitations, unchanged or deferred

1. Export starts with the existing canonical core backup, which still materializes original Best Frames as Base64 in memory; restoring likewise reconstructs the core JSON in memory. Moving images to separate ZIP entries fixes the **32 MiB ZIP JSON cap** but does NOT eliminate high-memory pressure for 100 MiB original collections. Very large on-device backups require a later streamed core serializer/restore path plus low-memory-device tests.
2. ZIP/JSON SHA-256 detects unintentional corruption or inconsistent payloads; it does not provide encryption or proof of authenticity. Keep backups private.
3. The in-memory core snapshot and pre-restore recovery files also require capacity testing when large original frames exist. Do not claim arbitrary-size backup success.
4. Personal Best Frames are different from external project attachments, which are not packaged.
5. No production signing migration, account-data changes, cloud schema migration or destructive data change is performed here.

## CI acceptance

- Inherited unit, lint, full Android emulator, APK signing and hash gates.
- V209 existing test checks compact marker, physical image ZIP entry, saved project URI reference, old-format compatibility, missing Best Frames manifest, tampered image-media detection, and tampered compact JSON with a rewritten ZIP CRC.
- Test on a disposable device with real ~35–50 MiB original images before approving for larger image libraries.
- Verify export-readback, preview, restore and rollback in isolated data, 100% and larger font sizes, low disk, process interruption, and in-place upgrade preserving existing projects/reminders.

**Stop ship:** Any missing/corrupt frame being silently discarded, Voice Idea regression, failure to validate an old backup, different app ID/signature, or a failed exact-head CI/emulator gate.
