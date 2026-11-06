#!/usr/bin/env bash
# V309: strict synthetic data-preservation QA. Disposable Android 15 emulator ONLY.
set -euo pipefail
test "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" = "1" || {
  echo 'FAIL: signed V309 owner upgrade gate is emulator-only' >&2
  exit 1
}
runner=com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner
old=output/v305/Backlot-V305-Signed-Owner-Preview-296.apk
new=output/v309/Backlot-V309-Signed-Owner-Activity-Review.apk
test_apk=output/v309/app-personalStable-androidTest.apk
for file in "$old" "$new" "$test_apk"; do test -s "$file"; done
run_fixture() {
  local cls="$1" method="$2"
  local logfile
  logfile="$(mktemp)"
  adb shell am instrument -w -r \
    -e hardeningTestEnvironment ci-emulator \
    -e class "com.framebynavin.app.upgrade.${cls}#${method}" "$runner" | tee "$logfile"
  if ! grep -Fq 'OK (1 test)' "$logfile" ||
     grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' "$logfile"; then
    cat "$logfile" >&2
    adb logcat -d -v threadtime | tail -140 || true
    rm -f "$logfile"
    exit 1
  fi
  rm -f "$logfile"
}
uid_of() {
  adb shell pm list packages -U com.framebynavin.app | tr -d '\r' |
    sed -n 's/.*package:com\.framebynavin\.app uid:\([0-9][0-9]*\).*/\1/p' | head -1
}

echo 'Scenario 1: V305 to V309 Creator project, Personal reminders, explicit Done and weekly schedule'
adb install "$old"
adb install -r "$test_apk"
adb shell pm clear com.framebynavin.app # only BEFORE fixture seed
run_fixture V309FromV305CreatorPersonalUpgradeDeviceTest seedOnV305
before="$(uid_of)"; test -n "$before"
adb install -r "$new" # no uninstall / no clear after seed
after="$(uid_of)"; test "$before" = "$after"
adb shell dumpsys package com.framebynavin.app | grep -m 1 'versionCode=309'
run_fixture V309FromV305CreatorPersonalUpgradeDeviceTest verifyOnV309

echo 'Scenario 2: independent V305 to V309 Space, journal and physical private JPEG'
adb uninstall com.framebynavin.app.test # emulator-only between synthetic scenarios
adb uninstall com.framebynavin.app
adb install "$old"
adb install -r "$test_apk"
adb shell pm clear com.framebynavin.app # only BEFORE fixture seed
run_fixture V309FromV305SpacePhotoUpgradeDeviceTest seedOnV305
before="$(uid_of)"; test -n "$before"
adb install -r "$new" # never clear app data
after="$(uid_of)"; test "$before" = "$after"
adb shell dumpsys package com.framebynavin.app | grep -m 1 'versionCode=309'
run_fixture V309FromV305SpacePhotoUpgradeDeviceTest verifyOnV309

echo 'PASS: two independent signed in-place upgrades preserving Creator, Personal, Space and photo bytes'
