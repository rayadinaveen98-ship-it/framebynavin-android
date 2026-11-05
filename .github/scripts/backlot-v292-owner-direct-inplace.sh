#!/usr/bin/env bash
# Disposable Android emulator ONLY. Never run against owner's actual phone.
set -euo pipefail
test "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" = "1" || {
  echo 'FAIL: upgrade gate requires an Android emulator' >&2
  exit 1
}
runner=com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner
new_apk=output/v292/Backlot-V292-Signed-Upgrade-Test.apk
runner_apk=output/v292/app-personalStable-androidTest.apk
old_v287=output/v287/Backlot-V287-Nap-Wake-Owner-Test.apk
old_v291=output/v291/Backlot-V291-Signed-Owner-Regression.apk
for f in "$runner_apk" "$new_apk" "$old_v287" "$old_v291"; do test -s "$f"; done

run_case() {
  local cls="$1" method="$2"
  local logfile="$(mktemp)"
  if ! adb shell am instrument -w -r \
    -e hardeningTestEnvironment ci-emulator \
    -e class "com.framebynavin.app.upgrade.${cls}#${method}" "$runner" | tee "$logfile"; then
    cat "$logfile" >&2
    adb logcat -d -v threadtime | tail -140 || true
    rm -f "$logfile"; exit 1
  fi
  if ! grep -Fq 'OK (1 test)' "$logfile" || \
      grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' "$logfile"; then
    cat "$logfile" >&2
    adb logcat -d -v threadtime | tail -140 || true
    rm -f "$logfile"; exit 1
  fi
  rm -f "$logfile"
}

echo 'V287 -> V292, preserving synthetic Creator/Personal/Done history'
adb install -r "$old_v287"
adb install -r "$runner_apk"
adb shell pm clear com.framebynavin.app # ONLY on emulator, BEFORE seed
run_case V292FromV287UpgradeDeviceTest seedOnV287
adb install -r "$new_apk"  # CRITICAL: NO UNINSTALL / NO CLEAR
adb shell dumpsys package com.framebynavin.app | grep -m 1 'versionCode=292'
run_case V292FromV287UpgradeDeviceTest verifyOnV292

echo 'Disposable emulator reset to create independent V291 photo fixture'
adb uninstall com.framebynavin.app.test
adb uninstall com.framebynavin.app
adb install -r "$old_v291"
adb install -r "$runner_apk"
adb shell pm clear com.framebynavin.app # ONLY before seeding new separate scenario
run_case V292FromV291SpacePhotoUpgradeDeviceTest seedOnV291
adb install -r "$new_apk"  # CRITICAL: NO UNINSTALL / NO CLEAR
adb shell dumpsys package com.framebynavin.app | grep -m 1 'versionCode=292'
run_case V292FromV291SpacePhotoUpgradeDeviceTest verifyOnV292
echo 'PASS: signed V287 and V291 direct in-place upgrades to V292 preserved synthetic owner data'
