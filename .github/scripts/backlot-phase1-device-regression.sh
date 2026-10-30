#!/usr/bin/env bash
# Phase 1: immutable Build270, synthetic Android emulators only.
set -euo pipefail
profile="$1"
case "$profile" in compact|tablet) ;; *) echo "Unknown phase1 profile" >&2; exit 2 ;; esac
mkdir -p "phase1-evidence/$profile"
test -s tests/app-debug.apk
test -s tests/app-debug-androidTest.apk
test -s owner/Backlot-RC25-Build270-Full-App.apk
test "$(sha256sum owner/Backlot-RC25-Build270-Full-App.apk | cut -d' ' -f1)" = \
  "74ec1aa31fd9cf9083103423bc6040e2451db1fb454419823150f5cd1b568780"
adb wait-for-device
adb shell getprop ro.build.version.sdk | tee "phase1-evidence/$profile/device-api.txt"
adb shell getprop ro.product.model | tee "phase1-evidence/$profile/device-model.txt"
if test "$profile" = compact; then
  adb shell wm size 900x1900
  adb shell wm density 400
  adb shell settings put system font_scale 1.30
else
  adb shell wm size 1600x2560
  adb shell wm density 320
  adb shell settings put system font_scale 1.15
fi
adb shell wm size | tee "phase1-evidence/$profile/size.txt"
adb shell wm density | tee "phase1-evidence/$profile/density.txt"
adb shell settings get system font_scale | tee "phase1-evidence/$profile/font-scale.txt"
if test "$profile" = tablet; then
  test -n "$(find old -name app-debug.apk -print -quit)"
  mkdir -p new
  cp tests/app-debug.apk new/app-debug.apk
  cp tests/app-debug-androidTest.apk new/app-debug-androidTest.apk
  # Synthetic, safe 269 -> 270 in-place upgrade with persisted Creator+Personal state.
  bash .github/scripts/backlot-rc25-android16.sh
  mv rc25-*.log "phase1-evidence/$profile/" || true
else
  adb install -r tests/app-debug.apk
  adb install -r tests/app-debug-androidTest.apk
fi
run_class() {
  local class="$1" short
  short="${class##*.}"
  local file="phase1-evidence/$profile/$short.txt"
  echo "PHASE1 START $profile : $short"
  local vprofile=default
  if test "$profile" = compact; then vprofile=compact; fi
  adb shell am instrument -w -r \
    -e class "$class" -e hardeningTestEnvironment ci-emulator \
    -e backlotVisualProfile "$vprofile" \
    com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner > "$file" 2>&1
  if ! grep -Eq 'OK \([1-9][0-9]* tests?\)' "$file"; then
    tail -90 "$file"
    echo "PHASE1 FAIL: $short"
    return 1
  fi
  grep -E '^OK \(|^Time: ' "$file" | tail -2
}
classes=(
  com.framebynavin.app.ui.V222ColdStartProjectsUiTest
  com.framebynavin.app.ui.V227IdeaFooterAdaptiveUiTest
  com.framebynavin.app.ui.V2203NewIdeaSafeFooterUiTest
  com.framebynavin.app.ui.V2204NewIdeaRootOverlayUiTest
  com.framebynavin.app.ui.V215HomeFirstFoldVisualUiTest
  com.framebynavin.app.ui.V215HomeSlideshowVisualUiTest
  com.framebynavin.app.ui.V220HomeConsolidationUiTest
  com.framebynavin.app.ui.V2214CompanionHomeUiTest
  com.framebynavin.app.ui.V2271PersonalEditorDeviceUiTest
  com.framebynavin.app.ui.V269DataPrivacyUiTest
  com.framebynavin.app.personal.V226PersonalBackupTransferTest
  com.framebynavin.app.data.V208PortableBackupIntegrityTest
  com.framebynavin.app.data.V209BackupCoverageTest
  com.framebynavin.app.data.V210BackupIoFailureTest
  com.framebynavin.app.data.V176ProjectPersistenceInstrumentedTest
  com.framebynavin.app.reminders.V268FullScreenPermissionFallbackTest
  com.framebynavin.app.widget.V2273WidgetMigrationUiTest
)
count=0
for class in "${classes[@]}"; do
  run_class "$class"
  count=$((count+1))
done
echo "PASS $profile $count instrumented classes" | tee "phase1-evidence/$profile/summary.txt"
# ONLY on disposable CI emulator: remove throwaway debug-install state and test
# the ACTUAL immutable signed Build270 preview owner artifact cold-booting.
adb uninstall com.framebynavin.app
adb install owner/Backlot-RC25-Build270-Full-App.apk
adb shell am start -n com.framebynavin.app/.MainActivity
sleep 9
adb shell pidof com.framebynavin.app | tee "phase1-evidence/$profile/owner-pid.txt"
adb exec-out screencap -p > "phase1-evidence/$profile/signed-Build270-cold-launch.png"
test -s "phase1-evidence/$profile/signed-Build270-cold-launch.png"
adb shell am force-stop com.framebynavin.app
adb shell am start -n com.framebynavin.app/.MainActivity
sleep 8
adb shell pidof com.framebynavin.app | tee "phase1-evidence/$profile/owner-relaunch-pid.txt"
adb exec-out screencap -p > "phase1-evidence/$profile/signed-Build270-relaunch.png"
test -s "phase1-evidence/$profile/signed-Build270-relaunch.png"
echo "PASS $profile: 17 UI/integrity classes + exact owner APK cold/relaunch" | tee -a "phase1-evidence/$profile/summary.txt"
