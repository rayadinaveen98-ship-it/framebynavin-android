#!/usr/bin/env bash
# This script MUST run as ONE shell process. android-emulator-runner v2 invokes
# each line of script: separately, so shell variables assigned on earlier lines
# disappear (previous CI failed at 'adb: filename doesn't end .apk: ').
#
# Disposable Android 15 emulator ONLY. Verify genuine package, signature,
# install baseline, seed target-app private data, upgrade IN PLACE, read it
# back, and confirm target launch. Never uninstall or clear after baseline.
set -euo pipefail
base="output/build320/Backlot-PhaseB-Personal-Stable-Candidate.apk"
next="app/build/outputs/apk/personalStable/app-personalStable.apk"
testapk="app/build/outputs/apk/androidTest/personalStable/app-personalStable-androidTest.apk"
mkdir -p output
test -s "$base" && test -s "$next" && test -s "$testapk"
adb wait-for-device
adb shell getprop sys.boot_completed | grep -qx 1

adb install -r "$base"
adb install -r "$testapk"
adb shell dumpsys package com.framebynavin.app > output/BEFORE-UPGRADE-PACKAGE.txt
grep -q "versionCode=320" output/BEFORE-UPGRADE-PACKAGE.txt
adb shell am start -W -n com.framebynavin.app/.MainActivity
sleep 5
timeout 8m adb shell am instrument -w \
  -e class com.framebynavin.app.personal.Phase1Build320InPlaceUpgradeDeviceTest#seedBuild320LocalDataBeforeInstallUpgrade \
  -e hardeningTestEnvironment ci-emulator \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
  > output/BUILD320-SENTINEL-WRITTEN.txt 2>&1 || {
    cat output/BUILD320-SENTINEL-WRITTEN.txt >&2
    exit 1
  }
cat output/BUILD320-SENTINEL-WRITTEN.txt
grep -q 'OK (1 test)' output/BUILD320-SENTINEL-WRITTEN.txt
adb shell am force-stop com.framebynavin.app

# Same package & certificate; no uninstall, no pm clear, no data migration reset.
adb install -r "$next"
adb shell dumpsys package com.framebynavin.app > output/AFTER-UPGRADE-PACKAGE.txt
grep -q "versionCode=321" output/AFTER-UPGRADE-PACKAGE.txt
timeout 8m adb shell am instrument -w \
  -e class com.framebynavin.app.personal.Phase1Build320InPlaceUpgradeDeviceTest#verifySameLocalDataAfterUpgradeTo321 \
  -e hardeningTestEnvironment ci-emulator \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
  > output/BUILD321-SENTINEL-SURVIVED.txt 2>&1 || {
    cat output/BUILD321-SENTINEL-SURVIVED.txt >&2
    exit 1
  }
cat output/BUILD321-SENTINEL-SURVIVED.txt
grep -q 'OK (1 test)' output/BUILD321-SENTINEL-SURVIVED.txt
adb shell am start -W -n com.framebynavin.app/.MainActivity
sleep 5
adb shell pidof com.framebynavin.app >/dev/null
printf '%s\n' 'PASS: genuine signed Build 320 -> 321 in-place install, app-private sentinel preserved, app launched' \
  > output/IN-PLACE-INSTALL-GATE.txt
