# Backlot 1.0 — D8 Android 15 emulator-only recovery evidence

Date: 2026-11-22

## Decision and evidence
- **GATE PASSED: Android 15 Lab startup smoke**, not physical device certification, stable promotion or production release.
- Founder authorized reattempt on the previously verified Lab APK without rerunning all compilation/JVM/lint.
- Previous run 45005329429: focused greeting tests and full JVM unit suite passed, previously compiled Lab APK SHA256/package/signer passed, signed owner-preview APK compiled and identity checks passed; Android 15 job failed BEFORE app launch.
- Root cause of previous Android 15 failure was CI environment: Ubuntu runner reported hardware acceleration unavailable; Android Emulator 37.3.10 exited with `Software CPU emulation (-accel off) is not supported` before Backlot could run. Earlier 900-second wait was misleading; it was NOT evidence of an app crash.
- Corrective **CI-only** workflow: `.github/workflows/backlot-d8-v1-emulator-only-kvm.yml`. Uses `/dev/kvm` udev permissions, proves KVM before launching emulator, reads prior artifact `Backlot-1-0-D8-VERIFIED-LAB-APKS-NOT-RELEASE`, rechecks original SHA-256 and unchanged app/build inputs, then runs existing `.github/scripts/backlot-lab-startup-smoke.sh` unmodified.
- Run **45012361033**, job **137177241392**, conclusion **success**; source SHA of run **46a4b3195f95c48911cdadbc57af2b3cc260d0ab**; artifact **14750626110** `Backlot-1-0-D8-ANDROID15-KVM-ONLY-EVIDENCE`.
- Log evidence: `PASS: runner can use KVM`; cached `app-debug.apk: OK`; Android emulator booted; `Starting: Intent { cmp=com.framebynavin.app.lab/com.framebynavin.app.MainActivity }`; `Status: ok`; `PASS: Backlot Lab launches to foreground, survives startup and reports no package fatal exception.` Success gate also confirms process and screenshot artifacts exist.
- **No Gradle invocation**, no new app APK build, no rerun of JVM, no lint rerun, no network/backend write, no `main` change. Changes since verified original app build are test assertions, CI workflow and docs, not app code.
- The separately built, **not certified** owner-preview APK artifact **14749642649** is `Backlot-1-0-D8-OWNER-PREVIEW-NOT-CERTIFIED`, from run **45005329429**, source SHA **0971dc6cfa7750726f33f08cdc08cedac2ffd485**. Owner app identity `com.framebynavin.app`, `versionName=1.0`, `versionCode=338`, label `Backlot`, existing public owner-preview signing certificate checked by CI; distinct from the isolated `com.framebynavin.app.lab` package.
- The previous overall run remains red as a historical record. Its one failing emulator job was superseded by the **green isolated** recovery, not rewritten or silently erased.

## Pending physical and release gates
1. Founder/owner-controlled physical device install and launch check with backup of existing user data and signing identity validation on any existing install. The Lab package and same-package owner preview serve different purposes.
2. Real-device Google sign in, reminders in Creator/Personal/21-day, music voice and repeat, Snooze/Done, double tap entrance, images/animated/video background, chosen offline speaker and personalized daypart greeting.
3. Long-idle Personal Reminder P0 and OEM power-management reliability require actual overnight/device-level evidence. Passing a short emulator smoke does not certify alarms, audio playback, foreground restrictions, lifecycle or absence of rare crashes.
4. No stable branch merge, production release, Play signing/rollout or hosted UX09 migration without separate founder approval.

## Links
- Emulator-only successful run: https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/45012361033
- Previous signed owner preview artifact: https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/45005329429/artifacts/14749642649
