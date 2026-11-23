# Backlot P10O — isolate Android 15 Quickstep ANR from Backlot first-run QA

## New direct evidence
- P10M [run 45202539065](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/45202539065): debug + AndroidTest compilations **PASSED**. Revised P10J instrumented test located Home Reminder Hub, then timed out **at line 114**, waiting for `v2232-open-personal` after attempting to open the card. Only first attempt executed; full route NOT CERTIFIED.
- P10N [run 45208442130](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/45208442130) used the **same P10M APKs**, no build, and successfully collected live screens before test teardown. At 6, 12, 20, 35 and 48 seconds the real emulator showed an OS modal `Quickstep isn't responding` over Backlot. Android `dumpsys activity` at those moments reported `mCurrentFocus=Application Not Responding: com.android.launcher3` while `com.framebynavin.app/.MainActivity` was resumed. XML showed Android system `Quickstep isn't responding`, `Close app`, `Wait`. This OS-level modal intercepts touches and makes Compose UI-test navigation inconclusive.
- Captured instrumentation diagnostic later logged `Process crashed`, without sufficient evidence that **Backlot** crashed. Since P10N force-stops the app for diagnostics and launcher ANR is concurrently active, do not attribute that line to Backlot without a specific app-process crash stack.
- The Backlot Home screen visible under the ANR modal contains the Reminder Hub; the existence of the Creator/Personal switch in source is verified. **No production route mutation has been authorized or shown necessary.**

## Narrow P10O QA remedy (no build)
- Reuse **unchanged** debug + AndroidTest APKs from P10M run `45202539065`; no Gradle, no new Android compilation, no test-source or production-code change.
- Android 15 API 35 AVD with **4 cores, 4096MB RAM and 512MB heap**; allow launcher to settle before instrumentation. These hardware inputs are supported by the emulator runner.
- On startup inspect UI hierarchy for Android Quickstep/Launcher ANR; if present, capture screenshot and dismiss only the Android OS `Close app` action. If ANR persists, **FAIL as infrastructure block**, not as proof that Backlot UI failed.
- Execute the unchanged P10M first-run test **twice** on clean disposable local profiles, with no competing UI taps during the actual test; capture exact JUnit results and logcat. If any fails, capture current screen and system hierarchy and distinguish launcher ANR from test/app failure.
- Accept 2/2 exact `OK (1 test)` only if both pass. Until then P10J/P10M **remain UNVERIFIED**.

## Guardrails
No live sign-in, hosted data, full JVM suite, production signing, owner-preview APK, merges, stable promotion or release. Keep accepted stable V337 SHA `815ba69775f75b6e5f25bb838c2c17b4f09c203e` and `main` protected. QA PR targets isolated P10O branch only.
