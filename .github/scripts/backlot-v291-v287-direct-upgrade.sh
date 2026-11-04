#!/usr/bin/env bash
# Disposable Android 15 CI emulator only. NEVER run against an owner device.
set -euo pipefail
test "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" = "1" || {
  echo 'FAIL: direct upgrade fixture is permitted only on an emulator' >&2
  exit 1
}
old_apk=output/v287/Backlot-V287-Nap-Wake-Owner-Test.apk
new_apk=output/v291/Backlot-V291-Direct-Upgrade.apk
runner_apk=output/v291/app-personalStable-androidTest.apk
for f in "$old_apk" "$new_apk" "$runner_apk"; do test -s "$f"; done

runner=com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner
run_case() {
  local method="$1"
  local phase="$2"
  local log
  log="$(mktemp)"
  if ! adb shell am instrument -w -r \
    -e hardeningTestEnvironment ci-emulator \
    -e class "com.framebynavin.app.upgrade.V291FromV287UpgradeDeviceTest#$method" "$runner" | tee "$log"; then
    echo "FAIL: $phase instrumentation command" >&2
    adb logcat -d -v threadtime | tail -180 || true
    rm -f "$log"
    exit 1
  fi
  if ! grep -Fq 'OK (1 test)' "$log" || \
     grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' "$log"; then
    echo "FAIL: $phase did not pass" >&2
    cat "$log" >&2
    adb logcat -d -v threadtime | tail -180 || true
    rm -f "$log"
    exit 1
  fi
  rm -f "$log"
}

echo 'Install signed V287 and seed Personal reminder/Done history/Creator project'
adb install -r "$old_apk"
adb install -r "$runner_apk"
adb shell pm clear com.framebynavin.app # disposable emulator only, BEFORE seed
run_case seedOnV287 seed

echo 'Upgrade IN PLACE from signed V287 to V291: NO UNINSTALL / NO CLEAR'
adb install -r "$new_apk"
adb shell dumpsys package com.framebynavin.app | grep -E -m 1 'versionCode=291'
run_case verifyOnV291 verify
echo 'PASS: direct V287 -> V291 upgrade preserved Personal history and Creator project'
