#!/usr/bin/env bash
set -euo pipefail
ROOT="app/src/main/java/com/framebynavin/app/reminders"
VOICE="$ROOT/VoiceReminderService.kt"
ALARM="$ROOT/AlarmRingingService.kt"
VSCREEN="$ROOT/VoiceReminderActivity.kt"
ASCREEN="$ROOT/AlarmActivity.kt"
POLICY="$ROOT/V178PlaybackLifecyclePolicy.kt"
ACK="$ROOT/ReminderAcknowledgementStore.kt"
for f in "$VOICE" "$ALARM" "$VSCREEN" "$ASCREEN" "$POLICY" "$ACK"; do test -s "$f"; done

# Sound must end at the last utterance plus a very short creator action grace.
grep -Fq 'VOICE_FINISH_GRACE_MILLIS = 1_500L' "$POLICY"
grep -Fq 'voiceWatchdogMillis(task)' "$VOICE"
grep -Fq 'finishUnansweredVoice(task, token, "voice_playback_finished_no_response")' "$VOICE"
grep -Fq 'finishUnansweredVoice(task, token, "voice_watchdog_timeout")' "$VOICE"
# Playback completion must NOT close an already-open response scene. Failure-path closures remain allowed.
! sed -n '/private fun finishUnansweredVoice(/,/private fun utteranceIndex/p' "$VOICE" | grep -Fq 'ReminderSurfaceRegistry.close(task.id, token)'
grep -Fq 'markVoiceCompleted(task.id, token)' "$VOICE"
grep -Fq 'stageLabel = "Voice finished · response still needed"' "$VOICE"
# There should no longer be a fixed full-window wait after the first TTS speak.
! grep -Fq '{ stopVoiceService(task.id, token) }' "$VOICE"
# The voice response scene has its own bounded window after the sound lifecycle.
grep -Fq 'V196ReminderResponseWindow.voiceActivityWatchdogMillis(task)' "$VSCREEN"
grep -Fq 'AFTER_SOUND_GRACE_MILLIS = 45_000L' "$ROOT/V196ReminderResponseWindow.kt"
grep -Fq 'stageLabel = "Voice screen timed out · response still needed"' "$VSCREEN"

# Alarm duration is still creator-owned, but its end is not a dismissal.
grep -Fq 'alarmTimeoutSeconds.coerceIn(30, 300)' "$POLICY"
grep -Fq 'val timeoutMillis = V178PlaybackLifecyclePolicy.alarmDurationMillis(task)' "$ALARM"
grep -Fq 'stopCurrent()' "$ALARM"
grep -Fq 'stageLabel = "Alarm ended · response still needed"' "$ALARM"
grep -Fq 'markUnanswered(task.id, token, "alarm_timeout")' "$ALARM"
# Ring duration does not imply user acknowledgement. No premature scene closure.
! sed -n '/val timeoutMillis = V178PlaybackLifecyclePolicy.alarmDurationMillis(task)/,/}, timeoutMillis)/p' "$ALARM" | grep -Fq 'ReminderSurfaceRegistry.close(task.id, token)'
grep -Fq 'V196ReminderResponseWindow.alarmActivityWatchdogMillis(task)' "$ASCREEN"
grep -Fq 'stageLabel = "Alarm screen timed out · response still needed"' "$ASCREEN"

# No new tokens, background retries, or automatic terminal response on audio completion.
grep -Fq 'occurrenceId = token' "$VOICE"
grep -Fq 'occurrenceId = token' "$ALARM"
grep -Fq 'if (old == ReminderAckState.RESPONDED) return false' "$ACK"
test -f app/src/test/java/com/framebynavin/app/reminders/V178PlaybackLifecyclePolicyTest.kt
test -f app/src/androidTest/java/com/framebynavin/app/reminders/V178UnansweredHandoffInstrumentedTest.kt
grep -Fq 'connectedDebugAndroidTest' .github/workflows/backlot-v178-reminder-playback-p2.yml
echo "V178 playback deadlines and V195.1 separate response-scene safety guards passed"
