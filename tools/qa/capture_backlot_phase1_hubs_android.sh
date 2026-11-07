#!/usr/bin/env bash
# Run all commands in a single Bash process (emulator runner executes script lines separately).
# Four native Android 15 screenshots: Director's Cut primary + Light compatibility.
set -euo pipefail

out="output/backlot-phase1-hubs-directors-cut"
mkdir -p "$out"
adb wait-for-device
adb shell getprop sys.boot_completed | grep -qx '1'
python3 - <<'PY'
from pathlib import Path
hub = Path("app/src/debug/java/com/framebynavin/app/phase1/Phase1HubVisualReview.kt").read_text()
assert "FrameTheme.DIRECTORS_CUT.palette" in hub, "Director's Cut source palette absent"
assert "FrameTheme.BACKLOT_LIGHT.palette" in hub, "Light compatibility source palette absent"
assert "palette.secondary" not in hub, "Do not make secondary gold a main Hub accent"
assert "HubGold" not in hub and "Phase1OptionAReviewTokens" not in hub, "Old gold mockup leaked into Hub"
print("Director's Cut primary theme reference verified; no gold mockup accents")
PY
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell wm size > "$out/DEVICE-DISPLAY.txt"
adb shell wm density >> "$out/DEVICE-DISPLAY.txt"

for scene in personal-hub creator-hub personal-hub-light creator-hub-light; do
  echo "Capturing Android 15 Director\x27s Cut/Light hub: $scene"
  adb shell am force-stop com.framebynavin.app || true
  adb shell am start -W -n com.framebynavin.app/.phase1.Phase1VisualReviewActivity --es phase1_screen "$scene"
  sleep 4
  python3 tools/qa/backlot_android_ui_guard.py "$scene" "$out"
  adb shell dumpsys activity activities > "$out/$scene.activity.txt"
  if ! grep -q 'Phase1VisualReviewActivity' "$out/$scene.activity.txt"; then
    echo "Review Activity missing for $scene" >&2
    adb logcat -d -t 400 > "$out/$scene.logcat.txt" || true
    exit 1
  fi
  adb exec-out screencap -p > "$out/$scene.png"
  python3 tools/qa/backlot_android_ui_guard.py "$scene" "$out" --strict
  rm -f "$out/$scene.system-dialog.xml"
  test -s "$out/$scene.png"
  python3 - "$out/$scene.png" <<'PY'
import sys, struct
from pathlib import Path
p = Path(sys.argv[1])
b = p.read_bytes()
assert b[:8] == b'\x89PNG\r\n\x1a\n', f'not PNG: {p}'
assert len(b) > 2000, f'PNG too small: {p}'
w, h = struct.unpack('>II', b[16:24])
assert w >= 600 and h >= 1000, f'unexpected screenshot bounds: {w}x{h}'
assert b[-8:-4] == b'IEND', f'PNG lacks ending chunk: {p}'
print(f'Valid Android emulator PNG: {p} {w}x{h} {len(b)} bytes')
PY
  rm -f "$out/$scene.activity.txt"
done
printf '%s\n' "$GITHUB_SHA" > "$out/SOURCE-COMMIT.txt"
printf '%s\n' "Director\x27s Cut primary, Light compatibility; native Android debug-only synthetic fixture; no release approval" > "$out/REVIEW-LIMITS.txt"
(cd "$out" && sha256sum *.png > SHA256SUMS.txt)
echo "Four native Android theme-correct hub preview screenshots captured and hashed."
