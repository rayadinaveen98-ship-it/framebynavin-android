# Backlot P10J — actual Android first-launch and routing QA

**Status:** STAGED / PENDING CI. Never claim Android first-run passes before actual emulator evidence.

## Verified earlier stages
- P10F `:app:compileDebugKotlin` PASSED — [45151511154](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/45151511154).
- P10G `P10ReminderVisualContractTest` PASSED — [45155679223](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/45155679223).
- P10H Android UI: Creator voice/alarm and Personal voice/alarm 4/4 PASSED in [45163457237](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/45163457237); accessibility-Open corrected 1/1 PASSED in [45171826200](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/45171826200). Both debug and AndroidTest APK artifacts compiled.
- P10I **ten selected first-run / navigation / voice orb policy test classes PASSED** in [45176708428](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/45176708428).

## Goal and exact first-run journey
Test through **real MainActivity** in an empty Android 15 emulator profile, without mocked route state:
1. Launch real Backlot, permit its initial recovery and welcome to complete; verify Account Choice.
2. Choose the **local-only** continue path. Do not use live Google Sign-in or register a cloud identity.
3. Confirm companion selection; enter original five-page Creator setup.
4. Select a valid Creator category, publishing platform and creator goal. Leave style optional and OS permissions ungranted. Finish setup once.
5. Verify Creator Home's real `v219-reminder-home-entry` exists and account, companion and five-page onboarding no longer overlay the app.
6. Use actual Home Reminder Hub card; open Personal and switch back to Creator via the real two-tab navigation.
7. Check the onboarding gates never reappear after these clicks.

## QA boundaries and failure policy
- One `P10JRealFirstRunNavigationUiTest` instrumented UI test with Android 15 API 35 emulator, fresh `pm clear com.framebynavin.app`. Background services run in the app's normal startup but **no live account login is performed**; test uses synthetic local-only state.
- The test may discover an actual product-flow bug (e.g. failed setup persistence, navigation overlay, startup recovery blocking UI). Do not bypass failures by setting `onboardingComplete` directly in the test.
- Build `:app:assembleDebug :app:assembleDebugAndroidTest` **only**; no `clean`, full JVM suite, main merge, owner-preview signed APK or release.
- Separate compile/artifact job and emulator job. Rerun emulator job only if no source changed and artifacts are reusable.
- On failure save exact instrumentation log, recent logcat and screenshot. Diagnose; fix only affected app/test code after reviewing whether failure reflects product behavior.
- Passing this one local account pathway **does not** certify real Google sign-in, cloud sync, all permissions, notification delivery, Android 11 behavior, performance, visual quality or physical-phone acceptance.
- P10J PR deliberately targets isolated QA branch, not protected stable V337 or `main`. No merges or production changes are authorized.

**Acceptance:** one valid `OK (1 test)` log at the matching GitHub SHA; no false positives from preflight/source-only tests. Following this stage, decide release-candidate freeze and consolidated final regression before owner-preview APK; physical-device visual and functional checks still required.
