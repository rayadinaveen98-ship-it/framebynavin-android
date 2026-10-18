#!/usr/bin/env bash
set -euo pipefail
UI="app/src/main/java/com/framebynavin/app/ui"
DATA="app/src/main/java/com/framebynavin/app/data"
R="app/src/main/java/com/framebynavin/app/reminders"
for file in "$UI/V180ReminderTimingControls.kt" "$UI/V20NewProjectWizard.kt" "$UI/V101BReminderUi.kt" "$UI/V144SettingsHub.kt" "$UI/FrameByNavinV101BApp.kt" "$DATA/CreatorOsSettings.kt" "$R/V179ReminderFollowUps.kt"; do
  test -s "$file"
done

# Creation, Quick Create and edit reuse existing persisted draft fields.
grep -Fq 'onVoiceRepeatCount: (Int) -> Unit' "$UI/V20NewProjectWizard.kt"
grep -Fq 'onVoiceRepeatCount = { repeatCount = it }' "$UI/V101BReminderUi.kt"
grep -Fq 'onVoiceRepeatGapSeconds = { repeatGap = it }' "$UI/V101BReminderUi.kt"
grep -Fq 'onAlarmTimeoutSeconds = { alarmTimeout = it }' "$UI/V101BReminderUi.kt"
test "$(grep -Fc 'V180ReminderTimingControls(' "$UI/V20NewProjectWizard.kt")" -eq 2
grep -Fq 'V180ReminderTimingControls(' "$UI/V101BReminderUi.kt"
grep -Fq 'voiceRepeatCount = repeatCount' "$UI/V101BReminderUi.kt"
grep -Fq 'voiceRepeatIntervalSeconds = repeatGap' "$UI/V101BReminderUi.kt"
grep -Fq 'alarmTimeoutSeconds = alarmTimeout' "$UI/V101BReminderUi.kt"

# Settings toggle persists, backs up/restores, and affects the WORKER (not merely the UI).
grep -Fq 'val unansweredFollowUpsEnabled: Boolean = true' "$DATA/CreatorOsSettings.kt"
grep -Fq 'prefs.getBoolean(KEY_NO_RESPONSE_FOLLOWUPS, true)' "$DATA/CreatorOsSettings.kt"
grep -Fq '.put("unansweredFollowUpsEnabled", value.unansweredFollowUpsEnabled)' "$DATA/CreatorOsSettings.kt"
grep -Fq 'obj.optBoolean("unansweredFollowUpsEnabled", true)' "$DATA/CreatorOsSettings.kt"
grep -Fq '.putBoolean(KEY_NO_RESPONSE_FOLLOWUPS, value.unansweredFollowUpsEnabled)' "$DATA/CreatorOsSettings.kt"
grep -Fq 'setUnansweredFollowUpsEnabled(value: Boolean)' "$DATA/CreatorOsSettings.kt"
grep -Fq 'v180-unanswered-followups-toggle' "$UI/V144SettingsHub.kt"
grep -Fq 'onUnansweredFollowUps = { enabled ->' "$UI/FrameByNavinV101BApp.kt"
grep -Fq 'V179ReminderFollowUps.cancelAll(context)' "$UI/FrameByNavinV101BApp.kt"
grep -Fq 'if (!enabled(applicationContext)) return Result.success()' "$R/V179ReminderFollowUps.kt"
grep -Fq 'if (!enabled(applicationContext)) return@readyTransaction' "$R/V179ReminderFollowUps.kt"
grep -Fq 'cancelAllWorkByTag(PREFIX)' "$R/V179ReminderFollowUps.kt"

# Preserve no loud retry policy and unchanged action-token authority.
grep -Fq 'discreet = true' "$R/V179ReminderFollowUps.kt"
grep -Fq 'isFollowUp = true' "$R/V179ReminderFollowUps.kt"
grep -Fq 'if (!occurrences.matches(task, token))' "$R/V179ReminderFollowUps.kt"
grep -Fq 'MAX_FOLLOW_UPS = 2' "$R/V179FollowUpPolicy.kt"
test -f app/src/test/java/com/framebynavin/app/ui/V180ReminderTimingPolicyTest.kt
test -f app/src/androidTest/java/com/framebynavin/app/ui/V180ReminderTimingUiTest.kt
test -f docs/qa/V180_REMINDER_CONTROLS_P4.md
grep -Fq 'connectedDebugAndroidTest' .github/workflows/backlot-v180-reminder-controls-p4.yml
echo "V180 creator timing, preference, backup and no-response safety safeguards passed"
