#!/usr/bin/env bash
# Real LIVE Hub components on disposable Android 15 emulator, not mocked Compose screens.
# No owner data, real reminders, cloud, or release APK are involved.
set -euo pipefail

out="output/backlot-phase1-live-directors-cut"
mkdir -p "$out"

adb wait-for-device
adb shell getprop sys.boot_completed | grep -qx '1'
adb shell wm size > "$out/DEVICE-DISPLAY.txt"
adb shell wm density >> "$out/DEVICE-DISPLAY.txt"
python3 - <<'PY'
from pathlib import Path
root = Path("app/src/main/java/com/framebynavin/app/ui")
for filename in [
    "V2232PersonalReminderHub.kt", "V157ReminderCenterUi.kt",
    "V295UniversalPersonalRoutines.kt", "V307CompactCreatorActivity.kt",
    "V2193ReminderHistoryUi.kt", "V274PersonalPurposeUi.kt",
]:
    raw = (root / filename).read_text()
    assert "MutedGold" not in raw, f"Legacy gold accent in {filename}"
for filename, action in [
    ("V2232PersonalReminderHub.kt", "onOpenCreator = onCreator"),
    ("V157ReminderCenterUi.kt", "onOpenPersonal = onPersonal"),
]:
    assert action in (root / filename).read_text(), f"Navigation callback lost: {filename}"
editor = (root / "V2232PersonalReminderHub.kt").read_text()
assert 'testTag("phase1-personal-toggle-suggestions")' in editor
assert 'visible = suggestionsOpen' in editor
assert 'Show Suggestions' in editor
print("PASS: Director's Cut sources, navigation callbacks and progressive suggestion control present")
PY
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk

echo "Running six shared, real-Hub, and editor disclosure instrumentation tests"
timeout 12m adb shell am instrument -w \
  -e class com.framebynavin.app.ui.BacklotReminderHubChromeTest \
  -e hardeningTestEnvironment ci-emulator \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
  > "$out/INSTRUMENTED-HUB-TESTS.txt" 2>&1 || {
  cat "$out/INSTRUMENTED-HUB-TESTS.txt" >&2
  exit 1
}
cat "$out/INSTRUMENTED-HUB-TESTS.txt"
if ! grep -q 'OK (6 tests)' "$out/INSTRUMENTED-HUB-TESTS.txt"; then
  echo "Hub callback test count/result was not confirmed" >&2
  exit 1
fi

for scene in creator personal creator-light personal-light personal-editor personal-editor-light; do
  echo "Rendering LIVE Android 15 Hub $scene"
  # These are disposable CI-emulator packages. Each new scene starts empty:
  # real owner devices, records, notifications, or cloud are never connected.
  adb shell am force-stop com.framebynavin.app || true
  adb shell pm clear com.framebynavin.app > /dev/null
  adb shell am start -W -n \
    com.framebynavin.app/.phase1.Phase1LiveReminderScreenReviewActivity \
    --es phase1_scene "$scene" --ez backlot_ci_visual_review true
  sleep 5
  python3 tools/qa/backlot_android_ui_guard.py "$scene" "$out"
  adb shell dumpsys activity activities > "$out/$scene.activity.txt"
  if ! grep -q 'Phase1LiveReminderScreenReviewActivity' "$out/$scene.activity.txt"; then
    echo "Real Hub debug review Activity not in foreground: $scene" >&2
    adb logcat -d -t 500 > "$out/$scene.logcat.txt" || true
    exit 1
  fi
  adb exec-out screencap -p > "$out/$scene.png"
  python3 tools/qa/backlot_android_ui_guard.py "$scene" "$out" --strict
  rm -f "$out/$scene.system-dialog.xml"
  python3 - "$out/$scene.png" <<'PY'
import struct, sys
from pathlib import Path
file = Path(sys.argv[1])
data = file.read_bytes()
assert data.startswith(b'\x89PNG\r\n\x1a\n') and data[-8:-4] == b'IEND', file
assert len(data) > 3000, file
width, height = struct.unpack('>II', data[16:24])
assert width >= 600 and height >= 1000, (file, width, height)
print(f'VALID real-hub screenshot: {file}, {width}x{height}, {len(data)} bytes')
PY
  if [[ "$scene" == personal-editor* ]]; then
    # Review BOTH states of the real editor; never add fixture-only behavior
    # or alter the production editor to force it open.
    adb shell uiautomator dump /sdcard/backlot-phase1-review-ui.xml >/dev/null
    adb exec-out cat /sdcard/backlot-phase1-review-ui.xml > "$out/$scene.before-expansion.xml"
    tap="$(python3 - "$out/$scene.before-expansion.xml" <<'PY'
import re, sys
from xml.etree import ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
for node in root.iter("node"):
    label = " ".join((node.attrib.get("text", ""), node.attrib.get("content-desc", "")))
    if "Show Suggestions" not in label:
        continue
    match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.attrib.get("bounds", ""))
    if match:
        x0, y0, x1, y1 = map(int, match.groups())
        if x1 > x0 and y1 > y0:
            print((x0+x1)//2, (y0+y1)//2)
            break
else:
    raise SystemExit("ERROR: Cannot locate visible Show Suggestions control; refusing blind tap")
PY
)"
    read -r tap_x tap_y <<< "$tap"
    adb shell input tap "$tap_x" "$tap_y"
    sleep 3
    expanded="$scene-expanded"
    adb exec-out screencap -p > "$out/$expanded.png"
    python3 tools/qa/backlot_android_ui_guard.py "$expanded" "$out" --strict
    adb shell uiautomator dump /sdcard/backlot-phase1-review-ui.xml >/dev/null
    adb exec-out cat /sdcard/backlot-phase1-review-ui.xml > "$out/$expanded.xml"
    python3 - "$out/$expanded.xml" <<'PY'
import sys
from xml.etree import ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
labels = " ".join(" ".join((n.attrib.get("text", ""), n.attrib.get("content-desc", ""))) for n in root.iter("node"))
assert "Hide Suggestions" in labels, "Expansion not visually confirmed in Android UI hierarchy"
print("PASS: expanded editor with Hide Suggestions visible")
PY
  fi
done

python3 - "$out" <<'PY'
from pathlib import Path
import struct, sys
images = sorted(Path(sys.argv[1]).glob("*.png"))
assert len(images) == 8, f"Expected six baseline and two expanded screenshots, got {len(images)}"
for file in images:
    data = file.read_bytes()
    assert data.startswith(b"\x89PNG\r\n\x1a\n") and data[-8:-4] == b"IEND", file
    width, height = struct.unpack(">II", data[16:24])
    assert width >= 600 and height >= 1000, (file, width, height)
print("PASS: all eight real Android screenshots have valid dimensions and PNG bounds")
PY

printf '%s\n' "$GITHUB_SHA" > "$out/SOURCE-COMMIT.txt"
printf '%s\n' "Real production Hub Composables in debug-only Activity; synthetic Creator fixtures, empty fresh Personal data; not a release build or owner approval" > "$out/REVIEW-LIMITS.txt"
(cd "$out" && sha256sum *.png > SHA256SUMS.txt)
echo "Eight live Hub/editor screenshots (including expanded suggestions) captured, guarded and checksum-verified."
