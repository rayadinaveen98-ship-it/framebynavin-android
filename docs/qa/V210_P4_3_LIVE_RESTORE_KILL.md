# Backlot V210 P4.3 — Live restore interruption acceptance

**Status:** DRAFT, exact-head CI required. This stacks on P4.2 / PR #96. It does not alter the default app behavior, its versionCode 236, signing, data schema, UI or permissions.

## Gap closed by this new test

Earlier P4.1 and P4.2 tests created synthetic recovery markers *outside* a real restore. P4.3 exercises an **actual ongoing** `CreatorBackupManager.restorePortableCompact` transaction:

1. Prepare an authentic compact Best Frames snapshot and record previous creator metadata and SHA-256-bound image references on a disposable Android 11 emulator.
2. Enter real `restorePortableCompact`; let it verify the input, publish the pending recovery marker, import creator metadata, install validated new original image files and **switch the visible order**.
3. Immediately afterward, invoke a narrowly scoped optional internal test callback `afterImagesCommittedForTest`. All normal callers use the default `null`. The callback asserts that the journal exists, confirms the image order changed, writes a small completion marker, and blocks.
4. CI runs this instrumentation in the background and polls `adb shell run-as com.framebynavin.app ...` until the committed-image marker appears. CI confirms `pending.json` still exists, then executes **`adb shell am force-stop com.framebynavin.app` while the transaction is active**.
5. A second, separate instrumentation process calls actual startup recovery unless it already occurred during app startup. It asserts the exact previous image hashes/order, previous creator tasks, ideas, weekly schedule and settings; it also demands a cleared journal and idempotent no-op on a second recovery.
6. Cleanup and restore of baseline happen only on the disposable CI emulator. Neither this fixture nor its cleanup may run on personal phones.

## Safety and limitations

- The only runtime-source difference is an optional `internal` test callback **after** the new image order commit. All installed app flows use the default `null`, so there is no new production feature or side effect.
- The emulator is disposable, but a failed kill/recovery test can leave temporary files; it is a **test failure**, never a reason to delete a personal-device restore journal.
- This does not test actual power-off/storage flush, Android low-disk faults, 65–100 MiB real-photo libraries, or memory pressure in older JSON/cloud image restore. Those remain stable-release blockers.
- No result is claimed until exact-head **build and both emulator jobs** are green; PR remains draft and cannot be treated as a stable release.
