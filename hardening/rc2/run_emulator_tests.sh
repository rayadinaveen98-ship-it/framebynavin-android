#!/usr/bin/env bash
set -Eeuo pipefail

# This script is executed only by the isolated Android CI emulator job.
# The emulator runner invokes each `script` line separately, so all shell
# functions, traps, and test commands must live in this single file.
mkdir -p hardening/diagnostics

capture_diagnostics() {
  local result=$?
  trap - EXIT
  if (( result != 0 )); then
    echo "Android instrumentation failed (exit ${result}); collecting diagnostics."
    adb logcat -d -v threadtime -t 12000 > hardening/diagnostics/logcat.txt 2>&1 || true
    adb shell dumpsys meminfo com.framebynavin.app > hardening/diagnostics/meminfo.txt 2>&1 || true
    adb shell dumpsys activity processes > hardening/diagnostics/processes.txt 2>&1 || true
  fi
  exit "$result"
}
trap capture_diagnostics EXIT

COMMON_ARGS=(
  --stacktrace
  -Pandroid.testInstrumentationRunnerArguments.hardeningTestEnvironment=ci-emulator
)

# First reproduce the historical concurrent-write regression in isolation.
gradle :app:connectedDebugAndroidTest "${COMMON_ARGS[@]}" \
  '-Pandroid.testInstrumentationRunnerArguments.class=com.framebynavin.app.data.V18ReliabilityAlpha18InstrumentationTest#taskStore_serializesConcurrentReadModifyWrite'

# Then run every instrumentation test. No failing or skipped test is silently
# converted to success; the Gradle exit status remains the release gate.
gradle :app:connectedDebugAndroidTest "${COMMON_ARGS[@]}"
