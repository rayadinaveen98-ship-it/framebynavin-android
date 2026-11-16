#!/usr/bin/env bash
# Run by android-emulator-runner via one Bash process on a disposable CI emulator.
# NO owner data, real account, production APK, or live deployment.
set -euo pipefail
out="output/p0-actual-alarm"
mkdir -p "$out"
adb wait-for-device
adb shell getprop sys.boot_completed | grep -qx '1'
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am force-stop com.framebynavin.app || true
# Safe ONLY because the CI runner booted a fresh synthetic emulator.
adb shell pm clear com.framebynavin.app | tee "$out/CLEARED_SYNTHETIC_CI_ONLY.txt"
adb shell pm grant com.framebynavin.app android.permission.POST_NOTIFICATIONS
adb shell appops set com.framebynavin.app SCHEDULE_EXACT_ALARM allow
adb shell appops get com.framebynavin.app SCHEDULE_EXACT_ALARM > "$out/EXACT_ALARM_APPOP.txt"
echo 'Executing naturally scheduled Android AlarmManager dispatch without manual broadcast.'
if timeout --signal=TERM --kill-after=20s 10m adb shell am instrument -w -r \
  -e hardeningTestEnvironment ci-emulator \
  -e class com.framebynavin.app.personal.P0ActualChallengeAlarmClockDeviceTest \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
  > "$out/ACTUAL_ANDROID_ALARM_TEST.txt" 2>&1; then
    cat "$out/ACTUAL_ANDROID_ALARM_TEST.txt"
else
    cat "$out/ACTUAL_ANDROID_ALARM_TEST.txt"
    exit 1
fi
grep -q 'OK (1 test)' "$out/ACTUAL_ANDROID_ALARM_TEST.txt"
printf '%s\n' 'PASS: exact timed OS delivery on an awake disposable Android 15 emulator; no manual broadcast.' > "$out/RESULT.txt"
printf '%s\n' 'Limits: no real device, Doze, forced-stop recovery, reboot rearm, audible playback, or missed-alert repair certified.' > "$out/LIMITS.txt"
echo 'PASS: naturally timed 21-day challenge alarm on CI Android 15 emulator'
