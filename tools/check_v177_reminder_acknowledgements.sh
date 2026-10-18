#!/usr/bin/env bash
set -euo pipefail
ROOT="app/src/main/java/com/framebynavin/app/reminders"
STORE="$ROOT/ReminderAcknowledgementStore.kt"
ACTIONS="$ROOT/ReminderActionReceiver.kt"
NOTIFICATIONS="$ROOT/ReminderNotifications.kt"
VOICE="$ROOT/VoiceReminderService.kt"
ALARM="$ROOT/AlarmRingingService.kt"
REGULAR="$ROOT/ReminderReceiver.kt"
SMART="$ROOT/EscalationReceiver.kt"
RECOVERY="$ROOT/ReminderRecoveryEngine.kt"
for file in "$STORE" "$ACTIONS" "$NOTIFICATIONS" "$VOICE" "$ALARM" "$REGULAR" "$SMART" "$RECOVERY"; do
  test -s "$file"
done

grep -Fq 'enum class ReminderAckState' "$STORE"
grep -Fq 'NOTIFICATION_POSTED' "$STORE"
grep -Fq 'UNANSWERED' "$STORE"
grep -Fq 'DELIVERY_FAILED' "$STORE"
grep -Fq 'RESPONDED' "$STORE"
grep -Fq 'MAX_PER_TASK = 12' "$STORE"
grep -Fq 'CreatorDataGate.generation(app)' "$STORE"
grep -Fq 'ReminderOccurrenceStore(app).matches(task, token)' "$STORE"
grep -Fq 'if (old == ReminderAckState.RESPONDED) return false' "$STORE"
grep -Fq 'if (last.occurrenceId != token' "$STORE"
grep -Fq 'markPosted(task.id, token)' "$NOTIFICATIONS"
grep -Fq 'acknowledgements.begin(task, token, "REGULAR")' "$REGULAR"
grep -Fq 'acknowledgements.begin(task, token, "SMART_' "$SMART"
grep -Fq 'markAlarmActive(task.id, token)' "$ALARM"
grep -Fq 'markUnanswered(task.id, token, "alarm_timeout")' "$ALARM"
grep -Fq 'markVoiceSpeaking(task.id, token)' "$VOICE"
grep -Fq 'markVoiceCompleted(task.id, token)' "$VOICE"
grep -Fq 'markUnanswered(task.id, token, "voice_timeout")' "$VOICE"
grep -Fq 'markResponded(taskId, token, action, now)' "$ACTIONS"
grep -Fq 'if (!accepted || updated == null)' "$ACTIONS"
grep -Fq 'stageRecovery?.finish(generation)' "$ACTIONS"
grep -Fq 'observeUnansweredNotification(task, nowMillis)' "$RECOVERY"
# Existing modes and timings must remain unchanged in Phase 1.
grep -Fq 'task.alarmTimeoutSeconds.coerceIn(30, 300)' "$ROOT/V178PlaybackLifecyclePolicy.kt"
grep -Fq 'task.voiceRepeatCount.coerceIn(1, 3)' "$VOICE"
grep -Fq 'scheduleNextIfUnanswered(task, stage, firedAt)' "$SMART"
grep -Fq 'val snoozeMinutes = CreatorOsSettingsStore(app).snapshot().snoozeMinutes' "$ACTIONS"
# The observation journal must never serialize personal project titles or notes.
if grep -Eq '\.put\("(title|notes|projectTitle|projectNotes)"' "$STORE"; then
  echo "Reminder acknowledgement store must remain free of project titles/notes" >&2
  exit 1
fi
test -f app/src/test/java/com/framebynavin/app/reminders/V177ReminderAcknowledgementPolicyTest.kt
test -f app/src/androidTest/java/com/framebynavin/app/reminders/V177ReminderAcknowledgementInstrumentedTest.kt
grep -Fq 'connectedDebugAndroidTest' .github/workflows/backlot-v177-reminder-ack-p1.yml
grep -Fq 'hardeningTestEnvironment=ci-emulator' .github/workflows/backlot-v177-reminder-ack-p1.yml
echo "V177 reminder acknowledgement, safety and privacy source contracts passed"
