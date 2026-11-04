#!/usr/bin/env bash
# Android-emulator-runner executes each YAML "script:" line separately.
# Keep the full V287 -> V288 preservation sequence in one bash process.
set -euo pipefail

V287_APK=output/v287/Backlot-V287-Nap-Wake-Owner-Test.apk
V288_APK=output/v288/Backlot-V288-Personal-OS-Owner-Test.apk
TEST_APK=app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
CLASS=com.framebynavin.app.upgrade.V288InPlaceUpgradeDeviceTest
RUNNER=com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner

test -s "$V287_APK"
test -s "$V288_APK"
test -s "$TEST_APK"

echo 'STEP 1: Install signed V287 fixture APK and test runner'
adb install -r "$V287_APK"
adb install -r "$TEST_APK"
# CI fixture only: ensure zero data from prior tests. Never do this on owner phones.
adb shell pm clear com.framebynavin.app

echo 'STEP 2: Seed synthetic Personal + Creator data on actual V287 code'
adb shell am instrument -w -r -e hardeningTestEnvironment ci-emulator \
  -e class "$CLASS#seedOnV287" "$RUNNER" | tee /tmp/backlot-v287-before-upgrade.log
grep -F 'OK (1 test)' /tmp/backlot-v287-before-upgrade.log
if grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' /tmp/backlot-v287-before-upgrade.log; then
  echo 'Synthetic V287 seed failed' >&2
  exit 1
fi

echo 'STEP 3: Install signed V288 over V287 with -r; NO uninstall / NO data clear'
adb install -r "$V288_APK"

echo 'STEP 4: Verify V287 Personal records, historical receipts and Creator data on V288'
adb shell am instrument -w -r -e hardeningTestEnvironment ci-emulator \
  -e class "$CLASS#verifyOnV288" "$RUNNER" | tee /tmp/backlot-v288-after-upgrade.log
grep -F 'OK (1 test)' /tmp/backlot-v288-after-upgrade.log
if grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' /tmp/backlot-v288-after-upgrade.log; then
  echo 'Signed V287 to V288 in-place upgrade lost or changed synthetic data' >&2
  exit 1
fi

echo 'PASS: V287 -> V288 preserves Personal records/history and Creator project after in-place update'
