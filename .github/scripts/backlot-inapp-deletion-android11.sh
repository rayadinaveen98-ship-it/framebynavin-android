#!/usr/bin/env bash
# Disposable Android 11 QA only: no cloud deletion requests; release gate stays OFF.
set -euo pipefail
adb wait-for-device
adb install -r artifacts/debug/app-debug.apk
apk="$(find artifacts -name '*androidTest.apk' -print -quit)"
test -n "$apk"
adb install -r "$apk"
adb shell am instrument -w -r \
  -e hardeningTestEnvironment ci-emulator \
  -e class 'com.framebynavin.app.cloud.V270AccountDeletionSafetyTest,com.framebynavin.app.ui.V269DataPrivacyUiTest' \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner | tee deletion-ui-tests.log
grep -Eq 'OK \([1-9][0-9]* tests?\)' deletion-ui-tests.log
echo 'PASS: Android11 Backlot deletion safety + Data & Privacy UI'
