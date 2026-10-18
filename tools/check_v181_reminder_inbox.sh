#!/usr/bin/env bash
set -euo pipefail
UI="app/src/main/java/com/framebynavin/app/ui"
REM="app/src/main/java/com/framebynavin/app/reminders"
for f in "$UI/V181ReminderInboxPolicy.kt" "$UI/V181ReminderInboxUi.kt" "$UI/V157ReminderCenterUi.kt" "$UI/V131PolishUi.kt" "$UI/FrameByNavinV101BApp.kt" "$REM/ReminderActionReceiver.kt" "$REM/ReminderAcknowledgementStore.kt" "$REM/V179FollowUpClaimStore.kt"; do test -s "$f"; done

grep -Fq 'ReminderActionSafety.isActionable(task)' "$UI/V181ReminderInboxPolicy.kt"
grep -Fq 'record.state != ReminderAckState.RESPONDED' "$UI/V181ReminderInboxPolicy.kt"
grep -Fq 'record.state == ReminderAckState.DELIVERY_FAILED' "$UI/V181ReminderInboxPolicy.kt"
grep -Fq 'V181ReminderResponseSection(' "$UI/V157ReminderCenterUi.kt"
grep -Fq 'V181ReminderResponseSection(' "$UI/V181ReminderInboxUi.kt"
grep -Fq 'journal.awaitingResponse(task)' "$UI/V181ReminderInboxUi.kt"
grep -Fq 'occurrences.matches(task, record.occurrenceId)' "$UI/V181ReminderInboxUi.kt"
grep -Fq 'V181ReminderInboxPolicy.failedDelivery' "$UI/V181ReminderInboxUi.kt"
grep -Fq 'V181PendingReminderCard(' "$UI/V181ReminderInboxUi.kt"
grep -Fq 'onAcknowledge(ReminderConstants.ACTION_STARTED)' "$UI/V181ReminderInboxUi.kt"
grep -Fq 'onAcknowledge(ReminderConstants.ACTION_SNOOZE)' "$UI/V181ReminderInboxUi.kt"
grep -Fq 'onAcknowledgeReminder = { task, token, action ->' "$UI/FrameByNavinV101BApp.kt"
grep -Fq 'ReminderActionReceiver::class.java' "$UI/FrameByNavinV101BApp.kt"
grep -Fq 'EXTRA_OCCURRENCE_ID, token' "$UI/FrameByNavinV101BApp.kt"
grep -Fq 'if (token.isBlank() || action !in acceptedActions)' "$REM/ReminderActionReceiver.kt"
grep -Fq '!occurrences.claim(task, token)' "$REM/ReminderActionReceiver.kt"
grep -Fq 'stageRecovery?.finish(generation)' "$REM/ReminderActionReceiver.kt"
grep -Fq 'V179ReminderFollowUps.cancel(app, taskId)' "$REM/ReminderActionReceiver.kt"
grep -Fq 'Integer.bitCount(obj.optInt("mask", 0) and 3)' "$REM/V179FollowUpClaimStore.kt"

# In-app history is read only and never stores titles in the acknowledgement journal.
if grep -Eq 'TaskStore\(|store.updateTask\(|occurrences.issue\(' "$UI/V181ReminderInboxUi.kt"; then
  echo "Inbox UI may only dispatch existing authorized actions, not write task data" >&2
  exit 1
fi
if grep -Eq '\.put\("(title|notes|projectTitle|projectNotes)"' "$REM/ReminderAcknowledgementStore.kt"; then
  echo "Acknowledgement history must not store project contents" >&2
  exit 1
fi

test -f app/src/test/java/com/framebynavin/app/ui/V181ReminderInboxPolicyTest.kt
test -f app/src/androidTest/java/com/framebynavin/app/ui/V181ReminderInboxUiTest.kt
test -f docs/qa/V181_REMINDER_INBOX_HISTORY_P5.md
grep -Fq 'connectedDebugAndroidTest' .github/workflows/backlot-v181-reminder-inbox-p5.yml
echo "V181 current-token inbox and activity-history safeguards passed"
