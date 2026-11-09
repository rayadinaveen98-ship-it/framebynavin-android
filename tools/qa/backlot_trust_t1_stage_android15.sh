#!/usr/bin/env bash
# Only run after the Trust T1 stage gate is explicitly prepared.
# Single disposable Android 15 emulator; NO cloud login or account operations.
set -euo pipefail
mkdir -p output/trust-t1-stage
adb wait-for-device
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
for spec in \
  "com.framebynavin.app.cloud.TrustT1ALegacyCloudSafetyDeviceTest TRUST-T1A-REMOTE 1" \
  "com.framebynavin.app.cloud.TrustT1BInventoryDeviceTest TRUST-T1B-INVENTORY 1" \
  "com.framebynavin.app.cloud.TrustT1B3VoiceSparkDeviceTest TRUST-T1B3-PREFS 2" \
  "com.framebynavin.app.cloud.TrustT1B4ReviewDeviceTest TRUST-T1B4-STABLE-READ 1" \
  "com.framebynavin.app.personal.Phase1HabitArchiveCoverageGuardDeviceTest TRUST-T1-ARCHIVE 1"; do
  set -- $spec
  cls="$1"; name="$2"; count="$3"
  timeout 20m adb shell am instrument -w -r \
    -e class "$cls" -e hardeningTestEnvironment ci-emulator \
    com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
    > "output/trust-t1-stage/$name.txt" 2>&1 || {
      cat "output/trust-t1-stage/$name.txt" >&2
      exit 1
    }
  cat "output/trust-t1-stage/$name.txt"
  if grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|INSTRUMENTATION_ABORTED|Process crashed' "output/trust-t1-stage/$name.txt"; then exit 1; fi
  grep -Fq "OK ($count test)" "output/trust-t1-stage/$name.txt" ||
    grep -Fq "OK ($count tests)" "output/trust-t1-stage/$name.txt"
done
echo "PASS: remote gate + voice/Spark inventory + v2 archive receipts" \
  > output/trust-t1-stage/T1-RESULTS.txt
