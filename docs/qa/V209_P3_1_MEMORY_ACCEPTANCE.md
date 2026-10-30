# Backlot V209 P3.1 — Large backup memory acceptance (draft)

## Verified root cause, 22 October 2026
P3.1 Android emulator workflow run [#39793083000](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/39793083000) failed its >32 MiB original Best Frames test. Android reported an OutOfMemoryError allocating ~145 MiB while `CreatorBackupManager.fingerprint()` built another giant string during restore/cleanup (heap growth limit ~192 MiB). This proves the P3 ZIP split alone **does not guarantee large-frame portability**.

## Corrective changes in build 235
- Replace the previous `buildString { ... }` legacy checksum calculation with streaming SHA-256 over 4096-character chunks. Preserve the **exact historical byte stream and UTF-16 length prefixes**, including surrogate-pair boundaries. Older v1–v9 backup checksums are unchanged.
- Stream SHA-256 for full recovery-journal strings as well, avoiding an additional contiguous UTF-8 byte-array allocation.
- Extend the emulator suite with an **explicit old-versus-new SHA-256 equivalence test**, including Telugu and emoji split across a chunk boundary.
- Keep the large-image acceptance test on a **disposable emulator**, where 2 valid PNGs (metadata-expanded) create a core backup exceeding the former 32 MiB ZIP JSON cap. Clear this test's active Best Frames order before cleanup restoration to avoid constructing yet another huge pre-restore recovery snapshot.
- Increment the preview to **2.8.5-v209-large-backup-memory-preview**, versionCode **235**. Package identity and personal prototype signing remain unchanged. All earlier releases remain untouched.

## Additional large-export/preview fix

A second >32 MiB Android test run [#39797913077](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/39797913077) showed an OOM in `org.json.JSONTokener.nextString` when `PortableBestFrameMedia.pack` reparsed the oversized canonical JSON, even after the streaming checksum fix.

New candidate source changes:
- `CreatorBackupManager.createPortableBackup()` encodes a compact portable v9 JSON envelope from the same locked snapshot, instead of producing and reparsing one enormous full-root Base64 JSON string.
- `PortableBestFrameMedia.prepare()` reads original image names/files directly and ties ordered originals to the compact marker; the ZIP still verifies hashes/sizes.
- `PortableBestFrameMedia.verifyCompactPreview()` verifies staged media, safe filenames, image bounds, media checksums and original canonical array SHA-256 **one image at a time** without recreating the giant root JSON, then `CreatorBackupManager.validatePortableCompact` validates the rest of the core data.
- The **restore path remains separate**: `PortableBestFrameMedia.expand` still reconstructs a full canonical Base64 JSON for core restore/journaling. Until large-image restore is proven or redesigned, DO NOT claim large backups are reliably restorable on low-RAM phones.

This is still a **draft**. No CI pass or APK should be claimed for this source revision before the exact-head suite finishes.

## What this does NOT prove
- Streaming checksums remove a large extra allocation, **not all** Base64 / JSONObject copies in `CreatorHeroArchive.export`, `CreatorBackupManager.snapshot`, `PortableBestFrameMedia.pack/expand`, and pre-restore journaling.
- A >32 MiB emulator pass is not a low-RAM phone proof and is not a guarantee at the 100 MiB original frames limit.
- No encrypted backup, external project attachment bytes, automatic cloud migration or production signing is added.

## Release gates
1. Build, lint, 671+ unit tests, signer/package/hash and inherited emulator tests must pass on the **exact-head commit**.
2. P3.1 stress acceptance must complete with **zero** OutOfMemoryErrors (including its final restore cleanup). Preserve logs as CI artifacts.
3. Validate old JSON/ZIP compatibility, voice recordings, frame checksums and reminder recovery.
4. Test actual photos, near-max image collections, low-disk and forced low-memory conditions before treating the 100 MiB allowance as reliable.
5. Never uninstall a populated device or declare stable automatically.

Follow-on: [issue #93](https://github.com/rayadinaveen98-ship-it/framebynavin-android/issues/93) for a bounded streaming snapshot/restore architecture.
