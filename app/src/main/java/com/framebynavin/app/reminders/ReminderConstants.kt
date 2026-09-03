package com.framebynavin.app.reminders

object ReminderConstants {
    const val CHANNEL_ID = "creator_reminders"
    const val ALARM_CHANNEL_ID = "creator_alarms_v1"
    const val EXTRA_TASK_ID = "task_id"
    const val EXTRA_TITLE = "title"
    const val EXTRA_PLATFORM = "platform"
    const val EXTRA_CONTENT_TYPE = "content_type"
    const val EXTRA_DUE_LABEL = "due_label"
    const val EXTRA_PRIORITY = "priority"
    const val EXTRA_NOTES = "notes"
    const val EXTRA_SCHEDULED_AT = "scheduled_at"
    const val EXTRA_TARGET_AT = "target_at"
    const val EXTRA_ALERT_TYPE = "alert_type"
    const val EXTRA_ALARM_SOUND_URI = "alarm_sound_uri"
    const val EXTRA_VOICE_ENABLED = "voice_enabled"
    const val EXTRA_ESCALATION_STAGE = "escalation_stage"
    const val ACTION_STARTED = "com.framebynavin.app.reminder.STARTED"
    const val ACTION_DONE = "com.framebynavin.app.reminder.DONE"
    const val ACTION_SNOOZE = "com.framebynavin.app.reminder.SNOOZE"
    const val SNOOZE_MINUTES = 10L
    const val WORKING_QUIET_MINUTES = 15L
}
