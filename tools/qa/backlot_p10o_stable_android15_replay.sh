#!/usr/bin/env bash
# P10O infra-only stabilization: same production + original P10M AndroidTest APKs.
# Emulator no longer shares a 2-core, low-RAM cold-boot Launcher ANR with UI tests.
# No Gradle, no APK rebuild, no production state or test-source changes.
set -euo pipefail
out="output/p10o/android15"
mkdir -p "$out"
adb wait-for-device
adb install -r output/p10o/apks/app-debug.apk
adb install -r output/p10o/apks/app-debug-androidTest.apk
echo 'P10O emulator warmup: allow Android Quickstep/launcher startup to settle'
sleep 18

# Catch OS-level ANR BEFORE instrumentation instead of mistaking a blocked
# system-dialog tap for a Backlot navigation issue.
detect_quickstep_anr() {
  local phase="$1"
  local xml="$out/window-$phase.xml"
  timeout 20s adb shell uiautomator dump /sdcard/p10o-ui.xml > "$out/window-$phase-dump.log" 2>&1 || true
  timeout 10s adb exec-out cat /sdcard/p10o-ui.xml > "$xml" || true
  [[ -s "$xml" ]] || return 1
  grep -Eq "Quickstep isn.t responding|Launcher isn.t responding" "$xml"
}
if detect_quickstep_anr boot; then
  timeout 10s adb exec-out screencap -p > "$out/boot-quickstep-anr.png" || true
  echo "INFRA: Quickstep ANR already displayed before opening Backlot" | tee -a "$out/summary.log"
  # Dismiss only the system launcher ANR (not a Backlot dialog).
  center=$(python3 - "$out/window-boot.xml" <<'PY'
import sys,re,xml.etree.ElementTree as ET
r=ET.parse(sys.argv[1]).getroot()
for n in r.iter("node"):
    if n.attrib.get("text")=="Close app":
        b=list(map(int,re.findall(r"\d+",n.attrib.get("bounds",""))))
        if len(b)==4:
            print((b[0]+b[2])//2,(b[1]+b[3])//2)
            break
PY
)
  if [[ -n "$center" ]]; then
    read -r x y <<< "$center"
    adb shell input tap "$x" "$y"
    sleep 5
  fi
  if detect_quickstep_anr boot-after-dismiss; then
    echo "INFRA BLOCKED: Quickstep ANR persists before test; no Backlot fault claim" >&2
    exit 1
  fi
fi

for repeat in 1 2; do
  adb shell am force-stop com.framebynavin.app || true
  adb shell pm clear com.framebynavin.app
  adb logcat -c
  echo "P10O real unchanged P10M first-run journey, local fresh profile $repeat/2"
  set +e
  timeout --signal=TERM --kill-after=5s 170s \
    adb shell am instrument -w -r \
      -e hardeningTestEnvironment ci-emulator \
      -e class com.framebynavin.app.ui.P10JRealFirstRunNavigationUiTest \
      com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
      2>&1 | tee "$out/navigation-$repeat.log"
  rc=${PIPESTATUS[0]}
  set -e
  adb logcat -d -t 3000 > "$out/navigation-$repeat-logcat.txt" || true
  if [[ $rc -ne 0 ]] ||
     ! grep -Fxq 'OK (1 test)' "$out/navigation-$repeat.log" ||
     grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|INSTRUMENTATION_ABORTED|Process crashed|shortMsg=' "$out/navigation-$repeat.log"; then
    timeout 12s adb exec-out screencap -p > "$out/navigation-$repeat-failure.png" || true
    if detect_quickstep_anr "failure-$repeat"; then
      echo "INFRA BLOCKED: Quickstep Android system ANR interfered with navigation run $repeat/2" | tee -a "$out/summary.log" >&2
    else
      echo "FAILED: actual navigation test $repeat/2; examine JUnit + screenshot, no app pass claim" | tee -a "$out/summary.log" >&2
    fi
    exit 1
  fi
  echo "PASS $repeat/2: unchanged real first-run and Creator > Personal > Creator reminder route" | tee -a "$out/summary.log"
done
echo "PASS 2/2 fresh Android 15 profiles; no builds or navigation/test source changes" | tee -a "$out/summary.log"
