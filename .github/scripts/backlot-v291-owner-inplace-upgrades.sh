#!/usr/bin/env bash
# Two independent Android 15 synthetic-data upgrade tests. EMULATOR ONLY.
# Never uninstall, clear, or reset an owner's phone. No cloud access.
set -euo pipefail
test "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" = "1" || {
  echo 'FAIL: upgrade fixture is restricted to disposable emulators' >&2
  exit 1
}

old288=output/v288/Backlot-V288-Personal-OS-Owner-Test.apk
old290=output/v290/Backlot-V290-Private-Spaces-Owner-Test.apk
new291=output/v291/Backlot-V291-Upgrade-Preservation.apk
test_apk=output/v291/app-personalStable-androidTest.apk
for file in "$old288" "$old290" "$new291" "$test_apk"; do test -s "$file"; done
runner=com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner

run_one() {
  local full_method="$1"
  local label="$2"
  local log_file
  log_file="$(mktemp)"
  echo "UPGRADE FIXTURE: $label"
  if ! adb shell am instrument -w -r \
      -e hardeningTestEnvironment ci-emulator \
      -e class "$full_method" "$runner" | tee "$log_file"; then
    echo "FAIL: Android instrumentation command for $label" >&2
    adb logcat -d -v threadtime | tail -180 || true
    rm -f "$log_file"
    exit 1
  fi
  if ! grep -Fq 'OK (1 test)' "$log_file" || \
      grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' "$log_file"; then
    echo "FAIL: $label; expected one passing instrumented test" >&2
    cat "$log_file" >&2
    adb logcat -d -v threadtime | tail -180 || true
    rm -f "$log_file"
    exit 1
  fi
  rm -f "$log_file"
}

# Case A: existing Personal + Creator fixture from V288.
echo 'CASE A: signed V288 -> V291; preserve reminders, Done history and projects'
adb install -r "$old288"
adb install -r "$test_apk"
adb shell pm clear com.framebynavin.app # disposable emulator BEFORE seeding only
run_one 'com.framebynavin.app.upgrade.V291FromV288UpgradeDeviceTest#seedOnV288' 'seed V288'
adb install -r "$new291" # upgrade IN PLACE; no clear or uninstall
adb shell dumpsys package com.framebynavin.app | grep -E -m 1 'versionCode=291'
run_one 'com.framebynavin.app.upgrade.V291FromV288UpgradeDeviceTest#verifyOnV291' 'verify Personal+Creator on V291'

# Case B needs a NEW disposable fixture, deliberately isolated from A.
# This uninstall is emulator-only BETWEEN test scenarios, NOT part of either
# upgrade and NEVER permissible on a real owner's device.
adb uninstall com.framebynavin.app.test || true
adb uninstall com.framebynavin.app
echo 'CASE B: signed V290 -> V291; preserve Spaces, notes, private JPEG bytes'
adb install -r "$old290"
adb install -r "$test_apk"
adb shell pm clear com.framebynavin.app # disposable emulator BEFORE seeding only
run_one 'com.framebynavin.app.upgrade.V291FromV290SpacePhotoUpgradeDeviceTest#seedOnV290' 'seed V290 Space+photo'
adb install -r "$new291" # upgrade IN PLACE; no clear or uninstall
adb shell dumpsys package com.framebynavin.app | grep -E -m 1 'versionCode=291'
run_one 'com.framebynavin.app.upgrade.V291FromV290SpacePhotoUpgradeDeviceTest#verifyOnV291' 'verify Space+JPEG on V291'
echo 'PASS: both signed in-place upgrades preserve synthetic owner data'
