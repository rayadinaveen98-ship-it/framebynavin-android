#!/usr/bin/env bash
# Disposable Android 15 CI emulator. Never uninstall / pm clear after the V322 seed.
# The script runs in ONE shell process; emulator-runner executes YAML script lines separately.
set -euo pipefail
mkdir -p output/v324-owner
old="output/v322/Backlot-v322-Rebuild-Schedule.apk"
next="output/v324-owner/Backlot-V324-Phase3-Owner-Review.apk"
testapk="app/build/outputs/apk/androidTest/personalStable/app-personalStable-androidTest.apk"
test -s "$old" && test -s "$next" && test -s "$testapk"
adb wait-for-device
adb shell getprop sys.boot_completed | grep -qx 1
adb install -r "$old"
adb install -r "$testapk"
adb shell dumpsys package com.framebynavin.app > output/v324-owner/BEFORE-UPGRADE-PACKAGE.txt
grep -q "versionCode=322" output/v324-owner/BEFORE-UPGRADE-PACKAGE.txt

echo "PHASE 1: add exact V322 owner schedule and walking receipt to app's private storage"
timeout 9m adb shell am instrument -w -r \
  -e class com.framebynavin.app.personal.BacklotV322To324SignedUpgradeTest#phase1SeedRealV322ScheduleWalkingChallengeAndCreatorData \
  -e hardeningTestEnvironment ci-emulator \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
  > output/v324-owner/V322-SEEDED-RECORDS.txt 2>&1 || { cat output/v324-owner/V322-SEEDED-RECORDS.txt >&2; exit 1; }
cat output/v324-owner/V322-SEEDED-RECORDS.txt
grep -Fq "OK (1 test)" output/v324-owner/V322-SEEDED-RECORDS.txt
adb shell am force-stop com.framebynavin.app

echo "PHASE 2: install V324 over V322; no uninstall, no clear"
adb install -r "$next"
adb shell dumpsys package com.framebynavin.app > output/v324-owner/AFTER-UPGRADE-PACKAGE.txt
grep -q "versionCode=324" output/v324-owner/AFTER-UPGRADE-PACKAGE.txt
timeout 9m adb shell am instrument -w -r \
  -e class com.framebynavin.app.personal.BacklotV322To324SignedUpgradeTest#phase2VerifyV324InPlaceInstallPreservedEveryScheduleRecordAndDay1Receipt \
  -e hardeningTestEnvironment ci-emulator \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
  > output/v324-owner/V324-DATA-SURVIVED.txt 2>&1 || { cat output/v324-owner/V324-DATA-SURVIVED.txt >&2; exit 1; }
cat output/v324-owner/V324-DATA-SURVIVED.txt
grep -Fq "OK (1 test)" output/v324-owner/V324-DATA-SURVIVED.txt

echo "PHASE 3: normal V324 app launch and second owner-data verification"
adb shell am start -W -n com.framebynavin.app/.MainActivity > output/v324-owner/V324-ACTIVITY-START.txt
sleep 4
adb shell pidof com.framebynavin.app >/dev/null
timeout 9m adb shell am instrument -w -r \
  -e class com.framebynavin.app.personal.BacklotV322To324SignedUpgradeTest#phase3VerifyAfterV324NormalActivityLaunch \
  -e hardeningTestEnvironment ci-emulator \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
  > output/v324-owner/V324-AFTER-LAUNCH-SURVIVED.txt 2>&1 || { cat output/v324-owner/V324-AFTER-LAUNCH-SURVIVED.txt >&2; exit 1; }
cat output/v324-owner/V324-AFTER-LAUNCH-SURVIVED.txt
grep -Fq "OK (1 test)" output/v324-owner/V324-AFTER-LAUNCH-SURVIVED.txt
printf '%s\n' "PASS: same signer/package; real signed V322 to V324 in-place update preserved 16 Personal schedule records, exact 21-day walking receipt and creator project after launch" \
  > output/v324-owner/UPGRADE-INSTALL-DATA-PRESERVATION.txt

# Phase 3F: run feature-level UI gates against the actually upgraded signed V324,
# never the debug APK. Only isolated synthetic Compose fixtures are used.
for spec in \
  "com.framebynavin.app.ui.Phase3SmartNextMoveUiTest PHASE3D-ANDROID 3" \
  "com.framebynavin.app.ui.Phase3HomeCompositionReviewTest PHASE3E-ANDROID 2" \
  "com.framebynavin.app.ui.V220HomeConsolidationUiTest HOME-3AB-ANDROID 5" \
  "com.framebynavin.app.ui.CreatorFocusSessionScreenUiTest FOCUS-3C-ANDROID 2"; do
  set -- $spec
  cls="$1"; log="$2"; count="$3"
  timeout 13m adb shell am instrument -w -r \
    -e class "$cls" -e hardeningTestEnvironment ci-emulator \
    -e backlotVisualProfile default \
    com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
    > "output/v324-owner/$log.txt" 2>&1 || { cat "output/v324-owner/$log.txt" >&2; exit 1; }
  cat "output/v324-owner/$log.txt"
  grep -Fq "OK ($count tests)" "output/v324-owner/$log.txt"
done

# Final evidence after all interactions: synthetic review must not change source owner data.
timeout 9m adb shell am instrument -w -r \
  -e class com.framebynavin.app.personal.BacklotV322To324SignedUpgradeTest#phase3VerifyAfterV324NormalActivityLaunch \
  -e hardeningTestEnvironment ci-emulator \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
  > output/v324-owner/POST-UI-OWNER-DATA-SURVIVED.txt 2>&1 || { cat output/v324-owner/POST-UI-OWNER-DATA-SURVIVED.txt >&2; exit 1; }
grep -Fq "OK (1 test)" output/v324-owner/POST-UI-OWNER-DATA-SURVIVED.txt

# Signed owner APK is non-debuggable: adb run-as is intentionally prohibited.
# Native UI tests verify screenshot creation in-app; the debug Android11 QA
# runner exports review PNGs separately. Do not weaken owner APK signing.
echo "PASS: V324 integrated native 3A-3E, Focus2, and V322 data-retention checks passed" > output/v324-owner/PHASE3F-ANDROID-ACCEPTANCE.txt
