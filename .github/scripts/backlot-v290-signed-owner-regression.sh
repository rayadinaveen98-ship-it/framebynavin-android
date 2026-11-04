#!/usr/bin/env bash
# Run the REAL signed/non-debuggable owner APK, not the debug application.
# Compose UI instrumentation uses ui-test-manifest's test-only ComponentActivity,
# injected only in the DEBUG target. Running those UI tests against a signed
# personalStable binary incorrectly resolves that Activity under the test APK.
# Debug-target UI tests already passed in upstream full regression. This gate
# verifies actual signed binary for non-Compose notification/DB/photo domains.
# The emulator contains disposable data only. NEVER run pm clear on owner phones.
# The instrumentation is built with testBuildType=personalStable, matching
# the module-suffixed Kotlin internal methods in the signed owner APK.
set -euo pipefail

owner_apk=output/Backlot-V290-Private-Spaces-Owner-Test.apk
test_apk=output/app-personalStable-androidTest.apk
test -s "$owner_apk"
test -s "$test_apk"

echo 'Install the exact signed V290 owner-preview APK (no debug replacement)'
adb install -r "$owner_apk"
adb install -r "$test_apk"
adb shell dumpsys package com.framebynavin.app | grep -E -m 1 'versionCode=290'
runner=com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner

for test_case in \
  com.framebynavin.app.personal.V285PersonalLifeNotificationsDeviceTest:2 \
  com.framebynavin.app.personal.V284DietFitnessPlanDeviceTest:2 \
  com.framebynavin.app.personal.spaces.V289SpacesDatabaseTest:4 \
  com.framebynavin.app.personal.spaces.V289SpacesMetadataRecoveryDeviceTest:3 \
  com.framebynavin.app.personal.spaces.V290PrivatePhotoJournalDeviceTest:3 \
  com.framebynavin.app.personal.spaces.V290InterruptedPhotoRecoveryDeviceTest:4; do
  classname="${test_case%:*}"
  expected="${test_case##*:}"
  echo "SIGNED OWNER APK TEST: $classname ($expected)"
  # Protect every test from previous synthetic data. Emulator only.
  adb shell pm clear com.framebynavin.app
  adb logcat -c || true
  log_file="$(mktemp)"
  if ! adb shell am instrument -w -r \
    -e hardeningTestEnvironment ci-emulator \
    -e class "$classname" "$runner" | tee "$log_file"; then
      echo "Instrumentation command failed for $classname" >&2
      adb logcat -d -v threadtime | tail -170 || true
      exit 1
  fi
  if [ "$expected" -eq 1 ]; then
    marker='OK (1 test)'
  else
    marker="OK ($expected tests)"
  fi
  if ! grep -Fq "$marker" "$log_file" || \
      grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' "$log_file"; then
    echo "Signed owner test did not pass: $classname" >&2
    cat "$log_file" >&2
    adb logcat -d -v threadtime | tail -200 || true
    exit 1
  fi
  rm -f "$log_file"
done

# Preserve the identity check until the final test; no debug app substitution.
adb shell dumpsys package com.framebynavin.app | grep -E -m 1 'versionCode=290'
echo 'PASS: signed V290 APK exercised 18 non-Compose Android tests in 6 classes'
