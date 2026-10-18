#!/usr/bin/env bash
set -euo pipefail
classes="com.framebynavin.app.data.V185IdeaProjectConversionTest,com.framebynavin.app.ui.V185IdeaConversionUiTest,com.framebynavin.app.data.V184QuickCaptureDraftTest,com.framebynavin.app.ui.V184QuickCaptureRecoveryUiTest,com.framebynavin.app.ui.V161QuickActionsUiTest,com.framebynavin.app.data.CreatorQuickCaptureWriterV183Test,com.framebynavin.app.reminders.V182ReminderChannelIsolationInstrumentedTest"
adb shell am instrument -w -r -e class "$classes" -e hardeningTestEnvironment ci-emulator com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner | tee v185-instrumentation.log
adb logcat -d > v185-logcat.txt
adb exec-out screencap -p > v185-emulator-screen.png || true
adb pull /sdcard/Android/data/com.framebynavin.app/files/v185-conversion-dialog.png v185-conversion-dialog.png || true
if grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|INSTRUMENTATION_ABORTED|shortMsg=|Process crashed' v185-instrumentation.log; then
  exit 1
fi
grep -Eq 'OK \(22 tests\)' v185-instrumentation.log
