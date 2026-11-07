#!/usr/bin/env bash
# Android-emulator-runner@v2 executes each YAML script line in a fresh shell.
# Run this standalone file as one command so the capture loop is a real unit.
set -euo pipefail

out="output/backlot-phase1-option-A"
mkdir -p "$out"
adb wait-for-device
adb shell getprop sys.boot_completed | grep -qx '1'
adb install -r app/build/outputs/apk/debug/app-debug.apk

for scene in cloud challenge-create challenge-dashboard alarm delete-account; do
  echo "Capturing native Compose fixture: $scene"
  adb shell am force-stop com.framebynavin.app || true
  adb shell am start -W -n com.framebynavin.app/.phase1.Phase1VisualReviewActivity --es phase1_screen "$scene"
  sleep 3
  adb shell dumpsys activity activities > "$out/$scene.activity.txt"
  if ! grep -q 'Phase1VisualReviewActivity' "$out/$scene.activity.txt"; then
    echo "Review Activity not visible for $scene" >&2
    adb logcat -d -t 300 > "$out/$scene.logcat.txt" || true
    exit 1
  fi
  adb exec-out screencap -p > "$out/$scene.png"
  test -s "$out/$scene.png"
  # PNG signature. An empty/error screenshot cannot pass this check.
  python3 - "$out/$scene.png" <<'PY'
import sys
from pathlib import Path
path = Path(sys.argv[1])
with path.open('rb') as file:
    assert file.read(8) == b'\x89PNG\r\n\x1a\n', f'Invalid PNG: {path}'
print('Verified PNG header:', path)
PY
  rm -f "$out/$scene.activity.txt"
done

printf '%s\n' "$GITHUB_SHA" > "$out/SOURCE-COMMIT.txt"
(
  cd "$out"
  sha256sum *.png > SHA256SUMS.txt
)
echo "Captured all five real Android 15 Compose screenshots."
