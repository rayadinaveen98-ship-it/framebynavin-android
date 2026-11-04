#!/usr/bin/env bash
# Android-emulator-runner executes each YAML "script:" line separately.
# Keep the full V288 -> V290 preservation sequence in one bash process.
set -euo pipefail

V288_APK=output/v288/Backlot-V288-Personal-OS-Owner-Test.apk
V290_APK=output/v290/Backlot-V290-Private-Spaces-Owner-Test.apk
TEST_APK=app/build/outputs/apk/androidTest/personalStable/app-personalStable-androidTest.apk
CLASS=com.framebynavin.app.upgrade.V290InPlaceUpgradeDeviceTest
RUNNER=com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner

test -s "$V288_APK"
test -s "$V290_APK"
test -s "$TEST_APK"

echo 'STEP 1: Install signed V288 fixture APK and test runner'
adb install -r "$V288_APK"
adb install -r "$TEST_APK"
# CI fixture only: ensure zero data from prior tests. Never do this on owner phones.
adb shell pm clear com.framebynavin.app

echo 'STEP 2: Seed synthetic Personal + Creator data on actual V288 code'
adb shell am instrument -w -r -e hardeningTestEnvironment ci-emulator \
  -e class "$CLASS#seedOnV288" "$RUNNER" | tee /tmp/backlot-v288-before-v290-upgrade.log
grep -F 'OK (1 test)' /tmp/backlot-v288-before-v290-upgrade.log
if grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' /tmp/backlot-v288-before-v290-upgrade.log; then
  echo 'Synthetic V288 seed failed' >&2
  exit 1
fi

echo 'STEP 3: Install signed V290 over V288 with -r; NO uninstall / NO data clear'
adb install -r "$V290_APK"

echo 'STEP 4: Verify V288 Personal records, history and Creator data on V290'
adb shell am instrument -w -r -e hardeningTestEnvironment ci-emulator \
  -e class "$CLASS#verifyOnV290" "$RUNNER" | tee /tmp/backlot-v290-after-upgrade.log
grep -F 'OK (1 test)' /tmp/backlot-v290-after-upgrade.log
if grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' /tmp/backlot-v290-after-upgrade.log; then
  echo 'Signed V288 to V290 in-place upgrade lost or changed synthetic data' >&2
  exit 1
fi

echo 'PASS: V288 -> V290 preserves Personal records/history and Creator project after in-place update'
