#!/usr/bin/env bash
set -euo pipefail
root=app/src/main/java/com/framebynavin/app/reminders
test -f "$root/V196ReminderLaunchReadiness.kt"
test -f "$root/V196ReminderResponseWindow.kt"
test -f app/src/test/java/com/framebynavin/app/reminders/V196ReminderReliabilityPolicyTest.kt
grep -Fq 'canUseFullScreenIntent()' "$root/V196ReminderLaunchReadiness.kt"
grep -Fq 'ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT' "$root/V196ReminderLaunchReadiness.kt"
grep -Fq 'V196ReminderResponseWindow.alarmActivityWatchdogMillis(task)' "$root/AlarmActivity.kt"
grep -Fq 'V196ReminderResponseWindow.voiceActivityWatchdogMillis(task)' "$root/VoiceReminderActivity.kt"
grep -Fq 'Full-screen alerts off · tap to enable' app/src/main/java/com/framebynavin/app/ui/V157ReminderCenterUi.kt
! grep -A45 'private fun finishUnansweredVoice' "$root/VoiceReminderService.kt" | grep -Fq 'ReminderSurfaceRegistry.close(task.id, token)'
! grep -A75 'val timeoutMillis = V178PlaybackLifecyclePolicy.alarmDurationMillis' "$root/AlarmRingingService.kt" | grep -Fq 'ReminderSurfaceRegistry.close(task.id, token)'
echo 'V195.1 notification/FSI checks and response-window guards passed'
