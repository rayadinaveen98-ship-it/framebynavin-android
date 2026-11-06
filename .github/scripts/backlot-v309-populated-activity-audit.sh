#!/usr/bin/env bash
# Disposable emulator ONLY. The seeded records/photos have no real owner data.
set -euo pipefail
test "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" = "1" || {
  echo "FAIL: populated Activity Center screenshot QA is emulator-only" >&2
  exit 1
}
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell pm clear com.framebynavin.app
log="$(mktemp)"
trap 'rm -f "$log"' EXIT
adb shell am instrument -w -r \
  -e hardeningTestEnvironment ci-emulator \
  -e class com.framebynavin.app.ui.V309PopulatedActivityVisualQaTest \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner | tee "$log"
if ! grep -Fq 'OK (1 test)' "$log" ||
   grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' "$log"; then
  cat "$log" >&2
  adb logcat -d -v threadtime | tail -160 || true
  exit 1
fi
mkdir -p output/v309-populated-activity
adb pull /sdcard/Android/data/com.framebynavin.app/files/v309-populated-activity-audit/. output/v309-populated-activity/
test "$(find output/v309-populated-activity -maxdepth 1 -name '*.png' | wc -l)" -eq 3
(cd output/v309-populated-activity && sha256sum *.png > SHA256SUMS.txt)
echo "PASS: V309 populated Activity Center Android 15 layout and three synthetic screenshots"
