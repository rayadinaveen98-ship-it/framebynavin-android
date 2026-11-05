#!/usr/bin/env bash
# CI-EMULATOR ONLY. Captures synthetic UI, never reads user's phone or cloud.
set -euo pipefail
test "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" = "1" || {
  echo "FAIL: visual capture must run only on a disposable emulator" >&2
  exit 1
}
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell pm clear com.framebynavin.app # disposable test emulator only
logfile="$(mktemp)"
if ! adb shell am instrument -w -r \
  -e hardeningTestEnvironment ci-emulator \
  -e class com.framebynavin.app.ui.V294VisualAuditDeviceTest \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner | tee "$logfile"; then
  adb logcat -d -v threadtime | tail -160 || true
  rm -f "$logfile";exit 1
fi
if ! grep -Fq "OK (10 tests)" "$logfile" ||
   grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' "$logfile"; then
  cat "$logfile" >&2
  adb logcat -d -v threadtime | tail -160 || true
  rm -f "$logfile";exit 1
fi
rm -f "$logfile"
# Test real navigation, backup boundaries and large-font Settings separately
# from screenshot captures. Keep this strictly on the disposable emulator.
settings_log="$(mktemp)"
adb shell am instrument -w -r \
  -e hardeningTestEnvironment ci-emulator \
  -e class com.framebynavin.app.ui.V167SettingsNavigationUiTest,com.framebynavin.app.ui.V170BackupSettingsUiTest,com.framebynavin.app.ui.V166SettingsSafetyUiTest,com.framebynavin.app.ui.V169SettingsRemindersPlanningUiTest,com.framebynavin.app.ui.V197SettingsOnboardingUiTest,com.framebynavin.app.ui.V288PersonalOsUiTest \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner | tee "$settings_log"
if ! grep -Fq "OK (20 tests)" "$settings_log" ||
   grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' "$settings_log"; then
  cat "$settings_log" >&2
  adb logcat -d -v threadtime | tail -180 || true
  rm -f "$settings_log"
  exit 1
fi
rm -f "$settings_log"
# Confirm Space CRUD stays isolated from Personal reminders and that deletion
# is secondary, explicit and cancellable; include the explicit metric regression.
spaces_log="$(mktemp)"
adb shell am instrument -w -r \
  -e hardeningTestEnvironment ci-emulator \
  -e class com.framebynavin.app.ui.V289PersonalSpacesUiTest,com.framebynavin.app.ui.V292SpaceMetricUiTest \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner | tee "$spaces_log"
if ! grep -Fq "OK (10 tests)" "$spaces_log" ||
   grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' "$spaces_log"; then
  cat "$spaces_log" >&2
  adb logcat -d -v threadtime | tail -180 || true
  rm -f "$spaces_log"
  exit 1
fi
rm -f "$spaces_log"
mkdir -p output/v294-visual-audit
adb pull /sdcard/Android/data/com.framebynavin.app/files/v294-visual-audit/. output/v294-visual-audit/
test "$(find output/v294-visual-audit -maxdepth 1 -name '*.png' | wc -l)" -eq 10
(cd output/v294-visual-audit && sha256sum *.png > SHA256SUMS.txt)
echo "PASS: ten synthetic V295 Android 15 screenshots captured; no owner data"
