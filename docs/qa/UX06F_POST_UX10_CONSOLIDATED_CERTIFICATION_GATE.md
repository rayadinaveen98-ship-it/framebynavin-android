# Backlot UX06F — Single Post-UX10 Integrated Certification Gate

**Date:** 2026-11-17
**Status:** TEST PLAN ONLY. NO TESTS RUN HERE. Do not claim compile, emulator, real device, APK, or Play approval.
**Target:** one integrated UX01–UX10 candidate, after reconciliation with accepted V337 P0 stable (`815ba69775f75b6e5f25bb838c2c17b4f09c203e`).
**Evidence rule:** each case requires result PASS/FAIL/BLOCKED, exact source/CI SHA, emulator/device details, reproduction, artifacts and owner sign-off. Draft source review cannot substitute for execution.

## 1. Why this gate is special

UX06A–E are sequential draft source stages layered on UX05.1E, while V337 P0 stable independently includes 19 commits not in UX06E ancestry. The UX06F draft repairs one concrete overlap in `Phase1HabitOccurrencePolicy.current` but the **full P0 reliability, ledger, restore and test behavior is not integrated yet**. No merge, stable promotion or hardware certification until the complete reconciliation and consolidated tests pass.

## 2. Entry criteria (all required)

- [ ] UX07, UX08, UX09 and UX10 source stages and change logs complete, scope reviewed by founder.
- [ ] Exact candidate SHA and base recorded; no unreviewed production/main/stable edits.
- [ ] All **22** accepted V337 P0 changed paths dispositioned against the candidate, including the shared `Phase1HabitLiveDelivery.kt` (preserved, intentionally superseded with evidence, or test-only retained).
- [ ] Existing source tests for UX06D and UX06E are included and not mistaken for executed tests.
- [ ] Migration/export/recovery fixtures use synthetic data only, plus owner-approved device backup exercises; no silent owner data reset.
- [ ] Real-device matrix, API coverage, and clear stop-the-line failure policy available.
- [ ] Baseline V337 can be reinstalled/compared without overwriting the user's original private data.

## 3. Consolidated cross-stage matrix

