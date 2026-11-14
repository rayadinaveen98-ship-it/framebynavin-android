#!/usr/bin/env bash
set -euo pipefail
mkdir -p output/rollback/device
(
  cd output/rollback/debug
  sha256sum -c SHA256SUMS.txt
)
adb install -r output/rollback/debug/app-debug.apk
adb install -r output/rollback/debug/app-debug-androidTest.apk
# Only presentation and NON-DESTRUCTIVE advanced local-erase tests. Do not
# invoke clearApplicationUserData() or alter a real owner's device.
adb shell am instrument -w -r \
  -e class com.framebynavin.app.ui.V167SettingsNavigationUiTest,com.framebynavin.app.ui.V170BackupSettingsUiTest,com.framebynavin.app.ui.V170LocalBackupScreenUiTest,com.framebynavin.app.ui.V197SettingsOnboardingUiTest,com.framebynavin.app.ui.V269DataPrivacyUiTest,com.framebynavin.app.ui.V341UnifiedBackupPanelUiTest,com.framebynavin.app.ui.V383DeviceResetSafetyAndroidTest \
  -e hardeningTestEnvironment ci-emulator \
  com.framebynavin.app.test/androidx.test.runner.AndroidJUnitRunner \
  | tee output/rollback/device/SETTINGS-AND-ERASE.log
! grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|INSTRUMENTATION_ABORTED|shortMsg=|Process crashed' output/rollback/device/SETTINGS-AND-ERASE.log
grep -Eq '^OK \([1-9][0-9]* tests?\)' output/rollback/device/SETTINGS-AND-ERASE.log
