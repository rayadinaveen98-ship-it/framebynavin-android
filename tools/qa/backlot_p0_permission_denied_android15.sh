#!/usr/bin/env bash
# P0 QA only; fresh disposable Android 15 emulator and synthetic challenge.
set -euo pipefail
out=output/p0-permission-denied
mkdir -p "$out"
adb wait-for-device
adb shell getprop sys.boot_completed | grep -qx '1'
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk

run_case() {
  local test_method="$1" output="$2"
  timeout --signal=TERM --kill-after=15s 6m adb shell am instrument -w -r \
    -e hardeningTestEnvironment ci-emulator \
    -e class "com.framebynavin.app.personal.P0ChallengePermissionDeniedDeviceTest#$test_method" \
    com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
    > "$out/$output" 2>&1 || {
      cat "$out/$output"; exit 1;
    }
  cat "$out/$output"
  grep -q 'OK (1 test)' "$out/$output"
}

# A: exact alarm access denied; verify inexact fallback and real Alert Health UI.
adb shell am force-stop com.framebynavin.app || true
adb shell pm clear com.framebynavin.app > "$out/EXACT_PHASE_CLEARED_CI_DATA.txt"
adb shell pm grant com.framebynavin.app android.permission.POST_NOTIFICATIONS
adb shell appops set com.framebynavin.app SCHEDULE_EXACT_ALARM ignore
adb shell appops get com.framebynavin.app SCHEDULE_EXACT_ALARM > "$out/EXACT_PERMISSION_DENIED.txt"
run_case exactAlarmDeniedUsesHonestInexactSchedulingAndUiWarning EXACT_DENIED_FALLBACK_TEST.txt

# B: distinct synthetic challenge, exact alarm scheduled while app notifications
# are allowed; then the HOST revokes POST_NOTIFICATIONS before the due time.
adb shell am force-stop com.framebynavin.app || true
adb shell pm clear com.framebynavin.app > "$out/NOTIFICATION_PHASE_CLEARED_CI_DATA.txt"
adb shell pm grant com.framebynavin.app android.permission.POST_NOTIFICATIONS
adb shell appops set com.framebynavin.app SCHEDULE_EXACT_ALARM allow
adb shell appops get com.framebynavin.app SCHEDULE_EXACT_ALARM > "$out/NOTIFICATION_PHASE_EXACT_ACCESS.txt"
run_case prepareExactAlarmThenHostDeniesNotifications NOTIFICATION_DENIED_PREPARE_TEST.txt
due=$(sed -n 's/.*P0_DENIED_NOTIFICATION_DUE_EPOCH_MS=\([0-9][0-9]*\).*/\1/p' "$out/NOTIFICATION_DENIED_PREPARE_TEST.txt" | tail -1)
test -n "$due"
printf '%s\n' "$due" > "$out/NOTIFICATION_DUE_EPOCH_MS.txt"
adb shell pm revoke com.framebynavin.app android.permission.POST_NOTIFICATIONS
adb shell appops get com.framebynavin.app POST_NOTIFICATION > "$out/NOTIFICATION_PERMISSION_DENIED.txt"

# Test process has exited. Do not send an alarm broadcast, launch the app,
# or run instrumentation before the future OS alarm's scheduled due time.
while :; do
  now=$(date -u +%s%3N)
  if (( now >= due + 40000 )); then break; fi
  sleep 3
done
marker=$(adb shell date +%s%3N | tr -d '\r')
printf 'VERIFIER_START_EPOCH_MS=%s\n' "$marker" > "$out/VERIFIER_START.txt"
timeout --signal=TERM --kill-after=15s 5m adb shell am instrument -w -r \
  -e hardeningTestEnvironment ci-emulator \
  -e P0_VERIFIER_START_EPOCH_MS "$marker" \
  -e class "com.framebynavin.app.personal.P0ChallengePermissionDeniedDeviceTest#notificationDeniedReportsBlockedAfterNaturalOsFire" \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
  > "$out/NOTIFICATION_DENIED_OS_FIRE_TEST.txt" 2>&1 || {
    cat "$out/NOTIFICATION_DENIED_OS_FIRE_TEST.txt"; exit 1;
  }
cat "$out/NOTIFICATION_DENIED_OS_FIRE_TEST.txt"
grep -q 'OK (1 test)' "$out/NOTIFICATION_DENIED_OS_FIRE_TEST.txt"
echo 'PASS: inexact fallback is honestly shown on denied exact access; OS fire with POST_NOTIFICATIONS denied records blocked, does not post or mark Done.' > "$out/RESULT.txt"
echo 'Limits: single disposable awake Android 15 emulator, not real inexact delivery timing, Doze, OEM/physical sound or release readiness.' > "$out/LIMITS.txt"
