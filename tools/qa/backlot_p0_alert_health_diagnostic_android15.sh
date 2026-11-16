#!/usr/bin/env bash
# QA characterization only: PASS means suspected discrepancy REPRODUCED.
set -euo pipefail
out="output/p0-alert-health-diagnostic"
mkdir -p "$out"
adb wait-for-device
adb shell getprop sys.boot_completed | grep -qx '1'
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am force-stop com.framebynavin.app || true
# Safe ONLY on the disposable newly booted CI emulator.
adb shell pm clear com.framebynavin.app > "$out/CLEARED_SYNTHETIC_CI_ONLY.txt"
adb shell pm grant com.framebynavin.app android.permission.POST_NOTIFICATIONS
adb shell appops set com.framebynavin.app SCHEDULE_EXACT_ALARM allow
adb shell appops get com.framebynavin.app SCHEDULE_EXACT_ALARM > "$out/EXACT_PERMISSION.txt"

if timeout --signal=TERM --kill-after=20s 5m adb shell am instrument -w -r \
  -e hardeningTestEnvironment ci-emulator \
  -e class com.framebynavin.app.personal.P0AlertHealthDiagnosticReproductionDeviceTest \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
  > "$out/DIAGNOSTIC_REPRODUCTION.txt" 2>&1; then
  cat "$out/DIAGNOSTIC_REPRODUCTION.txt"
else
  cat "$out/DIAGNOSTIC_REPRODUCTION.txt"
  exit 1
fi

grep -q 'OK (2 tests)' "$out/DIAGNOSTIC_REPRODUCTION.txt"
grep -q 'P0_DIAGNOSTIC_REPRODUCED=VOICE_FAILURE_MASKED' "$out/DIAGNOSTIC_REPRODUCTION.txt"
grep -q 'P0_DIAGNOSTIC_REPRODUCED=MISSING_CHANNEL_REPORTED_ALLOWED' "$out/DIAGNOSTIC_REPRODUCTION.txt"
printf '%s\n' 'CHARACTERIZATION PASS: two V337 Alert Health reporting discrepancies reproduced; NOT fixed or production-certified.' > "$out/RESULT.txt"
printf '%s\n' 'No production changes. Deliberately tests current incorrect behavior. Tests must be replaced with desired-contract regressions only after founder approves a fix.' > "$out/LIMITS.txt"
