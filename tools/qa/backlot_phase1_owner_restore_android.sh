#!/usr/bin/env bash
# Genuine encrypted owner-approved v2 Personal restore, all domain types and
# private photo SHA-256 on a disposable Android 15 CI emulator only.
set -euo pipefail
dir="output/backlot-phase1-v2-owner-restore"
mkdir -p "$dir"
adb wait-for-device
adb shell getprop sys.boot_completed | grep -qx '1'
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am force-stop com.framebynavin.app || true
adb shell pm clear com.framebynavin.app > /dev/null
timeout 25m adb shell am instrument -w \
  -e class com.framebynavin.app.personal.Phase1V2OwnerRestoreDeviceTest \
  -e hardeningTestEnvironment ci-emulator \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
  > "$dir/ANDROID-ACTUAL-RESTORE-TEST.txt" 2>&1 || {
    cat "$dir/ANDROID-ACTUAL-RESTORE-TEST.txt" >&2
    exit 1
  }
cat "$dir/ANDROID-ACTUAL-RESTORE-TEST.txt"
grep -q 'OK (1 test)' "$dir/ANDROID-ACTUAL-RESTORE-TEST.txt"
printf '%s\n' "$GITHUB_SHA" > "$dir/SOURCE-COMMIT.txt"
printf '%s\n' 'Only synthetic cleared-CI data. No owner device, cloud or production restore.' > "$dir/SCOPE.txt"
echo 'PASS: exact owner 21-day Done retained alongside new Personal, group, Space, note and JPEG'
