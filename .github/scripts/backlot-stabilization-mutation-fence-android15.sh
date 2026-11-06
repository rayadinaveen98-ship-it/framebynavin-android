#!/usr/bin/env bash
set -euo pipefail
test "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" = "1" || {
  echo "FAIL: only disposable emulator may run data-preservation mutation tests" >&2; exit 1
}
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell pm clear com.framebynavin.app

# Parent PR #313's recovery prerequisites MUST pass on this integrated exact
# source head, not only on the earlier standalone parent commit.
recoveryLog="$(mktemp)"
log="$(mktemp)"
trap 'rm -f "$recoveryLog" "$log"' EXIT
adb shell am instrument -w -r \
  -e hardeningTestEnvironment ci-emulator \
  -e class com.framebynavin.app.personal.StabilizationRestoreWriteAheadJournalDeviceTest,com.framebynavin.app.personal.StabilizationRestoreCrossStoreInterruptDeviceTest \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner | tee "$recoveryLog"
grep -Fq 'OK (3 tests)' "$recoveryLog"
if grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' "$recoveryLog"; then
  exit 1
fi

# Cross-store interrupted-write fixture leaves synthetic DataStore records.
# Clear only the disposable CI emulator between independent test clusters.
adb shell pm clear com.framebynavin.app
adb shell am instrument -w -r \
  -e hardeningTestEnvironment ci-emulator \
  -e class com.framebynavin.app.personal.StabilizationPersonalMutationGateDeviceTest,com.framebynavin.app.personal.spaces.V289SpacesMetadataRecoveryDeviceTest \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner | tee "$log"
grep -Fq 'OK (8 tests)' "$log"
if grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' "$log"; then exit 1; fi
echo 'PASS: Personal, Challenge, Group and Room recovery lock regressions'

# Repository constructor now requires Context. Run its existing database,
# photo-recovery and Space UI device regressions on the *integrated* source.
# Clear ONLY this disposable emulator's app, never an owner device.
adb shell pm clear com.framebynavin.app
spaceLog="$(mktemp)"
trap 'rm -f "$recoveryLog" "$log" "$spaceLog"' EXIT
adb shell am instrument -w -r \
  -e hardeningTestEnvironment ci-emulator \
  -e class com.framebynavin.app.personal.spaces.V289SpacesDatabaseTest,com.framebynavin.app.personal.spaces.V290InterruptedPhotoRecoveryDeviceTest,com.framebynavin.app.personal.spaces.V290PrivatePhotoJournalDeviceTest,com.framebynavin.app.ui.V289PersonalSpacesUiTest,com.framebynavin.app.ui.V292SpaceMetricUiTest \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner | tee "$spaceLog"
grep -Fq 'OK (21 tests)' "$spaceLog"
if grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' "$spaceLog"; then exit 1; fi
echo 'PASS: recovery, mutation-lock and existing Space UI regression groups'

# Linked-reference safety is a real Personal-only legacy restore test that
# requires an empty synthetic emulator DataStore at entry. Test in isolation.
adb shell pm clear com.framebynavin.app
linkLog="$(mktemp)"
trap 'rm -f "$recoveryLog" "$log" "$spaceLog" "$linkLog"' EXIT
adb shell am instrument -w -r \
  -e hardeningTestEnvironment ci-emulator \
  -e class com.framebynavin.app.personal.PersonalLinkedReferenceGuardDeviceTest \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner | tee "$linkLog"
grep -Fq 'OK (1 test)' "$linkLog"
if grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' "$linkLog"; then exit 1; fi
echo 'PASS: all 33 synthetic Android Phase A recovery, Room, Space UI and orphan-link cases'
