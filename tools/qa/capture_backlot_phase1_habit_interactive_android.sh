#!/usr/bin/env bash
# Android 15 Pixel 6 disposable review of one-activity 21-day challenge.
# No owner data, PersonalReminderStore, alarm scheduling, backup, or cloud.
set -euo pipefail
out="output/backlot-phase1-habit-interactive"
mkdir -p "$out"
adb wait-for-device
adb shell getprop sys.boot_completed | grep -qx 1

python3 - <<'PY'
from pathlib import Path
p = Path("app/src/debug/java/com/framebynavin/app/phase1/Phase1HabitInteractiveReviewActivity.kt")
raw = p.read_text()
for forbidden in (
    "PersonalReminderStore(", "Phase1HabitStore(", "AlarmManager(", "scheduleExact(",
    "FirebaseAuth(", "Supabase", "CloudRestore", "V297ChallengeStore(",
):
    assert forbidden not in raw, f"Forbidden connected side effect in review UI: {forbidden}"
assert "Phase1HabitChallenge(" in raw
assert "current.confirmToday(now)" in raw
assert "current.undoDay(" in raw
assert "current.setPaused(" in raw
print("PASS: real pure habit policy is used; no owner-data services are called")
PY

adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
timeout 14m adb shell am instrument -w \
  -e class com.framebynavin.app.phase1.Phase1HabitInteractiveReviewTest \
  -e hardeningTestEnvironment ci-emulator \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
  > "$out/INSTRUMENTED-HABIT-TESTS.txt" 2>&1 || {
  cat "$out/INSTRUMENTED-HABIT-TESTS.txt" >&2
  exit 1
}
cat "$out/INSTRUMENTED-HABIT-TESTS.txt"
grep -q 'OK (4 tests)' "$out/INSTRUMENTED-HABIT-TESTS.txt"

for scene in create create-no-alert day7 day7-done day7-paused; do
  echo "Capturing real interactive Android 15 challenge scene: $scene"
  adb shell am force-stop com.framebynavin.app || true
  adb shell pm clear com.framebynavin.app > /dev/null
  adb shell am start -W -n \
    com.framebynavin.app/.phase1.Phase1HabitInteractiveReviewActivity \
    --es habit_review_scene "$scene" --ez backlot_ci_visual_review true
  sleep 4
  adb shell dumpsys activity activities > "$out/$scene.activity.txt"
  grep -q 'Phase1HabitInteractiveReviewActivity' "$out/$scene.activity.txt" || {
    adb logcat -d -t 450 > "$out/$scene.logcat.txt" || true
    echo "Challenge review Activity not in foreground" >&2
    exit 1
  }
  adb shell uiautomator dump /sdcard/backlot-habit-review.xml > /dev/null
  adb exec-out cat /sdcard/backlot-habit-review.xml > "$out/$scene.xml"
  python3 - "$out/$scene.xml" "$scene" <<'PY'
import sys
from xml.etree import ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
scene = sys.argv[2]
labels = " ".join(
    n.attrib.get("text", "") + " " + n.attrib.get("content-desc", "")
    for n in root.iter("node")
).lower()
assert "backlot / personal" in labels, "Backlot review root was not rendered"
assert "review only" in labels
assert "not responding" not in labels and "isn't responding" not in labels
expected = "make it stick." if scene.startswith("create") else "your 21 days"
assert expected in labels, (scene, "Expected challenge screen not rendered")
if scene == "day7-done":
    assert "7 of 21 done" in labels
if scene == "day7-paused":
    assert "resume challenge" in labels
print("PASS: guarded Android screen, correct challenge scene", scene)
PY
  adb exec-out screencap -p > "$out/$scene.png"
done

python3 - "$out" <<'PY'
import struct, sys
from pathlib import Path
images = list(Path(sys.argv[1]).glob("*.png"))
assert len(images) == 5, f"Expected five real Android scene screenshots, got {len(images)}"
for f in images:
    data = f.read_bytes()
    assert data.startswith(b'\x89PNG\r\n\x1a\n') and data[-8:-4] == b'IEND', f
    w, h = struct.unpack(">II", data[16:24])
    assert len(data) > 3000 and w >= 600 and h >= 1000, (f, w, h)
print("PASS: 5 valid Android 15 native screenshot files")
PY
printf '%s\n' "$GITHUB_SHA" > "$out/SOURCE-COMMIT.txt"
printf '%s\n' "Synthetic review only; fixed November 2026 preview clock; production UI, existing reminders, alarms, cloud and owner data unchanged" > "$out/REVIEW-LIMITS.txt"
(cd "$out" && sha256sum *.png > SHA256SUMS.txt)
echo "Challenge interactive QA completed."
