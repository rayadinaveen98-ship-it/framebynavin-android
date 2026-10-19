#!/usr/bin/env bash
set -euo pipefail
src=app/src/main/java/com/framebynavin/app/reminders
test -f "$src/V199SparkMotionPolicy.kt"
test -f "$src/V199ReminderSpark.kt"
test -f app/src/test/java/com/framebynavin/app/reminders/V199SparkMotionPolicyTest.kt
test -f app/src/androidTest/java/com/framebynavin/app/reminders/V199ReminderSparkUiTest.kt
grep -Fq 'V199ReminderSpark(' "$src/ReminderCheckInScreen.kt"
grep -Fq 'CinePulseGuide(' "$src/V199ReminderSpark.kt"
grep -Fq 'CinePulseState.POINT else CinePulseState.PRESENT' "$src/V199ReminderSpark.kt"
grep -Fq 'v199-spark-alarm-fx' "$src/V199ReminderSpark.kt"
grep -Fq 'v199-spark-voice-fx' "$src/V199ReminderSpark.kt"
grep -Fq 'motionEnabled && !lowPower' "$src/V199ReminderSpark.kt"
grep -Fq 'if (animate) V199SparkMotionPolicy.scale(t) else 1f' "$src/V199ReminderSpark.kt"
grep -Fq 'v195-voice-guide' "$src/ReminderCheckInScreen.kt"
grep -Fq 'v195-alarm-guide' "$src/ReminderCheckInScreen.kt"
grep -Fq 'V197ReminderRevealPolicy.startRevealed(motionEnabled)' "$src/ReminderCheckInScreen.kt"
# Do not let a mascot-only visual update mutate reminder delivery or responses.
! grep -Eq 'AlarmRingingService|VoiceReminderService|ReminderScheduler|ReminderActionReceiver|NotificationManager' "$src/V199ReminderSpark.kt"
echo 'V195.4 Spark pose, accessibility, and visual-only contracts passed.'
