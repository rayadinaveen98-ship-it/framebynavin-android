#!/usr/bin/env bash
# P10N: observe P10M's exact unchanged MainActivity UI test mid-flight.
# Reuse previously-built P10M APKs; screenshots are not a test pass.
set -euo pipefail
out="output/p10n/android15"
mkdir -p "$out"
adb wait-for-device
adb install -r output/p10n/apks/app-debug.apk
adb install -r output/p10n/apks/app-debug-androidTest.apk
adb shell pm clear com.framebynavin.app
adb logcat -c

set +e
(
  timeout --signal=TERM --kill-after=5s 120s adb shell am instrument -w -r \
    -e hardeningTestEnvironment ci-emulator \
    -e class com.framebynavin.app.ui.P10JRealFirstRunNavigationUiTest \
    com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
    > "$out/unchanged-p10m-first-run.log" 2>&1
  echo "$?" > "$out/instrumentation-exit.txt"
) &
test_pid=$!
set -e

previous=0
for target in 6 12 20 35 48; do
  sleep "$((target - previous))"
  previous="$target"
  echo "P10N observation at $target seconds (while unchanged JUnit runs)"
  timeout 15s adb exec-out screencap -p > "$out/live-$target-s.png" || true
  adb shell dumpsys activity activities > "$out/activity-$target-s.txt" || true
done

# UIAutomator is optional, captured once BEFORE JUnit closes the Activity.
timeout 20s adb shell uiautomator dump /sdcard/p10n-window.xml > "$out/ui-dump.log" 2>&1 || true
timeout 12s adb exec-out cat /sdcard/p10n-window.xml > "$out/window-hierarchy.xml" || true

# Diagnostic capture only; a forced stop is not an app crash or successful test.
adb shell am force-stop com.framebynavin.app || true
kill "$test_pid" 2>/dev/null || true
wait "$test_pid" 2>/dev/null || true
adb logcat -d -t 4500 > "$out/observed-logcat.txt" || true
test -s "$out/live-12-s.png"
test -s "$out/live-20-s.png"
echo 'P10N live screenshots captured; P10M remains FAILED pending exact root-cause verification.'
