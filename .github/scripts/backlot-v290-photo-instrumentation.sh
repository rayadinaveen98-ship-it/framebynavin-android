#!/usr/bin/env bash
# Android-emulator-runner invokes each script line separately. Use ONE bash file.
set -euo pipefail

adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk

for entry in \
  "com.framebynavin.app.personal.spaces.V290PrivatePhotoJournalDeviceTest:3" \
  "com.framebynavin.app.personal.spaces.V290InterruptedPhotoRecoveryDeviceTest:3" \
  "com.framebynavin.app.personal.spaces.V289SpacesDatabaseTest:4" \
  "com.framebynavin.app.personal.spaces.V289SpacesMetadataRecoveryDeviceTest:3" \
  "com.framebynavin.app.ui.V289PersonalSpacesUiTest:6"; do
  class="${entry%:*}"
  count="${entry##*:}"
  echo "CI TEST CLASS: $class"
  adb logcat -c || true
  logfile="$(mktemp)"
  adb shell am instrument -w -r \
    -e hardeningTestEnvironment ci-emulator \
    -e class "$class" \
    com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner | tee "$logfile"
  if [ "$count" -eq 1 ]; then
    result="OK (1 test)"
  else
    result="OK ($count tests)"
  fi
  if ! grep -Fq "$result" "$logfile"; then
    echo "Expected Android instrumentation result missing for $class" >&2
    adb logcat -d -v threadtime | tail -180 || true
    exit 1
  fi
  if grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' "$logfile"; then
    echo "Android instrumentation failed for $class" >&2
    adb logcat -d -v threadtime | tail -180 || true
    exit 1
  fi
  rm -f "$logfile"
done

echo "PASS: V290 migration, private photo integrity, V289 DB/restore/UI regressions"
