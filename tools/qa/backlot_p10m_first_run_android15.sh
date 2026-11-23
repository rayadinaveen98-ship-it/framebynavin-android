#!/usr/bin/env bash
# P10M: two identical real first-run navigation test replays on disposable Android 15.
# Only AndroidTest semantics locator changed. No Google login or signed owner APK.
set -euo pipefail
out="output/p10m/android15"
mkdir -p "$out"
adb wait-for-device
adb install -r output/p10m/apks/app-debug.apk
adb install -r output/p10m/apks/app-debug-androidTest.apk

for repeat in 1 2; do
  adb shell am force-stop com.framebynavin.app || true
  adb shell pm clear com.framebynavin.app
  adb logcat -c
  echo "P10M real first-run navigation, fresh app state, attempt $repeat/2"
  set +e
  timeout --signal=TERM --kill-after=5s 150s \
    adb shell am instrument -w -r \
      -e hardeningTestEnvironment ci-emulator \
      -e class com.framebynavin.app.ui.P10JRealFirstRunNavigationUiTest \
      com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
      2>&1 | tee "$out/p10m-first-run-$repeat.log"
  rc=${PIPESTATUS[0]}
  set -e
  adb logcat -d -t 2200 > "$out/p10m-first-run-$repeat-logcat.txt" || true

  if [[ $rc -ne 0 ]] ||
     ! grep -Fxq 'OK (1 test)' "$out/p10m-first-run-$repeat.log" ||
     grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|INSTRUMENTATION_ABORTED|Process crashed|shortMsg=' "$out/p10m-first-run-$repeat.log"; then
     echo "FAILED: P10M Android first-run journey in run $repeat/2" >&2
     exit 1
  fi
  echo "PASS: P10M Android first-run journey $repeat/2"
done
echo "PASS: real MainActivity first-run, Creator Home, Hub, Personal, Creator, 2/2 runs"
