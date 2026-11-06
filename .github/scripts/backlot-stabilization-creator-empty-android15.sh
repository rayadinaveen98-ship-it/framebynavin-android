#!/usr/bin/env bash
set -euo pipefail
test "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" = "1" || {
  echo "FAIL: screenshot acceptance only authorized on disposable emulator" >&2
  exit 1
}
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell pm clear com.framebynavin.app
log="$(mktemp)"
trap 'rm -f "$log"' EXIT
adb shell am instrument -w -r \
  -e hardeningTestEnvironment ci-emulator \
  -e class com.framebynavin.app.ui.BacklotStabilizationEmptyCreatorUiTest \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner | tee "$log"
if ! grep -Fq 'OK (1 test)' "$log" ||
   grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' "$log"; then
  cat "$log" >&2
  adb logcat -d -v threadtime | tail -120 || true
  exit 1
fi
mkdir -p output/stabilization-creator-empty
adb pull /sdcard/Android/data/com.framebynavin.app/files/stabilization-visual-audit/. output/stabilization-creator-empty/
test "$(find output/stabilization-creator-empty -maxdepth 1 -name '*.png' | wc -l)" -eq 1
(cd output/stabilization-creator-empty && sha256sum *.png > SHA256SUMS.txt)
echo "PASS: minimal first-run UI, preserved History and Activity Center links, no change to reminder data"
