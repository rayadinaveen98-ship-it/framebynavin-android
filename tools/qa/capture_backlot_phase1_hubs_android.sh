#!/usr/bin/env bash
# Run all commands in a single Bash process (emulator runner executes script lines separately).
# Two native Android 15 screenshots from isolated debug-only preview screens.
set -euo pipefail

out="output/backlot-phase1-hubs-option-a"
mkdir -p "$out"
adb wait-for-device
adb shell getprop sys.boot_completed | grep -qx '1'
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell wm size > "$out/DEVICE-DISPLAY.txt"
adb shell wm density >> "$out/DEVICE-DISPLAY.txt"

for scene in personal-hub creator-hub; do
  echo "Capturing Android 15 Option A hub: $scene"
  adb shell am force-stop com.framebynavin.app || true
  adb shell am start -W -n com.framebynavin.app/.phase1.Phase1VisualReviewActivity --es phase1_screen "$scene"
  sleep 4
  adb shell dumpsys activity activities > "$out/$scene.activity.txt"
  if ! grep -q 'Phase1VisualReviewActivity' "$out/$scene.activity.txt"; then
    echo "Review Activity missing for $scene" >&2
    adb logcat -d -t 400 > "$out/$scene.logcat.txt" || true
    exit 1
  fi
  adb exec-out screencap -p > "$out/$scene.png"
  test -s "$out/$scene.png"
  python3 - "$out/$scene.png" <<'PY'
import sys, struct, zlib
from pathlib import Path
p = Path(sys.argv[1])
b = p.read_bytes()
assert b[:8] == b'\x89PNG\r\n\x1a\n', f'not PNG: {p}'
assert len(b) > 2000, f'PNG too small: {p}'
w, h = struct.unpack('>II', b[16:24])
assert w >= 600 and h >= 1000, f'unexpected screenshot bounds: {w}x{h}'
assert b[-12:-8] == b'IEND', f'PNG lacks ending chunk: {p}'
print(f'Valid Android emulator PNG: {p} {w}x{h} {len(b)} bytes')
PY
  rm -f "$out/$scene.activity.txt"
done
printf '%s\n' "$GITHUB_SHA" > "$out/SOURCE-COMMIT.txt"
printf '%s\n' "Option A direction only; native Android debug-only synthetic fixture; no release approval" > "$out/REVIEW-LIMITS.txt"
(cd "$out" && sha256sum *.png > SHA256SUMS.txt)
echo "Two native Android hub preview screenshots captured and hashed."
