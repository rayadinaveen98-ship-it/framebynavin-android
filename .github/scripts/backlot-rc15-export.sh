#!/usr/bin/env bash
set -euo pipefail

APP="$(find apks -type f -name app-debug.apk -print -quit)"
TEST="$(find apks -type f -name app-debug-androidTest.apk -print -quit)"
test -s "$APP" && test -s "$TEST"
adb install -r "$APP"
adb install -r "$TEST"
adb shell am instrument -w -r -e class 'com.framebynavin.app.data.RC15CrossInstallPortableTransferTest#phase1ExportFromAndroid11' -e hardeningTestEnvironment ci-emulator com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner | tee rc15-sender.log
grep -Fq 'OK (1 test)' rc15-sender.log

mkdir -p transfer
for name in creator.fbnbackup personal.json manifest.json; do
  # run-as reads only this synthetic fixture from the disposable emulator.
  adb exec-out run-as com.framebynavin.app cat "files/rc15_cross_install/$name" > "transfer/$name"
  test -s "transfer/$name"
done
python3 - <<'PY'
import hashlib, json
from pathlib import Path
p=Path("transfer")
m=json.loads((p/"manifest.json").read_text())
assert m["schema"]=="RC15CrossInstallSyntheticV1"
for name, key in (("creator.fbnbackup","creatorSha256"),("personal.json","personalSha256")):
    assert hashlib.sha256((p/name).read_bytes()).hexdigest()==m[key], name
assert all(x.is_file() for x in p.iterdir())
print("PASS sender binary transfer receipts and SHA-256 integrity")
PY
