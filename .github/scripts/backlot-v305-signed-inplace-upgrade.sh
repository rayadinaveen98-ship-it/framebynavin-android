#!/usr/bin/env bash
set -euo pipefail

old="output/v302/Backlot-V302-Signed-Owner-Preview-295.apk"
new="output/v305/Backlot-V305-Signed-Owner-Preview-296.apk"
test -s "$old" && test -s "$new"

uid_of() {
  adb shell pm list packages -U com.framebynavin.app |
    tr -d '\r' |
    sed -n 's/.*package:com\.framebynavin\.app uid:\([0-9][0-9]*\).*/\1/p' |
    head -1
}

echo "Install V302 signed preview first, to emulate the current owner's app"
adb install "$old"
before="$(uid_of)"
test -n "$before" || { echo "FAIL: cannot inspect V302 package UID" >&2; exit 1; }
adb shell dumpsys package com.framebynavin.app > output/v302/PREUPGRADE-PACKAGE.txt
grep -Fq 'versionCode=295' output/v302/PREUPGRADE-PACKAGE.txt

echo "Upgrade V302 to signed V305 (without uninstall or data clear)"
adb install -r "$new"
after="$(uid_of)"
test -n "$after" || { echo "FAIL: cannot inspect V305 package UID" >&2; exit 1; }
test "$before" = "$after" || {
  echo "FAIL: signed upgrade replaced package identity: $before to $after" >&2
  exit 1
}
adb shell dumpsys package com.framebynavin.app > output/v305/POSTUPGRADE-PACKAGE.txt
grep -Fq 'versionCode=296' output/v305/POSTUPGRADE-PACKAGE.txt

echo "Cold launch signed V305 owner-test APK"
adb shell monkey -p com.framebynavin.app -c android.intent.category.LAUNCHER 1
sleep 5
adb shell pm list packages com.framebynavin.app | grep -Fq 'package:com.framebynavin.app'
echo "PASS: signed V302 -> V305 in-place APK upgrade, version code 296, package UID $after retained"
