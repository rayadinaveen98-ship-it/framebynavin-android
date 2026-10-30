#!/usr/bin/env bash
set -euo pipefail
APP="$(find apks -type f -name app-debug.apk -print -quit)"
TEST="$(find apks -type f -name app-debug-androidTest.apk -print -quit)"
test -s "$APP" && test -s "$TEST"
for name in creator.fbnbackup personal.json manifest.json; do test -s "transfer/$name"; done

# New API36 AVD: no earlier Android 11 app data or session is installed.
adb install -r "$APP"
adb install -r "$TEST"
adb shell run-as com.framebynavin.app mkdir -p files/rc15_cross_install
for name in creator.fbnbackup personal.json manifest.json; do
  # Pipe synthetic test-only archive bytes into app-private storage. No account
  # data, cloud credentials, original owner device or public artifacts involved.
  adb shell "run-as com.framebynavin.app sh -c 'cat > files/rc15_cross_install/$name'" < "transfer/$name"
done

adb shell am instrument -w -r -e class 'com.framebynavin.app.data.RC15CrossInstallPortableTransferTest#phase2ImportIntoFreshAndroid16' -e hardeningTestEnvironment ci-emulator com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner | tee rc15-receiver.log
grep -Fq 'OK (1 test)' rc15-receiver.log
echo 'PASS cross-install isolated recovery from Android11 export to fresh Android16'
