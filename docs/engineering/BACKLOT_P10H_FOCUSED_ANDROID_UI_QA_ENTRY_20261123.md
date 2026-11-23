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
