# Backlot P10M — First-run Android UI test locator reliability

## Why P10M was needed
1. P10J [run 45182330346](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/45182330346): debug and instrumentation APK **COMPILED**, real first-run JUnit **FAILED** at `P10JRealFirstRunNavigationUiTest.kt:80`, waiting for `v219-reminder-home-entry` tag.
2. P10K [run 45197511085](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/45197511085): no-build live screenshots **verified actual Creator Home and REMINDER HUB were on screen** during that timeout. This is not evidence that setup saved incorrectly.
3. P10L [run 45198975854](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/45198975854): original unchanged JUnit **PASSED** in 11.167 seconds (`OK (1 test)`) after full first-run/Creator/Personal navigation. The separate P10L shell harness then tried an extra tap after JUnit had closed Backlot, causing a misleading overall FAIL at Android Launcher. That job **did not** establish repeatability.
4. P10L repeatability [run 45200907891](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/45200907891): first of two independent fresh-profile JUnit attempts **FAILED** at the same `awaitTag("v219-reminder-home-entry")` line 80 after 90 seconds. Second attempt appropriately skipped. Therefore original test is inconsistent, with a prior genuine pass and multiple timeouts; it is NOT certified.

## Narrow code change
Only `app/src/androidTest/java/com/framebynavin/app/ui/P10JRealFirstRunNavigationUiTest.kt` changed. Its Home wait now accepts either the actual visible label `REMINDER HUB` or the existing Home card tag **in the unmerged Compose semantics tree**. Click targets the actual tag when exposed in the unmerged tree or physically taps the visible Reminder Hub text as fallback. The test still exercises **real MainActivity** from local account choice through companion, original five-page setup, Creator Home, Reminder Hub, Personal and Creator. No direct setting manipulation or app navigation shortcut, no test skips, no host manipulation.

Only AndroidTest source changes require one new build of `:app:assembleDebug :app:assembleDebugAndroidTest`; a single isolated Android 15 emulator job then runs this corrected test **twice**, each from fresh disposable local app data. Both must produce exact `OK (1 test)`, or P10M fails. Reuse published APKs for future emulator-only retries. P10F–P10I passing stages are NOT reexecuted.

## Guardrails
- No production app code, real sign-in, hosted services/data, credentials, production signing, owner-preview APK, full JVM test suite, main merge or stable V337 promotion.
- This QA PR targets isolated `feature/backlot-v337-point10m-first-run-semantics-qa-20261123` branch. No merge authorized.
- P10M is **REQUESTED / PENDING** until GitHub Actions provides actual results. Even two emulator passes will not certify visuals, audio delivery, Google login, physical device or the full Backlot product.
