#!/usr/bin/env bash
set -euo pipefail
root=app/src/main/java/com/framebynavin/app/reminders
screen="$root/ReminderCheckInScreen.kt"
press="$root/V201ReminderPressFeedback.kt"
policy="$root/V201ReminderMotionPolicy.kt"
test -f "$press"
test -f "$policy"
test -f app/src/test/java/com/framebynavin/app/reminders/V201ReminderMotionPolicyTest.kt
test -f app/src/androidTest/java/com/framebynavin/app/reminders/V201ReminderFeedbackUiTest.kt
grep -Fq 'collectIsPressedAsState()' "$press"
grep -Fq 'animateFloatAsState(' "$press"
grep -Fq 'scaleX = scale' "$press"
grep -Fq 'scaleY = scale' "$press"
grep -Fq 'motionEnabled ->' "$policy" && { echo "Policy must not invoke callback logic"; exit 1; } || true
grep -Fq 'AnimatedVisibility(' "$screen"
grep -Fq 'expandVertically(' "$screen"
grep -Fq 'shrinkVertically(' "$screen"
grep -Fq 'v201PressFeedback(primaryPress, motionEnabled, prominent = true)' "$screen"
grep -Fq 'v201PressFeedback(snoozePress, motionEnabled)' "$screen"
grep -Fq 'v201PressFeedback(morePress, motionEnabled)' "$screen"
grep -Fq 'onClick = if (stageCheckIn) onStageDone else onWorking' "$screen"
grep -Fq 'onClick = onWorking' "$screen"
grep -Fq 'onClick = onSnooze' "$screen"
grep -Fq 'onClick = onPause' "$screen"
grep -Fq 'onClick = onReschedule' "$screen"
grep -Fq 'onClick = onReplay' "$screen"
grep -Fq 'onClick = onDismiss' "$screen"
! grep -Fq 'delay(' "$press"
! grep -Fq 'delay(' "$policy"
! grep -Fq 'LaunchedEffect' "$press"
echo 'V195.4 Phase 4C press, expand/collapse and synchronous callback contract passed.'
