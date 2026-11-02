#!/usr/bin/env bash
set -euo pipefail
adb install -r test-artifact/app-debug.apk
adb install -r test-artifact/app-debug-androidTest.apk

run_suite() {
  local klass="$1" count="$2" label="$3" report="/tmp/rc1-api36-${3}.log"
  adb shell pm clear com.framebynavin.app
  adb shell am instrument -w -r \
    -e hardeningTestEnvironment ci-emulator \
    -e class "$klass" \
    com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner | tee "$report"
  grep -F "OK ($count tests)" "$report"
  if grep -E 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' "$report"; then
    echo "FAIL API36 suite $label"
    return 1
  fi
  echo "PASS API36 suite $label $count tests"
}

run_suite com.framebynavin.app.reminders.V283CreatorAutoRetryDeviceTest 3 creator
run_suite com.framebynavin.app.personal.V282PersonalAutomaticRetryDeviceTest 2 personal
run_suite com.framebynavin.app.cloud.V279UnifiedPersonalCloudDeviceTest 3 cloud
run_suite com.framebynavin.app.personal.V280PersonalSnoozeBackupTest 2 snooze
run_suite com.framebynavin.app.reminders.V283Phase2ReminderSafetyDeviceTest 5 reliability
run_suite com.framebynavin.app.personal.V283NotificationFallbackDeviceTest 5 fullscreen

echo 'PASS RC1 Android16 API36 20 targeted Creator/Personal tests'
