#!/usr/bin/env bash
# Explicit Android 15 fail-closed encrypted archive coverage test.
# Only disposable cleared CI emulator data is touched.
set -euo pipefail
out="output/backlot-phase1-habit-archive-guard"
mkdir -p "$out"
adb wait-for-device
adb shell getprop sys.boot_completed | grep -qx '1'

python3 - <<'PY'
from pathlib import Path
src = Path('app/src/main/java/com/framebynavin/app/personal/V300PersonalArchiveSource.kt').read_text()
before = src.index('check(Phase1HabitStore(app).load().isEmpty())')
write = src.index('V300EncryptedPersonalArchive.write(')
assert before < write, "Incompleteness guard must run before any archive ZIP write"
test = Path('app/src/androidTest/java/com/framebynavin/app/personal/Phase1HabitArchiveCoverageGuardDeviceTest.kt').read_text()
for required in (
  'HardeningTestEnvironment.requireCiEmulator()',
  'check(habits.load().isEmpty()',
  'assertEquals("No misleading incomplete ZIP header", 0, bytes.size())',
  'assertEquals("Original immutable Done receipts survive", before, habits.exportJson())',
):
  assert required in test, required
print('PASS: source-level fail-closed archive order and disposable owner-data guard')
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
printf '%s\n' 'Fail-closed archive guard only. This is NOT full ZIP/cloud challenge coverage. No owner data.' > "$out/REVIEW-LIMITS.txt"
echo "PASS: encrypted Personal archive cannot omit a nonempty challenge archive silently"