| Gate | Scenario / evidence requirement | Expected |
|---|---|---|
| F01 P0 | P0 real 21-day Snooze with sub-millisecond persisted Instant; fired PendingIntent in milliseconds | Not falsely STALE_IGNORED; one actionable notification, next scheduled once, no Done |
| F02 P0 | P0 recovery ledger, Alert Health, reboot/time-zone/time change, restore forensics | Exact P0 baseline behavior preserved, no false failure/success |
| F03 P0 | Compare and retain all 22 V337 P0 changed paths and dependency tests | Approved disposition, no unnoticed drop/overwrite |
| F04 P0 | V337 data → integrated v3 challenge archive → restore and no-data-loss diff | Receipts, archived flags, notes, links, schedule, IDs unchanged; audio v1/v2 OFF/3/5s |
| F05 P0 | Attempt v3 import in older client / rollback plan | Fail closed without deleting data; downgrade/rollback explicitly gated |
| F06 P0 | Duplicate broadcasts, old tokens, muted/paused/deleted challenges, simultaneous recurrence | No duplicate notification, no speech for stale occurrence, no automatic Done |
| F07 P0 | Creator step Done, I'm Working, Dismiss and Smart live-stage Snooze | Only authoritative stage gate awards/completes; final Smart deadline semantics stable |
| F08 P0 | Personal Done vs Dismiss, locked device and stale response token | Exact distinct receipts; no title/photos/notes/actions visible while locked |
| F09 P0 | V297 legacy challenges and owner state | No conversion, deletion or receipts changed |
| F10 P1 | UX06A Creator 1–5 repeats and old three-repeat defaults | Saved/full-screen/service settings agree; 3 remains legacy default |
| F11 P1 | UX06A global Snooze 5/10/15/30, Smart MOVE vs live REMIND IN copy | No visible unsupported 20m chip; actual scheduler unchanged |
| F12 P1 | UX06B Creator focus rails, search, selections, exact deep-link and quick/full wizard | Read-only projections; no stale bulk selection or unintended writes |
| F13 P1 | UX06B Personal Today/due, next 7 days, Needs action, all; challenge projection | Counts reflect actual store; no fabricated cross-domain All feed |
| F14 P1 | Personal 4-step composer backward jump, forward validation, cancel and process death | No unsaved alarm, no bypass of original validation |
| F15 P1 | Five native Canvas scenes vs original seven Classic values after upgrade | All 12 IDs render, no saved enum remap, original Voice/Alarm preferences remain separate |
| F16 P1 | Premiere vs Classic/Cinematic precedence and previews | Premiere opt-in masks backdrop only; preview non-actionable, Challenge only sample |
| F17 P1 | Challenge 1–5 Voice, all five gaps and Piano/Custom/OFF, alert disabled by default | Original activity text, notification and explicit Done; no extra occurrence |
| F18 P1 | Challenge local music URI revoked, moved or restored on another device | Graceful fallback, no crash/upload; no false assertion that music played |
| F19 P1 | TTS initialization failure/instant callback, delayed callback, audio focus/interruptions | No stuck speech, stale generation, replay or unrelated service shutdown |
| F20 P1 | Active Challenge Voice → Snooze/Edit/Archive/Delete during speech | Matching speech/music stops; persisted receipts and future valid schedule remain consistent |
| F21 P1 | V407 optional Spark/Mira greeting ON/OFF, 3s, alerts and previews | Companion only in eligible live hero, never covers buttons, OFF by default |
| F22 P1 | Existing selected guide identity after update, historical older saved value | No erased preference or unrequested reward animation |
| F23 P1 | Notification app/channel access, Android 13+, exact alarm API 31+, FSI API 34+ | Accurate ALLOWED/LIMITED/BLOCKED/UNKNOWN/NOT USED; no false delivery claims |
| F24 P1 | Personal Voice-only FSI and user-opened Recheck/Settings | No irrelevant full-screen permission prompt; no automatic Android Settings launch |
| F25 P1 | Android 13–16 devices, battery saver, DND, no exact alarm, background FGS restriction | Actionable degraded notification, bounded failure/repair, no guaranteed speech claims |
| F26 P1 | 320dp width, large font 200%, landscape, tablet, scroll/gesture/back | Immediate accessible primary responses; diagnostics and selectors don't clip |
| F27 P1 | TalkBack, keyboard/switch, selected states, focus, reduced motion | Companion decorative, proper action semantics, static low-power art |
| F28 P1 | Android 15 P0 physical current and older tested device upgrade | P0 actual Snooze, account/login and reminder flows survive old→new install |
| F29 P1 | Full app upgrade + simultaneous Creator/Personal/Challenge/Ideas plus content workspace | Original owner writers and stage/achievement state unchanged |
| F30 P1 | Recovery of configured local-only assets, user erase and account deletion boundaries | Clear limits, no silently missing photo/audio/file, no account deletion claim |

Each UX06 individual matrix remains authoritative for fine-grained cases: UX06A R01–R35, UX06B B01–B25, UX06C C01–C30, UX06D D01–D40, UX06E E01–E40. This F01–F30 matrix is a cross-stage integration index, **not** a replacement for their tests.

## 4. Required execution order after UX10

1. Freeze candidate diff and SHA, reconcile P0, inspect Gradle/manifest/OS policy (no surprise changes to signing, package, permissions, target API or services).
2. Gradle sync, compile all variants, lint, JVM/unit suite and existing domain-specific policy tests; collect exact exit results.
3. Synthetic V337 upgrade/round-trip/rollback safety and deterministic reminder concurrency tests.
4. Android instrumented/emulator runs on supported OS versions, including Android 15 P0 actual Snooze and regression paths.
5. Real-device Creator, Personal, challenge and Ideas audio + full-screen/heads-up/lockscreen privacy, accessibility and small-form-factor performance.
6. Owner-directed physical testing of the **exact produced APK** with hash, signer and proven source correspondence; capture explicit accept/reject.
7. Only then propose merging/removing stacked drafts or creating a new internal stable; Play production separately NO-GO until release-specific gates are cleared.

## 5. Stop-the-line rules

**NO-GO immediately** if any known P0 stable fix is missing; challenge Snooze is falsely ignored; v3 drops any receipt/archive/link; a reminder is silently marked Done; Personal locked content leaks; older rollback corrupts data; a permission bypass is proposed; a stored data owner is replaced; or the integrated APK lacks full source and signing provenance.

**NO-GO for internal stable** until all critical gates F01–F09 pass, mandatory Android CI + hardware reports exist, the full A–E test detail has dispositions, and founder approval is explicit. P1 defects may be triaged only with written owner acceptance and no safety/privacy compromise.

**Current exit verdict:** source stage UX06F is ready for documentation handoff **only**. 0 of 30 F-gates executed, no integrated candidate, no APK or stable release.
