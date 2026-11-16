# Backlot P0 — reliability certification closure record and handoff
**Date:** 2026-11-16  
**Decision status:** CI technical acceptance **PASSED**; owner smoke feedback **REPORTED POSITIVE**; physical matrix **INCOMPLETE / not independently witnessed**; production merge and final release **NOT APPROVED**. This is a documentation closure of the P0 investigation/correction stage, not a declaration of GA readiness.

## Non-negotiable references
- Frozen owner-reference V337 branch: `stable/backlot-v337-premiere-owner-testing-20261114` at `678502f4626150ea1945b81eed1593a362377454`.
- Combined corrective PR [#389](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/389), source `815ba69775f75b6e5f25bb838c2c17b4f09c203e`, **draft, unmerged**.
- QA-only packaging branch `qa/backlot-v337-p0-physical-preview-apk-20261116`, packaging commit `3706a42d518618958f52c2d2d67936f947e45916`. No product source modifications relative to the verified `815ba697` commit.
- QA packaging [run 43867609066](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/43867609066) **SUCCESS**, artifact `14228797432` (`Backlot-V337-P0-Owner-Physical-QA-APK`), 30-day GitHub Actions artifact retention. Contains an `app-personalStable` preview-signed V337 Android package, not production signing.
- Never modify the frozen branch, owner-installed APK, or cloud data as part of document work.

## Verified CI evidence — exact PR #389 SHA `815ba697`
[Combined run 43855917050](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/43855917050) **SUCCESS**:
1. `full-jvm` job `133260056279`: full `:app:testDebugUnitTest` **BUILD SUCCESSFUL**, with no JVM test exclusions. Includes both recovery/security policy corrections and Snooze precision regressions.
2. `android15-unexcluded` job `133260056758`: `:app:assembleDebug` and `:app:assembleDebugAndroidTest` **BUILD SUCCESSFUL**. All tracked Android instrumentation sources compiled; V339–V343 were **not quarantined**. Compilation does not imply all instrumented tests executed.
3. Disposable Pixel 6 Android 15 / API 35: corrected absent-notification-channel contract `OK (1 test)`, and **real** 10-minute Snooze redelivery test `OK (1 test)`; neither test used a fabricated alarm-receiver broadcast. Both device tests ran on the integrated source SHA.
4. GitHub artifacts: `Backlot-P0-Combined-Full-JVM` (ID `14221862618`), `Backlot-P0-Combined-Android15` (ID `14224510322`).
5. Recovery fail-closed contract: stale archive proof is rejected for authentication before stale-owner-decision evaluation; malformed JSON archive maps to `IllegalArgumentException` without relaxing data-security rejection.
6. Challenge Snooze: a stored `Instant` can have sub-millisecond precision while Android `AlarmManager` targets are epoch milliseconds. `Phase1HabitOccurrencePolicy` now compares the calculated occurrence's exact `toEpochMilli()` to the supplied scheduled target; no broad timing tolerance, no progress/Done mutation. The pre-fix [diagnostic run 43849102704](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/43849102704) recorded `OS_FIRED` followed by `STALE_IGNORED`, persisted `2026-11-16T11:02:00.471598Z`; notifications allowed and channel importance 4. Successful integrated real 10-minute test provides corrected behavior evidence.
7. Voice delivery health now uses latest observed start/failure evidence per exact occurrence; a missing Android notification channel is no longer reported as allowed.

## Additional isolated diagnostic gates — *not* re-executed on full combined SHA
- [#380](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/380) actual timed OS challenge alarm: CI run `43813809584` success.
- [#381](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/381) alarm recovery after real emulator reboot: run `43819066388` success.
- [#382](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/382) challenge alarm during forced deep Doze: run `43822975226` success.
- [#383](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/383) denied exact-alarm / notification permissions: run `43825299522` success.
- [#384](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/384) **characterization** tests: run `43839109573` success means **old defects reproduced**, not that fixes were validated. Corrections are separately covered by PR #389.
These run results attach to the exact isolated audit commits, not magically to PR #389, OEMs, or all future APKs.

## Owner physical acceptance: evidence strength
- After downloading the signed QA APK and receiving a seven-part checklist, founder replied: **"ok all good"**.
- Record as `OWNER_REPORTED_SMOKE_PASS` for this owner QA build only; the transcript does **not** enumerate exact device model, Android version, each checklist outcome, screenshots, installation-over-old success, day/time of Snooze, screen-lock conditions, reboot, OEM idle, TalkBack, voice+music quality, crash logs, signing identity, or retained-record counts. Do not manufacture these as certified.
- This feedback supports closing the investigation/handoff phase; it does **not** close physical, compatibility, security and release operations gates.

## P0 issue disposition
| Issue | Evidence | Status |
| --- | --- | --- |
| Two old recovery regression failures | Existing assertions corrected + complete integrated JVM pass | CI verified |
| Missing-channel health false positive | Android 15 corrected-channel contract `OK (1 test)` | CI verified |
| Voice failure masked by earlier speech event | Per-occurrence chronology regression and full JVM pass | CI verified; audible TTS not measured |
| Snoozed OS occurrence falsely judged stale | Pre-fix ledger failure, millisecond fix, true 10-min Android 15 acceptance | CI verified + owner general positive feedback |
| V339–V343 older Android UI tests would not compile | Five test-only Kotlin/Compose repairs and full AndroidTest compilation | CI compiler verified; not all UI tests executed |
| CI emulator multiline shell lost variables | Replaced with one invoked Bash script in integrated workflow | CI verified |
| App data preservation, lockscreen presentation, OEM battery idle, music, accessibility | Owner generic positive smoke; no detailed physical record | **OPEN** |

## Explicitly preserved scope
- No settings Backup/Restore entry-point resurrection. Keep `Advanced > Start fresh on this phone` / local erasure behind confirmation; **do not execute it** during acceptance.
- Legacy non-exported `BackupActivity` and `PersonalBackupActivity` remain in the manifest despite no user-facing Settings links; reachability/security review is a separate decision. Do not delete as “cleanup” without approval.
- No changes to user data, DataStore format, Android manifest permissions, widgets, stage controls, companion assets, cloud migrations, production signer, releases, branch protection or existing owner installation.
- Public `personalStable` preview signer is **not** an appropriate production Play key.

## Sign-off and next gate
- **P0 investigation / implementation / integrated CI:** documentation accepted for closure review; tests certified at exact `815ba697`.
- **Owner physical smoke:** self-reported positive; detailed acceptance and reproducible device matrix not recorded.
- **Stable/main merge:** not authorized in this stage.
- **Full six-stage UX, 21-day Premiere production route, challenge voice+music, visual QA, accessibility, release signing, migration/reboot/rollback:** not certified. See six-stage audit and upgrade gate documents.
