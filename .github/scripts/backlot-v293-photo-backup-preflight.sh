#!/usr/bin/env bash
# Hard gate: disposable Android 15 emulator ONLY. Never run on an owner phone.
set -euo pipefail
test "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" = "1" || {
  echo 'FAIL: V293 backup preflight test is emulator only' >&2
  exit 1
}
test -s output/app-debug.apk
test -s output/app-debug-androidTest.apk
(cd output && sha256sum -c SHA256SUMS.txt)
adb install -r output/app-debug.apk
adb install -r output/app-debug-androidTest.apk
adb shell pm clear com.framebynavin.app # Synthetic CI fixture before tests ONLY
runner=com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner
logfile="$(mktemp)"
if ! adb shell am instrument -w -r \
  -e hardeningTestEnvironment ci-emulator \
  -e class com.framebynavin.app.personal.spaces.V293PhotoBackupPreflightDeviceTest \
  "$runner" | tee "$logfile"; then
  adb logcat -d -v threadtime | tail -180 || true
  rm -f "$logfile"
  exit 1
fi
if ! grep -Fq 'OK (3 tests)' "$logfile" || \
  grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' "$logfile"; then
  echo 'FAIL: expected three V293 backup preflight device tests' >&2
  cat "$logfile" >&2
  adb logcat -d -v threadtime | tail -180 || true
  rm -f "$logfile"
  exit 1
fi
rm -f "$logfile"
echo 'PASS: V293 photo backup preflight refuses changed or missing JPEG files'
