#!/usr/bin/env bash
# V308: CI-only synthetic data on a disposable Android 15 emulator.
set -euo pipefail
test "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" = "1" || {
  echo "FAIL: only disposable emulator may run v308 tests" >&2
  exit 1
}
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell pm clear com.framebynavin.app
logfile="$(mktemp)"
trap 'rm -f "$logfile"' EXIT
adb shell am instrument -w -r \
  -e hardeningTestEnvironment ci-emulator \
  -e class com.framebynavin.app.ui.V308SparkActivityDigestUiTest \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner | tee "$logfile"
if ! grep -Fq "OK (1 test)" "$logfile" ||
   grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' "$logfile"; then
  cat "$logfile" >&2
  adb logcat -d -v threadtime | tail -160 || true
  exit 1
fi
mkdir -p output/v308-visual-audit
adb pull /sdcard/Android/data/com.framebynavin.app/files/v308-visual-audit/. output/v308-visual-audit/
test "$(find output/v308-visual-audit -maxdepth 1 -name '*.png' | wc -l)" -eq 1
(cd output/v308-visual-audit && sha256sum *.png > SHA256SUMS.txt)
echo 'PASS: V308 Android UI test and one screenshot'
