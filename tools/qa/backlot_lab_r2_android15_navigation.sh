#!/usr/bin/env bash
# Backlot Lab R2: disposable Android 15 first-run journey, two replays.
# No Google sign-in, original Backlot package, cloud record writes or owner phone.
set -euo pipefail
out="output/backlot-r2/android15"
mkdir -p "$out"
pkg="com.framebynavin.app.lab"
testpkg="com.framebynavin.app.lab.test"
adb wait-for-device
adb install -r output/backlot-r2/apks/app-debug.apk
adb install -r output/backlot-r2/apks/app-debug-androidTest.apk
adb shell pm path "$pkg" | grep -Fq "package:"
adb shell pm path "$testpkg" | grep -Fq "package:"
if adb shell pm path com.framebynavin.app | grep -Fq "package:"; then
  echo "STOP: unrelated production Backlot package present in disposable QA emulator" >&2
  exit 1
fi
sleep 18
for attempt in 1 2; do
  adb shell am force-stop "$pkg" || true
  adb shell pm clear "$pkg" | grep -Fq "Success"
  adb logcat -c
  echo "Backlot Lab R2 real first-run test, fresh emulator app state, pass $attempt/2"
  set +e
  timeout --signal=TERM --kill-after=5s 190s \
    adb shell am instrument -w -r \
      -e hardeningTestEnvironment ci-emulator \
      -e class com.framebynavin.app.ui.P10JRealFirstRunNavigationUiTest \
      "$testpkg/androidx.test.runner.AndroidJUnitRunner" \
      2>&1 | tee "$out/first-run-$attempt.log"
  rc=${PIPESTATUS[0]}
  set -e
  adb logcat -d -t 2400 > "$out/first-run-$attempt-logcat.txt" || true
  if [[ $rc -ne 0 ]] ||
    ! grep -Fxq 'OK (1 test)' "$out/first-run-$attempt.log" ||
    grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|INSTRUMENTATION_ABORTED|Process crashed|shortMsg=' "$out/first-run-$attempt.log"; then
    echo "STOP: Android 15 Backlot Lab R2 navigation failed on pass $attempt/2" >&2
    exit 1
  fi
  echo "PASS: Backlot Lab R2 real first-run navigation $attempt/2"
done
echo "PASS: Android 15 Lab Creator-first setup -> Home -> Personal-default Reminder Hub -> Creator/Personal 2/2"
