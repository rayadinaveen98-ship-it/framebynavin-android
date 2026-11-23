# Backlot P10N — Creator Home Reminder Hub tap diagnosis (no build)

## Verified previous evidence
- P10M GitHub run [45202539065](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/45202539065): **PASS** Kotlin/debug and AndroidTest APK compilation. **FAIL** first of two Android 15 tests. It advances beyond original flaky Home-card lookup: `awaitHomeReminderHub()` succeeds, the test calls `openHomeReminderHub()`, then **90-second timeout at `P10JRealFirstRunNavigationUiTest.kt:114`**, `awaitTag("v2232-open-personal")`. The second attempt was not executed.
- The shared Creator Reminder Hub in `V157ReminderCenterUi.kt` does render `BacklotReminderHubWorkspaceTabs` with `v2232-open-personal` tag; `FrameByNavinV101BApp` wires its `onPersonal` callback to the Personal hub state. Thus the source route is present, but the actual screen after the Home click was not captured.
- The one-shot `P6CreatorEntryTransition` overlays the app after setup success, and can consume an early tap. It could explain a click that dismisses entry instead of opening Reminder Hub, but this is a **hypothesis**, not verified root cause.

## P10N diagnostic isolation
- Use unchanged debug and AndroidTest APKs from P10M run 45202539065. No Gradle, JVM or source recompile.
- Rerun the exact unchanged P10M Android 15 UI test on a disposable empty local profile. Capture the actual screen around 6, 12, 20, 35 and 48 seconds **before** test teardown; also grab the current Activity, optional accessibility hierarchy and logcat.
- A diagnostic success means **evidence was captured**, never that P10J/P10M passed. The test process will be force-stopped after evidence collection; this is not an app crash.
- Examine snapshots to distinguish an entry transition consuming the click from a Creator hub rendering/semantic tag problem. Make only a narrow test or production repair supported by this evidence.
- No Google sign-in, hosted user data, main merge, stable V337 promotion, owner-preview APK, full test suite or release.

Draft QA PR targets isolated P10N diagnostic branch only, not `main`.
