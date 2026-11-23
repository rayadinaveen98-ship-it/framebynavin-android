#!/usr/bin/env bash
# Backlot P10J: one real MainActivity first-run/navigation smoke on disposable CI emulator.
# No signed owner APK, real account authentication or hosted data mutation.
set -euo pipefail
out=output/p10j/android15
mkdir -p "$out"
adb wait-for-device
adb install -r output/p10j/apks/app-debug.apk
adb install -r output/p10j/apks/app-debug-androidTest.apk
# Disposable synthetic CI emulator state only.
adb shell pm clear com.framebynavin.app
adb logcat -c
set +e
timeout --signal=TERM --kill-after=20s 600s \
  adb shell am instrument -w -r \
    -e hardeningTestEnvironment ci-emulator \
    -e class com.framebynavin.app.ui.P10JRealFirstRunNavigationUiTest \
    com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
  2>&1 | tee "$out/first-run-navigation.log"
instrument_status=${PIPESTATUS[0]}
set -e
adb logcat -d -t 2400 > "$out/first-run-logcat.txt" || true
adb exec-out screencap -p > "$out/final-emulator-screen.png" || true
if [[ $instrument_status -ne 0 ]] ||
  grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|INSTRUMENTATION_ABORTED|Process crashed|shortMsg=' "$out/first-run-navigation.log" ||
  ! grep -Fq 'OK (1 test)' "$out/first-run-navigation.log"; then
  echo 'P10J Android 15 REAL first-run navigation smoke FAIL; inspect exact step/stack/screenshot' >&2
  exit 1
fi
echo 'PASS: local account -> companion -> 5-page setup -> Creator Home -> Reminders -> Personal -> Creator'
