#!/usr/bin/env bash
set -euo pipefail

# Invoked by the Android emulator action through `bash <path>`. Using a
# repository script avoids the action's POSIX /bin/sh inline quoting hazard.
# The artifact includes several files, so download-artifact preserves their
# original app/build/outputs/... paths. Locate the *exactly one* real APK and
# androidTest APK rather than assuming they were flattened into qa/.
mapfile -t apps < <(find qa -type f -name 'app-debug.apk' | sort)
mapfile -t tests < <(find qa -type f -name 'app-debug-androidTest.apk' | sort)
if [[ ${#apps[@]} -ne 1 || ${#tests[@]} -ne 1 ]]; then
  echo "Expected one Lab APK and one androidTest APK in the downloaded artifact."
  find qa -maxdepth 9 -type f -name '*.apk' -print | sort
  exit 1
fi
printf 'Lab APK: %s\nAndroid test APK: %s\n' "${apps[0]}" "${tests[0]}"
adb wait-for-device
adb install -r "${apps[0]}"
adb install -r "${tests[0]}"
adb shell pm path com.framebynavin.app.lab | grep -Fq 'package:'
adb shell pm path com.framebynavin.app.lab.test | grep -Fq 'package:'

# A nonzero instrument exit OR a failure to see both tests is a hard failure.
# Keep stdout for auditing (no private user content; synthetic tests only).
adb shell am instrument -w -r \
  -e class com.framebynavin.app.cloud.ux09bridge.Ux09CanonicalSourceRoundtripAndroidTest \
  com.framebynavin.app.lab.test/androidx.test.runner.AndroidJUnitRunner \
  | tee ux09-02c-android15-synthetic.log
grep -Fq 'OK (2 tests)' ux09-02c-android15-synthetic.log
echo 'PASS: Android 15 executed both real-serializer synthetic tests.'
