#!/usr/bin/env bash
# RC25 emulator-only Android 11 visual, privacy and personal backup smoke.
set -euo pipefail
test -s tests/app-debug.apk
test -s tests/app-debug-androidTest.apk
adb install -r tests/app-debug.apk
adb install -r tests/app-debug-androidTest.apk
for class in \
  com.framebynavin.app.ui.V269DataPrivacyUiTest \
  com.framebynavin.app.ui.V215HomeSlideshowVisualUiTest \
  com.framebynavin.app.ui.V2232PersonalReminderUiTest \
  com.framebynavin.app.personal.V226PersonalBackupTransferTest; do
  short="${class##*.}"
  adb shell am instrument -w -r \
    -e class "$class" -e hardeningTestEnvironment ci-emulator \
    com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner | tee "rc25-${short}.log"
  grep -Eq 'OK \([1-9][0-9]* tests\)' "rc25-${short}.log"
done
adb shell am start -n com.framebynavin.app/.MainActivity
echo "PASS Android11 visual, reminder, privacy and private backup smoke"
