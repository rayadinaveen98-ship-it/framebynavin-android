#!/usr/bin/env bash
set -euo pipefail
BASE="app/src/main/java/com/framebynavin/app/ui"
for file in V144SettingsHub.kt V19ContentWorkspaceAlpha3Ui.kt V19ContentWorkspaceAlpha4Ui.kt V19ContentWorkspaceAlpha5Ui.kt V19ContentWorkspaceAlpha6Ui.kt V19ContentWorkspaceUi.kt; do
  if grep -nE 'Funny and Cute|CONTENT PROJECT 2.0|SCRIPT STUDIO · RC2|PUBLISH STUDIO · RC2|DNA BLUEPRINT' "$BASE/$file"; then
    echo "Obsolete copy remains in $file" >&2
    exit 1
  fi
done
grep -Fq 'PBottomNavItem(PTab.CREATE, Icons.Outlined.MovieEdit, "Projects"' "$BASE/FrameByNavinV101BApp.kt"
grep -Fq 'V144CategoryCard("Spark", "Your creative companion"' "$BASE/V144SettingsHub.kt"
grep -Fq 'Text("VIDEO REVIEW"' "$BASE/V20VideoPostmortemUi.kt"
grep -Fq 'versionCode = 155' app/build.gradle.kts
grep -Fq 'versionName = "2.0.5-v155-reminder-reliability-r1-preview"' app/build.gradle.kts
grep -Fq 'var showAdvancedSetup by rememberSaveable(startWithQuickCreate) { mutableStateOf(!startWithQuickCreate) }' "$BASE/V20NewProjectWizard.kt"
grep -Fq 'onQuickCreate = { openComposer(quick = true) }' "$BASE/FrameByNavinV101BApp.kt"
grep -Fq 'Text("QUICK CREATE"' "$BASE/V131PolishUi.kt"
grep -Fq 'PROCESSING_SAFETY_DAYS = 2L' app/src/main/java/com/framebynavin/app/youtube/YouTubeAnalyticsQueryPolicy.kt
grep -Fq 'YouTubeAnalyticsQueryPolicy.shouldRefresh(' "$BASE/V11YouTubeInsights.kt"
REM="app/src/main/java/com/framebynavin/app/reminders"
grep -Fq 'SmartDeliveryDecision.RECOVER_LATE' "$REM/ReminderDeliveryPolicy.kt"
grep -Fq 'SmartDeliveryDecision.RECOVER_LATE ->' "$REM/EscalationReceiver.kt"
grep -Fq 'scheduler.cancelPendingOnly(task.id)' "$REM/ReminderRecoveryEngine.kt"
grep -Fq 'smart.cancelPendingOnly(task.id)' "$REM/ReminderRecoveryEngine.kt"
grep -Fq 'fallbackToNotification(task, token' "$REM/VoiceReminderService.kt"
grep -Fq 'onOpenReminderSettings = { showReminders = false; overlay = POverlay.SETTINGS }' "$BASE/FrameByNavinV101BApp.kt"
test -f app/src/test/java/com/framebynavin/app/reminders/ReminderR1ReliabilityTest.kt
test -f app/src/androidTest/java/com/framebynavin/app/reminders/V155ReminderRecoveryIsolationInstrumentedTest.kt
echo "V155 reminder reliability and V154 regression smoke checks passed"
