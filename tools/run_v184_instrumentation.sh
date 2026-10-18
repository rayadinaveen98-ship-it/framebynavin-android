#!/usr/bin/env bash
set -euo pipefail
classes="com.framebynavin.app.data.V184QuickCaptureDraftTest,com.framebynavin.app.ui.V184QuickCaptureRecoveryUiTest,com.framebynavin.app.ui.V161QuickActionsUiTest,com.framebynavin.app.data.CreatorQuickCaptureWriterV183Test,com.framebynavin.app.reminders.V182ReminderChannelIsolationInstrumentedTest"
adb shell am instrument -w -r -e class "$classes" -e hardeningTestEnvironment ci-emulator com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner | tee v184-instrumentation.log
adb logcat -d > v184-logcat.txt
adb exec-out screencap -p > v184-emulator-screen.png || true
if grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|INSTRUMENTATION_ABORTED|shortMsg=|Process crashed' v184-instrumentation.log; then
  exit 1
fi
grep -Eq 'OK \(14 tests\)' v184-instrumentation.log
