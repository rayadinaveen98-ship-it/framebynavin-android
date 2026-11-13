#!/usr/bin/env bash
set -euo pipefail
mkdir -p output/package-j-reset/device-results
adb install -r output/package-j-reset/app-debug.apk
adb install -r output/package-j-reset/app-debug-androidTest.apk
# The runner invokes 'script' through /bin/sh, so all compound bash work
# belongs in this single file instead of fragmented YAML shell lines.
adb shell am instrument -w -r \
  -e class com.framebynavin.app.ui.V383DeviceResetSafetyAndroidTest \
  -e hardeningTestEnvironment ci-emulator \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
  | tee output/package-j-reset/device-results/RESET-SAFETY.log
! grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|INSTRUMENTATION_ABORTED|shortMsg=|Process crashed' output/package-j-reset/device-results/RESET-SAFETY.log
grep -Eq '^OK \([1-9][0-9]* tests?\)' output/package-j-reset/device-results/RESET-SAFETY.log
