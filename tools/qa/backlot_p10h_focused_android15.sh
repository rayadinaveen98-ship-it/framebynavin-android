#!/usr/bin/env bash
# P10H retry: run only the previously failing accessible-Open P10 UI test.
# One bash process is required by reactivecircus/android-emulator-runner.
# No production auth, hosted services or real user data.
set -euo pipefail
output="output/p10h/android15"
mkdir -p "$output"
adb wait-for-device
adb install -r output/p10h/apks/app-debug.apk
adb install -r output/p10h/apks/app-debug-androidTest.apk
# Synthetic emulator state only.
adb shell pm clear com.framebynavin.app
adb logcat -c
set +e
adb shell am instrument -w -r \
  -e hardeningTestEnvironment ci-emulator \
  -e class 'com.framebynavin.app.reminders.P10LivingReminderVisualUiTest#explicitAccessibleOpenRevealsWithoutLegacyOneTapParticleTarget' \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
  2>&1 | tee "$output/targeted-p10-ui-test.log"
instrument_status=${PIPESTATUS[0]}
set -e
adb logcat -d -t 2000 > "$output/targeted-p10-logcat.txt" || true
if [[ $instrument_status -ne 0 ]] ||
   grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|INSTRUMENTATION_ABORTED|Process crashed|shortMsg=' "$output/targeted-p10-ui-test.log" ||
   ! grep -Fq 'OK (1 test)' "$output/targeted-p10-ui-test.log"; then
  echo "FAILED: single-case P10 accessible-Open UI retry. Inspect saved test log/logcat." >&2
  exit 1
fi
echo "PASS: the P10 accessible-Open UI test on Android 15 emulator"
