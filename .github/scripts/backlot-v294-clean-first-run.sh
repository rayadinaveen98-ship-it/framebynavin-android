#!/usr/bin/env bash
# ONLY run on disposable CI emulator. Never on a real owner's phone.
set -euo pipefail
test "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" = "1" || {
  echo 'FAIL: must run on a disposable Android emulator' >&2
  exit 1
}
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell pm clear com.framebynavin.app # emulator ONLY; before first-run test
logfile="$(mktemp)"
adb shell am instrument -w -r \
  -e hardeningTestEnvironment ci-emulator \
  -e class com.framebynavin.app.ui.V294FirstRunEmptyStateDeviceTest \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner | tee "$logfile"
if ! grep -Fq 'OK (1 test)' "$logfile" ||
   grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' "$logfile"; then
  cat "$logfile" >&2
  adb logcat -d -v threadtime | tail -180 || true
  rm -f "$logfile";exit 1
fi
rm -f "$logfile"
mkdir -p output/v294-first-run
printf 'V294 clean first-run: passed\nModels: Creator, Ideas, weekly schedule, Personal Reminders, Spaces\nOwner phone accessed: NO\nEmulator initial data: CLEARED BEFORE TEST\n' > output/v294-first-run/RESULT.txt
(cd output/v294-first-run && sha256sum RESULT.txt > SHA256SUMS.txt)
echo 'PASS: first-install contains no demo records; optional template catalog did not seed work'
