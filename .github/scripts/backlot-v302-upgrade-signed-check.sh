#!/usr/bin/env bash
set -euo pipefail

old=output/v294/Backlot-V294-Signed-Upgrade-Candidate.apk
new=output/v302/Backlot-V302-Signed-Owner-Preview-295.apk
test -s "$old" && test -s "$new"

echo "Install signed V294 baseline on clean Android 15 emulator"
adb install "$old"

# UID is the Android package identity retained by an in-place install.
# Do not depend on OEM-specific dumpsys package formatting.
uid_of() {
  adb shell pm list packages -U com.framebynavin.app |
    sed -n 's/.*package:com\.framebynavin\.app uid:\([0-9][0-9]*\).*/\1/p' |
    head -1
}
before="$(uid_of)"
test -n "$before" || { echo "Missing V294 UID" >&2; exit 1; }
adb shell dumpsys package com.framebynavin.app > output/v294/PREUPGRADE-PACKAGE.txt
grep -Fq 'versionCode=294' output/v294/PREUPGRADE-PACKAGE.txt

echo "Install signed V302 update WITHOUT uninstall or app-data clear"
adb install -r "$new"

after="$(uid_of)"
test -n "$after" || { echo "Missing V302 UID" >&2; exit 1; }
test "$before" = "$after" || {
  echo "FAIL: install replaced Android package identity: $before -> $after" >&2
  exit 1
}
adb shell dumpsys package com.framebynavin.app > output/v302/POSTUPGRADE-PACKAGE.txt
grep -Fq 'versionCode=295' output/v302/POSTUPGRADE-PACKAGE.txt

echo "Cold launch signed upgraded app"
adb shell monkey -p com.framebynavin.app -c android.intent.category.LAUNCHER 1
sleep 4
adb shell pm list packages com.framebynavin.app | grep -Fq 'package:com.framebynavin.app'
echo "PASS: V294 -> signed V302 versionCode=295 in-place install with UID=$after retained"
