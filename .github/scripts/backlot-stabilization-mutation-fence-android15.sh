#!/usr/bin/env bash
set -euo pipefail
test "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" = "1" || {
  echo "FAIL: only disposable emulator may run data-preservation mutation tests" >&2; exit 1
}
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell pm clear com.framebynavin.app
log="$(mktemp)"
trap 'rm -f "$log"' EXIT
adb shell am instrument -w -r \
  -e hardeningTestEnvironment ci-emulator \
  -e class com.framebynavin.app.personal.StabilizationPersonalMutationGateDeviceTest \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner | tee "$log"
grep -Fq 'OK (1 test)' "$log"
if grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' "$log"; then exit 1; fi
echo 'PASS: existing Personal, Challenge and Group records survive pending recovery lock'
