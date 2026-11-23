#!/usr/bin/env bash
# P10Q: two P6-entry-gated real first-run navigation test replays on disposable Android 15.
# Only AndroidTest Home-to-Hub handoff logic changed. No Google login or signed owner APK.
set -euo pipefail
out="output/p10q/android15"
mkdir -p "$out"
adb wait-for-device
adb install -r output/p10q/apks/app-debug.apk
adb install -r output/p10q/apks/app-debug-androidTest.apk

for repeat in 1 2; do
  adb shell am force-stop com.framebynavin.app || true
  adb shell pm clear com.framebynavin.app
  adb logcat -c
  echo "P10Q real first-run navigation, fresh app state, attempt $repeat/2"
  set +e
  timeout --signal=TERM --kill-after=5s 150s \
    adb shell am instrument -w -r \
      -e hardeningTestEnvironment ci-emulator \
      -e class com.framebynavin.app.ui.P10JRealFirstRunNavigationUiTest \
      com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
      2>&1 | tee "$out/p10q-first-run-$repeat.log"
  rc=${PIPESTATUS[0]}
  set -e
  adb logcat -d -t 2200 > "$out/p10q-first-run-$repeat-logcat.txt" || true

  if [[ $rc -ne 0 ]] ||
     ! grep -Fxq 'OK (1 test)' "$out/p10q-first-run-$repeat.log" ||
     grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|INSTRUMENTATION_ABORTED|Process crashed|shortMsg=' "$out/p10q-first-run-$repeat.log"; then
     echo "FAILED: P10Q Android first-run journey in run $repeat/2" >&2
     exit 1
  fi
  echo "PASS: P10Q Android first-run journey $repeat/2"
done
echo "PASS: real MainActivity first-run, Creator Home, Hub, Personal, Creator, 2/2 runs"
