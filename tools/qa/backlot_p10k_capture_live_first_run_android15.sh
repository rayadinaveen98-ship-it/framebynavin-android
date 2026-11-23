#!/usr/bin/env bash
# P10K diagnostic only, NOT a passing navigation/first-run certification.
# Reuse P10J test-only APKs; observe the real screen while the same test stalls.
set -euo pipefail
out=output/p10k/android15
mkdir -p "$out"
adb wait-for-device
adb install -r output/p10k/apks/app-debug.apk
adb install -r output/p10k/apks/app-debug-androidTest.apk
adb shell pm clear com.framebynavin.app
adb logcat -c

# Keep actual first-run test unchanged; capture before AndroidJUnitRunner
# closes the activity so the screenshot is not mistaken for the launcher.
set +e
(
  timeout --signal=TERM --kill-after=5s 100s adb shell am instrument -w -r \
    -e hardeningTestEnvironment ci-emulator \
    -e class com.framebynavin.app.ui.P10JRealFirstRunNavigationUiTest \
    com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
    > "$out/unchanged-first-run-test.log" 2>&1
  echo "$?" > "$out/instrumentation-exit-code.txt"
) &
test_pid=$!
set -e

previous=0
for target in 8 18 32 50; do
  wait_seconds=$((target - previous))
  sleep "$wait_seconds"
  previous="$target"
  echo "P10K live snapshot at elapsed $target seconds"
  timeout 15s adb exec-out screencap -p > "$out/live-$target-s.png" || true
  adb shell dumpsys activity activities > "$out/activity-$target-s.txt" || true
done

# Optional hierarchy at the stalled screen, before test teardown.
timeout 20s adb shell uiautomator dump /sdcard/p10k-window.xml > "$out/ui-hierarchy-dump.log" 2>&1 || true
timeout 10s adb pull /sdcard/p10k-window.xml "$out/window-hierarchy.xml" >/dev/null 2>&1 || true

# Evidence collection only. Do not claim test passing.
adb shell am force-stop com.framebynavin.app || true
kill "$test_pid" 2>/dev/null || true
wait "$test_pid" 2>/dev/null || true
adb logcat -d -t 4500 > "$out/live-diagnostic-logcat.txt" || true
test -s "$out/live-18-s.png"
test -s "$out/live-32-s.png"
echo "P10K evidence captured. P10J remains FAILED / UNCERTIFIED."
