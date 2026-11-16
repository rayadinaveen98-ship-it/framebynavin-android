#!/usr/bin/env bash
# QA only: disposable CI Android 15 emulator, synthetic challenge, no owner data.
set -euo pipefail
out="output/p0-doze-alarm"
mkdir -p "$out"
trap 'adb shell dumpsys deviceidle unforce >/dev/null 2>&1 || true; adb shell dumpsys battery reset >/dev/null 2>&1 || true' EXIT
adb wait-for-device
adb shell getprop sys.boot_completed | grep -qx '1'
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am force-stop com.framebynavin.app || true
adb shell pm clear com.framebynavin.app > "$out/CLEARED_SYNTHETIC_CI_ONLY.txt"
adb shell pm grant com.framebynavin.app android.permission.POST_NOTIFICATIONS
adb shell appops set com.framebynavin.app SCHEDULE_EXACT_ALARM allow
adb shell appops get com.framebynavin.app SCHEDULE_EXACT_ALARM > "$out/EXACT_ALARM_APPOP.txt"

# The first instrumentation exits BEFORE forced Doze is activated.
timeout --signal=TERM --kill-after=20s 5m adb shell am instrument -w -r \
  -e hardeningTestEnvironment ci-emulator \
  -e class com.framebynavin.app.personal.P0DozeChallengeAlarmDeviceTest#armSyntheticAlarmBeforeForcedDeviceIdle \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
  > "$out/BEFORE_DOZE_TEST.txt" 2>&1 || {
    cat "$out/BEFORE_DOZE_TEST.txt"; exit 1;
  }
cat "$out/BEFORE_DOZE_TEST.txt"
grep -q 'OK (1 test)' "$out/BEFORE_DOZE_TEST.txt"
due=$(sed -n 's/.*P0_DOZE_DUE_EPOCH_MS=\([0-9][0-9]*\).*/\1/p' "$out/BEFORE_DOZE_TEST.txt" | tail -1)
test -n "$due"
printf '%s\n' "$due" > "$out/DUE_EPOCH_MS.txt"

adb shell dumpsys battery unplug > "$out/BATTERY_UNPLUG.txt"
adb shell dumpsys deviceidle enable all > "$out/DEVICEIDLE_ENABLE.txt"
adb shell dumpsys deviceidle force-idle > "$out/DEVICEIDLE_FORCE.txt"
sleep 3
deep_state=$(adb shell dumpsys deviceidle get deep | tr -d '\r')
printf 'FORCED_DEEP_IDLE_STATE=%s\n' "$deep_state" > "$out/DOZE_STATE_PROOF.txt"
test "$deep_state" = "IDLE" || {
  echo "Refuse false-positive: Android did not enter deep IDLE" >&2
  exit 1
}
printf 'DEEP_IDLE_START_UTC=%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" >> "$out/DOZE_STATE_PROOF.txt"
# No application activity, instrumentation or manual HABIT_FIRE broadcast
# is started between forcing idle and the scheduled alarm.
deadline=$((due + 180000))
while :; do
  now=$(date -u +%s%3N)
  if (( now >= due + 40000 )); then break; fi
  if (( now > deadline )); then echo "Doze due-time deadline exceeded" >&2; exit 1; fi
  sleep 3
done
adb shell dumpsys deviceidle get deep > "$out/DEEP_IDLE_STATE_AFTER_DUE.txt"
# Capture the timestamp BEFORE instrumentation starts so OS event evidence
# cannot be produced by waking the app for verification.
verifier_start=$(adb shell date +%s%3N | tr -d '\r')
printf 'VERIFIER_START_EPOCH_MS=%s\n' "$verifier_start" >> "$out/DOZE_STATE_PROOF.txt"
timeout --signal=TERM --kill-after=20s 5m adb shell am instrument -w -r \
  -e hardeningTestEnvironment ci-emulator \
  -e P0_DOZE_VERIFIER_START_MS "$verifier_start" \
  -e class com.framebynavin.app.personal.P0DozeChallengeAlarmDeviceTest#verifyActualOsAlarmFiredBeforeDeviceWasInspected \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
  > "$out/AFTER_DOZE_TEST.txt" 2>&1 || {
    cat "$out/AFTER_DOZE_TEST.txt"; exit 1;
  }
cat "$out/AFTER_DOZE_TEST.txt"
grep -q 'OK (1 test)' "$out/AFTER_DOZE_TEST.txt"
echo 'PASS: actual Android RTC_WAKEUP setExactAndAllowWhileIdle alarm fired during forced deep Doze before verification instrumentation.' > "$out/RESULT.txt"
echo 'Limits: temporary forced deep idle on disposable Android 15 emulator only. Not physical OEM, prolonged Doze, battery-saver policy, force-stop, audible quality, or release.' > "$out/LIMITS.txt"
