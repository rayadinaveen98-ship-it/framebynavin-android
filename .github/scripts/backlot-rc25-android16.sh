#!/usr/bin/env bash
# RC25 emulator-only, non-destructive 269 -> 270 synthetic Creator/Personal upgrade.
set -euo pipefail
old_apk="$(find old -type f -name app-debug.apk -print -quit)"
new_apk="$(find new -type f -name app-debug.apk -print -quit)"
test_apk="$(find new -type f -name app-debug-androidTest.apk -print -quit)"
test -s "$old_apk" && test -s "$new_apk" && test -s "$test_apk"
adb install -r "$old_apk"
adb install -r "$test_apk"
class=com.framebynavin.app.data.RC25InPlaceUpgrade269To270Test
adb shell am instrument -w -r \
  -e class "$class#phase1SeedOlderAppAndCaptureVerifiedBackup" \
  -e hardeningTestEnvironment ci-emulator \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner | tee rc25-upgrade-seed.log
grep -Fq 'OK (1 test)' rc25-upgrade-seed.log
adb shell am force-stop com.framebynavin.app
# Never uninstall, clear user data, replace preview signing key or downgrade.
adb install -r "$new_apk"
adb shell am instrument -w -r \
  -e class "$class#phase2VerifyUpgradeWithoutUninstall" \
  -e hardeningTestEnvironment ci-emulator \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner | tee rc25-upgrade-new.log
grep -Fq 'OK (1 test)' rc25-upgrade-new.log
adb shell am start -n com.framebynavin.app/.MainActivity
sleep 5
adb shell am force-stop com.framebynavin.app
adb shell am instrument -w -r \
  -e class "$class#phase3VerifyAfterNewAppLaunchAndCleanUp" \
  -e hardeningTestEnvironment ci-emulator \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner | tee rc25-upgrade-relaunch.log
grep -Fq 'OK (1 test)' rc25-upgrade-relaunch.log
adb shell am instrument -w -r \
  -e class com.framebynavin.app.ui.V269DataPrivacyUiTest \
  -e hardeningTestEnvironment ci-emulator \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner | tee rc25-privacy.log
grep -Fq 'OK (3 tests)' rc25-privacy.log
echo "PASS Android16 Creator+Personal Build269->Build270 in-place upgrade and privacy"
