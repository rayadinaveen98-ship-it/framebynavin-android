#!/usr/bin/env bash
# P10H: run only five synthetic P10 replacement-presentation UI tests.
# One bash process is required by reactivecircus/android-emulator-runner.
# No production auth, hosted services or real user data.
set -euo pipefail
output="output/p10h/android15"
mkdir -p "$output"
adb wait-for-device
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
# Synthetic emulator state only.
adb shell pm clear com.framebynavin.app
adb logcat -c
set +e
adb shell am instrument -w -r \
  -e hardeningTestEnvironment ci-emulator \
  -e class com.framebynavin.app.reminders.P10LivingReminderVisualUiTest \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
  2>&1 | tee "$output/targeted-p10-ui-test.log"
instrument_status=${PIPESTATUS[0]}
set -e
adb logcat -d -t 2000 > "$output/targeted-p10-logcat.txt" || true
if [[ $instrument_status -ne 0 ]] ||
   grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|INSTRUMENTATION_ABORTED|Process crashed|shortMsg=' "$output/targeted-p10-ui-test.log" ||
   ! grep -Fq 'OK (5 tests)' "$output/targeted-p10-ui-test.log"; then
  echo "FAILED: five-case targeted P10 Android UI smoke. Inspect saved test log/logcat." >&2
  exit 1
fi
echo "PASS: five targeted Creator/Personal P10 UI cases on Android 15 emulator"
