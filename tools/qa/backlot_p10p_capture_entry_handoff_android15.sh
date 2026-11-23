#!/usr/bin/env bash
# P10P: capture the full-screen P6 handoff, attempted Home Reminder Hub tap,
# and post-tap screen without rebuilding the already compiled P10M APKs.
set -euo pipefail
out="output/p10p/android15"
mkdir -p "$out"
adb wait-for-device
adb install -r output/p10p/apks/app-debug.apk
adb install -r output/p10p/apks/app-debug-androidTest.apk
adb shell pm clear com.framebynavin.app
# 4-core emulator warmup to reduce Quickstep launcher boot ANR.
sleep 18
adb logcat -c
set +e
(
  timeout --signal=TERM --kill-after=5s 120s adb shell am instrument -w -r \
    -e hardeningTestEnvironment ci-emulator \
    -e class com.framebynavin.app.ui.P10JRealFirstRunNavigationUiTest \
    com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
    > "$out/unchanged-p10m-test.log" 2>&1
  echo "$?" > "$out/test-exit.txt"
) &
test_pid=$!
set -e

previous=0
for target in 4 7 11 17 25; do
  sleep "$((target - previous))"
  previous="$target"
  echo "Snapshot at $target seconds after instrumentation start"
  timeout 12s adb exec-out screencap -p > "$out/screen-$target-s.png" || true
  timeout 10s adb shell dumpsys activity activities > "$out/activity-$target-s.txt" || true
done

timeout 20s adb shell uiautomator dump /sdcard/p10p-window.xml > "$out/ui-dump.log" 2>&1 || true
timeout 10s adb exec-out cat /sdcard/p10p-window.xml > "$out/hierarchy.xml" || true
adb shell am force-stop com.framebynavin.app || true
kill "$test_pid" 2>/dev/null || true
wait "$test_pid" 2>/dev/null || true
adb logcat -d -t 3500 > "$out/logcat.txt" || true
test -s "$out/screen-7-s.png"
test -s "$out/screen-17-s.png"
echo 'P10P evidence collected, NOT an Android first-run test pass'
