#!/usr/bin/env bash
# Backlot Stage 1–6 smoke matrix: disposable CI Android emulator ONLY.
# NEVER run on the founder's device; the harness intentionally clears synthetic
# emulator application state before each isolated test class.
set -euo pipefail

out=output/p1-sixstage/android15
mkdir -p "$out"
adb wait-for-device
test "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" = "1" || {
  echo "REFUSED: this QA harness is restricted to disposable emulators." >&2
  exit 1
}
adb shell getprop sys.boot_completed | tr -d '\r' | grep -qx '1'

adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am force-stop com.framebynavin.app || true

classes=(
  com.framebynavin.app.ui.P1SixStageQuickstartSceneDeviceTest
  com.framebynavin.app.widget.P1SixStageWidgetProviderDeviceTest
  com.framebynavin.app.ui.V207FirstRunUiTest
  com.framebynavin.app.reminders.V195ReminderSceneUiTest
  com.framebynavin.app.reminders.V159ReminderAccessibilityAcceptanceTest
  com.framebynavin.app.ui.V2215CompanionAccessibilityUiTest
  com.framebynavin.app.ui.V294FirstRunEmptyStateDeviceTest
)

printf 'CI class\tstatus\n' > "$out/RESULTS.tsv"
for class in "${classes[@]}"; do
  name="${class##*.}"
  echo "Running disposable Android 15 Stage QA: $class"
  # Clear only the synthetic CI package. No owner data is accessible.
  adb shell pm clear com.framebynavin.app > "$out/CLEAR_${name}.txt"
  adb shell pm grant com.framebynavin.app android.permission.POST_NOTIFICATIONS
  adb shell appops set com.framebynavin.app SCHEDULE_EXACT_ALARM allow
  if timeout --signal=TERM --kill-after=20s 15m adb shell am instrument -w -r \
    -e hardeningTestEnvironment ci-emulator \
    -e backlotVisualProfile default \
    -e class "$class" \
    com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
    > "$out/${name}.txt" 2>&1; then
    if grep -Eq 'OK \([1-9][0-9]* tests?\)' "$out/${name}.txt" &&
       ! grep -Eq 'FAILURES!!!|INSTRUMENTATION_STATUS_CODE: -2' "$out/${name}.txt"; then
      printf '%s\tPASS\n' "$class" >> "$out/RESULTS.tsv"
      tail -n 12 "$out/${name}.txt"
    else
      printf '%s\tFAIL: no valid success receipt\n' "$class" >> "$out/RESULTS.tsv"
      cat "$out/${name}.txt"
      adb logcat -d -t 1200 > "$out/${name}-LOGCAT.txt" 2>&1 || true
      exit 1
    fi
  else
    printf '%s\tFAIL: instrumentation returned error or timed out\n' "$class" >> "$out/RESULTS.tsv"
    cat "$out/${name}.txt"
    adb logcat -d -t 1200 > "$out/${name}-LOGCAT.txt" 2>&1 || true
    exit 1
  fi
done
cat "$out/RESULTS.tsv"
printf '%s\n' 'Emulator test subset only. Real physical keyboard/accessibility/launcher pin/Doze/reboot/voice/music and owner migration are NOT certified.' > "$out/LIMITS.txt"
