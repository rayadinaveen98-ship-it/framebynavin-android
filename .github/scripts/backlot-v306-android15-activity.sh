#!/usr/bin/env bash
# V306: disposable CI emulator only. Never run against owner's device.
set -euo pipefail
test "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" = "1" || {
  echo "FAIL: V306 Activity Center UI test must run on an Android emulator" >&2
  exit 1
}
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell pm clear com.framebynavin.app
logfile="$(mktemp)"
trap 'rm -f "$logfile"' EXIT
adb shell am instrument -w -r \
  -e hardeningTestEnvironment ci-emulator \
  -e class com.framebynavin.app.ui.V306ActivityCenterUiTest \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner | tee "$logfile"
if ! grep -Fq "OK (2 tests)" "$logfile" ||
   grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' "$logfile"; then
  cat "$logfile" >&2
  adb logcat -d -v threadtime | tail -160 || true
  exit 1
fi
mkdir -p output/v306-visual-audit
adb pull /sdcard/Android/data/com.framebynavin.app/files/v306-visual-audit/. output/v306-visual-audit/
test "$(find output/v306-visual-audit -maxdepth 1 -name '*.png' | wc -l)" -eq 2
(cd output/v306-visual-audit && sha256sum *.png > SHA256SUMS.txt)
echo 'PASS: two V306 Android UI tests and 2 synthetic screenshots'
