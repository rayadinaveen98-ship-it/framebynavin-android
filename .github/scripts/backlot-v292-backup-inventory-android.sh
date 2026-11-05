#!/usr/bin/env bash
# CI emulator only, never on an owner phone. Synthetic data is disposable.
set -euo pipefail
test "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" = "1" || {
  echo 'FAIL: local inventory tests are allowed only on an Android emulator' >&2
  exit 1
}
test -s output/app-debug.apk
test -s output/app-debug-androidTest.apk
(cd output && sha256sum -c SHA256SUMS.txt)
adb install -r output/app-debug.apk
adb install -r output/app-debug-androidTest.apk

# This command clears only the CI emulator fixture, not user storage.
adb shell pm clear com.framebynavin.app

runner=com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner
log="$(mktemp)"
if ! adb shell am instrument -w -r \
    -e hardeningTestEnvironment ci-emulator \
    -e class com.framebynavin.app.cloud.V292LocalBackupInventoryDeviceTest \
    "$runner" | tee "$log"; then
  adb logcat -d -v threadtime | tail -140 || true
  rm -f "$log"
  exit 1
fi
if ! grep -Fq 'OK (4 tests)' "$log" || \
    grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' "$log"; then
  cat "$log" >&2
  adb logcat -d -v threadtime | tail -140 || true
  rm -f "$log"
  exit 1
fi
rm -f "$log"
echo 'PASS: no creation, accurate record counts, v1 no migration, corruption fail-closed'
