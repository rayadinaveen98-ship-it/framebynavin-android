#!/usr/bin/env bash
# P10L focused correction: repeat the ORIGINAL P10J JUnit first-run journey
# using the EXACT APKs that already compiled. No UIAutomator/tap racing.
# Fresh local app profile on each iteration. No signing, auth or hosted data.
set -euo pipefail
out="output/p10l/android15"
mkdir -p "$out"
adb wait-for-device
adb install -r output/p10l/apks/app-debug.apk
adb install -r output/p10l/apks/app-debug-androidTest.apk

# The prior shell-driven P10L job incorrectly marked FAIL after JUnit had
# completed successfully (OK (1 test), in 11.167s), then it tapped the
# launcher. Do NOT send external taps while Compose test drives the app.
for pass in 1 2; do
  echo "P10L actual first-run replay $pass/2 on a fresh disposable profile"
  adb shell am force-stop com.framebynavin.app || true
  adb shell pm clear com.framebynavin.app
  adb logcat -c
  set +e
  timeout --signal=TERM --kill-after=5s 180s \
    adb shell am instrument -w -r \
      -e hardeningTestEnvironment ci-emulator \
      -e class com.framebynavin.app.ui.P10JRealFirstRunNavigationUiTest \
      com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
      > "$out/first-run-repeat-$pass.log" 2>&1
  result=$?
  set -e
  adb logcat -d -t 2000 > "$out/first-run-repeat-$pass-logcat.txt" || true
  if [[ $result -ne 0 ]] ||
    ! grep -Fxq 'OK (1 test)' "$out/first-run-repeat-$pass.log" ||
    grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|INSTRUMENTATION_ABORTED|Process crashed|shortMsg=' "$out/first-run-repeat-$pass.log"; then
    echo "FAIL: original P10J real first-run navigation journey, repetition $pass/2" \
      | tee -a "$out/replay-summary.txt" >&2
    cat "$out/first-run-repeat-$pass.log" | tail -70 >&2
    exit 1
  fi
  echo "PASS $pass/2: real account choice, companion, five-page setup, Home, Hub, Personal, Creator" \
    | tee -a "$out/replay-summary.txt"
done
echo "PASS: Original P10J test repeated 2/2 times with no rebuild and no external tap racing" \
  | tee -a "$out/replay-summary.txt"
