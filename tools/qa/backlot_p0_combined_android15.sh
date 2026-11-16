#!/usr/bin/env bash
# Disposable Android 15 CI emulator ONLY. Never run on a founder device.
# Run as a single Bash process: android-emulator-runner executes each script
# line as a separate /bin/sh -c invocation, discarding shell local variables.
set -euo pipefail

out=output/p0-combined/android15
mkdir -p "$out"
adb wait-for-device
adb shell getprop sys.boot_completed | grep -qx '1'
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am force-stop com.framebynavin.app || true
adb shell pm clear com.framebynavin.app > "$out/CLEARED_CI_CHANNEL_TEST.txt"
adb shell pm grant com.framebynavin.app android.permission.POST_NOTIFICATIONS

# Capture the unmodified test output even if device instrumentation fails.
if timeout --signal=TERM --kill-after=20s 5m adb shell am instrument -w -r \
  -e hardeningTestEnvironment ci-emulator \
  -e class com.framebynavin.app.personal.P0AlertHealthCorrectedDeviceTest \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
  > "$out/CORRECTED_CHANNEL_CONTRACT.txt" 2>&1; then
  cat "$out/CORRECTED_CHANNEL_CONTRACT.txt"
else
  cat "$out/CORRECTED_CHANNEL_CONTRACT.txt"
  adb logcat -d -s BacklotHabit:W BacklotHabitEvidence:W \
    > "$out/CHANNEL_DELIVERY_WARNINGS.txt" 2>&1 || true
  exit 1
fi
grep -q 'OK (1 test)' "$out/CORRECTED_CHANNEL_CONTRACT.txt"

# Snooze test clears only this disposable emulator package, sets the exact
# alarm appop and waits for a naturally scheduled ten-minute OS redelivery.
bash tools/qa/backlot_p0_actual_snooze_android15.sh
printf '%s\n' 'PASS: corrected missing-channel contract and genuine OS 10-minute Snooze on the same integrated APK.' > "$out/RESULT.txt"
