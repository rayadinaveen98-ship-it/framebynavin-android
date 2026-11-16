# Backlot V337 — P0 integration readiness and test-debt decision gate
**Verified:** 2026-11-16. **State:** QA/documentation ONLY; no integrated source promotion or release authorized.

## 1. Authoritative frozen reference
- Repository: `rayadinaveen98-ship-it/framebynavin-android`
- Owner-reference stable: `stable/backlot-v337-premiere-owner-testing-20261114`
- Frozen stable SHA: `678502f4626150ea1945b81eed1593a362377454`
- `app/build.gradle.kts`: `com.framebynavin.app`, `versionCode=337`, `versionName=2.28.7-premiere-cinematic-enhanced-02`.
- Preserve owner APK, signing, installed data, existing projects, reminders, 21-day records and `Advanced > Erase local data`.
- Owner decision: **no backup or restore entries in Settings**. Never add them back as a test repair. Avoid any legacy unified backup/restore/delete reimplementation.

## 2. Exact CI provenance — separate snapshots, NOT a combined green release
| Evidence | Source | Observed | Meaning |
|---|---|---|---|
| Stacked six-stage Stage 6 #377 | `5f320f93c2a1fbc3c3c5253391454e2c1aa4e6d9`, CI #43597740085 | Integrated APK job PASS; `1708 tests completed, 2 failed` in JVM | Buildable Stage 6 candidate, **NOT fully accepted** |
| V394 owner reference #378 / V337 | `678502f4626150ea1945b81eed1593a362377454` | Frozen reference | Distinct from Stage 6 APK, no implicit full regression signoff |
| Recovery correction #379 | `a9dc4dc4f61dc6e21182b9b482c5b1d979d88397`, CI #43736971107 | Targeted + full JVM success, 1713 passed | Recovery fixes verified **on draft branch only** |
| Timed alarm / actual Alert Health #380 | `7d7d93b8277613093e23695ca347e798b7fe7c89`, CI #43813809584 | Android 15 emulator PASS | Isolated QA acceptance |
| Actual reboot #381 | `01da130d0ec59fcb7f8866cdf977a5ac9e6d95f2`, CI #43819066388 | Android 15 emulator PASS | Isolated QA acceptance |
| Forced deep Doze #382 | `a060937d5b3fea059cd7bae7d2281f7d6a625cfc`, CI #43822975226 | Android 15 emulator PASS | Short forced-idle proof, not OEM signoff |
| Denied special access/notifications #383 | `b97146fd8f6540811ff289fdb0de80bc808581b0`, CI #43825299522 | 3 Android 15 tests PASS | Fallback labeling and blocked posting, not inexact latency |
| Alert Health discrepancy characterization #384 | `a461c49162b1cb62978b64d6a51d2afcabfdfe00`, CI #43833425667 | 2 Android 15 tests PASS | **Two bugs reproduced, NOT corrected** |

All PRs #379–#384 remain draft/unmerged. Stacked feature PRs #369–#378 are also unmerged. Do not equate a green test on one PR with a combined release.

## 3. PR #379 source diff audit — only narrowly scoped recovery correction
Compared with the frozen V337 commit, PR #379 changes:
- `.github/workflows/backlot-p0-recovery-regression-isolated.yml` — QA-only workflow.
- `app/src/main/java/com/framebynavin/app/personal/Phase1V2RestoreDomainForensics.kt` — **one** production file; wraps an invalid `org.json.JSONException` as `IllegalArgumentException` with the original exception preserved as cause at the inverse restore forensic inspection boundary. Invalid records remain rejected; no writes introduced.
- `V352UnifiedRecoveryDomainPolicyTest.kt` — validates two distinct security failure cases rather than expecting stale-owner-choice diagnosis from unauthenticated archive proofs.
- `Phase1V2RestoreDomainForensicsTest.kt` — asserts malformed recovery archive rejection and its JSON cause.

**No approved merge exists.** Before a controlled integration candidate, require explicit owner authorization of the exact patch set and destination branch, preserve a known-good rollback ref, then run fresh complete regressions on the exact integrated commit.

## 4. Five V339–V343 UI instrumentations excluded in isolated Android CI
The proof workflows temporarily move these Kotlin source files out of the **ephemeral runner checkout** to compile targeted tests. They are NOT deleted or fixed in the repository. None is considered passed by an exclusion.

