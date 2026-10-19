#!/usr/bin/env bash
set -euo pipefail
src=app/src/main/java/com/framebynavin/app/reminders
screen="$src/ReminderCheckInScreen.kt"
policy="$src/V200ReminderLayoutPolicy.kt"
test -f "$policy"
test -f app/src/test/java/com/framebynavin/app/reminders/V200ReminderLayoutPolicyTest.kt
test -f app/src/androidTest/java/com/framebynavin/app/reminders/V200ReminderUiTest.kt
grep -Fq 'V200ReminderLayoutPolicy.isCompact(screen.screenHeightDp)' "$screen"
grep -Fq 'V200ReminderLayoutPolicy.useHorizontalActions(screen.screenWidthDp)' "$screen"
grep -Fq 'V199ReminderSpark(' "$screen"
grep -Fq 'V195ReminderParticleScene(' "$screen"
grep -Fq 'v200-primary-reminder-action' "$screen"
grep -Fq 'onClick = if (stageCheckIn) onStageDone else onWorking' "$screen"
grep -Fq 'onClick = onSnooze' "$screen"
grep -Fq 'onClick = onPause' "$screen"
grep -Fq 'onClick = onReschedule' "$screen"
grep -Fq 'onClick = onDismiss' "$screen"
grep -Fq 'onClick = onReplay' "$screen"
! grep -Fq 'WHAT HAPPENS NEXT' "$screen"
! grep -Fq '"UP NEXT"' "$screen"
! grep -Fq 'ReminderResponseCopy.followUp(' "$screen"
! grep -Fq 'delay(2_100L)' "$screen"
echo 'V195.4 Phase 4B minimal, responsive reminder controls contract passed.'
