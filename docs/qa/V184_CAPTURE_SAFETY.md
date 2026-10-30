# V184 — Phase 0 and first Phase 1A candidate

Baseline: V183 main `aa9e1aab939b7b075b50f9d5a952c6fb86b901eb`; main-head CI 39129657906 succeeded. Version 184 / `2.3.4-v184-capture-safety-preview` uses the same package and personal preview signing identity. Not a stable promotion.

## Implemented scope

- Reconcile README/current-stable with V183 and retain historical V149 release evidence.
- Check in the approved revision-2 master plan, including the locked Home slideshow requirement.
- Main-app Quick Capture journals text, notes and finalized recording metadata locally. It recovers on reopening and across process recreation.
- Close offers keep draft, keep editing or explicit discard. Active recording must be stopped/kept before closing or saving.
- A successful main-app save awaits an accepted vault write. Draft IDs stay stable for retry; replay cannot overwrite an idea with different content or resurrect a trashed idea.
- A recovered journal already committed to the vault is cleared without another capture.
- Finalized draft audio survives composable disposal and is protected from manual orphan cleanup. Explicit discard never deletes audio still referenced by a saved idea or a path outside the owned recording directory.
- Restore-generation changes invalidate the draft; older capture writes are rejected.

## Preservation inventory and boundaries

| Data | Authority / policy in this package |
|---|---|
| Existing projects, scripts, publications, recurring schedule | Existing stores/schema; no migration or replacement |
| Saved and Recently Deleted ideas/audio | Existing Idea Vault and portable voice media; no deletion during draft recovery |
| Home slideshow | Existing image store/UI untouched; preserve automatic slideshow in later Home phase |
| Reminders, settings, analytics | Existing implementation retained; inherited contracts/regressions remain required |
| Unfinished main Quick Capture | New local scratch journal bound to creator-data generation; intentionally excluded from cloud/portable backups |
| Still-recording audio | Recorder owns unfinished media; this package prevents intentional close/save while active, but does not promise recovery of an interrupted unfinished recording |
| Widget Quick Idea | Existing separate activity; durable vault writer retained. Shared capture UX consolidation remains a later bounded task |

Phase 1A is not complete: cross-store idea conversion, reminder-choice changes and progress semantics remain subsequent work. Phase 1B Insights trust work has not shipped.

## Required automated verification

Full JVM regression suite and lint, instrumentation compilation, preview APK assembly, inherited V161–V182 source gates, emulator execution of V184 journal/recovery tests plus V161 Quick Actions, V183 capture writer and reminder safety suites. Static source checks are supplementary, not runtime acceptance.

## Required device / visual acceptance

Upgrade from V183 without uninstall; verify original projects, images, scripts, voice ideas and reminders. Type Telugu/English text plus notes; keep/close/reopen; record, stop/keep, reopen/play; test failed save/retry and restart. Check microphone recording cannot be accidentally abandoned through the parent Close/Save. Exercise storage cleanup without removing a draft take. Verify font scaling, TalkBack, keyboard, dialog buttons and native Back. Run an export/restore drill on a separate test install and confirm old drafts do not replay.

No connected Android device/emulator or local Android/Gradle toolchain was available in the implementation workspace. Fresh screen captures, local runtime tests and physical-device checks are not claimed. GitHub Actions is the build and emulator execution environment; record actual results before delivering an APK as verified.

## Implementation-session verification status

- V161–V182 source contract scripts: 22/22 passed locally.
- Diff whitespace validation: passed.
- New automated tests prepared: store recreation/audio preservation, stale restore generation, protected audio cleanup, explicit keep/discard and save retry/recovery.
- Android/JVM build, lint and emulator execution for V184: not executed locally; no toolchain available.
- Repository push authorized explicitly by the creator; candidate source is `6064285a3655fda51a6e62307dc039fa8d06e36a`, PR #40.
- GitHub Actions run 39135871904: clean build, 542 JVM tests (zero failures), lint, instrumentation compilation and non-debuggable personalStable APK assembly passed. First emulator execution completed 14 tests with two failures: canonical path equality and recovered-screen readiness timeout. Neither is treated as passed. Follow-up normalizes the path assertion and adds a 30-second readiness bound with a semantic-tree diagnostic; all 14 tests must pass on rerun.
- Downloaded artifact ZIP digest matches GitHub. APK v2 signature and signed-content digest verified; signing certificate matches the existing prototype identity. Manifest package/version/non-debuggable status verified.
- Candidate APK SHA-256: `ebbc2f372b98f6718352c07eeff38b4659b9febcb723026ede8c888c14a88c9a`.
- Phase 0 visual/device baseline and all physical-device acceptance remain open.
