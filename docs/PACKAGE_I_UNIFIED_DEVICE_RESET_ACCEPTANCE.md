# Backlot Package I — Unified Local Delete / Reset (source handoff)

**Date:** 2026-11-13  
**Branch:** `feature/backlot-i-device-reset-safety-20261113` (based on the completed H3 Task 5 development branch)  
**Status:** source implementation + regression tests committed; **Android compilation, emulator and owner-device validation still required in Package J**. No stable APK or full integrated release is authorized by this note.

## Owner outcome and exact scope

Reuse the existing Advanced → **Start fresh on this phone** entry and Android's `ActivityManager.clearApplicationUserData()` rather than introducing independent partial delete implementations for Creator and Personal. The system operation requests clearing Backlot's app-managed local data, including:

- Creator projects, ideas, workflow, Content Workspace and local scheduling state
- app-owned Voice Idea recordings, Best Frames and Home media within app storage
- Personal reminders, Done history, linked groups and 21-day challenges
- Spaces, journals and app-owned private photos
- preferences, local sessions, app-managed media and **local recovery escrow/history**

The reset does **not** delete cloud provider accounts or remotely stored data; independently saved encrypted unified ZIPs, provider-owned documents and original external project files remain outside this device-only reset. A separate cloud/account deletion flow would require its own verified server-side contract.

A reset is not backup/restore. Do not imply that the data could be recovered from a local escrow after the system erase. Never use a hand-written per-domain wipe to approximate the system-level erase.

## Safety gates implemented

1. Keep the exact typed owner phrase `ERASE THIS PHONE` and the existing two-step review/final dialog.
2. Require five explicit acknowledgements: irrecoverable local record loss, cloud data unaffected, private app-owned media loss, local recovery/escrow loss, and verified off-app backup or conscious choice to lose data.
3. A read-only preflight fails closed when app-private storage is unreadable or any known active Creator/Personal/unified restore, selected-target, media shadow/inverse, or final-release marker exists. Creator's atomic `pending.json` / `.new` / `.bak` files are included.
4. Recovery evidence is **never automatically repaired, deleted or normalized** to let a reset succeed. The owner sees a clear block and may recheck after completing recovery.
5. Run the same preflight again at the irreversible final click; a changed confirmation or new pending recovery stops the request. Only then call the existing Android system clear API.
6. Treat a system API rejection or exception as **not confirmed erased**. Do not display an unsupported success claim.

## Boundaries and validation remaining

- This is a conservative evidence check, **not** a cross-process globally atomic reset/restore lock. The final device QA must verify there are no races with restore preparation and app workers at the system erase boundary. Do not claim crash-safe integrated release on source inspection alone.
- Some previously exported ZIPs might be incomplete as to external third-party files; explicitly keep original footage, attached media and credentials separately.
- The backup chosen for retention must be exported, reopened and authenticated **off device** before the owner erases the only app copy. We do not block a user who consciously chooses permanent loss after all acknowledgements.
- Package J must execute the pure unit tests, Android 11/15/16 emulator UI paths, cold-start and process-death cases, fresh/dirty recovery markers, notifications/work cleanup, private media checks, external files retention, and at least one owner-approved disposable-device confirmation.
- Neither these source changes nor green unit tests would authorize Play release or changing the established signing identity.

## Regression tests authored

`FreshStartPolicyTest.kt` covers every missing acknowledgement, phrase mismatch, unusable storage, active recovery evidence, all known H4 dependent recovery root names, Creator AtomicFile pending variants and preservation of inspected evidence. These tests have **not yet been executed** in the current session.

## Fixed next work

**Package I source is ready for integration review. Package J is next:** compile entire H3+I stack, resolve integration failures, run source/target restore, interruption, alarm, image/recording byte-integrity and local-reset acceptance, then issue a uniquely identified, verified signed APK. Keep unified Restore All disabled until its required recovery and device acceptance gates actually pass.
