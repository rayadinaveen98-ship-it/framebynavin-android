#!/usr/bin/env bash
# Android 11 native source/UI regression. Disposable CI emulator only.
set -euo pipefail
mkdir -p output/v324-android11
adb wait-for-device
adb shell getprop sys.boot_completed | grep -qx 1
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk

for spec in \
  "com.framebynavin.app.ui.Phase3SmartNextMoveUiTest PHASE3D 3" \
  "com.framebynavin.app.ui.Phase3HomeCompositionReviewTest PHASE3E 2" \
  "com.framebynavin.app.ui.V220HomeConsolidationUiTest HOME-3AB 5" \
  "com.framebynavin.app.ui.V215HomeFirstFoldVisualUiTest HOME-NAV 4" \
  "com.framebynavin.app.ui.CreatorFocusSessionScreenUiTest FOCUS-3C 2"; do
  set -- $spec
  cls="$1"; log="$2"; count="$3"
  timeout 13m adb shell am instrument -w -r \
    -e class "$cls" -e hardeningTestEnvironment ci-emulator \
    -e backlotVisualProfile default \
    com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
    > "output/v324-android11/$log.txt" 2>&1 || { cat "output/v324-android11/$log.txt" >&2; exit 1; }
  cat "output/v324-android11/$log.txt"
  grep -Fq "OK ($count tests)" "output/v324-android11/$log.txt"
done

for scene in empty-150 bilingual-200; do
  adb exec-out run-as com.framebynavin.app cat "files/phase3e-composition/${scene}.png" > "output/v324-android11/3E-${scene}.png"
  test -s "output/v324-android11/3E-${scene}.png"
done
echo "PASS: Android11 Home and Focus regression with 1.5x/2x font" > output/v324-android11/PHASE3F-ANDROID11-ACCEPTANCE.txt
