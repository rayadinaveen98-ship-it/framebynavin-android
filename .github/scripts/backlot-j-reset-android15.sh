#!/usr/bin/env bash
set -euo pipefail
mkdir -p output/package-j-reset/device-results
adb install -r output/package-j-reset/app-debug.apk
adb install -r output/package-j-reset/app-debug-androidTest.apk
# The runner invokes 'script' through /bin/sh, so all compound bash work
# belongs in this single file instead of fragmented YAML shell lines.
adb shell am instrument -w -r \
  -e class com.framebynavin.app.ui.V383DeviceResetSafetyAndroidTest,com.framebynavin.app.data.V384UnifiedRestoreEndToEndAndroidTest \
  -e hardeningTestEnvironment ci-emulator \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
  | tee output/package-j-reset/device-results/RESET-SAFETY.log
! grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|INSTRUMENTATION_ABORTED|shortMsg=|Process crashed' output/package-j-reset/device-results/RESET-SAFETY.log
grep -Eq '^OK \([1-9][0-9]* tests?\)' output/package-j-reset/device-results/RESET-SAFETY.log

# Exercise the Android SYSTEM app-data reset lifecycle only after all
# non-destructive UI and restore tests have succeeded. This CI-only operation
# must never run against a physical/owner device.
test "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" = "1"
test "$(adb shell getprop ro.build.version.sdk | tr -d '\r')" = "35"
package="com.framebynavin.app"
external="/sdcard/Download/backlot-v385-disposable-external-backup.txt"
owner_sentinel="files/backlot-v385-ci-app-owned-sentinel"
retained_history="files/backlot-unified-retained-recovery-history-v1"
adb shell mkdir -p /sdcard/Download
adb shell "printf 'v385-off-app-test-only\\n' > '$external'"
adb shell run-as "$package" sh -c "printf 'v385-private-test-only\\n' > '$owner_sentinel'"
adb shell run-as "$package" test -f "$owner_sentinel"
# The prior restore must have created protected retained original/target history.
adb shell run-as "$package" test -d "$retained_history"
result="$(adb shell pm clear "$package" | tr -d '\r')"
printf 'Android 15 emulator system clear result: %s\n' "$result" |
  tee output/package-j-reset/device-results/DEVICE-CLEAR.txt
test "$result" = "Success"
# pm clear preserves installed application binary, but erases its private files.
adb shell pm path "$package" | grep -q '^package:'
adb shell run-as "$package" pwd | grep -q "$package"
if adb shell run-as "$package" test -e "$owner_sentinel"; then
  echo 'FAIL: CI private owner sentinel survived system wipe' >&2
  exit 1
fi
if adb shell run-as "$package" test -e "$retained_history"; then
  echo 'FAIL: CI encrypted original/target history survived system wipe' >&2
  exit 1
fi
# Separately exported external file is outside app data and MUST remain.
adb shell cat "$external" | tr -d '\r' | grep -Fxq 'v385-off-app-test-only'
echo 'PASS: synthetic app-private records and escrow erased; external file retained' |
  tee -a output/package-j-reset/device-results/DEVICE-CLEAR.txt

