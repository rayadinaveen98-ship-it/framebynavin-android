# Backlot P10K — P10J first-run failure, evidence isolation (no build)

## Verified observations

- P10J [run 45182330346](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/45182330346): debug APK compilation **PASS**, AndroidTest APK compilation **PASS**, real MainActivity first-run Android 15 instrumentation **FAILED**.
- Exactly one P10J test timed out after 90,000 ms at `P10JRealFirstRunNavigationUiTest.kt:80`, which is `awaitTag("v219-reminder-home-entry")`, immediately after clicking the last step of the original five-page Creator wizard.
- Earlier steps in the same test reached the initial account screen, local-only continuation, companion selection and all five setup pages; the failure point does **not** prove the setup was saved successfully, and the rest of the Home/Personal/Creator navigation sequence was not reached.
- No fatal Backlot crash is established by the instrumentation output. The run's screenshot was captured after JUnit teardown and shows `Quickstep isn't responding` (system launcher), which is **not a trustworthy screenshot of Backlot at the moment of failure**.
- Source review: `CreatorOsSettingsStore.completeCreatorSetup()` commits the normalized creator profile and onboarding flag atomically and verifies the readback. `CreatorProfile.isComplete` requires nonblank Creator mode, one platform and primary goal; displayName is optional. These appear selected in test source; diagnosis must inspect actual on-device state instead of guessing.

## P10K diagnostic gate

- Reuse exact debug + AndroidTest APK bytes from P10J's already-successful build, [P10J run 45182330346](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/45182330346), artifact `Backlot-P10J-Disposable-App-and-Test-APKs`.
- **No Gradle, no compilation, no test edits, no production code edits**.
- A disposable Android 15 emulator starts the unchanged first-run instrumentation and captures screenshots at multiple points *while the test is running*, an optional Android UI hierarchy and logcat. Terminating the test after evidence capture is a diagnostic act, not a test pass.
- P10K success means only **useful evidence was captured**. P10J remains **FAILED / NOT CERTIFIED** pending an accurate fix and verified replay.
- This branch and draft PR target only the isolated P10K QA base, never `main` or stable V337. No production auth, Google account, hosted data mutation, personalStable APK or merge/release authorization.

## Next decision

Inspect live screenshots and, if available, UI hierarchy, to determine whether last-page setup failed to save, the Creator Home is covered by a transition or guided overlay, the reminder card is not in the visible semantics tree, or an OS issue interfered. Address the narrow root cause and avoid rerunning any completed Gradle or JVM gate without a code/test source change.
