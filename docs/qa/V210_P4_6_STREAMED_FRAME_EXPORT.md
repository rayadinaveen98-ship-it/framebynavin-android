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


## P4.6 engineering acceptance and release freeze (23 October 2026)

**Engineering test gate: PASS. Stable/physical acceptance: NOT YET.** The earlier "unverified" statements above are a historical record of failed and pending runs, not the current result.

- Exact **runtime-and-test source head**: \`92ef3a6b3aa7fb2ca28c09abe665bdf33bd34f78\`; QA-only documentation head that was actually built/tested: \`7c18d0c25dc567899e7e2b989840a6842cd66304\`.
- [GitHub Actions 39934884089](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/39934884089): **build SUCCESS; Android 11 emulator SUCCESS**. Source, unit, lint, instrumentation build, personal-stable signing and package/version checks passed.
- In the integrated Android 11 run: \`V210StreamedFramesExportTest\` **3/3**; inherited >32 MiB \`V209LargeFrameAcceptanceTest\` **2/2**; checkpoint and fresh-process restore recovery; in-flight restore force-stop/recovery; \`V210BackupIoFailureTest\` **2/2**; inherited UI/regression suites. These are emulator checks, not a claim of near-100 MiB real-photo success.
- **Preview artifact:** [Backlot-V210-P4.6-Integrated-QA-APK](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/39934884089/artifacts/12493452576). Archive includes the personalStable APK. SHA-256 of the **APK file** (not the artifact ZIP) from the successful build:
  \`b0ddf66445fbac95e2d464ed559875a0c24fce25a061be5a6a70a39ad07b1191\`.
- Package: \`com.framebynavin.app\`; versionCode **237**; versionName \`2.8.7-v210-streamed-export-preview\`. APK v2 signer certificate SHA-256:
  \`f0ddec789172ec9f83a037a95b14e90293126f61936d8bab056d3a57e0d472a0\`.
  This is the **public personal-sideload signer**, not private Play production signing. An unchanged signer fingerprint does not guarantee all in-place migrations; Android must accept the actual install.

### Physical-device acceptance — manual, no destructive shortcut

Use the artifact above; keep the existing V195.5 stable branch and installed user data. Do **not** uninstall, clear app storage, replace the signing key, or bypass Android's update verification. If a populated app cannot be updated in place, **stop** and record the error.

1. **Preservation first:** record the installed app version, export an independent creator backup to durable off-device storage where available, and separately copy important source media/attachments that a portable backup may only reference. A successful export is not proof of restore until revalidated. Do not use a production phone as a destructive restore test device.
2. **Artifact integrity:** download the GitHub Actions APK archive, extract it and compare the inner \`app-personalStable.apk\` SHA-256 with the value above (Windows PowerShell: \`Get-FileHash -Algorithm SHA256 .\app-personalStable.apk\`). A hash mismatch is a STOP.
3. **Safe upgrade:** attempt install as an Android update over the existing Backlot package, not as a fresh installation. If Android reports incompatible signing, storage or migration errors, STOP. Do not clear data as a workaround.
4. **Preservation smoke check:** after launch, confirm saved ideas, projects, scripts, voice ideas, settings, creator setup/companion, calendar, existing reminders, publication history, original Best Frames and authorized channel states are present. Check at least one reminder's actual voice/alarm/notification acknowledgment when due; inspect no duplicate or phantom reminder. Confirm Back/gesture/navigation and Settings/Insights/Revenue visually.
5. **Backup validity (non-destructive on populated phone):** export a portable backup to a user-controlled file provider; reopen in the app's **validation/preview only**, confirming media counts and any reference-only project attachments are disclosed accurately. Verify existing data is unchanged after canceling preview.
6. **Restore recovery on a disposable device/profile only:** take separate test data, back up → preview → restore → compare original Best Frames order, voice media, projects, schedules and reminder delivery; test damaged ZIP rejection. Never overwrite the creator's populated phone to prove a restore.
7. **Stress still pending:** 65–100 MiB real original photos on low-RAM physical hardware, real storage-full/provider write failures and sudden power loss. Older raw JSON/cloud image snapshots and 20 MiB-per-frame preview allocations remain memory risks. Record failures as release blockers; do not silently broaden scope to unrelated UI work.

### Release decision

**No merge, stable tag, Play launch, or automatic app rollout.** P4.6 is a CI-green **personal device-test candidate** only. When owner device checks pass, decide explicitly whether the remaining stress tests are necessary before a new personal-stable snapshot. Preserve V195.5 as the rollback reference and move separately to creator-facing UI/Insights roadmap work. Tracked stress backlog: [issue #93](https://github.com/rayadinaveen98-ship-it/framebynavin-android/issues/93).
