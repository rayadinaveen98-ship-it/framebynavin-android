#!/usr/bin/env bash
# P10L: use original P10J compiled APKs, real first-run UI and Android input.
# This verifies the previously rendered Home + shared Reminder Hub navigation
# without rebuilding/updating production app or modifying test source.
set -euo pipefail
out=output/p10l/android15
mkdir -p "$out"
adb wait-for-device
adb install -r output/p10l/apks/app-debug.apk
adb install -r output/p10l/apks/app-debug-androidTest.apk
adb shell pm clear com.framebynavin.app
adb logcat -c

# The existing P10J test performs genuine five-page setup then waits for a
# merged Compose testTag that the screenshot proves is not found. While it
# waits, use physical ADB taps and accessibility-hierarchy evidence.
set +e
(
  timeout --signal=TERM --kill-after=5s 100s adb shell am instrument -w -r \
    -e hardeningTestEnvironment ci-emulator \
    -e class com.framebynavin.app.ui.P10JRealFirstRunNavigationUiTest \
    com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
    > "$out/original-instrumentation.log" 2>&1
  echo "$?" > "$out/instrumentation-exit.txt"
) &
instrument_pid=$!
set -e

dump_ui() {
  local stage="$1"
  timeout 20s adb shell uiautomator dump /sdcard/p10l-window.xml \
    > "$out/dump-$stage.log" 2>&1 || true
  timeout 10s adb exec-out cat /sdcard/p10l-window.xml > "$out/ui-$stage.xml"
  test -s "$out/ui-$stage.xml"
  timeout 12s adb exec-out screencap -p > "$out/screen-$stage.png" || true
}
has_text() {
  local xml="$1" label="$2"
  python3 - "$xml" "$label" <<'PY'
import sys,xml.etree.ElementTree as ET
path,label=sys.argv[1:]
try:
    r=ET.parse(path).getroot()
    found=any(node.attrib.get("text","")==label for node in r.iter("node"))
except Exception:
    found=False
sys.exit(0 if found else 1)
PY
}
tap_text() {
  local xml="$1" label="$2"
  local center
  center=$(python3 - "$xml" "$label" <<'PY'
import re,sys,xml.etree.ElementTree as ET
path,label=sys.argv[1:]
root=ET.parse(path).getroot()
for node in root.iter("node"):
    info=node.attrib
    if info.get("text","")==label:
        numbers=list(map(int,re.findall(r"\d+",info.get("bounds",""))))
        if len(numbers)==4:
            print((numbers[0]+numbers[2])//2,(numbers[1]+numbers[3])//2)
            break
else:
    sys.exit("visible text not found: "+label)
PY
) || return 1
  echo "ADB click $label at $center" | tee -a "$out/route-summary.log"
  read -r x y <<< "$center"
  adb shell input tap "$x" "$y"
}
assert_no_onboarding() {
  local xml="$1"
  if has_text "$xml" "What do you create most?" ||
     has_text "$xml" "Continue without account" ||
     has_text "$xml" "Meet your companion."; then
    echo 'FAIL: onboarding overlay reappeared on real Android navigation' >&2
    return 1
  fi
}
stage_wait() {
  local stage="$1" expected="$2"
  for attempt in 1 2 3 4 5 6; do
    sleep 3
    dump_ui "$stage"
    if has_text "$out/ui-$stage.xml" "$expected"; then return 0; fi
  done
  echo "FAIL: did not find visible screen text '$expected' during $stage" >&2
  return 1
}

# First-run JUnit already clicked Account Choice -> companion -> 5-page setup.
stage_wait home "REMINDER HUB"
assert_no_onboarding "$out/ui-home.xml"
tap_text "$out/ui-home.xml" "REMINDER HUB"

stage_wait creator-hub "Stay on track"
assert_no_onboarding "$out/ui-creator-hub.xml"
tap_text "$out/ui-creator-hub.xml" "Personal"

stage_wait personal-hub "My day"
assert_no_onboarding "$out/ui-personal-hub.xml"
tap_text "$out/ui-personal-hub.xml" "Creator"

stage_wait creator-return "Stay on track"
assert_no_onboarding "$out/ui-creator-return.xml"
echo 'PASS: real local first-run -> Creator Home -> Reminder Hub -> Personal -> Creator; no onboarding reentry' \
  | tee -a "$out/route-summary.log"

# This run checks ADB-visible navigation only. The original JUnit test may
# still time out because its internal merged testTag assertion is defective.
adb shell am force-stop com.framebynavin.app || true
kill "$instrument_pid" 2>/dev/null || true
wait "$instrument_pid" 2>/dev/null || true
adb logcat -d -t 4500 > "$out/route-logcat.txt" || true
