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
grep -Fq 'versionCode = 160' app/build.gradle.kts
grep -Fq 'versionName = "2.1.0-v160-quick-actions-safety-p1-preview"' app/build.gradle.kts
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
test -f "$BASE/V156ReminderSetupUi.kt"
grep -Fq 'V156ReminderPlanPicker(' "$BASE/V20NewProjectWizard.kt"
grep -Fq 'V156ReminderPlanPicker(' "$BASE/V101BReminderUi.kt"
grep -Fq 'compact = true' "$BASE/V20NewProjectWizard.kt"
grep -Fq 'V156AdvancedReminderSettings(' "$BASE/V20NewProjectWizard.kt"
grep -Fq 'V156AdvancedReminderSettings(' "$BASE/V101BReminderUi.kt"
grep -Fq 'V20FieldLabel("REMIND ME AT")' "$BASE/V20NewProjectWizard.kt"
grep -Fq 'PComposerLabel("REMIND ME AT")' "$BASE/V101BReminderUi.kt"
grep -Fq 'ProjectAttentionPlan.entries.forEach' "$BASE/V156ReminderSetupUi.kt"
test -f app/src/test/java/com/framebynavin/app/ui/V156ReminderSetupCopyTest.kt
test -f app/src/androidTest/java/com/framebynavin/app/ui/V156ReminderSetupUiInstrumentedTest.kt
POLICY="app/src/main/java/com/framebynavin/app/data/ReminderCenterPolicy.kt"
CENTER="$BASE/V157ReminderCenterUi.kt"
MODEL="app/src/main/java/com/framebynavin/app/data/CreatorViewModel.kt"
test -f "$POLICY"
test -f "$CENTER"
grep -Fq 'enum class ReminderCenterGroup' "$POLICY"
grep -Fq 'val original = maxOf(nowMillis, task.reminderAtMillis)' "$POLICY"
grep -Fq 'ReminderActionSafety.fingerprint(task) == expectedFingerprint' "$POLICY"
grep -Fq 'V157ReminderCenterUi(' "$BASE/V131PolishUi.kt"
grep -Fq 'fun snoozeReminderFromCenter(' "$MODEL"
grep -Fq 'fun pauseReminderFromCenter(' "$MODEL"
grep -Fq 'fun rescheduleReminderFromCenter(' "$MODEL"
grep -Fq 'onSnoozeReminder = { task, minutes ->' "$BASE/FrameByNavinV101BApp.kt"
grep -Fq 'onPauseReminder = { task ->' "$BASE/FrameByNavinV101BApp.kt"
grep -Fq 'onRescheduleReminder = { task ->' "$BASE/FrameByNavinV101BApp.kt"
grep -Fq 'SNOOZE' "$CENTER"
grep -Fq 'CHANGE TIME' "$CENTER"
grep -Fq 'Pause for 2 hours' "$CENTER"
grep -Fq 'Turn off reminders' "$CENTER"
test -f app/src/test/java/com/framebynavin/app/data/ReminderCenterPolicyTest.kt
test -f app/src/androidTest/java/com/framebynavin/app/ui/V157ReminderCenterUiTest.kt
PREFS="app/src/main/java/com/framebynavin/app/data/CreatorOsSettings.kt"
QUIET="$REM/QuietHoursPolicy.kt"
CHECKIN="$REM/ReminderCheckInScreen.kt"
test -f "$QUIET"
grep -Fq 'fun active(' "$QUIET"
grep -Fq 'quietHoursEnabled: Boolean = false' "$PREFS"
grep -Fq 'quietHoursStartMinute' "$PREFS"
grep -Fq 'quietHoursEndMinute' "$PREFS"
grep -Fq '.put("quietHoursEnabled", value.quietHoursEnabled)' "$PREFS"
grep -Fq 'QUIET_CHANNEL_ID' "$REM/ReminderNotifications.kt"
grep -Fq '.setSilent(quiet)' "$REM/ReminderNotifications.kt"
grep -Fq 'onQuietHoursEnabled = {' "$BASE/FrameByNavinV101BApp.kt"
grep -Fq 'onQuietHoursWindow = { from, until ->' "$BASE/FrameByNavinV101BApp.kt"
grep -Fq 'V144SectionTitle("QUIET HOURS"' "$BASE/V144SettingsHub.kt"
grep -Fq '"MORE OPTIONS"' "$CHECKIN"
grep -Fq '"I\x27M WORKING"' "$CHECKIN" || grep -Fq "I'M WORKING" "$CHECKIN"
grep -Fq '"DISMISS FOR NOW"' "$CHECKIN"
grep -Fq 'engine.setLanguage(Locale.getDefault())' "$REM/VoiceReminderService.kt"
test -f app/src/test/java/com/framebynavin/app/reminders/QuietHoursPolicyV158Test.kt
test -f app/src/androidTest/java/com/framebynavin/app/reminders/V158ReminderCheckInUiTest.kt
test -f app/src/androidTest/java/com/framebynavin/app/data/V158QuietHoursSettingsInstrumentedTest.kt
grep -Fq 'testTag("reminder-plan-full-' "$BASE/V156ReminderSetupUi.kt"
grep -Fq 'testTag("reminder-plan-compact-' "$BASE/V156ReminderSetupUi.kt"
grep -Fq 'onNodeWithTag("reminder-plan-full-URGENT")' app/src/androidTest/java/com/framebynavin/app/ui/V156ReminderSetupUiInstrumentedTest.kt
grep -Fq 'rule.waitForIdle()' app/src/androidTest/java/com/framebynavin/app/ui/V156ReminderSetupUiInstrumentedTest.kt
for file in \
  app/src/androidTest/java/com/framebynavin/app/reminders/V159ReminderScheduleAcceptanceTest.kt \
  app/src/androidTest/java/com/framebynavin/app/reminders/V159ReminderActionIsolationTest.kt \
  app/src/androidTest/java/com/framebynavin/app/reminders/V159NotificationChannelAcceptanceTest.kt \
  app/src/androidTest/java/com/framebynavin/app/reminders/V159ReminderAccessibilityAcceptanceTest.kt; do
  test -f "$file"
