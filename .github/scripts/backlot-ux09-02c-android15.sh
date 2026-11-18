#!/usr/bin/env bash
set -euo pipefail

# Invoked by the Android emulator action through `bash <path>`. Using a
# repository script avoids the action's POSIX /bin/sh inline quoting hazard.
adb wait-for-device
adb install -r qa/app-debug.apk
adb install -r qa/app-debug-androidTest.apk
adb shell pm path com.framebynavin.app.lab
adb shell pm path com.framebynavin.app.lab.test

# A nonzero instrument exit OR a failure to see both tests is a hard failure.
# Keep stdout for auditing (no private user content; synthetic tests only).
adb shell am instrument -w -r \
  -e class com.framebynavin.app.cloud.ux09bridge.Ux09CanonicalSourceRoundtripAndroidTest \
  com.framebynavin.app.lab.test/androidx.test.runner.AndroidJUnitRunner \
  | tee ux09-02c-android15-synthetic.log
grep -Fq 'OK (2 tests)' ux09-02c-android15-synthetic.log
echo 'PASS: Android 15 executed both real-serializer synthetic tests.'
