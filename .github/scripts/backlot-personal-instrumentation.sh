#!/usr/bin/env bash
# Run as ONE bash process. The Android emulator action executes each "script"
# line separately, so a multi-line YAML for-loop is NOT a valid input.
set -euo pipefail

if [ "$#" -lt 1 ]; then
  echo "Provide at least one full AndroidJUnitRunner test class" >&2
  exit 2
fi

adb install -r output/app-debug.apk
adb install -r output/app-debug-androidTest.apk

for classname in "$@"; do
  echo "TEST CLASS: $classname"
  adb shell pm clear com.framebynavin.app
  logfile="$(mktemp)"
  adb shell am instrument -w -r \
    -e hardeningTestEnvironment ci-emulator \
    -e class "$classname" \
    com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner | tee "$logfile"
  expected=2
  case "$classname" in
    *V288PersonalOsUiTest) expected=4 ;;
    *V274SelfCareEditorUiTest) expected=1 ;;
  esac
  grep -F "OK (${expected} tests)" "$logfile"
  if grep -E 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' "$logfile"; then
    echo "Android instrumentation failed: $classname" >&2
    exit 1
  fi
  rm -f "$logfile"
done

echo "PASS: all requested Personal instrumentation classes"
