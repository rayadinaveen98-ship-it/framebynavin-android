#!/usr/bin/env bash
# Test-only, disposable Android 15 emulator. Never run on founder phone.
set -euo pipefail
out=output/p0-actual-snooze
mkdir -p "$out"
adb wait-for-device
adb shell getprop sys.boot_completed | grep -qx '1'
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am force-stop com.framebynavin.app || true
adb shell pm clear com.framebynavin.app > "$out/CLEARED_SYNTHETIC_CI_ONLY.txt"
adb shell pm grant com.framebynavin.app android.permission.POST_NOTIFICATIONS
adb shell appops set com.framebynavin.app SCHEDULE_EXACT_ALARM allow
adb shell appops get com.framebynavin.app SCHEDULE_EXACT_ALARM > "$out/EXACT_APP_OP.txt"
if timeout --signal=TERM --kill-after=20s 24m adb shell am instrument -w -r \
  -e hardeningTestEnvironment ci-emulator \
  -e class com.framebynavin.app.personal.P0ActualChallengeSnoozeDeviceTest \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
  > "$out/ACTUAL_SNOOZE_DELIVERY.txt" 2>&1; then
    cat "$out/ACTUAL_SNOOZE_DELIVERY.txt"
else
    cat "$out/ACTUAL_SNOOZE_DELIVERY.txt"
    # Diagnostic-only logcat from freshly cleared disposable CI emulator.
    adb logcat -d -s BacklotHabit:W BacklotHabitEvidence:W \
      > "$out/BACKLOT_DELIVERY_WARNINGS.txt" 2>&1 || true
    exit 1
fi
grep -q 'OK (1 test)' "$out/ACTUAL_SNOOZE_DELIVERY.txt"
printf '%s\n' 'PASS: naturally fired Android challenge alarm, real posted 10m Snooze action, naturally redelivered alarm, no Done or duplicate OS-fire.' > "$out/RESULT.txt"
printf '%s\n' 'Limits: disposable awake Android 15 emulator only; physical phone lockscreen, audio, OEM idle, and battery policies unverified.' > "$out/LIMITS.txt"