done
# Quick Actions Phase 1 guardrails: no silent deletion of recurring work.
VM="app/src/main/java/com/framebynavin/app/data/CreatorViewModel.kt"
WEEK_STORE="app/src/main/java/com/framebynavin/app/data/WeeklyScheduleStore.kt"
BACKUP="app/src/main/java/com/framebynavin/app/data/CreatorBackupManager.kt"
PORTABLE="app/src/main/java/com/framebynavin/app/data/PortableCreatorBackupManager.kt"
! grep -Fq 'removeUnstartedWeeklyProjects' "$VM"
! grep -Fq 'WeeklyScheduleEngine.isLegacySeedSlot(it.scheduleSlotId)' "$VM"
! grep -Fq 'syncScheduledTask(' "$VM"
grep -Fq 'if (enabled) syncWeeklyScheduleInternal()' "$VM"
grep -Fq 'val resolved = migrated' "$WEEK_STORE"
grep -Fq 'const val SCHEMA_VERSION = 9' "$BACKUP"
grep -Fq '.put("automationPreferences", requireNotNull(snapshot.automationPrefsJson))' "$BACKUP"
grep -Fq 'PortableVoiceExportPolicy.requireReadableOwnedRecording(' "$PORTABLE"
grep -Fq 'in 8..CreatorBackupManager.SCHEMA_VERSION' "$PORTABLE"
grep -Fq 'sizeBeforeCopy' app/src/main/java/com/framebynavin/app/data/PortableBackupArchive.kt
grep -Fq 'enabled = autoPlanEnabled' "$BASE/V08WeeklyScheduleUi.kt"
test -f app/src/test/java/com/framebynavin/app/data/V160QuickActionsPreservationTest.kt
test -f app/src/androidTest/java/com/framebynavin/app/data/V160QuickActionsSafetyInstrumentedTest.kt
echo "V160 Quick Actions P1 creator-data safety + V154–V159 preservation checks passed"
