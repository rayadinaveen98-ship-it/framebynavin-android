#!/usr/bin/env bash
# CW6: disposable Android CI emulator ONLY. Never run against the owner's device.
set -euo pipefail
mkdir -p output/cw6-android-qa
OUT="output/cw6-android-qa"
PACKAGE="com.framebynavin.app"
RUNNER="com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner"

run_suite() {
  local class="$1" profile="$2" name="${1##*.}"
  local log="$OUT/${profile}-${name}.log"
  echo "[CW6] ${profile}: ${class}"
  adb shell am instrument -w -r \
    -e class "$class" \
    -e hardeningTestEnvironment ci-emulator \
    "$RUNNER" | tee "$log"
  if grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|INSTRUMENTATION_ABORTED|shortMsg=|Process crashed' "$log"; then
    echo "FAIL: $class on $profile" >&2
    return 1
  fi
  if ! grep -Eq '^OK \([1-9][0-9]* tests?\)' "$log"; then
    echo "FAIL: no positive Android runner result from $class on $profile" >&2
    return 1
  fi
}

screenshot() {
  local profile="$1" name="$2" remote="$3"
  local local_path="$OUT/${profile}-${name}.png"
  adb exec-out run-as "$PACKAGE" cat "$remote" > "$local_path"
  test -s "$local_path"
  file "$local_path"
}

# Private data must not be imported: both app and tests are fresh disposable
# emulator installs. Existing genuine user state is NEVER modified.
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell pm list packages | grep -Fq "$PACKAGE"

# Full Creator project+backup and both new native editors on normal phone size.
for suite in \
  com.framebynavin.app.data.CreatorStageWorkPersistenceAndroidTest \
  com.framebynavin.app.data.CreatorOutputCraftPersistenceAndroidTest \
  com.framebynavin.app.data.CreatorStageTransitionPersistenceAndroidTest \
  com.framebynavin.app.data.CW6IntegratedWorkspaceLifecycleAndroidTest \
  com.framebynavin.app.ui.CW3StageDeskUiTest \
  com.framebynavin.app.ui.CW4OutputCraftDeskUiTest \
  com.framebynavin.app.ui.Phase3ExplicitBeginProjectUiTest; do
  run_suite "$suite" phone
done
screenshot phone stage-evidence files/cw3-visuals/cw3-evidence-desk.png
screenshot phone derivative-reel files/cw4-qa/cw4-reel-script.png

# 320dp compact view; don't let scroll/keyboard layouts hide required actions.
adb shell wm size 320x750
adb shell wm density 160
adb shell am force-stop "$PACKAGE"
run_suite com.framebynavin.app.ui.CW3StageDeskUiTest compact320
run_suite com.framebynavin.app.ui.CW4OutputCraftDeskUiTest compact320
screenshot compact320 stage-evidence files/cw3-visuals/cw3-evidence-desk.png
screenshot compact320 derivative-reel files/cw4-qa/cw4-reel-script.png

# 800dp+ tablet-style workspace (same known emulator; no second hardware).
adb shell wm size 1200x1920
adb shell wm density 240
adb shell am force-stop "$PACKAGE"
run_suite com.framebynavin.app.ui.CW3StageDeskUiTest tablet800
run_suite com.framebynavin.app.ui.CW4OutputCraftDeskUiTest tablet800
screenshot tablet800 stage-evidence files/cw3-visuals/cw3-evidence-desk.png
screenshot tablet800 derivative-reel files/cw4-qa/cw4-reel-script.png

adb shell wm size reset
adb shell wm density reset

sha256sum "$OUT"/*.png > "$OUT/SCREENSHOT-SHA256SUMS.txt"
git rev-parse HEAD > "$OUT/SOURCE-SHA.txt"
echo "CW6 emulator QA complete; publication and owner release remain blocked pending review."
