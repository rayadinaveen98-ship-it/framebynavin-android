#!/usr/bin/env bash
set -Eeuo pipefail

# Execute every command in one Bash process under the isolated CI emulator.
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

# Reproduce the historical concurrent-write regression in isolation.
gradle :app:connectedDebugAndroidTest "${COMMON_ARGS[@]}" \
  '-Pandroid.testInstrumentationRunnerArguments.class=com.framebynavin.app.data.V18ReliabilityAlpha18InstrumentationTest#taskStore_serializesConcurrentReadModifyWrite'

# Verify the Studio scroll and callback regression independently.
gradle :app:connectedDebugAndroidTest "${COMMON_ARGS[@]}" \
  '-Pandroid.testInstrumentationRunnerArguments.class=com.framebynavin.app.ui.V18CoreInteractionUiTest#studio_expandFocusAndAdvanceDispatchCorrectProject'

# Run the complete instrumentation suite. A failing test must fail the job.
gradle :app:connectedDebugAndroidTest "${COMMON_ARGS[@]}"
