#!/usr/bin/env bash
# Launches the REAL V217 guide sources through a separate offline Android app.
# Works in the existing Android emulator CI job. Does not touch user phones.
set -euo pipefail
REVIEW="com.framebynavin.v217review"
ORIGINAL="com.framebynavin.app"
APK="apks/review/v217review-debug.apk"
test -s "$APK"
mkdir -p v217-evidence
adb install -r "$APK"
adb shell pm list packages | grep -Fq "package:$ORIGINAL"
adb shell pm list packages | grep -Fq "package:$REVIEW"
adb logcat -c
adb shell am start -W -n "$REVIEW/com.framebynavin.app.preview.V217ReviewActivity"
sleep 3
adb exec-out screencap -p > v217-evidence/v217-sandbox-chooser.png
adb shell uiautomator dump /sdcard/v217-window.xml >/dev/null
adb exec-out cat /sdcard/v217-window.xml > v217-evidence/v217-chooser.xml
# Select the real first-run Frame button by accessibility description.
POS=$(python3 - <<'PY'
import re
import xml.etree.ElementTree as ET
root=ET.parse("v217-evidence/v217-chooser.xml").getroot()
options=[n.get("bounds") for n in root.iter("node") if n.get("content-desc")=="Select Frame"]
assert len(options)==1, f"Select Frame accessibility target missing: {options}"
nums=list(map(int,re.findall(r"\d+",options[0])))
assert len(nums)==4
print((nums[0]+nums[2])//2,(nums[1]+nums[3])//2)
PY
)
adb shell input tap $POS
sleep 2
adb exec-out screencap -p > v217-evidence/v217-sandbox-settings.png
adb shell uiautomator dump /sdcard/v217-window.xml >/dev/null
adb exec-out cat /sdcard/v217-window.xml > v217-evidence/v217-settings.xml
python3 - <<'PY'
from pathlib import Path
import xml.etree.ElementTree as ET
texts=" ".join((n.get("text") or "") for n in ET.parse("v217-evidence/v217-settings.xml").getroot().iter("node"))
assert "Companion Settings" in texts and "BACKLOT" in texts, texts[:1800]
for name in ("v217-sandbox-chooser.png","v217-sandbox-settings.png"):
 p=Path("v217-evidence")/name
 dat=p.read_bytes()
 assert dat[:8]==bytes.fromhex("89504e470d0a1a0a") and p.stat().st_size>10000,(name,len(dat))
 print(name,p.stat().st_size)
PY
# The review app requests no sensitive permissions, even though Backlot does.
PERMS=$(adb shell dumpsys package "$REVIEW" | sed -n '/requested permissions:/,/install permissions:/p')
for forbidden in "android.permission.INTERNET" "android.permission.RECORD_AUDIO" "android.permission.SCHEDULE_EXACT_ALARM" "android.permission.POST_NOTIFICATIONS" "android.permission.READ_CALENDAR"; do
 if grep -Fq "$forbidden" <<< "$PERMS"; then
   echo "UNSAFE REVIEW PERMISSION: $forbidden" >&2
   exit 1
 fi
done
if adb logcat -d -v brief | grep -E "FATAL EXCEPTION:|Process: $REVIEW.*FATAL" | tail -10; then
 echo "Warning: inspect logcat for unrelated fatal exceptions"
fi
echo "PASS: separate no-permissions review app launched and navigated; production Backlot remained installed."
