# Backlot P10H — focused Android 15 UI verification

**Founder instruction:** Continue verified post-source-staging QA without restarting the entire JVM/Gradle pipeline on a narrow failure.

## Verified prerequisite evidence
- P10F branch: `feature/backlot-v337-point10f-focused-qa-run-20261123`, successful source gate and `:app:compileDebugKotlin` in run [45151511154](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/45151511154).
- P10G focused unit test `P10ReminderVisualContractTest`: successful in run [45155679223](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/45155679223).
- Stage P10H: **TEST REQUESTED ONLY; NOT YET PASSED**.

## Test boundary
- Base branch `feature/backlot-v337-point10h-focused-android-ui-qa-20261123` descends from verified P10F/P10G commits; QA trigger PR targets only this branch, **not main**.
- Fast source gate precedes Gradle; packaging only debug app + test APKs needed by instrumented testing.
- Android 15 (API 35) emulator executes exactly five synthetic P10 Compose cases: Creator Voice, Creator Alarm, Personal Voice, Personal Alarm, explicit accessibility Open.
- Test source: `app/src/androidTest/java/com/framebynavin/app/reminders/P10LivingReminderVisualUiTest.kt`.
- No broad JVM or instrumentation suite, no personalStable owner APK, no private release signer, no deployment, and no changes to hosted/user data.
- Failure policy: stop before emulator on AndroidTest compilation failure; diagnose exact compiler/test error and rerun only affected gate. Do not claim physical-device acceptance from emulator checks.
- The accepted stable V337 SHA `815ba69775f75b6e5f25bb838c2c17b4f09c203e` and production `main` remain protected.

Workflow: `.github/workflows/backlot-p10h-focused-android-ui-qa.yml`.
Runner: `tools/qa/backlot_p10h_focused_android15.sh`.

## P10H compilation result and focused repair
- First P10H run [45159452197](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/45159452197) **FAILED** at `:app:compileDebugAndroidTestKotlin` (NOT emulator). The `:app:assembleDebug` task completed in that run. Compiler reported exactly three errors: two non-existent Compose `assertDoesNotExist` / `assertExists` import statements in `P10LivingReminderVisualUiTest.kt` and one obsolete `V202PremiumParticlesUiTest.kt` call to deleted `V195ReminderParticleScene`. D8 printed nonfatal stack-map warnings on preexisting Google Play Services Auth 21.5.0.
- P10H QA candidate removed only the two bad imports (assertion methods remain available on node interactions) and deleted the historical V202 UI test of the retired V195 renderer. The P10 replacement P10LivingReminderVisualUiTest remains; it covers five Creator/Personal/accessible-Open cases.
- Strengthened the no-JVM preflight to inspect all unit/instrumentation test sources for retired V195/V202/V218 APIs and invalid assertion imports, stopping before expensive compilation.
- **Evidence gate remains OPEN:** no claims of AndroidTest compilation success or emulator tests passing until a new GitHub run reports those results. No owner APK, merge or production service edits.

## Android 15 emulator round — actual results (run 45163457237)

- **Build successful:** `:app:assembleDebug` and `:app:assembleDebugAndroidTest` both finished successfully; no AndroidTest compile errors remained.
- **Android emulator execution:** 5 tests ran, **4 passed, 1 failed**. Passed: Creator Voice, Creator Alarm, Personal Voice, Personal Alarm. Failed: `explicitAccessibleOpenRevealsWithoutLegacyOneTapParticleTarget`, assertion `OPENED` missing after `performClick()`.
- **Root-cause diagnosis:** `performClick()` delivers a physical single-tap gesture, which the P10 entrance intentionally does not reveal on (double-tap gesture required). The accessible *semantic OnClick* action is the supported single activation for TalkBack. The test was exercising the wrong input method.
- **Focused correction:** Test now uses `.performSemanticsAction(SemanticsActions.OnClick)`, checks initial closed state and waits for Compose idle. **Production screen not changed.**
- **Avoid unnecessary reruns:** The Android CI workflow was separated into two jobs: (1) debug + instrumentation APK build and reusable artifacts; (2) Android 15 emulator running only the failed accessible-Open test (`OK (1 test)`). The four previously passed tests need not be reexecuted after a test-only fix. Emulator job can be rerun independently using unchanged APK artifacts if a transient emulator failure occurs.
- **Verification state:** Subsequent CI run [45167860087](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/45167860087) started/pending when this entry was authored. No pass claimed yet.
- **No owner APK / merge / production data modification.** This is still synthetic Android UI QA, not complete real-device acceptance.
