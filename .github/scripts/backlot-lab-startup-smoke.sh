#!/usr/bin/env bash
# Isolated Backlot Lab APK launch smoke test. Never clears user data or touches
# the production application ID, original Supabase, or physical devices.
set -euo pipefail
mkdir -p startup-smoke-evidence
artifact="lab-candidate/app-debug.apk"
package="com.framebynavin.app.lab"
activity="com.framebynavin.app.MainActivity"
test -s "$artifact"
adb install -r "$artifact" | tee startup-smoke-evidence/install.txt
adb shell am force-stop "$package" || true
adb logcat -c
set +e
adb shell am start -W -n "$package/$activity" 2>&1 | tee startup-smoke-evidence/activity-start.txt
start_status=${PIPESTATUS[0]}
set -e
sleep 20
adb shell pidof "$package" > startup-smoke-evidence/process.txt || true
adb shell dumpsys window | grep -E 'mCurrentFocus|mFocusedApp' > startup-smoke-evidence/foreground-window.txt || true
adb logcat -d -v time > startup-smoke-evidence/logcat.txt
adb exec-out screencap -p > startup-smoke-evidence/screen.png || true
if [ "$start_status" -ne 0 ] || ! grep -q 'Status: ok' startup-smoke-evidence/activity-start.txt; then
  echo 'FAIL: Android ActivityManager did not acknowledge Lab launch.'
  exit 1
fi
if [ ! -s startup-smoke-evidence/process.txt ]; then
  echo 'FAIL: Lab process died during 20-second startup window.'
  grep -n -A 15 -B 3 'FATAL EXCEPTION' startup-smoke-evidence/logcat.txt | tail -n 80 || true
  exit 1
fi
if grep -Eq 'Process: com\.framebynavin\.app\.lab, PID:' startup-smoke-evidence/logcat.txt; then
  echo 'FAIL: Lab process has AndroidRuntime fatal exception.'
  grep -n -A 15 -B 3 'FATAL EXCEPTION' startup-smoke-evidence/logcat.txt | tail -n 80 || true
  exit 1
fi
if ! grep -Fq "$package" startup-smoke-evidence/foreground-window.txt; then
  echo 'FAIL: Lab did not become the foreground window.'
  cat startup-smoke-evidence/foreground-window.txt
  exit 1
fi
echo 'PASS: Backlot Lab launches to foreground, survives startup and reports no package fatal exception.'
