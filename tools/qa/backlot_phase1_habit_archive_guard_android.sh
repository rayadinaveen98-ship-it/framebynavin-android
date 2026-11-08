#!/usr/bin/env bash
# Android 15 encrypted Personal ZIP v2 challenge receipt coverage test.
# Only disposable cleared CI emulator data is touched.
set -euo pipefail
out="output/backlot-phase1-habit-archive-guard"
mkdir -p "$out"
adb wait-for-device
adb shell getprop sys.boot_completed | grep -qx '1'

python3 - <<'PY'
from pathlib import Path
src = Path('app/src/main/java/com/framebynavin/app/personal/V300PersonalArchiveSource.kt').read_text()
assert 'singleActivityChallenges = Phase1HabitStore(app).exportJson()' in src
assert 'singleActivityChallenges = singleActivityChallenges' in src
codec = Path('app/src/main/java/com/framebynavin/app/personal/V300EncryptedPersonalArchive.kt').read_text()
assert 'VERSION_WITH_SINGLE_ACTIVITY = 2' in codec
assert 'Phase1HabitCodec.decode(it)' in codec
test = Path('app/src/androidTest/java/com/framebynavin/app/personal/Phase1HabitArchiveCoverageGuardDeviceTest.kt').read_text()
for required in (
  'HardeningTestEnvironment.requireCiEmulator()',
  'check(habits.load().isEmpty()',
  'assertEquals("Original immutable Done receipts survive", before, habits.exportJson())',
  'assertEquals(confirmed, Phase1HabitCodec.decode(',
):
  assert required in test, required
print('PASS: source-level v2 ZIP challenge coverage and disposable-device guard')
PY

adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am force-stop com.framebynavin.app || true
adb shell pm clear com.framebynavin.app > /dev/null

timeout 15m adb shell am instrument -w \
  -e class com.framebynavin.app.personal.Phase1HabitArchiveCoverageGuardDeviceTest \
  -e hardeningTestEnvironment ci-emulator \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
  > "$out/ANDROID-ARCHIVE-COVERAGE-TEST.txt" 2>&1 || {
    cat "$out/ANDROID-ARCHIVE-COVERAGE-TEST.txt" >&2
    exit 1
  }
cat "$out/ANDROID-ARCHIVE-COVERAGE-TEST.txt"
grep -q 'OK (1 test)' "$out/ANDROID-ARCHIVE-COVERAGE-TEST.txt"

printf '%s\n' "$GITHUB_SHA" > "$out/SOURCE-COMMIT.txt"
printf '%s\n' 'Encrypted ZIP v2 exact challenge receipts verified; cloud sync, owner-approved restore and real user data remain untested.' > "$out/REVIEW-LIMITS.txt"
echo "PASS: versioned encrypted Personal ZIP includes exact single-activity Done receipts"
