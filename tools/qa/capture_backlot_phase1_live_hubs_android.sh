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
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk

echo "Running three shared Hub callback instrumentation tests"
timeout 12m adb shell am instrument -w \
  -e class com.framebynavin.app.ui.BacklotReminderHubChromeTest \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
  > "$out/INSTRUMENTED-HUB-TESTS.txt" 2>&1 || {
  cat "$out/INSTRUMENTED-HUB-TESTS.txt" >&2
  exit 1
}
cat "$out/INSTRUMENTED-HUB-TESTS.txt"
if ! grep -q 'OK (3 tests)' "$out/INSTRUMENTED-HUB-TESTS.txt"; then
  echo "Hub callback test count/result was not confirmed" >&2
  exit 1
fi

for scene in creator personal creator-light personal-light; do
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
done

printf '%s\n' "$GITHUB_SHA" > "$out/SOURCE-COMMIT.txt"
printf '%s\n' "Real production Hub Composables in debug-only Activity; synthetic Creator fixtures, empty fresh Personal data; not a release build or owner approval" > "$out/REVIEW-LIMITS.txt"
(cd "$out" && sha256sum *.png > SHA256SUMS.txt)
echo "Four LIVE Reminder Hub screenshots captured, guarded and checksum-verified."
