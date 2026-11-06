#!/usr/bin/env bash
# Disposable Android 15 emulator only. All Personal/Space/Group fixtures synthetic.
set -euo pipefail
test "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" = "1" || {
  echo 'FAIL: V310 tests are only authorized on an emulator' >&2
  exit 1
}
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell pm clear com.framebynavin.app # only on disposable emulator, before fixtures
run_suite() {
  local cls="$1" count="$2"
  local logfile
  logfile="$(mktemp)"
  adb shell am instrument -w -r \
    -e hardeningTestEnvironment ci-emulator \
    -e class "$cls" \
    com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner | tee "$logfile"
  if ! grep -Fq "OK ($count test" "$logfile" ||
     grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' "$logfile"; then
    cat "$logfile" >&2
    adb logcat -d -v threadtime | tail -180 || true
    rm -f "$logfile"
    exit 1
  fi
  rm -f "$logfile"
}
run_suite com.framebynavin.app.ui.V310ReviewLinksUiTest 2
run_suite com.framebynavin.app.personal.V310CreateAndLinkDeviceTest 1
mkdir -p output/v310-personal-links
adb pull /sdcard/Android/data/com.framebynavin.app/files/v310-visual-audit/. output/v310-personal-links/
test "$(find output/v310-personal-links -maxdepth 1 -name '*.png' | wc -l)" -eq 1
(cd output/v310-personal-links && sha256sum *.png > SHA256SUMS.txt)
echo 'PASS: V310 reveal/selection UX, no phantom links, real Personal+Room+Group save and archived-space rejection'
