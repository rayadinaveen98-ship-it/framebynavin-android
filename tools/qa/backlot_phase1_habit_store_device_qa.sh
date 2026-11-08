#!/usr/bin/env bash
# REAL DataStore test on a clean, disposable Android 15 emulator.
# No owner data, alarms, cloud, accounts, migrations, or release APK.
set -euo pipefail
out="output/backlot-phase1-habit-store-device"
mkdir -p "$out"
adb wait-for-device
adb shell getprop sys.boot_completed | grep -qx '1'
python3 - <<'PY'
from pathlib import Path
test = Path('app/src/androidTest/java/com/framebynavin/app/personal/Phase1HabitStoreDeviceTest.kt').read_text()
assert 'HardeningTestEnvironment.requireCiEmulator()' in test
assert 'check(store.load().isEmpty()' in test
assert 'journal.deleteRecursively()' in test
assert 'V297ChallengeStore(app)' in test
assert 'PersonalReminderStore(app)' in test
print('PASS: emulator-only, cleared-data, legacy isolation and recovery marker safeguards')
PY
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am force-stop com.framebynavin.app || true
adb shell pm clear com.framebynavin.app > /dev/null
timeout 16m adb shell am instrument -w \
  -e class com.framebynavin.app.personal.Phase1HabitStoreDeviceTest \
  -e hardeningTestEnvironment ci-emulator \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
  > "$out/ANDROID-DATASTORE-TEST.txt" 2>&1 || {
  cat "$out/ANDROID-DATASTORE-TEST.txt" >&2
  exit 1
}
cat "$out/ANDROID-DATASTORE-TEST.txt"
grep -q 'OK (1 test)' "$out/ANDROID-DATASTORE-TEST.txt"

# Cross-process persistence test: writer and reader are separate AndroidJUnitRunner
# invocations, each in a new process. pm clear ONLY before the writer.
adb shell am force-stop com.framebynavin.app || true
adb shell pm clear com.framebynavin.app > /dev/null

timeout 12m adb shell am instrument -w \
  -e class com.framebynavin.app.personal.Phase1HabitProcessRestartDeviceTest#writeSynthetic \
  -e hardeningTestEnvironment ci-emulator \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
  > "$out/PROCESS-WRITE.txt" 2>&1 || {
    cat "$out/PROCESS-WRITE.txt" >&2
    exit 1
  }
cat "$out/PROCESS-WRITE.txt"
grep -q 'OK (1 test)' "$out/PROCESS-WRITE.txt"

# Explicitly kill the target app process. Never clear the saved DataStore here.
adb shell am force-stop com.framebynavin.app || true
sleep 2

timeout 12m adb shell am instrument -w \
  -e class com.framebynavin.app.personal.Phase1HabitProcessRestartDeviceTest#verifyAfterProcessRestart \
  -e hardeningTestEnvironment ci-emulator \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
  > "$out/PROCESS-READ-AFTER-FORCE-STOP.txt" 2>&1 || {
    cat "$out/PROCESS-READ-AFTER-FORCE-STOP.txt" >&2
    exit 1
  }
cat "$out/PROCESS-READ-AFTER-FORCE-STOP.txt"
grep -q 'OK (1 test)' "$out/PROCESS-READ-AFTER-FORCE-STOP.txt"
echo "PASS: identical challenge and Done receipt survived an app process restart"

printf '%s\n' "$GITHUB_SHA" > "$out/SOURCE-COMMIT.txt"
printf '%s\n' 'CI emulator only; synthetic personal data; no live owner restore, deletion, alarm or cloud' > "$out/REVIEW-LIMITS.txt"
echo "Single-activity challenge real DataStore integration proof passed."
