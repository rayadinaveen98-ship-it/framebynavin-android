#!/usr/bin/env bash
# Disposable Backlot Lab only. NEVER run on physical / owner package.
set -euo pipefail

mkdir -p output/recovery-stage-c
evidence=output/recovery-stage-c
package=com.framebynavin.app.lab
test_package=com.framebynavin.app.lab.test
runner="${test_package}/androidx.test.runner.AndroidJUnitRunner"
apk=lab-stage-b/app/build/outputs/apk/debug/app-debug.apk
test_apk=lab-stage-b/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk

test -s "$apk"
test -s "$test_apk"
test -s lab-stage-b/output/recovery-stage-b-android/SHA256SUMS.txt
( cd lab-stage-b && sha256sum -c output/recovery-stage-b-android/SHA256SUMS.txt ) | tee "$evidence/LAB-APK-SHA256.txt"

sdk="$(adb shell getprop ro.build.version.sdk | tr -d '\r')"
hardware="$(adb shell getprop ro.hardware | tr -d '\r')"
serial="$(adb get-serialno)"
{
  printf "SDK=%s\nHARDWARE=%s\nSERIAL=%s\n" "$sdk" "$hardware" "$serial"
  printf "PACKAGE=%s\nTEST_PACKAGE=%s\n" "$package" "$test_package"
} | tee "$evidence/ANDROID-IDENTITY.txt"
test "$sdk" = "35"
case "$hardware" in ranchu|goldfish) ;; *) echo "STOP: not a disposable Android emulator" >&2; exit 1;; esac
case "$serial" in emulator-*) ;; *) echo "STOP: unexpected ADB serial" >&2; exit 1;; esac

aapt_bin="$(find "$ANDROID_HOME/build-tools" -type f -name aapt | sort -V | tail -n 1)"
"$aapt_bin" dump badging "$apk" > "$evidence/APP-BADGING.txt"
"$aapt_bin" dump badging "$test_apk" > "$evidence/TEST-BADGING.txt"
grep -q "package: name='$package'" "$evidence/APP-BADGING.txt"
grep -q "package: name='$test_package'" "$evidence/TEST-BADGING.txt"
grep -q "application-label:'Backlot'" "$evidence/APP-BADGING.txt"

adb install -r "$apk" | tee "$evidence/APP-INSTALL.txt"
adb install -r "$test_apk" | tee "$evidence/TEST-INSTALL.txt"
adb shell pm list packages | grep -F "package:$package"
adb shell pm list packages | grep -F "package:$test_package"
adb logcat -c

collect() {
  local n="$1"
  adb exec-out screencap -p > "$evidence/${n}-screen.png" || true
  adb shell dumpsys window > "$evidence/${n}-window.txt" || true
  adb logcat -d -v time > "$evidence/${n}-logcat.txt" || true
}

run_class() {
  local class="$1" slug="$2" expected="$3"
  echo "START ${slug}: ${class}" | tee -a "$evidence/CASE-RESULTS.txt"
  local rc=0
  timeout --signal=TERM --kill-after=20s 14m adb shell am instrument -w -r \
    -e hardeningTestEnvironment ci-emulator -e class "$class" \
    "$runner" > "$evidence/${slug}-instrumentation.txt" 2>&1 || rc=$?
  cat "$evidence/${slug}-instrumentation.txt" | tail -n 65
  collect "$slug"
  if [ "$rc" -ne 0 ] || ! grep -Fq "OK (${expected} test" "$evidence/${slug}-instrumentation.txt" ||
       grep -Eq 'FAILURES!!!|Process crashed|INSTRUMENTATION_FAILED' "$evidence/${slug}-instrumentation.txt"; then
    echo "FAIL ${slug}: emulator instrumentation failed or passed count mismatched (exit=${rc})." | tee -a "$evidence/CASE-RESULTS.txt"
    return 1
  fi
  echo "PASS ${slug}: ${expected} tests" | tee -a "$evidence/CASE-RESULTS.txt"
}

# Synthetic tests first; no credentials or real account data.
run_class com.framebynavin.app.ui.BacklotV1RecoveryAccountUiTest account-retry 4
run_class com.framebynavin.app.reminders.BacklotV1RecoveryAppearanceUiTest reminder-appearance 2

# Root local-first test MUST start from truly fresh disposable Lab data.
# The hardware check above must precede this destructive command.
adb shell am force-stop "$package" || true
adb shell pm clear "$package" | tee "$evidence/LAB-DATA-CLEAR.txt"
grep -Fq Success "$evidence/LAB-DATA-CLEAR.txt"
run_class com.framebynavin.app.ui.BacklotV1RecoveryNavigationJourneyUiTest first-run-home-navigation 1

echo "PASS - all seven targeted UI tests; no owner app, hosted cloud or production package touched." | tee "$evidence/RESULT.txt"
