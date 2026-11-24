#!/usr/bin/env bash
# Only on a fresh disposable CI emulator; never run on owner hardware.
# Test the EXACT previously signed personalStable APK, no Gradle / app rebuild.
set -euo pipefail
out=output/backlot-s7/owner-apk-repro
mkdir -p "$out"
pkg=com.framebynavin.app.lab
app=output/backlot-s7/owner-apk/Backlot-Lab-R2-Stabilization-S6-Owner-Preview.apk
test -s "$app"
adb wait-for-device

snapshot() {
  local name="$1"
  adb logcat -d -v threadtime > "$out/${name}-logcat.txt" || true
  adb shell dumpsys activity activities > "$out/${name}-activities.txt" || true
  adb shell dumpsys package "$pkg" > "$out/${name}-package.txt" || true
  adb shell uiautomator dump /sdcard/backlot-owner-ui.xml > "$out/${name}-uiautomator.log" 2>&1 || true
  adb pull /sdcard/backlot-owner-ui.xml "$out/${name}-ui.xml" >/dev/null 2>&1 || true
  adb shell screencap -p /sdcard/backlot-owner-screen.png >/dev/null 2>&1 || true
  adb pull /sdcard/backlot-owner-screen.png "$out/${name}-screen.png" >/dev/null 2>&1 || true
  echo "=== SIGNIFICANT ANDROID CRASH LOGS ($name) ==="
  grep -En -A 32 -B 3 'FATAL EXCEPTION|AndroidRuntime|Process: com\.framebynavin\.app\.lab|Caused by:|Unable to (create|start|instantiate)|NoClassDefFoundError|VerifyError|ClassNotFoundException' "$out/${name}-logcat.txt" | tail -180 || true
}
check_alive() {
  local name="$1"
  snapshot "$name"
  if ! adb shell pidof "$pkg" | tr -d '\r' | grep -Eq '^[0-9]+'; then
    echo "FAIL: com.framebynavin.app.lab is not running after $name; see logcat/screenshot" | tee "$out/${name}-result.txt"
    return 1
  fi
  if grep -Eq 'FATAL EXCEPTION' "$out/${name}-logcat.txt" &&
     grep -Eq 'Process: com\.framebynavin\.app\.lab' "$out/${name}-logcat.txt"; then
    echo "FAIL: owner APK crashed on $name, even if a process restarted" | tee "$out/${name}-result.txt"
    return 1
  fi
  echo "PASS: owner app process remains alive after $name; check screen for visible startup" | tee "$out/${name}-result.txt"
}

# FRESH install (only disposable emulator environment).
if adb shell pm path "$pkg" | grep -Fq 'package:'; then
  echo "STOP: unexpected pre-existing Lab package in fresh emulator" >&2; exit 1
fi
if adb shell pm path com.framebynavin.app | grep -Fq 'package:'; then
  echo "STOP: production Backlot app present; abort" >&2; exit 1
fi
adb install -r "$app" | tee "$out/fresh-install.txt"
adb logcat -c
adb shell am start -W -n "$pkg/com.framebynavin.app.MainActivity" | tee "$out/fresh-start.txt"
sleep 32
check_alive fresh || exit 2
adb shell am force-stop "$pkg"
adb logcat -c
adb shell am start -W -n "$pkg/com.framebynavin.app.MainActivity" | tee "$out/fresh-restart.txt"
sleep 20
check_alive fresh-restart || exit 3

# Upgrade from the previous *debug* APK with guest onboarding state kept.
# This is a disposable simulator only. No cloud login, no removal of owner data.
debug=output/backlot-s7/previous-debug/app-debug.apk
instrumentation=output/backlot-s7/previous-debug/app-debug-androidTest.apk
test -s "$debug"; test -s "$instrumentation"
adb uninstall "$pkg" | tee "$out/disposable-emulator-only-uninstall.txt"
adb install -r "$debug" | tee "$out/older-debug-install.txt"
adb install -r "$instrumentation" | tee "$out/instrumentation-install.txt"
adb logcat -c
set +e
timeout --signal=TERM --kill-after=5s 200s \
  adb shell am instrument -w -r \
    -e hardeningTestEnvironment ci-emulator \
    -e class com.framebynavin.app.ui.P10JRealFirstRunNavigationUiTest \
    com.framebynavin.app.lab.test/androidx.test.runner.AndroidJUnitRunner \
    2>&1 | tee "$out/guest-first-run-before-upgrade.txt"
rc=${PIPESTATUS[0]}
set -e
if [[ "$rc" -ne 0 ]] || ! grep -Fxq 'OK (1 test)' "$out/guest-first-run-before-upgrade.txt"; then
  snapshot guest-seed-failure
  echo "BLOCKED: cannot prove guest-state setup before signed owner upgrade" >&2
  exit 4
fi
adb shell am force-stop "$pkg" || true
adb install -r "$app" | tee "$out/owner-in-place-upgrade.txt"
adb logcat -c
adb shell am start -W -n "$pkg/com.framebynavin.app.MainActivity" | tee "$out/upgraded-start.txt"
sleep 32
check_alive upgraded-with-guest-state || exit 5
echo "PASS: exact owner personalStable APK survived fresh start, restart and guest-state update; physical user data path still not certified" | tee "$out/SUMMARY.txt"
