#!/usr/bin/env bash
# EXACT production Compose visuals + real challenge DataStore; disposable CI only.
# Owner build 320, cloud, live alarms, Creator and Personal reminders untouched.
set -euo pipefail
out="output/backlot-phase1-real-challenge-store-ui"
mkdir -p "$out"
adb wait-for-device
adb shell getprop sys.boot_completed | grep -qx 1
python3 - <<'PY'
from pathlib import Path
src = Path('app/src/main/java/com/framebynavin/app/ui/Phase1HabitLocalStoreReview.kt').read_text()
shared = Path('app/src/main/java/com/framebynavin/app/ui/Phase1HabitSharedUi.kt').read_text()
debug = Path('app/src/debug/java/com/framebynavin/app/phase1/Phase1HabitInteractiveReviewActivity.kt').read_text()
assert 'Phase1HabitStore(context)' in src
assert 'store.confirmToday(' in src and 'store.undoDay(' in src and 'store.setPaused(' in src
assert 'CreateHabitReview(' in src and 'DashboardHabitReview(' in src
assert 'CreateHabitReview(' in debug and 'DashboardHabitReview(' in debug
assert 'Phase1HabitLocalStoreReview(onClose' in debug
assert 'Phase1HabitChallenge' in shared
for forbidden in ('AlarmManager(', 'PersonalReminderStore(', 'FirebaseAuth(', 'Supabase'):
    assert forbidden not in src
print('PASS: production and fixture use same pure UI; no unverified alarms or personal-data mutations')
PY
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am force-stop com.framebynavin.app || true
adb shell pm clear com.framebynavin.app > /dev/null

timeout 14m adb shell am instrument -w \
  -e class com.framebynavin.app.ui.Phase1HabitLocalScreenTest \
  -e hardeningTestEnvironment ci-emulator \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
  > "$out/REAL-UI-TO-DATASTORE-TEST.txt" 2>&1 || {
    cat "$out/REAL-UI-TO-DATASTORE-TEST.txt" >&2
    exit 1
  }
cat "$out/REAL-UI-TO-DATASTORE-TEST.txt"
grep -q 'OK (1 test)' "$out/REAL-UI-TO-DATASTORE-TEST.txt"

# DO NOT clear data. Screenshot the actual saved/paused challenge after
# finishing instrumentation and re-opening in a separate Activity process.
adb shell am force-stop com.framebynavin.app || true
adb shell am start -W -n \
  com.framebynavin.app/.phase1.Phase1HabitInteractiveReviewActivity \
  --es habit_review_scene local-store --ez backlot_ci_visual_review true
sleep 5
adb shell dumpsys activity activities > "$out/SAVED-ACTIVITY.txt"
grep -q 'Phase1HabitInteractiveReviewActivity' "$out/SAVED-ACTIVITY.txt"
adb shell uiautomator dump /sdcard/backlot-phase1-local-store.xml >/dev/null
adb exec-out cat /sdcard/backlot-phase1-local-store.xml > "$out/SAVED-SCREEN.xml"
python3 - "$out/SAVED-SCREEN.xml" <<'PY'
from xml.etree import ElementTree as ET
import sys
root=ET.parse(sys.argv[1]).getroot()
text=' '.join(
  (n.attrib.get('text','') + ' ' + n.attrib.get('content-desc',''))
  for n in root.iter('node')
).lower()
for expected in ('read ten pages', 'your 21 days', 'resume challenge', '0 of 21 done'):
    assert expected in text, f'Missing saved real UI text: {expected}'
print('PASS: actual persisted Challenge/Paused state is rendered by the production Compose screen')
PY
adb exec-out screencap -p > "$out/LOCAL-SAVED-PAUSED.png"
python3 - "$out/LOCAL-SAVED-PAUSED.png" <<'PY'
import struct,sys
from pathlib import Path
data=Path(sys.argv[1]).read_bytes()
assert data[:8] == b'\x89PNG\r\n\x1a\n' and data[-8:-4] == b'IEND'
w,h=struct.unpack('>II',data[16:24])
assert w>=600 and h>=1000 and len(data)>3000
print('PASS: real DataStore-backed Android screenshot',w,h,len(data))
PY
printf '%s\n' "$GITHUB_SHA" > "$out/SOURCE-COMMIT.txt"
printf '%s\n' 'Debug Activity on disposable CI emulator; REAL isolated store, synthetic progress, live alarm/cloud not connected' > "$out/REVIEW-LIMITS.txt"
(cd "$out" && sha256sum *.png > SHA256SUMS.txt)
echo 'PASS: frozen Phase 1 real UI-to-DataStore acceptance complete'
