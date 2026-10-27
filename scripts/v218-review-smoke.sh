#!/usr/bin/env bash
# Execute ONLY while CI Android emulator is running, never on the owner's phone.
set -euo pipefail
REVIEW="com.framebynavin.v218review"
APP="com.framebynavin.app"
APK="review-apk/v218review-debug.apk"
test -s "$APK"
mkdir -p v218-review-evidence
adb install -r "$APK"
adb shell pm list packages | grep -Fq "package:$REVIEW"
adb shell pm list packages | grep -Fq "package:$APP"
adb shell settings put global animator_duration_scale 1.0
adb shell settings put global transition_animation_scale 1.0
adb shell settings put global window_animation_scale 1.0
adb shell am force-stop "$REVIEW" || true
adb shell am start -W -n "$REVIEW/com.framebynavin.app.preview.V218WaveReviewActivity"
sleep 4
adb exec-out screencap -p > v218-review-evidence/review-alarm-wave.png
adb shell uiautomator dump /sdcard/v218-review.xml >/dev/null
adb exec-out cat /sdcard/v218-review.xml > v218-review-evidence/alarm.xml
python3 - <<'PY'
import xml.etree.ElementTree as ET
root=ET.parse("v218-review-evidence/alarm.xml").getroot()
nodes=list(root.iter("node"))
labels=[(n.get("text") or "")+" "+(n.get("content-desc") or "") for n in nodes]
assert any("BACKLOT / V218 WAVE LAB" in x for x in labels), labels[:30]
assert any("ALARM" in x for x in labels),labels[:30]
assert any("VOICE" in x for x in labels),labels[:30]
PY
# Use a real Android accessibility/UI hierarchy element, never hardcode tap coords.
get_center() {
  TARGET="$1" python3 - <<'PY'
import os,re,xml.etree.ElementTree as ET
root=ET.parse("v218-review-evidence/alarm.xml").getroot()
wanted=os.environ["TARGET"]
hits=[n.get("bounds") for n in root.iter("node")
    if n.get("text")==wanted or n.get("content-desc")==wanted]
assert hits, f"missing expected control {wanted}"
v=list(map(int,re.findall(r"\d+",hits[0])))
assert len(v)==4,v
print((v[0]+v[2])//2,(v[1]+v[3])//2)
PY
}
adb shell input tap $(get_center "VOICE")
sleep 3
adb exec-out screencap -p > v218-review-evidence/review-voice-wave.png
adb shell uiautomator dump /sdcard/v218-review.xml >/dev/null
adb exec-out cat /sdcard/v218-review.xml > v218-review-evidence/voice.xml
for image in v218-review-evidence/review-alarm-wave.png v218-review-evidence/review-voice-wave.png; do
  IMAGE="$image" python3 - <<'PY'
import os
from pathlib import Path
p=Path(os.environ["IMAGE"]); b=p.read_bytes()
assert b[:8]==bytes.fromhex("89504e470d0a1a0a") and len(b)>20000,(p,len(b))
print("PASS real emulator PNG:",p,len(b))
PY
done
# Only sandbox app itself has an Android package. NEVER update installed Backlot.
! adb shell dumpsys package "$REVIEW" | grep -F "android.permission.INTERNET"
! adb shell dumpsys package "$REVIEW" | grep -F "android.permission.SCHEDULE_EXACT_ALARM"
! adb shell dumpsys package "$REVIEW" | grep -F "android.permission.RECORD_AUDIO"
echo "PASS: review app launched side-by-side with existing Backlot, no sensitive permissions"
