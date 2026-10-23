#!/usr/bin/env bash
# Disposable Android CI emulator ONLY. This is not a user-device test.
# android-emulator-runner executes script: entries one physical line at a
# time, so the multi-line shell flow MUST run through this single Bash file.
set -euo pipefail

package=com.framebynavin.app
test_class=com.framebynavin.app.data.V210LiveRestoreKillTest
runner=com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner
active_log=v210-live-stop-active.log
recovery_log=v210-live-stop-recover.log
boundary_path=files/v210_live_restore_kill_qa/images_committed
pending_path=files/creator_restore_recovery_v181/pending.json

# Capture the *live test* logcat separately from earlier acceptance suites.
# Without this, Android prints only "Process crashed" in instrumentation output.
adb logcat -c
# Bound any unresponsive instrumentation process to 180 seconds.
timeout 180s adb shell am instrument -w -r \
  -e class "${test_class}#enterRealRestoreAfterPublishingImages" \
  -e hardeningTestEnvironment ci-emulator "${runner}" > "${active_log}" 2>&1 &
instrument_pid=$!

cleanup() {
  if kill -0 "${instrument_pid}" 2>/dev/null; then
    adb shell am force-stop "${package}" >/dev/null 2>&1 || true
    kill "${instrument_pid}" 2>/dev/null || true
    wait "${instrument_pid}" 2>/dev/null || true
  fi
}
trap cleanup EXIT

boundary_reached=0
for attempt in $(seq 1 45); do
  if adb shell run-as "${package}" sh -c "test -f ${boundary_path}" >/dev/null 2>&1; then
    boundary_reached=1
    break
  fi
  if ! kill -0 "${instrument_pid}" 2>/dev/null; then
    break
  fi
  sleep 1
done

if [[ "${boundary_reached}" -ne 1 ]]; then
  echo "FAIL: live restore did not commit image order and publish checkpoint"
  cat "${active_log}" || true
  adb logcat -d -v threadtime > v210-live-stop-logcat.txt || true
  echo "Recent Android fatal/crash/ANR events:"
  grep -E -i -C 8 'FATAL EXCEPTION|Process: com.framebynavin.app|OutOfMemoryError|ANR in com.framebynavin.app|am_crash|native crash' v210-live-stop-logcat.txt | tail -n 180 || true
  echo "App process status:"
  adb shell pidof "${package}" || true
  echo "Recovery fixture path status:"
  adb shell run-as "${package}" sh -c 'pwd; ls -l files/v210_live_restore_kill_qa; ls -l files/creator_restore_recovery_v181/pending.json' || true
  echo "Durable live-restore test stage (if setup began):"
  adb shell run-as "${package}" cat files/v210_live_restore_kill_qa/progress.txt || true
  exit 1
fi

# The image commit callback reports completion only after pending journal was
# written; separately confirm the *on-disk* pending file is still present.
adb shell run-as "${package}" sh -c "test -f ${pending_path}"
echo "Confirmed live image commit and pending rollback journal; force-stopping target."
adb shell am force-stop "${package}"
wait "${instrument_pid}" || true

# A forced stop is supposed to abort active instrumentation; that is not itself
# a test failure. The journal MUST remain for the fresh process to recover.
adb shell run-as "${package}" sh -c "test -f ${pending_path}"
echo "Confirmed pending journal survived forced stop."

adb shell am instrument -w -r \
  -e class "${test_class}#recoverRealTransactionInFreshProcess" \
  -e hardeningTestEnvironment ci-emulator "${runner}" | tee "${recovery_log}"
if grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|INSTRUMENTATION_ABORTED|shortMsg=|Process crashed' "${recovery_log}"; then
  echo "FAIL: recovery in fresh app process did not complete cleanly"
  exit 1
fi
grep -Fq 'OK (1 test)' "${recovery_log}"
echo "PASS: actual compact restore survived in-flight process force-stop and recovered in new process."
trap - EXIT
