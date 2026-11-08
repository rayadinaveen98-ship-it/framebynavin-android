#!/usr/bin/env bash
# Genuine Android 15 alarm + notification delivery and pause cancellation.
# CI-only, synthetic activity on an intentionally CLEARED emulator.
set -euo pipefail
out="output/backlot-phase1-live-alarm"
mkdir -p "$out"
adb wait-for-device
adb shell getprop sys.boot_completed | grep -qx '1'
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am force-stop com.framebynavin.app || true
adb shell pm clear com.framebynavin.app > /dev/null
adb shell pm grant com.framebynavin.app android.permission.POST_NOTIFICATIONS
timeout 12m adb shell am instrument -w \
  -e class com.framebynavin.app.personal.Phase1HabitLiveAlarmDeviceTest \
  -e hardeningTestEnvironment ci-emulator \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
  > "$out/ANDROID-LIVE-ALARM-TEST.txt" 2>&1 || {
    cat "$out/ANDROID-LIVE-ALARM-TEST.txt" >&2
    exit 1
  }
cat "$out/ANDROID-LIVE-ALARM-TEST.txt"
grep -q 'OK (1 test)' "$out/ANDROID-LIVE-ALARM-TEST.txt"
printf '%s\n' "$GITHUB_SHA" > "$out/SOURCE-COMMIT.txt"
printf '%s\n' 'Disposable emulator only; actual AlarmManager token, receiver, notification, no Done and pause cancellation.' > "$out/SCOPE.txt"
echo 'PASS: real 21-day alarm delivery and revocation on Android 15 emulator'
