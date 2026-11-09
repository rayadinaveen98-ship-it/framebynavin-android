#!/usr/bin/env bash
# Disposable Android 15 CI emulator. Never uninstall / pm clear after the V322 seed.
# The script runs in ONE shell process; emulator-runner executes YAML script lines separately.
set -euo pipefail
mkdir -p output/v323-owner
old="output/v322/Backlot-v322-Rebuild-Schedule.apk"
next="output/v323-owner/Backlot-V323-Focus-Owner-Review.apk"
testapk="app/build/outputs/apk/androidTest/personalStable/app-personalStable-androidTest.apk"
test -s "$old" && test -s "$next" && test -s "$testapk"
adb wait-for-device
adb shell getprop sys.boot_completed | grep -qx 1
adb install -r "$old"
adb install -r "$testapk"
adb shell dumpsys package com.framebynavin.app > output/v323-owner/BEFORE-UPGRADE-PACKAGE.txt
grep -q "versionCode=322" output/v323-owner/BEFORE-UPGRADE-PACKAGE.txt

echo "PHASE 1: add exact V322 owner schedule and walking receipt to app's private storage"
timeout 9m adb shell am instrument -w -r \
  -e class com.framebynavin.app.personal.BacklotV322To323SignedUpgradeTest#phase1SeedRealV322ScheduleWalkingChallengeAndCreatorData \
  -e hardeningTestEnvironment ci-emulator \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
  > output/v323-owner/V322-SEEDED-RECORDS.txt 2>&1 || { cat output/v323-owner/V322-SEEDED-RECORDS.txt >&2; exit 1; }
cat output/v323-owner/V322-SEEDED-RECORDS.txt
grep -Fq "OK (1 test)" output/v323-owner/V322-SEEDED-RECORDS.txt
adb shell am force-stop com.framebynavin.app

echo "PHASE 2: install V323 over V322; no uninstall, no clear"
adb install -r "$next"
adb shell dumpsys package com.framebynavin.app > output/v323-owner/AFTER-UPGRADE-PACKAGE.txt
grep -q "versionCode=323" output/v323-owner/AFTER-UPGRADE-PACKAGE.txt
timeout 9m adb shell am instrument -w -r \
  -e class com.framebynavin.app.personal.BacklotV322To323SignedUpgradeTest#phase2VerifyV323InPlaceInstallPreservedEveryScheduleRecordAndDay1Receipt \
  -e hardeningTestEnvironment ci-emulator \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
  > output/v323-owner/V323-DATA-SURVIVED.txt 2>&1 || { cat output/v323-owner/V323-DATA-SURVIVED.txt >&2; exit 1; }
cat output/v323-owner/V323-DATA-SURVIVED.txt
grep -Fq "OK (1 test)" output/v323-owner/V323-DATA-SURVIVED.txt

echo "PHASE 3: normal V323 app launch and second owner-data verification"
adb shell am start -W -n com.framebynavin.app/.MainActivity > output/v323-owner/V323-ACTIVITY-START.txt
sleep 4
adb shell pidof com.framebynavin.app >/dev/null
timeout 9m adb shell am instrument -w -r \
  -e class com.framebynavin.app.personal.BacklotV322To323SignedUpgradeTest#phase3VerifyAfterV323NormalActivityLaunch \
  -e hardeningTestEnvironment ci-emulator \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
  > output/v323-owner/V323-AFTER-LAUNCH-SURVIVED.txt 2>&1 || { cat output/v323-owner/V323-AFTER-LAUNCH-SURVIVED.txt >&2; exit 1; }
cat output/v323-owner/V323-AFTER-LAUNCH-SURVIVED.txt
grep -Fq "OK (1 test)" output/v323-owner/V323-AFTER-LAUNCH-SURVIVED.txt
printf '%s\n' "PASS: same signer/package; real signed V322 to V323 in-place update preserved 16 Personal schedule records, exact 21-day walking receipt and creator project after launch" \
  > output/v323-owner/UPGRADE-INSTALL-DATA-PRESERVATION.txt
