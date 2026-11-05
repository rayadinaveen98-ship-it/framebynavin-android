#!/usr/bin/env bash
# CI-EMULATOR ONLY. Captures synthetic UI, never reads user's phone or cloud.
set -euo pipefail
test "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" = "1" || {
  echo "FAIL: visual capture must run only on a disposable emulator" >&2
  exit 1
}
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell pm clear com.framebynavin.app # disposable test emulator only
logfile="$(mktemp)"
if ! adb shell am instrument -w -r \
  -e hardeningTestEnvironment ci-emulator \
  -e class com.framebynavin.app.ui.V294VisualAuditDeviceTest \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner | tee "$logfile"; then
  adb logcat -d -v threadtime | tail -160 || true
  rm -f "$logfile";exit 1
fi
if ! grep -Fq "OK (4 tests)" "$logfile" ||
   grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' "$logfile"; then
  cat "$logfile" >&2
  adb logcat -d -v threadtime | tail -160 || true
  rm -f "$logfile";exit 1
fi
rm -f "$logfile"
mkdir -p output/v294-visual-audit
adb pull /sdcard/Android/data/com.framebynavin.app/files/v294-visual-audit/. output/v294-visual-audit/
test "$(find output/v294-visual-audit -maxdepth 1 -name '*.png' | wc -l)" -eq 4
(cd output/v294-visual-audit && sha256sum *.png > SHA256SUMS.txt)
echo "PASS: four synthetic V294 Android 15 screenshots captured; no owner data"
