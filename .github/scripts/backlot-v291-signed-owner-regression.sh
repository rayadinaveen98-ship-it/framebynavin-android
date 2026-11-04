#!/usr/bin/env bash
# CI-EMULATOR ONLY. Exercise the actual non-debuggable, signed V291 owner APK.
# Never run this script on an owner phone. Synthetic test data is disposable.
# UI tests using debug-only ComponentActivity run in the separate V291 UI gate.
set -euo pipefail

owner="output/Backlot-V291-Signed-Owner-Regression.apk"
test_apk="output/app-personalStable-androidTest.apk"
test -s "$owner"
test -s "$test_apk"
(cd output && sha256sum -c SHA256SUMS.txt)

adb install -r "$owner"
adb install -r "$test_apk"
adb shell dumpsys package com.framebynavin.app | grep -E -m 1 'versionCode=291'

# A debug target would invalidate this signed-owner acceptance test.
if adb shell run-as com.framebynavin.app id >/dev/null 2>&1; then
  echo 'FAIL: target APK is debuggable; signed-owner gate cannot be trusted' >&2
  exit 1
fi

runner=com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner
total=0
for entry in \
  com.framebynavin.app.personal.V285PersonalLifeNotificationsDeviceTest:2 \
  com.framebynavin.app.personal.V284DietFitnessPlanDeviceTest:2 \
  com.framebynavin.app.personal.spaces.V289SpacesDatabaseTest:4 \
  com.framebynavin.app.personal.spaces.V289SpacesMetadataRecoveryDeviceTest:3 \
  com.framebynavin.app.personal.spaces.V290PrivatePhotoJournalDeviceTest:3 \
  com.framebynavin.app.personal.spaces.V290InterruptedPhotoRecoveryDeviceTest:4 \
  com.framebynavin.app.personal.spaces.V291PhotoOrientationDeviceTest:2; do
  classname="${entry%:*}"
  expected="${entry##*:}"
  echo "V291 SIGNED-OWNER TEST: $classname ($expected)"
  adb shell pm clear com.framebynavin.app
  adb logcat -c || true
  logfile="$(mktemp)"
  if ! adb shell am instrument -w -r \
      -e hardeningTestEnvironment ci-emulator \
      -e class "$classname" "$runner" | tee "$logfile"; then
    echo "Instrumentation failed for $classname" >&2
    adb logcat -d -v threadtime | tail -180 || true
    rm -f "$logfile"
    exit 1
  fi
  if [ "$expected" -eq 1 ]; then
    marker="OK (1 test)"
  else
    marker="OK ($expected tests)"
  fi
  if ! grep -Fq "$marker" "$logfile" || \
      grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' "$logfile"; then
    echo "Test count/status mismatch for $classname" >&2
    cat "$logfile" >&2
    adb logcat -d -v threadtime | tail -180 || true
    rm -f "$logfile"
    exit 1
  fi
  rm -f "$logfile"
  total=$((total + expected))
done
test "$total" -eq 20
adb shell dumpsys package com.framebynavin.app | grep -E -m 1 'versionCode=291'
echo "PASS: signed/non-debuggable V291 owner APK passed $total Android tests across 7 classes"
