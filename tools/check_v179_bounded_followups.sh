#!/usr/bin/env bash
set -euo pipefail
ROOT="app/src/main/java/com/framebynavin/app/reminders"
POLICY="$ROOT/V179FollowUpPolicy.kt"
JOBS="$ROOT/V179ReminderFollowUps.kt"
CLAIMS="$ROOT/V179FollowUpClaimStore.kt"
NOTIFICATIONS="$ROOT/ReminderNotifications.kt"
ACTIONS="$ROOT/ReminderActionReceiver.kt"
SMART="$ROOT/SmartEscalationScheduler.kt"
VOICE="$ROOT/VoiceReminderService.kt"
ALARM="$ROOT/AlarmRingingService.kt"
for f in "$POLICY" "$JOBS" "$CLAIMS" "$NOTIFICATIONS" "$ACTIONS" "$SMART" "$VOICE" "$ALARM"; do test -s "$f"; done

# Explicit bound; no repeat loop or self-issued token, always quiet and original actions.
grep -Fq 'MAX_FOLLOW_UPS = 2' "$POLICY"
grep -Fq 'POSTED_GRACE_MILLIS = 20L * 60_000L' "$POLICY"
grep -Fq 'task.status != TaskStatus.DONE' "$POLICY"
grep -Fq 'task.reminderMode != ReminderMode.SMART' "$POLICY"
grep -Fq 'record.state == ReminderAckState.UNANSWERED' "$POLICY"
grep -Fq 'OneTimeWorkRequestBuilder<V179NoResponseWorker>()' "$JOBS"
grep -Fq '.setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)' "$JOBS"
grep -Fq 'ExistingWorkPolicy.KEEP' "$JOBS"
grep -Fq 'CreatorDataGate.readyTransaction(applicationContext)' "$JOBS"
grep -Fq 'ReminderOccurrenceStore(applicationContext)' "$JOBS"
grep -Fq 'if (!occurrences.matches(task, token))' "$JOBS"
grep -Fq 'V179FollowUpClaimStore(applicationContext).claim(task.id, token, attempt)' "$JOBS"
grep -Fq 'discreet = true' "$JOBS"
grep -Fq 'isFollowUp = true' "$JOBS"
grep -Fq 'V179FollowUpPolicy.next(attempt)' "$JOBS"
grep -Fq 'cancelAllWorkByTag(taskTag(taskId))' "$JOBS"
grep -Fq 'prefs.edit().putString(taskId, record.toString()).commit()' "$CLAIMS"
grep -Fq 'V179ReminderFollowUps.cancel(app, taskId)' "$ACTIONS"
grep -Fq 'V179FollowUpClaimStore(app).clearTask(taskId)' "$ACTIONS"
grep -Fq 'V179ReminderFollowUps.armForPostedNotification(context, task, token)' "$NOTIFICATIONS"
grep -Fq 'val quiet = discreet ||' "$NOTIFICATIONS"
grep -Fq '.setSilent(quiet)' "$NOTIFICATIONS"
grep -Fq 'if (!isFollowUp && task.reminderMode' "$NOTIFICATIONS"
grep -Fq 'V179ReminderFollowUps.armAfterUnanswered(' "$VOICE"
grep -Fq 'V179ReminderFollowUps.armAfterUnanswered(' "$ALARM"
grep -Fq 'finishSessionPreservingUnanswered' "$SMART"

# No new noisy service call or new occurrence token in the follow-up worker.
if grep -Eq 'VoiceReminderService.start|AlarmRingingService.start|occurrences.issue|ACTION_SNOOZE' "$JOBS"; then
  echo "V179 follow-up must not ring, snooze automatically or issue new reminder token" >&2
  exit 1
fi
test -f app/src/test/java/com/framebynavin/app/reminders/V179FollowUpPolicyTest.kt
test -f app/src/androidTest/java/com/framebynavin/app/reminders/V179FollowUpClaimInstrumentedTest.kt
test -f docs/qa/V179_BOUNDED_NO_RESPONSE_FOLLOWUPS.md
grep -Fq 'connectedDebugAndroidTest' .github/workflows/backlot-v179-followups-p3.yml
echo "V179 bounded, privacy-minimal, non-disruptive follow-up source guards passed"
