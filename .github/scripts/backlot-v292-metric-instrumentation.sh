#!/usr/bin/env bash
# Disposable Android 15 emulator, not an owner phone.
set -euo pipefail
test "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" = "1" || {
  echo 'Tests may run only on an emulator' >&2
  exit 1
}
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
runner=com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner

for test_case in \
  com.framebynavin.app.ui.V292SpaceMetricUiTest:1 \
  com.framebynavin.app.ui.V289PersonalSpacesUiTest:6; do
  cls="${test_case%:*}"
  expected="${test_case##*:}"
  echo "V292 Android test $cls"
  adb shell pm clear com.framebynavin.app
  logfile="$(mktemp)"
  if ! adb shell am instrument -w -r \
      -e hardeningTestEnvironment ci-emulator \
      -e class "$cls" "$runner" | tee "$logfile"; then
    adb logcat -d -v threadtime | tail -160 || true
    rm -f "$logfile"
    exit 1
  fi
  if [ "$expected" -eq 1 ]; then
    expected_text="OK (1 test)"
  else
    expected_text="OK ($expected tests)"
  fi
  if ! grep -Fq "$expected_text" "$logfile" || \
    grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' "$logfile"; then
    echo "Android test mismatch for $cls" >&2
    cat "$logfile" >&2
    adb logcat -d -v threadtime | tail -160 || true
    rm -f "$logfile"
    exit 1
  fi
  rm -f "$logfile"
done
echo 'PASS: explicit offline metrics and existing Spaces UI regressions'
