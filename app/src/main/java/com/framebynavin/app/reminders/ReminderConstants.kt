package com.framebynavin.app.reminders

object ReminderConstants {
    const val CHANNEL_ID = "creator_reminders"
    const val EXTRA_TASK_ID = "task_id"
    const val EXTRA_TITLE = "title"
    const val EXTRA_PLATFORM = "platform"
    const val EXTRA_CONTENT_TYPE = "content_type"
    const val EXTRA_DUE_LABEL = "due_label"
    const val EXTRA_PRIORITY = "priority"
    const val EXTRA_NOTES = "notes"
    const val EXTRA_SCHEDULED_AT = "scheduled_at"
    const val ACTION_STARTED = "com.framebynavin.app.reminder.STARTED"
    const val ACTION_DONE = "com.framebynavin.app.reminder.DONE"
    const val ACTION_SNOOZE = "com.framebynavin.app.reminder.SNOOZE"
    const val SNOOZE_MINUTES = 10L
}
