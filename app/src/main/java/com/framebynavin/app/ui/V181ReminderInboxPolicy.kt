package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.reminders.ReminderAckRecord
import com.framebynavin.app.reminders.ReminderAckState
import com.framebynavin.app.reminders.ReminderActionSafety

/**
 * V181 displays evidence, not guessed engagement. An Android notification is never a
 * read receipt, and alarm/voice service activation is not proof of audibility.
 */
internal object V181ReminderInboxPolicy {
    fun actionable(task: CreatorTask, record: ReminderAckRecord?, tokenCurrent: Boolean): Boolean =
        record != null && tokenCurrent && ReminderActionSafety.isActionable(task) &&
            record.state != ReminderAckState.RESPONDED &&
            record.state != ReminderAckState.DELIVERY_FAILED

    fun failedDelivery(task: CreatorTask, record: ReminderAckRecord?, tokenCurrent: Boolean): Boolean =
        record != null && tokenCurrent && ReminderActionSafety.isActionable(task) &&
            record.state == ReminderAckState.DELIVERY_FAILED

    fun status(state: ReminderAckState): String = when (state) {
        ReminderAckState.ATTEMPTED -> "Preparing reminder"
        ReminderAckState.NOTIFICATION_POSTED -> "Notification sent to Android · no response yet"
        ReminderAckState.VOICE_ACTIVE -> "Voice alert started · no response yet"
        ReminderAckState.VOICE_SPEAKING -> "Voice is speaking · no response yet"
        ReminderAckState.VOICE_PLAYBACK_FINISHED -> "Voice finished · no response yet"
        ReminderAckState.ALARM_ACTIVE -> "Alarm started · no response yet"
        ReminderAckState.UNANSWERED -> "Needs your response"
        ReminderAckState.DELIVERY_FAILED -> "Delivery problem · check Android permissions"
        ReminderAckState.RESPONDED -> "You responded"
    }

    fun recentLabel(record: ReminderAckRecord): String = when (record.state) {
        ReminderAckState.RESPONDED -> when (record.reason) {
            "com.framebynavin.app.reminder.SNOOZE" -> "Snoozed by you"
            "com.framebynavin.app.reminder.STARTED" -> "Working confirmed"
            "com.framebynavin.app.reminder.PAUSE" -> "Paused by you"
            "com.framebynavin.app.reminder.RESCHEDULE" -> "Time changed by you"
            "com.framebynavin.app.reminder.STAGE_DONE" -> "Stage finished by you"
            "com.framebynavin.app.reminder.DISMISS", "com.framebynavin.app.reminder.DONE" -> "Dismissed by you"
            else -> "Action confirmed"
        }
        ReminderAckState.UNANSWERED -> if (record.reason == "superseded_without_response")
            "Replaced by a newer reminder" else "No response recorded"
        ReminderAckState.DELIVERY_FAILED -> "Android could not deliver alert"
        ReminderAckState.NOTIFICATION_POSTED -> "Notification posted"
        ReminderAckState.VOICE_ACTIVE, ReminderAckState.VOICE_SPEAKING -> "Voice attempted"
        ReminderAckState.VOICE_PLAYBACK_FINISHED -> "Voice playback completed"
        ReminderAckState.ALARM_ACTIVE -> "Alarm attempted"
        ReminderAckState.ATTEMPTED -> "Delivery attempted"
    }
}
