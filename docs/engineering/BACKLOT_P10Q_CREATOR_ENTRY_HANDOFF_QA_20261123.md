# Backlot P10Q — first-run Creator entry handoff before Reminder Hub navigation

## Evidence, not assumptions
- P10J Android15 original first-run UI smoke: app/test compiled, flaky `v219-reminder-home-entry` tag timeout on one run; one 11s successful complete JUnit in P10L first run.
- P10M [run 45202539065](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/45202539065): corrected unmerged Home locator allowed test to progress, then failed at line 114 waiting for Creator Reminder Hub's `v2232-open-personal` after attempting Home card click. App/test Kotlin compilation PASS; one of two UI tests FAIL.
- P10N [run 45208442130](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/45208442130): real Android Quickstep system ANR dialog obstructed Backlot in one emulator run; not proof of an app crash.
- P10O [run 45209811923](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/45209811923): 4-core/4096M Android15 emulator with warmup still timed out at the same Creator Hub Personal-tab checkpoint. No ANR at pretest or timeout; old timeout not solely explainable by Quickstep.
- P10P [run 45211173722](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/45211173722): no-build captures show page 2 of the real five-page setup at 4s, **Creator Home visible at 7s and through 25s**, Reminder Hub card present on Home throughout; Hub not opened. No Quickstep modal in these images. This proves successful setup and visible Home, but does not prove the exact reason first tap was missed.
- Production source `P6CreatorEntryTransition` is a one-shot 2.3s (180ms reduced motion) **full-screen composable drawn above Creator Home after durable setup**. Its supported onClick dismisses entry. The UI test had found the underlying Home card early via `useUnmergedTree=true`; the transition could intercept the first attempted card tap. This is plausible and consistent with screenshot timing, not direct recorded tap interception.

## Narrow P10Q AndroidTest correction
- Changed **only** `P10JRealFirstRunNavigationUiTest.kt`. After finding actual Creator Home, `finishCreatorEntryHandoff()` uses the real P6 surface's supported tap-to-finish if the overlay is still visible, waits for its disappearance in unmerged semantics, then taps the actual Home Reminder Hub Surface.
- Added separate expected Creator Reminder Hub root `v2232-reminders-space-switcher` checkpoint with 20s max and Personal tab with 15s max to isolate errors quickly rather than one 90s timeout.
- No production app source changed; no direct settings manipulation; real account choice (local-only) -> companion -> original five-page Creator wizard -> Home -> Creator Reminder Hub -> Personal -> Creator path preserved.
- Build only debug+AndroidTest APK prerequisites in one isolated job because AndroidTest source changed. No Gradle clean, full JVM suites, production owner APK or signing.
- Test exactly the new first-run scenario **twice** on disposable Android 15, fresh app profile each time, with 4 vCPU / 4096MB emulator and startup warmup. Publish compiled APK artifacts for emulator-only retries when byte-identical, preventing further rebuild waste.
- PASS requires exact `OK (1 test)` twice. Until then P10J/P10M/P10Q are **NOT CERTIFIED**.
- Protected main/stable V337 SHA `815ba69775f75b6e5f25bb838c2c17b4f09c203e`, hosted services, real identities and user data unchanged. PR remains draft QA-only, not main.
