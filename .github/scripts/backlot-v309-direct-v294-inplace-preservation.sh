#!/usr/bin/env bash
# Direct V294 -> V309 signed data-preserving upgrade. Disposable emulator only.
set -euo pipefail
test "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" = "1" || {
  echo "FAIL: this data-preservation fixture must run on a CI emulator" >&2
  exit 1
}
runner=com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner
old=output/v294/Backlot-V294-Signed-Upgrade-Candidate.apk
new=output/v309/Backlot-V309-Direct-Upgrade-QA.apk
test_apk=output/v309/app-personalStable-androidTest.apk
for p in "$old" "$new" "$test_apk"; do test -s "$p"; done

verify_one() {
  local cls="$1" method="$2" logfile
  logfile="$(mktemp)"
  adb shell am instrument -w -r \
    -e hardeningTestEnvironment ci-emulator \
    -e class "com.framebynavin.app.upgrade.${cls}#${method}" "$runner" | tee "$logfile"
  if ! grep -Fq "OK (1 test)" "$logfile" ||
     grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' "$logfile"; then
    cat "$logfile" >&2
    adb logcat -d -v threadtime | tail -160 || true
    rm -f "$logfile"
    exit 1
  fi
  rm -f "$logfile"
}
uid_of() {
  adb shell pm list packages -U com.framebynavin.app | tr -d '\r' |
    sed -n 's/.*package:com\.framebynavin\.app uid:\([0-9][0-9]*\).*/\1/p' | head -1
}

echo 'Scenario 1: V294 to V309 Creator, Personal, Done history and Friday schedule'
adb install -r "$old"
adb install -r "$test_apk"
adb shell pm clear com.framebynavin.app # disposable emulator BEFORE fixture, not after
verify_one V309FromV294CreatorPersonalUpgradeDeviceTest seedOnV294
before="$(uid_of)"; test -n "$before"
adb install -r "$new" # no uninstall/no pm clear after seed
after="$(uid_of)"; test "$before" = "$after"
adb shell dumpsys package com.framebynavin.app | grep -m 1 'versionCode=309'
verify_one V309FromV294CreatorPersonalUpgradeDeviceTest verifyOnV309

echo 'Scenario 2: V294 to V309 independent Space, journal and private JPEG'
adb uninstall com.framebynavin.app.test
adb uninstall com.framebynavin.app # emulator-only reset BETWEEN independent scenarios
adb install -r "$old"
adb install -r "$test_apk"
adb shell pm clear com.framebynavin.app # only BEFORE seeding second scenario
verify_one V309FromV294SpacePhotoUpgradeDeviceTest seedOnV294
before="$(uid_of)"; test -n "$before"
adb install -r "$new" # never clear seeded app
after="$(uid_of)"; test "$before" = "$after"
adb shell dumpsys package com.framebynavin.app | grep -m 1 'versionCode=309'
verify_one V309FromV294SpacePhotoUpgradeDeviceTest verifyOnV309

echo 'PASS: direct signed V294-to-V309 upgrade retained Creator, Personal, weekly slots, Space and private JPEG data'