| Source | Current gate | Product-truth instruction |
|---|---|---|
| `app/src/androidTest/java/com/framebynavin/app/ui/V339HomeOverflowMenuUiTest.kt` | Compile/test compatibility requires isolated diagnosis | Preserve the latest approved three-dot notification/Spark/gallery interaction. Do not alter production merely to satisfy stale selectors |
| `app/src/androidTest/java/com/framebynavin/app/ui/V340HomeSpacesEntryUiTest.kt` | Compile/test compatibility requires isolated diagnosis | Preserve Spaces and existing navigation / saved state |
| `app/src/androidTest/java/com/framebynavin/app/ui/V341UnifiedBackupPanelUiTest.kt` | **Stale expectation risk**: expects visible old backup panel and actions | User **removed Backup/Restore from Settings**; do not bring the UI back to make this old test green. Retire or rewrite tests only after reviewing actual reachable surfaces |
| `app/src/androidTest/java/com/framebynavin/app/ui/V342UnifiedRestorePreparationUiTest.kt` | **Stale expectation risk**: expects unified restore preparation UI | Same: test expectations must honor removal, not resurrect unused product features |
| `app/src/androidTest/java/com/framebynavin/app/ui/V343UnifiedRecoveryEscrowUiTest.kt` | **Stale expectation risk**: expects recovery escrow UI | Same: test expectations must honor removal and preserve Advanced local erase |

**Caveat:** This is a source review, not an independent compiler-error root cause for each file. Record actual Kotlin compile diagnostics before writing a fix; do not assume tests should be deleted or classify production behavior as broken solely because they were excluded.

## 5. Remaining product and device acceptance gates
- Diagnostic defects **AH-01**: a prior `VOICE_SPEAK_STARTED` masks later `VOICE_SPEAK_FAILED`; and **AH-02**: missing channel classified as allowed. Both reproducible via #384. Remain **unfixed**; production correction requires separate, explicit approval.
- Full-screen and voice+music parity for **21-day Challenge**: not implemented as a connected production experience in frozen source. V394 challenge preview is illustrative. **Future product work**, outside current P0 authorization.
- Physical owner/device acceptance **0/12**: locked/unlocked delivery, audible voice/music, snooze, reboot, longer idle, permission states, edit/delete and day-21/22 behavior. A connected physical handset is not available through this GitHub QA workflow.
- UI/visual QA: Spark/Mira pose consistency, cinematic scenes, widgets, first-run, settings, performance, large text, TalkBack and reduced motion still require real rendered/device acceptance.
- Inexact-alarm actual delivery latency, long OEM Doze, battery saver, multi-vendor coverage: **unverified**.
- Verify Android app-package/version, stable signing, data-preserving update and rollback on a *separate permitted test device*. Never `pm clear` or uninstall on the owner's phone.

## 6. Recommended next sequence — permission boundaries
1. **NOW / QA-only:** Freeze acceptance evidence with the exact above SHA/run mapping. Classify UI test debt as above. Reproduce specific compiler diagnostics without restoring deleted features. Document physical sign-off form. No release claims.
2. **REQUIRES FOUNDER APPROVAL:** Consider narrowly scoped fixes for AH-01 and AH-02 in a **separate production draft PR** with failing-first tests. No UX redesign or delivery schedule changes.
3. **REQUIRES FOUNDER APPROVAL:** Specify integration candidate branch and source fix adoption (#379), reconcile obsolete tests, run full JVM and complete Android instrumentation without exclusions (or explicitly justify replaced obsolete tests), test all staged features and sign/package only as permitted.
4. **HUMAN ACCEPTANCE REQUIRED:** Run PH-01 through PH-12 on a physical Android device and preserve redacted evidence. Mark each pass, fail or blocked; report wake-up sound and permission settings.
5. **RELEASE GATE:** Only after exact source, signing, rollout/rollback, privacy, and user-facing QA have all passed and founder explicitly approves release. Do not silently promote `main` or `stable`.

## 7. Scope limitations
No production code, theme, reminders, persistence, cloud schema, owner data, Android install, APK artifact or branch merge is modified by this QA document. This document is an integration **readiness plan**, not a statement that the combined candidate was built/tested or the defects were fixed.
