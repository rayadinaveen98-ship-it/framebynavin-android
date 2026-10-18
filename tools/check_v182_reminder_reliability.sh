#!/usr/bin/env bash
set -euo pipefail
ROOT="app/src/main/java/com/framebynavin/app"
REM="$ROOT/reminders"
DATA="$ROOT/data"
for file in "$REM/ReminderDeliveryPolicy.kt" "$REM/ReminderScheduler.kt" "$REM/ReminderRecoveryEngine.kt" "$REM/MissedReminderRecovery.kt" "$REM/SmartEscalationScheduler.kt" "$REM/ReminderNotifications.kt" "$REM/V179ReminderFollowUps.kt" "$REM/ReminderActionReceiver.kt" "$REM/VoiceReminderService.kt" "$REM/AlarmRingingService.kt" "$DATA/CreatorViewModel.kt"; do
  test -s "$file"
done

# No archived, completed or disabled project may reactivate through any reminder entry point.
grep -Fq 'ReminderActionSafety.isActionable(task)' "$REM/ReminderDeliveryPolicy.kt"
grep -Fq 'val active = ReminderActionSafety.isActionable(task)' "$REM/ReminderRecoveryEngine.kt"
grep -Fq 'val active = ReminderActionSafety.isActionable(task)' "$REM/MissedReminderRecovery.kt"
grep -Fq 'if (!ReminderActionSafety.isActionable(task)' "$REM/ReminderScheduler.kt"
grep -Fq 'ReminderActionSafety.isActionable(task)' "$REM/SmartEscalationScheduler.kt"
grep -Fq 'if (!ReminderActionSafety.isActionable(task)' "$DATA/CreatorViewModel.kt"

# Explicit edit/cancel or archived recovery clears obsolete WorkManager check-ins.
grep -Fq 'V179ReminderFollowUps.cancel(appContext, task.id)' "$REM/ReminderRecoveryEngine.kt"
test "$(grep -Fc 'V179ReminderFollowUps.cancel(getApplication<Application>(), task.id)' "$DATA/CreatorViewModel.kt")" -eq 1
grep -Fq 'V179ReminderFollowUps.cancel(getApplication<Application>(), taskId)' "$DATA/CreatorViewModel.kt"

# Follow-ups and end-of-sound handoff never create another audible alert.
grep -Fq 'fun canPost(context: Context, discreet: Boolean = false)' "$REM/ReminderNotifications.kt"
grep -Fq 'if (discreet) QUIET_CHANNEL_ID else ReminderConstants.CHANNEL_ID' "$REM/ReminderNotifications.kt"
grep -Fq 'if (!canPost(context, discreet = quiet)) return false' "$REM/ReminderNotifications.kt"
grep -Fq 'canPost(applicationContext, discreet = true)' "$REM/V179ReminderFollowUps.kt"
grep -Fq 'discreet = true' "$REM/VoiceReminderService.kt"
grep -Fq 'discreet = true' "$REM/AlarmRingingService.kt"
grep -Fq 'discreet = true' "$REM/VoiceReminderActivity.kt"
grep -Fq 'discreet = true' "$REM/AlarmActivity.kt"
grep -Fq 'MAX_FOLLOW_UPS = 2' "$REM/V179FollowUpPolicy.kt"
grep -Fq 'V179FollowUpClaimStore(applicationContext).claim(task.id, token, attempt)' "$REM/V179ReminderFollowUps.kt"

# All creator responses still go through the authenticated receiver and persist before acknowledging.
grep -Fq 'if (token.isBlank() || action !in acceptedActions)' "$REM/ReminderActionReceiver.kt"
grep -Fq '!occurrences.claim(task, token)' "$REM/ReminderActionReceiver.kt"
grep -Fq 'stageRecovery?.finish(generation)' "$REM/ReminderActionReceiver.kt"
grep -Fq 'ReminderAcknowledgementStore(app).markResponded(taskId, token, action, now)' "$REM/ReminderActionReceiver.kt"

test -f app/src/test/java/com/framebynavin/app/reminders/V182ReminderLifecycleRegressionTest.kt
test -f app/src/androidTest/java/com/framebynavin/app/reminders/V182ReminderChannelIsolationInstrumentedTest.kt
test -f docs/qa/V182_REMINDER_RELIABILITY_P6_ACCEPTANCE.md
grep -Fq 'connectedDebugAndroidTest' .github/workflows/backlot-v182-reminder-reliability-p6.yml
echo "V182 archive, quiet channel, token, persistence and bounded retry checks passed"
