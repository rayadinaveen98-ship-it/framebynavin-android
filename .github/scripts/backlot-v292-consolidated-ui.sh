#!/usr/bin/env bash
# Disposable Android 15 emulator only. Never clear app data on a real phone.
set -euo pipefail
test "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" = "1" || {
  echo 'FAIL: consolidated UI tests may run only on disposable emulator' >&2
  exit 1
}
(cd output && sha256sum -c SHA256SUMS.txt)
adb install -r output/app-debug.apk
adb install -r output/app-debug-androidTest.apk
runner=com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner
for entry in \
    com.framebynavin.app.ui.V289PersonalSpacesUiTest:6 \
    com.framebynavin.app.ui.V292SpaceMetricUiTest:1 \
    com.framebynavin.app.cloud.V292LocalBackupInventoryDeviceTest:4; do
  classname="${entry%:*}"
  expected="${entry##*:}"
  adb shell pm clear com.framebynavin.app # emulator-only clean synthetic setup
  logfile="$(mktemp)"
  if ! adb shell am instrument -w -r \
      -e hardeningTestEnvironment ci-emulator \
      -e class "$classname" "$runner" | tee "$logfile"; then
    adb logcat -d -v threadtime | tail -150 || true
    rm -f "$logfile"; exit 1
  fi
  if [ "$expected" -eq 1 ]; then marker='OK (1 test)'
  else marker="OK ($expected tests)"; fi
  if ! grep -Fq "$marker" "$logfile" || \
      grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' "$logfile"; then
    cat "$logfile" >&2
    adb logcat -d -v threadtime | tail -150 || true
    rm -f "$logfile"; exit 1
  fi
  rm -f "$logfile"
done
echo 'PASS: 11 Android UI/data coverage tests across consolidated V292'
