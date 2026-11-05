#!/usr/bin/env bash
# Disposable Android emulator ONLY. Never run on a real owner's phone.
set -euo pipefail
test "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" = "1" || {
  echo 'FAIL: V294 upgrade gate requires an Android emulator' >&2
  exit 1
}
runner=com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner
old_apk=output/v292/Backlot-V292-Consolidated-Signed-Owner.apk
new_apk=output/v294/Backlot-V294-Signed-Upgrade-Candidate.apk
test_apk=output/v294/app-personalStable-androidTest.apk
for f in "$old_apk" "$new_apk" "$test_apk"; do test -s "$f"; done
run_case() {
  local cls="$1" method="$2"
  local logfile="$(mktemp)"
  adb shell am instrument -w -r \
    -e hardeningTestEnvironment ci-emulator \
    -e class "com.framebynavin.app.upgrade.${cls}#${method}" "$runner" | tee "$logfile"
  if ! grep -Fq 'OK (1 test)' "$logfile" ||
     grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' "$logfile"; then
    cat "$logfile" >&2
    adb logcat -d -v threadtime | tail -160 || true
    rm -f "$logfile"; exit 1
  fi
  rm -f "$logfile"
}
echo 'Scenario 1: V292 -> V294 Creator, Personal, Done history and weekly plan'
adb install -r "$old_apk"
adb install -r "$test_apk"
adb shell pm clear com.framebynavin.app # emulator ONLY, before fixture seed
run_case V294FromV292CreatorPersonalUpgradeDeviceTest seedOnV292
adb install -r "$new_apk" # NO uninstall / NO clear after seed
adb shell dumpsys package com.framebynavin.app | grep -m 1 'versionCode=294'
run_case V294FromV292CreatorPersonalUpgradeDeviceTest verifyOnV294
echo 'Scenario 2: independent V292 -> V294 Space, journal and private JPEG'
adb uninstall com.framebynavin.app.test
adb uninstall com.framebynavin.app # emulator ONLY, between scenarios
adb install -r "$old_apk"
adb install -r "$test_apk"
adb shell pm clear com.framebynavin.app # emulator ONLY, before fixture seed
run_case V294FromV292SpacePhotoUpgradeDeviceTest seedOnV292
adb install -r "$new_apk" # NO uninstall / NO clear after seed
adb shell dumpsys package com.framebynavin.app | grep -m 1 'versionCode=294'
run_case V294FromV292SpacePhotoUpgradeDeviceTest verifyOnV294
echo 'PASS: V292 -> V294 in-place updates preserved all synthetic data'
