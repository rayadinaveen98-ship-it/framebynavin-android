#!/usr/bin/env bash
set -euo pipefail
test "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" = "1" || {
  echo 'BLOCKED: Phase B2 recovery fixture may run only on a disposable Android emulator' >&2
  exit 1
}
# Re-run all 33 Phase A safety regressions on this *new* source head.
bash .github/scripts/backlot-stabilization-mutation-fence-android15.sh

# The Phase B2 fixture deliberately leaves a PREPARED journal in app-private
# storage. Run it only after clearing the DISPOSABLE emulator and never on a
# real owner phone; its assertion proves ordinary writers are frozen.
adb shell pm clear com.framebynavin.app
log="$(mktemp)"
trap 'rm -f "$log"' EXIT
adb shell am instrument -w -r \
  -e hardeningTestEnvironment ci-emulator \
  -e class com.framebynavin.app.personal.B02OwnerPrecommitDeviceTest \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner | tee "$log"
grep -Fq 'OK (1 test)' "$log"
if grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' "$log"; then
  exit 1
fi
echo 'PASS: 33 inherited recovery tests + B2 authenticated synthetic precommit/freeze fixture'

# B3: run the real five-domain additive transaction on a *different* cleared
# synthetic emulator profile. It MUST retain a VERIFYING journal afterward.
adb shell pm clear com.framebynavin.app
adb shell am instrument -w -r \
  -e hardeningTestEnvironment ci-emulator \
  -e class com.framebynavin.app.personal.B03AdditiveRestoreDeviceTest \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner | tee "$log"
# Last run's output must independently prove the transaction passed.
grep -Fq 'OK (1 test)' "$log"
if grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' "$log"; then
  exit 1
fi
echo 'PASS: Android B3 synthetic additive Personal/Challenge/Group/Space/JPEG transaction'

# B4: a different disposable profile simulates process death after the
# Personal write and BEFORE_CHALLENGES checkpoint; no owner phone is touched.
adb shell pm clear com.framebynavin.app
adb shell am instrument -w -r \
  -e hardeningTestEnvironment ci-emulator \
  -e class com.framebynavin.app.personal.B04NoPhotoForwardResumeDeviceTest \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner | tee "$log"
grep -Fq 'OK (1 test)' "$log"
if grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' "$log"; then
  exit 1
fi
echo 'PASS: B04 no-photo owner-authenticated forward replay and journal finalization'
